package com.idlerpg.game.presentation.model

import com.idlerpg.game.core.id.ContentId
import com.idlerpg.game.domain.command.CommandRejectionCode
import com.idlerpg.game.presentation.content.PresentationAssetKey
import com.idlerpg.game.presentation.content.PresentationStringKey

enum class SkillLoadoutFeedbackKind {
    COMMAND_REJECTED,
    EQUIPPED,
    UNEQUIPPED,
    MOVED,
    EVOLVED
}

data class SkillLoadoutFeedbackUiState(
    val sequenceNumber: Long?,
    val kind: SkillLoadoutFeedbackKind,
    val skillId: ContentId? = null,
    val rejectionCode: CommandRejectionCode? = null,
    val fromIndex: Int? = null,
    val toIndex: Int? = null
)

data class SkillLoadoutMasteryRequirementUiState(
    val affinityId: ContentId,
    val titleStringKey: PresentationStringKey,
    val currentLevel: Long,
    val requiredLevel: Long,
    val met: Boolean
)

data class SkillLoadoutUnlockUiState(
    val requiredFeatureId: ContentId?,
    val currentPlayerLevel: Long,
    val requiredPlayerLevel: Long?,
    val masteryRequirements: List<SkillLoadoutMasteryRequirementUiState>,
    val unlocked: Boolean
)

data class SkillLoadoutSkillUiState(
    val skillId: ContentId,
    val titleStringKey: PresentationStringKey,
    val assetKey: PresentationAssetKey,
    val affinityIds: List<ContentId>,
    val cooldownMillis: Long,
    val recoveryMillis: Long,
    val equippedIndex: Int?,
    val queued: Boolean,
    val unlock: SkillLoadoutUnlockUiState,
    val canEquip: Boolean,
    val canUnequip: Boolean,
    val canMoveEarlier: Boolean,
    val canMoveLater: Boolean,
    val evolutions: List<SkillEvolutionBranchUiState> = emptyList(),
    val descriptionStringKey: PresentationStringKey = PresentationStringKey.DESC_SKILL
)

data class SkillEvolutionBranchUiState(
    val evolutionId: ContentId,
    val titleStringKey: PresentationStringKey,
    val descriptionStringKey: PresentationStringKey,
    val requiredAffinityTitleStringKey: PresentationStringKey,
    val requiredMasteryLevel: Long,
    val currentMasteryLevel: Long,
    val selected: Boolean,
    val canSelect: Boolean
)

data class SkillLoadoutSlotUiState(
    val index: Int,
    val skill: SkillLoadoutSkillUiState?
)

/**
 * FUI-04 immutable projection of the canonical ordered skill loadout.
 *
 * Capacity, unlock state, order, and queued state all originate from backend state/content
 * or backend read queries. This model carries no mutation logic.
 */
data class SkillLoadoutUiState(
    val capacity: Int,
    val equippedCount: Int,
    val capacityFull: Boolean,
    val slots: List<SkillLoadoutSlotUiState>,
    val availableSkills: List<SkillLoadoutSkillUiState>,
    val feedback: SkillLoadoutFeedbackUiState? = null
)
