package com.idlerpg.game.presentation.model

import com.idlerpg.game.core.id.ContentId
import com.idlerpg.game.core.id.InstanceId
import com.idlerpg.game.domain.command.CommandRejectionCode
import com.idlerpg.game.domain.definition.enemy.EnemyRole
import com.idlerpg.game.presentation.content.PresentationAssetKey
import com.idlerpg.game.presentation.content.PresentationStringKey

/** Presentation-only lifecycle label for the canonical CombatStatus. */
enum class BattleCombatStatusUi {
    IDLE,
    ACTIVE,
    VICTORY,
    DEFEAT
}

enum class BattleSkillReadinessUi {
    READY,
    COOLDOWN_WAIT,
    RESOURCE_WAIT,
    UNAVAILABLE
}

enum class BattleFeedbackKind {
    COMMAND_REJECTED,
    SKILL_QUEUED,
    SKILL_QUEUE_REPLACED,
    SKILL_QUEUE_DEFERRED,
    SKILL_QUEUE_CONSUMED,
    SKILL_QUEUE_CLEARED,
    SKILL_USED,
    DAMAGE,
    STATUS_APPLIED,
    HEALING,
    ENEMY_DEFEATED,
    CONVERGENCE,
    LOOT,
    LEVEL_UP,
    UPGRADE_PURCHASED,
    COMBAT_ENDED,
    PLAYER_DEFEATED,
    BOSS_PHASE
}

data class BattleImpactUiState(
    val sequenceNumber: Long,
    val targetInstanceId: InstanceId,
    val amountDisplay: String,
    val critical: Boolean
)

/** Reward facts emitted by the canonical encounter transition. */
data class BattleRewardItemUiState(
    val itemInstanceId: InstanceId,
    val itemDefinitionId: ContentId,
    val titleStringKey: PresentationStringKey?,
    val rarityTitleStringKey: PresentationStringKey?,
    val sentToOverflow: Boolean,
    val autoSalvaged: Boolean
)

data class BattleRewardUiState(
    val goldDisplay: String? = null,
    val experienceDisplay: String? = null,
    val items: List<BattleRewardItemUiState> = emptyList()
) {
    val isEmpty: Boolean
        get() = goldDisplay == null && experienceDisplay == null && items.isEmpty()
}

/**
 * Presentation-only link between real EnemyKilled events and the reward facts emitted by the
 * same runtime transition. Reward events currently carry no enemy instance id, so a transition
 * with multiple kills is represented as one formation-side reveal instead of falsely assigning
 * an aggregate reward to one monster.
 */
data class BattleKillRewardUiState(
    val defeatedEnemyInstanceIds: List<InstanceId>,
    val reward: BattleRewardUiState?
) {
    init {
        require(defeatedEnemyInstanceIds.isNotEmpty())
        require(defeatedEnemyInstanceIds.size == defeatedEnemyInstanceIds.distinct().size)
    }
}

/**
 * Presentation-only reward feed item. It is intentionally outside GameState and SaveData:
 * canonical rewards are already committed before this item is rendered, and the token only
 * gives Compose a stable animation/consumption identity.
 */
data class BattlePresentationRewardUiState(
    val token: Long,
    val sequenceNumber: Long?,
    val defeatedEnemyInstanceIds: List<InstanceId>,
    val reward: BattleRewardUiState?
) {
    init {
        require(token > 0L)
        require(defeatedEnemyInstanceIds.isNotEmpty())
        require(defeatedEnemyInstanceIds.size == defeatedEnemyInstanceIds.distinct().size)
    }
}

data class BattleFeedbackUiState(
    val sequenceNumber: Long?,
    val kind: BattleFeedbackKind,
    val contentId: ContentId? = null,
    val amountDisplay: String? = null,
    val rejectionCode: CommandRejectionCode? = null,
    val bossPhase: Int? = null,
    val bossTotalPhases: Int? = null,
    val impacts: List<BattleImpactUiState> = emptyList(),
    val reward: BattleRewardUiState? = null,
    val upgradePreviousLevel: Long? = null,
    val upgradeNewLevel: Long? = null,
    val upgradePreviousAttackDisplay: String? = null,
    val upgradeNewAttackDisplay: String? = null,
    val upgradeCostDisplay: String? = null,
    val killReward: BattleKillRewardUiState? = null
) {
    init {
        require((bossPhase == null) == (bossTotalPhases == null)) {
            "Boss phase and total phase must be supplied together"
        }
        if (bossPhase != null && bossTotalPhases != null) {
            require(bossPhase in 1..bossTotalPhases) { "Boss phase must be inside the authored range" }
        }
        require(kind == BattleFeedbackKind.BOSS_PHASE || bossPhase == null) {
            "Only BOSS_PHASE feedback may carry phase data"
        }
        require(kind != BattleFeedbackKind.BOSS_PHASE || bossPhase != null) {
            "BOSS_PHASE feedback requires phase data"
        }
    }
}

enum class BattleBossPhaseUi {
    BASTION,
    REFLECTION,
    FRACTURE
}

data class BattleStatusEffectUiState(
    val instanceId: InstanceId,
    val definitionId: ContentId,
    val titleStringKey: PresentationStringKey,
    val assetKey: PresentationAssetKey,
    val stackCount: Int,
    val remainingMillis: Long,
    val durationMillis: Long,
    val progressUnits: Int,
    val polarity: BattleStatusPolarityUi = BattleStatusPolarityUi.NEUTRAL,
    val detailStringKey: PresentationStringKey = PresentationStringKey.DESC_STATUS,
    val potencyDisplay: String? = null
) {
    init {
        require(stackCount > 0)
        require(remainingMillis >= 0L)
        require(durationMillis > 0L)
        require(progressUnits in 0..10_000)
    }
}

enum class BattleStatusPolarityUi {
    BENEFICIAL,
    HARMFUL,
    NEUTRAL
}

data class BattleMutationUiState(
    val mutationId: ContentId,
    val titleStringKey: PresentationStringKey,
    val assetKey: PresentationAssetKey
)

data class BattleEnemyUiState(
    val instanceId: InstanceId,
    val definitionId: ContentId,
    val titleStringKey: PresentationStringKey,
    val assetKey: PresentationAssetKey,
    val illustrationAssetKey: PresentationAssetKey?,
    val currentHealthDisplay: String,
    val maximumHealthDisplay: String,
    val healthProgressUnits: Int,
    val alive: Boolean,
    val scalingTier: Long,
    val attackTitle: String?,
    val attackAffinityId: ContentId?,
    val attackDamageDisplay: String?,
    val armorPenetrationDisplay: String?,
    val attackIntervalMillis: Long?,
    val nextAttackRemainingMillis: Long?,
    val attackFxAssetKey: PresentationAssetKey?,
    val encounterFrameAssetKey: PresentationAssetKey?,
    val statuses: List<BattleStatusEffectUiState>,
    val mutations: List<BattleMutationUiState>,
    val armorDisplay: String? = null,
    val role: EnemyRole? = null
) {
    init {
        require(healthProgressUnits in 0..10_000)
        require(scalingTier >= 0L)
        require(attackIntervalMillis == null || attackIntervalMillis > 0L)
        require(nextAttackRemainingMillis == null || nextAttackRemainingMillis >= 0L)
    }
}

data class BattleResourceUiState(
    val resourceId: ContentId,
    val titleStringKey: PresentationStringKey?,
    val amountDisplay: String
)

data class BattleResourceCostUiState(
    val resourceId: ContentId,
    val titleStringKey: PresentationStringKey?,
    val requiredDisplay: String,
    val availableDisplay: String,
    val sufficient: Boolean
)

data class BattlePlayerUiState(
    val currentHealthDisplay: String,
    val maximumHealthDisplay: String,
    val healthProgressUnits: Int,
    val attackDisplay: String,
    val armorDisplay: String,
    val basicAttackDpsDisplay: String,
    val basicAttackIntervalMillis: Long,
    val nextDecisionRemainingMillis: Long?,
    val resources: List<BattleResourceUiState>,
    val statuses: List<BattleStatusEffectUiState>,
    val level: Long = 1L,
    val criticalChanceDisplay: String = "0%",
    val criticalMultiplierDisplay: String = "100%",
    val damageReductionDisplay: String = "0%",
    val effectPowerDisplay: String = "100%",
    val healingPowerDisplay: String = "100%",
    val heroName: String? = null
) {
    init {
        require(healthProgressUnits in 0..10_000)
        require(basicAttackIntervalMillis > 0L)
        require(nextDecisionRemainingMillis == null || nextDecisionRemainingMillis >= 0L)
    }
}

data class BattleSkillUiState(
    val skillId: ContentId,
    val titleStringKey: PresentationStringKey,
    val assetKey: PresentationAssetKey,
    val affinityIds: List<ContentId>,
    val cooldownRemainingMillis: Long,
    val cooldownDurationMillis: Long,
    val cooldownProgressUnits: Int,
    val recoveryMillis: Long,
    val resourceCosts: List<BattleResourceCostUiState>,
    val queued: Boolean,
    val queueAllowed: Boolean,
    val readiness: BattleSkillReadinessUi,
    val rank: Long = 1L,
    val scalingDisplay: String? = null
) {
    init {
        require(cooldownRemainingMillis >= 0L)
        require(cooldownDurationMillis >= 0L)
        require(cooldownProgressUnits in 0..10_000)
        require(recoveryMillis > 0L)
    }
}

data class ResonanceAffinityUiState(
    val affinityId: ContentId,
    val titleStringKey: PresentationStringKey,
    val chargeDisplay: String,
    val capDisplay: String,
    val chargeProgressUnits: Int
) {
    init {
        require(chargeProgressUnits in 0..10_000)
    }
}

data class BattleDoctrineUiState(
    val enabled: Boolean,
    val ruleCount: Int,
    val ruleCapacity: Int,
    val queuedSkillTitleStringKey: PresentationStringKey?,
    val decisionRuleNumber: Int? = null,
    val decisionExplanation: String? = null
) {
    init {
        require(ruleCount >= 0)
        require(ruleCapacity > 0)
        require(decisionRuleNumber == null || decisionRuleNumber > 0)
    }
}

data class BattleUpgradeUiState(
    val upgradeId: ContentId,
    val titleStringKey: PresentationStringKey,
    val assetKey: PresentationAssetKey,
    val level: Long,
    val nextCostDisplay: String,
    val affordable: Boolean
) {
    init {
        require(level >= 0L)
    }
}

/** Canonical Battle projection consumed by FUI-03 Compose only. */
data class BattleUiState(
    val combatStatus: BattleCombatStatusUi,
    val regionTitleStringKey: PresentationStringKey?,
    val encounterTitleStringKey: PresentationStringKey?,
    val combatSequenceId: Long,
    val enemy: BattleEnemyUiState?,
    val player: BattlePlayerUiState,
    val equippedSkills: List<BattleSkillUiState>,
    val queuedSkillId: ContentId?,
    val resonance: List<ResonanceAffinityUiState>,
    val resonanceSequence: List<ContentId>,
    val doctrine: BattleDoctrineUiState,
    val basicAttackUpgrade: BattleUpgradeUiState,
    val canRetreat: Boolean,
    val canRetry: Boolean,
    val retryEncounterId: ContentId?,
    val feedback: BattleFeedbackUiState?,
    val presentationRewards: List<BattlePresentationRewardUiState> = emptyList(),
    val enemies: List<BattleEnemyUiState> = listOfNotNull(enemy),
    val currentWave: Int = 1,
    val totalWaves: Int = 1,
    val stageNumber: Int? = null,
    val bossPhase: BattleBossPhaseUi? = null,
    val backgroundAssetKey: PresentationAssetKey =
        PresentationAssetKey.TRAINING_HOLLOW_ILLUSTRATION,
    val automationMode: com.idlerpg.game.domain.model.world.WorldAutomationMode =
        com.idlerpg.game.domain.model.world.WorldAutomationMode.PUSH,
    val stageLabel: String? = null
) {
    init {
        require(combatSequenceId >= 0L)
        require(currentWave > 0)
        require(totalWaves > 0)
    }
}
