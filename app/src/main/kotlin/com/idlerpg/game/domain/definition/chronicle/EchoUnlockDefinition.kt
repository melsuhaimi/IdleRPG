package com.idlerpg.game.domain.definition.chronicle

import com.idlerpg.game.core.id.ContentId
import com.idlerpg.game.core.number.GameNumber

/** Permanent capability/information effect unlocked by lifetime Echo earned. */
sealed interface EchoUnlockEffect {

    /** Reveal one persistent hidden-content/discovery identifier. */
    data class RevealHiddenContent(
        val contentId: ContentId
    ) : EchoUnlockEffect

    /** Unlock one explicitly META-scoped FeatureUnlockDefinition. */
    data class UnlockPersistentFeature(
        val featureId: ContentId
    ) : EchoUnlockEffect
}

/**
 * Authored permanent Echo knowledge threshold.
 *
 * Foundation 17 does not add an Echo shop or spending command. Unlocks are deterministic
 * thresholds over lifetime Echo earned (`available + spent`). Once applied, the result is
 * stored in MetaState and never depends on the current unspent balance again.
 */
data class EchoUnlockDefinition(
    val id: ContentId,
    val requiredLifetimeEcho: GameNumber,
    val effects: List<EchoUnlockEffect>
) {
    init {
        require(requiredLifetimeEcho > GameNumber.ZERO) {
            "requiredLifetimeEcho must be > 0 for $id"
        }
        require(effects.isNotEmpty()) {
            "Echo unlock $id must contain at least one effect"
        }
    }
}
