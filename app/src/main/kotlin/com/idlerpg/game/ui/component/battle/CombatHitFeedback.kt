package com.idlerpg.game.ui.component.battle

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import com.idlerpg.game.presentation.model.BattleImpactUiState
import com.idlerpg.game.ui.theme.LocalReducedMotion
import kotlinx.coroutines.delay

/** Event-driven, bounded feedback. Animation never changes canonical combat. */
@Composable
fun CombatHitFeedback(impact: BattleImpactUiState?, modifier: Modifier = Modifier) {
    var latest by remember { mutableStateOf(impact) }
    LaunchedEffect(impact?.sequenceNumber) { if (impact != null) latest = impact }
    val visible = latest ?: return
    val reduced = LocalReducedMotion.current
    val progress = remember { Animatable(1f) }
    LaunchedEffect(visible.sequenceNumber, reduced) {
        progress.snapTo(0f)
        if (reduced) { delay(650L); progress.snapTo(1f) }
        else progress.animateTo(1f, tween(650))
    }
    if (progress.value >= 1f) return
    val color = if (visible.critical) Color(0xFFFFD166) else Color.White
    Box(modifier, contentAlignment = Alignment.TopCenter) {
        if (!reduced) Canvas(Modifier.fillMaxSize()) {
            val alpha = (1f - progress.value * 2f).coerceIn(0f, 1f)
            drawLine(color.copy(alpha = alpha), Offset(size.width * 0.18f, size.height * 0.72f),
                Offset(size.width * 0.82f, size.height * 0.2f), strokeWidth = 4f, cap = StrokeCap.Round)
        }
        Text((if (visible.critical) "! " else "") + visible.amountDisplay,
            color = color, fontWeight = FontWeight.ExtraBold, style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.graphicsLayer {
                alpha = 1f - progress.value
                translationY = if (reduced) 0f else -24f * progress.value
            })
    }
}
