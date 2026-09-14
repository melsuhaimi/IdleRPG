package com.idlerpg.game.domain.model.chronicle

import com.idlerpg.game.core.id.ContentId
import com.idlerpg.game.core.number.GameNumber

/** Persistent Chronicle-cycle metadata. */
data class ChronicleState(
    val currentChronicleNumber: Long = FIRST_CHRONICLE_NUMBER,
    val completedChronicles: GameNumber = GameNumber.ZERO,
    val bestMilestoneIds: Set<ContentId> = emptySet()
) {
    init {
        require(currentChronicleNumber > 0L) {
            "currentChronicleNumber must be positive: $currentChronicleNumber"
        }
    }

    companion object {
        const val FIRST_CHRONICLE_NUMBER: Long = 1L
    }
}
