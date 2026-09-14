package com.idlerpg.game.domain.event

import com.idlerpg.game.core.id.ContentId
import com.idlerpg.game.core.number.GameNumber

/** Completed Chronicle/Echo/meta-progression facts. */
sealed interface ChronicleEvent : GameEvent

/** RunState partitions that a Chronicle collapse reconstructs from fresh defaults. */
enum class ChronicleResetScope {
    PLAYER,
    COMBAT,
    WORLD,
    ECONOMY,
    RESONANCE,
    DOCTRINE,
    ADAPTATION,
    INVENTORY,
    PROGRESSION,
    QUESTS,
    RUN_STATISTICS
}

/** Canonical state partitions intentionally preserved across Chronicle collapse. */
enum class ChroniclePersistScope {
    ENGINE_STATE,
    CHRONICLE,
    ECHOES,
    DISCOVERIES,
    PERSISTENT_FEATURE_UNLOCKS,
    ACHIEVEMENTS,
    LIFETIME_STATISTICS
}

/**
 * Non-destructive deterministic preview produced before a destructive Chronicle commit.
 *
 * [previewToken] is intentionally opaque to presentation code. Commit must echo this
 * exact token so the Chronicle system can reject stale previews atomically.
 */
data class ChroniclePreviewPrepared(
    val previewToken: Long,
    val chronicleNumber: Long,
    val resetRuleVersion: Int,
    val currentTotalNormalClears: GameNumber,
    val requiredTotalNormalClears: GameNumber,
    val echoReward: GameNumber,
    val resetScopes: List<ChronicleResetScope>,
    val persistScopes: List<ChroniclePersistScope>
) : ChronicleEvent {
    init {
        require(previewToken > 0L) { "previewToken must be positive" }
        require(chronicleNumber > 0L) { "chronicleNumber must be positive" }
        require(resetRuleVersion > 0) { "resetRuleVersion must be positive" }
        require(requiredTotalNormalClears > GameNumber.ZERO) {
            "requiredTotalNormalClears must be > 0"
        }
        require(currentTotalNormalClears >= requiredTotalNormalClears) {
            "Chronicle preview can only be prepared for an eligible run"
        }
        require(echoReward > GameNumber.ZERO) { "echoReward must be > 0" }
        require(resetScopes.isNotEmpty()) { "resetScopes cannot be empty" }
        require(resetScopes.size == resetScopes.toSet().size) {
            "resetScopes cannot contain duplicates"
        }
        require(persistScopes.isNotEmpty()) { "persistScopes cannot be empty" }
        require(persistScopes.size == persistScopes.toSet().size) {
            "persistScopes cannot contain duplicates"
        }
    }
}

data class EchoGranted(
    val amount: GameNumber,
    val sourceId: ContentId? = null
) : ChronicleEvent

data class EchoSpent(
    val amount: GameNumber,
    val offerId: ContentId
) : ChronicleEvent {
    init {
        require(amount > GameNumber.ZERO) { "Echo spent amount must be > 0" }
    }
}

data class EchoOfferPurchased(
    val offerId: ContentId,
    val cost: GameNumber
) : ChronicleEvent {
    init {
        require(cost > GameNumber.ZERO) { "Echo offer cost must be > 0" }
    }
}

data class DiscoveryUnlocked(
    val discoveryId: ContentId
) : ChronicleEvent

data class ChronicleCollapseStarted(
    val chronicleNumber: Long
) : ChronicleEvent {
    init {
        require(chronicleNumber > 0L) { "chronicleNumber must be positive: $chronicleNumber" }
    }
}

data class ChronicleCollapsed(
    val completedChronicleNumber: Long,
    val nextChronicleNumber: Long
) : ChronicleEvent {
    init {
        require(completedChronicleNumber > 0L) {
            "completedChronicleNumber must be positive: $completedChronicleNumber"
        }
        require(nextChronicleNumber == completedChronicleNumber + 1L) {
            "nextChronicleNumber must immediately follow completedChronicleNumber"
        }
    }
}

data class NewChronicleStarted(
    val chronicleNumber: Long
) : ChronicleEvent {
    init {
        require(chronicleNumber > 0L) { "chronicleNumber must be positive: $chronicleNumber" }
    }
}
