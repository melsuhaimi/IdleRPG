package com.idlerpg.game.domain.system.doctrine

import com.idlerpg.game.domain.command.AddDoctrineRule
import com.idlerpg.game.domain.command.ApplyDoctrinePreset
import com.idlerpg.game.domain.command.DoctrinePreset
import com.idlerpg.game.domain.command.CommandRejectionCode
import com.idlerpg.game.domain.command.CommandRejectionReason
import com.idlerpg.game.domain.command.DisableDoctrineRule
import com.idlerpg.game.domain.command.DoctrineCommand
import com.idlerpg.game.domain.command.EnableDoctrineRule
import com.idlerpg.game.domain.command.GameCommand
import com.idlerpg.game.domain.command.MoveDoctrineRule
import com.idlerpg.game.domain.command.RemoveDoctrineRule
import com.idlerpg.game.domain.command.ReplaceDoctrine
import com.idlerpg.game.domain.command.ReplaceDoctrineRule
import com.idlerpg.game.domain.engine.CommandHandlingResult
import com.idlerpg.game.domain.engine.EngineContext
import com.idlerpg.game.domain.engine.GameCommandHandler
import com.idlerpg.game.domain.event.DoctrineUpdated
import com.idlerpg.game.domain.model.GameState
import com.idlerpg.game.domain.model.doctrine.DoctrineRule
import com.idlerpg.game.domain.model.doctrine.DoctrineState
import com.idlerpg.game.domain.model.doctrine.DoctrineAction
import com.idlerpg.game.domain.model.doctrine.DoctrineComparison
import com.idlerpg.game.domain.model.doctrine.DoctrineCondition
import com.idlerpg.game.domain.model.doctrine.DoctrinePredicate
import com.idlerpg.game.core.id.ContentId
import com.idlerpg.game.core.number.GameNumber
import com.idlerpg.game.core.number.Ratio
import com.idlerpg.game.data.content.DefaultGameContent

/**
 * Foundation 9 command handler for player-authored Doctrine configuration.
 *
 * All updates are validated before GameEngine commits them. AddDoctrineRule allocates its
 * InstanceId through EngineContext; a rejected command does not commit the generator
 * snapshot, preserving atomic deterministic identity behavior.
 */
object DoctrineSystem : GameCommandHandler {

    override fun handle(
        state: GameState,
        command: GameCommand,
        context: EngineContext
    ): CommandHandlingResult =
        when (command) {
            is ApplyDoctrinePreset -> commitCandidate(state, DoctrineState(true, presetRules(command.preset, state, context)), context)
            is ReplaceDoctrine ->
                commitCandidate(
                    state = state,
                    candidate = DoctrineState(
                        enabled = command.enabled,
                        rules = command.rules
                    ),
                    context = context
                )

            is AddDoctrineRule -> {
                val rule = DoctrineRule(
                    instanceId = context.nextInstanceId(),
                    enabled = command.enabled,
                    condition = command.condition,
                    action = command.action
                )
                commitCandidate(
                    state = state,
                    candidate = state.run.doctrine.copy(
                        rules = state.run.doctrine.rules + rule
                    ),
                    context = context
                )
            }

            is ReplaceDoctrineRule -> {
                val index = state.run.doctrine.rules.indexOfFirst {
                    it.instanceId == command.ruleId
                }
                if (index < 0) {
                    rejectedNotOwned(command.ruleId)
                } else {
                    val replacement = DoctrineRule(
                        instanceId = command.ruleId,
                        enabled = command.enabled,
                        condition = command.condition,
                        action = command.action
                    )
                    val rules = state.run.doctrine.rules.toMutableList()
                    rules[index] = replacement
                    commitCandidate(
                        state = state,
                        candidate = state.run.doctrine.copy(rules = rules),
                        context = context
                    )
                }
            }

            is RemoveDoctrineRule -> {
                if (state.run.doctrine.rules.none { it.instanceId == command.ruleId }) {
                    rejectedNotOwned(command.ruleId)
                } else {
                    commitCandidate(
                        state = state,
                        candidate = state.run.doctrine.copy(
                            rules = state.run.doctrine.rules.filterNot {
                                it.instanceId == command.ruleId
                            }
                        ),
                        context = context
                    )
                }
            }

            is MoveDoctrineRule -> {
                val rules = state.run.doctrine.rules
                val oldIndex = rules.indexOfFirst { it.instanceId == command.ruleId }
                when {
                    oldIndex < 0 -> rejectedNotOwned(command.ruleId)
                    command.newIndex !in rules.indices -> CommandHandlingResult.Rejected(
                        CommandRejectionReason(
                            code = CommandRejectionCode.INVALID_ARGUMENT,
                            subjectInstanceId = command.ruleId
                        )
                    )
                    else -> {
                        val mutable = rules.toMutableList()
                        val moved = mutable.removeAt(oldIndex)
                        mutable.add(command.newIndex, moved)
                        commitCandidate(
                            state = state,
                            candidate = state.run.doctrine.copy(rules = mutable),
                            context = context
                        )
                    }
                }
            }

            is EnableDoctrineRule ->
                setRuleEnabled(state, command.ruleId, true, context)

            is DisableDoctrineRule ->
                setRuleEnabled(state, command.ruleId, false, context)

            is DoctrineCommand -> CommandHandlingResult.Rejected(
                CommandRejectionReason(CommandRejectionCode.UNSUPPORTED)
            )

            else -> CommandHandlingResult.Rejected(
                CommandRejectionReason(CommandRejectionCode.UNSUPPORTED)
            )
        }

    private fun presetRules(preset: DoctrinePreset, state: GameState, context: EngineContext): List<DoctrineRule> {
        val healingSkills = setOf("skill.guard_mend", "skill.vital_surge")
        val defensiveSkills = healingSkills + "skill.glacial_ward"
        val capacity = context.balanceConfig.baseDoctrineRuleCapacity +
            if (DefaultGameContent.DOCTRINE_MEMORY_FEATURE_ID in state.meta.persistentFeatureUnlocks.unlockedFeatureIds) 1 else 0
        // Preserve loadout order within groups; setup skills precede the fast fallback.
        val skills = state.run.player.equippedSkillIds.distinct().sortedBy { skill ->
            when {
                skill.value == "skill.quick_slash" -> 3
                skill.value in defensiveSkills -> if (preset == DoctrinePreset.AGGRESSIVE) 2 else 0
                else -> 1
            }
        }.take(capacity)
        return skills.map { skill ->
            val predicate = if (skill.value in healingSkills) {
                DoctrinePredicate.PlayerHealthPercent(
                    DoctrineComparison.LESS_THAN_OR_EQUAL,
                    Ratio.ofUnits(when (preset) {
                        DoctrinePreset.SURVIVAL -> 7_000L
                        DoctrinePreset.BALANCED -> 5_000L
                        DoctrinePreset.AGGRESSIVE -> 3_500L
                    })
                )
            } else DoctrinePredicate.SkillReady(skill)
            DoctrineRule(context.nextInstanceId(), true, DoctrineCondition.Predicate(predicate), DoctrineAction.UseSkill(skill))
        }
    }

    private fun setRuleEnabled(
        state: GameState,
        ruleId: com.idlerpg.game.core.id.InstanceId,
        enabled: Boolean,
        context: EngineContext
    ): CommandHandlingResult {
        val index = state.run.doctrine.rules.indexOfFirst { it.instanceId == ruleId }
        if (index < 0) {
            return rejectedNotOwned(ruleId)
        }
        val rules = state.run.doctrine.rules.toMutableList()
        rules[index] = rules[index].copy(enabled = enabled)
        return commitCandidate(
            state = state,
            candidate = state.run.doctrine.copy(rules = rules),
            context = context
        )
    }

    private fun commitCandidate(
        state: GameState,
        candidate: DoctrineState,
        context: EngineContext
    ): CommandHandlingResult {
        val report = DoctrineValidator.validate(
            doctrine = candidate,
            contentRegistry = context.contentRegistry,
            ruleCapacity = context.balanceConfig.baseDoctrineRuleCapacity +
                if (DefaultGameContent.DOCTRINE_MEMORY_FEATURE_ID in
                    state.meta.persistentFeatureUnlocks.unlockedFeatureIds
                ) 1 else 0,
            maximumConditionDepth = context.balanceConfig.doctrineConditionMaxDepth,
            maximumSequenceSuffixLength = context.balanceConfig.resonanceSequenceBufferSize
        )
        if (!report.isValid) {
            return CommandHandlingResult.Rejected(
                report.rejectionReason
                    ?: CommandRejectionReason(CommandRejectionCode.INVALID_ARGUMENT)
            )
        }

        val newState = state.copy(
            run = state.run.copy(
                doctrine = candidate
            )
        )
        return CommandHandlingResult.Accepted(
            state = newState,
            events = listOf(
                DoctrineUpdated(
                    enabled = candidate.enabled,
                    orderedRuleIds = candidate.rules.map { it.instanceId }
                )
            )
        )
    }

    private fun rejectedNotOwned(
        ruleId: com.idlerpg.game.core.id.InstanceId
    ): CommandHandlingResult.Rejected =
        CommandHandlingResult.Rejected(
            CommandRejectionReason(
                code = CommandRejectionCode.NOT_OWNED,
                subjectInstanceId = ruleId
            )
        )
}
