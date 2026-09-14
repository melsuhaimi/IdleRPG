package com.idlerpg.game.domain.system.adaptation

import com.idlerpg.game.core.id.ContentId
import com.idlerpg.game.core.id.InstanceId
import com.idlerpg.game.core.number.GameMath
import com.idlerpg.game.core.number.GameNumber
import com.idlerpg.game.core.random.WeightedValue
import com.idlerpg.game.data.content.ContentRegistry
import com.idlerpg.game.domain.definition.Affinity
import com.idlerpg.game.domain.definition.adaptation.MutationDefinition
import com.idlerpg.game.domain.definition.adaptation.MutationEffectDefinition
import com.idlerpg.game.domain.definition.enemy.EnemyDefinition
import com.idlerpg.game.domain.definition.world.RegionDefinition
import com.idlerpg.game.domain.engine.EngineContext
import com.idlerpg.game.domain.event.GameEvent
import com.idlerpg.game.domain.event.MutationRolled
import com.idlerpg.game.domain.model.GameState
import com.idlerpg.game.domain.model.adaptation.ActiveMutationState
import com.idlerpg.game.domain.model.adaptation.RegionAdaptationState
import com.idlerpg.game.domain.model.combat.EnemyState
import com.idlerpg.game.core.number.Ratio
import com.idlerpg.game.core.time.GameDuration
import com.idlerpg.game.domain.system.combat.CombatMath
import java.math.BigInteger

/** Mutation selection output for one future enemy spawn. */
data class MutationRollResult(
    val mutations: List<ActiveMutationState>,
    val events: List<GameEvent>
)

/** Deterministic mutation eligibility, controlled-RNG selection, and mutation modifiers. */
object MutationSystem {

    fun eligibleMutationIds(
        regionDefinition: RegionDefinition,
        regionState: RegionAdaptationState,
        contentRegistry: ContentRegistry
    ): Set<ContentId> =
        regionDefinition.adaptationMutationIds
            .asSequence()
            .map(contentRegistry::mutation)
            .filter { definition ->
                val tier =
                    regionState.tierByAffinityId[definition.triggerAffinity.id] ?: 0
                tier >= definition.minimumAdaptationTier
            }
            .map { it.id }
            .toSortedSet()

    fun rollForEnemy(
        state: GameState,
        enemyDefinition: EnemyDefinition,
        enemyInstanceId: InstanceId,
        regionDefinition: RegionDefinition?,
        context: EngineContext
    ): MutationRollResult {
        if (regionDefinition == null ||
            regionDefinition.maxAdaptationMutationsPerEnemy <= 0 ||
            regionDefinition.adaptationMutationIds.isEmpty()
        ) {
            return MutationRollResult(emptyList(), emptyList())
        }

        val regionState =
            state.run.adaptation.regionStateById[regionDefinition.id]
                ?: return MutationRollResult(emptyList(), emptyList())

        val eligible = regionDefinition.adaptationMutationIds
            .asSequence()
            .map(context.contentRegistry::mutation)
            .filter { definition ->
                val tier =
                    regionState.tierByAffinityId[definition.triggerAffinity.id] ?: 0
                tier >= definition.minimumAdaptationTier &&
                    isCompatible(definition, enemyDefinition)
            }
            .sortedBy { it.id }
            .toMutableList()

        if (eligible.isEmpty()) {
            return MutationRollResult(emptyList(), emptyList())
        }

        val selected = mutableListOf<ActiveMutationState>()
        val events = mutableListOf<GameEvent>()
        val slots = minOf(
            regionDefinition.maxAdaptationMutationsPerEnemy,
            eligible.size
        )

        repeat(slots) {
            val picked = context.random.chooseWeighted(
                eligible.map { definition ->
                    WeightedValue(
                        value = definition,
                        weight = definition.selectionWeight
                    )
                }
            )
            eligible.remove(picked)

            val tier =
                regionState.tierByAffinityId[picked.triggerAffinity.id]
                    ?: error("Eligible mutation ${picked.id} lost its adaptation tier")
            selected += ActiveMutationState(
                mutationId = picked.id,
                sourceAffinityId = picked.triggerAffinity.id,
                adaptationTier = tier
            )
            events += MutationRolled(
                enemyInstanceId = enemyInstanceId,
                mutationId = picked.id
            )
        }

        return MutationRollResult(
            mutations = selected.sortedBy { it.mutationId },
            events = events
        )
    }

    /**
     * Applies active mutation damage modifiers in stable mutation-ID order.
     *
     * Sequential floor rounding is intentional and deterministic. The active mutation
     * snapshot belongs to the enemy, so pressure changes during the encounter cannot
     * retroactively change its modifiers.
     */
    fun applyDamageTakenModifiers(
        target: EnemyState,
        sourceAffinities: Set<Affinity>,
        baseDamage: GameNumber,
        contentRegistry: ContentRegistry
    ): GameNumber {
        if (baseDamage == GameNumber.ZERO || target.activeMutations.isEmpty()) {
            return baseDamage
        }

        var damage = baseDamage
        for (active in target.activeMutations.sortedBy { it.mutationId }) {
            for (effect in contentRegistry.mutation(active.mutationId).effects) when (effect) {
                is MutationEffectDefinition.DamageTakenMultiplierForAffinity -> {
                    if (effect.affinity in sourceAffinities) {
                        damage = GameMath.applyRatio(damage, effect.multiplier)
                        damage = GameMath.applyRatio(damage,
                            com.idlerpg.game.core.config.AdaptationCurve.resistanceMultiplier(active.adaptationTier))
                    }
                }
                else -> Unit
            }
        }
        return damage
    }

    /** Applies carrier-specific cadence effects after Elite cadence and before deadline storage. */
    fun enemyActionInterval(
        baseInterval: GameDuration,
        carrier: EnemyState,
        contentRegistry: ContentRegistry
    ): GameDuration {
        var interval = baseInterval
        for (active in carrier.activeMutations.sortedBy { it.mutationId }) {
            for (effect in contentRegistry.mutation(active.mutationId).effects) {
                if (effect is MutationEffectDefinition.EnemyActionIntervalMultiplier) {
                    val millis = BigInteger.valueOf(interval.millis)
                        .multiply(BigInteger.valueOf(effect.multiplier.units))
                        .divide(BigInteger.valueOf(Ratio.UNITS_PER_ONE))
                        .coerceAtLeast(BigInteger.ONE)
                        .min(BigInteger.valueOf(Long.MAX_VALUE))
                        .toLong()
                    interval = GameDuration.ofMillis(millis)
                    if (interval < CombatMath.MINIMUM_ACTION_INTERVAL) {
                        interval = CombatMath.MINIMUM_ACTION_INTERVAL
                    }
                }
            }
        }
        return interval
    }

    fun additionalResonanceDrain(
        carrier: EnemyState,
        contentRegistry: ContentRegistry
    ): GameNumber = carrier.activeMutations.sortedBy { it.mutationId }.fold(GameNumber.ZERO) { total, active ->
        contentRegistry.mutation(active.mutationId).effects.fold(total) { accumulated, effect ->
            when (effect) {
                is MutationEffectDefinition.AdditionalResonanceDrain -> accumulated + effect.amount
                else -> accumulated
            }
        }
    }

    /** Returns the strongest living suppression once; duplicate carriers never compound. */
    fun playerHealingMultiplier(
        state: GameState,
        contentRegistry: ContentRegistry
    ): Ratio = state.run.combat.enemies.asSequence()
        .filter { it.combatant.currentHealth > GameNumber.ZERO }
        .flatMap { enemy ->
            enemy.activeMutations.asSequence()
                .sortedBy { it.mutationId }
                .flatMap { active -> contentRegistry.mutation(active.mutationId).effects.asSequence() }
        }
        .filterIsInstance<MutationEffectDefinition.PlayerHealingMultiplier>()
        .map { it.multiplier }
        .minOrNull() ?: Ratio.ONE

    fun enemyOutgoingDamage(
        state: GameState,
        carrier: EnemyState,
        baseDamage: GameNumber,
        contentRegistry: ContentRegistry
    ): GameNumber {
        val playerStatuses = state.run.combat.playerCombatant?.statusEffects.orEmpty()
        var damage = baseDamage
        for (active in carrier.activeMutations.sortedBy { it.mutationId }) {
            for (effect in contentRegistry.mutation(active.mutationId).effects) {
                if (effect is MutationEffectDefinition.EnemyDamageMultiplierWhilePlayerHasAffinityStatus &&
                    playerStatuses.any { effect.affinity in contentRegistry.status(it.definitionId).affinityTags }
                ) {
                    damage = GameMath.applyRatio(damage, effect.multiplier)
                }
            }
        }
        return damage
    }

    private fun isCompatible(
        mutation: MutationDefinition,
        enemy: EnemyDefinition
    ): Boolean =
        mutation.requiredEnemyTags.isEmpty() ||
            mutation.requiredEnemyTags.all { it in enemy.adaptationTags }
}
