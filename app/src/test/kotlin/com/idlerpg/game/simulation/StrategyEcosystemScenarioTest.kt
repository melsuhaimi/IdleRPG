package com.idlerpg.game.simulation

import com.idlerpg.game.application.GameSessionFactory
import com.idlerpg.game.core.id.ContentId
import com.idlerpg.game.core.id.InstanceId
import com.idlerpg.game.core.number.GameNumber
import com.idlerpg.game.core.number.Ratio
import com.idlerpg.game.core.time.GameDuration
import com.idlerpg.game.data.content.DefaultGameContent
import com.idlerpg.game.data.content.TrainingHollowStrategyContent
import com.idlerpg.game.data.content.TrainingHollowWorldContent
import com.idlerpg.game.domain.definition.Affinity
import com.idlerpg.game.domain.definition.combat.EffectSpec
import com.idlerpg.game.domain.definition.enemy.EnemyRole
import com.idlerpg.game.domain.definition.world.EliteModifier
import com.idlerpg.game.domain.engine.ScheduledAction
import com.idlerpg.game.domain.engine.ScheduledActionExecution
import com.idlerpg.game.domain.engine.ScheduledActionType
import com.idlerpg.game.domain.event.ConvergenceTriggered
import com.idlerpg.game.domain.event.DamageDealt
import com.idlerpg.game.domain.event.HealingApplied
import com.idlerpg.game.domain.event.ResonanceConsumed
import com.idlerpg.game.domain.event.StatusApplied
import com.idlerpg.game.domain.model.GameState
import com.idlerpg.game.domain.model.resonance.ResonanceSequenceState
import com.idlerpg.game.domain.system.combat.CombatSystem
import com.idlerpg.game.domain.system.combat.DamageSystem
import com.idlerpg.game.domain.system.combat.StatusEffectSystem
import com.idlerpg.game.domain.system.combat.TargetingSystem
import com.idlerpg.game.domain.system.enemy.EnemyEliteSystem
import com.idlerpg.game.domain.system.resonance.ConvergenceSystem
import com.idlerpg.game.domain.system.stats.DerivedStatSystem

/** Deterministic Gate 3–4 acceptance scenarios for combos, roles, abilities, and elites. */
object StrategyEcosystemScenarioTest {
    fun run() {
        val factory = SimulationTestSupport.factory()
        val registry = factory.contentRegistry
        check(registry.allSkills().size == 15)
        check(registry.allEnemies().size == 9)
        check(registry.allConvergences().size == 8)
        check(registry.allEncounters().size == TrainingHollowWorldContent.MAX_STAGE)
        check(registry.enemy(DefaultGameContent.HOLLOW_BULWARK_ID).role == EnemyRole.PROTECTOR)
        check(registry.enemy(TrainingHollowStrategyContent.ECHO_LEECH_ID).role == EnemyRole.PARASITE)
        check(registry.skill(TrainingHollowStrategyContent.VOID_LANCE_ID).requiredFeatureId == TrainingHollowStrategyContent.VOID_LANCE_FEATURE_ID)
        check(registry.skill(TrainingHollowStrategyContent.IRON_VOW_ID).targetingRule == com.idlerpg.game.domain.definition.combat.SkillTargetingRule.SELF)
        check(registry.skill(TrainingHollowStrategyContent.STARFALL_ID).effects.any { effect -> effect is EffectSpec.DealDamage && effect.targetPattern == EffectSpec.TargetPattern.ALL_ENEMIES })

        val runtime = SimulationTestSupport.runtime(seed = 3_401L)
        SimulationTestSupport.startTraining(runtime)
        val started = runtime.state()
        formationAndAdaptiveRules(started, factory)
        convergenceEffectsAreCanonical(started, factory)
        scheduledEnemyAbilitiesAreCanonical(started, factory)
        eliteVocabularyIsObservable(started, factory)
    }

    private fun formationAndAdaptiveRules(started: GameState, factory: GameSessionFactory) {
        val registry = factory.contentRegistry
        val original = started.run.combat.enemies.single()
        val bulwark = original.copy(
            definitionId = DefaultGameContent.HOLLOW_BULWARK_ID,
            combatant = original.combatant.copy(currentHealth = GameNumber.of(180L))
        )
        val miteId = InstanceId(original.instanceId.value + 10_000L)
        val mite = original.copy(
            instanceId = miteId,
            definitionId = TrainingHollowStrategyContent.FROSTBOUND_MITE_ID,
            combatant = original.combatant.copy(instanceId = miteId, currentHealth = GameNumber.of(100L))
        )
        val originalEnemyDecisionAt =
            started.run.combat.nextEnemyDecisionAt[original.instanceId]
                ?: started.engine.simulationTime
        val protectedState = started.copy(
            run = started.run.copy(
                combat = started.run.combat.copy(
                    enemies = listOf(bulwark, mite),
                    nextEnemyDecisionAt = started.run.combat.nextEnemyDecisionAt +
                        (miteId to originalEnemyDecisionAt)
                )
            )
        )
        check(TargetingSystem.firstWithRole(protectedState.run.combat, registry, setOf(EnemyRole.PROTECTOR))?.instanceId == bulwark.instanceId)
        val playerId = protectedState.run.combat.playerCombatant?.instanceId ?: error("Missing player")
        val plainHit = EffectSpec.DealDamage(Ratio.ONE, canCritical = false)
        val protectedDamage = DamageSystem.dealToEnemy(
            protectedState, playerId, mite, GameNumber.of(20L), plainHit,
            setOf(Affinity.MIGHT), factory.createEngineContext()
        ).event.amount
        check(protectedDamage == GameNumber.of(13L))

        val mimic = mite.copy(
            definitionId = TrainingHollowStrategyContent.SHADE_MIMIC_ID,
            combatant = mite.combatant.copy(currentHealth = GameNumber.of(100L))
        )
        val adaptedState = started.copy(
            run = started.run.copy(
                combat = started.run.combat.copy(
                    enemies = listOf(mimic),
                    nextEnemyDecisionAt = mapOf(miteId to originalEnemyDecisionAt)
                ),
                resonance = started.run.resonance.copy(
                    sequence = ResonanceSequenceState(listOf(Affinity.EMBER.id))
                )
            )
        )
        val mirroredDamage = DamageSystem.dealToEnemy(
            adaptedState, playerId, mimic, GameNumber.of(20L), plainHit,
            setOf(Affinity.EMBER), factory.createEngineContext()
        ).event.amount
        check(mirroredDamage == GameNumber.of(14L))

        val chilledContext = factory.createEngineContext().also {
            it.beginExecution(started.engine)
        }
        val chilled = StatusEffectSystem.apply(
            started, original.instanceId, playerId,
            registry.status(DefaultGameContent.CHILL_STATUS_ID), chilledContext
        ).state
        check(DerivedStatSystem.actionInterval(chilled, GameDuration.ofMillis(1_000L), registry) == GameDuration.ofMillis(1_250L))
    }

    private fun convergenceEffectsAreCanonical(started: GameState, factory: GameSessionFactory) {
        val playerId = started.run.combat.playerCombatant?.instanceId ?: error("Missing player")
        val enemyId = started.run.combat.enemies.single().instanceId
        val flashfire = ConvergenceSystem.resolveFirstEligible(
            started.withResonance(
                mapOf(Affinity.TEMPO.id to GameNumber.ONE, Affinity.EMBER.id to GameNumber.of(2L)),
                listOf(Affinity.TEMPO.id, Affinity.EMBER.id, Affinity.EMBER.id)
            ),
            DefaultGameContent.FLAME_BRAND_ID, playerId, enemyId, factory.createEngineContext()
        )
        check(flashfire.triggeredConvergenceId == TrainingHollowStrategyContent.FLASHFIRE_ID)
        check(flashfire.events.filterIsInstance<ConvergenceTriggered>().size == 1)
        check(flashfire.events.filterIsInstance<DamageDealt>().isNotEmpty())
        check(TrainingHollowStrategyContent.FLASHFIRE_ID in flashfire.state.meta.discoveries.discoveredConvergenceIds)

        val refrain = ConvergenceSystem.resolveFirstEligible(
            started.withResonance(
                mapOf(Affinity.ARCANE.id to GameNumber.of(2L), Affinity.TEMPO.id to GameNumber.of(2L)),
                listOf(Affinity.ARCANE.id, Affinity.ARCANE.id, Affinity.TEMPO.id)
            ),
            TrainingHollowStrategyContent.RESONANCE_SHIFT_ID, playerId, enemyId,
            factory.createEngineContext()
        )
        check(refrain.triggeredConvergenceId == TrainingHollowStrategyContent.ARCANE_REFRAIN_ID)
        check(refrain.events.none { it is DamageDealt })
        check(refrain.state.run.resonance.chargeByAffinityId[Affinity.TEMPO.id] == GameNumber.ONE)
        check(refrain.state.run.resonance.chargeByAffinityId[Affinity.ARCANE.id] == GameNumber.of(3L))
    }

    private fun scheduledEnemyAbilitiesAreCanonical(started: GameState, factory: GameSessionFactory) {
        val playerId = started.run.combat.playerCombatant?.instanceId ?: error("Missing player")
        val mite = executeEnemy(started, TrainingHollowStrategyContent.FROSTBOUND_MITE_ID, TrainingHollowStrategyContent.FROSTBOUND_MITE_ATTACK_ID, factory)
        check(mite.events.filterIsInstance<StatusApplied>().any {
            it.targetInstanceId == playerId && it.statusDefinitionId == DefaultGameContent.CHILL_STATUS_ID
        })

        val charged = started.withResonance(mapOf(Affinity.EMBER.id to GameNumber.of(2L)), emptyList())
        val leech = executeEnemy(charged, TrainingHollowStrategyContent.ECHO_LEECH_ID, TrainingHollowStrategyContent.ECHO_LEECH_ATTACK_ID, factory)
        check(leech.events.filterIsInstance<ResonanceConsumed>().single().amount == GameNumber.ONE)
        check(leech.state.run.resonance.chargeByAffinityId[Affinity.EMBER.id] == GameNumber.ONE)

        val wisp = executeEnemy(started, DefaultGameContent.CINDER_WISP_ID, DefaultGameContent.CINDER_WISP_ATTACK_ID, factory)
        check(wisp.events.filterIsInstance<StatusApplied>().any {
            it.statusDefinitionId == TrainingHollowStrategyContent.SCORCHED_STATUS_ID
        })
        val seer = executeEnemy(charged, DefaultGameContent.ARCANE_SEER_ID, DefaultGameContent.ARCANE_SEER_ATTACK_ID, factory)
        check(seer.events.filterIsInstance<ResonanceConsumed>().single().amount == GameNumber.ONE)
    }

    private fun eliteVocabularyIsObservable(started: GameState, factory: GameSessionFactory) {
        val original = started.run.combat.enemies.single()
        val allTraits = EliteModifier.values().map { it.id }.toSet()
        val elite = original.copy(
            activeTraitIds = allTraits,
            combatant = original.combatant.copy(currentHealth = GameNumber.of(50L))
        )
        check(EnemyEliteSystem.spawnHealth(GameNumber.of(100L), allTraits) == GameNumber.of(125L))
        check(EnemyEliteSystem.attackInterval(GameDuration.ofMillis(2_000L), elite) == GameDuration.ofMillis(1_500L))
        check(EnemyEliteSystem.attackDamage(GameNumber.of(8L), elite) == GameNumber.of(10L))
        check(EnemyEliteSystem.regenerationAmount(GameNumber.of(100L), elite) == GameNumber.of(5L))
        val prepared = started.copy(
            run = started.run.copy(combat = started.run.combat.copy(enemies = listOf(elite)))
        )
        val attackId = factory.contentRegistry.enemy(elite.definitionId).attackDefinitionId ?: error("Missing attack")
        val acted = executeEnemy(prepared, elite.definitionId, attackId, factory, allTraits)
        check(acted.events.filterIsInstance<HealingApplied>().single().amount > GameNumber.ZERO)
    }

    private fun executeEnemy(
        state: GameState,
        enemyId: ContentId,
        attackId: ContentId,
        factory: GameSessionFactory,
        traits: Set<ContentId> = emptySet()
    ): ScheduledActionExecution {
        val original = state.run.combat.enemies.single()
        val enemy = original.copy(definitionId = enemyId, activeTraitIds = traits)
        val prepared = state.copy(
            run = state.run.copy(combat = state.run.combat.copy(enemies = listOf(enemy)))
        )
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

    private fun GameState.withResonance(
        charges: Map<ContentId, GameNumber>,
        sequence: List<ContentId>
    ): GameState = copy(
        run = run.copy(
            resonance = run.resonance.copy(
                chargeByAffinityId = charges,
                sequence = ResonanceSequenceState(sequence)
            )
        )
    )
}
