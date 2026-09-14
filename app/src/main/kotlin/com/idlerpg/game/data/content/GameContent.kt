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
 * Immutable authored-content aggregate for the strategy-first Training Hollow slice.
 * Canonical categories remain bounded Kotlin data and are validated before simulation.
 */
data class GameContent(
    val basicAttack: BasicAttackDefinition,
    val world: WorldDefinition,
    val skills: List<SkillDefinition> = emptyList(),
    val skillEvolutions: List<SkillEvolutionDefinition> = emptyList(),
    val statuses: List<StatusEffectDefinition> = emptyList(),
    val enemyAttacks: List<EnemyAttackDefinition> = emptyList(),
    val enemies: List<EnemyDefinition> = emptyList(),
    val costFormulas: List<CostFormulaDefinition> = emptyList(),
    val upgrades: List<UpgradeDefinition> = emptyList(),
    val regions: List<RegionDefinition> = emptyList(),
    val encounters: List<EncounterDefinition> = emptyList(),
    val bosses: List<BossDefinition> = emptyList(),
    val convergences: List<ConvergenceDefinition> = emptyList(),
    val adaptationThresholds: List<AdaptationThresholdDefinition> = emptyList(),
    val mutations: List<MutationDefinition> = emptyList(),
    val adaptationRewards: List<AdaptationRewardDefinition> = emptyList(),
    val items: List<ItemDefinition> = emptyList(),
    val equipmentDefinitions: List<EquipmentDefinition> = emptyList(),
    val affixes: List<AffixDefinition> = emptyList(),
    val lootTables: List<LootTableDefinition> = emptyList(),
    val defaultPlayerLevelCurveId: ContentId? = null,
    val levelCurves: List<LevelCurveDefinition> = emptyList(),
    val featureUnlocks: List<FeatureUnlockDefinition> = emptyList(),
    val masteries: List<MasteryDefinition> = emptyList(),
    val quests: List<QuestDefinition> = emptyList(),
    val achievements: List<AchievementDefinition> = emptyList(),
    val defaultChronicleDefinitionId: ContentId? = null,
    val chronicleDefinitions: List<ChronicleDefinition> = emptyList(),
    val echoUnlocks: List<EchoUnlockDefinition> = emptyList(),
    val echoOffers: List<EchoOfferDefinition> = emptyList()
)
