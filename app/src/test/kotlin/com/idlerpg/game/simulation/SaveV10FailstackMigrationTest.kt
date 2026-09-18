package com.idlerpg.game.simulation

import com.idlerpg.game.core.id.InstanceId
import com.idlerpg.game.data.content.DefaultGameContent
import com.idlerpg.game.data.local.SaveData
import com.idlerpg.game.data.local.SaveEnvelope
import com.idlerpg.game.data.local.SaveVersion
import com.idlerpg.game.data.local.migration.SaveMigrationRegistry
import com.idlerpg.game.domain.definition.Rarity
import com.idlerpg.game.domain.model.inventory.EnhancementLevel
import com.idlerpg.game.domain.model.inventory.ItemInstance

/** V9 saves gain zeroed failstacks without changing any existing item facts. */
object SaveV10FailstackMigrationTest {
    fun run() {
        val runtime = SimulationTestSupport.runtime(seed = 10_101L)
        val itemId = InstanceId(10_101L)
        val item = ItemInstance(
            instanceId = itemId,
            definitionId = DefaultGameContent.TRAINING_BLADE_ITEM_ID,
            rarity = Rarity.LEGENDARY,
            enhancementLevel = EnhancementLevel.PEN
        )
        val current = runtime.state().copy(
            run = runtime.state().run.copy(
                inventory = runtime.state().run.inventory.copy(
                    itemsById = mapOf(itemId to item)
                )
            )
        )
        val v10Data = SaveData.fromGameState(current)
        val v9Data = SaveData(
            v10Data.fields.filterKeys { !it.endsWith(".enhancementFailstack") }
        )
        val migrated = SaveMigrationRegistry().migrate(
            SaveEnvelope(
                schemaVersion = SaveVersion.V9,
                contentVersion = SimulationTestSupport.CONTENT_VERSION,
                writtenAtEpochMs = 10_101_000L,
                data = v9Data
            )
        )

        check(migrated.schemaVersion == SaveVersion.CURRENT)
        check(migrated.gameState() == current)
        check(
            migrated.gameState().run.inventory.item(itemId)?.enhancementFailstack == 0
        )
    }
}
