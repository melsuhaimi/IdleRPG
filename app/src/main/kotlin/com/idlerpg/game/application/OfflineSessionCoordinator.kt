package com.idlerpg.game.application

import com.idlerpg.game.core.id.ContentId
import com.idlerpg.game.core.number.GameNumber
import com.idlerpg.game.core.time.GameClock
import com.idlerpg.game.core.time.GameDuration
import com.idlerpg.game.data.local.SaveEnvelope
import com.idlerpg.game.data.local.SaveVersion
import com.idlerpg.game.data.repository.GameRepository
import com.idlerpg.game.domain.definition.CurrencyId
import com.idlerpg.game.domain.definition.Rarity
import com.idlerpg.game.domain.definition.progression.FeatureUnlockScope
import com.idlerpg.game.domain.definition.world.EncounterType
import com.idlerpg.game.domain.engine.EngineContext
import com.idlerpg.game.domain.engine.EngineDiagnostics
import com.idlerpg.game.domain.engine.EngineResult
import com.idlerpg.game.domain.engine.SimulationActionLimitExceededException
import com.idlerpg.game.domain.engine.SimulationEngine
import com.idlerpg.game.domain.event.CurrencyGranted
import com.idlerpg.game.domain.event.ExperienceGranted
import com.idlerpg.game.domain.event.FeatureUnlocked
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

/** Why offline advancement ended. */
enum class OfflineStoppingReason {
    ELAPSED,
    CLAIM_WINDOW_CAPPED,
    CLOCK_ROLLBACK,
    NO_ELIGIBLE_FARM_STAGE
}

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
    val rarity: Rarity
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
    val startingLevel: Long = 1L,
    val endingLevel: Long = 1L,
    val stoppingReason: OfflineStoppingReason = OfflineStoppingReason.ELAPSED,
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
        require(startingLevel > 0L && endingLevel > 0L) {
            "Offline levels must be positive"
        }
        require(endingLevel >= startingLevel) {
            "Offline level cannot decrease"
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

    /** Loads the current migrated save without simulating wall time. */
    fun loadSavedState(): GameState? = repository.load()?.gameState()

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
        val durationClamped = elapsed.requested > maximum
        val simulatedElapsed = if (durationClamped) maximum else elapsed.requested
        val farmTarget = latestEligibleFarmEncounter(before)

        // A return in the same wall-clock millisecond must not execute a scheduled action at
        // the current simulation time. There is no elapsed offline interval to claim, so keep
        // the saved state and event cursor untouched.
        val canonicalResult = if (simulatedElapsed == GameDuration.ZERO) {
            EngineResult(
                state = before,
                events = emptyList(),
                commandResult = null,
                diagnostics = EngineDiagnostics(processedScheduledActions = 0)
            )
        } else {
            advanceExact(
                state = offlineSimulationState(before, farmTarget),
                duration = simulatedElapsed
            )
        }
        val engineResult = projectOfflineResult(
            before = before,
            canonicalResult = canonicalResult,
            simulatedElapsed = simulatedElapsed
        )

        val expectedTime = before.engine.simulationTime + simulatedElapsed
        check(engineResult.state.engine.simulationTime == expectedTime) {
            "Offline simulation did not advance to the expected simulation time"
        }

        val summary = summarize(
            requestedElapsed = elapsed.requested,
            simulatedElapsed = simulatedElapsed,
            durationClamped = durationClamped,
            clockRollbackDetected = elapsed.clockRollbackDetected,
            stoppingReason = stoppingReasonFor(
                clockRollbackDetected = elapsed.clockRollbackDetected,
                durationClamped = durationClamped,
                farmTarget = farmTarget
            ),
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

    /** Selects the latest cleared non-boss encounter in the active region. */
    private fun latestEligibleFarmEncounter(state: GameState): ContentId? {
        val world = state.run.world
        val activeRegion = world.activeRegionId?.let(engineContext.contentRegistry::regionOrNull)
        return activeRegion
            ?.encounterIds
            ?.asReversed()
            ?.firstOrNull { candidate ->
                candidate in world.clearedEncounterIds &&
                    engineContext.contentRegistry.encounterOrNull(candidate)?.type != EncounterType.BOSS
            }
    }

    /**
     * Builds the canonical input for offline farming. A synthetic terminal encounter lets
     * EncounterSystem start the selected farm target without changing the saved world. The
     * projected result below discards every world/combat mutation from this temporary run.
     */
    private fun offlineSimulationState(state: GameState, target: ContentId?): GameState {
        val world = state.run.world
        val activeRegion = world.activeRegionId?.let(engineContext.contentRegistry::regionOrNull)

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
     * Keeps the deterministic engine cursor, Gold balance, and player XP/level from the
     * canonical result. Retained gameplay state remains unchanged except that every absolute
     * simulation timestamp is shifted with the advanced engine cursor.
     */
    private fun projectOfflineResult(
        before: GameState,
        canonicalResult: EngineResult,
        simulatedElapsed: GameDuration
    ): EngineResult {
        val timestampConsistentBefore = shiftRetainedSimulationTimestamps(
            state = before,
            elapsed = simulatedElapsed
        )
        val simulatedGold =
            canonicalResult.state.run.economy.wallet.amountsByCurrencyId[CurrencyId.GOLD]
                ?: GameNumber.ZERO
        val walletAmounts = timestampConsistentBefore.run.economy.wallet.amountsByCurrencyId.toMutableMap()
        if (simulatedGold == GameNumber.ZERO) {
            walletAmounts.remove(CurrencyId.GOLD)
        } else {
            walletAmounts[CurrencyId.GOLD] = simulatedGold
        }

        val projectedRun = timestampConsistentBefore.run.copy(
            economy = timestampConsistentBefore.run.economy.copy(
                wallet = timestampConsistentBefore.run.economy.wallet.copy(
                    amountsByCurrencyId = walletAmounts
                )
            ),
            progression = timestampConsistentBefore.run.progression.copy(
                playerLevel = canonicalResult.state.run.progression.playerLevel,
                featureUnlocks = canonicalResult.state.run.progression.featureUnlocks
            )
        )
        val projectedState = timestampConsistentBefore.copy(
            engine = canonicalResult.state.engine,
            run = projectedRun,
            meta = timestampConsistentBefore.meta
        )

        return canonicalResult.copy(
            state = projectedState,
            events = canonicalResult.events.filter(::isAllowedOfflineEvent)
        )
    }

    /**
     * Keeps retained gameplay facts aligned with the advanced deterministic simulation clock.
     * Offline farming discards combat outcomes, but an active combat save remains resumable
     * after the offline interval, so every stored deadline moves by the same elapsed duration.
     */
    private fun shiftRetainedSimulationTimestamps(
        state: GameState,
        elapsed: GameDuration
    ): GameState {
        if (elapsed == GameDuration.ZERO) return state

        val combat = state.run.combat
        val convergence = state.run.resonance.convergence
        return state.copy(
            run = state.run.copy(
                combat = combat.copy(
                    playerCombatant = combat.playerCombatant?.let {
                        shiftCombatantSimulationTimestamps(it, elapsed)
                    },
                    enemies = combat.enemies.map { enemy ->
                        enemy.copy(
                            combatant = shiftCombatantSimulationTimestamps(
                                enemy.combatant,
                                elapsed
                            )
                        )
                    },
                    nextPlayerDecisionAt = combat.nextPlayerDecisionAt?.plus(elapsed),
                    nextEnemyDecisionAt = combat.nextEnemyDecisionAt.mapValues { (_, dueAt) ->
                        dueAt + elapsed
                    },
                    encounterStartedAt = combat.encounterStartedAt?.plus(elapsed)
                ),
                resonance = state.run.resonance.copy(
                    convergence = convergence.copy(
                        readyAtById = convergence.readyAtById.mapValues { (_, readyAt) ->
                            readyAt + elapsed
                        }
                    )
                )
            )
        )
    }

    private fun shiftCombatantSimulationTimestamps(
        combatant: com.idlerpg.game.domain.model.combat.CombatantState,
        elapsed: GameDuration
    ): com.idlerpg.game.domain.model.combat.CombatantState =
        combatant.copy(
            cooldowns = combatant.cooldowns.copy(
                readyAtByActionId = combatant.cooldowns.readyAtByActionId.mapValues {
                    (_, readyAt) -> readyAt + elapsed
                }
            ),
            statusEffects = combatant.statusEffects.map { status ->
                status.copy(
                    appliedAt = status.appliedAt + elapsed,
                    expiresAt = status.expiresAt + elapsed,
                    nextPeriodicTickAt = status.nextPeriodicTickAt?.plus(elapsed)
                )
            }
        )
    }

    private fun isAllowedOfflineEvent(envelope: GameEventEnvelope): Boolean =
        when (val event = envelope.event) {
            is ExperienceGranted,
            is PlayerLeveledUp -> true
            is FeatureUnlocked ->
                engineContext.contentRegistry.featureUnlockOrNull(event.featureId)?.scope ==
                    FeatureUnlockScope.RUN
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
        after: GameState,
        stoppingReason: OfflineStoppingReason = OfflineStoppingReason.ELAPSED
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
            startingLevel = before.run.progression.playerLevel.level,
            endingLevel = after.run.progression.playerLevel.level,
            stoppingReason = stoppingReason,
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

    private fun stoppingReasonFor(
        clockRollbackDetected: Boolean,
        durationClamped: Boolean,
        farmTarget: ContentId?
    ): OfflineStoppingReason = when {
        clockRollbackDetected -> OfflineStoppingReason.CLOCK_ROLLBACK
        farmTarget == null -> OfflineStoppingReason.NO_ELIGIBLE_FARM_STAGE
        durationClamped -> OfflineStoppingReason.CLAIM_WINDOW_CAPPED
        else -> OfflineStoppingReason.ELAPSED
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
