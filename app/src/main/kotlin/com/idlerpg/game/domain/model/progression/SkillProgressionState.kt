package com.idlerpg.game.domain.model.progression

import com.idlerpg.game.core.id.ContentId

/**
 * Run-scoped progression for each authored skill.
 *
 * Player level remains a broad baseline, while these maps hold deliberate skill investment.
 * Rebirth creates a fresh ProgressionState, so rank, mastery, and refinement are reset with
 * the other run-level skill choices.
 */
data class SkillProgressionState(
    val rankBySkillId: Map<ContentId, Long> = emptyMap(),
    val masteryBySkillId: Map<ContentId, Long> = emptyMap(),
    val refinementBySkillId: Map<ContentId, Long> = emptyMap()
) {
    init {
        require(rankBySkillId.values.all { it >= 1L }) {
            "Skill ranks must be positive"
        }
        require(masteryBySkillId.values.all { it >= 0L }) {
            "Skill mastery cannot be negative"
        }
        require(refinementBySkillId.values.all { it >= 0L }) {
            "Skill refinement cannot be negative"
        }
    }
}
