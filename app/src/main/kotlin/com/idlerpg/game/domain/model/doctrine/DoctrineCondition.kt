package com.idlerpg.game.domain.model.doctrine

import com.idlerpg.game.core.id.ContentId
import com.idlerpg.game.core.number.GameNumber
import com.idlerpg.game.core.number.Ratio
import com.idlerpg.game.domain.definition.Affinity
import com.idlerpg.game.domain.definition.enemy.EnemyRole

/** Logical comparison vocabulary interpreted by DoctrineEvaluator. */
enum class DoctrineComparison {
    LESS_THAN,
    LESS_THAN_OR_EQUAL,
    EQUAL,
    GREATER_THAN_OR_EQUAL,
    GREATER_THAN
}

/** Status-effect subject available to Foundation 9 predicates. */
enum class DoctrineStatusTarget {
    PLAYER,
    PRIMARY_ENEMY
}

/**
 * Bounded condition expression tree.
 *
 * Foundation 9 implements the architecture's ALL / ANY / NOT / PREDICATE operators.
 * Maximum depth is enforced by DoctrineValidator rather than allowing arbitrary scripts.
 */
sealed interface DoctrineCondition {

    data class All(
        val conditions: List<DoctrineCondition>
    ) : DoctrineCondition

    data class Any(
        val conditions: List<DoctrineCondition>
    ) : DoctrineCondition

    data class Not(
        val condition: DoctrineCondition
    ) : DoctrineCondition

    data class Predicate(
        val predicate: DoctrinePredicate
    ) : DoctrineCondition
}

/** Required first Foundation 9 predicate families. */
sealed interface DoctrinePredicate {
    data class PlayerHealthPercent(val comparison: DoctrineComparison, val threshold: Ratio) : DoctrinePredicate
    data class EnemyCount(val comparison: DoctrineComparison, val amount: GameNumber) : DoctrinePredicate
    data class EnemyRolePresent(val role: EnemyRole) : DoctrinePredicate
    data object ElitePresent : DoctrinePredicate
    data object BossPresent : DoctrinePredicate

    /** Compare primary living enemy HP percentage against a fixed-point threshold. */
    data class EnemyHealthPercent(
        val comparison: DoctrineComparison,
        val threshold: Ratio
    ) : DoctrinePredicate

    /** True only when the authored skill exists and its stored cooldown deadline is ready. */
    data class SkillReady(
        val skillId: ContentId
    ) : DoctrinePredicate

    /** Compare current run-local Resonance charge for one canonical affinity. */
    data class ResonanceCharge(
        val affinity: Affinity,
        val comparison: DoctrineComparison,
        val amount: GameNumber
    ) : DoctrinePredicate

    /** Match an ordered suffix of the current bounded Resonance sequence. */
    data class SequenceSuffix(
        val affinities: List<Affinity>
    ) : DoctrinePredicate

    /** True when the selected combatant currently carries the status definition ID. */
    data class StatusPresent(
        val target: DoctrineStatusTarget,
        val statusDefinitionId: ContentId
    ) : DoctrinePredicate

    /** True when the selected combatant exists and lacks the status definition ID. */
    data class StatusAbsent(
        val target: DoctrineStatusTarget,
        val statusDefinitionId: ContentId
    ) : DoctrinePredicate
}
