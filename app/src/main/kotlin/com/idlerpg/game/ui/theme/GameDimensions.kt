package com.idlerpg.game.ui.theme

import androidx.compose.ui.unit.dp

/**
 * Responsive layout anchors for the portrait game shell.
 *
 * Artwork is deliberately not assigned a layout height. Panels establish their own measured
 * bounds and decorative art uses matchParentSize() so a source image can never stretch the
 * phone viewport by itself.
 */
object GameDimensions {
    val ShellHorizontalPadding = 10.dp

    val HudMinHeight = 112.dp
    val BattleHeaderMinHeight = 84.dp
    val BottomDockMinHeight = 68.dp

    val LargePanelRadius = 20.dp
    val StageRadius = 24.dp
    val ActorNameplateRadius = 10.dp
    val CombatCardRadius = 10.dp

    /** Compatibility aliases for older callers; they are layout anchors, not artwork sizes. */
    @Deprecated("Use HudMinHeight")
    val HudBannerHeight = HudMinHeight

    @Deprecated("Use BattleHeaderMinHeight")
    val BattleHeaderHeight = BattleHeaderMinHeight

    @Deprecated("Use BottomDockMinHeight")
    val BottomDockArtworkHeight = BottomDockMinHeight
}
