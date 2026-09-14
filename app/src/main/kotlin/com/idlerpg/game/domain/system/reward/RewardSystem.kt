package com.idlerpg.game.domain.system.reward

import com.idlerpg.game.core.id.ContentId
import com.idlerpg.game.core.number.GameMath
import com.idlerpg.game.core.number.GameNumber
import com.idlerpg.game.domain.definition.CurrencyId
import com.idlerpg.game.domain.definition.enemy.EnemyDefinition
import com.idlerpg.game.domain.engine.EngineContext
import com.idlerpg.game.domain.event.CurrencyGranted
import com.idlerpg.game.domain.event.GameEvent
import com.idlerpg.game.domain.model.GameState
import com.idlerpg.game.domain.model.combat.EnemyState
import com.idlerpg.game.domain.system.economy.TransactionSystem
import com.idlerpg.game.domain.system.inventory.InventoryCapacitySystem
import com.idlerpg.game.domain.system.loot.LootSystem
import com.idlerpg.game.domain.system.enemy.EnemyScalingSystem
import com.idlerpg.game.domain.system.progression.PlayerProgressionSystem

/** One authored/runtime currency component inside an atomic reward bundle. */
data class CurrencyReward(
    val currencyId: CurrencyId,
    val amount: GameNumber,
    val sourceId: ContentId? = null
)

/** One deterministic player-XP component inside an atomic reward bundle. */
data class ExperienceReward(
    val amount: GameNumber,
    val sourceId: ContentId? = null
)

/** One deterministic loot-table component inside an atomic reward bundle. */
data class LootTableReward(
    val lootTableId: ContentId,
    val sourceId: ContentId? = null
)

/**
 * Atomic reward bundle through Foundation 13.
 *
 * Echo/Chronicle components remain deferred to the foundations that own them.
 */
data class RewardBundle(
    val currencies: List<CurrencyReward> = emptyList(),
    val experience: List<ExperienceReward> = emptyList(),
    val lootTables: List<LootTableReward> = emptyList()
)

/** Result of atomically granting one reward bundle. */
data class RewardGrantResult(
    val state: GameState,
    val events: List<GameEvent>
)

/**
 * Grants reward bundles without duplicating economy, progression, or loot rules.
 *
 * Foundation 13 ordering is deterministic: currencies -> XP/feature unlocks -> loot.
 * Inventory overflow is an elastic safety net for unattended play, so reward delivery never
 * turns a soft capacity limit into a lost reward or a simulation exception.
 */
object RewardSystem {

    fun requiredLootSlots(
        bundle: RewardBundle,
        context: EngineContext
    ): Long = bundle.lootTables.fold(0L) { total, reward ->
        Math.addExact(
            total,
            LootSystem.requiredItemSlots(reward.lootTableId, context.contentRegistry)
        )
    }

    /**
     * Informational soft-cap check for projections and diagnostics.
     *
     * This is deliberately not a precondition for [grant]: generated items can be preserved
     * in elastic overflow after the visible overflow capacity is exhausted.
     */
    fun hasStorageFor(
        state: GameState,
        bundle: RewardBundle,
        context: EngineContext
    ): Boolean = InventoryCapacitySystem.totalStorageSlotsAvailable(
        state.run.inventory,
        context.balanceConfig
    ) >= requiredLootSlots(bundle, context)

    fun grant(
        state: GameState,
        bundle: RewardBundle,
        context: EngineContext,
        defeatedEnemy: EnemyState? = null
    ): RewardGrantResult {
        var transitioned = state
        var economy = state.run.economy
        val events = mutableListOf<GameEvent>()

        for (reward in bundle.currencies) {
            val multiplier = RewardMultiplierSystem.currencyMultiplier(
                state = transitioned,
                currencyId = reward.currencyId,
                contentRegistry = context.contentRegistry,
                sourceId = reward.sourceId,
                defeatedEnemy = defeatedEnemy
            )
            val grantedAmount = GameMath.applyRatio(
                value = reward.amount,
                ratio = multiplier
            )

            if (grantedAmount == GameNumber.ZERO) {
                continue
            }

            economy = TransactionSystem.grant(
                economy = economy,
                currencyId = reward.currencyId,
                amount = grantedAmount
            )
            events += CurrencyGranted(
                currencyId = reward.currencyId,
                amount = grantedAmount,
                sourceId = reward.sourceId
            )
        }

        transitioned = transitioned.copy(
            run = transitioned.run.copy(
                economy = economy
            )
        )

        for (reward in bundle.experience) {
            val progression = PlayerProgressionSystem.grantExperience(
                state = transitioned,
                amount = reward.amount,
                sourceId = reward.sourceId,
                contentRegistry = context.contentRegistry
            )
            transitioned = progression.state
            events += progression.events
        }

        for (lootReward in bundle.lootTables) {
            val loot = LootSystem.rollAndGrant(
                state = transitioned,
                lootTableId = lootReward.lootTableId,
                sourceDefinitionId = lootReward.sourceId,
                context = context
            )
            transitioned = loot.state
            events += loot.events
        }

        return RewardGrantResult(
            state = transitioned,
            events = events
        )
    }

    /** Defeated-enemy reward including Gold, XP, Adaptation compensation, and authored loot. */
    fun grantEnemyDefeatReward(
        state: GameState,
        enemyDefinition: EnemyDefinition,
        enemy: EnemyState,
        context: EngineContext
    ): RewardGrantResult {
        val encounter = state.run.world.currentEncounter
            ?.definitionId
            ?.let(context.contentRegistry::encounterOrNull)
        val stageMultiplier = encounter?.rewardMultiplier ?: com.idlerpg.game.core.number.Ratio.ONE
        val gold = GameMath.applyRatio(
            EnemyScalingSystem.scaledGoldReward(enemyDefinition, enemy.scalingTier),
            stageMultiplier
        )
        val experience = GameMath.applyRatio(
            EnemyScalingSystem.scaledExperienceReward(enemyDefinition, enemy.scalingTier),
            stageMultiplier
        )

        return grant(
            state = state,
            bundle = RewardBundle(
                currencies = listOf(
                    CurrencyReward(
                        currencyId = CurrencyId.GOLD,
                        amount = gold,
                        sourceId = enemyDefinition.id
                    )
                ),
                experience = listOf(
                    ExperienceReward(
                        amount = experience,
                        sourceId = enemyDefinition.id
                    )
                ),
                lootTables = run {
                    val encounterState = state.run.world.currentEncounter
                    val encounter = encounterState?.definitionId
                        ?.let(context.contentRegistry::encounterOrNull)
                    val bossFinalPhase = encounter?.type == com.idlerpg.game.domain.definition.world.EncounterType.BOSS &&
                        enemyDefinition.role == com.idlerpg.game.domain.definition.enemy.EnemyRole.BOSS &&
                        encounterState?.currentWave == encounter.waves
                    val lootId = if (encounter?.type == com.idlerpg.game.domain.definition.world.EncounterType.BOSS) {
                        if (bossFinalPhase) encounter.rewardLootTableId else null
                    } else encounter?.rewardLootTableId.orElse(enemyDefinition.lootTableId)
                    if (lootId == null) emptyList() else {
                        val region = encounter?.regionId?.let(state.run.world.regionProgressById::get)
                        val firstClearBonus = bossFinalPhase && encounter?.bossId?.let {
                            it !in (region?.clearedBossIds ?: emptySet())
                        } == true
                        List(if (firstClearBonus) 2 else 1) { LootTableReward(lootId, enemyDefinition.id) }
                    }
                }
            ),
            context = context,
            defeatedEnemy = enemy
        )
    }

    private fun ContentId?.orElse(fallback: ContentId?): ContentId? = this ?: fallback
}
