package com.idlerpg.game.ui.app

import com.idlerpg.game.core.id.InstanceId
import com.idlerpg.game.presentation.model.BattleUiState
import com.idlerpg.game.presentation.model.DoctrineUiState
import com.idlerpg.game.presentation.model.GearUiState
import com.idlerpg.game.presentation.model.GlobalHudUiState
import com.idlerpg.game.presentation.model.OfflineProgressUiState
import com.idlerpg.game.presentation.model.ProgressUiState
import com.idlerpg.game.presentation.model.SkillLoadoutUiState
import com.idlerpg.game.presentation.model.WorldUiState
import com.idlerpg.game.ui.navigation.GameDestination
import com.idlerpg.game.ui.navigation.NavigationAvailability
import com.idlerpg.game.ui.navigation.ProgressDestination

/** Production boot/runtime states exposed to Compose. */
enum class IdleRpgAppStatus {
    BOOTING,
    NEW_GAME,
    LOADING,
    RESUMING_OFFLINE,
    MENU,
    NAME_REQUIRED,
    READY,
    ERROR
}

enum class IdleRpgSaveStatus {
    IDLE,
    SAVING,
    SAVED,
    ERROR
}

enum class IdleRpgRuntimeFailureKind {
    INITIALIZATION,
    BACKGROUND_RESUME,
    SAVE,
    SIMULATION,
    COMMAND
}

/** Localized message selection stays in Compose; diagnostics are bounded runtime context only. */
data class RuntimeFailureUiState(
    val kind: IdleRpgRuntimeFailureKind,
    val diagnosticMessage: String
)

/**
 * FUI-10 root presentation state. Gameplay truth remains in the canonical runtime snapshot;
 * lifecycle summaries/errors and navigation are presentation/application state only.
 */
data class IdleRpgUiState(
    val status: IdleRpgAppStatus = IdleRpgAppStatus.BOOTING,
    val hasExistingSave: Boolean = false,
    val saveStatus: IdleRpgSaveStatus = IdleRpgSaveStatus.IDLE,
    val destination: GameDestination = GameDestination.BATTLE,
    val progressDestination: ProgressDestination = ProgressDestination.OVERVIEW,
    val navigationAvailability: NavigationAvailability = NavigationAvailability.BOOTSTRAP,
    val globalHud: GlobalHudUiState? = null,
    val battle: BattleUiState? = null,
    val skillLoadoutOpen: Boolean = false,
    val skillLoadout: SkillLoadoutUiState? = null,
    /** Transient presentation focus used by the Battle -> Build reward handoff. */
    val gearFocusItemId: InstanceId? = null,
    val world: WorldUiState? = null,
    val doctrine: DoctrineUiState? = null,
    val gear: GearUiState? = null,
    val progress: ProgressUiState? = null,
    val simulationTimeMillis: Long? = null,
    val offlineSummary: OfflineProgressUiState? = null,
    val runtimeFailure: RuntimeFailureUiState? = null,
    val canRetryInitialization: Boolean = false
)
