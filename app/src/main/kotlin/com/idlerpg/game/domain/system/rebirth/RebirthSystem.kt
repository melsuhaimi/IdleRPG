package com.idlerpg.game.domain.system.rebirth

import com.idlerpg.game.core.id.ContentId
import com.idlerpg.game.core.number.GameMath
import com.idlerpg.game.core.number.GameNumber
import com.idlerpg.game.domain.command.AllocateRebirthPoints
import com.idlerpg.game.domain.command.CommandRejectionCode
import com.idlerpg.game.domain.command.CommandRejectionReason
import com.idlerpg.game.domain.command.GameCommand
import com.idlerpg.game.domain.command.PerformRebirth
import com.idlerpg.game.domain.command.RebirthCommand
import com.idlerpg.game.domain.command.ResetRebirthAllocations
import com.idlerpg.game.domain.definition.CurrencyId
import com.idlerpg.game.domain.engine.CommandHandlingResult
import com.idlerpg.game.domain.engine.EngineContext
import com.idlerpg.game.domain.engine.GameCommandHandler
import com.idlerpg.game.domain.event.CurrencySpent
import com.idlerpg.game.domain.event.GameEvent
import com.idlerpg.game.domain.event.RebirthAllocationsReset
import com.idlerpg.game.domain.event.RebirthPerformed
import com.idlerpg.game.domain.event.RebirthPointsAllocated
import com.idlerpg.game.domain.model.GameState
import com.idlerpg.game.domain.model.combat.CombatState
import com.idlerpg.game.domain.model.economy.UpgradeProgressState
import com.idlerpg.game.domain.model.progression.ProgressionState
import com.idlerpg.game.domain.model.quest.QuestState
import com.idlerpg.game.domain.model.rebirth.RebirthPointPool
import com.idlerpg.game.domain.model.rebirth.RebirthState
import com.idlerpg.game.domain.model.rebirth.RebirthStat
import com.idlerpg.game.domain.model.world.WorldState
import com.idlerpg.game.domain.system.economy.TransactionSystem
import com.idlerpg.game.domain.system.stats.PlayerScalingSystem

/** Read-only values presented before a player chooses to Rebirth. */
data class RebirthPreview(
    val currentLevel: Long,
    val minimumLevel: Long,
    val eligible: Boolean,
    val nextRebirthNumber: Long,
    val goldCost: GameNumber,
    val normalPointsGranted: Long,
    val legacyPointsGranted: Long,
    val goldAvailable: GameNumber
)

/**
 * Owns the separate Rebirth lifecycle.
 *
 * Chronicle remains the destructive whole-run reset. Rebirth deliberately preserves the
 * player's wallet, inventory/equipment, resonances, doctrine, adaptation, and run statistics.
 */
object RebirthSystem : GameCommandHandler {
    const val MINIMUM_REBIRTH_LEVEL: Long = 1_000L
    const val FIRST_REBIRTH_GOLD_COST: Long = 100_000_000L
    const val NORMAL_POINTS_FIRST_REBIRTH: Long = 100L
    const val NORMAL_POINTS_PER_REBIRTH: Long = 25L
    const val GEM_RESPEC_COST: Long = 100L

    private val REBIRTH_COST_GROWTH = com.idlerpg.game.core.number.Ratio.ofUnits(12_500L)
    private val REBIRTH_PURPOSE_ID = ContentId("system.rebirth")
    private val RESPEC_PURPOSE_ID = ContentId("system.rebirth.respec")

    override fun handle(
        state: GameState,
        command: GameCommand,
        context: EngineContext
    ): CommandHandlingResult = when (command) {
        is PerformRebirth -> perform(state)
        is AllocateRebirthPoints -> allocate(state, command)
        is ResetRebirthAllocations -> resetAllocations(state, command)
        is RebirthCommand -> rejected(CommandRejectionCode.UNSUPPORTED)
        else -> rejected(CommandRejectionCode.UNSUPPORTED)
    }

    fun preview(state: GameState): RebirthPreview {
        val nextNumber = Math.addExact(state.meta.rebirth.completedRebirths, 1L)
        val normal = normalPointsForRebirth(nextNumber)
        return RebirthPreview(
            currentLevel = state.run.progression.playerLevel.level,
            minimumLevel = MINIMUM_REBIRTH_LEVEL,
            eligible = state.run.progression.playerLevel.level >= MINIMUM_REBIRTH_LEVEL &&
                TransactionSystem.canAfford(
                    state.run.economy,
                    CurrencyId.GOLD,
                    goldCostForRebirth(nextNumber)
                ),
            nextRebirthNumber = nextNumber,
            goldCost = goldCostForRebirth(nextNumber),
            normalPointsGranted = normal,
            legacyPointsGranted = legacyPointsForRebirth(nextNumber),
            goldAvailable = TransactionSystem.balance(state.run.economy, CurrencyId.GOLD)
        )
    }

    fun goldCostForRebirth(rebirthNumber: Long): GameNumber {
        require(rebirthNumber > 0L) { "rebirthNumber must be positive: $rebirthNumber" }
        return GameMath.compoundCeil(
            value = GameNumber.of(FIRST_REBIRTH_GOLD_COST),
            multiplier = REBIRTH_COST_GROWTH,
            steps = rebirthNumber - 1L
        )
    }

    fun normalPointsForRebirth(rebirthNumber: Long): Long {
        require(rebirthNumber > 0L) { "rebirthNumber must be positive: $rebirthNumber" }
        return Math.addExact(
            NORMAL_POINTS_FIRST_REBIRTH,
            Math.multiplyExact(NORMAL_POINTS_PER_REBIRTH, rebirthNumber - 1L)
        )
    }

    fun legacyPointsForRebirth(rebirthNumber: Long): Long =
        normalPointsForRebirth(rebirthNumber) / 5L

    private fun perform(state: GameState): CommandHandlingResult {
        val level = state.run.progression.playerLevel.level
        if (state.run.combat.status != com.idlerpg.game.domain.model.combat.CombatStatus.IDLE) {
            return rejected(CommandRejectionCode.INVALID_STATE)
        }
        if (level < MINIMUM_REBIRTH_LEVEL) {
            return rejected(CommandRejectionCode.NOT_READY)
        }

        val nextNumber = Math.addExact(state.meta.rebirth.completedRebirths, 1L)
        val cost = goldCostForRebirth(nextNumber)
        val economyAfterSpend = TransactionSystem.spend(
            state.run.economy,
            CurrencyId.GOLD,
            cost
        ) ?: return rejected(CommandRejectionCode.INSUFFICIENT_RESOURCE)

        val normalPoints = normalPointsForRebirth(nextNumber)
        val legacyPoints = legacyPointsForRebirth(nextNumber)
        val nextRebirth = state.meta.rebirth.copy(
            completedRebirths = nextNumber,
            normalPointsEarned = Math.addExact(
                state.meta.rebirth.normalPointsEarned,
                normalPoints
            ),
            legacyPointsEarned = Math.addExact(
                state.meta.rebirth.legacyPointsEarned,
                legacyPoints
            )
        )
        val resetProgression = ProgressionState()
        val nextBaseStats = RebirthStatSystem.apply(
            PlayerScalingSystem.baseStatsForLevel(
                state.run.player.baseStats,
                resetProgression.playerLevel.level
            ),
            nextRebirth
        )
        val resetRun = state.run.copy(
            player = state.run.player.copy(
                currentHealth = nextBaseStats.maxHealth,
                equippedSkillIds = emptyList(),
                selectedSkillEvolutionBySkillId = emptyMap()
            ),
            combat = CombatState(),
            world = WorldState(),
            economy = state.run.economy.copy(
                wallet = economyAfterSpend.wallet,
                upgrades = UpgradeProgressState()
            ),
            progression = resetProgression,
            quests = QuestState()
        )
        val transitioned = state.copy(
            run = resetRun,
            meta = state.meta.copy(rebirth = nextRebirth)
        )
        return CommandHandlingResult.Accepted(
            transitioned,
            listOf(
                CurrencySpent(
                    currencyId = CurrencyId.GOLD,
                    amount = cost,
                    purposeId = REBIRTH_PURPOSE_ID
                ),
                RebirthPerformed(
                    rebirthNumber = nextNumber,
                    previousLevel = level,
                    goldCost = cost,
                    normalPointsGranted = normalPoints,
                    legacyPointsGranted = legacyPoints
                )
            )
        )
    }

    private fun allocate(
        state: GameState,
        command: AllocateRebirthPoints
    ): CommandHandlingResult {
        val rebirth = state.meta.rebirth
        if (rebirth.completedRebirths == 0L) {
            return rejected(CommandRejectionCode.NOT_READY)
        }
        if (command.amount > rebirth.unspentPoints(command.pool)) {
            return rejected(CommandRejectionCode.INSUFFICIENT_RESOURCE)
        }
        val next = rebirth.allocate(command.pool, command.stat, command.amount)
        return CommandHandlingResult.Accepted(
            state.copy(meta = state.meta.copy(rebirth = next)),
            listOf(
                RebirthPointsAllocated(
                    pool = command.pool,
                    stat = command.stat,
                    amount = command.amount,
                    remainingPoints = next.unspentPoints(command.pool)
                )
            )
        )
    }

    private fun resetAllocations(
        state: GameState,
        command: ResetRebirthAllocations
    ): CommandHandlingResult {
        val rebirth = state.meta.rebirth
        if (rebirth.allocatedPoints(command.pool) == 0L) {
            return rejected(CommandRejectionCode.INVALID_STATE)
        }
        val cost = GameNumber.of(GEM_RESPEC_COST)
        val economyAfterSpend = TransactionSystem.spend(
            state.run.economy,
            CurrencyId.GEMS,
            cost
        ) ?: return rejected(CommandRejectionCode.INSUFFICIENT_RESOURCE)
        return CommandHandlingResult.Accepted(
            state.copy(
                run = state.run.copy(
                    economy = state.run.economy.copy(wallet = economyAfterSpend.wallet)
                ),
                meta = state.meta.copy(rebirth = rebirth.resetAllocations(command.pool))
            ),
            listOf(
                CurrencySpent(
                    currencyId = CurrencyId.GEMS,
                    amount = cost,
                    purposeId = RESPEC_PURPOSE_ID
                ),
                RebirthAllocationsReset(command.pool, cost)
            )
        )
    }

    private fun rejected(code: CommandRejectionCode): CommandHandlingResult.Rejected =
        CommandHandlingResult.Rejected(CommandRejectionReason(code = code))
}
