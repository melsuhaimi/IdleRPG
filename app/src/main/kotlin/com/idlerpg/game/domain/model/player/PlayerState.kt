package com.idlerpg.game.domain.model.player

import com.idlerpg.game.core.id.ContentId
import com.idlerpg.game.core.number.GameNumber

/** Run-level mutable player facts. Derived equipment/status-adjusted stats do not live here. */
data class PlayerState(
    val baseStats: BaseStats = BaseStats(),
    val currentHealth: GameNumber = GameNumber.of(100L),
    val resources: ResourceState = ResourceState(),
    val equippedSkillIds: List<ContentId> = emptyList(),
    val selectedSkillEvolutionBySkillId: Map<ContentId, ContentId> = emptyMap()
) {
    init {
        require(equippedSkillIds.size == equippedSkillIds.toSet().size) {
            "equippedSkillIds cannot contain duplicates"
        }
        require(selectedSkillEvolutionBySkillId.all { (skillId, evolutionId) -> skillId != evolutionId }) {
            "A skill cannot select itself as an evolution"
        }
    }
}
