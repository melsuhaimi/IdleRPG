package com.idlerpg.game.ui.component.premium

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardColors
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CardElevation
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.idlerpg.game.R
import com.idlerpg.game.ui.theme.PanelHighlight
import com.idlerpg.game.ui.theme.ObsidianSurface1

/**
 * Shared secondary-surface card for game information.
 *
 * The card keeps Material's semantics and interaction behavior while giving the game a
 * consistent edge, elevation, and shape. Focal panels continue to use [PremiumPanel].
 */
@Composable
fun GameCard(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(12.dp),
    colors: CardColors = CardDefaults.cardColors(
        containerColor = ObsidianSurface1.copy(alpha = 0.96f)
    ),
    elevation: CardElevation = CardDefaults.cardElevation(
        defaultElevation = 0.dp,
        pressedElevation = 4.dp
    ),
    border: BorderStroke? = BorderStroke(
        width = 1.dp,
        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.28f)
    ),
    accent: Color? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier,
        shape = shape,
        colors = colors,
        elevation = elevation,
        border = border,
        content = { GameCardContent(content = content, accent = accent) }
    )
}

/** Clickable variant with the same visual treatment as [GameCard]. */
@Composable
fun GameCard(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape = RoundedCornerShape(12.dp),
    colors: CardColors = CardDefaults.cardColors(
        containerColor = ObsidianSurface1.copy(alpha = 0.96f)
    ),
    elevation: CardElevation = CardDefaults.cardElevation(
        defaultElevation = 0.dp,
        pressedElevation = 4.dp
    ),
    border: BorderStroke? = BorderStroke(
        width = 1.dp,
        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.28f)
    ),
    accent: Color? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        onClick = onClick,
        modifier = modifier
            .heightIn(min = 48.dp)
            .widthIn(min = 48.dp),
        enabled = enabled,
        shape = shape,
        colors = colors,
        elevation = elevation,
        border = border,
        content = { GameCardContent(content = content, accent = accent) }
    )
}

@Composable
private fun GameCardContent(
    content: @Composable ColumnScope.() -> Unit,
    accent: Color?
) {
    Box(modifier = Modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color.White.copy(alpha = 0.035f),
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.08f)
                        )
                    )
                )
        )
        Column(
            modifier = Modifier.fillMaxWidth(),
            content = content
        )
        accent?.let { accentColor ->
            Box(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .fillMaxHeight()
                    .width(1.5.dp)
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                accentColor.copy(alpha = 0.92f),
                                accentColor.copy(alpha = 0.22f),
                                accentColor.copy(alpha = 0.72f)
                            )
                        )
                    )
            )
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .align(Alignment.TopCenter)
                .background(
                    Brush.horizontalGradient(
                        listOf(
                            Color.Transparent,
                            (accent ?: PanelHighlight).copy(alpha = 0.42f),
                            Color.Transparent
                        )
                    )
                )
        )
    }
}
