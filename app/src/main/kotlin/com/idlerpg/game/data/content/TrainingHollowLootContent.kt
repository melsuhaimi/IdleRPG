package com.idlerpg.game.data.content

import com.idlerpg.game.core.id.ContentId
import com.idlerpg.game.core.number.GameNumber
import com.idlerpg.game.core.number.Ratio
import com.idlerpg.game.domain.definition.Affinity
import com.idlerpg.game.domain.definition.EquipmentSlot
import com.idlerpg.game.domain.definition.Rarity
import com.idlerpg.game.domain.definition.item.AffixDefinition
import com.idlerpg.game.domain.definition.item.AffixEffectDefinition
import com.idlerpg.game.domain.definition.item.EquipmentDefinition
import com.idlerpg.game.domain.definition.item.EquipmentEffectDefinition
import com.idlerpg.game.domain.definition.item.ItemCategory
import com.idlerpg.game.domain.definition.item.ItemDefinition
import com.idlerpg.game.domain.definition.item.LootTableDefinition
import com.idlerpg.game.domain.definition.item.LootTableEntry
import com.idlerpg.game.domain.definition.item.SalvageProfile

/** Compact first-region loot catalog: six slots and sixteen bounded affix identities. */
object TrainingHollowLootContent {
    val BOSS_LOOT_TABLE_ID = ContentId("loot.training_hollow.hollow_warden")
    val ELITE_LOOT_TABLE_ID = ContentId("loot.training_hollow.elite")
    val ANOMALY_LOOT_TABLE_ID = ContentId("loot.training_hollow.anomaly")
    val ARMOR_ITEM_ID = ContentId("item.fracture_mail")
    val HELM_ITEM_ID = ContentId("item.seer_helm")
    val BOOTS_ITEM_ID = ContentId("item.rift_boots")
    val ACCESSORY_ITEM_ID = ContentId("item.echo_sigil")
    val ARMOR_EQUIPMENT_ID = ContentId("equipment.fracture_mail")
    val HELM_EQUIPMENT_ID = ContentId("equipment.seer_helm")
    val BOOTS_EQUIPMENT_ID = ContentId("equipment.rift_boots")
    val ACCESSORY_EQUIPMENT_ID = ContentId("equipment.echo_sigil")

    private val allSlots = EquipmentSlot.values().toSet()
    private val affinityAffixes = Affinity.values().map { affinity ->
        AffixDefinition(
            id = ContentId("affix.resonant.${affinity.id.value.substringAfterLast('.') }"),
            displayName = "${affinityLabel(affinity)} Charge",
            compatibleSlots = setOf(EquipmentSlot.ACCESSORY, EquipmentSlot.CATALYST),
            selectionWeight = 45L,
            minimumRollValue = 1L,
            maximumRollValue = 1L,
            effect = AffixEffectDefinition.ResonanceChargeBonus(affinity)
        )
    }
    private fun attackAffix(id: String, name: String, slots: Set<EquipmentSlot>, weight: Long) =
        AffixDefinition(ContentId(id), name, slots, weight, 1L, 3L,
            AffixEffectDefinition.FlatAttackPowerPerRollUnit(GameNumber.ONE))

    private fun affinityLabel(affinity: Affinity): String = when (affinity) {
        Affinity.MIGHT -> "ATK"
        Affinity.TEMPO -> "SPD"
        Affinity.EMBER -> "Fire"
        Affinity.FROST -> "Ice"
        Affinity.ARCANE -> "Magic"
        Affinity.VITALITY -> "HP"
        Affinity.SHADOW -> "Dark"
        Affinity.GUARD -> "DEF"
    }
    private fun armorAffix(id: String, name: String, slots: Set<EquipmentSlot>, weight: Long) =
        AffixDefinition(ContentId(id), name, slots, weight, 1L, 4L,
            AffixEffectDefinition.FlatArmorPerRollUnit(GameNumber.ONE))

    val affixes = listOf(
        attackAffix("affix.brutal", "Brutal", setOf(EquipmentSlot.WEAPON), 80L),
        attackAffix("affix.relentless", "Relentless", setOf(EquipmentSlot.WEAPON, EquipmentSlot.BOOTS), 65L),
        attackAffix("affix.hollow_tuned", "Hollow-Tuned", allSlots, 40L),
        attackAffix("affix.executioner", "Executioner", setOf(EquipmentSlot.WEAPON, EquipmentSlot.ACCESSORY), 45L),
        armorAffix("affix.reinforced", "Reinforced", setOf(EquipmentSlot.ARMOR, EquipmentSlot.HELM), 85L),
        armorAffix("affix.warded", "Warded", setOf(EquipmentSlot.ARMOR, EquipmentSlot.CATALYST), 65L),
        armorAffix("affix.steadfast", "Steadfast", setOf(EquipmentSlot.BOOTS, EquipmentSlot.ACCESSORY), 55L)
    ) + affinityAffixes

    val equipmentDefinitions = listOf(
        EquipmentDefinition(ARMOR_EQUIPMENT_ID, EquipmentSlot.ARMOR,
            listOf(
                EquipmentEffectDefinition.FlatArmor(GameNumber.of(5L)),
                EquipmentEffectDefinition.SkillHealingMultiplier(
                    DefaultGameContent.GUARD_MEND_ID, Ratio.ofUnits(13_000L)
                )
            )),
        EquipmentDefinition(HELM_EQUIPMENT_ID, EquipmentSlot.HELM,
            listOf(
                EquipmentEffectDefinition.FlatArmor(GameNumber.of(3L)),
                EquipmentEffectDefinition.SkillDamageAgainstStatus(
                    DefaultGameContent.FROST_LANCE_ID,
                    DefaultGameContent.CHILL_STATUS_ID,
                    Ratio.ofUnits(15_000L)
                )
            )),
        EquipmentDefinition(BOOTS_EQUIPMENT_ID, EquipmentSlot.BOOTS,
            listOf(
                EquipmentEffectDefinition.FlatAttackPower(GameNumber.ONE),
                EquipmentEffectDefinition.SkillResonanceBonus(
                    DefaultGameContent.QUICK_SLASH_ID, Affinity.TEMPO, GameNumber.ONE
                )
            )),
        EquipmentDefinition(ACCESSORY_EQUIPMENT_ID, EquipmentSlot.ACCESSORY,
            listOf(
                EquipmentEffectDefinition.ResonanceChargeBonus(Affinity.ARCANE, GameNumber.ONE),
                EquipmentEffectDefinition.SkillExecute(
                    DefaultGameContent.UMBRAL_CUT_ID,
                    Ratio.ofUnits(3_500L),
                    Ratio.ofUnits(16_000L)
                )
            ))
    )

    private val salvage = SalvageProfile(
        Rarity.ordered().associateWith { rarity -> GameNumber.of((rarity.rank + 1L) * 3L) }
    )
    val items = listOf(
        item(ARMOR_ITEM_ID, "Fracture Mail", ARMOR_EQUIPMENT_ID, EquipmentSlot.ARMOR),
        item(HELM_ITEM_ID, "Seer's Helm", HELM_EQUIPMENT_ID, EquipmentSlot.HELM),
        item(BOOTS_ITEM_ID, "Riftstep Boots", BOOTS_EQUIPMENT_ID, EquipmentSlot.BOOTS),
        item(ACCESSORY_ITEM_ID, "Legacy Sigil", ACCESSORY_EQUIPMENT_ID, EquipmentSlot.ACCESSORY)
    )

    val lootTable = LootTableDefinition(
        id = DefaultGameContent.TRAINING_SLIME_LOOT_TABLE_ID,
        rolls = 1,
        dropChancePerRoll = Ratio.ofUnits(700L),
        entries = (
            listOf(DefaultGameContent.TRAINING_BLADE_ITEM_ID, DefaultGameContent.TRAINING_CATALYST_ITEM_ID) +
                items.map { it.id }
            ).map { itemId ->
                LootTableEntry(
                    itemDefinitionId = itemId,
                    weight = 100L,
                    rarityWeights = mapOf(
                        Rarity.COMMON to 6_000L,
                        Rarity.UNCOMMON to 2_700L,
                        Rarity.RARE to 1_000L,
                        Rarity.EPIC to 270L,
                        Rarity.LEGENDARY to 30L
                    )
                )
            }
    )
    val eliteLootTable = lootTable.copy(
        id = ELITE_LOOT_TABLE_ID,
        dropChancePerRoll = Ratio.ofUnits(2_500L)
    )
    val anomalyLootTable = lootTable.copy(
        id = ANOMALY_LOOT_TABLE_ID,
        dropChancePerRoll = Ratio.ofUnits(1_800L)
    )
    val bossLootTable = lootTable.copy(id = BOSS_LOOT_TABLE_ID, dropChancePerRoll = Ratio.ONE,
        entries = lootTable.entries.map { it.copy(rarityWeights = mapOf(Rarity.RARE to 7_500L, Rarity.EPIC to 2_250L, Rarity.LEGENDARY to 250L)) })

    val lootTables = listOf(lootTable, eliteLootTable, anomalyLootTable, bossLootTable)

    fun allowedAffixIdsFor(slot: EquipmentSlot): List<ContentId> =
        (affixes.filter { slot in it.compatibleSlots }.map { it.id } +
            if (slot == EquipmentSlot.WEAPON) listOf(DefaultGameContent.KEEN_AFFIX_ID) else emptyList())
            .sorted()

    private fun item(id: ContentId, name: String, equipmentId: ContentId, slot: EquipmentSlot) = ItemDefinition(
        id = id,
        displayName = name,
        category = ItemCategory.EQUIPMENT,
        minimumRarity = Rarity.COMMON,
        maximumRarity = Rarity.LEGENDARY,
        equipmentDefinitionId = equipmentId,
        allowedAffixIds = allowedAffixIdsFor(slot),
        salvageProfile = salvage
    )
}
