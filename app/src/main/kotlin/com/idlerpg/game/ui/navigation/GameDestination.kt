package com.idlerpg.game.ui.navigation

import androidx.annotation.StringRes
import com.idlerpg.game.R
import com.idlerpg.game.presentation.content.PresentationAssetKey

enum class GameDestination(
    @StringRes val labelResId: Int,
    val iconAssetKey: PresentationAssetKey
) {
    BATTLE(R.string.nav_battle, PresentationAssetKey.NAV_BATTLE),
    WORLD(R.string.nav_world, PresentationAssetKey.NAV_WORLD),
    DOCTRINE(R.string.nav_doctrine, PresentationAssetKey.NAV_DOCTRINE),
    GEAR(R.string.nav_gear, PresentationAssetKey.NAV_GEAR),
    PROGRESS(R.string.nav_progress, PresentationAssetKey.NAV_PROGRESS)
}

enum class ProgressDestination(
    @StringRes val labelResId: Int
) {
    OVERVIEW(R.string.progress_overview),
    CORE_GROWTH(R.string.progress_core_growth),
    MASTERY(R.string.progress_mastery),
    QUESTS(R.string.progress_quests),
    ACHIEVEMENTS(R.string.progress_achievements),
    DISCOVERIES(R.string.progress_discoveries),
    ECHO(R.string.progress_echo),
    CHRONICLE(R.string.progress_chronicle)
}
