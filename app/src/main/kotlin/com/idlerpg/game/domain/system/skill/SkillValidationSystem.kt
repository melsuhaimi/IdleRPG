package com.idlerpg.game.domain.system.skill

import com.idlerpg.game.core.id.ContentId
import com.idlerpg.game.core.number.GameNumber
import com.idlerpg.game.data.content.ContentRegistry
import com.idlerpg.game.domain.command.CommandRejectionCode
import com.idlerpg.game.domain.command.CommandRejectionReason
import com.idlerpg.game.domain.definition.combat.SkillDefinition
import com.idlerpg.game.domain.definition.combat.SkillTargetingRule
import com.idlerpg.game.domain.model.GameState
import com.idlerpg.game.domain.model.combat.CombatStatus
import com.idlerpg.game.domain.system.combat.CooldownSystem
import com.idlerpg.game.domain.system.combat.TargetingSystem

/** Classification used only by deterministic manual-queue execution fallback. */
enum class SkillExecutionRejectionDisposition {
    TRANSIENT,
    PERMANENT
}

/** Pure deterministic legality checks for player skills and manual queueing. */
object SkillValidationSystem {

    fun rejectionReason(
        state: GameState,
        skillId: ContentId,
        contentRegistry: ContentRegistry
    ): CommandRejectionReason? {
        val definition = contentRegistry.skillOrNull(skillId)
            ?: return CommandRejectionReason(
                code = CommandRejectionCode.UNKNOWN_CONTENT,
                subjectContentId = skillId
            )
        return rejectionReason(state, definition)
    }

    fun rejectionReason(
        state: GameState,
        definition: SkillDefinition
    ): CommandRejectionReason? {
        val queueRejection = queueRejectionReason(state, definition)
        if (queueRejection != null) {
            return queueRejection
        }

        if (!CooldownSystem.isPlayerSkillReady(state, definition.id)) {
            return CommandRejectionReason(
                code = CommandRejectionCode.COOLDOWN_ACTIVE,
                subjectContentId = definition.id
            )
        }

        for ((resourceId, requiredAmount) in definition.resourceCosts.entries.sortedBy { it.key }) {
            val available = state.run.player.resources.amounts[resourceId] ?: GameNumber.ZERO
            if (available < requiredAmount) {
                return CommandRejectionReason(
                    code = CommandRejectionCode.INSUFFICIENT_RESOURCE,
                    subjectContentId = resourceId
                )
            }
        }

        return null
    }

    /**
     * Queue-time legality deliberately excludes cooldown/resource readiness.
     *
     * A valid manual request may wait across multiple normal decision points while those
     * transient conditions clear. Structural/content/loadout/target invalidity rejects the
     * queue immediately.
     */
    fun queueRejectionReason(
        state: GameState,
        skillId: ContentId,
        contentRegistry: ContentRegistry
    ): CommandRejectionReason? {
        val definition = contentRegistry.skillOrNull(skillId)
            ?: return CommandRejectionReason(
                code = CommandRejectionCode.UNKNOWN_CONTENT,
                subjectContentId = skillId
            )
        return queueRejectionReason(state, definition)
    }

    fun queueRejectionReason(
        state: GameState,
        definition: SkillDefinition
    ): CommandRejectionReason? {
        if (state.run.combat.status != CombatStatus.ACTIVE ||
            state.run.combat.playerCombatant == null
        ) {
            return CommandRejectionReason(
                code = CommandRejectionCode.INVALID_STATE,
                subjectContentId = definition.id
            )
        }

        if (definition.requiresEquipped &&
            definition.id !in state.run.player.equippedSkillIds
        ) {
            return CommandRejectionReason(
                code = CommandRejectionCode.INVALID_STATE,
                subjectContentId = definition.id
            )
        }

        val unlockRejection = unlockRejectionReason(state, definition)
        if (unlockRejection != null) {
            return unlockRejection
        }

        when (definition.targetingRule) {
            SkillTargetingRule.PRIMARY_ENEMY ->
                if (TargetingSystem.primaryLivingEnemy(state.run.combat) == null) {
                    return CommandRejectionReason(
                        code = CommandRejectionCode.INVALID_STATE,
                        subjectContentId = definition.id
                    )
                }

            SkillTargetingRule.LOWEST_HEALTH_ENEMY,
            SkillTargetingRule.HIGHEST_HEALTH_ENEMY,
            SkillTargetingRule.PROTECTOR_FIRST,
            SkillTargetingRule.CASTER_OR_SUPPORT_FIRST ->
                if (TargetingSystem.primaryLivingEnemy(state.run.combat) == null) {
                    return CommandRejectionReason(
                        code = CommandRejectionCode.INVALID_STATE,
                        subjectContentId = definition.id
                    )
                }

            SkillTargetingRule.SELF -> Unit
        }

        return null
    }

    /** Reusable unlock-only check for loadout mutation outside combat. */
    fun unlockRejectionReason(
        state: GameState,
        definition: SkillDefinition
    ): CommandRejectionReason? {
        val requiredFeatureId = definition.requiredFeatureId ?: return null
        if (requiredFeatureId !in state.run.progression.featureUnlocks.unlockedFeatureIds &&
            requiredFeatureId !in state.meta.persistentFeatureUnlocks.unlockedFeatureIds
        ) {
            return CommandRejectionReason(
                code = CommandRejectionCode.LOCKED,
                subjectContentId = requiredFeatureId
            )
        }
        return null
    }

    /**
     * Central classification for a queued skill that became non-executable at a decision.
     * Only cooldown/resource shortages are intentionally retained for a later retry.
     */
    fun executionRejectionDisposition(
        reason: CommandRejectionReason
    ): SkillExecutionRejectionDisposition =
        when (reason.code) {
            CommandRejectionCode.COOLDOWN_ACTIVE,
            CommandRejectionCode.INSUFFICIENT_RESOURCE ->
                SkillExecutionRejectionDisposition.TRANSIENT

            else -> SkillExecutionRejectionDisposition.PERMANENT
        }
}
