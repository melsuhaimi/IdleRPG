package com.idlerpg.game.ui.component.battle

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.idlerpg.game.R
import com.idlerpg.game.presentation.model.BattlePlayerUiState
import com.idlerpg.game.ui.component.premium.PremiumImageProgressBar
import com.idlerpg.game.ui.component.premium.PremiumPanel
import com.idlerpg.game.ui.content.stringResId

@Composable
fun PlayerCombatCard(
    player: BattlePlayerUiState,
    modifier: Modifier = Modifier
) {
    PremiumPanel(
        backgroundResId = R.drawable.panel_primary_premium,
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 18.dp),
        verticalSpacing = 7.dp
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = stringResource(R.string.battle_player_readout),
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = stringResource(
                    R.string.battle_health_format,
                    player.currentHealthDisplay,
                    player.maximumHealthDisplay
                ),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        PremiumImageProgressBar(
            progressUnits = player.healthProgressUnits,
            trackResId = R.drawable.bar_hp_track_premium,
            fillResId = R.drawable.bar_hp_fill_premium,
            contentDescriptionText = stringResource(
                R.string.a11y_health_progress_format,
                stringResource(R.string.battle_player_readout),
                player.currentHealthDisplay,
                player.maximumHealthDisplay
            ),
            modifier = Modifier.fillMaxWidth(),
            height = 28.dp
        )

        val largeText = LocalDensity.current.fontScale >= 1.3f
        if (largeText) {
            Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                StatText(stringResource(R.string.battle_attack), player.attackDisplay)
                StatText(stringResource(R.string.battle_armor), player.armorDisplay)
                StatText(stringResource(R.string.battle_dps), player.basicAttackDpsDisplay)
            }
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                StatText(stringResource(R.string.battle_attack), player.attackDisplay)
                StatText(stringResource(R.string.battle_armor), player.armorDisplay)
                StatText(stringResource(R.string.battle_dps), player.basicAttackDpsDisplay)
            }
        }

        Text(
            text = stringResource(
                R.string.battle_basic_interval_format,
                player.basicAttackIntervalMillis
            ),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = player.nextDecisionRemainingMillis?.let { remaining ->
                stringResource(R.string.battle_next_decision_format, remaining)
            } ?: stringResource(R.string.battle_next_decision_idle),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.secondary
        )

        if (player.resources.isNotEmpty()) {
            Text(
                text = stringResource(R.string.battle_resources),
                style = MaterialTheme.typography.labelLarge
            )
            player.resources.forEach { resource ->
                val title = resource.titleStringKey?.let { key ->
                    stringResource(key.stringResId())
                } ?: stringResource(R.string.battle_resource_generic)
                Text(
                    text = "$title · ${resource.amountDisplay}",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }

        if (player.statuses.isNotEmpty()) {
            Text(
                text = stringResource(R.string.battle_player_statuses),
                style = MaterialTheme.typography.labelLarge
            )
            player.statuses.forEach { status ->
                Text(
                    text = stringResource(
                        R.string.battle_status_format,
                        stringResource(status.titleStringKey.stringResId()),
                        status.stackCount,
                        status.remainingMillis
                    ),
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

@Composable
private fun StatText(label: String, value: String) {
    Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium
        )
    }
}
