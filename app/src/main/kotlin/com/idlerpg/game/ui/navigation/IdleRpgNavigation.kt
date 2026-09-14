package com.idlerpg.game.ui.navigation

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.idlerpg.game.R
import com.idlerpg.game.ui.content.drawableResId
import com.idlerpg.game.ui.theme.ObsidianOutline
import com.idlerpg.game.ui.theme.ObsidianSurface1
import com.idlerpg.game.ui.theme.PanelHighlight
import com.idlerpg.game.ui.theme.ResourceGold
import com.idlerpg.game.ui.theme.TextSecondary
import com.idlerpg.game.ui.theme.GameDimensions

/**
 * Compact game navigation dock. Selection is communicated by color, a quiet halo, and an
 * underline so the battlefield remains the visual priority. Destination availability is still
 * owned by [NavigationAvailability]; this component only renders the supplied list.
 */
@Composable
fun IdleRpgBottomNavigation(
    selected: GameDestination,
    onSelect: (GameDestination) -> Unit,
    destinations: List<GameDestination> = listOf(
        GameDestination.BATTLE,
        GameDestination.WORLD,
        GameDestination.GEAR,
        GameDestination.PROGRESS
    ),
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = ObsidianSurface1.copy(alpha = 0.98f),
        shadowElevation = 6.dp,
        tonalElevation = 0.dp,
        shape = RoundedCornerShape(GameDimensions.LargePanelRadius),
        border = BorderStroke(
            width = 1.dp,
            color = ObsidianOutline.copy(alpha = 0.56f)
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(GameDimensions.LargePanelRadius))
        ) {
            Image(
                painter = painterResource(R.drawable.panel_secondary_premium),
                contentDescription = null,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(GameDimensions.BottomDockArtworkHeight)
                    .alpha(0.34f),
                contentScale = ContentScale.FillBounds
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(GameDimensions.BottomDockArtworkHeight)
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                Color(0xB80A1322),
                                ObsidianSurface1.copy(alpha = 0.98f)
                            )
                        )
                    )
            )
            Image(
                painter = painterResource(R.drawable.ui_panel_frame_generated),
                contentDescription = null,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(GameDimensions.BottomDockArtworkHeight)
                    .alpha(0.62f),
                contentScale = ContentScale.FillBounds
            )
            Column(modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.dp)
                    .background(
                        Brush.horizontalGradient(
                            listOf(
                                Color.Transparent,
                                PanelHighlight.copy(alpha = 0.62f),
                                ResourceGold.copy(alpha = 0.68f),
                                Color.Transparent
                            )
                        )
                    )
            )
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 6.dp)
            ) {
                val scrollItems = maxWidth < 352.dp && destinations.size > 4
                val rowModifier = if (scrollItems) {
                    Modifier.horizontalScroll(rememberScrollState())
                } else {
                    Modifier.fillMaxWidth()
                }

                Row(
                    modifier = rowModifier,
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    destinations.forEach { destination ->
                        val selectedItem = if (
                            selected == GameDestination.DOCTRINE &&
                            GameDestination.GEAR in destinations
                        ) {
                            destination == GameDestination.GEAR
                        } else {
                            destination == selected
                        }
                        GameNavigationItem(
                            destination = destination,
                            selected = selectedItem,
                            compactLabel = scrollItems,
                            onClick = { onSelect(destination) },
                            modifier = if (scrollItems) {
                                Modifier.width(70.dp)
                            } else {
                                Modifier.weight(1f)
                            }
                        )
                    }
                }
            }
            }
        }
    }
}

@Composable
private fun GameNavigationItem(
    destination: GameDestination,
    selected: Boolean,
    compactLabel: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val fullLabel = stringResource(destination.labelResId)

    Box(
        modifier = modifier
            .heightIn(min = 76.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(
                if (selected) {
                    Brush.verticalGradient(
                        listOf(
                            ResourceGold.copy(alpha = 0.24f),
                            ResourceGold.copy(alpha = 0.045f)
                        )
                    )
                } else {
                    Brush.verticalGradient(listOf(Color.Transparent, Color.Transparent))
                }
            )
            .border(
                width = if (selected) 1.dp else 0.dp,
                color = if (selected) ResourceGold.copy(alpha = 0.62f) else Color.Transparent,
                shape = RoundedCornerShape(10.dp)
            )
            .selectable(
                selected = selected,
                onClick = onClick,
                role = Role.Tab
            )
            .semantics {
                contentDescription = fullLabel
            },
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(if (selected) 42.dp else 36.dp)
                    .clip(RoundedCornerShape(99.dp))
                    .background(
                        if (selected) ResourceGold.copy(alpha = 0.18f) else Color.Transparent
                    ),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(destination.iconAssetKey.drawableResId()),
                    contentDescription = null,
                    modifier = Modifier.size(if (selected) 30.dp else 26.dp),
                    contentScale = ContentScale.Fit
                )
            }
            Text(
                text = fullLabel.uppercase(),
                color = if (selected) ResourceGold else TextSecondary,
                style = androidx.compose.material3.MaterialTheme.typography.labelSmall.copy(
                    fontSize = if (compactLabel) 10.sp else 11.sp,
                    letterSpacing = 0.35.sp
                ),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Box(
                modifier = Modifier
                    .size(width = if (selected) 42.dp else 30.dp, height = 3.dp)
                    .clip(RoundedCornerShape(99.dp))
                    .background(if (selected) ResourceGold else Color.Transparent)
            )
        }
    }
}

@Composable
fun ProgressDestinationBar(
    selected: ProgressDestination,
    onSelect: (ProgressDestination) -> Unit,
    destinations: List<ProgressDestination> = ProgressDestination.values().toList(),
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 12.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        destinations.forEach { destination ->
            val isSelected = destination == selected
            val label = stringResource(destination.labelResId)
            Box(
                modifier = Modifier
                    .heightIn(min = 52.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(
                        if (isSelected) {
                            Brush.verticalGradient(
                                listOf(
                                    ResourceGold.copy(alpha = 0.14f),
                                    ResourceGold.copy(alpha = 0.035f)
                                )
                            )
                        } else {
                            Brush.verticalGradient(listOf(Color.Transparent, Color.Transparent))
                        }
                    )
                    .selectable(
                        selected = isSelected,
                        onClick = { onSelect(destination) },
                        role = Role.Tab
                    )
                    .semantics {
                        contentDescription = label
                    },
                contentAlignment = Alignment.Center
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = label.uppercase(),
                        color = if (isSelected) ResourceGold else TextSecondary,
                        style = androidx.compose.material3.MaterialTheme.typography.labelSmall.copy(
                            letterSpacing = 0.45.sp
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Box(
                        modifier = Modifier
                            .size(width = 24.dp, height = 3.dp)
                            .clip(RoundedCornerShape(99.dp))
                            .background(if (isSelected) ResourceGold else Color.Transparent)
                    )
                }
            }
        }
    }
}
