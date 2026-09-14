package com.idlerpg.game.simulation

import com.idlerpg.game.application.GameRuntime
import com.idlerpg.game.application.OfflineSessionCoordinator
import com.idlerpg.game.core.id.InstanceId
import com.idlerpg.game.core.number.GameNumber
import com.idlerpg.game.core.number.Ratio
import com.idlerpg.game.core.time.GameDuration
import com.idlerpg.game.data.content.DefaultGameContent
import com.idlerpg.game.data.local.SaveEnvelope
import com.idlerpg.game.domain.command.QueueSkillCast
import com.idlerpg.game.domain.definition.combat.EffectSpec
import com.idlerpg.game.domain.event.DamageDealt
import com.idlerpg.game.domain.event.HealingApplied
import com.idlerpg.game.domain.event.SkillUsed
import com.idlerpg.game.domain.engine.ScheduledAction
import com.idlerpg.game.domain.engine.ScheduledActionType
import com.idlerpg.game.domain.model.combat.StatusEffectState
import com.idlerpg.game.domain.system.combat.ActionResolutionSystem
import com.idlerpg.game.domain.system.combat.DamageSystem
import com.idlerpg.game.domain.system.combat.HealingSystem
import com.idlerpg.game.domain.system.combat.StatusEffectSystem

/** Exact authored identities and deterministic cross-skill mechanics for Gate 2 slice one. */
object SkillBuildcraftCoreScenarioTest {
    fun run() {
        val factory = SimulationTestSupport.factory()
        val registry = factory.contentRegistry
        check(registry.allSkills().size == 12)

        val quick = registry.skill(DefaultGameContent.QUICK_SLASH_ID)
        check(quick.effects.filterIsInstance<EffectSpec.DealDamage>().single().hitCount == 3)
        val cinder = registry.skill(DefaultGameContent.CINDER_MARK_ID)
        check(cinder.effects.filterIsInstance<EffectSpec.ApplyStatus>().size == 2)
        check(registry.status(DefaultGameContent.BURNING_STATUS_ID).maximumStacks == 3)

        val runtime = SimulationTestSupport.runtime(seed = 2_001L)
        SimulationTestSupport.startTraining(runtime)
        val started = runtime.state()
        val original = started.run.combat.enemies.single()
        val secondId = InstanceId(original.instanceId.value + 50_000L)
        val second = original.copy(
            instanceId = secondId,
            combatant = original.combatant.copy(
                instanceId = secondId,
                currentHealth = GameNumber.of(20L)
            )
        )
        val player = started.run.combat.playerCombatant ?: error("Missing player")
        val playerId = player.instanceId
        val multi = started.copy(
            run = started.run.copy(
                player = started.run.player.copy(
                    baseStats = started.run.player.baseStats.copy(
                        attackPower = GameNumber.of(20L),
                        healingPower = Ratio.ofUnits(15_000L)
                    ),
                    currentHealth = GameNumber.of(40L)
                ),
                combat = started.run.combat.copy(
                    playerCombatant = player.copy(currentHealth = GameNumber.of(40L)),
                    enemies = listOf(original, second)
                )
            )
        )
        val arcane = registry.skill(DefaultGameContent.ARCANE_PULSE_ID)
        val pulse = ActionResolutionSystem.resolvePrimitiveEffects(
            state = multi,
            actorInstanceId = playerId,
            targetInstanceId = original.instanceId,
            effects = arcane.effects,
            context = factory.createEngineContext()
        )
        val pulseHits = pulse.events.filterIsInstance<DamageDealt>()
        check(pulseHits.size == 2)
        check(pulseHits.map { it.targetInstanceId } == listOf(original.instanceId, secondId))

        val quickHits = ActionResolutionSystem.resolvePrimitiveEffects(
            state = multi,
            actorInstanceId = playerId,
            targetInstanceId = original.instanceId,
            effects = quick.effects,
            context = factory.createEngineContext()
        ).events.filterIsInstance<DamageDealt>()
        check(quickHits.size == 3)
        check(quickHits.all { !it.critical })

        val chill = StatusEffectState(
            instanceId = InstanceId(90_001L),
            definitionId = DefaultGameContent.CHILL_STATUS_ID,
            sourceInstanceId = playerId,
            stackCount = 1,
            appliedAt = started.engine.simulationTime,
            expiresAt = started.engine.simulationTime + com.idlerpg.game.core.time.GameDuration.ofSeconds(4L)
        )
        val chilled = original.copy(combatant = original.combatant.copy(statusEffects = listOf(chill)))
        val pulseEffect = arcane.effects.filterIsInstance<EffectSpec.DealDamage>().single()
        val normalDamage = DamageSystem.dealToEnemy(
            multi, playerId, original, GameNumber.of(20L), pulseEffect, setOf(), factory.createEngineContext()
        ).event.amount
        val chilledDamage = DamageSystem.dealToEnemy(
            multi, playerId, chilled, GameNumber.of(20L), pulseEffect, setOf(), factory.createEngineContext()
        ).event.amount
        check(normalDamage == GameNumber.of(11L))
        check(chilledDamage == GameNumber.of(21L))

        val stagger = chill.copy(
            instanceId = InstanceId(90_002L),
            definitionId = DefaultGameContent.STAGGER_STATUS_ID
        )
        val staggered = original.copy(combatant = original.combatant.copy(statusEffects = listOf(stagger)))
        val heavyEffect = registry.skill(DefaultGameContent.HEAVY_STRIKE_ID)
            .effects.filterIsInstance<EffectSpec.DealDamage>().single()
        val heavyNormal = DamageSystem.dealToEnemy(
            multi, playerId, original, GameNumber.of(20L), heavyEffect, setOf(), factory.createEngineContext()
        ).event.amount
        val heavyStaggered = DamageSystem.dealToEnemy(
            multi, playerId, staggered, GameNumber.of(20L), heavyEffect, setOf(), factory.createEngineContext()
        ).event.amount
        check(heavyNormal == GameNumber.of(45L))
        check(heavyStaggered == GameNumber.of(51L))

        val umbral = registry.skill(DefaultGameContent.UMBRAL_CUT_ID)
            .effects.filterIsInstance<EffectSpec.DealDamage>().single()
        val healthyDamage = DamageSystem.dealToEnemy(
            multi, playerId, original, GameNumber.of(20L), umbral, setOf(), factory.createEngineContext()
        ).event.amount
        val weakened = original.copy(combatant = original.combatant.copy(currentHealth = GameNumber.of(20L)))
        val executeDamage = DamageSystem.dealToEnemy(
            multi, playerId, weakened, GameNumber.of(20L), umbral, setOf(), factory.createEngineContext()
        ).event.amount
        check(healthyDamage == GameNumber.of(16L))
        check(executeDamage == GameNumber.of(20L))

        val guardHealAmount = registry.skill(DefaultGameContent.GUARD_MEND_ID)
            .effects.filterIsInstance<EffectSpec.Heal>().single().flatAmount
        val guardHealing = HealingSystem.healPlayer(
            multi, playerId, playerId, guardHealAmount, registry
        )
        check(guardHealing.event.amount == GameNumber.of(45L))
        val vitalHealAmount = (registry.status(DefaultGameContent.VITAL_REGENERATION_STATUS_ID)
            .periodicEffects.single() as EffectSpec.Heal).flatAmount
        val vitalHealing = HealingSystem.healPlayer(
            multi, playerId, playerId, vitalHealAmount, registry
        )
        check(vitalHealing.event.amount == GameNumber.of(12L))

        val vitalTickAt = started.engine.simulationTime + GameDuration.ofSeconds(1L)
        val vitalStatus = StatusEffectState(
            instanceId = InstanceId(90_004L),
            definitionId = DefaultGameContent.VITAL_REGENERATION_STATUS_ID,
            sourceInstanceId = playerId,
            stackCount = 1,
            appliedAt = started.engine.simulationTime,
            expiresAt = started.engine.simulationTime + GameDuration.ofSeconds(4L),
            nextPeriodicTickAt = vitalTickAt
        )
        val vitalState = multi.copy(
            engine = multi.engine.copy(simulationTime = vitalTickAt),
            run = multi.run.copy(
                combat = multi.run.combat.copy(
                    playerCombatant = multi.run.combat.playerCombatant?.copy(
                        statusEffects = listOf(vitalStatus)
                    )
                )
            )
        )
        val vitalTick = StatusEffectSystem.execute(
            vitalState,
            ScheduledAction(
                dueAt = vitalTickAt,
                type = ScheduledActionType.STATUS_PERIODIC,
                stableTieBreakKey = vitalStatus.instanceId.value,
                ownerInstanceId = playerId,
                sourceContentId = DefaultGameContent.VITAL_REGENERATION_STATUS_ID
            ),
            factory.createEngineContext()
        )
        check(vitalTick.events.filterIsInstance<HealingApplied>().single().amount == GameNumber.of(12L))

        val tickAt = started.engine.simulationTime + GameDuration.ofSeconds(1L)
        val burning = StatusEffectState(
            instanceId = InstanceId(90_003L),
            definitionId = DefaultGameContent.BURNING_STATUS_ID,
            sourceInstanceId = playerId,
            stackCount = 3,
            appliedAt = started.engine.simulationTime,
            expiresAt = started.engine.simulationTime + GameDuration.ofSeconds(4L),
            nextPeriodicTickAt = tickAt
        )
        val burningState = multi.copy(
            engine = multi.engine.copy(simulationTime = tickAt),
            run = multi.run.copy(
                combat = multi.run.combat.copy(
                    enemies = listOf(
                        original.copy(combatant = original.combatant.copy(statusEffects = listOf(burning))),
                        second
                    )
                )
            )
        )
        val burnTick = StatusEffectSystem.execute(
            burningState,
            ScheduledAction(
                dueAt = tickAt,
                type = ScheduledActionType.STATUS_PERIODIC,
                stableTieBreakKey = burning.instanceId.value,
                ownerInstanceId = original.instanceId,
                sourceContentId = DefaultGameContent.BURNING_STATUS_ID
            ),
            factory.createEngineContext()
        )
        check(burnTick.events.filterIsInstance<DamageDealt>().size == 3)

        flameAndCinderProduceCanonicalThreeStackBurn()
        umbralCutTargetsLowestHealthEnemy()
        activeAndOfflineQuickSlashAreEquivalent()
    }

    private fun flameAndCinderProduceCanonicalThreeStackBurn() {
        val runtime = SimulationTestSupport.runtime(seed = 2_003L)
        SimulationTestSupport.startTraining(runtime)
        val started = runtime.state()
        runtime.replaceLoadedState(
            started.copy(
                run = started.run.copy(
                    player = started.run.player.copy(
                        equippedSkillIds = listOf(
                            DefaultGameContent.FLAME_BRAND_ID,
                            DefaultGameContent.CINDER_MARK_ID
                        )
                    ),
                    progression = started.run.progression.copy(
                        featureUnlocks = started.run.progression.featureUnlocks.copy(
                            unlockedFeatureIds = setOf(
                                DefaultGameContent.FLAME_BRAND_FEATURE_ID,
                                DefaultGameContent.CINDER_MARK_FEATURE_ID
                            )
                        )
                    )
                )
            )
        )
        SimulationTestSupport.checkAccepted(
            runtime.dispatch(QueueSkillCast(DefaultGameContent.FLAME_BRAND_ID))
        )
        runtime.advance(GameDuration.ofSeconds(1L))
        SimulationTestSupport.checkAccepted(
            runtime.dispatch(QueueSkillCast(DefaultGameContent.CINDER_MARK_ID))
        )
        runtime.advance(GameDuration.ofSeconds(1L))
        val burning = runtime.state().run.combat.enemies.single().combatant.statusEffects
            .single { it.definitionId == DefaultGameContent.BURNING_STATUS_ID }
        check(burning.stackCount == 3)
        check(burning.nextPeriodicTickAt == runtime.state().engine.simulationTime + GameDuration.ofSeconds(1L))
    }

    private fun umbralCutTargetsLowestHealthEnemy() {
        val runtime = SimulationTestSupport.runtime(seed = 2_004L)
        SimulationTestSupport.startTraining(runtime)
        val started = runtime.state()
        val original = started.run.combat.enemies.single()
        val weakerId = InstanceId(original.instanceId.value + 60_000L)
        val weaker = original.copy(
            instanceId = weakerId,
            combatant = original.combatant.copy(
                instanceId = weakerId,
                currentHealth = GameNumber.of(20L)
            )
        )
        runtime.replaceLoadedState(
            started.copy(
                run = started.run.copy(
                    player = started.run.player.copy(
                        equippedSkillIds = listOf(DefaultGameContent.UMBRAL_CUT_ID)
                    ),
                    progression = started.run.progression.copy(
                        featureUnlocks = started.run.progression.featureUnlocks.copy(
                            unlockedFeatureIds = setOf(DefaultGameContent.UMBRAL_CUT_FEATURE_ID)
                        )
                    ),
                    combat = started.run.combat.copy(enemies = listOf(original, weaker))
                )
            )
        )
        SimulationTestSupport.checkAccepted(
            runtime.dispatch(QueueSkillCast(DefaultGameContent.UMBRAL_CUT_ID))
        )
        val result = runtime.advance(GameDuration.ofSeconds(1L))
        val used = result.events.map { it.event }.filterIsInstance<SkillUsed>().single()
        check(used.targetInstanceIds == listOf(weakerId))
        check(result.events.map { it.event }.filterIsInstance<DamageDealt>()
            .single().targetInstanceId == weakerId)
    }

    private fun activeAndOfflineQuickSlashAreEquivalent() {
        val runtime = SimulationTestSupport.runtime(seed = 2_002L)
        SimulationTestSupport.startTraining(runtime)
        val started = runtime.state()
        runtime.replaceLoadedState(
            started.copy(
                run = started.run.copy(
                    player = started.run.player.copy(
                        equippedSkillIds = listOf(DefaultGameContent.QUICK_SLASH_ID)
                    )
                )
            )
        )
        SimulationTestSupport.checkAccepted(
            runtime.dispatch(QueueSkillCast(DefaultGameContent.QUICK_SLASH_ID))
        )
        val startingState = runtime.state()
        val factory = SimulationTestSupport.factory()
        val foreground = GameRuntime(factory.loadedGame(startingState), factory)
        val foregroundResult = foreground.advance(GameDuration.ofSeconds(1L))
        check(foregroundResult.events.count { it.event is DamageDealt } == 3)

        val savedAt = 9_000_000L
        val repository = SimulationTestSupport.InMemoryGameRepository(
            SaveEnvelope.create(
                gameState = startingState,
                contentVersion = SimulationTestSupport.CONTENT_VERSION,
                writtenAtEpochMs = savedAt
            )
        )
        val offline = OfflineSessionCoordinator(
            repository = repository,
            clock = SimulationTestSupport.MutableClock(savedAt + 1_000L),
            engineContext = factory.createEngineContext()
        ).resume() ?: error("Expected offline skill simulation")
        check(foregroundResult.state == offline.engineResult.state)
        check(foregroundResult.events == offline.engineResult.events)
    }
}
