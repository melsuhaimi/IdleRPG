package com.idlerpg.game.presentation.model

import com.idlerpg.game.core.id.ContentId
import com.idlerpg.game.domain.command.CommandRejectionCode
import com.idlerpg.game.presentation.content.PresentationAssetKey
import com.idlerpg.game.presentation.content.PresentationStringKey
import com.idlerpg.game.domain.model.world.WorldAutomationMode
import com.idlerpg.game.domain.model.world.PushFailurePolicy

/** Presentation-only encounter category. */
enum class WorldEncounterTypeUi {
    NORMAL,
    ELITE,
    ANOMALY,
    BOSS
}

enum class WorldEncounterStatusUi {
    IDLE,
    ACTIVE,
    CLEARED,
    FAILED,
    RETREATED
}

data class WorldEncounterUiState(
    val encounterId: ContentId,
    val titleStringKey: PresentationStringKey,
    val iconAssetKey: PresentationAssetKey,
    val type: WorldEncounterTypeUi,
    val status: WorldEncounterStatusUi,
    val encounterIndex: Long?,
    val enemyTitleStringKey: PresentationStringKey,
    val enemyIllustrationAssetKey: PresentationAssetKey?,
    val encounterFrameAssetKey: PresentationAssetKey,
    val attackTitle: String,
    val attackAffinityStringKey: PresentationStringKey,
    val attackDamageDisplay: String,
    val attackIntervalMillis: Long,
    val armorPenetrationDisplay: String,
    val selectedRegion: Boolean,
    val canStart: Boolean,
    val canRetreat: Boolean,
    val cleared: Boolean,
    val sectorIndex: Int,
    val sectorTitleStringKey: PresentationStringKey,
    val sectorBackgroundAssetKey: PresentationAssetKey,
    val rewardFocus: String,
    val stageLabel: String? = null,
    val waveCount: Int = 1,
    val difficultyDisplay: String? = null,
    val expectedGoldDisplay: String? = null,
    val expectedExperienceDisplay: String? = null,
    val rewardMultiplierDisplay: String? = null
)

data class WorldAffinityAdaptationUiState(
    val affinityId: ContentId,
    val titleStringKey: PresentationStringKey,
    val iconAssetKey: PresentationAssetKey,
    val pressureDisplay: String,
    val tier: Int,
    val nextTier: Int?,
    val nextThresholdDisplay: String?,
    val currentEncounterContributionDisplay: String,
    val recentEncounterContributionDisplay: String,
    val hasPressure: Boolean = false
)

data class WorldMutationUiState(
    val mutationId: ContentId,
    val titleStringKey: PresentationStringKey,
    val iconAssetKey: PresentationAssetKey,
    val triggerAffinityStringKey: PresentationStringKey,
    val minimumTier: Int,
    val currentlyEligible: Boolean,
    val activeOnCurrentEnemy: Boolean,
    val forecastOnly: Boolean,
    val effects: List<WorldMutationEffectUi>
)

data class WorldMutationEffectUi(
    val kind: WorldMutationEffectKindUi,
    val magnitudeDisplay: String
)

enum class WorldMutationEffectKindUi {
    AFFINITY_RESISTANCE,
    FASTER_CADENCE,
    EXTRA_RESONANCE_DRAIN,
    HEALING_SUPPRESSION,
    GUARD_PRESSURE
}

data class WorldRegionUiState(
    val regionId: ContentId,
    val titleStringKey: PresentationStringKey,
    val descriptionStringKey: PresentationStringKey,
    val iconAssetKey: PresentationAssetKey,
    val illustrationAssetKey: PresentationAssetKey?,
    val unlocked: Boolean,
    val selected: Boolean,
    val highestClearedEncounterTier: Long,
    val normalClearsDisplay: String,
    val eliteClearsDisplay: String,
    val encounters: List<WorldEncounterUiState>,
    val adaptation: List<WorldAffinityAdaptationUiState>,
    val mutations: List<WorldMutationUiState>,
    val hiddenMutationCount: Int,
    val adaptationForecastUnlocked: Boolean,
    val activeEnemyRewardMultiplierDisplay: String
)

data class WorldInventoryBlockUiState(
    val blocked: Boolean,
    val normalUsed: Long,
    val normalCapacity: Long,
    val overflowUsed: Long,
    val overflowCapacity: Long
)

enum class WorldFeedbackKind {
    COMMAND_REJECTED,
    REGION_SELECTED,
    ENCOUNTER_STARTED,
    ENCOUNTER_CLEARED,
    ENCOUNTER_RETREATED,
    ENCOUNTER_FAILED,
    ADAPTATION_TIER_CHANGED,
    MUTATION_ROLLED,
    INVENTORY_BLOCKED,
    INVENTORY_UNBLOCKED
}

data class WorldFeedbackUiState(
    val sequenceNumber: Long?,
    val kind: WorldFeedbackKind,
    val contentId: ContentId? = null,
    val affinityId: ContentId? = null,
    val tier: Int? = null,
    val rejectionCode: CommandRejectionCode? = null
)

data class WorldUiState(
    val regions: List<WorldRegionUiState>,
    val activeRegionId: ContentId?,
    val inventory: WorldInventoryBlockUiState,
    val automationMode: WorldAutomationMode = WorldAutomationMode.PUSH,
    val selectedFarmEncounterId: ContentId? = null,
    val pushFailurePolicy: PushFailurePolicy = PushFailurePolicy.FARM_HIGHEST_CLEARED,
    val feedback: WorldFeedbackUiState? = null
)
