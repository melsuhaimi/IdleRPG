package com.idlerpg.game.presentation

import com.idlerpg.game.core.id.InstanceId
import com.idlerpg.game.core.number.Ratio
import com.idlerpg.game.domain.command.AddDoctrineRule
import com.idlerpg.game.domain.command.CommandCorrelationId
import com.idlerpg.game.domain.command.DisableDoctrineRule
import com.idlerpg.game.domain.command.EnableDoctrineRule
import com.idlerpg.game.domain.command.MoveDoctrineRule
import com.idlerpg.game.domain.command.RemoveDoctrineRule
import com.idlerpg.game.domain.command.ReplaceDoctrine
import com.idlerpg.game.domain.command.ReplaceDoctrineRule
import com.idlerpg.game.domain.model.GameState
import com.idlerpg.game.domain.model.doctrine.DoctrineAction
import com.idlerpg.game.domain.model.doctrine.DoctrineComparison
import com.idlerpg.game.domain.model.doctrine.DoctrineCondition
import com.idlerpg.game.domain.model.doctrine.DoctrinePredicate
import com.idlerpg.game.domain.model.doctrine.DoctrineRule
import com.idlerpg.game.presentation.intent.DoctrineUiIntent
import com.idlerpg.game.presentation.intent.toGameCommand

/** Dependency-free one-gesture -> one Doctrine command mapping regression. */
object DoctrineIntentMappingTest {
    @JvmStatic
    fun main(args: Array<String>) {
        val condition = DoctrineCondition.Predicate(
            DoctrinePredicate.EnemyHealthPercent(
                comparison = DoctrineComparison.LESS_THAN_OR_EQUAL,
                threshold = Ratio.HALF
            )
        )
        val rule = DoctrineRule(
            instanceId = InstanceId(41L),
            condition = condition,
            action = DoctrineAction.UseBasicAttack
        )
        val base = GameState.newGame(randomSeed = 806L)
        val state = base.copy(
            run = base.run.copy(
                doctrine = base.run.doctrine.copy(rules = listOf(rule))
            )
        )
        val correlation = CommandCorrelationId(9L)

        val toggle = DoctrineUiIntent.SetDoctrineEnabled(false)
            .toGameCommand(correlation, state) as ReplaceDoctrine
        check(!toggle.enabled)
        check(toggle.rules == listOf(rule))
        check(toggle.correlationId == correlation)

        val add = DoctrineUiIntent.AddRule(
            condition = condition,
            action = DoctrineAction.UseBasicAttack,
            enabled = true
        ).toGameCommand(correlation, state) as AddDoctrineRule
        check(add.condition == condition)
        check(add.action == DoctrineAction.UseBasicAttack)
        check(add.enabled)

        val replace = DoctrineUiIntent.ReplaceRule(
            ruleId = rule.instanceId,
            condition = condition,
            action = DoctrineAction.UseBasicAttack,
            enabled = false
        ).toGameCommand(correlation, state) as ReplaceDoctrineRule
        check(replace.ruleId == rule.instanceId)
        check(!replace.enabled)

        val remove = DoctrineUiIntent.RemoveRule(rule.instanceId)
            .toGameCommand(correlation, state) as RemoveDoctrineRule
        check(remove.ruleId == rule.instanceId)

        val move = DoctrineUiIntent.MoveRule(rule.instanceId, 0)
            .toGameCommand(correlation, state) as MoveDoctrineRule
        check(move.ruleId == rule.instanceId)
        check(move.newIndex == 0)

        val disable = DoctrineUiIntent.SetRuleEnabled(rule.instanceId, false)
            .toGameCommand(correlation, state) as DisableDoctrineRule
        check(disable.ruleId == rule.instanceId)

        val enable = DoctrineUiIntent.SetRuleEnabled(rule.instanceId, true)
            .toGameCommand(correlation, state) as EnableDoctrineRule
        check(enable.ruleId == rule.instanceId)

        println("FUI06_DOCTRINE_INTENT_MAPPING_PASS")
    }
}
