package com.idlerpg.game.simulation

import com.idlerpg.game.core.time.GameDuration
import org.junit.Test

/**
 * Normal JUnit entry points for the executable scenario inventory and the heavyweight balance
 * smoke. Keeping them separate prevents the long-run check from being duplicated by the inventory
 * runner while ensuring both are discoverable by the normal test task.
 */
class IntegrationScenarioTest {
    @Test
    fun completeScenarioInventoryPasses() {
        runScenarioInventory()
    }

    @Test
    fun longRunBalanceSmokePasses() {
        LongRunSimulationTest.run(duration = GameDuration.ofMinutes(30L))
    }
}
