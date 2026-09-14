package com.idlerpg.game.application

import com.idlerpg.game.core.number.GameNumber
import com.idlerpg.game.core.time.GameClock
import com.idlerpg.game.core.time.GameDuration
import com.idlerpg.game.data.local.SaveEnvelope
import com.idlerpg.game.data.local.SaveVersion
import com.idlerpg.game.data.repository.GameRepository
import com.idlerpg.game.domain.definition.CurrencyId
import com.idlerpg.game.domain.engine.EngineContext
import com.idlerpg.game.domain.engine.EngineDiagnostics
import com.idlerpg.game.domain.engine.EngineResult
import com.idlerpg.game.domain.engine.SimulationActionLimitExceededException
import com.idlerpg.game.domain.engine.SimulationEngine
import com.idlerpg.game.domain.event.AdaptationTierChanged
import com.idlerpg.game.domain.event.BossPhaseChanged
import com.idlerpg.game.domain.event.ConvergenceTriggered
import com.idlerpg.game.domain.event.CurrencyGranted
import com.idlerpg.game.domain.event.EncounterCleared
import com.idlerpg.game.domain.event.EnemyKilled
import com.idlerpg.game.domain.event.ExperienceGranted
import com.idlerpg.game.domain.event.GameEventEnvelope
import com.idlerpg.game.domain.event.ItemAdded
import com.idlerpg.game.domain.event.ItemAutoSalvaged
import com.idlerpg.game.domain.event.ItemDropped
import com.idlerpg.game.domain.event.ItemSentToOverflow
import com.idlerpg.game.domain.event.MasteryIncreased
import com.idlerpg.game.domain.event.EncounterFailed
import com.idlerpg.game.domain.event.EncounterStarted
import com.idlerpg.game.domain.definition.Rarity
import com.idlerpg.game.domain.definition.enemy.EnemyRole
import com.idlerpg.game.domain.definition.world.EncounterType
import com.idlerpg.game.domain.model.GameState

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
        "savedAtEpochMs=$savedAtEpochMs, currentEpochMs=$currentEpochMs"
)

/**
 * Derived summary of one offline advancement.
 *
 * This object is informational only. No value in this summary grants gameplay rewards;
 * all rewards have already been produced by the canonical SimulationEngine event stream.
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
            "eventCount cannot be negative: $eventCount"
        }
        require(notableDrops.size <= 3) { "At most three notable drops may be presented" }
        require(listOfNotNull(startingStage, endingStage, deepestStage, currentWallStage).all { it > 0 }) {
            "Offline stage numbers must be positive"
        }
    }
}

/**
 * Result accepted by the future GameSession after a save has been resumed and checkpointed.
 */
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
 * Foundation 16 wall-time -> deterministic simulation bridge.
 *
 * Canonical flow:
 *
 * load SaveEnvelope
 * -> read injected GameClock
 * -> derive non-negative elapsed wall duration
 * -> clamp through BalanceConfig.maximumOfflineDuration
 * -> advance the existing SimulationEngine exactly once
 * -> derive an informational summary from resulting domain events
 * -> checkpoint the advanced state before returning it to a future foreground session
 *
 * No combat, Gold, XP, loot, Resonance, Adaptation, or quest formula is duplicated here.
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
            "GameClock returned negative epoch milliseconds: $nowEpochMs"
        }

        val elapsed = calculateElapsed(
            savedAtEpochMs = sourceEnvelope.writtenAtEpochMs,
            currentEpochMs = nowEpochMs
        )

        val maximum = engineContext.balanceConfig.maximumOfflineDuration
        val simulatedElapsed = if (elapsed.requested > maximum) {
            maximum
        } else {
            elapsed.requested
        }

        val engineResult = advanceExact(
            state = before,
            duration = simulatedElapsed
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
        // anchor instead of moving the save timestamp backwards. Otherwise a successful
        // resume consumes the elapsed interval, including any time discarded by the cap.
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
     * Runs only the canonical SimulationEngine. If one very long exact advancement hits
     * the engine's per-call safety ceiling, split the duration deterministically and
     * continue with the resulting canonical state. No gameplay formula is approximated.
     *
     * A duration of 0 or 1 ms that still exceeds the action ceiling cannot be split
     * meaningfully and the original exception is propagated as a genuine pathological
     * scheduler/content condition.
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
            if (duration.millis <= 1L) {
                throw limit
            }

            val firstMillis = duration.millis / 2L
            val secondMillis = duration.millis - firstMillis

            val first = advanceExact(
                state = state,
                duration = GameDuration.ofMillis(firstMillis)
            )
            val second = advanceExact(
                state = first.state,
                duration = GameDuration.ofMillis(secondMillis)
            )

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

    internal fun summarize(
        requestedElapsed: GameDuration,
        simulatedElapsed: GameDuration,
        durationClamped: Boolean,
        clockRollbackDetected: Boolean,
        events: List<GameEventEnvelope>,
        before: GameState,
        after: GameState
    ): OfflineProgressSummary {
        var enemiesDefeated = GameNumber.ZERO
        var encountersCleared = GameNumber.ZERO
        var goldGranted = GameNumber.ZERO
        var experienceGranted = GameNumber.ZERO
        var masteryGranted = GameNumber.ZERO
        var itemsFound = GameNumber.ZERO
        var itemsKept = GameNumber.ZERO
        var itemsOverflowed = GameNumber.ZERO
        var itemsAutoSalvaged = GameNumber.ZERO
        var autoSalvageGold = GameNumber.ZERO
        var eliteEncountersCleared = GameNumber.ZERO
        var anomalyEncountersCleared = GameNumber.ZERO
        var bossesDefeated = GameNumber.ZERO
        var convergencesTriggered = GameNumber.ZERO
        var adaptationTierChanges = GameNumber.ZERO
        var currentWallStage: Int? = null
        val visitedStages = listOfNotNull(stageForState(before)).toMutableList()
        val notableCandidates = mutableListOf<OfflineNotableDrop>()

        for (envelope in events) {
            when (val event = envelope.event) {
                is EnemyKilled ->
                    enemiesDefeated += GameNumber.ONE

                is EncounterCleared -> {
                    encountersCleared += GameNumber.ONE
                    stageForEncounter(event.encounterDefinitionId)?.let(visitedStages::add)
                    when (engineContext.contentRegistry.encounter(event.encounterDefinitionId).type) {
                        EncounterType.ELITE -> eliteEncountersCleared += GameNumber.ONE
                        EncounterType.ANOMALY -> anomalyEncountersCleared += GameNumber.ONE
                        EncounterType.BOSS -> bossesDefeated += GameNumber.ONE
                        EncounterType.NORMAL -> Unit
                    }
                }

                is EncounterStarted ->
                    stageForEncounter(event.encounterDefinitionId)?.let(visitedStages::add)

                is EncounterFailed -> {
                    currentWallStage = stageForEncounter(event.encounterDefinitionId)
                    currentWallStage?.let(visitedStages::add)
                }

                is CurrencyGranted ->
                    if (event.currencyId == CurrencyId.GOLD) {
                        goldGranted += event.amount
                    }

                is ExperienceGranted ->
                    experienceGranted += event.amount

                is MasteryIncreased ->
                    masteryGranted += event.amount

                is ItemDropped -> {
                    itemsFound += GameNumber.ONE
                    if (event.rarity.rank >= Rarity.RARE.rank) {
                        notableCandidates += OfflineNotableDrop(
                            displayName = engineContext.contentRegistry.item(event.itemDefinitionId).displayName,
                            rarity = event.rarity
                        )
                    }
                }

                is ItemAdded ->
                    itemsKept += GameNumber.ONE

                is ItemSentToOverflow ->
                    itemsOverflowed += GameNumber.ONE

                is ItemAutoSalvaged -> {
                    itemsAutoSalvaged += GameNumber.ONE
                    autoSalvageGold += event.goldGranted
                }

                is ConvergenceTriggered ->
                    convergencesTriggered += GameNumber.ONE

                is AdaptationTierChanged ->
                    adaptationTierChanges += GameNumber.ONE

                else -> Unit
            }
        }

        stageForState(after)?.let(visitedStages::add)
        val failedEventIndex = events.indexOfLast { it.event is EncounterFailed }
        val failedEncounterId = (events.getOrNull(failedEventIndex)?.event as? EncounterFailed)
            ?.encounterDefinitionId
        val failedEncounter = failedEncounterId?.let(engineContext.contentRegistry::encounter)
        val failedBossPhase = failedEncounter?.bossId?.let { bossId ->
            events.take((failedEventIndex + 1).coerceAtLeast(0)).asReversed()
                .firstNotNullOfOrNull { envelope ->
                    (envelope.event as? BossPhaseChanged)
                        ?.takeIf { it.bossId == bossId }
                        ?.phase
                }
        }
        val failedWave = failedBossPhase
            ?.coerceIn(1, failedEncounter?.waves ?: 1)
            ?: before.run.world.currentEncounter
                ?.takeIf { it.definitionId == failedEncounterId }
                ?.currentWave
                ?.coerceIn(1, failedEncounter?.waves ?: 1)
            ?: after.run.world.currentEncounter
                ?.takeIf { it.definitionId == failedEncounterId }
                ?.currentWave
                ?.coerceIn(1, failedEncounter?.waves ?: 1)
            ?: 1
        val failedRoles = failedEncounter?.enemyDefinitionIdsForWave(failedWave)
            ?.map { engineContext.contentRegistry.enemy(it).role }
            .orEmpty()
        val tacticalInsight = when {
            currentWallStage == null -> null
            EnemyRole.PROTECTOR in failedRoles -> OfflineTacticalInsight.PROTECTOR_BLOCKING
            EnemyRole.ADAPTIVE in failedRoles -> OfflineTacticalInsight.ADAPTIVE_RESISTANCE
            failedRoles.any { it in setOf(EnemyRole.CASTER, EnemyRole.DISRUPTOR, EnemyRole.CONTROLLER) } ->
                OfflineTacticalInsight.CASTER_DISRUPTION
            failedRoles.size >= 3 -> OfflineTacticalInsight.SWARM_PRESSURE
            adaptationTierChanges > GameNumber.ZERO -> OfflineTacticalInsight.ADAPTATION_PRESSURE
            else -> OfflineTacticalInsight.SURVIVAL_PRESSURE
        }
        val notableDrops = notableCandidates.withIndex()
            .sortedWith(
                compareByDescending<IndexedValue<OfflineNotableDrop>> { it.value.rarity.rank }
                    .thenBy { it.index }
            )
            .take(3)
            .map { it.value }

        return OfflineProgressSummary(
            requestedElapsed = requestedElapsed,
            simulatedElapsed = simulatedElapsed,
            durationClamped = durationClamped,
            clockRollbackDetected = clockRollbackDetected,
            enemiesDefeated = enemiesDefeated,
            encountersCleared = encountersCleared,
            goldGranted = goldGranted,
            experienceGranted = experienceGranted,
            masteryGranted = masteryGranted,
            itemsFound = itemsFound,
            itemsKept = itemsKept,
            itemsOverflowed = itemsOverflowed,
            itemsAutoSalvaged = itemsAutoSalvaged,
            autoSalvageGold = autoSalvageGold,
            eliteEncountersCleared = eliteEncountersCleared,
            anomalyEncountersCleared = anomalyEncountersCleared,
            bossesDefeated = bossesDefeated,
            convergencesTriggered = convergencesTriggered,
            adaptationTierChanges = adaptationTierChanges,
            startingStage = stageForState(before),
            endingStage = stageForState(after),
            deepestStage = visitedStages.filter { it > 0 }.maxOrNull(),
            currentWallStage = currentWallStage,
            notableDrops = notableDrops,
            tacticalInsight = tacticalInsight,
            eventCount = events.size
        )
    }

    private fun stageForState(state: GameState): Int? {
        state.run.world.currentEncounter?.definitionId?.let { return stageForEncounter(it) }
        return state.run.world.clearedEncounterIds.mapNotNull(::stageForEncounter).maxOrNull()
    }

    private fun stageForEncounter(encounterId: com.idlerpg.game.core.id.ContentId): Int? {
        val encounter = engineContext.contentRegistry.encounterOrNull(encounterId) ?: return null
        val index = engineContext.contentRegistry.region(encounter.regionId).encounterIds.indexOf(encounterId)
        return index.takeIf { it >= 0 }?.plus(1)
    }

    private data class CalculatedElapsed(
        val requested: GameDuration,
        val clockRollbackDetected: Boolean
    )
}
