package com.idlerpg.game.domain.system.doctrine

import com.idlerpg.game.core.id.ContentId
import com.idlerpg.game.core.id.InstanceId
import com.idlerpg.game.core.number.GameNumber
import com.idlerpg.game.core.number.Ratio
import com.idlerpg.game.data.content.ContentRegistry
import com.idlerpg.game.domain.command.CommandRejectionCode
import com.idlerpg.game.domain.command.CommandRejectionReason
import com.idlerpg.game.domain.definition.Affinity
import com.idlerpg.game.domain.engine.EngineContext
import com.idlerpg.game.domain.event.DoctrineActionRejected
import com.idlerpg.game.domain.event.DoctrineFallbackUsed
import com.idlerpg.game.domain.event.DoctrineRuleSelected
import com.idlerpg.game.domain.event.DoctrineRuleEvaluated
import com.idlerpg.game.domain.event.GameEvent
import com.idlerpg.game.domain.model.GameState
import com.idlerpg.game.domain.model.combat.CombatStatus
import com.idlerpg.game.domain.system.skill.SkillValidationSystem
import com.idlerpg.game.domain.model.combat.StatusEffectState
import com.idlerpg.game.domain.model.doctrine.DoctrineAction
import com.idlerpg.game.domain.model.doctrine.DoctrineComparison
import com.idlerpg.game.domain.model.doctrine.DoctrineCondition
import com.idlerpg.game.domain.model.doctrine.DoctrinePredicate
import com.idlerpg.game.domain.model.doctrine.DoctrineStatusTarget
import com.idlerpg.game.domain.system.combat.TargetingSystem
import com.idlerpg.game.domain.system.enemy.EnemyScalingSystem
import java.math.BigInteger
import com.idlerpg.game.domain.definition.enemy.EnemyRole
import com.idlerpg.game.domain.definition.world.EncounterType

/** Deterministic selected action plus Doctrine debug/domain events. */
data class DoctrineEvaluationResult(
    val action: DoctrineAction,
    val selectedRuleId: InstanceId? = null,
    val events: List<GameEvent> = emptyList()
)

/**
 * Pure deterministic Doctrine rule evaluator.
 *
 * Rules are read first-to-last. A true condition with an illegal action emits
 * DoctrineActionRejected and evaluation continues. The first true + legal action wins.
 * If Doctrine is disabled or no rule wins, Basic Attack is selected deterministically.
 * No RNG is consumed.
 */
object DoctrineEvaluator {

    fun selectAction(
        state: GameState,
        context: EngineContext
    ): DoctrineEvaluationResult {
        val doctrine = state.run.doctrine
        val events = mutableListOf<GameEvent>()

        if (doctrine.enabled) {
            for (rule in doctrine.rules) {
                if (!rule.enabled) {
                    continue
                }
                val matched = evaluateCondition(state, rule.condition, context)
                val explanation = explainCondition(state, rule.condition, context)
                events += DoctrineRuleEvaluated(rule.instanceId, matched, explanation)
                if (!matched) {
                    continue
                }

                val rejection = actionRejectionReason(
                    state = state,
                    action = rule.action,
                    context = context
                )

                if (rejection != null) {
                    events += DoctrineActionRejected(
                        ruleId = rule.instanceId,
                        actionId = actionId(rule.action, context.contentRegistry),
                        reason = rejection
                    )
                    continue
                }

                events += DoctrineRuleSelected(
                    ruleId = rule.instanceId,
                    actionId = actionId(rule.action, context.contentRegistry), explanation = explanation
                )
                return DoctrineEvaluationResult(
                    action = rule.action,
                    selectedRuleId = rule.instanceId,
                    events = events
                )
            }
        }

        val fallback = DoctrineAction.UseBasicAttack
        events += DoctrineFallbackUsed(
            actionId = context.contentRegistry.basicAttack.id,
            explanation = "No enabled legal rule matched; using Basic Attack"
        )
        return DoctrineEvaluationResult(
            action = fallback,
            selectedRuleId = null,
            events = events
        )
    }

    fun evaluateCondition(
        state: GameState,
        condition: DoctrineCondition,
        context: EngineContext
    ): Boolean =
        when (condition) {
            is DoctrineCondition.All ->
                condition.conditions.isNotEmpty() &&
                    condition.conditions.all { evaluateCondition(state, it, context) }

            is DoctrineCondition.Any ->
                condition.conditions.isNotEmpty() &&
                    condition.conditions.any { evaluateCondition(state, it, context) }

            is DoctrineCondition.Not ->
                !evaluateCondition(state, condition.condition, context)

            is DoctrineCondition.Predicate ->
                evaluatePredicate(state, condition.predicate, context)
        }

    fun actionRejectionReason(
        state: GameState,
        action: DoctrineAction,
        context: EngineContext
    ): CommandRejectionReason? {
        if (state.run.combat.status != CombatStatus.ACTIVE) {
            return CommandRejectionReason(CommandRejectionCode.INVALID_STATE)
        }
        if (TargetingSystem.primaryLivingEnemy(state.run.combat) == null) {
            return CommandRejectionReason(CommandRejectionCode.INVALID_STATE)
        }

        return when (action) {
            DoctrineAction.UseBasicAttack -> null

            is DoctrineAction.UseSkill ->
                SkillValidationSystem.rejectionReason(
                    state = state,
                    skillId = action.skillId,
                    contentRegistry = context.contentRegistry
                )
        }
    }

    private fun evaluatePredicate(
        state: GameState,
        predicate: DoctrinePredicate,
        context: EngineContext
    ): Boolean {
        return when (predicate) {
            is DoctrinePredicate.PlayerHealthPercent -> {
                val maximum = com.idlerpg.game.domain.system.stats.DerivedStatSystem.maximumHealth(
                    state,
                    context.contentRegistry
                )
                maximum > GameNumber.ZERO && compare(Ratio.ofUnits(state.run.player.currentHealth.toBigInteger()
                    .multiply(BigInteger.valueOf(Ratio.UNITS_PER_ONE)).divide(maximum.toBigInteger())
                    .min(BigInteger.valueOf(Ratio.UNITS_PER_ONE)).longValueExact()), predicate.threshold, predicate.comparison)
            }
            is DoctrinePredicate.EnemyCount -> compare(GameNumber.of(state.run.combat.enemies.count {
                it.combatant.currentHealth > GameNumber.ZERO }.toLong()), predicate.amount, predicate.comparison)
            is DoctrinePredicate.EnemyRolePresent -> state.run.combat.enemies.any { it.combatant.currentHealth > GameNumber.ZERO &&
                context.contentRegistry.enemy(it.definitionId).role == predicate.role }
            DoctrinePredicate.ElitePresent -> state.run.world.currentEncounter?.definitionId
                ?.let(context.contentRegistry::encounterOrNull)?.type == EncounterType.ELITE
            DoctrinePredicate.BossPresent -> state.run.world.currentEncounter?.definitionId
                ?.let(context.contentRegistry::encounterOrNull)?.bossId != null || state.run.combat.enemies.any {
                it.combatant.currentHealth > GameNumber.ZERO && context.contentRegistry.enemy(it.definitionId).role == EnemyRole.BOSS }
            is DoctrinePredicate.EnemyHealthPercent -> {
                val hp = primaryEnemyHealthRatio(state, context)
                hp != null && compare(hp, predicate.threshold, predicate.comparison)
            }

            is DoctrinePredicate.SkillReady ->
                SkillValidationSystem.rejectionReason(
                    state = state,
                    skillId = predicate.skillId,
                    contentRegistry = context.contentRegistry
                ) == null

            is DoctrinePredicate.ResonanceCharge -> {
                val current = state.run.resonance.chargeByAffinityId[predicate.affinity.id]
                    ?: GameNumber.ZERO
                compare(current, predicate.amount, predicate.comparison)
            }

            is DoctrinePredicate.SequenceSuffix -> {
                if (predicate.affinities.isEmpty()) {
                    false
                } else {
                    val expected = predicate.affinities.map(Affinity::id)
                    val actual = state.run.resonance.sequence.affinityIds
                    actual.size >= expected.size &&
                        actual.takeLast(expected.size) == expected
                }
            }

            is DoctrinePredicate.StatusPresent -> {
                val statuses = statusesForTarget(state, predicate.target)
                statuses != null &&
                    statuses.any { it.definitionId == predicate.statusDefinitionId }
            }

            is DoctrinePredicate.StatusAbsent -> {
                val statuses = statusesForTarget(state, predicate.target)
                statuses != null &&
                    statuses.none { it.definitionId == predicate.statusDefinitionId }
            }
        }
    }

    fun explainCondition(state: GameState, condition: DoctrineCondition, context: EngineContext): String = when (condition) {
        is DoctrineCondition.All -> condition.conditions.joinToString(" + ") { explainCondition(state, it, context) }
        is DoctrineCondition.Any -> condition.conditions.joinToString(" or ") { explainCondition(state, it, context) }
        is DoctrineCondition.Not -> "not (${explainCondition(state, condition.condition, context)})"
        is DoctrineCondition.Predicate -> {
            val label = when (val p = condition.predicate) {
                is DoctrinePredicate.PlayerHealthPercent -> "player HP threshold"; is DoctrinePredicate.EnemyCount -> "enemy count ${p.amount}"
                is DoctrinePredicate.EnemyRolePresent -> "${p.role.name.lowercase()} present"; DoctrinePredicate.ElitePresent -> "elite present"
                DoctrinePredicate.BossPresent -> "boss present"; is DoctrinePredicate.EnemyHealthPercent -> "enemy HP threshold"
                is DoctrinePredicate.SkillReady -> "${p.skillId} ready"; is DoctrinePredicate.ResonanceCharge -> "${p.affinity.name.lowercase()} charge"
                is DoctrinePredicate.SequenceSuffix -> "sequence suffix"; is DoctrinePredicate.StatusPresent -> "status present"; is DoctrinePredicate.StatusAbsent -> "status absent"
            }
            "$label ${if (evaluatePredicate(state, condition.predicate, context)) "matched" else "failed"}"
        }
    }

    private fun statusesForTarget(
        state: GameState,
        target: DoctrineStatusTarget
    ): List<StatusEffectState>? =
        when (target) {
            DoctrineStatusTarget.PLAYER ->
                state.run.combat.playerCombatant?.statusEffects

            DoctrineStatusTarget.PRIMARY_ENEMY ->
                TargetingSystem.primaryLivingEnemy(state.run.combat)
                    ?.combatant
                    ?.statusEffects
        }

    private fun primaryEnemyHealthRatio(
        state: GameState,
        context: EngineContext
    ): Ratio? {
        val enemy = TargetingSystem.primaryLivingEnemy(state.run.combat) ?: return null
        val definition = context.contentRegistry.enemyOrNull(enemy.definitionId) ?: return null
        val region = state.run.world.activeRegionId?.let(context.contentRegistry::regionOrNull)
        val maximumHealth = EnemyScalingSystem.scaledHealth(
            enemyDefinition = definition,
            regionDefinition = region,
            scalingTier = enemy.scalingTier
        )
        if (maximumHealth == GameNumber.ZERO) {
            return null
        }

        val numerator = enemy.combatant.currentHealth.toBigInteger()
            .multiply(BigInteger.valueOf(Ratio.UNITS_PER_ONE))
        var units = numerator.divide(maximumHealth.toBigInteger())
        val maximumUnits = BigInteger.valueOf(Ratio.UNITS_PER_ONE)
        if (units > maximumUnits) {
            units = maximumUnits
        }
        return Ratio.ofUnits(units.longValueExact())
    }

    private fun compare(
        left: Ratio,
        right: Ratio,
        comparison: DoctrineComparison
    ): Boolean = compareResult(left.compareTo(right), comparison)

    private fun compare(
        left: GameNumber,
        right: GameNumber,
        comparison: DoctrineComparison
    ): Boolean = compareResult(left.compareTo(right), comparison)

    private fun compareResult(
        result: Int,
        comparison: DoctrineComparison
    ): Boolean =
        when (comparison) {
            DoctrineComparison.LESS_THAN -> result < 0
            DoctrineComparison.LESS_THAN_OR_EQUAL -> result <= 0
            DoctrineComparison.EQUAL -> result == 0
            DoctrineComparison.GREATER_THAN_OR_EQUAL -> result >= 0
            DoctrineComparison.GREATER_THAN -> result > 0
        }

    private fun actionId(
        action: DoctrineAction,
        contentRegistry: ContentRegistry
    ): ContentId =
        when (action) {
            DoctrineAction.UseBasicAttack -> contentRegistry.basicAttack.id
            is DoctrineAction.UseSkill -> action.skillId
        }
}
