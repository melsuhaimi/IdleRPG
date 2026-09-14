package com.idlerpg.game.presentation

import com.idlerpg.game.data.content.DefaultGameContent
import com.idlerpg.game.domain.command.ClearQueuedSkillCast
import com.idlerpg.game.domain.command.CommandCorrelationId
import com.idlerpg.game.domain.command.PurchaseUpgrade
import com.idlerpg.game.domain.command.QueueSkillCast
import com.idlerpg.game.domain.command.RetreatEncounter
import com.idlerpg.game.presentation.intent.BattleUiIntent
import com.idlerpg.game.presentation.intent.toGameCommand

/** Dependency-free FUI-03 proof that every Battle mutation maps to exactly one command. */
object BattleIntentMappingTest {
    @JvmStatic
    fun main(args: Array<String>) {
        val correlation = CommandCorrelationId(41L)

        val queue = BattleUiIntent.QueueSkill(DefaultGameContent.HEAVY_STRIKE_ID)
            .toGameCommand(correlation) as QueueSkillCast
        check(queue.skillId == DefaultGameContent.HEAVY_STRIKE_ID)
        check(queue.correlationId == correlation)

        val clear = BattleUiIntent.ClearQueuedSkill.toGameCommand(correlation)
            as ClearQueuedSkillCast
        check(clear.correlationId == correlation)

        val purchase = BattleUiIntent.PurchaseUpgradeLevel(
            DefaultGameContent.BASIC_ATTACK_POWER_UPGRADE_ID
        ).toGameCommand(correlation) as PurchaseUpgrade
        check(purchase.upgradeId == DefaultGameContent.BASIC_ATTACK_POWER_UPGRADE_ID)
        check(purchase.quantity == 1L)
        check(purchase.correlationId == correlation)

        val retreat = BattleUiIntent.Retreat.toGameCommand(correlation) as RetreatEncounter
        check(retreat.correlationId == correlation)

        println("FUI03_BATTLE_INTENT_MAPPING_PASS")
    }
}
