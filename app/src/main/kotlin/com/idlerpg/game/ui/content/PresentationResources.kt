package com.idlerpg.game.ui.content

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.idlerpg.game.R
import com.idlerpg.game.presentation.content.PresentationAssetKey
import com.idlerpg.game.presentation.content.PresentationStringKey

@StringRes
fun PresentationStringKey.stringResId(): Int = when (this) {
    PresentationStringKey.WORLD -> R.string.content_world
    PresentationStringKey.TRAINING_HOLLOW -> R.string.content_training_hollow
    PresentationStringKey.OUTER_FRACTURE -> R.string.world_sector_outer_fracture
    PresentationStringKey.RESONANT_DEPTHS -> R.string.world_sector_resonant_depths
    PresentationStringKey.WARDEN_CORE -> R.string.world_sector_warden_core
    PresentationStringKey.SLIME_ENCOUNTER -> R.string.content_slime_encounter
    PresentationStringKey.RIFTFANG_ENCOUNTER -> R.string.content_riftfang_encounter
    PresentationStringKey.CINDER_WISP_ENCOUNTER -> R.string.content_cinder_wisp_encounter
    PresentationStringKey.HOLLOW_BULWARK_ENCOUNTER -> R.string.content_hollow_bulwark_encounter
    PresentationStringKey.ARCANE_SEER_ENCOUNTER -> R.string.content_arcane_seer_encounter
    PresentationStringKey.BASIC_ATTACK -> R.string.content_basic_attack
    PresentationStringKey.HEAVY_STRIKE -> R.string.content_heavy_strike
    PresentationStringKey.EARTHBREAKER -> R.string.content_earthbreaker
    PresentationStringKey.EXECUTIONER -> R.string.content_executioner
    PresentationStringKey.WILDSPARK -> R.string.content_wildspark
    PresentationStringKey.SEARING_BRAND -> R.string.content_searing_brand
    PresentationStringKey.PERMAFROST -> R.string.content_permafrost
    PresentationStringKey.SHATTER_SPEAR -> R.string.content_shatter_spear
    PresentationStringKey.REAPERS_ARC -> R.string.content_reapers_arc
    PresentationStringKey.SANGUINE_EDGE -> R.string.content_sanguine_edge
    PresentationStringKey.QUICK_SLASH -> R.string.content_quick_slash
    PresentationStringKey.FLAME_BRAND -> R.string.content_flame_brand
    PresentationStringKey.CINDER_MARK -> R.string.content_cinder_mark
    PresentationStringKey.GUARD_MEND -> R.string.content_guard_mend
    PresentationStringKey.FROST_LANCE -> R.string.content_frost_lance
    PresentationStringKey.ARCANE_PULSE -> R.string.content_arcane_pulse
    PresentationStringKey.VITAL_SURGE -> R.string.content_vital_surge
    PresentationStringKey.UMBRAL_CUT -> R.string.content_umbral_cut
    PresentationStringKey.GLACIAL_WARD -> R.string.content_glacial_ward
    PresentationStringKey.RESONANCE_SHIFT -> R.string.content_resonance_shift
    PresentationStringKey.BLOOD_ECLIPSE_SKILL -> R.string.content_blood_eclipse_skill
    PresentationStringKey.VOID_LANCE -> R.string.content_void_lance
    PresentationStringKey.IRON_VOW -> R.string.content_iron_vow
    PresentationStringKey.STARFALL -> R.string.content_starfall
    PresentationStringKey.BURNING -> R.string.content_burning
    PresentationStringKey.SCORCHED -> R.string.content_scorched
    PresentationStringKey.GUARD_FOCUS -> R.string.content_guard_focus
    PresentationStringKey.STAGGER -> R.string.content_stagger
    PresentationStringKey.DEEP_STAGGER -> R.string.content_deep_stagger
    PresentationStringKey.CHILL -> R.string.content_chill
    PresentationStringKey.VITAL_REGENERATION -> R.string.content_vital_regeneration
    PresentationStringKey.FROSTBOUND_MITE -> R.string.content_frostbound_mite
    PresentationStringKey.ECHO_LEECH -> R.string.content_echo_leech
    PresentationStringKey.SHADE_MIMIC -> R.string.content_shade_mimic
    PresentationStringKey.FORGED_FLAME -> R.string.content_forged_flame
    PresentationStringKey.FLASHFIRE -> R.string.content_flashfire
    PresentationStringKey.BASTION_PULSE -> R.string.content_bastion_pulse
    PresentationStringKey.SHATTERFIELD -> R.string.content_shatterfield
    PresentationStringKey.BLOOD_ECLIPSE -> R.string.content_blood_eclipse
    PresentationStringKey.OVERDRIVE -> R.string.content_overdrive
    PresentationStringKey.WINTER_BASTION -> R.string.content_winter_bastion
    PresentationStringKey.ARCANE_REFRAIN -> R.string.content_arcane_refrain
    PresentationStringKey.SLIME -> R.string.content_slime
    PresentationStringKey.RIFTFANG -> R.string.content_riftfang
    PresentationStringKey.CINDER_WISP -> R.string.content_cinder_wisp
    PresentationStringKey.HOLLOW_BULWARK -> R.string.content_hollow_bulwark
    PresentationStringKey.ARCANE_SEER -> R.string.content_arcane_seer
    PresentationStringKey.BASIC_ATTACK_POWER -> R.string.content_basic_attack_power
    PresentationStringKey.ENDURANCE -> R.string.content_upgrade_endurance
    PresentationStringKey.ARMOR_TRAINING -> R.string.content_upgrade_armor_training
    PresentationStringKey.TEMPO_TRAINING -> R.string.content_upgrade_tempo_training
    PresentationStringKey.PRECISION -> R.string.content_upgrade_precision
    PresentationStringKey.LETHALITY -> R.string.content_upgrade_lethality
    PresentationStringKey.CHANNELING -> R.string.content_upgrade_channeling
    PresentationStringKey.RESTORATION -> R.string.content_upgrade_restoration
    PresentationStringKey.ASH_SKIN -> R.string.content_ash_skin
    PresentationStringKey.STONEHIDE -> R.string.content_stonehide
    PresentationStringKey.REFLEX_CARAPACE -> R.string.content_reflex_carapace
    PresentationStringKey.ICEBOUND_CORE -> R.string.content_icebound_core
    PresentationStringKey.NULL_VEIL -> R.string.content_null_veil
    PresentationStringKey.BLIGHTBLOOD -> R.string.content_blightblood
    PresentationStringKey.GLOOM_WARD -> R.string.content_gloom_ward
    PresentationStringKey.SIEGE_HUNGER -> R.string.content_siege_hunger
    PresentationStringKey.HOLLOW_WARDEN -> R.string.content_hollow_warden
    PresentationStringKey.LEGACY_ACCELERATION -> R.string.content_legacy_acceleration
    PresentationStringKey.DOCTRINE_MEMORY -> R.string.content_doctrine_memory
    PresentationStringKey.EXPEDITION_MEMORY -> R.string.content_expedition_memory
    PresentationStringKey.TRAINING_BLADE -> R.string.content_training_blade
    PresentationStringKey.TRAINING_CATALYST -> R.string.content_training_catalyst
    PresentationStringKey.FRACTURE_MAIL -> R.string.content_fracture_mail
    PresentationStringKey.SEERS_HELM -> R.string.content_seers_helm
    PresentationStringKey.RIFTSTEP_BOOTS -> R.string.content_riftstep_boots
    PresentationStringKey.ECHO_SIGIL -> R.string.content_echo_sigil
    PresentationStringKey.VOIDGLASS_EDGE -> R.string.content_voidglass_edge
    PresentationStringKey.WARDEN_PLATE -> R.string.content_warden_plate
    PresentationStringKey.STARFALL_VISOR -> R.string.content_starfall_visor
    PresentationStringKey.RESONANT_CORE -> R.string.content_resonant_core
    PresentationStringKey.DUSK_SIGIL -> R.string.content_dusk_sigil
    PresentationStringKey.KEEN -> R.string.content_keen
    PresentationStringKey.BRUTAL -> R.string.content_affix_brutal
    PresentationStringKey.RELENTLESS -> R.string.content_affix_relentless
    PresentationStringKey.HOLLOW_TUNED -> R.string.content_affix_hollow_tuned
    PresentationStringKey.EXECUTIONER_AFFIX -> R.string.content_affix_executioner
    PresentationStringKey.REINFORCED -> R.string.content_affix_reinforced
    PresentationStringKey.WARDED -> R.string.content_affix_warded
    PresentationStringKey.STEADFAST -> R.string.content_affix_steadfast
    PresentationStringKey.MIGHT_RESONANCE -> R.string.content_affix_might_resonance
    PresentationStringKey.TEMPO_RESONANCE -> R.string.content_affix_tempo_resonance
    PresentationStringKey.EMBER_RESONANCE -> R.string.content_affix_ember_resonance
    PresentationStringKey.FROST_RESONANCE -> R.string.content_affix_frost_resonance
    PresentationStringKey.ARCANE_RESONANCE -> R.string.content_affix_arcane_resonance
    PresentationStringKey.VITALITY_RESONANCE -> R.string.content_affix_vitality_resonance
    PresentationStringKey.SHADOW_RESONANCE -> R.string.content_affix_shadow_resonance
    PresentationStringKey.GUARD_RESONANCE -> R.string.content_affix_guard_resonance
    PresentationStringKey.QUEST_FIRST_RESONANCE -> R.string.content_quest_first_resonance
    PresentationStringKey.QUEST_FIRST_RESONANCE_OBJECTIVE -> R.string.content_quest_first_resonance_objective
    PresentationStringKey.QUEST_BREAK_PROTECTOR -> R.string.content_quest_break_protector
    PresentationStringKey.QUEST_BREAK_PROTECTOR_OBJECTIVE -> R.string.content_quest_break_protector_objective
    PresentationStringKey.QUEST_SILENCE_SEER -> R.string.content_quest_silence_seer
    PresentationStringKey.QUEST_SILENCE_SEER_OBJECTIVE -> R.string.content_quest_silence_seer_objective
    PresentationStringKey.QUEST_WORTHY_FIND -> R.string.content_quest_worthy_find
    PresentationStringKey.QUEST_WORTHY_FIND_OBJECTIVE -> R.string.content_quest_worthy_find_objective
    PresentationStringKey.QUEST_INTO_DEPTHS -> R.string.content_quest_into_depths
    PresentationStringKey.QUEST_INTO_DEPTHS_OBJECTIVE -> R.string.content_quest_into_depths_objective
    PresentationStringKey.QUEST_END_WARDEN -> R.string.content_quest_end_warden
    PresentationStringKey.QUEST_END_WARDEN_OBJECTIVE -> R.string.content_quest_end_warden_objective
    PresentationStringKey.HOLLOW_PATROL -> R.string.content_hollow_patrol
    PresentationStringKey.HOLLOW_PATROL_OBJECTIVE -> R.string.content_hollow_patrol_objective
    PresentationStringKey.STEADY_EXPEDITION -> R.string.content_steady_expedition
    PresentationStringKey.STEADY_EXPEDITION_OBJECTIVE -> R.string.content_steady_expedition_objective
    PresentationStringKey.RESONANCE_PRACTICE -> R.string.content_resonance_practice
    PresentationStringKey.RESONANCE_PRACTICE_OBJECTIVE -> R.string.content_resonance_practice_objective
    PresentationStringKey.FIRST_HUNT -> R.string.content_first_hunt
    PresentationStringKey.FIRST_HUNT_OBJECTIVE -> R.string.content_first_hunt_objective
    PresentationStringKey.FORGED_FLAME_ACHIEVEMENT -> R.string.content_forged_flame_achievement
    PresentationStringKey.FORGED_FLAME_ACHIEVEMENT_OBJECTIVE ->
        R.string.content_forged_flame_achievement_objective
    PresentationStringKey.CHRONICLE_COLLAPSE -> R.string.content_chronicle_collapse
    PresentationStringKey.FIRST_COLLAPSE -> R.string.content_first_collapse
    PresentationStringKey.ADAPTATION_FORECAST -> R.string.content_adaptation_forecast
    PresentationStringKey.MIGHT -> R.string.content_affinity_might
    PresentationStringKey.TEMPO -> R.string.content_affinity_tempo
    PresentationStringKey.EMBER -> R.string.content_affinity_ember
    PresentationStringKey.FROST -> R.string.content_affinity_frost
    PresentationStringKey.ARCANE -> R.string.content_affinity_arcane
    PresentationStringKey.VITALITY -> R.string.content_affinity_vitality
    PresentationStringKey.SHADOW -> R.string.content_affinity_shadow
    PresentationStringKey.GUARD -> R.string.content_affinity_guard
    PresentationStringKey.MIGHT_MASTERY -> R.string.content_mastery_might
    PresentationStringKey.TEMPO_MASTERY -> R.string.content_mastery_tempo
    PresentationStringKey.EMBER_MASTERY -> R.string.content_mastery_ember
    PresentationStringKey.FROST_MASTERY -> R.string.content_mastery_frost
    PresentationStringKey.ARCANE_MASTERY -> R.string.content_mastery_arcane
    PresentationStringKey.VITALITY_MASTERY -> R.string.content_mastery_vitality
    PresentationStringKey.SHADOW_MASTERY -> R.string.content_mastery_shadow
    PresentationStringKey.GUARD_MASTERY -> R.string.content_mastery_guard
    PresentationStringKey.COMMON -> R.string.content_rarity_common
    PresentationStringKey.UNCOMMON -> R.string.content_rarity_uncommon
    PresentationStringKey.RARE -> R.string.content_rarity_rare
    PresentationStringKey.EPIC -> R.string.content_rarity_epic
    PresentationStringKey.LEGENDARY -> R.string.content_rarity_legendary
    PresentationStringKey.DESC_WORLD -> R.string.content_desc_world
    PresentationStringKey.DESC_REGION -> R.string.content_desc_region
    PresentationStringKey.DESC_ENCOUNTER -> R.string.content_desc_encounter
    PresentationStringKey.DESC_SKILL -> R.string.content_desc_skill
    PresentationStringKey.DESC_HEAVY_STRIKE -> R.string.content_desc_heavy_strike
    PresentationStringKey.DESC_EARTHBREAKER -> R.string.content_desc_earthbreaker
    PresentationStringKey.DESC_EXECUTIONER -> R.string.content_desc_executioner
    PresentationStringKey.DESC_WILDSPARK -> R.string.content_desc_wildspark
    PresentationStringKey.DESC_SEARING_BRAND -> R.string.content_desc_searing_brand
    PresentationStringKey.DESC_PERMAFROST -> R.string.content_desc_permafrost
    PresentationStringKey.DESC_SHATTER_SPEAR -> R.string.content_desc_shatter_spear
    PresentationStringKey.DESC_REAPERS_ARC -> R.string.content_desc_reapers_arc
    PresentationStringKey.DESC_SANGUINE_EDGE -> R.string.content_desc_sanguine_edge
    PresentationStringKey.DESC_QUICK_SLASH -> R.string.content_desc_quick_slash
    PresentationStringKey.DESC_FLAME_BRAND -> R.string.content_desc_flame_brand
    PresentationStringKey.DESC_CINDER_MARK -> R.string.content_desc_cinder_mark
    PresentationStringKey.DESC_GUARD_MEND -> R.string.content_desc_guard_mend
    PresentationStringKey.DESC_FROST_LANCE -> R.string.content_desc_frost_lance
    PresentationStringKey.DESC_ARCANE_PULSE -> R.string.content_desc_arcane_pulse
    PresentationStringKey.DESC_VITAL_SURGE -> R.string.content_desc_vital_surge
    PresentationStringKey.DESC_UMBRAL_CUT -> R.string.content_desc_umbral_cut
    PresentationStringKey.DESC_GLACIAL_WARD -> R.string.content_desc_glacial_ward
    PresentationStringKey.DESC_RESONANCE_SHIFT -> R.string.content_desc_resonance_shift
    PresentationStringKey.DESC_BLOOD_ECLIPSE_SKILL -> R.string.content_desc_blood_eclipse_skill
    PresentationStringKey.DESC_STATUS -> R.string.content_desc_status
    PresentationStringKey.DESC_STATUS_BURNING -> R.string.battle_status_detail_burning
    PresentationStringKey.DESC_STATUS_SCORCHED -> R.string.battle_status_detail_scorched
    PresentationStringKey.DESC_STATUS_GUARD_FOCUS -> R.string.battle_status_detail_guard_focus
    PresentationStringKey.DESC_STATUS_GLACIAL_WARD -> R.string.battle_status_detail_glacial_ward
    PresentationStringKey.DESC_STATUS_STAGGER -> R.string.battle_status_detail_stagger
    PresentationStringKey.DESC_STATUS_DEEP_STAGGER -> R.string.battle_status_detail_deep_stagger
    PresentationStringKey.DESC_STATUS_CHILL -> R.string.battle_status_detail_chill
    PresentationStringKey.DESC_STATUS_VITAL_REGENERATION -> R.string.battle_status_detail_vital_regeneration
    PresentationStringKey.DESC_CONVERGENCE -> R.string.content_desc_convergence
    PresentationStringKey.DESC_ENEMY -> R.string.content_desc_enemy
    PresentationStringKey.DESC_UPGRADE -> R.string.content_desc_upgrade
    PresentationStringKey.DESC_MUTATION -> R.string.content_desc_mutation
    PresentationStringKey.DESC_ITEM -> R.string.content_desc_item
    PresentationStringKey.DESC_AFFIX -> R.string.content_desc_affix
    PresentationStringKey.DESC_FEATURE_UNLOCK -> R.string.content_desc_feature_unlock
    PresentationStringKey.DESC_QUEST -> R.string.content_desc_quest
    PresentationStringKey.DESC_QUEST_OBJECTIVE -> R.string.content_desc_quest_objective
    PresentationStringKey.DESC_ACHIEVEMENT -> R.string.content_desc_achievement
    PresentationStringKey.DESC_ACHIEVEMENT_OBJECTIVE ->
        R.string.content_desc_achievement_objective
    PresentationStringKey.DESC_CHRONICLE -> R.string.content_desc_chronicle
    PresentationStringKey.DESC_MILESTONE -> R.string.content_desc_milestone
    PresentationStringKey.DESC_ECHO_TRAINING -> R.string.content_desc_echo_training
    PresentationStringKey.DESC_LEGACY_ACCELERATION -> R.string.content_desc_legacy_acceleration
    PresentationStringKey.DESC_DOCTRINE_MEMORY -> R.string.content_desc_doctrine_memory
    PresentationStringKey.DESC_EXPEDITION_MEMORY -> R.string.content_desc_expedition_memory
    PresentationStringKey.DESC_ADAPTATION_FORECAST -> R.string.content_desc_adaptation_forecast
    PresentationStringKey.DESC_ECHO_OFFER -> R.string.content_desc_echo_offer
    PresentationStringKey.DESC_DISCOVERY -> R.string.content_desc_discovery
    PresentationStringKey.DESC_AFFINITY -> R.string.content_desc_affinity
    PresentationStringKey.DESC_MASTERY -> R.string.content_desc_mastery
    PresentationStringKey.DESC_RARITY -> R.string.content_desc_rarity
}

@DrawableRes
fun PresentationAssetKey.drawableResId(): Int = when (this) {
    PresentationAssetKey.NAV_BATTLE -> R.drawable.nav_battle_premium
    PresentationAssetKey.NAV_WORLD -> R.drawable.nav_world_premium
    PresentationAssetKey.NAV_DOCTRINE -> R.drawable.nav_doctrine_premium
    PresentationAssetKey.NAV_GEAR -> R.drawable.nav_gear_premium
    PresentationAssetKey.NAV_PROGRESS -> R.drawable.nav_progress_premium

    PresentationAssetKey.WORLD -> R.drawable.ic_content_world
    PresentationAssetKey.TRAINING_HOLLOW -> R.drawable.ic_region_training_hollow
    PresentationAssetKey.SLIME_ENCOUNTER -> R.drawable.icon_enemy_hollow_slime
    PresentationAssetKey.RIFTFANG_ENCOUNTER -> R.drawable.icon_enemy_riftfang
    PresentationAssetKey.CINDER_WISP_ENCOUNTER -> R.drawable.icon_enemy_cinder_wisp
    PresentationAssetKey.HOLLOW_BULWARK_ENCOUNTER -> R.drawable.icon_enemy_hollow_bulwark
    PresentationAssetKey.ARCANE_SEER_ENCOUNTER -> R.drawable.icon_enemy_arcane_seer
    PresentationAssetKey.BASIC_ATTACK -> R.drawable.ic_skill_basic_attack
    PresentationAssetKey.HEAVY_STRIKE -> R.drawable.ic_skill_heavy_strike
    PresentationAssetKey.QUICK_SLASH -> R.drawable.ic_skill_quick_slash
    PresentationAssetKey.FLAME_BRAND -> R.drawable.ic_skill_flame_brand
    PresentationAssetKey.CINDER_MARK -> R.drawable.ic_skill_cinder_mark
    PresentationAssetKey.GUARD_MEND -> R.drawable.ic_skill_guard_mend
    PresentationAssetKey.FROST_LANCE -> R.drawable.ic_skill_frost_lance
    PresentationAssetKey.ARCANE_PULSE -> R.drawable.ic_skill_arcane_pulse
    PresentationAssetKey.VITAL_SURGE -> R.drawable.ic_skill_vital_surge
    PresentationAssetKey.UMBRAL_CUT -> R.drawable.ic_skill_umbral_cut
    PresentationAssetKey.GLACIAL_WARD -> R.drawable.ic_skill_glacial_ward
    PresentationAssetKey.RESONANCE_SHIFT -> R.drawable.ic_skill_resonance_shift
    PresentationAssetKey.BLOOD_ECLIPSE_SKILL -> R.drawable.ic_skill_blood_eclipse
    PresentationAssetKey.VOID_LANCE -> R.drawable.ic_skill_void_lance
    PresentationAssetKey.IRON_VOW -> R.drawable.ic_skill_iron_vow
    PresentationAssetKey.STARFALL -> R.drawable.ic_skill_starfall
    PresentationAssetKey.BURNING -> R.drawable.ic_status_burning
    PresentationAssetKey.GUARD_FOCUS -> R.drawable.ic_status_guard_focus
    PresentationAssetKey.STAGGER -> R.drawable.ic_skill_heavy_strike
    PresentationAssetKey.CHILL -> R.drawable.ic_skill_frost_lance
    PresentationAssetKey.VITAL_REGENERATION -> R.drawable.ic_skill_vital_surge
    PresentationAssetKey.FROSTBOUND_MITE -> R.drawable.ic_enemy_frostbound_mite
    PresentationAssetKey.ECHO_LEECH -> R.drawable.ic_enemy_echo_leech
    PresentationAssetKey.SHADE_MIMIC -> R.drawable.ic_enemy_shade_mimic
    PresentationAssetKey.FORGED_FLAME -> R.drawable.ic_convergence_forged_flame
    PresentationAssetKey.FLASHFIRE -> R.drawable.ic_convergence_flashfire
    PresentationAssetKey.BASTION_PULSE -> R.drawable.ic_convergence_bastion_pulse
    PresentationAssetKey.SHATTERFIELD -> R.drawable.ic_convergence_shatterfield
    PresentationAssetKey.BLOOD_ECLIPSE -> R.drawable.ic_convergence_blood_eclipse
    PresentationAssetKey.OVERDRIVE -> R.drawable.ic_convergence_overdrive
    PresentationAssetKey.WINTER_BASTION -> R.drawable.ic_convergence_winter_bastion
    PresentationAssetKey.ARCANE_REFRAIN -> R.drawable.ic_convergence_arcane_refrain
    PresentationAssetKey.SLIME -> R.drawable.icon_enemy_hollow_slime
    PresentationAssetKey.RIFTFANG -> R.drawable.icon_enemy_riftfang
    PresentationAssetKey.CINDER_WISP -> R.drawable.icon_enemy_cinder_wisp
    PresentationAssetKey.HOLLOW_BULWARK -> R.drawable.icon_enemy_hollow_bulwark
    PresentationAssetKey.ARCANE_SEER -> R.drawable.icon_enemy_arcane_seer
    PresentationAssetKey.BASIC_ATTACK_POWER -> R.drawable.ic_upgrade_attack
    PresentationAssetKey.ENDURANCE -> R.drawable.ic_affinity_vitality
    PresentationAssetKey.ARMOR_TRAINING -> R.drawable.ic_affinity_guard
    PresentationAssetKey.TEMPO_TRAINING -> R.drawable.ic_affinity_tempo
    PresentationAssetKey.PRECISION -> R.drawable.ic_affinity_arcane
    PresentationAssetKey.LETHALITY -> R.drawable.ic_affinity_shadow
    PresentationAssetKey.CHANNELING -> R.drawable.ic_convergence_arcane_refrain
    PresentationAssetKey.RESTORATION -> R.drawable.ic_skill_guard_mend
    PresentationAssetKey.ASH_SKIN -> R.drawable.ic_mutation_ash_skin
    PresentationAssetKey.TRAINING_BLADE -> R.drawable.ic_item_training_blade
    PresentationAssetKey.TRAINING_CATALYST -> R.drawable.ic_item_training_catalyst
    PresentationAssetKey.FRACTURE_MAIL -> R.drawable.ic_item_fracture_mail
    PresentationAssetKey.SEERS_HELM -> R.drawable.ic_item_seer_helm
    PresentationAssetKey.RIFTSTEP_BOOTS -> R.drawable.ic_item_riftstep_boots
    PresentationAssetKey.ECHO_SIGIL -> R.drawable.ic_item_echo_sigil
    PresentationAssetKey.VOIDGLASS_EDGE -> R.drawable.ic_item_voidglass_edge
    PresentationAssetKey.WARDEN_PLATE -> R.drawable.ic_item_warden_plate
    PresentationAssetKey.STARFALL_VISOR -> R.drawable.ic_item_starfall_visor
    PresentationAssetKey.RESONANT_CORE -> R.drawable.ic_item_resonant_core
    PresentationAssetKey.DUSK_SIGIL -> R.drawable.ic_item_dusk_sigil
    PresentationAssetKey.KEEN -> R.drawable.ic_affix_keen
    PresentationAssetKey.FIRST_HUNT -> R.drawable.ic_quest_first_hunt
    PresentationAssetKey.FIRST_HUNT_OBJECTIVE -> R.drawable.ic_objective_target
    PresentationAssetKey.FORGED_FLAME_ACHIEVEMENT -> R.drawable.ic_achievement_forged_flame
    PresentationAssetKey.FORGED_FLAME_ACHIEVEMENT_OBJECTIVE -> R.drawable.ic_objective_convergence
    PresentationAssetKey.CHRONICLE -> R.drawable.ic_chronicle
    PresentationAssetKey.FIRST_COLLAPSE -> R.drawable.ic_milestone_first_collapse
    PresentationAssetKey.ADAPTATION_FORECAST_OFFER -> R.drawable.ic_echo_adaptation_forecast
    PresentationAssetKey.ADAPTATION_FORECAST_DISCOVERY -> R.drawable.ic_discovery_adaptation_forecast

    PresentationAssetKey.MIGHT,
    PresentationAssetKey.MIGHT_MASTERY -> R.drawable.ic_affinity_might
    PresentationAssetKey.TEMPO,
    PresentationAssetKey.TEMPO_MASTERY -> R.drawable.ic_affinity_tempo
    PresentationAssetKey.EMBER,
    PresentationAssetKey.EMBER_MASTERY -> R.drawable.ic_affinity_ember
    PresentationAssetKey.FROST,
    PresentationAssetKey.FROST_MASTERY -> R.drawable.ic_affinity_frost
    PresentationAssetKey.ARCANE,
    PresentationAssetKey.ARCANE_MASTERY -> R.drawable.ic_affinity_arcane
    PresentationAssetKey.VITALITY,
    PresentationAssetKey.VITALITY_MASTERY -> R.drawable.ic_affinity_vitality
    PresentationAssetKey.SHADOW,
    PresentationAssetKey.SHADOW_MASTERY -> R.drawable.ic_affinity_shadow
    PresentationAssetKey.GUARD,
    PresentationAssetKey.GUARD_MASTERY -> R.drawable.ic_affinity_guard

    PresentationAssetKey.COMMON -> R.drawable.ic_rarity_common
    PresentationAssetKey.UNCOMMON -> R.drawable.ic_rarity_uncommon
    PresentationAssetKey.RARE -> R.drawable.ic_rarity_rare
    PresentationAssetKey.EPIC -> R.drawable.ic_rarity_epic
    PresentationAssetKey.LEGENDARY -> R.drawable.ic_rarity_legendary

    PresentationAssetKey.TRAINING_HOLLOW_ILLUSTRATION -> R.drawable.battle_backdrop_warden_generated
    PresentationAssetKey.TRAINING_HOLLOW_OUTER_FRACTURE_BACKGROUND ->
        R.drawable.bg_training_hollow_outer_fracture
    PresentationAssetKey.TRAINING_HOLLOW_RESONANT_DEPTHS_BACKGROUND ->
        R.drawable.bg_training_hollow_resonant_depths
    PresentationAssetKey.TRAINING_HOLLOW_WARDEN_CORE_BACKGROUND ->
        R.drawable.battle_backdrop_warden_generated
    PresentationAssetKey.TRAINING_HOLLOW_OUTER_FRACTURE_BANNER ->
        R.drawable.banner_training_hollow_outer_fracture
    PresentationAssetKey.TRAINING_HOLLOW_RESONANT_DEPTHS_BANNER ->
        R.drawable.banner_training_hollow_resonant_depths
    PresentationAssetKey.TRAINING_HOLLOW_WARDEN_CORE_BANNER ->
        R.drawable.hud_header_castle_generated
    PresentationAssetKey.SLIME_ILLUSTRATION -> R.drawable.enemy_hollow_slime
    PresentationAssetKey.RIFTFANG_ILLUSTRATION -> R.drawable.enemy_riftfang
    PresentationAssetKey.CINDER_WISP_ILLUSTRATION -> R.drawable.enemy_cinder_wisp
    PresentationAssetKey.HOLLOW_BULWARK_ILLUSTRATION -> R.drawable.enemy_aegis_golem_generated
    PresentationAssetKey.ARCANE_SEER_ILLUSTRATION -> R.drawable.enemy_arcane_seer
    PresentationAssetKey.ECHO_BOUND_HERO_ILLUSTRATION -> R.drawable.hero_mel_arcane_generated
    PresentationAssetKey.FROSTBOUND_MITE_ILLUSTRATION -> R.drawable.enemy_frostbound_mite
    PresentationAssetKey.ECHO_LEECH_ILLUSTRATION -> R.drawable.enemy_echo_leech
    PresentationAssetKey.SHADE_MIMIC_ILLUSTRATION -> R.drawable.enemy_shade_mimic
    PresentationAssetKey.HOLLOW_WARDEN_ILLUSTRATION -> R.drawable.enemy_hollow_warden_generated
    PresentationAssetKey.SLIME_ATTACK_FX -> R.drawable.fx_enemy_hollow_slime
    PresentationAssetKey.RIFTFANG_ATTACK_FX -> R.drawable.fx_enemy_riftfang
    PresentationAssetKey.CINDER_WISP_ATTACK_FX -> R.drawable.fx_enemy_cinder_wisp
    PresentationAssetKey.HOLLOW_BULWARK_ATTACK_FX -> R.drawable.fx_enemy_hollow_bulwark
    PresentationAssetKey.ARCANE_SEER_ATTACK_FX -> R.drawable.fx_enemy_arcane_seer
    PresentationAssetKey.ENCOUNTER_FRAME_NORMAL -> R.drawable.frame_encounter_normal
    PresentationAssetKey.ENCOUNTER_FRAME_ELITE -> R.drawable.frame_encounter_elite
    PresentationAssetKey.ENCOUNTER_FRAME_ANOMALY -> R.drawable.frame_encounter_anomaly
    PresentationAssetKey.CHRONICLE_ILLUSTRATION -> R.drawable.art_chronicle
    PresentationAssetKey.ADAPTATION_FORECAST_ILLUSTRATION -> R.drawable.art_adaptation_forecast
}
