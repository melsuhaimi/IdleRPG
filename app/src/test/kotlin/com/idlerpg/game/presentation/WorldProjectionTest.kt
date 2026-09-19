package com.idlerpg.game.presentation

import com.idlerpg.game.core.config.GameConfig
import com.idlerpg.game.core.id.ContentId
import com.idlerpg.game.core.id.InstanceId
import com.idlerpg.game.core.number.GameNumber
import com.idlerpg.game.core.number.GameRate
import com.idlerpg.game.core.number.Ratio
import com.idlerpg.game.core.time.GameDuration
import com.idlerpg.game.data.content.DefaultGameContent
import com.idlerpg.game.data.content.TrainingHollowAdaptationContent
import com.idlerpg.game.domain.command.CommandRejectionReason
import com.idlerpg.game.domain.definition.Affinity
import com.idlerpg.game.domain.model.GameState
import com.idlerpg.game.domain.model.adaptation.ActiveMutationState
import com.idlerpg.game.domain.model.adaptation.AffinityExposureState
import com.idlerpg.game.domain.model.adaptation.AdaptationState
import com.idlerpg.game.domain.model.adaptation.RegionAdaptationState
import com.idlerpg.game.domain.model.combat.CombatState
import com.idlerpg.game.domain.model.combat.CombatStatus
import com.idlerpg.game.domain.model.combat.CombatantState
import com.idlerpg.game.domain.model.combat.EnemyState
import com.idlerpg.game.domain.model.inventory.InventoryState
import com.idlerpg.game.domain.model.world.EncounterState
import com.idlerpg.game.domain.model.world.WorldState
import com.idlerpg.game.presentation.content.PresentationContentRegistry
import com.idlerpg.game.presentation.model.WorldEncounterStatusUi
import com.idlerpg.game.presentation.projection.WorldProjector
import com.idlerpg.game.presentation.query.GameReadQueries
import com.idlerpg.game.presentation.query.WorldReadQueries

/** Canonical-state/content -> FUI-05 World/Adaptation projection regression. */
object WorldProjectionTest {
    @JvmStatic
    fun main(args: Array<String>) {
        val content = DefaultGameContent.registry()
        val presentation = PresentationContentRegistry.default()
        val queries = ProjectionReadQueries()
        val projector = WorldProjector(
            contentRegistry = content,
            presentationContentRegistry = presentation,
            readQueries = queries,
            worldReadQueries = ProjectionWorldReadQueries()
        )

        val fresh = projector.project(GameState.newGame(randomSeed = 805L))
        check(fresh.regions.size == 1)
        val freshRegion = fresh.regions.single()
        check(freshRegion.unlocked)
        check(!freshRegion.selected)
        check(freshRegion.adaptation.size == 8)
        check(freshRegion.hiddenMutationCount == 8)
        check(!freshRegion.adaptationForecastUnlocked)
        check(freshRegion.mutations.isEmpty())
        check(!fresh.inventory.blocked)

        val enemyId = InstanceId(22L)
        val base = GameState.newGame(randomSeed = 805L)
        val active = base.copy(
            run = base.run.copy(
                world = WorldState(
                    activeRegionId = DefaultGameContent.TRAINING_HOLLOW_REGION_ID,
                    unlockedRegionIds = setOf(DefaultGameContent.TRAINING_HOLLOW_REGION_ID),
                    currentEncounter = EncounterState(
                        definitionId = DefaultGameContent.TRAINING_SLIME_ENCOUNTER_ID,
                        encounterIndex = 3L,
                        encounterSeed = 805L,
                        spawnedEnemyIds = listOf(enemyId)
                    )
                ),
                combat = CombatState(
                    status = CombatStatus.ACTIVE,
                    playerCombatant = CombatantState(
                        instanceId = InstanceId(11L),
                        currentHealth = GameNumber.of(100L)
                    ),
                    enemies = listOf(
                        EnemyState(
                            instanceId = enemyId,
                            definitionId = DefaultGameContent.SLIME_ID,
                            combatant = CombatantState(
                                instanceId = enemyId,
                                currentHealth = GameNumber.of(100L)
                            ),
                            activeMutations = listOf(
                                ActiveMutationState(
                                    mutationId = DefaultGameContent.ASH_SKIN_MUTATION_ID,
                                    sourceAffinityId = Affinity.EMBER.id,
                                    adaptationTier = 1
                                )
                            )
                        )
                    ),
                    combatSequenceId = 1L,
                    encounterStartedAt = com.idlerpg.game.core.time.GameTime.ZERO
                ),
                adaptation = AdaptationState(
                    regionStateById = mapOf(
                        DefaultGameContent.TRAINING_HOLLOW_REGION_ID to
                            RegionAdaptationState(
                                exposureByAffinityId = mapOf(
                                    Affinity.EMBER.id to AffinityExposureState(
                                        pressure = GameNumber.of(75L),
                                        currentEncounterContribution = GameNumber.of(4L),
                                        recentEncounterContribution = GameNumber.of(10L)
                                    )
                                ),
                                tierByAffinityId = mapOf(Affinity.EMBER.id to 1),
                                unlockedMutationIds = setOf(
                                    DefaultGameContent.ASH_SKIN_MUTATION_ID
                                )
                            )
                    )
                )
            )
        )
        val activeUi = projector.project(active)
        val region = activeUi.regions.single()
        check(region.selected)
        val encounter = region.encounters.single {
            it.encounterId == DefaultGameContent.TRAINING_SLIME_ENCOUNTER_ID
        }
        check(encounter.status == WorldEncounterStatusUi.ACTIVE)
        check(encounter.canRetreat)
        check(!encounter.canStart)
        val ember = region.adaptation.single { it.affinityId == Affinity.EMBER.id }
        check(ember.pressureDisplay == "75")
        check(ember.tier == 1)
        check(ember.nextTier == 2)
        check(ember.nextThresholdDisplay == "250")
        val ashSkin = region.mutations.single()
        check(ashSkin.currentlyEligible)
        check(ashSkin.activeOnCurrentEnemy)
        check(!ashSkin.forecastOnly)
        check(ashSkin.effects == listOf(
            com.idlerpg.game.presentation.model.WorldMutationEffectUi(
                kind = com.idlerpg.game.presentation.model.WorldMutationEffectKindUi.AFFINITY_RESISTANCE,
                magnitudeDisplay = "25%"
            )
        ))
        check(region.hiddenMutationCount == 7)
        check(region.activeEnemyRewardMultiplierDisplay == "150%")

        val allKnown = active.copy(run = active.run.copy(
            adaptation = active.run.adaptation.copy(
                regionStateById = active.run.adaptation.regionStateById +
                    (DefaultGameContent.TRAINING_HOLLOW_REGION_ID to
                        checkNotNull(active.run.adaptation.regionStateById[
                            DefaultGameContent.TRAINING_HOLLOW_REGION_ID
                        ]).copy(unlockedMutationIds = content.allMutations().map { it.id }.toSet()))
            )
        ))
        val knownMutations = projector.project(allKnown).regions.single().mutations.associateBy { it.mutationId }
        fun behavior(id: ContentId) = checkNotNull(knownMutations[id]).effects.last()
        check(behavior(TrainingHollowAdaptationContent.REFLEX_CARAPACE_ID).magnitudeDisplay == "15%")
        check(behavior(TrainingHollowAdaptationContent.NULL_VEIL_ID).magnitudeDisplay == "1")
        check(behavior(TrainingHollowAdaptationContent.BLIGHTBLOOD_ID).magnitudeDisplay == "30%")
        check(behavior(TrainingHollowAdaptationContent.SIEGE_HUNGER_ID).magnitudeDisplay == "15%")

        val blockedBase = GameState.newGame(randomSeed = 806L)
        val blocked = blockedBase.copy(
            run = blockedBase.run.copy(
                world = WorldState(
                    activeRegionId = DefaultGameContent.TRAINING_HOLLOW_REGION_ID,
                    unlockedRegionIds = setOf(DefaultGameContent.TRAINING_HOLLOW_REGION_ID)
                ),
                inventory = InventoryState(
                    slotCapacity = 1L,
                    itemsById = emptyMap()
                )
            )
        )
        queries.blocked = true
        val blockedUi = projector.project(blocked)
        check(blockedUi.inventory.blocked)
        check(!blockedUi.regions.single().encounters.single().canStart)

        println("FUI05_WORLD_PROJECTION_PASS")
    }

    private class ProjectionWorldReadQueries : WorldReadQueries {
        override fun nextAdaptationThreshold(
            regionId: ContentId,
            currentTier: Int
        ): GameNumber? = when (currentTier) {
            0 -> GameNumber.of(60L)
            1 -> GameNumber.of(250L)
            2 -> GameNumber.of(500L)
            3 -> GameNumber.of(900L)
            else -> null
        }

        override fun activeEnemyRewardMultiplier(state: GameState): Ratio =
            if (state.run.combat.enemies.firstOrNull()?.activeMutations?.isNotEmpty() == true) {
                Ratio.ofUnits(15_000L)
            } else {
                Ratio.ONE
            }
    }

    private class ProjectionReadQueries : GameReadQueries {
        var blocked: Boolean = false
        override fun attackPower(state: GameState): GameNumber = GameNumber.of(10L)
        override fun armor(state: GameState): GameNumber = GameNumber.ZERO
        override fun basicAttackInterval(state: GameState): GameDuration = GameDuration.ofSeconds(1L)
        override fun basicAttackDps(state: GameState): GameRate = GameRate.of(10L)
        override fun enemyMaximumHealth(
            state: GameState,
            enemy: EnemyState
        ): GameNumber = GameNumber.of(100L)
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
        override fun inventoryProgressionBlocked(state: GameState): Boolean = blocked
    }
}
