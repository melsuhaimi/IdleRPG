package com.idlerpg.game.ui.component.battle

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.idlerpg.game.R
import com.idlerpg.game.presentation.model.BattleEnemyUiState
import com.idlerpg.game.ui.component.premium.PremiumBadgeIcon
import com.idlerpg.game.ui.component.premium.PremiumImageProgressBar
import com.idlerpg.game.ui.component.premium.PremiumPanel
import com.idlerpg.game.ui.content.drawableResId
import com.idlerpg.game.ui.content.stringResId

@Composable
fun BattleEncounterCard(
    enemy: BattleEnemyUiState?,
    regionTitle: String,
    encounterTitle: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        EncounterBanner(
            frameResId = enemy?.encounterFrameAssetKey?.drawableResId()
                ?: R.drawable.frame_encounter_normal,
            regionTitle = regionTitle,
            encounterTitle = encounterTitle
        )

        if (enemy == null) {
            PremiumPanel(
                backgroundResId = R.drawable.panel_primary_premium,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = stringResource(R.string.battle_no_active_enemy),
                    style = MaterialTheme.typography.headlineSmall
                )
                Text(
                    text = stringResource(R.string.battle_no_active_enemy_detail),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            return@Column
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(292.dp)
        ) {
            Image(
                painter = painterResource(R.drawable.card_enemy_showcase_premium),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.FillBounds
            )

            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 18.dp, vertical = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .weight(1.08f)
                        .height(226.dp),
                    contentAlignment = Alignment.Center
                ) {
                    enemy.illustrationAssetKey?.let { illustration ->
                        Image(
                            painter = painterResource(illustration.drawableResId()),
                            contentDescription = stringResource(
                                R.string.a11y_enemy_illustration_format,
                                stringResource(enemy.titleStringKey.stringResId())
                            ),
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Fit
                        )
                    }
                    enemy.attackFxAssetKey?.let { fx ->
                        if ((enemy.nextAttackRemainingMillis ?: Long.MAX_VALUE) <= 350L) {
                            Image(
                                painter = painterResource(fx.drawableResId()),
                                contentDescription = null,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(22.dp)
                                    .alpha(0.76f),
                                contentScale = ContentScale.Fit
                            )
                        }
                    }
                }

                Column(
                    modifier = Modifier.weight(0.92f),
                    verticalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Image(
                            painter = painterResource(enemy.assetKey.drawableResId()),
                            contentDescription = null,
                            modifier = Modifier.size(34.dp),
                            contentScale = ContentScale.Fit
                        )
                        Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                            Text(
                                text = stringResource(enemy.titleStringKey.stringResId()),
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                text = stringResource(
                                    R.string.battle_enemy_tier_format,
                                    enemy.scalingTier
                                ),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        PremiumBadgeIcon(
                            drawableResId = R.drawable.badge_threat_premium,
                            contentDescriptionText = null,
                            size = 24.dp
                        )
                        Text(
                            text = stringResource(
                                R.string.battle_health_format,
                                enemy.currentHealthDisplay,
                                enemy.maximumHealthDisplay
                            ),
                            style = MaterialTheme.typography.labelMedium
                        )
                    }

                    PremiumImageProgressBar(
                        progressUnits = enemy.healthProgressUnits,
                        trackResId = R.drawable.bar_hp_track_premium,
                        fillResId = R.drawable.bar_hp_fill_premium,
                        contentDescriptionText = stringResource(
                            R.string.a11y_health_progress_format,
                            stringResource(enemy.titleStringKey.stringResId()),
                            enemy.currentHealthDisplay,
                            enemy.maximumHealthDisplay
                        ),
                        modifier = Modifier.fillMaxWidth(),
                        height = 28.dp
                    )

                    if (enemy.attackTitle != null &&
                        enemy.attackDamageDisplay != null &&
                        enemy.attackIntervalMillis != null
                    ) {
                        val affinityLabel = enemy.attackAffinityId
                            ?.let { affinityId ->
                                stringResource(com.idlerpg.game.ui.theme.affinityTitleResId(affinityId))
                            }
                            ?: "—"
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(5.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            PremiumBadgeIcon(
                                drawableResId = R.drawable.badge_affinity_premium,
                                contentDescriptionText = null,
                                size = 24.dp
                            )
                            Text(
                                text = enemy.attackTitle,
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.tertiary
                            )
                        }
                        Text(
                            text = stringResource(
                                R.string.battle_enemy_attack_profile_format,
                                enemy.attackTitle,
                                affinityLabel,
                                enemy.attackDamageDisplay,
                                enemy.attackIntervalMillis / 1000f
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        enemy.nextAttackRemainingMillis?.let { remaining ->
                            Text(
                                text = stringResource(
                                    R.string.battle_enemy_next_attack_format,
                                    remaining / 1000f
                                ),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }
                    }
                }
            }
        }

        if (enemy.mutations.isNotEmpty() || enemy.statuses.isNotEmpty()) {
            PremiumPanel(
                backgroundResId = R.drawable.panel_secondary_premium,
                modifier = Modifier.fillMaxWidth(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(
                    horizontal = 18.dp,
                    vertical = 14.dp
                )
            ) {
                if (enemy.mutations.isNotEmpty()) {
                    Text(
                        text = stringResource(R.string.battle_mutations),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.tertiary
                    )
                    enemy.mutations.forEach { mutation ->
                        Text(
                            text = "• ${stringResource(mutation.titleStringKey.stringResId())}",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
                if (enemy.statuses.isNotEmpty()) {
                    Text(
                        text = stringResource(R.string.battle_enemy_statuses),
                        style = MaterialTheme.typography.labelLarge
                    )
                    enemy.statuses.forEach { status ->
                        Text(
                            text = stringResource(
                                R.string.battle_status_format,
                                stringResource(status.titleStringKey.stringResId()),
                                status.stackCount,
                                status.remainingMillis
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EncounterBanner(
    frameResId: Int,
    regionTitle: String,
    encounterTitle: String
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(66.dp),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(frameResId),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.FillBounds
        )
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(1.dp)
        ) {
            Text(
                text = regionTitle.uppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.secondary
            )
            Text(
                text = encounterTitle,
                style = MaterialTheme.typography.titleMedium
            )
        }
    }
}

@Composable
fun BattleProgressBar(
    progressUnits: Int,
    contentDescription: String,
    modifier: Modifier = Modifier
) {
    PremiumImageProgressBar(
        progressUnits = progressUnits,
        trackResId = R.drawable.bar_xp_track_premium,
        fillResId = R.drawable.bar_xp_fill_premium,
        contentDescriptionText = contentDescription,
        modifier = modifier,
        height = 22.dp
    )
}
