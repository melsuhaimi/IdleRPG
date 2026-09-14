package com.idlerpg.game.presentation

import com.idlerpg.game.core.id.ContentId
import com.idlerpg.game.core.id.InstanceId
import com.idlerpg.game.core.number.GameNumber
import com.idlerpg.game.core.number.GameRate
import com.idlerpg.game.core.time.GameDuration
import com.idlerpg.game.data.content.DefaultGameContent
import com.idlerpg.game.domain.command.CommandRejectionReason
import com.idlerpg.game.domain.model.GameState
import com.idlerpg.game.domain.model.combat.CombatState
import com.idlerpg.game.domain.model.combat.CombatStatus
import com.idlerpg.game.domain.model.combat.CombatantState
import com.idlerpg.game.domain.model.combat.EnemyState
import com.idlerpg.game.domain.model.combat.QueuedPlayerAction
import com.idlerpg.game.domain.model.player.PlayerState
import com.idlerpg.game.presentation.content.PresentationContentRegistry
import com.idlerpg.game.presentation.projection.SkillLoadoutProjector
import com.idlerpg.game.presentation.query.GameReadQueries

/** Dependency-free canonical-state/content -> FUI-04 loadout projection regression. */
object SkillLoadoutProjectionTest {
    @JvmStatic
    fun main(args: Array<String>) {
        val content = DefaultGameContent.registry()
        val presentation = PresentationContentRegistry.default()
        val queries = LoadoutReadQueries()
        val projector = SkillLoadoutProjector(
            contentRegistry = content,
            presentationContentRegistry = presentation,
            readQueries = queries
        )
        val fresh = projector.project(GameState.newGame(randomSeed = 804L))
        check(fresh.capacity == 4)
        check(fresh.equippedCount == 0)
        check(fresh.slots.size == 4)
        check(fresh.slots.all { it.skill == null })

        val heavy = fresh.availableSkills.single {
            it.skillId == DefaultGameContent.HEAVY_STRIKE_ID
        }
        val quick = fresh.availableSkills.single {
            it.skillId == DefaultGameContent.QUICK_SLASH_ID
        }
        check(heavy.unlock.unlocked)
        check(quick.unlock.unlocked)
        check(heavy.canEquip)
        check(quick.canEquip)
        check(heavy.evolutions.size == 2)
        check(heavy.evolutions.none { it.canSelect })
        check(heavy.evolutions.all {
            it.requiredAffinityTitleStringKey ==
                com.idlerpg.game.presentation.content.PresentationStringKey.MIGHT
        })

        queries.masteryLevel = 3L
        queries.featuresUnlocked = true
        val selectedState = GameState.newGame(randomSeed = 805L).let { base ->
            base.copy(run = base.run.copy(player = base.run.player.copy(
                selectedSkillEvolutionBySkillId = mapOf(
                    DefaultGameContent.HEAVY_STRIKE_ID to
                        DefaultGameContent.EARTHBREAKER_EVOLUTION_ID
                )
            )))
        }
        val evolvedHeavy = projector.project(selectedState).availableSkills.single {
            it.skillId == DefaultGameContent.HEAVY_STRIKE_ID
        }
        check(evolvedHeavy.evolutions.single { it.evolutionId == DefaultGameContent.EARTHBREAKER_EVOLUTION_ID }.selected)
        check(!evolvedHeavy.evolutions.single { it.evolutionId == DefaultGameContent.EARTHBREAKER_EVOLUTION_ID }.canSelect)
        check(evolvedHeavy.evolutions.single { it.evolutionId == DefaultGameContent.EXECUTIONER_EVOLUTION_ID }.canSelect)
        queries.masteryLevel = 0L

        val cinder = fresh.availableSkills.single {
            it.skillId == DefaultGameContent.CINDER_MARK_ID
        }
        check(!cinder.unlock.unlocked)
        check(cinder.descriptionStringKey == com.idlerpg.game.presentation.content.PresentationStringKey.DESC_CINDER_MARK)
        check(cinder.unlock.requiredPlayerLevel == 5L)
        check(cinder.unlock.masteryRequirements.isEmpty())
        check(!cinder.canEquip)

        val guard = fresh.availableSkills.single {
            it.skillId == DefaultGameContent.GUARD_MEND_ID
        }
        check(!guard.unlock.unlocked)
        check(guard.unlock.requiredPlayerLevel == 3L)

        val flame = fresh.availableSkills.single {
            it.skillId == DefaultGameContent.FLAME_BRAND_ID
        }
        check(!flame.unlock.unlocked)
        check(flame.unlock.requiredPlayerLevel == 20L)
        check(flame.evolutions.size == 2)
        check(flame.evolutions.all {
            it.requiredAffinityTitleStringKey ==
                com.idlerpg.game.presentation.content.PresentationStringKey.EMBER
        })
        val emberRequirement = flame.unlock.masteryRequirements.single()
        check(emberRequirement.affinityId == com.idlerpg.game.domain.definition.Affinity.EMBER.id)
        check(emberRequirement.requiredLevel == 3L)
        check(emberRequirement.currentLevel == 0L)
        check(!emberRequirement.met)

        val frost = fresh.availableSkills.single {
            it.skillId == DefaultGameContent.FROST_LANCE_ID
        }
        check(frost.evolutions.size == 2)
        check(frost.evolutions.all {
            it.requiredAffinityTitleStringKey ==
                com.idlerpg.game.presentation.content.PresentationStringKey.FROST
        })
        val umbral = fresh.availableSkills.single {
            it.skillId == DefaultGameContent.UMBRAL_CUT_ID
        }
        check(umbral.evolutions.size == 2)
        check(umbral.evolutions.all {
            it.requiredAffinityTitleStringKey ==
                com.idlerpg.game.presentation.content.PresentationStringKey.SHADOW
        })

        queries.masteryLevel = 3L
        val multiSelectedState = GameState.newGame(randomSeed = 806L).let { base ->
            base.copy(run = base.run.copy(player = base.run.player.copy(
                selectedSkillEvolutionBySkillId = mapOf(
                    DefaultGameContent.FLAME_BRAND_ID to
                        DefaultGameContent.WILDSPARK_EVOLUTION_ID,
                    DefaultGameContent.FROST_LANCE_ID to
                        DefaultGameContent.SHATTER_SPEAR_EVOLUTION_ID,
                    DefaultGameContent.UMBRAL_CUT_ID to
                        DefaultGameContent.SANGUINE_EDGE_EVOLUTION_ID
                )
            )))
        }
        val multiSelected = projector.project(multiSelectedState).availableSkills
        check(multiSelected.single { it.skillId == DefaultGameContent.FLAME_BRAND_ID }
            .evolutions.single { it.evolutionId == DefaultGameContent.WILDSPARK_EVOLUTION_ID }.selected)
        check(multiSelected.single { it.skillId == DefaultGameContent.FROST_LANCE_ID }
            .evolutions.single { it.evolutionId == DefaultGameContent.SHATTER_SPEAR_EVOLUTION_ID }.selected)
        check(multiSelected.single { it.skillId == DefaultGameContent.UMBRAL_CUT_ID }
            .evolutions.single { it.evolutionId == DefaultGameContent.SANGUINE_EDGE_EVOLUTION_ID }.selected)
        check(multiSelected.filter { it.skillId in setOf(
            DefaultGameContent.FLAME_BRAND_ID,
            DefaultGameContent.FROST_LANCE_ID,
            DefaultGameContent.UMBRAL_CUT_ID
        ) }.all { skill -> skill.evolutions.count { it.canSelect } == 1 })
        queries.masteryLevel = 0L
        queries.featuresUnlocked = false

        val canonical = GameState.newGame(randomSeed = 804L).let { base ->
            base.copy(
                run = base.run.copy(
                    player = PlayerState(
                        equippedSkillIds = listOf(
                            DefaultGameContent.HEAVY_STRIKE_ID,
                            DefaultGameContent.QUICK_SLASH_ID
                        )
                    ),
                    combat = CombatState(
                        status = CombatStatus.ACTIVE,
                        playerCombatant = CombatantState(
                            instanceId = InstanceId(1L),
                            currentHealth = GameNumber.of(100L)
                        ),
                        queuedPlayerAction = QueuedPlayerAction.Skill(
                            DefaultGameContent.HEAVY_STRIKE_ID
                        )
                    )
                )
            )
        }
        val equipped = projector.project(canonical)
        check(equipped.equippedCount == 2)
        check(equipped.slots[0].skill?.skillId == DefaultGameContent.HEAVY_STRIKE_ID)
        check(equipped.slots[1].skill?.skillId == DefaultGameContent.QUICK_SLASH_ID)
        check(equipped.slots[2].skill == null)
        check(equipped.slots[3].skill == null)

        val equippedHeavy = equipped.slots[0].skill ?: error("Heavy Strike missing")
        val equippedQuick = equipped.slots[1].skill ?: error("Quick Slash missing")
        check(equippedHeavy.queued)
        check(!equippedHeavy.canMoveEarlier)
        check(equippedHeavy.canMoveLater)
        check(equippedQuick.canMoveEarlier)
        check(!equippedQuick.canMoveLater)
        check(equipped.availableSkills.none {
            it.skillId == DefaultGameContent.HEAVY_STRIKE_ID ||
                it.skillId == DefaultGameContent.QUICK_SLASH_ID
        })

        println("FUI04_SKILL_LOADOUT_PROJECTION_PASS")
    }

    private class LoadoutReadQueries : GameReadQueries {
        var masteryLevel: Long = 0L
        var featuresUnlocked: Boolean = false
        override fun attackPower(state: GameState): GameNumber = GameNumber.of(10L)
        override fun armor(state: GameState): GameNumber = GameNumber.ZERO
        override fun basicAttackInterval(state: GameState): GameDuration =
            GameDuration.ofSeconds(1L)
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
        override fun playerExperienceToNextLevel(state: GameState): GameNumber =
            GameNumber.of(100L)
        override fun masteryLevel(state: GameState, affinityId: ContentId): Long = masteryLevel
        override fun masteryExperienceToNextLevel(
            state: GameState,
            affinityId: ContentId
        ): GameNumber = GameNumber.of(100L)
        override fun isFeatureUnlocked(state: GameState, featureId: ContentId): Boolean =
            featuresUnlocked
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
