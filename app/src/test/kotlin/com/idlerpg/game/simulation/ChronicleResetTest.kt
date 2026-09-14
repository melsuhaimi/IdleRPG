package com.idlerpg.game.simulation

import com.idlerpg.game.core.number.GameNumber
import com.idlerpg.game.core.time.GameDuration
import com.idlerpg.game.data.content.DefaultGameContent
import com.idlerpg.game.domain.command.CommitChronicleCollapse
import com.idlerpg.game.domain.command.RequestChroniclePreview
import com.idlerpg.game.domain.event.ChroniclePreviewPrepared
import com.idlerpg.game.domain.model.RunState
import com.idlerpg.game.domain.model.economy.UpgradeProgressState

/** Chronicle preview/commit must reset RunState while preserving persistent MetaState. */
object ChronicleResetTest {
    fun run() {
        val runtime = SimulationTestSupport.runtime(seed = 303L)
        SimulationTestSupport.makeChronicleEligible(runtime)
        val eligible = runtime.state()
        runtime.replaceLoadedState(
            eligible.copy(
                run = eligible.run.copy(
                    economy = eligible.run.economy.copy(
                        upgrades = UpgradeProgressState(
                            DefaultGameContent.registry().allUpgrades().associate { it.id to 7L }
                        )
                    ),
                    player = eligible.run.player.copy(
                        selectedSkillEvolutionBySkillId = mapOf(
                            DefaultGameContent.HEAVY_STRIKE_ID to
                                DefaultGameContent.EARTHBREAKER_EVOLUTION_ID,
                            DefaultGameContent.FLAME_BRAND_ID to
                                DefaultGameContent.WILDSPARK_EVOLUTION_ID,
                            DefaultGameContent.FROST_LANCE_ID to
                                DefaultGameContent.SHATTER_SPEAR_EVOLUTION_ID,
                            DefaultGameContent.UMBRAL_CUT_ID to
                                DefaultGameContent.SANGUINE_EDGE_EVOLUTION_ID
                        )
                    )
                )
            )
        )

        val preview = runtime.dispatch(RequestChroniclePreview())
        SimulationTestSupport.checkAccepted(preview)
        val previewEvent = preview.events
            .map { it.event }
            .filterIsInstance<ChroniclePreviewPrepared>()
            .single()

        SimulationTestSupport.checkAccepted(
            runtime.dispatch(
                CommitChronicleCollapse(
                    expectedPreviewToken = previewEvent.previewToken
                )
            )
        )

        val state = runtime.state()
        check(state.run == RunState())
        check(state.meta.chronicle.currentChronicleNumber == 2L)
        check(state.meta.chronicle.completedChronicles == GameNumber.ONE)
        check(state.meta.echoes.available == GameNumber.of(35L))
        check(state.meta.echoes.spent == GameNumber.ZERO)
        check(state.meta.echoes.purchasedOfferIds.isEmpty())
        check(
            DefaultGameContent.ADAPTATION_FORECAST_DISCOVERY_ID !in
                state.meta.discoveries.unlockedHiddenContentIds
        )
    }
}
