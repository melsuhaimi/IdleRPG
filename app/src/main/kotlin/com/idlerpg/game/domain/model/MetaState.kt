package com.idlerpg.game.domain.model

import com.idlerpg.game.domain.model.chronicle.ChronicleState
import com.idlerpg.game.domain.model.chronicle.DiscoveryState
import com.idlerpg.game.domain.model.achievement.AchievementState
import com.idlerpg.game.domain.model.chronicle.EchoState
import com.idlerpg.game.domain.model.progression.FeatureUnlockState
import com.idlerpg.game.domain.model.statistics.StatisticsState

/**
 * Chronicle-persistent knowledge and lifetime state.
 *
 * Foundation 14 adds persistent achievement progress here. Run-only challenges must not
 * be stored in this lifetime/meta aggregate.
 */
data class MetaState(
    val chronicle: ChronicleState = ChronicleState(),
    val echoes: EchoState = EchoState(),
    val discoveries: DiscoveryState = DiscoveryState(),
    val persistentFeatureUnlocks: FeatureUnlockState = FeatureUnlockState(),
    val achievements: AchievementState = AchievementState(),
    val lifetimeStatistics: StatisticsState = StatisticsState(),
    /** Player-facing identity that survives Chronicle/Prestige resets. */
    val heroName: String? = null
)
