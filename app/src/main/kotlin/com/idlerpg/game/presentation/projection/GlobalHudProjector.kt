package com.idlerpg.game.presentation.projection

import com.idlerpg.game.core.number.GameNumber
import com.idlerpg.game.domain.definition.CurrencyId
import com.idlerpg.game.domain.model.GameState
import com.idlerpg.game.presentation.content.PresentationContentRegistry
import com.idlerpg.game.presentation.format.GameNumberFormatter
import com.idlerpg.game.presentation.model.GlobalHudSaveStatus
import com.idlerpg.game.presentation.model.GlobalHudUiState
import com.idlerpg.game.presentation.query.GameReadQueries
import com.idlerpg.game.presentation.runtime.RuntimeSaveStatus
import java.math.BigInteger

/** Pure canonical-state -> global HUD projection. */
class GlobalHudProjector(
    private val presentationContentRegistry: PresentationContentRegistry,
    private val readQueries: GameReadQueries
) {
    fun project(
        state: GameState,
        saveStatus: RuntimeSaveStatus
    ): GlobalHudUiState {
        val playerLevel = state.run.progression.playerLevel
        val xpToNext = readQueries.playerExperienceToNextLevel(state)
        val inventory = state.run.inventory
        val gold = state.run.economy.wallet.amountsByCurrencyId[CurrencyId.GOLD]
            ?: GameNumber.ZERO
        val regionTitleKey = state.run.world.activeRegionId
            ?.let(presentationContentRegistry::entryOrNull)
            ?.titleStringKey

        return GlobalHudUiState(
            playerLevel = playerLevel.level,
            currentExperienceDisplay = GameNumberFormatter.compact(playerLevel.currentExperience),
            experienceToNextDisplay = GameNumberFormatter.compact(xpToNext),
            experienceProgressUnits = experienceProgressUnits(
                current = playerLevel.currentExperience,
                required = xpToNext
            ),
            goldDisplay = GameNumberFormatter.compact(gold),
            echoDisplay = GameNumberFormatter.compact(state.meta.echoes.available),
            currentRegionTitleKey = regionTitleKey,
            saveStatus = when (saveStatus) {
                RuntimeSaveStatus.IDLE -> GlobalHudSaveStatus.IDLE
                RuntimeSaveStatus.SAVING -> GlobalHudSaveStatus.SAVING
                RuntimeSaveStatus.SAVED -> GlobalHudSaveStatus.SAVED
                RuntimeSaveStatus.ERROR -> GlobalHudSaveStatus.ERROR
            },
            inventoryUsed = inventory.itemsById.size,
            inventoryCapacity = readQueries.effectiveInventoryCapacity(state),
            overflowUsed = inventory.overflowItemsById.size,
            overflowCapacity = readQueries.inventoryOverflowCapacity(),
            inventoryProgressionBlocked = readQueries.inventoryProgressionBlocked(state)
        )
    }

    private fun experienceProgressUnits(
        current: GameNumber,
        required: GameNumber
    ): Int {
        if (required == GameNumber.ZERO) {
            return 10_000
        }
        val numerator = current.toBigInteger().multiply(BigInteger.valueOf(10_000L))
        val units = numerator.divide(required.toBigInteger())
            .coerceIn(BigInteger.ZERO, BigInteger.valueOf(10_000L))
        return units.toInt()
    }
}
