package com.idlerpg.game.simulation

import com.idlerpg.game.core.id.ContentId
import com.idlerpg.game.data.content.DefaultGameContent
import com.idlerpg.game.domain.command.CommandRejectionCode
import com.idlerpg.game.domain.command.PurchaseUpgrade
import com.idlerpg.game.domain.command.SelectRegion
import com.idlerpg.game.domain.engine.CommandResult

/** Expected invalid player intent must reject without partial canonical mutation. */
object CommandRejectionTest {
    fun run() {
        val runtime = SimulationTestSupport.runtime(seed = 7L)

        val beforePurchase = runtime.state()
        val purchase = runtime.dispatch(
            PurchaseUpgrade(DefaultGameContent.BASIC_ATTACK_POWER_UPGRADE_ID)
        )
        val purchaseRejection = purchase.commandResult as? CommandResult.Rejected
            ?: error("Expected unaffordable purchase rejection")
        check(purchaseRejection.reason.code == CommandRejectionCode.INSUFFICIENT_RESOURCE)
        check(purchase.state == beforePurchase)
        check(purchase.events.isEmpty())

        val beforeWorld = runtime.state()
        val unknown = runtime.dispatch(
            SelectRegion(ContentId("region.unknown"))
        )
        val worldRejection = unknown.commandResult as? CommandResult.Rejected
            ?: error("Expected unknown region rejection")
        check(worldRejection.reason.code == CommandRejectionCode.UNKNOWN_CONTENT)
        check(unknown.state == beforeWorld)
        check(unknown.events.isEmpty())
    }
}
