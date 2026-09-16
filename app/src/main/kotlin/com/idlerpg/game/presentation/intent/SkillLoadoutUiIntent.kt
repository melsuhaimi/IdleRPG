package com.idlerpg.game.presentation.intent

import com.idlerpg.game.core.id.ContentId
import com.idlerpg.game.domain.command.CommandCorrelationId
import com.idlerpg.game.domain.command.EquipSkill
import com.idlerpg.game.domain.command.GameCommand
import com.idlerpg.game.domain.command.MoveEquippedSkill
import com.idlerpg.game.domain.command.UnequipSkill
import com.idlerpg.game.domain.command.SelectSkillEvolution
import com.idlerpg.game.domain.command.UpgradeSkillRank
import com.idlerpg.game.domain.command.UpgradeSkillMastery
import com.idlerpg.game.domain.command.RefineSkill

/**
 * FUI-04 skill-loadout presentation intents.
 *
 * Every gameplay-changing intent maps one-to-one to the already accepted FBE-01
 * [GameCommand] surface. Presentation never edits PlayerState.equippedSkillIds directly.
 */
sealed interface SkillLoadoutUiIntent {
    data class Equip(
        val skillId: ContentId
    ) : SkillLoadoutUiIntent

    data class Unequip(
        val skillId: ContentId
    ) : SkillLoadoutUiIntent

    data class Move(
        val fromIndex: Int,
        val toIndex: Int
    ) : SkillLoadoutUiIntent

    data class SelectEvolution(
        val skillId: ContentId,
        val evolutionId: ContentId
    ) : SkillLoadoutUiIntent

    data class UpgradeRank(
        val skillId: ContentId
    ) : SkillLoadoutUiIntent

    data class UpgradeMastery(
        val skillId: ContentId
    ) : SkillLoadoutUiIntent

    data class Refine(
        val skillId: ContentId
    ) : SkillLoadoutUiIntent
}

fun SkillLoadoutUiIntent.toGameCommand(
    correlationId: CommandCorrelationId
): GameCommand = when (this) {
    is SkillLoadoutUiIntent.Equip -> EquipSkill(
        skillId = skillId,
        correlationId = correlationId
    )
    is SkillLoadoutUiIntent.Unequip -> UnequipSkill(
        skillId = skillId,
        correlationId = correlationId
    )
    is SkillLoadoutUiIntent.Move -> MoveEquippedSkill(
        fromIndex = fromIndex,
        toIndex = toIndex,
        correlationId = correlationId
    )
    is SkillLoadoutUiIntent.SelectEvolution -> SelectSkillEvolution(
        skillId = skillId,
        evolutionId = evolutionId,
        correlationId = correlationId
    )
    is SkillLoadoutUiIntent.UpgradeRank -> UpgradeSkillRank(
        skillId = skillId,
        correlationId = correlationId
    )
    is SkillLoadoutUiIntent.UpgradeMastery -> UpgradeSkillMastery(
        skillId = skillId,
        correlationId = correlationId
    )
    is SkillLoadoutUiIntent.Refine -> RefineSkill(
        skillId = skillId,
        correlationId = correlationId
    )
}
