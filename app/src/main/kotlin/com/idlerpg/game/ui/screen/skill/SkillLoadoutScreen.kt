package com.idlerpg.game.ui.screen.skill

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.idlerpg.game.R
import com.idlerpg.game.core.id.ContentId
import com.idlerpg.game.domain.command.CommandRejectionCode
import com.idlerpg.game.presentation.intent.SkillLoadoutUiIntent
import com.idlerpg.game.presentation.model.SkillEvolutionBranchUiState
import com.idlerpg.game.presentation.model.SkillLoadoutFeedbackKind
import com.idlerpg.game.presentation.model.SkillLoadoutFeedbackUiState
import com.idlerpg.game.presentation.model.SkillLoadoutSkillUiState
import com.idlerpg.game.presentation.model.SkillLoadoutSlotUiState
import com.idlerpg.game.presentation.model.SkillLoadoutUiState
import com.idlerpg.game.ui.component.SkillArtwork
import com.idlerpg.game.ui.component.premium.GameButton
import com.idlerpg.game.ui.component.premium.GameCard
import com.idlerpg.game.ui.component.premium.GameChoiceButton
import com.idlerpg.game.ui.component.premium.GameDivider
import com.idlerpg.game.ui.component.premium.GameMetricChip
import com.idlerpg.game.ui.component.premium.GameOutlinedButton
import com.idlerpg.game.ui.component.premium.GameSectionHeader
import com.idlerpg.game.ui.component.premium.GameStatusPill
import com.idlerpg.game.ui.component.skillRoleResource
import com.idlerpg.game.ui.content.stringResId
import com.idlerpg.game.ui.motion.eventFeedbackPulse
import com.idlerpg.game.ui.theme.ArcaneViolet
import com.idlerpg.game.ui.theme.ErrorRose
import com.idlerpg.game.ui.theme.ObsidianOutline
import com.idlerpg.game.ui.theme.ObsidianSurface1
import com.idlerpg.game.ui.theme.ObsidianSurface2
import com.idlerpg.game.ui.theme.ObsidianSurface3
import com.idlerpg.game.ui.theme.PanelHighlight
import com.idlerpg.game.ui.theme.PositiveGreen
import com.idlerpg.game.ui.theme.ResonanceTeal
import com.idlerpg.game.ui.theme.ResourceGold
import com.idlerpg.game.ui.theme.TextSecondary

/**
 * Focused skill workshop. The composable only projects state and dispatches loadout intents;
 * ordering, unlocks, cooldowns, and evolution validation remain authoritative elsewhere.
 */
@Composable
fun SkillLoadoutScreen(
    state: SkillLoadoutUiState,
    onIntent: (SkillLoadoutUiIntent) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedIdValue by rememberSaveable { mutableStateOf<String?>(null) }
    val selectedId = selectedIdValue?.let(ContentId::parse)
    val firstEquippedId = state.slots.firstOrNull { it.skill != null }?.skill?.skillId
    val allSkills = state.slots.mapNotNull { it.skill } + state.availableSkills
    val focusedId = selectedId
        ?.takeIf { id -> allSkills.any { it.skillId == id } }
        ?: firstEquippedId
    val selected = allSkills.firstOrNull { it.skillId == focusedId }
    val selectedSlot = state.slots.firstOrNull { it.skill?.skillId == focusedId }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 12.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        GameSectionHeader(
            eyebrow = stringResource(R.string.nav_gear),
            title = stringResource(R.string.loadout_title),
            subtitle = stringResource(R.string.loadout_tap_hint),
            trailing = {
                GameOutlinedButton(onClick = onClose) {
                    Text(stringResource(R.string.loadout_close))
                }
            }
        )

        LoadoutHeaderRail(state = state)
        Text(
            "Basic Attack is always available. Slots define the build; Auto Battle follows your rules.",
            style = MaterialTheme.typography.bodySmall,
            color = TextSecondary
        )

        state.feedback?.let { feedback ->
            LoadoutFeedbackCard(state = state, feedback = feedback)
        }

        LoadoutSlotsPane(
            state = state,
            selectedId = focusedId,
            onSelect = { selectedIdValue = it?.value }
        )

        if (selected != null) {
            SkillFocusPane(
                skill = selected,
                equippedSlot = selectedSlot,
                capacityFull = state.capacityFull,
                onIntent = onIntent
            )
        } else {
            EmptyFocusPane()
        }

        AvailableSkillsPane(
            state = state,
            selectedId = focusedId,
            onSelect = { selectedIdValue = it.value }
        )

        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun LoadoutHeaderRail(state: SkillLoadoutUiState) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        GameMetricChip(
            label = stringResource(R.string.loadout_equipped_slots),
            value = stringResource(R.string.loadout_capacity_format, state.equippedCount, state.capacity),
            accent = if (state.capacityFull) ResourceGold else ResonanceTeal,
            modifier = Modifier.weight(1f)
        )
        GameMetricChip(
            label = stringResource(R.string.loadout_available_skills),
            value = state.availableSkills.size.toString(),
            accent = ArcaneViolet,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun LoadoutSlotsPane(
    state: SkillLoadoutUiState,
    selectedId: ContentId?,
    onSelect: (ContentId?) -> Unit
) {
    GameCard(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = ObsidianSurface2),
        border = BorderStroke(1.dp, PanelHighlight.copy(alpha = 0.52f))
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(7.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    Text(
                        text = stringResource(R.string.loadout_equipped_slots),
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = stringResource(R.string.loadout_active_slots_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
                GameStatusPill(
                    text = stringResource(
                        if (state.capacityFull) R.string.loadout_status_full
                        else R.string.loadout_status_ready
                    ),
                    accent = if (state.capacityFull) ResourceGold else ResonanceTeal
                )
            }
            GameDivider()
            if (state.slots.isEmpty()) {
                Text(
                    text = stringResource(R.string.loadout_empty_slot_detail),
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            } else {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    state.slots.forEach { slot ->
                        LoadoutSlotTile(
                            slot = slot,
                            selected = slot.skill?.skillId == selectedId,
                            onClick = { onSelect(slot.skill?.skillId) }
                        )
                    }
                }
            }
            Text(
                text = if (state.capacityFull) {
                    stringResource(R.string.loadout_capacity_full)
                } else {
                    stringResource(
                        R.string.loadout_capacity_remaining_format,
                        (state.capacity - state.equippedCount).coerceAtLeast(0)
                    )
                },
                style = MaterialTheme.typography.bodySmall,
                color = if (state.capacityFull) ResourceGold else TextSecondary
            )
        }
    }
}

@Composable
private fun LoadoutSlotTile(
    slot: SkillLoadoutSlotUiState,
    selected: Boolean,
    onClick: () -> Unit
) {
    val skill = slot.skill
    val skillTitle = skill?.let { stringResource(it.titleStringKey.stringResId()) }
    GameCard(
        onClick = onClick,
        enabled = skill != null,
        modifier = Modifier.width(84.dp),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(
            if (selected) 2.dp else 1.dp,
            if (selected) ResourceGold else ObsidianOutline.copy(alpha = 0.58f)
        ),
        accent = if (selected) ResourceGold else ArcaneViolet,
        colors = CardDefaults.cardColors(
            containerColor = if (selected) ObsidianSurface3 else ObsidianSurface1
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            if (skill == null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1.05f)
                        .clip(RoundedCornerShape(9.dp))
                        .background(ObsidianSurface3.copy(alpha = 0.44f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(R.string.loadout_empty_slot),
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary
                    )
                }
            } else {
                SkillArtwork(
                    skill.skillId,
                    Modifier
                        .fillMaxWidth()
                        .aspectRatio(1.05f)
                        .clip(RoundedCornerShape(9.dp))
                        .semantics {
                            contentDescription = skillTitle.orEmpty()
                        }
                )
                Text(
                    text = stringResource(skill.titleStringKey.stringResId()),
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                    maxLines = 2,
                    overflow = TextOverflow.Clip
                )
            }
            Text(
                text = stringResource(R.string.loadout_slot_format, slot.index + 1),
                style = MaterialTheme.typography.labelSmall,
                color = if (selected) ResourceGold else TextSecondary
            )
        }
    }
}

@Composable
private fun SkillFocusPane(
    skill: SkillLoadoutSkillUiState,
    equippedSlot: SkillLoadoutSlotUiState?,
    capacityFull: Boolean,
    onIntent: (SkillLoadoutUiIntent) -> Unit
) {
    GameCard(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = ObsidianSurface1),
        border = BorderStroke(1.dp, ArcaneViolet.copy(alpha = 0.46f)),
        accent = ArcaneViolet
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                val compact = maxWidth < 360.dp
                if (compact) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        FocusArtwork(skill)
                        SkillSummary(skill)
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        FocusArtwork(skill, Modifier.width(108.dp))
                        SkillSummary(skill, Modifier.weight(1f))
                    }
                }
            }

            SkillFacts(skill)
            SkillTechnicalDetails(skill)
            SkillInvestmentActions(skill, onIntent)
            SkillEvolutionChoices(skill, onIntent)
            GameDivider()
            SkillLoadoutActions(
                skill = skill,
                equippedSlot = equippedSlot,
                capacityFull = capacityFull,
                onIntent = onIntent
            )
        }
    }
}

@Composable
private fun FocusArtwork(skill: SkillLoadoutSkillUiState, modifier: Modifier = Modifier) {
    val skillTitle = stringResource(skill.titleStringKey.stringResId())
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(7.dp)
    ) {
        SkillArtwork(
            skill.skillId,
            Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(RoundedCornerShape(14.dp))
                .semantics {
                    contentDescription = skillTitle
                }
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            skill.affinityIds.take(3).forEach { affinityId ->
                com.idlerpg.game.ui.theme.AffinityIcon(
                    affinityId = affinityId,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
            }
            Text(
                text = stringResource(skillRoleResource(skill.skillId)),
                style = MaterialTheme.typography.labelMedium,
                color = ResonanceTeal,
                maxLines = 1,
                overflow = TextOverflow.Clip
            )
        }
    }
}

@Composable
private fun SkillSummary(skill: SkillLoadoutSkillUiState, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(7.dp)
    ) {
        Text(
            text = stringResource(skill.titleStringKey.stringResId()),
            style = MaterialTheme.typography.titleLarge,
            maxLines = 2,
            overflow = TextOverflow.Clip
        )
        Text(
            text = stringResource(skill.descriptionStringKey.stringResId()),
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary
        )
        GameStatusPill(
            text = if (skill.equippedIndex != null) {
                stringResource(R.string.loadout_slot_format, skill.equippedIndex + 1)
            } else if (!skill.unlock.unlocked) {
                stringResource(R.string.loadout_locked)
            } else {
                stringResource(R.string.loadout_status_ready)
            },
            accent = when {
                !skill.unlock.unlocked -> ResourceGold
                skill.equippedIndex != null -> ResonanceTeal
                else -> ArcaneViolet
            }
        )
    }
}

@Composable
private fun SkillFacts(skill: SkillLoadoutSkillUiState) {
    Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
        Text(
            text = stringResource(
                R.string.loadout_timing_format,
                skill.cooldownMillis,
                skill.recoveryMillis
            ),
            style = MaterialTheme.typography.bodySmall,
            color = TextSecondary
        )
        if (skill.queued) {
            Text(
                text = stringResource(R.string.loadout_queued_unequip_warning),
                style = MaterialTheme.typography.bodySmall,
                color = ResourceGold
            )
        }
    }
}

@Composable
private fun SkillTechnicalDetails(skill: SkillLoadoutSkillUiState) {
    var expanded by rememberSaveable(skill.skillId.value) { mutableStateOf(false) }
    Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
        GameOutlinedButton(
            onClick = { expanded = !expanded },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(if (expanded) "Hide technical details" else "Show technical details")
        }
        if (expanded) {
            GameCard(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = ObsidianSurface2)
            ) {
                Column(
                    modifier = Modifier.padding(11.dp),
                    verticalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    skill.technicalDetails.forEach { detail ->
                        Text(
                            text = detail,
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SkillInvestmentActions(
    skill: SkillLoadoutSkillUiState,
    onIntent: (SkillLoadoutUiIntent) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
        Text(
            text = "Rank " + skill.rank + "/" + (skill.maxRank ?: "∞") +
                " · Mastery " + skill.mastery + "/" + skill.masteryCap +
                " · Refinement " + skill.refinement + "/" + skill.refinementCap,
            style = MaterialTheme.typography.bodySmall,
            color = TextSecondary
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            GameOutlinedButton(
                onClick = { onIntent(SkillLoadoutUiIntent.UpgradeRank(skill.skillId)) },
                enabled = skill.canUpgradeRank,
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    "Rank +1\n" + skill.rankUpgradeCostDisplay,
                    maxLines = 2,
                    style = MaterialTheme.typography.labelSmall,
                    textAlign = TextAlign.Center
                )
            }
            GameOutlinedButton(
                onClick = { onIntent(SkillLoadoutUiIntent.UpgradeMastery(skill.skillId)) },
                enabled = skill.canUpgradeMastery,
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    "Mastery +1\n" + skill.masteryUpgradeCostDisplay,
                    maxLines = 2,
                    style = MaterialTheme.typography.labelSmall,
                    textAlign = TextAlign.Center
                )
            }
            GameOutlinedButton(
                onClick = { onIntent(SkillLoadoutUiIntent.Refine(skill.skillId)) },
                enabled = skill.canRefine,
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    "Refine +1\n" + skill.refinementCostDisplay,
                    maxLines = 2,
                    style = MaterialTheme.typography.labelSmall,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun SkillLoadoutActions(
    skill: SkillLoadoutSkillUiState,
    equippedSlot: SkillLoadoutSlotUiState?,
    capacityFull: Boolean,
    onIntent: (SkillLoadoutUiIntent) -> Unit
) {
    if (equippedSlot == null) {
        if (!skill.unlock.unlocked) {
            SkillUnlockRequirements(skill)
        } else if (capacityFull) {
            Text(
                text = stringResource(R.string.loadout_capacity_full_for_skill),
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
            )
        } else {
            Text(
                text = stringResource(R.string.loadout_ready_to_equip),
                style = MaterialTheme.typography.bodySmall,
                color = ResonanceTeal
            )
        }
        GameButton(
            onClick = { onIntent(SkillLoadoutUiIntent.Equip(skill.skillId)) },
            enabled = skill.canEquip,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(stringResource(R.string.loadout_equip))
        }
        return
    }

    Text(
        text = stringResource(R.string.loadout_slot_format, equippedSlot.index + 1),
        style = MaterialTheme.typography.labelLarge,
        color = ResonanceTeal
    )
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        GameOutlinedButton(
            onClick = {
                onIntent(
                    SkillLoadoutUiIntent.Move(
                        fromIndex = equippedSlot.index,
                        toIndex = equippedSlot.index - 1
                    )
                )
            },
            enabled = skill.canMoveEarlier,
            modifier = Modifier.weight(1f)
        ) {
            Text(stringResource(R.string.loadout_move_earlier), maxLines = 1, overflow = TextOverflow.Clip)
        }
        GameOutlinedButton(
            onClick = {
                onIntent(
                    SkillLoadoutUiIntent.Move(
                        fromIndex = equippedSlot.index,
                        toIndex = equippedSlot.index + 1
                    )
                )
            },
            enabled = skill.canMoveLater,
            modifier = Modifier.weight(1f)
        ) {
            Text(stringResource(R.string.loadout_move_later), maxLines = 1, overflow = TextOverflow.Clip)
        }
    }
    GameButton(
        onClick = { onIntent(SkillLoadoutUiIntent.Unequip(skill.skillId)) },
        enabled = skill.canUnequip,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(stringResource(R.string.loadout_unequip))
    }
}

@Composable
private fun AvailableSkillsPane(
    state: SkillLoadoutUiState,
    selectedId: ContentId?,
    onSelect: (ContentId) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = stringResource(R.string.loadout_available_skills),
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = stringResource(
                    R.string.loadout_capacity_format,
                    state.equippedCount,
                    state.capacity
                ),
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
            )
        }
        if (state.availableSkills.isEmpty()) {
            EmptySkillsPane()
        } else {
            state.availableSkills.forEach { skill ->
                AvailableSkillRow(
                    skill = skill,
                    selected = skill.skillId == selectedId,
                    capacityFull = state.capacityFull,
                    onClick = { onSelect(skill.skillId) }
                )
            }
        }
    }
}

@Composable
private fun AvailableSkillRow(
    skill: SkillLoadoutSkillUiState,
    selected: Boolean,
    capacityFull: Boolean,
    onClick: () -> Unit
) {
    val skillTitle = stringResource(skill.titleStringKey.stringResId())
    GameCard(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 68.dp),
        border = BorderStroke(
            if (selected) 2.dp else 1.dp,
            if (selected) ResourceGold else ObsidianOutline.copy(alpha = 0.48f)
        ),
        accent = if (selected) ResourceGold else ResonanceTeal,
        colors = CardDefaults.cardColors(
            containerColor = if (selected) ObsidianSurface2 else ObsidianSurface1
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            SkillArtwork(
                skill.skillId,
                Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .semantics {
                        contentDescription = skillTitle
                    }
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Text(
                    text = stringResource(skill.titleStringKey.stringResId()),
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Clip
                )
                Text(
                    text = stringResource(skill.descriptionStringKey.stringResId()),
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Clip
                )
                Text(
                    text = stringResource(
                        R.string.skill_cooldown_seconds,
                        skill.cooldownMillis / 1000f
                    ),
                    style = MaterialTheme.typography.labelSmall,
                    color = ResonanceTeal
                )
            }
            GameStatusPill(
                text = when {
                    !skill.unlock.unlocked -> stringResource(R.string.loadout_locked)
                    capacityFull -> stringResource(R.string.loadout_status_full)
                    else -> stringResource(R.string.loadout_status_ready)
                },
                accent = when {
                    !skill.unlock.unlocked -> ResourceGold
                    capacityFull -> TextSecondary
                    else -> ResonanceTeal
                }
            )
        }
    }
}

@Composable
private fun EmptyFocusPane() {
    GameCard(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = ObsidianSurface1)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = stringResource(R.string.loadout_choose_skill_hint),
                style = MaterialTheme.typography.titleSmall
            )
            Text(
                text = stringResource(R.string.loadout_empty_slot_detail),
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
            )
        }
    }
}

@Composable
private fun EmptySkillsPane() {
    GameCard(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = ObsidianSurface1)
    ) {
        Text(
            text = stringResource(R.string.loadout_no_available_skills),
            modifier = Modifier.padding(14.dp),
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary
        )
    }
}

@Composable
private fun SkillUnlockRequirements(skill: SkillLoadoutSkillUiState) {
    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
        Text(
            text = stringResource(R.string.loadout_locked),
            style = MaterialTheme.typography.labelLarge,
            color = ResourceGold
        )
        skill.unlock.requiredPlayerLevel?.let { requiredLevel ->
            Text(
                text = stringResource(
                    R.string.loadout_unlock_player_level_format,
                    requiredLevel,
                    skill.unlock.currentPlayerLevel
                ),
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
            )
        }
        skill.unlock.masteryRequirements.forEach { requirement ->
            Text(
                text = stringResource(
                    R.string.loadout_unlock_mastery_format,
                    stringResource(requirement.titleStringKey.stringResId()),
                    requirement.requiredLevel,
                    requirement.currentLevel
                ),
                style = MaterialTheme.typography.bodySmall,
                color = if (requirement.met) PositiveGreen else TextSecondary
            )
        }
    }
}

@Composable
private fun SkillEvolutionChoices(
    skill: SkillLoadoutSkillUiState,
    onIntent: (SkillLoadoutUiIntent) -> Unit
) {
    if (skill.evolutions.isEmpty()) return
    var expanded by remember(skill.skillId) { mutableStateOf(false) }
    GameOutlinedButton(
        onClick = { expanded = !expanded },
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(stringResource(R.string.skill_evolutions_toggle))
    }
    if (!expanded) return
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        skill.evolutions.forEach { evolution ->
            EvolutionRow(evolution = evolution, skill = skill, onIntent = onIntent)
        }
    }
}

@Composable
private fun EvolutionRow(
    evolution: SkillEvolutionBranchUiState,
    skill: SkillLoadoutSkillUiState,
    onIntent: (SkillLoadoutUiIntent) -> Unit
) {
    GameCard(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (evolution.selected) ObsidianSurface3 else ObsidianSurface2
        ),
        border = BorderStroke(
            1.dp,
            if (evolution.selected) ResonanceTeal.copy(alpha = 0.72f)
            else ObsidianOutline.copy(alpha = 0.42f)
        )
    ) {
        Column(
            modifier = Modifier.padding(11.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = stringResource(evolution.titleStringKey.stringResId()),
                style = MaterialTheme.typography.titleSmall
            )
            Text(
                text = stringResource(evolution.descriptionStringKey.stringResId()),
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
            )
            Text(
                text = stringResource(
                    R.string.loadout_evolution_mastery,
                    stringResource(evolution.requiredAffinityTitleStringKey.stringResId()),
                    evolution.currentMasteryLevel,
                    evolution.requiredMasteryLevel
                ),
                style = MaterialTheme.typography.labelSmall,
                color = if (evolution.canSelect || evolution.selected) ResonanceTeal else TextSecondary
            )
            GameChoiceButton(
                selected = evolution.selected,
                onClick = {
                    onIntent(
                        SkillLoadoutUiIntent.SelectEvolution(
                            skill.skillId,
                            evolution.evolutionId
                        )
                    )
                },
                enabled = evolution.canSelect,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    stringResource(
                        if (evolution.selected) R.string.loadout_evolution_selected
                        else R.string.loadout_select_evolution
                    )
                )
            }
        }
    }
}

@Composable
private fun LoadoutFeedbackCard(
    state: SkillLoadoutUiState,
    feedback: SkillLoadoutFeedbackUiState
) {
    val skillTitle = feedback.skillId?.let { id ->
        state.slots.asSequence()
            .mapNotNull { it.skill }
            .plus(state.availableSkills.asSequence())
            .firstOrNull { it.skillId == id }
            ?.let { stringResource(it.titleStringKey.stringResId()) }
    } ?: stringResource(R.string.loadout_skill_generic)

    val message = when (feedback.kind) {
        SkillLoadoutFeedbackKind.COMMAND_REJECTED -> stringResource(
            R.string.loadout_feedback_rejected,
            loadoutRejectionMessage(feedback.rejectionCode)
        )
        SkillLoadoutFeedbackKind.EQUIPPED -> stringResource(
            R.string.loadout_feedback_equipped,
            skillTitle
        )
        SkillLoadoutFeedbackKind.UNEQUIPPED -> stringResource(
            R.string.loadout_feedback_unequipped,
            skillTitle
        )
        SkillLoadoutFeedbackKind.MOVED -> stringResource(
            R.string.loadout_feedback_moved,
            skillTitle,
            (feedback.toIndex ?: 0) + 1
        )
        SkillLoadoutFeedbackKind.EVOLVED -> stringResource(
            R.string.loadout_feedback_evolved,
            skillTitle
        )
    }

    GameCard(
        modifier = Modifier
            .fillMaxWidth()
            .eventFeedbackPulse(feedback.sequenceNumber),
        colors = CardDefaults.cardColors(
            containerColor = if (feedback.kind == SkillLoadoutFeedbackKind.COMMAND_REJECTED) {
                ErrorRose.copy(alpha = 0.16f)
            } else {
                ResonanceTeal.copy(alpha = 0.13f)
            }
        ),
        border = BorderStroke(
            1.dp,
            if (feedback.kind == SkillLoadoutFeedbackKind.COMMAND_REJECTED) {
                ErrorRose.copy(alpha = 0.55f)
            } else {
                ResonanceTeal.copy(alpha = 0.46f)
            }
        ),
        accent = if (feedback.kind == SkillLoadoutFeedbackKind.COMMAND_REJECTED) ErrorRose else ResonanceTeal
    ) {
        Text(
            text = message,
            modifier = Modifier.padding(12.dp),
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@Composable
private fun loadoutRejectionMessage(code: CommandRejectionCode?): String = when (code) {
    CommandRejectionCode.INVALID_ARGUMENT -> stringResource(R.string.rejection_invalid_argument)
    CommandRejectionCode.INVALID_STATE -> stringResource(R.string.rejection_invalid_state)
    CommandRejectionCode.UNKNOWN_CONTENT -> stringResource(R.string.rejection_unknown_content)
    CommandRejectionCode.LOCKED -> stringResource(R.string.rejection_locked)
    CommandRejectionCode.NOT_OWNED -> stringResource(R.string.rejection_not_owned)
    CommandRejectionCode.ALREADY_OWNED -> stringResource(R.string.rejection_already_owned)
    CommandRejectionCode.INSUFFICIENT_RESOURCE -> stringResource(R.string.rejection_insufficient_resource)
    CommandRejectionCode.CAPACITY_EXCEEDED -> stringResource(R.string.rejection_capacity_exceeded)
    CommandRejectionCode.COOLDOWN_ACTIVE -> stringResource(R.string.rejection_cooldown_active)
    CommandRejectionCode.NOT_READY -> stringResource(R.string.rejection_not_ready)
    CommandRejectionCode.ALREADY_CLAIMED -> stringResource(R.string.rejection_already_claimed)
    CommandRejectionCode.STALE_PREVIEW -> stringResource(R.string.rejection_stale_preview)
    CommandRejectionCode.UNSUPPORTED -> stringResource(R.string.rejection_unsupported)
    null -> stringResource(R.string.loadout_feedback_unknown)
}
