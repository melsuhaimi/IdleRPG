package com.idlerpg.game.domain.system.stats

import com.idlerpg.game.core.number.GameNumber
import com.idlerpg.game.core.number.GameRate
import com.idlerpg.game.core.number.Ratio
import com.idlerpg.game.core.time.GameDuration
import com.idlerpg.game.data.content.ContentRegistry
import com.idlerpg.game.domain.definition.combat.BasicAttackDefinition
import com.idlerpg.game.domain.model.GameState
import com.idlerpg.game.domain.system.combat.CombatMath
import java.math.BigDecimal

/** Builds authoritative derived combat readouts from canonical state and definitions. */
object DerivedStatSystem {

    fun attackPower(state: GameState, contentRegistry: ContentRegistry): GameNumber =
        ModifierSystem.attackPower(
            state = state,
            base = PlayerScalingSystem.baseStatsForLevel(
                state.run.player.baseStats,
                state.run.progression.playerLevel.level
            ).attackPower,
            contentRegistry = contentRegistry
        )

    fun armor(state: GameState, contentRegistry: ContentRegistry): GameNumber =
        ModifierSystem.armor(
            state = state,
            base = PlayerScalingSystem.baseStatsForLevel(
                state.run.player.baseStats,
                state.run.progression.playerLevel.level
            ).armor,
            contentRegistry = contentRegistry
        )

    fun basicAttackInterval(
        state: GameState,
        definition: BasicAttackDefinition,
        contentRegistry: ContentRegistry? = null
    ): GameDuration = actionInterval(state, definition.interval, contentRegistry)

    fun actionSpeed(state: GameState, contentRegistry: ContentRegistry): Ratio =
        ModifierSystem.actionSpeed(
            state,
            PlayerScalingSystem.baseStatsForLevel(
                state.run.player.baseStats,
                state.run.progression.playerLevel.level
            ).actionSpeed,
            contentRegistry
        )

    fun actionInterval(
        state: GameState,
        base: GameDuration,
        contentRegistry: ContentRegistry? = null
    ): GameDuration {
        val actionSpeed = contentRegistry?.let { DerivedStatSystem.actionSpeed(state, it) }
            ?: PlayerScalingSystem.baseStatsForLevel(
                state.run.player.baseStats,
                state.run.progression.playerLevel.level
            ).actionSpeed
        var interval = com.idlerpg.game.domain.system.combat.CombatMath.actionInterval(
            base = base,
            actionSpeed = actionSpeed
        )
        val statuses = state.run.combat.playerCombatant?.statusEffects.orEmpty()
            .sortedBy { it.instanceId }
        for (status in statuses) {
            val definition = contentRegistry?.statusOrNull(status.definitionId) ?: continue
            for (modifier in definition.modifiers) {
                if (modifier is com.idlerpg.game.domain.definition.combat.StatusModifierDefinition.ActionIntervalMultiplier) {
                    repeat(status.stackCount) {
                        val millis = com.idlerpg.game.core.number.GameMath.applyRatio(
                            GameNumber.of(interval.millis),
                            modifier.multiplierPerStack
                        ).toBigInteger()
                            .min(java.math.BigInteger.valueOf(Long.MAX_VALUE))
                            .longValueExact()
                        interval = GameDuration.ofMillis(maxOf(1L, millis))
                    }
                }
            }
        }
        return interval
    }

    fun maximumHealth(state: GameState, contentRegistry: ContentRegistry): GameNumber =
        ModifierSystem.maximumHealth(
            state,
            PlayerScalingSystem.baseStatsForLevel(
                state.run.player.baseStats,
                state.run.progression.playerLevel.level
            ).maxHealth,
            contentRegistry
        )
    fun criticalChance(state: GameState, contentRegistry: ContentRegistry) =
        ModifierSystem.criticalChance(
            state,
            PlayerScalingSystem.baseStatsForLevel(
                state.run.player.baseStats,
                state.run.progression.playerLevel.level
            ).criticalChance,
            contentRegistry
        )
            .let { Ratio.ofUnits(it.units.coerceAtMost(Ratio.UNITS_PER_ONE)) }
    fun criticalMultiplier(state: GameState, contentRegistry: ContentRegistry) =
        ModifierSystem.criticalMultiplier(
            state,
            PlayerScalingSystem.baseStatsForLevel(
                state.run.player.baseStats,
                state.run.progression.playerLevel.level
            ).criticalMultiplier,
            contentRegistry
        )
    fun effectPower(state: GameState, contentRegistry: ContentRegistry) =
        ModifierSystem.effectPower(
            state,
            PlayerScalingSystem.baseStatsForLevel(
                state.run.player.baseStats,
                state.run.progression.playerLevel.level
            ).effectPower,
            contentRegistry
        )
    fun healingPower(state: GameState, contentRegistry: ContentRegistry) =
        ModifierSystem.healingPower(
            state,
            PlayerScalingSystem.baseStatsForLevel(
                state.run.player.baseStats,
                state.run.progression.playerLevel.level
            ).healingPower,
            contentRegistry
        )

    /** Armor's readable percentage for stat panels and tooltips. */
    fun damageReduction(state: GameState, contentRegistry: ContentRegistry): Ratio {
        val armor = armor(state, contentRegistry)
        val numerator = armor.toBigInteger()
            .multiply(java.math.BigInteger.valueOf(Ratio.UNITS_PER_ONE))
        val denominator = CombatMath.DEFAULT_ARMOR_SCALE.toBigInteger().add(armor.toBigInteger())
        if (denominator == java.math.BigInteger.ZERO) return Ratio.ZERO
        return Ratio.ofUnits(
            numerator.divide(denominator)
                .coerceAtMost(java.math.BigInteger.valueOf(Ratio.UNITS_PER_ONE))
                .longValueExact()
        )
    }

    /** Derived DPS; never stored in canonical GameState. */
    fun basicAttackDps(
        state: GameState,
        definition: BasicAttackDefinition,
        contentRegistry: ContentRegistry
    ): GameRate {
        val interval = basicAttackInterval(state, definition, contentRegistry)
        require(interval > GameDuration.ZERO) {
            "Basic attack interval must be positive to derive DPS"
        }
        val attack = attackPower(state, contentRegistry).toBigInteger()
        val numerator = BigDecimal(attack).multiply(BigDecimal.valueOf(1_000L))
        return GameRate.fromBigDecimal(
            numerator.divide(
                BigDecimal.valueOf(interval.millis),
                GameRate.SCALE,
                java.math.RoundingMode.DOWN
            )
        )
    }
}
