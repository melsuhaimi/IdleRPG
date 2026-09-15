package com.idlerpg.game.ui.screen.progress

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.idlerpg.game.R
import com.idlerpg.game.core.id.ContentId
import com.idlerpg.game.domain.command.CommandRejectionCode
import com.idlerpg.game.domain.event.ChroniclePersistScope
import com.idlerpg.game.domain.event.ChronicleResetScope
import com.idlerpg.game.presentation.intent.ProgressUiIntent
import com.idlerpg.game.presentation.model.AchievementProgressUiState
import com.idlerpg.game.presentation.model.ChroniclePreviewUiState
import com.idlerpg.game.presentation.model.ChronicleProgressUiState
import com.idlerpg.game.presentation.model.CoreGrowthTrackUiState
import com.idlerpg.game.presentation.model.EchoOfferEffectKind
import com.idlerpg.game.presentation.model.EchoOfferStatus
import com.idlerpg.game.presentation.model.EchoOfferUiState
import com.idlerpg.game.presentation.model.EchoShopUiState
import com.idlerpg.game.presentation.model.MasteryProgressUiState
import com.idlerpg.game.presentation.model.ObjectiveProgressUiState
import com.idlerpg.game.presentation.model.PersistentDiscoveryKind
import com.idlerpg.game.presentation.model.PersistentDiscoveryUiState
import com.idlerpg.game.presentation.model.ProgressClaimStatus
import com.idlerpg.game.presentation.model.ProgressFeedbackKind
import com.idlerpg.game.presentation.model.ProgressFeedbackUiState
import com.idlerpg.game.presentation.model.ProgressNextGoalKind
import com.idlerpg.game.presentation.model.ProgressNextGoalUiState
import com.idlerpg.game.presentation.model.ProgressRewardUiState
import com.idlerpg.game.presentation.model.ProgressUiState
import com.idlerpg.game.presentation.model.PowerScoreUiState
import com.idlerpg.game.presentation.model.QuestProgressUiState
import com.idlerpg.game.presentation.model.StatOverviewUiState
import com.idlerpg.game.ui.content.drawableResId
import com.idlerpg.game.ui.content.stringResId
import com.idlerpg.game.ui.component.premium.GameButton
import com.idlerpg.game.ui.component.premium.GameCard
import com.idlerpg.game.ui.component.premium.GameDivider
import com.idlerpg.game.ui.component.premium.GameMetricChip
import com.idlerpg.game.ui.component.premium.GameOutlinedButton
import com.idlerpg.game.ui.component.premium.GameProgressBar
import com.idlerpg.game.ui.component.premium.GameSectionHeader
import com.idlerpg.game.ui.component.premium.GameStatusPill
import com.idlerpg.game.ui.component.premium.PremiumPanel
import com.idlerpg.game.ui.navigation.ProgressDestination
import com.idlerpg.game.ui.motion.eventFeedbackPulse

/** Growth surface. Every mutation remains an explicit player intent. */
@Composable
fun ProgressScreen(
    state: ProgressUiState,
    destination: ProgressDestination,
    onIntent: (ProgressUiIntent) -> Unit,
    onDismissChroniclePreview: () -> Unit,
    modifier: Modifier = Modifier
) {
    var purchaseOptionIndex by rememberSaveable { mutableIntStateOf(0) }
    val useTwoColumns = LocalConfiguration.current.screenWidthDp >= 520 && LocalDensity.current.fontScale < 1.3f
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            ProgressHeader(destination)
        }
        state.feedback?.let { feedback ->
            item {
                ProgressFeedbackCard(
                    feedback = feedback,
                    state = state
                )
            }
        }

        when (destination) {
            ProgressDestination.OVERVIEW -> {
                item { OverviewCard(state, useTwoColumns = useTwoColumns) }
                item {
                    SectionTitle(
                        title = stringResource(R.string.progress_unlocked_features),
                        subtitle = stringResource(R.string.progress_unlocked_features_subtitle)
                    )
                }
                if (state.overview.unlockedFeatures.isEmpty()) {
                    item { EmptyCard(stringResource(R.string.progress_no_unlocked_features)) }
                } else {
                    items(
                        items = state.overview.unlockedFeatures,
                        key = { it.featureId.value }
                    ) { feature ->
                        CompactContentCard(
                            title = stringResource(feature.titleStringKey.stringResId()),
                            iconRes = feature.iconAssetKey.drawableResId()
                        )
                    }
                }
            }
            ProgressDestination.CORE_GROWTH -> {
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        SectionTitle(
                            title = stringResource(R.string.progress_core_growth_title),
                            subtitle = stringResource(R.string.progress_core_growth_subtitle)
                        )
                        Text(
                            text = stringResource(
                                R.string.progress_core_growth_gold,
                                state.coreGrowthGoldDisplay
                            ),
                            color = MaterialTheme.colorScheme.secondary,
                            style = MaterialTheme.typography.titleMedium
                        )
                        val options = state.coreGrowth.firstOrNull()?.purchaseOptions.orEmpty()
                        if (options.isNotEmpty()) {
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                options.forEachIndexed { index, option ->
                                    if (purchaseOptionIndex == index) {
                                        GameButton(
                                            onClick = { purchaseOptionIndex = index },
                                            modifier = Modifier.widthIn(min = 84.dp)
                                        ) {
                                            Text(option.quantityLabel, maxLines = 1)
                                        }
                                    } else {
                                        GameOutlinedButton(
                                            onClick = { purchaseOptionIndex = index },
                                            modifier = Modifier.widthIn(min = 84.dp)
                                        ) {
                                            Text(option.quantityLabel, maxLines = 1)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                if (useTwoColumns) {
                    items(state.coreGrowth.chunked(2), key = { row -> row.joinToString("|") { it.upgradeId.value } }) { row ->
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            row.forEach { track ->
                                CoreGrowthCard(track, purchaseOptionIndex, Modifier.weight(1f)) { quantity ->
                                    onIntent(ProgressUiIntent.PurchaseCoreGrowth(track.upgradeId, quantity))
                                }
                            }
                            if (row.size == 1) androidx.compose.foundation.layout.Spacer(Modifier.weight(1f))
                        }
                    }
                } else {
                    items(state.coreGrowth, key = { it.upgradeId.value }) { track ->
                        CoreGrowthCard(track, purchaseOptionIndex) { quantity ->
                            onIntent(ProgressUiIntent.PurchaseCoreGrowth(track.upgradeId, quantity))
                        }
                    }
                }
            }
            ProgressDestination.MASTERY -> {
                item {
                    SectionTitle(
                        title = stringResource(R.string.progress_mastery_title),
                        subtitle = stringResource(R.string.progress_mastery_subtitle)
                    )
                }
                if (useTwoColumns) {
                    items(state.masteries.chunked(2), key = { row -> row.joinToString("|") { it.masteryId.value } }) { row ->
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            row.forEach { mastery -> MasteryCard(mastery, Modifier.weight(1f)) }
                            if (row.size == 1) androidx.compose.foundation.layout.Spacer(Modifier.weight(1f))
                        }
                    }
                } else {
                    items(state.masteries, key = { it.masteryId.value }) { mastery -> MasteryCard(mastery) }
                }
            }
            ProgressDestination.QUESTS -> {
                item {
                    SectionTitle(
                        title = stringResource(R.string.progress_quests_title),
                        subtitle = stringResource(R.string.progress_quests_subtitle)
                    )
                }
                if (state.quests.isEmpty()) {
                    item { EmptyCard(stringResource(R.string.progress_no_quests)) }
                } else {
                    items(
                        items = state.quests,
                        key = { it.questId.value }
                    ) { quest ->
                        QuestCard(
                            quest = quest,
                            onClaim = {
                                onIntent(ProgressUiIntent.ClaimQuest(quest.questId))
                            }
                        )
                    }
                }
            }
            ProgressDestination.ACHIEVEMENTS -> {
                item {
                    SectionTitle(
                        title = stringResource(R.string.progress_achievements_title),
                        subtitle = stringResource(R.string.progress_achievements_subtitle)
                    )
                }
                if (state.achievements.isEmpty()) {
                    item { EmptyCard(stringResource(R.string.progress_no_achievements)) }
                } else {
                    items(
                        items = state.achievements,
                        key = { it.achievementId.value }
                    ) { achievement ->
                        AchievementCard(
                            achievement = achievement,
                            onClaim = {
                                onIntent(
                                    ProgressUiIntent.ClaimAchievement(
                                        achievement.achievementId
                                    )
                                )
                            }
                        )
                    }
                }
            }
            ProgressDestination.DISCOVERIES -> {
                item {
                    SectionTitle(
                        title = stringResource(R.string.progress_discoveries_title),
                        subtitle = stringResource(R.string.progress_discoveries_subtitle)
                    )
                }
                if (state.discoveries.isEmpty()) {
                    item { EmptyCard(stringResource(R.string.progress_no_discoveries)) }
                } else {
                    items(
                        items = state.discoveries,
                        key = { "${it.kind}:${it.contentId.value}" }
                    ) { discovery ->
                        DiscoveryCard(discovery)
                    }
                }
            }
            ProgressDestination.ECHO -> {
                item {
                    EchoBalanceCard(state.echoShop)
                }
                item {
                    SectionTitle(
                        title = stringResource(R.string.progress_echo_shop_title),
                        subtitle = stringResource(R.string.progress_echo_shop_subtitle)
                    )
                }
                if (state.echoShop.offers.isEmpty()) {
                    item { EmptyCard(stringResource(R.string.progress_no_echo_offers)) }
                } else {
                    items(
                        items = state.echoShop.offers,
                        key = { it.offerId.value }
                    ) { offer ->
                        EchoOfferCard(
                            offer = offer,
                            onPurchase = {
                                onIntent(ProgressUiIntent.PurchaseEcho(offer.offerId))
                            }
                        )
                    }
                }
            }
            ProgressDestination.CHRONICLE -> {
                item {
                    ChronicleCard(
                        chronicle = state.chronicle,
                        previewRequestPending = state.chroniclePreviewRequestPending,
                        onRequestPreview = {
                            onIntent(ProgressUiIntent.RequestChronicle)
                        }
                    )
                }
                if (state.chronicle.bestMilestones.isNotEmpty()) {
                    item {
                        SectionTitle(
                            title = stringResource(R.string.progress_chronicle_milestones),
                            subtitle = stringResource(R.string.progress_chronicle_milestones_subtitle)
                        )
                    }
                    items(
                        items = state.chronicle.bestMilestones,
                        key = { it.milestoneId.value }
                    ) { milestone ->
                        CompactContentCard(
                            title = stringResource(milestone.titleStringKey.stringResId()),
                            iconRes = milestone.iconAssetKey.drawableResId()
                        )
                    }
                }
            }
        }

    }

    state.chroniclePreview?.let { preview ->
        ChroniclePreviewDialog(
            preview = preview,
            commitPending = state.chronicleCommitPending,
            onDismiss = onDismissChroniclePreview,
            onCommit = {
                onIntent(ProgressUiIntent.ConfirmChronicleCollapse)
            }
        )
    }
}

@Composable
private fun ProgressHeader(destination: ProgressDestination) {
    GameSectionHeader(
        eyebrow = stringResource(R.string.nav_progress),
        title = stringResource(destination.labelResId),
        subtitle = if (destination == ProgressDestination.OVERVIEW) {
            stringResource(R.string.progress_fui08_subtitle)
        } else {
            null
        }
    )
}

@Composable
private fun OverviewCard(state: ProgressUiState, useTwoColumns: Boolean) {
    val overview = state.overview
    var expandedStatId by rememberSaveable { mutableStateOf<String?>(null) }
    PremiumPanel(
        backgroundResId = R.drawable.panel_secondary_premium,
        modifier = Modifier.fillMaxWidth(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(20.dp),
        verticalSpacing = 12.dp,
        accent = com.idlerpg.game.ui.theme.ResourceGold
    ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = stringResource(
                            R.string.progress_player_level_format,
                            overview.playerLevel
                        ),
                        style = MaterialTheme.typography.titleLarge
                    )
                    Text(
                        text = if (overview.atMaximumPlayerLevel) {
                            stringResource(R.string.progress_player_max_level)
                        } else {
                            stringResource(
                                R.string.progress_player_xp_format,
                                overview.currentExperienceDisplay,
                                overview.experienceToNextLevelDisplay
                            )
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                GameStatusPill(
                    text = if (overview.chronicleEligible) {
                        stringResource(R.string.progress_chronicle_ready)
                    } else {
                        stringResource(R.string.progress_chronicle_not_ready)
                    },
                    accent = if (overview.chronicleEligible) {
                        MaterialTheme.colorScheme.secondary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
            }
            GameProgressBar(
                progressUnits = overview.experienceProgressUnits,
                accent = com.idlerpg.game.ui.theme.ResourceGold,
                modifier = Modifier.fillMaxWidth(),
                contentDescriptionText = stringResource(
                    R.string.progress_player_xp_format,
                    overview.currentExperienceDisplay,
                    overview.experienceToNextLevelDisplay
                )
            )
            NextGoalPanel(goal = overview.nextGoal)
            PowerScorePanel(score = overview.powerScore)
            GameDivider()
            Text(
                text = stringResource(R.string.progress_combat_readout),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.secondary
            )
            overview.statCards.chunked(if (useTwoColumns) 2 else 1).forEach { rowStats ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    rowStats.forEach { stat ->
                        StatInfoTile(
                            stat = stat,
                            expanded = expandedStatId == stat.id,
                            onClick = {
                                expandedStatId = if (expandedStatId == stat.id) null else stat.id
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    if (useTwoColumns) {
                        repeat(2 - rowStats.size) { Spacer(Modifier.weight(1f)) }
                    }
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                GameMetricChip(
                    label = stringResource(R.string.progress_echo_balance_title),
                    value = overview.echoAvailableDisplay,
                    accent = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.weight(1f)
                )
                GameMetricChip(
                    label = stringResource(R.string.progress_chronicle),
                    value = stringResource(
                        R.string.progress_chronicle_progress_format,
                        state.chronicle.currentNormalClearsDisplay,
                        state.chronicle.requiredNormalClearsDisplay
                    ),
                    accent = MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier.weight(1f)
                )
            }
    }
}

@Composable
private fun PowerScorePanel(score: PowerScoreUiState) {
    GameCard(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = com.idlerpg.game.ui.theme.ObsidianSurface2.copy(alpha = 0.78f)
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = "POWER SCORE  " + score.totalDisplay,
                style = MaterialTheme.typography.titleMedium,
                color = com.idlerpg.game.ui.theme.ResourceGold
            )
            score.components.forEach { component ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(component.label, style = MaterialTheme.typography.labelSmall)
                    Text(component.valueDisplay, style = MaterialTheme.typography.labelMedium)
                }
                Text(
                    component.formula,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = "Expected hit " + score.expectedBasicAttackDamageDisplay +
                    " · Effective health " + score.effectiveHealthDisplay,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.secondary
            )
        }
    }
}

@Composable
private fun NextGoalPanel(goal: ProgressNextGoalUiState) {
    val targetTitle = goal.titleStringKey?.let { stringResource(it.stringResId()) }
    GameCard(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = com.idlerpg.game.ui.theme.ObsidianSurface2.copy(alpha = 0.78f)),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            com.idlerpg.game.ui.theme.ResourceGold.copy(alpha = 0.42f)
        ),
        accent = com.idlerpg.game.ui.theme.ResourceGold
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = stringResource(R.string.progress_next_goal_title),
                style = MaterialTheme.typography.labelSmall,
                color = com.idlerpg.game.ui.theme.ResourceGold
            )
            Text(
                text = targetTitle ?: stringResource(R.string.progress_core_growth_mastered),
                style = MaterialTheme.typography.titleMedium
            )
            when (goal.kind) {
                ProgressNextGoalKind.MASTERY_UNLOCK -> Text(
                    text = stringResource(
                        R.string.progress_mastery_unlock,
                        goal.requiredLevel,
                        targetTitle ?: stringResource(R.string.progress_goal_requirement_unknown)
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.secondary
                )
                ProgressNextGoalKind.CORE_MILESTONE -> Text(
                    text = stringResource(
                        R.string.progress_core_growth_milestone,
                        goal.requiredLevel,
                        goal.bonusLevels
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.secondary
                )
                ProgressNextGoalKind.COMPLETE -> Text(
                    text = stringResource(R.string.progress_core_growth_mastered),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.secondary
                )
            }
            if (goal.remainingLevels > 0L) {
                Text(
                    text = stringResource(
                        R.string.progress_goal_levels_remaining,
                        goal.remainingLevels
                    ),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (goal.requirements.isNotEmpty()) {
                Text(
                    text = stringResource(R.string.progress_next_goal_requirements),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                goal.requirements.forEach { requirement ->
                    val requirementTitle = requirement.titleStringKey?.let {
                        stringResource(it.stringResId())
                    } ?: stringResource(R.string.progress_goal_requirement_unknown)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = requirementTitle,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = "${requirement.currentLevel}/${requirement.requiredLevel}",
                            style = MaterialTheme.typography.labelMedium,
                            color = if (requirement.remainingLevels == 0L) {
                                MaterialTheme.colorScheme.secondary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StatInfoTile(
    stat: StatOverviewUiState,
    expanded: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    GameCard(
        onClick = onClick,
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = if (expanded) {
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.72f)
            } else {
                MaterialTheme.colorScheme.surface
            }
        ),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (expanded) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.outline.copy(alpha = 0.42f)
        ),
        accent = if (expanded) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Text(stat.label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
            Text(stat.valueDisplay, style = MaterialTheme.typography.titleMedium)
            Text(
                text = if (expanded) "${stat.description} ${stat.formula}" else stat.description,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = if (expanded) 4 else 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun CoreGrowthCard(
    track: CoreGrowthTrackUiState,
    purchaseOptionIndex: Int,
    modifier: Modifier = Modifier,
    onPurchase: (Long) -> Unit
) {
    GameCard(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        accent = MaterialTheme.colorScheme.tertiary
    ) {
        Column(
            modifier = Modifier.padding(11.dp),
            verticalArrangement = Arrangement.spacedBy(7.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    painter = painterResource(track.iconAssetKey.drawableResId()),
                    contentDescription = stringResource(track.titleStringKey.stringResId()),
                    tint = MaterialTheme.colorScheme.secondary
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(track.titleStringKey.stringResId()),
                        style = MaterialTheme.typography.titleSmall
                    )
                    Text(
                        text = stringResource(R.string.progress_core_growth_level, track.level),
                        style = MaterialTheme.typography.labelLarge
                    )
                    Text(
                        text = stringResource(
                            R.string.progress_core_growth_effect,
                            track.currentEffectDisplay
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            if (track.legacyStartingLevel > 0L) Text(
                stringResource(R.string.progress_legacy_training_start, track.legacyStartingLevel),
                style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary
            )
            Text(
                text = if (track.nextMilestoneLevel != null) {
                    stringResource(
                        R.string.progress_core_growth_milestone,
                        track.nextMilestoneLevel,
                        track.nextMilestoneBonusLevels ?: 0L
                    )
                } else {
                    stringResource(R.string.progress_core_growth_mastered)
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.secondary
            )
            track.purchaseOptions.getOrNull(purchaseOptionIndex)?.let { option ->
                GameButton(
                    onClick = { onPurchase(option.quantity) }, enabled = option.enabled,
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(5.dp)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(option.quantityLabel)
                        Text(stringResource(R.string.progress_core_growth_cost, option.costDisplay), style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }
    }
}

@Composable
private fun MasteryCard(mastery: MasteryProgressUiState, modifier: Modifier = Modifier) {
    GameCard(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier.padding(11.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                painter = painterResource(mastery.iconAssetKey.drawableResId()),
                contentDescription = stringResource(mastery.titleStringKey.stringResId()),
                tint = MaterialTheme.colorScheme.secondary
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Text(
                    text = stringResource(mastery.affinityTitleStringKey.stringResId()),
                    style = MaterialTheme.typography.titleSmall
                )
                Text(
                    text = stringResource(
                        R.string.progress_mastery_level_format,
                        mastery.level,
                        mastery.maximumLevel?.toString() ?: stringResource(R.string.progress_unbounded)
                    ),
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = if (mastery.atMaximumLevel) {
                        stringResource(R.string.progress_mastery_maximum)
                    } else {
                        stringResource(
                            R.string.progress_mastery_to_next_format,
                            mastery.experienceToNextLevelDisplay
                        )
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                mastery.unlocks.forEach { unlock ->
                    Text(
                        stringResource(R.string.progress_mastery_unlock, unlock.requiredLevel, stringResource(unlock.titleStringKey.stringResId())),
                        style = MaterialTheme.typography.bodySmall,
                        color = if (mastery.level >= unlock.requiredLevel) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (mastery.unlocks.isNotEmpty()) Text(stringResource(R.string.progress_mastery_other_requirements), style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

@Composable
private fun QuestCard(
    quest: QuestProgressUiState,
    onClaim: () -> Unit
) {
    ClaimCard(
        title = stringResource(quest.titleStringKey.stringResId()),
        iconRes = quest.iconAssetKey.drawableResId(),
        status = quest.status,
        objectives = quest.objectives,
        reward = quest.reward,
        footer = if (quest.repeatable) stringResource(R.string.progress_contract_cycles, quest.pendingCountDisplay)
        else stringResource(
            R.string.progress_quest_claim_counts_format,
            quest.claimedCountDisplay,
            quest.completionCountDisplay
        ),
        canClaim = quest.canClaim,
        onClaim = onClaim
    )
}

@Composable
private fun AchievementCard(
    achievement: AchievementProgressUiState,
    onClaim: () -> Unit
) {
    ClaimCard(
        title = stringResource(achievement.titleStringKey.stringResId()),
        iconRes = achievement.iconAssetKey.drawableResId(),
        status = achievement.status,
        objectives = achievement.objectives,
        reward = achievement.reward,
        footer = stringResource(R.string.progress_achievement_persistent),
        canClaim = achievement.canClaim,
        onClaim = onClaim
    )
}

@Composable
private fun ClaimCard(
    title: String,
    iconRes: Int,
    status: ProgressClaimStatus,
    objectives: List<ObjectiveProgressUiState>,
    reward: ProgressRewardUiState,
    footer: String,
    canClaim: Boolean,
    onClaim: () -> Unit
) {
    GameCard(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    painter = painterResource(iconRes),
                    contentDescription = title,
                    tint = MaterialTheme.colorScheme.secondary
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = title, style = MaterialTheme.typography.titleMedium)
                    Text(
                        text = claimStatusLabel(status),
                        style = MaterialTheme.typography.labelLarge,
                        color = claimStatusColor(status)
                    )
                }
            }

            objectives.forEach { objective ->
                ObjectiveRow(objective)
            }

            RewardPreview(reward)
            Text(
                text = footer,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (canClaim) GameButton(onClick = onClaim, modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = when (status) {
                        ProgressClaimStatus.READY_TO_CLAIM -> stringResource(R.string.progress_claim_reward)
                        ProgressClaimStatus.CLAIMED -> stringResource(R.string.progress_claimed)
                        ProgressClaimStatus.LOCKED -> stringResource(R.string.progress_locked)
                        ProgressClaimStatus.IN_PROGRESS -> stringResource(R.string.progress_in_progress)
                    }
                )
            } else Text(claimStatusLabel(status), style = MaterialTheme.typography.labelMedium, color = claimStatusColor(status))
        }
    }
}

@Composable
private fun ObjectiveRow(objective: ObjectiveProgressUiState) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = stringResource(objective.titleStringKey.stringResId()),
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = stringResource(
                    R.string.progress_objective_count_format,
                    objective.currentDisplay,
                    objective.requiredDisplay
                ),
                style = MaterialTheme.typography.labelMedium
            )
        }
        GameProgressBar(
            progressUnits = objective.progressUnits,
            accent = com.idlerpg.game.ui.theme.PositiveGreen,
            modifier = Modifier.fillMaxWidth(),
            contentDescriptionText = stringResource(
                R.string.progress_objective_count_format,
                objective.currentDisplay,
                objective.requiredDisplay
            )
        )
    }
}

@Composable
private fun RewardPreview(reward: ProgressRewardUiState) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            text = stringResource(R.string.progress_reward_preview),
            style = MaterialTheme.typography.labelLarge
        )
        if (reward.isEmpty) {
            Text(
                text = stringResource(R.string.progress_reward_none),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            reward.goldDisplay?.let {
                Text(stringResource(R.string.progress_reward_gold_format, it))
            }
            reward.experienceDisplay?.let {
                Text(stringResource(R.string.progress_reward_xp_format, it))
            }
            if (reward.lootTableCount > 0) {
                Text(
                    stringResource(
                        R.string.progress_reward_loot_tables_format,
                        reward.lootTableCount
                    )
                )
            }
        }
    }
}


@Composable
private fun DiscoveryCard(discovery: PersistentDiscoveryUiState) {
    GameCard(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                painter = painterResource(discovery.iconAssetKey.drawableResId()),
                contentDescription = stringResource(discovery.titleStringKey.stringResId()),
                tint = MaterialTheme.colorScheme.secondary
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(discovery.titleStringKey.stringResId()),
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = discoveryKindLabel(discovery.kind),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun EchoBalanceCard(echoShop: EchoShopUiState) {
    PremiumPanel(
        backgroundResId = R.drawable.panel_secondary_premium,
        modifier = Modifier.fillMaxWidth(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
        verticalSpacing = 8.dp
    ) {
            Text(
                text = stringResource(R.string.progress_echo_balance_title),
                style = MaterialTheme.typography.titleLarge
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                GameMetricChip(
                    label = stringResource(R.string.progress_echo_balance_title),
                    value = echoShop.availableDisplay,
                    accent = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.weight(1f)
                )
                GameMetricChip(
                    label = stringResource(R.string.progress_echo_spent_label),
                    value = echoShop.spentDisplay,
                    accent = MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier.weight(1f)
                )
            }
            Text(
                stringResource(
                    R.string.progress_echo_lifetime_format,
                    echoShop.lifetimeEarnedDisplay
                )
            )
            Text(
                text = stringResource(R.string.progress_echo_balance_note),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
    }
}

@Composable
private fun EchoOfferCard(
    offer: EchoOfferUiState,
    onPurchase: () -> Unit
) {
    GameCard(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    painter = painterResource(offer.iconAssetKey.drawableResId()),
                    contentDescription = stringResource(offer.titleStringKey.stringResId()),
                    tint = MaterialTheme.colorScheme.secondary
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(offer.titleStringKey.stringResId()),
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = echoOfferStatusLabel(offer.status),
                        style = MaterialTheme.typography.labelLarge,
                        color = if (offer.status == EchoOfferStatus.AFFORDABLE) {
                            MaterialTheme.colorScheme.secondary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                }
                Text(
                    text = stringResource(
                        R.string.progress_echo_cost_format,
                        offer.costDisplay
                    ),
                    style = MaterialTheme.typography.labelLarge
                )
            }

            Text(stringResource(offer.descriptionStringKey.stringResId()), style = MaterialTheme.typography.bodyMedium)
            Text(
                text = stringResource(R.string.progress_echo_effects_title),
                style = MaterialTheme.typography.labelLarge
            )
            offer.effects.forEach { effect ->
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        painter = painterResource(effect.iconAssetKey.drawableResId()),
                        contentDescription = stringResource(effect.titleStringKey.stringResId()),
                        tint = MaterialTheme.colorScheme.tertiary
                    )
                    Text(
                        text = when (effect.kind) {
                            EchoOfferEffectKind.REVEAL_HIDDEN_CONTENT -> stringResource(
                                R.string.progress_echo_effect_reveal_format,
                                stringResource(effect.titleStringKey.stringResId())
                            )
                            EchoOfferEffectKind.UNLOCK_PERSISTENT_FEATURE -> stringResource(
                                R.string.progress_echo_effect_unlock_format,
                                stringResource(effect.titleStringKey.stringResId())
                            )
                        },
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            GameButton(
                onClick = onPurchase,
                enabled = offer.canPurchase,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = when (offer.status) {
                        EchoOfferStatus.AFFORDABLE -> stringResource(R.string.progress_echo_purchase)
                        EchoOfferStatus.PURCHASED -> stringResource(R.string.progress_echo_purchased)
                        EchoOfferStatus.INSUFFICIENT_ECHO -> stringResource(R.string.progress_echo_insufficient)
                        EchoOfferStatus.LOCKED_BY_PREREQUISITE -> stringResource(R.string.progress_echo_prerequisite_locked)
                    }
                )
            }
        }
    }
}

@Composable
private fun ChronicleCard(
    chronicle: ChronicleProgressUiState,
    previewRequestPending: Boolean,
    onRequestPreview: () -> Unit
) {
    PremiumPanel(
        backgroundResId = R.drawable.bg_training_hollow_warden_core,
        modifier = Modifier.fillMaxWidth(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
        verticalSpacing = 10.dp
    ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    painter = painterResource(chronicle.iconAssetKey.drawableResId()),
                    contentDescription = stringResource(chronicle.titleStringKey.stringResId()),
                    tint = MaterialTheme.colorScheme.secondary
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(chronicle.titleStringKey.stringResId()),
                        style = MaterialTheme.typography.titleLarge
                    )
                    Text(
                        text = stringResource(
                            R.string.progress_chronicle_number_format,
                            chronicle.currentChronicleNumber
                        ),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                GameStatusPill(
                    text = if (chronicle.eligible) {
                        stringResource(R.string.progress_chronicle_ready)
                    } else {
                        stringResource(R.string.progress_chronicle_not_ready)
                    },
                    accent = if (chronicle.eligible) {
                        MaterialTheme.colorScheme.secondary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
            }

            Text(
                stringResource(
                    R.string.progress_chronicle_completed_format,
                    chronicle.completedChroniclesDisplay
                )
            )
            Text(
                stringResource(
                    R.string.progress_chronicle_clears_format,
                    chronicle.currentNormalClearsDisplay,
                    chronicle.requiredNormalClearsDisplay
                )
            )
            chronicle.requiredBossTitle?.let { title ->
                Text(stringResource(
                    if (chronicle.requiredBossDefeated) R.string.progress_boss_complete else R.string.progress_boss_required,
                    stringResource(title.stringResId())
                ), color = if (chronicle.requiredBossDefeated) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text(
                stringResource(
                    R.string.progress_chronicle_reward_format,
                    chronicle.echoRewardDisplay
                )
            )
            Text(
                text = stringResource(R.string.progress_chronicle_reset_warning),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            GameButton(
                onClick = onRequestPreview,
                enabled = chronicle.eligible && !previewRequestPending,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    if (previewRequestPending) {
                        stringResource(R.string.progress_chronicle_preview_pending)
                    } else {
                        stringResource(R.string.progress_chronicle_preview)
                    }
                )
            }
    }
}

@Composable
private fun ChroniclePreviewDialog(
    preview: ChroniclePreviewUiState,
    commitPending: Boolean,
    onDismiss: () -> Unit,
    onCommit: () -> Unit
) {
    AlertDialog(
        onDismissRequest = {
            if (!commitPending) onDismiss()
        },
        title = {
            Text(
                stringResource(
                    R.string.progress_chronicle_preview_title_format,
                    preview.chronicleNumber
                )
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .heightIn(max = 360.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    stringResource(
                        R.string.progress_chronicle_preview_rule_format,
                        preview.resetRuleVersion
                    )
                )
                Text(
                    stringResource(
                        R.string.progress_chronicle_preview_clears_format,
                        preview.currentNormalClearsDisplay,
                        preview.requiredNormalClearsDisplay
                    )
                )
                Text(
                    stringResource(
                        R.string.progress_chronicle_preview_grant_format,
                        preview.echoRewardDisplay
                    ),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.secondary
                )
                Text(
                    text = stringResource(R.string.progress_chronicle_will_reset),
                    style = MaterialTheme.typography.labelLarge
                )
                preview.resetScopes.forEach { scope ->
                    Text("• ${chronicleResetScopeLabel(scope)}")
                }
                Text(
                    text = stringResource(R.string.progress_chronicle_will_persist),
                    style = MaterialTheme.typography.labelLarge
                )
                preview.persistScopes.forEach { scope ->
                    Text("• ${chroniclePersistScopeLabel(scope)}")
                }
                Text(
                    text = stringResource(R.string.progress_chronicle_preview_pause_note),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            GameButton(
                onClick = onCommit,
                enabled = !commitPending
            ) {
                Text(
                    if (commitPending) {
                        stringResource(R.string.progress_chronicle_committing)
                    } else {
                        stringResource(R.string.progress_chronicle_commit)
                    }
                )
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !commitPending
            ) {
                Text(stringResource(R.string.progress_chronicle_cancel))
            }
        }
    )
}

@Composable
private fun discoveryKindLabel(kind: PersistentDiscoveryKind): String = when (kind) {
    PersistentDiscoveryKind.CONVERGENCE -> stringResource(R.string.progress_discovery_convergence)
    PersistentDiscoveryKind.MUTATION -> stringResource(R.string.progress_discovery_mutation)
    PersistentDiscoveryKind.ENEMY_KNOWLEDGE -> stringResource(R.string.progress_discovery_enemy)
    PersistentDiscoveryKind.HIDDEN_CONTENT -> stringResource(R.string.progress_discovery_hidden)
}

@Composable
private fun echoOfferStatusLabel(status: EchoOfferStatus): String = when (status) {
    EchoOfferStatus.LOCKED_BY_PREREQUISITE -> stringResource(R.string.progress_echo_prerequisite_locked)
    EchoOfferStatus.AFFORDABLE -> stringResource(R.string.progress_echo_affordable)
    EchoOfferStatus.INSUFFICIENT_ECHO -> stringResource(R.string.progress_echo_insufficient)
    EchoOfferStatus.PURCHASED -> stringResource(R.string.progress_echo_purchased)
}

@Composable
private fun chronicleResetScopeLabel(scope: ChronicleResetScope): String = when (scope) {
    ChronicleResetScope.PLAYER -> stringResource(R.string.progress_chronicle_scope_player)
    ChronicleResetScope.COMBAT -> stringResource(R.string.progress_chronicle_scope_combat)
    ChronicleResetScope.WORLD -> stringResource(R.string.progress_chronicle_scope_world)
    ChronicleResetScope.ECONOMY -> stringResource(R.string.progress_chronicle_scope_economy)
    ChronicleResetScope.RESONANCE -> stringResource(R.string.progress_chronicle_scope_resonance)
    ChronicleResetScope.DOCTRINE -> stringResource(R.string.progress_chronicle_scope_doctrine)
    ChronicleResetScope.ADAPTATION -> stringResource(R.string.progress_chronicle_scope_adaptation)
    ChronicleResetScope.INVENTORY -> stringResource(R.string.progress_chronicle_scope_inventory)
    ChronicleResetScope.PROGRESSION -> stringResource(R.string.progress_chronicle_scope_progression)
    ChronicleResetScope.QUESTS -> stringResource(R.string.progress_chronicle_scope_quests)
    ChronicleResetScope.RUN_STATISTICS -> stringResource(R.string.progress_chronicle_scope_run_statistics)
}

@Composable
private fun chroniclePersistScopeLabel(scope: ChroniclePersistScope): String = when (scope) {
    ChroniclePersistScope.ENGINE_STATE -> stringResource(R.string.progress_chronicle_persist_engine)
    ChroniclePersistScope.CHRONICLE -> stringResource(R.string.progress_chronicle_persist_chronicle)
    ChroniclePersistScope.ECHOES -> stringResource(R.string.progress_chronicle_persist_echoes)
    ChroniclePersistScope.DISCOVERIES -> stringResource(R.string.progress_chronicle_persist_discoveries)
    ChroniclePersistScope.PERSISTENT_FEATURE_UNLOCKS -> stringResource(R.string.progress_chronicle_persist_features)
    ChroniclePersistScope.ACHIEVEMENTS -> stringResource(R.string.progress_chronicle_persist_achievements)
    ChroniclePersistScope.LIFETIME_STATISTICS -> stringResource(R.string.progress_chronicle_persist_lifetime)
}

@Composable
private fun ProgressFeedbackCard(
    feedback: ProgressFeedbackUiState,
    state: ProgressUiState
) {
    val message = when (feedback.kind) {
        ProgressFeedbackKind.COMMAND_REJECTED -> rejectionMessage(feedback.rejectionCode)
        ProgressFeedbackKind.QUEST_COMPLETED -> stringResource(R.string.progress_feedback_quest_ready)
        ProgressFeedbackKind.QUEST_REWARD_CLAIMED -> stringResource(R.string.progress_feedback_quest_claimed)
        ProgressFeedbackKind.ACHIEVEMENT_COMPLETED -> stringResource(R.string.progress_feedback_achievement_ready)
        ProgressFeedbackKind.ACHIEVEMENT_REWARD_CLAIMED -> stringResource(R.string.progress_feedback_achievement_claimed)
        ProgressFeedbackKind.PLAYER_LEVELED_UP -> stringResource(
            R.string.progress_feedback_level_up_format,
            feedback.amountDisplay ?: "?"
        )
        ProgressFeedbackKind.MASTERY_INCREASED -> stringResource(
            R.string.progress_feedback_mastery_format,
            affinityTitle(state, feedback.affinityId),
            feedback.amountDisplay ?: "0"
        )
        ProgressFeedbackKind.CORE_GROWTH_PURCHASED ->
            stringResource(R.string.progress_core_growth_feedback)
        ProgressFeedbackKind.ECHO_OFFER_PURCHASED -> stringResource(
            R.string.progress_feedback_echo_purchased,
            feedback.amountDisplay ?: "?"
        )
        ProgressFeedbackKind.DISCOVERY_UNLOCKED -> stringResource(
            R.string.progress_feedback_discovery_unlocked
        )
        ProgressFeedbackKind.ECHO_GRANTED -> stringResource(
            R.string.progress_feedback_echo_granted,
            feedback.amountDisplay ?: "?"
        )
        ProgressFeedbackKind.CHRONICLE_PREVIEW_READY -> stringResource(
            R.string.progress_feedback_chronicle_preview
        )
        ProgressFeedbackKind.CHRONICLE_COLLAPSED -> stringResource(
            R.string.progress_feedback_chronicle_collapsed,
            feedback.amountDisplay ?: "?"
        )
    }

    GameCard(
        modifier = Modifier
            .fillMaxWidth()
            .eventFeedbackPulse(feedback.sequenceNumber),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        )
    ) {
        Text(
            text = message,
            modifier = Modifier.padding(12.dp),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSecondaryContainer
        )
    }
}

@Composable
private fun affinityTitle(state: ProgressUiState, affinityId: ContentId?): String {
    val mastery = state.masteries.firstOrNull { it.affinityId == affinityId }
    return if (mastery == null) {
        stringResource(R.string.progress_affinity_generic)
    } else {
        stringResource(mastery.affinityTitleStringKey.stringResId())
    }
}

@Composable
private fun rejectionMessage(code: CommandRejectionCode?): String = when (code) {
    CommandRejectionCode.INVALID_ARGUMENT -> stringResource(R.string.progress_rejection_invalid_argument)
    CommandRejectionCode.INVALID_STATE -> stringResource(R.string.progress_rejection_invalid_state)
    CommandRejectionCode.UNKNOWN_CONTENT -> stringResource(R.string.progress_rejection_unknown_content)
    CommandRejectionCode.LOCKED -> stringResource(R.string.progress_rejection_locked)
    CommandRejectionCode.NOT_OWNED -> stringResource(R.string.progress_rejection_not_owned)
    CommandRejectionCode.ALREADY_OWNED -> stringResource(R.string.progress_rejection_already_owned)
    CommandRejectionCode.INSUFFICIENT_RESOURCE -> stringResource(R.string.progress_rejection_insufficient_resource)
    CommandRejectionCode.CAPACITY_EXCEEDED -> stringResource(R.string.progress_rejection_capacity)
    CommandRejectionCode.COOLDOWN_ACTIVE -> stringResource(R.string.progress_rejection_invalid_state)
    CommandRejectionCode.NOT_READY -> stringResource(R.string.progress_rejection_not_ready)
    CommandRejectionCode.ALREADY_CLAIMED -> stringResource(R.string.progress_rejection_already_claimed)
    CommandRejectionCode.STALE_PREVIEW -> stringResource(R.string.progress_rejection_stale_preview)
    CommandRejectionCode.UNSUPPORTED -> stringResource(R.string.progress_rejection_generic)
    null -> stringResource(R.string.progress_rejection_generic)
}

@Composable
private fun claimStatusLabel(status: ProgressClaimStatus): String = when (status) {
    ProgressClaimStatus.LOCKED -> stringResource(R.string.progress_locked)
    ProgressClaimStatus.IN_PROGRESS -> stringResource(R.string.progress_in_progress)
    ProgressClaimStatus.READY_TO_CLAIM -> stringResource(R.string.progress_ready_to_claim)
    ProgressClaimStatus.CLAIMED -> stringResource(R.string.progress_claimed)
}

@Composable
private fun claimStatusColor(status: ProgressClaimStatus) = when (status) {
    ProgressClaimStatus.READY_TO_CLAIM -> MaterialTheme.colorScheme.secondary
    ProgressClaimStatus.CLAIMED -> MaterialTheme.colorScheme.tertiary
    ProgressClaimStatus.LOCKED -> MaterialTheme.colorScheme.onSurfaceVariant
    ProgressClaimStatus.IN_PROGRESS -> MaterialTheme.colorScheme.primary
}

@Composable
private fun CompactContentCard(title: String, iconRes: Int) {
    GameCard(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                painter = painterResource(iconRes),
                contentDescription = title,
                tint = MaterialTheme.colorScheme.secondary
            )
            Text(text = title, style = MaterialTheme.typography.bodyLarge)
        }
    }
}

@Composable
private fun SectionTitle(title: String, subtitle: String) {
    GameSectionHeader(
        eyebrow = stringResource(R.string.nav_progress),
        title = title,
        subtitle = subtitle
    )
}

@Composable
private fun EmptyCard(message: String) {
    GameCard(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Text(
            text = message,
            modifier = Modifier.padding(14.dp),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
