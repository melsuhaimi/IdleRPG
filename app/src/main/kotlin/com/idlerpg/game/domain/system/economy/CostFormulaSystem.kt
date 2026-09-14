package com.idlerpg.game.domain.system.economy

import com.idlerpg.game.core.number.GameNumber
import com.idlerpg.game.domain.definition.economy.CostFormulaDefinition
import java.math.BigInteger

/** Deterministic evaluator for authored upgrade-cost curves. */
object CostFormulaSystem {

    fun costAtLevel(
        definition: CostFormulaDefinition,
        level: Long
    ): GameNumber {
        require(level >= 0L) {
            "Upgrade level cannot be negative: $level"
        }

        return when (definition) {
            is CostFormulaDefinition.Linear ->
                definition.baseCost +
                    (definition.incrementPerLevel * level)
        }
    }

    /**
     * Exact atomic cost for buying [quantity] consecutive levels beginning at [startLevel].
     *
     * The linear implementation uses a BigInteger arithmetic-series calculation rather
     * than looping once per requested level, so very large valid quantities stay exact.
     */
    fun totalCost(
        definition: CostFormulaDefinition,
        startLevel: Long,
        quantity: Long
    ): GameNumber {
        require(startLevel >= 0L) {
            "Upgrade startLevel cannot be negative: $startLevel"
        }
        require(quantity > 0L) {
            "Upgrade quantity must be positive: $quantity"
        }

        return when (definition) {
            is CostFormulaDefinition.Linear -> {
                val n = BigInteger.valueOf(quantity)
                val start = BigInteger.valueOf(startLevel)
                val one = BigInteger.ONE
                val two = BigInteger.valueOf(2L)

                val levelSum =
                    n.multiply(start)
                        .add(
                            n.multiply(n.subtract(one))
                                .divide(two)
                        )

                val baseTotal =
                    definition.baseCost
                        .toBigInteger()
                        .multiply(n)

                val incrementTotal =
                    definition.incrementPerLevel
                        .toBigInteger()
                        .multiply(levelSum)

                GameNumber.fromBigInteger(
                    baseTotal.add(incrementTotal)
                )
            }
        }
    }
}
