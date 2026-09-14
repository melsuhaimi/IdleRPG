package com.idlerpg.game.domain.system.chronicle

import com.idlerpg.game.core.number.GameNumber
import com.idlerpg.game.domain.command.ChronicleCommand
import com.idlerpg.game.domain.command.CommandRejectionCode
import com.idlerpg.game.domain.command.CommandRejectionReason
import com.idlerpg.game.domain.command.CommitChronicleCollapse
import com.idlerpg.game.domain.command.GameCommand
import com.idlerpg.game.domain.command.RequestChroniclePreview
import com.idlerpg.game.domain.definition.chronicle.ChronicleDefinition
import com.idlerpg.game.domain.engine.CommandHandlingResult
import com.idlerpg.game.domain.engine.EngineContext
import com.idlerpg.game.domain.engine.GameCommandHandler
import com.idlerpg.game.domain.event.ChronicleCollapseStarted
import com.idlerpg.game.domain.event.ChronicleCollapsed
import com.idlerpg.game.domain.event.ChroniclePersistScope
import com.idlerpg.game.domain.event.ChroniclePreviewPrepared
import com.idlerpg.game.domain.event.ChronicleResetScope
import com.idlerpg.game.domain.event.GameEvent
import com.idlerpg.game.domain.event.NewChronicleStarted
import com.idlerpg.game.domain.model.GameState
import com.idlerpg.game.domain.system.state.RunStateFactory

/**
 * Foundation 17 deterministic Chronicle preview/commit command handler.
 *
 * Reset behavior is structural: a collapse replaces the entire RunState with a fresh
 * configured RunState while preserving and intentionally transforming MetaState. EngineState is
 * left to GameEngine so simulation time, RNG continuation, event sequence, and instance
 * IDs remain canonical across the reset.
 */
object ChronicleSystem : GameCommandHandler {

    val RESET_SCOPES: List<ChronicleResetScope> = listOf(
        ChronicleResetScope.PLAYER,
        ChronicleResetScope.COMBAT,
        ChronicleResetScope.WORLD,
        ChronicleResetScope.ECONOMY,
        ChronicleResetScope.RESONANCE,
        ChronicleResetScope.DOCTRINE,
        ChronicleResetScope.ADAPTATION,
        ChronicleResetScope.INVENTORY,
        ChronicleResetScope.PROGRESSION,
        ChronicleResetScope.QUESTS,
        ChronicleResetScope.RUN_STATISTICS
    )

    val PERSIST_SCOPES: List<ChroniclePersistScope> = listOf(
        ChroniclePersistScope.ENGINE_STATE,
        ChroniclePersistScope.CHRONICLE,
        ChroniclePersistScope.ECHOES,
        ChroniclePersistScope.DISCOVERIES,
        ChroniclePersistScope.PERSISTENT_FEATURE_UNLOCKS,
        ChroniclePersistScope.ACHIEVEMENTS,
        ChroniclePersistScope.LIFETIME_STATISTICS
    )

    override fun handle(
        state: GameState,
        command: GameCommand,
        context: EngineContext
    ): CommandHandlingResult = when (command) {
        is RequestChroniclePreview -> requestPreview(state, context)
        is CommitChronicleCollapse -> commit(state, command, context)
        is ChronicleCommand -> rejected(CommandRejectionCode.UNSUPPORTED)
        else -> rejected(CommandRejectionCode.UNSUPPORTED)
    }

    fun totalNormalClears(state: GameState): GameNumber =
        state.run.world.regionProgressById
            .toSortedMap()
            .values
            .fold(GameNumber.ZERO) { total, region ->
                total + region.normalClears
            }

    fun isEligible(
        state: GameState,
        definition: ChronicleDefinition
    ): Boolean = totalNormalClears(state) >= definition.requiredTotalNormalClears && (definition.requiredBossId == null || state.run.world.regionProgressById.values.any { definition.requiredBossId in it.clearedBossIds })

    fun echoReward(state: GameState, definition: ChronicleDefinition): GameNumber {
        val deepest = state.run.world.regionProgressById.values.maxOfOrNull { it.highestClearedEncounterTier } ?: 0L
        val elites = state.run.world.regionProgressById.values.fold(GameNumber.ZERO) { total, it -> total + it.eliteClears }.toBigInteger().min(java.math.BigInteger.valueOf(20L)).longValueExact()
        return definition.echoReward + definition.echoPerDeepestStage * deepest + definition.echoPerEliteClear * elites
    }

    private fun requestPreview(
        state: GameState,
        context: EngineContext
    ): CommandHandlingResult {
        val definition = context.contentRegistry.defaultChronicleDefinition()
        val currentClears = totalNormalClears(state)
        if (!isEligible(state, definition)) {
            return rejected(
                code = CommandRejectionCode.LOCKED,
                definition = definition
            )
        }

        // RequestChroniclePreview emits exactly one raw event. GameEngine will advance
        // nextEventSequenceNumber by one when committing it. The token deliberately
        // describes that post-preview state so an immediate commit is valid.
        val token = previewToken(
            state = state,
            nextEventSequenceNumber = Math.addExact(
                state.engine.nextEventSequenceNumber,
                1L
            )
        )

        return CommandHandlingResult.Accepted(
            state = state,
            events = listOf(
                ChroniclePreviewPrepared(
                    previewToken = token,
                    chronicleNumber = state.meta.chronicle.currentChronicleNumber,
                    resetRuleVersion = definition.resetRuleVersion,
                    currentTotalNormalClears = currentClears,
                    requiredTotalNormalClears = definition.requiredTotalNormalClears,
                    echoReward = echoReward(state, definition),
                    resetScopes = RESET_SCOPES,
                    persistScopes = PERSIST_SCOPES
                )
            )
        )
    }

    private fun commit(
        state: GameState,
        command: CommitChronicleCollapse,
        context: EngineContext
    ): CommandHandlingResult {
        val definition = context.contentRegistry.defaultChronicleDefinition()

        val currentToken = previewToken(
            state = state,
            nextEventSequenceNumber = state.engine.nextEventSequenceNumber
        )
        if (command.expectedPreviewToken != currentToken) {
            return rejected(
                code = CommandRejectionCode.STALE_PREVIEW,
                definition = definition
            )
        }

        if (!isEligible(state, definition)) {
            return rejected(
                code = CommandRejectionCode.LOCKED,
                definition = definition
            )
        }

        val completedChronicleNumber = state.meta.chronicle.currentChronicleNumber
        val nextChronicleNumber = Math.addExact(completedChronicleNumber, 1L)
        val updatedChronicle = state.meta.chronicle.copy(
            currentChronicleNumber = nextChronicleNumber,
            completedChronicles = state.meta.chronicle.completedChronicles + GameNumber.ONE,
            bestMilestoneIds = definition.milestoneId?.let { milestone ->
                state.meta.chronicle.bestMilestoneIds + milestone
            } ?: state.meta.chronicle.bestMilestoneIds
        )

        val baseMeta = state.meta.copy(
            chronicle = updatedChronicle
        )
        val echoGrant = EchoSystem.grant(
            meta = baseMeta,
            amount = echoReward(state, definition),
            sourceId = definition.id
        )

        val events = buildList<GameEvent> {
            add(ChronicleCollapseStarted(completedChronicleNumber))
            addAll(echoGrant.events)
            add(
                ChronicleCollapsed(
                    completedChronicleNumber = completedChronicleNumber,
                    nextChronicleNumber = nextChronicleNumber
                )
            )
            add(NewChronicleStarted(nextChronicleNumber))
        }

        return CommandHandlingResult.Accepted(
            state = state.copy(
                run = RunStateFactory.fresh(context.balanceConfig, echoGrant.meta),
                meta = echoGrant.meta
            ),
            events = events
        )
    }

    /**
     * Deterministic positive token over the engine/meta/progress coordinates that make a
     * previously shown preview stale when meaningful execution occurs afterward.
     */
    private fun previewToken(
        state: GameState,
        nextEventSequenceNumber: Long
    ): Long {
        var token = 17L

        fun mix(value: Long) {
            token = token * 31L + value
        }

        fun mixText(value: String) {
            value.forEach { character ->
                mix(character.code.toLong())
            }
        }

        mix(state.engine.simulationTime.millis)
        mix(state.engine.randomState.state)
        mix(state.engine.randomState.algorithmVersion.toLong())
        mix(nextEventSequenceNumber)
        mix(state.engine.nextInstanceIdCounter)
        mix(state.meta.chronicle.currentChronicleNumber)
        mixText(state.meta.chronicle.completedChronicles.toPlainString())
        mixText(totalNormalClears(state).toPlainString())
        state.run.world.regionProgressById.toSortedMap().forEach { (regionId, progress) -> mixText(regionId.value); mix(progress.highestClearedEncounterTier); progress.clearedBossIds.sorted().forEach { mixText(it.value) } }
        mixText(state.meta.echoes.available.toPlainString())
        mixText(state.meta.echoes.spent.toPlainString())

        val positive = token and Long.MAX_VALUE
        return if (positive == 0L) 1L else positive
    }

    private fun rejected(
        code: CommandRejectionCode,
        definition: ChronicleDefinition? = null
    ): CommandHandlingResult.Rejected =
        CommandHandlingResult.Rejected(
            CommandRejectionReason(
                code = code,
                subjectContentId = definition?.id
            )
        )
}
