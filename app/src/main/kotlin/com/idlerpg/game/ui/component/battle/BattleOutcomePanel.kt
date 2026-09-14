package com.idlerpg.game.ui.component.battle

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.idlerpg.game.R
import com.idlerpg.game.core.id.InstanceId
import com.idlerpg.game.presentation.model.BattleCombatStatusUi
import com.idlerpg.game.presentation.model.BattleFeedbackKind
import com.idlerpg.game.presentation.model.BattleRewardUiState
import com.idlerpg.game.presentation.model.BattleUiState
import com.idlerpg.game.ui.component.premium.GameButton
import com.idlerpg.game.ui.component.premium.GameOutlinedButton
import com.idlerpg.game.ui.component.premium.PremiumPanel
import com.idlerpg.game.ui.content.stringResId

/**
 * Non-blocking, fact-only handoff after a completed encounter.
 *
 * Reward facts are projected from the completed canonical transition. The panel never estimates
 * or recomputes Gold, XP, or item values.
 */
@Composable
fun BattleOutcomePanel(
    state: BattleUiState,
    onOpenBuild: (InstanceId?) -> Unit,
    onOpenFarm: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (state.combatStatus != BattleCombatStatusUi.VICTORY) return

    val feedback = state.feedback
    val detail = when (feedback?.kind) {
        BattleFeedbackKind.LOOT -> stringResource(R.string.battle_feedback_loot)
        BattleFeedbackKind.LEVEL_UP -> feedback.amountDisplay?.let { level ->
            stringResource(R.string.battle_feedback_level_up, level)
        }
        BattleFeedbackKind.UPGRADE_PURCHASED -> stringResource(R.string.battle_feedback_upgrade)
        BattleFeedbackKind.CONVERGENCE -> stringResource(R.string.battle_feedback_convergence)
        BattleFeedbackKind.ENEMY_DEFEATED -> stringResource(R.string.battle_feedback_enemy_defeated)
        BattleFeedbackKind.COMBAT_ENDED -> null
        else -> null
    }
    val inspectableItemId = feedback?.reward?.items
        ?.firstOrNull { !it.sentToOverflow && !it.autoSalvaged }
        ?.itemInstanceId

    PremiumPanel(
        backgroundResId = R.drawable.panel_secondary_premium,
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
        verticalSpacing = 7.dp
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = stringResource(R.string.world_feedback_encounter_cleared),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.secondary
            )
            detail?.let { message ->
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            feedback?.reward?.let { reward ->
                BattleRewardSummary(reward)
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            GameButton(
                onClick = { onOpenBuild(inspectableItemId) },
                modifier = Modifier.widthIn(min = 132.dp)
            ) {
                Text(
                    stringResource(
                        if (inspectableItemId != null) {
                            R.string.battle_inspect_reward
                        } else {
                            R.string.nav_gear
                        }
                    )
                )
            }
            GameOutlinedButton(
                onClick = onOpenFarm,
                modifier = Modifier.widthIn(min = 112.dp)
            ) {
                Text(stringResource(R.string.battle_open_farm))
            }
        }
    }
}

@Composable
private fun BattleRewardSummary(reward: BattleRewardUiState) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        reward.goldDisplay?.let { gold ->
            Text(
                text = stringResource(R.string.battle_reward_gold_format, gold),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.tertiary
            )
        }
        reward.experienceDisplay?.let { experience ->
            Text(
                text = stringResource(R.string.battle_reward_xp_format, experience),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.secondary
            )
        }
        reward.items.forEach { item ->
            val title = item.titleStringKey?.let { stringResource(it.stringResId()) }
                ?: stringResource(R.string.battle_reward_item_unknown)
            val rarity = item.rarityTitleStringKey?.let { stringResource(it.stringResId()) }
            val overflow = if (item.sentToOverflow) {
                stringResource(R.string.battle_reward_overflow)
            } else {
                null
            }
            val salvaged = if (item.autoSalvaged) {
                stringResource(R.string.battle_reward_auto_salvaged)
            } else {
                null
            }
            val suffix = listOfNotNull(rarity, overflow, salvaged)
                .takeIf { it.isNotEmpty() }
                ?.joinToString(prefix = " · ", separator = " · ")
                .orEmpty()
            Text(
                text = stringResource(R.string.battle_reward_item_format, title, suffix),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
