package com.idlerpg.game.ui.component.battle

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.idlerpg.game.R
import com.idlerpg.game.core.id.ContentId
import com.idlerpg.game.presentation.model.BattleSkillReadinessUi
import com.idlerpg.game.presentation.model.BattleSkillUiState
import com.idlerpg.game.ui.component.SkillArtwork
import com.idlerpg.game.ui.component.premium.GameButton
import com.idlerpg.game.ui.component.premium.GameOutlinedButton
import com.idlerpg.game.ui.component.premium.PremiumPanel
import com.idlerpg.game.ui.content.stringResId
import com.idlerpg.game.ui.theme.ObsidianOutline
import com.idlerpg.game.ui.theme.ObsidianSurface1
import com.idlerpg.game.ui.theme.ObsidianSurface2
import com.idlerpg.game.ui.theme.ObsidianSurface3
import com.idlerpg.game.ui.theme.affinityVisualToken

@Composable
fun SkillActionBar(
    skills: List<BattleSkillUiState>,
    queuedSkillId: ContentId?,
    onQueueSkill: (ContentId) -> Unit,
    onClearQueuedSkill: () -> Unit,
    onOpenSkillLoadout: () -> Unit,
    compact: Boolean = false,
    modifier: Modifier = Modifier
) {
    PremiumPanel(
        backgroundResId = R.drawable.panel_secondary_premium,
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 7.dp),
        verticalSpacing = 5.dp
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.battle_combat_dock),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.secondary
                )
                Text(
                    text = stringResource(R.string.battle_queue_hint),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (queuedSkillId != null) {
                GameOutlinedButton(
                    onClick = onClearQueuedSkill,
                    contentPadding = PaddingValues(horizontal = 6.dp)
                ) {
                    Text(stringResource(R.string.battle_clear_queue))
                }
            }
            GameOutlinedButton(
                onClick = onOpenSkillLoadout,
                contentPadding = PaddingValues(horizontal = 6.dp)
            ) {
                Text(stringResource(R.string.battle_loadout_short))
            }
        }

        if (skills.isEmpty()) {
            GameButton(
                onClick = onOpenSkillLoadout,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.battle_manage_loadout))
            }
        } else {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(7.dp)
            ) {
                skills.take(4).forEach { skill ->
                    SkillTile(
                        skill = skill,
                        compact = compact,
                        onClick = { onQueueSkill(skill.skillId) },
                        modifier = Modifier.width(if (compact) 78.dp else 88.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun SkillTile(
    skill: BattleSkillUiState,
    compact: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val affinity = skill.affinityIds.firstOrNull()
    val token = affinity?.let(::affinityVisualToken)
    val skillName = stringResource(skill.titleStringKey.stringResId())
    val readiness = when {
        skill.queued -> stringResource(R.string.battle_skill_queued_next)
        skill.readiness == BattleSkillReadinessUi.READY ->
            stringResource(R.string.battle_skill_ready)
        skill.readiness == BattleSkillReadinessUi.COOLDOWN_WAIT ->
            stringResource(
                R.string.battle_skill_cooldown_seconds,
                skill.cooldownRemainingMillis / 1000f
            )
        skill.readiness == BattleSkillReadinessUi.RESOURCE_WAIT ->
            stringResource(R.string.battle_skill_resource_wait)
        else -> stringResource(R.string.battle_skill_unavailable)
    }
    val accent = token?.color ?: MaterialTheme.colorScheme.primary
    val tileColor = when {
        skill.queued -> accent.copy(alpha = 0.22f)
        skill.readiness == BattleSkillReadinessUi.READY -> ObsidianSurface2.copy(alpha = 0.96f)
        else -> ObsidianSurface1.copy(alpha = 0.86f)
    }
    val borderColor = when {
        skill.queued -> accent
        skill.readiness == BattleSkillReadinessUi.READY -> accent.copy(alpha = 0.62f)
        else -> ObsidianOutline.copy(alpha = 0.52f)
    }
    val progress = skill.cooldownProgressUnits.coerceIn(0, 10_000) / 10_000f

    Surface(
        modifier = modifier
            .heightIn(min = if (compact) 72.dp else 82.dp)
            .semantics {
                contentDescription = skillName + ". " + readiness
            }
            .clip(RoundedCornerShape(14.dp))
            .clickable(
                enabled = skill.queueAllowed,
                role = Role.Button,
                onClick = onClick
            ),
        shape = RoundedCornerShape(14.dp),
        color = tileColor,
        border = BorderStroke(
            width = if (skill.queued) 1.5.dp else 1.dp,
            color = borderColor
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 5.dp, vertical = 5.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(if (compact) 2.dp else 4.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(if (compact) 34.dp else 46.dp)
                    .clip(RoundedCornerShape(10.dp))
            ) {
                SkillArtwork(skill.skillId, Modifier.fillMaxSize())
                if (skill.queued) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .background(accent.copy(alpha = 0.92f))
                    ) {
                        Text(
                            text = stringResource(R.string.battle_skill_queued_next),
                            modifier = Modifier.align(Alignment.Center),
                            style = MaterialTheme.typography.labelSmall,
                            color = ObsidianSurface1
                        )
                    }
                }
            }
            Text(
                text = skillName,
                style = if (compact) {
                    MaterialTheme.typography.labelSmall
                } else {
                    MaterialTheme.typography.labelMedium
                },
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(3.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "R${skill.rank}",
                    style = MaterialTheme.typography.labelSmall,
                    color = accent
                )
                Text(
                    text = readiness,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (skill.queued || skill.readiness == BattleSkillReadinessUi.READY) {
                        accent
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                token?.let {
                    Icon(
                        painter = painterResource(it.iconResId),
                        contentDescription = null,
                        tint = it.color,
                        modifier = Modifier.size(13.dp)
                    )
                }
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp)
                    .clip(RoundedCornerShape(99.dp))
                    .background(ObsidianSurface3)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(progress)
                        .fillMaxHeight()
                        .background(accent)
                )
            }
        }
    }
}
