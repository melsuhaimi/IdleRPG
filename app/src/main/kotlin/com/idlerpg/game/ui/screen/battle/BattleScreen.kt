package com.idlerpg.game.ui.screen.battle

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import com.idlerpg.game.R
import com.idlerpg.game.core.id.InstanceId
import com.idlerpg.game.domain.command.CommandRejectionCode
import com.idlerpg.game.domain.definition.enemy.EnemyRole
import com.idlerpg.game.domain.model.world.WorldAutomationMode
import com.idlerpg.game.presentation.content.PresentationAssetKey
import com.idlerpg.game.presentation.content.PresentationStringKey
import com.idlerpg.game.presentation.intent.BattleUiIntent
import com.idlerpg.game.presentation.model.BattleBossPhaseUi
import com.idlerpg.game.presentation.model.BattleCombatStatusUi
import com.idlerpg.game.presentation.model.BattleEnemyUiState
import com.idlerpg.game.presentation.model.BattleFeedbackKind
import com.idlerpg.game.presentation.model.BattleFeedbackUiState
import com.idlerpg.game.presentation.model.BattleImpactUiState
import com.idlerpg.game.presentation.model.BattlePlayerUiState
import com.idlerpg.game.presentation.model.BattlePresentationRewardUiState
import com.idlerpg.game.presentation.model.BattleRewardUiState
import com.idlerpg.game.presentation.model.BattleSkillReadinessUi
import com.idlerpg.game.presentation.model.BattleSkillUiState
import com.idlerpg.game.presentation.model.BattleStatusEffectUiState
import com.idlerpg.game.presentation.model.BattleStatusPolarityUi
import com.idlerpg.game.presentation.model.BattleUiState
import com.idlerpg.game.ui.component.SkillArtwork
import com.idlerpg.game.ui.component.battle.CombatHitFeedback
import com.idlerpg.game.ui.component.premium.GameButton
import com.idlerpg.game.ui.component.premium.GameOutlinedButton
import com.idlerpg.game.ui.content.drawableResId
import com.idlerpg.game.ui.content.stringResId
import com.idlerpg.game.ui.feedback.BattleHapticEffect
import com.idlerpg.game.ui.motion.eventFeedbackPulse
import com.idlerpg.game.ui.theme.ArcaneViolet
import com.idlerpg.game.ui.theme.ErrorRose
import com.idlerpg.game.ui.theme.ObsidianBackground
import com.idlerpg.game.ui.theme.ObsidianOutline
import com.idlerpg.game.ui.theme.ObsidianSurface1
import com.idlerpg.game.ui.theme.ObsidianSurface2
import com.idlerpg.game.ui.theme.ObsidianSurface3
import com.idlerpg.game.ui.theme.PositiveGreen
import com.idlerpg.game.ui.theme.ResourceGold
import com.idlerpg.game.ui.theme.ResonanceTeal
import com.idlerpg.game.ui.theme.TextPrimary
import com.idlerpg.game.ui.theme.TextSecondary
import com.idlerpg.game.ui.theme.GameDimensions
import com.idlerpg.game.ui.theme.affinityVisualToken
import com.idlerpg.game.ui.theme.LocalReducedMotion
import kotlinx.coroutines.delay

private val StageBorder = Color(0xFF4B5677)
private val StageFog = Color(0xFF9D8CFF)
private val StageGround = Color(0xFF0A0D18)
private val DisabledInk = Color(0xFF737A8D)
private const val BATTLE_REWARD_FLOAT_MILLIS = 2_500
private const val BATTLE_DEFEATED_SNAPSHOT_MILLIS = 560L
private const val BATTLE_VICTORY_SNAPSHOT_MILLIS = 760L

/**
 * Battle presentation only. Every gameplay value is projected by [BattleUiState]; this screen
 * does not simulate combat, estimate rewards, or predict an outcome.
 */
@Composable
fun BattleScreen(
    state: BattleUiState,
    hapticsEnabled: Boolean,
    onIntent: (BattleUiIntent) -> Unit,
    onOpenSkillLoadout: () -> Unit,
    onOpenBuild: (InstanceId?) -> Unit = {},
    onOpenDoctrine: () -> Unit = {},
    onSetDoctrineEnabled: (Boolean) -> Unit = {},
    onOpenFarm: () -> Unit = {},
    onConsumePresentationReward: (Long) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val compact = LocalConfiguration.current.screenHeightDp < 760 ||
        LocalConfiguration.current.screenWidthDp < 360
    var consumedRewardTokens by remember(state.combatSequenceId) {
        mutableStateOf<Set<Long>>(emptySet())
    }

    BattleHapticEffect(feedback = state.feedback, enabled = hapticsEnabled)

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(ObsidianBackground)
    ) {
        Image(
            painter = painterResource(state.backgroundAssetKey.drawableResId()),
            contentDescription = null,
            modifier = Modifier
                .fillMaxSize()
                .alpha(0.86f),
            contentScale = ContentScale.Crop
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xE9060811),
                            Color(0x35060811),
                            Color(0xD9060811)
                        )
                    )
                )
        )

        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = if (compact) 6.dp else 10.dp, vertical = 6.dp)
        ) {
            val lowerContentMinimumHeight = if (compact) 72.dp else 88.dp
            val battleHeaderMinimumHeight = if (compact) {
                GameDimensions.BattleHeaderMinHeight
            } else {
                GameDimensions.BattleHeaderMinHeight + 8.dp
            }
            val battleBudget = (
                maxHeight - battleHeaderMinimumHeight - lowerContentMinimumHeight
                ).coerceAtLeast(0.dp)
            val minimumBattleHeight = (if (compact) 260.dp else 300.dp)
                .coerceAtMost(battleBudget)
            val preferredBattleHeight = maxHeight * if (compact) 0.54f else 0.58f
            val battlefieldHeight = preferredBattleHeight.coerceIn(
                minimumBattleHeight,
                battleBudget
            )
            val presentationReward = state.presentationRewards.firstOrNull { reward ->
                reward.reward != null && reward.token !in consumedRewardTokens
            }
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(if (compact) 5.dp else 6.dp)
            ) {
                BattleHeader(
                    state = state,
                    onSetDoctrineEnabled = onSetDoctrineEnabled,
                    onRetreat = if (state.canRetreat) {
                        { onIntent(BattleUiIntent.Retreat) }
                    } else {
                        null
                    },
                    compact = compact
                )

                BattleStage(
                    state = state,
                    compact = compact,
                    onIntent = onIntent,
                    onOpenBuild = onOpenBuild,
                    onOpenDoctrine = onOpenDoctrine,
                    onOpenFarm = onOpenFarm,
                    onDeployStartingEncounter = {
                        onIntent(BattleUiIntent.DeployStartingEncounter)
                    },
                    presentationReward = presentationReward,
                    onRewardFinished = { token ->
                        consumedRewardTokens = consumedRewardTokens + token
                        onConsumePresentationReward(token)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(battlefieldHeight)
                )

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(if (compact) 5.dp else 6.dp)
                ) {
                    BattleCombatStatsRail(
                        player = state.player,
                        compact = compact,
                        modifier = Modifier.fillMaxWidth()
                    )
                    BattleCommandDock(
                        state = state,
                        compact = compact,
                        onIntent = onIntent,
                        onOpenSkillLoadout = onOpenSkillLoadout,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

@Composable
private fun BattleHeader(
    state: BattleUiState,
    onSetDoctrineEnabled: (Boolean) -> Unit,
    onRetreat: (() -> Unit)?,
    compact: Boolean
) {
    val regionTitle = state.regionTitleStringKey?.let { stringResource(it.stringResId()) }
        ?: stringResource(R.string.hud_no_region)
    val automationDescription = if (state.doctrine.enabled) {
        stringResource(R.string.battle_auto_battle_on_description)
    } else {
        stringResource(R.string.battle_auto_battle_off_description)
    }
    val automationStateDescription = if (state.doctrine.enabled) {
        stringResource(R.string.battle_auto_battle_on)
    } else {
        stringResource(R.string.battle_auto_battle_off)
    }
    val modeLabel = when (state.automationMode) {
        WorldAutomationMode.PUSH -> stringResource(R.string.battle_mode_push)
        WorldAutomationMode.FARM -> stringResource(R.string.battle_mode_farm)
    }
    val modeIcon = when (state.automationMode) {
        WorldAutomationMode.PUSH -> R.drawable.ic_mode_push
        WorldAutomationMode.FARM -> R.drawable.ic_mode_farm
    }
    val hasSelectedEncounter = state.encounterTitleStringKey != null
    val lifecycleLabel = when (state.combatStatus) {
        BattleCombatStatusUi.IDLE -> stringResource(
            if (hasSelectedEncounter) R.string.battle_status_ready
            else R.string.battle_status_standby
        )
        BattleCombatStatusUi.ACTIVE -> stringResource(R.string.battle_status_combat)
        BattleCombatStatusUi.VICTORY -> stringResource(R.string.battle_status_victory)
        BattleCombatStatusUi.DEFEAT -> stringResource(R.string.battle_status_defeat)
    }
    val lifecycleAccent = when (state.combatStatus) {
        BattleCombatStatusUi.IDLE -> if (hasSelectedEncounter) ResourceGold else TextSecondary
        BattleCombatStatusUi.ACTIVE -> ResonanceTeal
        BattleCombatStatusUi.VICTORY -> PositiveGreen
        BattleCombatStatusUi.DEFEAT -> ErrorRose
    }
    val stageTagline = when (state.combatStatus) {
        BattleCombatStatusUi.IDLE -> stringResource(
            if (hasSelectedEncounter) R.string.battle_ready_tagline
            else R.string.battle_standby_tagline
        )
        BattleCombatStatusUi.ACTIVE -> stringResource(R.string.battle_stage_tagline)
        BattleCombatStatusUi.VICTORY -> stringResource(R.string.battle_victory_tagline)
        BattleCombatStatusUi.DEFEAT -> stringResource(R.string.battle_defeat_tagline)
    }

    val shape = RoundedCornerShape(
        if (compact) GameDimensions.ActorNameplateRadius + 6.dp
        else GameDimensions.LargePanelRadius
    )
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = if (compact) 64.dp else GameDimensions.BattleHeaderMinHeight),
        shape = shape,
        color = ObsidianSurface1.copy(alpha = 0.90f),
        border = BorderStroke(1.dp, StageBorder.copy(alpha = 0.76f)),
        tonalElevation = 0.dp,
        shadowElevation = 6.dp
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(shape)
        ) {
            Image(
                painter = painterResource(R.drawable.hud_header_castle_generated),
                contentDescription = null,
                modifier = Modifier
                    .matchParentSize()
                    .alpha(0.38f),
                contentScale = ContentScale.Crop
            )
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(
                        Brush.horizontalGradient(
                            listOf(
                                Color(0xD2070D1A),
                                Color(0xA80A1528),
                                Color(0xD9070D1A)
                            )
                        )
                    )
            )
            Image(
                painter = painterResource(R.drawable.ui_panel_frame_generated),
                contentDescription = null,
                modifier = Modifier
                    .matchParentSize()
                    .alpha(0.54f),
                contentScale = ContentScale.FillBounds
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = if (compact) 64.dp else GameDimensions.BattleHeaderMinHeight + 8.dp)
                    .padding(horizontal = if (compact) 8.dp else 10.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(if (compact) 5.dp else 7.dp)
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        modifier = Modifier.size(if (compact) 36.dp else 42.dp),
                        shape = RoundedCornerShape(10.dp),
                        color = ResourceGold.copy(alpha = 0.13f),
                        border = BorderStroke(1.dp, ResourceGold.copy(alpha = 0.58f))
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_nav_battle),
                            contentDescription = null,
                            tint = ResourceGold,
                            modifier = Modifier.padding(if (compact) 8.dp else 9.dp)
                        )
                    }
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(1.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.battle_training_hall),
                            style = if (compact) MaterialTheme.typography.titleMedium
                            else MaterialTheme.typography.titleLarge,
                            color = TextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Clip
                        )
                        Text(
                            text = regionTitle.uppercase(),
                            style = MaterialTheme.typography.labelSmall.copy(
                                letterSpacing = 0.75.sp
                            ),
                            color = ResonanceTeal,
                            maxLines = 1,
                            overflow = TextOverflow.Clip
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            state.stageNumber?.let { stageNumber ->
                                Text(
                                    text = stringResource(
                                        R.string.battle_stage_format,
                                        stageNumber.toString()
                                    ),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = ResourceGold,
                                    maxLines = 1
                                )
                            }
                            Text(
                                text = if (state.combatStatus == BattleCombatStatusUi.ACTIVE) {
                                    stringResource(
                                        R.string.battle_wave_format,
                                        state.currentWave,
                                        state.totalWaves
                                    )
                                } else {
                                    lifecycleLabel
                                },
                                style = MaterialTheme.typography.labelSmall,
                                color = lifecycleAccent,
                                maxLines = 1
                            )
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = ResourceGold.copy(alpha = 0.12f),
                                border = BorderStroke(1.dp, ResourceGold.copy(alpha = 0.34f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                                ) {
                                    Icon(
                                        painter = painterResource(modeIcon),
                                        contentDescription = null,
                                        tint = ResourceGold,
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Text(
                                        text = modeLabel,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = ResourceGold,
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                        Text(
                            text = stageTagline,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 7.sp,
                                letterSpacing = 0.8.sp
                            ),
                            color = TextSecondary.copy(alpha = 0.82f),
                            maxLines = 1,
                            overflow = TextOverflow.Clip
                        )
                    }
                }

                Surface(
                    modifier = Modifier
                        .heightIn(min = if (compact) 44.dp else 48.dp)
                        .widthIn(min = if (compact) 80.dp else 96.dp)
                        .clickable(
                            role = Role.Button,
                            onClick = { onSetDoctrineEnabled(!state.doctrine.enabled) }
                        )
                        .semantics {
                            contentDescription = automationDescription
                            stateDescription = automationStateDescription
                        },
                    shape = RoundedCornerShape(10.dp),
                    color = if (state.doctrine.enabled) ResonanceTeal.copy(alpha = 0.16f)
                    else ObsidianSurface1.copy(alpha = 0.86f),
                    border = BorderStroke(
                        1.dp,
                        if (state.doctrine.enabled) ResonanceTeal.copy(alpha = 0.78f)
                        else ObsidianOutline.copy(alpha = 0.82f)
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Icon(
                            painter = painterResource(
                                if (state.doctrine.enabled) {
                                    R.drawable.ic_auto_battle_pause
                                } else {
                                    R.drawable.ic_auto_battle_play
                                }
                            ),
                            contentDescription = null,
                            tint = if (state.doctrine.enabled) ResonanceTeal else TextSecondary,
                            modifier = Modifier.size(if (compact) 18.dp else 20.dp)
                        )
                        Column(
                            verticalArrangement = Arrangement.spacedBy(1.dp),
                            horizontalAlignment = Alignment.Start
                        ) {
                            Text(
                                text = stringResource(R.string.battle_auto_battle_label),
                                style = MaterialTheme.typography.labelSmall,
                                color = TextPrimary,
                                maxLines = 1
                            )
                            Text(
                                text = if (state.doctrine.enabled) {
                                    stringResource(R.string.battle_auto_battle_on)
                                } else {
                                    stringResource(R.string.battle_auto_battle_off)
                                },
                                style = MaterialTheme.typography.titleSmall,
                                color = if (state.doctrine.enabled) ResonanceTeal else TextSecondary,
                                maxLines = 1
                            )
                        }
                    }
                }

                onRetreat?.let { retreat ->
                    val retreatDescription = stringResource(R.string.battle_retreat_description)
                    Surface(
                        modifier = Modifier
                            .heightIn(min = 44.dp)
                            .widthIn(min = if (compact) 44.dp else 60.dp)
                            .clickable(role = Role.Button, onClick = retreat)
                            .semantics { contentDescription = retreatDescription },
                        shape = RoundedCornerShape(10.dp),
                        color = ObsidianSurface1.copy(alpha = 0.86f),
                        border = BorderStroke(1.dp, ArcaneViolet.copy(alpha = 0.66f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = if (compact) 10.dp else 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.ic_retreat),
                                contentDescription = null,
                                tint = ArcaneViolet,
                                modifier = Modifier.size(18.dp)
                            )
                            if (!compact) {
                                Text(
                                    text = stringResource(R.string.battle_retreat),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextPrimary,
                                    maxLines = 1
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
private fun BattleStage(
    state: BattleUiState,
    compact: Boolean,
    onIntent: (BattleUiIntent) -> Unit,
    onOpenBuild: (InstanceId?) -> Unit,
    onOpenDoctrine: () -> Unit,
    onOpenFarm: () -> Unit,
    onDeployStartingEncounter: () -> Unit,
    presentationReward: BattlePresentationRewardUiState?,
    onRewardFinished: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val allLivingEnemies = state.enemies.filter { it.alive }
    val livingEnemies = allLivingEnemies
    val feedback = state.feedback
    val rewardAvailable = presentationReward?.reward != null &&
        state.combatStatus != BattleCombatStatusUi.VICTORY
    val showEvent = feedback != null && !rewardAvailable &&
        state.combatStatus != BattleCombatStatusUi.DEFEAT &&
        feedback.kind != BattleFeedbackKind.COMBAT_ENDED &&
        feedback.kind != BattleFeedbackKind.ENEMY_DEFEATED &&
        feedback.kind != BattleFeedbackKind.LOOT

    var previousEnemies by remember(state.combatSequenceId) {
        mutableStateOf<List<BattleEnemyUiState>>(emptyList())
    }
    var defeatedSnapshots by remember(state.combatSequenceId) {
        mutableStateOf<List<BattleEnemyUiState>>(emptyList())
    }
    var holdIncomingFormation by remember(state.combatSequenceId) {
        mutableStateOf(false)
    }
    val enemyIdentity = state.enemies
        .map { it.instanceId to it.alive }
        .sortedBy { it.first }
    LaunchedEffect(state.combatSequenceId, enemyIdentity) {
        val previousIds = previousEnemies.map { it.instanceId }.toSet()
        val currentById = state.enemies.associateBy { it.instanceId }
        val removed = previousEnemies.filter { previous ->
            previous.alive && currentById[previous.instanceId]?.alive != true
        }
        previousEnemies = state.enemies
        if (removed.isNotEmpty()) {
            val incomingFormation = state.enemies.any { it.instanceId !in previousIds }
            val snapshot = (
                defeatedSnapshots + removed.map { enemy ->
                    enemy.copy(
                        currentHealthDisplay = "0",
                        healthProgressUnits = 0,
                        alive = false
                    )
                }
                ).distinctBy { it.instanceId }
            defeatedSnapshots = snapshot
            holdIncomingFormation = incomingFormation
            delay(
                if (state.combatStatus == BattleCombatStatusUi.VICTORY) {
                    BATTLE_VICTORY_SNAPSHOT_MILLIS
                } else {
                    BATTLE_DEFEATED_SNAPSHOT_MILLIS
                }
            )
            if (defeatedSnapshots == snapshot) {
                defeatedSnapshots = emptyList()
                holdIncomingFormation = false
            }
        }
    }
    val currentEnemyById = state.enemies.associateBy { it.instanceId }
    val pendingDefeatSnapshot = previousEnemies.any { previous ->
        previous.alive && currentEnemyById[previous.instanceId]?.alive != true
    }
    val completionRevealReady = defeatedSnapshots.isEmpty() && !pendingDefeatSnapshot
    val displayLivingEnemies = if (holdIncomingFormation) emptyList() else livingEnemies
    val displayThreat = displayLivingEnemies.minByOrNull {
        it.nextAttackRemainingMillis ?: Long.MAX_VALUE
    }

    Box(
        modifier = modifier
            .clip(
                RoundedCornerShape(
                    if (compact) GameDimensions.ActorNameplateRadius + 8.dp
                    else GameDimensions.StageRadius
                )
            )
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0x7F0B1020),
                        Color(0xA60A0E1A),
                        Color(0xE6090B14)
                    )
                )
            )
            .background(
                Brush.radialGradient(
                    colors = listOf(StageFog.copy(alpha = 0.10f), Color.Transparent),
                    radius = 540f
                )
            )
            .border(
                BorderStroke(1.dp, StageBorder.copy(alpha = 0.68f)),
                RoundedCornerShape(
                    if (compact) GameDimensions.ActorNameplateRadius + 8.dp
                    else GameDimensions.StageRadius
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(state.backgroundAssetKey.drawableResId()),
            contentDescription = null,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    scaleX = 1.06f
                    scaleY = 1.06f
                    translationY = -8f
                }
                .alpha(0.58f),
            contentScale = ContentScale.Crop
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color(0x6B071021),
                            Color.Transparent,
                            Color(0xC90A0D17)
                        )
                    )
                )
        )
        Canvas(Modifier.fillMaxSize()) {
            val groundY = size.height * 0.82f
            drawLine(
                color = StageBorder.copy(alpha = 0.28f),
                start = androidx.compose.ui.geometry.Offset(size.width * 0.07f, groundY),
                end = androidx.compose.ui.geometry.Offset(size.width * 0.93f, groundY),
                strokeWidth = 1.5f,
                cap = StrokeCap.Round
            )
            drawOval(
                brush = Brush.radialGradient(
                    colors = listOf(StageGround.copy(alpha = 0.92f), Color.Transparent)
                ),
                topLeft = androidx.compose.ui.geometry.Offset(size.width * 0.08f, groundY - 22f),
                size = androidx.compose.ui.geometry.Size(size.width * 0.84f, 82f)
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = if (compact) 7.dp else 10.dp, vertical = 7.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.dp)
                    .background(
                        Brush.horizontalGradient(
                            listOf(
                                Color.Transparent,
                                ResonanceTeal.copy(alpha = 0.66f),
                                ResourceGold.copy(alpha = 0.72f),
                                Color.Transparent
                            )
                        ),
                        RoundedCornerShape(99.dp)
                    )
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(
                        when {
                            showEvent && compact -> 38.dp
                            showEvent -> 42.dp
                            else -> 4.dp
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (showEvent) {
                    BattleEventBanner(state = state, modifier = Modifier.fillMaxWidth())
                }
            }

            if (displayLivingEnemies.isEmpty() && defeatedSnapshots.isEmpty()) {
                BattleEmptyState(
                    state = state,
                    onDeployStartingEncounter = onDeployStartingEncounter,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                )
            } else {
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(if (compact) 3.dp else 6.dp)
                ) {
                    HeroStageActor(
                        player = state.player,
                        compact = compact,
                        modifier = Modifier.weight(0.96f)
                    )
                    BattleFocusDivider(compact = compact)
                    EnemyFormationStage(
                        enemies = displayLivingEnemies,
                        defeatedEnemies = defeatedSnapshots,
                        impacts = feedback?.impacts.orEmpty(),
                        compact = compact,
                        modifier = Modifier.weight(2.04f)
                    )
                }
            }

            displayThreat?.let {
                BattleThreatRail(
                    threat = it,
                    compact = compact,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        if (rewardAvailable) {
            presentationReward?.reward?.let { reward ->
                BattleRewardFloat(
                    reward = reward,
                    defeatedEnemyCount = presentationReward.defeatedEnemyInstanceIds.size,
                    presentationToken = presentationReward.token,
                    compact = compact,
                    onFinished = { onRewardFinished(presentationReward.token) },
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .offset(y = (-20).dp)
                        .padding(end = if (compact) 12.dp else 28.dp)
                )
            }
        }

        when {
            state.combatStatus == BattleCombatStatusUi.VICTORY && completionRevealReady -> {
                BattleVictoryOverlay(
                    state = state,
                    onOpenBuild = onOpenBuild,
                    onOpenFarm = onOpenFarm,
                    compact = compact,
                    modifier = Modifier.align(Alignment.Center)
                )
            }
            state.combatStatus == BattleCombatStatusUi.DEFEAT -> {
                BattleDefeatOverlay(
                    state = state,
                    onIntent = onIntent,
                    onOpenBuild = onOpenBuild,
                    onOpenDoctrine = onOpenDoctrine,
                    onOpenFarm = onOpenFarm,
                    compact = compact,
                    modifier = Modifier.align(Alignment.Center)
                )
            }
        }
    }
}

/** Lightweight kill reward reveal driven only by canonical reward events. */
@Composable
private fun BattleRewardFloat(
    reward: BattleRewardUiState,
    defeatedEnemyCount: Int,
    presentationToken: Long,
    compact: Boolean,
    onFinished: () -> Unit,
    modifier: Modifier = Modifier
) {
    val reducedMotion = LocalReducedMotion.current
    val rewardDescription = if (defeatedEnemyCount > 1) {
        stringResource(
            R.string.battle_reward_float_multiple_description,
            defeatedEnemyCount
        )
    } else {
        stringResource(R.string.battle_reward_float_description)
    }
    val progress = remember(presentationToken) { Animatable(0f) }
    LaunchedEffect(presentationToken, reducedMotion) {
        progress.snapTo(0f)
        if (reducedMotion) {
            delay(900L)
            progress.snapTo(1f)
        } else {
            progress.animateTo(1f, animationSpec = tween(BATTLE_REWARD_FLOAT_MILLIS))
        }
        onFinished()
    }
    if (progress.value >= 1f) return

    Column(
        modifier = modifier
            .widthIn(max = if (compact) 164.dp else 210.dp)
            .graphicsLayer {
                alpha = 1f - progress.value
                translationY = if (reducedMotion) 0f else -28f * progress.value
            }
            .semantics {
                contentDescription = rewardDescription
            },
        horizontalAlignment = Alignment.End,
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        reward.goldDisplay?.let { value ->
            BattleRewardFloatLine(
                icon = R.drawable.ic_resource_gold,
                iconDescription = stringResource(R.string.battle_reward_gold_icon_description),
                text = stringResource(R.string.battle_reward_gold_format, value),
                color = ResourceGold,
                presentationToken = presentationToken,
                lineIndex = 0,
                reducedMotion = reducedMotion
            )
        }
        reward.experienceDisplay?.let { value ->
            BattleRewardFloatLine(
                icon = R.drawable.ic_resource_xp,
                iconDescription = stringResource(R.string.battle_reward_xp_icon_description),
                text = stringResource(R.string.battle_reward_xp_format, value),
                color = ResonanceTeal,
                presentationToken = presentationToken,
                lineIndex = 1,
                reducedMotion = reducedMotion
            )
        }
        reward.items.forEachIndexed { index, item ->
            val title = item.titleStringKey?.let { stringResource(it.stringResId()) }
                ?: stringResource(R.string.battle_reward_item_unknown)
            BattleRewardFloatLine(
                icon = R.drawable.ic_reward_loot,
                iconDescription = stringResource(R.string.battle_reward_loot_icon_description),
                text = title,
                color = TextPrimary,
                presentationToken = presentationToken,
                lineIndex = index + 2,
                reducedMotion = reducedMotion
            )
        }
    }
}

@Composable
private fun BattleRewardFloatLine(
    icon: Int,
    iconDescription: String,
    text: String,
    color: Color,
    presentationToken: Long,
    lineIndex: Int,
    reducedMotion: Boolean
) {
    val lineProgress = remember(presentationToken, lineIndex) { Animatable(0f) }
    LaunchedEffect(presentationToken, lineIndex, reducedMotion) {
        lineProgress.snapTo(0f)
        if (reducedMotion) {
            delay(760L)
            lineProgress.snapTo(1f)
        } else {
            delay(lineIndex * 140L)
            lineProgress.animateTo(1f, animationSpec = tween(1_650))
        }
    }
    if (lineProgress.value >= 1f) return

    Row(
        modifier = Modifier.graphicsLayer {
            alpha = 1f - lineProgress.value
            translationY = if (reducedMotion) 0f else -12f * lineProgress.value
        },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(
            painter = painterResource(icon),
            contentDescription = iconDescription,
            tint = color,
            modifier = Modifier.size(16.dp)
        )
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium.battleTerminal(),
            color = color,
            maxLines = 1,
            overflow = TextOverflow.Clip
        )
    }
}

@Composable
private fun BattleFocusDivider(compact: Boolean) {
    Canvas(
        modifier = Modifier
            .width(if (compact) 12.dp else 22.dp)
            .height(if (compact) 72.dp else 96.dp)
            .alpha(0.75f)
    ) {
        val x = size.width / 2f
        drawLine(
            color = StageBorder.copy(alpha = 0.40f),
            start = androidx.compose.ui.geometry.Offset(x, 0f),
            end = androidx.compose.ui.geometry.Offset(x, size.height),
            strokeWidth = 1f
        )
        drawCircle(
            color = ResourceGold.copy(alpha = 0.82f),
            radius = 3.5f,
            center = androidx.compose.ui.geometry.Offset(x, size.height * 0.5f)
        )
    }
}

@Composable
private fun BattleEmptyState(
    state: BattleUiState,
    onDeployStartingEncounter: () -> Unit,
    modifier: Modifier = Modifier
) {
    val hasSelectedEncounter = state.encounterTitleStringKey != null
    val isWaitingForDeployment = state.combatStatus == BattleCombatStatusUi.IDLE
    val statusLabel = when (state.combatStatus) {
        BattleCombatStatusUi.IDLE -> stringResource(
            if (hasSelectedEncounter) R.string.battle_status_ready
            else R.string.battle_status_standby
        )
        BattleCombatStatusUi.ACTIVE -> stringResource(R.string.battle_status_combat)
        BattleCombatStatusUi.VICTORY -> stringResource(R.string.battle_status_victory)
        BattleCombatStatusUi.DEFEAT -> stringResource(R.string.battle_status_defeat)
    }
    val statusAccent = when (state.combatStatus) {
        BattleCombatStatusUi.IDLE -> if (hasSelectedEncounter) ResourceGold else TextSecondary
        BattleCombatStatusUi.ACTIVE -> ResonanceTeal
        BattleCombatStatusUi.VICTORY -> PositiveGreen
        BattleCombatStatusUi.DEFEAT -> ErrorRose
    }

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 320.dp),
            shape = RoundedCornerShape(18.dp),
            color = ObsidianSurface1.copy(alpha = 0.84f),
            border = BorderStroke(1.dp, statusAccent.copy(alpha = 0.52f)),
            tonalElevation = 0.dp
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 18.dp, vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(7.dp)
            ) {
                Text(
                    text = statusLabel,
                    style = MaterialTheme.typography.labelSmall.copy(
                        letterSpacing = 1.2.sp,
                        fontWeight = FontWeight.SemiBold
                    ),
                    color = statusAccent
                )
                Text(
                    text = when {
                        isWaitingForDeployment && hasSelectedEncounter ->
                            stringResource(R.string.battle_target_ready_title)
                        isWaitingForDeployment -> stringResource(R.string.battle_standby_title)
                        else -> stringResource(R.string.battle_no_active_enemy)
                    },
                    style = MaterialTheme.typography.titleLarge,
                    color = TextPrimary,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = when {
                        isWaitingForDeployment && hasSelectedEncounter ->
                            stringResource(R.string.battle_target_ready_detail)
                        isWaitingForDeployment -> stringResource(R.string.battle_standby_detail)
                        else -> stringResource(R.string.battle_no_active_enemy_detail)
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.widthIn(max = 270.dp)
                )
                if (!hasSelectedEncounter) {
                    Spacer(Modifier.height(4.dp))
                    GameButton(
                        onClick = onDeployStartingEncounter,
                        modifier = Modifier.heightIn(min = 48.dp)
                    ) {
                        Text(stringResource(R.string.battle_deploy_to_hollow))
                    }
                }
            }
        }
    }
}

@Composable
private fun HeroStageActor(
    player: BattlePlayerUiState,
    compact: Boolean,
    modifier: Modifier = Modifier
) {
    BoxWithConstraints(modifier = modifier.fillMaxHeight()) {
        val baseImageSize = if (compact) 152.dp else 190.dp
        val reservedHeight = if (compact) 72.dp else 84.dp
        val imageSize = baseImageSize
            .coerceAtMost(
                (maxHeight - reservedHeight).coerceAtLeast(if (compact) 76.dp else 96.dp)
            )
            .coerceAtMost(maxWidth)
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(if (compact) 3.dp else 5.dp)
        ) {
            ActorNameplate(
                title = player.heroName?.takeIf { it.isNotBlank() }
                    ?: stringResource(R.string.battle_player_title),
                subtitle = stringResource(
                    R.string.battle_health_format,
                    player.currentHealthDisplay,
                    player.maximumHealthDisplay
                ),
                accent = ResonanceTeal,
                compact = compact,
                modifier = Modifier.fillMaxWidth()
            )

            Box(
                modifier = Modifier
                    .size(imageSize)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(ResonanceTeal.copy(alpha = 0.26f), Color.Transparent)
                        ),
                        CircleShape
                    )
            ) {
                Image(
                    painter = painterResource(
                        PresentationAssetKey.ECHO_BOUND_HERO_ILLUSTRATION.drawableResId()
                    ),
                    contentDescription = player.heroName?.takeIf { it.isNotBlank() }
                        ?: stringResource(R.string.battle_player_title),
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            scaleX = 1.025f
                            scaleY = 1.025f
                            translationY = -2f
                        }
                        .padding(2.dp),
                    contentScale = ContentScale.Fit
                )
            }

            BattleProgressBar(
                progressUnits = player.healthProgressUnits,
                accent = PositiveGreen,
                contentDescriptionText = stringResource(
                    R.string.a11y_health_progress_format,
                    stringResource(R.string.battle_player_title),
                    player.currentHealthDisplay,
                    player.maximumHealthDisplay
                ),
                modifier = Modifier.fillMaxWidth(),
                height = if (compact) 8.dp else 10.dp
            )

            BattleStatusPips(
                statuses = player.statuses,
                compact = compact,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun EnemyFormationStage(
    enemies: List<BattleEnemyUiState>,
    defeatedEnemies: List<BattleEnemyUiState>,
    impacts: List<BattleImpactUiState>,
    compact: Boolean,
    modifier: Modifier = Modifier
) {
    BoxWithConstraints(modifier = modifier.fillMaxHeight()) {
        val formation = (enemies + defeatedEnemies).distinctBy { it.instanceId }
        if (formation.isEmpty()) return@BoxWithConstraints

        // New content is capped at three actors. The two-column fallback keeps legacy saves
        // readable without silently dropping an actor from an older formation.
        val columns = when {
            formation.size == 1 -> 1
            formation.size <= 3 -> formation.size
            else -> 2
        }
        val rows = formation.chunked(columns)
        val horizontalSpacing = if (compact) 3.dp else 5.dp
        val verticalSpacing = if (compact) 4.dp else 6.dp
        val maxRowHeight = (
            maxHeight - verticalSpacing * (rows.size - 1)
            ).coerceAtLeast(0.dp) / rows.size
        val reservedHeight = when {
            formation.size == 1 -> if (compact) 78.dp else 90.dp
            formation.size <= 3 -> if (compact) 72.dp else 82.dp
            else -> if (compact) 88.dp else 100.dp
        }
        val imageHeight = (maxRowHeight - reservedHeight).coerceAtLeast(
            if (compact) 42.dp else 54.dp
        )
        val maxActorWidth = (
            (maxWidth - horizontalSpacing * (columns - 1)).coerceAtLeast(0.dp) / columns
            ).coerceAtLeast(if (compact) 44.dp else 56.dp)
        val baseSize = when (formation.size) {
            1 -> if (compact) 172.dp else 204.dp
            2 -> if (compact) 140.dp else 176.dp
            3 -> if (compact) 118.dp else 146.dp
            else -> if (compact) 108.dp else 132.dp
        }
        val desiredSize = baseSize
            .coerceAtMost(imageHeight)
            .coerceAtMost(maxActorWidth)
        val threatId = enemies.minByOrNull {
            it.nextAttackRemainingMillis ?: Long.MAX_VALUE
        }?.instanceId

        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(verticalSpacing)
        ) {
            rows.forEach { rowEnemies ->
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(horizontalSpacing),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    rowEnemies.forEach { enemy ->
                        key(enemy.instanceId) {
                            EnemyStageActor(
                                enemy = enemy,
                                imageSize = desiredSize,
                                isThreat = enemy.instanceId == threatId,
                                isDefeatedSnapshot = !enemy.alive,
                                compact = compact,
                                impact = impacts.lastOrNull {
                                    it.targetInstanceId == enemy.instanceId
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                            )
                        }
                    }
                    repeat(columns - rowEnemies.size) {
                        Spacer(Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
private fun EnemyStageActor(
    enemy: BattleEnemyUiState,
    imageSize: Dp,
    isThreat: Boolean,
    isDefeatedSnapshot: Boolean,
    compact: Boolean,
    impact: BattleImpactUiState?,
    modifier: Modifier = Modifier
) {
    val accent = when {
        isDefeatedSnapshot -> TextSecondary
        isThreat -> ErrorRose
        else -> ArcaneViolet.copy(alpha = 0.86f)
    }
    Column(
        modifier = modifier.alpha(if (isDefeatedSnapshot) 0.45f else 1f),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(if (compact) 2.dp else 4.dp)
    ) {
        ActorNameplate(
            title = stringResource(enemy.titleStringKey.stringResId()),
            subtitle = listOfNotNull(
                enemy.role?.let { role ->
                    stringResource(R.string.battle_enemy_role_format, enemyRoleLabel(role))
                },
                stringResource(
                    R.string.battle_health_compact_format,
                    enemy.currentHealthDisplay,
                    enemy.maximumHealthDisplay
                )
            ).joinToString(" · "),
            accent = accent,
            compact = compact,
            modifier = Modifier.fillMaxWidth()
        )

        Box(
            modifier = Modifier
                .size(imageSize)
                .background(
                    Brush.radialGradient(colors = listOf(accent.copy(alpha = 0.30f), Color.Transparent)),
                    CircleShape
                )
        ) {
            Image(
                painter = painterResource(
                    (enemy.illustrationAssetKey ?: enemy.assetKey).drawableResId()
                ),
                contentDescription = stringResource(enemy.titleStringKey.stringResId()),
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        scaleX = 1.02f
                        scaleY = 1.02f
                        translationY = -3f
                    }
                    .padding(if (compact) 1.dp else 3.dp),
                contentScale = ContentScale.Fit
            )
            if (!isDefeatedSnapshot) {
                CombatHitFeedback(impact = impact, modifier = Modifier.fillMaxSize())
            }
        }

        BattleProgressBar(
            progressUnits = enemy.healthProgressUnits,
            accent = accent,
            contentDescriptionText = stringResource(
                R.string.battle_health_format,
                enemy.currentHealthDisplay,
                enemy.maximumHealthDisplay
            ),
            modifier = Modifier.fillMaxWidth(),
            height = if (compact) 7.dp else 9.dp
        )

        BattleStatusPips(
            statuses = enemy.statuses,
            mutations = enemy.mutations.map { it.titleStringKey },
            compact = compact,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun ActorNameplate(
    title: String,
    subtitle: String,
    accent: Color,
    compact: Boolean,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .heightIn(min = if (compact) 38.dp else 44.dp)
            .widthIn(min = 0.dp),
        shape = RoundedCornerShape(10.dp),
        color = ObsidianSurface1.copy(alpha = 0.82f),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.52f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(min = 0.dp)
                .padding(horizontal = 7.dp, vertical = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(1.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium.battleTerminal().copy(
                    fontSize = if (compact) 8.sp else 9.sp,
                    lineHeight = if (compact) 10.sp else 11.sp,
                    letterSpacing = 0.sp
                ),
                color = TextPrimary,
                modifier = Modifier.fillMaxWidth(),
                maxLines = 2,
                softWrap = true,
                overflow = TextOverflow.Clip,
                textAlign = TextAlign.Center
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall.battleTerminal().copy(
                    fontSize = if (compact) 7.sp else 8.sp,
                    lineHeight = if (compact) 9.sp else 10.sp,
                    letterSpacing = 0.sp
                ),
                color = accent,
                modifier = Modifier.fillMaxWidth(),
                maxLines = 2,
                softWrap = true,
                overflow = TextOverflow.Clip,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun BattleProgressBar(
    progressUnits: Int,
    accent: Color,
    contentDescriptionText: String,
    modifier: Modifier = Modifier,
    height: Dp = 9.dp
) {
    Box(
        modifier = modifier
            .height(height)
            .clip(RoundedCornerShape(99.dp))
            .background(ObsidianSurface3.copy(alpha = 0.90f))
            .semantics { contentDescription = contentDescriptionText }
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(progressUnits.coerceIn(0, 10_000) / 10_000f)
                .fillMaxHeight()
                .background(
                    Brush.horizontalGradient(colors = listOf(accent.copy(alpha = 0.72f), accent))
                )
        )
    }
}

@Composable
private fun BattleStatusPips(
    statuses: List<BattleStatusEffectUiState>,
    mutations: List<PresentationStringKey> = emptyList(),
    compact: Boolean,
    modifier: Modifier = Modifier
) {
    if (statuses.isEmpty() && mutations.isEmpty()) return
    var inspectedStatusId by remember { mutableStateOf<InstanceId?>(null) }
    Row(
        modifier = modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        statuses.forEach { status ->
            val title = stringResource(status.titleStringKey.stringResId())
            val accent = statusAccent(status)
            val statusDescription = stringResource(
                R.string.battle_status_format,
                title,
                status.stackCount,
                status.remainingMillis
            )
            Box {
                Surface(
                    modifier = Modifier
                        .heightIn(min = 44.dp)
                        .widthIn(min = 44.dp)
                        .combinedClickable(
                            role = Role.Button,
                            onClick = { inspectedStatusId = status.instanceId },
                            onLongClick = { inspectedStatusId = status.instanceId }
                        )
                        .semantics { contentDescription = statusDescription },
                    shape = RoundedCornerShape(7.dp),
                    color = accent.copy(alpha = 0.14f),
                    border = BorderStroke(1.dp, accent.copy(alpha = 0.42f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Icon(
                            painter = painterResource(status.assetKey.drawableResId()),
                            contentDescription = null,
                            tint = accent,
                            modifier = Modifier.size(if (compact) 16.dp else 18.dp)
                        )
                        Text(
                            text = "x${status.stackCount}",
                            style = MaterialTheme.typography.labelSmall.battleTerminal(),
                            color = accent
                        )
                    }
                }
                DropdownMenu(
                    expanded = inspectedStatusId == status.instanceId,
                    onDismissRequest = { inspectedStatusId = null },
                    modifier = Modifier.widthIn(max = 250.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleSmall.battleTerminal(),
                            color = accent
                        )
                        Text(
                            text = when (status.polarity) {
                                BattleStatusPolarityUi.HARMFUL ->
                                    stringResource(R.string.battle_status_debuff)
                                BattleStatusPolarityUi.BENEFICIAL ->
                                    stringResource(R.string.battle_status_buff)
                                BattleStatusPolarityUi.NEUTRAL ->
                                    stringResource(R.string.battle_status_neutral)
                            },
                            style = MaterialTheme.typography.labelSmall.battleTerminal(),
                            color = TextSecondary
                        )
                        Text(
                            text = stringResource(status.detailStringKey.stringResId()),
                            style = MaterialTheme.typography.bodySmall,
                            color = TextPrimary
                        )
                        Text(
                            text = stringResource(
                                R.string.battle_status_duration_format,
                                status.remainingMillis / 1000f,
                                status.stackCount
                            ),
                            style = MaterialTheme.typography.labelSmall.battleTerminal(),
                            color = TextSecondary
                        )
                        status.potencyDisplay?.let { potency ->
                            Text(
                                text = stringResource(R.string.battle_status_potency_format, potency),
                                style = MaterialTheme.typography.labelSmall.battleTerminal(),
                                color = TextSecondary
                            )
                        }
                    }
                }
            }
        }
        mutations.forEach { mutation ->
            val title = stringResource(mutation.stringResId())
            Surface(
                modifier = Modifier.semantics { contentDescription = title },
                shape = RoundedCornerShape(6.dp),
                color = ResourceGold.copy(alpha = 0.14f),
                border = BorderStroke(1.dp, ResourceGold.copy(alpha = 0.35f))
            ) {
                Text(
                    text = "⬢",
                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp),
                    style = MaterialTheme.typography.labelSmall,
                    color = ResourceGold
                )
            }
        }
    }
}

private fun statusAccent(status: BattleStatusEffectUiState): Color =
    when (status.polarity) {
        BattleStatusPolarityUi.HARMFUL -> ErrorRose
        BattleStatusPolarityUi.BENEFICIAL -> PositiveGreen
        BattleStatusPolarityUi.NEUTRAL -> ResourceGold
    }

@Composable
private fun BattleThreatRail(
    threat: BattleEnemyUiState,
    compact: Boolean,
    modifier: Modifier = Modifier
) {
    val attack = threat.attackTitle ?: stringResource(R.string.battle_incoming_strike)
    val damage = threat.attackDamageDisplay?.let {
        stringResource(R.string.battle_attack_profile_format, it)
    }
    Surface(
        modifier = modifier.heightIn(min = if (compact) 42.dp else 50.dp),
        shape = RoundedCornerShape(14.dp),
        color = Color(0xB9141425),
        border = BorderStroke(1.dp, ErrorRose.copy(alpha = 0.62f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = if (compact) 7.dp else 9.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(if (compact) 28.dp else 32.dp)
                    .background(ErrorRose.copy(alpha = 0.16f), RoundedCornerShape(9.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_stat_attack),
                    contentDescription = null,
                    tint = ErrorRose,
                    modifier = Modifier.size(if (compact) 16.dp else 19.dp)
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(
                        R.string.battle_threat_format,
                        stringResource(threat.titleStringKey.stringResId())
                    ),
                    style = MaterialTheme.typography.labelSmall,
                    color = ErrorRose,
                    maxLines = 1,
                    overflow = TextOverflow.Clip
                )
                Text(
                    text = listOfNotNull(attack, damage).joinToString(" · "),
                    style = MaterialTheme.typography.bodySmall,
                    color = TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Clip
                )
            }
            Text(
                text = threat.nextAttackRemainingMillis?.let { remaining ->
                    stringResource(R.string.battle_enemy_next_attack_format, remaining / 1000f)
                } ?: stringResource(R.string.battle_no_attack_eta),
                style = MaterialTheme.typography.titleSmall,
                color = ResourceGold,
                maxLines = 1
            )
            Text(
                text = "›",
                style = MaterialTheme.typography.headlineSmall,
                color = ArcaneViolet,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun BattleCombatStatsRail(
    player: BattlePlayerUiState,
    compact: Boolean,
    modifier: Modifier = Modifier
) {
    val widthDp = LocalConfiguration.current.screenWidthDp
    val fontScale = LocalDensity.current.fontScale
    val primaryColumns = when {
        widthDp < 300 || fontScale >= 1.45f -> 1
        else -> 3
    }
    val stats = listOf(
        BattleStatVisual(
            icon = R.drawable.ic_stat_attack,
            label = stringResource(R.string.battle_attack),
            value = player.attackDisplay,
            color = ResourceGold,
            description = stringResource(R.string.battle_stat_attack_description)
        ),
        BattleStatVisual(
            icon = R.drawable.ic_stat_health,
            label = stringResource(R.string.battle_stat_health),
            value = stringResource(
                R.string.battle_stat_health_value_format,
                player.currentHealthDisplay,
                player.maximumHealthDisplay
            ),
            color = PositiveGreen,
            description = stringResource(R.string.battle_stat_health_description)
        ),
        BattleStatVisual(
            icon = R.drawable.ic_stat_armor,
            label = stringResource(R.string.battle_armor),
            value = player.armorDisplay,
            color = ArcaneViolet,
            description = stringResource(R.string.battle_stat_armor_description)
        ),
        BattleStatVisual(
            icon = R.drawable.ic_stat_crit,
            label = stringResource(R.string.battle_stat_crit),
            value = player.criticalChanceDisplay,
            color = ErrorRose,
            description = stringResource(R.string.battle_stat_crit_description)
        ),
        BattleStatVisual(
            icon = R.drawable.ic_stat_crit_damage,
            label = stringResource(R.string.battle_stat_crit_damage),
            value = player.criticalMultiplierDisplay,
            color = ResourceGold,
            description = stringResource(R.string.battle_stat_crit_damage_description)
        ),
        BattleStatVisual(
            icon = R.drawable.ic_stat_dps,
            label = stringResource(R.string.battle_dps),
            value = player.basicAttackDpsDisplay,
            color = ResonanceTeal,
            description = stringResource(R.string.battle_stat_dps_description)
        ),
        BattleStatVisual(
            icon = R.drawable.ic_stat_skill_power,
            label = stringResource(R.string.battle_stat_skill_power),
            value = player.effectPowerDisplay,
            color = ArcaneViolet,
            description = stringResource(R.string.battle_stat_skill_power_description)
        ),
        BattleStatVisual(
            icon = R.drawable.ic_stat_healing,
            label = stringResource(R.string.battle_stat_healing),
            value = player.healingPowerDisplay,
            color = PositiveGreen,
            description = stringResource(R.string.battle_stat_healing_description)
        )
    )
    val primaryStats = stats.take(3)
    val secondaryStats = stats.drop(3)

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(
            if (compact) GameDimensions.ActorNameplateRadius + 4.dp
            else GameDimensions.CombatCardRadius + 7.dp
        ),
        color = ObsidianSurface1.copy(alpha = 0.90f),
        border = BorderStroke(1.dp, ResonanceTeal.copy(alpha = 0.46f))
    ) {
        Column(
            modifier = Modifier.padding(horizontal = if (compact) 7.dp else 9.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.dp)
                    .background(
                        Brush.horizontalGradient(
                            listOf(
                                Color.Transparent,
                                ResonanceTeal.copy(alpha = 0.72f),
                                ResourceGold.copy(alpha = 0.68f),
                                Color.Transparent
                            )
                        ),
                        RoundedCornerShape(99.dp)
                    )
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                    Text(
                        text = stringResource(R.string.battle_combat_stats),
                        style = MaterialTheme.typography.labelMedium.battleTerminal(),
                        color = ResonanceTeal
                    )
                    Text(
                        text = stringResource(R.string.battle_readout_tagline),
                        style = MaterialTheme.typography.labelSmall.battleTerminal().copy(
                            fontSize = 7.sp,
                            letterSpacing = 0.35.sp
                        ),
                        color = TextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Clip
                    )
                }
                Spacer(
                    modifier = Modifier
                        .weight(1f)
                        .height(1.dp)
                        .background(ResonanceTeal.copy(alpha = 0.34f))
                )
            }
            primaryStats.chunked(primaryColumns).forEach { rowStats ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    rowStats.forEach { stat ->
                        BattleStatChip(
                            stat = stat,
                            compact = compact,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    repeat(primaryColumns - rowStats.size) {
                        Spacer(Modifier.weight(1f))
                    }
                }
            }
            if (secondaryStats.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    secondaryStats.forEach { stat ->
                        BattleStatChip(
                            stat = stat,
                            compact = compact,
                            modifier = Modifier.widthIn(min = if (compact) 106.dp else 122.dp)
                        )
                    }
                }
            }
        }
    }
}

private data class BattleStatVisual(
    val icon: Int,
    val label: String,
    val value: String,
    val color: Color,
    val description: String
)

@Composable
private fun BattleStatChip(
    stat: BattleStatVisual,
    compact: Boolean,
    modifier: Modifier = Modifier
) {
    val valueFontSize = when {
        stat.value.length >= 15 -> 9.sp
        stat.value.length >= 11 -> 10.sp
        compact -> 12.sp
        else -> 14.sp
    }
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = if (compact) 68.dp else 76.dp)
            .semantics {
                contentDescription = stat.label + ": " + stat.value + ". " + stat.description
            },
        shape = RoundedCornerShape(10.dp),
        color = ObsidianSurface2.copy(alpha = 0.88f),
        border = BorderStroke(1.dp, stat.color.copy(alpha = 0.42f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 7.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Surface(
                    modifier = Modifier.size(if (compact) 25.dp else 28.dp),
                    shape = RoundedCornerShape(8.dp),
                    color = stat.color.copy(alpha = 0.14f),
                    border = BorderStroke(1.dp, stat.color.copy(alpha = 0.32f))
                ) {
                    Icon(
                        painter = painterResource(stat.icon),
                        contentDescription = null,
                        tint = stat.color,
                        modifier = Modifier.padding(if (compact) 5.dp else 6.dp)
                    )
                }
                Text(
                    text = stat.label.uppercase(),
                    modifier = Modifier
                        .weight(1f)
                        .widthIn(min = 0.dp),
                    style = MaterialTheme.typography.labelSmall.battleTerminal().copy(
                        fontSize = if (compact) 8.sp else 9.sp,
                        letterSpacing = 0.3.sp
                    ),
                    color = TextSecondary,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Clip
                )
            }
            Text(
                text = stat.value,
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(min = 0.dp),
                style = MaterialTheme.typography.labelMedium.battleTerminal().copy(
                    fontSize = valueFontSize
                ),
                color = TextPrimary,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Clip
            )
        }
    }
}

@Composable
private fun BattleCommandDock(
    state: BattleUiState,
    compact: Boolean,
    onIntent: (BattleUiIntent) -> Unit,
    onOpenSkillLoadout: () -> Unit,
    modifier: Modifier = Modifier
) {
    var skillsExpanded by rememberSaveable(state.combatSequenceId) {
        mutableStateOf(true)
    }
    val skillsToggleDescription = stringResource(
        if (skillsExpanded) {
            R.string.battle_skills_collapse
        } else {
            R.string.battle_skills_expand
        }
    )

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(if (compact) 16.dp else 20.dp),
        color = ObsidianSurface1.copy(alpha = 0.95f),
        border = BorderStroke(1.dp, ObsidianOutline.copy(alpha = 0.76f)),
        tonalElevation = 0.dp,
        shadowElevation = 6.dp
    ) {
        Column(
            modifier = Modifier.padding(horizontal = if (compact) 7.dp else 9.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.spacedBy(if (compact) 4.dp else 5.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.dp)
                    .background(
                        Brush.horizontalGradient(
                            listOf(
                                Color.Transparent,
                                ArcaneViolet.copy(alpha = 0.74f),
                                ResourceGold.copy(alpha = 0.64f),
                                Color.Transparent
                            )
                        ),
                        RoundedCornerShape(99.dp)
                    )
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(
                        role = Role.Button,
                        onClick = { skillsExpanded = !skillsExpanded }
                    )
                    .semantics {
                        contentDescription = skillsToggleDescription
                    },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.battle_combat_dock),
                        style = MaterialTheme.typography.labelLarge,
                        color = ArcaneViolet
                    )
                    Text(
                        text = queueSummary(state),
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Clip
                    )
                }
                Text(
                    text = if (skillsExpanded) "−" else "+",
                    style = MaterialTheme.typography.titleMedium,
                    color = ResourceGold
                )
                if (state.queuedSkillId != null) {
                    GameOutlinedButton(
                        onClick = { onIntent(BattleUiIntent.ClearQueuedSkill) },
                        modifier = Modifier.heightIn(min = 48.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(stringResource(R.string.battle_clear_queue))
                    }
                }
                SkillLoadoutGlyphButton(
                    onClick = onOpenSkillLoadout,
                    compact = compact
                )
            }

            if (skillsExpanded) {
                if (state.equippedSkills.isEmpty()) {
                    GameButton(
                        onClick = onOpenSkillLoadout,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp)
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_skill_loadout),
                            contentDescription = null,
                            tint = ResourceGold,
                            modifier = Modifier.size(22.dp)
                        )
                        Text(stringResource(R.string.battle_no_equipped_skills))
                    }
                } else {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        state.equippedSkills.take(4).forEach { skill ->
                            BattleSkillTile(
                                skill = skill,
                                compact = compact,
                                onClick = { onIntent(BattleUiIntent.QueueSkill(skill.skillId)) },
                                modifier = Modifier.width(if (compact) 68.dp else 74.dp)
                            )
                        }
                    }
                }
            }

            BattleComboRail(
                state = state,
                compact = compact,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun SkillLoadoutGlyphButton(
    onClick: () -> Unit,
    compact: Boolean
) {
    val description = stringResource(R.string.battle_open_skill_loadout_description)
    Surface(
        modifier = Modifier
            .size(if (compact) 44.dp else 48.dp)
            .clickable(role = Role.Button, onClick = onClick)
            .semantics { contentDescription = description },
        shape = RoundedCornerShape(12.dp),
        color = ArcaneViolet.copy(alpha = 0.16f),
        border = BorderStroke(1.dp, ArcaneViolet.copy(alpha = 0.62f))
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_skill_loadout),
            contentDescription = null,
            tint = ResourceGold,
            modifier = Modifier.padding(if (compact) 10.dp else 12.dp)
        )
    }
}

@Composable
private fun queueSummary(state: BattleUiState): String {
    val queued = state.queuedSkillId?.let { queuedId ->
        state.equippedSkills.firstOrNull { it.skillId == queuedId }
            ?.let { stringResource(it.titleStringKey.stringResId()) }
    }
    return queued?.let { stringResource(R.string.battle_queued_skill_format, it) }
        ?: stringResource(R.string.battle_queue_hint)
}

@Composable
private fun BattleSkillTile(
    skill: BattleSkillUiState,
    compact: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val skillName = stringResource(skill.titleStringKey.stringResId())
    val affinity = skill.affinityIds.firstOrNull()
    val token = affinity?.let(::affinityVisualToken)
    val accent = token?.color ?: ArcaneViolet
    val readiness = skillReadinessLabel(skill)
    val tileColor = when {
        skill.queued -> accent.copy(alpha = 0.22f)
        skill.readiness == BattleSkillReadinessUi.READY -> ObsidianSurface2.copy(alpha = 0.98f)
        else -> ObsidianBackground.copy(alpha = 0.82f)
    }
    val borderColor = when {
        skill.queued -> ResourceGold
        skill.readiness == BattleSkillReadinessUi.READY -> accent.copy(alpha = 0.74f)
        else -> ObsidianOutline.copy(alpha = 0.64f)
    }
    val enabled = skill.queueAllowed
    val accessibilityText = "$skillName. ${stringResource(R.string.battle_queue_skill)}. $readiness"

    Surface(
        modifier = modifier
            .heightIn(min = if (compact) 62.dp else 70.dp)
            .clip(RoundedCornerShape(14.dp))
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .semantics { contentDescription = accessibilityText },
        shape = RoundedCornerShape(14.dp),
        color = tileColor,
        border = BorderStroke(
            width = if (skill.queued) 1.5.dp else 1.dp,
            color = borderColor
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(1.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.dp)
                    .background(accent, RoundedCornerShape(99.dp))
            )
            Box(
                modifier = Modifier
                    .size(if (compact) 32.dp else 36.dp)
                    .clip(RoundedCornerShape(10.dp))
            ) {
                SkillArtwork(skill.skillId, Modifier.fillMaxSize())
                if (skill.readiness != BattleSkillReadinessUi.READY && !skill.queued) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.42f))
                    )
                    if (skill.cooldownRemainingMillis > 0L) {
                        Text(
                            text = stringResource(
                                R.string.battle_skill_cooldown_seconds,
                                skill.cooldownRemainingMillis / 1000f
                            ),
                            modifier = Modifier.align(Alignment.Center),
                            style = MaterialTheme.typography.labelSmall,
                            color = TextPrimary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
                if (skill.queued) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .background(ResourceGold.copy(alpha = 0.94f))
                    ) {
                        Text(
                            text = stringResource(R.string.battle_skill_queued_next),
                            modifier = Modifier.fillMaxWidth(),
                            style = MaterialTheme.typography.labelSmall,
                            color = ObsidianBackground,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
            Text(
                text = skillName,
                style = MaterialTheme.typography.labelSmall,
                color = if (enabled) TextPrimary else DisabledInk,
                maxLines = 1,
                overflow = TextOverflow.Clip
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(3.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("R${skill.rank}", style = MaterialTheme.typography.labelSmall, color = accent)
                Text(
                    text = readiness,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (enabled) accent else DisabledInk,
                    maxLines = 1,
                    overflow = TextOverflow.Clip
                )
                token?.let {
                    Icon(
                        painter = painterResource(it.iconResId),
                        contentDescription = stringResource(it.labelResId),
                        tint = it.color,
                        modifier = Modifier.size(12.dp)
                    )
                }
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp)
                    .clip(RoundedCornerShape(99.dp))
                    .background(ObsidianSurface3)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(
                            skill.cooldownProgressUnits.coerceIn(0, 10_000) / 10_000f
                        )
                        .fillMaxHeight()
                        .background(accent)
                )
            }
        }
    }
}

@Composable
private fun skillReadinessLabel(skill: BattleSkillUiState): String = when {
    skill.queued -> stringResource(R.string.battle_skill_queued_next)
    skill.readiness == BattleSkillReadinessUi.READY -> stringResource(R.string.battle_skill_ready)
    skill.readiness == BattleSkillReadinessUi.COOLDOWN_WAIT -> stringResource(
        R.string.battle_skill_cooldown_seconds,
        skill.cooldownRemainingMillis / 1000f
    )
    skill.readiness == BattleSkillReadinessUi.RESOURCE_WAIT ->
        stringResource(R.string.battle_skill_resource_wait)
    else -> stringResource(R.string.battle_skill_unavailable)
}

@Composable
private fun BattleComboRail(
    state: BattleUiState,
    compact: Boolean,
    modifier: Modifier = Modifier
) {
    val sequenceNames = state.resonanceSequence.mapNotNull { affinityId ->
        state.resonance.firstOrNull { it.affinityId == affinityId }
            ?.let { stringResource(it.titleStringKey.stringResId()) }
    }
    val sequenceDescription = if (sequenceNames.isEmpty()) {
        stringResource(R.string.battle_resonance_sequence_empty_a11y)
    } else {
        stringResource(
            R.string.battle_resonance_sequence_a11y,
            sequenceNames.joinToString()
        )
    }

    Surface(
        modifier = modifier
            .heightIn(min = 38.dp)
            .semantics { contentDescription = sequenceDescription },
        shape = RoundedCornerShape(11.dp),
        color = ObsidianSurface2.copy(alpha = 0.76f),
        border = BorderStroke(1.dp, ResonanceTeal.copy(alpha = 0.40f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 6.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Column(
                modifier = Modifier.widthIn(min = if (compact) 34.dp else 52.dp),
                verticalArrangement = Arrangement.spacedBy(1.dp)
            ) {
                Text(
                    text = stringResource(R.string.battle_resonance_compact),
                    style = MaterialTheme.typography.labelSmall,
                    color = ResonanceTeal
                )
                if (!compact) {
                    Text(
                        text = stringResource(R.string.battle_resonance_sequence),
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary,
                        maxLines = 1
                    )
                }
            }
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (state.resonanceSequence.isEmpty()) {
                    Text("—", style = MaterialTheme.typography.labelMedium, color = TextSecondary)
                } else {
                    state.resonanceSequence.take(if (compact) 4 else 6).forEach { affinityId ->
                        val token = affinityVisualToken(affinityId)
                        Icon(
                            painter = painterResource(token.iconResId),
                            contentDescription = stringResource(token.labelResId),
                            tint = token.color,
                            modifier = Modifier.size(if (compact) 16.dp else 18.dp)
                        )
                    }
                }
            }
            if (!compact) {
                state.resonance.maxByOrNull { it.chargeProgressUnits }?.let { affinity ->
                    Text(
                        text = "${affinity.chargeDisplay}/${affinity.capDisplay}",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextPrimary,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

@Composable
private fun BattleEventBanner(
    state: BattleUiState,
    modifier: Modifier = Modifier
) {
    val feedback = state.feedback ?: return
    val message = feedbackMessage(state, feedback)
    val accent = feedbackAccent(feedback)
    Surface(
        modifier = modifier
            .heightIn(min = 40.dp)
            .eventFeedbackPulse(feedback.sequenceNumber)
            .semantics { contentDescription = message },
        shape = RoundedCornerShape(11.dp),
        color = accent.copy(alpha = 0.12f),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.48f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(7.dp)
        ) {
            Text(
                text = feedbackGlyph(feedback),
                style = MaterialTheme.typography.titleSmall,
                color = accent,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = message,
                style = MaterialTheme.typography.labelMedium,
                color = accent,
                maxLines = 2,
                overflow = TextOverflow.Clip
            )
        }
    }
}

@Composable
private fun BattleVictoryOverlay(
    state: BattleUiState,
    onOpenBuild: (InstanceId?) -> Unit,
    onOpenFarm: () -> Unit,
    compact: Boolean,
    modifier: Modifier = Modifier
) {
    val feedback = state.feedback
    val reward = feedback?.reward
    val inspectableItemId = reward?.items
        ?.firstOrNull { !it.sentToOverflow && !it.autoSalvaged }
        ?.itemInstanceId
    val detail = when (feedback?.kind) {
        BattleFeedbackKind.LOOT -> stringResource(R.string.battle_feedback_loot)
        BattleFeedbackKind.LEVEL_UP -> feedback.amountDisplay?.let { level ->
            stringResource(R.string.battle_feedback_level_up, level)
        }
        BattleFeedbackKind.UPGRADE_PURCHASED -> feedback.upgradeCostDisplay?.let { cost ->
            stringResource(R.string.battle_feedback_upgrade_spent, cost)
        } ?: stringResource(R.string.battle_feedback_upgrade)
        BattleFeedbackKind.CONVERGENCE -> stringResource(R.string.battle_feedback_convergence)
        BattleFeedbackKind.ENEMY_DEFEATED -> stringResource(R.string.battle_feedback_enemy_defeated)
        else -> null
    }

    BattleOutcomeSurface(
        accent = ResourceGold,
        modifier = modifier
            .padding(horizontal = if (compact) 8.dp else 18.dp)
            .widthIn(max = 360.dp)
    ) {
        Text(
            text = stringResource(R.string.battle_stage_cleared),
            style = MaterialTheme.typography.headlineSmall,
            color = ResourceGold
        )
        detail?.let {
            Text(text = it, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
        }
        reward?.let { BattleRewardFacts(it) }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            GameButton(
                onClick = { onOpenBuild(inspectableItemId) },
                modifier = Modifier
                    .widthIn(min = 132.dp)
                    .heightIn(min = 48.dp)
            ) {
                Text(
                    stringResource(
                        if (inspectableItemId != null) R.string.battle_inspect_reward
                        else R.string.nav_gear
                    )
                )
            }
            GameOutlinedButton(
                onClick = onOpenFarm,
                modifier = Modifier
                    .widthIn(min = 112.dp)
                    .heightIn(min = 48.dp)
            ) {
                Text(stringResource(R.string.battle_open_farm))
            }
        }
    }
}

@Composable
private fun BattleDefeatOverlay(
    state: BattleUiState,
    onIntent: (BattleUiIntent) -> Unit,
    onOpenBuild: (InstanceId?) -> Unit,
    onOpenDoctrine: () -> Unit,
    onOpenFarm: () -> Unit,
    compact: Boolean,
    modifier: Modifier = Modifier
) {
    BattleOutcomeSurface(
        accent = ErrorRose,
        modifier = modifier
            .padding(horizontal = if (compact) 8.dp else 18.dp)
            .widthIn(max = 360.dp)
    ) {
        Text(
            text = stringResource(R.string.battle_defeat_title),
            style = MaterialTheme.typography.headlineSmall,
            color = ErrorRose
        )
        Text(
            text = stringResource(R.string.battle_defeat_detail),
            style = MaterialTheme.typography.bodySmall,
            color = TextSecondary
        )
        state.bossPhase?.let { phase ->
            Text(text = bossPhaseLabel(phase), style = MaterialTheme.typography.labelMedium, color = ResourceGold)
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            state.retryEncounterId?.let { retryId ->
                GameButton(
                    onClick = { onIntent(BattleUiIntent.RetryEncounter(retryId)) },
                    modifier = Modifier
                        .widthIn(min = 152.dp)
                        .heightIn(min = 48.dp)
                ) {
                    Text(stringResource(R.string.battle_retry_failed_encounter))
                }
            }
            GameOutlinedButton(
                onClick = { onOpenBuild(null) },
                modifier = Modifier
                    .widthIn(min = 112.dp)
                    .heightIn(min = 48.dp)
            ) {
                Text(stringResource(R.string.nav_gear))
            }
            GameOutlinedButton(
                onClick = onOpenDoctrine,
                modifier = Modifier
                    .widthIn(min = 132.dp)
                    .heightIn(min = 48.dp)
            ) {
                Text(stringResource(R.string.nav_doctrine))
            }
            GameOutlinedButton(
                onClick = onOpenFarm,
                modifier = Modifier
                    .widthIn(min = 96.dp)
                    .heightIn(min = 48.dp)
            ) {
                Text(stringResource(R.string.battle_open_farm))
            }
        }
    }
}

@Composable
private fun BattleOutcomeSurface(
    accent: Color,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        color = ObsidianSurface1.copy(alpha = 0.97f),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.70f)),
        shadowElevation = 12.dp,
        tonalElevation = 0.dp
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 15.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            content = content
        )
    }
}

@Composable
private fun BattleRewardFacts(reward: BattleRewardUiState) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        reward.goldDisplay?.let { gold ->
            Text(
                text = stringResource(R.string.battle_reward_gold_format, gold),
                style = MaterialTheme.typography.bodyMedium,
                color = ResourceGold
            )
        }
        reward.experienceDisplay?.let { experience ->
            Text(
                text = stringResource(R.string.battle_reward_xp_format, experience),
                style = MaterialTheme.typography.bodyMedium,
                color = ResonanceTeal
            )
        }
        reward.items.forEach { item ->
            val title = item.titleStringKey?.let { stringResource(it.stringResId()) }
                ?: stringResource(R.string.battle_reward_item_unknown)
            val rarity = item.rarityTitleStringKey?.let { stringResource(it.stringResId()) }
            val stateLabel = when {
                item.sentToOverflow -> stringResource(R.string.battle_reward_overflow)
                item.autoSalvaged -> stringResource(R.string.battle_reward_auto_salvaged)
                else -> null
            }
            val suffix = listOfNotNull(rarity, stateLabel)
                .takeIf { it.isNotEmpty() }
                ?.joinToString(prefix = " · ", separator = " · ")
                .orEmpty()
            Text(
                text = stringResource(R.string.battle_reward_item_format, title, suffix),
                style = MaterialTheme.typography.bodySmall,
                color = TextPrimary,
                maxLines = 2,
                overflow = TextOverflow.Clip
            )
        }
    }
}

@Composable
private fun bossPhaseLabel(phase: BattleBossPhaseUi): String = when (phase) {
    BattleBossPhaseUi.BASTION -> stringResource(R.string.battle_boss_phase_bastion)
    BattleBossPhaseUi.REFLECTION -> stringResource(R.string.battle_boss_phase_reflection)
    BattleBossPhaseUi.FRACTURE -> stringResource(R.string.battle_boss_phase_fracture)
}

@Composable
private fun enemyRoleLabel(role: EnemyRole): String = when (role) {
    EnemyRole.SWARM -> stringResource(R.string.battle_enemy_role_swarm)
    EnemyRole.ASSASSIN -> stringResource(R.string.battle_enemy_role_assassin)
    EnemyRole.CASTER -> stringResource(R.string.battle_enemy_role_caster)
    EnemyRole.PROTECTOR -> stringResource(R.string.battle_enemy_role_protector)
    EnemyRole.DISRUPTOR -> stringResource(R.string.battle_enemy_role_disruptor)
    EnemyRole.CONTROLLER -> stringResource(R.string.battle_enemy_role_controller)
    EnemyRole.PARASITE -> stringResource(R.string.battle_enemy_role_parasite)
    EnemyRole.ADAPTIVE -> stringResource(R.string.battle_enemy_role_adaptive)
    EnemyRole.BOSS -> stringResource(R.string.battle_enemy_role_boss)
}

@Composable
private fun feedbackMessage(
    state: BattleUiState,
    feedback: BattleFeedbackUiState
): String {
    val contentTitle = feedback.contentId?.let { id ->
        when (id) {
            state.basicAttackUpgrade.upgradeId ->
                stringResource(state.basicAttackUpgrade.titleStringKey.stringResId())
            else -> state.equippedSkills
                .firstOrNull { it.skillId == id }
                ?.let { skill -> stringResource(skill.titleStringKey.stringResId()) }
                ?: state.enemies
                    .firstOrNull { it.definitionId == id }
                    ?.let { enemy -> stringResource(enemy.titleStringKey.stringResId()) }
                ?: state.player.statuses
                    .firstOrNull { it.definitionId == id }
                    ?.let { status -> stringResource(status.titleStringKey.stringResId()) }
                ?: state.enemies
                    .asSequence()
                    .flatMap { it.statuses.asSequence() }
                    .firstOrNull { it.definitionId == id }
                    ?.let { status -> stringResource(status.titleStringKey.stringResId()) }
        }
    }

    return when (feedback.kind) {
        BattleFeedbackKind.COMMAND_REJECTED -> stringResource(
            R.string.battle_feedback_rejected,
            rejectionMessage(feedback.rejectionCode)
        )
        BattleFeedbackKind.SKILL_QUEUED -> stringResource(
            R.string.battle_feedback_skill_queued,
            contentTitle ?: stringResource(R.string.battle_feedback_skill)
        )
        BattleFeedbackKind.SKILL_QUEUE_REPLACED -> stringResource(
            R.string.battle_feedback_skill_replaced,
            contentTitle ?: stringResource(R.string.battle_feedback_skill)
        )
        BattleFeedbackKind.SKILL_QUEUE_DEFERRED -> stringResource(
            R.string.battle_feedback_skill_deferred,
            contentTitle ?: stringResource(R.string.battle_feedback_skill)
        )
        BattleFeedbackKind.SKILL_QUEUE_CONSUMED -> stringResource(
            R.string.battle_feedback_skill_consumed,
            contentTitle ?: stringResource(R.string.battle_feedback_skill)
        )
        BattleFeedbackKind.SKILL_QUEUE_CLEARED -> stringResource(
            R.string.battle_feedback_skill_cleared
        )
        BattleFeedbackKind.SKILL_USED -> stringResource(
            R.string.battle_feedback_skill_used,
            contentTitle ?: stringResource(R.string.battle_feedback_skill)
        )
        BattleFeedbackKind.DAMAGE -> feedback.amountDisplay?.let { amount ->
            if (feedback.impacts.any { it.critical }) {
                stringResource(R.string.battle_feedback_critical_damage, amount)
            } else {
                stringResource(R.string.battle_feedback_damage, amount)
            }
        } ?: stringResource(R.string.battle_feedback_unknown)
        BattleFeedbackKind.STATUS_APPLIED -> stringResource(
            R.string.battle_feedback_status_applied,
            contentTitle ?: stringResource(R.string.battle_feedback_status)
        )
        BattleFeedbackKind.HEALING -> feedback.amountDisplay?.let { amount ->
            stringResource(R.string.battle_feedback_heal, amount)
        } ?: stringResource(R.string.battle_feedback_unknown)
        BattleFeedbackKind.ENEMY_DEFEATED -> stringResource(R.string.battle_feedback_enemy_defeated)
        BattleFeedbackKind.CONVERGENCE -> stringResource(R.string.battle_feedback_convergence)
        BattleFeedbackKind.LOOT -> stringResource(R.string.battle_feedback_loot)
        BattleFeedbackKind.LEVEL_UP -> feedback.amountDisplay?.let { level ->
            stringResource(R.string.battle_feedback_level_up, level)
        } ?: stringResource(R.string.battle_feedback_unknown)
        BattleFeedbackKind.UPGRADE_PURCHASED -> stringResource(R.string.battle_feedback_upgrade)
        BattleFeedbackKind.COMBAT_ENDED -> stringResource(R.string.battle_feedback_combat_ended)
        BattleFeedbackKind.PLAYER_DEFEATED -> stringResource(R.string.battle_feedback_player_defeated)
        BattleFeedbackKind.BOSS_PHASE -> when (feedback.bossPhase) {
            1 -> stringResource(R.string.battle_feedback_boss_phase_bastion)
            2 -> stringResource(R.string.battle_feedback_boss_phase_reflection)
            3 -> stringResource(R.string.battle_feedback_boss_phase_fracture)
            else -> stringResource(
                R.string.battle_feedback_boss_phase_generic,
                feedback.bossPhase ?: 0,
                feedback.bossTotalPhases ?: 0
            )
        }
    }
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
    null -> stringResource(R.string.battle_feedback_unknown)
}

private fun feedbackAccent(feedback: BattleFeedbackUiState): Color = when {
    feedback.kind == BattleFeedbackKind.COMMAND_REJECTED ||
        feedback.kind == BattleFeedbackKind.PLAYER_DEFEATED -> ErrorRose
    feedback.kind == BattleFeedbackKind.BOSS_PHASE ||
        feedback.kind == BattleFeedbackKind.LOOT ||
        feedback.kind == BattleFeedbackKind.LEVEL_UP ||
        feedback.kind == BattleFeedbackKind.UPGRADE_PURCHASED ||
        feedback.impacts.any { it.critical } -> ResourceGold
    feedback.kind == BattleFeedbackKind.HEALING ||
        feedback.kind == BattleFeedbackKind.STATUS_APPLIED -> PositiveGreen
    else -> ResonanceTeal
}

private fun feedbackGlyph(feedback: BattleFeedbackUiState): String = when {
    feedback.kind == BattleFeedbackKind.COMMAND_REJECTED ||
        feedback.kind == BattleFeedbackKind.PLAYER_DEFEATED -> "!"
    feedback.kind == BattleFeedbackKind.LOOT ||
        feedback.kind == BattleFeedbackKind.LEVEL_UP -> "✦"
    feedback.kind == BattleFeedbackKind.HEALING -> "+"
    feedback.kind == BattleFeedbackKind.DAMAGE -> "⚔"
    else -> "•"
}

/** Platform monospace keeps the terminal influence dependency-free and readable on Android. */
private fun TextStyle.battleTerminal(): TextStyle = copy(
    fontFamily = FontFamily.Monospace,
    letterSpacing = if (letterSpacing.value == 0f) 0.2.sp else letterSpacing
)
