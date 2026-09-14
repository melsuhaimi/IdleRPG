package com.idlerpg.game.ui.component.premium

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.idlerpg.game.ui.theme.ArcaneViolet
import com.idlerpg.game.ui.theme.LocalReducedMotion
import com.idlerpg.game.ui.theme.MotionTokens
import com.idlerpg.game.ui.theme.ObsidianOutline
import com.idlerpg.game.ui.theme.ObsidianSurface1
import com.idlerpg.game.ui.theme.ObsidianSurface2
import com.idlerpg.game.ui.theme.ObsidianSurface3
import com.idlerpg.game.ui.theme.ResourceGold

/** Shared game-facing primitives. These define the visual language without owning game state. */

@Composable
fun GameSectionHeader(
    eyebrow: String,
    title: String,
    subtitle: String? = null,
    modifier: Modifier = Modifier,
    trailing: (@Composable () -> Unit)? = null
) {
    Column(
        modifier = modifier.semantics { heading() },
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = eyebrow.uppercase(),
                    style = MaterialTheme.typography.labelSmall.copy(
                        letterSpacing = 1.45.sp,
                        fontWeight = FontWeight.SemiBold
                    ),
                    color = ResourceGold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(width = 2.dp, height = 20.dp)
                            .background(
                                Brush.verticalGradient(
                                    listOf(
                                        ResourceGold.copy(alpha = 0.94f),
                                        ArcaneViolet.copy(alpha = 0.76f)
                                    )
                                ),
                                RoundedCornerShape(99.dp)
                            )
                    )
                    Text(
                        text = title,
                        style = MaterialTheme.typography.headlineSmall,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            trailing?.let { slot -> slot() }
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .widthIn(min = 42.dp, max = 56.dp)
                    .fillMaxHeight()
                    .background(ResourceGold, RoundedCornerShape(99.dp))
            )
            Spacer(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .padding(start = 8.dp)
                    .background(ObsidianOutline.copy(alpha = 0.42f), RoundedCornerShape(99.dp))
            )
        }
        subtitle?.takeIf { it.isNotBlank() }?.let { description ->
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun GameMetricChip(
    label: String,
    value: String,
    accent: Color,
    modifier: Modifier = Modifier,
    contentDescriptionText: String? = null
) {
    val semanticsModifier = Modifier.semantics(mergeDescendants = true) {
        contentDescriptionText?.let { description ->
            contentDescription = description
        }
    }
    Surface(
        modifier = modifier
            .then(semanticsModifier)
            .heightIn(min = 48.dp),
        shape = RoundedCornerShape(10.dp),
        color = ObsidianSurface1.copy(alpha = 0.92f),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.30f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .width(2.dp)
                    .height(28.dp)
                    .background(
                        Brush.verticalGradient(
                            listOf(accent.copy(alpha = 0.45f), accent)
                        ),
                        RoundedCornerShape(99.dp)
                    )
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(1.dp)
            ) {
                Text(
                    text = label.uppercase(),
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 10.sp,
                        letterSpacing = 0.75.sp
                    ),
                    color = accent,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = value,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
fun GameStatusPill(
    text: String,
    accent: Color,
    modifier: Modifier = Modifier,
    contentDescriptionText: String? = null
) {
    Surface(
        modifier = modifier
            .heightIn(min = 36.dp)
            .semantics(mergeDescendants = true) {
                contentDescriptionText?.let { description ->
                    contentDescription = description
                }
            },
        shape = RoundedCornerShape(99.dp),
        color = accent.copy(alpha = 0.11f),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.34f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(7.dp)
                    .background(accent, RoundedCornerShape(99.dp))
            )
            Text(
                text = text.uppercase(),
                style = MaterialTheme.typography.labelSmall.copy(
                    letterSpacing = 0.7.sp,
                    fontWeight = FontWeight.SemiBold
                ),
                color = accent,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun GameProgressBar(
    progressUnits: Int,
    accent: Color,
    modifier: Modifier = Modifier,
    height: androidx.compose.ui.unit.Dp = 10.dp,
    contentDescriptionText: String? = null
) {
    val targetFraction = progressUnits.coerceIn(0, 10_000) / 10_000f
    val fraction by animateFloatAsState(
        targetValue = targetFraction,
        animationSpec = if (LocalReducedMotion.current) {
            snap()
        } else {
            tween(MotionTokens.CARD_MILLIS)
        },
        label = "game_progress"
    )
    val semanticsModifier = Modifier.semantics(mergeDescendants = true) {
        contentDescriptionText?.let { description ->
            contentDescription = description
        }
        progressBarRangeInfo = ProgressBarRangeInfo(targetFraction, 0f..1f)
    }
    Box(
        modifier = modifier
            .then(semanticsModifier)
            .height(height)
            .clip(RoundedCornerShape(99.dp))
            .background(ObsidianSurface3.copy(alpha = 0.92f))
    ) {
        if (fraction > 0f) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(fraction)
                    .fillMaxHeight()
                    .background(
                        Brush.horizontalGradient(
                            listOf(accent.copy(alpha = 0.66f), accent)
                        ),
                        RoundedCornerShape(99.dp)
                    )
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(Color.White.copy(alpha = 0.28f))
                )
            }
        }
    }
}

@Composable
fun GameButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    contentPadding: PaddingValues = PaddingValues(horizontal = 16.dp, vertical = 0.dp),
    content: @Composable RowScope.() -> Unit
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.heightInGame(),
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = ResourceGold,
            contentColor = ObsidianSurface1,
            disabledContainerColor = ObsidianOutline.copy(alpha = 0.36f),
            disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant
        ),
        elevation = ButtonDefaults.buttonElevation(
            defaultElevation = 1.dp,
            pressedElevation = 4.dp,
            disabledElevation = 0.dp
        ),
        border = BorderStroke(
            1.dp,
            if (enabled) ResourceGold.copy(alpha = 0.88f)
            else ObsidianOutline.copy(alpha = 0.26f)
        ),
        contentPadding = contentPadding,
        content = content
    )
}

@Composable
fun GameOutlinedButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    contentPadding: PaddingValues = PaddingValues(horizontal = 14.dp, vertical = 0.dp),
    content: @Composable RowScope.() -> Unit
) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.heightInGame(),
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = MaterialTheme.colorScheme.onSurface,
            disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant
        ),
        border = BorderStroke(
            1.dp,
            if (enabled) ObsidianOutline.copy(alpha = 0.86f)
            else ObsidianOutline.copy(alpha = 0.34f)
        ),
        contentPadding = contentPadding,
        content = content
    )
}

@Composable
fun GameChoiceButton(
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    contentPadding: PaddingValues = PaddingValues(horizontal = 14.dp, vertical = 0.dp),
    content: @Composable RowScope.() -> Unit
) {
    if (selected) {
        GameButton(
            onClick = onClick,
            modifier = modifier,
            enabled = enabled,
            contentPadding = contentPadding,
            content = content
        )
    } else {
        GameOutlinedButton(
            onClick = onClick,
            modifier = modifier,
            enabled = enabled,
            contentPadding = contentPadding,
            content = content
        )
    }
}

private fun Modifier.heightInGame(): Modifier = heightIn(min = 48.dp).widthIn(min = 48.dp)

@Composable
fun GameDivider(modifier: Modifier = Modifier) {
    Spacer(
        modifier = modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(
                Brush.horizontalGradient(
                    listOf(
                        Color.Transparent,
                        ObsidianOutline.copy(alpha = 0.72f),
                        Color.Transparent
                    )
                )
            )
    )
}
