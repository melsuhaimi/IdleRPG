package com.idlerpg.game.simulation

import com.idlerpg.game.core.random.SeededGameRandom
import com.idlerpg.game.data.content.DefaultGameContent
import com.idlerpg.game.domain.model.rebirth.RebirthPointPool
import com.idlerpg.game.domain.model.rebirth.RebirthState
import com.idlerpg.game.domain.model.rebirth.RebirthStat
import com.idlerpg.game.domain.system.rebirth.RebirthStatSystem
import com.idlerpg.game.domain.system.loot.LootTableSystem

/** Same controlled seed produces the same optional-drop and item-quality stream. */
object LootDeterminismTest {
    fun run() {
        val registry = SimulationTestSupport.factory().contentRegistry
        val table = registry.lootTable(DefaultGameContent.TRAINING_SLIME_LOOT_TABLE_ID)
        fun stream(seed: Long, legendaryBonusWeight: Long = 0L) = buildList {
            val random = SeededGameRandom(seed)
            repeat(2_000) {
                addAll(
                    LootTableSystem.roll(
                        table,
                        registry,
                        random,
                        legendaryBonusWeight = legendaryBonusWeight
                    )
                )
            }
        }
        val first = stream(991L)
        val second = stream(991L)
        check(first == second)
        check(first.isNotEmpty())
        check(first.size < 300)

        val rebirth = RebirthState(
            normalPointsEarned = 25L,
            normalAllocations = mapOf(RebirthStat.LEGENDARY_FIND to 25L)
        )
        check(
            RebirthStatSystem.legendaryLootBonusWeight(rebirth) == 25L
        )
        val capped = RebirthState(
            normalPointsEarned = 900L,
            normalAllocations = mapOf(RebirthStat.LEGENDARY_FIND to 900L)
        )
        check(
            RebirthStatSystem.legendaryLootBonusWeight(capped) ==
                RebirthStatSystem.MAX_LEGENDARY_FIND_WEIGHT
        )
        val boostedFirst = stream(991L, 25L)
        check(boostedFirst == stream(991L, 25L))
    }
}
