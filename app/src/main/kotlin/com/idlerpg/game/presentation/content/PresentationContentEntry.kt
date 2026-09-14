package com.idlerpg.game.presentation.content

import com.idlerpg.game.core.id.ContentId

/** User-facing semantic category; gameplay behavior never branches on this value. */
enum class PresentationContentKind {
    WORLD,
    REGION,
    ENCOUNTER,
    SKILL,
    STATUS,
    CONVERGENCE,
    ENEMY,
    UPGRADE,
    MUTATION,
    ITEM,
    AFFIX,
    FEATURE_UNLOCK,
    QUEST,
    OBJECTIVE,
    ACHIEVEMENT,
    CHRONICLE,
    MILESTONE,
    ECHO_OFFER,
    DISCOVERY,
    AFFINITY,
    MASTERY,
    RARITY
}

/**
 * Presentation-only metadata keyed by canonical [ContentId].
 *
 * No damage, cost, reward, cooldown, unlock threshold, or other gameplay value belongs here.
 */
data class PresentationContentEntry(
    val contentId: ContentId,
    val kind: PresentationContentKind,
    val titleStringKey: PresentationStringKey,
    val shortDescriptionStringKey: PresentationStringKey,
    val iconAssetKey: PresentationAssetKey,
    val illustrationAssetKey: PresentationAssetKey? = null
)
