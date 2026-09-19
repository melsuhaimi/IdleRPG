package com.idlerpg.game.simulation

import com.idlerpg.game.core.config.AdaptationCurve
import com.idlerpg.game.core.config.BalanceConfig
import com.idlerpg.game.core.number.GameNumber
import com.idlerpg.game.data.content.DefaultGameContent
import com.idlerpg.game.data.content.TrainingHollowQuestContent
import com.idlerpg.game.data.local.SaveData
import com.idlerpg.game.domain.command.ApplyDoctrinePreset
import com.idlerpg.game.domain.command.DoctrinePreset
import com.idlerpg.game.domain.definition.Affinity
import com.idlerpg.game.domain.definition.combat.EffectSpec
import com.idlerpg.game.domain.engine.CommandHandlingResult
import com.idlerpg.game.domain.engine.EngineContext
import com.idlerpg.game.domain.event.ConvergenceTriggered
import com.idlerpg.game.domain.event.HealingApplied
import com.idlerpg.game.domain.model.GameState
import com.idlerpg.game.domain.model.adaptation.AdaptationState
import com.idlerpg.game.domain.model.adaptation.AffinityExposureState
import com.idlerpg.game.domain.model.adaptation.RegionAdaptationState
import com.idlerpg.game.domain.model.doctrine.DoctrineAction
import com.idlerpg.game.domain.system.adaptation.AdaptationSystem
import com.idlerpg.game.domain.system.combat.ActionResolutionSystem
import com.idlerpg.game.domain.system.doctrine.DoctrineSystem
import com.idlerpg.game.domain.system.progression.FeatureUnlockSystem
import com.idlerpg.game.domain.system.progression.PlayerProgressionSystem
import com.idlerpg.game.domain.system.quest.QuestSystem
import com.idlerpg.game.domain.system.skill.SkillValidationSystem
import com.idlerpg.game.presentation.content.PresentationContentRegistry

/** Save retention, extended progression, defensive exposure and loadout-aware presets. */
object ProgressionRepairScenarioTest {
    @JvmStatic fun main(args: Array<String>) {
        run()
        println("PROGRESSION_REPAIR_PASS")
    }

    private val registry = DefaultGameContent.registry()
    private val regionId = DefaultGameContent.TRAINING_HOLLOW_REGION_ID

    fun run() {
        tierBoundariesAndRecovery()
        levelCapAndRetainedUnlocks()
        questEligibilityAndPresentation()
        presetsAndEffectiveHealing()
    }

    private fun tierBoundariesAndRecovery() {
        val thresholds = registry.adaptationThreshold(DefaultGameContent.STANDARD_ADAPTATION_THRESHOLDS_ID)
        for (tier in 1..1_000) {
            val threshold = AdaptationCurve.threshold(tier)
            check(thresholds.tierForPressure(threshold) == tier)
            check(thresholds.tierForPressure(threshold - GameNumber.ONE) == tier - 1)
        }
        check(AdaptationCurve.resistanceMultiplier(4).units == 10_000L)
        check(AdaptationCurve.resistanceMultiplier(1_000).units == 7_500L)
        check(AdaptationCurve.rewardBonusUnits(1_000) == 5_000L)
        val initial = GameState.newGame(609L)
        fun pressureState(pressure: GameNumber) = initial.copy(run = initial.run.copy(
            adaptation = AdaptationState(mapOf(regionId to RegionAdaptationState(
                exposureByAffinityId = mapOf(Affinity.TEMPO.id to AffinityExposureState(pressure = pressure)),
                tierByAffinityId = mapOf(Affinity.TEMPO.id to 4)
            )))
        ))
        val legacy = pressureState(GameNumber.of(136_740L))
        fun complete(state: GameState) = AdaptationSystem.completeEncounter(state, regionId, registry, BalanceConfig())
        val result = complete(legacy)
        check(result == complete(SaveData.fromGameState(legacy).toGameState()))
        check(result.state.run.adaptation.regionStateById.getValue(regionId)
            .exposureByAffinityId.getValue(Affinity.TEMPO.id).pressure == GameNumber.of(134_006L))
        val capped = complete(pressureState(AdaptationCurve.threshold(1_000) * 100L))
        check(capped.state.run.adaptation.regionStateById.getValue(regionId)
            .exposureByAffinityId.getValue(Affinity.TEMPO.id).pressure < AdaptationCurve.threshold(1_000))
    }

    private fun levelCapAndRetainedUnlocks() {
        val initial = GameState.newGame(707L)
        val oldCap = initial.copy(run = initial.run.copy(progression = initial.run.progression.copy(
            playerLevel = initial.run.progression.playerLevel.copy(level = 100L)
        )))
        val next = PlayerProgressionSystem.grantExperience(
            oldCap,
            PlayerProgressionSystem.experienceToNextLevel(oldCap, registry),
            null,
            registry
        ).state
        check(next.run.progression.playerLevel.level == 101L)
        val preMaximum = next.copy(
            run = next.run.copy(
                progression = next.run.progression.copy(
                    playerLevel = next.run.progression.playerLevel.copy(
                        level = 14_999L,
                        currentExperience = GameNumber.ZERO
                    )
                )
            )
        )
        val maximum = PlayerProgressionSystem.grantExperience(
            preMaximum,
            PlayerProgressionSystem.experienceToNextLevel(preMaximum, registry),
            null,
            registry
        ).state
        check(maximum.run.progression.playerLevel.level == 15_000L)
        check(PlayerProgressionSystem.experienceToNextLevel(maximum, registry) == GameNumber.ZERO)
        check(SaveData.fromGameState(maximum).toGameState() == maximum)
        val levelSix = initial.copy(run = initial.run.copy(progression = initial.run.progression.copy(
            playerLevel = initial.run.progression.playerLevel.copy(level = 6L)
        )))
        val unlocked = FeatureUnlockSystem.unlockEligible(levelSix, registry).state
        check(DefaultGameContent.CINDER_MARK_FEATURE_ID in unlocked.run.progression.featureUnlocks.unlockedFeatureIds)
        check(DefaultGameContent.FROST_LANCE_FEATURE_ID !in unlocked.run.progression.featureUnlocks.unlockedFeatureIds)
        val owned = initial.copy(run = initial.run.copy(progression = initial.run.progression.copy(
            featureUnlocks = initial.run.progression.featureUnlocks.copy(unlockedFeatureIds = setOf(DefaultGameContent.FROST_LANCE_FEATURE_ID))
        )))
        check(SkillValidationSystem.unlockRejectionReason(FeatureUnlockSystem.unlockEligible(owned, registry).state,
            registry.skill(DefaultGameContent.FROST_LANCE_ID)) == null)
    }

    private fun questEligibilityAndPresentation() {
        val initial = GameState.newGame(808L)
        val eligible = initial.copy(run = initial.run.copy(progression = initial.run.progression.copy(
            playerLevel = initial.run.progression.playerLevel.copy(level = 3L)
        )))
        val context = EngineContext(registry)
        val id = TrainingHollowQuestContent.questId("first_resonance")
        val event = ConvergenceTriggered(DefaultGameContent.FORGED_FLAME_ID)
        check(QuestSystem.react(initial, event, context).state.run.quests.progressFor(id).completionCount == GameNumber.ZERO)
        val completed = QuestSystem.react(eligible, event, context).state
        check(completed.run.quests.progressFor(id).completionCount == GameNumber.ONE)
        check(completed.run.quests.progressFor(id).claimedCount == GameNumber.ZERO)
        check(QuestSystem.react(completed, event, context).state == completed)
        check(registry.allQuests().count { !it.repeatable } == 7)
        check(registry.allQuests().count { it.repeatable } == 3)
        val presentation = PresentationContentRegistry.default()
        registry.allQuests().forEach { quest ->
            presentation.entry(quest.id)
            quest.objectives.forEach { presentation.entry(it.id) }
        }
    }

    private fun presetsAndEffectiveHealing() {
        val runtime = SimulationTestSupport.runtime(seed = 967L)
        if (runtime.state().run.combat.playerCombatant == null) SimulationTestSupport.startTraining(runtime)
        val initial = runtime.state()
        val player = requireNotNull(initial.run.combat.playerCombatant)
        val state = initial.copy(run = initial.run.copy(
            player = initial.run.player.copy(currentHealth = GameNumber.ONE,
                equippedSkillIds = listOf(DefaultGameContent.QUICK_SLASH_ID, DefaultGameContent.HEAVY_STRIKE_ID)),
            combat = initial.run.combat.copy(playerCombatant = player.copy(currentHealth = GameNumber.ONE))
        ))
        for (preset in DoctrinePreset.values()) {
            val context = EngineContext(registry).also { it.beginExecution(state.engine) }
            val result = DoctrineSystem.handle(state, ApplyDoctrinePreset(preset), context)
            check(result is CommandHandlingResult.Accepted)
            check(result.state.run.doctrine.rules.map { (it.action as DoctrineAction.UseSkill).skillId } ==
                listOf(DefaultGameContent.HEAVY_STRIKE_ID, DefaultGameContent.QUICK_SLASH_ID))
        }
        fun heal(input: GameState) = ActionResolutionSystem.resolvePrimitiveEffects(
            input, player.instanceId, player.instanceId, listOf(EffectSpec.Heal(GameNumber.of(100_000L))),
            EngineContext(registry).also { it.beginExecution(input.engine) }, listOf(Affinity.GUARD)
        )
        val healed = heal(state)
        val amount = healed.events.filterIsInstance<HealingApplied>().single().amount
        check(amount > GameNumber.ZERO)
        check(healed.state.run.adaptation.regionStateById.getValue(regionId)
            .exposureByAffinityId.getValue(Affinity.GUARD.id).currentEncounterContribution == amount)
        val overheal = heal(healed.state)
        check(overheal.events.filterIsInstance<HealingApplied>().single().amount == GameNumber.ZERO)
        check(overheal.state.run.adaptation == healed.state.run.adaptation)
    }
}
