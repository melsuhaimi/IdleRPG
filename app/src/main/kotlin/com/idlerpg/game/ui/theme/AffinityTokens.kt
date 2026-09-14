package com.idlerpg.game.ui.theme

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import com.idlerpg.game.R
import com.idlerpg.game.core.id.ContentId
import com.idlerpg.game.domain.definition.Affinity

/** FUI-03 semantic affinity treatment: color is always paired with a distinct glyph. */
data class AffinityVisualToken(
    val glyph: String,
    val color: Color,
    @DrawableRes val iconResId: Int,
    @StringRes val labelResId: Int
)

fun affinityVisualToken(affinityId: ContentId): AffinityVisualToken = when (affinityId) {
    Affinity.MIGHT.id -> AffinityVisualToken("◆", Color(0xFFFF6470), R.drawable.ic_affinity_might, R.string.content_affinity_might)
    Affinity.TEMPO.id -> AffinityVisualToken("↻", Color(0xFF49D6E9), R.drawable.ic_affinity_tempo, R.string.content_affinity_tempo)
    Affinity.EMBER.id -> AffinityVisualToken("▲", Color(0xFFFF8A45), R.drawable.ic_affinity_ember, R.string.content_affinity_ember)
    Affinity.FROST.id -> AffinityVisualToken("✣", Color(0xFF8CBFFF), R.drawable.ic_affinity_frost, R.string.content_affinity_frost)
    Affinity.ARCANE.id -> AffinityVisualToken("✦", Color(0xFFB277FF), R.drawable.ic_affinity_arcane, R.string.content_affinity_arcane)
    Affinity.VITALITY.id -> AffinityVisualToken("✚", Color(0xFF67D889), R.drawable.ic_affinity_vitality, R.string.content_affinity_vitality)
    Affinity.SHADOW.id -> AffinityVisualToken("◐", Color(0xFF7F78D9), R.drawable.ic_affinity_shadow, R.string.content_affinity_shadow)
    Affinity.GUARD.id -> AffinityVisualToken("⬡", Color(0xFFD7C86E), R.drawable.ic_affinity_guard, R.string.content_affinity_guard)
    else -> AffinityVisualToken("?", Color(0xFFAEB4C2), R.drawable.ic_content_placeholder, R.string.progress_affinity_generic)
}

@StringRes
fun affinityTitleResId(affinityId: ContentId): Int = affinityVisualToken(affinityId).labelResId

@Composable
fun AffinityIcon(
    affinityId: ContentId,
    contentDescription: String?,
    modifier: Modifier = Modifier
) {
    val token = affinityVisualToken(affinityId)
    Icon(
        painter = painterResource(token.iconResId),
        contentDescription = contentDescription,
        tint = token.color,
        modifier = modifier
    )
}
