package com.idlerpg.game.simulation

import com.idlerpg.game.core.id.InstanceId
import com.idlerpg.game.core.number.GameNumber
import com.idlerpg.game.core.number.Ratio
import com.idlerpg.game.data.local.SaveData
import com.idlerpg.game.data.content.DefaultGameContent
import com.idlerpg.game.domain.command.AllocateRebirthPoints
import com.idlerpg.game.domain.command.CommandRejectionCode
import com.idlerpg.game.domain.command.PerformRebirth
import com.idlerpg.game.domain.command.ResetRebirthAllocations
import com.idlerpg.game.domain.command.SelectRegion
import com.idlerpg.game.domain.command.StartEncounter
import com.idlerpg.game.domain.command.PurchaseUpgrade
import com.idlerpg.game.domain.definition.CurrencyId
import com.idlerpg.game.domain.event.CurrencyGranted
import com.idlerpg.game.domain.engine.CommandResult
import com.idlerpg.game.domain.model.doctrine.DoctrineAction
import com.idlerpg.game.domain.model.doctrine.DoctrineComparison
import com.idlerpg.game.domain.model.doctrine.DoctrineCondition
import com.idlerpg.game.domain.model.doctrine.DoctrinePredicate
import com.idlerpg.game.domain.model.doctrine.DoctrineRule
import com.idlerpg.game.domain.model.economy.CurrencyWallet
import com.idlerpg.game.domain.model.economy.UpgradeProgressState
import com.idlerpg.game.domain.model.player.BaseStats
import com.idlerpg.game.domain.model.progression.PlayerLevelState
import com.idlerpg.game.domain.model.rebirth.RebirthPointPool
import com.idlerpg.game.domain.model.rebirth.RebirthStat
import com.idlerpg.game.domain.system.rebirth.RebirthSystem
import com.idlerpg.game.domain.system.stats.DerivedStatSystem

/** Rebirth is an atomic soft reset with permanent point progression. */
object RebirthScenarioTest {
    fun run() {
        rejectedBeforeThresholdIsAtomic()
        rejectedDuringActiveCombatIsAtomic()
        resetPreservesLongLivedStateAndClearsRunProgression()
        allocationAndGemRespecArePersistent()
        deepRebirthRetainsPointsAndGrantsModestReward()
    }

    private fun rejectedBeforeThresholdIsAtomic() {
        val runtime = SimulationTestSupport.runtime(seed = 9_001L)
        val before = runtime.state()
        val result = runtime.dispatch(PerformRebirth())
        check((result.commandResult as CommandResult.Rejected).reason.code == CommandRejectionCode.NOT_READY)
        check(runtime.state() == before)
    }

    private fun rejectedDuringActiveCombatIsAtomic() {
        val runtime = SimulationTestSupport.runtime(seed = 9_004L)
        SimulationTestSupport.startTraining(runtime)
        val active = runtime.state().copy(
            run = runtime.state().run.copy(
                economy = runtime.state().run.economy.copy(
                    wallet = CurrencyWallet(
                        amountsByCurrencyId = mapOf(
                            CurrencyId.GOLD to GameNumber.of(100_000_000L)
                        )
                    )
                ),
                progression = runtime.state().run.progression.copy(
                    playerLevel = PlayerLevelState(RebirthSystem.MINIMUM_REBIRTH_LEVEL)
                )
            )
        )
        runtime.replaceLoadedState(active)
        val before = runtime.state()
        check(!RebirthSystem.preview(before).eligible)
        val result = runtime.dispatch(PerformRebirth())
        check((result.commandResult as CommandResult.Rejected).reason.code == CommandRejectionCode.INVALID_STATE)
        check(runtime.state() == before)
    }

    private fun resetPreservesLongLivedStateAndClearsRunProgression() {
        val runtime = SimulationTestSupport.runtime(seed = 9_002L)
        SimulationTestSupport.startTraining(runtime)
        val before = runtime.state()
        val equippedSkill = DefaultGameContent.QUICK_SLASH_ID
        val mature = before.copy(
            run = before.run.copy(
                combat = com.idlerpg.game.domain.model.combat.CombatState(),
                player = before.run.player.copy(
                    baseStats = BaseStats(
                        attackPower = GameNumber.of(15L),
                        maxHealth = GameNumber.of(120L)
                    ),
                    equippedSkillIds = listOf(equippedSkill)
                ),
                doctrine = before.run.doctrine.copy(
                    enabled = false,
                    rules = listOf(
                        DoctrineRule(
                            instanceId = InstanceId(1L),
                            condition = DoctrineCondition.Predicate(
                                DoctrinePredicate.EnemyHealthPercent(
                                    comparison = DoctrineComparison.LESS_THAN_OR_EQUAL,
                                    threshold = Ratio.ONE
                                )
                            ),
                            action = DoctrineAction.UseBasicAttack
                        )
                    )
                ),
                economy = before.run.economy.copy(
                    wallet = CurrencyWallet(
                        amountsByCurrencyId = mapOf(
                            CurrencyId.GOLD to GameNumber.of(100_000_000L),
                            CurrencyId.GEMS to GameNumber.of(100L)
                        )
                    ),
                    upgrades = UpgradeProgressState(
                        levelByUpgradeId = mapOf(
                            DefaultGameContent.BASIC_ATTACK_POWER_UPGRADE_ID to 3L
                        )
                    )
                ),
                progression = before.run.progression.copy(
                    playerLevel = PlayerLevelState(
                        level = RebirthSystem.MINIMUM_REBIRTH_LEVEL
                    ),
                    featureUnlocks = before.run.progression.featureUnlocks.copy(
                        unlockedFeatureIds = setOf(DefaultGameContent.QUICK_SLASH_ID)
                    )
                )
            )
        )
        runtime.replaceLoadedState(mature)
        val preservedInventory = mature.run.inventory
        val preservedResonance = mature.run.resonance
        val preservedAdaptation = mature.run.adaptation
        check(mature.run.doctrine.rules.isNotEmpty())
        val result = runtime.dispatch(PerformRebirth())
        SimulationTestSupport.checkAccepted(result)
        val after = runtime.state()

        check(after.meta.rebirth.completedRebirths == 1L)
        check(after.meta.rebirth.normalPointsEarned == 100L)
        check(after.meta.rebirth.legacyPointsEarned == 20L)
        check(after.run.progression.playerLevel.level == 1L)
        check(after.run.progression.playerLevel.currentExperience == GameNumber.ZERO)
        check(
            after.run.player.currentHealth ==
                DerivedStatSystem.maximumHealth(
                    after,
                    SimulationTestSupport.factory().contentRegistry
                )
        )
        check(after.run.progression.featureUnlocks.unlockedFeatureIds.isEmpty())
        check(after.run.player.equippedSkillIds.isEmpty())
        check(after.run.player.selectedSkillEvolutionBySkillId.isEmpty())
        check(after.run.economy.upgrades.levelByUpgradeId.isEmpty())
        check(after.run.world == com.idlerpg.game.domain.model.world.WorldState())
        check(after.run.combat == com.idlerpg.game.domain.model.combat.CombatState())
        check(SimulationTestSupport.gold(after) == GameNumber.ZERO)
        check(after.run.economy.wallet.amountsByCurrencyId[CurrencyId.GEMS] == GameNumber.of(100L))
        check(after.run.inventory == preservedInventory)
        check(after.run.resonance == preservedResonance)
        check(!after.run.doctrine.enabled)
        check(after.run.doctrine.rules.isEmpty())
        check(after.run.adaptation == preservedAdaptation)
    }

    private fun deepRebirthRetainsPointsAndGrantsModestReward() {
        val runtime = SimulationTestSupport.runtime(seed = 9_005L)
        val initial = runtime.state()
        runtime.replaceLoadedState(
            initial.copy(
                run = initial.run.copy(
                    economy = initial.run.economy.copy(
                        wallet = CurrencyWallet(
                            amountsByCurrencyId = mapOf(
                                CurrencyId.GOLD to GameNumber.of(100_000_000L)
                            )
                        )
                    ),
                    progression = initial.run.progression.copy(
                        playerLevel = PlayerLevelState(level = 15_000L)
                    )
                )
            )
        )

        val result = runtime.dispatch(PerformRebirth())
        SimulationTestSupport.checkAccepted(result)
        val after = runtime.state()
        check(after.meta.rebirth.completedRebirths == 1L)
        check(after.meta.rebirth.normalPointsEarned == 100L)
        check(after.meta.rebirth.legacyPointsEarned == 20L)
        check(
            after.run.economy.wallet.amountsByCurrencyId[CurrencyId.ENHANCEMENT_MATERIAL] ==
                GameNumber.of(RebirthSystem.DEEP_REBIRTH_ENHANCEMENT_MATERIAL_REWARD)
        )
        check(
            result.events.map { it.event }.filterIsInstance<CurrencyGranted>().single {
                it.currencyId == CurrencyId.ENHANCEMENT_MATERIAL
            }.amount == GameNumber.of(RebirthSystem.DEEP_REBIRTH_ENHANCEMENT_MATERIAL_REWARD)
        )
    }

    private fun allocationAndGemRespecArePersistent() {
        val runtime = SimulationTestSupport.runtime(seed = 9_003L)
        val initial = runtime.state()
        runtime.replaceLoadedState(initial.copy(
            run = initial.run.copy(
                economy = initial.run.economy.copy(
                    wallet = CurrencyWallet(
                        amountsByCurrencyId = mapOf(
                            CurrencyId.GOLD to GameNumber.of(100_000_000L),
                            CurrencyId.GEMS to GameNumber.of(100L)
                        )
                    )
                ),
                progression = initial.run.progression.copy(
                    playerLevel = PlayerLevelState(RebirthSystem.MINIMUM_REBIRTH_LEVEL)
                )
            )
        ))
        SimulationTestSupport.checkAccepted(runtime.dispatch(PerformRebirth()))
        SimulationTestSupport.checkAccepted(
            runtime.dispatch(
                AllocateRebirthPoints(
                    pool = RebirthPointPool.NORMAL,
                    stat = RebirthStat.ATTACK_POWER,
                    amount = 10L
                )
            )
        )
        check(runtime.state().meta.rebirth.unspentPoints(RebirthPointPool.NORMAL) == 90L)
        check(
            DerivedStatSystem.attackPower(
                runtime.state(),
                SimulationTestSupport.factory().contentRegistry
            ) == GameNumber.of(20L)
        )
        SimulationTestSupport.checkAccepted(
            runtime.dispatch(ResetRebirthAllocations(RebirthPointPool.NORMAL))
        )
        check(runtime.state().meta.rebirth.allocation(
            RebirthPointPool.NORMAL,
            RebirthStat.ATTACK_POWER
        ) == 0L)
        check(runtime.state().meta.rebirth.unspentPoints(RebirthPointPool.NORMAL) == 100L)
        check(SimulationTestSupport.gold(runtime.state()) == GameNumber.ZERO)
        check(
            runtime.state().run.economy.wallet.amountsByCurrencyId[CurrencyId.GEMS] ==
                GameNumber.ZERO
        )

        val restored = SaveData.fromGameState(runtime.state()).toGameState()
        check(restored == runtime.state())
    }
}
