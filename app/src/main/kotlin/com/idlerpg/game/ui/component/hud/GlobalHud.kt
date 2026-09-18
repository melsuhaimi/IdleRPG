package com.idlerpg.game.ui.component.hud

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt
import com.idlerpg.game.R
import com.idlerpg.game.presentation.model.GlobalHudSaveStatus
import com.idlerpg.game.presentation.model.GlobalHudUiState
import com.idlerpg.game.ui.component.premium.GameOutlinedButton
import com.idlerpg.game.ui.content.stringResId
import com.idlerpg.game.ui.theme.ObsidianOutline
import com.idlerpg.game.ui.theme.ObsidianSurface1
import com.idlerpg.game.ui.theme.ObsidianSurface2
import com.idlerpg.game.ui.theme.ObsidianSurface3
import com.idlerpg.game.ui.theme.PanelHighlight
import com.idlerpg.game.ui.theme.ResourceGold
import com.idlerpg.game.ui.theme.ResonanceTeal
import com.idlerpg.game.ui.theme.GameDimensions

@Composable
fun GlobalHud(
    state: GlobalHudUiState,
    onSaveNow: () -> Unit,
    onOpenAccessibility: () -> Unit,
    modifier: Modifier = Modifier
) {
    val saveStateDescription = when (state.saveStatus) {
        GlobalHudSaveStatus.IDLE -> stringResource(R.string.hud_save)
        GlobalHudSaveStatus.SAVING -> stringResource(R.string.save_status_saving)
        GlobalHudSaveStatus.SAVED -> stringResource(R.string.save_status_saved_short)
        GlobalHudSaveStatus.ERROR -> stringResource(R.string.save_status_error_short)
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = GameDimensions.HudMinHeight),
        shape = RoundedCornerShape(GameDimensions.LargePanelRadius),
        color = ObsidianSurface1.copy(alpha = 0.93f),
        border = BorderStroke(1.dp, ObsidianOutline.copy(alpha = 0.82f)),
        shadowElevation = 0.dp,
        tonalElevation = 0.dp
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(GameDimensions.LargePanelRadius))
        ) {
            Image(
                painter = painterResource(R.drawable.hud_header_castle_generated),
                contentDescription = null,
                modifier = Modifier
                    .matchParentSize()
                    .alpha(0.12f),
                contentScale = ContentScale.Crop
            )
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                Color(0xCC08101E),
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
                    .matchParentSize()
                    .alpha(0.12f),
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
                                    PanelHighlight.copy(alpha = 0.82f),
                                    ResonanceTeal.copy(alpha = 0.68f),
                                    ResourceGold.copy(alpha = 0.92f),
                                    Color.Transparent
                                )
                            )
                        )
                )
                BoxWithConstraints(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 8.dp)
                ) {
                    val compact = maxWidth < 380.dp
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(if (compact) 5.dp else 7.dp)
                    ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.hud_expedition_label).uppercase(),
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 10.sp,
                                letterSpacing = 0.9.sp
                            ),
                            color = ResourceGold,
                            maxLines = 1,
                            overflow = TextOverflow.Clip
                        )
                        Text(
                            text = state.currentRegionTitleKey?.let {
                                stringResource(it.stringResId())
                            } ?: stringResource(R.string.hud_no_region),
                            style = MaterialTheme.typography.headlineSmall.copy(
                                fontSize = if (compact) 15.sp else 17.sp,
                                lineHeight = if (compact) 18.sp else 20.sp
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Clip
                        )

                    }

                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = ObsidianSurface2.copy(alpha = 0.88f),
                        border = BorderStroke(1.dp, ResourceGold.copy(alpha = 0.62f))
                    ) {
                        Text(
                            text = stringResource(R.string.hud_level_format, state.playerLevel),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                            style = MaterialTheme.typography.titleSmall,
                            color = ResourceGold,
                            maxLines = 1
                        )
                    }

                    IconButton(
                        onClick = onOpenAccessibility,
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_accessibility),
                            contentDescription = stringResource(R.string.hud_accessibility),
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    GameOutlinedButton(
                        onClick = onSaveNow,
                        enabled = state.saveStatus != GlobalHudSaveStatus.SAVING,
                        modifier = Modifier.semantics {
                            stateDescription = saveStateDescription
                        },
                        contentPadding = PaddingValues(
                            horizontal = if (compact) 7.dp else 10.dp,
                            vertical = 5.dp
                        )
                    ) {
                        Text(
                            text = saveStateDescription,
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontSize = if (compact) 9.sp else 10.sp
                            ),
                            maxLines = 1
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(if (compact) 5.dp else 7.dp)
                ) {
                    HudResourceValue(
                        label = stringResource(R.string.hud_gold),
                        value = state.goldDisplay,
                        accent = ResourceGold,
                        iconResId = R.drawable.gold_coin_stack_generated,
                        compact = compact,
                        contentDescriptionText = stringResource(
                            R.string.hud_gold_value_format,
                            state.goldDisplay
                        ),
                        modifier = Modifier.weight(0.82f)
                    )
                    HudRailDivider()
                    HudResourceValue(
                        label = stringResource(R.string.hud_echo),
                        value = state.echoDisplay,
                        accent = ResonanceTeal,
                        iconResId = R.drawable.lumen_crystal_generated,
                        compact = compact,
                        contentDescriptionText = stringResource(
                            R.string.hud_legacy_value_format,
                            state.echoDisplay
                        ),
                        modifier = Modifier.weight(0.82f)
                    )
                    HudRailDivider()
                    Column(
                        modifier = Modifier.weight(1.65f),
                        verticalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = stringResource(R.string.hud_xp).uppercase(),
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 10.sp,
                                    letterSpacing = 0.45.sp
                                ),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1
                            )
                            Text(
                                text = stringResource(
                                    R.string.hud_xp_percent_format,
                                    (state.experienceProgressUnits / 100f).roundToInt()
                                ),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Clip
                            )
                        }
                        SegmentedExperienceBar(
                            progressUnits = state.experienceProgressUnits,
                            contentDescriptionText = stringResource(
                                R.string.a11y_xp_progress_format,
                                state.currentExperienceDisplay,
                                state.experienceToNextDisplay
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }


                    }
                }
            }
        }
    }
}

@Composable
private fun HudResourceValue(
    label: String,
    value: String,
    accent: Color,
    contentDescriptionText: String,
    iconResId: Int,
    compact: Boolean,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
             .heightIn(min = if (compact) 38.dp else 40.dp)
            .semantics(mergeDescendants = true) {
                contentDescription = contentDescriptionText
            },
        shape = RoundedCornerShape(11.dp),
        color = ObsidianSurface2.copy(alpha = 0.72f),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.28f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = if (compact) 5.dp else 6.dp,
                    vertical = if (compact) 3.dp else 4.dp
                ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(if (compact) 3.dp else 5.dp)
        ) {
            Box(
                modifier = Modifier
                    .width(2.dp)
                    .height(if (compact) 20.dp else 22.dp)
                    .background(
                        Brush.verticalGradient(
                            listOf(accent.copy(alpha = 0.38f), accent)
                        ),
                        RoundedCornerShape(99.dp)
                    )
            )
            Image(
                painter = painterResource(iconResId),
                contentDescription = null,
                modifier = Modifier.size(if (compact) 18.dp else 20.dp),
                contentScale = ContentScale.Fit
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .widthIn(min = 0.dp),
                verticalArrangement = Arrangement.spacedBy(1.dp)
            ) {
                Text(
                    text = label.uppercase(),
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 10.sp,
                        letterSpacing = if (compact) 0.55.sp else 0.7.sp
                    ),
                    color = accent,
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Clip
                )
                Text(
                    text = value,
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontSize = when {
                            value.length >= 10 -> 8.sp
                            value.length >= 8 -> 9.sp
                            value.length >= 6 -> 10.sp
                            else -> 11.sp
                        }
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Clip
                )
            }
        }
    }
}

@Composable
private fun SegmentedExperienceBar(
    progressUnits: Int,
    contentDescriptionText: String,
    modifier: Modifier = Modifier
) {
    val segmentCount = 10
    val normalized = progressUnits.coerceIn(0, 10_000) / 10_000f * segmentCount
    Row(
        modifier = modifier
            .heightIn(min = 6.dp)
            .semantics { contentDescription = contentDescriptionText },
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(segmentCount) { index ->
            val fill = (normalized - index).coerceIn(0f, 1f)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(6.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(ObsidianSurface3.copy(alpha = 0.95f))
            ) {
                if (fill > 0f) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(fill)
                            .fillMaxHeight()
                            .background(ResonanceTeal)
                    )
                }
            }
        }
    }
}

@Composable
private fun HudRailDivider() {
    Box(
        modifier = Modifier
            .width(1.dp)
            .height(28.dp)
            .background(ObsidianOutline.copy(alpha = 0.56f))
    )
}
