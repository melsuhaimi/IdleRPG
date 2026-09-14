package com.idlerpg.game.simulation

import com.idlerpg.game.core.id.ContentId
import com.idlerpg.game.core.id.InstanceId
import com.idlerpg.game.core.number.GameNumber
import com.idlerpg.game.core.number.Ratio
import com.idlerpg.game.core.time.GameDuration
import com.idlerpg.game.data.content.ContentRegistry
import com.idlerpg.game.data.content.DefaultGameContent
import com.idlerpg.game.data.local.SaveData
import com.idlerpg.game.domain.definition.Affinity
import com.idlerpg.game.domain.definition.combat.EffectSpec
import com.idlerpg.game.domain.definition.combat.SkillDefinition
import com.idlerpg.game.domain.definition.resonance.ConvergenceDefinition
import com.idlerpg.game.domain.definition.resonance.ResonanceEmissionDefinition
import com.idlerpg.game.domain.definition.resonance.ResonancePatternDefinition
import com.idlerpg.game.domain.engine.EngineContext
import com.idlerpg.game.domain.event.ConvergenceTriggered
import com.idlerpg.game.domain.event.DamageDealt
import com.idlerpg.game.domain.event.HealingApplied
import com.idlerpg.game.domain.model.GameState
import com.idlerpg.game.domain.system.combat.ActionResolutionSystem
import com.idlerpg.game.domain.system.combat.StatusEffectSystem

/** Regression for a lethal skill followed by a selected-target Convergence. */
object DefeatedTargetFollowUpScenarioTest {
    @JvmStatic
    fun main(args: Array<String>) {
        run()
        println("DEFEATED_TARGET_FOLLOWUP_PASS")
    }

    fun run() {
        lethalSkillStillTriggersConvergenceAndHealing()
        areaEffectsSurviveWithoutRetargetingSelectedEffects()
        liveFollowUpMatchesOrdinaryEffects()
        invalidOrdinaryAndUnknownFollowUpTargetsStillReject()
        replayFromSaveProducesIdenticalStateAndEvents()
        lethalFinalPeriodicTickStillResolvesExpiryHealing()
    }

    private fun fixture(): GameState {
        val runtime = SimulationTestSupport.runtime(seed = 9_673L)
        if (runtime.state().run.combat.playerCombatant == null) {
            SimulationTestSupport.startTraining(runtime)
        }
        val state = runtime.state()
        val player = requireNotNull(state.run.combat.playerCombatant)
        val enemy = state.run.combat.enemies.first()
        return state.copy(run = state.run.copy(
            player = state.run.player.copy(currentHealth = GameNumber.of(10L)),
            combat = state.run.combat.copy(
                playerCombatant = player.copy(currentHealth = GameNumber.of(10L)),
                enemies = listOf(enemy.copy(combatant = enemy.combatant.copy(currentHealth = GameNumber.ONE)))
            )
        ))
    }

    private val burstId = ContentId("convergence.regression_followup")
    private val directDamage = EffectSpec.DealDamage(
        powerRatio = Ratio.ZERO, flatBonus = GameNumber.of(100L), canCritical = false
    )
    private val skill = SkillDefinition(
        id = ContentId("skill.regression_lethal"),
        effects = listOf(directDamage),
        affinityTags = setOf(Affinity.MIGHT),
        resonanceEmissions = listOf(ResonanceEmissionDefinition(Affinity.MIGHT, GameNumber.ONE))
    )

    private fun context(state: GameState): EngineContext {
        val baseContent = DefaultGameContent.create()
        val content = baseContent.copy(convergences = baseContent.convergences + listOf(
            ConvergenceDefinition(
                id = burstId,
                pattern = ResonancePatternDefinition(listOf(Affinity.MIGHT)),
                effects = listOf(directDamage, EffectSpec.Heal(GameNumber.of(5L)))
            )
        ))
        return EngineContext(contentRegistry = ContentRegistry(content)).also {
            it.beginExecution(state.engine)
        }
    }

    private fun lethalSkillStillTriggersConvergenceAndHealing() {
        val state = fixture()
        val playerId = requireNotNull(state.run.combat.playerCombatant).instanceId
        val enemyId = state.run.combat.enemies.single().instanceId
        val result = ActionResolutionSystem.resolveSkill(state, playerId, enemyId, skill, context(state))
        check(result.events.filterIsInstance<ConvergenceTriggered>().single().convergenceId == burstId)
        check(result.events.filterIsInstance<DamageDealt>().size == 1)
        check(result.state.run.combat.enemies.single().combatant.currentHealth == GameNumber.ZERO)
        check(requireNotNull(result.state.run.combat.playerCombatant).currentHealth > GameNumber.of(10L))
        check(burstId in result.state.meta.discoveries.discoveredConvergenceIds)
    }

    private fun areaEffectsSurviveWithoutRetargetingSelectedEffects() {
        val initial = fixture()
        val first = initial.run.combat.enemies.single()
        val extraId = InstanceId(initial.engine.nextInstanceIdCounter + 10L)
        val second = first.copy(instanceId = extraId, combatant = first.combatant.copy(instanceId = extraId))
        val state = initial.copy(run = initial.run.copy(combat = initial.run.combat.copy(enemies = listOf(
            first.copy(combatant = first.combatant.copy(currentHealth = GameNumber.ZERO)), second
        ))))
        val result = ActionResolutionSystem.resolveFollowUpEffects(
            state, requireNotNull(state.run.combat.playerCombatant).instanceId, first.instanceId,
            listOf(directDamage, directDamage.copy(targetPattern = EffectSpec.TargetPattern.ALL_ENEMIES)),
            context(state)
        )
        val damage = result.events.filterIsInstance<DamageDealt>()
        check(damage.size == 1 && damage.single().targetInstanceId == extraId)
    }

    private fun liveFollowUpMatchesOrdinaryEffects() {
        val state = fixture()
        val playerId = requireNotNull(state.run.combat.playerCombatant).instanceId
        val target = state.run.combat.enemies.single().instanceId
        val effects = listOf(directDamage)
        val direct = ActionResolutionSystem.resolvePrimitiveEffects(state, playerId, target, effects, context(state))
        val followUp = ActionResolutionSystem.resolveFollowUpEffects(state, playerId, target, effects, context(state))
        check(direct == followUp)
    }

    private fun invalidOrdinaryAndUnknownFollowUpTargetsStillReject() {
        val initial = fixture()
        val enemy = initial.run.combat.enemies.single()
        val state = initial.copy(run = initial.run.copy(combat = initial.run.combat.copy(enemies = listOf(
            enemy.copy(combatant = enemy.combatant.copy(currentHealth = GameNumber.ZERO))
        ))))
        val playerId = requireNotNull(state.run.combat.playerCombatant).instanceId
        check(runCatching {
            ActionResolutionSystem.resolvePrimitiveEffects(state, playerId, enemy.instanceId, listOf(directDamage), context(state))
        }.exceptionOrNull() is IllegalArgumentException)
        check(runCatching {
            ActionResolutionSystem.resolveFollowUpEffects(state, playerId, InstanceId(Long.MAX_VALUE), listOf(directDamage), context(state))
        }.exceptionOrNull() is IllegalArgumentException)
        val skipped = ActionResolutionSystem.resolveFollowUpEffects(
            state, playerId, enemy.instanceId,
            listOf(EffectSpec.ApplyStatus(DefaultGameContent.BURNING_STATUS_ID)), context(state)
        )
        check(skipped.state == state && skipped.events.isEmpty())
    }

    private fun replayFromSaveProducesIdenticalStateAndEvents() {
        val state = fixture()
        val restored = SaveData.fromGameState(state).toGameState()
        fun resolve(input: GameState) = ActionResolutionSystem.resolveSkill(
            input, requireNotNull(input.run.combat.playerCombatant).instanceId,
            input.run.combat.enemies.single().instanceId, skill, context(input)
        )
        check(resolve(state) == resolve(restored))
    }

    private fun lethalFinalPeriodicTickStillResolvesExpiryHealing() {
        val state = fixture()
        val baseContent = DefaultGameContent.create()
        val duration = GameDuration.ofSeconds(1L)
        val definition = baseContent.statuses.first { it.id == DefaultGameContent.BURNING_STATUS_ID }.copy(
            id = ContentId("status.regression_final_tick"),
            duration = duration,
            periodicInterval = duration,
            periodicEffects = listOf(directDamage),
            expireEffects = listOf(directDamage, EffectSpec.Heal(GameNumber.of(5L)))
        )
        val context = EngineContext(ContentRegistry(baseContent.copy(statuses = baseContent.statuses + definition)))
        context.beginExecution(state.engine)
        val playerId = requireNotNull(state.run.combat.playerCombatant).instanceId
        val targetId = state.run.combat.enemies.single().instanceId
        val applied = StatusEffectSystem.apply(state, playerId, targetId, definition, context).state
        val atExpiry = applied.copy(engine = applied.engine.copy(simulationTime = applied.engine.simulationTime + duration))
        val action = StatusEffectSystem.scheduledActions(atExpiry).single {
            it.ownerInstanceId == targetId && it.sourceContentId == definition.id
        }
        val result = StatusEffectSystem.execute(atExpiry, action, context)
        check(result.events.filterIsInstance<DamageDealt>().count { it.targetInstanceId == targetId } == 1)
        check(result.events.filterIsInstance<HealingApplied>().any {
            it.targetInstanceId == playerId && it.amount > GameNumber.ZERO
        })
    }
}
