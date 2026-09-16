package com.idlerpg.game.domain.definition.world

import com.idlerpg.game.core.id.ContentId
import com.idlerpg.game.core.number.Ratio

/** Reusable encounter-level elite vocabulary snapshotted onto spawned enemies. */
enum class EliteModifier(val id: ContentId) {
    FRENZIED(ContentId("elite.frenzied")),
    SHIELDED(ContentId("elite.shielded")),
    RESONANT(ContentId("elite.resonant")),
    REGENERATING(ContentId("elite.regenerating"))
}

/** Authored encounter category. */
enum class EncounterType {
    NORMAL,
    ELITE,
    ANOMALY,
    BOSS
}

/**
 * Static encounter composition and continuation rule.
 *
 * Normal and anomaly waves contain one to three active enemies. Elite and boss encounters are
 * single-target encounters so their artwork, targeting, and combat readout stay legible on phones.
 * Repeated definition IDs remain valid for normal swarm compositions; runtime InstanceIds preserve
 * actor identity and order.
 */
data class EncounterDefinition(
    val id: ContentId,
    val regionId: ContentId,
    val type: EncounterType,
    val enemyDefinitionIds: List<ContentId>,
    val waves: Int = 1,
    val nextEncounterId: ContentId? = null,
    val bossId: ContentId? = null,
    val eliteModifiers: Set<EliteModifier> = emptySet(),
    val rewardLootTableId: ContentId? = null,
    /** Optional authored formation per wave; empty preserves the shared legacy formation. */
    val waveEnemyDefinitionIds: List<List<ContentId>> = emptyList(),
    /** Optional player-facing stage label; null keeps legacy metadata-driven naming. */
    val displayName: String? = null,
    /** Additional stage reward multiplier, applied after enemy role/tier growth. */
    val rewardMultiplier: Ratio = Ratio.ONE
) {
    init {
        require(enemyDefinitionIds.size in 1..MAX_ACTIVE_ENEMIES) {
            "EncounterDefinition must contain one to three active enemies for $id"
        }
        require(waves in 1..10) {
            "EncounterDefinition.waves must be between one and ten for $id"
        }
        require(waveEnemyDefinitionIds.isEmpty() || waveEnemyDefinitionIds.size == waves) {
            "EncounterDefinition.waveEnemyDefinitionIds must be empty or define all $waves waves for $id"
        }
        require(waveEnemyDefinitionIds.all { it.size in 1..MAX_ACTIVE_ENEMIES }) {
            "EncounterDefinition wave formations must contain one to three active enemies for $id"
        }
        if (type == EncounterType.ELITE || type == EncounterType.BOSS) {
            require(enemyDefinitionIds.size == 1) {
                "${type.name.lowercase().replaceFirstChar { it.uppercase() }} encounter $id must contain exactly one active enemy"
            }
            require(waveEnemyDefinitionIds.all { it.size == 1 }) {
                "${type.name.lowercase().replaceFirstChar { it.uppercase() }} encounter $id must contain exactly one active enemy in every wave"
            }
        }
        require(displayName == null || displayName.isNotBlank()) {
            "EncounterDefinition.displayName cannot be blank for $id"
        }
        require(rewardMultiplier > Ratio.ZERO) {
            "EncounterDefinition.rewardMultiplier must be positive for $id"
        }
        require(waveEnemyDefinitionIds.isEmpty() || waveEnemyDefinitionIds.first() == enemyDefinitionIds) {
            "EncounterDefinition.enemyDefinitionIds must mirror the first authored wave for $id"
        }
        if (type == EncounterType.BOSS) {
            require(bossId != null) {
                "Boss encounter $id must reference bossId"
            }
        } else {
            require(bossId == null) {
                "Non-boss encounter $id cannot reference bossId"
            }
        }
    }

    companion object {
        const val MAX_ACTIVE_ENEMIES: Int = 3
    }

    fun enemyDefinitionIdsForWave(wave: Int): List<ContentId> {
        require(wave in 1..waves) { "Wave $wave is outside 1..$waves for $id" }
        return if (waveEnemyDefinitionIds.isEmpty()) {
            enemyDefinitionIds
        } else {
            waveEnemyDefinitionIds[wave - 1]
        }
    }
}
