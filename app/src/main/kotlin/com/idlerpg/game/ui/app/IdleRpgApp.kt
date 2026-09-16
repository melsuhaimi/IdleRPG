package com.idlerpg.game.ui.app

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.background
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.idlerpg.game.R
import com.idlerpg.game.core.id.InstanceId
import com.idlerpg.game.presentation.intent.BattleUiIntent
import com.idlerpg.game.presentation.intent.GearUiIntent
import com.idlerpg.game.presentation.intent.ProgressUiIntent
import com.idlerpg.game.presentation.intent.SkillLoadoutUiIntent
import com.idlerpg.game.presentation.intent.WorldUiIntent
import com.idlerpg.game.presentation.model.OfflineProgressUiState
import com.idlerpg.game.presentation.model.OfflineStoppingReasonUi
import com.idlerpg.game.ui.accessibility.PresentationPreferencesState
import com.idlerpg.game.ui.accessibility.rememberPresentationPreferences
import com.idlerpg.game.presentation.doctrine.DoctrineDraftAction
import com.idlerpg.game.ui.component.hud.GlobalHud
import com.idlerpg.game.ui.component.premium.GameCard
import com.idlerpg.game.ui.component.premium.GameButton
import com.idlerpg.game.ui.component.premium.GameOutlinedButton
import com.idlerpg.game.ui.component.premium.GameLoadingSurface
import com.idlerpg.game.ui.navigation.GameDestination
import com.idlerpg.game.ui.navigation.IdleRpgBottomNavigation
import com.idlerpg.game.ui.navigation.NavigationBackAction
import com.idlerpg.game.ui.navigation.NavigationBackPolicy
import com.idlerpg.game.ui.navigation.ProgressDestination
import com.idlerpg.game.ui.navigation.ProgressDestinationBar
import com.idlerpg.game.ui.screen.battle.BattleScreen
import com.idlerpg.game.ui.screen.doctrine.DoctrineScreen
import com.idlerpg.game.ui.screen.gear.GearScreen
import com.idlerpg.game.ui.screen.progress.ProgressScreen
import com.idlerpg.game.ui.screen.skill.SkillLoadoutScreen
import com.idlerpg.game.ui.screen.world.WorldScreen
import com.idlerpg.game.ui.theme.GameMotionScope
import com.idlerpg.game.ui.theme.GameDimensions
import com.idlerpg.game.ui.theme.LocalReducedMotion
import com.idlerpg.game.ui.theme.MotionTokens
import com.idlerpg.game.ui.theme.ObsidianBackground
import com.idlerpg.game.ui.theme.ObsidianSurface1
import com.idlerpg.game.ui.theme.ResourceGold
import com.idlerpg.game.ui.theme.TextPrimary
import com.idlerpg.game.ui.theme.TextSecondary

/** FUI-10 production shell with hardened offline/lifecycle and runtime-error UX. */
@Composable
fun IdleRpgApp(
    state: IdleRpgUiState,
    onRetryInitialization: () -> Unit,
    onRetryBackgroundResume: () -> Unit,
    onContinueWithoutBackgroundProgress: () -> Unit,
    onAcknowledgeOfflineSummary: () -> Unit,
    onSaveNow: () -> Unit,
    onRetrySave: () -> Unit,
    onClearOperationError: () -> Unit,
    onContinueGame: () -> Unit = {},
    onStartNewGame: (String) -> Unit = {},
    onSetHeroName: (String) -> Unit = {},
    onQuitGame: () -> Unit = {},
    onSelectDestination: (GameDestination) -> Unit = {},
    onSelectProgressDestination: (ProgressDestination) -> Unit = {},
    onBattleIntent: (BattleUiIntent) -> Unit = {},
    onConsumeBattlePresentationReward: (Long) -> Unit = {},
    onOpenSkillLoadout: () -> Unit = {},
    onCloseSkillLoadout: () -> Unit = {},
    onSkillLoadoutIntent: (SkillLoadoutUiIntent) -> Unit = {},
    onWorldIntent: (WorldUiIntent) -> Unit = {},
    onOpenGear: (InstanceId?) -> Unit = {},
    onGearIntent: (GearUiIntent) -> Unit = {},
    onProgressIntent: (ProgressUiIntent) -> Unit = {},
    onDismissChroniclePreview: () -> Unit = {},
    onSetDoctrineEnabled: (Boolean) -> Unit = {},
    onApplyDoctrinePreset: (com.idlerpg.game.domain.command.DoctrinePreset) -> Unit = {},
    onSetDoctrineRuleEnabled: (com.idlerpg.game.core.id.InstanceId, Boolean) -> Unit = { _, _ -> },
    onMoveDoctrineRule: (com.idlerpg.game.core.id.InstanceId, Int) -> Unit = { _, _ -> },
    onRemoveDoctrineRule: (com.idlerpg.game.core.id.InstanceId) -> Unit = {},
    onBeginAddDoctrineRule: () -> Unit = {},
    onBeginEditDoctrineRule: (com.idlerpg.game.core.id.InstanceId) -> Unit = {},
    onDoctrineDraftAction: (DoctrineDraftAction) -> Unit = {},
    onCommitDoctrineDraft: () -> Unit = {},
    onCancelDoctrineDraft: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val presentationPreferences = rememberPresentationPreferences()
    var accessibilityDialogOpen by rememberSaveable { mutableStateOf(false) }
    val backAction = NavigationBackPolicy.resolve(
        destination = state.destination,
        progressDestination = state.progressDestination,
        skillLoadoutOpen = state.skillLoadoutOpen
    )

    BackHandler(
        enabled = state.status == IdleRpgAppStatus.READY &&
            state.offlineSummary == null &&
            !accessibilityDialogOpen &&
            backAction != NavigationBackAction.NONE
    ) {
        when (backAction) {
            NavigationBackAction.CLOSE_SKILL_LOADOUT -> onCloseSkillLoadout()
            NavigationBackAction.SHOW_PROGRESS_OVERVIEW ->
                onSelectProgressDestination(ProgressDestination.OVERVIEW)
            NavigationBackAction.OPEN_BUILD -> onSelectDestination(GameDestination.GEAR)
            NavigationBackAction.OPEN_BATTLE -> onSelectDestination(GameDestination.BATTLE)
            NavigationBackAction.NONE -> Unit
        }
    }

    GameMotionScope(reducedMotion = presentationPreferences.reducedMotion) {
        Surface(
            modifier = modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            when (state.status) {
            IdleRpgAppStatus.BOOTING -> BootSurface(
                title = stringResource(R.string.runtime_status_booting)
            )
            IdleRpgAppStatus.NEW_GAME -> BootSurface(
                title = stringResource(R.string.runtime_status_new_game)
            )
            IdleRpgAppStatus.MENU -> MainMenuSurface(
                hasExistingSave = state.hasExistingSave,
                onContinueGame = onContinueGame,
                onStartNewGame = onStartNewGame,
                onOpenSettings = { accessibilityDialogOpen = true },
                onQuitGame = onQuitGame
            )
            IdleRpgAppStatus.NAME_REQUIRED -> HeroNameRequiredSurface(
                onSetHeroName = onSetHeroName
            )
            IdleRpgAppStatus.LOADING -> BootSurface(
                title = stringResource(R.string.runtime_status_loading)
            )
            IdleRpgAppStatus.RESUMING_OFFLINE -> BootSurface(
                title = stringResource(R.string.runtime_status_resuming_offline)
            )
            IdleRpgAppStatus.ERROR -> RuntimeErrorSurface(
                failure = state.runtimeFailure,
                canRetryInitialization = state.canRetryInitialization,
                onRetryInitialization = onRetryInitialization,
                onRetryBackgroundResume = onRetryBackgroundResume,
                onContinueWithoutBackgroundProgress = onContinueWithoutBackgroundProgress
            )
            IdleRpgAppStatus.READY -> ReadyShell(
                state = state,
                hapticsEnabled = presentationPreferences.hapticsEnabled,
                onSaveNow = onSaveNow,
                onOpenAccessibility = { accessibilityDialogOpen = true },
                onRetrySave = onRetrySave,
                onClearOperationError = onClearOperationError,
                onAcknowledgeOfflineSummary = onAcknowledgeOfflineSummary,
                onSelectDestination = onSelectDestination,
                onSelectProgressDestination = onSelectProgressDestination,
                onBattleIntent = onBattleIntent,
                onConsumeBattlePresentationReward = onConsumeBattlePresentationReward,
                onOpenSkillLoadout = onOpenSkillLoadout,
                onCloseSkillLoadout = onCloseSkillLoadout,
                onSkillLoadoutIntent = onSkillLoadoutIntent,
                onWorldIntent = onWorldIntent,
                onOpenGear = onOpenGear,
                onGearIntent = onGearIntent,
                onProgressIntent = onProgressIntent,
                onDismissChroniclePreview = onDismissChroniclePreview,
                onSetDoctrineEnabled = onSetDoctrineEnabled,
                onApplyDoctrinePreset = onApplyDoctrinePreset,
                onSetDoctrineRuleEnabled = onSetDoctrineRuleEnabled,
                onMoveDoctrineRule = onMoveDoctrineRule,
                onRemoveDoctrineRule = onRemoveDoctrineRule,
                onBeginAddDoctrineRule = onBeginAddDoctrineRule,
                onBeginEditDoctrineRule = onBeginEditDoctrineRule,
                onDoctrineDraftAction = onDoctrineDraftAction,
                onCommitDoctrineDraft = onCommitDoctrineDraft,
                onCancelDoctrineDraft = onCancelDoctrineDraft
            )
        }
    }
    }

    if (accessibilityDialogOpen) {
        AccessibilityPreferencesDialog(
            preferences = presentationPreferences,
            onDismiss = { accessibilityDialogOpen = false }
        )
    }
}

@Composable
private fun ReadyShell(
    state: IdleRpgUiState,
    hapticsEnabled: Boolean,
    onSaveNow: () -> Unit,
    onOpenAccessibility: () -> Unit,
    onRetrySave: () -> Unit,
    onClearOperationError: () -> Unit,
    onAcknowledgeOfflineSummary: () -> Unit,
    onSelectDestination: (GameDestination) -> Unit,
    onSelectProgressDestination: (ProgressDestination) -> Unit,
    onBattleIntent: (BattleUiIntent) -> Unit,
    onConsumeBattlePresentationReward: (Long) -> Unit,
    onOpenSkillLoadout: () -> Unit,
    onCloseSkillLoadout: () -> Unit,
    onSkillLoadoutIntent: (SkillLoadoutUiIntent) -> Unit,
    onWorldIntent: (WorldUiIntent) -> Unit,
    onOpenGear: (InstanceId?) -> Unit,
    onGearIntent: (GearUiIntent) -> Unit,
    onProgressIntent: (ProgressUiIntent) -> Unit,
    onDismissChroniclePreview: () -> Unit,
    onSetDoctrineEnabled: (Boolean) -> Unit,
    onApplyDoctrinePreset: (com.idlerpg.game.domain.command.DoctrinePreset) -> Unit,
    onSetDoctrineRuleEnabled: (com.idlerpg.game.core.id.InstanceId, Boolean) -> Unit,
    onMoveDoctrineRule: (com.idlerpg.game.core.id.InstanceId, Int) -> Unit,
    onRemoveDoctrineRule: (com.idlerpg.game.core.id.InstanceId) -> Unit,
    onBeginAddDoctrineRule: () -> Unit,
    onBeginEditDoctrineRule: (com.idlerpg.game.core.id.InstanceId) -> Unit,
    onDoctrineDraftAction: (DoctrineDraftAction) -> Unit,
    onCommitDoctrineDraft: () -> Unit,
    onCancelDoctrineDraft: () -> Unit
) {
    val hud = state.globalHud ?: return

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            GlobalHud(
                state = hud,
                onSaveNow = onSaveNow,
                onOpenAccessibility = onOpenAccessibility,
                modifier = Modifier.padding(
                    horizontal = GameDimensions.ShellHorizontalPadding,
                    vertical = 4.dp
                )
            )
        },
        bottomBar = {
            IdleRpgBottomNavigation(
                selected = state.destination,
                onSelect = onSelectDestination,
                destinations = state.navigationAvailability.primaryDestinations,
                modifier = Modifier.padding(
                    horizontal = GameDimensions.ShellHorizontalPadding,
                    vertical = 4.dp
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            state.runtimeFailure
                ?.takeIf { failure ->
                    failure.kind == IdleRpgRuntimeFailureKind.SAVE ||
                        failure.kind == IdleRpgRuntimeFailureKind.COMMAND
                }
                ?.let { failure ->
                    OperationErrorCard(
                        failure = failure,
                        onRetrySave = onRetrySave,
                        onClear = onClearOperationError,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }

            if (state.destination == GameDestination.PROGRESS) {
                ProgressDestinationBar(
                    selected = state.progressDestination,
                    onSelect = onSelectProgressDestination,
                    destinations = state.navigationAvailability.progressDestinations,
                    modifier = Modifier.padding(start = 0.dp, top = 8.dp, end = 0.dp, bottom = 0.dp)
                )
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                val reducedMotion = LocalReducedMotion.current
                val backdropResId = when (state.destination) {
                    GameDestination.BATTLE -> R.drawable.battle_backdrop_warden_generated
                    GameDestination.WORLD -> R.drawable.bg_training_hollow_outer_fracture
                    GameDestination.DOCTRINE -> R.drawable.bg_training_hollow_resonant_depths
                    GameDestination.GEAR -> R.drawable.bg_training_hollow_outer_fracture
                    GameDestination.PROGRESS -> R.drawable.bg_training_hollow_warden_core
                }
                Image(
                    painter = painterResource(backdropResId),
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxSize()
                        .alpha(0.14f),
                    contentScale = ContentScale.Crop
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    Color.Transparent,
                                    ObsidianBackground.copy(alpha = 0.28f),
                                    ObsidianBackground.copy(alpha = 0.82f)
                                )
                            )
                        )
                )
                AnimatedContent(
                    targetState = state.destination,
                    transitionSpec = {
                        if (reducedMotion) {
                            fadeIn(tween(1)) togetherWith fadeOut(tween(1))
                        } else {
                            fadeIn(tween(MotionTokens.CARD_MILLIS)) togetherWith
                                fadeOut(tween(MotionTokens.MICRO_MILLIS))
                        }
                    },
                    label = "destination_transition"
                ) { destination ->
                    DestinationContent(
                        state = state,
                        destination = destination,
                        hapticsEnabled = hapticsEnabled,
                        onSelectDestination = onSelectDestination,
                        onBattleIntent = onBattleIntent,
                        onConsumeBattlePresentationReward = onConsumeBattlePresentationReward,
                        onOpenSkillLoadout = onOpenSkillLoadout,
                        onCloseSkillLoadout = onCloseSkillLoadout,
                        onSkillLoadoutIntent = onSkillLoadoutIntent,
                        onWorldIntent = onWorldIntent,
                        onOpenGear = onOpenGear,
                        onGearIntent = onGearIntent,
                        onProgressIntent = onProgressIntent,
                        onDismissChroniclePreview = onDismissChroniclePreview,
                        onSetDoctrineEnabled = onSetDoctrineEnabled,
                        onApplyDoctrinePreset = onApplyDoctrinePreset,
                        onSetDoctrineRuleEnabled = onSetDoctrineRuleEnabled,
                        onMoveDoctrineRule = onMoveDoctrineRule,
                        onRemoveDoctrineRule = onRemoveDoctrineRule,
                        onBeginAddDoctrineRule = onBeginAddDoctrineRule,
                        onBeginEditDoctrineRule = onBeginEditDoctrineRule,
                        onDoctrineDraftAction = onDoctrineDraftAction,
                        onCommitDoctrineDraft = onCommitDoctrineDraft,
                        onCancelDoctrineDraft = onCancelDoctrineDraft
                    )
                }
            }
        }
    }

    state.offlineSummary?.let { summary ->
        OfflineProgressDialog(
            summary = summary,
            onContinue = onAcknowledgeOfflineSummary
        )
    }
}

@Composable
private fun DestinationContent(
    state: IdleRpgUiState,
    destination: GameDestination,
    hapticsEnabled: Boolean,
    onSelectDestination: (GameDestination) -> Unit,
    onBattleIntent: (BattleUiIntent) -> Unit,
    onConsumeBattlePresentationReward: (Long) -> Unit,
    onOpenSkillLoadout: () -> Unit,
    onCloseSkillLoadout: () -> Unit,
    onSkillLoadoutIntent: (SkillLoadoutUiIntent) -> Unit,
    onWorldIntent: (WorldUiIntent) -> Unit,
    onOpenGear: (InstanceId?) -> Unit,
    onGearIntent: (GearUiIntent) -> Unit,
    onProgressIntent: (ProgressUiIntent) -> Unit,
    onDismissChroniclePreview: () -> Unit,
    onSetDoctrineEnabled: (Boolean) -> Unit,
    onApplyDoctrinePreset: (com.idlerpg.game.domain.command.DoctrinePreset) -> Unit,
    onSetDoctrineRuleEnabled: (com.idlerpg.game.core.id.InstanceId, Boolean) -> Unit,
    onMoveDoctrineRule: (com.idlerpg.game.core.id.InstanceId, Int) -> Unit,
    onRemoveDoctrineRule: (com.idlerpg.game.core.id.InstanceId) -> Unit,
    onBeginAddDoctrineRule: () -> Unit,
    onBeginEditDoctrineRule: (com.idlerpg.game.core.id.InstanceId) -> Unit,
    onDoctrineDraftAction: (DoctrineDraftAction) -> Unit,
    onCommitDoctrineDraft: () -> Unit,
    onCancelDoctrineDraft: () -> Unit
) {
    when (destination) {
        GameDestination.BATTLE -> {
            val battle = state.battle
            if (battle == null) {
                PhasePlaceholderScreen(
                    eyebrow = stringResource(R.string.nav_battle),
                    title = stringResource(R.string.screen_battle_title),
                    description = stringResource(R.string.battle_projection_unavailable)
                )
            } else {
                BattleScreen(
                    state = battle,
                    hapticsEnabled = hapticsEnabled,
                    onIntent = onBattleIntent,
                    onConsumePresentationReward = onConsumeBattlePresentationReward,
                    onOpenSkillLoadout = onOpenSkillLoadout,
                    onOpenBuild = onOpenGear,
                    onOpenDoctrine = { onSelectDestination(GameDestination.DOCTRINE) },
                    onSetDoctrineEnabled = onSetDoctrineEnabled,
                    onOpenFarm = { onSelectDestination(GameDestination.WORLD) }
                )
            }
        }
        GameDestination.WORLD -> {
            val world = state.world
            if (world == null) {
                PhasePlaceholderScreen(
                    eyebrow = stringResource(R.string.nav_world),
                    title = stringResource(R.string.screen_world_title),
                    description = stringResource(R.string.world_projection_unavailable)
                )
            } else {
                WorldScreen(
                    state = world,
                    onIntent = onWorldIntent,
                    onOpenGear = { onOpenGear(null) }
                )
            }
        }
        GameDestination.DOCTRINE -> {
            val doctrine = state.doctrine
            if (doctrine == null) {
                PhasePlaceholderScreen(
                    eyebrow = stringResource(R.string.nav_doctrine),
                    title = stringResource(R.string.screen_doctrine_title),
                    description = stringResource(R.string.doctrine_projection_unavailable)
                )
            } else {
                DoctrineScreen(
                    state = doctrine,
                    onApplyPreset = onApplyDoctrinePreset,
                    onSetDoctrineEnabled = onSetDoctrineEnabled,
                    onSetRuleEnabled = onSetDoctrineRuleEnabled,
                    onMoveRule = onMoveDoctrineRule,
                    onRemoveRule = onRemoveDoctrineRule,
                    onBeginAddRule = onBeginAddDoctrineRule,
                    onBeginEditRule = onBeginEditDoctrineRule,
                    onDraftAction = onDoctrineDraftAction,
                    onCommitDraft = onCommitDoctrineDraft,
                    onCancelDraft = onCancelDoctrineDraft
                )
            }
        }
        GameDestination.GEAR -> {
            if (state.skillLoadoutOpen) {
                val loadout = state.skillLoadout
                if (loadout == null) {
                    PhasePlaceholderScreen(
                        eyebrow = stringResource(R.string.nav_gear),
                        title = stringResource(R.string.loadout_title),
                        description = stringResource(R.string.loadout_projection_unavailable)
                    )
                } else {
                    SkillLoadoutScreen(
                        state = loadout,
                        onIntent = onSkillLoadoutIntent,
                        onClose = onCloseSkillLoadout
                    )
                }
            } else {
                val gear = state.gear
                if (gear == null) {
                    PhasePlaceholderScreen(
                        eyebrow = stringResource(R.string.nav_gear),
                        title = stringResource(R.string.screen_gear_title),
                        description = stringResource(R.string.gear_projection_unavailable)
                    )
                } else {
                    GearScreen(
                        state = gear,
                        focusItemId = state.gearFocusItemId,
                        onIntent = onGearIntent,
                        onOpenSkills = onOpenSkillLoadout,
                        onOpenDoctrine = {
                            onSelectDestination(GameDestination.DOCTRINE)
                        }
                    )
                }
            }
        }
        GameDestination.PROGRESS -> {
            val progress = state.progress
            if (progress == null) {
                PhasePlaceholderScreen(
                    eyebrow = stringResource(R.string.nav_progress),
                    title = stringResource(state.progressDestination.labelResId),
                    description = stringResource(R.string.progress_projection_unavailable)
                )
            } else {
                ProgressScreen(
                    state = progress,
                    destination = state.progressDestination,
                    onIntent = onProgressIntent,
                    onDismissChroniclePreview = onDismissChroniclePreview
                )
            }
        }
    }
}

@Composable
private fun PhasePlaceholderScreen(
    eyebrow: String,
    title: String,
    description: String
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = eyebrow.uppercase(),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.secondary
        )
        Text(
            text = title,
            style = MaterialTheme.typography.headlineMedium
        )
        Text(
            text = description,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(8.dp))
        GameCard(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            accent = MaterialTheme.colorScheme.primary
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = stringResource(R.string.shell_canonical_runtime_live),
                    style = MaterialTheme.typography.titleSmall
                )
            }
        }
    }
}

@Composable
private fun BootSurface(title: String) {
    GameLoadingSurface(title = title)
}

@Composable
private fun MainMenuSurface(
    hasExistingSave: Boolean,
    onContinueGame: () -> Unit,
    onStartNewGame: (String) -> Unit,
    onOpenSettings: () -> Unit,
    onQuitGame: () -> Unit
) {
    var nameDialogOpen by rememberSaveable { mutableStateOf(false) }
    var newGameConfirmationOpen by rememberSaveable { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
        Image(
            painter = painterResource(R.drawable.bg_battle_premium),
            contentDescription = null,
            modifier = Modifier
                .fillMaxSize()
                .alpha(0.72f),
            contentScale = ContentScale.Crop
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    androidx.compose.ui.graphics.Brush.verticalGradient(
                        listOf(Color(0xF0080A12), Color(0xB70C1020), Color(0xF0080A12))
                    )
                )
        )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 34.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = stringResource(R.string.app_name_display).uppercase(),
                style = MaterialTheme.typography.displaySmall,
                color = ResourceGold,
                textAlign = TextAlign.Center
            )
            Text(
                text = stringResource(R.string.main_menu_subtitle),
                style = MaterialTheme.typography.titleMedium,
                color = TextPrimary,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 4.dp)
            )
            Text(
                text = stringResource(R.string.main_menu_description),
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .padding(top = 12.dp, bottom = 26.dp)
                    .widthIn(max = 340.dp)
            )

            GameCard(
                modifier = Modifier.widthIn(max = 420.dp),
                colors = CardDefaults.cardColors(
                    containerColor = ObsidianSurface1.copy(alpha = 0.94f)
                ),
                accent = ResourceGold
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (hasExistingSave) {
                        GameButton(
                            onClick = onContinueGame,
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 52.dp)
                        ) {
                            Text(stringResource(R.string.main_menu_continue))
                        }
                        GameOutlinedButton(
                            onClick = { newGameConfirmationOpen = true },
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 48.dp)
                        ) {
                            Text(stringResource(R.string.main_menu_new_expedition))
                        }
                    } else {
                        GameButton(
                            onClick = { nameDialogOpen = true },
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 52.dp)
                        ) {
                            Text(stringResource(R.string.main_menu_start_expedition))
                        }
                    }
                    TextButton(
                        onClick = onOpenSettings,
                        modifier = Modifier.heightIn(min = 48.dp)
                    ) {
                        Text(stringResource(R.string.main_menu_settings))
                    }
                    TextButton(
                        onClick = onQuitGame,
                        modifier = Modifier.heightIn(min = 48.dp)
                    ) {
                        Text(stringResource(R.string.main_menu_quit))
                    }
                }
            }
        }
    }

    if (newGameConfirmationOpen) {
        AlertDialog(
            onDismissRequest = { newGameConfirmationOpen = false },
            title = { Text(stringResource(R.string.main_menu_new_game_confirm_title)) },
            text = { Text(stringResource(R.string.main_menu_new_game_confirm_message)) },
            dismissButton = {
                TextButton(onClick = { newGameConfirmationOpen = false }) {
                    Text(stringResource(R.string.main_menu_cancel))
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        newGameConfirmationOpen = false
                        nameDialogOpen = true
                    }
                ) {
                    Text(stringResource(R.string.main_menu_new_expedition))
                }
            }
        )
    }

    if (nameDialogOpen) {
        HeroNameDialog(
            onDismiss = { nameDialogOpen = false },
            onConfirm = { name ->
                nameDialogOpen = false
                onStartNewGame(name)
            }
        )
    }
}

@Composable
private fun HeroNameRequiredSurface(
    onSetHeroName: (String) -> Unit
) {
    var name by rememberSaveable { mutableStateOf("") }
    val normalized = name.trim()
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ObsidianBackground),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .padding(24.dp)
                .widthIn(max = 420.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = stringResource(R.string.hero_name_required_title),
                style = MaterialTheme.typography.headlineSmall,
                color = ResourceGold,
                textAlign = TextAlign.Center
            )
            Text(
                text = stringResource(R.string.hero_name_required_message),
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
                textAlign = TextAlign.Center
            )
            OutlinedTextField(
                value = name,
                onValueChange = { name = it.take(20) },
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.hero_name_label)) },
                singleLine = true,
                supportingText = { Text(stringResource(R.string.hero_name_length_hint)) }
            )
            GameButton(
                onClick = { onSetHeroName(normalized) },
                enabled = normalized.isNotEmpty(),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 52.dp)
            ) {
                Text(stringResource(R.string.hero_name_continue))
            }
        }
    }
}

@Composable
private fun HeroNameDialog(
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var name by rememberSaveable { mutableStateOf("") }
    val normalized = name.trim()
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.hero_name_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(stringResource(R.string.hero_name_message))
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it.take(20) },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(R.string.hero_name_label)) },
                    singleLine = true,
                    supportingText = { Text(stringResource(R.string.hero_name_length_hint)) }
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.main_menu_cancel))
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(normalized) },
                enabled = normalized.isNotEmpty()
            ) {
                Text(stringResource(R.string.hero_name_continue))
            }
        }
    )
}

@Composable
private fun RuntimeErrorSurface(
    failure: RuntimeFailureUiState?,
    canRetryInitialization: Boolean,
    onRetryInitialization: () -> Unit,
    onRetryBackgroundResume: () -> Unit,
    onContinueWithoutBackgroundProgress: () -> Unit
) {
    val kind = failure?.kind
    val title = when (kind) {
        IdleRpgRuntimeFailureKind.INITIALIZATION ->
            stringResource(R.string.runtime_failure_initialization_title)
        IdleRpgRuntimeFailureKind.BACKGROUND_RESUME ->
            stringResource(R.string.runtime_failure_background_resume_title)
        IdleRpgRuntimeFailureKind.SIMULATION ->
            stringResource(R.string.runtime_failure_simulation_title)
        IdleRpgRuntimeFailureKind.SAVE ->
            stringResource(R.string.runtime_failure_save_title)
        IdleRpgRuntimeFailureKind.COMMAND ->
            stringResource(R.string.runtime_failure_command_title)
        null -> stringResource(R.string.runtime_status_error)
    }
    val message = when (kind) {
        IdleRpgRuntimeFailureKind.INITIALIZATION ->
            stringResource(R.string.runtime_failure_initialization_message)
        IdleRpgRuntimeFailureKind.BACKGROUND_RESUME ->
            stringResource(R.string.runtime_failure_background_resume_message)
        IdleRpgRuntimeFailureKind.SIMULATION ->
            stringResource(R.string.runtime_failure_simulation_message)
        IdleRpgRuntimeFailureKind.SAVE ->
            stringResource(R.string.runtime_failure_save_message)
        IdleRpgRuntimeFailureKind.COMMAND ->
            stringResource(R.string.runtime_failure_command_message)
        null -> stringResource(R.string.runtime_error_unknown)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center
    ) {
        GameCard(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.errorContainer
            ),
            accent = MaterialTheme.colorScheme.error
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
                Text(
                    text = message,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
                when (kind) {
                    IdleRpgRuntimeFailureKind.INITIALIZATION ->
                        if (canRetryInitialization) {
                            GameButton(onClick = onRetryInitialization) {
                                Text(stringResource(R.string.runtime_retry_load))
                            }
                        }
                    IdleRpgRuntimeFailureKind.BACKGROUND_RESUME -> {
                        GameButton(onClick = onRetryBackgroundResume) {
                            Text(stringResource(R.string.runtime_retry_offline_resume))
                        }
                        TextButton(onClick = onContinueWithoutBackgroundProgress) {
                            Text(stringResource(R.string.runtime_continue_without_offline))
                        }
                        Text(
                            text = stringResource(R.string.runtime_continue_without_offline_warning),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                    IdleRpgRuntimeFailureKind.SIMULATION ->
                        Text(
                            text = stringResource(R.string.runtime_simulation_fault_recovery),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    else -> Unit
                }
            }
        }
    }
}

@Composable
private fun OperationErrorCard(
    failure: RuntimeFailureUiState,
    onRetrySave: () -> Unit,
    onClear: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isSaveFailure = failure.kind == IdleRpgRuntimeFailureKind.SAVE
    val title = if (isSaveFailure) {
        stringResource(R.string.runtime_failure_save_title)
    } else {
        stringResource(R.string.runtime_failure_command_title)
    }
    val message = if (isSaveFailure) {
        stringResource(R.string.runtime_failure_save_message)
    } else {
        stringResource(R.string.runtime_failure_command_message)
    }

    GameCard(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer
        ),
        accent = MaterialTheme.colorScheme.error
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onErrorContainer
            )
            Text(
                text = message,
                color = MaterialTheme.colorScheme.onErrorContainer
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (isSaveFailure) {
                    TextButton(onClick = onRetrySave) {
                        Text(stringResource(R.string.runtime_retry_save))
                    }
                }
                TextButton(onClick = onClear) {
                    Text(stringResource(R.string.runtime_dismiss_error))
                }
            }
        }
    }
}

@Composable
private fun AccessibilityPreferencesDialog(
    preferences: PresentationPreferencesState,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(stringResource(R.string.accessibility_title))
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.accessibility_reduced_motion),
                            style = MaterialTheme.typography.titleSmall
                        )
                        Text(
                            text = stringResource(R.string.accessibility_reduced_motion_description),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = preferences.reducedMotion,
                        onCheckedChange = preferences::updateReducedMotion
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.accessibility_haptics),
                            style = MaterialTheme.typography.titleSmall
                        )
                        Text(
                            text = stringResource(R.string.accessibility_haptics_description),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = preferences.hapticsEnabled,
                        onCheckedChange = preferences::updateHapticsEnabled
                    )
                }
                Text(
                    text = stringResource(R.string.accessibility_gameplay_timing_note),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.accessibility_close))
            }
        }
    )
}

@Composable
private fun OfflineProgressDialog(
    summary: OfflineProgressUiState,
    onContinue: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onContinue,
        title = {
            Text(stringResource(R.string.offline_summary_title))
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = stringResource(
                        R.string.offline_duration_format,
                        summary.simulatedDurationDisplay
                    ),
                    style = MaterialTheme.typography.titleSmall
                )
                if (summary.durationClamped) {
                    Text(
                        text = stringResource(
                            R.string.offline_requested_duration_format,
                            summary.requestedDurationDisplay
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                summary.levelJourney?.let {
                    OfflineMetricLine(stringResource(R.string.offline_level_change), it)
                }
                OfflineMetricLine(
                    label = stringResource(R.string.offline_stopping_reason),
                    value = offlineStoppingReasonLabel(summary.stoppingReason)
                )
                summary.stageJourney?.let {
                    OfflineMetricLine(stringResource(R.string.offline_stage_journey), it)
                }
                summary.deepestStage?.let {
                    OfflineMetricLine(stringResource(R.string.offline_highest_stage), it)
                }

                if (summary.enemiesDefeated != "0") OfflineMetricLine(stringResource(R.string.offline_kills), summary.enemiesDefeated)
                if (summary.encountersCleared != "0") OfflineMetricLine(stringResource(R.string.offline_encounters), summary.encountersCleared)
                if (summary.goldGranted != "0") OfflineMetricLine(stringResource(R.string.offline_gold), "+${summary.goldGranted}")
                if (summary.experienceGranted != "0") OfflineMetricLine(stringResource(R.string.offline_xp), "+${summary.experienceGranted}")
                if (summary.masteryGranted != "0") OfflineMetricLine(stringResource(R.string.offline_mastery), "+${summary.masteryGranted}")
                if (summary.itemsFound != "0") OfflineMetricLine(stringResource(R.string.offline_items), summary.itemsFound)
                if (summary.itemsKept != "0") OfflineMetricLine(stringResource(R.string.offline_items_kept), summary.itemsKept)
                if (summary.itemsOverflowed != "0") {
                    OfflineMetricLine(stringResource(R.string.offline_items_overflowed), summary.itemsOverflowed)
                }
                if (summary.itemsAutoSalvaged != "0") OfflineMetricLine(
                    stringResource(R.string.offline_auto_salvaged),
                    "${summary.itemsAutoSalvaged} · +${summary.autoSalvageGold} Gold"
                )
                if (summary.eliteEncountersCleared != "0") OfflineMetricLine(stringResource(R.string.offline_elites), summary.eliteEncountersCleared)
                if (summary.anomalyEncountersCleared != "0") OfflineMetricLine(stringResource(R.string.offline_anomalies), summary.anomalyEncountersCleared)
                if (summary.bossesDefeated != "0") OfflineMetricLine(stringResource(R.string.offline_bosses), summary.bossesDefeated)
                if (summary.convergencesTriggered != "0") OfflineMetricLine(
                    label = stringResource(R.string.offline_convergences),
                    value = summary.convergencesTriggered
                )

                if (summary.notableDrops.isNotEmpty()) {
                    Text(
                        text = stringResource(R.string.offline_notable_drops),
                        style = MaterialTheme.typography.titleSmall
                    )
                    summary.notableDrops.forEach { drop ->
                        Text(text = drop, style = MaterialTheme.typography.bodyMedium)
                    }
                }
                summary.currentWallStage?.let {
                    OfflineMetricLine(stringResource(R.string.offline_current_wall), it)
                }
                if (summary.adaptationTierChanges != "0") OfflineMetricLine(
                    label = stringResource(R.string.offline_adaptation_changes),
                    value = summary.adaptationTierChanges
                )

                if (summary.durationClamped) {
                    Text(
                        text = stringResource(R.string.offline_clamped),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (summary.clockRollbackDetected) {
                    Text(
                        text = stringResource(R.string.offline_clock_rollback),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        confirmButton = {
            GameButton(onClick = onContinue) {
                Text(stringResource(R.string.offline_continue))
            }
        }
    )
}

@Composable
private fun offlineStoppingReasonLabel(reason: OfflineStoppingReasonUi): String = when (reason) {
    OfflineStoppingReasonUi.ELAPSED -> stringResource(R.string.offline_stop_elapsed)
    OfflineStoppingReasonUi.CLAIM_WINDOW_CAPPED -> stringResource(R.string.offline_stop_claim_window)
    OfflineStoppingReasonUi.CLOCK_ROLLBACK -> stringResource(R.string.offline_stop_clock_rollback)
    OfflineStoppingReasonUi.NO_ELIGIBLE_FARM_STAGE -> stringResource(R.string.offline_stop_no_farm_stage)
}

@Composable
private fun OfflineMetricLine(
    label: String,
    value: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            modifier = Modifier.padding(start = 12.dp),
            textAlign = TextAlign.End,
            style = MaterialTheme.typography.labelLarge
        )
    }
}
