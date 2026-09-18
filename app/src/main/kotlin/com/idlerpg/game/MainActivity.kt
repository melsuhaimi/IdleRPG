package com.idlerpg.game

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.idlerpg.game.presentation.intent.ProgressUiIntent
import com.idlerpg.game.ui.app.ForegroundSimulationDriver
import com.idlerpg.game.ui.app.IdleRpgApp
import com.idlerpg.game.ui.app.IdleRpgApplication
import com.idlerpg.game.ui.app.IdleRpgViewModel
import com.idlerpg.game.ui.theme.IdleRPGTheme

/** Thin production Compose host. Canonical runtime ownership lives in IdleRpgApplication. */
class MainActivity : ComponentActivity() {
    private val gameViewModel: IdleRpgViewModel by viewModels {
        IdleRpgViewModel.factory(
            runtimeController = appContainer.runtimeController,
            globalHudProjector = appContainer.globalHudProjector,
            battleProjector = appContainer.battleProjector,
            skillLoadoutProjector = appContainer.skillLoadoutProjector,
            worldProjector = appContainer.worldProjector,
            doctrineProjector = appContainer.doctrineProjector,
            gearProjector = appContainer.gearProjector,
            progressProjector = appContainer.progressProjector,
            offlineProgressProjector = appContainer.offlineProgressProjector,
            gameEventPresenter = appContainer.gameEventPresenter
        )
    }

    private lateinit var foregroundSimulationDriver: ForegroundSimulationDriver

    private val appContainer
        get() = (application as IdleRpgApplication).container

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        foregroundSimulationDriver = ForegroundSimulationDriver(
            runtimeController = appContainer.runtimeController
        )

        setContent {
            IdleRPGTheme {
                val state by gameViewModel.uiState.collectAsState()
                val chroniclePreviewSessionActive =
                    state.progress?.chroniclePreviewSessionActive == true

                LaunchedEffect(chroniclePreviewSessionActive) {
                    if (chroniclePreviewSessionActive) {
                        foregroundSimulationDriver.pauseForCanonicalPreview()
                    } else {
                        foregroundSimulationDriver.resumeAfterCanonicalPreview()
                    }
                }

                IdleRpgApp(
                    state = state,
                    onRetryInitialization = gameViewModel::retryInitialization,
                    onRetryBackgroundResume = gameViewModel::retryBackgroundResume,
                    onContinueWithoutBackgroundProgress =
                        gameViewModel::continueWithoutBackgroundProgress,
                    onAcknowledgeOfflineSummary = gameViewModel::acknowledgeOfflineSummary,
                    onSaveNow = gameViewModel::saveNow,
                    onRetrySave = gameViewModel::retrySave,
                    onClearOperationError = gameViewModel::clearOperationError,
                    onContinueGame = gameViewModel::continueGame,
                    onContinueWithSavedGame = {
                        foregroundSimulationDriver.resetAfterRuntimeRecovery()
                        gameViewModel.continueWithSavedGame()
                    },
                    onStartNewGame = gameViewModel::startNewGame,
                    onSetHeroName = gameViewModel::setHeroName,
                    onQuitGame = { finishAndRemoveTask() },
                    onSelectDestination = gameViewModel::selectDestination,
                    onSelectProgressDestination = gameViewModel::selectProgressDestination,
                    onBattleIntent = gameViewModel::handleBattleIntent,
                    onConsumeBattlePresentationReward =
                        gameViewModel::consumeBattlePresentationReward,
                    onOpenSkillLoadout = gameViewModel::openSkillLoadout,
                    onCloseSkillLoadout = gameViewModel::closeSkillLoadout,
                    onSkillLoadoutIntent = gameViewModel::handleSkillLoadoutIntent,
                    onWorldIntent = gameViewModel::handleWorldIntent,
                    onOpenGear = { itemId -> gameViewModel.openGear(itemId) },
                    onGearIntent = gameViewModel::handleGearIntent,
                    onProgressIntent = { intent ->
                        // Pause synchronously before RequestChroniclePreview is queued.
                        // The runtime controller is FIFO, so the final elapsed advance is
                        // committed before the preview command observes canonical state.
                        if (intent == ProgressUiIntent.RequestChronicle) {
                            foregroundSimulationDriver.pauseForCanonicalPreview()
                        }
                        gameViewModel.handleProgressIntent(intent)
                    },
                    onDismissChroniclePreview = gameViewModel::dismissChroniclePreview,
                    onSetDoctrineEnabled = gameViewModel::setDoctrineEnabled,
                    onApplyDoctrinePreset = gameViewModel::applyDoctrinePreset,
                    onSetDoctrineRuleEnabled = gameViewModel::setDoctrineRuleEnabled,
                    onMoveDoctrineRule = gameViewModel::moveDoctrineRule,
                    onRemoveDoctrineRule = gameViewModel::removeDoctrineRule,
                    onBeginAddDoctrineRule = gameViewModel::beginAddDoctrineRule,
                    onBeginEditDoctrineRule = gameViewModel::beginEditDoctrineRule,
                    onDoctrineDraftAction = gameViewModel::updateDoctrineDraft,
                    onCommitDoctrineDraft = gameViewModel::commitDoctrineDraft,
                    onCancelDoctrineDraft = gameViewModel::cancelDoctrineDraft
                )
            }
        }
    }

    override fun onStart() {
        super.onStart()

        // A genuine prior onStop creates an application-scoped lifecycle barrier. Resume it
        // through OfflineSessionCoordinator before foreground pumping establishes a baseline.
        appContainer.runtimeController.resumeAfterBackgroundIfNeeded()

        if (gameViewModel.uiState.value.progress?.chroniclePreviewSessionActive == true) {
            foregroundSimulationDriver.pauseForCanonicalPreview()
        }
        foregroundSimulationDriver.start()
    }

    override fun onStop() {
        if (isChangingConfigurations) {
            // Same process + same application runtime: flush measured foreground time only.
            foregroundSimulationDriver.stopForHostRecreation()
        } else {
            // Real background transition: checkpoint now; next onStart consumes wall time via
            // the exact Foundation-16 offline-resume path. Any Chronicle preview token is no
            // longer valid once offline progression can occur, so discard only that UI token.
            foregroundSimulationDriver.stopAndCheckpointBackground()
            gameViewModel.onAppBackgrounded()
        }
        super.onStop()
    }
}
