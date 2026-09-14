package com.idlerpg.game.data.content

import com.idlerpg.game.core.number.GameNumber
import com.idlerpg.game.core.time.GameDuration
import com.idlerpg.game.core.validation.ValidationIssue
import com.idlerpg.game.core.validation.ValidationResult
import com.idlerpg.game.core.validation.ValidationSeverity
import com.idlerpg.game.domain.definition.Affinity
import com.idlerpg.game.domain.definition.adaptation.MutationEffectDefinition
import com.idlerpg.game.domain.definition.achievement.AchievementObjectiveDefinition
import com.idlerpg.game.domain.definition.chronicle.EchoUnlockEffect
import com.idlerpg.game.domain.definition.combat.EffectSpec
import com.idlerpg.game.domain.definition.combat.StatusEffectDefinition
import com.idlerpg.game.domain.definition.economy.CostFormulaDefinition
import com.idlerpg.game.domain.definition.economy.UpgradeEffectDefinition
import com.idlerpg.game.domain.definition.item.ItemCategory
import com.idlerpg.game.domain.definition.progression.FeatureUnlockScope
import com.idlerpg.game.domain.definition.progression.LevelCurveDefinition
import com.idlerpg.game.domain.definition.quest.QuestObjectiveDefinition
import com.idlerpg.game.domain.definition.resonance.ResonanceConsumePolicy
import com.idlerpg.game.domain.definition.world.EncounterType

/** Validates the complete authored-content graph through Foundation 17. */
object ContentValidator {

    fun validate(content: GameContent): ValidationResult {
        val issues = mutableListOf<ValidationIssue>()

        validateUniqueIds(content, issues)
        validateBasicAttack(content, issues)
        validateStatuses(content, issues)
        validateSkills(content, issues)
        validateSkillEvolutions(content, issues)
        validateEnemyAttacks(content, issues)
        validateEnemies(content, issues)
        validateCostFormulas(content, issues)
        validateUpgrades(content, issues)
        validateWorld(content, issues)
        validateRegions(content, issues)
        validateEncounters(content, issues)
        validateBosses(content, issues)
        validateConvergences(content, issues)
        validateAdaptation(content, issues)
        validateItems(content, issues)
        validateEquipment(content, issues)
        validateAffixes(content, issues)
        validateLootTables(content, issues)
        validateProgression(content, issues)
        validateQuests(content, issues)
        validateAchievements(content, issues)
        validateChronicle(content, issues)

        return ValidationResult(issues)
    }

    private fun validateUniqueIds(
        content: GameContent,
        issues: MutableList<ValidationIssue>
    ) {
        val allIds = buildList {
            add(content.basicAttack.id)
            add(content.world.id)
            addAll(content.skills.map { it.id })
            addAll(content.skillEvolutions.map { it.id })
            addAll(content.statuses.map { it.id })
            addAll(content.enemyAttacks.map { it.id })
            addAll(content.enemies.map { it.id })
            addAll(content.costFormulas.map { it.id })
            addAll(content.upgrades.map { it.id })
            addAll(content.regions.map { it.id })
            addAll(content.encounters.map { it.id })
            addAll(content.bosses.map { it.id })
            addAll(content.convergences.map { it.id })
            addAll(content.adaptationThresholds.map { it.id })
            addAll(content.mutations.map { it.id })
            addAll(content.adaptationRewards.map { it.id })
            addAll(content.items.map { it.id })
            addAll(content.equipmentDefinitions.map { it.id })
            addAll(content.affixes.map { it.id })
            addAll(content.lootTables.map { it.id })
            addAll(content.levelCurves.map { it.id })
            addAll(content.featureUnlocks.map { it.id })
            addAll(content.masteries.map { it.id })
            addAll(content.quests.map { it.id })
            addAll(content.achievements.map { it.id })
            addAll(content.chronicleDefinitions.map { it.id })
            addAll(content.echoUnlocks.map { it.id })
            addAll(content.echoOffers.map { it.id })
        }

        allIds
            .groupingBy { it }
            .eachCount()
            .filterValues { count -> count > 1 }
            .keys
            .sorted()
            .forEach { duplicateId ->
                issues += error(
                    code = "content.duplicate_id",
                    path = "content[$duplicateId]",
                    message = "ContentId $duplicateId is declared more than once"
                )
            }
    }

    private fun validateBasicAttack(
        content: GameContent,
        issues: MutableList<ValidationIssue>
    ) {
        val basicAttack = content.basicAttack

        if (basicAttack.interval <= GameDuration.ZERO) {
            issues += error(
                code = "content.basic_attack.invalid_interval",
                path = "basicAttack.interval",
                message = "Basic attack interval must be greater than zero"
            )
        }

        if (basicAttack.effects.isEmpty()) {
            issues += error(
                code = "content.basic_attack.no_effects",
                path = "basicAttack.effects",
                message = "Basic attack must contain at least one EffectSpec"
            )
        }

        basicAttack.effects.forEachIndexed { index, effect ->
            validateEffect(
                effect = effect,
                path = "basicAttack.effects[$index]",
                content = content,
                issues = issues
            )
        }
    }

    private fun validateStatuses(
        content: GameContent,
        issues: MutableList<ValidationIssue>
    ) {
        content.statuses.sortedBy { it.id }.forEach { status ->
            if (status.displayName.isBlank()) {
                issues += error(
                    code = "content.status.blank_display_name",
                    path = "statuses[${status.id}].displayName",
                    message = "Status ${status.id} must have a non-blank display name"
                )
            }
            if (status.duration <= GameDuration.ZERO) {
                issues += error(
                    code = "content.status.invalid_duration",
                    path = "statuses[${status.id}].duration",
                    message = "Status ${status.id} duration must be > 0"
                )
            }
            if (status.maximumStacks <= 0) {
                issues += error(
                    code = "content.status.invalid_max_stacks",
                    path = "statuses[${status.id}].maximumStacks",
                    message = "Status ${status.id} maximumStacks must be positive"
                )
            }
            if (status.periodicInterval != null && status.periodicInterval <= GameDuration.ZERO) {
                issues += error(
                    code = "content.status.invalid_periodic_interval",
                    path = "statuses[${status.id}].periodicInterval",
                    message = "Status ${status.id} periodic interval must be > 0"
                )
            }
            status.periodicEffects.forEachIndexed { index, effect ->
                validateEffect(
                    effect = effect,
                    path = "statuses[${status.id}].periodicEffects[$index]",
                    content = content,
                    issues = issues
                )
            }
            status.expireEffects.forEachIndexed { index, effect ->
                validateEffect(
                    effect = effect,
                    path = "statuses[${status.id}].expireEffects[$index]",
                    content = content,
                    issues = issues
                )
            }
        }
    }

    private fun validateSkills(
        content: GameContent,
        issues: MutableList<ValidationIssue>
    ) {
        content.skills
            .sortedBy { it.id }
            .forEach { skill ->
                if (skill.effects.isEmpty()) {
                    issues += error(
                        code = "content.skill.no_effects",
                        path = "skills[${skill.id}].effects",
                        message = "Skill ${skill.id} must contain at least one EffectSpec"
                    )
                }

                skill.effects.forEachIndexed { index, effect ->
                    validateEffect(
                        effect = effect,
                        path = "skills[${skill.id}].effects[$index]",
                        content = content,
                        issues = issues
                    )
                }

                skill.resonanceEmissions.forEachIndexed { index, emission ->
                    if (emission.amount <= GameNumber.ZERO) {
                        issues += error(
                            code = "content.skill.invalid_resonance_amount",
                            path = "skills[${skill.id}].resonanceEmissions[$index].amount",
                            message = "Resonance emission amount must be greater than zero"
                        )
                    }
                }

                val requiredFeatureId = skill.requiredFeatureId
                if (requiredFeatureId != null &&
                    content.featureUnlocks.none { it.id == requiredFeatureId }
                ) {
                    issues += error(
                        code = "content.skill.unknown_feature_unlock",
                        path = "skills[${skill.id}].requiredFeatureId",
                        message = "Skill ${skill.id} references unknown feature $requiredFeatureId"
                    )
                }
            }
    }

    private fun validateSkillEvolutions(
        content: GameContent,
        issues: MutableList<ValidationIssue>
    ) {
        val knownSkillIds = content.skills.map { it.id }.toSet()
        val knownMasteryAffinities = content.masteries.map { it.affinity }.toSet()
        val masteryByAffinity = content.masteries.associateBy { it.affinity }
        content.skillEvolutions.sortedBy { it.id }.forEach { evolution ->
            if (evolution.id == evolution.baseSkillId) {
                issues += error(
                    code = "content.skill_evolution.self_reference",
                    path = "skillEvolutions[${evolution.id}].baseSkillId",
                    message = "Evolution ID cannot equal its base skill ID"
                )
            }
            if (evolution.baseSkillId !in knownSkillIds) {
                issues += error(
                    code = "content.skill_evolution.unknown_base_skill",
                    path = "skillEvolutions[${evolution.id}].baseSkillId",
                    message = "Evolution ${evolution.id} references unknown skill ${evolution.baseSkillId}"
                )
            }
            if (evolution.requiredAffinity !in knownMasteryAffinities) {
                issues += error(
                    code = "content.skill_evolution.unknown_mastery",
                    path = "skillEvolutions[${evolution.id}].requiredAffinity",
                    message = "Evolution ${evolution.id} requires an unauthored mastery"
                )
            }
            val maximumMasteryLevel = masteryByAffinity[evolution.requiredAffinity]?.maxLevel
            if (maximumMasteryLevel != null &&
                evolution.requiredMasteryLevel > maximumMasteryLevel
            ) {
                issues += error(
                    code = "content.skill_evolution.unreachable_mastery",
                    path = "skillEvolutions[${evolution.id}].requiredMasteryLevel",
                    message = "Evolution ${evolution.id} requires mastery ${evolution.requiredMasteryLevel} above maximum $maximumMasteryLevel"
                )
            }
            evolution.replacementEffects.forEachIndexed { index, effect ->
                validateEffect(
                    effect,
                    "skillEvolutions[${evolution.id}].replacementEffects[$index]",
                    content,
                    issues
                )
            }
        }
        content.skillEvolutions.groupBy { it.baseSkillId }.filterValues { it.size > 2 }
            .keys.sorted().forEach { skillId ->
                issues += error(
                    code = "content.skill_evolution.too_many_branches",
                    path = "skillEvolutions[$skillId]",
                    message = "The first-region evolution contract allows at most two branches per skill"
                )
            }
    }

    private fun validateEnemyAttacks(
        content: GameContent,
        issues: MutableList<ValidationIssue>
    ) {
        content.enemyAttacks.sortedBy { it.id }.forEach { attack ->
            if (attack.displayName.isBlank()) {
                issues += error(
                    code = "content.enemy_attack.blank_display_name",
                    path = "enemyAttacks[${attack.id}].displayName",
                    message = "Enemy attack ${attack.id} must have a non-blank display name"
                )
            }
            if (attack.interval <= GameDuration.ZERO) {
                issues += error(
                    code = "content.enemy_attack.invalid_interval",
                    path = "enemyAttacks[${attack.id}].interval",
                    message = "Enemy attack ${attack.id} interval must be > 0"
                )
            }
            if (attack.baseDamage <= GameNumber.ZERO) {
                issues += error(
                    code = "content.enemy_attack.invalid_damage",
                    path = "enemyAttacks[${attack.id}].baseDamage",
                    message = "Enemy attack ${attack.id} baseDamage must be > 0"
                )
            }
            if (attack.appliedStatusId != null &&
                content.statuses.none { it.id == attack.appliedStatusId }
            ) {
                issues += error(
                    code = "content.enemy_attack.unknown_status",
                    path = "enemyAttacks[${attack.id}].appliedStatusId",
                    message = "Enemy attack ${attack.id} references unknown status ${attack.appliedStatusId}"
                )
            }
        }
    }

    private fun validateEnemies(
        content: GameContent,
        issues: MutableList<ValidationIssue>
    ) {
        val knownSkillIds = content.skills.map { it.id }.toSet()
        val knownAttackIds = content.enemyAttacks.map { it.id }.toSet()

        content.enemies
            .sortedBy { it.id }
            .forEach { enemy ->
                if (enemy.displayName.isBlank()) {
                    issues += error(
                        code = "content.enemy.blank_display_name",
                        path = "enemies[${enemy.id}].displayName",
                        message = "Enemy ${enemy.id} must have a non-blank display name"
                    )
                }

                if (enemy.baseHealth <= GameNumber.ZERO) {
                    issues += error(
                        code = "content.enemy.invalid_health",
                        path = "enemies[${enemy.id}].baseHealth",
                        message = "Enemy ${enemy.id} baseHealth must be greater than zero"
                    )
                }

                val attackDefinitionId = enemy.attackDefinitionId
                if (attackDefinitionId != null && attackDefinitionId !in knownAttackIds) {
                    issues += error(
                        code = "content.enemy.unknown_attack",
                        path = "enemies[${enemy.id}].attackDefinitionId",
                        message = "Enemy ${enemy.id} references unknown enemy attack $attackDefinitionId"
                    )
                }

                if (enemy.skillIds.size != enemy.skillIds.toSet().size) {
                    issues += error(
                        code = "content.enemy.duplicate_skill",
                        path = "enemies[${enemy.id}].skillIds",
                        message = "Enemy ${enemy.id} skillIds cannot contain duplicates"
                    )
                }

                enemy.skillIds
                    .filterNot { it in knownSkillIds }
                    .sorted()
                    .forEach { missingSkillId ->
                        issues += error(
                            code = "content.enemy.unknown_skill",
                            path = "enemies[${enemy.id}].skillIds[$missingSkillId]",
                            message = "Enemy ${enemy.id} references unknown skill $missingSkillId"
                        )
                    }

                val lootTableId = enemy.lootTableId
                if (lootTableId != null &&
                    content.lootTables.none { it.id == lootTableId }
                ) {
                    issues += error(
                        code = "content.enemy.unknown_loot_table",
                        path = "enemies[${enemy.id}].lootTableId",
                        message = "Enemy ${enemy.id} references unknown loot table $lootTableId"
                    )
                }
            }
    }

    private fun validateCostFormulas(
        content: GameContent,
        issues: MutableList<ValidationIssue>
    ) {
        content.costFormulas
            .sortedBy { it.id }
            .forEach { formula ->
                when (formula) {
                    is CostFormulaDefinition.Linear -> {
                        if (formula.baseCost <= GameNumber.ZERO) {
                            issues += error(
                                code = "content.cost_formula.invalid_base",
                                path = "costFormulas[${formula.id}].baseCost",
                                message = "Linear cost formula ${formula.id} baseCost must be > 0"
                            )
                        }
                    }
                }
            }
    }

    private fun validateUpgrades(
        content: GameContent,
        issues: MutableList<ValidationIssue>
    ) {
        val knownFormulaIds = content.costFormulas.map { it.id }.toSet()

        content.upgrades
            .sortedBy { it.id }
            .forEach { upgrade ->
                if (upgrade.costFormulaId !in knownFormulaIds) {
                    issues += error(
                        code = "content.upgrade.unknown_cost_formula",
                        path = "upgrades[${upgrade.id}].costFormulaId",
                        message =
                            "Upgrade ${upgrade.id} references unknown cost formula " +
                                "${upgrade.costFormulaId}"
                    )
                }

                if (upgrade.maxLevel != null && upgrade.maxLevel <= 0L) {
                    issues += error(
                        code = "content.upgrade.invalid_max_level",
                        path = "upgrades[${upgrade.id}].maxLevel",
                        message = "Upgrade ${upgrade.id} maxLevel must be positive"
                    )
                }

                when (val effect = upgrade.effect) {
                    is UpgradeEffectDefinition.FlatAttackPowerPerLevel -> {
                        if (effect.amountPerLevel <= GameNumber.ZERO) {
                            issues += error(
                                code = "content.upgrade.invalid_attack_effect",
                                path = "upgrades[${upgrade.id}].effect.amountPerLevel",
                                message =
                                    "Upgrade ${upgrade.id} attack amount per level must be > 0"
                            )
                        }
                    }
                    is UpgradeEffectDefinition.FlatMaxHealthPerLevel ->
                        validatePositiveUpgradeAmount(upgrade.id, "max_health", effect.amountPerLevel, issues)
                    is UpgradeEffectDefinition.FlatArmorPerLevel ->
                        validatePositiveUpgradeAmount(upgrade.id, "armor", effect.amountPerLevel, issues)
                    is UpgradeEffectDefinition.ActionSpeedPerLevel ->
                        validatePositiveUpgradeRatio(upgrade.id, "action_speed", effect.ratioPerLevel, issues)
                    is UpgradeEffectDefinition.CriticalChancePerLevel ->
                        validatePositiveUpgradeRatio(upgrade.id, "critical_chance", effect.ratioPerLevel, issues)
                    is UpgradeEffectDefinition.CriticalMultiplierPerLevel ->
                        validatePositiveUpgradeRatio(upgrade.id, "critical_multiplier", effect.ratioPerLevel, issues)
                    is UpgradeEffectDefinition.EffectPowerPerLevel ->
                        validatePositiveUpgradeRatio(upgrade.id, "effect_power", effect.ratioPerLevel, issues)
                    is UpgradeEffectDefinition.HealingPowerPerLevel ->
                        validatePositiveUpgradeRatio(upgrade.id, "healing_power", effect.ratioPerLevel, issues)
                }

                upgrade.milestones.forEachIndexed { index, milestone ->
                    if (milestone.level <= 0L || milestone.bonusEquivalentLevels <= 0L ||
                        (upgrade.maxLevel != null && milestone.level > upgrade.maxLevel)
                    ) {
                        issues += error(
                            code = "content.upgrade.invalid_milestone",
                            path = "upgrades[${upgrade.id}].milestones[$index]",
                            message = "Upgrade ${upgrade.id} has an invalid milestone"
                        )
                    }
                }
            }
    }

    private fun validatePositiveUpgradeAmount(
        upgradeId: com.idlerpg.game.core.id.ContentId,
        effectName: String,
        amount: GameNumber,
        issues: MutableList<ValidationIssue>
    ) {
        if (amount <= GameNumber.ZERO) {
            issues += error(
                code = "content.upgrade.invalid_${effectName}_effect",
                path = "upgrades[$upgradeId].effect",
                message = "Upgrade $upgradeId $effectName amount per level must be > 0"
            )
        }
    }

    private fun validatePositiveUpgradeRatio(
        upgradeId: com.idlerpg.game.core.id.ContentId,
        effectName: String,
        ratio: com.idlerpg.game.core.number.Ratio,
        issues: MutableList<ValidationIssue>
    ) {
        if (ratio <= com.idlerpg.game.core.number.Ratio.ZERO) {
            issues += error(
                code = "content.upgrade.invalid_${effectName}_effect",
                path = "upgrades[$upgradeId].effect",
                message = "Upgrade $upgradeId $effectName ratio per level must be > 0"
            )
        }
    }

    private fun validateWorld(
        content: GameContent,
        issues: MutableList<ValidationIssue>
    ) {
        val knownRegionIds = content.regions.map { it.id }.toSet()

        content.world.regionIds
            .filterNot { it in knownRegionIds }
            .sorted()
            .forEach { missingRegionId ->
                issues += error(
                    code = "content.world.unknown_region",
                    path = "world.regionIds[$missingRegionId]",
                    message = "World ${content.world.id} references unknown region $missingRegionId"
                )
            }

        content.world.startingRegionIds
            .filterNot { it in knownRegionIds }
            .sorted()
            .forEach { missingRegionId ->
                issues += error(
                    code = "content.world.unknown_starting_region",
                    path = "world.startingRegionIds[$missingRegionId]",
                    message =
                        "World ${content.world.id} starting region $missingRegionId does not exist"
                )
            }
    }

    private fun validateRegions(
        content: GameContent,
        issues: MutableList<ValidationIssue>
    ) {
        val encountersById = content.encounters.associateBy { it.id }
        val bossesById = content.bosses.associateBy { it.id }

        content.regions
            .sortedBy { it.id }
            .forEach { region ->
                region.encounterIds.forEach { encounterId ->
                    val encounter = encountersById[encounterId]
                    if (encounter == null) {
                        issues += error(
                            code = "content.region.unknown_encounter",
                            path = "regions[${region.id}].encounterIds[$encounterId]",
                            message = "Region ${region.id} references unknown encounter $encounterId"
                        )
                    } else if (encounter.regionId != region.id) {
                        issues += error(
                            code = "content.region.encounter_region_mismatch",
                            path = "regions[${region.id}].encounterIds[$encounterId]",
                            message =
                                "Encounter $encounterId belongs to ${encounter.regionId}, " +
                                    "not ${region.id}"
                        )
                    }
                }

                region.bossIds.forEach { bossId ->
                    val boss = bossesById[bossId]
                    if (boss == null) {
                        issues += error(
                            code = "content.region.unknown_boss",
                            path = "regions[${region.id}].bossIds[$bossId]",
                            message = "Region ${region.id} references unknown boss $bossId"
                        )
                    } else if (boss.regionId != region.id) {
                        issues += error(
                            code = "content.region.boss_region_mismatch",
                            path = "regions[${region.id}].bossIds[$bossId]",
                            message =
                                "Boss $bossId belongs to ${boss.regionId}, not ${region.id}"
                        )
                    }
                }
            }
    }

    private fun validateEncounters(
        content: GameContent,
        issues: MutableList<ValidationIssue>
    ) {
        val knownRegionIds = content.regions.map { it.id }.toSet()
        val knownEnemyIds = content.enemies.map { it.id }.toSet()
        val knownLootTableIds = content.lootTables.map { it.id }.toSet()
        val encountersById = content.encounters.associateBy { it.id }
        val bossesById = content.bosses.associateBy { it.id }

        content.encounters
            .sortedBy { it.id }
            .forEach { encounter ->
                if (encounter.regionId !in knownRegionIds) {
                    issues += error(
                        code = "content.encounter.unknown_region",
                        path = "encounters[${encounter.id}].regionId",
                        message =
                            "Encounter ${encounter.id} references unknown region " +
                                "${encounter.regionId}"
                    )
                }

                if (encounter.waveEnemyDefinitionIds.isEmpty()) {
                    encounter.enemyDefinitionIds
                        .filterNot { it in knownEnemyIds }
                        .sorted()
                        .forEach { enemyId ->
                            issues += error(
                                code = "content.encounter.unknown_enemy",
                                path = "encounters[${encounter.id}].enemyDefinitionIds[$enemyId]",
                                message = "Encounter ${encounter.id} references unknown enemy $enemyId"
                            )
                        }
                } else {
                    (1..encounter.waves).forEach { wave ->
                        encounter.enemyDefinitionIdsForWave(wave)
                            .filterNot { it in knownEnemyIds }
                            .sorted()
                            .forEach { enemyId ->
                                issues += error(
                                    code = "content.encounter.unknown_enemy",
                                    path = "encounters[${encounter.id}].wave[$wave].enemyDefinitionIds[$enemyId]",
                                    message = "Encounter ${encounter.id} wave $wave references unknown enemy $enemyId"
                                )
                            }
                    }
                }

                if (encounter.rewardLootTableId != null &&
                    encounter.rewardLootTableId !in knownLootTableIds
                ) {
                    issues += error(
                        code = "content.encounter.unknown_reward_loot_table",
                        path = "encounters[${encounter.id}].rewardLootTableId",
                        message = "Encounter ${encounter.id} references unknown reward loot table ${encounter.rewardLootTableId}"
                    )
                }

                encounter.nextEncounterId?.let { nextId ->
                    val next = encountersById[nextId]
                    if (next == null) {
                        issues += error(
                            code = "content.encounter.unknown_next",
                            path = "encounters[${encounter.id}].nextEncounterId",
                            message =
                                "Encounter ${encounter.id} references unknown next encounter $nextId"
                        )
                    } else if (next.regionId != encounter.regionId) {
                        issues += error(
                            code = "content.encounter.next_region_mismatch",
                            path = "encounters[${encounter.id}].nextEncounterId",
                            message =
                                "Encounter ${encounter.id} cannot continue into region " +
                                    "${next.regionId}"
                        )
                    }
                }

                if (encounter.type == EncounterType.BOSS) {
                    val bossId = encounter.bossId
                    val boss = bossId?.let(bossesById::get)

                    if (boss == null) {
                        issues += error(
                            code = "content.encounter.unknown_boss",
                            path = "encounters[${encounter.id}].bossId",
                            message = "Boss encounter ${encounter.id} must reference a known boss"
                        )
                    } else if (boss.encounterDefinitionId != encounter.id) {
                        issues += error(
                            code = "content.encounter.boss_mismatch",
                            path = "encounters[${encounter.id}].bossId",
                            message =
                                "Boss ${boss.id} points to ${boss.encounterDefinitionId}, " +
                                    "not ${encounter.id}"
                        )
                    }
                }
            }
    }

    private fun validateBosses(
        content: GameContent,
        issues: MutableList<ValidationIssue>
    ) {
        val regionsById = content.regions.associateBy { it.id }
        val encountersById = content.encounters.associateBy { it.id }

        content.bosses
            .sortedBy { it.id }
            .forEach { boss ->
                val region = regionsById[boss.regionId]
                if (region == null) {
                    issues += error(
                        code = "content.boss.unknown_region",
                        path = "bosses[${boss.id}].regionId",
                        message = "Boss ${boss.id} references unknown region ${boss.regionId}"
                    )
                } else if (boss.id !in region.bossIds) {
                    issues += error(
                        code = "content.boss.missing_region_reference",
                        path = "bosses[${boss.id}]",
                        message =
                            "Boss ${boss.id} is not listed by owning region ${boss.regionId}"
                    )
                }

                val encounter = encountersById[boss.encounterDefinitionId]
                if (encounter == null) {
                    issues += error(
                        code = "content.boss.unknown_encounter",
                        path = "bosses[${boss.id}].encounterDefinitionId",
                        message =
                            "Boss ${boss.id} references unknown encounter " +
                                "${boss.encounterDefinitionId}"
                    )
                } else {
                    if (encounter.regionId != boss.regionId) {
                        issues += error(
                            code = "content.boss.encounter_region_mismatch",
                            path = "bosses[${boss.id}].encounterDefinitionId",
                            message =
                                "Boss ${boss.id} encounter belongs to ${encounter.regionId}, " +
                                    "not ${boss.regionId}"
                        )
                    }
                    if (encounter.type != EncounterType.BOSS) {
                        issues += error(
                            code = "content.boss.encounter_not_boss",
                            path = "bosses[${boss.id}].encounterDefinitionId",
                            message =
                                "Boss ${boss.id} must reference an encounter of type BOSS"
                        )
                    }
                    if (encounter.bossId != boss.id) {
                        issues += error(
                            code = "content.boss.encounter_back_reference",
                            path = "bosses[${boss.id}].encounterDefinitionId",
                            message =
                                "Boss encounter ${encounter.id} must reference boss ${boss.id}"
                        )
                    }
                }
            }
    }

    private fun validateConvergences(
        content: GameContent,
        issues: MutableList<ValidationIssue>
    ) {
        content.convergences
            .sortedBy { it.id }
            .forEach { convergence ->
                if (convergence.pattern.affinities.isEmpty()) {
                    issues += error(
                        code = "content.convergence.empty_pattern",
                        path = "convergences[${convergence.id}].pattern",
                        message = "Convergence pattern cannot be empty"
                    )
                }

                convergence.minimumChargeByAffinity
                    .entries
                    .sortedBy { it.key.id }
                    .forEach { (affinity, amount) ->
                        if (amount <= GameNumber.ZERO) {
                            issues += error(
                                code = "content.convergence.invalid_minimum_charge",
                                path = "convergences[${convergence.id}].minimumCharge[${affinity.id}]",
                                message = "Minimum Resonance charge must be greater than zero"
                            )
                        }
                    }

                if (convergence.consumePolicy == ResonanceConsumePolicy.REQUIRED_CHARGE &&
                    convergence.minimumChargeByAffinity.isEmpty()
                ) {
                    issues += error(
                        code = "content.convergence.consume_without_requirement",
                        path = "convergences[${convergence.id}].consumePolicy",
                        message = "REQUIRED_CHARGE requires at least one minimum charge entry"
                    )
                }

                if (convergence.maxTriggersPerEncounter != null &&
                    convergence.maxTriggersPerEncounter <= 0L
                ) {
                    issues += error(
                        code = "content.convergence.invalid_trigger_limit",
                        path = "convergences[${convergence.id}].maxTriggersPerEncounter",
                        message = "Convergence trigger limit must be positive when set"
                    )
                }

                if (convergence.effects.isEmpty()) {
                    issues += error(
                        code = "content.convergence.no_effects",
                        path = "convergences[${convergence.id}].effects",
                        message = "Convergence must contain at least one EffectSpec"
                    )
                }

                convergence.effects.forEachIndexed { index, effect ->
                    validateEffect(
                        effect = effect,
                        path = "convergences[${convergence.id}].effects[$index]",
                        content = content,
                        issues = issues
                    )
                }
            }
    }

    private fun validateAdaptation(
        content: GameContent,
        issues: MutableList<ValidationIssue>
    ) {
        val thresholdsById = content.adaptationThresholds.associateBy { it.id }
        val mutationsById = content.mutations.associateBy { it.id }
        val rewardsById = content.adaptationRewards.associateBy { it.id }

        content.adaptationThresholds
            .sortedBy { it.id }
            .forEach { definition ->
                val ordered = definition.minimumPressureByTier.entries.sortedBy { it.key }

                if (ordered.isEmpty()) {
                    issues += error(
                        code = "content.adaptation_threshold.empty",
                        path = "adaptationThresholds[${definition.id}]",
                        message = "Adaptation threshold definition cannot be empty"
                    )
                }

                if (ordered.map { it.key } != (1..ordered.size).toList()) {
                    issues += error(
                        code = "content.adaptation_threshold.non_contiguous_tiers",
                        path = "adaptationThresholds[${definition.id}].minimumPressureByTier",
                        message = "Adaptation tiers must be contiguous from tier 1"
                    )
                }

                var previous = GameNumber.ZERO
                for ((tier, threshold) in ordered) {
                    if (threshold <= previous) {
                        issues += error(
                            code = "content.adaptation_threshold.non_increasing",
                            path =
                                "adaptationThresholds[${definition.id}].minimumPressureByTier[$tier]",
                            message = "Adaptation thresholds must strictly increase by tier"
                        )
                    }
                    previous = threshold
                }
            }

        content.mutations
            .sortedBy { it.id }
            .forEach { mutation ->
                if (mutation.displayName.isBlank()) {
                    issues += error(
                        code = "content.mutation.blank_display_name",
                        path = "mutations[${mutation.id}].displayName",
                        message = "Mutation displayName cannot be blank"
                    )
                }
                if (mutation.minimumAdaptationTier <= 0) {
                    issues += error(
                        code = "content.mutation.invalid_minimum_tier",
                        path = "mutations[${mutation.id}].minimumAdaptationTier",
                        message = "Mutation minimum tier must be positive"
                    )
                }
                if (mutation.selectionWeight <= 0L) {
                    issues += error(
                        code = "content.mutation.invalid_weight",
                        path = "mutations[${mutation.id}].selectionWeight",
                        message = "Mutation selectionWeight must be positive"
                    )
                }

                mutation.effects.forEachIndexed { effectIndex, effect ->
                    val effectPath = "mutations[${mutation.id}].effects[$effectIndex]"
                    when (effect) {
                        is MutationEffectDefinition.DamageTakenMultiplierForAffinity ->
                            if (effect.multiplier.units <= 0L) {
                                issues += error(
                                    code = "content.mutation.invalid_damage_multiplier",
                                    path = "$effectPath.multiplier",
                                    message = "Mutation damage multiplier must be greater than zero"
                                )
                            }
                        is MutationEffectDefinition.EnemyActionIntervalMultiplier ->
                            if (effect.multiplier.units !in 1L..20_000L) {
                                issues += error(
                                    code = "content.mutation.invalid_interval_multiplier",
                                    path = "$effectPath.multiplier",
                                    message = "Mutation interval multiplier must be within (0%, 200%]"
                                )
                            }
                        is MutationEffectDefinition.AdditionalResonanceDrain ->
                            if (effect.amount <= com.idlerpg.game.core.number.GameNumber.ZERO) {
                                issues += error(
                                    code = "content.mutation.invalid_resonance_drain",
                                    path = "$effectPath.amount",
                                    message = "Mutation Resonance drain must be positive"
                                )
                            }
                        is MutationEffectDefinition.PlayerHealingMultiplier ->
                            if (effect.multiplier.units !in 1L..10_000L) {
                                issues += error(
                                    code = "content.mutation.invalid_healing_multiplier",
                                    path = "$effectPath.multiplier",
                                    message = "Mutation healing multiplier must be within (0%, 100%]"
                                )
                            }
                        is MutationEffectDefinition.EnemyDamageMultiplierWhilePlayerHasAffinityStatus ->
                            if (effect.multiplier.units !in 10_000L..20_000L) {
                                issues += error(
                                    code = "content.mutation.invalid_conditional_damage_multiplier",
                                    path = "$effectPath.multiplier",
                                    message = "Conditional damage multiplier must be within [100%, 200%]"
                                )
                            }
                    }
                }
            }

        content.adaptationRewards
            .sortedBy { it.id }
            .forEach { reward ->
                if (reward.maximumMultiplier < com.idlerpg.game.core.number.Ratio.ONE) {
                    issues += error(
                        code = "content.adaptation_reward.invalid_maximum",
                        path = "adaptationRewards[${reward.id}].maximumMultiplier",
                        message = "Adaptation reward maximum must be at least 100%"
                    )
                }
            }

        content.regions
            .sortedBy { it.id }
            .forEach { region ->
                val thresholdId = region.adaptationThresholdDefinitionId
                if (thresholdId != null && thresholdId !in thresholdsById) {
                    issues += error(
                        code = "content.region.unknown_adaptation_threshold",
                        path = "regions[${region.id}].adaptationThresholdDefinitionId",
                        message = "Region ${region.id} references unknown Adaptation threshold $thresholdId"
                    )
                }

                if (region.adaptationMutationIds.size != region.adaptationMutationIds.toSet().size) {
                    issues += error(
                        code = "content.region.duplicate_adaptation_mutation",
                        path = "regions[${region.id}].adaptationMutationIds",
                        message = "Region ${region.id} Adaptation mutation IDs cannot duplicate"
                    )
                }

                region.adaptationMutationIds
                    .filterNot { it in mutationsById }
                    .sorted()
                    .forEach { mutationId ->
                        issues += error(
                            code = "content.region.unknown_adaptation_mutation",
                            path = "regions[${region.id}].adaptationMutationIds[$mutationId]",
                            message = "Region ${region.id} references unknown mutation $mutationId"
                        )
                    }

                val rewardId = region.adaptationRewardDefinitionId
                if (rewardId != null && rewardId !in rewardsById) {
                    issues += error(
                        code = "content.region.unknown_adaptation_reward",
                        path = "regions[${region.id}].adaptationRewardDefinitionId",
                        message = "Region ${region.id} references unknown Adaptation reward $rewardId"
                    )
                }

                if (region.adaptationMutationIds.isNotEmpty() &&
                    region.maxAdaptationMutationsPerEnemy <= 0
                ) {
                    issues += error(
                        code = "content.region.invalid_adaptation_mutation_slots",
                        path = "regions[${region.id}].maxAdaptationMutationsPerEnemy",
                        message = "Region with Adaptation mutations requires at least one mutation slot"
                    )
                }

                val thresholds = thresholdId?.let(thresholdsById::get)
                if (thresholds != null) {
                    val maximumTier =
                        thresholds.minimumPressureByTier.keys.maxOrNull() ?: 0
                    region.adaptationMutationIds
                        .mapNotNull(mutationsById::get)
                        .filter { it.minimumAdaptationTier > maximumTier }
                        .sortedBy { it.id }
                        .forEach { mutation ->
                            issues += error(
                                code = "content.region.unreachable_adaptation_mutation",
                                path = "regions[${region.id}].adaptationMutationIds[${mutation.id}]",
                                message =
                                    "Mutation ${mutation.id} requires tier " +
                                        "${mutation.minimumAdaptationTier}, but region threshold " +
                                        "curve reaches only tier $maximumTier"
                            )
                        }
                }
            }
    }


    private fun validateItems(
        content: GameContent,
        issues: MutableList<ValidationIssue>
    ) {
        val equipmentById = content.equipmentDefinitions.associateBy { it.id }
        val affixesById = content.affixes.associateBy { it.id }

        content.items
            .sortedBy { it.id }
            .forEach { item ->
                if (item.displayName.isBlank()) {
                    issues += error(
                        code = "content.item.blank_display_name",
                        path = "items[${item.id}].displayName",
                        message = "Item ${item.id} must have a non-blank display name"
                    )
                }

                val equipmentId = item.equipmentDefinitionId
                when (item.category) {
                    ItemCategory.EQUIPMENT -> {
                        val equipment = equipmentId?.let(equipmentById::get)
                        if (equipment == null) {
                            issues += error(
                                code = "content.item.unknown_equipment_definition",
                                path = "items[${item.id}].equipmentDefinitionId",
                                message =
                                    "Equipment item ${item.id} must reference known EquipmentDefinition"
                            )
                        } else {
                            item.allowedAffixIds
                                .mapNotNull(affixesById::get)
                                .filter { equipment.slot !in it.compatibleSlots }
                                .sortedBy { it.id }
                                .forEach { affix ->
                                    issues += error(
                                        code = "content.item.incompatible_affix",
                                        path = "items[${item.id}].allowedAffixIds[${affix.id}]",
                                        message =
                                            "Affix ${affix.id} is incompatible with slot ${equipment.slot}"
                                    )
                                }
                        }
                    }

                    ItemCategory.MATERIAL -> {
                        if (equipmentId != null) {
                            issues += error(
                                code = "content.item.material_has_equipment_definition",
                                path = "items[${item.id}].equipmentDefinitionId",
                                message = "Material item ${item.id} cannot be equipment"
                            )
                        }
                    }
                }

                if (item.allowedAffixIds.size != item.allowedAffixIds.toSet().size) {
                    issues += error(
                        code = "content.item.duplicate_affix",
                        path = "items[${item.id}].allowedAffixIds",
                        message = "Item ${item.id} cannot repeat allowed affix IDs"
                    )
                }

                item.allowedAffixIds
                    .filterNot { it in affixesById }
                    .sorted()
                    .forEach { affixId ->
                        issues += error(
                            code = "content.item.unknown_affix",
                            path = "items[${item.id}].allowedAffixIds[$affixId]",
                            message = "Item ${item.id} references unknown affix $affixId"
                        )
                    }

                val requiredSalvageRarities =
                    RarityRange.between(
                        minimum = item.minimumRarity,
                        maximum = item.maximumRarity
                    )
                requiredSalvageRarities
                    .filterNot { it in item.salvageProfile.goldByRarity }
                    .forEach { rarity ->
                        issues += error(
                            code = "content.item.missing_salvage_value",
                            path = "items[${item.id}].salvageProfile[$rarity]",
                            message =
                                "Item ${item.id} must define salvage Gold for allowed rarity $rarity"
                        )
                    }
            }
    }

    private fun validateEquipment(
        content: GameContent,
        issues: MutableList<ValidationIssue>
    ) {
        val referencedIds =
            content.items.mapNotNull { it.equipmentDefinitionId }.toSet()

        content.equipmentDefinitions
            .sortedBy { it.id }
            .filterNot { it.id in referencedIds }
            .forEach { equipment ->
                issues += error(
                    code = "content.equipment.unreferenced",
                    path = "equipmentDefinitions[${equipment.id}]",
                    message =
                        "EquipmentDefinition ${equipment.id} is not referenced by an item"
                )
            }
    }

    private fun validateAffixes(
        content: GameContent,
        issues: MutableList<ValidationIssue>
    ) {
        content.affixes
            .sortedBy { it.id }
            .forEach { affix ->
                if (affix.displayName.isBlank()) {
                    issues += error(
                        code = "content.affix.blank_display_name",
                        path = "affixes[${affix.id}].displayName",
                        message = "Affix ${affix.id} must have a non-blank display name"
                    )
                }
                if (affix.compatibleSlots.isEmpty()) {
                    issues += error(
                        code = "content.affix.no_compatible_slots",
                        path = "affixes[${affix.id}].compatibleSlots",
                        message = "Affix ${affix.id} must support at least one slot"
                    )
                }
                if (affix.selectionWeight <= 0L) {
                    issues += error(
                        code = "content.affix.invalid_weight",
                        path = "affixes[${affix.id}].selectionWeight",
                        message = "Affix ${affix.id} selection weight must be positive"
                    )
                }
                if (affix.minimumRollValue < 0L ||
                    affix.maximumRollValue < affix.minimumRollValue
                ) {
                    issues += error(
                        code = "content.affix.invalid_roll_range",
                        path = "affixes[${affix.id}]",
                        message = "Affix ${affix.id} roll range is invalid"
                    )
                }
            }
    }

    private fun validateLootTables(
        content: GameContent,
        issues: MutableList<ValidationIssue>
    ) {
        val itemsById = content.items.associateBy { it.id }

        content.lootTables
            .sortedBy { it.id }
            .forEach { table ->
                if (table.rolls <= 0) {
                    issues += error(
                        code = "content.loot_table.invalid_rolls",
                        path = "lootTables[${table.id}].rolls",
                        message = "Loot table ${table.id} rolls must be positive"
                    )
                }

                if (table.entries.isEmpty()) {
                    issues += error(
                        code = "content.loot_table.empty",
                        path = "lootTables[${table.id}].entries",
                        message = "Loot table ${table.id} cannot be empty"
                    )
                }

                if (table.entries.map { it.itemDefinitionId }.size !=
                    table.entries.map { it.itemDefinitionId }.toSet().size
                ) {
                    issues += error(
                        code = "content.loot_table.duplicate_item",
                        path = "lootTables[${table.id}].entries",
                        message = "Loot table ${table.id} cannot repeat an item definition"
                    )
                }

                table.entries
                    .sortedBy { it.itemDefinitionId }
                    .forEach { entry ->
                        val item = itemsById[entry.itemDefinitionId]
                        if (item == null) {
                            issues += error(
                                code = "content.loot_table.unknown_item",
                                path =
                                    "lootTables[${table.id}].entries[${entry.itemDefinitionId}]",
                                message =
                                    "Loot table ${table.id} references unknown item " +
                                        "${entry.itemDefinitionId}"
                            )
                        } else {
                            if (entry.weight <= 0L) {
                                issues += error(
                                    code = "content.loot_table.invalid_item_weight",
                                    path =
                                        "lootTables[${table.id}].entries[${entry.itemDefinitionId}].weight",
                                    message = "Loot entry weight must be positive"
                                )
                            }

                            val eligiblePositiveRarities =
                                entry.rarityWeights
                                    .filterValues { it > 0L }
                                    .keys
                                    .filter(item::allowsRarity)

                            if (eligiblePositiveRarities.isEmpty()) {
                                issues += error(
                                    code = "content.loot_table.no_eligible_rarity",
                                    path =
                                        "lootTables[${table.id}].entries[${entry.itemDefinitionId}]" +
                                            ".rarityWeights",
                                    message =
                                        "Loot entry ${entry.itemDefinitionId} has no positive rarity " +
                                            "inside the item rarity bounds"
                                )
                            }

                            entry.rarityWeights
                                .filterValues { it < 0L }
                                .keys
                                .forEach { rarity ->
                                    issues += error(
                                        code = "content.loot_table.negative_rarity_weight",
                                        path =
                                            "lootTables[${table.id}].entries[${entry.itemDefinitionId}]" +
                                                ".rarityWeights[$rarity]",
                                        message = "Loot rarity weights cannot be negative"
                                    )
                                }
                        }
                    }
            }
    }

    private fun validateProgression(
        content: GameContent,
        issues: MutableList<ValidationIssue>
    ) {
        val defaultCurveId = content.defaultPlayerLevelCurveId
        if (defaultCurveId == null) {
            issues += error(
                code = "content.progression.missing_default_level_curve",
                path = "defaultPlayerLevelCurveId",
                message = "Foundation 13 content must define a default player level curve"
            )
        } else if (content.levelCurves.none { it.id == defaultCurveId }) {
            issues += error(
                code = "content.progression.unknown_default_level_curve",
                path = "defaultPlayerLevelCurveId",
                message = "Default player level curve $defaultCurveId does not exist"
            )
        }

        content.levelCurves.sortedBy { it.id }.forEach { curve ->
            when (curve) {
                is LevelCurveDefinition.Linear -> {
                    if (curve.baseExperienceToNextLevel <= GameNumber.ZERO) {
                        issues += error(
                            code = "content.progression.invalid_level_curve_base",
                            path = "levelCurves[${curve.id}].baseExperienceToNextLevel",
                            message = "Level curve base experience must be > 0"
                        )
                    }
                    if (curve.maxLevel != null && curve.maxLevel!! < 1L) {
                        issues += error(
                            code = "content.progression.invalid_level_curve_max",
                            path = "levelCurves[${curve.id}].maxLevel",
                            message = "Level curve maxLevel must be >= 1"
                        )
                    }
                }
                is LevelCurveDefinition.Progressive -> {
                    if (curve.baseExperienceToNextLevel <= GameNumber.ZERO) {
                        issues += error(
                            code = "content.progression.invalid_level_curve_base",
                            path = "levelCurves[${curve.id}].baseExperienceToNextLevel",
                            message = "Level curve base experience must be > 0"
                        )
                    }
                    if (curve.experienceIncrementPerLevel < GameNumber.ZERO) {
                        issues += error(
                            code = "content.progression.invalid_level_curve_increment",
                            path = "levelCurves[${curve.id}].experienceIncrementPerLevel",
                            message = "Progressive level curve increment cannot be negative"
                        )
                    }
                    if (curve.accelerationStartLevel < 1L) {
                        issues += error(
                            code = "content.progression.invalid_level_curve_acceleration_start",
                            path = "levelCurves[${curve.id}].accelerationStartLevel",
                            message = "Progressive level curve accelerationStartLevel must be >= 1"
                        )
                    }
                    if (curve.accelerationPerLevel < GameNumber.ZERO) {
                        issues += error(
                            code = "content.progression.invalid_level_curve_acceleration",
                            path = "levelCurves[${curve.id}].accelerationPerLevel",
                            message = "Progressive level curve acceleration cannot be negative"
                        )
                    }
                    if (curve.maxLevel != null && curve.maxLevel!! < 1L) {
                        issues += error(
                            code = "content.progression.invalid_level_curve_max",
                            path = "levelCurves[${curve.id}].maxLevel",
                            message = "Level curve maxLevel must be >= 1"
                        )
                    }
                }
            }
        }

        val knownAffinityIds = Affinity.values().map { it.id }.toSet()
        val duplicateMasteryAffinities = content.masteries
            .groupingBy { it.affinity.id }
            .eachCount()
            .filterValues { it > 1 }
            .keys
            .sorted()
        duplicateMasteryAffinities.forEach { affinityId ->
            issues += error(
                code = "content.progression.duplicate_mastery_affinity",
                path = "masteries[$affinityId]",
                message = "Only one MasteryDefinition may own affinity $affinityId"
            )
        }

        content.masteries.sortedBy { it.id }.forEach { mastery ->
            if (mastery.affinity.id !in knownAffinityIds) {
                issues += error(
                    code = "content.progression.unknown_mastery_affinity",
                    path = "masteries[${mastery.id}].affinity",
                    message = "Mastery ${mastery.id} references unknown affinity ${mastery.affinity.id}"
                )
            }
            if (mastery.baseExperienceToNextLevel <= GameNumber.ZERO) {
                issues += error(
                    code = "content.progression.invalid_mastery_base",
                    path = "masteries[${mastery.id}].baseExperienceToNextLevel",
                    message = "Mastery base experience must be > 0"
                )
            }
            val masteryMaxLevel = mastery.maxLevel
            if (masteryMaxLevel != null && masteryMaxLevel < 1L) {
                issues += error(
                    code = "content.progression.invalid_mastery_max",
                    path = "masteries[${mastery.id}].maxLevel",
                    message = "Mastery maxLevel must be >= 1"
                )
            }
        }

        val masteryAffinityIds = content.masteries.map { it.affinity.id }.toSet()
        content.featureUnlocks.sortedBy { it.id }.forEach { feature ->
            if (feature.requiredPlayerLevel <= 0L) {
                issues += error(
                    code = "content.progression.invalid_feature_level",
                    path = "featureUnlocks[${feature.id}].requiredPlayerLevel",
                    message = "Feature required player level must be positive"
                )
            }
            feature.requiredMasteryLevels.entries.sortedBy { it.key }.forEach { (affinityId, level) ->
                if (affinityId !in knownAffinityIds) {
                    issues += error(
                        code = "content.progression.unknown_feature_affinity",
                        path = "featureUnlocks[${feature.id}].requiredMasteryLevels[$affinityId]",
                        message = "Feature ${feature.id} references unknown affinity $affinityId"
                    )
                } else if (affinityId !in masteryAffinityIds) {
                    issues += error(
                        code = "content.progression.missing_feature_mastery_definition",
                        path = "featureUnlocks[${feature.id}].requiredMasteryLevels[$affinityId]",
                        message = "Feature ${feature.id} requires mastery without a MasteryDefinition"
                    )
                }
                if (level <= 0L) {
                    issues += error(
                        code = "content.progression.invalid_feature_mastery_level",
                        path = "featureUnlocks[${feature.id}].requiredMasteryLevels[$affinityId]",
                        message = "Feature mastery requirement must be positive"
                    )
                }
            }
        }
    }


    private fun validateQuests(
        content: GameContent,
        issues: MutableList<ValidationIssue>
    ) {
        val enemyIds = content.enemies.map { it.id }.toSet()
        val encounterIds = content.encounters.map { it.id }.toSet()
        val convergenceIds = content.convergences.map { it.id }.toSet()
        val itemIds = content.items.map { it.id }.toSet()
        val lootTableIds = content.lootTables.map { it.id }.toSet()
        val featureIds = content.featureUnlocks.map { it.id }.toSet()

        content.quests.sortedBy { it.id }.forEach { quest ->
            if (quest.repeatable && quest.reward.lootTableIds.isNotEmpty()) {
                issues += error(
                    code = "content.quest.repeatable_loot_not_supported",
                    path = "quests[${quest.id}].reward.lootTableIds",
                    message = "Repeatable contracts bank currency and XP; loot rewards must use a milestone quest"
                )
            }

            quest.requiredFeatureIds.sorted().forEach { featureId ->
                if (featureId !in featureIds) {
                    issues += error(
                        code = "content.quest.unknown_required_feature",
                        path = "quests[${quest.id}].requiredFeatureIds[$featureId]",
                        message = "Quest ${quest.id} references unknown feature $featureId"
                    )
                }
            }

            quest.objectives.sortedBy { it.id }.forEach { objective ->
                if (objective.requiredCount <= GameNumber.ZERO) {
                    issues += error(
                        code = "content.quest.invalid_required_count",
                        path = "quests[${quest.id}].objectives[${objective.id}]",
                        message = "Quest objective requiredCount must be > 0"
                    )
                }

                when (objective) {
                    is QuestObjectiveDefinition.KillEnemy ->
                        objective.enemyDefinitionId?.let { enemyId ->
                            if (enemyId !in enemyIds) {
                                issues += error(
                                    code = "content.quest.unknown_enemy",
                                    path = "quests[${quest.id}].objectives[${objective.id}]",
                                    message = "Quest ${quest.id} references unknown enemy $enemyId"
                                )
                            }
                        }

                    is QuestObjectiveDefinition.ClearEncounter ->
                        objective.encounterDefinitionId?.let { encounterId ->
                            if (encounterId !in encounterIds) {
                                issues += error(
                                    code = "content.quest.unknown_encounter",
                                    path = "quests[${quest.id}].objectives[${objective.id}]",
                                    message =
                                        "Quest ${quest.id} references unknown encounter $encounterId"
                                )
                            }
                        }

                    is QuestObjectiveDefinition.TriggerConvergence ->
                        objective.convergenceId?.let { convergenceId ->
                            if (convergenceId !in convergenceIds) {
                                issues += error(
                                    code = "content.quest.unknown_convergence",
                                    path = "quests[${quest.id}].objectives[${objective.id}]",
                                    message =
                                        "Quest ${quest.id} references unknown convergence $convergenceId"
                                )
                            }
                        }

                    is QuestObjectiveDefinition.AcquireItem ->
                        objective.itemDefinitionId?.let { itemId ->
                            if (itemId !in itemIds) {
                                issues += error(
                                    code = "content.quest.unknown_item",
                                    path = "quests[${quest.id}].objectives[${objective.id}]",
                                    message = "Quest ${quest.id} references unknown item $itemId"
                                )
                            }
                        }
                }
            }

            quest.reward.lootTableIds.sorted().forEach { lootTableId ->
                if (lootTableId !in lootTableIds) {
                    issues += error(
                        code = "content.quest.unknown_reward_loot_table",
                        path = "quests[${quest.id}].reward.lootTableIds[$lootTableId]",
                        message = "Quest ${quest.id} reward references unknown loot table $lootTableId"
                    )
                }
            }
        }
    }

    private fun validateAchievements(
        content: GameContent,
        issues: MutableList<ValidationIssue>
    ) {
        val enemyIds = content.enemies.map { it.id }.toSet()
        val encounterIds = content.encounters.map { it.id }.toSet()
        val convergenceIds = content.convergences.map { it.id }.toSet()
        val itemIds = content.items.map { it.id }.toSet()
        val lootTableIds = content.lootTables.map { it.id }.toSet()
        val featureIds = content.featureUnlocks.map { it.id }.toSet()

        content.achievements.sortedBy { it.id }.forEach { achievement ->
            achievement.requiredFeatureIds.sorted().forEach { featureId ->
                if (featureId !in featureIds) {
                    issues += error(
                        code = "content.achievement.unknown_required_feature",
                        path =
                            "achievements[${achievement.id}].requiredFeatureIds[$featureId]",
                        message =
                            "Achievement ${achievement.id} references unknown feature $featureId"
                    )
                }
            }

            achievement.objectives.sortedBy { it.id }.forEach { objective ->
                if (objective.requiredCount <= GameNumber.ZERO) {
                    issues += error(
                        code = "content.achievement.invalid_required_count",
                        path =
                            "achievements[${achievement.id}].objectives[${objective.id}]",
                        message = "Achievement objective requiredCount must be > 0"
                    )
                }

                when (objective) {
                    is AchievementObjectiveDefinition.KillEnemy ->
                        objective.enemyDefinitionId?.let { enemyId ->
                            if (enemyId !in enemyIds) {
                                issues += error(
                                    code = "content.achievement.unknown_enemy",
                                    path =
                                        "achievements[${achievement.id}].objectives[${objective.id}]",
                                    message =
                                        "Achievement ${achievement.id} references unknown enemy $enemyId"
                                )
                            }
                        }

                    is AchievementObjectiveDefinition.ClearEncounter ->
                        objective.encounterDefinitionId?.let { encounterId ->
                            if (encounterId !in encounterIds) {
                                issues += error(
                                    code = "content.achievement.unknown_encounter",
                                    path =
                                        "achievements[${achievement.id}].objectives[${objective.id}]",
                                    message =
                                        "Achievement ${achievement.id} references unknown encounter " +
                                            encounterId
                                )
                            }
                        }

                    is AchievementObjectiveDefinition.TriggerConvergence ->
                        objective.convergenceId?.let { convergenceId ->
                            if (convergenceId !in convergenceIds) {
                                issues += error(
                                    code = "content.achievement.unknown_convergence",
                                    path =
                                        "achievements[${achievement.id}].objectives[${objective.id}]",
                                    message =
                                        "Achievement ${achievement.id} references unknown convergence " +
                                            convergenceId
                                )
                            }
                        }

                    is AchievementObjectiveDefinition.AcquireItem ->
                        objective.itemDefinitionId?.let { itemId ->
                            if (itemId !in itemIds) {
                                issues += error(
                                    code = "content.achievement.unknown_item",
                                    path =
                                        "achievements[${achievement.id}].objectives[${objective.id}]",
                                    message =
                                        "Achievement ${achievement.id} references unknown item $itemId"
                                )
                            }
                        }
                }
            }

            achievement.reward.lootTableIds.sorted().forEach { lootTableId ->
                if (lootTableId !in lootTableIds) {
                    issues += error(
                        code = "content.achievement.unknown_reward_loot_table",
                        path =
                            "achievements[${achievement.id}].reward.lootTableIds[$lootTableId]",
                        message =
                            "Achievement ${achievement.id} reward references unknown loot table " +
                                lootTableId
                    )
                }
            }
        }
    }

    private fun validateChronicle(
        content: GameContent,
        issues: MutableList<ValidationIssue>
    ) {
        val defaultId = content.defaultChronicleDefinitionId
        if (defaultId == null) {
            issues += error(
                code = "content.chronicle.missing_default_definition",
                path = "defaultChronicleDefinitionId",
                message = "Foundation 17 content must define a default ChronicleDefinition"
            )
        } else if (content.chronicleDefinitions.none { it.id == defaultId }) {
            issues += error(
                code = "content.chronicle.unknown_default_definition",
                path = "defaultChronicleDefinitionId",
                message = "Default ChronicleDefinition $defaultId does not exist"
            )
        }

        content.chronicleDefinitions.sortedBy { it.id }.forEach { definition ->
            if (definition.resetRuleVersion <= 0) {
                issues += error(
                    code = "content.chronicle.invalid_reset_rule_version",
                    path = "chronicleDefinitions[${definition.id}].resetRuleVersion",
                    message = "Chronicle resetRuleVersion must be positive"
                )
            }
            if (definition.requiredTotalNormalClears <= GameNumber.ZERO) {
                issues += error(
                    code = "content.chronicle.invalid_required_clears",
                    path = "chronicleDefinitions[${definition.id}].requiredTotalNormalClears",
                    message = "Chronicle requiredTotalNormalClears must be > 0"
                )
            }
            if (definition.echoReward <= GameNumber.ZERO) {
                issues += error(
                    code = "content.chronicle.invalid_echo_reward",
                    path = "chronicleDefinitions[${definition.id}].echoReward",
                    message = "Chronicle echoReward must be > 0"
                )
            }
            definition.requiredBossId?.let { bossId ->
                if (content.bosses.none { it.id == bossId }) {
                    issues += error(
                        code = "content.chronicle.unknown_required_boss",
                        path = "chronicleDefinitions[${definition.id}].requiredBossId",
                        message = "Chronicle references unknown boss $bossId"
                    )
                }
            }
        }

        val featuresById = content.featureUnlocks.associateBy { it.id }
        content.echoUnlocks.sortedBy { it.id }.forEach { unlock ->
            if (unlock.requiredLifetimeEcho <= GameNumber.ZERO) {
                issues += error(
                    code = "content.echo_unlock.invalid_threshold",
                    path = "echoUnlocks[${unlock.id}].requiredLifetimeEcho",
                    message = "Echo unlock threshold must be > 0"
                )
            }
            if (unlock.effects.isEmpty()) {
                issues += error(
                    code = "content.echo_unlock.no_effects",
                    path = "echoUnlocks[${unlock.id}].effects",
                    message = "Echo unlock must contain at least one effect"
                )
            }

            validateEchoEffects(
                ownerKind = "Echo unlock",
                ownerPath = "echoUnlocks[${unlock.id}]",
                ownerId = unlock.id.toString(),
                effects = unlock.effects,
                featuresById = featuresById,
                issues = issues
            )
        }

        val offersById = content.echoOffers.associateBy { it.id }
        content.echoOffers.sortedBy { it.id }.forEach { offer ->
            if (offer.cost <= GameNumber.ZERO) {
                issues += error(
                    code = "content.echo_offer.invalid_cost",
                    path = "echoOffers[${offer.id}].cost",
                    message = "Echo offer cost must be > 0"
                )
            }
            if (offer.effects.isEmpty()) {
                issues += error(
                    code = "content.echo_offer.no_effects",
                    path = "echoOffers[${offer.id}].effects",
                    message = "Echo offer must contain at least one effect"
                )
            }
            if (offer.repeatable) {
                issues += error(
                    code = "content.echo_offer.repeatable_unsupported",
                    path = "echoOffers[${offer.id}].repeatable",
                    message = "FBE-03 supports nonrepeatable Echo offers only"
                )
            }
            if (offer.id in offer.requiredOfferIds) {
                issues += error(
                    code = "content.echo_offer.self_prerequisite",
                    path = "echoOffers[${offer.id}].requiredOfferIds",
                    message = "Echo offer ${offer.id} cannot require itself"
                )
            }

            offer.requiredOfferIds.sorted().forEach { requiredId ->
                if (requiredId !in offersById) {
                    issues += error(
                        code = "content.echo_offer.unknown_prerequisite",
                        path = "echoOffers[${offer.id}].requiredOfferIds[$requiredId]",
                        message =
                            "Echo offer ${offer.id} references unknown prerequisite $requiredId"
                    )
                }
            }

            validateEchoEffects(
                ownerKind = "Echo offer",
                ownerPath = "echoOffers[${offer.id}]",
                ownerId = offer.id.toString(),
                effects = offer.effects,
                featuresById = featuresById,
                issues = issues
            )
        }

        validateEchoOfferPrerequisiteCycles(content, issues)
    }

    private fun validateEchoEffects(
        ownerKind: String,
        ownerPath: String,
        ownerId: String,
        effects: List<EchoUnlockEffect>,
        featuresById: Map<com.idlerpg.game.core.id.ContentId, com.idlerpg.game.domain.definition.progression.FeatureUnlockDefinition>,
        issues: MutableList<ValidationIssue>
    ) {
        effects.forEachIndexed { index, effect ->
            when (effect) {
                is EchoUnlockEffect.RevealHiddenContent -> Unit

                is EchoUnlockEffect.UnlockPersistentFeature -> {
                    val feature = featuresById[effect.featureId]
                    if (feature == null) {
                        issues += error(
                            code = "content.echo_effect.unknown_feature",
                            path = "$ownerPath.effects[$index]",
                            message =
                                "$ownerKind $ownerId references unknown feature ${effect.featureId}"
                        )
                    } else if (feature.scope != FeatureUnlockScope.META) {
                        issues += error(
                            code = "content.echo_effect.run_scoped_feature",
                            path = "$ownerPath.effects[$index]",
                            message =
                                "$ownerKind $ownerId can only unlock META-scoped features"
                        )
                    }
                }
            }
        }
    }

    private fun validateEchoOfferPrerequisiteCycles(
        content: GameContent,
        issues: MutableList<ValidationIssue>
    ) {
        val offersById = content.echoOffers.associateBy { it.id }
        val visiting = mutableSetOf<com.idlerpg.game.core.id.ContentId>()
        val visited = mutableSetOf<com.idlerpg.game.core.id.ContentId>()

        fun visit(id: com.idlerpg.game.core.id.ContentId) {
            if (id in visited) return
            if (!visiting.add(id)) {
                issues += error(
                    code = "content.echo_offer.prerequisite_cycle",
                    path = "echoOffers[$id].requiredOfferIds",
                    message = "Echo offer prerequisite graph contains a cycle at $id"
                )
                return
            }

            offersById[id]
                ?.requiredOfferIds
                ?.sorted()
                ?.filter { it in offersById }
                ?.forEach(::visit)

            visiting.remove(id)
            visited += id
        }

        offersById.keys.sorted().forEach(::visit)
    }

    private object RarityRange {
        fun between(
            minimum: com.idlerpg.game.domain.definition.Rarity,
            maximum: com.idlerpg.game.domain.definition.Rarity
        ): List<com.idlerpg.game.domain.definition.Rarity> =
            com.idlerpg.game.domain.definition.Rarity.ordered()
                .filter { it.rank in minimum.rank..maximum.rank }
    }

    private fun validateEffect(
        effect: EffectSpec,
        path: String,
        content: GameContent,
        issues: MutableList<ValidationIssue>
    ) {
        when (effect) {
            is EffectSpec.DealDamage -> {
                val hasPositiveRatio = effect.powerRatio.units > 0L
                val hasFlatBonus = effect.flatBonus > GameNumber.ZERO
                if (!hasPositiveRatio && !hasFlatBonus) {
                    issues += error(
                        code = "content.effect.zero_damage",
                        path = path,
                        message = "DealDamage must have a positive powerRatio or flatBonus"
                    )
                }
                effect.conditions.forEachIndexed { index, condition ->
                    if (condition is EffectSpec.DamageCondition.TargetHasStatus &&
                        content.statuses.none { it.id == condition.statusDefinitionId }
                    ) {
                        issues += error(
                            code = "content.effect.condition_unknown_status",
                            path = "$path.conditions[$index]",
                            message = "Damage condition references unknown status ${condition.statusDefinitionId}"
                        )
                    }
                }
            }

            is EffectSpec.Heal -> {
                if (effect.flatAmount <= GameNumber.ZERO) {
                    issues += error(
                        code = "content.effect.zero_heal",
                        path = path,
                        message = "Heal.flatAmount must be > 0"
                    )
                }
            }

            is EffectSpec.ApplyStatus -> {
                if (content.statuses.none { it.id == effect.statusDefinitionId }) {
                    issues += error(
                        code = "content.effect.unknown_status",
                        path = path,
                        message = "ApplyStatus references unknown status ${effect.statusDefinitionId}"
                    )
                }
            }

            is EffectSpec.RemoveStatus -> {
                if (content.statuses.none { it.id == effect.statusDefinitionId }) {
                    issues += error(
                        code = "content.effect.unknown_status",
                        path = path,
                        message = "RemoveStatus references unknown status ${effect.statusDefinitionId}"
                    )
                }
            }

            is EffectSpec.ShiftResonance -> Unit
        }
    }

    private fun error(
        code: String,
        path: String,
        message: String
    ): ValidationIssue = ValidationIssue(
        code = code,
        severity = ValidationSeverity.ERROR,
        path = path,
        message = message
    )
}
