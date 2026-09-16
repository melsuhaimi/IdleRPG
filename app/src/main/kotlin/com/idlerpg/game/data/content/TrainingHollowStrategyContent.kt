package com.idlerpg.game.data.content

import com.idlerpg.game.core.id.ContentId
import com.idlerpg.game.core.number.GameNumber
import com.idlerpg.game.core.number.Ratio
import com.idlerpg.game.core.time.GameDuration
import com.idlerpg.game.domain.definition.Affinity
import com.idlerpg.game.domain.definition.DamageKind
import com.idlerpg.game.domain.definition.combat.EffectSpec
import com.idlerpg.game.domain.definition.combat.SkillDefinition
import com.idlerpg.game.domain.definition.combat.SkillTargetingRule
import com.idlerpg.game.domain.definition.combat.StatusEffectDefinition
import com.idlerpg.game.domain.definition.combat.StatusModifierDefinition
import com.idlerpg.game.domain.definition.enemy.EnemyAttackDefinition
import com.idlerpg.game.domain.definition.enemy.EnemyDefinition
import com.idlerpg.game.domain.definition.enemy.EnemyRole
import com.idlerpg.game.domain.definition.progression.FeatureUnlockDefinition
import com.idlerpg.game.domain.definition.progression.FeatureUnlockScope
import com.idlerpg.game.domain.definition.resonance.ConvergenceDefinition
import com.idlerpg.game.domain.definition.resonance.ResonanceConsumePolicy
import com.idlerpg.game.domain.definition.resonance.ResonanceEmissionDefinition
import com.idlerpg.game.domain.definition.resonance.ResonancePatternDefinition
import com.idlerpg.game.domain.definition.world.EliteModifier
import com.idlerpg.game.domain.definition.world.EncounterDefinition
import com.idlerpg.game.domain.definition.world.EncounterType

/** Gate 3–4 strategy content kept separate from the predecessor verification catalog. */
object TrainingHollowStrategyContent {
    val GLACIAL_WARD_ID = ContentId("skill.glacial_ward")
    val RESONANCE_SHIFT_ID = ContentId("skill.resonance_shift")
    val BLOOD_ECLIPSE_SKILL_ID = ContentId("skill.blood_eclipse")
    val VOID_LANCE_ID = ContentId("skill.void_lance")
    val IRON_VOW_ID = ContentId("skill.iron_vow")
    val STARFALL_ID = ContentId("skill.starfall")
    val GLACIAL_WARD_STATUS_ID = ContentId("status.glacial_ward")
    val SCORCHED_STATUS_ID = ContentId("status.scorched")

    val FROSTBOUND_MITE_ID = ContentId("enemy.frostbound_mite")
    val ECHO_LEECH_ID = ContentId("enemy.echo_leech")
    val SHADE_MIMIC_ID = ContentId("enemy.shade_mimic")
    val FROSTBOUND_MITE_ATTACK_ID = ContentId("enemy_attack.frostbound_mite.chill_bite")
    val ECHO_LEECH_ATTACK_ID = ContentId("enemy_attack.echo_leech.resonance_siphon")
    val SHADE_MIMIC_ATTACK_ID = ContentId("enemy_attack.shade_mimic.reflected_cut")
    val FROSTBOUND_MITE_ENCOUNTER_ID = ContentId("encounter.training_hollow.frostbound_mite")
    val ECHO_LEECH_ENCOUNTER_ID = ContentId("encounter.training_hollow.echo_leech")
    val SHADE_MIMIC_ENCOUNTER_ID = ContentId("encounter.training_hollow.shade_mimic")

    val FLASHFIRE_ID = ContentId("convergence.flashfire")
    val BASTION_PULSE_ID = ContentId("convergence.bastion_pulse")
    val SHATTERFIELD_ID = ContentId("convergence.shatterfield")
    val BLOOD_ECLIPSE_ID = ContentId("convergence.blood_eclipse")
    val OVERDRIVE_ID = ContentId("convergence.overdrive")
    val WINTER_BASTION_ID = ContentId("convergence.winter_bastion")
    val ARCANE_REFRAIN_ID = ContentId("convergence.arcane_refrain")

    val GLACIAL_WARD_FEATURE_ID = ContentId("feature.skill.glacial_ward")
    val RESONANCE_SHIFT_FEATURE_ID = ContentId("feature.skill.resonance_shift")
    val BLOOD_ECLIPSE_FEATURE_ID = ContentId("feature.skill.blood_eclipse")
    val VOID_LANCE_FEATURE_ID = ContentId("feature.skill.void_lance")
    val IRON_VOW_FEATURE_ID = ContentId("feature.skill.iron_vow")
    val STARFALL_FEATURE_ID = ContentId("feature.skill.starfall")

    val skills = listOf(
        SkillDefinition(
            id = GLACIAL_WARD_ID,
            cooldown = GameDuration.ofSeconds(6L),
            targetingRule = SkillTargetingRule.SELF,
            effects = listOf(EffectSpec.ApplyStatus(GLACIAL_WARD_STATUS_ID, EffectSpec.TargetPattern.SELF)),
            affinityTags = setOf(Affinity.FROST, Affinity.GUARD),
            resonanceEmissions = listOf(ResonanceEmissionDefinition(Affinity.FROST, GameNumber.ONE)),
            requiredFeatureId = GLACIAL_WARD_FEATURE_ID
        ),
        SkillDefinition(
            id = RESONANCE_SHIFT_ID,
            cooldown = GameDuration.ofSeconds(4L),
            targetingRule = SkillTargetingRule.CASTER_OR_SUPPORT_FIRST,
            effects = listOf(EffectSpec.DealDamage(Ratio.ofUnits(4_000L), damageKind = DamageKind.ARCANE)),
            affinityTags = setOf(Affinity.ARCANE),
            resonanceEmissions = listOf(ResonanceEmissionDefinition(Affinity.ARCANE, GameNumber.of(2L))),
            requiredFeatureId = RESONANCE_SHIFT_FEATURE_ID
        ),
        SkillDefinition(
            id = BLOOD_ECLIPSE_SKILL_ID,
            cooldown = GameDuration.ofSeconds(5L),
            targetingRule = SkillTargetingRule.LOWEST_HEALTH_ENEMY,
            effects = listOf(
                EffectSpec.DealDamage(Ratio.ofUnits(11_000L), damageKind = DamageKind.SHADOW),
                EffectSpec.Heal(GameNumber.of(14L), EffectSpec.TargetPattern.SELF)
            ),
            affinityTags = setOf(Affinity.SHADOW, Affinity.VITALITY),
            resonanceEmissions = listOf(
                ResonanceEmissionDefinition(Affinity.SHADOW, GameNumber.ONE),
                ResonanceEmissionDefinition(Affinity.VITALITY, GameNumber.ONE)
            ),
            requiredFeatureId = BLOOD_ECLIPSE_FEATURE_ID
        ),
        SkillDefinition(
            id = VOID_LANCE_ID,
            cooldown = GameDuration.ofSeconds(5L),
            effects = listOf(
                EffectSpec.DealDamage(
                    powerRatio = Ratio.ofUnits(14_000L),
                    damageKind = DamageKind.ARCANE,
                    conditions = listOf(
                        EffectSpec.DamageCondition.TargetHasStatus(
                            DefaultGameContent.CHILL_STATUS_ID,
                            Ratio.ofUnits(8_000L)
                        )
                    )
                ),
                EffectSpec.ApplyStatus(DefaultGameContent.CHILL_STATUS_ID)
            ),
            affinityTags = setOf(Affinity.ARCANE, Affinity.FROST),
            resonanceEmissions = listOf(
                ResonanceEmissionDefinition(Affinity.ARCANE, GameNumber.ONE)
            ),
            requiredFeatureId = VOID_LANCE_FEATURE_ID
        ),
        SkillDefinition(
            id = IRON_VOW_ID,
            cooldown = GameDuration.ofSeconds(8L),
            targetingRule = SkillTargetingRule.SELF,
            effects = listOf(
                EffectSpec.Heal(GameNumber.of(50L), EffectSpec.TargetPattern.SELF),
                EffectSpec.ApplyStatus(
                    DefaultGameContent.GUARD_FOCUS_STATUS_ID,
                    EffectSpec.TargetPattern.SELF
                )
            ),
            affinityTags = setOf(Affinity.GUARD, Affinity.VITALITY),
            resonanceEmissions = listOf(
                ResonanceEmissionDefinition(Affinity.GUARD, GameNumber.ONE),
                ResonanceEmissionDefinition(Affinity.VITALITY, GameNumber.ONE)
            ),
            requiredFeatureId = IRON_VOW_FEATURE_ID
        ),
        SkillDefinition(
            id = STARFALL_ID,
            cooldown = GameDuration.ofSeconds(7L),
            effects = listOf(
                EffectSpec.DealDamage(
                    powerRatio = Ratio.ofUnits(7_500L),
                    damageKind = DamageKind.ELEMENTAL,
                    targetPattern = EffectSpec.TargetPattern.ALL_ENEMIES
                ),
                EffectSpec.ApplyStatus(
                    DefaultGameContent.BURNING_STATUS_ID,
                    EffectSpec.TargetPattern.ALL_ENEMIES
                )
            ),
            affinityTags = setOf(Affinity.EMBER, Affinity.ARCANE),
            resonanceEmissions = listOf(
                ResonanceEmissionDefinition(Affinity.EMBER, GameNumber.ONE),
                ResonanceEmissionDefinition(Affinity.ARCANE, GameNumber.ONE)
            ),
            requiredFeatureId = STARFALL_FEATURE_ID
        )
    )

    val statuses = listOf(
        StatusEffectDefinition(
            id = GLACIAL_WARD_STATUS_ID,
            displayName = "Ice Barrier",
            duration = GameDuration.ofSeconds(5L),
            modifiers = listOf(StatusModifierDefinition.FlatArmor(GameNumber.of(24L))),
            affinityTags = setOf(Affinity.FROST, Affinity.GUARD)
        ),
        StatusEffectDefinition(
            id = SCORCHED_STATUS_ID,
            displayName = "Scorched",
            duration = GameDuration.ofSeconds(4L),
            modifiers = listOf(
                StatusModifierDefinition.IncomingDamageMultiplier(Ratio.ofUnits(12_000L))
            ),
            affinityTags = setOf(Affinity.EMBER)
        )
    )

    val enemyAttacks = listOf(
        EnemyAttackDefinition(
            FROSTBOUND_MITE_ATTACK_ID,
            "Chill Bite",
            GameDuration.ofMillis(1_600L),
            GameNumber.of(6L),
            affinity = Affinity.FROST,
            damageKind = DamageKind.ELEMENTAL,
            appliedStatusId = DefaultGameContent.CHILL_STATUS_ID
        ),
        EnemyAttackDefinition(
            ECHO_LEECH_ATTACK_ID,
            "Charge Drain",
            GameDuration.ofMillis(2_100L),
            GameNumber.of(8L),
            affinity = Affinity.VITALITY,
            damageKind = DamageKind.ARCANE,
            resonanceDrain = GameNumber.ONE
        ),
        EnemyAttackDefinition(
            SHADE_MIMIC_ATTACK_ID,
            "Reflected Cut",
            GameDuration.ofMillis(1_300L),
            GameNumber.of(9L),
            affinity = Affinity.SHADOW,
            damageKind = DamageKind.SHADOW
        )
    )

    val enemies = listOf(
        EnemyDefinition(
            FROSTBOUND_MITE_ID,
            "Frostbound Mite",
            GameNumber.of(92L),
            GameNumber.of(14L),
            GameNumber.of(14L),
            DefaultGameContent.TRAINING_SLIME_LOOT_TABLE_ID,
            FROSTBOUND_MITE_ATTACK_ID,
            role = EnemyRole.CONTROLLER
        ),
        EnemyDefinition(
            ECHO_LEECH_ID,
            "Soul Leech",
            GameNumber.of(135L),
            GameNumber.of(18L),
            GameNumber.of(18L),
            DefaultGameContent.TRAINING_SLIME_LOOT_TABLE_ID,
            ECHO_LEECH_ATTACK_ID,
            role = EnemyRole.PARASITE
        ),
        EnemyDefinition(
            SHADE_MIMIC_ID,
            "Shade Mimic",
            GameNumber.of(165L),
            GameNumber.of(22L),
            GameNumber.of(22L),
            DefaultGameContent.TRAINING_SLIME_LOOT_TABLE_ID,
            SHADE_MIMIC_ATTACK_ID,
            role = EnemyRole.ADAPTIVE
        )
    )

    val encounters = listOf(
        EncounterDefinition(
            FROSTBOUND_MITE_ENCOUNTER_ID,
            DefaultGameContent.TRAINING_HOLLOW_REGION_ID,
            EncounterType.NORMAL,
            listOf(FROSTBOUND_MITE_ID, DefaultGameContent.RIFTFANG_ID),
            nextEncounterId = ECHO_LEECH_ENCOUNTER_ID
        ),
        EncounterDefinition(
            ECHO_LEECH_ENCOUNTER_ID,
            DefaultGameContent.TRAINING_HOLLOW_REGION_ID,
            EncounterType.ELITE,
            listOf(ECHO_LEECH_ID),
            nextEncounterId = SHADE_MIMIC_ENCOUNTER_ID,
            eliteModifiers = setOf(EliteModifier.RESONANT, EliteModifier.REGENERATING),
            rewardLootTableId = TrainingHollowLootContent.ELITE_LOOT_TABLE_ID
        ),
        EncounterDefinition(
            SHADE_MIMIC_ENCOUNTER_ID,
            DefaultGameContent.TRAINING_HOLLOW_REGION_ID,
            EncounterType.ANOMALY,
            listOf(SHADE_MIMIC_ID, DefaultGameContent.ARCANE_SEER_ID, FROSTBOUND_MITE_ID),
            nextEncounterId = ContentId("encounter.training_hollow.stage_09"),
            eliteModifiers = setOf(EliteModifier.FRENZIED, EliteModifier.SHIELDED),
            rewardLootTableId = TrainingHollowLootContent.ANOMALY_LOOT_TABLE_ID
        )
    )

    val convergences = listOf(
        convergence(
            FLASHFIRE_ID,
            listOf(Affinity.TEMPO, Affinity.EMBER, Affinity.EMBER),
            listOf(
                EffectSpec.DealDamage(
                    Ratio.ofUnits(6_500L),
                    damageKind = DamageKind.ELEMENTAL,
                    targetPattern = EffectSpec.TargetPattern.ALL_ENEMIES
                ),
                EffectSpec.ApplyStatus(DefaultGameContent.BURNING_STATUS_ID, EffectSpec.TargetPattern.ALL_ENEMIES)
            )
        ),
        convergence(
            BASTION_PULSE_ID,
            listOf(Affinity.GUARD, Affinity.VITALITY, Affinity.GUARD),
            listOf(
                EffectSpec.Heal(GameNumber.of(35L)),
                EffectSpec.ApplyStatus(DefaultGameContent.GUARD_FOCUS_STATUS_ID, EffectSpec.TargetPattern.SELF)
            )
        ),
        convergence(
            SHATTERFIELD_ID,
            listOf(Affinity.FROST, Affinity.ARCANE, Affinity.FROST),
            listOf(
                EffectSpec.DealDamage(
                    Ratio.ofUnits(6_000L),
                    damageKind = DamageKind.ARCANE,
                    targetPattern = EffectSpec.TargetPattern.ALL_ENEMIES,
                    conditions = listOf(
                        EffectSpec.DamageCondition.TargetHasStatus(
                            DefaultGameContent.CHILL_STATUS_ID,
                            Ratio.ofUnits(7_000L)
                        )
                    )
                ),
                EffectSpec.RemoveStatus(DefaultGameContent.CHILL_STATUS_ID, EffectSpec.TargetPattern.ALL_ENEMIES)
            )
        ),
        convergence(
            BLOOD_ECLIPSE_ID,
            listOf(Affinity.SHADOW, Affinity.VITALITY, Affinity.SHADOW),
            listOf(
                EffectSpec.DealDamage(Ratio.ofUnits(12_000L), damageKind = DamageKind.SHADOW),
                EffectSpec.Heal(GameNumber.of(22L))
            )
        ),
        convergence(
            OVERDRIVE_ID,
            listOf(Affinity.TEMPO, Affinity.MIGHT, Affinity.TEMPO),
            listOf(EffectSpec.DealDamage(Ratio.ofUnits(4_000L), hitCount = 3))
        ),
        convergence(
            WINTER_BASTION_ID,
            listOf(Affinity.GUARD, Affinity.FROST, Affinity.GUARD),
            listOf(
                EffectSpec.Heal(GameNumber.of(20L)),
                EffectSpec.ApplyStatus(GLACIAL_WARD_STATUS_ID, EffectSpec.TargetPattern.SELF)
            )
        ),
        ConvergenceDefinition(
            id = ARCANE_REFRAIN_ID,
            pattern = ResonancePatternDefinition(
                listOf(Affinity.ARCANE, Affinity.ARCANE, Affinity.TEMPO)
            ),
            minimumChargeByAffinity = mapOf(
                Affinity.ARCANE to GameNumber.of(2L),
                Affinity.TEMPO to GameNumber.ONE
            ),
            consumePolicy = ResonanceConsumePolicy.NONE,
            priority = 70,
            cooldown = GameDuration.ofSeconds(4L),
            maxTriggersPerEncounter = 3,
            effects = listOf(
                EffectSpec.ShiftResonance(Affinity.TEMPO, Affinity.ARCANE, GameNumber.ONE)
            ),
            discoverOnFirstTrigger = true
        )
    )

    val featureUnlocks = listOf(
        FeatureUnlockDefinition(GLACIAL_WARD_FEATURE_ID, FeatureUnlockScope.RUN, 24L,
            requiredMasteryLevels = mapOf(Affinity.FROST.id to 3L, Affinity.GUARD.id to 3L)),
        FeatureUnlockDefinition(RESONANCE_SHIFT_FEATURE_ID, FeatureUnlockScope.RUN, 28L,
            requiredMasteryLevels = mapOf(Affinity.ARCANE.id to 4L)),
        FeatureUnlockDefinition(BLOOD_ECLIPSE_FEATURE_ID, FeatureUnlockScope.RUN, 32L,
            requiredMasteryLevels = mapOf(Affinity.SHADOW.id to 3L, Affinity.VITALITY.id to 3L)),
        FeatureUnlockDefinition(
            VOID_LANCE_FEATURE_ID,
            FeatureUnlockScope.RUN,
            38L,
            requiredMasteryLevels = mapOf(Affinity.ARCANE.id to 6L, Affinity.FROST.id to 4L)
        ),
        FeatureUnlockDefinition(
            IRON_VOW_FEATURE_ID,
            FeatureUnlockScope.RUN,
            50L,
            requiredMasteryLevels = mapOf(Affinity.GUARD.id to 7L, Affinity.VITALITY.id to 6L)
        ),
        FeatureUnlockDefinition(
            STARFALL_FEATURE_ID,
            FeatureUnlockScope.RUN,
            65L,
            requiredMasteryLevels = mapOf(Affinity.EMBER.id to 8L, Affinity.ARCANE.id to 8L)
        )
    )

    private fun convergence(
        id: ContentId,
        pattern: List<Affinity>,
        effects: List<EffectSpec>
    ): ConvergenceDefinition = ConvergenceDefinition(
        id = id,
        pattern = ResonancePatternDefinition(pattern),
        minimumChargeByAffinity = pattern.groupingBy { it }.eachCount()
            .mapValues { GameNumber.of(it.value.toLong()) },
        consumePolicy = ResonanceConsumePolicy.REQUIRED_CHARGE,
        priority = 80,
        cooldown = GameDuration.ofSeconds(1L),
        effects = effects,
        discoverOnFirstTrigger = true
    )
}
