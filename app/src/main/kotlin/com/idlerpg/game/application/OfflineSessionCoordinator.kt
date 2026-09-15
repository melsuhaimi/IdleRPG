package com.idlerpg.game.application

import com.idlerpg.game.core.id.ContentId
import com.idlerpg.game.core.number.GameNumber
import com.idlerpg.game.core.time.GameClock
import com.idlerpg.game.core.time.GameDuration
import com.idlerpg.game.data.local.SaveEnvelope
import com.idlerpg.game.data.local.SaveVersion
import com.idlerpg.game.data.repository.GameRepository
import com.idlerpg.game.domain.definition.CurrencyId
import com.idlerpg.game.domain.definition.world.EncounterType
import com.idlerpg.game.domain.engine.EngineContext
import com.idlerpg.game.domain.engine.EngineDiagnostics
import com.idlerpg.game.domain.engine.EngineResult
import com.idlerpg.game.domain.engine.SimulationActionLimitExceededException
import com.idlerpg.game.domain.engine.SimulationEngine
import com.idlerpg.game.domain.event.CurrencyGranted
import com.idlerpg.game.domain.event.ExperienceGranted
import com.idlerpg.game.domain.event.GameEventEnvelope
import com.idlerpg.game.domain.event.PlayerLeveledUp
import com.idlerpg.game.domain.model.GameState
import com.idlerpg.game.domain.model.combat.CombatState
import com.idlerpg.game.domain.model.world.EncounterState
import com.idlerpg.game.domain.model.world.EncounterStatus
import com.idlerpg.game.domain.model.world.WorldAutomationMode

/**
 * Policy for a device wall clock that is earlier than the save timestamp.
 *
 * [CLAMP_TO_ZERO] grants no offline progression and preserves the previous save timestamp
 * so moving the clock backwards cannot create a new earlier progression anchor.
 */
enum class OfflineClockRollbackPolicy {
    CLAMP_TO_ZERO,
    REJECT
}

/** Raised when rollback policy explicitly rejects a backwards device clock. */
class OfflineClockRollbackException(
    savedAtEpochMs: Long,
    currentEpochMs: Long
) : IllegalStateException(
    "Current wall clock precedes save timestamp: " +
        "savedAtEpochMs=" + savedAtEpochMs + ", currentEpochMs=" + currentEpochMs
)

/**
 * Retained for compatibility with the return-screen vocabulary. The offline contract does
 * not currently expose tactical advice because offline simulation cannot change encounter
 * progress or award combat-side systems.
 */
enum class OfflineTacticalInsight {
    ADAPTATION_PRESSURE,
    PROTECTOR_BLOCKING,
    ADAPTIVE_RESISTANCE,
    CASTER_DISRUPTION,
    SWARM_PRESSURE,
    SURVIVAL_PRESSURE
}

data class OfflineNotableDrop(
    val displayName: String,
    val rarity: com.idlerpg.game.domain.definition.Rarity
)

/**
 * Summary of one offline advancement.
 *
 * The only offline gameplay rewards are Gold and XP. The remaining fields are retained as
 * zero-valued compatibility fields for existing presentation clients; they never imply that
 * loot, mastery, quests, achievements, or stage progress were granted offline.
 */
data class OfflineProgressSummary(
    val requestedElapsed: GameDuration,
    val simulatedElapsed: GameDuration,
    val durationClamped: Boolean,
    val clockRollbackDetected: Boolean,
    val enemiesDefeated: GameNumber,
    val encountersCleared: GameNumber,
    val goldGranted: GameNumber,
    val experienceGranted: GameNumber,
    val masteryGranted: GameNumber,
    val itemsFound: GameNumber,
    val itemsKept: GameNumber,
    val itemsOverflowed: GameNumber,
    val itemsAutoSalvaged: GameNumber,
    val autoSalvageGold: GameNumber,
    val eliteEncountersCleared: GameNumber,
    val anomalyEncountersCleared: GameNumber,
    val bossesDefeated: GameNumber,
    val convergencesTriggered: GameNumber,
    val adaptationTierChanges: GameNumber,
    val startingStage: Int?,
    val endingStage: Int?,
    val deepestStage: Int?,
    val currentWallStage: Int?,
    val notableDrops: List<OfflineNotableDrop>,
    val tacticalInsight: OfflineTacticalInsight?,
    val eventCount: Int
) {
    init {
        require(simulatedElapsed <= requestedElapsed) {
            "simulatedElapsed cannot exceed requestedElapsed"
        }
        require(eventCount >= 0) {
            "eventCount cannot be negative: " + eventCount
        }
        require(notableDrops.isEmpty()) {
            "Offline summaries cannot contain item drops"
        }
        require(listOfNotNull(startingStage, endingStage, deepestStage, currentWallStage).all { it > 0 }) {
            "Offline stage numbers must be positive"
        }
    }
}

/** Result accepted by the future GameSession after a save has been resumed and checkpointed. */
data class OfflineResumeResult(
    val engineResult: EngineResult,
    val summary: OfflineProgressSummary,
    val sourceWrittenAtEpochMs: Long,
    val checkpointWrittenAtEpochMs: Long
) {
    val state: GameState
        get() = engineResult.state

    val events: List<GameEventEnvelope>
        get() = engineResult.events
}

/**
 * Wall-time -> deterministic offline simulation bridge.
 *
 * Offline progression deliberately reuses the canonical simulation engine for combat timing,
 * RNG, and Gold/XP formulas, but projects its result through the offline contract before
 * returning or saving it:
 *
 * - farm the latest cleared non-boss encounter in the active region;
 * - retain only Gold and XP changes;
 * - preserve gear, materials, quests, mastery, achievements, and stage progress;
 * - cap the simulated duration through BalanceConfig.
 */
class OfflineSessionCoordinator(
    private val repository: GameRepository,
    private val clock: GameClock,
    private val engineContext: EngineContext,
    private val clockRollbackPolicy: OfflineClockRollbackPolicy =
        OfflineClockRollbackPolicy.CLAMP_TO_ZERO
) {

    /**
     * Loads and resumes the current save, or returns null when no save exists.
     *
     * A successful advancement is persisted before this method returns. This prevents the
     * same saved wall-time interval from being granted repeatedly if the process closes
     * immediately after resume but before a later autosave exists.
     */
    fun resume(): OfflineResumeResult? {
        val sourceEnvelope = repository.load() ?: return null
        val before = sourceEnvelope.gameState()
        val nowEpochMs = clock.nowEpochMs()

        require(nowEpochMs >= 0L) {
            "GameClock returned negative epoch milliseconds: " + nowEpochMs
        }

        val elapsed = calculateElapsed(
            savedAtEpochMs = sourceEnvelope.writtenAtEpochMs,
            currentEpochMs = nowEpochMs
        )
        val maximum = engineContext.balanceConfig.maximumOfflineDuration
        val simulatedElapsed = if (elapsed.requested > maximum) maximum else elapsed.requested

        val canonicalResult = advanceExact(
            state = offlineSimulationState(before),
            duration = simulatedElapsed
        )
        val engineResult = projectOfflineResult(
            before = before,
            canonicalResult = canonicalResult
        )

        val expectedTime = before.engine.simulationTime + simulatedElapsed
        check(engineResult.state.engine.simulationTime == expectedTime) {
            "Offline simulation did not advance to the expected simulation time"
        }

        val summary = summarize(
            requestedElapsed = elapsed.requested,
            simulatedElapsed = simulatedElapsed,
            durationClamped = elapsed.requested > simulatedElapsed,
            clockRollbackDetected = elapsed.clockRollbackDetected,
            events = engineResult.events,
            before = before,
            after = engineResult.state
        )

        // When the clock moved backwards and policy clamps to zero, retain the original
        // anchor instead of moving the save timestamp backwards.
        val checkpointWrittenAtEpochMs = if (elapsed.clockRollbackDetected) {
            sourceEnvelope.writtenAtEpochMs
        } else {
            nowEpochMs
        }

        val checkpoint = SaveEnvelope.create(
            gameState = engineResult.state,
            contentVersion = sourceEnvelope.contentVersion,
            writtenAtEpochMs = checkpointWrittenAtEpochMs,
            schemaVersion = SaveVersion.CURRENT
        )

        if (engineResult.state != before ||
            checkpointWrittenAtEpochMs != sourceEnvelope.writtenAtEpochMs
        ) {
            repository.save(checkpoint)
        }

        return OfflineResumeResult(
            engineResult = engineResult,
            summary = summary,
            sourceWrittenAtEpochMs = sourceEnvelope.writtenAtEpochMs,
            checkpointWrittenAtEpochMs = checkpointWrittenAtEpochMs
        )
    }

    /**
     * Builds the canonical input for offline farming. A synthetic terminal encounter lets
     * EncounterSystem start the selected farm target without changing the saved world. The
     * projected result below discards every world/combat mutation from this temporary run.
     */
    private fun offlineSimulationState(state: GameState): GameState {
        val world = state.run.world
        val activeRegion = world.activeRegionId?.let(engineContext.contentRegistry::regionOrNull)
        val target = activeRegion
            ?.encounterIds
            ?.asReversed()
            ?.firstOrNull { candidate ->
                candidate in world.clearedEncounterIds &&
                    engineContext.contentRegistry.encounterOrNull(candidate)?.type != EncounterType.BOSS
            }

        val farmingWorld = if (target == null || activeRegion == null) {
            world.copy(
                currentEncounter = null,
                automationMode = WorldAutomationMode.FARM,
                selectedFarmEncounterId = null
            )
        } else {
            val encounterIndex = activeRegion.encounterIds.indexOf(target)
                .takeIf { it >= 0 }
                ?.plus(1)
                ?.toLong()
                ?: 0L
            world.copy(
                currentEncounter = EncounterState(
                    definitionId = target,
                    encounterIndex = encounterIndex,
                    encounterSeed = 0L,
                    status = EncounterStatus.CLEARED
                ),
                automationMode = WorldAutomationMode.FARM,
                selectedFarmEncounterId = target
            )
        }

        return state.copy(
            run = state.run.copy(
                world = farmingWorld,
                combat = CombatState()
            )
        )
    }

    /**
     * Keeps only the deterministic engine cursor, Gold balance, and player XP/level from the
     * canonical result. All other run and meta partitions remain byte-for-byte equivalent to
     * the saved state.
     */
    private fun projectOfflineResult(
        before: GameState,
        canonicalResult: EngineResult
    ): EngineResult {
        val simulatedGold =
            canonicalResult.state.run.economy.wallet.amountsByCurrencyId[CurrencyId.GOLD]
                ?: GameNumber.ZERO
        val walletAmounts = before.run.economy.wallet.amountsByCurrencyId.toMutableMap()
        if (simulatedGold == GameNumber.ZERO) {
            walletAmounts.remove(CurrencyId.GOLD)
        } else {
            walletAmounts[CurrencyId.GOLD] = simulatedGold
        }

        val projectedRun = before.run.copy(
            economy = before.run.economy.copy(
                wallet = before.run.economy.wallet.copy(
                    amountsByCurrencyId = walletAmounts
                )
            ),
            progression = before.run.progression.copy(
                playerLevel = canonicalResult.state.run.progression.playerLevel
            )
        )
        val projectedState = before.copy(
            engine = canonicalResult.state.engine,
            run = projectedRun,
            meta = before.meta
        )

        return canonicalResult.copy(
            state = projectedState,
            events = canonicalResult.events.filter(::isAllowedOfflineEvent)
        )
    }

    private fun isAllowedOfflineEvent(envelope: GameEventEnvelope): Boolean =
        when (val event = envelope.event) {
            is ExperienceGranted,
            is PlayerLeveledUp -> true
            is CurrencyGranted -> event.currencyId == CurrencyId.GOLD
            else -> false
        }

    /**
     * Runs the canonical SimulationEngine. If a long exact advancement hits the engine's
     * safety ceiling, split the duration deterministically and continue with the resulting
     * canonical state.
     */
    private fun advanceExact(
        state: GameState,
        duration: GameDuration
    ): EngineResult =
        try {
            SimulationEngine.advance(
                state = state,
                duration = duration,
                context = engineContext
            )
        } catch (limit: SimulationActionLimitExceededException) {
            if (duration.millis <= 1L) throw limit

            val firstMillis = duration.millis / 2L
            val secondMillis = duration.millis - firstMillis
            val first = advanceExact(state, GameDuration.ofMillis(firstMillis))
            val second = advanceExact(first.state, GameDuration.ofMillis(secondMillis))

            EngineResult(
                state = second.state,
                events = first.events + second.events,
                commandResult = null,
                diagnostics = EngineDiagnostics(
                    processedScheduledActions = Math.addExact(
                        first.diagnostics.processedScheduledActions,
                        second.diagnostics.processedScheduledActions
                    )
                )
            )
        }

    private fun calculateElapsed(
        savedAtEpochMs: Long,
        currentEpochMs: Long
    ): CalculatedElapsed {
        if (currentEpochMs >= savedAtEpochMs) {
            return CalculatedElapsed(
                requested = GameDuration.ofMillis(currentEpochMs - savedAtEpochMs),
                clockRollbackDetected = false
            )
        }

        return when (clockRollbackPolicy) {
            OfflineClockRollbackPolicy.CLAMP_TO_ZERO ->
                CalculatedElapsed(
                    requested = GameDuration.ZERO,
                    clockRollbackDetected = true
                )
            OfflineClockRollbackPolicy.REJECT ->
                throw OfflineClockRollbackException(
                    savedAtEpochMs = savedAtEpochMs,
                    currentEpochMs = currentEpochMs
                )
        }
    }

    /**
     * Projects the offline result into its return-screen summary. Only CurrencyGranted(GOLD)
     * and ExperienceGranted are counted; all other compatibility counters stay at zero.
     */
    internal fun summarize(
        requestedElapsed: GameDuration,
        simulatedElapsed: GameDuration,
        durationClamped: Boolean,
        clockRollbackDetected: Boolean,
        events: List<GameEventEnvelope>,
        before: GameState,
        after: GameState
    ): OfflineProgressSummary {
        var goldGranted = GameNumber.ZERO
        var experienceGranted = GameNumber.ZERO
        for (envelope in events) {
            when (val event = envelope.event) {
                is CurrencyGranted -> if (event.currencyId == CurrencyId.GOLD) {
                    goldGranted += event.amount
                }
                is ExperienceGranted -> experienceGranted += event.amount
                else -> Unit
            }
        }

        val stage = stageForState(before) ?: stageForState(after)
        return OfflineProgressSummary(
            requestedElapsed = requestedElapsed,
            simulatedElapsed = simulatedElapsed,
            durationClamped = durationClamped,
            clockRollbackDetected = clockRollbackDetected,
            enemiesDefeated = GameNumber.ZERO,
            encountersCleared = GameNumber.ZERO,
            goldGranted = goldGranted,
            experienceGranted = experienceGranted,
            masteryGranted = GameNumber.ZERO,
            itemsFound = GameNumber.ZERO,
            itemsKept = GameNumber.ZERO,
            itemsOverflowed = GameNumber.ZERO,
            itemsAutoSalvaged = GameNumber.ZERO,
            autoSalvageGold = GameNumber.ZERO,
            eliteEncountersCleared = GameNumber.ZERO,
            anomalyEncountersCleared = GameNumber.ZERO,
            bossesDefeated = GameNumber.ZERO,
            convergencesTriggered = GameNumber.ZERO,
            adaptationTierChanges = GameNumber.ZERO,
            startingStage = stage,
            endingStage = stage,
            deepestStage = stage,
            currentWallStage = null,
            notableDrops = emptyList(),
            tacticalInsight = null,
            eventCount = events.size
        )
    }

    private fun stageForState(state: GameState): Int? {
        state.run.world.currentEncounter?.definitionId?.let { return stageForEncounter(it) }
        return state.run.world.clearedEncounterIds.mapNotNull(::stageForEncounter).maxOrNull()
    }

    private fun stageForEncounter(encounterId: ContentId): Int? {
        val encounter = engineContext.contentRegistry.encounterOrNull(encounterId) ?: return null
        val index = engineContext.contentRegistry.region(encounter.regionId).encounterIds.indexOf(encounterId)
        return index.takeIf { it >= 0 }?.plus(1)
    }

    private data class CalculatedElapsed(
        val requested: GameDuration,
        val clockRollbackDetected: Boolean
    )
}
