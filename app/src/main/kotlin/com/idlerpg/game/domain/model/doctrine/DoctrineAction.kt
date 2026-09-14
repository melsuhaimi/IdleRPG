package com.idlerpg.game.domain.model.doctrine

import com.idlerpg.game.core.id.ContentId

/**
 * Bounded Foundation 9 Doctrine action vocabulary.
 *
 * Doctrine data never contains executable Kotlin/script expressions. Systems interpret
 * these immutable action values deterministically.
 */
sealed interface DoctrineAction {

    /** Deterministic fallback-compatible normal Basic Attack. */
    object UseBasicAttack : DoctrineAction

    /** Attempt to use one authored skill by stable ContentId. */
    data class UseSkill(
        val skillId: ContentId
    ) : DoctrineAction
}
