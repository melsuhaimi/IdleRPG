package com.idlerpg.game.simulation

import com.idlerpg.game.core.id.ContentId
import com.idlerpg.game.core.number.GameNumber
import com.idlerpg.game.core.time.GameDuration
import com.idlerpg.game.data.content.DefaultGameContent
import com.idlerpg.game.data.local.SaveData
import com.idlerpg.game.domain.command.CommandRejectionCode
import com.idlerpg.game.domain.command.CommitChronicleCollapse
import com.idlerpg.game.domain.command.PurchaseEchoOffer
import com.idlerpg.game.domain.command.RequestChroniclePreview
import com.idlerpg.game.domain.engine.CommandResult
import com.idlerpg.game.domain.event.ChroniclePreviewPrepared
import com.idlerpg.game.domain.event.DiscoveryUnlocked
import com.idlerpg.game.domain.event.EchoOfferPurchased
import com.idlerpg.game.domain.event.EchoSpent
import com.idlerpg.game.domain.system.chronicle.EchoSystem

/** FBE-03 deterministic manual Echo-spend and persistence regression. */
object EchoShopScenarioTest {

    fun run() {
        val runtime = SimulationTestSupport.runtime(seed = 803L)
        val offerId = DefaultGameContent.ADAPTATION_FORECAST_ECHO_OFFER_ID
        val discoveryId = DefaultGameContent.ADAPTATION_FORECAST_DISCOVERY_ID

        val registry = SimulationTestSupport.factory().contentRegistry
        check(registry.allEchoUnlocks().isEmpty())
        check(registry.allEchoOffers().size == 12)
        check(registry.echoOffer(offerId).cost == GameNumber.of(5L))

        val initial = runtime.state()
        check(initial.meta.echoes.available == GameNumber.ZERO)
        check(initial.meta.echoes.spent == GameNumber.ZERO)
        check(initial.meta.echoes.purchasedOfferIds.isEmpty())
        check(discoveryId !in initial.meta.discoveries.unlockedHiddenContentIds)

        val unknown = runtime.dispatch(
            PurchaseEchoOffer(ContentId("echo_offer.unknown"))
        )
        checkRejected(unknown.commandResult, CommandRejectionCode.UNKNOWN_CONTENT)
        check(runtime.state() == initial)

        val insufficient = runtime.dispatch(PurchaseEchoOffer(offerId))
        checkRejected(
            insufficient.commandResult,
            CommandRejectionCode.INSUFFICIENT_RESOURCE
        )
        check(runtime.state() == initial)

        SimulationTestSupport.makeChronicleEligible(runtime)
        collapse(runtime)

        val earned = runtime.state()
        check(earned.meta.echoes.available == GameNumber.of(35L))
        check(earned.meta.echoes.spent == GameNumber.ZERO)
        check(earned.meta.echoes.purchasedOfferIds.isEmpty())
        check(EchoSystem.lifetimeEarned(earned.meta.echoes) == GameNumber.of(35L))
        check(discoveryId !in earned.meta.discoveries.unlockedHiddenContentIds)

        val purchase = runtime.dispatch(PurchaseEchoOffer(offerId))
        SimulationTestSupport.checkAccepted(purchase)

        val purchaseEvents = purchase.events.map { it.event }
        check(purchaseEvents.size == 3)
        check(purchaseEvents[0] == EchoSpent(GameNumber.of(5L), offerId))
        check(purchaseEvents[1] == EchoOfferPurchased(offerId, GameNumber.of(5L)))
        check(purchaseEvents[2] == DiscoveryUnlocked(discoveryId))

        val purchased = runtime.state()
        check(purchased.meta.echoes.available == GameNumber.of(30L))
        check(purchased.meta.echoes.spent == GameNumber.of(5L))
        check(offerId in purchased.meta.echoes.purchasedOfferIds)
        check(discoveryId in purchased.meta.discoveries.unlockedHiddenContentIds)
        check(EchoSystem.lifetimeEarned(purchased.meta.echoes) == GameNumber.of(35L))

        val duplicateState = runtime.state()
        val duplicate = runtime.dispatch(PurchaseEchoOffer(offerId))
        checkRejected(duplicate.commandResult, CommandRejectionCode.ALREADY_OWNED)
        check(runtime.state() == duplicateState)

        val roundTrip = SaveData.fromGameState(runtime.state()).toGameState()
        check(roundTrip == runtime.state())

        SimulationTestSupport.makeChronicleEligible(runtime)
        collapse(runtime)

        val persisted = runtime.state()
        check(persisted.meta.echoes.available == GameNumber.of(65L))
        check(persisted.meta.echoes.spent == GameNumber.of(5L))
        check(offerId in persisted.meta.echoes.purchasedOfferIds)
        check(discoveryId in persisted.meta.discoveries.unlockedHiddenContentIds)
        check(EchoSystem.lifetimeEarned(persisted.meta.echoes) == GameNumber.of(70L))
    }

    private fun collapse(runtime: com.idlerpg.game.application.GameRuntime) {
        val preview = runtime.dispatch(RequestChroniclePreview())
        SimulationTestSupport.checkAccepted(preview)
        val prepared = preview.events
            .map { it.event }
            .filterIsInstance<ChroniclePreviewPrepared>()
            .single()
        SimulationTestSupport.checkAccepted(
            runtime.dispatch(CommitChronicleCollapse(prepared.previewToken))
        )
    }

    private fun checkRejected(
        result: CommandResult?,
        expectedCode: CommandRejectionCode
    ) {
        val rejected = result as? CommandResult.Rejected
            ?: error("Expected rejected command, got $result")
        check(rejected.reason.code == expectedCode) {
            "Expected rejection $expectedCode, got ${rejected.reason.code}"
        }
    }
}
