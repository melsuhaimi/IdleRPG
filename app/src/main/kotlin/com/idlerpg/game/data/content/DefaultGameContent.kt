package com.idlerpg.game.data.content

import com.idlerpg.game.core.id.ContentId
import com.idlerpg.game.core.number.GameNumber
import com.idlerpg.game.core.number.Ratio
import com.idlerpg.game.core.time.GameDuration
import com.idlerpg.game.domain.definition.Affinity
import com.idlerpg.game.domain.definition.CurrencyId
import com.idlerpg.game.domain.definition.DamageKind
import com.idlerpg.game.domain.definition.EquipmentSlot
import com.idlerpg.game.domain.definition.Rarity
import com.idlerpg.game.domain.definition.adaptation.AdaptationRewardDefinition
import com.idlerpg.game.domain.definition.adaptation.AdaptationThresholdDefinition
import com.idlerpg.game.domain.definition.adaptation.MutationDefinition
import com.idlerpg.game.domain.definition.adaptation.MutationEffectDefinition
import com.idlerpg.game.domain.definition.achievement.AchievementDefinition
import com.idlerpg.game.domain.definition.achievement.AchievementObjectiveDefinition
import com.idlerpg.game.domain.definition.achievement.AchievementRewardDefinition
import com.idlerpg.game.domain.definition.chronicle.ChronicleDefinition
import com.idlerpg.game.domain.definition.chronicle.EchoOfferDefinition
import com.idlerpg.game.domain.definition.chronicle.EchoUnlockEffect
import com.idlerpg.game.domain.definition.combat.BasicAttackDefinition
import com.idlerpg.game.domain.definition.combat.EffectSpec
import com.idlerpg.game.domain.definition.combat.SkillDefinition
import com.idlerpg.game.domain.definition.combat.SkillEvolutionDefinition
import com.idlerpg.game.domain.definition.combat.SkillTargetingRule
import com.idlerpg.game.domain.definition.combat.StatusEffectDefinition
import com.idlerpg.game.domain.definition.combat.StatusModifierDefinition
import com.idlerpg.game.domain.definition.combat.StatusStackingPolicy
import com.idlerpg.game.domain.definition.economy.CostFormulaDefinition
import com.idlerpg.game.domain.definition.economy.UpgradeDefinition
import com.idlerpg.game.domain.definition.economy.UpgradeEffectDefinition
import com.idlerpg.game.domain.definition.economy.UpgradeMilestoneDefinition
import com.idlerpg.game.domain.definition.enemy.EnemyAttackDefinition
import com.idlerpg.game.domain.definition.enemy.EnemyDefinition
import com.idlerpg.game.domain.definition.item.AffixDefinition
import com.idlerpg.game.domain.definition.item.AffixEffectDefinition
import com.idlerpg.game.domain.definition.item.EquipmentDefinition
import com.idlerpg.game.domain.definition.item.EquipmentEffectDefinition
import com.idlerpg.game.domain.definition.item.ItemCategory
import com.idlerpg.game.domain.definition.item.ItemDefinition
import com.idlerpg.game.domain.definition.item.LootTableDefinition
import com.idlerpg.game.domain.definition.item.LootTableEntry
import com.idlerpg.game.domain.definition.item.SalvageProfile
import com.idlerpg.game.domain.definition.progression.FeatureUnlockDefinition
import com.idlerpg.game.domain.definition.progression.FeatureUnlockScope
import com.idlerpg.game.domain.definition.progression.LevelCurveDefinition
import com.idlerpg.game.domain.definition.progression.MasteryDefinition
import com.idlerpg.game.domain.definition.quest.QuestDefinition
import com.idlerpg.game.domain.definition.quest.QuestObjectiveDefinition
import com.idlerpg.game.domain.definition.quest.QuestRewardDefinition
import com.idlerpg.game.domain.definition.resonance.ConvergenceDefinition
import com.idlerpg.game.domain.definition.resonance.ResonanceConsumePolicy
import com.idlerpg.game.domain.definition.resonance.ResonanceEmissionDefinition
import com.idlerpg.game.domain.definition.resonance.ResonancePatternDefinition
import com.idlerpg.game.domain.definition.world.EncounterDefinition
import com.idlerpg.game.domain.definition.world.EncounterType
import com.idlerpg.game.domain.definition.world.RegionDefinition
import com.idlerpg.game.domain.definition.world.WorldDefinition

/** Kotlin-authored verification content required through Foundation 17. */
object DefaultGameContent {

    val WORLD_ID = ContentId("world.default")
    val TRAINING_HOLLOW_REGION_ID = ContentId("region.training_hollow")
    val TRAINING_SLIME_ENCOUNTER_ID = ContentId("encounter.training_hollow.slime")
    val RIFTFANG_ENCOUNTER_ID = ContentId("encounter.training_hollow.riftfang")
    val CINDER_WISP_ENCOUNTER_ID = ContentId("encounter.training_hollow.cinder_wisp")
    val HOLLOW_BULWARK_ENCOUNTER_ID = ContentId("encounter.training_hollow.hollow_bulwark")
    val ARCANE_SEER_ENCOUNTER_ID = ContentId("encounter.training_hollow.arcane_seer")

    val BASIC_ATTACK_ID = ContentId("skill.basic_attack")
    val HEAVY_STRIKE_ID = ContentId("skill.heavy_strike")
    val EARTHBREAKER_EVOLUTION_ID = ContentId("skill_evolution.heavy_strike.earthbreaker")
    val EXECUTIONER_EVOLUTION_ID = ContentId("skill_evolution.heavy_strike.executioner")
    val WILDSPARK_EVOLUTION_ID = ContentId("skill_evolution.flame_brand.wildspark")
    val SEARING_BRAND_EVOLUTION_ID = ContentId("skill_evolution.flame_brand.searing_brand")
    val PERMAFROST_EVOLUTION_ID = ContentId("skill_evolution.frost_lance.permafrost")
    val SHATTER_SPEAR_EVOLUTION_ID = ContentId("skill_evolution.frost_lance.shatter_spear")
    val REAPERS_ARC_EVOLUTION_ID = ContentId("skill_evolution.umbral_cut.reapers_arc")
    val SANGUINE_EDGE_EVOLUTION_ID = ContentId("skill_evolution.umbral_cut.sanguine_edge")
    val QUICK_SLASH_ID = ContentId("skill.quick_slash")
    val FLAME_BRAND_ID = ContentId("skill.flame_brand")
    val CINDER_MARK_ID = ContentId("skill.cinder_mark")
    val GUARD_MEND_ID = ContentId("skill.guard_mend")
    val FROST_LANCE_ID = ContentId("skill.frost_lance")
    val ARCANE_PULSE_ID = ContentId("skill.arcane_pulse")
    val VITAL_SURGE_ID = ContentId("skill.vital_surge")
    val UMBRAL_CUT_ID = ContentId("skill.umbral_cut")

    val BURNING_STATUS_ID = ContentId("status.burning")
    val GUARD_FOCUS_STATUS_ID = ContentId("status.guard_focus")
    val STAGGER_STATUS_ID = ContentId("status.stagger")
    val DEEP_STAGGER_STATUS_ID = ContentId("status.deep_stagger")
    val CHILL_STATUS_ID = ContentId("status.chill")
    val VITAL_REGENERATION_STATUS_ID = ContentId("status.vital_regeneration")
    val FORGED_FLAME_ID = ContentId("convergence.forged_flame")

    val SLIME_ID = ContentId("enemy.slime")
    val RIFTFANG_ID = ContentId("enemy.riftfang")
    val CINDER_WISP_ID = ContentId("enemy.cinder_wisp")
    val HOLLOW_BULWARK_ID = ContentId("enemy.hollow_bulwark")
    val ARCANE_SEER_ID = ContentId("enemy.arcane_seer")
    val SLIME_ADAPTATION_TAG_ID = ContentId("tag.enemy.slime")

    val SLIME_ATTACK_ID = ContentId("enemy_attack.slime_impact")
    val RIFTFANG_ATTACK_ID = ContentId("enemy_attack.riftfang_slash")
    val CINDER_WISP_ATTACK_ID = ContentId("enemy_attack.cinder_bolt")
    val HOLLOW_BULWARK_ATTACK_ID = ContentId("enemy_attack.bulwark_slam")
    val ARCANE_SEER_ATTACK_ID = ContentId("enemy_attack.arcane_burst")

    val BASIC_ATTACK_POWER_UPGRADE_ID = ContentId("upgrade.basic_attack_power")
    val ENDURANCE_UPGRADE_ID = ContentId("upgrade.endurance")
    val ARMOR_TRAINING_UPGRADE_ID = ContentId("upgrade.armor_training")
    val TEMPO_TRAINING_UPGRADE_ID = ContentId("upgrade.tempo_training")
    val PRECISION_UPGRADE_ID = ContentId("upgrade.precision")
    val LETHALITY_UPGRADE_ID = ContentId("upgrade.lethality")
    val CHANNELING_UPGRADE_ID = ContentId("upgrade.channeling")
    val RESTORATION_UPGRADE_ID = ContentId("upgrade.restoration")
    val BASIC_ATTACK_POWER_COST_FORMULA_ID = ContentId("cost.basic_attack_power.linear")
    val STANDARD_ADAPTATION_THRESHOLDS_ID = ContentId("adaptation.threshold.standard")
    val TRAINING_HOLLOW_ADAPTATION_REWARD_ID = ContentId("adaptation.reward.training_hollow")
    val ASH_SKIN_MUTATION_ID = ContentId("mutation.ash_skin")

    val TRAINING_BLADE_ITEM_ID = ContentId("item.training_blade")
    val TRAINING_BLADE_EQUIPMENT_ID = ContentId("equipment.training_blade")
    val TRAINING_CATALYST_ITEM_ID = ContentId("item.training_catalyst")
    val TRAINING_CATALYST_EQUIPMENT_ID = ContentId("equipment.training_catalyst")
    val KEEN_AFFIX_ID = ContentId("affix.keen")
    val TRAINING_SLIME_LOOT_TABLE_ID = ContentId("loot.training_slime")

    val PLAYER_LEVEL_CURVE_ID = ContentId("level_curve.player.standard")
    val CINDER_MARK_FEATURE_ID = ContentId("feature.skill.cinder_mark")
    val GUARD_MEND_FEATURE_ID = ContentId("feature.skill.guard_mend")
    val FLAME_BRAND_FEATURE_ID = ContentId("feature.skill.flame_brand")
    val FROST_LANCE_FEATURE_ID = ContentId("feature.skill.frost_lance")
    val ARCANE_PULSE_FEATURE_ID = ContentId("feature.skill.arcane_pulse")
    val VITAL_SURGE_FEATURE_ID = ContentId("feature.skill.vital_surge")
    val UMBRAL_CUT_FEATURE_ID = ContentId("feature.skill.umbral_cut")

    val FIRST_HUNT_QUEST_ID = ContentId("quest.training.first_hunt")
    val FIRST_HUNT_OBJECTIVE_ID =
        ContentId("objective.quest.training.first_hunt.slimes")
    val FORGED_FLAME_ACHIEVEMENT_ID =
        ContentId("achievement.convergence.first_forged_flame")
    val FORGED_FLAME_ACHIEVEMENT_OBJECTIVE_ID =
        ContentId("objective.achievement.convergence.first_forged_flame")

    val STANDARD_CHRONICLE_ID = ContentId("chronicle.standard")
    val FIRST_CHRONICLE_MILESTONE_ID = ContentId("milestone.chronicle.first_collapse")
    val ADAPTATION_FORECAST_ECHO_OFFER_ID = ContentId("echo_unlock.adaptation_forecast")
    /** Stable historical alias retained for V1 -> V2 migration fixtures. */
    val ADAPTATION_FORECAST_ECHO_UNLOCK_ID = ADAPTATION_FORECAST_ECHO_OFFER_ID
    val ADAPTATION_FORECAST_DISCOVERY_ID = ContentId("discovery.adaptation_forecast")
    val LEGACY_ACCELERATION_ECHO_OFFER_ID = ContentId("echo_unlock.legacy_acceleration")
    val DOCTRINE_MEMORY_ECHO_OFFER_ID = ContentId("echo_unlock.doctrine_memory")
    val EXPEDITION_MEMORY_ECHO_OFFER_ID = ContentId("echo_unlock.expedition_memory")
    val LEGACY_ACCELERATION_FEATURE_ID = ContentId("feature.echo.legacy_acceleration")
    val DOCTRINE_MEMORY_FEATURE_ID = ContentId("feature.echo.doctrine_memory")
    val EXPEDITION_MEMORY_FEATURE_ID = ContentId("feature.echo.expedition_memory")

    fun masteryDefinitionId(affinity: Affinity): ContentId =
        ContentId("mastery.${affinity.id.value.substringAfterLast('.')}")

    fun create(): GameContent = GameContent(
        basicAttack = BasicAttackDefinition(
            id = BASIC_ATTACK_ID,
            interval = GameDuration.ofMillis(1_000L),
            effects = listOf(
                EffectSpec.DealDamage(
                    powerRatio = Ratio.ONE,
                    flatBonus = GameNumber.ZERO,
                    damageKind = DamageKind.PHYSICAL
                )
            )
        ),
        world = WorldDefinition(
            id = WORLD_ID,
            regionIds = listOf(TRAINING_HOLLOW_REGION_ID),
            startingRegionIds = setOf(TRAINING_HOLLOW_REGION_ID)
        ),
        skills = listOf(
            SkillDefinition(
                id = HEAVY_STRIKE_ID,
                cooldown = GameDuration.ofSeconds(3L),
                recovery = GameDuration.ofMillis(1_500L),
                effects = listOf(
                    EffectSpec.DealDamage(Ratio.ofUnits(22_500L), canCritical = true),
                    EffectSpec.ApplyStatus(STAGGER_STATUS_ID)
                ),
                affinityTags = setOf(Affinity.MIGHT),
                resonanceEmissions = listOf(ResonanceEmissionDefinition(Affinity.MIGHT, GameNumber.ONE))
            ),
            SkillDefinition(
                id = QUICK_SLASH_ID,
                cooldown = GameDuration.ofMillis(1_200L),
                recovery = GameDuration.ofMillis(650L),
                effects = listOf(EffectSpec.DealDamage(Ratio.ofUnits(3_500L), hitCount = 3)),
                affinityTags = setOf(Affinity.TEMPO),
                resonanceEmissions = listOf(ResonanceEmissionDefinition(Affinity.TEMPO, GameNumber.ONE))
            ),
            SkillDefinition(
                id = FLAME_BRAND_ID,
                cooldown = GameDuration.ofSeconds(2L),
                effects = listOf(
                    EffectSpec.DealDamage(
                        Ratio.ofUnits(8_000L),
                        damageKind = DamageKind.ELEMENTAL
                    ),
                    EffectSpec.ApplyStatus(BURNING_STATUS_ID)
                ),
                affinityTags = setOf(Affinity.EMBER),
                resonanceEmissions = listOf(ResonanceEmissionDefinition(Affinity.EMBER, GameNumber.ONE)),
                requiredFeatureId = FLAME_BRAND_FEATURE_ID
            ),
            SkillDefinition(
                id = CINDER_MARK_ID,
                cooldown = GameDuration.ofSeconds(4L),
                effects = listOf(
                    EffectSpec.DealDamage(
                        Ratio.ofUnits(2_500L),
                        damageKind = DamageKind.ELEMENTAL,
                        canCritical = false
                    ),
                    EffectSpec.ApplyStatus(BURNING_STATUS_ID),
                    EffectSpec.ApplyStatus(BURNING_STATUS_ID)
                ),
                affinityTags = setOf(Affinity.EMBER),
                resonanceEmissions = listOf(ResonanceEmissionDefinition(Affinity.EMBER, GameNumber.ONE)),
                requiredFeatureId = CINDER_MARK_FEATURE_ID
            ),
            SkillDefinition(
                id = GUARD_MEND_ID,
                cooldown = GameDuration.ofSeconds(5L),
                targetingRule = SkillTargetingRule.SELF,
                effects = listOf(
                    EffectSpec.Heal(GameNumber.of(30L)),
                    EffectSpec.ApplyStatus(GUARD_FOCUS_STATUS_ID)
                ),
                affinityTags = setOf(Affinity.GUARD),
                resonanceEmissions = listOf(ResonanceEmissionDefinition(Affinity.GUARD, GameNumber.ONE)),
                requiredFeatureId = GUARD_MEND_FEATURE_ID
            ),
            SkillDefinition(
                id = FROST_LANCE_ID,
                cooldown = GameDuration.ofSeconds(2L),
                effects = listOf(
                    EffectSpec.DealDamage(
                        Ratio.ofUnits(9_000L),
                        damageKind = DamageKind.ELEMENTAL
                    ),
                    EffectSpec.ApplyStatus(CHILL_STATUS_ID)
                ),
                affinityTags = setOf(Affinity.FROST),
                resonanceEmissions = listOf(ResonanceEmissionDefinition(Affinity.FROST, GameNumber.ONE)),
                requiredFeatureId = FROST_LANCE_FEATURE_ID
            ),
            SkillDefinition(
                id = ARCANE_PULSE_ID,
                cooldown = GameDuration.ofSeconds(3L),
                effects = listOf(
                    EffectSpec.DealDamage(
                        powerRatio = Ratio.ofUnits(5_500L),
                        damageKind = DamageKind.ARCANE,
                        targetPattern = EffectSpec.TargetPattern.ALL_ENEMIES,
                        conditions = listOf(
                            EffectSpec.DamageCondition.TargetHasStatus(
                                CHILL_STATUS_ID, Ratio.ofUnits(5_000L)
                            )
                        )
                    )
                ),
                affinityTags = setOf(Affinity.ARCANE),
                resonanceEmissions = listOf(ResonanceEmissionDefinition(Affinity.ARCANE, GameNumber.ONE)),
                requiredFeatureId = ARCANE_PULSE_FEATURE_ID
            ),
            SkillDefinition(
                id = VITAL_SURGE_ID,
                cooldown = GameDuration.ofSeconds(6L),
                targetingRule = SkillTargetingRule.SELF,
                effects = listOf(EffectSpec.ApplyStatus(VITAL_REGENERATION_STATUS_ID)),
                affinityTags = setOf(Affinity.VITALITY),
                resonanceEmissions = listOf(ResonanceEmissionDefinition(Affinity.VITALITY, GameNumber.ONE)),
                requiredFeatureId = VITAL_SURGE_FEATURE_ID
            ),
            SkillDefinition(
                id = UMBRAL_CUT_ID,
                cooldown = GameDuration.ofSeconds(2L),
                targetingRule = SkillTargetingRule.LOWEST_HEALTH_ENEMY,
                effects = listOf(
                    EffectSpec.DealDamage(
                        powerRatio = Ratio.ofUnits(8_000L),
                        damageKind = DamageKind.SHADOW,
                        conditions = listOf(
                            EffectSpec.DamageCondition.TargetHealthAtOrBelow(
                                Ratio.ofUnits(3_000L), Ratio.ONE
                            )
                        )
                    )
                ),
                affinityTags = setOf(Affinity.SHADOW),
                resonanceEmissions = listOf(ResonanceEmissionDefinition(Affinity.SHADOW, GameNumber.ONE)),
                requiredFeatureId = UMBRAL_CUT_FEATURE_ID
            )
        ) + TrainingHollowStrategyContent.skills,
        skillEvolutions = listOf(
            SkillEvolutionDefinition(
                id = EARTHBREAKER_EVOLUTION_ID,
                baseSkillId = HEAVY_STRIKE_ID,
                requiredAffinity = Affinity.MIGHT,
                requiredMasteryLevel = 3L,
                replacementEffects = listOf(
                    EffectSpec.DealDamage(Ratio.ofUnits(18_000L), canCritical = true),
                    EffectSpec.DealDamage(
                        Ratio.ofUnits(9_000L),
                        canCritical = false,
                        targetPattern = EffectSpec.TargetPattern.ADJACENT_ENEMIES
                    ),
                    EffectSpec.ApplyStatus(DEEP_STAGGER_STATUS_ID)
                )
            ),
            SkillEvolutionDefinition(
                id = EXECUTIONER_EVOLUTION_ID,
                baseSkillId = HEAVY_STRIKE_ID,
                requiredAffinity = Affinity.MIGHT,
                requiredMasteryLevel = 3L,
                replacementEffects = listOf(
                    EffectSpec.DealDamage(
                        powerRatio = Ratio.ofUnits(18_000L),
                        canCritical = true,
                        conditions = listOf(
                            EffectSpec.DamageCondition.TargetHealthAtOrBelow(
                                threshold = Ratio.ofUnits(3_000L),
                                bonusPowerRatio = Ratio.ofUnits(16_000L)
                            )
                        )
                    ),
                    EffectSpec.ApplyStatus(DEEP_STAGGER_STATUS_ID)
                )
            ),
            SkillEvolutionDefinition(
                id = WILDSPARK_EVOLUTION_ID,
                baseSkillId = FLAME_BRAND_ID,
                requiredAffinity = Affinity.EMBER,
                requiredMasteryLevel = 3L,
                replacementEffects = listOf(
                    EffectSpec.DealDamage(
                        powerRatio = Ratio.ofUnits(5_000L),
                        damageKind = DamageKind.ELEMENTAL,
                        targetPattern = EffectSpec.TargetPattern.ALL_ENEMIES
                    ),
                    EffectSpec.ApplyStatus(BURNING_STATUS_ID, EffectSpec.TargetPattern.ALL_ENEMIES)
                )
            ),
            SkillEvolutionDefinition(
                id = SEARING_BRAND_EVOLUTION_ID,
                baseSkillId = FLAME_BRAND_ID,
                requiredAffinity = Affinity.EMBER,
                requiredMasteryLevel = 3L,
                replacementEffects = listOf(
                    EffectSpec.DealDamage(
                        Ratio.ofUnits(12_000L),
                        damageKind = DamageKind.ELEMENTAL
                    ),
                    EffectSpec.ApplyStatus(BURNING_STATUS_ID),
                    EffectSpec.ApplyStatus(BURNING_STATUS_ID)
                )
            ),
            SkillEvolutionDefinition(
                id = PERMAFROST_EVOLUTION_ID,
                baseSkillId = FROST_LANCE_ID,
                requiredAffinity = Affinity.FROST,
                requiredMasteryLevel = 3L,
                replacementEffects = listOf(
                    EffectSpec.DealDamage(
                        powerRatio = Ratio.ofUnits(5_500L),
                        damageKind = DamageKind.ELEMENTAL,
                        targetPattern = EffectSpec.TargetPattern.ALL_ENEMIES
                    ),
                    EffectSpec.ApplyStatus(
                        CHILL_STATUS_ID,
                        EffectSpec.TargetPattern.ALL_ENEMIES
                    )
                )
            ),
            SkillEvolutionDefinition(
                id = SHATTER_SPEAR_EVOLUTION_ID,
                baseSkillId = FROST_LANCE_ID,
                requiredAffinity = Affinity.FROST,
                requiredMasteryLevel = 3L,
                replacementEffects = listOf(
                    EffectSpec.DealDamage(
                        powerRatio = Ratio.ofUnits(8_000L),
                        damageKind = DamageKind.ELEMENTAL,
                        conditions = listOf(
                            EffectSpec.DamageCondition.TargetHasStatus(
                                CHILL_STATUS_ID,
                                Ratio.ofUnits(12_000L)
                            )
                        )
                    ),
                    EffectSpec.RemoveStatus(CHILL_STATUS_ID)
                )
            ),
            SkillEvolutionDefinition(
                id = REAPERS_ARC_EVOLUTION_ID,
                baseSkillId = UMBRAL_CUT_ID,
                requiredAffinity = Affinity.SHADOW,
                requiredMasteryLevel = 3L,
                replacementEffects = listOf(
                    EffectSpec.DealDamage(
                        powerRatio = Ratio.ofUnits(5_000L),
                        damageKind = DamageKind.SHADOW,
                        targetPattern = EffectSpec.TargetPattern.ALL_ENEMIES,
                        conditions = listOf(
                            EffectSpec.DamageCondition.TargetHealthAtOrBelow(
                                Ratio.ofUnits(3_000L),
                                Ratio.ofUnits(7_500L)
                            )
                        )
                    )
                )
            ),
            SkillEvolutionDefinition(
                id = SANGUINE_EDGE_EVOLUTION_ID,
                baseSkillId = UMBRAL_CUT_ID,
                requiredAffinity = Affinity.SHADOW,
                requiredMasteryLevel = 3L,
                replacementEffects = listOf(
                    EffectSpec.DealDamage(
                        powerRatio = Ratio.ofUnits(9_000L),
                        damageKind = DamageKind.SHADOW,
                        conditions = listOf(
                            EffectSpec.DamageCondition.TargetHealthAtOrBelow(
                                Ratio.ofUnits(3_000L),
                                Ratio.ofUnits(5_000L)
                            )
                        )
                    ),
                    EffectSpec.Heal(GameNumber.of(15L))
                )
            )
        ),
        statuses = listOf(
            StatusEffectDefinition(
                id = BURNING_STATUS_ID,
                displayName = "Burning",
                duration = GameDuration.ofSeconds(4L),
                stackingPolicy = StatusStackingPolicy.STACK_AND_REFRESH,
                maximumStacks = 3,
                periodicInterval = GameDuration.ofSeconds(1L),
                periodicEffects = listOf(
                    EffectSpec.DealDamage(
                        powerRatio = Ratio.ofUnits(2_500L),
                        damageKind = DamageKind.ELEMENTAL,
                        scalingPolicy = EffectSpec.DamageScalingPolicy.ATTACK_AND_EFFECT_POWER,
                        canCritical = false
                    )
                ),
                affinityTags = setOf(Affinity.EMBER)
            ),
            StatusEffectDefinition(
                id = GUARD_FOCUS_STATUS_ID,
                displayName = "Defense Up",
                duration = GameDuration.ofSeconds(3L),
                modifiers = listOf(StatusModifierDefinition.FlatArmor(GameNumber.of(15L))),
                affinityTags = setOf(Affinity.GUARD)
            ),
            StatusEffectDefinition(
                id = STAGGER_STATUS_ID,
                displayName = "Stagger",
                duration = GameDuration.ofSeconds(3L),
                modifiers = listOf(
                    StatusModifierDefinition.DamageTakenBonus(Ratio.ofUnits(1_500L))
                ),
                affinityTags = setOf(Affinity.MIGHT)
            ),
            StatusEffectDefinition(
                id = DEEP_STAGGER_STATUS_ID,
                displayName = "Heavy Stun",
                duration = GameDuration.ofSeconds(5L),
                modifiers = listOf(
                    StatusModifierDefinition.DamageTakenBonus(Ratio.ofUnits(2_500L))
                ),
                affinityTags = setOf(Affinity.MIGHT)
            ),
            StatusEffectDefinition(
                id = CHILL_STATUS_ID,
                displayName = "Chill",
                duration = GameDuration.ofSeconds(4L),
                modifiers = listOf(
                    StatusModifierDefinition.ActionIntervalMultiplier(Ratio.ofUnits(12_500L))
                ),
                affinityTags = setOf(Affinity.FROST)
            ),
            StatusEffectDefinition(
                id = VITAL_REGENERATION_STATUS_ID,
                displayName = "Regeneration",
                duration = GameDuration.ofSeconds(4L),
                periodicInterval = GameDuration.ofSeconds(1L),
                periodicEffects = listOf(EffectSpec.Heal(GameNumber.of(8L))),
                affinityTags = setOf(Affinity.VITALITY)
            )
        ) + TrainingHollowStrategyContent.statuses,
        enemyAttacks = listOf(
            EnemyAttackDefinition(SLIME_ATTACK_ID, "Slime Splash", GameDuration.ofMillis(2_200L), GameNumber.of(6L), affinity = Affinity.MIGHT),
            EnemyAttackDefinition(RIFTFANG_ATTACK_ID, "Rift Claw", GameDuration.ofMillis(900L), GameNumber.of(4L), affinity = Affinity.TEMPO),
            EnemyAttackDefinition(CINDER_WISP_ATTACK_ID, "Cinder Bolt", GameDuration.ofMillis(1_800L), GameNumber.of(9L), GameNumber.of(2L), Affinity.EMBER, damageKind = DamageKind.ELEMENTAL, appliedStatusId = TrainingHollowStrategyContent.SCORCHED_STATUS_ID),
            EnemyAttackDefinition(HOLLOW_BULWARK_ATTACK_ID, "Obsidian Slam", GameDuration.ofMillis(3_000L), GameNumber.of(18L), GameNumber.of(4L), Affinity.GUARD),
            EnemyAttackDefinition(ARCANE_SEER_ATTACK_ID, "Void Burst", GameDuration.ofMillis(2_000L), GameNumber.of(12L), GameNumber.of(8L), Affinity.ARCANE, damageKind = DamageKind.ARCANE, resonanceDrain = GameNumber.ONE)
        ) + TrainingHollowStrategyContent.enemyAttacks + HollowWardenContent.attack,
        enemies = listOf(
            EnemyDefinition(SLIME_ID, "Hollow Slime", GameNumber.of(100L), GameNumber.of(10L), GameNumber.of(10L), TRAINING_SLIME_LOOT_TABLE_ID, SLIME_ATTACK_ID, adaptationTags = setOf(SLIME_ADAPTATION_TAG_ID), role = com.idlerpg.game.domain.definition.enemy.EnemyRole.SWARM),
            EnemyDefinition(RIFTFANG_ID, "Riftfang", GameNumber.of(80L), GameNumber.of(12L), GameNumber.of(12L), TRAINING_SLIME_LOOT_TABLE_ID, RIFTFANG_ATTACK_ID, role = com.idlerpg.game.domain.definition.enemy.EnemyRole.ASSASSIN),
            EnemyDefinition(CINDER_WISP_ID, "Cinder Wisp", GameNumber.of(120L), GameNumber.of(14L), GameNumber.of(14L), TRAINING_SLIME_LOOT_TABLE_ID, CINDER_WISP_ATTACK_ID, role = com.idlerpg.game.domain.definition.enemy.EnemyRole.CASTER),
            EnemyDefinition(HOLLOW_BULWARK_ID, "Hollow Bulwark", GameNumber.of(180L), GameNumber.of(20L), GameNumber.of(20L), TRAINING_SLIME_LOOT_TABLE_ID, HOLLOW_BULWARK_ATTACK_ID, role = com.idlerpg.game.domain.definition.enemy.EnemyRole.PROTECTOR),
            EnemyDefinition(ARCANE_SEER_ID, "Magic Seer", GameNumber.of(150L), GameNumber.of(24L), GameNumber.of(24L), TRAINING_SLIME_LOOT_TABLE_ID, ARCANE_SEER_ATTACK_ID, role = com.idlerpg.game.domain.definition.enemy.EnemyRole.DISRUPTOR)
        ) + TrainingHollowStrategyContent.enemies + HollowWardenContent.enemy,
        costFormulas = listOf(
            CostFormulaDefinition.Linear(
                id = BASIC_ATTACK_POWER_COST_FORMULA_ID,
                baseCost = GameNumber.of(20L),
                incrementPerLevel = GameNumber.of(10L)
            )
        ),
        upgrades = listOf(
            UpgradeDefinition(
                id = BASIC_ATTACK_POWER_UPGRADE_ID,
                currencyId = CurrencyId.GOLD,
                costFormulaId = BASIC_ATTACK_POWER_COST_FORMULA_ID,
                effect = UpgradeEffectDefinition.FlatAttackPowerPerLevel(GameNumber.of(5L)),
                milestones = coreGrowthMilestones(),
                maxLevel = null
            ),
            UpgradeDefinition(
                ENDURANCE_UPGRADE_ID, CurrencyId.GOLD, BASIC_ATTACK_POWER_COST_FORMULA_ID,
                UpgradeEffectDefinition.FlatMaxHealthPerLevel(GameNumber.of(10L)),
                coreGrowthMilestones(), maxLevel = 50L
            ),
            UpgradeDefinition(
                ARMOR_TRAINING_UPGRADE_ID, CurrencyId.GOLD, BASIC_ATTACK_POWER_COST_FORMULA_ID,
                UpgradeEffectDefinition.FlatArmorPerLevel(GameNumber.of(2L)),
                coreGrowthMilestones(), maxLevel = 50L
            ),
            UpgradeDefinition(
                TEMPO_TRAINING_UPGRADE_ID, CurrencyId.GOLD, BASIC_ATTACK_POWER_COST_FORMULA_ID,
                UpgradeEffectDefinition.ActionSpeedPerLevel(Ratio.ofUnits(200L)),
                coreGrowthMilestones(), maxLevel = 50L
            ),
            UpgradeDefinition(
                PRECISION_UPGRADE_ID, CurrencyId.GOLD, BASIC_ATTACK_POWER_COST_FORMULA_ID,
                UpgradeEffectDefinition.CriticalChancePerLevel(Ratio.ofUnits(100L)),
                coreGrowthMilestones(), maxLevel = 50L
            ),
            UpgradeDefinition(
                LETHALITY_UPGRADE_ID, CurrencyId.GOLD, BASIC_ATTACK_POWER_COST_FORMULA_ID,
                UpgradeEffectDefinition.CriticalMultiplierPerLevel(Ratio.ofUnits(250L)),
                coreGrowthMilestones(), maxLevel = 50L
            ),
            UpgradeDefinition(
                CHANNELING_UPGRADE_ID, CurrencyId.GOLD, BASIC_ATTACK_POWER_COST_FORMULA_ID,
                UpgradeEffectDefinition.EffectPowerPerLevel(Ratio.ofUnits(250L)),
                coreGrowthMilestones(), maxLevel = 50L
            ),
            UpgradeDefinition(
                RESTORATION_UPGRADE_ID, CurrencyId.GOLD, BASIC_ATTACK_POWER_COST_FORMULA_ID,
                UpgradeEffectDefinition.HealingPowerPerLevel(Ratio.ofUnits(250L)),
                coreGrowthMilestones(), maxLevel = 50L
            )
        ),
        regions = listOf(
            RegionDefinition(
                id = TRAINING_HOLLOW_REGION_ID,
                displayName = "Training Grounds",
                encounterIds = listOf(
                    TRAINING_SLIME_ENCOUNTER_ID,
                    RIFTFANG_ENCOUNTER_ID,
                    CINDER_WISP_ENCOUNTER_ID,
                    HOLLOW_BULWARK_ENCOUNTER_ID,
                    ARCANE_SEER_ENCOUNTER_ID,
                    TrainingHollowStrategyContent.FROSTBOUND_MITE_ENCOUNTER_ID,
                    TrainingHollowStrategyContent.ECHO_LEECH_ENCOUNTER_ID,
                    TrainingHollowStrategyContent.SHADE_MIMIC_ENCOUNTER_ID
                ) + TrainingHollowWorldContent.encounters.map { it.id },
                bossIds = listOf(HollowWardenContent.BOSS_ID) +
                    TrainingHollowWorldContent.additionalBosses.map { it.id },
                enemyHealthGrowthPerTier = GameNumber.of(5L),
                rewardMultiplier = Ratio.ONE,
                adaptationThresholdDefinitionId = STANDARD_ADAPTATION_THRESHOLDS_ID,
                adaptationMutationIds = TrainingHollowAdaptationContent.mutations.map { it.id },
                adaptationRewardDefinitionId = TRAINING_HOLLOW_ADAPTATION_REWARD_ID,
                maxAdaptationMutationsPerEnemy = 1
            )
        ),
        encounters = listOf(
            EncounterDefinition(TRAINING_SLIME_ENCOUNTER_ID, TRAINING_HOLLOW_REGION_ID, EncounterType.NORMAL, listOf(SLIME_ID), nextEncounterId = RIFTFANG_ENCOUNTER_ID),
            EncounterDefinition(RIFTFANG_ENCOUNTER_ID, TRAINING_HOLLOW_REGION_ID, EncounterType.NORMAL, listOf(RIFTFANG_ID), nextEncounterId = CINDER_WISP_ENCOUNTER_ID),
            EncounterDefinition(CINDER_WISP_ENCOUNTER_ID, TRAINING_HOLLOW_REGION_ID, EncounterType.NORMAL, listOf(CINDER_WISP_ID), nextEncounterId = HOLLOW_BULWARK_ENCOUNTER_ID),
            EncounterDefinition(HOLLOW_BULWARK_ENCOUNTER_ID, TRAINING_HOLLOW_REGION_ID, EncounterType.ELITE, listOf(HOLLOW_BULWARK_ID, SLIME_ID), waves = 2, nextEncounterId = ARCANE_SEER_ENCOUNTER_ID, rewardLootTableId = TrainingHollowLootContent.ELITE_LOOT_TABLE_ID),
            EncounterDefinition(ARCANE_SEER_ENCOUNTER_ID, TRAINING_HOLLOW_REGION_ID, EncounterType.ANOMALY, listOf(ARCANE_SEER_ID), nextEncounterId = TrainingHollowStrategyContent.FROSTBOUND_MITE_ENCOUNTER_ID, rewardLootTableId = TrainingHollowLootContent.ANOMALY_LOOT_TABLE_ID)
        ) + TrainingHollowStrategyContent.encounters + TrainingHollowWorldContent.encounters,
        bosses = listOf(HollowWardenContent.boss) + TrainingHollowWorldContent.additionalBosses,
        convergences = listOf(
            ConvergenceDefinition(
                id = FORGED_FLAME_ID,
                pattern = ResonancePatternDefinition(
                    affinities = listOf(Affinity.MIGHT, Affinity.MIGHT, Affinity.EMBER)
                ),
                minimumChargeByAffinity = mapOf(
                    Affinity.MIGHT to GameNumber.of(2L),
                    Affinity.EMBER to GameNumber.ONE
                ),
                consumePolicy = ResonanceConsumePolicy.REQUIRED_CHARGE,
                priority = 100,
                cooldown = GameDuration.ZERO,
                maxTriggersPerEncounter = null,
                effects = listOf(
                    EffectSpec.DealDamage(Ratio.ONE, damageKind = DamageKind.PHYSICAL)
                ),
                discoverOnFirstTrigger = true
            )
        ) + TrainingHollowStrategyContent.convergences,
        adaptationThresholds = listOf(
            AdaptationThresholdDefinition(
                id = STANDARD_ADAPTATION_THRESHOLDS_ID,
                minimumPressureByTier = (1..com.idlerpg.game.core.config.AdaptationCurve.MAXIMUM_TIER)
                    .associateWith(com.idlerpg.game.core.config.AdaptationCurve::threshold)
            )
        ),
        mutations = TrainingHollowAdaptationContent.mutations,
        adaptationRewards = listOf(
            AdaptationRewardDefinition(
                id = TRAINING_HOLLOW_ADAPTATION_REWARD_ID,
                bonusPerActiveMutation = Ratio.HALF,
                maximumMultiplier = Ratio.ofUnits(30_000L)
            )
        ),
        equipmentDefinitions = listOf(
            EquipmentDefinition(
                id = TRAINING_BLADE_EQUIPMENT_ID,
                slot = EquipmentSlot.WEAPON,
                effects = listOf(
                    EquipmentEffectDefinition.FlatAttackPower(GameNumber.of(2L)),
                    EquipmentEffectDefinition.SkillCleave(
                        HEAVY_STRIKE_ID,
                        Ratio.ofUnits(4_000L)
                    )
                )
            ),
            EquipmentDefinition(
                id = TRAINING_CATALYST_EQUIPMENT_ID,
                slot = EquipmentSlot.CATALYST,
                effects = listOf(
                    EquipmentEffectDefinition.ResonanceChargeBonus(
                        affinity = Affinity.MIGHT,
                        amountPerEmission = GameNumber.ONE
                    ),
                    EquipmentEffectDefinition.SkillSequencePreservation(
                        ARCANE_PULSE_ID,
                        entryCount = 1
                    )
                )
            )
        ) + TrainingHollowLootContent.equipmentDefinitions,
        affixes = listOf(
            AffixDefinition(
                id = KEEN_AFFIX_ID,
                displayName = "Keen",
                compatibleSlots = setOf(EquipmentSlot.WEAPON),
                selectionWeight = 100L,
                minimumRollValue = 1L,
                maximumRollValue = 5L,
                effect = AffixEffectDefinition.FlatAttackPowerPerRollUnit(GameNumber.ONE)
            )
        ) + TrainingHollowLootContent.affixes,
        items = listOf(
            ItemDefinition(
                id = TRAINING_BLADE_ITEM_ID,
                displayName = "Training Blade",
                category = ItemCategory.EQUIPMENT,
                minimumRarity = Rarity.COMMON,
                maximumRarity = Rarity.LEGENDARY,
                equipmentDefinitionId = TRAINING_BLADE_EQUIPMENT_ID,
                allowedAffixIds = TrainingHollowLootContent.allowedAffixIdsFor(EquipmentSlot.WEAPON),
                salvageProfile = SalvageProfile(
                    goldByRarity = mapOf(
                        Rarity.COMMON to GameNumber.ONE,
                        Rarity.UNCOMMON to GameNumber.of(2L),
                        Rarity.RARE to GameNumber.of(4L),
                        Rarity.EPIC to GameNumber.of(8L),
                        Rarity.LEGENDARY to GameNumber.of(16L)
                    )
                )
            ),
            ItemDefinition(
                id = TRAINING_CATALYST_ITEM_ID,
                displayName = "Training Catalyst",
                category = ItemCategory.EQUIPMENT,
                minimumRarity = Rarity.COMMON,
                maximumRarity = Rarity.LEGENDARY,
                equipmentDefinitionId = TRAINING_CATALYST_EQUIPMENT_ID,
                allowedAffixIds = TrainingHollowLootContent.allowedAffixIdsFor(EquipmentSlot.CATALYST),
                salvageProfile = SalvageProfile(
                    goldByRarity = Rarity.ordered().associateWith { rarity ->
                        GameNumber.of((rarity.rank + 1L) * 2L)
                    }
                )
            )
        ) + TrainingHollowLootContent.items,
        lootTables = TrainingHollowLootContent.lootTables,
        defaultPlayerLevelCurveId = PLAYER_LEVEL_CURVE_ID,
        levelCurves = listOf(
            LevelCurveDefinition.Progressive(
                id = PLAYER_LEVEL_CURVE_ID,
                baseExperienceToNextLevel = GameNumber.of(20L),
                experienceIncrementPerLevel = GameNumber.of(10L),
                accelerationStartLevel = 10L,
                accelerationPerLevel = GameNumber.ONE,
                maxLevel = 10_000L
            )
        ),
        featureUnlocks = listOf(
            FeatureUnlockDefinition(CINDER_MARK_FEATURE_ID, FeatureUnlockScope.RUN, 5L),
            FeatureUnlockDefinition(GUARD_MEND_FEATURE_ID, FeatureUnlockScope.RUN, 3L),
            FeatureUnlockDefinition(
                FLAME_BRAND_FEATURE_ID, FeatureUnlockScope.RUN, 20L,
                requiredMasteryLevels = mapOf(Affinity.EMBER.id to 3L)
            ),
            FeatureUnlockDefinition(FROST_LANCE_FEATURE_ID, FeatureUnlockScope.RUN, 8L),
            FeatureUnlockDefinition(ARCANE_PULSE_FEATURE_ID, FeatureUnlockScope.RUN, 14L),
            FeatureUnlockDefinition(VITAL_SURGE_FEATURE_ID, FeatureUnlockScope.RUN, 11L),
            FeatureUnlockDefinition(UMBRAL_CUT_FEATURE_ID, FeatureUnlockScope.RUN, 17L),
            FeatureUnlockDefinition(LEGACY_ACCELERATION_FEATURE_ID, FeatureUnlockScope.META),
            FeatureUnlockDefinition(DOCTRINE_MEMORY_FEATURE_ID, FeatureUnlockScope.META),
            FeatureUnlockDefinition(EXPEDITION_MEMORY_FEATURE_ID, FeatureUnlockScope.META)
        ) + TrainingHollowStrategyContent.featureUnlocks + EchoTrainingContent.features,
        masteries = Affinity.values().map { affinity ->
            MasteryDefinition(
                id = masteryDefinitionId(affinity),
                affinity = affinity,
                baseExperienceToNextLevel = GameNumber.of(20L),
                experienceIncrementPerLevel = GameNumber.of(10L),
                maxLevel = 20L
            )
        },
        quests = listOf(
            QuestDefinition(
                id = FIRST_HUNT_QUEST_ID,
                displayName = "First Hunt",
                objectives = listOf(
                    QuestObjectiveDefinition.KillEnemy(
                        id = FIRST_HUNT_OBJECTIVE_ID,
                        enemyDefinitionId = SLIME_ID,
                        requiredCount = GameNumber.of(3L)
                    )
                ),
                reward = QuestRewardDefinition(
                    currencies = mapOf(
                        CurrencyId.GOLD to GameNumber.of(25L)
                    )
                )
            )
        ) + TrainingHollowQuestContent.quests,
        achievements = listOf(
            AchievementDefinition(
                id = FORGED_FLAME_ACHIEVEMENT_ID,
                displayName = "Forged Flame",
                objectives = listOf(
                    AchievementObjectiveDefinition.TriggerConvergence(
                        id = FORGED_FLAME_ACHIEVEMENT_OBJECTIVE_ID,
                        convergenceId = FORGED_FLAME_ID,
                        requiredCount = GameNumber.ONE
                    )
                ),
                reward = AchievementRewardDefinition(
                    currencies = mapOf(
                        CurrencyId.GOLD to GameNumber.of(10L)
                    )
                )
            )
        ),
        defaultChronicleDefinitionId = STANDARD_CHRONICLE_ID,
        chronicleDefinitions = listOf(
            ChronicleDefinition(
                id = STANDARD_CHRONICLE_ID,
                resetRuleVersion = 1,
                requiredTotalNormalClears = GameNumber.of(15L),
                echoReward = GameNumber.of(5L),
                milestoneId = FIRST_CHRONICLE_MILESTONE_ID,
                requiredBossId = HollowWardenContent.BOSS_ID,
                echoPerDeepestStage = GameNumber.ONE,
                echoPerEliteClear = GameNumber.ONE
            )
        ),
        echoOffers = listOf(
            EchoOfferDefinition(
                id = ADAPTATION_FORECAST_ECHO_OFFER_ID,
                cost = GameNumber.of(5L),
                effects = listOf(
                    EchoUnlockEffect.RevealHiddenContent(
                        contentId = ADAPTATION_FORECAST_DISCOVERY_ID
                    )
                )
            ),
            EchoOfferDefinition(LEGACY_ACCELERATION_ECHO_OFFER_ID, GameNumber.of(12L), listOf(EchoUnlockEffect.UnlockPersistentFeature(LEGACY_ACCELERATION_FEATURE_ID))),
            EchoOfferDefinition(DOCTRINE_MEMORY_ECHO_OFFER_ID, GameNumber.of(18L), listOf(EchoUnlockEffect.UnlockPersistentFeature(DOCTRINE_MEMORY_FEATURE_ID)), requiredOfferIds = setOf(LEGACY_ACCELERATION_ECHO_OFFER_ID)),
            EchoOfferDefinition(EXPEDITION_MEMORY_ECHO_OFFER_ID, GameNumber.of(18L), listOf(EchoUnlockEffect.UnlockPersistentFeature(EXPEDITION_MEMORY_FEATURE_ID)), requiredOfferIds = setOf(LEGACY_ACCELERATION_ECHO_OFFER_ID))
        ) + EchoTrainingContent.offers
    )

    private fun coreGrowthMilestones(): List<UpgradeMilestoneDefinition> = listOf(
        UpgradeMilestoneDefinition(level = 10L, bonusEquivalentLevels = 2L),
        UpgradeMilestoneDefinition(level = 20L, bonusEquivalentLevels = 3L)
    )

    fun registry(): ContentRegistry = ContentRegistry(create())
}
