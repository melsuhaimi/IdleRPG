package com.idlerpg.game.data.local.migration

import com.idlerpg.game.data.local.SaveData
import com.idlerpg.game.data.local.SaveVersion

/** Initializes Push mode and conservatively reconstructs mastered Training Hollow stages. */
class V4ToV5SaveMigration : SaveMigration {
    override val fromVersion: SaveVersion = SaveVersion.V4
    override val toVersion: SaveVersion = SaveVersion.V5

    override fun migrate(data: SaveData): SaveData {
        val fields = data.fields.toMutableMap()
        fields.putIfAbsent("run.world.automationMode", "PUSH")
        fields.putIfAbsent("run.world.selectedFarmEncounterId.present", "0")
        fields.putIfAbsent("run.world.pushFailurePolicy", "FARM_HIGHEST_CLEARED")
        if ("run.world.clearedEncounterIds.count" !in fields) {
            val trainingIndex = (0 until (fields["run.world.regionProgressById.count"]?.toIntOrNull() ?: 0))
                .firstOrNull { index ->
                    fields["run.world.regionProgressById.$index.regionId"] == TRAINING_HOLLOW_ID
                }
            val predecessorDepth = trainingIndex?.let { index ->
                fields["run.world.regionProgressById.$index.progress.highestClearedEncounterTier"]
                    ?.toLongOrNull()
            }?.coerceAtMost(8L) ?: 0L
            val currentEncounterId = if (fields["run.world.currentEncounter.present"] == "1" && fields["run.world.currentEncounter.status"] == "CLEARED") {
                fields["run.world.currentEncounter.definitionId"]
            } else null
            val inferredDepth = maxOf(
                predecessorDepth,
                (TRAINING_HOLLOW_STAGE_IDS.take(8).indexOf(currentEncounterId) + 1).toLong()
            ).toInt()
            val clearedIds = TRAINING_HOLLOW_STAGE_IDS.take(inferredDepth)
            fields["run.world.clearedEncounterIds.count"] = clearedIds.size.toString()
            clearedIds.forEachIndexed { index, id ->
                fields["run.world.clearedEncounterIds.$index"] = id
            }
        }
        return SaveData(fields)
    }

    private companion object {
        const val TRAINING_HOLLOW_ID = "region.training_hollow"
        val TRAINING_HOLLOW_STAGE_IDS = listOf(
            "encounter.training_hollow.slime",
            "encounter.training_hollow.riftfang",
            "encounter.training_hollow.cinder_wisp",
            "encounter.training_hollow.hollow_bulwark",
            "encounter.training_hollow.arcane_seer",
            "encounter.training_hollow.frostbound_mite",
            "encounter.training_hollow.echo_leech",
            "encounter.training_hollow.shade_mimic"
        ) + (9..30).map { stage ->
            "encounter.training_hollow.stage_${stage.toString().padStart(2, '0')}"
        }
    }
}
