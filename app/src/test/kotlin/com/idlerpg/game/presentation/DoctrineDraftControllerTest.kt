package com.idlerpg.game.presentation

import com.idlerpg.game.core.id.InstanceId
import com.idlerpg.game.data.content.DefaultGameContent
import com.idlerpg.game.domain.definition.Affinity
import com.idlerpg.game.domain.model.doctrine.DoctrineAction
import com.idlerpg.game.domain.model.doctrine.DoctrineCondition
import com.idlerpg.game.domain.model.doctrine.DoctrinePredicate
import com.idlerpg.game.domain.model.doctrine.DoctrineRule
import com.idlerpg.game.presentation.doctrine.DoctrineDraftAction
import com.idlerpg.game.presentation.doctrine.DoctrineDraftController
import com.idlerpg.game.presentation.doctrine.DoctrineDraftConversionResult
import com.idlerpg.game.presentation.doctrine.DoctrineDraftPath
import com.idlerpg.game.presentation.model.DoctrineActionKindUi
import com.idlerpg.game.presentation.model.DoctrineConditionKindUi

/** FUI-06 presentation-draft reducer and domain conversion regression. */
object DoctrineDraftControllerTest {
    @JvmStatic
    fun main(args: Array<String>) {
        var draft = DoctrineDraftController.newDraft()
        draft = DoctrineDraftController.reduce(
            draft,
            DoctrineDraftAction.SetConditionKind(
                DoctrineDraftPath(),
                DoctrineConditionKindUi.ALL
            ),
            maximumDepth = 4,
            sequenceCapacity = 8
        )
        draft = DoctrineDraftController.reduce(
            draft,
            DoctrineDraftAction.AddChild(DoctrineDraftPath()),
            maximumDepth = 4,
            sequenceCapacity = 8
        )
        check(draft.condition.children.size == 2)

        val second = DoctrineDraftPath(listOf(1))
        draft = DoctrineDraftController.reduce(
            draft,
            DoctrineDraftAction.SetConditionKind(
                second,
                DoctrineConditionKindUi.SEQUENCE_SUFFIX
            ),
            maximumDepth = 4,
            sequenceCapacity = 8
        )
        for (affinity in listOf(Affinity.MIGHT, Affinity.MIGHT, Affinity.EMBER)) {
            draft = DoctrineDraftController.reduce(
                draft,
                DoctrineDraftAction.AppendSequenceAffinity(second, affinity.id),
                maximumDepth = 4,
                sequenceCapacity = 8
            )
        }
        draft = DoctrineDraftController.reduce(
            draft,
            DoctrineDraftAction.SetActionKind(DoctrineActionKindUi.SKILL),
            maximumDepth = 4,
            sequenceCapacity = 8
        )
        draft = DoctrineDraftController.reduce(
            draft,
            DoctrineDraftAction.SetActionSkill(DefaultGameContent.HEAVY_STRIKE_ID),
            maximumDepth = 4,
            sequenceCapacity = 8
        )

        val converted = DoctrineDraftController.toDomain(
            draft,
            maximumDepth = 4,
            sequenceCapacity = 8
        ) as DoctrineDraftConversionResult.Ready
        val all = converted.value.condition as DoctrineCondition.All
        check(all.conditions.size == 2)
        val suffix = (all.conditions[1] as DoctrineCondition.Predicate).predicate as
            DoctrinePredicate.SequenceSuffix
        check(suffix.affinities == listOf(Affinity.MIGHT, Affinity.MIGHT, Affinity.EMBER))
        val action = converted.value.action as DoctrineAction.UseSkill
        check(action.skillId == DefaultGameContent.HEAVY_STRIKE_ID)
        check(converted.value.ruleId == null)

        val depthTwo = DoctrineDraftController.reduce(
            DoctrineDraftController.newDraft(),
            DoctrineDraftAction.SetConditionKind(
                DoctrineDraftPath(),
                DoctrineConditionKindUi.NOT
            ),
            maximumDepth = 2,
            sequenceCapacity = 8
        )
        val blocked = DoctrineDraftController.reduce(
            depthTwo,
            DoctrineDraftAction.SetConditionKind(
                DoctrineDraftPath(listOf(0)),
                DoctrineConditionKindUi.ALL
            ),
            maximumDepth = 2,
            sequenceCapacity = 8
        )
        check(blocked.condition.children.single().kind ==
            DoctrineConditionKindUi.ENEMY_HEALTH_PERCENT)

        val existing = DoctrineRule(
            instanceId = InstanceId(77L),
            enabled = false,
            condition = all.conditions.first(),
            action = DoctrineAction.UseBasicAttack
        )
        val edit = DoctrineDraftController.fromRule(existing)
        check(edit.ruleId == InstanceId(77L))
        check(!edit.enabled)

        println("FUI06_DOCTRINE_DRAFT_CONTROLLER_PASS")
    }
}
