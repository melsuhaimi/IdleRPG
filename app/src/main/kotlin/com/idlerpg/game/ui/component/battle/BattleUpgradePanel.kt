package com.idlerpg.game.ui.component.battle

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.idlerpg.game.R
import com.idlerpg.game.presentation.model.BattleFeedbackKind
import com.idlerpg.game.presentation.model.BattleUiState
import com.idlerpg.game.ui.component.premium.GameButton
import com.idlerpg.game.ui.component.premium.PremiumPanel
import com.idlerpg.game.ui.content.stringResId

/** Existing Gold-funded attack upgrade, rendered from the canonical Battle projection. */
@Composable
fun BattleUpgradePanel(
    state: BattleUiState,
    onPurchase: () -> Unit,
    modifier: Modifier = Modifier
) {
    val upgrade = state.basicAttackUpgrade
    val purchase = state.feedback?.takeIf {
        it.kind == BattleFeedbackKind.UPGRADE_PURCHASED
    }

    PremiumPanel(
        backgroundResId = R.drawable.panel_secondary_premium,
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
        verticalSpacing = 5.dp
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(1.dp)
            ) {
                Text(
                    text = stringResource(upgrade.titleStringKey.stringResId()),
                    style = MaterialTheme.typography.labelLarge
                )
                Text(
                    text = stringResource(
                        R.string.battle_upgrade_level_format,
                        upgrade.level
                    ) + " · " + stringResource(
                        R.string.battle_stat_value_format,
                        stringResource(R.string.battle_attack),
                        state.player.attackDisplay
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                purchase?.let { result ->
                    if (result.upgradePreviousLevel != null && result.upgradeNewLevel != null) {
                        Text(
                            text = stringResource(
                                R.string.battle_upgrade_level_delta_format,
                                result.upgradePreviousLevel,
                                result.upgradeNewLevel
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                    if (result.upgradePreviousAttackDisplay != null &&
                        result.upgradeNewAttackDisplay != null
                    ) {
                        Text(
                            text = stringResource(
                                R.string.battle_upgrade_attack_delta_format,
                                result.upgradePreviousAttackDisplay,
                                result.upgradeNewAttackDisplay
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.tertiary
                        )
                    }
                }
            }
            GameButton(
                onClick = onPurchase,
                enabled = upgrade.affordable && !state.canRetry,
                modifier = Modifier.widthIn(min = 112.dp),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Text(
                    text = stringResource(
                        R.string.battle_upgrade_button_format,
                        upgrade.nextCostDisplay
                    )
                )
            }
        }
    }
}
