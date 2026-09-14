package com.idlerpg.game.presentation.model

import com.idlerpg.game.core.id.ContentId
import com.idlerpg.game.domain.command.CommandRejectionCode
import com.idlerpg.game.domain.event.ChroniclePersistScope
import com.idlerpg.game.domain.event.ChronicleResetScope
import com.idlerpg.game.presentation.content.PresentationAssetKey
import com.idlerpg.game.presentation.content.PresentationStringKey

/** Canonical claim lifecycle rendered by Progress. */
enum class ProgressClaimStatus {
    LOCKED,
    IN_PROGRESS,
    READY_TO_CLAIM,
    CLAIMED
}

enum class ProgressFeedbackKind {
    COMMAND_REJECTED,
    QUEST_COMPLETED,
    QUEST_REWARD_CLAIMED,
    ACHIEVEMENT_COMPLETED,
    ACHIEVEMENT_REWARD_CLAIMED,
    PLAYER_LEVELED_UP,
    MASTERY_INCREASED,
    CORE_GROWTH_PURCHASED,
    ECHO_OFFER_PURCHASED,
    DISCOVERY_UNLOCKED,
    ECHO_GRANTED,
    CHRONICLE_PREVIEW_READY,
    CHRONICLE_COLLAPSED
}

enum class CoreGrowthEffectKind {
    ATTACK_POWER,
    MAX_HEALTH,
    ARMOR,
    ACTION_SPEED,
    CRITICAL_CHANCE,
    CRITICAL_MULTIPLIER,
    EFFECT_POWER,
    HEALING_POWER
}

data class CoreGrowthPurchaseOptionUiState(
    val quantity: Long,
    val quantityLabel: String,
    val costDisplay: String,
    val enabled: Boolean,
    val maximum: Boolean = false
)

data class CoreGrowthTrackUiState(
    val upgradeId: ContentId,
    val titleStringKey: PresentationStringKey,
    val iconAssetKey: PresentationAssetKey,
    val effectKind: CoreGrowthEffectKind,
    val level: Long,
    val currentEffectDisplay: String,
    val nextMilestoneLevel: Long?,
    val nextMilestoneBonusLevels: Long?,
    val purchaseOptions: List<CoreGrowthPurchaseOptionUiState>,
    val legacyStartingLevel: Long = 0L
)

data class ProgressFeedbackUiState(
    val sequenceNumber: Long?,
    val kind: ProgressFeedbackKind,
    val contentId: ContentId? = null,
    val affinityId: ContentId? = null,
    val amountDisplay: String? = null,
    val rejectionCode: CommandRejectionCode? = null
)

data class ProgressFeatureUiState(
    val featureId: ContentId,
    val titleStringKey: PresentationStringKey,
    val iconAssetKey: PresentationAssetKey
)

/** Compact stat tile data; formulas stay visible in the detail surface, not hidden in code. */
data class StatOverviewUiState(
    val id: String,
    val label: String,
    val valueDisplay: String,
    val description: String,
    val formula: String
)

enum class ProgressNextGoalKind {
    MASTERY_UNLOCK,
    CORE_MILESTONE,
    COMPLETE
}

data class ProgressGoalRequirementUiState(
    val requirementId: ContentId,
    val titleStringKey: PresentationStringKey?,
    val currentLevel: Long,
    val requiredLevel: Long
) {
    val remainingLevels: Long
        get() = (requiredLevel - currentLevel).coerceAtLeast(0L)
}

data class ProgressNextGoalUiState(
    val kind: ProgressNextGoalKind = ProgressNextGoalKind.COMPLETE,
    val requiredLevel: Long = 0L,
    val titleStringKey: PresentationStringKey? = null,
    val bonusLevels: Long = 0L,
    /** Stable authored content target; presentation never derives identity from display text. */
    val targetId: ContentId? = null,
    /** Remaining levels for this target, calculated from the authoritative projection input. */
    val remainingLevels: Long = 0L,
    /** All authored requirements relevant to this target, including already-met requirements. */
    val requirements: List<ProgressGoalRequirementUiState> = emptyList()
)

data class ProgressOverviewUiState(
    val playerLevel: Long,
    val currentExperienceDisplay: String,
    val experienceToNextLevelDisplay: String,
    val experienceProgressUnits: Int,
    val atMaximumPlayerLevel: Boolean,
    val unlockedFeatures: List<ProgressFeatureUiState>,
    val echoAvailableDisplay: String,
    val echoSpentDisplay: String,
    val chronicleEligible: Boolean,
    val experienceRemainingDisplay: String = "0",
    val statCards: List<StatOverviewUiState> = emptyList(),
    val nextGoal: ProgressNextGoalUiState = ProgressNextGoalUiState()
)

data class MasteryUnlockUiState(
    val titleStringKey: PresentationStringKey,
    val requiredLevel: Long
)

data class MasteryProgressUiState(
    val masteryId: ContentId,
    val affinityId: ContentId,
    val titleStringKey: PresentationStringKey,
    val affinityTitleStringKey: PresentationStringKey,
    val iconAssetKey: PresentationAssetKey,
    val level: Long,
    val maximumLevel: Long?,
    val totalExperienceDisplay: String,
    val experienceToNextLevelDisplay: String,
    val atMaximumLevel: Boolean,
    val unlocks: List<MasteryUnlockUiState> = emptyList()
)

data class ObjectiveProgressUiState(
    val objectiveId: ContentId,
    val titleStringKey: PresentationStringKey,
    val iconAssetKey: PresentationAssetKey,
    val currentDisplay: String,
    val requiredDisplay: String,
    val progressUnits: Int,
    val complete: Boolean
)

data class ProgressRewardUiState(
    val goldDisplay: String? = null,
    val experienceDisplay: String? = null,
    val lootTableCount: Int = 0
) {
    val isEmpty: Boolean
        get() = goldDisplay == null && experienceDisplay == null && lootTableCount == 0
}

data class QuestProgressUiState(
    val questId: ContentId,
    val titleStringKey: PresentationStringKey,
    val iconAssetKey: PresentationAssetKey,
    val status: ProgressClaimStatus,
    val requiredPlayerLevel: Long,
    val objectives: List<ObjectiveProgressUiState>,
    val reward: ProgressRewardUiState,
    val completionCountDisplay: String,
    val claimedCountDisplay: String,
    val canClaim: Boolean,
    val repeatable: Boolean = false,
    val pendingCountDisplay: String = "0"
)

data class AchievementProgressUiState(
    val achievementId: ContentId,
    val titleStringKey: PresentationStringKey,
    val iconAssetKey: PresentationAssetKey,
    val status: ProgressClaimStatus,
    val objectives: List<ObjectiveProgressUiState>,
    val reward: ProgressRewardUiState,
    val canClaim: Boolean
)

enum class PersistentDiscoveryKind {
    CONVERGENCE,
    MUTATION,
    ENEMY_KNOWLEDGE,
    HIDDEN_CONTENT
}

data class PersistentDiscoveryUiState(
    val contentId: ContentId,
    val kind: PersistentDiscoveryKind,
    val titleStringKey: PresentationStringKey,
    val iconAssetKey: PresentationAssetKey
)

enum class EchoOfferStatus {
    LOCKED_BY_PREREQUISITE,
    AFFORDABLE,
    INSUFFICIENT_ECHO,
    PURCHASED
}

enum class EchoOfferEffectKind {
    REVEAL_HIDDEN_CONTENT,
    UNLOCK_PERSISTENT_FEATURE
}

data class EchoOfferEffectUiState(
    val kind: EchoOfferEffectKind,
    val targetId: ContentId,
    val titleStringKey: PresentationStringKey,
    val iconAssetKey: PresentationAssetKey
)

data class EchoOfferUiState(
    val offerId: ContentId,
    val titleStringKey: PresentationStringKey,
    val iconAssetKey: PresentationAssetKey,
    val costDisplay: String,
    val effects: List<EchoOfferEffectUiState>,
    val status: EchoOfferStatus,
    val canPurchase: Boolean,
    val descriptionStringKey: PresentationStringKey = PresentationStringKey.DESC_ECHO_OFFER
)

data class EchoShopUiState(
    val availableDisplay: String,
    val spentDisplay: String,
    val lifetimeEarnedDisplay: String,
    val offers: List<EchoOfferUiState>
)

data class ChronicleMilestoneUiState(
    val milestoneId: ContentId,
    val titleStringKey: PresentationStringKey,
    val iconAssetKey: PresentationAssetKey
)

data class ChronicleProgressUiState(
    val chronicleId: ContentId,
    val titleStringKey: PresentationStringKey,
    val iconAssetKey: PresentationAssetKey,
    val currentChronicleNumber: Long,
    val completedChroniclesDisplay: String,
    val currentNormalClearsDisplay: String,
    val requiredNormalClearsDisplay: String,
    val echoRewardDisplay: String,
    val eligible: Boolean,
    val bestMilestones: List<ChronicleMilestoneUiState>,
    val requiredBossTitle: PresentationStringKey? = null,
    val requiredBossDefeated: Boolean = true
)

/** Canonical preview event retained only in presentation memory until confirm/cancel. */
data class ChroniclePreviewUiState(
    val previewToken: Long,
    val chronicleNumber: Long,
    val resetRuleVersion: Int,
    val currentNormalClearsDisplay: String,
    val requiredNormalClearsDisplay: String,
    val echoRewardDisplay: String,
    val resetScopes: List<ChronicleResetScope>,
    val persistScopes: List<ChroniclePersistScope>
)

/** Screen-specific immutable FUI-09 projection. */
data class ProgressUiState(
    val overview: ProgressOverviewUiState,
    val coreGrowthGoldDisplay: String,
    val coreGrowth: List<CoreGrowthTrackUiState>,
    val masteries: List<MasteryProgressUiState>,
    val quests: List<QuestProgressUiState>,
    val achievements: List<AchievementProgressUiState>,
    val discoveries: List<PersistentDiscoveryUiState>,
    val echoShop: EchoShopUiState,
    val chronicle: ChronicleProgressUiState,
    val chroniclePreview: ChroniclePreviewUiState? = null,
    val chroniclePreviewRequestPending: Boolean = false,
    val chronicleCommitPending: Boolean = false,
    val feedback: ProgressFeedbackUiState? = null
) {
    val chroniclePreviewSessionActive: Boolean
        get() = chroniclePreview != null ||
            chroniclePreviewRequestPending ||
            chronicleCommitPending
}
