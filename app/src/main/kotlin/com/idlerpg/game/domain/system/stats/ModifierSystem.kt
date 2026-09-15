package com.idlerpg.game.domain.system.stats

import com.idlerpg.game.core.number.GameNumber
import com.idlerpg.game.core.number.GameMath
import com.idlerpg.game.core.number.Ratio
import com.idlerpg.game.core.id.ContentId
import com.idlerpg.game.core.id.InstanceId
import com.idlerpg.game.data.content.ContentRegistry
import com.idlerpg.game.domain.definition.Affinity
import com.idlerpg.game.domain.definition.EquipmentSlot
import com.idlerpg.game.domain.definition.combat.StatusModifierDefinition
import com.idlerpg.game.domain.definition.economy.UpgradeEffectDefinition
import com.idlerpg.game.domain.definition.item.AffixEffectDefinition
import com.idlerpg.game.domain.definition.item.EquipmentEffectDefinition
import com.idlerpg.game.domain.model.GameState
import com.idlerpg.game.domain.system.inventory.EquipmentScalingSystem
import com.idlerpg.game.domain.system.rebirth.RebirthStatSystem

/**
 * Deterministic modifier aggregation.
 *
 * Foundation 12 activates equipment/affix/status contributions in addition to run
 * upgrades. Collection order is explicit: upgrades by ContentId, equipment by stable slot,
 * item affixes by ContentId, then active player statuses by InstanceId.
 */
object ModifierSystem {

    fun attackPower(
        state: GameState,
        base: GameNumber,
        contentRegistry: ContentRegistry
    ): GameNumber {
        var result = base

        for (upgrade in contentRegistry.allUpgrades()) {
            val purchasedLevel = state.run.economy.upgrades.levelByUpgradeId[upgrade.id] ?: 0L
            val level = upgrade.effectiveLevel(purchasedLevel)
            if (level == 0L) continue
            when (val effect = upgrade.effect) {
                is UpgradeEffectDefinition.FlatAttackPowerPerLevel ->
                    result += effect.amountPerLevel * level
                else -> Unit
            }
        }

        forEachEquippedEffectWithRarity(state, contentRegistry) { rarity, enhancementLevel, effect ->
            if (effect is EquipmentEffectDefinition.FlatAttackPower) {
                result += EquipmentScalingSystem.scaleFlat(effect.amount, rarity, enhancementLevel)
            }
        }
        forEachEquippedAffix(state, contentRegistry) { rolledValue, effect ->
            if (effect is AffixEffectDefinition.FlatAttackPowerPerRollUnit) {
                result += effect.amountPerRollUnit * rolledValue
            }
        }

        val playerStatuses = state.run.combat.playerCombatant
            ?.statusEffects
            ?.sortedBy { it.instanceId }
            .orEmpty()
        for (status in playerStatuses) {
            val definition = contentRegistry.statusOrNull(status.definitionId) ?: continue
            for (modifier in definition.modifiers) {
                if (modifier is StatusModifierDefinition.FlatAttackPower) {
                    result += modifier.amountPerStack * status.stackCount.toLong()
                }
            }
        }

        return result
    }

    fun armor(
        state: GameState,
        base: GameNumber,
        contentRegistry: ContentRegistry
    ): GameNumber {
        var result = base
        for (upgrade in contentRegistry.allUpgrades()) {
            val level = upgrade.effectiveLevel(
                state.run.economy.upgrades.levelByUpgradeId[upgrade.id] ?: 0L
            )
            if (level > 0L && upgrade.effect is UpgradeEffectDefinition.FlatArmorPerLevel) {
                result += upgrade.effect.amountPerLevel * level
            }
        }
        forEachEquippedEffectWithRarity(state, contentRegistry) { rarity, enhancementLevel, effect ->
            if (effect is EquipmentEffectDefinition.FlatArmor) {
                result += EquipmentScalingSystem.scaleFlat(effect.amount, rarity, enhancementLevel)
            }
        }
        forEachEquippedAffix(state, contentRegistry) { rolledValue, effect ->
            if (effect is AffixEffectDefinition.FlatArmorPerRollUnit) {
                result += effect.amountPerRollUnit * rolledValue
            }
        }
        val playerStatuses = state.run.combat.playerCombatant
            ?.statusEffects
            ?.sortedBy { it.instanceId }
            .orEmpty()
        for (status in playerStatuses) {
            val definition = contentRegistry.statusOrNull(status.definitionId) ?: continue
            for (modifier in definition.modifiers) {
                if (modifier is StatusModifierDefinition.FlatArmor) {
                    result += modifier.amountPerStack * status.stackCount.toLong()
                }
            }
        }
        return result
    }

    fun maximumHealth(state: GameState, base: GameNumber, contentRegistry: ContentRegistry): GameNumber {
        var result = base
        for (upgrade in contentRegistry.allUpgrades()) {
            val level = upgrade.effectiveLevel(state.run.economy.upgrades.levelByUpgradeId[upgrade.id] ?: 0L)
            if (level > 0L && upgrade.effect is UpgradeEffectDefinition.FlatMaxHealthPerLevel) {
                result += upgrade.effect.amountPerLevel * level
            }
        }
        return result
    }

    fun actionSpeed(state: GameState, base: Ratio, contentRegistry: ContentRegistry): Ratio =
        ratioStat(state, base, contentRegistry) { effect ->
            (effect as? UpgradeEffectDefinition.ActionSpeedPerLevel)?.ratioPerLevel
        }

    fun criticalChance(state: GameState, base: Ratio, contentRegistry: ContentRegistry): Ratio =
        ratioStat(state, base, contentRegistry) { effect ->
            (effect as? UpgradeEffectDefinition.CriticalChancePerLevel)?.ratioPerLevel
        }

    fun criticalMultiplier(state: GameState, base: Ratio, contentRegistry: ContentRegistry): Ratio =
        ratioStat(state, base, contentRegistry) { effect ->
            (effect as? UpgradeEffectDefinition.CriticalMultiplierPerLevel)?.ratioPerLevel
        }

    fun effectPower(state: GameState, base: Ratio, contentRegistry: ContentRegistry): Ratio =
        ratioStat(state, base, contentRegistry) { effect ->
            (effect as? UpgradeEffectDefinition.EffectPowerPerLevel)?.ratioPerLevel
        }

    fun healingPower(state: GameState, base: Ratio, contentRegistry: ContentRegistry): Ratio =
        ratioStat(state, base, contentRegistry) { effect ->
            (effect as? UpgradeEffectDefinition.HealingPowerPerLevel)?.ratioPerLevel
        }

    private fun ratioStat(
        state: GameState,
        base: Ratio,
        contentRegistry: ContentRegistry,
        amount: (UpgradeEffectDefinition) -> Ratio?
    ): Ratio {
        var units = base.units
        for (upgrade in contentRegistry.allUpgrades()) {
            val perLevel = amount(upgrade.effect) ?: continue
            val level = upgrade.effectiveLevel(state.run.economy.upgrades.levelByUpgradeId[upgrade.id] ?: 0L)
            units = GameMath.ratioAfterSteps(
                base = Ratio.ofUnits(units),
                growthPerStep = perLevel,
                steps = level
            ).units
        }
        return Ratio.ofUnits(units)
    }

    fun resonanceEmissionAmount(
        state: GameState,
        affinity: Affinity,
        baseAmount: GameNumber,
        contentRegistry: ContentRegistry
    ): GameNumber {
        var result = baseAmount
        forEachEquippedEffect(state, contentRegistry) { effect ->
            if (effect is EquipmentEffectDefinition.ResonanceChargeBonus &&
                effect.affinity == affinity
            ) {
                result += effect.amountPerEmission
            }
        }
        forEachEquippedAffix(state, contentRegistry) { _, effect ->
            if (effect is AffixEffectDefinition.ResonanceChargeBonus &&
                effect.affinity == affinity
            ) {
                result += effect.amountPerEmission
            }
        }
        return result
    }

    fun skillDamageMultiplier(
        state: GameState,
        skillId: ContentId,
        targetId: InstanceId,
        contentRegistry: ContentRegistry
    ): Ratio {
        val target = state.run.combat.enemies.firstOrNull { it.instanceId == targetId }
            ?: return Ratio.ONE
        var result = Ratio.ONE
        forEachEquippedEffect(state, contentRegistry) { effect ->
            when (effect) {
                is EquipmentEffectDefinition.SkillDamageAgainstStatus ->
                    if (effect.skillId == skillId && target.combatant.statusEffects.any {
                            it.definitionId == effect.statusId
                        }) result = multiplyRatios(result, effect.multiplier)
                is EquipmentEffectDefinition.SkillExecute ->
                    if (effect.skillId == skillId) {
                        val enemy = contentRegistry.enemy(target.definitionId)
                        val maximum = com.idlerpg.game.domain.system.enemy.EnemyScalingSystem.scaledHealth(
                            enemy, state.run.world.activeRegionId?.let(contentRegistry::regionOrNull), target.scalingTier
                        )
                        if (target.combatant.currentHealth * Ratio.UNITS_PER_ONE <=
                            maximum * effect.healthThreshold.units
                        ) result = multiplyRatios(result, effect.multiplier)
                    }
                else -> Unit
            }
        }
        return result
    }

    fun skillCleaveRatio(state: GameState, skillId: ContentId, contentRegistry: ContentRegistry): Ratio? {
        var result: Ratio? = null
        forEachEquippedEffect(state, contentRegistry) { effect ->
            if (effect is EquipmentEffectDefinition.SkillCleave && effect.skillId == skillId) {
                result = effect.secondaryDamageRatio
            }
        }
        return result
    }

    fun skillHealingMultiplier(state: GameState, skillId: ContentId, contentRegistry: ContentRegistry): Ratio {
        var result = Ratio.ONE
        forEachEquippedEffect(state, contentRegistry) { effect ->
            if (effect is EquipmentEffectDefinition.SkillHealingMultiplier && effect.skillId == skillId) {
                result = multiplyRatios(result, effect.multiplier)
            }
        }
        return result
    }

    fun skillResonanceBonuses(
        state: GameState,
        skillId: ContentId,
        contentRegistry: ContentRegistry
    ): Map<Affinity, GameNumber> {
        val result = linkedMapOf<Affinity, GameNumber>()
        val maximumHealth = maximumHealth(
            state,
            RebirthStatSystem.apply(
                PlayerScalingSystem.baseStatsForLevel(
                    state.run.player.baseStats,
                    state.run.progression.playerLevel.level
                ),
                state.meta.rebirth
            ).maxHealth,
            contentRegistry
        )
        forEachEquippedEffect(state, contentRegistry) { effect ->
            if (effect is EquipmentEffectDefinition.SkillResonanceBonus && effect.skillId == skillId) {
                val threshold = effect.playerHealthAtMost
                val qualifies = threshold == null ||
                    state.run.player.currentHealth * Ratio.UNITS_PER_ONE <= maximumHealth * threshold.units
                if (qualifies) result[effect.affinity] = (result[effect.affinity] ?: GameNumber.ZERO) + effect.amount
            }
        }
        return result
    }

    fun preservedSequenceEntries(state: GameState, skillId: ContentId, contentRegistry: ContentRegistry): Int {
        var result = 0
        forEachEquippedEffect(state, contentRegistry) { effect ->
            if (effect is EquipmentEffectDefinition.SkillSequencePreservation && effect.skillId == skillId) {
                result = maxOf(result, effect.entryCount)
            }
        }
        return result
    }

    private fun multiplyRatios(left: Ratio, right: Ratio): Ratio = Ratio.ofUnits(
        com.idlerpg.game.core.number.GameMath.multiplyRatios(left, right).units
    )

    private inline fun forEachEquippedEffectWithRarity(
        state: GameState,
        contentRegistry: ContentRegistry,
        block: (
            com.idlerpg.game.domain.definition.Rarity,
            Int,
            EquipmentEffectDefinition
        ) -> Unit
    ) {
        for (slot in EquipmentSlot.values().sortedBy { it.id }) {
            val itemId = state.run.inventory.equipment.itemIn(slot) ?: continue
            val item = state.run.inventory.itemsById[itemId]
                ?: error("Equipped item $itemId is not owned")
            val itemDefinition = contentRegistry.item(item.definitionId)
            val equipmentDefinition = contentRegistry.equipment(
                itemDefinition.equipmentDefinitionId
                    ?: error("Equipped item ${item.definitionId} has no equipment definition")
            )
            equipmentDefinition.activeEffects(item.rarity).forEach { effect ->
                block(item.rarity, item.enhancementLevel, effect)
            }
        }
    }

    private inline fun forEachEquippedEffect(
        state: GameState,
        contentRegistry: ContentRegistry,
        block: (EquipmentEffectDefinition) -> Unit
    ) {
        for (slot in EquipmentSlot.values().sortedBy { it.id }) {
            val itemId = state.run.inventory.equipment.itemIn(slot) ?: continue
            val item = state.run.inventory.itemsById[itemId]
                ?: error("Equipped item $itemId is not owned")
            val itemDefinition = contentRegistry.item(item.definitionId)
            val equipmentDefinition = contentRegistry.equipment(
                itemDefinition.equipmentDefinitionId
                    ?: error("Equipped item ${item.definitionId} has no equipment definition")
            )
            equipmentDefinition.activeEffects(item.rarity).forEach(block)
        }
    }

    private inline fun forEachEquippedAffix(
        state: GameState,
        contentRegistry: ContentRegistry,
        block: (Long, AffixEffectDefinition) -> Unit
    ) {
        for (slot in EquipmentSlot.values().sortedBy { it.id }) {
            val itemId = state.run.inventory.equipment.itemIn(slot) ?: continue
            val item = state.run.inventory.itemsById[itemId]
                ?: error("Equipped item $itemId is not owned")
            for (rolled in item.affixes.sortedBy { it.affixId }) {
                val effect = contentRegistry.affix(rolled.affixId).effect ?: continue
                block(rolled.value, effect)
            }
        }
    }
}
