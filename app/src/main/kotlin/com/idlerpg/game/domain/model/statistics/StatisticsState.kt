package com.idlerpg.game.domain.model.statistics

import com.idlerpg.game.core.id.ContentId
import com.idlerpg.game.core.number.GameNumber

/**
 * Bounded aggregate statistic counters keyed by stable statistic ID.
 *
 * The same structure can be owned independently by RunState and MetaState, making reset
 * scope explicit without storing an unbounded event history.
 */
data class StatisticsState(
    val countersByStatisticId: Map<ContentId, GameNumber> = emptyMap()
)
