package com.idlerpg.game.domain.definition.combat

import com.idlerpg.game.core.id.ContentId
import com.idlerpg.game.domain.definition.Affinity

/** One mutually exclusive, mastery-gated behavioral branch for an existing skill. */
data class SkillEvolutionDefinition(
    val id: ContentId,
    val baseSkillId: ContentId,
    val requiredAffinity: Affinity,
    val requiredMasteryLevel: Long,
    val replacementEffects: List<EffectSpec>
) {
    init {
        require(requiredMasteryLevel > 0L)
        require(replacementEffects.isNotEmpty())
    }
}
