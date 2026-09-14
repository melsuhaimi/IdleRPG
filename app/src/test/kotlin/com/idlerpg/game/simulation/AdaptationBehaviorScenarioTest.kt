package com.idlerpg.game.simulation

import com.idlerpg.game.application.GameSessionFactory
import com.idlerpg.game.core.id.ContentId
import com.idlerpg.game.core.id.InstanceId
import com.idlerpg.game.core.number.GameNumber
import com.idlerpg.game.core.number.Ratio
import com.idlerpg.game.core.time.GameDuration
import com.idlerpg.game.core.time.GameTime
import com.idlerpg.game.data.content.DefaultGameContent
import com.idlerpg.game.data.content.TrainingHollowAdaptationContent
import com.idlerpg.game.data.content.TrainingHollowStrategyContent
import com.idlerpg.game.domain.definition.Affinity
import com.idlerpg.game.domain.definition.adaptation.MutationEffectDefinition
import com.idlerpg.game.domain.definition.combat.EffectSpec
import com.idlerpg.game.domain.engine.ScheduledAction
import com.idlerpg.game.domain.engine.ScheduledActionExecution
import com.idlerpg.game.domain.engine.ScheduledActionType
import com.idlerpg.game.domain.event.DamageDealt
import com.idlerpg.game.domain.event.HealingApplied
import com.idlerpg.game.domain.event.ResonanceConsumed
import com.idlerpg.game.domain.model.GameState
import com.idlerpg.game.domain.model.adaptation.ActiveMutationState
import com.idlerpg.game.domain.model.combat.EnemyState
import com.idlerpg.game.domain.model.combat.StatusEffectState
import com.idlerpg.game.domain.model.resonance.ResonanceState
import com.idlerpg.game.domain.system.adaptation.MutationSystem
import com.idlerpg.game.domain.system.combat.ActionResolutionSystem
import com.idlerpg.game.domain.system.combat.CombatSystem
import com.idlerpg.game.domain.system.combat.HealingSystem

/** Gate 8 behavioral mutations remain bounded, observable, and deterministic. */
object AdaptationBehaviorScenarioTest {
    fun run() {
        val factory = SimulationTestSupport.factory()
        val runtime = SimulationTestSupport.runtime(9_201L)
        SimulationTestSupport.startTraining(runtime)
        val started = runtime.state()
        authoredVocabulary(factory)
        healingPressure(started, factory)
        cadenceAndResonancePressure(started, factory)
        guardPressureIsOwnerSpecific(started, factory)
        deterministicReplay(started, factory)
    }

    private fun authoredVocabulary(factory: GameSessionFactory) {
        val mutations = factory.contentRegistry.allMutations()
        check(mutations.size == 8)
        check(mutations.map { it.id.value to it.triggerAffinity } == listOf(
            "mutation.ash_skin" to Affinity.EMBER,
            "mutation.blightblood" to Affinity.VITALITY,
            "mutation.gloom_ward" to Affinity.SHADOW,
            "mutation.icebound_core" to Affinity.FROST,
            "mutation.null_veil" to Affinity.ARCANE,
            "mutation.reflex_carapace" to Affinity.TEMPO,
            "mutation.siege_hunger" to Affinity.GUARD,
            "mutation.stonehide" to Affinity.MIGHT
        ))
        check(mutations.map { it.triggerAffinity }.toSet() == Affinity.values().toSet())
        check(mutations.all { it.minimumAdaptationTier == 1 && it.selectionWeight == 100L })
        check(mutations.all {
            it.effect is MutationEffectDefinition.DamageTakenMultiplierForAffinity
        })
        check(mutations.all { mutation ->
            val multiplier = (mutation.effect as MutationEffectDefinition.DamageTakenMultiplierForAffinity).multiplier
            multiplier == if (mutation.triggerAffinity == Affinity.TEMPO) {
                Ratio.ofUnits(7_800L)
            } else {
                Ratio.ofUnits(7_500L)
            }
        })
        val reward = factory.contentRegistry.adaptationReward(
            DefaultGameContent.TRAINING_HOLLOW_ADAPTATION_REWARD_ID
        )
        check(reward.bonusPerActiveMutation == Ratio.HALF)
        check(reward.maximumMultiplier == Ratio.ofUnits(30_000L))
        check(runCatching {
            MutationEffectDefinition.PlayerHealingMultiplier(Ratio.ZERO)
        }.isFailure)
        check(runCatching {
            MutationEffectDefinition.EnemyActionIntervalMultiplier(Ratio.ZERO)
        }.isFailure)
        check(runCatching {
            MutationEffectDefinition.AdditionalResonanceDrain(GameNumber.ZERO)
        }.isFailure)
        check(runCatching {
            MutationEffectDefinition.EnemyDamageMultiplierWhilePlayerHasAffinityStatus(
                Affinity.GUARD,
                Ratio.ofUnits(9_999L)
            )
        }.isFailure)
        check(runCatching {
            MutationEffectDefinition.EnemyDamageMultiplierWhilePlayerHasAffinityStatus(
                Affinity.GUARD,
                Ratio.ofUnits(20_001L)
            )
        }.isFailure)
    }

    private fun healingPressure(started: GameState, factory: GameSessionFactory) {
        val player = checkNotNull(started.run.combat.playerCombatant)
        val hurt = started.copy(run = started.run.copy(
            player = started.run.player.copy(currentHealth = GameNumber.of(20L)),
            combat = started.run.combat.copy(
                playerCombatant = player.copy(currentHealth = GameNumber.of(20L)),
                enemies = listOf(withMutation(started.run.combat.enemies.single(),
                    TrainingHollowAdaptationContent.BLIGHTBLOOD_ID, Affinity.VITALITY))
            )
        ))
        fun heal(state: GameState): GameNumber = HealingSystem.healPlayer(
            state, player.instanceId, player.instanceId, GameNumber.of(50L), factory.contentRegistry
        ).event.amount
        check(heal(hurt) == GameNumber.of(35L))

        val carrier = hurt.run.combat.enemies.single()
        val duplicateId = InstanceId(carrier.instanceId.value + 10_000L)
        val duplicate = carrier.copy(
            instanceId = duplicateId,
            combatant = carrier.combatant.copy(instanceId = duplicateId)
        )
        check(heal(hurt.copy(run = hurt.run.copy(combat = hurt.run.combat.copy(
            enemies = listOf(carrier, duplicate)
        )))) == GameNumber.of(35L))
        check(heal(hurt.copy(run = hurt.run.copy(combat = hurt.run.combat.copy(
            enemies = listOf(carrier.copy(combatant = carrier.combatant.copy(currentHealth = GameNumber.ZERO)))
        )))) == GameNumber.of(50L))

        val context = factory.createEngineContext()
        val periodic = ActionResolutionSystem.resolvePrimitiveEffects(
            hurt, player.instanceId, player.instanceId,
            factory.contentRegistry.status(DefaultGameContent.VITAL_REGENERATION_STATUS_ID).periodicEffects,
            listOf(Affinity.VITALITY), context
        )
        check(periodic.events.filterIsInstance<HealingApplied>().single().amount == GameNumber.of(5L))
        val enemyId = hurt.run.combat.enemies.single().instanceId
        val convergence = ActionResolutionSystem.resolvePrimitiveEffects(
            hurt, player.instanceId, enemyId,
            factory.contentRegistry.convergence(TrainingHollowStrategyContent.BASTION_PULSE_ID)
                .effects.filterIsInstance<EffectSpec.Heal>(),
            listOf(Affinity.GUARD, Affinity.VITALITY), factory.createEngineContext()
        )
        check(convergence.events.filterIsInstance<HealingApplied>().single().amount == GameNumber.of(24L))
    }

    private fun cadenceAndResonancePressure(started: GameState, factory: GameSessionFactory) {
        val original = started.run.combat.enemies.single()
        val reflex = withMutation(
            original,
            TrainingHollowAdaptationContent.REFLEX_CARAPACE_ID,
            Affinity.TEMPO
        )
        check(MutationSystem.enemyActionInterval(
            GameDuration.ofMillis(2_000L), reflex, factory.contentRegistry
        ) == GameDuration.ofMillis(1_700L))
        val reflexAction = executeEnemy(started, reflex, DefaultGameContent.SLIME_ATTACK_ID, factory)
        val nextAt = checkNotNull(reflexAction.state.run.combat.nextEnemyDecisionAt[reflex.instanceId])
        check(nextAt.millis - started.engine.simulationTime.millis == 1_870L)

        val nullVeil = withMutation(
            original,
            TrainingHollowAdaptationContent.NULL_VEIL_ID,
            Affinity.ARCANE
        )
        val charged = started.copy(run = started.run.copy(
            resonance = ResonanceState(mapOf(Affinity.EMBER.id to GameNumber.of(3L)))
        ))
        val drained = executeEnemy(charged, nullVeil, DefaultGameContent.SLIME_ATTACK_ID, factory)
        check(drained.events.filterIsInstance<ResonanceConsumed>().single().amount == GameNumber.ONE)
        check(drained.state.run.resonance.chargeByAffinityId[Affinity.EMBER.id] == GameNumber.of(2L))
    }

    private fun guardPressureIsOwnerSpecific(started: GameState, factory: GameSessionFactory) {
        val player = checkNotNull(started.run.combat.playerCombatant)
        val ward = StatusEffectState(
            instanceId = InstanceId(90_001L),
            definitionId = TrainingHollowStrategyContent.GLACIAL_WARD_STATUS_ID,
            sourceInstanceId = player.instanceId,
            appliedAt = GameTime.ZERO,
            expiresAt = GameTime.ZERO + GameDuration.ofSeconds(5L)
        )
        val guarded = started.copy(run = started.run.copy(combat = started.run.combat.copy(
            playerCombatant = player.copy(statusEffects = listOf(ward))
        )))
        val base = guarded.run.combat.enemies.single().copy(
            definitionId = DefaultGameContent.HOLLOW_BULWARK_ID
        )
        val siege = withMutation(
            base,
            TrainingHollowAdaptationContent.SIEGE_HUNGER_ID,
            Affinity.GUARD
        )
        fun damage(state: GameState, actor: EnemyState): GameNumber = executeEnemy(
            state, actor, DefaultGameContent.HOLLOW_BULWARK_ATTACK_ID, factory
        ).events.filterIsInstance<DamageDealt>().first { it.sourceInstanceId == actor.instanceId }.amount

        val ordinaryGuarded = damage(guarded, base)
        val siegeGuarded = damage(guarded, siege)
        val siegeUnguarded = damage(started, siege)
        check(siegeGuarded > ordinaryGuarded)
        check(siegeGuarded < siegeUnguarded)

        val otherId = InstanceId(siege.instanceId.value + 20_000L)
        val otherSiege = siege.copy(
            instanceId = otherId,
            combatant = siege.combatant.copy(instanceId = otherId)
        )
        val mixed = guarded.copy(run = guarded.run.copy(combat = guarded.run.combat.copy(
            enemies = listOf(base, otherSiege)
        )))
        check(damage(mixed, base) == ordinaryGuarded)
    }

    private fun deterministicReplay(started: GameState, factory: GameSessionFactory) {
        fun scenario(): ScheduledActionExecution {
            val enemy = withMutation(
                started.run.combat.enemies.single(),
                TrainingHollowAdaptationContent.NULL_VEIL_ID,
                Affinity.ARCANE
            )
            val state = started.copy(run = started.run.copy(
                resonance = ResonanceState(mapOf(Affinity.FROST.id to GameNumber.of(2L)))
            ))
            return executeEnemy(state, enemy, DefaultGameContent.SLIME_ATTACK_ID, factory)
        }
        check(scenario() == scenario())
    }

    private fun withMutation(enemy: EnemyState, id: ContentId, affinity: Affinity): EnemyState =
        enemy.copy(activeMutations = listOf(ActiveMutationState(id, affinity.id, 1)))

    private fun executeEnemy(
        state: GameState,
        enemy: EnemyState,
        attackId: ContentId,
        factory: GameSessionFactory
    ): ScheduledActionExecution {
        val otherEnemies = state.run.combat.enemies.filterNot { it.instanceId == enemy.instanceId }
        val prepared = state.copy(run = state.run.copy(combat = state.run.combat.copy(
            enemies = (otherEnemies + enemy).sortedBy { it.instanceId }
        )))
        return CombatSystem.execute(
            prepared,
            ScheduledAction(
                dueAt = prepared.engine.simulationTime,
                type = ScheduledActionType.ENEMY_DECISION,
                stableTieBreakKey = enemy.instanceId.value,
                ownerInstanceId = enemy.instanceId,
                sourceContentId = attackId
            ),
            factory.createEngineContext()
        )
    }
}
