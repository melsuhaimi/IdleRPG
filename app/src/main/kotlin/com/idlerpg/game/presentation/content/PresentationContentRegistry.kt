package com.idlerpg.game.presentation.content

import com.idlerpg.game.core.id.ContentId
import com.idlerpg.game.data.content.ContentRegistry
import com.idlerpg.game.data.content.TrainingHollowWorldContent
import com.idlerpg.game.domain.definition.Affinity
import com.idlerpg.game.domain.definition.Rarity

/** Deterministic coverage result used by the FUI-02 metadata regression. */
data class PresentationContentCoverage(
    val requiredVisibleIds: Set<ContentId>,
    val systemOnlyIds: Set<ContentId>,
    val missingVisibleIds: Set<ContentId>,
    val unclassifiedRegistryIds: Set<ContentId>
) {
    val isComplete: Boolean
        get() = missingVisibleIds.isEmpty() && unclassifiedRegistryIds.isEmpty()
}

/**
 * Presentation metadata registry keyed exclusively by stable backend [ContentId].
 *
 * Current authored content is small enough for a checked-in deterministic registry. When
 * backend content expands, [coverage] deliberately fails until the new user-visible IDs are
 * classified and localized here.
 */
class PresentationContentRegistry private constructor(
    entries: List<PresentationContentEntry>,
    private val extraRequiredVisibleIds: Set<ContentId>,
    private val extraSystemOnlyIds: Set<ContentId>
) {
    private val entriesById: Map<ContentId, PresentationContentEntry> =
        entries.sortedBy { it.contentId }.associateBy { it.contentId }

    init {
        require(entries.size == entriesById.size) {
            "PresentationContentRegistry contains duplicate ContentIds"
        }
    }

    fun entry(contentId: ContentId): PresentationContentEntry =
        entriesById[contentId] ?: error("Missing presentation metadata: $contentId")

    fun entryOrNull(contentId: ContentId): PresentationContentEntry? =
        entriesById[contentId]

    fun allEntries(): List<PresentationContentEntry> =
        entriesById.values.sortedBy { it.contentId }

    fun coverage(contentRegistry: ContentRegistry): PresentationContentCoverage {
        val requiredVisible = buildSet {
            add(contentRegistry.world.id)
            add(contentRegistry.basicAttack.id)
            addAll(contentRegistry.allSkills().map { it.id })
            addAll(contentRegistry.allSkillEvolutions().map { it.id })
            addAll(contentRegistry.allStatuses().map { it.id })
            addAll(contentRegistry.allEnemies().map { it.id })
            addAll(contentRegistry.allUpgrades().map { it.id })
            addAll(contentRegistry.allRegions().map { it.id })
            addAll(contentRegistry.allEncounters().map { it.id })
            addAll(contentRegistry.allBosses().map { it.id })
            addAll(contentRegistry.allConvergences().map { it.id })
            addAll(contentRegistry.allMutations().map { it.id })
            addAll(contentRegistry.allItems().map { it.id })
            addAll(contentRegistry.allAffixes().map { it.id })
            addAll(contentRegistry.allFeatureUnlocks().map { it.id })
            addAll(contentRegistry.allMasteries().map { it.id })
            addAll(contentRegistry.allQuests().map { it.id })
            addAll(contentRegistry.allQuests().flatMap { quest -> quest.objectives.map { it.id } })
            addAll(contentRegistry.allAchievements().map { it.id })
            addAll(contentRegistry.allChronicleDefinitions().map { it.id })
            addAll(contentRegistry.allEchoOffers().map { it.id })
            addAll(Affinity.values().map { it.id })
            addAll(Rarity.values().map { it.id })
            addAll(extraRequiredVisibleIds)
        }

        val systemOnly = buildSet {
            addAll(contentRegistry.allEnemyAttacks().map { it.id })
            addAll(contentRegistry.allCostFormulas().map { it.id })
            addAll(contentRegistry.allAdaptationThresholds().map { it.id })
            addAll(contentRegistry.allAdaptationRewards().map { it.id })
            addAll(contentRegistry.allEquipmentDefinitions().map { it.id })
            addAll(contentRegistry.allLootTables().map { it.id })
            addAll(contentRegistry.allLevelCurves().map { it.id })
            addAll(contentRegistry.allEchoUnlocks().map { it.id })
            addAll(extraSystemOnlyIds)
        }

        val registryKnown = buildSet {
            add(contentRegistry.world.id)
            add(contentRegistry.basicAttack.id)
            addAll(contentRegistry.allSkills().map { it.id })
            addAll(contentRegistry.allSkillEvolutions().map { it.id })
            addAll(contentRegistry.allStatuses().map { it.id })
            addAll(contentRegistry.allEnemies().map { it.id })
            addAll(contentRegistry.allEnemyAttacks().map { it.id })
            addAll(contentRegistry.allCostFormulas().map { it.id })
            addAll(contentRegistry.allUpgrades().map { it.id })
            addAll(contentRegistry.allRegions().map { it.id })
            addAll(contentRegistry.allEncounters().map { it.id })
            addAll(contentRegistry.allBosses().map { it.id })
            addAll(contentRegistry.allConvergences().map { it.id })
            addAll(contentRegistry.allAdaptationThresholds().map { it.id })
            addAll(contentRegistry.allMutations().map { it.id })
            addAll(contentRegistry.allAdaptationRewards().map { it.id })
            addAll(contentRegistry.allItems().map { it.id })
            addAll(contentRegistry.allEquipmentDefinitions().map { it.id })
            addAll(contentRegistry.allAffixes().map { it.id })
            addAll(contentRegistry.allLootTables().map { it.id })
            addAll(contentRegistry.allLevelCurves().map { it.id })
            addAll(contentRegistry.allFeatureUnlocks().map { it.id })
            addAll(contentRegistry.allMasteries().map { it.id })
            addAll(contentRegistry.allQuests().map { it.id })
            addAll(contentRegistry.allQuests().flatMap { quest -> quest.objectives.map { it.id } })
            addAll(contentRegistry.allAchievements().map { it.id })
            addAll(contentRegistry.allChronicleDefinitions().map { it.id })
            addAll(contentRegistry.allEchoUnlocks().map { it.id })
            addAll(contentRegistry.allEchoOffers().map { it.id })
        }

        return PresentationContentCoverage(
            requiredVisibleIds = requiredVisible.toSortedSet(),
            systemOnlyIds = systemOnly.toSortedSet(),
            missingVisibleIds = (requiredVisible - entriesById.keys).toSortedSet(),
            unclassifiedRegistryIds =
                (registryKnown - requiredVisible - systemOnly).toSortedSet()
        )
    }

    companion object {
        private val FIRST_HUNT_OBJECTIVE =
            ContentId("objective.quest.training.first_hunt.slimes")
        private val FORGED_FLAME_ACHIEVEMENT_OBJECTIVE =
            ContentId("objective.achievement.convergence.first_forged_flame")
        private val FIRST_COLLAPSE_MILESTONE =
            ContentId("milestone.chronicle.first_collapse")
        private val ADAPTATION_FORECAST_DISCOVERY =
            ContentId("discovery.adaptation_forecast")
        private val SLIME_ADAPTATION_TAG =
            ContentId("tag.enemy.slime")

        fun default(): PresentationContentRegistry {
            val entries = mutableListOf<PresentationContentEntry>()

            fun add(
                id: String,
                kind: PresentationContentKind,
                title: PresentationStringKey,
                description: PresentationStringKey,
                icon: PresentationAssetKey,
                illustration: PresentationAssetKey? = null
            ) {
                entries += PresentationContentEntry(
                    contentId = ContentId(id),
                    kind = kind,
                    titleStringKey = title,
                    shortDescriptionStringKey = description,
                    iconAssetKey = icon,
                    illustrationAssetKey = illustration
                )
            }

            add("world.default", PresentationContentKind.WORLD,
                PresentationStringKey.WORLD, PresentationStringKey.DESC_WORLD,
                PresentationAssetKey.WORLD)
            add("region.training_hollow", PresentationContentKind.REGION,
                PresentationStringKey.TRAINING_HOLLOW, PresentationStringKey.DESC_REGION,
                PresentationAssetKey.TRAINING_HOLLOW,
                PresentationAssetKey.TRAINING_HOLLOW_ILLUSTRATION)
            add("encounter.training_hollow.slime", PresentationContentKind.ENCOUNTER,
                PresentationStringKey.SLIME_ENCOUNTER, PresentationStringKey.DESC_ENCOUNTER,
                PresentationAssetKey.SLIME_ENCOUNTER)
            add("encounter.training_hollow.riftfang", PresentationContentKind.ENCOUNTER,
                PresentationStringKey.RIFTFANG_ENCOUNTER, PresentationStringKey.DESC_ENCOUNTER,
                PresentationAssetKey.RIFTFANG_ENCOUNTER)
            add("encounter.training_hollow.cinder_wisp", PresentationContentKind.ENCOUNTER,
                PresentationStringKey.CINDER_WISP_ENCOUNTER, PresentationStringKey.DESC_ENCOUNTER,
                PresentationAssetKey.CINDER_WISP_ENCOUNTER)
            add("encounter.training_hollow.hollow_bulwark", PresentationContentKind.ENCOUNTER,
                PresentationStringKey.HOLLOW_BULWARK_ENCOUNTER, PresentationStringKey.DESC_ENCOUNTER,
                PresentationAssetKey.HOLLOW_BULWARK_ENCOUNTER)
            add("encounter.training_hollow.arcane_seer", PresentationContentKind.ENCOUNTER,
                PresentationStringKey.ARCANE_SEER_ENCOUNTER, PresentationStringKey.DESC_ENCOUNTER,
                PresentationAssetKey.ARCANE_SEER_ENCOUNTER)
            add("encounter.training_hollow.frostbound_mite", PresentationContentKind.ENCOUNTER,
                PresentationStringKey.FROSTBOUND_MITE, PresentationStringKey.DESC_ENCOUNTER,
                PresentationAssetKey.FROSTBOUND_MITE, PresentationAssetKey.FROSTBOUND_MITE_ILLUSTRATION)
            add("encounter.training_hollow.echo_leech", PresentationContentKind.ENCOUNTER,
                PresentationStringKey.ECHO_LEECH, PresentationStringKey.DESC_ENCOUNTER,
                PresentationAssetKey.ECHO_LEECH, PresentationAssetKey.ECHO_LEECH_ILLUSTRATION)
            add("encounter.training_hollow.shade_mimic", PresentationContentKind.ENCOUNTER,
                PresentationStringKey.SHADE_MIMIC, PresentationStringKey.DESC_ENCOUNTER,
                PresentationAssetKey.SHADE_MIMIC, PresentationAssetKey.SHADE_MIMIC_ILLUSTRATION)
            TrainingHollowWorldContent.encounters.sortedBy { it.id }.forEach { encounter ->
                entries += PresentationContentEntry(
                    contentId = encounter.id,
                    kind = PresentationContentKind.ENCOUNTER,
                    titleStringKey = PresentationStringKey.TRAINING_HOLLOW,
                    shortDescriptionStringKey = PresentationStringKey.DESC_ENCOUNTER,
                    iconAssetKey = PresentationAssetKey.TRAINING_HOLLOW
                )
            }

            add("skill.basic_attack", PresentationContentKind.SKILL,
                PresentationStringKey.BASIC_ATTACK, PresentationStringKey.DESC_SKILL,
                PresentationAssetKey.BASIC_ATTACK)
            add("skill.heavy_strike", PresentationContentKind.SKILL,
                PresentationStringKey.HEAVY_STRIKE, PresentationStringKey.DESC_HEAVY_STRIKE,
                PresentationAssetKey.HEAVY_STRIKE)
            add("skill_evolution.heavy_strike.earthbreaker", PresentationContentKind.SKILL,
                PresentationStringKey.EARTHBREAKER, PresentationStringKey.DESC_EARTHBREAKER,
                PresentationAssetKey.HEAVY_STRIKE)
            add("skill_evolution.heavy_strike.executioner", PresentationContentKind.SKILL,
                PresentationStringKey.EXECUTIONER, PresentationStringKey.DESC_EXECUTIONER,
                PresentationAssetKey.HEAVY_STRIKE)
            add("skill_evolution.flame_brand.wildspark", PresentationContentKind.SKILL,
                PresentationStringKey.WILDSPARK, PresentationStringKey.DESC_WILDSPARK,
                PresentationAssetKey.FLAME_BRAND)
            add("skill_evolution.flame_brand.searing_brand", PresentationContentKind.SKILL,
                PresentationStringKey.SEARING_BRAND, PresentationStringKey.DESC_SEARING_BRAND,
                PresentationAssetKey.FLAME_BRAND)
            add("skill_evolution.frost_lance.permafrost", PresentationContentKind.SKILL,
                PresentationStringKey.PERMAFROST, PresentationStringKey.DESC_PERMAFROST,
                PresentationAssetKey.FROST_LANCE)
            add("skill_evolution.frost_lance.shatter_spear", PresentationContentKind.SKILL,
                PresentationStringKey.SHATTER_SPEAR, PresentationStringKey.DESC_SHATTER_SPEAR,
                PresentationAssetKey.FROST_LANCE)
            add("skill_evolution.umbral_cut.reapers_arc", PresentationContentKind.SKILL,
                PresentationStringKey.REAPERS_ARC, PresentationStringKey.DESC_REAPERS_ARC,
                PresentationAssetKey.UMBRAL_CUT)
            add("skill_evolution.umbral_cut.sanguine_edge", PresentationContentKind.SKILL,
                PresentationStringKey.SANGUINE_EDGE, PresentationStringKey.DESC_SANGUINE_EDGE,
                PresentationAssetKey.UMBRAL_CUT)
            add("skill.quick_slash", PresentationContentKind.SKILL,
                PresentationStringKey.QUICK_SLASH, PresentationStringKey.DESC_QUICK_SLASH,
                PresentationAssetKey.QUICK_SLASH)
            add("skill.flame_brand", PresentationContentKind.SKILL,
                PresentationStringKey.FLAME_BRAND, PresentationStringKey.DESC_FLAME_BRAND,
                PresentationAssetKey.FLAME_BRAND)
            add("skill.cinder_mark", PresentationContentKind.SKILL,
                PresentationStringKey.CINDER_MARK, PresentationStringKey.DESC_CINDER_MARK,
                PresentationAssetKey.CINDER_MARK)
            add("skill.guard_mend", PresentationContentKind.SKILL,
                PresentationStringKey.GUARD_MEND, PresentationStringKey.DESC_GUARD_MEND,
                PresentationAssetKey.GUARD_MEND)
            add("skill.frost_lance", PresentationContentKind.SKILL,
                PresentationStringKey.FROST_LANCE, PresentationStringKey.DESC_FROST_LANCE,
                PresentationAssetKey.FROST_LANCE)
            add("skill.arcane_pulse", PresentationContentKind.SKILL,
                PresentationStringKey.ARCANE_PULSE, PresentationStringKey.DESC_ARCANE_PULSE,
                PresentationAssetKey.ARCANE_PULSE)
            add("skill.vital_surge", PresentationContentKind.SKILL,
                PresentationStringKey.VITAL_SURGE, PresentationStringKey.DESC_VITAL_SURGE,
                PresentationAssetKey.VITAL_SURGE)
            add("skill.umbral_cut", PresentationContentKind.SKILL,
                PresentationStringKey.UMBRAL_CUT, PresentationStringKey.DESC_UMBRAL_CUT,
                PresentationAssetKey.UMBRAL_CUT)
            add("skill.glacial_ward", PresentationContentKind.SKILL,
                PresentationStringKey.GLACIAL_WARD, PresentationStringKey.DESC_GLACIAL_WARD,
                PresentationAssetKey.GLACIAL_WARD)
            add("skill.resonance_shift", PresentationContentKind.SKILL,
                PresentationStringKey.RESONANCE_SHIFT, PresentationStringKey.DESC_RESONANCE_SHIFT,
                PresentationAssetKey.RESONANCE_SHIFT)
            add("skill.blood_eclipse", PresentationContentKind.SKILL,
                PresentationStringKey.BLOOD_ECLIPSE_SKILL,
                PresentationStringKey.DESC_BLOOD_ECLIPSE_SKILL,
                PresentationAssetKey.BLOOD_ECLIPSE_SKILL)

            add("status.burning", PresentationContentKind.STATUS,
                PresentationStringKey.BURNING, PresentationStringKey.DESC_STATUS,
                PresentationAssetKey.BURNING)
            add("status.guard_focus", PresentationContentKind.STATUS,
                PresentationStringKey.GUARD_FOCUS, PresentationStringKey.DESC_STATUS,
                PresentationAssetKey.GUARD_FOCUS)
            add("status.stagger", PresentationContentKind.STATUS,
                PresentationStringKey.STAGGER, PresentationStringKey.DESC_STATUS,
                PresentationAssetKey.STAGGER)
            add("status.deep_stagger", PresentationContentKind.STATUS,
                PresentationStringKey.DEEP_STAGGER, PresentationStringKey.DESC_STATUS,
                PresentationAssetKey.STAGGER)
            add("status.chill", PresentationContentKind.STATUS,
                PresentationStringKey.CHILL, PresentationStringKey.DESC_STATUS,
                PresentationAssetKey.CHILL)
            add("status.vital_regeneration", PresentationContentKind.STATUS,
                PresentationStringKey.VITAL_REGENERATION, PresentationStringKey.DESC_STATUS,
                PresentationAssetKey.VITAL_REGENERATION)
            add("status.glacial_ward", PresentationContentKind.STATUS,
                PresentationStringKey.GLACIAL_WARD, PresentationStringKey.DESC_STATUS,
                PresentationAssetKey.GLACIAL_WARD)
            add("status.scorched", PresentationContentKind.STATUS,
                PresentationStringKey.SCORCHED, PresentationStringKey.DESC_STATUS_SCORCHED,
                PresentationAssetKey.BURNING)
            add("convergence.forged_flame", PresentationContentKind.CONVERGENCE,
                PresentationStringKey.FORGED_FLAME, PresentationStringKey.DESC_CONVERGENCE,
                PresentationAssetKey.FORGED_FLAME)
            add("convergence.flashfire", PresentationContentKind.CONVERGENCE,
                PresentationStringKey.FLASHFIRE, PresentationStringKey.DESC_CONVERGENCE,
                PresentationAssetKey.FLASHFIRE)
            add("convergence.bastion_pulse", PresentationContentKind.CONVERGENCE,
                PresentationStringKey.BASTION_PULSE, PresentationStringKey.DESC_CONVERGENCE,
                PresentationAssetKey.BASTION_PULSE)
            add("convergence.shatterfield", PresentationContentKind.CONVERGENCE,
                PresentationStringKey.SHATTERFIELD, PresentationStringKey.DESC_CONVERGENCE,
                PresentationAssetKey.SHATTERFIELD)
            add("convergence.blood_eclipse", PresentationContentKind.CONVERGENCE,
                PresentationStringKey.BLOOD_ECLIPSE, PresentationStringKey.DESC_CONVERGENCE,
                PresentationAssetKey.BLOOD_ECLIPSE)
            add("convergence.overdrive", PresentationContentKind.CONVERGENCE,
                PresentationStringKey.OVERDRIVE, PresentationStringKey.DESC_CONVERGENCE,
                PresentationAssetKey.OVERDRIVE)
            add("convergence.winter_bastion", PresentationContentKind.CONVERGENCE,
                PresentationStringKey.WINTER_BASTION, PresentationStringKey.DESC_CONVERGENCE,
                PresentationAssetKey.WINTER_BASTION)
            add("convergence.arcane_refrain", PresentationContentKind.CONVERGENCE,
                PresentationStringKey.ARCANE_REFRAIN, PresentationStringKey.DESC_CONVERGENCE,
                PresentationAssetKey.ARCANE_REFRAIN)
            add("enemy.slime", PresentationContentKind.ENEMY,
                PresentationStringKey.SLIME, PresentationStringKey.DESC_ENEMY,
                PresentationAssetKey.SLIME, PresentationAssetKey.SLIME_ILLUSTRATION)
            add("enemy.riftfang", PresentationContentKind.ENEMY,
                PresentationStringKey.RIFTFANG, PresentationStringKey.DESC_ENEMY,
                PresentationAssetKey.RIFTFANG, PresentationAssetKey.RIFTFANG_ILLUSTRATION)
            add("enemy.cinder_wisp", PresentationContentKind.ENEMY,
                PresentationStringKey.CINDER_WISP, PresentationStringKey.DESC_ENEMY,
                PresentationAssetKey.CINDER_WISP, PresentationAssetKey.CINDER_WISP_ILLUSTRATION)
            add("enemy.hollow_bulwark", PresentationContentKind.ENEMY,
                PresentationStringKey.HOLLOW_BULWARK, PresentationStringKey.DESC_ENEMY,
                PresentationAssetKey.HOLLOW_BULWARK, PresentationAssetKey.HOLLOW_BULWARK_ILLUSTRATION)
            add("enemy.arcane_seer", PresentationContentKind.ENEMY,
                PresentationStringKey.ARCANE_SEER, PresentationStringKey.DESC_ENEMY,
                PresentationAssetKey.ARCANE_SEER, PresentationAssetKey.ARCANE_SEER_ILLUSTRATION)
            add("enemy.frostbound_mite", PresentationContentKind.ENEMY,
                PresentationStringKey.FROSTBOUND_MITE, PresentationStringKey.DESC_ENEMY,
                PresentationAssetKey.FROSTBOUND_MITE, PresentationAssetKey.FROSTBOUND_MITE_ILLUSTRATION)
            add("enemy.echo_leech", PresentationContentKind.ENEMY,
                PresentationStringKey.ECHO_LEECH, PresentationStringKey.DESC_ENEMY,
                PresentationAssetKey.ECHO_LEECH, PresentationAssetKey.ECHO_LEECH_ILLUSTRATION)
            add("enemy.shade_mimic", PresentationContentKind.ENEMY,
                PresentationStringKey.SHADE_MIMIC, PresentationStringKey.DESC_ENEMY,
                PresentationAssetKey.SHADE_MIMIC, PresentationAssetKey.SHADE_MIMIC_ILLUSTRATION)
            add("upgrade.basic_attack_power", PresentationContentKind.UPGRADE,
                PresentationStringKey.BASIC_ATTACK_POWER, PresentationStringKey.DESC_UPGRADE,
                PresentationAssetKey.BASIC_ATTACK_POWER)
            add("upgrade.endurance", PresentationContentKind.UPGRADE,
                PresentationStringKey.ENDURANCE, PresentationStringKey.DESC_UPGRADE,
                PresentationAssetKey.ENDURANCE)
            add("upgrade.armor_training", PresentationContentKind.UPGRADE,
                PresentationStringKey.ARMOR_TRAINING, PresentationStringKey.DESC_UPGRADE,
                PresentationAssetKey.ARMOR_TRAINING)
            add("upgrade.tempo_training", PresentationContentKind.UPGRADE,
                PresentationStringKey.TEMPO_TRAINING, PresentationStringKey.DESC_UPGRADE,
                PresentationAssetKey.TEMPO_TRAINING)
            add("upgrade.precision", PresentationContentKind.UPGRADE,
                PresentationStringKey.PRECISION, PresentationStringKey.DESC_UPGRADE,
                PresentationAssetKey.PRECISION)
            add("upgrade.lethality", PresentationContentKind.UPGRADE,
                PresentationStringKey.LETHALITY, PresentationStringKey.DESC_UPGRADE,
                PresentationAssetKey.LETHALITY)
            add("upgrade.channeling", PresentationContentKind.UPGRADE,
                PresentationStringKey.CHANNELING, PresentationStringKey.DESC_UPGRADE,
                PresentationAssetKey.CHANNELING)
            add("upgrade.restoration", PresentationContentKind.UPGRADE,
                PresentationStringKey.RESTORATION, PresentationStringKey.DESC_UPGRADE,
                PresentationAssetKey.RESTORATION)
            add("mutation.ash_skin", PresentationContentKind.MUTATION,
                PresentationStringKey.ASH_SKIN, PresentationStringKey.DESC_MUTATION,
                PresentationAssetKey.ASH_SKIN)
            listOf(
                "mutation.stonehide" to PresentationStringKey.STONEHIDE,
                "mutation.reflex_carapace" to PresentationStringKey.REFLEX_CARAPACE,
                "mutation.icebound_core" to PresentationStringKey.ICEBOUND_CORE,
                "mutation.null_veil" to PresentationStringKey.NULL_VEIL,
                "mutation.blightblood" to PresentationStringKey.BLIGHTBLOOD,
                "mutation.gloom_ward" to PresentationStringKey.GLOOM_WARD,
                "mutation.siege_hunger" to PresentationStringKey.SIEGE_HUNGER
            ).forEach { (id, title) -> add(id, PresentationContentKind.MUTATION, title, PresentationStringKey.DESC_MUTATION, PresentationAssetKey.ASH_SKIN) }
            add("enemy.hollow_warden", PresentationContentKind.ENEMY, PresentationStringKey.HOLLOW_WARDEN, PresentationStringKey.DESC_ENEMY, PresentationAssetKey.ARCANE_SEER, PresentationAssetKey.HOLLOW_WARDEN_ILLUSTRATION)
            add("boss.hollow_warden", PresentationContentKind.ENEMY, PresentationStringKey.HOLLOW_WARDEN, PresentationStringKey.DESC_ENEMY, PresentationAssetKey.ARCANE_SEER, PresentationAssetKey.HOLLOW_WARDEN_ILLUSTRATION)
            add("boss.training_hollow.stage_45", PresentationContentKind.ENEMY, PresentationStringKey.HOLLOW_WARDEN, PresentationStringKey.DESC_ENEMY, PresentationAssetKey.ARCANE_SEER, PresentationAssetKey.HOLLOW_WARDEN_ILLUSTRATION)
            add("boss.training_hollow.stage_60", PresentationContentKind.ENEMY, PresentationStringKey.HOLLOW_WARDEN, PresentationStringKey.DESC_ENEMY, PresentationAssetKey.ARCANE_SEER, PresentationAssetKey.HOLLOW_WARDEN_ILLUSTRATION)
            add("boss.training_hollow.stage_72", PresentationContentKind.ENEMY, PresentationStringKey.HOLLOW_WARDEN, PresentationStringKey.DESC_ENEMY, PresentationAssetKey.ARCANE_SEER, PresentationAssetKey.HOLLOW_WARDEN_ILLUSTRATION)
            add("item.training_blade", PresentationContentKind.ITEM,
                PresentationStringKey.TRAINING_BLADE, PresentationStringKey.DESC_ITEM,
                PresentationAssetKey.TRAINING_BLADE)
            add("item.training_catalyst", PresentationContentKind.ITEM,
                PresentationStringKey.TRAINING_CATALYST, PresentationStringKey.DESC_ITEM,
                PresentationAssetKey.TRAINING_CATALYST)
            add("item.fracture_mail", PresentationContentKind.ITEM,
                PresentationStringKey.FRACTURE_MAIL, PresentationStringKey.DESC_ITEM,
                PresentationAssetKey.FRACTURE_MAIL)
            add("item.seer_helm", PresentationContentKind.ITEM,
                PresentationStringKey.SEERS_HELM, PresentationStringKey.DESC_ITEM,
                PresentationAssetKey.SEERS_HELM)
            add("item.rift_boots", PresentationContentKind.ITEM,
                PresentationStringKey.RIFTSTEP_BOOTS, PresentationStringKey.DESC_ITEM,
                PresentationAssetKey.RIFTSTEP_BOOTS)
            add("item.echo_sigil", PresentationContentKind.ITEM,
                PresentationStringKey.ECHO_SIGIL, PresentationStringKey.DESC_ITEM,
                PresentationAssetKey.ECHO_SIGIL)
            add("affix.keen", PresentationContentKind.AFFIX,
                PresentationStringKey.KEEN, PresentationStringKey.DESC_AFFIX,
                PresentationAssetKey.KEEN)
            listOf(
                Triple("affix.brutal", PresentationStringKey.BRUTAL, PresentationAssetKey.MIGHT),
                Triple("affix.relentless", PresentationStringKey.RELENTLESS, PresentationAssetKey.TEMPO),
                Triple("affix.hollow_tuned", PresentationStringKey.HOLLOW_TUNED, PresentationAssetKey.ARCANE),
                Triple("affix.executioner", PresentationStringKey.EXECUTIONER_AFFIX, PresentationAssetKey.SHADOW),
                Triple("affix.reinforced", PresentationStringKey.REINFORCED, PresentationAssetKey.GUARD),
                Triple("affix.warded", PresentationStringKey.WARDED, PresentationAssetKey.FROST),
                Triple("affix.steadfast", PresentationStringKey.STEADFAST, PresentationAssetKey.VITALITY),
                Triple("affix.resonant.might", PresentationStringKey.MIGHT_RESONANCE, PresentationAssetKey.MIGHT),
                Triple("affix.resonant.tempo", PresentationStringKey.TEMPO_RESONANCE, PresentationAssetKey.TEMPO),
                Triple("affix.resonant.ember", PresentationStringKey.EMBER_RESONANCE, PresentationAssetKey.EMBER),
                Triple("affix.resonant.frost", PresentationStringKey.FROST_RESONANCE, PresentationAssetKey.FROST),
                Triple("affix.resonant.arcane", PresentationStringKey.ARCANE_RESONANCE, PresentationAssetKey.ARCANE),
                Triple("affix.resonant.vitality", PresentationStringKey.VITALITY_RESONANCE, PresentationAssetKey.VITALITY),
                Triple("affix.resonant.shadow", PresentationStringKey.SHADOW_RESONANCE, PresentationAssetKey.SHADOW),
                Triple("affix.resonant.guard", PresentationStringKey.GUARD_RESONANCE, PresentationAssetKey.GUARD)
            ).forEach { (id, title, icon) ->
                add(id, PresentationContentKind.AFFIX, title, PresentationStringKey.DESC_AFFIX, icon)
            }

            add("feature.skill.cinder_mark", PresentationContentKind.FEATURE_UNLOCK,
                PresentationStringKey.CINDER_MARK, PresentationStringKey.DESC_FEATURE_UNLOCK,
                PresentationAssetKey.CINDER_MARK)
            add("feature.skill.guard_mend", PresentationContentKind.FEATURE_UNLOCK,
                PresentationStringKey.GUARD_MEND, PresentationStringKey.DESC_FEATURE_UNLOCK,
                PresentationAssetKey.GUARD_MEND)
            add("feature.skill.flame_brand", PresentationContentKind.FEATURE_UNLOCK,
                PresentationStringKey.FLAME_BRAND, PresentationStringKey.DESC_FEATURE_UNLOCK,
                PresentationAssetKey.FLAME_BRAND)
            add("feature.skill.frost_lance", PresentationContentKind.FEATURE_UNLOCK,
                PresentationStringKey.FROST_LANCE, PresentationStringKey.DESC_FEATURE_UNLOCK,
                PresentationAssetKey.FROST_LANCE)
            add("feature.skill.arcane_pulse", PresentationContentKind.FEATURE_UNLOCK,
                PresentationStringKey.ARCANE_PULSE, PresentationStringKey.DESC_FEATURE_UNLOCK,
                PresentationAssetKey.ARCANE_PULSE)
            add("feature.skill.vital_surge", PresentationContentKind.FEATURE_UNLOCK,
                PresentationStringKey.VITAL_SURGE, PresentationStringKey.DESC_FEATURE_UNLOCK,
                PresentationAssetKey.VITAL_SURGE)
            add("feature.skill.umbral_cut", PresentationContentKind.FEATURE_UNLOCK,
                PresentationStringKey.UMBRAL_CUT, PresentationStringKey.DESC_FEATURE_UNLOCK,
                PresentationAssetKey.UMBRAL_CUT)
            add("feature.skill.glacial_ward", PresentationContentKind.FEATURE_UNLOCK,
                PresentationStringKey.GLACIAL_WARD, PresentationStringKey.DESC_FEATURE_UNLOCK,
                PresentationAssetKey.GLACIAL_WARD)
            add("feature.skill.resonance_shift", PresentationContentKind.FEATURE_UNLOCK,
                PresentationStringKey.RESONANCE_SHIFT, PresentationStringKey.DESC_FEATURE_UNLOCK,
                PresentationAssetKey.RESONANCE_SHIFT)
            add("feature.skill.blood_eclipse", PresentationContentKind.FEATURE_UNLOCK,
                PresentationStringKey.BLOOD_ECLIPSE_SKILL,
                PresentationStringKey.DESC_FEATURE_UNLOCK,
                PresentationAssetKey.BLOOD_ECLIPSE_SKILL)

            add("quest.training.first_resonance", PresentationContentKind.QUEST,
                PresentationStringKey.QUEST_FIRST_RESONANCE, PresentationStringKey.DESC_QUEST,
                PresentationAssetKey.FIRST_HUNT)
            add("objective.quest.training.first_resonance", PresentationContentKind.OBJECTIVE,
                PresentationStringKey.QUEST_FIRST_RESONANCE_OBJECTIVE, PresentationStringKey.DESC_QUEST_OBJECTIVE,
                PresentationAssetKey.FIRST_HUNT_OBJECTIVE)
            add("quest.training.break_protector", PresentationContentKind.QUEST,
                PresentationStringKey.QUEST_BREAK_PROTECTOR, PresentationStringKey.DESC_QUEST,
                PresentationAssetKey.FIRST_HUNT)
            add("objective.quest.training.break_protector", PresentationContentKind.OBJECTIVE,
                PresentationStringKey.QUEST_BREAK_PROTECTOR_OBJECTIVE, PresentationStringKey.DESC_QUEST_OBJECTIVE,
                PresentationAssetKey.FIRST_HUNT_OBJECTIVE)
            add("quest.training.silence_seer", PresentationContentKind.QUEST,
                PresentationStringKey.QUEST_SILENCE_SEER, PresentationStringKey.DESC_QUEST,
                PresentationAssetKey.FIRST_HUNT)
            add("objective.quest.training.silence_seer", PresentationContentKind.OBJECTIVE,
                PresentationStringKey.QUEST_SILENCE_SEER_OBJECTIVE, PresentationStringKey.DESC_QUEST_OBJECTIVE,
                PresentationAssetKey.FIRST_HUNT_OBJECTIVE)
            add("quest.training.worthy_find", PresentationContentKind.QUEST,
                PresentationStringKey.QUEST_WORTHY_FIND, PresentationStringKey.DESC_QUEST,
                PresentationAssetKey.FIRST_HUNT)
            add("objective.quest.training.worthy_find", PresentationContentKind.OBJECTIVE,
                PresentationStringKey.QUEST_WORTHY_FIND_OBJECTIVE, PresentationStringKey.DESC_QUEST_OBJECTIVE,
                PresentationAssetKey.FIRST_HUNT_OBJECTIVE)
            add("quest.training.into_depths", PresentationContentKind.QUEST,
                PresentationStringKey.QUEST_INTO_DEPTHS, PresentationStringKey.DESC_QUEST,
                PresentationAssetKey.FIRST_HUNT)
            add("objective.quest.training.into_depths", PresentationContentKind.OBJECTIVE,
                PresentationStringKey.QUEST_INTO_DEPTHS_OBJECTIVE, PresentationStringKey.DESC_QUEST_OBJECTIVE,
                PresentationAssetKey.FIRST_HUNT_OBJECTIVE)
            add("quest.training.end_warden", PresentationContentKind.QUEST,
                PresentationStringKey.QUEST_END_WARDEN, PresentationStringKey.DESC_QUEST,
                PresentationAssetKey.FIRST_HUNT)
            add("objective.quest.training.end_warden", PresentationContentKind.OBJECTIVE,
                PresentationStringKey.QUEST_END_WARDEN_OBJECTIVE, PresentationStringKey.DESC_QUEST_OBJECTIVE,
                PresentationAssetKey.FIRST_HUNT_OBJECTIVE)
            add("quest.training.hollow_patrol", PresentationContentKind.QUEST,
                PresentationStringKey.HOLLOW_PATROL, PresentationStringKey.DESC_QUEST,
                PresentationAssetKey.FIRST_HUNT)
            add("objective.quest.training.hollow_patrol", PresentationContentKind.OBJECTIVE,
                PresentationStringKey.HOLLOW_PATROL_OBJECTIVE, PresentationStringKey.DESC_QUEST_OBJECTIVE,
                PresentationAssetKey.FIRST_HUNT_OBJECTIVE)
            add("quest.training.steady_expedition", PresentationContentKind.QUEST,
                PresentationStringKey.STEADY_EXPEDITION, PresentationStringKey.DESC_QUEST,
                PresentationAssetKey.FIRST_HUNT)
            add("objective.quest.training.steady_expedition", PresentationContentKind.OBJECTIVE,
                PresentationStringKey.STEADY_EXPEDITION_OBJECTIVE, PresentationStringKey.DESC_QUEST_OBJECTIVE,
                PresentationAssetKey.FIRST_HUNT_OBJECTIVE)
            add("quest.training.resonance_practice", PresentationContentKind.QUEST,
                PresentationStringKey.RESONANCE_PRACTICE, PresentationStringKey.DESC_QUEST,
                PresentationAssetKey.FIRST_HUNT)
            add("objective.quest.training.resonance_practice", PresentationContentKind.OBJECTIVE,
                PresentationStringKey.RESONANCE_PRACTICE_OBJECTIVE, PresentationStringKey.DESC_QUEST_OBJECTIVE,
                PresentationAssetKey.FIRST_HUNT_OBJECTIVE)
            add("quest.training.first_hunt", PresentationContentKind.QUEST,
                PresentationStringKey.FIRST_HUNT, PresentationStringKey.DESC_QUEST,
                PresentationAssetKey.FIRST_HUNT)
            add(FIRST_HUNT_OBJECTIVE.value, PresentationContentKind.OBJECTIVE,
                PresentationStringKey.FIRST_HUNT_OBJECTIVE,
                PresentationStringKey.DESC_QUEST_OBJECTIVE,
                PresentationAssetKey.FIRST_HUNT_OBJECTIVE)
            add("achievement.convergence.first_forged_flame",
                PresentationContentKind.ACHIEVEMENT,
                PresentationStringKey.FORGED_FLAME_ACHIEVEMENT,
                PresentationStringKey.DESC_ACHIEVEMENT,
                PresentationAssetKey.FORGED_FLAME_ACHIEVEMENT)
            add(FORGED_FLAME_ACHIEVEMENT_OBJECTIVE.value,
                PresentationContentKind.OBJECTIVE,
                PresentationStringKey.FORGED_FLAME_ACHIEVEMENT_OBJECTIVE,
                PresentationStringKey.DESC_ACHIEVEMENT_OBJECTIVE,
                PresentationAssetKey.FORGED_FLAME_ACHIEVEMENT_OBJECTIVE)

            add("chronicle.standard", PresentationContentKind.CHRONICLE,
                PresentationStringKey.CHRONICLE_COLLAPSE, PresentationStringKey.DESC_CHRONICLE,
                PresentationAssetKey.CHRONICLE, PresentationAssetKey.CHRONICLE_ILLUSTRATION)
            add(FIRST_COLLAPSE_MILESTONE.value, PresentationContentKind.MILESTONE,
                PresentationStringKey.FIRST_COLLAPSE, PresentationStringKey.DESC_MILESTONE,
                PresentationAssetKey.FIRST_COLLAPSE)
            add("echo_unlock.adaptation_forecast", PresentationContentKind.ECHO_OFFER,
                PresentationStringKey.ADAPTATION_FORECAST, PresentationStringKey.DESC_ADAPTATION_FORECAST,
                PresentationAssetKey.ADAPTATION_FORECAST_OFFER,
                PresentationAssetKey.ADAPTATION_FORECAST_ILLUSTRATION)
            listOf(
                Triple("echo_unlock.legacy_acceleration", PresentationStringKey.LEGACY_ACCELERATION, "feature.echo.legacy_acceleration"),
                Triple("echo_unlock.doctrine_memory", PresentationStringKey.DOCTRINE_MEMORY, "feature.echo.doctrine_memory"),
                Triple("echo_unlock.expedition_memory", PresentationStringKey.EXPEDITION_MEMORY, "feature.echo.expedition_memory")
            ).forEach { (offerId, title, featureId) ->
                val description = when (title) {
                    PresentationStringKey.LEGACY_ACCELERATION -> PresentationStringKey.DESC_LEGACY_ACCELERATION
                    PresentationStringKey.DOCTRINE_MEMORY -> PresentationStringKey.DESC_DOCTRINE_MEMORY
                    else -> PresentationStringKey.DESC_EXPEDITION_MEMORY
                }
                add(offerId, PresentationContentKind.ECHO_OFFER, title, description, PresentationAssetKey.ADAPTATION_FORECAST_OFFER)
                add(featureId, PresentationContentKind.FEATURE_UNLOCK, title, PresentationStringKey.DESC_FEATURE_UNLOCK, PresentationAssetKey.ADAPTATION_FORECAST_DISCOVERY)
            }
            com.idlerpg.game.data.content.EchoTrainingContent.upgradeIds.forEach { id ->
                val training = entries.single { it.contentId == id }
                add(com.idlerpg.game.data.content.EchoTrainingContent.offerId(id).value,
                    PresentationContentKind.ECHO_OFFER, training.titleStringKey,
                    PresentationStringKey.DESC_ECHO_TRAINING, training.iconAssetKey)
                add(com.idlerpg.game.data.content.EchoTrainingContent.featureId(id).value,
                    PresentationContentKind.FEATURE_UNLOCK, training.titleStringKey,
                    PresentationStringKey.DESC_ECHO_TRAINING, training.iconAssetKey)
            }
            add(ADAPTATION_FORECAST_DISCOVERY.value, PresentationContentKind.DISCOVERY,
                PresentationStringKey.ADAPTATION_FORECAST, PresentationStringKey.DESC_DISCOVERY,
                PresentationAssetKey.ADAPTATION_FORECAST_DISCOVERY,
                PresentationAssetKey.ADAPTATION_FORECAST_ILLUSTRATION)

            val affinityMetadata = mapOf(
                Affinity.MIGHT to Triple(PresentationStringKey.MIGHT,
                    PresentationStringKey.MIGHT_MASTERY, PresentationAssetKey.MIGHT),
                Affinity.TEMPO to Triple(PresentationStringKey.TEMPO,
                    PresentationStringKey.TEMPO_MASTERY, PresentationAssetKey.TEMPO),
                Affinity.EMBER to Triple(PresentationStringKey.EMBER,
                    PresentationStringKey.EMBER_MASTERY, PresentationAssetKey.EMBER),
                Affinity.FROST to Triple(PresentationStringKey.FROST,
                    PresentationStringKey.FROST_MASTERY, PresentationAssetKey.FROST),
                Affinity.ARCANE to Triple(PresentationStringKey.ARCANE,
                    PresentationStringKey.ARCANE_MASTERY, PresentationAssetKey.ARCANE),
                Affinity.VITALITY to Triple(PresentationStringKey.VITALITY,
                    PresentationStringKey.VITALITY_MASTERY, PresentationAssetKey.VITALITY),
                Affinity.SHADOW to Triple(PresentationStringKey.SHADOW,
                    PresentationStringKey.SHADOW_MASTERY, PresentationAssetKey.SHADOW),
                Affinity.GUARD to Triple(PresentationStringKey.GUARD,
                    PresentationStringKey.GUARD_MASTERY, PresentationAssetKey.GUARD)
            )
            val masteryAssets = mapOf(
                Affinity.MIGHT to PresentationAssetKey.MIGHT_MASTERY,
                Affinity.TEMPO to PresentationAssetKey.TEMPO_MASTERY,
                Affinity.EMBER to PresentationAssetKey.EMBER_MASTERY,
                Affinity.FROST to PresentationAssetKey.FROST_MASTERY,
                Affinity.ARCANE to PresentationAssetKey.ARCANE_MASTERY,
                Affinity.VITALITY to PresentationAssetKey.VITALITY_MASTERY,
                Affinity.SHADOW to PresentationAssetKey.SHADOW_MASTERY,
                Affinity.GUARD to PresentationAssetKey.GUARD_MASTERY
            )
            Affinity.values().forEach { affinity ->
                val metadata = affinityMetadata.getValue(affinity)
                entries += PresentationContentEntry(
                    contentId = affinity.id,
                    kind = PresentationContentKind.AFFINITY,
                    titleStringKey = metadata.first,
                    shortDescriptionStringKey = PresentationStringKey.DESC_AFFINITY,
                    iconAssetKey = metadata.third
                )
                entries += PresentationContentEntry(
                    contentId = ContentId("mastery.${affinity.id.value.substringAfterLast('.') }"),
                    kind = PresentationContentKind.MASTERY,
                    titleStringKey = metadata.second,
                    shortDescriptionStringKey = PresentationStringKey.DESC_MASTERY,
                    iconAssetKey = masteryAssets.getValue(affinity)
                )
            }

            val rarityMetadata = mapOf(
                Rarity.COMMON to Pair(PresentationStringKey.COMMON, PresentationAssetKey.COMMON),
                Rarity.UNCOMMON to Pair(PresentationStringKey.UNCOMMON, PresentationAssetKey.UNCOMMON),
                Rarity.RARE to Pair(PresentationStringKey.RARE, PresentationAssetKey.RARE),
                Rarity.EPIC to Pair(PresentationStringKey.EPIC, PresentationAssetKey.EPIC),
                Rarity.LEGENDARY to Pair(PresentationStringKey.LEGENDARY, PresentationAssetKey.LEGENDARY)
            )
            Rarity.values().forEach { rarity ->
                val metadata = rarityMetadata.getValue(rarity)
                entries += PresentationContentEntry(
                    contentId = rarity.id,
                    kind = PresentationContentKind.RARITY,
                    titleStringKey = metadata.first,
                    shortDescriptionStringKey = PresentationStringKey.DESC_RARITY,
                    iconAssetKey = metadata.second
                )
            }

            return PresentationContentRegistry(
                entries = entries,
                extraRequiredVisibleIds = setOf(
                    FIRST_HUNT_OBJECTIVE,
                    FORGED_FLAME_ACHIEVEMENT_OBJECTIVE,
                    FIRST_COLLAPSE_MILESTONE,
                    ADAPTATION_FORECAST_DISCOVERY
                ),
                extraSystemOnlyIds = setOf(SLIME_ADAPTATION_TAG)
            )
        }
    }
}
