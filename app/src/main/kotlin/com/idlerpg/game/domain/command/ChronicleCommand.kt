package com.idlerpg.game.domain.command

/** Chronicle/meta-progression player intent. Reset behavior belongs to Foundation 17. */
sealed interface ChronicleCommand : GameCommand

/** Request a non-destructive preview of the currently available Chronicle collapse. */
data class RequestChroniclePreview(
    override val correlationId: CommandCorrelationId? = null
) : ChronicleCommand

/**
 * Attempt to commit the exact preview version previously shown to the player.
 * A stale or mismatched token must be rejected atomically by the future Chronicle system.
 */
data class CommitChronicleCollapse(
    val expectedPreviewToken: Long,
    override val correlationId: CommandCorrelationId? = null
) : ChronicleCommand {
    init {
        require(expectedPreviewToken > 0L) {
            "expectedPreviewToken must be positive: $expectedPreviewToken"
        }
    }
}
