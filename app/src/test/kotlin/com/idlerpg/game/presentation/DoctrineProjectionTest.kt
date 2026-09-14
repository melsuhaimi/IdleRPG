package com.idlerpg.game.presentation

import com.idlerpg.game.core.id.ContentId
import com.idlerpg.game.core.id.InstanceId
import com.idlerpg.game.core.number.GameNumber
import com.idlerpg.game.core.number.GameRate
import com.idlerpg.game.core.number.Ratio
import com.idlerpg.game.core.time.GameDuration
import com.idlerpg.game.data.content.DefaultGameContent
import com.idlerpg.game.domain.command.CommandRejectionReason
import com.idlerpg.game.domain.definition.Affinity
import com.idlerpg.game.domain.model.GameState
import com.idlerpg.game.domain.model.combat.EnemyState
import com.idlerpg.game.domain.model.doctrine.DoctrineAction
import com.idlerpg.game.domain.model.doctrine.DoctrineComparison
import com.idlerpg.game.domain.model.doctrine.DoctrineCondition
import com.idlerpg.game.domain.model.doctrine.DoctrinePredicate
import com.idlerpg.game.domain.model.doctrine.DoctrineRule
import com.idlerpg.game.domain.model.player.PlayerState
import com.idlerpg.game.domain.model.resonance.ConvergenceState
import com.idlerpg.game.domain.model.resonance.ResonanceSequenceState
import com.idlerpg.game.domain.model.resonance.ResonanceState
import com.idlerpg.game.presentation.content.PresentationContentRegistry
import com.idlerpg.game.presentation.doctrine.DoctrineDraftController
import com.idlerpg.game.presentation.model.DoctrineFeedbackKind
import com.idlerpg.game.presentation.model.DoctrineFeedbackUiState
import com.idlerpg.game.presentation.projection.DoctrineProjector
import com.idlerpg.game.presentation.query.DoctrineReadQueries
import com.idlerpg.game.presentation.query.GameReadQueries

/** Canonical state/content -> FUI-06 Doctrine/Resonance projection regression. */
object DoctrineProjectionTest {
    @JvmStatic
    fun main(args: Array<String>) {
        val content = DefaultGameContent.registry()
        val presentation = PresentationContentRegistry.default()
        val queries = ProjectionReadQueries()
        val doctrineQueries = ProjectionDoctrineReadQueries()
        val projector = DoctrineProjector(
            contentRegistry = content,
            presentationContentRegistry = presentation,
            readQueries = queries,
            doctrineReadQueries = doctrineQueries
        )

        val validRule = DoctrineRule(
            instanceId = InstanceId(91L),
            condition = DoctrineCondition.Predicate(
                DoctrinePredicate.SkillReady(DefaultGameContent.HEAVY_STRIKE_ID)
            ),
            action = DoctrineAction.UseSkill(DefaultGameContent.HEAVY_STRIKE_ID)
        )
        val staleRule = DoctrineRule(
            instanceId = InstanceId(92L),
            condition = DoctrineCondition.Predicate(
                DoctrinePredicate.EnemyHealthPercent(
                    DoctrineComparison.LESS_THAN_OR_EQUAL,
                    Ratio.HALF
                )
            ),
            action = DoctrineAction.UseSkill(DefaultGameContent.QUICK_SLASH_ID)
        )
        val base = GameState.newGame(randomSeed = 806L)
        val state = base.copy(
            run = base.run.copy(
                player = PlayerState(
                    equippedSkillIds = listOf(DefaultGameContent.HEAVY_STRIKE_ID)
                ),
                doctrine = base.run.doctrine.copy(
                    enabled = true,
                    rules = listOf(validRule, staleRule)
                ),
                resonance = ResonanceState(
                    chargeByAffinityId = mapOf(
                        Affinity.MIGHT.id to GameNumber.of(2L),
                        Affinity.EMBER.id to GameNumber.ONE
                    ),
                    sequence = ResonanceSequenceState(
                        affinityIds = listOf(
                            Affinity.MIGHT.id,
                            Affinity.MIGHT.id,
                            Affinity.EMBER.id
                        )
                    ),
                    convergence = ConvergenceState(
                        triggerCountById = mapOf(
                            DefaultGameContent.FORGED_FLAME_ID to GameNumber.of(3L)
                        ),
                        encounterTriggerCountById = mapOf(
                            DefaultGameContent.FORGED_FLAME_ID to 1L
                        )
                    )
                )
            ),
            meta = base.meta.copy(
                discoveries = base.meta.discoveries.copy(
                    discoveredConvergenceIds = setOf(DefaultGameContent.FORGED_FLAME_ID)
                )
            )
        )
        val feedback = DoctrineFeedbackUiState(
            sequenceNumber = 10L,
            kind = DoctrineFeedbackKind.RULE_SELECTED,
            ruleId = validRule.instanceId,
            contentId = DefaultGameContent.HEAVY_STRIKE_ID
        )
        val ui = projector.project(
            state = state,
            draft = DoctrineDraftController.newDraft(),
            feedback = feedback,
            mutationPending = false
        )

        check(ui.ruleCapacity == 8)
        check(ui.conditionMaxDepth == 4)
        check(ui.sequenceCapacity == 8)
        check(!ui.mutationPending)
        check(ui.rules.size == 2)
        check(ui.rules[0].selectedByLatestFeedback)
        check(!ui.rules[0].hasUnequippedSkillReference)
        check(ui.rules[1].hasUnequippedSkillReference)
        check(ui.skillChoices.single {
            it.skillId == DefaultGameContent.HEAVY_STRIKE_ID
        }.selectableForDoctrine)
        check(!ui.skillChoices.single {
            it.skillId == DefaultGameContent.QUICK_SLASH_ID
        }.selectableForDoctrine)
        check(ui.resonance.size == 8)
        check(ui.resonanceSequence == listOf(
            Affinity.MIGHT.id,
            Affinity.MIGHT.id,
            Affinity.EMBER.id
        ))
        val forged = ui.convergences.single()
        check(forged.convergenceId == DefaultGameContent.FORGED_FLAME_ID)
        check(forged.discovered)
        check(forged.eligibleNow)
        check(forged.totalTriggerCountDisplay == "3")
        check(forged.encounterTriggerCount == 1L)
        check(ui.draft != null)

        println("FUI06_DOCTRINE_PROJECTION_PASS")
    }

    private class ProjectionDoctrineReadQueries : DoctrineReadQueries {
        override fun resonanceSequenceCapacity(): Int = 8
        override fun convergenceEligible(
            state: GameState,
            convergenceId: ContentId
        ): Boolean = convergenceId == DefaultGameContent.FORGED_FLAME_ID
    }

    private class ProjectionReadQueries : GameReadQueries {
        override fun attackPower(state: GameState): GameNumber = GameNumber.of(10L)
        override fun armor(state: GameState): GameNumber = GameNumber.ZERO
        override fun basicAttackInterval(state: GameState): GameDuration = GameDuration.ofSeconds(1L)
        override fun basicAttackDps(state: GameState): GameRate = GameRate.of(10L)
        override fun enemyMaximumHealth(state: GameState, enemy: EnemyState): GameNumber =
            GameNumber.of(100L)
        override fun skillQueueRejection(
            state: GameState,
            skillId: ContentId
        ): CommandRejectionReason? = null
        override fun skillExecutionRejection(
            state: GameState,
            skillId: ContentId
        ): CommandRejectionReason? = null
        override fun upgradeLevel(state: GameState, upgradeId: ContentId): Long = 0L
        override fun upgradeCurrentCost(state: GameState, upgradeId: ContentId): GameNumber =
            GameNumber.of(20L)
        override fun playerExperienceToNextLevel(state: GameState): GameNumber = GameNumber.of(100L)
        override fun masteryLevel(state: GameState, affinityId: ContentId): Long = 0L
        override fun masteryExperienceToNextLevel(
            state: GameState,
            affinityId: ContentId
        ): GameNumber = GameNumber.of(100L)
        override fun isFeatureUnlocked(state: GameState, featureId: ContentId): Boolean = false
        override fun chronicleEligible(state: GameState): Boolean = false
        override fun doctrineCapacity(state: GameState): Int = 8
        override fun doctrineConditionMaxDepth(): Int = 4
        override fun resonanceChargeCap(): GameNumber = GameNumber.of(100L)
        override fun skillLoadoutCapacity(): Int = 4
        override fun effectiveInventoryCapacity(state: GameState): Long = 60L
        override fun inventoryOverflowCapacity(): Long = 20L
        override fun nextInventoryExpansionCost(state: GameState): GameNumber = GameNumber.of(100L)
        override fun inventoryProgressionBlocked(state: GameState): Boolean = false
    }
}
