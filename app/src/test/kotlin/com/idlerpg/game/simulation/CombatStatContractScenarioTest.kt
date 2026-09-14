package com.idlerpg.game.simulation

import com.idlerpg.game.core.number.GameNumber
import com.idlerpg.game.core.number.Ratio
import com.idlerpg.game.core.time.GameDuration
import com.idlerpg.game.data.local.SaveData
import com.idlerpg.game.domain.definition.DamageKind
import com.idlerpg.game.domain.definition.combat.EffectSpec
import com.idlerpg.game.domain.engine.EngineContext
import com.idlerpg.game.domain.event.DamageDealt
import com.idlerpg.game.domain.system.combat.ActionResolutionSystem
import com.idlerpg.game.domain.system.combat.CombatMath
import com.idlerpg.game.domain.system.combat.HealingSystem

/** Paired executable outcomes for every combat-stat contract activated by this transaction. */
object CombatStatContractScenarioTest {
    fun run() {
        actionSpeedChangesDecisionCadence()
        criticalChanceAndMultiplierChangeDamage()
        effectPowerChangesPeriodicOutput()
        healingPowerChangesRecovery()
        armorUsesDiminishingMitigation()
        nonDefaultStatsRoundTrip()
    }

    private fun actionSpeedChangesDecisionCadence() {
        fun firstDeadline(speed: Ratio): Long {
            val runtime = SimulationTestSupport.runtime(seed = 1_201L)
            val initial = runtime.state()
            runtime.replaceLoadedState(initial.copy(
                run = initial.run.copy(player = initial.run.player.copy(
                    baseStats = initial.run.player.baseStats.copy(actionSpeed = speed)
                ))
            ))
            SimulationTestSupport.startTraining(runtime)
            return runtime.state().run.combat.nextPlayerDecisionAt?.millis
                ?: error("Missing player deadline")
        }
        check(firstDeadline(Ratio.ONE) == 1_000L)
        check(firstDeadline(Ratio.ofUnits(20_000L)) == 500L)
        check(CombatMath.actionInterval(GameDuration.ofSeconds(1L), Ratio.ofUnits(100_000L)).millis == 250L)
    }

    private fun criticalChanceAndMultiplierChangeDamage() {
        fun hit(chance: Ratio, multiplier: Ratio): DamageDealt {
            val runtime = SimulationTestSupport.runtime(seed = 1_202L)
            val initial = runtime.state()
            runtime.replaceLoadedState(initial.copy(
                run = initial.run.copy(player = initial.run.player.copy(
                    baseStats = initial.run.player.baseStats.copy(
                        criticalChance = chance,
                        criticalMultiplier = multiplier
                    )
                ))
            ))
            SimulationTestSupport.startTraining(runtime)
            val enemyId = runtime.state().run.combat.enemies.single().instanceId
            return runtime.advance(GameDuration.ofSeconds(1L)).events.map { it.event }
                .filterIsInstance<DamageDealt>().single { it.targetInstanceId == enemyId }
        }
        val baseline = hit(Ratio.ZERO, Ratio.ONE)
        val critical = hit(Ratio.ONE, Ratio.ofUnits(20_000L))
        check(!baseline.critical && baseline.amount == GameNumber.of(10L))
        check(critical.critical && critical.amount == GameNumber.of(20L))
    }

    private fun effectPowerChangesPeriodicOutput() {
        fun damage(power: Ratio): GameNumber {
            val runtime = SimulationTestSupport.runtime(seed = 1_203L)
            SimulationTestSupport.startTraining(runtime)
            val started = runtime.state()
            val state = started.copy(run = started.run.copy(player = started.run.player.copy(
                baseStats = started.run.player.baseStats.copy(effectPower = power)
            )))
            val playerId = state.run.combat.playerCombatant?.instanceId ?: error("Missing player")
            val enemyId = state.run.combat.enemies.single().instanceId
            val effect = EffectSpec.DealDamage(
                powerRatio = Ratio.HALF,
                damageKind = DamageKind.PHYSICAL,
                scalingPolicy = EffectSpec.DamageScalingPolicy.ATTACK_AND_EFFECT_POWER,
                canCritical = false
            )
            val result = ActionResolutionSystem.resolvePrimitiveEffects(
                state, playerId, enemyId, listOf(effect),
                EngineContext(contentRegistry = SimulationTestSupport.factory().contentRegistry)
            )
            return result.events.filterIsInstance<DamageDealt>().single().amount
        }
        check(damage(Ratio.ONE) == GameNumber.of(5L))
        check(damage(Ratio.ofUnits(20_000L)) == GameNumber.of(10L))
    }

    private fun healingPowerChangesRecovery() {
        fun healed(power: Ratio): GameNumber {
            val runtime = SimulationTestSupport.runtime(seed = 1_204L)
            SimulationTestSupport.startTraining(runtime)
            val started = runtime.state()
            val player = started.run.combat.playerCombatant ?: error("Missing player")
            val state = started.copy(run = started.run.copy(
                player = started.run.player.copy(
                    currentHealth = GameNumber.of(40L),
                    baseStats = started.run.player.baseStats.copy(healingPower = power)
                ),
                combat = started.run.combat.copy(
                    playerCombatant = player.copy(currentHealth = GameNumber.of(40L))
                )
            ))
            return HealingSystem.healPlayer(
                state, player.instanceId, player.instanceId, GameNumber.of(25L),
                SimulationTestSupport.factory().contentRegistry
            ).event.amount
        }
        check(healed(Ratio.ONE) == GameNumber.of(25L))
        check(healed(Ratio.ofUnits(20_000L)) == GameNumber.of(50L))
    }

    private fun armorUsesDiminishingMitigation() {
        val baseline = CombatMath.mitigate(GameNumber.of(100L), GameNumber.ZERO, GameNumber.ZERO)
        val armored = CombatMath.mitigate(GameNumber.of(100L), GameNumber.of(100L), GameNumber.ZERO)
        val penetrated = CombatMath.mitigate(GameNumber.of(100L), GameNumber.of(100L), GameNumber.of(50L))
        check(baseline == GameNumber.of(100L))
        check(armored == GameNumber.of(50L))
        check(penetrated == GameNumber.of(66L))
    }

    private fun nonDefaultStatsRoundTrip() {
        val runtime = SimulationTestSupport.runtime(seed = 1_205L)
        val initial = runtime.state()
        val stats = initial.run.player.baseStats.copy(
            actionSpeed = Ratio.ofUnits(12_500L),
            criticalChance = Ratio.ofUnits(2_500L),
            criticalMultiplier = Ratio.ofUnits(17_500L),
            effectPower = Ratio.ofUnits(13_000L),
            healingPower = Ratio.ofUnits(14_000L)
        )
        val restored = SaveData.fromGameState(initial.copy(
            run = initial.run.copy(player = initial.run.player.copy(baseStats = stats))
        )).toGameState()
        check(restored.run.player.baseStats == stats)
    }
}
