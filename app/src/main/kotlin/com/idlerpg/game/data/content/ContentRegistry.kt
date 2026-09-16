package com.idlerpg.game.data.content

import com.idlerpg.game.core.id.ContentId
import com.idlerpg.game.domain.definition.adaptation.AdaptationRewardDefinition
import com.idlerpg.game.domain.definition.adaptation.AdaptationThresholdDefinition
import com.idlerpg.game.domain.definition.adaptation.MutationDefinition
import com.idlerpg.game.domain.definition.achievement.AchievementDefinition
import com.idlerpg.game.domain.definition.chronicle.ChronicleDefinition
import com.idlerpg.game.domain.definition.chronicle.EchoOfferDefinition
import com.idlerpg.game.domain.definition.chronicle.EchoUnlockDefinition
import com.idlerpg.game.domain.definition.combat.BasicAttackDefinition
import com.idlerpg.game.domain.definition.combat.SkillDefinition
import com.idlerpg.game.domain.definition.combat.SkillEvolutionDefinition
import com.idlerpg.game.domain.definition.combat.StatusEffectDefinition
import com.idlerpg.game.domain.definition.economy.CostFormulaDefinition
import com.idlerpg.game.domain.definition.economy.UpgradeDefinition
import com.idlerpg.game.domain.definition.enemy.EnemyAttackDefinition
import com.idlerpg.game.domain.definition.enemy.EnemyDefinition
import com.idlerpg.game.domain.definition.item.AffixDefinition
import com.idlerpg.game.domain.definition.item.EquipmentDefinition
import com.idlerpg.game.domain.definition.item.ItemDefinition
import com.idlerpg.game.domain.definition.item.LootTableDefinition
import com.idlerpg.game.domain.definition.progression.FeatureUnlockDefinition
import com.idlerpg.game.domain.definition.progression.LevelCurveDefinition
import com.idlerpg.game.domain.definition.progression.MasteryDefinition
import com.idlerpg.game.domain.definition.quest.QuestDefinition
import com.idlerpg.game.domain.definition.resonance.ConvergenceDefinition
import com.idlerpg.game.domain.definition.world.BossDefinition
import com.idlerpg.game.domain.definition.world.EncounterDefinition
import com.idlerpg.game.domain.definition.world.RegionDefinition
import com.idlerpg.game.domain.definition.world.WorldDefinition

/**
 * Validated stable-ID lookup facade for authored content through Foundation 17.
 *
 * Iteration methods always return explicit ContentId ordering. Gameplay must not rely on
 * HashMap iteration order.
 */
class ContentRegistry(
    content: GameContent
) {
    val basicAttack: BasicAttackDefinition = content.basicAttack
    val world: WorldDefinition = content.world

    private val skillsById: Map<ContentId, SkillDefinition> =
        content.skills.sortedBy { it.id }.associateBy { it.id }

    private val skillEvolutionsById: Map<ContentId, SkillEvolutionDefinition> =
        content.skillEvolutions.sortedBy { it.id }.associateBy { it.id }

    private val statusesById: Map<ContentId, StatusEffectDefinition> =
        content.statuses.sortedBy { it.id }.associateBy { it.id }

    private val enemyAttacksById: Map<ContentId, EnemyAttackDefinition> =
        content.enemyAttacks.sortedBy { it.id }.associateBy { it.id }

    private val enemiesById: Map<ContentId, EnemyDefinition> =
        content.enemies.sortedBy { it.id }.associateBy { it.id }

    private val costFormulasById: Map<ContentId, CostFormulaDefinition> =
        content.costFormulas.sortedBy { it.id }.associateBy { it.id }

    private val upgradesById: Map<ContentId, UpgradeDefinition> =
        content.upgrades.sortedBy { it.id }.associateBy { it.id }

    private val regionsById: Map<ContentId, RegionDefinition> =
        content.regions.sortedBy { it.id }.associateBy { it.id }

    private val encountersById: Map<ContentId, EncounterDefinition> =
        content.encounters.sortedBy { it.id }.associateBy { it.id }

    private val bossesById: Map<ContentId, BossDefinition> =
        content.bosses.sortedBy { it.id }.associateBy { it.id }

    private val convergencesById: Map<ContentId, ConvergenceDefinition> =
        content.convergences.sortedBy { it.id }.associateBy { it.id }

    private val adaptationThresholdsById: Map<ContentId, AdaptationThresholdDefinition> =
        content.adaptationThresholds.sortedBy { it.id }.associateBy { it.id }

    private val mutationsById: Map<ContentId, MutationDefinition> =
        content.mutations.sortedBy { it.id }.associateBy { it.id }

    private val adaptationRewardsById: Map<ContentId, AdaptationRewardDefinition> =
        content.adaptationRewards.sortedBy { it.id }.associateBy { it.id }

    private val itemsById: Map<ContentId, ItemDefinition> =
        content.items.sortedBy { it.id }.associateBy { it.id }

    private val equipmentById: Map<ContentId, EquipmentDefinition> =
        content.equipmentDefinitions.sortedBy { it.id }.associateBy { it.id }

    private val affixesById: Map<ContentId, AffixDefinition> =
        content.affixes.sortedBy { it.id }.associateBy { it.id }

    private val lootTablesById: Map<ContentId, LootTableDefinition> =
        content.lootTables.sortedBy { it.id }.associateBy { it.id }

    private val levelCurvesById: Map<ContentId, LevelCurveDefinition> =
        content.levelCurves.sortedBy { it.id }.associateBy { it.id }

    private val featureUnlocksById: Map<ContentId, FeatureUnlockDefinition> =
        content.featureUnlocks.sortedBy { it.id }.associateBy { it.id }

    private val masteriesById: Map<ContentId, MasteryDefinition> =
        content.masteries.sortedBy { it.id }.associateBy { it.id }

    private val questsById: Map<ContentId, QuestDefinition> =
        content.quests.sortedBy { it.id }.associateBy { it.id }

    private val achievementsById: Map<ContentId, AchievementDefinition> =
        content.achievements.sortedBy { it.id }.associateBy { it.id }

    private val chronicleDefinitionsById: Map<ContentId, ChronicleDefinition> =
        content.chronicleDefinitions.sortedBy { it.id }.associateBy { it.id }

    private val echoUnlocksById: Map<ContentId, EchoUnlockDefinition> =
        content.echoUnlocks.sortedBy { it.id }.associateBy { it.id }

    private val echoOffersById: Map<ContentId, EchoOfferDefinition> =
        content.echoOffers.sortedBy { it.id }.associateBy { it.id }

    private val masteriesByAffinityId: Map<ContentId, MasteryDefinition> =
        content.masteries.sortedBy { it.affinity.id }.associateBy { it.affinity.id }

    private val defaultPlayerLevelCurveId: ContentId? = content.defaultPlayerLevelCurveId
    private val defaultChronicleDefinitionId: ContentId? = content.defaultChronicleDefinitionId

    init {
        val validation = ContentValidator.validate(content)
        require(validation.isValid) {
            validation.errors.joinToString(
                prefix = "Invalid GameContent:\n",
                separator = "\n"
            ) { issue ->
                "[${issue.code}] ${issue.path}: ${issue.message}"
            }
        }
    }

    fun skill(id: ContentId): SkillDefinition =
        skillsById[id] ?: error("Unknown SkillDefinition: $id")

    fun skillOrNull(id: ContentId): SkillDefinition? = skillsById[id]

    fun skillEvolution(id: ContentId): SkillEvolutionDefinition =
        skillEvolutionsById[id] ?: error("Unknown SkillEvolutionDefinition: $id")

    fun skillEvolutionOrNull(id: ContentId): SkillEvolutionDefinition? =
        skillEvolutionsById[id]

    fun skillEvolutionsFor(baseSkillId: ContentId): List<SkillEvolutionDefinition> =
        skillEvolutionsById.values.filter { it.baseSkillId == baseSkillId }.sortedBy { it.id }

    fun status(id: ContentId): StatusEffectDefinition =
        statusesById[id] ?: error("Unknown StatusEffectDefinition: $id")

    fun statusOrNull(id: ContentId): StatusEffectDefinition? = statusesById[id]

    fun enemyAttack(id: ContentId): EnemyAttackDefinition =
        enemyAttacksById[id] ?: error("Unknown EnemyAttackDefinition: $id")

    fun enemyAttackOrNull(id: ContentId): EnemyAttackDefinition? = enemyAttacksById[id]

    fun enemy(id: ContentId): EnemyDefinition =
        enemiesById[id] ?: error("Unknown EnemyDefinition: $id")

    fun enemyOrNull(id: ContentId): EnemyDefinition? = enemiesById[id]

    fun costFormula(id: ContentId): CostFormulaDefinition =
        costFormulasById[id] ?: error("Unknown CostFormulaDefinition: $id")

    fun costFormulaOrNull(id: ContentId): CostFormulaDefinition? = costFormulasById[id]

    fun upgrade(id: ContentId): UpgradeDefinition =
        upgradesById[id] ?: error("Unknown UpgradeDefinition: $id")

    fun upgradeOrNull(id: ContentId): UpgradeDefinition? = upgradesById[id]

    fun region(id: ContentId): RegionDefinition =
        regionsById[id] ?: error("Unknown RegionDefinition: $id")

    fun regionOrNull(id: ContentId): RegionDefinition? = regionsById[id]

    fun encounter(id: ContentId): EncounterDefinition =
        encountersById[id] ?: error("Unknown EncounterDefinition: $id")

    fun encounterOrNull(id: ContentId): EncounterDefinition? = encountersById[id]

    fun boss(id: ContentId): BossDefinition =
        bossesById[id] ?: error("Unknown BossDefinition: $id")

    fun bossOrNull(id: ContentId): BossDefinition? = bossesById[id]

    fun convergence(id: ContentId): ConvergenceDefinition =
        convergencesById[id] ?: error("Unknown ConvergenceDefinition: $id")

    fun convergenceOrNull(id: ContentId): ConvergenceDefinition? = convergencesById[id]

    fun adaptationThreshold(id: ContentId): AdaptationThresholdDefinition =
        adaptationThresholdsById[id] ?: error("Unknown AdaptationThresholdDefinition: $id")

    fun adaptationThresholdOrNull(id: ContentId): AdaptationThresholdDefinition? =
        adaptationThresholdsById[id]

    fun mutation(id: ContentId): MutationDefinition =
        mutationsById[id] ?: error("Unknown MutationDefinition: $id")

    fun mutationOrNull(id: ContentId): MutationDefinition? = mutationsById[id]

    fun adaptationReward(id: ContentId): AdaptationRewardDefinition =
        adaptationRewardsById[id] ?: error("Unknown AdaptationRewardDefinition: $id")

    fun adaptationRewardOrNull(id: ContentId): AdaptationRewardDefinition? =
        adaptationRewardsById[id]

    fun item(id: ContentId): ItemDefinition =
        itemsById[id] ?: error("Unknown ItemDefinition: $id")

    fun itemOrNull(id: ContentId): ItemDefinition? = itemsById[id]

    fun equipment(id: ContentId): EquipmentDefinition =
        equipmentById[id] ?: error("Unknown EquipmentDefinition: $id")

    fun equipmentOrNull(id: ContentId): EquipmentDefinition? = equipmentById[id]

    fun affix(id: ContentId): AffixDefinition =
        affixesById[id] ?: error("Unknown AffixDefinition: $id")

    fun affixOrNull(id: ContentId): AffixDefinition? = affixesById[id]

    fun lootTable(id: ContentId): LootTableDefinition =
        lootTablesById[id] ?: error("Unknown LootTableDefinition: $id")

    fun lootTableOrNull(id: ContentId): LootTableDefinition? = lootTablesById[id]

    fun levelCurve(id: ContentId): LevelCurveDefinition =
        levelCurvesById[id] ?: error("Unknown LevelCurveDefinition: $id")

    fun levelCurveOrNull(id: ContentId): LevelCurveDefinition? = levelCurvesById[id]

    fun defaultPlayerLevelCurve(): LevelCurveDefinition {
        val id = defaultPlayerLevelCurveId
            ?: error("GameContent does not define defaultPlayerLevelCurveId")
        return levelCurve(id)
    }

    fun featureUnlock(id: ContentId): FeatureUnlockDefinition =
        featureUnlocksById[id] ?: error("Unknown FeatureUnlockDefinition: $id")

    fun featureUnlockOrNull(id: ContentId): FeatureUnlockDefinition? = featureUnlocksById[id]

    fun mastery(id: ContentId): MasteryDefinition =
        masteriesById[id] ?: error("Unknown MasteryDefinition: $id")

    fun masteryOrNull(id: ContentId): MasteryDefinition? = masteriesById[id]

    fun masteryForAffinityOrNull(affinityId: ContentId): MasteryDefinition? =
        masteriesByAffinityId[affinityId]

    fun quest(id: ContentId): QuestDefinition =
        questsById[id] ?: error("Unknown QuestDefinition: $id")

    fun questOrNull(id: ContentId): QuestDefinition? = questsById[id]

    fun achievement(id: ContentId): AchievementDefinition =
        achievementsById[id] ?: error("Unknown AchievementDefinition: $id")

    fun achievementOrNull(id: ContentId): AchievementDefinition? = achievementsById[id]

    fun chronicleDefinition(id: ContentId): ChronicleDefinition =
        chronicleDefinitionsById[id] ?: error("Unknown ChronicleDefinition: $id")

    fun chronicleDefinitionOrNull(id: ContentId): ChronicleDefinition? =
        chronicleDefinitionsById[id]

    fun defaultChronicleDefinition(): ChronicleDefinition {
        val id = defaultChronicleDefinitionId
            ?: error("GameContent does not define defaultChronicleDefinitionId")
        return chronicleDefinition(id)
    }

    fun echoUnlock(id: ContentId): EchoUnlockDefinition =
        echoUnlocksById[id] ?: error("Unknown EchoUnlockDefinition: $id")

    fun echoUnlockOrNull(id: ContentId): EchoUnlockDefinition? = echoUnlocksById[id]

    fun echoOffer(id: ContentId): EchoOfferDefinition =
        echoOffersById[id] ?: error("Unknown EchoOfferDefinition: $id")

    fun echoOfferOrNull(id: ContentId): EchoOfferDefinition? = echoOffersById[id]

    fun allSkills(): List<SkillDefinition> = skillsById.values.sortedBy { it.id }

    fun allStatuses(): List<StatusEffectDefinition> = statusesById.values.sortedBy { it.id }

    fun allEnemyAttacks(): List<EnemyAttackDefinition> =
        enemyAttacksById.values.sortedBy { it.id }

    fun allEnemies(): List<EnemyDefinition> = enemiesById.values.sortedBy { it.id }

    fun allCostFormulas(): List<CostFormulaDefinition> =
        costFormulasById.values.sortedBy { it.id }

    fun allUpgrades(): List<UpgradeDefinition> = upgradesById.values.sortedBy { it.id }

    fun allRegions(): List<RegionDefinition> = regionsById.values.sortedBy { it.id }

    fun allEncounters(): List<EncounterDefinition> =
        encountersById.values.sortedBy { it.id }

    fun allBosses(): List<BossDefinition> = bossesById.values.sortedBy { it.id }

    fun allConvergences(): List<ConvergenceDefinition> =
        convergencesById.values.sortedBy { it.id }

    fun allSkillEvolutions(): List<SkillEvolutionDefinition> =
        skillEvolutionsById.values.sortedBy { it.id }

    fun allAdaptationThresholds(): List<AdaptationThresholdDefinition> =
        adaptationThresholdsById.values.sortedBy { it.id }

    fun allMutations(): List<MutationDefinition> =
        mutationsById.values.sortedBy { it.id }

    fun allAdaptationRewards(): List<AdaptationRewardDefinition> =
        adaptationRewardsById.values.sortedBy { it.id }

    fun allItems(): List<ItemDefinition> =
        itemsById.values.sortedBy { it.id }

    fun allEquipmentDefinitions(): List<EquipmentDefinition> =
        equipmentById.values.sortedBy { it.id }

    fun allAffixes(): List<AffixDefinition> =
        affixesById.values.sortedBy { it.id }

    fun allLootTables(): List<LootTableDefinition> =
        lootTablesById.values.sortedBy { it.id }

    fun allLevelCurves(): List<LevelCurveDefinition> =
        levelCurvesById.values.sortedBy { it.id }

    fun allFeatureUnlocks(): List<FeatureUnlockDefinition> =
        featureUnlocksById.values.sortedBy { it.id }

    fun allMasteries(): List<MasteryDefinition> =
        masteriesById.values.sortedBy { it.id }

    fun allQuests(): List<QuestDefinition> =
        questsById.values.sortedBy { it.id }

    fun allAchievements(): List<AchievementDefinition> =
        achievementsById.values.sortedBy { it.id }

    fun allChronicleDefinitions(): List<ChronicleDefinition> =
        chronicleDefinitionsById.values.sortedBy { it.id }

    fun allEchoUnlocks(): List<EchoUnlockDefinition> =
        echoUnlocksById.values.sortedBy { it.id }

    fun allEchoOffers(): List<EchoOfferDefinition> =
        echoOffersById.values.sortedBy { it.id }

    fun contains(id: ContentId): Boolean =
        id == basicAttack.id ||
            id == world.id ||
            id in skillsById ||
            id in statusesById ||
            id in skillEvolutionsById ||
            id in enemyAttacksById ||
            id in enemiesById ||
            id in costFormulasById ||
            id in upgradesById ||
            id in regionsById ||
            id in encountersById ||
            id in bossesById ||
            id in convergencesById ||
            id in adaptationThresholdsById ||
            id in mutationsById ||
            id in adaptationRewardsById ||
            id in itemsById ||
            id in equipmentById ||
            id in affixesById ||
            id in lootTablesById ||
            id in levelCurvesById ||
            id in featureUnlocksById ||
            id in masteriesById ||
            id in questsById ||
            id in achievementsById ||
            id in chronicleDefinitionsById ||
            id in echoUnlocksById ||
            id in echoOffersById
}
