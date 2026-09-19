package com.idlerpg.game.simulation

import com.idlerpg.game.core.number.GameNumber
import com.idlerpg.game.data.local.SaveData
import com.idlerpg.game.data.local.SaveEnvelope
import com.idlerpg.game.data.local.SaveVersion
import com.idlerpg.game.data.local.migration.SaveMigrationRegistry

/** Exact V2 -> V3 wave-state migration plus current-schema deterministic round trip. */
object SaveV3WaveMigrationTest {
    fun run() {
        val runtime = SimulationTestSupport.runtime(seed = 1_303L)
        SimulationTestSupport.startTraining(runtime)
        val current = runtime.state()
        val v3Data = SaveData.fromGameState(current)
        check(v3Data.fields["run.world.currentEncounter.currentWave"] == "1")

        val v2Fields = v3Data.fields.filterKeys { key ->
            key != "run.world.currentEncounter.currentWave" &&
                !key.startsWith("run.inventory.lootFilter.") &&
                key != "run.world.automationMode" &&
                !key.startsWith("run.world.selectedFarmEncounterId.") &&
                key != "run.world.pushFailurePolicy" &&
                !key.startsWith("run.world.clearedEncounterIds.") &&
                !key.startsWith("run.player.selectedSkillEvolutionBySkillId.") &&
                !key.startsWith("meta.heroName.") &&
                !key.startsWith("meta.rebirth.") &&
                !key.startsWith("run.progression.skillProgression.") &&
                !key.endsWith(".enhancementFailstack")
        }
        val v2Envelope = SaveEnvelope(
            schemaVersion = SaveVersion.V2,
            contentVersion = SimulationTestSupport.CONTENT_VERSION,
            writtenAtEpochMs = 1_303_000L,
            data = SaveData(v2Fields)
        )
        val migrated = SaveMigrationRegistry().migrate(v2Envelope)
        check(migrated.schemaVersion == SaveVersion.CURRENT)
        check(migrated.gameState() == current)

        val noEncounter = current.copy(
            run = current.run.copy(
                combat = com.idlerpg.game.domain.model.combat.CombatState(),
                world = current.run.world.copy(currentEncounter = null)
            )
        )
        val absentV3 = SaveData.fromGameState(noEncounter)
        val absentV2 = SaveData(absentV3.fields - "run.world.currentEncounter.currentWave")
        val absentMigrated = SaveMigrationRegistry().migrate(
            SaveEnvelope(
                schemaVersion = SaveVersion.V2,
                contentVersion = SimulationTestSupport.CONTENT_VERSION,
                writtenAtEpochMs = 1_303_001L,
                data = absentV2
            )
        )
        check(absentMigrated.gameState() == noEncounter)
        check("run.world.currentEncounter.currentWave" !in absentMigrated.data.fields)

        val roundTrip = SaveData.fromGameState(migrated.gameState()).toGameState()
        check(roundTrip == current)
        check(SimulationTestSupport.gold(roundTrip) >= GameNumber.ZERO)
    }
}
