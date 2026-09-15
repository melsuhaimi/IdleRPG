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

        val targetEnemy = baseline.enemies.firstOrNull { enemy ->
            baseline.encounters.any { encounter ->
                encounter.waveEnemyDefinitionIds.isEmpty() &&
                    enemy.id in encounter.enemyDefinitionIds
            }
        } ?: error("Expected an encounter-referenced enemy")
        val targetEncounter = baseline.encounters.first { encounter ->
            encounter.waveEnemyDefinitionIds.isEmpty() &&
                targetEnemy.id in encounter.enemyDefinitionIds
        }
        val missingAttack = baseline.copy(
            enemies = baseline.enemies.map { enemy ->
                if (enemy.id == targetEnemy.id) {
                    enemy.copy(attackDefinitionId = null)
                } else {
                    enemy
                }
            }
        )
        val missingAttackResult = ContentValidator.validate(missingAttack)

        check(!missingAttackResult.isValid)
        check(missingAttackResult.errors.any { issue ->
            issue.code == "content.encounter.enemy_missing_attack" &&
                issue.path ==
                    "encounters[${targetEncounter.id}].enemyDefinitionIds[${targetEnemy.id}].attackDefinitionId"
        })
    }
}
