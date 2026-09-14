package com.idlerpg.game.simulation

import com.idlerpg.game.core.number.GameNumber
import com.idlerpg.game.core.time.GameDuration
import com.idlerpg.game.data.content.DefaultGameContent
import com.idlerpg.game.domain.command.CommandRejectionCode
import com.idlerpg.game.domain.command.PurchaseUpgrade
import com.idlerpg.game.domain.engine.CommandResult
import com.idlerpg.game.domain.system.economy.UpgradeSystem
import com.idlerpg.game.domain.system.stats.DerivedStatSystem

/** Verified Phase 2 cost curve and no-negative/no-partial-purchase invariants. */
object EconomyInvariantTest {
    fun run() {
        val factory = SimulationTestSupport.factory()
        val runtime = com.idlerpg.game.application.GameRuntime(
            initialSession = factory.newGame(9L),
            sessionFactory = factory
        )
        SimulationTestSupport.startTraining(runtime)
        runtime.advance(GameDuration.ofSeconds(20L))

        check(SimulationTestSupport.gold(runtime.state()) == GameNumber.of(22L))
        check(
            UpgradeSystem.currentCost(
                state = runtime.state(),
                upgradeId = DefaultGameContent.BASIC_ATTACK_POWER_UPGRADE_ID,
                context = factory.createEngineContext()
            ) == GameNumber.of(20L)
        )

        SimulationTestSupport.checkAccepted(
            runtime.dispatch(
                PurchaseUpgrade(DefaultGameContent.BASIC_ATTACK_POWER_UPGRADE_ID)
            )
        )
        check(SimulationTestSupport.gold(runtime.state()) == GameNumber.of(2L))
        check(
            runtime.state().run.economy.upgrades.levelByUpgradeId[
                DefaultGameContent.BASIC_ATTACK_POWER_UPGRADE_ID
            ] == 1L
        )
        check(
            DerivedStatSystem.attackPower(
                runtime.state(),
                factory.contentRegistry
            ) == GameNumber.of(15L)
        )
        check(
            UpgradeSystem.currentCost(
                state = runtime.state(),
                upgradeId = DefaultGameContent.BASIC_ATTACK_POWER_UPGRADE_ID,
                context = factory.createEngineContext()
            ) == GameNumber.of(30L)
        )

        val beforeRejected = runtime.state()
        val rejected = runtime.dispatch(
            PurchaseUpgrade(DefaultGameContent.BASIC_ATTACK_POWER_UPGRADE_ID)
        )
        val rejection = rejected.commandResult as? CommandResult.Rejected
            ?: error("Expected insufficient-resource rejection")
        check(rejection.reason.code == CommandRejectionCode.INSUFFICIENT_RESOURCE)
        check(rejected.state == beforeRejected)
        check(SimulationTestSupport.gold(rejected.state) >= GameNumber.ZERO)
    }
}
