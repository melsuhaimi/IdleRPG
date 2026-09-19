package com.idlerpg.game.simulation

import com.idlerpg.game.core.id.ContentId
import com.idlerpg.game.core.id.InstanceId
import com.idlerpg.game.core.number.GameNumber
import com.idlerpg.game.data.content.DefaultGameContent
import com.idlerpg.game.data.content.HollowWardenContent
import com.idlerpg.game.domain.definition.Rarity
import com.idlerpg.game.domain.model.GameState
import com.idlerpg.game.domain.model.achievement.AchievementProgressState
import com.idlerpg.game.domain.model.chronicle.ChronicleState
import com.idlerpg.game.domain.model.chronicle.DiscoveryState
import com.idlerpg.game.domain.model.chronicle.EchoState
import com.idlerpg.game.domain.model.economy.UpgradeProgressState
import com.idlerpg.game.domain.model.inventory.ItemInstance
import com.idlerpg.game.domain.model.progression.AffinityMasteryState
import com.idlerpg.game.domain.model.progression.PlayerLevelState
import com.idlerpg.game.domain.model.quest.QuestProgressState
import com.idlerpg.game.domain.model.world.RegionProgressState
import com.idlerpg.game.ui.navigation.GameDestination
import com.idlerpg.game.ui.navigation.NavigationAvailability
import com.idlerpg.game.ui.navigation.ProgressDestination
import com.idlerpg.game.ui.navigation.ProgressiveDisclosurePolicy

/** Presentation-only progressive disclosure remains deterministic and Chronicle-safe. */
object ProgressiveDisclosureScenarioTest {
    private val sampleId = ContentId("test.progressive_disclosure")

    fun run() {
        val pristine = SimulationTestSupport.factory().newGame(10_001L).state()
        val bootstrap = ProgressiveDisclosurePolicy.project(pristine)
        check(bootstrap.primaryDestinations == listOf(
            GameDestination.BATTLE,
            GameDestination.WORLD,
            GameDestination.GEAR
        ))
        check(bootstrap.allows(GameDestination.GEAR))
        check(bootstrap.allows(GameDestination.DOCTRINE))
        check(bootstrap.sanitize(GameDestination.PROGRESS) == GameDestination.BATTLE)
        check(bootstrap.sanitize(ProgressDestination.CHRONICLE) == ProgressDestination.OVERVIEW)
        val hiddenProgress = bootstrap.resolve(
            GameDestination.PROGRESS,
            ProgressDestination.CHRONICLE
        )
        check(hiddenProgress.destination == GameDestination.BATTLE)
        check(hiddenProgress.progressDestination == ProgressDestination.OVERVIEW)
        check(ProgressiveDisclosurePolicy.project(null) == NavigationAvailability.BOOTSTRAP)

        val affordable = ProgressiveDisclosurePolicy.project(pristine, coreGrowthAffordable = true)
        check(GameDestination.PROGRESS in affordable.primaryDestinations)
        check(GameDestination.GEAR in affordable.primaryDestinations)
        check(affordable.progressDestinations == listOf(
            ProgressDestination.OVERVIEW,
            ProgressDestination.CORE_GROWTH
        ))
        check(affordable.sanitize(ProgressDestination.CHRONICLE) ==
            ProgressDestination.OVERVIEW)
        val hiddenChronicle = affordable.resolve(
            GameDestination.PROGRESS,
            ProgressDestination.CHRONICLE
        )
        check(hiddenChronicle.destination == GameDestination.PROGRESS)
        check(hiddenChronicle.progressDestination == ProgressDestination.OVERVIEW)

        val firstProgress = pristine.copy(run = pristine.run.copy(
            progression = pristine.run.progression.copy(
                playerLevel = PlayerLevelState(currentExperience = GameNumber.ONE)
            )
        ))
        check(GameDestination.GEAR in
            ProgressiveDisclosurePolicy.project(firstProgress).primaryDestinations)

        val firstClear = pristine.copy(run = pristine.run.copy(
            world = pristine.run.world.copy(
                unlockedRegionIds = setOf(DefaultGameContent.TRAINING_HOLLOW_REGION_ID),
                regionProgressById = mapOf(
                    DefaultGameContent.TRAINING_HOLLOW_REGION_ID to
                        RegionProgressState(highestClearedEncounterTier = 1L)
                )
            )
        ))
        val retainedAfterSpending = ProgressiveDisclosurePolicy.project(firstClear)
        check(GameDestination.GEAR in retainedAfterSpending.primaryDestinations)
        check(GameDestination.PROGRESS in retainedAfterSpending.primaryDestinations)

        val levelTwo = pristine.copy(run = pristine.run.copy(
            progression = pristine.run.progression.copy(playerLevel = PlayerLevelState(level = 2L))
        ))
        val levelAvailability = ProgressiveDisclosurePolicy.project(levelTwo)
        check(GameDestination.GEAR in levelAvailability.primaryDestinations)
        check(GameDestination.PROGRESS in levelAvailability.primaryDestinations)
        check(levelAvailability.allows(GameDestination.DOCTRINE))
        val buildRequest = bootstrap.sanitize(GameDestination.GEAR)
        check(buildRequest == GameDestination.GEAR)
        check(levelAvailability.sanitize(buildRequest) == GameDestination.GEAR)
        check(levelAvailability.resolve(
            buildRequest,
            ProgressDestination.CHRONICLE
        ).destination == GameDestination.GEAR)

        val item = ItemInstance(
            instanceId = InstanceId(99_001L),
            definitionId = DefaultGameContent.TRAINING_BLADE_ITEM_ID,
            rarity = Rarity.RARE
        )
        val itemState = pristine.copy(run = pristine.run.copy(
            inventory = pristine.run.inventory.copy(itemsById = mapOf(item.instanceId to item))
        ))
        check(GameDestination.GEAR in
            ProgressiveDisclosurePolicy.project(itemState).primaryDestinations)

        val revealedSections = levelTwo.copy(
            run = levelTwo.run.copy(
                economy = levelTwo.run.economy.copy(
                    upgrades = UpgradeProgressState(mapOf(
                        DefaultGameContent.BASIC_ATTACK_POWER_UPGRADE_ID to 1L
                    ))
                ),
                progression = levelTwo.run.progression.copy(
                    playerLevel = PlayerLevelState(level = 2L),
                    affinityMastery = AffinityMasteryState(mapOf(sampleId to GameNumber.ONE))
                ),
                quests = levelTwo.run.quests.copy(
                    progressByQuestId = mapOf(sampleId to QuestProgressState())
                )
            ),
            meta = levelTwo.meta.copy(
                achievements = levelTwo.meta.achievements.copy(
                    progressByAchievementId = mapOf(sampleId to AchievementProgressState())
                ),
                discoveries = DiscoveryState(discoveredConvergenceIds = setOf(sampleId)),
                echoes = EchoState(available = GameNumber.ONE)
            )
        )
        val sections = ProgressiveDisclosurePolicy.project(revealedSections).progressDestinations
        check(ProgressDestination.MASTERY in sections)
        check(ProgressDestination.QUESTS in sections)
        check(ProgressDestination.ACHIEVEMENTS in sections)
        check(ProgressDestination.DISCOVERIES in sections)
        check(ProgressDestination.ECHO in sections)
        check(ProgressDestination.CHRONICLE !in sections)

        fun withTrainingProgress(progress: RegionProgressState): GameState = levelTwo.copy(
            run = levelTwo.run.copy(world = levelTwo.run.world.copy(
                unlockedRegionIds = setOf(DefaultGameContent.TRAINING_HOLLOW_REGION_ID),
                regionProgressById = mapOf(DefaultGameContent.TRAINING_HOLLOW_REGION_ID to progress)
            ))
        )
        val wardenCore = ProgressiveDisclosurePolicy.project(withTrainingProgress(
            RegionProgressState(highestClearedEncounterTier = 20L)
        ))
        check(ProgressDestination.CHRONICLE in wardenCore.progressDestinations)
        val bossCleared = ProgressiveDisclosurePolicy.project(withTrainingProgress(
            RegionProgressState(clearedBossIds = setOf(HollowWardenContent.BOSS_ID))
        ))
        check(ProgressDestination.CHRONICLE in bossCleared.progressDestinations)

        val postChronicle = pristine.copy(meta = pristine.meta.copy(
            chronicle = ChronicleState(completedChronicles = GameNumber.ONE)
        ))
        val retained = ProgressiveDisclosurePolicy.project(postChronicle)
        check(retained.primaryDestinations == listOf(
            GameDestination.BATTLE,
            GameDestination.WORLD,
            GameDestination.GEAR,
            GameDestination.PROGRESS
        ))
        check(ProgressDestination.ECHO in retained.progressDestinations)
        check(ProgressDestination.CHRONICLE in retained.progressDestinations)
        check(retained.progressDestinations == ProgressDestination.values().toList())
        check(retained.sanitize(GameDestination.PROGRESS) == GameDestination.PROGRESS)
        check(retained.sanitize(ProgressDestination.CHRONICLE) ==
            ProgressDestination.CHRONICLE)
        check(retained == ProgressiveDisclosurePolicy.project(postChronicle))
        check(postChronicle == pristine.copy(meta = pristine.meta.copy(
            chronicle = ChronicleState(completedChronicles = GameNumber.ONE)
        )))
    }
}
