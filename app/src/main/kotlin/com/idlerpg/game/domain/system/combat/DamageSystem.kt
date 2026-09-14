package com.idlerpg.game.domain.system.combat

import com.idlerpg.game.core.id.InstanceId
import com.idlerpg.game.core.number.GameMath
import com.idlerpg.game.core.number.GameNumber
import com.idlerpg.game.core.number.Ratio
import com.idlerpg.game.data.content.ContentRegistry
import com.idlerpg.game.domain.definition.Affinity
import com.idlerpg.game.domain.definition.DamageKind
import com.idlerpg.game.domain.definition.combat.EffectSpec
import com.idlerpg.game.domain.definition.enemy.EnemyRole
import com.idlerpg.game.domain.engine.EngineContext
import com.idlerpg.game.domain.event.DamageDealt
import com.idlerpg.game.domain.model.combat.EnemyState
import com.idlerpg.game.domain.system.adaptation.MutationSystem
import com.idlerpg.game.domain.system.enemy.EnemyScalingSystem
import java.math.BigInteger

/** Result of applying one direct deterministic damage effect to one enemy. */
data class PlayerDamageResult(
    val state: com.idlerpg.game.domain.model.GameState,
    val event: DamageDealt
)

data class EnemyDamageResult(
    val enemy: EnemyState,
    val event: DamageDealt
)

/** Owns deterministic direct-damage calculation, mutation mitigation, and HP subtraction. */
object DamageSystem {

    fun dealToEnemy(
        state: com.idlerpg.game.domain.model.GameState,
        sourceInstanceId: InstanceId,
        target: EnemyState,
        attackPower: GameNumber,
        effect: EffectSpec.DealDamage,
        sourceAffinities: Set<Affinity>,
        context: EngineContext
    ): EnemyDamageResult {
        val effectiveRatio = Ratio.ofUnits(
            effect.conditions
                .filter { conditionApplies(state, target, it, context.contentRegistry) }
                .fold(effect.powerRatio) { total, condition ->
                    GameMath.ratioAfterSteps(
                        base = total,
                        growthPerStep = condition.bonusPowerRatio,
                        steps = 1L
                    )
                }.units
        )
        val scaledPower = GameMath.applyRatio(
            value = attackPower,
            ratio = effectiveRatio
        )
        val rawOutput = scaledPower + effect.flatBonus
        val scaledOutput = when (effect.scalingPolicy) {
            EffectSpec.DamageScalingPolicy.ATTACK_POWER -> rawOutput
            EffectSpec.DamageScalingPolicy.ATTACK_AND_EFFECT_POWER ->
                GameMath.applyRatio(
                    rawOutput,
                    com.idlerpg.game.domain.system.stats.DerivedStatSystem.effectPower(state, context.contentRegistry)
                )
        }
        val criticalChance = com.idlerpg.game.domain.system.stats.DerivedStatSystem.criticalChance(state, context.contentRegistry)
        val critical = effect.canCritical && criticalChance > com.idlerpg.game.core.number.Ratio.ZERO &&
            CombatMath.isCritical(context.random.nextLong(CombatMath.CRITICAL_ROLL_BOUND), criticalChance)
        val preMutationDamage = if (critical) {
            GameMath.applyRatio(
                scaledOutput,
                com.idlerpg.game.domain.system.stats.DerivedStatSystem.criticalMultiplier(state, context.contentRegistry)
            )
        } else scaledOutput
        val statusAdjustedDamage = GameMath.applyRatio(
            preMutationDamage,
            damageTakenRatio(target, context.contentRegistry)
        )
        // Combat order is explicit: skill scaling -> effect power -> crit -> status
        // vulnerability -> physical armor -> formation -> adaptation -> HP clamp.
        val targetDefinition = context.contentRegistry.enemy(target.definitionId)
        val armorAdjustedDamage = if (effect.damageKind == DamageKind.PHYSICAL) {
            CombatMath.mitigate(
                damage = statusAdjustedDamage,
                armor = EnemyScalingSystem.scaledArmor(targetDefinition, target.scalingTier),
                armorPenetration = GameNumber.ZERO
            )
        } else {
            statusAdjustedDamage
        }
        val formationAdjustedDamage = formationAdjustedDamage(
            state = state,
            target = target,
            sourceAffinities = sourceAffinities,
            damage = armorAdjustedDamage,
            contentRegistry = context.contentRegistry
        )
        val requestedDamage = MutationSystem.applyDamageTakenModifiers(
            target = target,
            sourceAffinities = sourceAffinities,
            baseDamage = formationAdjustedDamage,
            contentRegistry = context.contentRegistry
        )
        val currentHealth = target.combatant.currentHealth
        val appliedDamage =
            if (requestedDamage > currentHealth) currentHealth else requestedDamage

        val updatedEnemy = target.copy(
            combatant = target.combatant.copy(
                currentHealth = currentHealth - appliedDamage
            )
        )

        return EnemyDamageResult(
            enemy = updatedEnemy,
            event = DamageDealt(
                sourceInstanceId = sourceInstanceId,
                targetInstanceId = target.instanceId,
                amount = appliedDamage,
                damageKindId = effect.damageKind.id,
                critical = critical
            )
        )
    }

    private fun formationAdjustedDamage(
        state: com.idlerpg.game.domain.model.GameState,
        target: EnemyState,
        sourceAffinities: Set<Affinity>,
        damage: GameNumber,
        contentRegistry: ContentRegistry
    ): GameNumber {
        var adjusted = damage
        val targetRole = contentRegistry.enemy(target.definitionId).role
        val protectorPresent = state.run.combat.enemies.any { enemy ->
            enemy.instanceId != target.instanceId &&
                enemy.combatant.currentHealth > GameNumber.ZERO &&
                contentRegistry.enemy(enemy.definitionId).role == EnemyRole.PROTECTOR
        }
        if (protectorPresent && targetRole != EnemyRole.PROTECTOR) {
            adjusted = GameMath.applyRatio(adjusted, Ratio.ofUnits(6_500L))
        }
        val recentAffinityId = state.run.resonance.sequence.affinityIds.lastOrNull()
        if (targetRole == EnemyRole.ADAPTIVE &&
            recentAffinityId != null &&
            sourceAffinities.any { it.id == recentAffinityId }
        ) {
            adjusted = GameMath.applyRatio(adjusted, Ratio.ofUnits(7_000L))
        }
        return adjusted
    }
    private fun conditionApplies(
        state: com.idlerpg.game.domain.model.GameState,
        target: EnemyState,
        condition: EffectSpec.DamageCondition,
        contentRegistry: ContentRegistry
    ): Boolean = when (condition) {
        is EffectSpec.DamageCondition.TargetHasStatus ->
            target.combatant.statusEffects.any {
                it.definitionId == condition.statusDefinitionId
            }
        is EffectSpec.DamageCondition.TargetHealthAtOrBelow -> {
            val encounter = state.run.world.currentEncounter
            val region = encounter?.let {
                contentRegistry.region(contentRegistry.encounter(it.definitionId).regionId)
            }
            val maximum = EnemyScalingSystem.scaledHealth(
                enemyDefinition = contentRegistry.enemy(target.definitionId),
                regionDefinition = region,
                scalingTier = target.scalingTier
            )
            target.combatant.currentHealth.toBigInteger()
                .multiply(BigInteger.valueOf(Ratio.UNITS_PER_ONE)) <=
                maximum.toBigInteger().multiply(BigInteger.valueOf(condition.threshold.units))
        }
    }

    private fun damageTakenRatio(
        target: EnemyState,
        contentRegistry: ContentRegistry
    ): Ratio = target.combatant.statusEffects
            .sortedBy { it.instanceId }
            .fold(Ratio.ONE) { total, status ->
                val statusBonus = contentRegistry.status(status.definitionId).modifiers
                    .fold(Ratio.ZERO) { modifierTotal, modifier ->
                        when (modifier) {
                            is com.idlerpg.game.domain.definition.combat.StatusModifierDefinition.DamageTakenBonus ->
                                GameMath.ratioAfterSteps(
                                    base = modifierTotal,
                                    growthPerStep = modifier.ratioPerStack,
                                    steps = status.stackCount.toLong()
                                )
                            else -> modifierTotal
                        }
                    }
                GameMath.ratioAfterSteps(
                    base = total,
                    growthPerStep = statusBonus,
                    steps = 1L
                )
            }

    fun dealToPlayer(
        state: com.idlerpg.game.domain.model.GameState,
        sourceInstanceId: InstanceId,
        baseDamage: GameNumber,
        armorPenetration: GameNumber,
        damageKindId: com.idlerpg.game.core.id.ContentId,
        contentRegistry: ContentRegistry
    ): PlayerDamageResult {
        val target = state.run.combat.playerCombatant
            ?: error("Enemy damage requires an active player combatant")
        val pressuredDamage = target.statusEffects.sortedBy { it.instanceId }.fold(baseDamage) { damage, status ->
            contentRegistry.status(status.definitionId).modifiers.fold(damage) { adjusted, modifier ->
                when (modifier) {
                    is com.idlerpg.game.domain.definition.combat.StatusModifierDefinition.IncomingDamageMultiplier ->
                        (1..status.stackCount).fold(adjusted) { stacked, _ ->
                            GameMath.applyRatio(stacked, modifier.multiplierPerStack)
                        }
                    else -> adjusted
                }
            }
        }
        val armor = com.idlerpg.game.domain.system.stats.DerivedStatSystem.armor(state, contentRegistry)
        val requested = CombatMath.mitigate(pressuredDamage, armor, armorPenetration)
        val applied = if (requested > target.currentHealth) target.currentHealth else requested
        val updatedCombatant = target.copy(currentHealth = target.currentHealth - applied)
        val updatedState = state.copy(
            run = state.run.copy(
                player = state.run.player.copy(currentHealth = updatedCombatant.currentHealth),
                combat = state.run.combat.copy(playerCombatant = updatedCombatant)
            )
        )
        return PlayerDamageResult(
            state = updatedState,
            event = DamageDealt(
                sourceInstanceId = sourceInstanceId,
                targetInstanceId = target.instanceId,
                amount = applied,
                damageKindId = damageKindId,
                critical = false
            )
        )
    }


}
