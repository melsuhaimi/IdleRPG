package com.idlerpg.game.domain.engine

import com.idlerpg.game.domain.command.CommandRejectionReason

/** Explicit outcome of attempting one GameCommand. */
sealed interface CommandResult {

    /** The command was accepted and its complete transition was committed atomically. */
    object Accepted : CommandResult

    /** The command was rejected; canonical gameplay state remains unchanged. */
    data class Rejected(
        val reason: CommandRejectionReason
    ) : CommandResult
}
