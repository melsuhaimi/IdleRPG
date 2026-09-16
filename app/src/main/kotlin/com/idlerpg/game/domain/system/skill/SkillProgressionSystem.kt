package com.idlerpg.game.domain.system.skill

import com.idlerpg.game.core.id.ContentId
import com.idlerpg.game.core.number.GameMath
import com.idlerpg.game.core.number.GameNumber
import com.idlerpg.game.core.number.Ratio
import com.idlerpg.game.domain.command.CommandRejectionCode
import com.idlerpg.game.domain.command.CommandRejectionReason
import com.idlerpg.game.domain.command.GameCommand
import com.idlerpg.game.domain.command.RefineSkill
import com.idlerpg.game.domain.command.SkillProgressionCommand
import com.idlerpg.game.domain.command.UpgradeSkillMastery
import com.idlerpg.game.domain.command.UpgradeSkillRank
import com.idlerpg.game.domain.definition.CurrencyId
import com.idlerpg.game.domain.engine.CommandHandlingResult
import com.idlerpg.game.domain.engine.EngineContext
import com.idlerpg.game.domain.engine.GameCommandHandler
import com.idlerpg.game.domain.event.CurrencySpent
import com.idlerpg.game.domain.event.GameEvent
import com.idlerpg.game.domain.event.SkillMasteryIncreased
import com.idlerpg.game.domain.event.SkillRankIncreased
import com.idlerpg.game.domain.event.SkillRefinementIncreased
import com.idlerpg.game.domain.model.GameState

/**
 * Owns the three deliberate skill-investment tracks.
 *
 * Rank, mastery, and refinement are independent optional Gold sinks with authored caps. Each
 * transition is atomic and therefore safe to invoke from menus or save boundaries without
 * partially charging the player.
 */
object SkillProgressionSystem : GameCommandHandler {
    const val MAX_MASTERY: Long = 50L
    const val MAX_REFINEMENT: Long = 10L

    private val RANK_COST_GROWTH: Ratio = Ratio.ofUnits(11_200L)
    private val MASTERY_COST_GROWTH: Ratio = Ratio.ofUnits(11_800L)
    private val REFINEMENT_COST_GROWTH: Ratio = Ratio.ofUnits(13_500L)

    override fun handle(
        state: GameState,
        command: GameCommand,
        context: EngineContext
    ): CommandHandlingResult =
        when (command) {
            is UpgradeSkillRank -> upgradeRank(state, command, context)
            is UpgradeSkillMastery -> upgradeMastery(state, command, context)
            is RefineSkill -> refine(state, command, context)
            is SkillProgressionCommand ->
                rejected(CommandRejectionCode.UNSUPPORTED, null)
            else ->
                rejected(CommandRejectionCode.UNSUPPORTED, null)
        }

    fun rankUpgradeCost(rank: Long): GameNumber {
        require(rank >= 1L) { "rank must be positive: " + rank }
        return GameMath.compoundCeil(
            value = GameNumber.of(1_000L),
            multiplier = RANK_COST_GROWTH,
            steps = rank - 1L
        )
    }

    fun masteryUpgradeCost(mastery: Long): GameNumber {
        require(mastery >= 0L) { "mastery cannot be negative: " + mastery }
        return GameMath.compoundCeil(
            value = GameNumber.of(5_000L),
            multiplier = MASTERY_COST_GROWTH,
            steps = mastery
        )
    }

    fun refinementCost(refinement: Long): GameNumber {
        require(refinement >= 0L) { "refinement cannot be negative: " + refinement }
        return GameMath.compoundCeil(
            value = GameNumber.of(50_000L),
            multiplier = REFINEMENT_COST_GROWTH,
            steps = refinement
        )
    }

    private fun upgradeRank(
        state: GameState,
        command: UpgradeSkillRank,
        context: EngineContext
    ): CommandHandlingResult {
        val definition = try {
            knownUnlockedSkill(state, command.skillId, context)
        } catch (rejection: SkillProgressionRejection) {
            return CommandHandlingResult.Rejected(rejection.reason)
        } ?: return rejected(CommandRejectionCode.UNKNOWN_CONTENT, command.skillId)
        val currentRank = SkillScalingSystem.rank(state, definition)
        val maximumRank = definition.maxRank ?: MAX_UNBOUNDED_RANK
        if (currentRank >= maximumRank) {
            return rejected(CommandRejectionCode.NOT_READY, definition.id)
        }
        val cost = rankUpgradeCost(currentRank)
        val charged = chargeGold(state, cost)
            ?: return rejected(CommandRejectionCode.INSUFFICIENT_RESOURCE, CurrencyId.GOLD.id)
        val next = state.run.progression.skillProgression.copy(
            rankBySkillId = state.run.progression.skillProgression.rankBySkillId +
                (definition.id to currentRank + 1L)
        )
        return accepted(
            state = charged.copy(
                run = charged.run.copy(
                    progression = charged.run.progression.copy(skillProgression = next)
                )
            ),
            events = listOf(
                CurrencySpent(CurrencyId.GOLD, cost, definition.id),
                SkillRankIncreased(definition.id, currentRank, currentRank + 1L, cost)
            )
        )
    }

    private fun upgradeMastery(
        state: GameState,
        command: UpgradeSkillMastery,
        context: EngineContext
    ): CommandHandlingResult {
        val definition = try {
            knownUnlockedSkill(state, command.skillId, context)
        } catch (rejection: SkillProgressionRejection) {
            return CommandHandlingResult.Rejected(rejection.reason)
        } ?: return rejected(CommandRejectionCode.UNKNOWN_CONTENT, command.skillId)
        val progression = state.run.progression.skillProgression
        val current = progression.masteryBySkillId[definition.id] ?: 0L
        if (current >= MAX_MASTERY) {
            return rejected(CommandRejectionCode.NOT_READY, definition.id)
        }
        val cost = masteryUpgradeCost(current)
        val charged = chargeGold(state, cost)
            ?: return rejected(CommandRejectionCode.INSUFFICIENT_RESOURCE, CurrencyId.GOLD.id)
        val next = progression.copy(
            masteryBySkillId = progression.masteryBySkillId +
                (definition.id to current + 1L)
        )
        return accepted(
            state = charged.copy(
                run = charged.run.copy(
                    progression = charged.run.progression.copy(skillProgression = next)
                )
            ),
            events = listOf(
                CurrencySpent(CurrencyId.GOLD, cost, definition.id),
                SkillMasteryIncreased(definition.id, current, current + 1L, cost)
            )
        )
    }

    private fun refine(
        state: GameState,
        command: RefineSkill,
        context: EngineContext
    ): CommandHandlingResult {
        val definition = try {
            knownUnlockedSkill(state, command.skillId, context)
        } catch (rejection: SkillProgressionRejection) {
            return CommandHandlingResult.Rejected(rejection.reason)
        } ?: return rejected(CommandRejectionCode.UNKNOWN_CONTENT, command.skillId)
        val progression = state.run.progression.skillProgression
        val current = progression.refinementBySkillId[definition.id] ?: 0L
        if (current >= MAX_REFINEMENT) {
            return rejected(CommandRejectionCode.NOT_READY, definition.id)
        }
        val cost = refinementCost(current)
        val charged = chargeGold(state, cost)
            ?: return rejected(CommandRejectionCode.INSUFFICIENT_RESOURCE, CurrencyId.GOLD.id)
        val next = progression.copy(
            refinementBySkillId = progression.refinementBySkillId +
                (definition.id to current + 1L)
        )
        return accepted(
            state = charged.copy(
                run = charged.run.copy(
                    progression = charged.run.progression.copy(skillProgression = next)
                )
            ),
            events = listOf(
                CurrencySpent(CurrencyId.GOLD, cost, definition.id),
                SkillRefinementIncreased(definition.id, current, current + 1L, cost)
            )
        )
    }

    private fun knownUnlockedSkill(
        state: GameState,
        skillId: ContentId,
        context: EngineContext
    ) = context.contentRegistry.skillOrNull(skillId)?.also { definition ->
        val rejection = SkillValidationSystem.unlockRejectionReason(state, definition)
        if (rejection != null) {
            throw SkillProgressionRejection(rejection)
        }
    }

    private class SkillProgressionRejection(
        val reason: CommandRejectionReason
    ) : RuntimeException()

    private fun chargeGold(
        state: GameState,
        amount: GameNumber
    ): GameState? {
        val wallet = state.run.economy.wallet
        val current = wallet.amountsByCurrencyId[CurrencyId.GOLD] ?: GameNumber.ZERO
        if (current < amount) return null
        val updated = wallet.amountsByCurrencyId.toMutableMap()
        val remaining = current - amount
        if (remaining == GameNumber.ZERO) updated.remove(CurrencyId.GOLD)
        else updated[CurrencyId.GOLD] = remaining
        return state.copy(
            run = state.run.copy(
                economy = state.run.economy.copy(
                    wallet = wallet.copy(amountsByCurrencyId = updated)
                )
            )
        )
    }

    private fun accepted(
        state: GameState,
        events: List<GameEvent>
    ) = CommandHandlingResult.Accepted(state, events)

    private fun rejected(
        code: CommandRejectionCode,
        subjectContentId: ContentId?
    ) = CommandHandlingResult.Rejected(
        CommandRejectionReason(code = code, subjectContentId = subjectContentId)
    )

    private const val MAX_UNBOUNDED_RANK: Long = 1_000L
}
