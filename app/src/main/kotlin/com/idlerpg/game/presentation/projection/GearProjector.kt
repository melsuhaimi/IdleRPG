package com.idlerpg.game.presentation.projection

import com.idlerpg.game.core.id.ContentId
import com.idlerpg.game.core.number.GameNumber
import com.idlerpg.game.data.content.ContentRegistry
import com.idlerpg.game.domain.definition.Affinity
import com.idlerpg.game.domain.definition.CurrencyId
import com.idlerpg.game.domain.definition.EquipmentSlot
import com.idlerpg.game.domain.definition.item.EquipmentEffectDefinition
import com.idlerpg.game.domain.model.GameState
import com.idlerpg.game.domain.model.inventory.InventoryState
import com.idlerpg.game.domain.model.inventory.EnhancementLevel
import com.idlerpg.game.domain.model.inventory.ItemInstance
import com.idlerpg.game.domain.model.inventory.EquipmentLoadoutState
import com.idlerpg.game.domain.system.stats.DerivedStatSystem
import com.idlerpg.game.presentation.content.PresentationContentRegistry
import com.idlerpg.game.presentation.format.GameNumberFormatter
import com.idlerpg.game.presentation.model.GearAffixUiState
import com.idlerpg.game.presentation.model.GearCapacityUiState
import com.idlerpg.game.presentation.model.GearComparisonUiState
import com.idlerpg.game.presentation.model.GearEffectKind
import com.idlerpg.game.presentation.model.GearEffectUiState
import com.idlerpg.game.presentation.model.GearEquipmentSlotUiState
import com.idlerpg.game.presentation.model.GearFeedbackUiState
import com.idlerpg.game.presentation.model.GearItemUiState
import com.idlerpg.game.presentation.model.GearUiState
import com.idlerpg.game.presentation.query.GearReadQueries
import java.math.BigInteger

/** Pure FUI-07 canonical inventory/equipment -> presentation projection. */
class GearProjector(
    private val contentRegistry: ContentRegistry,
    private val presentationContentRegistry: PresentationContentRegistry,
    private val readQueries: GearReadQueries
) {
    fun project(
        state: GameState,
        feedback: GearFeedbackUiState? = null
    ): GearUiState {
        val inventory = state.run.inventory
        val normalCapacity = readQueries.effectiveNormalCapacity(state)
        val normalAvailable = readQueries.normalSlotsAvailable(state)
        val overflowCapacity = readQueries.overflowCapacity()
        val overflowAvailable = readQueries.overflowSlotsAvailable(state)
        val normalUsed = inventory.itemsById.size.toLong()
        val overflowUsed = inventory.overflowItemsById.size.toLong()
        val nextExpansionCost = readQueries.nextExpansionCost(state)
        val gold = state.run.economy.wallet.amountsByCurrencyId[CurrencyId.GOLD]
            ?: GameNumber.ZERO
        val maximumCapacity = readQueries.maximumCapacity()
        val currentAttack = DerivedStatSystem.attackPower(state, contentRegistry)
        val currentArmor = DerivedStatSystem.armor(state, contentRegistry)

        val owned = inventory.itemsById.values
            .sortedBy { it.instanceId }
            .map { item ->
                projectItem(
                    item = item,
                    state = state,
                    inventory = inventory,
                    currentAttack = currentAttack,
                    currentArmor = currentArmor,
                    overflow = false,
                    canClaimOverflow = false
                )
            }
        val ownedById = owned.associateBy { it.instanceId }

        val overflow = inventory.overflowItemsById.values
            .sortedBy { it.instanceId }
            .map { item ->
                projectItem(
                    item = item,
                    state = state,
                    inventory = inventory,
                    currentAttack = currentAttack,
                    currentArmor = currentArmor,
                    overflow = true,
                    canClaimOverflow = normalAvailable > 0L
                )
            }

        return GearUiState(
            capacity = GearCapacityUiState(
                normalUsed = normalUsed,
                normalCapacity = normalCapacity,
                normalAvailable = normalAvailable,
                normalProgressUnits = progressUnits(normalUsed, normalCapacity),
                overflowUsed = overflowUsed,
                overflowCapacity = overflowCapacity,
                overflowAvailable = overflowAvailable,
                overflowProgressUnits = progressUnits(overflowUsed, overflowCapacity),
                capacityPerExpansion = readQueries.capacityPerExpansion(),
                maximumCapacity = maximumCapacity,
                nextExpansionGoldCostDisplay = GameNumberFormatter.compact(nextExpansionCost),
                canAffordExpansion = gold >= nextExpansionCost,
                atMaximumCapacity = normalCapacity >= maximumCapacity,
                progressionBlocked = readQueries.progressionBlocked(state)
            ),
            equipmentSlots = EquipmentSlot.values().map { slot ->
                GearEquipmentSlotUiState(
                    slot = slot,
                    equippedItem = inventory.equipment.itemIn(slot)?.let(ownedById::get)
                )
            },
            ownedItems = owned,
            overflowItems = overflow,
            autoSalvageEnabled = inventory.lootFilter.autoSalvageEnabled,
            minimumKeepRarity = inventory.lootFilter.minimumKeepRarity,
            feedback = feedback
        )
    }

    private fun projectItem(
        item: ItemInstance,
        state: GameState,
        inventory: InventoryState,
        currentAttack: GameNumber,
        currentArmor: GameNumber,
        overflow: Boolean,
        canClaimOverflow: Boolean
    ): GearItemUiState {
        val definition = contentRegistry.item(item.definitionId)
        val presentation = presentationContentRegistry.entry(item.definitionId)
        val rarityPresentation = presentationContentRegistry.entry(item.rarity.id)
        val equipmentDefinition = definition.equipmentDefinitionId?.let(contentRegistry::equipment)
        val equippedSlot = if (overflow) null else inventory.equipment.slotOf(item.instanceId)
        val locked = !overflow && inventory.locks.isLocked(item.instanceId)

        return GearItemUiState(
            instanceId = item.instanceId,
            definitionId = item.definitionId,
            titleStringKey = presentation.titleStringKey,
            iconAssetKey = presentation.iconAssetKey,
            rarityId = item.rarity.id,
            rarityTitleStringKey = rarityPresentation.titleStringKey,
            equipmentSlot = equipmentDefinition?.slot,
            equippedSlot = equippedSlot,
            locked = locked,
            overflow = overflow,
            salvageGoldDisplay = GameNumberFormatter.compact(
                definition.salvageProfile.goldFor(item.rarity)
            ),
            affixes = item.affixes.map { rolled ->
                val affixPresentation = presentationContentRegistry.entry(rolled.affixId)
                GearAffixUiState(
                    affixId = rolled.affixId,
                    titleStringKey = affixPresentation.titleStringKey,
                    iconAssetKey = affixPresentation.iconAssetKey,
                    rolledValue = rolled.value
                )
            },
            effects = equipmentDefinition?.activeEffects(item.rarity).orEmpty().map(::projectEffect),
            canEquip = !overflow && equipmentDefinition != null && equippedSlot == null,
            canUnequip = !overflow && equippedSlot != null,
            canLock = !overflow && !locked,
            canUnlock = !overflow && locked,
            canSalvage = !overflow && !locked && equippedSlot == null,
            canClaimOverflow = overflow && canClaimOverflow,
            canSalvageOverflow = overflow,
            mainStat = item.mainStat?.let { rolled ->
                val affixPresentation = presentationContentRegistry.entry(rolled.affixId)
                GearAffixUiState(
                    affixId = rolled.affixId,
                    titleStringKey = affixPresentation.titleStringKey,
                    iconAssetKey = affixPresentation.iconAssetKey,
                    rolledValue = rolled.value,
                    isMainStat = true
                )
            },
            enhancementLevel = item.enhancementLevel,
            enhancementLabel = EnhancementLevel.displayName(item.enhancementLevel),
            comparison = comparisonFor(
                item = item,
                state = state,
                inventory = inventory,
                currentAttack = currentAttack,
                currentArmor = currentArmor,
                slot = equipmentDefinition?.slot,
                overflow = overflow,
                equippedSlot = equippedSlot
            )
        )
    }

    private fun comparisonFor(
        item: ItemInstance,
        state: GameState,
        inventory: InventoryState,
        currentAttack: GameNumber,
        currentArmor: GameNumber,
        slot: EquipmentSlot?,
        overflow: Boolean,
        equippedSlot: EquipmentSlot?
    ): GearComparisonUiState? {
        // An overflow item cannot be equipped yet, and an already-equipped item has no pending
        // trade-off to preview. Both cases deliberately omit the comparison rather than implying
        // an action the command layer will reject.
        if (overflow || slot == null || equippedSlot != null) return null

        val simulatedLoadout = inventory.equipment.itemBySlot.toMutableMap().apply {
            put(slot, item.instanceId)
        }
        val simulatedState = state.copy(
            run = state.run.copy(
                inventory = inventory.copy(
                    equipment = EquipmentLoadoutState(simulatedLoadout.toMap())
                )
            )
        )
        val resultingAttack = DerivedStatSystem.attackPower(simulatedState, contentRegistry)
        val resultingArmor = DerivedStatSystem.armor(simulatedState, contentRegistry)
        return GearComparisonUiState(
            currentAttackDisplay = GameNumberFormatter.compact(currentAttack),
            resultingAttackDisplay = GameNumberFormatter.compact(resultingAttack),
            attackDeltaDisplay = signedDelta(currentAttack, resultingAttack),
            currentArmorDisplay = GameNumberFormatter.compact(currentArmor),
            resultingArmorDisplay = GameNumberFormatter.compact(resultingArmor),
            armorDeltaDisplay = signedDelta(currentArmor, resultingArmor)
        )
    }

    private fun signedDelta(before: GameNumber, after: GameNumber): String {
        val delta = after.toBigInteger().subtract(before.toBigInteger())
        if (delta.signum() == 0) return "0"
        val magnitude = GameNumber.fromBigInteger(delta.abs())
        val prefix = if (delta.signum() > 0) "+" else "−"
        return prefix + GameNumberFormatter.compact(magnitude)
    }

    private fun projectEffect(effect: EquipmentEffectDefinition): GearEffectUiState =
        when (effect) {
            is EquipmentEffectDefinition.FlatAttackPower -> GearEffectUiState(
                kind = GearEffectKind.FLAT_ATTACK_POWER,
                amountDisplay = GameNumberFormatter.compact(effect.amount)
            )
            is EquipmentEffectDefinition.FlatArmor -> GearEffectUiState(
                kind = GearEffectKind.FLAT_ARMOR,
                amountDisplay = GameNumberFormatter.compact(effect.amount)
            )
            is EquipmentEffectDefinition.ResonanceChargeBonus -> GearEffectUiState(
                kind = GearEffectKind.RESONANCE_CHARGE_BONUS,
                amountDisplay = GameNumberFormatter.compact(effect.amountPerEmission),
                affinityId = effect.affinity.id,
                affinityTitleStringKey = presentationContentRegistry.entry(effect.affinity.id).titleStringKey
            )
            is EquipmentEffectDefinition.SkillCleave -> trait(effect.skillId, "Cleave")
            is EquipmentEffectDefinition.SkillDamageAgainstStatus -> trait(effect.skillId, "Status Damage")
            is EquipmentEffectDefinition.SkillExecute -> trait(effect.skillId, "Execute")
            is EquipmentEffectDefinition.SkillResonanceBonus -> trait(effect.skillId, "Combo")
            is EquipmentEffectDefinition.SkillHealingMultiplier -> trait(effect.skillId, "HEAL")
            is EquipmentEffectDefinition.SkillSequencePreservation -> trait(effect.skillId, "Combo Memory")
        }

    private fun trait(skillId: ContentId, identity: String) = GearEffectUiState(
        kind = GearEffectKind.SKILL_TRAIT,
        amountDisplay = identity,
        skillTitleStringKey = presentationContentRegistry.entryOrNull(skillId)?.titleStringKey
    )

    private fun progressUnits(used: Long, capacity: Long): Int {
        if (capacity <= 0L || used <= 0L) return 0
        val numerator = BigInteger.valueOf(used)
            .multiply(BigInteger.valueOf(PROGRESS_SCALE.toLong()))
        val denominator = BigInteger.valueOf(capacity)
        return numerator.divide(denominator)
            .coerceAtMost(BigInteger.valueOf(PROGRESS_SCALE.toLong()))
            .toInt()
    }

    companion object {
        private const val PROGRESS_SCALE: Int = 10_000
    }
}
