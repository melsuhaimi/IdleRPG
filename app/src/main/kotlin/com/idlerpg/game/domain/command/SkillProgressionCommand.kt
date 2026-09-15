package com.idlerpg.game.domain.command

import com.idlerpg.game.core.id.ContentId

/** Intent for the deliberately slow, run-scoped skill investment tracks. */
sealed interface SkillProgressionCommand : SkillCommand

data class UpgradeSkillRank(
    val skillId: ContentId,
    override val correlationId: CommandCorrelationId? = null
) : SkillProgressionCommand

data class UpgradeSkillMastery(
    val skillId: ContentId,
    override val correlationId: CommandCorrelationId? = null
) : SkillProgressionCommand

data class RefineSkill(
    val skillId: ContentId,
    override val correlationId: CommandCorrelationId? = null
) : SkillProgressionCommand
