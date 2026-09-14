package com.idlerpg.game.ui.screen.doctrine

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.size
import com.idlerpg.game.R
import com.idlerpg.game.core.id.ContentId
import com.idlerpg.game.core.id.InstanceId
import com.idlerpg.game.domain.command.CommandRejectionCode
import com.idlerpg.game.domain.command.DoctrinePreset
import com.idlerpg.game.presentation.doctrine.DoctrineDraftAction
import com.idlerpg.game.presentation.doctrine.DoctrineDraftPath
import com.idlerpg.game.presentation.model.DoctrineActionKindUi
import com.idlerpg.game.presentation.model.DoctrineComparisonUi
import com.idlerpg.game.presentation.model.DoctrineConditionDraftUiState
import com.idlerpg.game.presentation.model.DoctrineConditionKindUi
import com.idlerpg.game.domain.definition.enemy.EnemyRole
import com.idlerpg.game.presentation.model.DoctrineDraftErrorUi
import com.idlerpg.game.presentation.model.DoctrineFeedbackKind
import com.idlerpg.game.presentation.model.DoctrineFeedbackUiState
import com.idlerpg.game.presentation.model.DoctrineRuleUiState
import com.idlerpg.game.presentation.model.DoctrineStatusTargetUi
import com.idlerpg.game.presentation.model.DoctrineUiState
import com.idlerpg.game.ui.content.stringResId
import com.idlerpg.game.ui.component.premium.GameButton
import com.idlerpg.game.ui.component.premium.GameCard
import com.idlerpg.game.ui.component.premium.GameChoiceButton
import com.idlerpg.game.ui.component.premium.GameOutlinedButton
import com.idlerpg.game.ui.component.premium.GameProgressBar
import com.idlerpg.game.ui.component.premium.GameSectionHeader
import com.idlerpg.game.ui.component.premium.GameStatusPill
import com.idlerpg.game.ui.theme.AffinityIcon
import com.idlerpg.game.ui.theme.affinityVisualToken
import com.idlerpg.game.ui.motion.eventFeedbackPulse

@Composable
fun DoctrineScreen(
    state: DoctrineUiState,
    onApplyPreset: (com.idlerpg.game.domain.command.DoctrinePreset) -> Unit,
    onSetDoctrineEnabled: (Boolean) -> Unit,
    onSetRuleEnabled: (InstanceId, Boolean) -> Unit,
    onMoveRule: (InstanceId, Int) -> Unit,
    onRemoveRule: (InstanceId) -> Unit,
    onBeginAddRule: () -> Unit,
    onBeginEditRule: (InstanceId) -> Unit,
    onDraftAction: (DoctrineDraftAction) -> Unit,
    onCommitDraft: () -> Unit,
    onCancelDraft: () -> Unit,
    modifier: Modifier = Modifier
) {
    var pendingPresetName by rememberSaveable { mutableStateOf<String?>(null) }

    pendingPresetName?.let { presetName ->
        DoctrinePreset.values().firstOrNull { it.name == presetName }?.let { preset ->
            AlertDialog(
                onDismissRequest = { pendingPresetName = null },
                title = { Text(stringResource(R.string.doctrine_preset_confirm_title)) },
                text = {
                    Text(
                        stringResource(
                            R.string.doctrine_preset_confirm_message,
                            presetLabel(preset)
                        )
                    )
                },
                confirmButton = {
                    GameButton(
                        onClick = {
                            pendingPresetName = null
                            onApplyPreset(preset)
                        }
                    ) { Text(stringResource(R.string.doctrine_preset_confirm_action)) }
                },
                dismissButton = {
                    TextButton(onClick = { pendingPresetName = null }) {
                        Text(stringResource(R.string.doctrine_cancel_edit))
                    }
                }
            )
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        GameSectionHeader(
            eyebrow = stringResource(R.string.nav_doctrine),
            title = stringResource(R.string.doctrine_title),
            subtitle = stringResource(R.string.doctrine_first_match_note)
        )
        PriorityCard(state)
        Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            DoctrinePreset.values().forEach { preset ->
                GameOutlinedButton(
                    onClick = {
                        if (state.rules.isEmpty()) {
                            onApplyPreset(preset)
                        } else {
                            pendingPresetName = preset.name
                        }
                    },
                    enabled = state.draft == null && !state.mutationPending
                ) {
                    Text(presetLabel(preset))
                }
            }
        }

        state.feedback?.let { DoctrineFeedbackCard(state, it) }

        DoctrineHeader(
            state = state,
            onSetDoctrineEnabled = onSetDoctrineEnabled,
            onBeginAddRule = onBeginAddRule
        )

        if (state.mutationPending) {
            Text(
                text = stringResource(R.string.doctrine_command_pending),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.secondary
            )
        }

        if (state.rules.isEmpty()) {
            GameCard(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = stringResource(R.string.doctrine_no_rules),
                        style = MaterialTheme.typography.titleSmall
                    )
                    Text(
                        text = stringResource(R.string.doctrine_first_match_note),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    GameButton(
                        onClick = onBeginAddRule,
                        enabled = !state.capacityFull && state.draft == null && !state.mutationPending,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            if (state.capacityFull) {
                                stringResource(R.string.doctrine_capacity_full)
                            } else {
                                stringResource(R.string.doctrine_add_rule)
                            }
                        )
                    }
                }
            }
        } else {
            state.rules.forEach { rule ->
                DoctrineRuleCard(
                    state = state,
                    rule = rule,
                    onSetRuleEnabled = onSetRuleEnabled,
                    onMoveRule = onMoveRule,
                    onRemoveRule = onRemoveRule,
                    onBeginEditRule = onBeginEditRule
                )
            }
        }

        if (state.draft != null) {
            DoctrineDraftEditor(
                state = state,
                onDraftAction = onDraftAction,
                onCommitDraft = onCommitDraft,
                onCancelDraft = onCancelDraft
            )
        }

        ResonanceCard(state)
        ConvergenceSection(state)
    }
}

@Composable
private fun PriorityCard(state: DoctrineUiState) {
    GameCard(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        ),
        accent = MaterialTheme.colorScheme.primary
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = stringResource(R.string.doctrine_priority_title),
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        text = stringResource(
                            R.string.doctrine_capacity_format,
                            state.ruleCount,
                            state.ruleCapacity
                        ),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.72f)
                    )
                }
                GameStatusPill(
                    text = if (state.enabled) {
                        stringResource(R.string.doctrine_enabled)
                    } else {
                        stringResource(R.string.doctrine_disabled)
                    },
                    accent = MaterialTheme.colorScheme.secondary
                )
            }
            state.queuedManualSkillTitleStringKey?.let { queued ->
                Text(
                    text = stringResource(
                        R.string.doctrine_priority_manual_queued_format,
                        stringResource(queued.stringResId())
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
    }
}

@Composable
private fun ResonanceCard(state: DoctrineUiState) {
    GameCard(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        accent = MaterialTheme.colorScheme.secondary
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = stringResource(R.string.doctrine_resonance_title),
                style = MaterialTheme.typography.titleMedium
            )
            state.resonance.forEach { affinity ->
                val token = affinityVisualToken(affinity.affinityId)
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            AffinityIcon(
                                affinityId = affinity.affinityId,
                                contentDescription = stringResource(affinity.titleStringKey.stringResId()),
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = stringResource(affinity.titleStringKey.stringResId()),
                                style = MaterialTheme.typography.labelLarge,
                                color = token.color,
                                modifier = Modifier.padding(start = 6.dp)
                            )
                        }
                        Text(
                            text = stringResource(
                                R.string.doctrine_resonance_charge_format,
                                affinity.chargeDisplay,
                                affinity.capDisplay
                            ),
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                    GameProgressBar(
                        progressUnits = affinity.chargeProgressUnits,
                        accent = token.color,
                        modifier = Modifier.fillMaxWidth(),
                        contentDescriptionText = stringResource(
                            R.string.doctrine_resonance_charge_format,
                            affinity.chargeDisplay,
                            affinity.capDisplay
                        )
                    )
                }
            }

            Text(
                text = stringResource(
                    R.string.doctrine_sequence_title_format,
                    state.resonanceSequence.size,
                    state.sequenceCapacity
                ),
                style = MaterialTheme.typography.titleSmall
            )
            if (state.resonanceSequence.isEmpty()) {
                Text(
                    text = stringResource(R.string.doctrine_sequence_empty),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    state.resonanceSequence.forEachIndexed { index, affinityId ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "${index + 1}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            AffinityIcon(
                                affinityId = affinityId,
                                contentDescription = null,
                                modifier = Modifier.padding(start = 2.dp).size(18.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DoctrineHeader(
    state: DoctrineUiState,
    onSetDoctrineEnabled: (Boolean) -> Unit,
    onBeginAddRule: () -> Unit
) {
    GameCard(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        accent = if (state.enabled) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.outline
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = stringResource(R.string.doctrine_rules_title),
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = stringResource(
                            R.string.doctrine_capacity_format,
                            state.ruleCount,
                            state.ruleCapacity
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    GameStatusPill(
                        text = if (state.enabled) {
                            stringResource(R.string.doctrine_enabled)
                        } else {
                            stringResource(R.string.doctrine_disabled)
                        },
                        accent = if (state.enabled) {
                            MaterialTheme.colorScheme.secondary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                    Switch(
                        checked = state.enabled,
                        onCheckedChange = onSetDoctrineEnabled,
                        enabled = !state.mutationPending
                    )
                }
            }
            Text(
                text = stringResource(R.string.doctrine_first_match_note),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            GameButton(
                onClick = onBeginAddRule,
                enabled = !state.capacityFull && state.draft == null && !state.mutationPending
            ) {
                Text(
                    if (state.capacityFull) {
                        stringResource(R.string.doctrine_capacity_full)
                    } else {
                        stringResource(R.string.doctrine_add_rule)
                    }
                )
            }
        }
    }
}

@Composable
private fun DoctrineRuleCard(
    state: DoctrineUiState,
    rule: DoctrineRuleUiState,
    onSetRuleEnabled: (InstanceId, Boolean) -> Unit,
    onMoveRule: (InstanceId, Int) -> Unit,
    onRemoveRule: (InstanceId) -> Unit,
    onBeginEditRule: (InstanceId) -> Unit
) {
    GameCard(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (rule.selectedByLatestFeedback) {
                MaterialTheme.colorScheme.secondaryContainer
            } else {
                MaterialTheme.colorScheme.surface
            }
        ),
        accent = if (rule.selectedByLatestFeedback) {
            MaterialTheme.colorScheme.secondary
        } else if (rule.enabled) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.outline
        }
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.doctrine_rule_priority_format, rule.index + 1),
                    style = MaterialTheme.typography.titleSmall
                )
                GameStatusPill(
                    text = if (rule.enabled) {
                        stringResource(R.string.doctrine_rule_enabled)
                    } else {
                        stringResource(R.string.doctrine_rule_disabled)
                    },
                    accent = if (rule.enabled) {
                        MaterialTheme.colorScheme.secondary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
            }

            Text(
                text = stringResource(
                    R.string.doctrine_when_format,
                    conditionSummaryLine(state, rule.condition)
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.doctrine_rule_arrow),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.secondary
                )
                Text(
                    text = actionLabel(state, rule.action.kind, rule.action.skillId),
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            if (rule.hasUnequippedSkillReference) {
                Text(
                    text = stringResource(R.string.doctrine_rule_skill_reference_warning),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }

            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                GameOutlinedButton(
                    onClick = { onMoveRule(rule.ruleId, rule.index - 1) },
                    enabled = rule.canMoveEarlier && !state.mutationPending
                ) {
                    Text(stringResource(R.string.doctrine_move_earlier))
                }
                GameOutlinedButton(
                    onClick = { onMoveRule(rule.ruleId, rule.index + 1) },
                    enabled = rule.canMoveLater && !state.mutationPending
                ) {
                    Text(stringResource(R.string.doctrine_move_later))
                }
                GameOutlinedButton(
                    onClick = { onSetRuleEnabled(rule.ruleId, !rule.enabled) },
                    enabled = !state.mutationPending
                ) {
                    Text(
                        if (rule.enabled) {
                            stringResource(R.string.doctrine_disable_rule)
                        } else {
                            stringResource(R.string.doctrine_enable_rule)
                        }
                    )
                }
                GameOutlinedButton(
                    onClick = { onBeginEditRule(rule.ruleId) },
                    enabled = state.draft == null && !state.mutationPending
                ) {
                    Text(stringResource(R.string.doctrine_edit_rule))
                }
                GameOutlinedButton(
                    onClick = { onRemoveRule(rule.ruleId) },
                    enabled = state.draft == null && !state.mutationPending
                ) {
                    Text(stringResource(R.string.doctrine_remove_rule))
                }
            }
        }
    }
}

@Composable
private fun DoctrineDraftEditor(
    state: DoctrineUiState,
    onDraftAction: (DoctrineDraftAction) -> Unit,
    onCommitDraft: () -> Unit,
    onCancelDraft: () -> Unit
) {
    val draft = state.draft ?: return
    GameCard(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer
        ),
        accent = MaterialTheme.colorScheme.tertiary
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = if (draft.ruleId == null) {
                    stringResource(R.string.doctrine_editor_add_title)
                } else {
                    stringResource(R.string.doctrine_editor_edit_title)
                },
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onTertiaryContainer
            )
            Text(
                text = stringResource(
                    R.string.doctrine_editor_depth_limit_format,
                    state.conditionMaxDepth
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onTertiaryContainer
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Switch(
                    checked = draft.enabled,
                    onCheckedChange = {
                        onDraftAction(DoctrineDraftAction.SetDraftEnabled(it))
                    }
                )
                Text(
                    text = stringResource(R.string.doctrine_editor_rule_enabled),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onTertiaryContainer
                )
            }

            ConditionEditor(
                state = state,
                node = draft.condition,
                path = DoctrineDraftPath(),
                depth = 1,
                onDraftAction = onDraftAction
            )

            ActionEditor(
                state = state,
                onDraftAction = onDraftAction
            )

            draft.localError?.let { error ->
                Text(
                    text = draftErrorLabel(error),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }

            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                GameButton(
                    onClick = onCommitDraft,
                    enabled = !state.mutationPending
                ) {
                    Text(stringResource(R.string.doctrine_save_rule))
                }
                GameOutlinedButton(
                    onClick = onCancelDraft,
                    enabled = !state.mutationPending
                ) {
                    Text(stringResource(R.string.doctrine_cancel_edit))
                }
            }
        }
    }
}

@Composable
private fun ConditionEditor(
    state: DoctrineUiState,
    node: DoctrineConditionDraftUiState,
    path: DoctrineDraftPath,
    depth: Int,
    onDraftAction: (DoctrineDraftAction) -> Unit
) {
    GameCard(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        accent = MaterialTheme.colorScheme.primary
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = stringResource(R.string.doctrine_condition_depth_format, depth),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.secondary
            )

            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                DoctrineConditionKindUi.values().forEach { kind ->
                    val structuralBlocked = depth >= state.conditionMaxDepth &&
                        (kind == DoctrineConditionKindUi.ALL ||
                            kind == DoctrineConditionKindUi.ANY ||
                            kind == DoctrineConditionKindUi.NOT)
                    GameChoiceButton(
                        selected = node.kind == kind,
                        onClick = {
                            onDraftAction(
                                DoctrineDraftAction.SetConditionKind(path, kind)
                            )
                        },
                        enabled = !structuralBlocked
                    ) { Text(conditionKindLabel(kind)) }
                }
            }

            when (node.kind) {
                DoctrineConditionKindUi.ALL,
                DoctrineConditionKindUi.ANY -> {
                    node.children.forEachIndexed { index, child ->
                        ConditionEditor(
                            state = state,
                            node = child,
                            path = DoctrineDraftPath(path.indices + index),
                            depth = depth + 1,
                            onDraftAction = onDraftAction
                        )
                        if (node.children.size > 1) {
                            GameOutlinedButton(
                                onClick = {
                                    onDraftAction(
                                        DoctrineDraftAction.RemoveChild(path, index)
                                    )
                                }
                            ) {
                                Text(stringResource(R.string.doctrine_remove_condition))
                            }
                        }
                    }
                    GameButton(
                        onClick = {
                            onDraftAction(DoctrineDraftAction.AddChild(path))
                        },
                        enabled = depth < state.conditionMaxDepth
                    ) {
                        Text(stringResource(R.string.doctrine_add_condition))
                    }
                }
                DoctrineConditionKindUi.NOT -> {
                    val child = node.children.firstOrNull()
                    if (child != null) {
                        ConditionEditor(
                            state = state,
                            node = child,
                            path = DoctrineDraftPath(path.indices + 0),
                            depth = depth + 1,
                            onDraftAction = onDraftAction
                        )
                    }
                }
                DoctrineConditionKindUi.PLAYER_HEALTH_PERCENT,
                DoctrineConditionKindUi.ENEMY_HEALTH_PERCENT -> {
                    ComparisonSelector(node, path, onDraftAction)
                    StepperRow(
                        label = stringResource(
                            if (node.kind == DoctrineConditionKindUi.PLAYER_HEALTH_PERCENT) R.string.doctrine_player_hp_percent_format else R.string.doctrine_enemy_hp_percent_format,
                            node.enemyHealthPercent
                        ),
                        onDecrease = {
                            onDraftAction(
                                DoctrineDraftAction.AdjustEnemyHealthPercent(path, -5)
                            )
                        },
                        onIncrease = {
                            onDraftAction(
                                DoctrineDraftAction.AdjustEnemyHealthPercent(path, 5)
                            )
                        },
                        decreaseEnabled = node.enemyHealthPercent > 0,
                        increaseEnabled = node.enemyHealthPercent < 100
                    )
                }
                DoctrineConditionKindUi.ENEMY_COUNT -> { ComparisonSelector(node, path, onDraftAction); StepperRow(stringResource(R.string.doctrine_enemy_count_format, node.resonanceAmount), { onDraftAction(DoctrineDraftAction.AdjustResonanceAmount(path, -1L)) }, { onDraftAction(DoctrineDraftAction.AdjustResonanceAmount(path, 1L)) }, node.resonanceAmount > 1L, node.resonanceAmount < 5L) }
                DoctrineConditionKindUi.ENEMY_ROLE_PRESENT -> Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) { EnemyRole.values().forEach { role -> GameChoiceButton(selected = node.enemyRole == role, onClick = { onDraftAction(DoctrineDraftAction.SetEnemyRole(path, role)) }) { Text(role.name.lowercase().replaceFirstChar(Char::uppercase)) } } }
                DoctrineConditionKindUi.ELITE_PRESENT -> Text(stringResource(R.string.doctrine_elite_present_help))
                DoctrineConditionKindUi.BOSS_PRESENT -> Text(stringResource(R.string.doctrine_boss_present_help))
                DoctrineConditionKindUi.SKILL_READY -> {
                    SkillSelector(
                        state = state,
                        selectedSkillId = node.skillId,
                        onSelect = {
                            onDraftAction(
                                DoctrineDraftAction.SetPredicateSkill(path, it)
                            )
                        }
                    )
                }
                DoctrineConditionKindUi.RESONANCE_CHARGE -> {
                    ComparisonSelector(node, path, onDraftAction)
                    AffinitySelector(
                        state = state,
                        selectedAffinityId = node.affinityId,
                        onSelect = {
                            onDraftAction(DoctrineDraftAction.SetAffinity(path, it))
                        }
                    )
                    StepperRow(
                        label = stringResource(
                            R.string.doctrine_resonance_amount_format,
                            node.resonanceAmount
                        ),
                        onDecrease = {
                            onDraftAction(
                                DoctrineDraftAction.AdjustResonanceAmount(path, -1L)
                            )
                        },
                        onIncrease = {
                            onDraftAction(
                                DoctrineDraftAction.AdjustResonanceAmount(path, 1L)
                            )
                        },
                        decreaseEnabled = node.resonanceAmount > 0L,
                        increaseEnabled = true
                    )
                }
                DoctrineConditionKindUi.SEQUENCE_SUFFIX -> {
                    Text(
                        text = stringResource(
                            R.string.doctrine_sequence_builder_format,
                            node.sequenceAffinityIds.size,
                            state.sequenceCapacity
                        ),
                        style = MaterialTheme.typography.bodySmall
                    )
                    if (node.sequenceAffinityIds.isNotEmpty()) {
                        Row(
                            modifier = Modifier.horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            node.sequenceAffinityIds.forEach { affinityId ->
                                AffinityIcon(
                                    affinityId = affinityId,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                    AffinityAppendSelector(
                        state = state,
                        enabled = node.sequenceAffinityIds.size < state.sequenceCapacity,
                        onAppend = {
                            onDraftAction(
                                DoctrineDraftAction.AppendSequenceAffinity(path, it)
                            )
                        }
                    )
                    GameOutlinedButton(
                        onClick = {
                            onDraftAction(
                                DoctrineDraftAction.RemoveLastSequenceAffinity(path)
                            )
                        },
                        enabled = node.sequenceAffinityIds.isNotEmpty()
                    ) {
                        Text(stringResource(R.string.doctrine_sequence_remove_last))
                    }
                }
                DoctrineConditionKindUi.STATUS_PRESENT,
                DoctrineConditionKindUi.STATUS_ABSENT -> {
                    StatusTargetSelector(node, path, onDraftAction)
                    StatusSelector(
                        state = state,
                        selectedStatusId = node.statusDefinitionId,
                        onSelect = {
                            onDraftAction(
                                DoctrineDraftAction.SetStatusDefinition(path, it)
                            )
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun ActionEditor(
    state: DoctrineUiState,
    onDraftAction: (DoctrineDraftAction) -> Unit
) {
    val action = state.draft?.action ?: return
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = stringResource(R.string.doctrine_action_label),
            style = MaterialTheme.typography.labelLarge
        )
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            GameChoiceButton(
                selected = action.kind == DoctrineActionKindUi.BASIC_ATTACK,
                onClick = {
                    onDraftAction(
                        DoctrineDraftAction.SetActionKind(DoctrineActionKindUi.BASIC_ATTACK)
                    )
                },
            ) { Text(stringResource(state.basicAttackTitleStringKey.stringResId())) }
            GameChoiceButton(
                selected = action.kind == DoctrineActionKindUi.SKILL,
                onClick = {
                    onDraftAction(
                        DoctrineDraftAction.SetActionKind(DoctrineActionKindUi.SKILL)
                    )
                },
                enabled = state.skillChoices.any { it.selectableForDoctrine }
            ) { Text(stringResource(R.string.doctrine_action_skill)) }
        }
        if (action.kind == DoctrineActionKindUi.SKILL) {
            SkillSelector(
                state = state,
                selectedSkillId = action.skillId,
                onSelect = {
                    onDraftAction(DoctrineDraftAction.SetActionSkill(it))
                }
            )
        }
    }
}

@Composable
private fun ComparisonSelector(
    node: DoctrineConditionDraftUiState,
    path: DoctrineDraftPath,
    onDraftAction: (DoctrineDraftAction) -> Unit
) {
    Row(
        modifier = Modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        DoctrineComparisonUi.values().forEach { comparison ->
            GameChoiceButton(
                selected = node.comparison == comparison,
                onClick = {
                    onDraftAction(
                        DoctrineDraftAction.SetComparison(path, comparison)
                    )
                }
            ) { Text(comparisonLabel(comparison)) }
        }
    }
}

@Composable
private fun SkillSelector(
    state: DoctrineUiState,
    selectedSkillId: ContentId?,
    onSelect: (ContentId) -> Unit
) {
    val selectable = state.skillChoices.filter { it.selectableForDoctrine }
    if (selectable.isEmpty()) {
        Text(
            text = stringResource(R.string.doctrine_no_equipped_skills),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.error
        )
        return
    }
    Row(
        modifier = Modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        selectable.forEach { skill ->
            GameChoiceButton(
                selected = selectedSkillId == skill.skillId,
                onClick = { onSelect(skill.skillId) }
            ) { Text(stringResource(skill.titleStringKey.stringResId())) }
        }
    }
}

@Composable
private fun AffinitySelector(
    state: DoctrineUiState,
    selectedAffinityId: ContentId?,
    onSelect: (ContentId) -> Unit
) {
    Row(
        modifier = Modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        state.affinityChoices.forEach { affinity ->
            GameChoiceButton(
                selected = selectedAffinityId == affinity.affinityId,
                onClick = { onSelect(affinity.affinityId) }
            ) {
                AffinityIcon(
                    affinityId = affinity.affinityId,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = stringResource(affinity.titleStringKey.stringResId()),
                    modifier = Modifier.padding(start = 4.dp)
                )
            }
        }
    }
}

@Composable
private fun AffinityAppendSelector(
    state: DoctrineUiState,
    enabled: Boolean,
    onAppend: (ContentId) -> Unit
) {
    Row(
        modifier = Modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        state.affinityChoices.forEach { affinity ->
            GameOutlinedButton(
                onClick = { onAppend(affinity.affinityId) },
                enabled = enabled
            ) {
                AffinityIcon(
                    affinityId = affinity.affinityId,
                    contentDescription = stringResource(affinity.titleStringKey.stringResId()),
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
private fun StatusTargetSelector(
    node: DoctrineConditionDraftUiState,
    path: DoctrineDraftPath,
    onDraftAction: (DoctrineDraftAction) -> Unit
) {
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        DoctrineStatusTargetUi.values().forEach { target ->
            GameChoiceButton(
                selected = node.statusTarget == target,
                onClick = {
                    onDraftAction(DoctrineDraftAction.SetStatusTarget(path, target))
                }
            ) { Text(statusTargetLabel(target)) }
        }
    }
}

@Composable
private fun StatusSelector(
    state: DoctrineUiState,
    selectedStatusId: ContentId?,
    onSelect: (ContentId) -> Unit
) {
    Row(
        modifier = Modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        state.statusChoices.forEach { status ->
            GameChoiceButton(
                selected = selectedStatusId == status.statusId,
                onClick = { onSelect(status.statusId) }
            ) { Text(stringResource(status.titleStringKey.stringResId())) }
        }
    }
}

@Composable
private fun StepperRow(
    label: String,
    onDecrease: () -> Unit,
    onIncrease: () -> Unit,
    decreaseEnabled: Boolean,
    increaseEnabled: Boolean
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        GameOutlinedButton(onClick = onDecrease, enabled = decreaseEnabled) {
            Text("−")
        }
        Text(label, style = MaterialTheme.typography.bodyMedium)
        GameOutlinedButton(onClick = onIncrease, enabled = increaseEnabled) {
            Text("+")
        }
    }
}

@Composable
private fun conditionSummaryLine(
    state: DoctrineUiState,
    node: DoctrineConditionDraftUiState
): String = when (node.kind) {
    DoctrineConditionKindUi.ALL -> stringResource(R.string.doctrine_condition_all)
    DoctrineConditionKindUi.ANY -> stringResource(R.string.doctrine_condition_any)
    DoctrineConditionKindUi.NOT -> stringResource(R.string.doctrine_condition_not)
    DoctrineConditionKindUi.PLAYER_HEALTH_PERCENT -> stringResource(R.string.doctrine_condition_player_hp_summary, comparisonLabel(node.comparison), node.enemyHealthPercent)
    DoctrineConditionKindUi.ENEMY_COUNT -> stringResource(R.string.doctrine_condition_enemy_count_summary, comparisonLabel(node.comparison), node.resonanceAmount)
    DoctrineConditionKindUi.ENEMY_ROLE_PRESENT -> stringResource(R.string.doctrine_condition_enemy_role_summary, node.enemyRole.name.lowercase().replaceFirstChar(Char::uppercase))
    DoctrineConditionKindUi.ELITE_PRESENT -> stringResource(R.string.doctrine_condition_elite_present)
    DoctrineConditionKindUi.BOSS_PRESENT -> stringResource(R.string.doctrine_condition_boss_present)
    DoctrineConditionKindUi.ENEMY_HEALTH_PERCENT -> stringResource(
        R.string.doctrine_condition_enemy_hp_summary,
        comparisonLabel(node.comparison),
        node.enemyHealthPercent
    )
    DoctrineConditionKindUi.SKILL_READY -> stringResource(
        R.string.doctrine_condition_skill_ready_summary,
        skillTitle(state, node.skillId)
    )
    DoctrineConditionKindUi.RESONANCE_CHARGE -> stringResource(
        R.string.doctrine_condition_resonance_summary,
        affinityTitle(state, node.affinityId),
        comparisonLabel(node.comparison),
        node.resonanceAmount
    )
    DoctrineConditionKindUi.SEQUENCE_SUFFIX -> stringResource(
        R.string.doctrine_condition_sequence_summary,
        node.sequenceAffinityIds.joinToString(" ") { affinityVisualToken(it).glyph }
    )
    DoctrineConditionKindUi.STATUS_PRESENT -> stringResource(
        R.string.doctrine_condition_status_present_summary,
        statusTargetLabel(node.statusTarget),
        statusTitle(state, node.statusDefinitionId)
    )
    DoctrineConditionKindUi.STATUS_ABSENT -> stringResource(
        R.string.doctrine_condition_status_absent_summary,
        statusTargetLabel(node.statusTarget),
        statusTitle(state, node.statusDefinitionId)
    )
}

@Composable
private fun ConvergenceSection(state: DoctrineUiState) {
    Text(
        text = stringResource(R.string.doctrine_convergences_title),
        style = MaterialTheme.typography.titleMedium
    )
    state.convergences.forEach { convergence ->
        GameCard(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = stringResource(convergence.titleStringKey.stringResId()),
                        style = MaterialTheme.typography.titleSmall
                    )
                    Text(
                        text = if (convergence.eligibleNow) {
                            stringResource(R.string.doctrine_convergence_ready)
                        } else {
                            stringResource(R.string.doctrine_convergence_waiting)
                        },
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
                Text(
                    text = stringResource(
                        R.string.doctrine_convergence_pattern_format,
                        convergence.patternAffinityIds.joinToString(" ") {
                            affinityVisualToken(it).glyph
                        }
                    ),
                    style = MaterialTheme.typography.bodySmall
                )
                if (convergence.minimumCharges.isNotEmpty()) {
                    Text(
                        text = convergence.minimumCharges.joinToString(" · ") { requirement ->
                            "${affinityVisualToken(requirement.affinityId).glyph} ${requirement.requiredDisplay}"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    text = stringResource(
                        R.string.doctrine_convergence_counts_format,
                        convergence.totalTriggerCountDisplay,
                        convergence.encounterTriggerCount
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = if (convergence.discovered) {
                        stringResource(R.string.doctrine_convergence_discovered)
                    } else {
                        stringResource(R.string.doctrine_convergence_undiscovered)
                    },
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

@Composable
private fun DoctrineFeedbackCard(
    state: DoctrineUiState,
    feedback: DoctrineFeedbackUiState
) {
    val isError = feedback.kind == DoctrineFeedbackKind.COMMAND_REJECTED ||
        feedback.kind == DoctrineFeedbackKind.ACTION_REJECTED
    GameCard(
        modifier = Modifier
            .fillMaxWidth()
            .eventFeedbackPulse(feedback.sequenceNumber),
        colors = CardDefaults.cardColors(
            containerColor = if (isError) {
                MaterialTheme.colorScheme.errorContainer
            } else {
                MaterialTheme.colorScheme.primaryContainer
            }
        )
    ) {
        Text(
            text = when (feedback.kind) {
                DoctrineFeedbackKind.COMMAND_REJECTED -> stringResource(
                    R.string.doctrine_feedback_rejected,
                    rejectionMessage(feedback.rejectionCode)
                )
                DoctrineFeedbackKind.DOCTRINE_UPDATED -> stringResource(
                    R.string.doctrine_feedback_updated
                )
                DoctrineFeedbackKind.RULE_SELECTED -> stringResource(
                    R.string.doctrine_feedback_rule_selected,
                    actionTitle(state, feedback.contentId)
                )
                DoctrineFeedbackKind.ACTION_REJECTED -> stringResource(
                    R.string.doctrine_feedback_action_rejected,
                    actionTitle(state, feedback.contentId),
                    rejectionMessage(feedback.rejectionCode)
                )
                DoctrineFeedbackKind.FALLBACK_USED -> stringResource(
                    R.string.doctrine_feedback_fallback,
                    actionTitle(state, feedback.contentId)
                )
                DoctrineFeedbackKind.RESONANCE_GENERATED -> stringResource(
                    R.string.doctrine_feedback_resonance_generated,
                    feedback.amountDisplay ?: "0",
                    affinityTitle(state, feedback.affinityId)
                )
                DoctrineFeedbackKind.RESONANCE_CONSUMED -> stringResource(
                    R.string.doctrine_feedback_resonance_consumed,
                    feedback.amountDisplay ?: "0",
                    affinityTitle(state, feedback.affinityId)
                )
                DoctrineFeedbackKind.CONVERGENCE_TRIGGERED -> stringResource(
                    R.string.doctrine_feedback_convergence_triggered,
                    actionTitle(state, feedback.contentId)
                )
                DoctrineFeedbackKind.CONVERGENCE_DISCOVERED -> stringResource(
                    R.string.doctrine_feedback_convergence_discovered,
                    actionTitle(state, feedback.contentId)
                )
            },
            modifier = Modifier.padding(12.dp),
            style = MaterialTheme.typography.bodyMedium,
            color = if (isError) {
                MaterialTheme.colorScheme.onErrorContainer
            } else {
                MaterialTheme.colorScheme.onPrimaryContainer
            }
        )
    }
}

@Composable
private fun actionLabel(
    state: DoctrineUiState,
    kind: DoctrineActionKindUi,
    skillId: ContentId?
): String = when (kind) {
    DoctrineActionKindUi.BASIC_ATTACK ->
        stringResource(state.basicAttackTitleStringKey.stringResId())
    DoctrineActionKindUi.SKILL -> skillTitle(state, skillId)
}

@Composable
private fun presetLabel(preset: DoctrinePreset): String = when (preset) {
    DoctrinePreset.BALANCED -> stringResource(R.string.doctrine_preset_balanced)
    DoctrinePreset.AGGRESSIVE -> stringResource(R.string.doctrine_preset_aggressive)
    DoctrinePreset.SURVIVAL -> stringResource(R.string.doctrine_preset_survival)
}

@Composable
private fun actionTitle(state: DoctrineUiState, contentId: ContentId?): String {
    if (contentId == null) {
        return stringResource(R.string.doctrine_unknown_content)
    }
    state.skillChoices.firstOrNull { it.skillId == contentId }?.let {
        return stringResource(it.titleStringKey.stringResId())
    }
    state.convergences.firstOrNull { it.convergenceId == contentId }?.let {
        return stringResource(it.titleStringKey.stringResId())
    }
    return if (contentId == state.basicAttackId) {
        stringResource(state.basicAttackTitleStringKey.stringResId())
    } else {
        stringResource(R.string.doctrine_unknown_content)
    }
}

@Composable
private fun skillTitle(state: DoctrineUiState, skillId: ContentId?): String =
    state.skillChoices.firstOrNull { it.skillId == skillId }
        ?.let { stringResource(it.titleStringKey.stringResId()) }
        ?: stringResource(R.string.doctrine_choose_skill)

@Composable
private fun statusTitle(state: DoctrineUiState, statusId: ContentId?): String =
    state.statusChoices.firstOrNull { it.statusId == statusId }
        ?.let { stringResource(it.titleStringKey.stringResId()) }
        ?: stringResource(R.string.doctrine_choose_status)

@Composable
private fun affinityTitle(state: DoctrineUiState, affinityId: ContentId?): String =
    state.affinityChoices.firstOrNull { it.affinityId == affinityId }
        ?.let { stringResource(it.titleStringKey.stringResId()) }
        ?: stringResource(R.string.doctrine_choose_affinity)

@Composable
private fun conditionKindLabel(kind: DoctrineConditionKindUi): String = when (kind) {
    DoctrineConditionKindUi.ALL -> stringResource(R.string.doctrine_condition_all)
    DoctrineConditionKindUi.ANY -> stringResource(R.string.doctrine_condition_any)
    DoctrineConditionKindUi.NOT -> stringResource(R.string.doctrine_condition_not)
    DoctrineConditionKindUi.PLAYER_HEALTH_PERCENT -> stringResource(R.string.doctrine_condition_player_hp)
    DoctrineConditionKindUi.ENEMY_COUNT -> stringResource(R.string.doctrine_condition_enemy_count)
    DoctrineConditionKindUi.ENEMY_ROLE_PRESENT -> stringResource(R.string.doctrine_condition_enemy_role)
    DoctrineConditionKindUi.ELITE_PRESENT -> stringResource(R.string.doctrine_condition_elite_present)
    DoctrineConditionKindUi.BOSS_PRESENT -> stringResource(R.string.doctrine_condition_boss_present)
    DoctrineConditionKindUi.ENEMY_HEALTH_PERCENT -> stringResource(R.string.doctrine_condition_enemy_hp)
    DoctrineConditionKindUi.SKILL_READY -> stringResource(R.string.doctrine_condition_skill_ready)
    DoctrineConditionKindUi.RESONANCE_CHARGE -> stringResource(R.string.doctrine_condition_resonance_charge)
    DoctrineConditionKindUi.SEQUENCE_SUFFIX -> stringResource(R.string.doctrine_condition_sequence_suffix)
    DoctrineConditionKindUi.STATUS_PRESENT -> stringResource(R.string.doctrine_condition_status_present)
    DoctrineConditionKindUi.STATUS_ABSENT -> stringResource(R.string.doctrine_condition_status_absent)
}

@Composable
private fun comparisonLabel(comparison: DoctrineComparisonUi): String = when (comparison) {
    DoctrineComparisonUi.LESS_THAN -> stringResource(R.string.doctrine_comparison_less)
    DoctrineComparisonUi.LESS_THAN_OR_EQUAL -> stringResource(R.string.doctrine_comparison_less_equal)
    DoctrineComparisonUi.EQUAL -> stringResource(R.string.doctrine_comparison_equal)
    DoctrineComparisonUi.GREATER_THAN_OR_EQUAL -> stringResource(R.string.doctrine_comparison_greater_equal)
    DoctrineComparisonUi.GREATER_THAN -> stringResource(R.string.doctrine_comparison_greater)
}

@Composable
private fun statusTargetLabel(target: DoctrineStatusTargetUi): String = when (target) {
    DoctrineStatusTargetUi.PLAYER -> stringResource(R.string.doctrine_target_player)
    DoctrineStatusTargetUi.PRIMARY_ENEMY -> stringResource(R.string.doctrine_target_enemy)
}

@Composable
private fun draftErrorLabel(error: DoctrineDraftErrorUi): String = when (error) {
    DoctrineDraftErrorUi.MISSING_SKILL -> stringResource(R.string.doctrine_error_missing_skill)
    DoctrineDraftErrorUi.MISSING_STATUS -> stringResource(R.string.doctrine_error_missing_status)
    DoctrineDraftErrorUi.EMPTY_SEQUENCE -> stringResource(R.string.doctrine_error_empty_sequence)
    DoctrineDraftErrorUi.INVALID_AFFINITY -> stringResource(R.string.doctrine_error_invalid_affinity)
    DoctrineDraftErrorUi.CONDITION_DEPTH_EXCEEDED -> stringResource(R.string.doctrine_error_depth)
}

@Composable
private fun rejectionMessage(code: CommandRejectionCode?): String = when (code) {
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
    null -> stringResource(R.string.doctrine_unknown_content)
}
