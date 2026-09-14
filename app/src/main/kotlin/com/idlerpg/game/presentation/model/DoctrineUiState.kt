package com.idlerpg.game.presentation.model

import com.idlerpg.game.core.id.ContentId
import com.idlerpg.game.core.id.InstanceId
import com.idlerpg.game.domain.command.CommandRejectionCode
import com.idlerpg.game.domain.definition.enemy.EnemyRole
import com.idlerpg.game.presentation.content.PresentationStringKey

/** Presentation-only Doctrine condition node vocabulary. */
enum class DoctrineConditionKindUi {
    ALL,
    ANY,
    NOT,
    PLAYER_HEALTH_PERCENT, ENEMY_COUNT, ENEMY_ROLE_PRESENT, ELITE_PRESENT, BOSS_PRESENT,
    ENEMY_HEALTH_PERCENT,
    SKILL_READY,
    RESONANCE_CHARGE,
    SEQUENCE_SUFFIX,
    STATUS_PRESENT,
    STATUS_ABSENT
}

enum class DoctrineComparisonUi {
    LESS_THAN,
    LESS_THAN_OR_EQUAL,
    EQUAL,
    GREATER_THAN_OR_EQUAL,
    GREATER_THAN
}

enum class DoctrineStatusTargetUi {
    PLAYER,
    PRIMARY_ENEMY
}

enum class DoctrineActionKindUi {
    BASIC_ATTACK,
    SKILL
}

enum class DoctrineDraftErrorUi {
    MISSING_SKILL,
    MISSING_STATUS,
    EMPTY_SEQUENCE,
    INVALID_AFFINITY,
    CONDITION_DEPTH_EXCEEDED
}

/**
 * Presentation-only editable Doctrine condition tree.
 *
 * This is deliberately not canonical gameplay state. It is an unfinished UI draft that
 * becomes a backend DoctrineCondition only when the player explicitly commits it.
 */
data class DoctrineConditionDraftUiState(
    val kind: DoctrineConditionKindUi = DoctrineConditionKindUi.ENEMY_HEALTH_PERCENT,
    val children: List<DoctrineConditionDraftUiState> = emptyList(),
    val comparison: DoctrineComparisonUi = DoctrineComparisonUi.LESS_THAN_OR_EQUAL,
    val enemyHealthPercent: Int = 50,
    val skillId: ContentId? = null,
    val affinityId: ContentId? = null,
    val resonanceAmount: Long = 1L,
    val sequenceAffinityIds: List<ContentId> = emptyList(),
    val statusTarget: DoctrineStatusTargetUi = DoctrineStatusTargetUi.PRIMARY_ENEMY,
    val statusDefinitionId: ContentId? = null,
    val enemyRole: EnemyRole = EnemyRole.PROTECTOR
) {
    init {
        require(enemyHealthPercent in 0..100) {
            "enemyHealthPercent must be in 0..100"
        }
        require(resonanceAmount >= 0L) {
            "resonanceAmount cannot be negative"
        }
    }
}

data class DoctrineActionDraftUiState(
    val kind: DoctrineActionKindUi = DoctrineActionKindUi.BASIC_ATTACK,
    val skillId: ContentId? = null
)

data class DoctrineRuleDraftUiState(
    val ruleId: InstanceId? = null,
    val enabled: Boolean = true,
    val condition: DoctrineConditionDraftUiState = DoctrineConditionDraftUiState(),
    val action: DoctrineActionDraftUiState = DoctrineActionDraftUiState(),
    val localError: DoctrineDraftErrorUi? = null
)

data class DoctrineSkillChoiceUiState(
    val skillId: ContentId,
    val titleStringKey: PresentationStringKey,
    val equipped: Boolean,
    val unlocked: Boolean,
    val requiresEquipped: Boolean,
    val selectableForDoctrine: Boolean
)

data class DoctrineStatusChoiceUiState(
    val statusId: ContentId,
    val titleStringKey: PresentationStringKey
)

data class DoctrineAffinityChoiceUiState(
    val affinityId: ContentId,
    val titleStringKey: PresentationStringKey
)

data class DoctrineRuleUiState(
    val ruleId: InstanceId,
    val index: Int,
    val enabled: Boolean,
    val condition: DoctrineConditionDraftUiState,
    val action: DoctrineActionDraftUiState,
    val hasUnequippedSkillReference: Boolean,
    val selectedByLatestFeedback: Boolean,
    val canMoveEarlier: Boolean,
    val canMoveLater: Boolean
)

data class DoctrineResonanceAffinityUiState(
    val affinityId: ContentId,
    val titleStringKey: PresentationStringKey,
    val chargeDisplay: String,
    val capDisplay: String,
    val chargeProgressUnits: Int
)

data class DoctrineConvergenceRequirementUiState(
    val affinityId: ContentId,
    val titleStringKey: PresentationStringKey,
    val requiredDisplay: String
)

data class DoctrineConvergenceUiState(
    val convergenceId: ContentId,
    val titleStringKey: PresentationStringKey,
    val discovered: Boolean,
    val eligibleNow: Boolean,
    val patternAffinityIds: List<ContentId>,
    val minimumCharges: List<DoctrineConvergenceRequirementUiState>,
    val totalTriggerCountDisplay: String,
    val encounterTriggerCount: Long
)

enum class DoctrineFeedbackKind {
    COMMAND_REJECTED,
    DOCTRINE_UPDATED,
    RULE_SELECTED,
    ACTION_REJECTED,
    FALLBACK_USED,
    RESONANCE_GENERATED,
    RESONANCE_CONSUMED,
    CONVERGENCE_TRIGGERED,
    CONVERGENCE_DISCOVERED
}

data class DoctrineFeedbackUiState(
    val sequenceNumber: Long?,
    val kind: DoctrineFeedbackKind,
    val ruleId: InstanceId? = null,
    val contentId: ContentId? = null,
    val affinityId: ContentId? = null,
    val amountDisplay: String? = null,
    val rejectionCode: CommandRejectionCode? = null,
    val explanation: String? = null
)

/** FUI-06 canonical Doctrine + Resonance projection plus presentation-only rule draft. */
data class DoctrineUiState(
    val enabled: Boolean,
    val ruleCount: Int,
    val ruleCapacity: Int,
    val conditionMaxDepth: Int,
    val sequenceCapacity: Int,
    val mutationPending: Boolean,
    val queuedManualSkillTitleStringKey: PresentationStringKey?,
    val basicAttackId: ContentId,
    val basicAttackTitleStringKey: PresentationStringKey,
    val rules: List<DoctrineRuleUiState>,
    val draft: DoctrineRuleDraftUiState?,
    val skillChoices: List<DoctrineSkillChoiceUiState>,
    val statusChoices: List<DoctrineStatusChoiceUiState>,
    val affinityChoices: List<DoctrineAffinityChoiceUiState>,
    val resonance: List<DoctrineResonanceAffinityUiState>,
    val resonanceSequence: List<ContentId>,
    val convergences: List<DoctrineConvergenceUiState>,
    val feedback: DoctrineFeedbackUiState? = null
) {
    val capacityFull: Boolean
        get() = ruleCount >= ruleCapacity
}
