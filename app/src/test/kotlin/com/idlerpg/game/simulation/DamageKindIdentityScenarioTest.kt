package com.idlerpg.game.simulation

import com.idlerpg.game.application.GameRuntime
import com.idlerpg.game.core.id.ContentId
import com.idlerpg.game.core.number.GameNumber
import com.idlerpg.game.core.time.GameDuration
import com.idlerpg.game.data.content.DefaultGameContent
import com.idlerpg.game.data.content.HollowWardenContent
import com.idlerpg.game.data.content.TrainingHollowStrategyContent
import com.idlerpg.game.domain.command.QueueSkillCast
import com.idlerpg.game.domain.definition.DamageKind
import com.idlerpg.game.domain.definition.combat.EffectSpec
import com.idlerpg.game.domain.engine.EngineResult
import com.idlerpg.game.domain.event.ConvergenceTriggered
import com.idlerpg.game.domain.event.DamageDealt

/** Canonical affinity identity from authored content through emitted combat events. */
object DamageKindIdentityScenarioTest {
    fun run() {
        authoredKindsAndExceptionsAreExplicit()
        playerSkillsAndBurningEmitTheirKinds()
        enemyAttacksEmitTheirKinds()
        flashfireEmitsElementalDamage()

        val first = castBaseSkill(
            DefaultGameContent.ARCANE_PULSE_ID,
            DefaultGameContent.ARCANE_PULSE_FEATURE_ID,
            7_400L
        )
        val replay = castBaseSkill(
            DefaultGameContent.ARCANE_PULSE_ID,
            DefaultGameContent.ARCANE_PULSE_FEATURE_ID,
            7_400L
        )
        check(first == replay)
    }

    private fun authoredKindsAndExceptionsAreExplicit() {
        val registry = SimulationTestSupport.factory().contentRegistry
        val expectedSkills = mapOf(
            DefaultGameContent.FLAME_BRAND_ID to DamageKind.ELEMENTAL,
            DefaultGameContent.CINDER_MARK_ID to DamageKind.ELEMENTAL,
            DefaultGameContent.FROST_LANCE_ID to DamageKind.ELEMENTAL,
            DefaultGameContent.ARCANE_PULSE_ID to DamageKind.ARCANE,
            DefaultGameContent.UMBRAL_CUT_ID to DamageKind.SHADOW
        )
        expectedSkills.forEach { (skillId, expected) ->
            check(registry.skill(skillId).effects.filterIsInstance<EffectSpec.DealDamage>()
                .all { it.damageKind == expected })
        }
        check(registry.status(DefaultGameContent.BURNING_STATUS_ID).periodicEffects
            .filterIsInstance<EffectSpec.DealDamage>().single().damageKind == DamageKind.ELEMENTAL)

        val expectedAttacks = mapOf(
            DefaultGameContent.CINDER_WISP_ATTACK_ID to DamageKind.ELEMENTAL,
            DefaultGameContent.ARCANE_SEER_ATTACK_ID to DamageKind.ARCANE,
            TrainingHollowStrategyContent.FROSTBOUND_MITE_ATTACK_ID to DamageKind.ELEMENTAL
        )
        expectedAttacks.forEach { (attackId, expected) ->
            check(registry.enemyAttack(attackId).damageKind == expected)
        }
        check(registry.convergence(TrainingHollowStrategyContent.FLASHFIRE_ID).effects
            .filterIsInstance<EffectSpec.DealDamage>().single().damageKind == DamageKind.ELEMENTAL)

        // Mixed-affinity identity exceptions are authored, intentional, and regression-locked.
        check(registry.convergence(DefaultGameContent.FORGED_FLAME_ID).effects
            .filterIsInstance<EffectSpec.DealDamage>().single().damageKind == DamageKind.PHYSICAL)
        check(registry.convergence(TrainingHollowStrategyContent.OVERDRIVE_ID).effects
            .filterIsInstance<EffectSpec.DealDamage>().single().damageKind == DamageKind.PHYSICAL)
        check(registry.enemyAttack(TrainingHollowStrategyContent.ECHO_LEECH_ATTACK_ID).damageKind ==
            DamageKind.ARCANE)
        check(registry.enemyAttack(HollowWardenContent.ATTACK_ID).damageKind == DamageKind.ARCANE)
    }

    private fun playerSkillsAndBurningEmitTheirKinds() {
        val cases = listOf(
            Triple(DefaultGameContent.FLAME_BRAND_ID, DefaultGameContent.FLAME_BRAND_FEATURE_ID, DamageKind.ELEMENTAL),
            Triple(DefaultGameContent.CINDER_MARK_ID, DefaultGameContent.CINDER_MARK_FEATURE_ID, DamageKind.ELEMENTAL),
            Triple(DefaultGameContent.FROST_LANCE_ID, DefaultGameContent.FROST_LANCE_FEATURE_ID, DamageKind.ELEMENTAL),
            Triple(DefaultGameContent.ARCANE_PULSE_ID, DefaultGameContent.ARCANE_PULSE_FEATURE_ID, DamageKind.ARCANE),
            Triple(DefaultGameContent.UMBRAL_CUT_ID, DefaultGameContent.UMBRAL_CUT_FEATURE_ID, DamageKind.SHADOW)
        )
        cases.forEachIndexed { index, (skillId, featureId, expected) ->
            val result = castBaseSkill(skillId, featureId, 7_410L + index)
            val playerId = result.state.run.combat.playerCombatant?.instanceId
                ?: error("Missing player combatant")
            val direct = result.events.map { it.event }.filterIsInstance<DamageDealt>()
                .first { it.sourceInstanceId == playerId }
            check(direct.damageKindId == expected.id)
        }

        val burningRuntime = preparedRuntime(7_420L)
        unlockAndEquip(
            burningRuntime,
            listOf(DefaultGameContent.FLAME_BRAND_ID),
            setOf(DefaultGameContent.FLAME_BRAND_FEATURE_ID)
        )
        SimulationTestSupport.checkAccepted(
            burningRuntime.dispatch(QueueSkillCast(DefaultGameContent.FLAME_BRAND_ID))
        )
        burningRuntime.advance(GameDuration.ofSeconds(1L))
        val playerId = burningRuntime.state().run.combat.playerCombatant?.instanceId
            ?: error("Missing player combatant")
        val tick = burningRuntime.advance(GameDuration.ofSeconds(1L))
        check(tick.events.map { it.event }.filterIsInstance<DamageDealt>().any {
            it.sourceInstanceId == playerId && it.damageKindId == DamageKind.ELEMENTAL.id
        })
    }

    private fun enemyAttacksEmitTheirKinds() {
        val cases = listOf(
            Triple(DefaultGameContent.CINDER_WISP_ID, DefaultGameContent.CINDER_WISP_ATTACK_ID, DamageKind.ELEMENTAL),
            Triple(DefaultGameContent.ARCANE_SEER_ID, DefaultGameContent.ARCANE_SEER_ATTACK_ID, DamageKind.ARCANE),
            Triple(
                TrainingHollowStrategyContent.FROSTBOUND_MITE_ID,
                TrainingHollowStrategyContent.FROSTBOUND_MITE_ATTACK_ID,
                DamageKind.ELEMENTAL
            )
        )
        cases.forEachIndexed { index, (enemyId, attackId, expected) ->
            val runtime = preparedRuntime(7_430L + index)
            val state = runtime.state()
            val enemy = state.run.combat.enemies.single()
            val attack = SimulationTestSupport.factory().contentRegistry.enemyAttack(attackId)
            runtime.replaceLoadedState(state.copy(run = state.run.copy(
                combat = state.run.combat.copy(
                    enemies = listOf(enemy.copy(
                        definitionId = enemyId,
                        combatant = enemy.combatant.copy(currentHealth = GameNumber.of(10_000L))
                    )),
                    nextEnemyDecisionAt = mapOf(
                        enemy.instanceId to (state.engine.simulationTime + attack.interval)
                    )
                )
            )))
            val result = runtime.advance(attack.interval)
            val incoming = result.events.map { it.event }.filterIsInstance<DamageDealt>()
                .single { it.sourceInstanceId == enemy.instanceId }
            check(incoming.damageKindId == expected.id)
        }
    }

    private fun flashfireEmitsElementalDamage() {
        val runtime = preparedRuntime(7_440L)
        unlockAndEquip(
            runtime,
            listOf(
                DefaultGameContent.QUICK_SLASH_ID,
                DefaultGameContent.FLAME_BRAND_ID,
                DefaultGameContent.CINDER_MARK_ID
            ),
            setOf(
                DefaultGameContent.FLAME_BRAND_FEATURE_ID,
                DefaultGameContent.CINDER_MARK_FEATURE_ID
            )
        )
        listOf(
            DefaultGameContent.QUICK_SLASH_ID,
            DefaultGameContent.FLAME_BRAND_ID,
            DefaultGameContent.CINDER_MARK_ID
        ).forEach { skillId ->
            SimulationTestSupport.checkAccepted(runtime.dispatch(QueueSkillCast(skillId)))
            val result = runtime.advance(GameDuration.ofSeconds(1L))
            if (skillId == DefaultGameContent.CINDER_MARK_ID) {
                check(result.events.map { it.event }.filterIsInstance<ConvergenceTriggered>()
                    .single().convergenceId == TrainingHollowStrategyContent.FLASHFIRE_ID)
                val playerId = result.state.run.combat.playerCombatant?.instanceId
                    ?: error("Missing player combatant")
                check(result.events.map { it.event }.filterIsInstance<DamageDealt>()
                    .filter { it.sourceInstanceId == playerId }
                    .all { it.damageKindId == DamageKind.ELEMENTAL.id })
            }
        }
    }

    private fun castBaseSkill(skillId: ContentId, featureId: ContentId, seed: Long): EngineResult {
        val runtime = preparedRuntime(seed)
        unlockAndEquip(runtime, listOf(skillId), setOf(featureId))
        SimulationTestSupport.checkAccepted(runtime.dispatch(QueueSkillCast(skillId)))
        return runtime.advance(GameDuration.ofSeconds(1L))
    }

    private fun preparedRuntime(seed: Long): GameRuntime {
        val runtime = SimulationTestSupport.runtime(seed = seed)
        SimulationTestSupport.startTraining(runtime)
        val state = runtime.state()
        val enemy = state.run.combat.enemies.single()
        runtime.replaceLoadedState(state.copy(run = state.run.copy(
            combat = state.run.combat.copy(enemies = listOf(
                enemy.copy(combatant = enemy.combatant.copy(currentHealth = GameNumber.of(10_000L)))
            ))
        )))
        return runtime
    }

    private fun unlockAndEquip(
        runtime: GameRuntime,
        skillIds: List<ContentId>,
        featureIds: Set<ContentId>
    ) {
        val state = runtime.state()
        runtime.replaceLoadedState(state.copy(run = state.run.copy(
            player = state.run.player.copy(equippedSkillIds = skillIds),
            progression = state.run.progression.copy(
                featureUnlocks = state.run.progression.featureUnlocks.copy(
                    unlockedFeatureIds = state.run.progression.featureUnlocks.unlockedFeatureIds + featureIds
                )
            )
        )))
    }
}
