package com.idlerpg.game.domain.system.enemy

import com.idlerpg.game.core.id.ContentId
import com.idlerpg.game.domain.engine.EngineContext
import com.idlerpg.game.domain.event.AdaptedEnemySpawned
import com.idlerpg.game.domain.event.GameEvent
import com.idlerpg.game.domain.model.GameState
import com.idlerpg.game.domain.model.combat.CombatantState
import com.idlerpg.game.domain.model.combat.EnemyState
import com.idlerpg.game.domain.system.adaptation.MutationSystem

/** Enemy plus spawn-time Adaptation events. */
data class EnemyCreationResult(
    val enemy: EnemyState,
    val events: List<GameEvent>
)

/** Creates deterministic runtime enemy instances from static content and regional ecology. */
object EnemyFactory {

    fun create(
        state: GameState,
        definitionId: ContentId,
        context: EngineContext,
        scalingTier: Long = 0L,
        regionDefinitionId: ContentId? = null,
        activeTraitIds: Set<ContentId> = emptySet()
    ): EnemyCreationResult {
        require(scalingTier >= 0L) {
            "Enemy scalingTier cannot be negative: $scalingTier"
        }

        val definition = context.contentRegistry.enemy(definitionId)
        val regionDefinition =
            regionDefinitionId?.let(context.contentRegistry::region)
        val scaledHealth = EnemyScalingSystem.scaledHealth(
            enemyDefinition = definition,
            regionDefinition = regionDefinition,
            scalingTier = scalingTier
        )
        val traitAdjustedHealth = EnemyEliteSystem.spawnHealth(scaledHealth, activeTraitIds)
        val instanceId = context.nextInstanceId()

        val mutationRoll = MutationSystem.rollForEnemy(
            state = state,
            enemyDefinition = definition,
            enemyInstanceId = instanceId,
            regionDefinition = regionDefinition,
            context = context
        )

        val enemy = EnemyState(
            instanceId = instanceId,
            definitionId = definition.id,
            combatant = CombatantState(
                instanceId = instanceId,
                currentHealth = traitAdjustedHealth
            ),
            scalingTier = scalingTier,
            activeTraitIds = activeTraitIds.toSortedSet(),
            activeMutations = mutationRoll.mutations
        )

        val spawnEvents = buildList {
            addAll(mutationRoll.events)
            if (enemy.activeMutations.isNotEmpty()) {
                add(
                    AdaptedEnemySpawned(
                        enemyInstanceId = enemy.instanceId,
                        enemyDefinitionId = enemy.definitionId,
                        mutationIds = enemy.activeMutations.map { it.mutationId }
                    )
                )
            }
        }

        return EnemyCreationResult(
            enemy = enemy,
            events = spawnEvents
        )
    }
}
