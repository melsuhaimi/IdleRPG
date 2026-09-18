package com.idlerpg.game.presentation.model

import com.idlerpg.game.core.id.ContentId
import com.idlerpg.game.core.id.InstanceId
import com.idlerpg.game.domain.command.CommandRejectionCode
import com.idlerpg.game.domain.definition.EquipmentSlot
import com.idlerpg.game.domain.definition.Rarity
import com.idlerpg.game.presentation.content.PresentationStringKey
import com.idlerpg.game.presentation.content.PresentationAssetKey

enum class GearEffectKind {
    FLAT_ATTACK_POWER,
    FLAT_ARMOR,
    RESONANCE_CHARGE_BONUS,
    SKILL_TRAIT
}

data class GearEffectUiState(
    val kind: GearEffectKind,
    val amountDisplay: String,
    val affinityId: ContentId? = null,
    val affinityTitleStringKey: PresentationStringKey? = null,
    val skillTitleStringKey: PresentationStringKey? = null
)

data class GearAffixUiState(
    val affixId: ContentId,
    val titleStringKey: PresentationStringKey,
    val iconAssetKey: PresentationAssetKey,
    val rolledValue: Long,
    val isMainStat: Boolean = false,
    val minimumRoll: Long = rolledValue,
    val maximumRoll: Long = rolledValue
)

/**
 * Authoritative read-only preview for equipping an item into its authored slot.
 *
 * Values are produced by the presentation projector from the canonical derived-stat system;
 * Compose must display them, never recalculate them.
 */
data class GearComparisonUiState(
    val currentAttackDisplay: String,
    val resultingAttackDisplay: String,
    val attackDeltaDisplay: String,
    val currentArmorDisplay: String,
    val resultingArmorDisplay: String,
    val armorDeltaDisplay: String
)

data class GearItemUiState(
    val instanceId: InstanceId,
    val definitionId: ContentId,
    val titleStringKey: PresentationStringKey,
    val iconAssetKey: PresentationAssetKey,
    val rarityId: ContentId,
    val rarityTitleStringKey: PresentationStringKey,
    val equipmentSlot: EquipmentSlot?,
    val equippedSlot: EquipmentSlot?,
    val locked: Boolean,
    val overflow: Boolean,
    val salvageGoldDisplay: String,
    val affixes: List<GearAffixUiState>,
    val effects: List<GearEffectUiState>,
    val canEquip: Boolean,
    val canUnequip: Boolean,
    val canLock: Boolean,
    val canUnlock: Boolean,
    val canSalvage: Boolean,
    val canClaimOverflow: Boolean,
    val canSalvageOverflow: Boolean,
    val comparison: GearComparisonUiState? = null,
    val mainStat: GearAffixUiState? = null,
    val enhancementLevel: Int = 0,
    val enhancementLabel: String = "+0",
    val enhancementTargetLabel: String = "MAX",
    val enhancementFailstack: Int = 0,
    val enhancementSuccessChanceDisplay: String = "0%",
    val enhancementMaterialCostDisplay: String = "0",
    val enhancementProtectionGemCostDisplay: String = "0",
    val enhancementFailureLevelDisplay: String = "+0",
    val enhancementFailureFailstackDisplay: String = "0",
    val refinementMaterialCostDisplay: String = "0",
    val canEnhance: Boolean = false,
    val canEnhanceWithProtection: Boolean = false,
    val canRefine: Boolean = false
)

data class GearEquipmentSlotUiState(
    val slot: EquipmentSlot,
    val equippedItem: GearItemUiState?
)

data class GearCapacityUiState(
    val normalUsed: Long,
    val normalCapacity: Long,
    val normalAvailable: Long,
    val normalProgressUnits: Int,
    val overflowUsed: Long,
    val overflowCapacity: Long,
    val overflowAvailable: Long,
    val overflowProgressUnits: Int,
    val capacityPerExpansion: Long,
    val maximumCapacity: Long,
    val nextExpansionGoldCostDisplay: String,
    val canAffordExpansion: Boolean,
    val atMaximumCapacity: Boolean,
    val progressionBlocked: Boolean
)

enum class GearFeedbackKind {
    EQUIPPED,
    UNEQUIPPED,
    LOCKED,
    UNLOCKED,
    SALVAGED,
    CAPACITY_EXPANDED,
    OVERFLOW_CLAIMED,
    OVERFLOW_SALVAGED,
    ITEM_TO_OVERFLOW,
    ENHANCED,
    REFINED,
    PROGRESSION_BLOCKED,
    PROGRESSION_UNBLOCKED,
    COMMAND_REJECTED
}

data class GearFeedbackUiState(
    val sequenceNumber: Long?,
    val kind: GearFeedbackKind,
    val itemInstanceId: InstanceId? = null,
    val itemDefinitionId: ContentId? = null,
    val slot: EquipmentSlot? = null,
    val amountDisplay: String? = null,
    val previousCapacity: Long? = null,
    val newCapacity: Long? = null,
    val availableStorageSlots: Long? = null,
    val enhancementSucceeded: Boolean? = null,
    val rejectionCode: CommandRejectionCode? = null
)

data class GearUiState(
    val capacity: GearCapacityUiState,
    val equipmentSlots: List<GearEquipmentSlotUiState>,
    val ownedItems: List<GearItemUiState>,
    val overflowItems: List<GearItemUiState>,
    val autoSalvageEnabled: Boolean = true,
    val minimumKeepRarity: Rarity = Rarity.RARE,
    val feedback: GearFeedbackUiState? = null
)
