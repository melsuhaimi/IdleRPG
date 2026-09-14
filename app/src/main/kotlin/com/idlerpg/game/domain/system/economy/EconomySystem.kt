package com.idlerpg.game.domain.system.economy

import com.idlerpg.game.domain.command.CommandRejectionCode
import com.idlerpg.game.domain.command.CommandRejectionReason
import com.idlerpg.game.domain.command.GameCommand
import com.idlerpg.game.domain.command.PurchaseUpgrade
import com.idlerpg.game.domain.engine.CommandHandlingResult
import com.idlerpg.game.domain.engine.EngineContext
import com.idlerpg.game.domain.engine.GameCommandHandler
import com.idlerpg.game.domain.model.GameState

/**
 * Foundation 6 command handler for run-economy intent.
 *
 * Later foundations may compose this handler with world/inventory/Chronicle handlers.
 */
object EconomySystem : GameCommandHandler {

    override fun handle(
        state: GameState,
        command: GameCommand,
        context: EngineContext
    ): CommandHandlingResult =
        when (command) {
            is PurchaseUpgrade -> {
                when (
                    val purchase = UpgradeSystem.purchase(
                        state = state,
                        upgradeId = command.upgradeId,
                        quantity = command.quantity,
                        context = context
                    )
                ) {
                    is UpgradePurchaseResult.Accepted ->
                        CommandHandlingResult.Accepted(
                            state = purchase.state,
                            events = purchase.events
                        )

                    is UpgradePurchaseResult.Rejected ->
                        CommandHandlingResult.Rejected(
                            reason = purchase.reason
                        )
                }
            }

            else -> CommandHandlingResult.Rejected(
                CommandRejectionReason(
                    code = CommandRejectionCode.UNSUPPORTED
                )
            )
        }
}
