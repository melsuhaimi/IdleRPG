package com.idlerpg.game.domain.system.chronicle

import com.idlerpg.game.domain.command.CommandRejectionCode
import com.idlerpg.game.domain.command.CommandRejectionReason
import com.idlerpg.game.domain.command.PurchaseEchoOffer
import com.idlerpg.game.domain.definition.chronicle.EchoOfferDefinition
import com.idlerpg.game.domain.engine.CommandHandlingResult
import com.idlerpg.game.domain.engine.EngineContext
import com.idlerpg.game.domain.event.EchoOfferPurchased
import com.idlerpg.game.domain.event.EchoSpent
import com.idlerpg.game.domain.model.GameState
import com.idlerpg.game.domain.model.MetaState

/** Atomic owner of nonrepeatable persistent Echo-shop purchases. */
object EchoOfferSystem {

    fun isPurchased(
        meta: MetaState,
        definition: EchoOfferDefinition
    ): Boolean = definition.id in meta.echoes.purchasedOfferIds

    fun prerequisitesSatisfied(
        meta: MetaState,
        definition: EchoOfferDefinition
    ): Boolean = definition.requiredOfferIds.all { requiredId ->
        requiredId in meta.echoes.purchasedOfferIds
    }

    fun handle(
        state: GameState,
        command: PurchaseEchoOffer,
        context: EngineContext
    ): CommandHandlingResult {
        val definition = context.contentRegistry.echoOfferOrNull(command.offerId)
            ?: return CommandHandlingResult.Rejected(
                CommandRejectionReason(
                    code = CommandRejectionCode.UNKNOWN_CONTENT,
                    subjectContentId = command.offerId
                )
            )

        if (definition.repeatable) {
            return CommandHandlingResult.Rejected(
                CommandRejectionReason(
                    code = CommandRejectionCode.UNSUPPORTED,
                    subjectContentId = command.offerId
                )
            )
        }

        if (isPurchased(state.meta, definition)) {
            return CommandHandlingResult.Rejected(
                CommandRejectionReason(
                    code = CommandRejectionCode.ALREADY_OWNED,
                    subjectContentId = command.offerId
                )
            )
        }

        if (!prerequisitesSatisfied(state.meta, definition)) {
            return CommandHandlingResult.Rejected(
                CommandRejectionReason(
                    code = CommandRejectionCode.LOCKED,
                    subjectContentId = command.offerId
                )
            )
        }

        if (state.meta.echoes.available < definition.cost) {
            return CommandHandlingResult.Rejected(
                CommandRejectionReason(
                    code = CommandRejectionCode.INSUFFICIENT_RESOURCE,
                    subjectContentId = command.offerId
                )
            )
        }

        val purchasedEchoes = state.meta.echoes.copy(
            available = state.meta.echoes.available - definition.cost,
            spent = state.meta.echoes.spent + definition.cost,
            purchasedOfferIds = state.meta.echoes.purchasedOfferIds + definition.id
        )
        val purchasedMeta = state.meta.copy(echoes = purchasedEchoes)

        val effects = DiscoverySystem.applyEffects(
            meta = purchasedMeta,
            effects = definition.effects
        )
        val nextState = EchoTrainingSystem.applyPurchase(state.copy(meta = effects.meta), definition.id, context.contentRegistry)

        return CommandHandlingResult.Accepted(
            state = nextState,
            events = buildList {
                add(EchoSpent(amount = definition.cost, offerId = definition.id))
                add(EchoOfferPurchased(offerId = definition.id, cost = definition.cost))
                addAll(effects.events)
            }
        )
    }
}
