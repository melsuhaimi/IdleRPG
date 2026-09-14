package com.idlerpg.game.ui.component.resonance

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.idlerpg.game.R
import com.idlerpg.game.core.id.ContentId
import com.idlerpg.game.presentation.model.ResonanceAffinityUiState
import com.idlerpg.game.ui.component.premium.PremiumPanel
import com.idlerpg.game.ui.content.stringResId
import com.idlerpg.game.ui.theme.affinityVisualToken

@Composable
fun ResonanceLoom(affinities: List<ResonanceAffinityUiState>, sequence: List<ContentId>, modifier: Modifier = Modifier) {
    val visibleSequence = sequence.takeLast(5)
    val sequenceLabels = mutableListOf<String>()
    for (id in visibleSequence) {
        affinities.firstOrNull { it.affinityId == id }?.let { affinity ->
            sequenceLabels += stringResource(affinity.titleStringKey.stringResId())
        }
    }
    val sequenceDescription = if (sequenceLabels.isEmpty()) {
        stringResource(R.string.battle_resonance_sequence_empty_a11y)
    } else {
        stringResource(R.string.battle_resonance_sequence_a11y, sequenceLabels.joinToString(" → "))
    }
    PremiumPanel(
        backgroundResId = R.drawable.panel_secondary_premium,
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
        verticalSpacing = 4.dp
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.battle_resonance_compact), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.secondary)
            Row(
                Modifier.weight(1f).semantics {
                    contentDescription = sequenceDescription
                },
                horizontalArrangement = Arrangement.Center
            ) {
                if (visibleSequence.isEmpty()) Text("◇  ◇  ◇", color = MaterialTheme.colorScheme.onSurfaceVariant)
                else visibleSequence.forEach { id ->
                    val token = affinityVisualToken(id)
                    Icon(
                        painter = painterResource(token.iconResId),
                        contentDescription = null,
                        tint = token.color,
                        modifier = Modifier.size(17.dp)
                    )
                }
            }
            Text("${visibleSequence.size}/5", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            affinities.forEach { affinity ->
                val token = affinityVisualToken(affinity.affinityId)
                val fraction = affinity.chargeProgressUnits.coerceIn(0, 10_000) / 10_000f
                val title = stringResource(affinity.titleStringKey.stringResId())
                val chargeDescription = stringResource(
                    R.string.battle_resonance_charge_a11y,
                    title,
                    affinity.chargeDisplay,
                    affinity.capDisplay
                )
                Column(
                    Modifier.weight(1f).semantics(mergeDescendants = true) {
                        contentDescription = chargeDescription
                        progressBarRangeInfo = ProgressBarRangeInfo(fraction, 0f..1f)
                    },
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        painter = painterResource(token.iconResId),
                        contentDescription = title,
                        tint = token.color,
                        modifier = Modifier.size(16.dp)
                    )
                    Box(Modifier.fillMaxWidth().height(3.dp).clip(RoundedCornerShape(99.dp)).background(MaterialTheme.colorScheme.surfaceVariant)) {
                        Box(Modifier.fillMaxWidth(fraction).height(3.dp).background(token.color))
                    }
                }
            }
        }
    }
}
