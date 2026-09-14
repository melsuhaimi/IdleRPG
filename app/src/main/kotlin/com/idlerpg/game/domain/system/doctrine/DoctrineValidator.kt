package com.idlerpg.game.domain.system.doctrine

import com.idlerpg.game.core.id.ContentId
import com.idlerpg.game.core.id.InstanceId
import com.idlerpg.game.core.validation.ValidationIssue
import com.idlerpg.game.core.validation.ValidationResult
import com.idlerpg.game.core.validation.ValidationSeverity
import com.idlerpg.game.data.content.ContentRegistry
import com.idlerpg.game.domain.command.CommandRejectionCode
import com.idlerpg.game.domain.command.CommandRejectionReason
import com.idlerpg.game.domain.model.doctrine.DoctrineAction
import com.idlerpg.game.domain.model.doctrine.DoctrineCondition
import com.idlerpg.game.domain.model.doctrine.DoctrinePredicate
import com.idlerpg.game.domain.model.doctrine.DoctrineState

/** Structured validation report used before a Doctrine command is committed. */
data class DoctrineValidationReport(
    val validation: ValidationResult,
    val rejectionReason: CommandRejectionReason? = null
) {
    val isValid: Boolean
        get() = validation.isValid
}

/**
 * Structural/semantic Doctrine validation.
 *
 * Foundation 9 validates only capabilities that legally exist now. Status definitions do
 * not exist until Foundation 12, so status predicates validate stable ID shape through
 * ContentId itself but cannot yet require a registered StatusEffectDefinition.
 */
object DoctrineValidator {

    fun validate(
        doctrine: DoctrineState,
        contentRegistry: ContentRegistry,
        ruleCapacity: Int,
        maximumConditionDepth: Int,
        maximumSequenceSuffixLength: Int
    ): DoctrineValidationReport {
        require(ruleCapacity > 0) { "ruleCapacity must be positive" }
        require(maximumConditionDepth > 0) { "maximumConditionDepth must be positive" }
        require(maximumSequenceSuffixLength > 0) {
            "maximumSequenceSuffixLength must be positive"
        }

        val issues = mutableListOf<ValidationIssue>()
        var firstRejection: CommandRejectionReason? = null

        fun record(
            code: String,
            path: String,
            message: String,
            rejection: CommandRejectionReason
        ) {
            issues += ValidationIssue(
                code = code,
                severity = ValidationSeverity.ERROR,
                path = path,
                message = message
            )
            if (firstRejection == null) {
                firstRejection = rejection
            }
        }

        if (doctrine.rules.size > ruleCapacity) {
            record(
                code = "doctrine.rule_capacity_exceeded",
                path = "doctrine.rules",
                message = "Rule count ${doctrine.rules.size} exceeds capacity $ruleCapacity",
                rejection = CommandRejectionReason(
                    code = CommandRejectionCode.CAPACITY_EXCEEDED
                )
            )
        }

        val seenIds = mutableSetOf<InstanceId>()
        doctrine.rules.forEachIndexed { index, rule ->
            val rulePath = "doctrine.rules[$index]"

            if (!seenIds.add(rule.instanceId)) {
                record(
                    code = "doctrine.duplicate_rule_id",
                    path = "$rulePath.instanceId",
                    message = "Duplicate Doctrine rule InstanceId ${rule.instanceId}",
                    rejection = CommandRejectionReason(
                        code = CommandRejectionCode.INVALID_ARGUMENT,
                        subjectInstanceId = rule.instanceId
                    )
                )
            }

            val depth = conditionDepth(rule.condition)
            if (depth > maximumConditionDepth) {
                record(
                    code = "doctrine.condition_depth_exceeded",
                    path = "$rulePath.condition",
                    message = "Condition depth $depth exceeds maximum $maximumConditionDepth",
                    rejection = CommandRejectionReason(
                        code = CommandRejectionCode.CAPACITY_EXCEEDED,
                        subjectInstanceId = rule.instanceId
                    )
                )
            }

            validateCondition(
                condition = rule.condition,
                path = "$rulePath.condition",
                contentRegistry = contentRegistry,
                maximumSequenceSuffixLength = maximumSequenceSuffixLength,
                record = ::record
            )

            when (val action = rule.action) {
                DoctrineAction.UseBasicAttack -> Unit

                is DoctrineAction.UseSkill -> {
                    if (contentRegistry.skillOrNull(action.skillId) == null) {
                        record(
                            code = "doctrine.unknown_action_skill",
                            path = "$rulePath.action.skillId",
                            message = "Unknown skill ${action.skillId}",
                            rejection = CommandRejectionReason(
                                code = CommandRejectionCode.UNKNOWN_CONTENT,
                                subjectContentId = action.skillId,
                                subjectInstanceId = rule.instanceId
                            )
                        )
                    }
                }
            }
        }

        return DoctrineValidationReport(
            validation = ValidationResult(issues),
            rejectionReason = firstRejection
        )
    }

    fun conditionDepth(condition: DoctrineCondition): Int =
        when (condition) {
            is DoctrineCondition.Predicate -> 1
            is DoctrineCondition.Not -> 1 + conditionDepth(condition.condition)
            is DoctrineCondition.All ->
                1 + (condition.conditions.maxOfOrNull(::conditionDepth) ?: 0)
            is DoctrineCondition.Any ->
                1 + (condition.conditions.maxOfOrNull(::conditionDepth) ?: 0)
        }

    private fun validateCondition(
        condition: DoctrineCondition,
        path: String,
        contentRegistry: ContentRegistry,
        maximumSequenceSuffixLength: Int,
        record: (
            code: String,
            path: String,
            message: String,
            rejection: CommandRejectionReason
        ) -> Unit
    ) {
        when (condition) {
            is DoctrineCondition.All -> {
                if (condition.conditions.isEmpty()) {
                    record(
                        "doctrine.empty_all",
                        path,
                        "ALL requires at least one child condition",
                        CommandRejectionReason(CommandRejectionCode.INVALID_ARGUMENT)
                    )
                }
                condition.conditions.forEachIndexed { index, child ->
                    validateCondition(
                        child,
                        "$path.conditions[$index]",
                        contentRegistry,
                        maximumSequenceSuffixLength,
                        record
                    )
                }
            }

            is DoctrineCondition.Any -> {
                if (condition.conditions.isEmpty()) {
                    record(
                        "doctrine.empty_any",
                        path,
                        "ANY requires at least one child condition",
                        CommandRejectionReason(CommandRejectionCode.INVALID_ARGUMENT)
                    )
                }
                condition.conditions.forEachIndexed { index, child ->
                    validateCondition(
                        child,
                        "$path.conditions[$index]",
                        contentRegistry,
                        maximumSequenceSuffixLength,
                        record
                    )
                }
            }

            is DoctrineCondition.Not ->
                validateCondition(
                    condition.condition,
                    "$path.condition",
                    contentRegistry,
                    maximumSequenceSuffixLength,
                    record
                )

            is DoctrineCondition.Predicate ->
                validatePredicate(
                    condition.predicate,
                    "$path.predicate",
                    contentRegistry,
                    maximumSequenceSuffixLength,
                    record
                )
        }
    }

    private fun validatePredicate(
        predicate: DoctrinePredicate,
        path: String,
        contentRegistry: ContentRegistry,
        maximumSequenceSuffixLength: Int,
        record: (
            code: String,
            path: String,
            message: String,
            rejection: CommandRejectionReason
        ) -> Unit
    ) {
        when (predicate) {
            is DoctrinePredicate.PlayerHealthPercent -> if (predicate.threshold > com.idlerpg.game.core.number.Ratio.ONE) record("doctrine.invalid_player_hp_percent", "$path.threshold", "Player HP threshold exceeds 100%", CommandRejectionReason(CommandRejectionCode.INVALID_ARGUMENT))
            is DoctrinePredicate.EnemyCount -> if (predicate.amount < com.idlerpg.game.core.number.GameNumber.ONE || predicate.amount > com.idlerpg.game.core.number.GameNumber.of(5L)) record("doctrine.invalid_enemy_count", "$path.amount", "Enemy count must be 1..5", CommandRejectionReason(CommandRejectionCode.INVALID_ARGUMENT))
            is DoctrinePredicate.EnemyRolePresent, DoctrinePredicate.ElitePresent, DoctrinePredicate.BossPresent -> Unit
            is DoctrinePredicate.EnemyHealthPercent -> {
                if (predicate.threshold.units > com.idlerpg.game.core.number.Ratio.ONE.units) {
                    record(
                        "doctrine.invalid_enemy_hp_percent",
                        "$path.threshold",
                        "Enemy HP percentage threshold cannot exceed 100%",
                        CommandRejectionReason(CommandRejectionCode.INVALID_ARGUMENT)
                    )
                }
            }

            is DoctrinePredicate.SkillReady -> {
                if (contentRegistry.skillOrNull(predicate.skillId) == null) {
                    record(
                        "doctrine.unknown_predicate_skill",
                        "$path.skillId",
                        "Unknown skill ${predicate.skillId}",
                        CommandRejectionReason(
                            code = CommandRejectionCode.UNKNOWN_CONTENT,
                            subjectContentId = predicate.skillId
                        )
                    )
                }
            }

            is DoctrinePredicate.ResonanceCharge -> Unit

            is DoctrinePredicate.SequenceSuffix -> {
                if (predicate.affinities.isEmpty()) {
                    record(
                        "doctrine.empty_sequence_suffix",
                        "$path.affinities",
                        "Sequence suffix predicate requires at least one affinity",
                        CommandRejectionReason(CommandRejectionCode.INVALID_ARGUMENT)
                    )
                }
                if (predicate.affinities.size > maximumSequenceSuffixLength) {
                    record(
                        "doctrine.sequence_suffix_too_long",
                        "$path.affinities",
                        "Sequence suffix length ${predicate.affinities.size} exceeds " +
                            "buffer size $maximumSequenceSuffixLength",
                        CommandRejectionReason(CommandRejectionCode.CAPACITY_EXCEEDED)
                    )
                }
            }

            is DoctrinePredicate.StatusPresent -> Unit
            is DoctrinePredicate.StatusAbsent -> Unit
        }
    }
}
