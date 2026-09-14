package com.idlerpg.game.domain.system.combat

import com.idlerpg.game.core.id.InstanceId
import com.idlerpg.game.core.number.GameNumber
import com.idlerpg.game.core.number.GameMath
import com.idlerpg.game.core.number.Ratio
import com.idlerpg.game.domain.definition.Affinity
import com.idlerpg.game.domain.definition.combat.BasicAttackDefinition
import com.idlerpg.game.domain.definition.combat.EffectSpec
import com.idlerpg.game.domain.definition.combat.SkillDefinition
import com.idlerpg.game.domain.engine.EngineContext
import com.idlerpg.game.domain.event.GameEvent
import com.idlerpg.game.domain.event.ResonanceConsumed
import com.idlerpg.game.domain.event.ResonanceGenerated
import com.idlerpg.game.domain.event.SkillUsed
import com.idlerpg.game.domain.model.GameState
import com.idlerpg.game.domain.system.adaptation.AdaptationExposureSystem
import com.idlerpg.game.domain.system.progression.AffinityMasterySystem
import com.idlerpg.game.domain.system.resonance.ConvergenceSystem
import com.idlerpg.game.domain.system.resonance.ResonanceSystem
import com.idlerpg.game.domain.system.skill.SkillScalingSystem
import com.idlerpg.game.domain.system.stats.DerivedStatSystem
import com.idlerpg.game.domain.system.stats.ModifierSystem

/** State/events produced by resolving one validated combat action. */
data class ActionResolutionResult(
    val state: GameState,
    val events: List<GameEvent>
)

/** Expands validated actions/status effects into ordered primitive effects. */
object ActionResolutionSystem {

    fun resolveBasicAttack(
        state: GameState,
        actorInstanceId: InstanceId,
        targetInstanceId: InstanceId,
        definition: BasicAttackDefinition,
        context: EngineContext
    ): ActionResolutionResult =
        resolvePrimitiveEffects(
            state = state,
            actorInstanceId = actorInstanceId,
            targetInstanceId = targetInstanceId,
            effects = definition.effects,
            context = context,
            adaptationAffinitySequence = definition.affinityTags.sortedBy { it.id }
        )

    /**
     * Canonical skill order:
     * direct effects -> effective-contribution observation -> modified declared Resonance
     * emissions -> sequence update -> at most one Convergence -> Convergence effects.
     */
    fun resolveSkill(
        state: GameState,
        actorInstanceId: InstanceId,
        targetInstanceId: InstanceId,
        definition: SkillDefinition,
        context: EngineContext
    ): ActionResolutionResult {
        val events = mutableListOf<GameEvent>()
        val preservedTailCount = ModifierSystem.preservedSequenceEntries(
            state, definition.id, context.contentRegistry
        )
        val preservedTail = state.run.resonance.sequence.affinityIds.takeLast(preservedTailCount)
        val damageMultiplier = ModifierSystem.skillDamageMultiplier(
            state, definition.id, targetInstanceId, context.contentRegistry
        )
        val healingMultiplier = ModifierSystem.skillHealingMultiplier(
            state, definition.id, context.contentRegistry
        )
        val scaledEffects = SkillScalingSystem.scaledEffects(state, definition)
        val traitAdjustedEffects = scaledEffects.map { effect ->
            when (effect) {
                is EffectSpec.DealDamage -> effect.copy(
                    powerRatio = multiplyRatios(effect.powerRatio, damageMultiplier)
                )
                is EffectSpec.Heal -> effect.copy(
                    flatAmount = GameMath.applyRatio(effect.flatAmount, healingMultiplier)
                )
                else -> effect
            }
        }.toMutableList()
        ModifierSystem.skillCleaveRatio(state, definition.id, context.contentRegistry)?.let { cleave ->
            scaledEffects.filterIsInstance<EffectSpec.DealDamage>()
                .filter { it.targetPattern == EffectSpec.TargetPattern.SELECTED }
                .forEach { effect ->
                    traitAdjustedEffects += effect.copy(
                        powerRatio = multiplyRatios(effect.powerRatio, cleave),
                        targetPattern = EffectSpec.TargetPattern.OTHER_ENEMIES
                    )
                }
        }
        events += SkillUsed(
            actorInstanceId = actorInstanceId,
            skillId = definition.id,
            targetInstanceIds = affectedTargetIds(
                state = state,
                selectedTargetId = targetInstanceId,
                effects = traitAdjustedEffects
            )
        )

        val direct = resolvePrimitiveEffects(
            state = state,
            actorInstanceId = actorInstanceId,
            targetInstanceId = targetInstanceId,
            effects = traitAdjustedEffects,
            context = context,
            adaptationAffinitySequence = definition.affinityTags.sortedBy { it.id }
        )
        var currentState = direct.state
        events += direct.events

        val bonusByAffinity = ModifierSystem.skillResonanceBonuses(
            currentState, definition.id, context.contentRegistry
        )
        val modifiedEmissions = definition.resonanceEmissions.map { emission ->
            emission.copy(
                amount = ModifierSystem.resonanceEmissionAmount(
                    state = currentState,
                    affinity = emission.affinity,
                    baseAmount = emission.amount,
                    contentRegistry = context.contentRegistry
                ) + (bonusByAffinity[emission.affinity] ?: GameNumber.ZERO)
            )
        } + bonusByAffinity.filterKeys { affinity ->
            definition.resonanceEmissions.none { it.affinity == affinity }
        }.map { (affinity, amount) ->
            com.idlerpg.game.domain.definition.resonance.ResonanceEmissionDefinition(affinity, amount)
        }

        val resonance = ResonanceSystem.emitAll(
            state = currentState,
            emissions = modifiedEmissions,
            sourceDefinitionId = definition.id,
            sourceInstanceId = actorInstanceId,
            maximumChargePerAffinity = context.balanceConfig.resonanceChargeCapPerAffinity,
            sequenceMaximumSize = context.balanceConfig.resonanceSequenceBufferSize
        )
        currentState = resonance.state
        events += resonance.events

        val convergence = ConvergenceSystem.resolveFirstEligible(
            state = currentState,
            sourceActionId = definition.id,
            sourceInstanceId = actorInstanceId,
            targetInstanceId = targetInstanceId,
            context = context
        )
        currentState = convergence.state
        events += convergence.events

        if (preservedTail.isNotEmpty()) {
            val sequence = (currentState.run.resonance.sequence.affinityIds + preservedTail)
                .takeLast(context.balanceConfig.resonanceSequenceBufferSize)
            currentState = currentState.copy(
                run = currentState.run.copy(
                    resonance = currentState.run.resonance.copy(
                        sequence = currentState.run.resonance.sequence.copy(affinityIds = sequence)
                    )
                )
            )
        }

        return ActionResolutionResult(currentState, events)
    }

    /**
     * Post-action effects may retain an enemy defeated by the triggering action.
     * Selected effects then expire without retargeting; self and all/other-enemy effects run.
     * Unknown IDs remain errors, and ordinary actions keep their living-target check.
     */
    fun resolveFollowUpEffects(
        state: GameState,
        actorInstanceId: InstanceId,
        targetInstanceId: InstanceId,
        effects: List<EffectSpec>,
        context: EngineContext,
        adaptationAffinitySequence: List<Affinity> = emptyList()
    ): ActionResolutionResult {
        val knownTarget = state.run.combat.playerCombatant?.instanceId == targetInstanceId ||
            state.run.combat.enemies.any { it.instanceId == targetInstanceId }
        require(knownTarget) { "Follow-up target $targetInstanceId is not present" }
        val remainingEffects = if (isLivingCombatant(state, targetInstanceId)) effects else
            effects.filterNot { effectTargetPattern(it) == EffectSpec.TargetPattern.SELECTED }
        return resolvePrimitiveEffects(
            state, actorInstanceId, targetInstanceId, remainingEffects, context,
            adaptationAffinitySequence
        )
    }

    /** Primitive effects do not emit Resonance by themselves. */
    fun resolvePrimitiveEffects(
        state: GameState,
        actorInstanceId: InstanceId,
        targetInstanceId: InstanceId,
        effects: List<EffectSpec>,
        context: EngineContext,
        adaptationAffinitySequence: List<Affinity> = emptyList()
    ): ActionResolutionResult {
        var currentState = state
        val events = mutableListOf<GameEvent>()
        if (effects.any { effectTargetPattern(it) == EffectSpec.TargetPattern.SELECTED }) {
            require(isLivingCombatant(state, targetInstanceId)) {
                "Selected target $targetInstanceId is not a living combatant"
            }
        }

        for (effect in effects) {
            when (effect) {
                is EffectSpec.DealDamage -> {
                    val targetIds = effectTargetIds(
                        state = currentState,
                        actorInstanceId = actorInstanceId,
                        selectedTargetId = targetInstanceId,
                        targetPattern = effect.targetPattern
                    )
                    require(effect.targetPattern != EffectSpec.TargetPattern.SELF) {
                        "Damage effects cannot target SELF"
                    }
                    // Earlier effects in the same authored sequence may have defeated the
                    // selected target. Later hits/status follow-ups then resolve as bounded
                    // no-ops instead of attempting to act on a dead combatant.
                    if (targetIds.isEmpty()) {
                        continue
                    }
                    val attackPower = DerivedStatSystem.attackPower(
                        currentState,
                        context.contentRegistry
                    )

                    repeat(effect.hitCount) {
                        for (resolvedTargetId in targetIds) {
                            val target = currentState.run.combat.enemies
                                .firstOrNull {
                                    it.instanceId == resolvedTargetId &&
                                        it.combatant.currentHealth > GameNumber.ZERO
                                }
                                ?: continue
                            val damage = DamageSystem.dealToEnemy(
                                state = currentState,
                                sourceInstanceId = actorInstanceId,
                                target = target,
                                attackPower = attackPower,
                                effect = effect,
                                sourceAffinities = adaptationAffinitySequence.toSet(),
                                context = context
                            )
                            currentState = currentState.copy(
                                run = currentState.run.copy(
                                    combat = currentState.run.combat.copy(
                                        enemies = currentState.run.combat.enemies.map { enemy ->
                                            if (enemy.instanceId == resolvedTargetId) damage.enemy else enemy
                                        }
                                    )
                                )
                            )
                            currentState = AdaptationExposureSystem.observeEffectiveContribution(
                                state = currentState,
                                affinitySequence = adaptationAffinitySequence,
                                effectiveAmount = damage.event.amount
                            )
                            events += damage.event

                            if (isPlayerActor(currentState, actorInstanceId)) {
                                val mastery = AffinityMasterySystem.grantEffectiveContribution(
                                    state = currentState,
                                    affinities = adaptationAffinitySequence,
                                    effectiveAmount = damage.event.amount,
                                    contentRegistry = context.contentRegistry
                                )
                                currentState = mastery.state
                                events += mastery.events
                            }
                        }
                    }
                }

                is EffectSpec.Heal -> {
                    val healingTarget = effectTargetIds(
                        currentState, actorInstanceId, targetInstanceId, effect.targetPattern
                    ).singleOrNull() ?: continue
                    val healing = HealingSystem.healPlayer(
                        state = currentState,
                        sourceInstanceId = actorInstanceId,
                        targetInstanceId = healingTarget,
                        requestedAmount = effect.flatAmount,
                        contentRegistry = context.contentRegistry
                    )
                    currentState = healing.state
                    events += healing.event

                    if (isPlayerActor(currentState, actorInstanceId)) {
                        currentState = AdaptationExposureSystem.observeEffectiveContribution(
                            state = currentState,
                            affinitySequence = adaptationAffinitySequence,
                            effectiveAmount = healing.event.amount
                        )
                        val mastery = AffinityMasterySystem.grantEffectiveContribution(
                            state = currentState,
                            affinities = adaptationAffinitySequence,
                            effectiveAmount = healing.event.amount,
                            contentRegistry = context.contentRegistry
                        )
                        currentState = mastery.state
                        events += mastery.events
                    }
                }

                is EffectSpec.ApplyStatus -> {
                    effectTargetIds(
                        currentState, actorInstanceId, targetInstanceId, effect.targetPattern
                    ).forEach { resolvedTargetId ->
                        val transition = StatusEffectSystem.apply(
                            state = currentState,
                            sourceInstanceId = actorInstanceId,
                            targetInstanceId = resolvedTargetId,
                            definition = context.contentRegistry.status(effect.statusDefinitionId),
                            context = context
                        )
                        currentState = transition.state
                        events += transition.events
                    }
                }

                is EffectSpec.RemoveStatus -> {
                    effectTargetIds(
                        currentState, actorInstanceId, targetInstanceId, effect.targetPattern
                    ).forEach { resolvedTargetId ->
                        val transition = StatusEffectSystem.removeByDefinition(
                            state = currentState,
                            targetInstanceId = resolvedTargetId,
                            statusDefinitionId = effect.statusDefinitionId
                        )
                        currentState = transition.state
                        events += transition.events
                    }
                }

                is EffectSpec.ShiftResonance -> {
                    val charges = currentState.run.resonance.chargeByAffinityId
                    val available = charges[effect.fromAffinity.id] ?: GameNumber.ZERO
                    val shifted = if (available < effect.amount) available else effect.amount
                    if (shifted > GameNumber.ZERO) {
                        val destination = charges[effect.toAffinity.id] ?: GameNumber.ZERO
                        currentState = currentState.copy(
                            run = currentState.run.copy(
                                resonance = currentState.run.resonance.copy(
                                    chargeByAffinityId = charges +
                                        (effect.fromAffinity.id to (available - shifted)) +
                                        (effect.toAffinity.id to (destination + shifted))
                                )
                            )
                        )
                        events += ResonanceConsumed(effect.fromAffinity.id, shifted)
                        events += ResonanceGenerated(
                            effect.toAffinity.id,
                            shifted,
                            com.idlerpg.game.core.id.ContentId("effect.resonance_shift"),
                            actorInstanceId
                        )
                    }
                }
            }
        }

        return ActionResolutionResult(currentState, events)
    }

    private fun affectedTargetIds(
        state: GameState,
        selectedTargetId: InstanceId,
        effects: List<EffectSpec>
    ): List<InstanceId> {
        val playerId = state.run.combat.playerCombatant?.instanceId
            ?: error("Active skill resolution requires player combatant")
        return effects.flatMap { effect ->
            val pattern = effectTargetPattern(effect)
            effectTargetIds(state, playerId, selectedTargetId, pattern)
        }.distinct()
    }

    private fun effectTargetPattern(effect: EffectSpec): EffectSpec.TargetPattern = when (effect) {
        is EffectSpec.DealDamage -> effect.targetPattern
        is EffectSpec.Heal -> effect.targetPattern
        is EffectSpec.ApplyStatus -> effect.targetPattern
        is EffectSpec.RemoveStatus -> effect.targetPattern
        is EffectSpec.ShiftResonance -> EffectSpec.TargetPattern.SELF
    }

    private fun effectTargetIds(
        state: GameState,
        actorInstanceId: InstanceId,
        selectedTargetId: InstanceId,
        targetPattern: EffectSpec.TargetPattern
    ): List<InstanceId> = when (targetPattern) {
        EffectSpec.TargetPattern.SELF -> listOf(actorInstanceId)
        EffectSpec.TargetPattern.ALL_ENEMIES ->
            TargetingSystem.allLivingEnemies(state.run.combat).map { it.instanceId }
        EffectSpec.TargetPattern.ADJACENT_ENEMIES -> {
            val livingIds = TargetingSystem.allLivingEnemies(state.run.combat).map { it.instanceId }
            val selectedIndex = livingIds.indexOf(selectedTargetId)
            if (selectedIndex < 0) emptyList() else listOfNotNull(
                livingIds.getOrNull(selectedIndex - 1),
                livingIds.getOrNull(selectedIndex + 1)
            )
        }
        EffectSpec.TargetPattern.OTHER_ENEMIES ->
            TargetingSystem.allLivingEnemies(state.run.combat)
                .map { it.instanceId }
                .filterNot { it == selectedTargetId }
        EffectSpec.TargetPattern.SELECTED -> {
            val player = state.run.combat.playerCombatant
            if (player?.instanceId == selectedTargetId && player.currentHealth > GameNumber.ZERO) {
                listOf(selectedTargetId)
            } else {
                val enemy = state.run.combat.enemies.firstOrNull {
                    it.instanceId == selectedTargetId &&
                        it.combatant.currentHealth > GameNumber.ZERO
                }
                if (enemy == null) emptyList() else listOf(enemy.instanceId)
            }
        }
    }

    private fun isPlayerActor(state: GameState, actorInstanceId: InstanceId): Boolean =
        state.run.combat.playerCombatant?.instanceId == actorInstanceId

    private fun isLivingCombatant(state: GameState, instanceId: InstanceId): Boolean =
        state.run.combat.playerCombatant?.let {
            it.instanceId == instanceId && it.currentHealth > GameNumber.ZERO
        } == true || state.run.combat.enemies.any {
            it.instanceId == instanceId && it.combatant.currentHealth > GameNumber.ZERO
        }

    private fun multiplyRatios(left: Ratio, right: Ratio): Ratio =
        GameMath.multiplyRatios(left, right)
}
