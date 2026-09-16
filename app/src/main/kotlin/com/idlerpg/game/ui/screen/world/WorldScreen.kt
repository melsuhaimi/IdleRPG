package com.idlerpg.game.ui.screen.world

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import com.idlerpg.game.R
import com.idlerpg.game.domain.command.CommandRejectionCode
import com.idlerpg.game.presentation.intent.WorldUiIntent
import com.idlerpg.game.presentation.model.WorldAffinityAdaptationUiState
import com.idlerpg.game.presentation.model.WorldEncounterStatusUi
import com.idlerpg.game.presentation.model.WorldEncounterTypeUi
import com.idlerpg.game.presentation.model.WorldEncounterUiState
import com.idlerpg.game.presentation.model.WorldFeedbackKind
import com.idlerpg.game.presentation.model.WorldFeedbackUiState
import com.idlerpg.game.presentation.model.WorldMutationUiState
import com.idlerpg.game.presentation.model.WorldRegionUiState
import com.idlerpg.game.presentation.model.WorldUiState
import com.idlerpg.game.domain.model.world.WorldAutomationMode
import com.idlerpg.game.ui.component.premium.GameCard
import com.idlerpg.game.ui.component.premium.GameButton
import com.idlerpg.game.ui.component.premium.GameChoiceButton
import com.idlerpg.game.ui.component.premium.GameOutlinedButton
import com.idlerpg.game.ui.component.premium.GameSectionHeader
import com.idlerpg.game.ui.component.premium.GameStatusPill
import com.idlerpg.game.ui.component.premium.PremiumPanel
import com.idlerpg.game.ui.content.drawableResId
import com.idlerpg.game.ui.content.stringResId
import com.idlerpg.game.ui.motion.eventFeedbackPulse

/** Premium World + Encounter + Adaptation production surface. */
@Composable
fun WorldScreen(
    state: WorldUiState,
    onIntent: (WorldUiIntent) -> Unit,
    onOpenGear: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF080A12))
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                WorldHeroBanner(modifier = Modifier.padding(top = 10.dp))
            }

            item {
                PremiumPanel(
                    backgroundResId = R.drawable.panel_secondary_premium,
                    modifier = Modifier.fillMaxWidth(),
                    accent = if (state.automationMode == WorldAutomationMode.PUSH) {
                        MaterialTheme.colorScheme.secondary
                    } else {
                        com.idlerpg.game.ui.theme.ResourceGold
                    }
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = stringResource(R.string.world_run_mode),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.secondary
                                )
                                GameStatusPill(
                                    text = stringResource(
                                        if (state.automationMode == WorldAutomationMode.PUSH)
                                            R.string.world_mode_push else R.string.world_mode_farm
                                    ),
                                    accent = if (state.automationMode == WorldAutomationMode.PUSH) {
                                        MaterialTheme.colorScheme.secondary
                                    } else {
                                        com.idlerpg.game.ui.theme.ResourceGold
                                    }
                                )
                            }
                            Text(
                                text = stringResource(
                                    if (state.automationMode == WorldAutomationMode.PUSH)
                                        R.string.world_mode_push_active else R.string.world_mode_farm_active
                                ),
                                modifier = Modifier.weight(1.4f),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 2
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            GameChoiceButton(
                                selected = state.automationMode == WorldAutomationMode.PUSH,
                                onClick = {
                                    onIntent(WorldUiIntent.ConfigureAutomationIntent(WorldAutomationMode.PUSH))
                                },
                                modifier = Modifier.weight(1f)
                            ) { Text(stringResource(R.string.world_mode_push)) }
                            GameChoiceButton(
                                selected = state.automationMode == WorldAutomationMode.FARM,
                                onClick = {
                                    onIntent(
                                        WorldUiIntent.ConfigureAutomationIntent(
                                            WorldAutomationMode.FARM,
                                            state.selectedFarmEncounterId
                                        )
                                    )
                                },
                                modifier = Modifier.weight(1f)
                            ) { Text(stringResource(R.string.world_mode_farm)) }
                        }
                    }
                }
            }

            state.feedback?.let { feedback ->
                item { WorldFeedbackCard(feedback) }
            }

            if (state.inventory.blocked) {
                item {
                    InventoryBlockCard(
                        state = state,
                        onOpenGear = onOpenGear
                    )
                }
            }

            items(
                items = state.regions,
                key = { it.regionId.value }
            ) { region ->
                RegionCard(
                    region = region,
                    inventoryBlocked = state.inventory.blocked,
                    onIntent = onIntent
                )
            }

        }
    }
}

@Composable
private fun WorldHeroBanner(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(176.dp)
            .clip(RoundedCornerShape(20.dp))
            .border(
                1.dp,
                MaterialTheme.colorScheme.outline.copy(alpha = 0.58f),
                RoundedCornerShape(20.dp)
            ),
        contentAlignment = Alignment.BottomStart
    ) {
        Image(
            painter = painterResource(R.drawable.bg_world_premium),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color.Transparent,
                            Color(0xB8070910)
                        )
                    )
                )
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(3.dp)
                .align(Alignment.TopCenter)
                .background(
                    Brush.horizontalGradient(
                        listOf(
                            Color.Transparent,
                            com.idlerpg.game.ui.theme.ResonanceTeal.copy(alpha = 0.86f),
                            com.idlerpg.game.ui.theme.ResourceGold.copy(alpha = 0.76f),
                            Color.Transparent
                        )
                    )
                )
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Text(
                text = stringResource(R.string.nav_world).uppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.secondary
            )
            Text(
                text = stringResource(R.string.world_title),
                style = MaterialTheme.typography.headlineMedium
            )
            Text(
                text = stringResource(R.string.world_subtitle),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2
            )
        }
    }
}

@Composable
private fun InventoryBlockCard(
    state: WorldUiState,
    onOpenGear: () -> Unit
) {
    GameCard(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = stringResource(R.string.world_inventory_blocked_title),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onErrorContainer
            )
            Text(
                text = stringResource(
                    R.string.world_inventory_blocked_detail,
                    state.inventory.normalUsed,
                    state.inventory.normalCapacity,
                    state.inventory.overflowUsed,
                    state.inventory.overflowCapacity
                ),
                color = MaterialTheme.colorScheme.onErrorContainer
            )
            GameButton(onClick = onOpenGear) {
                Text(stringResource(R.string.world_open_gear))
            }
        }
    }
}

@Composable
private fun RegionCard(
    region: WorldRegionUiState,
    inventoryBlocked: Boolean,
    onIntent: (WorldUiIntent) -> Unit
) {
    var selectedEncounterId by rememberSaveable(region.regionId.value) { mutableStateOf<String?>(null) }
    var showThreatIntel by rememberSaveable(region.regionId.value) { mutableStateOf(false) }
    val selectedEncounter = region.encounters.firstOrNull { it.status == WorldEncounterStatusUi.ACTIVE }
        ?: region.encounters.firstOrNull { it.encounterId.value == selectedEncounterId }
        ?: region.encounters.firstOrNull { it.canStart }
        ?: region.encounters.lastOrNull { it.cleared }
    PremiumPanel(
        backgroundResId = R.drawable.panel_secondary_premium,
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 14.dp),
        verticalSpacing = 9.dp,
        accent = if (region.selected) {
            com.idlerpg.game.ui.theme.ResourceGold
        } else {
            MaterialTheme.colorScheme.secondary
        }
    ) {
        region.illustrationAssetKey?.let { illustration ->
            val title = stringResource(region.titleStringKey.stringResId())
            Image(
                painter = painterResource(illustration.drawableResId()),
                contentDescription = stringResource(
                    R.string.a11y_region_illustration_format,
                    title
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(96.dp)
                    .clip(RoundedCornerShape(14.dp)),
                contentScale = ContentScale.Crop
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = stringResource(region.titleStringKey.stringResId()),
                    style = MaterialTheme.typography.titleLarge
                )
                Text(
                    text = stringResource(region.descriptionStringKey.stringResId()),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (!region.selected) {
                GameOutlinedButton(
                    onClick = {
                        onIntent(WorldUiIntent.SelectRegionIntent(region.regionId))
                    },
                    enabled = region.unlocked
                ) {
                    Text(
                        if (region.unlocked) {
                            stringResource(R.string.world_select_region)
                        } else {
                            stringResource(R.string.world_region_locked)
                        }
                    )
                }
            } else {
                Text(
                    text = stringResource(R.string.world_region_selected),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.secondary
                )
            }
        }

        Text(
            text = stringResource(
                R.string.world_region_progress_format,
                region.normalClearsDisplay,
                region.eliteClearsDisplay,
                region.highestClearedEncounterTier
            ),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        GameSectionHeader(
            eyebrow = stringResource(R.string.world_encounters),
            title = stringResource(R.string.world_route_title),
            subtitle = stringResource(R.string.world_route_subtitle)
        )
        region.encounters.groupBy { it.sectorIndex }.toSortedMap().forEach { (_, sector) ->
            SectorBanner(
                title = stringResource(sector.first().sectorTitleStringKey.stringResId()),
                backgroundResId = sector.first().sectorBackgroundAssetKey.drawableResId()
            )
            StageRoute(
                sector = sector,
                allEncounters = region.encounters,
                selectedId = selectedEncounter?.encounterId?.value,
                onSelect = { selectedEncounterId = it.encounterId.value }
            )
        }

        selectedEncounter?.let { encounter ->
            SelectedEncounterCard(
                encounter = encounter,
                stageNumber = region.encounters.indexOfFirst { it.encounterId == encounter.encounterId } + 1,
                inventoryBlocked = inventoryBlocked,
                onIntent = onIntent
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = stringResource(R.string.world_threat_intel),
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = stringResource(R.string.world_threat_intel_subtitle),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            GameOutlinedButton(
                onClick = { showThreatIntel = !showThreatIntel },
                contentPadding = PaddingValues(horizontal = 10.dp)
            ) {
                Text(
                    stringResource(
                        if (showThreatIntel) R.string.world_threat_hide else R.string.world_threat_inspect
                    )
                )
            }
        }
        if (showThreatIntel) {
            val visibleAffinities = region.adaptation.filter { it.hasPressure || it.tier > 0 }
            if (visibleAffinities.isEmpty()) {
                Text(
                    stringResource(R.string.world_adaptation_quiet),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                visibleAffinities.forEach { affinity -> AdaptationAffinityRow(affinity) }
            }
            GameStatusPill(
                text = stringResource(
                    R.string.world_active_reward_multiplier_format,
                    region.activeEnemyRewardMultiplierDisplay
                ),
                accent = com.idlerpg.game.ui.theme.ResourceGold
            )
            Text(
                text = stringResource(R.string.world_mutations_title),
                style = MaterialTheme.typography.titleSmall
            )
            if (region.mutations.isEmpty()) {
                Text(
                    text = if (region.hiddenMutationCount > 0) {
                        stringResource(R.string.world_mutation_forecast_locked)
                    } else {
                        stringResource(R.string.world_no_mutations_known)
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                region.mutations.forEach { mutation -> MutationRow(mutation) }
            }
            if (region.adaptationForecastUnlocked) {
                Text(
                    text = stringResource(R.string.world_mutation_forecast_unlocked),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.secondary
                )
            }
        }
    }
}

@Composable
private fun SectorBanner(
    title: String,
    @androidx.annotation.DrawableRes backgroundResId: Int
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .clip(RoundedCornerShape(14.dp)),
        contentAlignment = Alignment.BottomStart
    ) {
        Image(
            painter = painterResource(backgroundResId),
            contentDescription = null,
            modifier = Modifier.fillMaxSize().alpha(0.72f),
            contentScale = ContentScale.Crop
        )
        Text(
            text = title,
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0x99070910))
                .padding(horizontal = 14.dp, vertical = 9.dp),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.secondary
        )
    }
}

@Composable
private fun StageRoute(
    sector: List<WorldEncounterUiState>,
    allEncounters: List<WorldEncounterUiState>,
    selectedId: String?,
    onSelect: (WorldEncounterUiState) -> Unit
) {
    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        // Keep route nodes comfortably tappable on compact phones while still using the
        // available width on tablets. The route remains a single vertical surface, so it does
        // not introduce a nested scroll container inside the Adventure screen.
        val nodeSpacing = 8.dp
        val minimumNodeWidth = 64.dp
        val columns = ((maxWidth + nodeSpacing) / (minimumNodeWidth + nodeSpacing))
            .toInt()
            .coerceIn(1, 5)

        Column(verticalArrangement = Arrangement.spacedBy(nodeSpacing)) {
            sector.chunked(columns).forEach { stageRow ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(nodeSpacing)
                ) {
                    stageRow.forEach { encounter ->
                        val stageNumber = allEncounters.indexOfFirst {
                            it.encounterId == encounter.encounterId
                        } + 1
                        StageNode(
                            encounter = encounter,
                            stageNumber = stageNumber,
                            selected = encounter.encounterId.value == selectedId,
                            onClick = { onSelect(encounter) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    repeat(columns - stageRow.size) {
                        Spacer(Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
private fun StageNode(
    encounter: WorldEncounterUiState,
    stageNumber: Int,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val accent = when (encounter.type) {
        WorldEncounterTypeUi.NORMAL -> MaterialTheme.colorScheme.secondary
        WorldEncounterTypeUi.ELITE -> com.idlerpg.game.ui.theme.ResourceGold
        WorldEncounterTypeUi.ANOMALY -> MaterialTheme.colorScheme.tertiary
        WorldEncounterTypeUi.BOSS -> MaterialTheme.colorScheme.error
    }
    val stageA11y = stringResource(
        R.string.world_stage_a11y,
        stageNumber,
        stringResource(encounter.titleStringKey.stringResId()),
        encounterStatusLabel(encounter.status, stageNumber.toLong())
    )
    GameCard(
        onClick = onClick,
        modifier = modifier
            .sizeIn(minWidth = 48.dp, minHeight = 48.dp)
            .aspectRatio(1f)
            .semantics { contentDescription = stageA11y },
        shape = RoundedCornerShape(14.dp),
        border = androidx.compose.foundation.BorderStroke(
            if (selected) 2.dp else 1.dp,
            if (selected) accent else accent.copy(alpha = 0.30f)
        ),
        accent = accent,
        colors = CardDefaults.cardColors(
            containerColor = when {
                selected -> MaterialTheme.colorScheme.primaryContainer
                encounter.cleared -> MaterialTheme.colorScheme.surfaceVariant
                encounter.canStart -> MaterialTheme.colorScheme.surface
                else -> MaterialTheme.colorScheme.surface.copy(alpha = 0.52f)
            }
        )
    ) {
        Box(Modifier.fillMaxSize()) {
            Text(stageNumber.toString(), Modifier.align(Alignment.Center), style = MaterialTheme.typography.titleMedium)
            Box(Modifier.align(Alignment.TopEnd).padding(6.dp).size(7.dp).clip(CircleShape).background(accent))
            Text(
                when {
                    encounter.status == WorldEncounterStatusUi.ACTIVE -> stringResource(R.string.world_stage_current_marker)
                    encounter.cleared -> "✓"
                    encounter.type == WorldEncounterTypeUi.ELITE -> stringResource(R.string.world_stage_elite_marker)
                    encounter.type == WorldEncounterTypeUi.ANOMALY -> stringResource(R.string.world_stage_anomaly_marker)
                    encounter.type == WorldEncounterTypeUi.BOSS -> stringResource(R.string.world_stage_boss_marker)
                    else -> ""
                },
                Modifier.align(Alignment.BottomCenter).padding(bottom = 3.dp),
                style = MaterialTheme.typography.labelSmall,
                color = accent,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun SelectedEncounterCard(
    encounter: WorldEncounterUiState,
    stageNumber: Int,
    inventoryBlocked: Boolean,
    onIntent: (WorldUiIntent) -> Unit
) {
    PremiumPanel(
        backgroundResId = encounter.encounterFrameAssetKey.drawableResId(),
        contentPadding = PaddingValues(14.dp),
        verticalSpacing = 7.dp,
        accent = when (encounter.type) {
            WorldEncounterTypeUi.BOSS -> MaterialTheme.colorScheme.error
            WorldEncounterTypeUi.ELITE -> com.idlerpg.game.ui.theme.ResourceGold
            WorldEncounterTypeUi.ANOMALY -> MaterialTheme.colorScheme.tertiary
            WorldEncounterTypeUi.NORMAL -> MaterialTheme.colorScheme.secondary
        }
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Image(
                painter = painterResource((encounter.enemyIllustrationAssetKey ?: encounter.iconAssetKey).drawableResId()),
                contentDescription = stringResource(encounter.enemyTitleStringKey.stringResId()),
                modifier = Modifier.size(58.dp),
                contentScale = ContentScale.Fit
            )
            Column(Modifier.weight(1f)) {
                Text(
                    encounter.stageLabel ?: "STAGE ${stageNumber.toString().padStart(2, '0')}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.secondary
                )
                Text(stringResource(encounter.titleStringKey.stringResId()), style = MaterialTheme.typography.titleMedium, maxLines = 2)
                Text(
                    "${encounterTypeLabel(encounter.type)} · ${stringResource(encounter.enemyTitleStringKey.stringResId())}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.tertiary
                )
                Text(encounterStatusLabel(encounter.status, stageNumber.toLong()), style = MaterialTheme.typography.labelSmall, maxLines = 1)
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            GameStatusPill(
                text = encounter.difficultyDisplay ?: stringResource(R.string.world_tier_unknown),
                accent = when (encounter.type) {
                    WorldEncounterTypeUi.BOSS -> MaterialTheme.colorScheme.error
                    WorldEncounterTypeUi.ELITE -> com.idlerpg.game.ui.theme.ResourceGold
                    else -> MaterialTheme.colorScheme.tertiary
                }
            )
            Text(
                "${encounter.waveCount} WAVE${if (encounter.waveCount == 1) "" else "S"}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Text(
            stringResource(
                R.string.world_encounter_attack_format,
                encounter.attackTitle,
                stringResource(encounter.attackAffinityStringKey.stringResId()),
                encounter.attackDamageDisplay,
                encounter.attackIntervalMillis / 1000f
            ),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = stringResource(R.string.world_expected_rewards),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.secondary
        )
        Text(
            "${encounter.expectedGoldDisplay ?: "—"} GOLD  ·  " +
                "${encounter.expectedExperienceDisplay ?: "—"} XP  ·  ${encounter.rewardMultiplierDisplay ?: "100%"}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 2,
            overflow = TextOverflow.Clip
        )
        Text(
            text = stringResource(R.string.world_reward_focus_format, encounter.rewardFocus),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.secondary,
            maxLines = 2,
            overflow = TextOverflow.Clip
        )
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (encounter.canRetreat) {
                GameOutlinedButton(
                    onClick = { onIntent(WorldUiIntent.RetreatEncounterIntent) },
                    modifier = Modifier.weight(1f)
                ) {
                    Text(stringResource(R.string.world_retreat))
                }
            } else {
                GameButton(
                    onClick = { onIntent(WorldUiIntent.StartEncounterIntent(encounter.encounterId)) },
                    enabled = encounter.canStart && !inventoryBlocked,
                    modifier = Modifier.weight(1f)
                ) { Text(if (inventoryBlocked) stringResource(R.string.world_start_blocked) else stringResource(R.string.world_start_encounter)) }
                if (encounter.cleared) {
                    GameOutlinedButton(
                        onClick = { onIntent(WorldUiIntent.ConfigureAutomationIntent(WorldAutomationMode.FARM, encounter.encounterId)) },
                        modifier = Modifier.weight(1f)
                    ) { Text(stringResource(R.string.world_farm_here)) }
                }
            }
        }
    }
}

@Composable
private fun AdaptationAffinityRow(state: WorldAffinityAdaptationUiState) {
    GameCard(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        accent = MaterialTheme.colorScheme.secondary
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = stringResource(state.titleStringKey.stringResId()),
                    style = MaterialTheme.typography.labelLarge
                )
                Text(
                    text = stringResource(R.string.world_adaptation_tier_format, state.tier),
                    style = MaterialTheme.typography.labelMedium
                )
            }
            val nextThresholdDisplay = state.nextThresholdDisplay
            Text(
                text = if (nextThresholdDisplay == null) {
                    stringResource(
                        R.string.world_adaptation_pressure_max_format,
                        state.pressureDisplay
                    )
                } else {
                    stringResource(
                        R.string.world_adaptation_pressure_format,
                        state.pressureDisplay,
                        nextThresholdDisplay,
                        state.nextTier ?: state.tier
                    )
                },
                style = MaterialTheme.typography.bodySmall
            )
            if (state.currentEncounterContributionDisplay != "0" ||
                state.recentEncounterContributionDisplay != "0"
            ) {
                Text(
                    text = stringResource(
                        R.string.world_adaptation_contribution_format,
                        state.currentEncounterContributionDisplay,
                        state.recentEncounterContributionDisplay
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun MutationRow(state: WorldMutationUiState) {
    GameCard(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        accent = MaterialTheme.colorScheme.tertiary
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = stringResource(state.titleStringKey.stringResId()),
                style = MaterialTheme.typography.labelLarge
            )
            Text(
                text = stringResource(
                    R.string.world_mutation_requirement_format,
                    stringResource(state.triggerAffinityStringKey.stringResId()),
                    state.minimumTier
                ),
                style = MaterialTheme.typography.bodySmall
            )
            Text(
                text = when {
                    state.activeOnCurrentEnemy -> stringResource(R.string.world_mutation_active_enemy)
                    state.currentlyEligible -> stringResource(R.string.world_mutation_eligible_future)
                    state.forecastOnly -> stringResource(R.string.world_mutation_forecast_only)
                    else -> stringResource(R.string.world_no_mutations_known)
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            state.effects.forEach { effect ->
                Text(
                    text = when (effect.kind) {
                        com.idlerpg.game.presentation.model.WorldMutationEffectKindUi.AFFINITY_RESISTANCE ->
                            stringResource(
                                R.string.world_mutation_effect_resistance,
                                effect.magnitudeDisplay,
                                stringResource(state.triggerAffinityStringKey.stringResId())
                            )
                        com.idlerpg.game.presentation.model.WorldMutationEffectKindUi.FASTER_CADENCE ->
                            stringResource(R.string.world_mutation_effect_cadence, effect.magnitudeDisplay)
                        com.idlerpg.game.presentation.model.WorldMutationEffectKindUi.EXTRA_RESONANCE_DRAIN ->
                            stringResource(R.string.world_mutation_effect_resonance_drain, effect.magnitudeDisplay)
                        com.idlerpg.game.presentation.model.WorldMutationEffectKindUi.HEALING_SUPPRESSION ->
                            stringResource(R.string.world_mutation_effect_healing, effect.magnitudeDisplay)
                        com.idlerpg.game.presentation.model.WorldMutationEffectKindUi.GUARD_PRESSURE ->
                            stringResource(R.string.world_mutation_effect_guard_pressure, effect.magnitudeDisplay)
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
private fun WorldFeedbackCard(feedback: WorldFeedbackUiState) {
    GameCard(
        modifier = Modifier
            .fillMaxWidth()
            .eventFeedbackPulse(feedback.sequenceNumber),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        ),
        accent = MaterialTheme.colorScheme.primary
    ) {
        Text(
            text = when (feedback.kind) {
                WorldFeedbackKind.COMMAND_REJECTED -> stringResource(
                    R.string.world_feedback_rejected,
                    rejectionMessage(feedback.rejectionCode)
                )
                WorldFeedbackKind.REGION_SELECTED -> stringResource(R.string.world_feedback_region_selected)
                WorldFeedbackKind.ENCOUNTER_STARTED -> stringResource(R.string.world_feedback_encounter_started)
                WorldFeedbackKind.ENCOUNTER_CLEARED -> stringResource(R.string.world_feedback_encounter_cleared)
                WorldFeedbackKind.ENCOUNTER_RETREATED -> stringResource(R.string.world_feedback_encounter_retreated)
                WorldFeedbackKind.ENCOUNTER_FAILED -> stringResource(R.string.world_feedback_encounter_failed)
                WorldFeedbackKind.ADAPTATION_TIER_CHANGED -> stringResource(
                    R.string.world_feedback_adaptation_tier,
                    feedback.tier ?: 0
                )
                WorldFeedbackKind.MUTATION_ROLLED -> stringResource(R.string.world_feedback_mutation_rolled)
                WorldFeedbackKind.INVENTORY_BLOCKED -> stringResource(R.string.world_feedback_inventory_blocked)
                WorldFeedbackKind.INVENTORY_UNBLOCKED -> stringResource(R.string.world_feedback_inventory_unblocked)
            },
            modifier = Modifier.padding(12.dp),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )
    }
}

@Composable
private fun encounterTypeLabel(type: WorldEncounterTypeUi): String = when (type) {
    WorldEncounterTypeUi.NORMAL -> stringResource(R.string.world_encounter_type_normal)
    WorldEncounterTypeUi.ELITE -> stringResource(R.string.world_encounter_type_elite)
    WorldEncounterTypeUi.ANOMALY -> stringResource(R.string.world_encounter_type_anomaly)
    WorldEncounterTypeUi.BOSS -> stringResource(R.string.world_encounter_type_boss)
}

@Composable
private fun encounterStatusLabel(
    status: WorldEncounterStatusUi,
    encounterIndex: Long?
): String = when (status) {
    WorldEncounterStatusUi.IDLE -> stringResource(R.string.world_encounter_idle)
    WorldEncounterStatusUi.ACTIVE -> stringResource(
        R.string.world_encounter_active_format,
        encounterIndex ?: 0L
    )
    WorldEncounterStatusUi.CLEARED -> stringResource(R.string.world_encounter_cleared)
    WorldEncounterStatusUi.FAILED -> stringResource(R.string.world_encounter_failed)
    WorldEncounterStatusUi.RETREATED -> stringResource(R.string.world_encounter_retreated)
}

@Composable
private fun rejectionMessage(code: CommandRejectionCode?): String = when (code) {
    CommandRejectionCode.INVALID_ARGUMENT -> stringResource(R.string.rejection_invalid_argument)
    CommandRejectionCode.INVALID_STATE -> stringResource(R.string.rejection_invalid_state)
    CommandRejectionCode.UNKNOWN_CONTENT -> stringResource(R.string.rejection_unknown_content)
    CommandRejectionCode.LOCKED -> stringResource(R.string.rejection_locked)
    CommandRejectionCode.NOT_OWNED -> stringResource(R.string.rejection_not_owned)
    CommandRejectionCode.ALREADY_OWNED -> stringResource(R.string.rejection_already_owned)
    CommandRejectionCode.INSUFFICIENT_RESOURCE -> stringResource(R.string.rejection_insufficient_resource)
    CommandRejectionCode.CAPACITY_EXCEEDED -> stringResource(R.string.rejection_capacity_exceeded)
    CommandRejectionCode.COOLDOWN_ACTIVE -> stringResource(R.string.rejection_cooldown_active)
    CommandRejectionCode.NOT_READY -> stringResource(R.string.rejection_not_ready)
    CommandRejectionCode.ALREADY_CLAIMED -> stringResource(R.string.rejection_already_claimed)
    CommandRejectionCode.STALE_PREVIEW -> stringResource(R.string.rejection_stale_preview)
    CommandRejectionCode.UNSUPPORTED -> stringResource(R.string.rejection_unsupported)
    null -> stringResource(R.string.world_feedback_unknown)
}
