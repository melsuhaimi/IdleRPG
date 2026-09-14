package com.idlerpg.game.simulation

import com.idlerpg.game.core.number.GameNumber
import com.idlerpg.game.data.content.DefaultGameContent
import com.idlerpg.game.data.content.EchoTrainingContent
import com.idlerpg.game.data.local.SaveData
import com.idlerpg.game.domain.command.PurchaseEchoOffer
import com.idlerpg.game.domain.command.RequestChroniclePreview
import com.idlerpg.game.domain.command.CommitChronicleCollapse
import com.idlerpg.game.domain.event.ChroniclePreviewPrepared
import com.idlerpg.game.domain.system.progression.FeatureUnlockSystem
import com.idlerpg.game.domain.system.chronicle.EchoSystem
import com.idlerpg.game.domain.system.chronicle.EchoTrainingSystem

object EchoTrainingScenarioTest {
    fun run() {
        val factory = SimulationTestSupport.factory()
        val runtime = SimulationTestSupport.runtime(seed = 810L)
        val initial = runtime.state()
        val eligible = FeatureUnlockSystem.unlockEligible(initial, factory.contentRegistry).state
        check(DefaultGameContent.LEGACY_ACCELERATION_FEATURE_ID !in eligible.meta.persistentFeatureUnlocks.unlockedFeatureIds)
        check(EchoTrainingSystem.startingLevels(eligible.meta).isEmpty())
        EchoTrainingContent.upgradeIds.forEach { id ->
            check(EchoTrainingContent.featureId(id) !in eligible.meta.persistentFeatureUnlocks.unlockedFeatureIds)
        }
        val grant = EchoSystem.grant(initial.meta, GameNumber.of(64L), DefaultGameContent.STANDARD_CHRONICLE_ID)
        runtime.replaceLoadedState(initial.copy(meta = grant.meta))
        val selected = EchoTrainingContent.upgradeIds.first()
        val offer = EchoTrainingContent.offerId(selected)
        SimulationTestSupport.checkAccepted(runtime.dispatch(PurchaseEchoOffer(offer)))
        val trained = runtime.state()
        check(trained.run.economy.upgrades.levelByUpgradeId[selected] == 5L)
        check(trained.meta.echoes.available == GameNumber.of(56L))
        check(EchoTrainingSystem.startingLevels(trained.meta) == mapOf(selected to 5L))
        check(SaveData.fromGameState(trained).toGameState() == trained)
        runtime.dispatch(PurchaseEchoOffer(offer))
        check(runtime.state() == trained) // No duplicate credits or second charge.
        SimulationTestSupport.makeChronicleEligible(runtime)
        val preview = runtime.dispatch(RequestChroniclePreview())
        SimulationTestSupport.checkAccepted(preview)
        val token = preview.events.map { it.event }.filterIsInstance<ChroniclePreviewPrepared>().single().previewToken
        SimulationTestSupport.checkAccepted(runtime.dispatch(CommitChronicleCollapse(token)))
        val reborn = runtime.state()
        check(reborn.run.economy.upgrades.levelByUpgradeId == mapOf(selected to 5L))
        check(offer in reborn.meta.echoes.purchasedOfferIds)
        check(reborn.run.progression.playerLevel.level == 1L)
    }
}
