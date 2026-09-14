package com.idlerpg.game.presentation.projection

import com.idlerpg.game.core.id.ContentId
import com.idlerpg.game.core.number.GameNumber
import com.idlerpg.game.data.content.ContentRegistry
import com.idlerpg.game.domain.definition.Affinity
import com.idlerpg.game.domain.model.GameState
import com.idlerpg.game.domain.model.combat.QueuedPlayerAction
import com.idlerpg.game.domain.model.doctrine.DoctrineAction
import com.idlerpg.game.domain.model.doctrine.DoctrineCondition
import com.idlerpg.game.domain.model.doctrine.DoctrinePredicate
import com.idlerpg.game.presentation.content.PresentationContentRegistry
import com.idlerpg.game.presentation.doctrine.DoctrineDraftController
import com.idlerpg.game.presentation.format.GameNumberFormatter
import com.idlerpg.game.presentation.model.DoctrineAffinityChoiceUiState
import com.idlerpg.game.presentation.model.DoctrineConvergenceRequirementUiState
import com.idlerpg.game.presentation.model.DoctrineConvergenceUiState
import com.idlerpg.game.presentation.model.DoctrineFeedbackKind
import com.idlerpg.game.presentation.model.DoctrineFeedbackUiState
import com.idlerpg.game.presentation.model.DoctrineResonanceAffinityUiState
import com.idlerpg.game.presentation.model.DoctrineRuleDraftUiState
import com.idlerpg.game.presentation.model.DoctrineRuleUiState
import com.idlerpg.game.presentation.model.DoctrineSkillChoiceUiState
import com.idlerpg.game.presentation.model.DoctrineStatusChoiceUiState
import com.idlerpg.game.presentation.model.DoctrineUiState
import com.idlerpg.game.presentation.query.DoctrineReadQueries
import com.idlerpg.game.presentation.query.GameReadQueries
import java.math.BigInteger

/** Pure FUI-06 canonical Doctrine + Resonance projection. */
class DoctrineProjector(
    private val contentRegistry: ContentRegistry,
    private val presentationContentRegistry: PresentationContentRegistry,
    private val readQueries: GameReadQueries,
    private val doctrineReadQueries: DoctrineReadQueries
) {
    fun project(
        state: GameState,
        draft: DoctrineRuleDraftUiState?,
        feedback: DoctrineFeedbackUiState?,
        mutationPending: Boolean
    ): DoctrineUiState {
        val equipped = state.run.player.equippedSkillIds.toSet()
        val skills = contentRegistry.allSkills().map { definition ->
            val requiredFeatureId = definition.requiredFeatureId
            val unlocked = requiredFeatureId == null ||
                readQueries.isFeatureUnlocked(state, requiredFeatureId)
            val equippedNow = definition.id in equipped
            DoctrineSkillChoiceUiState(
                skillId = definition.id,
                titleStringKey = presentationContentRegistry.entry(definition.id).titleStringKey,
                equipped = equippedNow,
                unlocked = unlocked,
                requiresEquipped = definition.requiresEquipped,
                selectableForDoctrine = unlocked &&
                    (!definition.requiresEquipped || equippedNow)
            )
        }
        val statuses = contentRegistry.allStatuses().map { definition ->
            DoctrineStatusChoiceUiState(
                statusId = definition.id,
                titleStringKey = presentationContentRegistry.entry(definition.id).titleStringKey
            )
        }
        val affinities = Affinity.values().map { affinity ->
            DoctrineAffinityChoiceUiState(
                affinityId = affinity.id,
                titleStringKey = presentationContentRegistry.entry(affinity.id).titleStringKey
            )
        }
        val selectedRuleId = feedback
            ?.takeIf { it.kind == DoctrineFeedbackKind.RULE_SELECTED }
            ?.ruleId
        val rules = state.run.doctrine.rules.mapIndexed { index, rule ->
            val asDraft = DoctrineDraftController.fromRule(rule)
            DoctrineRuleUiState(
                ruleId = rule.instanceId,
                index = index,
                enabled = rule.enabled,
                condition = asDraft.condition,
                action = asDraft.action,
                hasUnequippedSkillReference = hasUnequippedSkillReference(
                    condition = rule.condition,
                    action = rule.action,
                    state = state
                ),
                selectedByLatestFeedback = selectedRuleId == rule.instanceId,
                canMoveEarlier = index > 0,
                canMoveLater = index < state.run.doctrine.rules.lastIndex
            )
        }

        val chargeCap = readQueries.resonanceChargeCap()
        val resonance = Affinity.values().map { affinity ->
            val charge = state.run.resonance.chargeByAffinityId[affinity.id] ?: GameNumber.ZERO
            DoctrineResonanceAffinityUiState(
                affinityId = affinity.id,
                titleStringKey = presentationContentRegistry.entry(affinity.id).titleStringKey,
                chargeDisplay = GameNumberFormatter.compact(charge),
                capDisplay = GameNumberFormatter.compact(chargeCap),
                chargeProgressUnits = ratioUnits(charge, chargeCap)
            )
        }

        val convergences = contentRegistry.allConvergences().map { definition ->
            DoctrineConvergenceUiState(
                convergenceId = definition.id,
                titleStringKey = presentationContentRegistry.entry(definition.id).titleStringKey,
                discovered = definition.id in state.meta.discoveries.discoveredConvergenceIds,
                eligibleNow = doctrineReadQueries.convergenceEligible(state, definition.id),
                patternAffinityIds = definition.pattern.affinities.map(Affinity::id),
                minimumCharges = definition.minimumChargeByAffinity.entries
                    .sortedBy { it.key.id }
                    .map { (affinity, amount) ->
                        DoctrineConvergenceRequirementUiState(
                            affinityId = affinity.id,
                            titleStringKey = presentationContentRegistry.entry(affinity.id).titleStringKey,
                            requiredDisplay = GameNumberFormatter.compact(amount)
                        )
                    },
                totalTriggerCountDisplay = GameNumberFormatter.compact(
                    state.run.resonance.convergence.triggerCountById[definition.id]
                        ?: GameNumber.ZERO
                ),
                encounterTriggerCount =
                    state.run.resonance.convergence.encounterTriggerCountById[definition.id] ?: 0L
            )
        }

        val queuedSkillId = (state.run.combat.queuedPlayerAction as? QueuedPlayerAction.Skill)
            ?.skillId

        return DoctrineUiState(
            enabled = state.run.doctrine.enabled,
            ruleCount = state.run.doctrine.ruleCount,
            ruleCapacity = readQueries.doctrineCapacity(state),
            conditionMaxDepth = readQueries.doctrineConditionMaxDepth(),
            sequenceCapacity = doctrineReadQueries.resonanceSequenceCapacity(),
            mutationPending = mutationPending,
            queuedManualSkillTitleStringKey = queuedSkillId
                ?.let(presentationContentRegistry::entryOrNull)
                ?.titleStringKey,
            basicAttackId = contentRegistry.basicAttack.id,
            basicAttackTitleStringKey = presentationContentRegistry
                .entry(contentRegistry.basicAttack.id)
                .titleStringKey,
            rules = rules,
            draft = draft,
            skillChoices = skills,
            statusChoices = statuses,
            affinityChoices = affinities,
            resonance = resonance,
            resonanceSequence = state.run.resonance.sequence.affinityIds,
            convergences = convergences,
            feedback = feedback
        )
    }

    private fun hasUnequippedSkillReference(
        condition: DoctrineCondition,
        action: DoctrineAction,
        state: GameState
    ): Boolean {
        val equipped = state.run.player.equippedSkillIds.toSet()
        fun unavailable(skillId: ContentId): Boolean {
            val definition = contentRegistry.skillOrNull(skillId) ?: return true
            val requiredFeatureId = definition.requiredFeatureId
            if (requiredFeatureId != null &&
                !readQueries.isFeatureUnlocked(state, requiredFeatureId)
            ) {
                return true
            }
            return definition.requiresEquipped && skillId !in equipped
        }
        fun conditionHasUnavailableSkill(node: DoctrineCondition): Boolean = when (node) {
            is DoctrineCondition.All -> node.conditions.any(::conditionHasUnavailableSkill)
            is DoctrineCondition.Any -> node.conditions.any(::conditionHasUnavailableSkill)
            is DoctrineCondition.Not -> conditionHasUnavailableSkill(node.condition)
            is DoctrineCondition.Predicate -> when (val predicate = node.predicate) {
                is DoctrinePredicate.SkillReady -> unavailable(predicate.skillId)
                else -> false
            }
        }
        val actionUnavailable = when (action) {
            DoctrineAction.UseBasicAttack -> false
            is DoctrineAction.UseSkill -> unavailable(action.skillId)
        }
        return actionUnavailable || conditionHasUnavailableSkill(condition)
    }

    private fun ratioUnits(current: GameNumber, maximum: GameNumber): Int {
        if (maximum == GameNumber.ZERO) {
            return 0
        }
        val numerator = current.toBigInteger().multiply(BigInteger.valueOf(10_000L))
        return numerator.divide(maximum.toBigInteger())
            .coerceIn(BigInteger.ZERO, BigInteger.valueOf(10_000L))
            .toInt()
    }
}
