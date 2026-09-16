package com.idlerpg.game.simulation

import com.idlerpg.game.presentation.OfflineProgressProjectionTest
import org.junit.Test

/** Runs focused PR #3 contract checks through Android unit tests. */
class IntegrationScenarioTest {
    @Test
    fun rebirthContractPasses() {
        RebirthScenarioTest.run()
    }

    @Test
    fun offlineReturnContractPasses() {
        OfflineReturnScenarioTest.run()
    }

    @Test
    fun activeOfflineEquivalencePasses() {
        ActiveOfflineEquivalenceTest.run()
    }

    @Test
    fun worldProgressionContractPasses() {
        WorldProgressionScenarioTest.run()
    }

    @Test
    fun enemyCombatContractPasses() {
        EnemyCombatVarietyScenarioTest.run()
    }

    @Test
    fun prototypeParityContractPasses() {
        PrototypeParityTest.run()
    }

    @Test
    fun offlineProjectionContractPasses() {
        OfflineProgressProjectionTest.main(emptyArray())
    }
}
