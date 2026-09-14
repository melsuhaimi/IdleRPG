package com.idlerpg.game.simulation

import com.idlerpg.game.application.GameRuntime
import com.idlerpg.game.core.number.GameNumber
import com.idlerpg.game.core.number.Ratio
import com.idlerpg.game.core.time.GameDuration
import com.idlerpg.game.data.content.DefaultGameContent
import com.idlerpg.game.data.local.SaveData
import com.idlerpg.game.domain.command.CommandRejectionCode
import com.idlerpg.game.domain.command.PurchaseUpgrade
import com.idlerpg.game.domain.definition.CurrencyId
import com.idlerpg.game.domain.definition.DamageKind
import com.idlerpg.game.domain.definition.combat.EffectSpec
import com.idlerpg.game.domain.engine.CommandResult
import com.idlerpg.game.domain.event.UpgradePurchased
import com.idlerpg.game.domain.event.DamageDealt
import com.idlerpg.game.domain.model.GameState
import com.idlerpg.game.domain.model.economy.CurrencyWallet
import com.idlerpg.game.domain.model.economy.UpgradeProgressState
import com.idlerpg.game.domain.system.economy.UpgradeSystem
import com.idlerpg.game.domain.system.combat.ActionResolutionSystem
import com.idlerpg.game.domain.system.combat.DamageSystem
import com.idlerpg.game.domain.system.combat.HealingSystem
import com.idlerpg.game.domain.system.stats.DerivedStatSystem

/** Gate 39: eight live stat contracts, exact atomic quantities, milestones, and V6 persistence. */
object CoreGrowthScenarioTest {
    fun run() {
        val factory = SimulationTestSupport.factory()
        val registry = factory.contentRegistry
        val context = factory.createEngineContext()
        val upgrades = registry.allUpgrades().sortedBy { it.id }
        check(upgrades.size == 8)
        check(upgrades.single { it.id == DefaultGameContent.BASIC_ATTACK_POWER_UPGRADE_ID }.maxLevel == null)
        check(upgrades.filterNot { it.id == DefaultGameContent.BASIC_ATTACK_POWER_UPGRADE_ID }
            .all { it.maxLevel == 50L })

        val fresh = factory.newGame(7_001L).state()
        val funded = withGold(fresh, GameNumber.of(10_000L))
        check(UpgradeSystem.purchaseCost(funded, DefaultGameContent.BASIC_ATTACK_POWER_UPGRADE_ID, 1L, context) == GameNumber.of(20L))
        check(UpgradeSystem.purchaseCost(funded, DefaultGameContent.BASIC_ATTACK_POWER_UPGRADE_ID, 10L, context) == GameNumber.of(650L))
        check(UpgradeSystem.purchaseCost(funded, DefaultGameContent.BASIC_ATTACK_POWER_UPGRADE_ID, 25L, context) == GameNumber.of(3_500L))
        assertAtomicPurchase(factory, fresh, 10L, GameNumber.of(650L))
        assertAtomicPurchase(factory, fresh, 25L, GameNumber.of(3_500L))

        val maxState = withGold(fresh, GameNumber.of(1_000L))
        check(UpgradeSystem.maximumAffordableQuantity(maxState, DefaultGameContent.BASIC_ATTACK_POWER_UPGRADE_ID, context) == 12L)
        val runtime = GameRuntime(factory.loadedGame(maxState), factory)
        val maxPurchase = runtime.dispatch(PurchaseUpgrade(DefaultGameContent.BASIC_ATTACK_POWER_UPGRADE_ID, 12L))
        SimulationTestSupport.checkAccepted(maxPurchase)
        check(runtime.state().run.economy.upgrades.levelByUpgradeId[DefaultGameContent.BASIC_ATTACK_POWER_UPGRADE_ID] == 12L)
        check(SimulationTestSupport.gold(runtime.state()) == GameNumber.of(100L))
        check(maxPurchase.events.map { it.event }.filterIsInstance<UpgradePurchased>().single().quantity == 12L)

        val insufficientRuntime = GameRuntime(factory.loadedGame(withGold(fresh, GameNumber.of(19L))), factory)
        val beforeRejected = insufficientRuntime.state()
        val rejected = insufficientRuntime.dispatch(PurchaseUpgrade(DefaultGameContent.BASIC_ATTACK_POWER_UPGRADE_ID, 1L))
        check((rejected.commandResult as CommandResult.Rejected).reason.code == CommandRejectionCode.INSUFFICIENT_RESOURCE)
        check(rejected.state == beforeRejected)
        val bulkRuntime = GameRuntime(factory.loadedGame(withGold(fresh, GameNumber.of(649L))), factory)
        val bulkBefore = bulkRuntime.state()
        val bulkRejected = bulkRuntime.dispatch(
            PurchaseUpgrade(DefaultGameContent.BASIC_ATTACK_POWER_UPGRADE_ID, 10L)
        )
        check((bulkRejected.commandResult as CommandResult.Rejected).reason.code == CommandRejectionCode.INSUFFICIENT_RESOURCE)
        check(bulkRejected.state == bulkBefore)

        val attackDefinition = upgrades.single { it.id == DefaultGameContent.BASIC_ATTACK_POWER_UPGRADE_ID }
        check(attackDefinition.effectiveLevel(9L) == 9L)
        check(attackDefinition.effectiveLevel(10L) == 12L)
        check(attackDefinition.effectiveLevel(19L) == 21L)
        check(attackDefinition.effectiveLevel(20L) == 25L)
        check(
            DerivedStatSystem.attackPower(
                withLevels(fresh, mapOf(DefaultGameContent.BASIC_ATTACK_POWER_UPGRADE_ID to 10L)), registry
            ) - DerivedStatSystem.attackPower(
                withLevels(fresh, mapOf(DefaultGameContent.BASIC_ATTACK_POWER_UPGRADE_ID to 9L)), registry
            ) == GameNumber.of(15L)
        )
        check(
            DerivedStatSystem.attackPower(
                withLevels(fresh, mapOf(DefaultGameContent.BASIC_ATTACK_POWER_UPGRADE_ID to 20L)), registry
            ) - DerivedStatSystem.attackPower(
                withLevels(fresh, mapOf(DefaultGameContent.BASIC_ATTACK_POWER_UPGRADE_ID to 19L)), registry
            ) == GameNumber.of(20L)
        )

        val base = fresh
        fun atOne(id: com.idlerpg.game.core.id.ContentId): GameState = withLevels(base, mapOf(id to 1L))
        check(DerivedStatSystem.attackPower(atOne(DefaultGameContent.BASIC_ATTACK_POWER_UPGRADE_ID), registry) > DerivedStatSystem.attackPower(base, registry))
        check(DerivedStatSystem.maximumHealth(atOne(DefaultGameContent.ENDURANCE_UPGRADE_ID), registry) > DerivedStatSystem.maximumHealth(base, registry))
        check(DerivedStatSystem.armor(atOne(DefaultGameContent.ARMOR_TRAINING_UPGRADE_ID), registry) > DerivedStatSystem.armor(base, registry))
        check(DerivedStatSystem.basicAttackInterval(atOne(DefaultGameContent.TEMPO_TRAINING_UPGRADE_ID), registry.basicAttack, registry) < DerivedStatSystem.basicAttackInterval(base, registry.basicAttack, registry))
        check(DerivedStatSystem.criticalChance(atOne(DefaultGameContent.PRECISION_UPGRADE_ID), registry) > DerivedStatSystem.criticalChance(base, registry))
        check(DerivedStatSystem.criticalMultiplier(atOne(DefaultGameContent.LETHALITY_UPGRADE_ID), registry) > DerivedStatSystem.criticalMultiplier(base, registry))
        check(DerivedStatSystem.effectPower(atOne(DefaultGameContent.CHANNELING_UPGRADE_ID), registry) > DerivedStatSystem.effectPower(base, registry))
        check(DerivedStatSystem.healingPower(atOne(DefaultGameContent.RESTORATION_UPGRADE_ID), registry) > DerivedStatSystem.healingPower(base, registry))
        verifyCombatOutcomes(factory, base)

        val precision49 = withLevels(base, mapOf(DefaultGameContent.PRECISION_UPGRADE_ID to 49L))
        val precision50 = withLevels(base, mapOf(DefaultGameContent.PRECISION_UPGRADE_ID to 50L))
        check(DerivedStatSystem.criticalChance(precision50, registry) > DerivedStatSystem.criticalChance(precision49, registry))
        check(UpgradeSystem.maximumAffordableQuantity(withGold(precision50, GameNumber.of(1_000_000L)), DefaultGameContent.PRECISION_UPGRADE_ID, context) == 0L)
        val cappedRuntime = GameRuntime(factory.loadedGame(withGold(precision50, GameNumber.of(1_000_000L))), factory)
        val cappedBefore = cappedRuntime.state()
        val capped = cappedRuntime.dispatch(PurchaseUpgrade(DefaultGameContent.PRECISION_UPGRADE_ID, 1L))
        check((capped.commandResult as CommandResult.Rejected).reason.code == CommandRejectionCode.CAPACITY_EXCEEDED)
        check(capped.state == cappedBefore)

        val allLevels = upgrades.mapIndexed { index, upgrade -> upgrade.id to (index + 1L) }.toMap()
        val saved = withLevels(funded, allLevels)
        check(SaveData.fromGameState(saved).toGameState() == saved)
    }

    private fun verifyCombatOutcomes(
        factory: com.idlerpg.game.application.GameSessionFactory,
        fresh: GameState
    ) {
        val registry = factory.contentRegistry

        fun started(
            levels: Map<com.idlerpg.game.core.id.ContentId, Long> = emptyMap(),
            baseStats: com.idlerpg.game.domain.model.player.BaseStats? = null
        ): GameState {
            var state = withLevels(fresh, levels)
            if (baseStats != null) {
                state = state.copy(run = state.run.copy(player = state.run.player.copy(baseStats = baseStats)))
            }
            val runtime = GameRuntime(factory.loadedGame(state), factory)
            SimulationTestSupport.startTraining(runtime)
            return runtime.state()
        }

        fun firstHit(
            levels: Map<com.idlerpg.game.core.id.ContentId, Long> = emptyMap(),
            baseStats: com.idlerpg.game.domain.model.player.BaseStats? = null
        ): DamageDealt {
            var state = withLevels(fresh, levels)
            if (baseStats != null) {
                state = state.copy(run = state.run.copy(player = state.run.player.copy(baseStats = baseStats)))
            }
            val runtime = GameRuntime(factory.loadedGame(state), factory)
            SimulationTestSupport.startTraining(runtime)
            val enemyId = runtime.state().run.combat.enemies.single().instanceId
            return runtime.advance(GameDuration.ofSeconds(1L)).events.map { it.event }
                .filterIsInstance<DamageDealt>().single { it.targetInstanceId == enemyId }
        }

        check(firstHit().amount == GameNumber.of(10L))
        check(firstHit(mapOf(DefaultGameContent.BASIC_ATTACK_POWER_UPGRADE_ID to 1L)).amount == GameNumber.of(15L))

        val unarmored = started()
        val armored = started(mapOf(DefaultGameContent.ARMOR_TRAINING_UPGRADE_ID to 10L))
        val source = unarmored.run.combat.enemies.single().instanceId
        val damageKindId = DamageKind.PHYSICAL.id
        val unarmoredDamage = DamageSystem.dealToPlayer(
            unarmored, source, GameNumber.of(50L), GameNumber.ZERO, damageKindId, registry
        ).event.amount
        val armoredDamage = DamageSystem.dealToPlayer(
            armored, armored.run.combat.enemies.single().instanceId,
            GameNumber.of(50L), GameNumber.ZERO, damageKindId, registry
        ).event.amount
        check(armoredDamage < unarmoredDamage)

        val normalDeadline = started().run.combat.nextPlayerDecisionAt ?: error("Missing deadline")
        val tempoDeadline = started(mapOf(DefaultGameContent.TEMPO_TRAINING_UPGRADE_ID to 10L))
            .run.combat.nextPlayerDecisionAt ?: error("Missing tempo deadline")
        check(tempoDeadline < normalDeadline)

        fun emptyHealth(state: GameState): Pair<GameState, com.idlerpg.game.core.id.InstanceId> {
            val player = state.run.combat.playerCombatant ?: error("Missing combatant")
            return state.copy(run = state.run.copy(
                player = state.run.player.copy(currentHealth = GameNumber.ZERO),
                combat = state.run.combat.copy(
                    playerCombatant = player.copy(currentHealth = GameNumber.ZERO)
                )
            )) to player.instanceId
        }
        val (enduranceState, enduranceId) = emptyHealth(
            started(mapOf(DefaultGameContent.ENDURANCE_UPGRADE_ID to 1L))
        )
        val enduranceHeal = HealingSystem.healPlayer(
            enduranceState, enduranceId, enduranceId, GameNumber.of(1_000L), registry,
            scaleWithHealingPower = false
        )
        check(enduranceHeal.event.amount == GameNumber.of(110L))

        val guaranteedBase = fresh.run.player.baseStats.copy(
            criticalChance = Ratio.ofUnits(4_500L)
        )
        val precision = firstHit(
            mapOf(DefaultGameContent.PRECISION_UPGRADE_ID to 50L), guaranteedBase
        )
        check(precision.critical)

        val criticalBase = fresh.run.player.baseStats.copy(
            criticalChance = Ratio.ONE,
            criticalMultiplier = Ratio.ofUnits(20_000L)
        )
        val criticalBaseline = firstHit(baseStats = criticalBase)
        val lethal = firstHit(
            mapOf(DefaultGameContent.LETHALITY_UPGRADE_ID to 10L), criticalBase
        )
        check(lethal.critical && lethal.amount > criticalBaseline.amount)

        fun primitiveDamage(levels: Map<com.idlerpg.game.core.id.ContentId, Long>): GameNumber {
            val state = started(levels)
            val playerId = state.run.combat.playerCombatant?.instanceId ?: error("Missing player")
            val enemyId = state.run.combat.enemies.single().instanceId
            val effect = EffectSpec.DealDamage(
                powerRatio = Ratio.HALF,
                damageKind = DamageKind.PHYSICAL,
                scalingPolicy = EffectSpec.DamageScalingPolicy.ATTACK_AND_EFFECT_POWER,
                canCritical = false
            )
            return ActionResolutionSystem.resolvePrimitiveEffects(
                state, playerId, enemyId, listOf(effect), factory.createEngineContext()
            ).events.filterIsInstance<DamageDealt>().single().amount
        }
        check(primitiveDamage(mapOf(DefaultGameContent.CHANNELING_UPGRADE_ID to 10L)) > primitiveDamage(emptyMap()))

        fun restored(levels: Map<com.idlerpg.game.core.id.ContentId, Long>): GameNumber {
            val (state, playerId) = emptyHealth(started(levels))
            return HealingSystem.healPlayer(
                state, playerId, playerId, GameNumber.of(25L), registry
            ).event.amount
        }
        check(restored(mapOf(DefaultGameContent.RESTORATION_UPGRADE_ID to 10L)) > restored(emptyMap()))
    }

    private fun withGold(state: GameState, amount: GameNumber): GameState = state.copy(
        run = state.run.copy(
            economy = state.run.economy.copy(
                wallet = CurrencyWallet(mapOf(CurrencyId.GOLD to amount))
            )
        )
    )

    private fun assertAtomicPurchase(
        factory: com.idlerpg.game.application.GameSessionFactory,
        fresh: GameState,
        quantity: Long,
        expectedCost: GameNumber
    ) {
        val openingGold = GameNumber.of(10_000L)
        val runtime = GameRuntime(factory.loadedGame(withGold(fresh, openingGold)), factory)
        val result = runtime.dispatch(
            PurchaseUpgrade(DefaultGameContent.BASIC_ATTACK_POWER_UPGRADE_ID, quantity)
        )
        SimulationTestSupport.checkAccepted(result)
        check(SimulationTestSupport.gold(runtime.state()) == openingGold - expectedCost)
        check(runtime.state().run.economy.upgrades.levelByUpgradeId[
            DefaultGameContent.BASIC_ATTACK_POWER_UPGRADE_ID
        ] == quantity)
        val event = result.events.map { it.event }.filterIsInstance<UpgradePurchased>().single()
        check(event.quantity == quantity)
        check(event.newLevel == quantity)
        check(event.totalCost == expectedCost)
    }

    private fun withLevels(
        state: GameState,
        levels: Map<com.idlerpg.game.core.id.ContentId, Long>
    ): GameState = state.copy(
        run = state.run.copy(
            economy = state.run.economy.copy(upgrades = UpgradeProgressState(levels.toSortedMap()))
        )
    )
}
