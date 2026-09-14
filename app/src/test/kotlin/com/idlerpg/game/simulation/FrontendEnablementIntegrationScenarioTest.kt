package com.idlerpg.game.simulation

/**
 * FBE-05 final integration gate over every post-Foundation-18 frontend-enablement surface.
 *
 * Each constituent scenario keeps ownership of its focused assertions. This aggregator
 * exists so one dependency-free executable harness can prove the final command router,
 * Save V2 contract, and deterministic manual interaction stack together.
 */
object FrontendEnablementIntegrationScenarioTest {
    fun run() {
        ManualSkillQueueScenarioTest.run()
        SkillLoadoutScenarioTest.run()
        QuestClaimScenarioTest.run()
        AchievementClaimScenarioTest.run()
        EchoShopScenarioTest.run()
        InventoryCapacityScenarioTest.run()
        SaveV2MigrationTest.run()
    }
}
