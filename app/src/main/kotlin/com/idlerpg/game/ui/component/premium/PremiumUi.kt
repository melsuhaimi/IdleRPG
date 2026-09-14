package com.idlerpg.game.ui.component.premium

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.idlerpg.game.R
import com.idlerpg.game.ui.theme.ObsidianBackground
import com.idlerpg.game.ui.theme.GameDimensions
import com.idlerpg.game.ui.theme.LocalReducedMotion
import com.idlerpg.game.ui.theme.MotionTokens

/** Decorative presentation primitives only. They never own gameplay state. */
@Composable
fun PremiumPanel(
    backgroundResId: Int,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(horizontal = 20.dp, vertical = 18.dp),
    verticalSpacing: Dp = 10.dp,
    accent: Color? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val panelAccent = accent ?: MaterialTheme.colorScheme.primary
    GameCard(
        modifier = modifier,
        shape = RoundedCornerShape(GameDimensions.LargePanelRadius),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            panelAccent.copy(alpha = if (accent == null) 0.26f else 0.48f)
        ),
        accent = accent
    ) {
        Box {
            Image(
                painter = painterResource(backgroundResId),
                contentDescription = null,
                modifier = Modifier
                    // Decoration must follow content size, never expand a Scaffold top bar.
                    .matchParentSize()
                    .align(Alignment.Center)
                    .alpha(0.19f),
                contentScale = ContentScale.Crop
            )
            Image(
                painter = painterResource(R.drawable.ui_panel_frame_generated),
                contentDescription = null,
                modifier = Modifier
                    .matchParentSize()
                    .alpha(0.34f),
                contentScale = ContentScale.FillBounds
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.dp)
                    .align(Alignment.TopCenter)
                    .alpha(0.76f)
                    .background(
                        androidx.compose.ui.graphics.Brush.horizontalGradient(
                            listOf(
                                MaterialTheme.colorScheme.primary.copy(alpha = 0f),
                                panelAccent.copy(alpha = 0.76f),
                                MaterialTheme.colorScheme.tertiary.copy(alpha = 0.48f),
                                MaterialTheme.colorScheme.primary.copy(alpha = 0f)
                            )
                        )
                    )
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(32.dp)
                    .align(Alignment.BottomCenter)
                    .background(
                        Brush.verticalGradient(
                            listOf(Color.Transparent, ObsidianBackground.copy(alpha = 0.34f))
                        )
                    )
            )
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(contentPadding),
                verticalArrangement = Arrangement.spacedBy(verticalSpacing),
                content = content
            )
        }
    }
}

@Composable
fun PremiumImageProgressBar(
    progressUnits: Int,
    trackResId: Int,
    fillResId: Int,
    contentDescriptionText: String,
    modifier: Modifier = Modifier,
    height: Dp = 34.dp
) {
    val targetFraction = progressUnits.coerceIn(0, 10_000) / 10_000f
    val fraction by animateFloatAsState(
        targetValue = targetFraction,
        animationSpec = if (LocalReducedMotion.current) snap() else tween(MotionTokens.CARD_MILLIS),
        label = "premium_progress"
    )
    Box(
        modifier = modifier
            .height(height)
            .semantics(mergeDescendants = true) {
                contentDescription = contentDescriptionText
                progressBarRangeInfo = ProgressBarRangeInfo(targetFraction, 0f..1f)
            }
    ) {
        Image(
            painter = painterResource(trackResId),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.FillBounds
        )
        if (fraction > 0f) {
            Image(
                painter = painterResource(fillResId),
                contentDescription = null,
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(
                        start = height * 0.95f,
                        end = height * 0.42f
                    )
                    .fillMaxWidth(fraction)
                    .height(height * 0.34f),
                contentScale = ContentScale.FillBounds
            )
        }
    }
}

@Composable
fun PremiumBadgeIcon(
    drawableResId: Int,
    contentDescriptionText: String?,
    modifier: Modifier = Modifier,
    size: Dp = 30.dp
) {
    Image(
        painter = painterResource(drawableResId),
        contentDescription = contentDescriptionText,
        modifier = modifier.size(size),
        contentScale = ContentScale.Fit
    )
}
