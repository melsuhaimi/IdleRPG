package com.idlerpg.game.presentation

import com.idlerpg.game.data.content.DefaultGameContent
import com.idlerpg.game.domain.command.CommandCorrelationId
import com.idlerpg.game.domain.command.EquipSkill
import com.idlerpg.game.domain.command.SelectSkillEvolution
import com.idlerpg.game.domain.command.MoveEquippedSkill
import com.idlerpg.game.domain.command.UnequipSkill
import com.idlerpg.game.presentation.intent.SkillLoadoutUiIntent
import com.idlerpg.game.presentation.intent.toGameCommand

/** Dependency-free FUI-04 proof that every loadout mutation maps to exactly one command. */
object SkillLoadoutIntentMappingTest {
    @JvmStatic
    fun main(args: Array<String>) {
        val correlation = CommandCorrelationId(404L)

        val equip = SkillLoadoutUiIntent.Equip(DefaultGameContent.HEAVY_STRIKE_ID)
            .toGameCommand(correlation) as EquipSkill
        check(equip.skillId == DefaultGameContent.HEAVY_STRIKE_ID)

        val evolution = SkillLoadoutUiIntent.SelectEvolution(
            DefaultGameContent.HEAVY_STRIKE_ID,
            DefaultGameContent.EARTHBREAKER_EVOLUTION_ID
        ).toGameCommand(correlation) as SelectSkillEvolution
        check(evolution.skillId == DefaultGameContent.HEAVY_STRIKE_ID)
        check(evolution.evolutionId == DefaultGameContent.EARTHBREAKER_EVOLUTION_ID)
        check(equip.correlationId == correlation)

        val unequip = SkillLoadoutUiIntent.Unequip(DefaultGameContent.HEAVY_STRIKE_ID)
            .toGameCommand(correlation) as UnequipSkill
        check(unequip.skillId == DefaultGameContent.HEAVY_STRIKE_ID)
        check(unequip.correlationId == correlation)

        val move = SkillLoadoutUiIntent.Move(fromIndex = 1, toIndex = 0)
            .toGameCommand(correlation) as MoveEquippedSkill
        check(move.fromIndex == 1)
        check(move.toIndex == 0)
        check(move.correlationId == correlation)

        println("FUI04_SKILL_LOADOUT_INTENT_MAPPING_PASS")
    }
}
