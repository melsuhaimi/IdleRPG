package com.idlerpg.game.presentation.runtime

import com.idlerpg.game.application.OfflineProgressSummary
import com.idlerpg.game.domain.engine.CommandResult
import com.idlerpg.game.domain.event.GameEventEnvelope
import com.idlerpg.game.domain.model.GameState

/** Application-host lifecycle state for the one canonical production runtime. */
enum class RuntimeHostStatus {
    BOOTING,
    NEW_GAME,
    LOADING,
    RESUMING_OFFLINE,
    MENU,
    NAME_REQUIRED,
    READY,
    ERROR
}

/** Persistence status for explicit/lifecycle checkpoints. */
enum class RuntimeSaveStatus {
    IDLE,
    SAVING,
    SAVED,
    ERROR
}

/**
 * Categorizes application/runtime failures without putting localized Android text into the
 * runtime layer. [diagnosticMessage] is bounded developer-facing context, not a gameplay fact.
 */
enum class RuntimeFailureKind {
    INITIALIZATION,
    BACKGROUND_RESUME,
    SAVE,
    SIMULATION,
    COMMAND
}

data class RuntimeFailure(
    val kind: RuntimeFailureKind,
    val diagnosticMessage: String
)

/**
 * One serialized foreground command/simulation result captured immediately after the
 * backend transition completes. Presentation consumes this stream; it is never persisted.
 */
data class RuntimeTransition(
    val state: GameState,
    val events: List<GameEventEnvelope>,
    val commandResult: CommandResult?
)

/** Immutable application-host snapshot observed by the production ViewModel. */
data class GameRuntimeHostState(
    val status: RuntimeHostStatus = RuntimeHostStatus.BOOTING,
    val snapshot: GameState? = null,
    val hasExistingSave: Boolean = false,
    val offlineSummary: OfflineProgressSummary? = null,
    val saveStatus: RuntimeSaveStatus = RuntimeSaveStatus.IDLE,
    val failure: RuntimeFailure? = null,
    val canRetryInitialization: Boolean = false
)
