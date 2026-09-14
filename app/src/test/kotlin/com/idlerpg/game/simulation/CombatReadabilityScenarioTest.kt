package com.idlerpg.game.simulation

import com.idlerpg.game.core.id.InstanceId
import com.idlerpg.game.core.number.GameNumber
import com.idlerpg.game.core.time.GameDuration
import com.idlerpg.game.core.time.GameTime
import com.idlerpg.game.data.content.DefaultGameContent
import com.idlerpg.game.domain.event.DamageDealt
import com.idlerpg.game.domain.event.GameEventEnvelope
import com.idlerpg.game.domain.model.GameState
import com.idlerpg.game.presentation.event.GameEventPresenter
import com.idlerpg.game.presentation.runtime.RuntimeTransition

/** Presentation must retain target/critical identity without changing the gameplay snapshot. */
object CombatReadabilityScenarioTest {
    fun run() {
        val content = DefaultGameContent.create()
        check(content.skills.all { it.cooldown > GameDuration.ZERO })
        val state = GameState.newGame(96L)
        val target = InstanceId(99L)
        val events = (1L..20L).map { sequence -> GameEventEnvelope(sequence, GameTime.ZERO,
            DamageDealt(targetInstanceId = target, amount = GameNumber.of(sequence), critical = sequence == 20L)) }
        val transition = RuntimeTransition(state, events, null)
        val result = requireNotNull(GameEventPresenter().presentBattle(transition))
        check(result.impacts.size == 12)
        check(result.impacts.first().sequenceNumber == 9L)
        check(result.impacts.last().sequenceNumber == 20L)
        check(result.impacts.all { it.targetInstanceId == target })
        check(result.impacts.last().amountDisplay == "20" && result.impacts.last().critical)
        check(transition.state == state)
        check(GameEventPresenter().presentBattle(RuntimeTransition(state, emptyList(), null)) == null)
    }
}
