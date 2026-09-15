package com.idlerpg.game.simulation

import com.idlerpg.game.core.config.BalanceConfig
import com.idlerpg.game.core.id.ContentId
import com.idlerpg.game.core.id.InstanceId
import com.idlerpg.game.data.content.ContentRegistry
import com.idlerpg.game.data.content.DefaultGameContent
import com.idlerpg.game.data.content.TrainingHollowLootContent
import com.idlerpg.game.domain.definition.EquipmentSlot
import com.idlerpg.game.domain.definition.Rarity
import com.idlerpg.game.domain.model.GameState
import com.idlerpg.game.domain.model.inventory.ItemInstance
import com.idlerpg.game.presentation.content.PresentationAssetKey
import com.idlerpg.game.presentation.content.PresentationContentRegistry
import com.idlerpg.game.presentation.content.PresentationStringKey
import com.idlerpg.game.presentation.projection.GearProjector
import com.idlerpg.game.presentation.query.DefaultGearReadQueries

/** Every first-region drop projects its own localized identity and usable icon. */
object LootPresentationIdentityScenarioTest {
    fun run() {
        val content = ContentRegistry(DefaultGameContent.create())
        val presentation = PresentationContentRegistry.default()
        val expectedItems = linkedMapOf(
            DefaultGameContent.TRAINING_BLADE_ITEM_ID to
                (PresentationStringKey.TRAINING_BLADE to PresentationAssetKey.TRAINING_BLADE),
            DefaultGameContent.TRAINING_CATALYST_ITEM_ID to
                (PresentationStringKey.TRAINING_CATALYST to PresentationAssetKey.TRAINING_CATALYST),
            TrainingHollowLootContent.ARMOR_ITEM_ID to
                (PresentationStringKey.FRACTURE_MAIL to PresentationAssetKey.FRACTURE_MAIL),
            TrainingHollowLootContent.HELM_ITEM_ID to
                (PresentationStringKey.SEERS_HELM to PresentationAssetKey.SEERS_HELM),
            TrainingHollowLootContent.BOOTS_ITEM_ID to
                (PresentationStringKey.RIFTSTEP_BOOTS to PresentationAssetKey.RIFTSTEP_BOOTS),
            TrainingHollowLootContent.ACCESSORY_ITEM_ID to
                (PresentationStringKey.ECHO_SIGIL to PresentationAssetKey.ECHO_SIGIL),
            TrainingHollowLootContent.VOIDGLASS_EDGE_ITEM_ID to
                (PresentationStringKey.VOIDGLASS_EDGE to PresentationAssetKey.VOIDGLASS_EDGE),
            TrainingHollowLootContent.WARDEN_PLATE_ITEM_ID to
                (PresentationStringKey.WARDEN_PLATE to PresentationAssetKey.WARDEN_PLATE),
            TrainingHollowLootContent.STARFALL_VISOR_ITEM_ID to
                (PresentationStringKey.STARFALL_VISOR to PresentationAssetKey.STARFALL_VISOR),
            TrainingHollowLootContent.RESONANT_CORE_ITEM_ID to
                (PresentationStringKey.RESONANT_CORE to PresentationAssetKey.RESONANT_CORE),
            TrainingHollowLootContent.DUSK_SIGIL_ITEM_ID to
                (PresentationStringKey.DUSK_SIGIL to PresentationAssetKey.DUSK_SIGIL)
        )
        expectedItems.forEach { (id, expected) ->
            val entry = presentation.entry(id)
            check(entry.titleStringKey == expected.first)
            check(entry.iconAssetKey == expected.second)
        }
        check(expectedItems.values.map { it.first }.toSet().size == expectedItems.size)
        check(expectedItems.values.map { it.second }.toSet().size == expectedItems.size)

        val expectedAffixes = linkedMapOf(
            "affix.brutal" to (PresentationStringKey.BRUTAL to PresentationAssetKey.MIGHT),
            "affix.relentless" to (PresentationStringKey.RELENTLESS to PresentationAssetKey.TEMPO),
            "affix.hollow_tuned" to (PresentationStringKey.HOLLOW_TUNED to PresentationAssetKey.ARCANE),
            "affix.executioner" to (PresentationStringKey.EXECUTIONER_AFFIX to PresentationAssetKey.SHADOW),
            "affix.reinforced" to (PresentationStringKey.REINFORCED to PresentationAssetKey.GUARD),
            "affix.warded" to (PresentationStringKey.WARDED to PresentationAssetKey.FROST),
            "affix.steadfast" to (PresentationStringKey.STEADFAST to PresentationAssetKey.VITALITY),
            "affix.resonant.might" to (PresentationStringKey.MIGHT_RESONANCE to PresentationAssetKey.MIGHT),
            "affix.resonant.tempo" to (PresentationStringKey.TEMPO_RESONANCE to PresentationAssetKey.TEMPO),
            "affix.resonant.ember" to (PresentationStringKey.EMBER_RESONANCE to PresentationAssetKey.EMBER),
            "affix.resonant.frost" to (PresentationStringKey.FROST_RESONANCE to PresentationAssetKey.FROST),
            "affix.resonant.arcane" to (PresentationStringKey.ARCANE_RESONANCE to PresentationAssetKey.ARCANE),
            "affix.resonant.vitality" to (PresentationStringKey.VITALITY_RESONANCE to PresentationAssetKey.VITALITY),
            "affix.resonant.shadow" to (PresentationStringKey.SHADOW_RESONANCE to PresentationAssetKey.SHADOW),
            "affix.resonant.guard" to (PresentationStringKey.GUARD_RESONANCE to PresentationAssetKey.GUARD)
        )
        check(TrainingHollowLootContent.affixes.map { it.id.value }.toSet() ==
            expectedAffixes.keys)
        val keenEntry = presentation.entry(DefaultGameContent.KEEN_AFFIX_ID)
        check(keenEntry.titleStringKey == PresentationStringKey.KEEN)
        check(keenEntry.iconAssetKey == PresentationAssetKey.KEEN)
        check(content.allAffixes().map { it.id }.toSet() ==
            expectedAffixes.keys.map(::ContentId).toSet() + DefaultGameContent.KEEN_AFFIX_ID)
        expectedAffixes.forEach { (id, expected) ->
            val entry = presentation.entry(ContentId(id))
            check(entry.titleStringKey == expected.first)
            check(entry.iconAssetKey == expected.second)
            check(entry.titleStringKey != PresentationStringKey.KEEN)
        }
        check(expectedAffixes.values.map { it.first }.toSet().size == expectedAffixes.size)

        val items = expectedItems.keys.mapIndexed { index, definitionId ->
            val instanceId = InstanceId(20_000L + index)
            instanceId to ItemInstance(instanceId, definitionId, Rarity.RARE)
        }.toMap()
        val base = GameState.newGame(10_201L)
        val state = base.copy(run = base.run.copy(
            inventory = base.run.inventory.copy(itemsById = items)
        ))
        val gear = GearProjector(
            contentRegistry = content,
            presentationContentRegistry = presentation,
            readQueries = DefaultGearReadQueries(BalanceConfig())
        ).project(state)
        gear.ownedItems.forEach { item ->
            check(item.iconAssetKey == expectedItems.getValue(item.definitionId).second)
        }
        check(gear.equipmentSlots.map { it.slot } == EquipmentSlot.values().toList())
        check(gear.equipmentSlots.size == 6)
    }
}
