package com.idlerpg.game.domain.system.player

import com.idlerpg.game.domain.command.CommandRejectionCode
import com.idlerpg.game.domain.command.CommandRejectionReason
import com.idlerpg.game.domain.command.GameCommand
import com.idlerpg.game.domain.command.PlayerCommand
import com.idlerpg.game.domain.command.SetHeroName
import com.idlerpg.game.domain.engine.CommandHandlingResult
import com.idlerpg.game.domain.engine.EngineContext
import com.idlerpg.game.domain.engine.GameCommandHandler
import com.idlerpg.game.domain.event.HeroNamed
import com.idlerpg.game.domain.model.GameState

/** Owns player-facing identity mutation without exposing mutable state to Compose. */
object PlayerSystem : GameCommandHandler {
    const val MAX_HERO_NAME_LENGTH: Int = 20

    override fun handle(
        state: GameState,
        command: GameCommand,
        context: EngineContext
    ): CommandHandlingResult = when (command) {
        is SetHeroName -> {
            val normalized = command.name.trim()
            if (normalized.isEmpty() || normalized.length > MAX_HERO_NAME_LENGTH) {
                CommandHandlingResult.Rejected(
                    CommandRejectionReason(CommandRejectionCode.INVALID_ARGUMENT)
                )
            } else {
                CommandHandlingResult.Accepted(
                    state = state.copy(meta = state.meta.copy(heroName = normalized)),
                    events = listOf(HeroNamed(normalized))
                )
            }
        }
        is PlayerCommand -> CommandHandlingResult.Rejected(
            CommandRejectionReason(CommandRejectionCode.UNSUPPORTED)
        )
        else -> CommandHandlingResult.Rejected(
            CommandRejectionReason(CommandRejectionCode.UNSUPPORTED)
        )
    }
}
