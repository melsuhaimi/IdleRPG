package com.idlerpg.game.presentation

import com.idlerpg.game.core.id.ContentId
import com.idlerpg.game.core.number.GameNumber
import com.idlerpg.game.core.number.GameRate
import com.idlerpg.game.core.time.GameDuration
import com.idlerpg.game.data.content.DefaultGameContent
import com.idlerpg.game.domain.command.CommandRejectionReason
import com.idlerpg.game.domain.model.GameState
import com.idlerpg.game.domain.model.MetaState
import com.idlerpg.game.domain.model.chronicle.ChronicleState
import com.idlerpg.game.domain.model.chronicle.DiscoveryState
import com.idlerpg.game.domain.model.chronicle.EchoState
import com.idlerpg.game.domain.model.achievement.AchievementProgressState
import com.idlerpg.game.domain.model.achievement.AchievementState
import com.idlerpg.game.domain.model.combat.EnemyState
import com.idlerpg.game.domain.model.progression.AffinityMasteryState
import com.idlerpg.game.domain.model.progression.PlayerLevelState
import com.idlerpg.game.domain.model.progression.ProgressionState
import com.idlerpg.game.domain.model.quest.QuestProgressState
import com.idlerpg.game.domain.model.quest.QuestState
import com.idlerpg.game.presentation.content.PresentationContentRegistry
import com.idlerpg.game.presentation.model.EchoOfferStatus
import com.idlerpg.game.presentation.model.PersistentDiscoveryKind
import com.idlerpg.game.presentation.model.ProgressClaimStatus
import com.idlerpg.game.presentation.projection.ProgressProjector
import com.idlerpg.game.presentation.query.GameReadQueries
import com.idlerpg.game.presentation.query.ProgressReadQueries

/** Canonical state -> FUI-09 Progress projection regression. */
object ProgressProjectionTest {
    @JvmStatic
    fun main(args: Array<String>) {
        val content = DefaultGameContent.registry()
        val presentation = PresentationContentRegistry.default()
        val projector = ProgressProjector(
            contentRegistry = content,
            presentationContentRegistry = presentation,
            readQueries = ProjectionReadQueries(),
            progressReadQueries = ProjectionProgressReadQueries()
        )

        val base = GameState.newGame(randomSeed = 808L)
        val firstHunt = content.allQuests().single { it.id == DefaultGameContent.FIRST_HUNT_QUEST_ID }
        val firstHuntObjective = firstHunt.objectives.single()
        val forgedFlame = content.allAchievements().single()
        val forgedFlameObjective = forgedFlame.objectives.single()

        val state = base.copy(
            run = base.run.copy(
                world = base.run.world.copy(
                    unlockedRegionIds = setOf(DefaultGameContent.TRAINING_HOLLOW_REGION_ID),
                    regionProgressById = mapOf(
                    DefaultGameContent.TRAINING_HOLLOW_REGION_ID to
                        com.idlerpg.game.domain.model.world.RegionProgressState(
                            highestClearedEncounterTier = 30L,
                            normalClears = GameNumber.of(15L),
                            clearedBossIds = setOf(com.idlerpg.game.data.content.HollowWardenContent.BOSS_ID)))),
                progression = ProgressionState(
                    playerLevel = PlayerLevelState(
                        level = 3L,
                        currentExperience = GameNumber.of(15L)
                    ),
                    affinityMastery = AffinityMasteryState(
                        experienceByAffinityId = mapOf(
                            com.idlerpg.game.domain.definition.Affinity.EMBER.id to GameNumber.of(42L)
                        )
                    )
                ),
                quests = QuestState(
                    progressByQuestId = mapOf(
                        firstHunt.id to QuestProgressState(
                            progressByObjectiveId = mapOf(
                                firstHuntObjective.id to firstHuntObjective.requiredCount
                            ),
                            completionCount = GameNumber.ONE,
                            claimedCount = GameNumber.ZERO
                        )
                    )
                )
            ),
            meta = MetaState(
                chronicle = ChronicleState(
                    currentChronicleNumber = 2L,
                    completedChronicles = GameNumber.ONE,
                    bestMilestoneIds = setOf(ContentId("milestone.chronicle.first_collapse"))
                ),
                echoes = EchoState(
                    available = GameNumber.of(5L),
                    spent = GameNumber.ZERO
                ),
                achievements = AchievementState(
                    progressByAchievementId = mapOf(
                        forgedFlame.id to AchievementProgressState(
                            progressByObjectiveId = mapOf(
                                forgedFlameObjective.id to forgedFlameObjective.requiredCount
                            ),
                            completed = true,
                            rewardClaimed = false
                        )
                    )
                )
            )
        )

        val ui = projector.project(state)
        check(ui.overview.playerLevel == 3L)
        check(ui.rebirth.currentLevel == 3L)
        check(!ui.rebirth.eligible)
        check(ui.rebirth.stats.size == 9)
        check(ui.rebirth.stats.any { it.stat.name == "LEGENDARY_FIND" })
        check(ui.overview.nextGoal.targetId != null)
        check(ui.overview.nextGoal.remainingLevels >= 0L)
        check(ui.overview.currentExperienceDisplay == "15")
        check(ui.overview.experienceToNextLevelDisplay == "50")
        check(ui.overview.experienceProgressUnits == 3_000)
        check(ui.coreGrowth.size == 8)
        check(ui.coreGrowth.all { it.purchaseOptions.size == 4 })
        val weaponTraining = ui.coreGrowth.single {
            it.upgradeId == DefaultGameContent.BASIC_ATTACK_POWER_UPGRADE_ID
        }
        check(weaponTraining.nextMilestoneLevel == 10L)
        check(weaponTraining.nextMilestoneBonusLevels == 2L)
        check(weaponTraining.purchaseOptions.map { it.costDisplay } == listOf("20", "650", "3500", "900"))
        check(weaponTraining.purchaseOptions.map { it.enabled } == listOf(true, true, false, true))
        check(weaponTraining.purchaseOptions.last().quantity == 12L)
        check(ui.masteries.size == 8)
        val ember = ui.masteries.single {
            it.affinityId == com.idlerpg.game.domain.definition.Affinity.EMBER.id
        }
        check(ember.level == 2L)
        check(ember.totalExperienceDisplay == "42")
        check(ember.experienceToNextLevelDisplay == "8")

        val quest = ui.quests.single { it.questId == DefaultGameContent.FIRST_HUNT_QUEST_ID }
        check(quest.status == ProgressClaimStatus.READY_TO_CLAIM)
        check(quest.canClaim)
        check(quest.reward.goldDisplay == "25")
        check(quest.objectives.single().progressUnits == 10_000)
        check(quest.completionCountDisplay == "1")
        check(quest.claimedCountDisplay == "0")

        val achievement = ui.achievements.single()
        check(achievement.status == ProgressClaimStatus.READY_TO_CLAIM)
        check(achievement.canClaim)
        check(achievement.reward.goldDisplay == "10")
        check(achievement.objectives.single().complete)

        check(ui.discoveries.isEmpty())
        check(ui.echoShop.availableDisplay == "5")
        check(ui.echoShop.spentDisplay == "0")
        check(ui.echoShop.lifetimeEarnedDisplay == "5")
        val forecastOffer = ui.echoShop.offers.single { it.offerId == DefaultGameContent.ADAPTATION_FORECAST_ECHO_OFFER_ID }
        check(forecastOffer.status == EchoOfferStatus.AFFORDABLE)
        check(forecastOffer.canPurchase)
        check(forecastOffer.costDisplay == "5")
        check(ui.chronicle.currentChronicleNumber == 2L)
        check(ui.chronicle.completedChroniclesDisplay == "1")
        check(ui.chronicle.currentNormalClearsDisplay == "15")
        check(ui.chronicle.requiredNormalClearsDisplay == "15")
        check(ui.chronicle.eligible)
        check(ui.chronicle.echoRewardDisplay == "35")
        check(ui.chronicle.bestMilestones.size == 1)

        val claimedState = state.copy(
            run = state.run.copy(
                quests = QuestState(
                    progressByQuestId = mapOf(
                        firstHunt.id to state.run.quests.progressFor(firstHunt.id).copy(
                            claimedCount = GameNumber.ONE
                        )
                    )
                )
            ),
            meta = state.meta.copy(
                echoes = EchoState(
                    available = GameNumber.ZERO,
                    spent = GameNumber.of(5L),
                    purchasedOfferIds = setOf(ContentId("echo_unlock.adaptation_forecast"))
                ),
                discoveries = DiscoveryState(
                    unlockedHiddenContentIds = setOf(
                        ContentId("discovery.adaptation_forecast")
                    )
                ),
                achievements = AchievementState(
                    progressByAchievementId = mapOf(
                        forgedFlame.id to state.meta.achievements.progressFor(forgedFlame.id).copy(
                            rewardClaimed = true
                        )
                    )
                )
            )
        )
        val claimedUi = projector.project(claimedState)
        check(claimedUi.quests.single { it.questId == DefaultGameContent.FIRST_HUNT_QUEST_ID }.status == ProgressClaimStatus.CLAIMED)
        check(!claimedUi.quests.single { it.questId == DefaultGameContent.FIRST_HUNT_QUEST_ID }.canClaim)
        check(claimedUi.achievements.single().status == ProgressClaimStatus.CLAIMED)
        check(!claimedUi.achievements.single().canClaim)
        val claimedForecast = claimedUi.echoShop.offers.single { it.offerId == DefaultGameContent.ADAPTATION_FORECAST_ECHO_OFFER_ID }
        check(claimedForecast.status == EchoOfferStatus.PURCHASED)
        check(!claimedForecast.canPurchase)
        check(claimedUi.echoShop.lifetimeEarnedDisplay == "5")
        val discovery = claimedUi.discoveries.single()
        check(discovery.contentId == ContentId("discovery.adaptation_forecast"))
        check(discovery.kind == PersistentDiscoveryKind.HIDDEN_CONTENT)

        println("FUI09_PROGRESS_PROJECTION_PASS")
    }

    private class ProjectionProgressReadQueries : ProgressReadQueries {
        override fun isQuestEligible(state: GameState, questId: ContentId): Boolean = true
        override fun isAchievementEligible(
            state: GameState,
            achievementId: ContentId
        ): Boolean = true
        override fun chronicleTotalNormalClears(state: GameState): GameNumber =
            GameNumber.of(15L)
        override fun isEchoOfferPurchased(state: GameState, offerId: ContentId): Boolean =
            offerId in state.meta.echoes.purchasedOfferIds
        override fun areEchoOfferPrerequisitesSatisfied(
            state: GameState,
            offerId: ContentId
        ): Boolean = true
    }

    private class ProjectionReadQueries : GameReadQueries {
        override fun attackPower(state: GameState): GameNumber = GameNumber.ZERO
        override fun armor(state: GameState): GameNumber = GameNumber.ZERO
        override fun basicAttackInterval(state: GameState): GameDuration = GameDuration.ZERO
        override fun basicAttackDps(state: GameState): GameRate = GameRate.ZERO
        override fun enemyMaximumHealth(state: GameState, enemy: EnemyState): GameNumber =
            GameNumber.ZERO
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
            GameNumber.ZERO
        override fun upgradePurchaseCost(
            state: GameState,
            upgradeId: ContentId,
            quantity: Long
        ): GameNumber = GameNumber.of(5L * quantity * quantity + 15L * quantity)
        override fun maximumAffordableUpgradeQuantity(state: GameState, upgradeId: ContentId): Long =
            12L
        override fun playerExperienceToNextLevel(state: GameState): GameNumber =
            GameNumber.of(50L)
        override fun masteryLevel(state: GameState, affinityId: ContentId): Long =
            if (affinityId.value == "affinity.ember") 2L else 1L
        override fun masteryExperienceToNextLevel(
            state: GameState,
            affinityId: ContentId
        ): GameNumber = if (affinityId.value == "affinity.ember") {
            GameNumber.of(8L)
        } else {
            GameNumber.of(20L)
        }
        override fun isFeatureUnlocked(state: GameState, featureId: ContentId): Boolean = false
        override fun chronicleEligible(state: GameState): Boolean = true
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
