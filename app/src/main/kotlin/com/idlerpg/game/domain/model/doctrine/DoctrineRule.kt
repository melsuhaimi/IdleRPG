package com.idlerpg.game.domain.model.doctrine

import com.idlerpg.game.core.id.InstanceId

/**
 * One ordered player-authored automation rule.
 *
 * Rule priority is the rule's position in DoctrineState.rules. InstanceId is stable
 * runtime identity used by commands and Doctrine events.
 */
data class DoctrineRule(
    val instanceId: InstanceId,
    val enabled: Boolean = true,
    val condition: DoctrineCondition,
    val action: DoctrineAction
)
