package com.idlerpg.game.ui.navigation

import com.idlerpg.game.core.number.GameNumber
import com.idlerpg.game.data.content.DefaultGameContent
import com.idlerpg.game.data.content.HollowWardenContent
import com.idlerpg.game.domain.model.GameState

/** Presentation-only navigation availability derived from canonical progress. */
data class NavigationAvailability(
    val primaryDestinations: List<GameDestination>,
    val progressDestinations: List<ProgressDestination>
) {
    fun allows(destination: GameDestination): Boolean =
        destination in primaryDestinations ||
            (destination == GameDestination.DOCTRINE && GameDestination.GEAR in primaryDestinations)

    fun sanitize(destination: GameDestination): GameDestination =
        destination.takeIf(::allows) ?: GameDestination.BATTLE

    fun sanitize(destination: ProgressDestination): ProgressDestination =
        destination.takeIf { it in progressDestinations } ?: ProgressDestination.OVERVIEW

    fun resolve(
        destination: GameDestination,
        progressDestination: ProgressDestination
    ): NavigationSelection {
        val effectiveDestination = sanitize(destination)
        return NavigationSelection(
            destination = effectiveDestination,
            progressDestination = if (effectiveDestination == GameDestination.PROGRESS) {
                sanitize(progressDestination)
            } else {
                ProgressDestination.OVERVIEW
            }
        )
    }

    companion object {
        val BOOTSTRAP = NavigationAvailability(
            primaryDestinations = listOf(GameDestination.BATTLE, GameDestination.WORLD),
            progressDestinations = listOf(ProgressDestination.OVERVIEW)
        )
    }
}

data class NavigationSelection(
    val destination: GameDestination,
    val progressDestination: ProgressDestination
)

/**
 * Route-level Back actions. These actions intentionally stop at the app's root Battle screen;
 * the Android activity remains responsible for exiting once this policy returns [NONE].
 */
enum class NavigationBackAction {
    NONE,
    CLOSE_SKILL_LOADOUT,
    SHOW_PROGRESS_OVERVIEW,
    OPEN_BUILD,
    OPEN_BATTLE
}

/** Pure navigation policy used by Compose so Back behavior is deterministic and testable. */
object NavigationBackPolicy {
    fun resolve(
        destination: GameDestination,
        progressDestination: ProgressDestination,
        skillLoadoutOpen: Boolean
    ): NavigationBackAction = when {
        skillLoadoutOpen -> NavigationBackAction.CLOSE_SKILL_LOADOUT
        destination == GameDestination.PROGRESS &&
            progressDestination != ProgressDestination.OVERVIEW ->
            NavigationBackAction.SHOW_PROGRESS_OVERVIEW
        destination == GameDestination.DOCTRINE -> NavigationBackAction.OPEN_BUILD
        destination != GameDestination.BATTLE -> NavigationBackAction.OPEN_BATTLE
        else -> NavigationBackAction.NONE
    }
}

/**
 * Unfolds the game when each system becomes useful. It never writes canonical state.
 * [coreGrowthAffordable] is supplied by the existing read-query-backed Progress projector.
 */
object ProgressiveDisclosurePolicy {
    private const val BUILD_LEVEL = 2L
    private const val WARDEN_CORE_ENTRY_TIER = 20L

    fun project(
        state: GameState?,
        coreGrowthAffordable: Boolean = false
    ): NavigationAvailability {
        if (state == null) return NavigationAvailability.BOOTSTRAP

        val priorChronicle = state.meta.chronicle.completedChronicles > GameNumber.ZERO
        val playerLevel = state.run.progression.playerLevel
        val hasRunProgress = playerLevel.level >= BUILD_LEVEL ||
            playerLevel.currentExperience > GameNumber.ZERO ||
            state.run.world.regionProgressById.values.any {
                it.highestClearedEncounterTier > 0L ||
                    it.normalClears > GameNumber.ZERO ||
                    it.eliteClears > GameNumber.ZERO
            }
        val hasGrowthEvidence = state.run.economy.upgrades.levelByUpgradeId.values.any { it > 0L }
        // Build is a first-session decision surface, not a reward-gated destination. The Build
        // screen owns its empty-inventory state, while Skills must be reachable before the first
        // drop so the player can understand the available combat choices immediately.
        val buildVisible = true
        val growthVisible = priorChronicle ||
            hasRunProgress ||
            hasGrowthEvidence ||
            coreGrowthAffordable

        val primary = buildList {
            add(GameDestination.BATTLE)
            add(GameDestination.WORLD)
            if (buildVisible) add(GameDestination.GEAR)
            if (growthVisible) add(GameDestination.PROGRESS)
        }

        if (!growthVisible) {
            return NavigationAvailability(primary, listOf(ProgressDestination.OVERVIEW))
        }

        val masteryVisible = priorChronicle ||
            state.run.progression.affinityMastery.experienceByAffinityId.values.any {
                it > GameNumber.ZERO
            }
        val questsVisible = priorChronicle || state.run.quests.progressByQuestId.isNotEmpty()
        val achievementsVisible = priorChronicle ||
            state.meta.achievements.progressByAchievementId.isNotEmpty()
        val discoveries = state.meta.discoveries
        val discoveriesVisible = priorChronicle ||
            discoveries.discoveredConvergenceIds.isNotEmpty() ||
            discoveries.discoveredMutationIds.isNotEmpty() ||
            discoveries.discoveredEnemyKnowledgeIds.isNotEmpty() ||
            discoveries.unlockedHiddenContentIds.isNotEmpty()
        val echoes = state.meta.echoes
        val echoVisible = priorChronicle ||
            echoes.available > GameNumber.ZERO ||
            echoes.spent > GameNumber.ZERO ||
            echoes.purchasedOfferIds.isNotEmpty()
        val trainingProgress = state.run.world.regionProgressById[
            DefaultGameContent.TRAINING_HOLLOW_REGION_ID
        ]
        val chronicleVisible = priorChronicle ||
            (trainingProgress?.highestClearedEncounterTier ?: 0L) >= WARDEN_CORE_ENTRY_TIER ||
            HollowWardenContent.BOSS_ID in (trainingProgress?.clearedBossIds ?: emptySet())

        val progress = buildList {
            add(ProgressDestination.OVERVIEW)
            add(ProgressDestination.CORE_GROWTH)
            if (masteryVisible) add(ProgressDestination.MASTERY)
            if (questsVisible) add(ProgressDestination.QUESTS)
            if (achievementsVisible) add(ProgressDestination.ACHIEVEMENTS)
            if (discoveriesVisible) add(ProgressDestination.DISCOVERIES)
            if (echoVisible) add(ProgressDestination.ECHO)
            if (chronicleVisible) add(ProgressDestination.CHRONICLE)
        }
        return NavigationAvailability(primary, progress)
    }
}
