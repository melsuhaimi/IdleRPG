package com.idlerpg.game.domain.command

import com.idlerpg.game.core.id.ContentId
import com.idlerpg.game.core.id.InstanceId

/**
 * Root contract for immutable player/application intent.
 *
 * Commands express an attempt. They do not assert that the requested transition has
 * happened, and they never contain mutable domain-state objects.
 */
sealed interface GameCommand {
    val correlationId: CommandCorrelationId?
}

/**
 * Optional stable command correlation token used to associate resulting events with the
 * command that caused them. The engine will not use this value as gameplay identity.
 */
data class CommandCorrelationId(
    val value: Long
) {
    init {
        require(value > 0L) { "CommandCorrelationId must be positive: $value" }
    }
}

/** Structured command-rejection vocabulary shared by engine systems and presentation. */
enum class CommandRejectionCode {
    INVALID_ARGUMENT,
    INVALID_STATE,
    UNKNOWN_CONTENT,
    LOCKED,
    NOT_OWNED,
    ALREADY_OWNED,
    INSUFFICIENT_RESOURCE,
    CAPACITY_EXCEEDED,
    COOLDOWN_ACTIVE,
    NOT_READY,
    ALREADY_CLAIMED,
    STALE_PREVIEW,
    UNSUPPORTED
}

/** Player identity actions that are persisted with the Chronicle/meta state. */
sealed interface PlayerCommand : GameCommand

/** Assigns the one player-facing hero name used by the expedition and battle UI. */
data class SetHeroName(
    val name: String,
    override val correlationId: CommandCorrelationId? = null
) : PlayerCommand {
    init {
        require(name.length <= 64) { "Hero name input cannot exceed 64 characters" }
    }
}

/**
 * Machine-readable rejection context.
 *
 * Human-facing localized text is deliberately not stored here. Stable IDs identify the
 * content/runtime subject involved in the rejection when one exists.
 */
data class CommandRejectionReason(
    val code: CommandRejectionCode,
    val subjectContentId: ContentId? = null,
    val subjectInstanceId: InstanceId? = null
)
