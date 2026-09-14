package com.idlerpg.game.ui.component

import android.content.res.Resources
import android.graphics.BitmapFactory
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import com.idlerpg.game.R
import com.idlerpg.game.core.id.ContentId

/** Shared decoded atlas: twelve images use one texture without storing a Context. */
private object SkillAtlas {
    private var bitmap: ImageBitmap? = null
    @Synchronized fun load(resources: Resources): ImageBitmap = bitmap ?: BitmapFactory
        .decodeResource(resources, R.drawable.skill_atlas).asImageBitmap().also { bitmap = it }
    val ids = listOf("heavy_strike", "quick_slash", "flame_brand", "cinder_mark",
        "guard_mend", "frost_lance", "arcane_pulse", "vital_surge",
        "umbral_cut", "glacial_ward", "resonance_shift", "blood_eclipse")
}

@Composable
fun SkillArtwork(skillId: ContentId, modifier: Modifier = Modifier) {
    val atlas = SkillAtlas.load(LocalContext.current.resources)
    val tile = SkillAtlas.ids.indexOf(skillId.value.removePrefix("skill."))
    if (tile < 0) return
    Canvas(modifier) {
        val width = atlas.width / 4
        // Authored row boundaries exclude the atlas separators; artwork stays inside each tile.
        val rows = intArrayOf(0, 355, 700, 1086)
        val row = tile / 4
        drawImage(atlas, srcOffset = IntOffset((tile % 4) * width + 3, rows[row] + 3),
            srcSize = IntSize(width - 6, rows[row + 1] - rows[row] - 6),
            dstSize = IntSize(size.width.toInt(), size.height.toInt()))
    }
}

fun skillRoleResource(skillId: ContentId): Int = when (skillId.value) {
    "skill.guard_mend", "skill.vital_surge" -> R.string.skill_role_healing
    "skill.glacial_ward" -> R.string.skill_role_defense
    "skill.frost_lance" -> R.string.skill_role_control
    "skill.arcane_pulse" -> R.string.skill_role_area
    "skill.resonance_shift" -> R.string.skill_role_combo
    "skill.cinder_mark", "skill.flame_brand" -> R.string.skill_role_burning
    "skill.umbral_cut" -> R.string.skill_role_execute
    "skill.blood_eclipse" -> R.string.skill_role_drain
    else -> R.string.skill_role_damage
}
