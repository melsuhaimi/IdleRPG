package com.idlerpg.game.domain.system.skill

import com.idlerpg.game.data.content.ContentRegistry
import com.idlerpg.game.domain.command.CommandRejectionCode
import com.idlerpg.game.domain.command.CommandRejectionReason
import com.idlerpg.game.domain.command.SelectSkillEvolution
import com.idlerpg.game.domain.definition.combat.SkillDefinition
import com.idlerpg.game.domain.engine.CommandHandlingResult
import com.idlerpg.game.domain.engine.EngineContext
import com.idlerpg.game.domain.event.SkillEvolutionSelected
import com.idlerpg.game.domain.model.GameState
import com.idlerpg.game.domain.system.progression.AffinityMasterySystem

/** Owns mutually exclusive run-level skill-evolution selection and resolution. */
object SkillEvolutionSystem {

    fun handle(
        state: GameState,
        command: SelectSkillEvolution,
        context: EngineContext
    ): CommandHandlingResult {
        val baseSkill = context.contentRegistry.skillOrNull(command.skillId)
            ?: return rejected(CommandRejectionCode.UNKNOWN_CONTENT, command.skillId)
        val evolution = context.contentRegistry.skillEvolutionOrNull(command.evolutionId)
            ?: return rejected(CommandRejectionCode.UNKNOWN_CONTENT, command.evolutionId)
        if (evolution.baseSkillId != baseSkill.id) {
            return rejected(CommandRejectionCode.INVALID_ARGUMENT, command.evolutionId)
        }
        val unlockRejection = SkillValidationSystem.unlockRejectionReason(state, baseSkill)
        if (unlockRejection != null) return CommandHandlingResult.Rejected(unlockRejection)
        val masteryLevel = AffinityMasterySystem.levelFor(
            state,
            evolution.requiredAffinity.id,
            context.contentRegistry
        )
        if (masteryLevel < evolution.requiredMasteryLevel) {
            return rejected(CommandRejectionCode.LOCKED, evolution.id)
        }

        val selections = state.run.player.selectedSkillEvolutionBySkillId
        val previous = selections[baseSkill.id]
        if (previous == evolution.id) {
            return rejected(CommandRejectionCode.ALREADY_OWNED, evolution.id)
        }
        return CommandHandlingResult.Accepted(
            state.copy(
                run = state.run.copy(
                    player = state.run.player.copy(
                        selectedSkillEvolutionBySkillId = selections + (baseSkill.id to evolution.id)
                    )
                )
            ),
            listOf(SkillEvolutionSelected(baseSkill.id, evolution.id, previous))
        )
    }

    fun effectiveDefinition(
        state: GameState,
        base: SkillDefinition,
        contentRegistry: ContentRegistry
    ): SkillDefinition {
        val evolutionId = state.run.player.selectedSkillEvolutionBySkillId[base.id] ?: return base
        val evolution = contentRegistry.skillEvolutionOrNull(evolutionId)
            ?: return base
        if (evolution.baseSkillId != base.id) return base
        return base.copy(effects = evolution.replacementEffects)
    }

    private fun rejected(
        code: CommandRejectionCode,
        subject: com.idlerpg.game.core.id.ContentId
    ) = CommandHandlingResult.Rejected(CommandRejectionReason(code, subjectContentId = subject))
}
