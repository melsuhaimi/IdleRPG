package com.idlerpg.game.simulation

import com.idlerpg.game.data.content.DefaultGameContent
import com.idlerpg.game.data.content.TrainingHollowWorldContent
import com.idlerpg.game.domain.model.player.BaseStats
import com.idlerpg.game.domain.system.combat.CombatMath
import com.idlerpg.game.domain.system.enemy.EnemyScalingSystem
import com.idlerpg.game.domain.system.progression.PlayerProgressionSystem
import com.idlerpg.game.domain.system.stats.PlayerScalingSystem
import com.idlerpg.game.core.number.GameNumber
import com.idlerpg.game.core.number.Ratio

/** Long-run contract checks for formulas that must remain deterministic as content expands. */
object IncrementalProgressionContractTest {
    fun run() {
        val factory = SimulationTestSupport.factory()
        val registry = factory.contentRegistry

        check(TrainingHollowWorldContent.MAX_STAGE >= 120)
        check(registry.allEncounters().size == TrainingHollowWorldContent.MAX_STAGE)
        check(TrainingHollowWorldContent.additionalBosses.size == 7)
        check(TrainingHollowWorldContent.encounters.count {
            it.type == com.idlerpg.game.domain.definition.world.EncounterType.BOSS
        } == 8)
        check(TrainingHollowWorldContent.encounters.last().nextEncounterId == null)

        val checkpoints = listOf(1L, 10L, 25L, 50L, 100L)
        val levelBase = factory.newGame(18_001L).state()
        val xpCosts = checkpoints.map { level ->
            val state = levelBase.copy(
                run = levelBase.run.copy(
                    progression = levelBase.run.progression.copy(
                        playerLevel = levelBase.run.progression.playerLevel.copy(level = level)
                    )
                )
            )
            PlayerProgressionSystem.experienceToNextLevel(state, registry)
        }
        check(xpCosts == listOf(100L, 108L, 122L, 148L, 221L).map(GameNumber::of))

        fun stateAtLevel(level: Long) = levelBase.copy(
            run = levelBase.run.copy(
                progression = levelBase.run.progression.copy(
                    playerLevel = levelBase.run.progression.playerLevel.copy(level = level)
                )
            )
        )
        check(
            PlayerProgressionSystem.experienceToNextLevel(stateAtLevel(800L), registry) ==
                GameNumber.of(58_206L)
        )
        check(
            PlayerProgressionSystem.experienceToNextLevel(stateAtLevel(801L), registry) ==
                GameNumber.of(58_905L)
        )
        check(
            PlayerProgressionSystem.experienceToNextLevel(stateAtLevel(1_000L), registry) ==
                GameNumber.of(632_511L)
        )
        check(
            PlayerProgressionSystem.experienceToNextLevel(stateAtLevel(14_999L), registry).toPlainString() ==
                "1736681689828688423672605407345848192064237607405376617300084897172117018074535"
        )
        check(PlayerProgressionSystem.experienceToNextLevel(stateAtLevel(15_000L), registry) == GameNumber.ZERO)
        val baselines = checkpoints.map { level ->
            PlayerScalingSystem.baseStatsForLevel(BaseStats(), level)
        }
        baselines.zipWithNext().forEach { (before, after) ->
            check(after.attackPower > before.attackPower)
            check(after.maxHealth > before.maxHealth)
            check(after.armor > before.armor)
            check(after.actionSpeed > before.actionSpeed)
            check(after.criticalChance > before.criticalChance)
            check(after.criticalMultiplier > before.criticalMultiplier)
        }
        check(baselines.last().maxHealth == GameNumber.of(1_288L))

        val region = registry.region(DefaultGameContent.TRAINING_HOLLOW_REGION_ID)
        val slime = registry.enemy(DefaultGameContent.SLIME_ID)
        val assassin = registry.enemy(DefaultGameContent.RIFTFANG_ID)
        val slimeAttack = registry.enemyAttack(slime.attackDefinitionId!!)
        val assassinAttack = registry.enemyAttack(assassin.attackDefinitionId!!)
        val tiers = listOf(0L, 9L, 24L, 49L, 99L)
        val slimeHealth = tiers.map {
            EnemyScalingSystem.scaledHealth(slime, region, it)
        }
        slimeHealth.zipWithNext().forEach { (before, after) -> check(after > before) }
        check(
            EnemyScalingSystem.scaledAttack(assassin, assassinAttack, 99L) >
                EnemyScalingSystem.scaledAttack(slime, slimeAttack, 99L)
        )
        check(
            EnemyScalingSystem.scaledAttackInterval(assassin, assassinAttack, 99L).millis >=
                CombatMath.MINIMUM_ACTION_INTERVAL.millis
        )
        check(
            EnemyScalingSystem.scaledExperienceReward(assassin, 99L) >
                EnemyScalingSystem.scaledExperienceReward(assassin, 0L)
        )

        check(
            CombatMath.mitigate(
                damage = GameNumber.of(100L),
                armor = GameNumber.of(100L),
                armorPenetration = GameNumber.ZERO
            ) == GameNumber.of(50L)
        )
        check(CombatMath.isCritical(9_999L, Ratio.ofUnits(15_000L)))
        check(!CombatMath.isCritical(9_999L, Ratio.ZERO))
    }
}
