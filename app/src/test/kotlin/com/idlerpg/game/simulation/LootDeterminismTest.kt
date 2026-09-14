package com.idlerpg.game.simulation

import com.idlerpg.game.core.random.SeededGameRandom
import com.idlerpg.game.data.content.DefaultGameContent
import com.idlerpg.game.domain.system.loot.LootTableSystem

/** Same controlled seed produces the same optional-drop and item-quality stream. */
object LootDeterminismTest {
    fun run() {
        val registry = SimulationTestSupport.factory().contentRegistry
        val table = registry.lootTable(DefaultGameContent.TRAINING_SLIME_LOOT_TABLE_ID)
        fun stream(seed: Long) = buildList {
            val random = SeededGameRandom(seed)
            repeat(2_000) { addAll(LootTableSystem.roll(table, registry, random)) }
        }
        val first = stream(991L)
        val second = stream(991L)
        check(first == second)
        check(first.isNotEmpty())
        check(first.size < 300)
    }
}
