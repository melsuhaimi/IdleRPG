package com.idlerpg.game.ui.theme

import androidx.compose.ui.unit.dp

/**
 * Responsive layout anchors for the portrait game shell.
 *
 * Artwork is deliberately not assigned a layout height. Panels establish their own measured
 * bounds and decorative art uses matchParentSize() so a source image can never stretch the phone
 * viewport by itself.
 */
object GameDimensions {
    val ShellHorizontalPadding = 10.dp

    val HudMinHeight = 96.dp
    val BattleHeaderMinHeight = 72.dp
    val BottomDockMinHeight = 60.dp

    val LargePanelRadius = 16.dp
    val StageRadius = 18.dp
    val ActorNameplateRadius = 8.dp
    val CombatCardRadius = 8.dp

    /** Compatibility aliases for older callers; they are layout anchors, not artwork sizes. */
    @Deprecated("Use HudMinHeight")
    val HudBannerHeight = HudMinHeight

    @Deprecated("Use BattleHeaderMinHeight")
    val BattleHeaderHeight = BattleHeaderMinHeight

    @Deprecated("Use BottomDockMinHeight")
    val BottomDockArtworkHeight = BottomDockMinHeight
}
