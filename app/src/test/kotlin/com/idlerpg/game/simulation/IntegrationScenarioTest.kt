package com.idlerpg.game.simulation

import org.junit.Test

/**
 * Normal JUnit entry point for the complete executable scenario inventory.
 *
 * The inventory itself remains centralized in LongRunSimulationTest so the plain runner and
 * the command-line harness execute the same set of checks.
 */
class IntegrationScenarioTest {
    @Test
    fun completeScenarioInventoryPasses() {
        runAllScenarioChecks()
    }
}
