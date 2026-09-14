package com.idlerpg.game.presentation.doctrine

import com.idlerpg.game.core.id.ContentId
import com.idlerpg.game.core.number.GameNumber
import com.idlerpg.game.core.number.Ratio
import com.idlerpg.game.domain.definition.Affinity
import com.idlerpg.game.domain.definition.enemy.EnemyRole
import com.idlerpg.game.domain.model.doctrine.DoctrineAction
import com.idlerpg.game.domain.model.doctrine.DoctrineComparison
import com.idlerpg.game.domain.model.doctrine.DoctrineCondition
import com.idlerpg.game.domain.model.doctrine.DoctrinePredicate
import com.idlerpg.game.domain.model.doctrine.DoctrineRule
import com.idlerpg.game.domain.model.doctrine.DoctrineStatusTarget
import com.idlerpg.game.presentation.model.DoctrineActionDraftUiState
import com.idlerpg.game.presentation.model.DoctrineActionKindUi
import com.idlerpg.game.presentation.model.DoctrineComparisonUi
import com.idlerpg.game.presentation.model.DoctrineConditionDraftUiState
import com.idlerpg.game.presentation.model.DoctrineConditionKindUi
import com.idlerpg.game.presentation.model.DoctrineDraftErrorUi
import com.idlerpg.game.presentation.model.DoctrineRuleDraftUiState
import com.idlerpg.game.presentation.model.DoctrineStatusTargetUi

/** Zero-based child-index path into a presentation-only Doctrine condition draft. */
data class DoctrineDraftPath(
    val indices: List<Int> = emptyList()
)

/** Presentation-only draft edits. None of these are gameplay commands. */
sealed interface DoctrineDraftAction {
    data class SetDraftEnabled(val enabled: Boolean) : DoctrineDraftAction
    data class SetActionKind(val kind: DoctrineActionKindUi) : DoctrineDraftAction
    data class SetActionSkill(val skillId: ContentId) : DoctrineDraftAction
    data class SetConditionKind(
        val path: DoctrineDraftPath,
        val kind: DoctrineConditionKindUi
    ) : DoctrineDraftAction
    data class SetComparison(
        val path: DoctrineDraftPath,
        val comparison: DoctrineComparisonUi
    ) : DoctrineDraftAction
    data class AdjustEnemyHealthPercent(
        val path: DoctrineDraftPath,
        val delta: Int
    ) : DoctrineDraftAction
    data class SetPredicateSkill(
        val path: DoctrineDraftPath,
        val skillId: ContentId
    ) : DoctrineDraftAction
    data class SetAffinity(
        val path: DoctrineDraftPath,
        val affinityId: ContentId
    ) : DoctrineDraftAction
    data class AdjustResonanceAmount(
        val path: DoctrineDraftPath,
        val delta: Long
    ) : DoctrineDraftAction
    data class AppendSequenceAffinity(
        val path: DoctrineDraftPath,
        val affinityId: ContentId
    ) : DoctrineDraftAction
    data class RemoveLastSequenceAffinity(
        val path: DoctrineDraftPath
    ) : DoctrineDraftAction
    data class SetStatusTarget(
        val path: DoctrineDraftPath,
        val target: DoctrineStatusTargetUi
    ) : DoctrineDraftAction
    data class SetStatusDefinition(
        val path: DoctrineDraftPath,
        val statusId: ContentId
    ) : DoctrineDraftAction
    data class SetEnemyRole(val path: DoctrineDraftPath, val role: EnemyRole) : DoctrineDraftAction
    data class AddChild(val path: DoctrineDraftPath) : DoctrineDraftAction
    data class RemoveChild(
        val path: DoctrineDraftPath,
        val childIndex: Int
    ) : DoctrineDraftAction
}

data class DoctrineDraftDomainValue(
    val ruleId: com.idlerpg.game.core.id.InstanceId?,
    val enabled: Boolean,
    val condition: DoctrineCondition,
    val action: DoctrineAction
)

sealed interface DoctrineDraftConversionResult {
    data class Ready(val value: DoctrineDraftDomainValue) : DoctrineDraftConversionResult
    data class Invalid(val error: DoctrineDraftErrorUi) : DoctrineDraftConversionResult
}

/** Owns deterministic presentation-draft editing and domain conversion. */
object DoctrineDraftController {
    fun newDraft(): DoctrineRuleDraftUiState = DoctrineRuleDraftUiState()

    fun fromRule(rule: DoctrineRule): DoctrineRuleDraftUiState = DoctrineRuleDraftUiState(
        ruleId = rule.instanceId,
        enabled = rule.enabled,
        condition = fromCondition(rule.condition),
        action = fromAction(rule.action)
    )

    fun reduce(
        draft: DoctrineRuleDraftUiState,
        action: DoctrineDraftAction,
        maximumDepth: Int,
        sequenceCapacity: Int
    ): DoctrineRuleDraftUiState {
        require(maximumDepth > 0) { "maximumDepth must be positive" }
        require(sequenceCapacity > 0) { "sequenceCapacity must be positive" }

        val cleared = draft.copy(localError = null)
        return when (action) {
            is DoctrineDraftAction.SetDraftEnabled -> cleared.copy(enabled = action.enabled)
            is DoctrineDraftAction.SetActionKind -> cleared.copy(
                action = DoctrineActionDraftUiState(kind = action.kind)
            )
            is DoctrineDraftAction.SetActionSkill -> cleared.copy(
                action = DoctrineActionDraftUiState(
                    kind = DoctrineActionKindUi.SKILL,
                    skillId = action.skillId
                )
            )
            is DoctrineDraftAction.SetConditionKind -> cleared.copy(
                condition = updateNode(cleared.condition, action.path.indices) { node, depth ->
                    if (action.kind.isComposite && depth >= maximumDepth) {
                        node
                    } else {
                        defaultNode(action.kind)
                    }
                }
            )
            is DoctrineDraftAction.SetComparison -> cleared.copy(
                condition = updateNode(cleared.condition, action.path.indices) { node, _ ->
                    node.copy(comparison = action.comparison)
                }
            )
            is DoctrineDraftAction.AdjustEnemyHealthPercent -> cleared.copy(
                condition = updateNode(cleared.condition, action.path.indices) { node, _ ->
                    node.copy(
                        enemyHealthPercent = (node.enemyHealthPercent + action.delta)
                            .coerceIn(0, 100)
                    )
                }
            )
            is DoctrineDraftAction.SetPredicateSkill -> cleared.copy(
                condition = updateNode(cleared.condition, action.path.indices) { node, _ ->
                    node.copy(skillId = action.skillId)
                }
            )
            is DoctrineDraftAction.SetAffinity -> cleared.copy(
                condition = updateNode(cleared.condition, action.path.indices) { node, _ ->
                    node.copy(affinityId = action.affinityId)
                }
            )
            is DoctrineDraftAction.AdjustResonanceAmount -> cleared.copy(
                condition = updateNode(cleared.condition, action.path.indices) { node, _ ->
                    node.copy(
                        resonanceAmount = safeAdjust(node.resonanceAmount, action.delta)
                    )
                }
            )
            is DoctrineDraftAction.AppendSequenceAffinity -> cleared.copy(
                condition = updateNode(cleared.condition, action.path.indices) { node, _ ->
                    if (node.sequenceAffinityIds.size >= sequenceCapacity) {
                        node
                    } else {
                        node.copy(
                            sequenceAffinityIds = node.sequenceAffinityIds + action.affinityId
                        )
                    }
                }
            )
            is DoctrineDraftAction.RemoveLastSequenceAffinity -> cleared.copy(
                condition = updateNode(cleared.condition, action.path.indices) { node, _ ->
                    node.copy(sequenceAffinityIds = node.sequenceAffinityIds.dropLast(1))
                }
            )
            is DoctrineDraftAction.SetStatusTarget -> cleared.copy(
                condition = updateNode(cleared.condition, action.path.indices) { node, _ ->
                    node.copy(statusTarget = action.target)
                }
            )
            is DoctrineDraftAction.SetStatusDefinition -> cleared.copy(
                condition = updateNode(cleared.condition, action.path.indices) { node, _ ->
                    node.copy(statusDefinitionId = action.statusId)
                }
            )
            is DoctrineDraftAction.SetEnemyRole -> cleared.copy(condition = updateNode(cleared.condition, action.path.indices) { node, _ -> node.copy(enemyRole = action.role) })
            is DoctrineDraftAction.AddChild -> cleared.copy(
                condition = updateNode(cleared.condition, action.path.indices) { node, depth ->
                    if (depth >= maximumDepth ||
                        (node.kind != DoctrineConditionKindUi.ALL &&
                            node.kind != DoctrineConditionKindUi.ANY)
                    ) {
                        node
                    } else {
                        node.copy(children = node.children + defaultNode(
                            DoctrineConditionKindUi.ENEMY_HEALTH_PERCENT
                        ))
                    }
                }
            )
            is DoctrineDraftAction.RemoveChild -> cleared.copy(
                condition = updateNode(cleared.condition, action.path.indices) { node, _ ->
                    if ((node.kind == DoctrineConditionKindUi.ALL ||
                            node.kind == DoctrineConditionKindUi.ANY) &&
                        node.children.size > 1 &&
                        action.childIndex in node.children.indices
                    ) {
                        node.copy(
                            children = node.children.filterIndexed { index, _ ->
                                index != action.childIndex
                            }
                        )
                    } else {
                        node
                    }
                }
            )
        }
    }

    fun toDomain(
        draft: DoctrineRuleDraftUiState,
        maximumDepth: Int,
        sequenceCapacity: Int
    ): DoctrineDraftConversionResult {
        if (depth(draft.condition) > maximumDepth) {
            return DoctrineDraftConversionResult.Invalid(
                DoctrineDraftErrorUi.CONDITION_DEPTH_EXCEEDED
            )
        }

        val conditionResult = conditionToDomain(draft.condition, sequenceCapacity)
        if (conditionResult is ConditionConversion.Invalid) {
            return DoctrineDraftConversionResult.Invalid(conditionResult.error)
        }
        conditionResult as ConditionConversion.Ready

        val action = when (draft.action.kind) {
            DoctrineActionKindUi.BASIC_ATTACK -> DoctrineAction.UseBasicAttack
            DoctrineActionKindUi.SKILL -> {
                val skillId = draft.action.skillId
                    ?: return DoctrineDraftConversionResult.Invalid(
                        DoctrineDraftErrorUi.MISSING_SKILL
                    )
                DoctrineAction.UseSkill(skillId)
            }
        }

        return DoctrineDraftConversionResult.Ready(
            DoctrineDraftDomainValue(
                ruleId = draft.ruleId,
                enabled = draft.enabled,
                condition = conditionResult.condition,
                action = action
            )
        )
    }

    fun withError(
        draft: DoctrineRuleDraftUiState,
        error: DoctrineDraftErrorUi
    ): DoctrineRuleDraftUiState = draft.copy(localError = error)

    fun depth(condition: DoctrineConditionDraftUiState): Int = when (condition.kind) {
        DoctrineConditionKindUi.ALL,
        DoctrineConditionKindUi.ANY ->
            1 + (condition.children.maxOfOrNull(::depth) ?: 0)
        DoctrineConditionKindUi.NOT ->
            1 + (condition.children.firstOrNull()?.let(::depth) ?: 0)
        else -> 1
    }

    private fun defaultNode(kind: DoctrineConditionKindUi): DoctrineConditionDraftUiState =
        when (kind) {
            DoctrineConditionKindUi.ALL,
            DoctrineConditionKindUi.ANY,
            DoctrineConditionKindUi.NOT -> DoctrineConditionDraftUiState(
                kind = kind,
                children = listOf(
                    DoctrineConditionDraftUiState(
                        kind = DoctrineConditionKindUi.ENEMY_HEALTH_PERCENT
                    )
                )
            )
            DoctrineConditionKindUi.RESONANCE_CHARGE -> DoctrineConditionDraftUiState(
                kind = kind,
                affinityId = Affinity.MIGHT.id,
                resonanceAmount = 1L
            )
            DoctrineConditionKindUi.SEQUENCE_SUFFIX -> DoctrineConditionDraftUiState(
                kind = kind,
                sequenceAffinityIds = emptyList()
            )
            else -> DoctrineConditionDraftUiState(kind = kind)
        }

    private fun updateNode(
        root: DoctrineConditionDraftUiState,
        path: List<Int>,
        depth: Int = 1,
        transform: (DoctrineConditionDraftUiState, Int) -> DoctrineConditionDraftUiState
    ): DoctrineConditionDraftUiState {
        if (path.isEmpty()) {
            return transform(root, depth)
        }
        val index = path.first()
        if (index !in root.children.indices) {
            return root
        }
        val updatedChildren = root.children.toMutableList()
        updatedChildren[index] = updateNode(
            root = updatedChildren[index],
            path = path.drop(1),
            transform = transform,
            depth = depth + 1
        )
        return root.copy(children = updatedChildren)
    }

    private fun safeAdjust(current: Long, delta: Long): Long = when {
        delta > 0L && current > Long.MAX_VALUE - delta -> Long.MAX_VALUE
        delta < 0L && current < -delta -> 0L
        else -> (current + delta).coerceAtLeast(0L)
    }

    private sealed interface ConditionConversion {
        data class Ready(val condition: DoctrineCondition) : ConditionConversion
        data class Invalid(val error: DoctrineDraftErrorUi) : ConditionConversion
    }

    private fun conditionToDomain(
        draft: DoctrineConditionDraftUiState,
        sequenceCapacity: Int
    ): ConditionConversion {
        return when (draft.kind) {
        DoctrineConditionKindUi.ALL -> {
            val children = convertChildren(draft.children, sequenceCapacity)
            if (children is ChildrenConversion.Invalid) {
                ConditionConversion.Invalid(children.error)
            } else {
                children as ChildrenConversion.Ready
                ConditionConversion.Ready(DoctrineCondition.All(children.conditions))
            }
        }
        DoctrineConditionKindUi.ANY -> {
            val children = convertChildren(draft.children, sequenceCapacity)
            if (children is ChildrenConversion.Invalid) {
                ConditionConversion.Invalid(children.error)
            } else {
                children as ChildrenConversion.Ready
                ConditionConversion.Ready(DoctrineCondition.Any(children.conditions))
            }
        }
        DoctrineConditionKindUi.NOT -> {
            val child = draft.children.firstOrNull()
                ?: return ConditionConversion.Invalid(
                    DoctrineDraftErrorUi.CONDITION_DEPTH_EXCEEDED
                )
            when (val converted = conditionToDomain(child, sequenceCapacity)) {
                is ConditionConversion.Invalid -> converted
                is ConditionConversion.Ready -> ConditionConversion.Ready(
                    DoctrineCondition.Not(converted.condition)
                )
            }
        }
        DoctrineConditionKindUi.PLAYER_HEALTH_PERCENT -> ConditionConversion.Ready(DoctrineCondition.Predicate(DoctrinePredicate.PlayerHealthPercent(draft.comparison.toDomain(), Ratio.ofUnits(draft.enemyHealthPercent.toLong() * 100L))))
        DoctrineConditionKindUi.ENEMY_COUNT -> ConditionConversion.Ready(DoctrineCondition.Predicate(DoctrinePredicate.EnemyCount(draft.comparison.toDomain(), GameNumber.of(draft.resonanceAmount.coerceIn(1L, 5L)))))
        DoctrineConditionKindUi.ENEMY_ROLE_PRESENT -> ConditionConversion.Ready(DoctrineCondition.Predicate(DoctrinePredicate.EnemyRolePresent(draft.enemyRole)))
        DoctrineConditionKindUi.ELITE_PRESENT -> ConditionConversion.Ready(DoctrineCondition.Predicate(DoctrinePredicate.ElitePresent))
        DoctrineConditionKindUi.BOSS_PRESENT -> ConditionConversion.Ready(DoctrineCondition.Predicate(DoctrinePredicate.BossPresent))
        DoctrineConditionKindUi.ENEMY_HEALTH_PERCENT -> ConditionConversion.Ready(
            DoctrineCondition.Predicate(
                DoctrinePredicate.EnemyHealthPercent(
                    comparison = draft.comparison.toDomain(),
                    threshold = Ratio.ofUnits(draft.enemyHealthPercent.toLong() * 100L)
                )
            )
        )
        DoctrineConditionKindUi.SKILL_READY -> {
            val skillId = draft.skillId
                ?: return ConditionConversion.Invalid(DoctrineDraftErrorUi.MISSING_SKILL)
            ConditionConversion.Ready(
                DoctrineCondition.Predicate(DoctrinePredicate.SkillReady(skillId))
            )
        }
        DoctrineConditionKindUi.RESONANCE_CHARGE -> {
            val affinity = draft.affinityId.toAffinityOrNull()
                ?: return ConditionConversion.Invalid(DoctrineDraftErrorUi.INVALID_AFFINITY)
            ConditionConversion.Ready(
                DoctrineCondition.Predicate(
                    DoctrinePredicate.ResonanceCharge(
                        affinity = affinity,
                        comparison = draft.comparison.toDomain(),
                        amount = GameNumber.of(draft.resonanceAmount)
                    )
                )
            )
        }
        DoctrineConditionKindUi.SEQUENCE_SUFFIX -> {
            if (draft.sequenceAffinityIds.isEmpty()) {
                return ConditionConversion.Invalid(DoctrineDraftErrorUi.EMPTY_SEQUENCE)
            }
            if (draft.sequenceAffinityIds.size > sequenceCapacity) {
                return ConditionConversion.Invalid(
                    DoctrineDraftErrorUi.CONDITION_DEPTH_EXCEEDED
                )
            }
            val affinities = draft.sequenceAffinityIds.map { id ->
                id.toAffinityOrNull()
                    ?: return ConditionConversion.Invalid(DoctrineDraftErrorUi.INVALID_AFFINITY)
            }
            ConditionConversion.Ready(
                DoctrineCondition.Predicate(DoctrinePredicate.SequenceSuffix(affinities))
            )
        }
        DoctrineConditionKindUi.STATUS_PRESENT,
        DoctrineConditionKindUi.STATUS_ABSENT -> {
            val statusId = draft.statusDefinitionId
                ?: return ConditionConversion.Invalid(DoctrineDraftErrorUi.MISSING_STATUS)
            val target = draft.statusTarget.toDomain()
            val predicate = if (draft.kind == DoctrineConditionKindUi.STATUS_PRESENT) {
                DoctrinePredicate.StatusPresent(target, statusId)
            } else {
                DoctrinePredicate.StatusAbsent(target, statusId)
            }
            ConditionConversion.Ready(DoctrineCondition.Predicate(predicate))
        }
        }
    }

    private sealed interface ChildrenConversion {
        data class Ready(val conditions: List<DoctrineCondition>) : ChildrenConversion
        data class Invalid(val error: DoctrineDraftErrorUi) : ChildrenConversion
    }

    private fun convertChildren(
        children: List<DoctrineConditionDraftUiState>,
        sequenceCapacity: Int
    ): ChildrenConversion {
        if (children.isEmpty()) {
            return ChildrenConversion.Invalid(DoctrineDraftErrorUi.CONDITION_DEPTH_EXCEEDED)
        }
        val converted = mutableListOf<DoctrineCondition>()
        for (child in children) {
            when (val result = conditionToDomain(child, sequenceCapacity)) {
                is ConditionConversion.Invalid -> return ChildrenConversion.Invalid(result.error)
                is ConditionConversion.Ready -> converted += result.condition
            }
        }
        return ChildrenConversion.Ready(converted)
    }

    private fun fromCondition(condition: DoctrineCondition): DoctrineConditionDraftUiState =
        when (condition) {
            is DoctrineCondition.All -> DoctrineConditionDraftUiState(
                kind = DoctrineConditionKindUi.ALL,
                children = condition.conditions.map(::fromCondition)
            )
            is DoctrineCondition.Any -> DoctrineConditionDraftUiState(
                kind = DoctrineConditionKindUi.ANY,
                children = condition.conditions.map(::fromCondition)
            )
            is DoctrineCondition.Not -> DoctrineConditionDraftUiState(
                kind = DoctrineConditionKindUi.NOT,
                children = listOf(fromCondition(condition.condition))
            )
            is DoctrineCondition.Predicate -> fromPredicate(condition.predicate)
        }

    private fun fromPredicate(predicate: DoctrinePredicate): DoctrineConditionDraftUiState =
        when (predicate) {
            is DoctrinePredicate.PlayerHealthPercent -> DoctrineConditionDraftUiState(kind = DoctrineConditionKindUi.PLAYER_HEALTH_PERCENT, comparison = predicate.comparison.toUi(), enemyHealthPercent = (predicate.threshold.units / 100L).toInt().coerceIn(0, 100))
            is DoctrinePredicate.EnemyCount -> DoctrineConditionDraftUiState(kind = DoctrineConditionKindUi.ENEMY_COUNT, comparison = predicate.comparison.toUi(), resonanceAmount = predicate.amount.toBigInteger().toLong().coerceIn(1L, 5L))
            is DoctrinePredicate.EnemyRolePresent -> DoctrineConditionDraftUiState(kind = DoctrineConditionKindUi.ENEMY_ROLE_PRESENT, enemyRole = predicate.role)
            DoctrinePredicate.ElitePresent -> DoctrineConditionDraftUiState(kind = DoctrineConditionKindUi.ELITE_PRESENT)
            DoctrinePredicate.BossPresent -> DoctrineConditionDraftUiState(kind = DoctrineConditionKindUi.BOSS_PRESENT)
            is DoctrinePredicate.EnemyHealthPercent -> DoctrineConditionDraftUiState(
                kind = DoctrineConditionKindUi.ENEMY_HEALTH_PERCENT,
                comparison = predicate.comparison.toUi(),
                enemyHealthPercent = (predicate.threshold.units / 100L).toInt().coerceIn(0, 100)
            )
            is DoctrinePredicate.SkillReady -> DoctrineConditionDraftUiState(
                kind = DoctrineConditionKindUi.SKILL_READY,
                skillId = predicate.skillId
            )
            is DoctrinePredicate.ResonanceCharge -> DoctrineConditionDraftUiState(
                kind = DoctrineConditionKindUi.RESONANCE_CHARGE,
                comparison = predicate.comparison.toUi(),
                affinityId = predicate.affinity.id,
                resonanceAmount = predicate.amount.toBigInteger()
                    .coerceAtMost(java.math.BigInteger.valueOf(Long.MAX_VALUE))
                    .toLong()
            )
            is DoctrinePredicate.SequenceSuffix -> DoctrineConditionDraftUiState(
                kind = DoctrineConditionKindUi.SEQUENCE_SUFFIX,
                sequenceAffinityIds = predicate.affinities.map(Affinity::id)
            )
            is DoctrinePredicate.StatusPresent -> DoctrineConditionDraftUiState(
                kind = DoctrineConditionKindUi.STATUS_PRESENT,
                statusTarget = predicate.target.toUi(),
                statusDefinitionId = predicate.statusDefinitionId
            )
            is DoctrinePredicate.StatusAbsent -> DoctrineConditionDraftUiState(
                kind = DoctrineConditionKindUi.STATUS_ABSENT,
                statusTarget = predicate.target.toUi(),
                statusDefinitionId = predicate.statusDefinitionId
            )
        }

    private fun fromAction(action: DoctrineAction): DoctrineActionDraftUiState =
        when (action) {
            DoctrineAction.UseBasicAttack -> DoctrineActionDraftUiState(
                kind = DoctrineActionKindUi.BASIC_ATTACK
            )
            is DoctrineAction.UseSkill -> DoctrineActionDraftUiState(
                kind = DoctrineActionKindUi.SKILL,
                skillId = action.skillId
            )
        }

    private val DoctrineConditionKindUi.isComposite: Boolean
        get() = this == DoctrineConditionKindUi.ALL ||
            this == DoctrineConditionKindUi.ANY ||
            this == DoctrineConditionKindUi.NOT

    private fun DoctrineComparisonUi.toDomain(): DoctrineComparison = when (this) {
        DoctrineComparisonUi.LESS_THAN -> DoctrineComparison.LESS_THAN
        DoctrineComparisonUi.LESS_THAN_OR_EQUAL -> DoctrineComparison.LESS_THAN_OR_EQUAL
        DoctrineComparisonUi.EQUAL -> DoctrineComparison.EQUAL
        DoctrineComparisonUi.GREATER_THAN_OR_EQUAL -> DoctrineComparison.GREATER_THAN_OR_EQUAL
        DoctrineComparisonUi.GREATER_THAN -> DoctrineComparison.GREATER_THAN
    }

    private fun DoctrineComparison.toUi(): DoctrineComparisonUi = when (this) {
        DoctrineComparison.LESS_THAN -> DoctrineComparisonUi.LESS_THAN
        DoctrineComparison.LESS_THAN_OR_EQUAL -> DoctrineComparisonUi.LESS_THAN_OR_EQUAL
        DoctrineComparison.EQUAL -> DoctrineComparisonUi.EQUAL
        DoctrineComparison.GREATER_THAN_OR_EQUAL -> DoctrineComparisonUi.GREATER_THAN_OR_EQUAL
        DoctrineComparison.GREATER_THAN -> DoctrineComparisonUi.GREATER_THAN
    }

    private fun DoctrineStatusTargetUi.toDomain(): DoctrineStatusTarget = when (this) {
        DoctrineStatusTargetUi.PLAYER -> DoctrineStatusTarget.PLAYER
        DoctrineStatusTargetUi.PRIMARY_ENEMY -> DoctrineStatusTarget.PRIMARY_ENEMY
    }

    private fun DoctrineStatusTarget.toUi(): DoctrineStatusTargetUi = when (this) {
        DoctrineStatusTarget.PLAYER -> DoctrineStatusTargetUi.PLAYER
        DoctrineStatusTarget.PRIMARY_ENEMY -> DoctrineStatusTargetUi.PRIMARY_ENEMY
    }

    private fun ContentId?.toAffinityOrNull(): Affinity? =
        this?.let { id -> Affinity.values().firstOrNull { it.id == id } }
}
