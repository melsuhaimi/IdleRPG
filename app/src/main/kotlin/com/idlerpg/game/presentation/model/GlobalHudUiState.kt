package com.idlerpg.game.presentation.model

import com.idlerpg.game.presentation.content.PresentationStringKey

enum class GlobalHudSaveStatus {
    IDLE,
    SAVING,
    SAVED,
    ERROR
}

/** Compact canonical facts that remain visible across primary destinations. */
data class GlobalHudUiState(
    val playerLevel: Long,
    val currentExperienceDisplay: String,
    val experienceToNextDisplay: String,
    val experienceProgressUnits: Int,
    val goldDisplay: String,
    val echoDisplay: String,
    val currentRegionTitleKey: PresentationStringKey?,
    val saveStatus: GlobalHudSaveStatus,
    val inventoryUsed: Int,
    val inventoryCapacity: Long,
    val overflowUsed: Int,
    val overflowCapacity: Long,
    val inventoryProgressionBlocked: Boolean
) {
    init {
        require(experienceProgressUnits in 0..10_000)
    }
}
