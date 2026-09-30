package com.idlerpg.game.data.content

import com.idlerpg.game.core.id.ContentId
import com.idlerpg.game.core.number.Ratio
import com.idlerpg.game.domain.definition.world.EliteModifier
import com.idlerpg.game.domain.definition.world.BossDefinition
import com.idlerpg.game.domain.definition.world.EncounterDefinition
import com.idlerpg.game.domain.definition.world.EncounterType

/** Stages 9–240 provide a long-form push/farm route with recurring milestone bosses. */
object TrainingHollowWorldContent {
    const val MAX_STAGE: Int = 240

    private val bossStages = setOf(30, 45) + (60..MAX_STAGE step 12).toSet()
    private val eliteStages = setOf(10, 15, 20, 25, 29)
    private val anomalyStages = setOf(12, 18, 24, 27)
    private const val INITIAL_NORMAL_CLEAR_COUNT: Long = 4L

    val STAGE_45_BOSS_ID = ContentId("boss.training_hollow.stage_45")
    val STAGE_60_BOSS_ID = ContentId("boss.training_hollow.stage_60")
    val STAGE_72_BOSS_ID = ContentId("boss.training_hollow.stage_72")
    val STAGE_84_BOSS_ID = ContentId("boss.training_hollow.stage_84")
    val STAGE_96_BOSS_ID = ContentId("boss.training_hollow.stage_96")
    val STAGE_108_BOSS_ID = ContentId("boss.training_hollow.stage_108")
    val STAGE_120_BOSS_ID = ContentId("boss.training_hollow.stage_120")
    val STAGE_240_BOSS_ID = ContentId("boss.training_hollow.stage_240")

    fun stageId(number: Int): ContentId =
        ContentId("encounter.training_hollow.stage_${number.toString().padStart(2, '0')}")

    val encounters: List<EncounterDefinition> = (9..MAX_STAGE).map { stage ->
        val type = encounterType(stage)
        val enemies = when (stage) {
            9 -> listOf(DefaultGameContent.SLIME_ID, DefaultGameContent.SLIME_ID, DefaultGameContent.RIFTFANG_ID)
            10 -> listOf(DefaultGameContent.HOLLOW_BULWARK_ID, DefaultGameContent.CINDER_WISP_ID)
            11 -> listOf(TrainingHollowStrategyContent.FROSTBOUND_MITE_ID, DefaultGameContent.RIFTFANG_ID)
            12 -> listOf(TrainingHollowStrategyContent.ECHO_LEECH_ID, TrainingHollowStrategyContent.FROSTBOUND_MITE_ID)
            13 -> listOf(DefaultGameContent.ARCANE_SEER_ID, DefaultGameContent.CINDER_WISP_ID)
            14 -> listOf(DefaultGameContent.SLIME_ID, DefaultGameContent.SLIME_ID, DefaultGameContent.SLIME_ID, DefaultGameContent.SLIME_ID)
            15 -> listOf(DefaultGameContent.HOLLOW_BULWARK_ID, TrainingHollowStrategyContent.ECHO_LEECH_ID)
            16 -> listOf(TrainingHollowStrategyContent.SHADE_MIMIC_ID, DefaultGameContent.RIFTFANG_ID)
            17 -> listOf(TrainingHollowStrategyContent.FROSTBOUND_MITE_ID, TrainingHollowStrategyContent.FROSTBOUND_MITE_ID, DefaultGameContent.CINDER_WISP_ID)
            18 -> listOf(DefaultGameContent.ARCANE_SEER_ID, TrainingHollowStrategyContent.ECHO_LEECH_ID, DefaultGameContent.HOLLOW_BULWARK_ID)
            19 -> listOf(DefaultGameContent.RIFTFANG_ID, DefaultGameContent.RIFTFANG_ID, DefaultGameContent.CINDER_WISP_ID)
            20 -> listOf(TrainingHollowStrategyContent.SHADE_MIMIC_ID, DefaultGameContent.HOLLOW_BULWARK_ID)
            21 -> listOf(DefaultGameContent.SLIME_ID, DefaultGameContent.SLIME_ID, TrainingHollowStrategyContent.ECHO_LEECH_ID, TrainingHollowStrategyContent.FROSTBOUND_MITE_ID)
            22 -> listOf(DefaultGameContent.HOLLOW_BULWARK_ID, DefaultGameContent.ARCANE_SEER_ID, DefaultGameContent.CINDER_WISP_ID)
            23 -> listOf(TrainingHollowStrategyContent.SHADE_MIMIC_ID, TrainingHollowStrategyContent.FROSTBOUND_MITE_ID, DefaultGameContent.RIFTFANG_ID)
            24 -> listOf(TrainingHollowStrategyContent.ECHO_LEECH_ID, DefaultGameContent.ARCANE_SEER_ID, DefaultGameContent.HOLLOW_BULWARK_ID)
            25 -> listOf(DefaultGameContent.HOLLOW_BULWARK_ID, DefaultGameContent.HOLLOW_BULWARK_ID, DefaultGameContent.CINDER_WISP_ID)
            26 -> listOf(DefaultGameContent.RIFTFANG_ID, DefaultGameContent.RIFTFANG_ID, TrainingHollowStrategyContent.FROSTBOUND_MITE_ID, TrainingHollowStrategyContent.ECHO_LEECH_ID)
            27 -> listOf(TrainingHollowStrategyContent.SHADE_MIMIC_ID, DefaultGameContent.ARCANE_SEER_ID, DefaultGameContent.CINDER_WISP_ID)
            28 -> listOf(DefaultGameContent.SLIME_ID, DefaultGameContent.SLIME_ID, DefaultGameContent.HOLLOW_BULWARK_ID, TrainingHollowStrategyContent.ECHO_LEECH_ID, DefaultGameContent.ARCANE_SEER_ID)
            29 -> listOf(TrainingHollowStrategyContent.SHADE_MIMIC_ID, DefaultGameContent.HOLLOW_BULWARK_ID, TrainingHollowStrategyContent.FROSTBOUND_MITE_ID)
            30 -> listOf(HollowWardenContent.ENEMY_ID, DefaultGameContent.HOLLOW_BULWARK_ID)
            else -> generatedComposition(stage, type)
        }.let { composition ->
            when (type) {
                EncounterType.ELITE,
                EncounterType.BOSS -> composition.take(1)
                else -> composition.take(EncounterDefinition.MAX_ACTIVE_ENEMIES)
            }
        }
        val modifiers = when (type) {
            EncounterType.ELITE -> setOf(EliteModifier.FRENZIED, EliteModifier.REGENERATING)
            EncounterType.ANOMALY -> setOf(EliteModifier.SHIELDED, EliteModifier.RESONANT)
            else -> emptySet()
        }
        EncounterDefinition(
            id = stageId(stage),
            regionId = DefaultGameContent.TRAINING_HOLLOW_REGION_ID,
            type = type,
            enemyDefinitionIds = enemies,
            waves = when {
                type == EncounterType.BOSS -> 3
                stage in setOf(14, 21, 28) -> 2
                stage > 30 && stage % 10 == 0 -> 2
                else -> 1
            },
            nextEncounterId = if (stage < MAX_STAGE) stageId(stage + 1) else null,
            bossId = bossIdForStage(stage, type),
            eliteModifiers = modifiers,
            rewardLootTableId = when (type) {
                EncounterType.ELITE -> TrainingHollowLootContent.ELITE_LOOT_TABLE_ID
                EncounterType.ANOMALY -> TrainingHollowLootContent.ANOMALY_LOOT_TABLE_ID
                EncounterType.BOSS -> TrainingHollowLootContent.BOSS_LOOT_TABLE_ID
                else -> null
            },
            waveEnemyDefinitionIds = bossWavesOrEmpty(type),
            displayName = "Training Hollow · Stage ${stage.toString().padStart(2, '0')}",
            rewardMultiplier = if (stage <= 30) Ratio.ONE else stageRewardMultiplier(stage)
        )
    }

    private fun generatedType(stage: Int): EncounterType = when {
        stage % 9 == 0 -> EncounterType.ANOMALY
        stage % 5 == 0 -> EncounterType.ELITE
        else -> EncounterType.NORMAL
    }

    private fun encounterType(stage: Int): EncounterType = when {
        stage in bossStages -> EncounterType.BOSS
        stage in eliteStages -> EncounterType.ELITE
        stage in anomalyStages -> EncounterType.ANOMALY
        else -> generatedType(stage)
    }

    private fun bossIdForStage(stage: Int, type: EncounterType): ContentId? =
        if (type != EncounterType.BOSS) null else bossIdForStage(stage)

    private fun bossIdForStage(stage: Int): ContentId = when (stage) {
        30 -> HollowWardenContent.BOSS_ID
        45 -> STAGE_45_BOSS_ID
        60 -> STAGE_60_BOSS_ID
        72 -> STAGE_72_BOSS_ID
        84 -> STAGE_84_BOSS_ID
        96 -> STAGE_96_BOSS_ID
        108 -> STAGE_108_BOSS_ID
        120 -> STAGE_120_BOSS_ID
        240 -> STAGE_240_BOSS_ID
        else -> ContentId("boss.training_hollow.stage_$stage")
    }

    /** Each milestone is a separate boss contract so first-clear state remains meaningful. */
    val additionalBosses: List<BossDefinition> = bossStages
        .filterNot { it == 30 }
        .sorted()
        .map { stage ->
            BossDefinition(
                id = bossIdForStage(stage),
                regionId = DefaultGameContent.TRAINING_HOLLOW_REGION_ID,
                encounterDefinitionId = stageId(stage),
                requiredNormalClears = normalClearRequirement(stage),
                chronicleMilestoneRelevant = true
            )
        }

    private fun normalClearRequirement(bossStage: Int): Long =
        Math.addExact(
            INITIAL_NORMAL_CLEAR_COUNT,
            (9 until bossStage).count { stage ->
                encounterType(stage) == EncounterType.NORMAL
            }.toLong()
        )

    /** Content-driven compositions keep future stage additions deterministic and varied. */
    private fun generatedComposition(stage: Int, type: EncounterType): List<ContentId> {
        if (type == EncounterType.BOSS) {
            return listOf(HollowWardenContent.ENEMY_ID)
        }
        val patterns = listOf(
            listOf(DefaultGameContent.SLIME_ID, DefaultGameContent.RIFTFANG_ID, DefaultGameContent.SLIME_ID),
            listOf(DefaultGameContent.CINDER_WISP_ID, DefaultGameContent.RIFTFANG_ID),
            listOf(DefaultGameContent.HOLLOW_BULWARK_ID, DefaultGameContent.SLIME_ID, DefaultGameContent.CINDER_WISP_ID),
            listOf(TrainingHollowStrategyContent.FROSTBOUND_MITE_ID, DefaultGameContent.ARCANE_SEER_ID),
            listOf(TrainingHollowStrategyContent.ECHO_LEECH_ID, DefaultGameContent.RIFTFANG_ID, DefaultGameContent.CINDER_WISP_ID),
            listOf(TrainingHollowStrategyContent.SHADE_MIMIC_ID, DefaultGameContent.HOLLOW_BULWARK_ID),
            listOf(DefaultGameContent.ARCANE_SEER_ID, TrainingHollowStrategyContent.ECHO_LEECH_ID, DefaultGameContent.HOLLOW_BULWARK_ID),
            listOf(DefaultGameContent.SLIME_ID, TrainingHollowStrategyContent.FROSTBOUND_MITE_ID, TrainingHollowStrategyContent.SHADE_MIMIC_ID, DefaultGameContent.CINDER_WISP_ID)
        )
        return patterns[(stage - 31).mod(patterns.size)]
    }

    private fun bossWavesOrEmpty(type: EncounterType): List<List<ContentId>> =
        if (type != EncounterType.BOSS) emptyList() else listOf(
            listOf(HollowWardenContent.ENEMY_ID),
            listOf(HollowWardenContent.ENEMY_ID),
            listOf(HollowWardenContent.ENEMY_ID)
        )

    private fun stageRewardMultiplier(stage: Int): Ratio =
        Ratio.ofUnits(10_000L + (stage - 30L) * 125L)
}
