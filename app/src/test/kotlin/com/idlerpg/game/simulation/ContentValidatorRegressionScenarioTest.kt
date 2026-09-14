package com.idlerpg.game.simulation

import com.idlerpg.game.core.id.ContentId
import com.idlerpg.game.data.content.ContentValidator
import com.idlerpg.game.data.content.DefaultGameContent

/** Locks encounter reward-loot references at the authored-content validation seam. */
object ContentValidatorRegressionScenarioTest {
    fun run() {
        val baseline = DefaultGameContent.create()
        check(ContentValidator.validate(baseline).isValid)

        val target = baseline.encounters.firstOrNull { it.rewardLootTableId != null }
            ?: error("Training Hollow must contain an encounter reward loot table")
        val missingId = ContentId("loot.missing.compile_regression")
        val invalid = baseline.copy(
            encounters = baseline.encounters.map { encounter ->
                if (encounter.id == target.id) {
                    encounter.copy(rewardLootTableId = missingId)
                } else {
                    encounter
                }
            }
        )
        val result = ContentValidator.validate(invalid)

        check(!result.isValid)
        check(result.errors.any { issue ->
            issue.code == "content.encounter.unknown_reward_loot_table" &&
                issue.path == "encounters[${target.id}].rewardLootTableId"
        })
    }
}
