package com.idlerpg.game.domain.system.inventory

import com.idlerpg.game.core.id.ContentId
import com.idlerpg.game.core.number.GameNumber
import com.idlerpg.game.core.number.Ratio
import com.idlerpg.game.domain.command.CommandRejectionCode
import com.idlerpg.game.domain.command.CommandRejectionReason
import com.idlerpg.game.domain.command.EnhanceItem
import com.idlerpg.game.domain.command.GameCommand
import com.idlerpg.game.domain.command.InventoryCommand
import com.idlerpg.game.domain.command.RefineItem
import com.idlerpg.game.domain.definition.CurrencyId
import com.idlerpg.game.domain.engine.CommandHandlingResult
import com.idlerpg.game.domain.engine.EngineContext
import com.idlerpg.game.domain.engine.GameCommandHandler
import com.idlerpg.game.domain.event.CurrencySpent
import com.idlerpg.game.domain.event.GameEvent
import com.idlerpg.game.domain.event.GearEnhancementAttempted
import com.idlerpg.game.domain.event.GearRefined
import com.idlerpg.game.domain.model.GameState
import com.idlerpg.game.domain.model.inventory.EnhancementLevel
import com.idlerpg.game.domain.model.inventory.ItemInstance
import com.idlerpg.game.domain.model.inventory.RolledAffix
import com.idlerpg.game.domain.system.economy.TransactionSystem

/** Read-only exact enhancement values shown before an attempt is committed. */
data class GearEnhancementPreview(
    val itemInstanceId: com.idlerpg.game.core.id.InstanceId,
    val currentLevel: Int,
    val targetLevel: Int?,
    val successChance: Ratio,
    val materialCost: GameNumber,
    val protectionGemCost: GameNumber,
    val failureLevelWithoutProtection: Int,
    val currentFailstack: Int = 0,
    val failureFailstack: Int = 0
)

/**
 * Owns the separate base-enhancement and rolled-line refinement systems.
 *
 * Every attempt spends its material before consuming RNG. A failed high-rank attempt can
 * downgrade one level, while protection prevents only that downgrade; no attempt destroys
 * the item or changes its rolled lines.
 */
object GearEnhancementSystem : GameCommandHandler {
    private val ENHANCEMENT_PURPOSE_ID = ContentId("system.gear.enhancement")
    private val REFINEMENT_PURPOSE_ID = ContentId("system.gear.refinement")

    override fun handle(
        state: GameState,
        command: GameCommand,
        context: EngineContext
    ): CommandHandlingResult = when (command) {
        is EnhanceItem -> enhance(state, command, context)
        is RefineItem -> refine(state, command, context)
        is InventoryCommand -> rejected(CommandRejectionCode.UNSUPPORTED)
        else -> rejected(CommandRejectionCode.UNSUPPORTED)
    }

    fun enhancementSuccessChance(
        level: Int,
        failstack: Int = 0
    ): Ratio {
        require(level in EnhancementLevel.INITIAL until EnhancementLevel.MAX) {
            "Enhancement level is not attemptable: $level"
        }
        val failstackBonus = EnhancementLevel.failstackBonusUnits(failstack)
        val baseUnits = when {
            level <= 4 -> 9_000L - level * 1_000L
            level <= 9 -> 4_500L - (level - 5L) * 500L
            level <= 13 -> 2_000L - (level - 10L) * 500L
            level == 14 -> 300L
            level == 15 -> 200L
            level == 16 -> 100L
            level == 17 -> 50L
            level == 18 -> 25L
            else -> 10L
        }
        return Ratio.ofUnits(
            (baseUnits + failstackBonus).coerceAtMost(Ratio.UNITS_PER_ONE)
        )
    }

    fun materialCostFor(level: Int): GameNumber {
        require(level in EnhancementLevel.INITIAL until EnhancementLevel.MAX)
        return GameNumber.of(5L + level.toLong())
    }

    fun preview(
        item: ItemInstance,
        useProtection: Boolean = false
    ): GearEnhancementPreview {
        val nextLevel = EnhancementLevel.next(item.enhancementLevel)
        val protectionCost = if (useProtection && EnhancementLevel.isHighRisk(item.enhancementLevel)) {
            GameNumber.ONE
        } else {
            GameNumber.ZERO
        }
        return GearEnhancementPreview(
            itemInstanceId = item.instanceId,
            currentLevel = item.enhancementLevel,
            targetLevel = nextLevel,
            successChance = if (nextLevel == null) {
                Ratio.ZERO
            } else {
                enhancementSuccessChance(
                    item.enhancementLevel,
                    item.enhancementFailstack
                )
            },
            materialCost = if (nextLevel == null) {
                GameNumber.ZERO
            } else {
                materialCostFor(item.enhancementLevel)
            },
            protectionGemCost = protectionCost,
            failureLevelWithoutProtection = if (nextLevel == null) {
                item.enhancementLevel
            } else {
                EnhancementLevel.downgradeAfterFailure(item.enhancementLevel)
            },
            currentFailstack = item.enhancementFailstack,
            failureFailstack = if (nextLevel == null) {
                item.enhancementFailstack
            } else {
                EnhancementLevel.nextFailstack(item.enhancementFailstack)
            }
        )
    }

    private fun enhance(
        state: GameState,
        command: EnhanceItem,
        context: EngineContext
    ): CommandHandlingResult {
        val item = ownedNormalItem(state, command.itemInstanceId)
            ?: return rejected(CommandRejectionCode.NOT_OWNED, command.itemInstanceId)
        val currentLevel = item.enhancementLevel
        if (EnhancementLevel.next(currentLevel) == null) {
            return rejected(CommandRejectionCode.INVALID_STATE, command.itemInstanceId)
        }
        if (command.useProtection && !EnhancementLevel.isHighRisk(currentLevel)) {
            return rejected(CommandRejectionCode.INVALID_ARGUMENT, command.itemInstanceId)
        }

        val materialCost = materialCostFor(currentLevel)
        val materialSpent = TransactionSystem.spend(
            state.run.economy,
            CurrencyId.ENHANCEMENT_MATERIAL,
            materialCost
        ) ?: return rejected(CommandRejectionCode.INSUFFICIENT_RESOURCE, command.itemInstanceId)
        val gemCost = if (command.useProtection) GameNumber.ONE else GameNumber.ZERO
        val economyAfterSpend = if (gemCost > GameNumber.ZERO) {
            TransactionSystem.spend(materialSpent, CurrencyId.GEMS, gemCost)
                ?: return rejected(CommandRejectionCode.INSUFFICIENT_RESOURCE, command.itemInstanceId)
        } else {
            materialSpent
        }

        val chance = enhancementSuccessChance(
            currentLevel,
            item.enhancementFailstack
        )
        val success = context.random.nextLong(Ratio.UNITS_PER_ONE) < chance.units
        val nextLevel = EnhancementLevel.next(currentLevel)
            ?: error("Validated enhancement level has no next level")
        val resultingLevel = if (success || command.useProtection) {
            if (success) nextLevel else currentLevel
        } else {
            EnhancementLevel.downgradeAfterFailure(currentLevel)
        }
        val resultingFailstack = if (success) {
            EnhancementLevel.INITIAL
        } else {
            EnhancementLevel.nextFailstack(item.enhancementFailstack)
        }
        val nextItem = item.copy(
            enhancementLevel = resultingLevel,
            enhancementFailstack = resultingFailstack
        )
        val nextInventory = state.run.inventory.copy(
            itemsById = (state.run.inventory.itemsById + (item.instanceId to nextItem)).toSortedMap()
        )
        val events = buildList<GameEvent> {
            add(CurrencySpent(
                currencyId = CurrencyId.ENHANCEMENT_MATERIAL,
                amount = materialCost,
                purposeId = ENHANCEMENT_PURPOSE_ID
            ))
            if (gemCost > GameNumber.ZERO) {
                add(CurrencySpent(
                    currencyId = CurrencyId.GEMS,
                    amount = gemCost,
                    purposeId = ENHANCEMENT_PURPOSE_ID
                ))
            }
            add(GearEnhancementAttempted(
                itemInstanceId = item.instanceId,
                previousEnhancementLevel = currentLevel,
                resultingEnhancementLevel = resultingLevel,
                success = success,
                protectionUsed = command.useProtection,
                materialCost = materialCost,
                gemCost = gemCost,
                successChance = chance,
                previousFailstack = item.enhancementFailstack,
                resultingFailstack = resultingFailstack
            ))
        }
        return CommandHandlingResult.Accepted(
            state.copy(run = state.run.copy(
                inventory = nextInventory,
                economy = state.run.economy.copy(wallet = economyAfterSpend.wallet)
            )),
            events
        )
    }

    private fun refine(
        state: GameState,
        command: RefineItem,
        context: EngineContext
    ): CommandHandlingResult {
        val item = ownedNormalItem(state, command.itemInstanceId)
            ?: return rejected(CommandRejectionCode.NOT_OWNED, command.itemInstanceId)
        val target = (listOfNotNull(item.mainStat) + item.affixes)
            .firstOrNull { it.affixId == command.affixId }
            ?: return rejected(CommandRejectionCode.INVALID_ARGUMENT, command.itemInstanceId)
        val definition = context.contentRegistry.affixOrNull(command.affixId)
            ?: return rejected(CommandRejectionCode.UNKNOWN_CONTENT, command.affixId)
        val rangeSize = Math.addExact(
            Math.subtractExact(definition.maximumRollValue, definition.minimumRollValue),
            1L
        )
        val materialCost = GameNumber.ONE
        val economyAfterSpend = TransactionSystem.spend(
            state.run.economy,
            CurrencyId.REFINEMENT_MATERIAL,
            materialCost
        ) ?: return rejected(CommandRejectionCode.INSUFFICIENT_RESOURCE, command.itemInstanceId)
        val resultingValue = Math.addExact(
            definition.minimumRollValue,
            context.random.nextLong(rangeSize)
        )
        val updatedRoll = target.copy(value = resultingValue)
        val nextItem = replaceRoll(item, command.affixId, updatedRoll)
        val nextInventory = state.run.inventory.copy(
            itemsById = (state.run.inventory.itemsById + (item.instanceId to nextItem)).toSortedMap()
        )
        return CommandHandlingResult.Accepted(
            state.copy(run = state.run.copy(
                inventory = nextInventory,
                economy = state.run.economy.copy(wallet = economyAfterSpend.wallet)
            )),
            listOf(
                CurrencySpent(
                    currencyId = CurrencyId.REFINEMENT_MATERIAL,
                    amount = materialCost,
                    purposeId = REFINEMENT_PURPOSE_ID
                ),
                GearRefined(
                    itemInstanceId = item.instanceId,
                    affixId = command.affixId,
                    previousValue = target.value,
                    resultingValue = resultingValue,
                    isMainStat = item.mainStat?.affixId == command.affixId
                )
            )
        )
    }

    private fun ownedNormalItem(
        state: GameState,
        itemInstanceId: com.idlerpg.game.core.id.InstanceId
    ): ItemInstance? =
        state.run.inventory.itemsById[itemInstanceId]

    private fun replaceRoll(
        item: ItemInstance,
        affixId: ContentId,
        updated: RolledAffix
    ): ItemInstance =
        if (item.mainStat?.affixId == affixId) {
            item.copy(mainStat = updated)
        } else {
            item.copy(
                affixes = item.affixes.map { roll ->
                    if (roll.affixId == affixId) updated else roll
                }
            )
        }

    private fun rejected(
        code: CommandRejectionCode,
        subjectInstanceId: com.idlerpg.game.core.id.InstanceId? = null
    ): CommandHandlingResult.Rejected =
        CommandHandlingResult.Rejected(
            CommandRejectionReason(
                code = code,
                subjectInstanceId = subjectInstanceId
            )
        )

    private fun rejected(
        code: CommandRejectionCode,
        subjectContentId: ContentId
    ): CommandHandlingResult.Rejected =
        CommandHandlingResult.Rejected(
            CommandRejectionReason(
                code = code,
                subjectContentId = subjectContentId
            )
        )
}
