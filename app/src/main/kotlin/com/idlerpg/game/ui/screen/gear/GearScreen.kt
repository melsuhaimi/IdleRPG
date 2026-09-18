package com.idlerpg.game.ui.screen.gear

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.idlerpg.game.R
import com.idlerpg.game.core.id.ContentId
import com.idlerpg.game.core.id.InstanceId
import com.idlerpg.game.domain.command.CommandRejectionCode
import com.idlerpg.game.domain.definition.EquipmentSlot
import com.idlerpg.game.domain.definition.Rarity
import com.idlerpg.game.presentation.intent.GearUiIntent
import com.idlerpg.game.presentation.model.GearEffectKind
import com.idlerpg.game.presentation.model.GearEffectUiState
import com.idlerpg.game.presentation.model.GearFeedbackKind
import com.idlerpg.game.presentation.model.GearFeedbackUiState
import com.idlerpg.game.presentation.model.GearItemUiState
import com.idlerpg.game.presentation.model.GearUiState
import com.idlerpg.game.ui.component.premium.GameButton
import com.idlerpg.game.ui.component.premium.GameCard
import com.idlerpg.game.ui.component.premium.GameChoiceButton
import com.idlerpg.game.ui.component.premium.GameDivider
import com.idlerpg.game.ui.component.premium.GameMetricChip
import com.idlerpg.game.ui.component.premium.GameOutlinedButton
import com.idlerpg.game.ui.component.premium.GameProgressBar
import com.idlerpg.game.ui.component.premium.GameSectionHeader
import com.idlerpg.game.ui.component.premium.GameStatusPill
import com.idlerpg.game.ui.component.premium.PremiumPanel
import com.idlerpg.game.ui.content.drawableResId
import com.idlerpg.game.ui.content.stringResId
import com.idlerpg.game.ui.motion.eventFeedbackPulse
import com.idlerpg.game.ui.theme.ArcaneViolet
import com.idlerpg.game.ui.theme.ObsidianOutline
import com.idlerpg.game.ui.theme.ObsidianSurface1
import com.idlerpg.game.ui.theme.ObsidianSurface2
import com.idlerpg.game.ui.theme.ObsidianSurface3
import com.idlerpg.game.ui.theme.PositiveGreen
import com.idlerpg.game.ui.theme.ResourceGold
import com.idlerpg.game.ui.theme.ResonanceTeal
import com.idlerpg.game.ui.theme.WarningAmber

private sealed interface PendingGearDestructiveAction {
    val itemInstanceId: InstanceId

    data class SalvageOwned(override val itemInstanceId: InstanceId) : PendingGearDestructiveAction
    data class SalvageOverflow(override val itemInstanceId: InstanceId) : PendingGearDestructiveAction
}

/** Equipment workshop: the projection remains authoritative; this file only composes it. */
@Composable
fun GearScreen(
    state: GearUiState,
    focusItemId: InstanceId? = null,
    onIntent: (GearUiIntent) -> Unit,
    onOpenSkills: () -> Unit = {},
    onOpenDoctrine: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var selectedItemValue by rememberSaveable { mutableStateOf(focusItemId?.value) }
    var selectedItemIds by remember { mutableStateOf(emptySet<InstanceId>()) }
    var selectedOverflowIds by remember { mutableStateOf(emptySet<InstanceId>()) }
    var confirmBulkSalvage by remember { mutableStateOf(false) }
    var confirmOverflowSalvage by remember { mutableStateOf(false) }
    var salvageBelowRarity by remember { mutableStateOf<Rarity?>(null) }
    var pendingDestructiveAction by remember {
        mutableStateOf<PendingGearDestructiveAction?>(null)
    }
    var rarityFilterId by rememberSaveable { mutableStateOf<String?>(null) }
    var slotFilterId by rememberSaveable { mutableStateOf<String?>(null) }
    var sortModeId by rememberSaveable { mutableStateOf("rarity") }
    var storageTabId by rememberSaveable { mutableStateOf("inventory") }
    var lockedOnly by rememberSaveable { mutableStateOf(false) }

    val allItems = state.ownedItems + state.overflowItems
    val storageIsStash = storageTabId == "stash"
    val rarityFilter = rarityFilterId?.let { id ->
        Rarity.values().firstOrNull { it.id.value == id }
    }
    val slotFilter = slotFilterId?.let { id ->
        EquipmentSlot.values().firstOrNull { it.id.value == id }
    }
    val sortItems: (List<GearItemUiState>) -> List<GearItemUiState> = { items ->
        when (sortModeId) {
            "newest" -> items.sortedByDescending { it.instanceId.value }
            else -> items.sortedWith(
                compareByDescending<GearItemUiState> { item ->
                    Rarity.values().firstOrNull { rarity -> rarity.id == item.rarityId }?.rank ?: 0
                }.thenByDescending { it.instanceId.value }
            )
        }
    }
    val visibleOwnedItems = sortItems(
        state.ownedItems
            .filter { rarityFilter == null || it.rarityId == rarityFilter.id }
            .filter { slotFilter == null || it.equipmentSlot == slotFilter }
            .filter { !lockedOnly || it.locked }
    )
    val visibleOverflowItems = sortItems(
        state.overflowItems
            .filter { rarityFilter == null || it.rarityId == rarityFilter.id }
            .filter { slotFilter == null || it.equipmentSlot == slotFilter }
    )
    val visibleItems = if (storageIsStash) visibleOverflowItems else visibleOwnedItems
    val selectedItem = allItems.firstOrNull { it.instanceId.value == selectedItemValue }
    val equippedComparison = selectedItem?.equipmentSlot?.let { slot ->
        state.equipmentSlots.firstOrNull { it.slot == slot }?.equippedItem
    }?.takeUnless { it?.instanceId == selectedItem?.instanceId }
    val selectedSalvageableIds = selectedItemIds.intersect(state.ownedItems.map { it.instanceId }.toSet())

    LaunchedEffect(focusItemId) {
        focusItemId?.let { selectedItemValue = it.value }
    }
    LaunchedEffect(state.ownedItems, state.overflowItems) {
        if (selectedItemValue != null && allItems.none { it.instanceId.value == selectedItemValue }) {
            selectedItemValue = null
        }
        selectedItemIds = selectedItemIds.intersect(state.ownedItems.map { it.instanceId }.toSet())
        selectedOverflowIds = selectedOverflowIds.intersect(state.overflowItems.map { it.instanceId }.toSet())
    }
    LaunchedEffect(pendingDestructiveAction, allItems) {
        if (pendingDestructiveAction != null && allItems.none {
                it.instanceId == pendingDestructiveAction?.itemInstanceId
            }
        ) {
            pendingDestructiveAction = null
        }
    }

    LaunchedEffect(rarityFilterId, slotFilterId, lockedOnly, storageTabId) {
        val visibleOwnedIds = visibleOwnedItems.map { it.instanceId }.toSet()
        val visibleOverflowIds = visibleOverflowItems.map { it.instanceId }.toSet()
        selectedItemIds = selectedItemIds.intersect(visibleOwnedIds)
        selectedOverflowIds = selectedOverflowIds.intersect(visibleOverflowIds)
    }


    selectedItem?.let { inspected ->
        Dialog(onDismissRequest = { selectedItemValue = null }, properties = DialogProperties(usePlatformDefaultWidth = false)) {
            Column(
                Modifier.padding(12.dp).widthIn(max = 640.dp).fillMaxWidth()
                    .heightIn(max = (LocalConfiguration.current.screenHeightDp * 0.88f).dp)
                    .clip(RoundedCornerShape(16.dp)).background(ObsidianSurface1)
            ) {
                Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("EQUIPMENT", Modifier.weight(1f), style = MaterialTheme.typography.labelLarge, color = ResourceGold)
                    TextButton(onClick = { selectedItemValue = null }) { Text("Close") }
                }
                Column(Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState())) {
                    FocusedGearSheet(
                        item = inspected,
                        equippedComparison = equippedComparison,
                        onIntent = onIntent,
                        onRequestSalvage = {
                            pendingDestructiveAction = if (inspected.overflow) PendingGearDestructiveAction.SalvageOverflow(inspected.instanceId)
                            else PendingGearDestructiveAction.SalvageOwned(inspected.instanceId)
                        }
                    )
                }
            }
        }
    }

    if (confirmBulkSalvage) {
        AlertDialog(
            onDismissRequest = { confirmBulkSalvage = false },
            title = { Text(stringResource(R.string.gear_bulk_salvage_confirm_title)) },
            text = {
                Text(stringResource(R.string.gear_bulk_salvage_confirm_body, selectedSalvageableIds.size))
            },
            confirmButton = {
                GameButton(
                    onClick = {
                        onIntent(GearUiIntent.SalvageSelected(selectedSalvageableIds))
                        selectedItemIds = emptySet()
                        confirmBulkSalvage = false
                    },
                    enabled = selectedSalvageableIds.isNotEmpty()
                ) { Text(stringResource(R.string.gear_salvage_selected)) }
            },
            dismissButton = {
                TextButton(onClick = { confirmBulkSalvage = false }) {
                    Text(stringResource(R.string.gear_cancel))
                }
            }
        )
    }
    if (confirmOverflowSalvage) {
        AlertDialog(
            onDismissRequest = { confirmOverflowSalvage = false },
            title = { Text(stringResource(R.string.gear_bulk_salvage_confirm_title)) },
            text = {
                Text(stringResource(R.string.gear_bulk_salvage_confirm_body, selectedOverflowIds.size))
            },
            confirmButton = {
                GameButton(
                    onClick = {
                        onIntent(GearUiIntent.SalvageOverflowSelected(selectedOverflowIds))
                        selectedOverflowIds = emptySet()
                        confirmOverflowSalvage = false
                    },
                    enabled = selectedOverflowIds.isNotEmpty()
                ) { Text(stringResource(R.string.gear_salvage_selected)) }
            },
            dismissButton = {
                TextButton(onClick = { confirmOverflowSalvage = false }) {
                    Text(stringResource(R.string.gear_cancel))
                }
            }
        )
    }
    salvageBelowRarity?.let { rarity ->
        AlertDialog(
            onDismissRequest = { salvageBelowRarity = null },
            title = { Text(stringResource(R.string.gear_bulk_salvage_confirm_title)) },
            text = { Text(stringResource(R.string.gear_salvage_below_confirm, rarity.name)) },
            confirmButton = {
                GameButton(
                    onClick = {
                        onIntent(GearUiIntent.SalvageBelow(rarity))
                        salvageBelowRarity = null
                    }
                ) { Text(stringResource(R.string.gear_salvage_below, rarity.name)) }
            },
            dismissButton = {
                TextButton(onClick = { salvageBelowRarity = null }) {
                    Text(stringResource(R.string.gear_cancel))
                }
            }
        )
    }
    pendingDestructiveAction?.let { pending ->
        allItems.firstOrNull { it.instanceId == pending.itemInstanceId }?.let { item ->
            SalvageConfirmationDialog(
                item = item,
                onConfirm = {
                    val intent = when (pending) {
                        is PendingGearDestructiveAction.SalvageOwned -> GearUiIntent.Salvage(pending.itemInstanceId)
                        is PendingGearDestructiveAction.SalvageOverflow -> GearUiIntent.SalvageOverflow(pending.itemInstanceId)
                    }
                    pendingDestructiveAction = null
                    onIntent(intent)
                },
                onDismiss = { pendingDestructiveAction = null }
            )
        }
    }

    val listState = rememberLazyListState()

    LazyColumn(
        state = listState,
        modifier = modifier.fillMaxSize().padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 14.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item(key = "gear-header") {
            BuildHeader(onOpenSkills = onOpenSkills, onOpenDoctrine = onOpenDoctrine)
        }
        state.feedback?.let { feedback ->
            item(key = "gear-feedback-${feedback.sequenceNumber ?: -1L}-${feedback.kind}") {
                GearFeedbackBanner(feedback)
            }
        }
        item(key = "gear-armory") {
            ArmoryPanel(
                state = state,
                selectedItemValue = selectedItemValue,
                onSelectItem = { selectedItemValue = it }
            )
        }
        item(key = "gear-capacity") {
            CapacityPanel(state = state, onExpand = { onIntent(GearUiIntent.ExpandCapacity) })
        }
        item(key = "gear-storage-tabs") {
            StorageTabs(
                stashSelected = storageIsStash,
                inventoryCount = state.capacity.normalUsed,
                inventoryCapacity = state.capacity.normalCapacity,
                stashCount = state.capacity.overflowUsed,
                onSelectInventory = {
                    storageTabId = "inventory"
                    selectedOverflowIds = emptySet()
                    selectedItemValue = null
                },
                onSelectStash = {
                    storageTabId = "stash"
                    selectedItemIds = emptySet()
                    selectedItemValue = null
                }
            )
        }
        item(key = "gear-inventory-tools") {
            InventoryToolbar(
                state = state,
                showingStash = storageIsStash,
                visibleItemIds = visibleItems.map { it.instanceId }.toSet(),
                selectedItemIds = if (storageIsStash) selectedOverflowIds else selectedItemIds,
                rarityFilter = rarityFilter,
                slotFilter = slotFilter,
                sortModeId = sortModeId,
                lockedOnly = lockedOnly,
                onSelectAll = {
                    if (storageIsStash) {
                        selectedOverflowIds = visibleOverflowItems.map { it.instanceId }.toSet()
                    } else {
                        selectedItemIds = visibleOwnedItems
                            .filter { it.canSalvage }
                            .map { it.instanceId }
                            .toSet()
                    }
                },
                onSelectRarity = { rarityFilterId = it?.id?.value },
                onSelectSlot = { slotFilterId = it?.id?.value },
                onToggleSort = {
                    sortModeId = if (sortModeId == "rarity") "newest" else "rarity"
                },
                onToggleLocked = { lockedOnly = !lockedOnly },
                onSelectKeepRarity = { rarity ->
                    onIntent(GearUiIntent.SetLootFilter(true, rarity))
                },
                onDisableAutoSalvage = {
                    onIntent(GearUiIntent.SetLootFilter(false, state.minimumKeepRarity))
                },
                onSalvageBelow = { salvageBelowRarity = state.minimumKeepRarity },
                onBulkSalvage = {
                    if (storageIsStash) confirmOverflowSalvage = true else confirmBulkSalvage = true
                },
                onClearSelection = {
                    if (storageIsStash) selectedOverflowIds = emptySet() else selectedItemIds = emptySet()
                }
            )
        }
        if (visibleItems.isEmpty()) {
            item(key = if (storageIsStash) "gear-stash-empty" else "gear-inventory-empty") {
                EmptyStoragePanel(
                    if (storageIsStash) {
                        stringResource(R.string.gear_overflow_empty)
                    } else {
                        stringResource(R.string.gear_inventory_empty)
                    }
                )
            }
        } else {
            items(
                visibleItems,
                key = { item ->
                    if (storageIsStash) "stash-${item.instanceId.value}"
                    else "owned-${item.instanceId.value}"
                }
            ) { item ->
                if (storageIsStash) {
                    StashInventoryRow(
                        item = item,
                        selected = item.instanceId.value == selectedItemValue,
                        bulkSelected = item.instanceId in selectedOverflowIds,
                        onSelect = { selectedItemValue = item.instanceId.value },
                        onToggleSelected = {
                            selectedOverflowIds = if (item.instanceId in selectedOverflowIds) {
                                selectedOverflowIds - item.instanceId
                            } else {
                                selectedOverflowIds + item.instanceId
                            }
                        },
                        onIntent = onIntent,
                        onRequestSalvage = {
                            pendingDestructiveAction = PendingGearDestructiveAction.SalvageOverflow(item.instanceId)
                        }
                    )
                } else {
                    ArmoryInventoryRow(
                        item = item,
                        selected = item.instanceId.value == selectedItemValue,
                        bulkSelected = item.instanceId in selectedItemIds,
                        onSelect = { selectedItemValue = item.instanceId.value },
                        onToggleSelected = {
                            if (item.canSalvage) {
                                selectedItemIds = if (item.instanceId in selectedItemIds) {
                                    selectedItemIds - item.instanceId
                                } else {
                                    selectedItemIds + item.instanceId
                                }
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun BuildHeader(onOpenSkills: () -> Unit, onOpenDoctrine: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        GameSectionHeader(
            eyebrow = stringResource(R.string.gear_armory_eyebrow),
            title = stringResource(R.string.gear_title),
            subtitle = stringResource(R.string.gear_subtitle),
            trailing = {
                GameStatusPill(
                    text = stringResource(R.string.gear_build_badge),
                    accent = ArcaneViolet
                )
            }
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            GameChoiceButton(
                selected = true,
                onClick = {},
                modifier = Modifier.widthIn(min = 132.dp)
            ) {
                Text(stringResource(R.string.gear_equipment_title))
            }
            GameOutlinedButton(
                onClick = onOpenSkills,
                modifier = Modifier.widthIn(min = 118.dp)
            ) {
                Text(stringResource(R.string.build_open_skills))
            }
            GameOutlinedButton(
                onClick = onOpenDoctrine,
                modifier = Modifier.widthIn(min = 132.dp)
            ) {
                Text(stringResource(R.string.build_open_doctrine))
            }
        }
    }
}

@Composable
private fun ArmoryPanel(state: GearUiState, selectedItemValue: Long?, onSelectItem: (Long?) -> Unit) {
    val equippedCount = state.equipmentSlots.count { it.equippedItem != null }
    PremiumPanel(
        backgroundResId = R.drawable.panel_primary_premium,
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 14.dp),
        verticalSpacing = 12.dp,
        accent = ResonanceTeal
    ) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(stringResource(R.string.gear_equipment_title), style = MaterialTheme.typography.titleLarge)
                Text(stringResource(R.string.gear_active_loadout), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            GameStatusPill(text = "$equippedCount / ${state.equipmentSlots.size}", accent = ResonanceTeal)
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Column(modifier = Modifier.weight(0.84f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf(EquipmentSlot.WEAPON, EquipmentSlot.HELM, EquipmentSlot.BOOTS).forEach { slot ->
                    EquipmentSlotNode(slot, state.itemFor(slot), state.itemFor(slot)?.instanceId?.value == selectedItemValue, onSelectItem)
                }
            }
            Box(
                modifier = Modifier
                    .weight(1.22f)
                    .height(280.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(
                        Brush.radialGradient(
                            listOf(
                                ArcaneViolet.copy(alpha = 0.28f),
                                ObsidianSurface1.copy(alpha = 0.92f),
                                Color.Transparent
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(R.drawable.hero_echo_bound),
                    contentDescription = stringResource(R.string.gear_equipment_title),
                    modifier = Modifier.fillMaxSize().padding(4.dp),
                    contentScale = ContentScale.Fit
                )
                Text(
                    stringResource(R.string.gear_hero_label),
                    modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 8.dp),
                    style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.4.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Column(modifier = Modifier.weight(0.84f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf(EquipmentSlot.ARMOR, EquipmentSlot.ACCESSORY, EquipmentSlot.CATALYST).forEach { slot ->
                    EquipmentSlotNode(slot, state.itemFor(slot), state.itemFor(slot)?.instanceId?.value == selectedItemValue, onSelectItem)
                }
            }
        }
        GameDivider()
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            GameMetricChip(stringResource(R.string.gear_normal_capacity_label), "${state.capacity.normalUsed}/${state.capacity.normalCapacity}", ResonanceTeal, Modifier.weight(1f))
            GameMetricChip(stringResource(R.string.gear_overflow_capacity_label), "${state.capacity.overflowUsed}/${state.capacity.overflowCapacity}", WarningAmber, Modifier.weight(1f))
        }
    }
}

private fun GearUiState.itemFor(slot: EquipmentSlot): GearItemUiState? = equipmentSlots.firstOrNull { it.slot == slot }?.equippedItem

@Composable
private fun EquipmentSlotNode(
    slot: EquipmentSlot,
    item: GearItemUiState?,
    selected: Boolean,
    onSelectItem: (Long?) -> Unit
) {
    GameCard(
        onClick = { item?.let { onSelectItem(it.instanceId.value) } },
        enabled = item != null,
        modifier = Modifier.fillMaxWidth().heightIn(min = 88.dp),
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(if (selected) 2.dp else 1.dp, if (selected) ResourceGold else ObsidianOutline.copy(alpha = 0.52f)),
        accent = if (selected) ResourceGold else item?.let { rarityAccent(it.rarityId) },
        colors = CardDefaults.cardColors(containerColor = if (selected) ResourceGold.copy(alpha = 0.15f) else ObsidianSurface2.copy(alpha = 0.84f))
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 5.dp, vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            if (item == null) {
                Box(modifier = Modifier.size(40.dp).clip(RoundedCornerShape(10.dp)).background(ObsidianSurface3.copy(alpha = 0.58f)), contentAlignment = Alignment.Center) {
                    Text("—", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                Image(
                    painter = painterResource(item.iconAssetKey.drawableResId()),
                    contentDescription = stringResource(item.titleStringKey.stringResId()),
                    modifier = Modifier.size(40.dp),
                    contentScale = ContentScale.Fit
                )
            }
            Text(slotLabel(slot), style = MaterialTheme.typography.labelSmall, maxLines = 1, overflow = TextOverflow.Clip)
            if (item != null) {
                Text(stringResource(item.rarityTitleStringKey.stringResId()), style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp), color = rarityAccent(item.rarityId))
            }
        }
    }
}

@Composable
private fun FocusedGearSheet(
    item: GearItemUiState,
    equippedComparison: GearItemUiState?,
    onIntent: (GearUiIntent) -> Unit,
    onRequestSalvage: () -> Unit
) {
    var useProtection by remember(item.instanceId) { mutableStateOf(false) }
    PremiumPanel(
        backgroundResId = R.drawable.panel_secondary_premium,
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 14.dp),
        verticalSpacing = 10.dp,
        accent = rarityAccent(item.rarityId)
    ) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(stringResource(R.string.gear_inspection_eyebrow), style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.4.sp), color = ResourceGold)
                Text(stringResource(R.string.gear_show_details), style = MaterialTheme.typography.titleLarge)
            }
            when {
                item.locked -> GameStatusPill(stringResource(R.string.gear_locked_state), ResourceGold)
                item.equippedSlot != null -> GameStatusPill(stringResource(R.string.gear_status_equipped), ResonanceTeal)
                else -> GameStatusPill(stringResource(item.rarityTitleStringKey.stringResId()), rarityAccent(item.rarityId))
            }
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            ItemIdentity(item, rarityAccent(item.rarityId), Modifier.weight(1f))
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                if (item.equippedSlot != null) {
                    FactualEffectColumn(stringResource(R.string.gear_equipped), item, ResonanceTeal)
                } else {
                    FactualEffectColumn(stringResource(R.string.gear_candidate), item, ResourceGold)
                    FactualEffectColumn(
                        if (equippedComparison == null) stringResource(R.string.gear_no_item) else stringResource(R.string.gear_equipped),
                        equippedComparison,
                        ResonanceTeal
                    )
                }
            }
        }
        GameDivider()
        item.comparison?.let { comparison ->
            AuthoritativeStatComparison(comparison)
            GameDivider()
        }
        if (item.equippedSlot == null) {
            FactualEffectComparison(candidate = item, equipped = equippedComparison)
        }
        if (!item.overflow) {
            GearEnhancementPanel(
                item = item,
                useProtection = useProtection,
                onToggleProtection = { useProtection = !useProtection },
                onEnhance = {
                    onIntent(GearUiIntent.Enhance(item.instanceId, useProtection))
                }
            )
            GameDivider()
        }
        AffixList(item, onIntent)
        if (item.overflow) {
            Text(stringResource(R.string.gear_overflow_item_note), style = MaterialTheme.typography.bodySmall, color = WarningAmber)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                GameButton(
                    onClick = { onIntent(GearUiIntent.ClaimOverflow(item.instanceId)) },
                    enabled = item.canClaimOverflow,
                    modifier = Modifier.weight(1f)
                ) { Text(if (item.canClaimOverflow) stringResource(R.string.gear_claim_overflow) else stringResource(R.string.gear_claim_need_slot)) }
                GameOutlinedButton(onClick = onRequestSalvage, enabled = item.canSalvageOverflow, modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.gear_salvage_overflow))
                }
            }
        } else {
            item.equipmentSlot?.let { slot ->
                GameButton(
                    onClick = {
                        if (item.canUnequip) onIntent(GearUiIntent.Unequip(item.equippedSlot ?: slot))
                        else onIntent(GearUiIntent.Equip(item.instanceId, slot))
                    },
                    enabled = item.canEquip || item.canUnequip,
                    modifier = Modifier.fillMaxWidth()
                ) { Text(if (item.canUnequip) stringResource(R.string.gear_unequip) else stringResource(R.string.gear_equip)) }
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (item.canLock || item.canUnlock) {
                    GameOutlinedButton(
                        onClick = { onIntent(GearUiIntent.SetLocked(item.instanceId, !item.locked)) },
                        modifier = Modifier.weight(1f)
                    ) { Text(if (item.locked) stringResource(R.string.gear_unlock) else stringResource(R.string.gear_lock)) }
                }
                GameOutlinedButton(onClick = onRequestSalvage, enabled = item.canSalvage, modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.gear_salvage))
                }
            }
        }
    }
}

@Composable
private fun AuthoritativeStatComparison(
    comparison: com.idlerpg.game.presentation.model.GearComparisonUiState
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = stringResource(R.string.gear_stat_preview_title).uppercase(),
            style = MaterialTheme.typography.labelLarge,
            color = ResourceGold
        )
        Text(
            text = stringResource(R.string.gear_stat_preview_detail),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        StatPreviewRow(
            label = stringResource(R.string.battle_attack),
            current = comparison.currentAttackDisplay,
            resulting = comparison.resultingAttackDisplay,
            delta = comparison.attackDeltaDisplay
        )
        StatPreviewRow(
            label = stringResource(R.string.battle_armor),
            current = comparison.currentArmorDisplay,
            resulting = comparison.resultingArmorDisplay,
            delta = comparison.armorDeltaDisplay
        )
    }
}

@Composable
private fun StatPreviewRow(
    label: String,
    current: String,
    resulting: String,
    delta: String
) {
    val deltaColor = when {
        delta.startsWith("+") -> PositiveGreen
        delta.startsWith("−") || delta.startsWith("-") -> WarningAmber
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(label, modifier = Modifier.weight(1f), style = MaterialTheme.typography.labelMedium)
        Text(
            text = "$current → $resulting",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = delta,
            style = MaterialTheme.typography.labelLarge,
            color = deltaColor
        )
    }
}

@Composable
private fun ItemIdentity(item: GearItemUiState, accent: Color, modifier: Modifier = Modifier) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Box(
            modifier = Modifier.size(94.dp).clip(RoundedCornerShape(20.dp)).background(Brush.radialGradient(listOf(accent.copy(alpha = 0.27f), ObsidianSurface2))),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(item.iconAssetKey.drawableResId()),
                contentDescription = stringResource(item.titleStringKey.stringResId()),
                modifier = Modifier.size(70.dp),
                contentScale = ContentScale.Fit
            )
        }
        Text(stringResource(item.titleStringKey.stringResId()), style = MaterialTheme.typography.titleMedium, maxLines = 2, overflow = TextOverflow.Clip, textAlign = TextAlign.Center)
        Text(
            listOfNotNull(item.equipmentSlot?.let { slotLabel(it) }, stringResource(item.rarityTitleStringKey.stringResId())).joinToString(" · "),
            style = MaterialTheme.typography.labelSmall,
            color = accent,
            maxLines = 1,
            overflow = TextOverflow.Clip
        )
    }
}

/** Displays source-provided effects only; no power score or invented numeric delta is derived. */
@Composable
private fun FactualEffectColumn(title: String, item: GearItemUiState?, accent: Color) {
    Column(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(ObsidianSurface2.copy(alpha = 0.78f)).padding(8.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Text(title, style = MaterialTheme.typography.labelSmall, color = accent)
        if (item == null) {
            Text(stringResource(R.string.gear_no_item), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            Text(slotLabel(item.equipmentSlot ?: EquipmentSlot.WEAPON), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            item.effects.forEach { effect ->
                Text(effectSummary(effect), style = MaterialTheme.typography.bodySmall, maxLines = 2, overflow = TextOverflow.Clip)
            }
            if (item.effects.isEmpty()) Text(stringResource(R.string.gear_no_effects), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun FactualEffectComparison(candidate: GearItemUiState, equipped: GearItemUiState?) {
    Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
        Text(stringResource(R.string.gear_effect_comparison_title), style = MaterialTheme.typography.labelLarge, color = ResourceGold)
        FactualEffectGroup(stringResource(R.string.gear_candidate_provides), candidate.effects, PositiveGreen)
        if (equipped != null) FactualEffectGroup(stringResource(R.string.gear_current_provides), equipped.effects, WarningAmber)
    }
}

@Composable
private fun FactualEffectGroup(title: String, effects: List<GearEffectUiState>, accent: Color) {
    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Text(title, style = MaterialTheme.typography.labelSmall, color = accent)
        if (effects.isEmpty()) {
            Text(stringResource(R.string.gear_no_effects), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            effects.forEach { effect ->
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("•", style = MaterialTheme.typography.bodySmall, color = accent)
                    Text(effectSummary(effect), style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun AffixList(item: GearItemUiState, onIntent: (GearUiIntent) -> Unit) {
    var pendingAffixId by remember(item.instanceId) { mutableStateOf<ContentId?>(null) }
    val affixes = listOfNotNull(item.mainStat) + item.affixes
    if (affixes.isEmpty()) return
    affixes.firstOrNull { it.affixId == pendingAffixId }?.let { affix ->
        AlertDialog(
            onDismissRequest = { pendingAffixId = null },
            title = { Text("Reroll ${stringResource(affix.titleStringKey.stringResId())}?") },
            text = { Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Current roll: ${affix.rolledValue}")
                Text("Possible result: ${affix.minimumRoll}–${affix.maximumRoll}. The new value can be lower, equal, or higher.")
                Text("Cost: ${item.refinementMaterialCostDisplay} refinement material. Enhancement and other stat lines stay unchanged.")
            } },
            confirmButton = { GameButton(onClick = {
                pendingAffixId = null
                onIntent(GearUiIntent.Refine(item.instanceId, affix.affixId))
            }, enabled = item.canRefine) { Text("Spend material and reroll") } },
            dismissButton = { TextButton(onClick = { pendingAffixId = null }) { Text("Cancel") } }
        )
    }
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            stringResource(R.string.gear_affixes_title),
            style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.1.sp),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        affixes.forEach { affix ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Image(
                    painter = painterResource(affix.iconAssetKey.drawableResId()),
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    contentScale = ContentScale.Fit
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        stringResource(
                            R.string.gear_affix_format,
                            stringResource(affix.titleStringKey.stringResId()),
                            affix.rolledValue
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = if (affix.isMainStat) ResourceGold else ResonanceTeal
                    )
                    if (affix.isMainStat) {
                        Text(
                            "Main stat",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                if (item.canRefine) {
                    GameOutlinedButton(
                        onClick = {
                            pendingAffixId = affix.affixId
                        },
                        modifier = Modifier.widthIn(min = 72.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                    ) {
                        Text("Refine", maxLines = 1)
                    }
                }
            }
        }
    }
}

@Composable
private fun GearEnhancementPanel(
    item: GearItemUiState,
    useProtection: Boolean,
    onToggleProtection: () -> Unit,
    onEnhance: () -> Unit
) {
    GameCard(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = ObsidianSurface1.copy(alpha = 0.9f)),
        border = BorderStroke(1.dp, ResourceGold.copy(alpha = 0.38f)),
        accent = ResourceGold
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                "BASE ENHANCEMENT",
                style = MaterialTheme.typography.labelLarge,
                color = ResourceGold
            )
            Text(
                item.enhancementLabel + " → " + item.enhancementTargetLabel,
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                "Success chance: " + item.enhancementSuccessChanceDisplay +
                    " · failstack: " + item.enhancementFailstack + "/20",
                style = MaterialTheme.typography.bodySmall
            )
            Text(
                "Cost: " + item.enhancementMaterialCostDisplay +
                    " enhancement material",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                "Failure without protection: " + item.enhancementFailureLevelDisplay +
                    " · failstack → " + item.enhancementFailureFailstackDisplay,
                style = MaterialTheme.typography.bodySmall,
                color = WarningAmber
            )
            if (item.enhancementProtectionGemCostDisplay != "0") {
                GameChoiceButton(
                    selected = useProtection,
                    onClick = onToggleProtection,
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                ) {
                    Text(
                        if (useProtection) {
                            "Protection ON · costs " + item.enhancementProtectionGemCostDisplay + " Gem"
                        } else {
                            "Use protection · costs " + item.enhancementProtectionGemCostDisplay + " Gem"
                        },
                        maxLines = 1,
                        overflow = TextOverflow.Clip
                    )
                }
                if (useProtection) {
                    Text(
                        "A protected failure keeps " + item.enhancementLabel +
                            " and still consumes materials.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
            } else {
                Text(
                    "Protection unlocks from PRI onward. Materials are always consumed; no durability is lost.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            val canAttempt = if (useProtection) {
                item.canEnhanceWithProtection
            } else {
                item.canEnhance
            }
            GameButton(
                onClick = onEnhance,
                enabled = canAttempt,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    when {
                        item.enhancementTargetLabel == "MAX" -> "PEN reached"
                        canAttempt -> "Attempt " + item.enhancementTargetLabel
                        else -> "Need materials or Gems"
                    }
                )
            }
        }
    }
}

@Composable
private fun CapacityPanel(state: GearUiState, onExpand: () -> Unit) {
    val capacity = state.capacity
    PremiumPanel(
        backgroundResId = R.drawable.panel_secondary_premium,
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
        verticalSpacing = 9.dp
    ) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    if (capacity.progressionBlocked) stringResource(R.string.gear_inventory_full_title) else stringResource(R.string.gear_capacity_title),
                    style = MaterialTheme.typography.titleMedium,
                    color = if (capacity.progressionBlocked) WarningAmber else MaterialTheme.colorScheme.onSurface
                )
                Text(
                    stringResource(R.string.gear_capacity_expand_format, capacity.capacityPerExpansion, capacity.nextExpansionGoldCostDisplay),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Clip
                )
            }
            GameStatusPill(if (capacity.atMaximumCapacity) stringResource(R.string.gear_capacity_maximum) else "${capacity.normalAvailable} OPEN", if (capacity.progressionBlocked) WarningAmber else ResonanceTeal)
        }
        CapacityMeter(stringResource(R.string.gear_normal_capacity_label), capacity.normalUsed, capacity.normalCapacity, capacity.normalProgressUnits, ResonanceTeal)
        CapacityMeter(stringResource(R.string.gear_overflow_capacity_label), capacity.overflowUsed, capacity.overflowCapacity, capacity.overflowProgressUnits, WarningAmber)
        if (capacity.progressionBlocked) Text(stringResource(R.string.gear_inventory_full_message), style = MaterialTheme.typography.bodySmall, color = WarningAmber)
        GameButton(
            onClick = onExpand,
            enabled = !capacity.atMaximumCapacity && capacity.canAffordExpansion,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                when {
                    capacity.atMaximumCapacity -> stringResource(R.string.gear_capacity_maximum)
                    !capacity.canAffordExpansion -> stringResource(R.string.gear_capacity_need_gold)
                    else -> stringResource(R.string.gear_expand_capacity)
                }
            )
        }
    }
}

@Composable
private fun CapacityMeter(label: String, used: Long, total: Long, progressUnits: Int, accent: Color) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, style = MaterialTheme.typography.labelMedium, color = accent)
            Text(stringResource(R.string.gear_capacity_count_format, used, total), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        GameProgressBar(
            progressUnits = progressUnits,
            accent = accent,
            modifier = Modifier.fillMaxWidth(),
            height = 7.dp,
            contentDescriptionText = stringResource(R.string.gear_capacity_count_format, used, total)
        )
    }
}

@Composable
private fun StorageTabs(
    stashSelected: Boolean,
    inventoryCount: Long,
    inventoryCapacity: Long,
    stashCount: Long,
    onSelectInventory: () -> Unit,
    onSelectStash: () -> Unit
) {
    GameCard(
        modifier = Modifier.fillMaxWidth(),
        border = BorderStroke(1.dp, ObsidianOutline.copy(alpha = 0.58f)),
        colors = CardDefaults.cardColors(containerColor = ObsidianSurface1.copy(alpha = 0.92f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(6.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            GameChoiceButton(
                selected = !stashSelected,
                onClick = onSelectInventory,
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(horizontal = 7.dp, vertical = 0.dp)
            ) {
                Text(
                    stringResource(R.string.gear_inventory_tab_format, inventoryCount, inventoryCapacity),
                    style = MaterialTheme.typography.labelSmall,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Clip,
                    textAlign = TextAlign.Center
                )
            }
            GameChoiceButton(
                selected = stashSelected,
                onClick = onSelectStash,
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(horizontal = 7.dp, vertical = 0.dp)
            ) {
                Text(
                    stringResource(R.string.gear_stash_tab_format, stashCount),
                    style = MaterialTheme.typography.labelSmall,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Clip,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun InventoryToolbar(
    state: GearUiState,
    showingStash: Boolean,
    visibleItemIds: Set<InstanceId>,
    selectedItemIds: Set<InstanceId>,
    rarityFilter: Rarity?,
    slotFilter: EquipmentSlot?,
    sortModeId: String,
    lockedOnly: Boolean,
    onSelectAll: () -> Unit,
    onSelectRarity: (Rarity?) -> Unit,
    onSelectSlot: (EquipmentSlot?) -> Unit,
    onToggleSort: () -> Unit,
    onToggleLocked: () -> Unit,
    onSelectKeepRarity: (Rarity) -> Unit,
    onDisableAutoSalvage: () -> Unit,
    onSalvageBelow: () -> Unit,
    onBulkSalvage: () -> Unit,
    onClearSelection: () -> Unit
) {
    val storageAccent = if (showingStash) WarningAmber else ResonanceTeal
    GameCard(
        modifier = Modifier.fillMaxWidth(),
        border = BorderStroke(1.dp, storageAccent.copy(alpha = 0.44f)),
        colors = CardDefaults.cardColors(containerColor = ObsidianSurface1.copy(alpha = 0.96f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            GameSectionHeader(
                eyebrow = stringResource(
                    if (showingStash) R.string.gear_overflow_title else R.string.gear_inventory_title
                ),
                title = if (showingStash) {
                    stringResource(
                        R.string.gear_overflow_count_format,
                        state.capacity.overflowUsed,
                        state.capacity.overflowCapacity
                    )
                } else {
                    stringResource(
                        R.string.gear_inventory_count_short_format,
                        state.capacity.normalUsed,
                        state.capacity.normalCapacity
                    )
                },
                subtitle = stringResource(
                    if (showingStash) R.string.gear_overflow_item_note else R.string.gear_canonical_note
                )
            )

            Text(
                stringResource(R.string.gear_filter_rarity_label),
                style = MaterialTheme.typography.labelSmall,
                color = storageAccent
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                GameChoiceButton(
                    selected = rarityFilter == null,
                    onClick = { onSelectRarity(null) },
                    modifier = Modifier.widthIn(min = 62.dp),
                    contentPadding = PaddingValues(horizontal = 9.dp, vertical = 0.dp)
                ) {
                    Text(
                        stringResource(R.string.gear_filter_all_short),
                        style = MaterialTheme.typography.labelSmall,
                        maxLines = 1,
                        overflow = TextOverflow.Clip
                    )
                }
                Rarity.ordered().forEach { rarity ->
                    GameChoiceButton(
                        selected = rarityFilter == rarity,
                        onClick = { onSelectRarity(rarity) },
                        modifier = Modifier.widthIn(min = 78.dp),
                        contentPadding = PaddingValues(horizontal = 9.dp, vertical = 0.dp)
                    ) {
                        Text(
                            rarityLabel(rarity),
                            style = MaterialTheme.typography.labelSmall,
                            maxLines = 1,
                            overflow = TextOverflow.Clip
                        )
                    }
                }
            }

            Text(
                stringResource(R.string.gear_filter_slot_label),
                style = MaterialTheme.typography.labelSmall,
                color = storageAccent
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                GameChoiceButton(
                    selected = slotFilter == null,
                    onClick = { onSelectSlot(null) },
                    modifier = Modifier.widthIn(min = 82.dp),
                    contentPadding = PaddingValues(horizontal = 9.dp, vertical = 0.dp)
                ) {
                    Text(
                        stringResource(R.string.gear_filter_all_slots_short),
                        style = MaterialTheme.typography.labelSmall,
                        maxLines = 1,
                        overflow = TextOverflow.Clip
                    )
                }
                EquipmentSlot.values().forEach { slot ->
                    GameChoiceButton(
                        selected = slotFilter == slot,
                        onClick = { onSelectSlot(slot) },
                        modifier = Modifier.widthIn(min = 78.dp),
                        contentPadding = PaddingValues(horizontal = 9.dp, vertical = 0.dp)
                    ) {
                        Text(
                            slotLabel(slot),
                            style = MaterialTheme.typography.labelSmall,
                            maxLines = 1,
                            overflow = TextOverflow.Clip
                        )
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                if (!showingStash) {
                    GameChoiceButton(
                        selected = lockedOnly,
                        onClick = onToggleLocked,
                        modifier = Modifier.widthIn(min = 106.dp),
                        contentPadding = PaddingValues(horizontal = 9.dp, vertical = 0.dp)
                    ) {
                        Text(
                            stringResource(
                                if (lockedOnly) R.string.gear_locked_only else R.string.gear_show_all
                            ),
                            style = MaterialTheme.typography.labelSmall,
                            maxLines = 1,
                            overflow = TextOverflow.Clip
                        )
                    }
                }
                GameOutlinedButton(
                    onClick = onToggleSort,
                    modifier = Modifier.widthIn(min = 112.dp),
                    contentPadding = PaddingValues(horizontal = 9.dp, vertical = 0.dp)
                ) {
                    Text(
                        stringResource(
                            if (sortModeId == "rarity") {
                                R.string.gear_sort_rarity
                            } else {
                                R.string.gear_sort_newest
                            }
                        ),
                        style = MaterialTheme.typography.labelSmall,
                        maxLines = 1,
                        overflow = TextOverflow.Clip
                    )
                }
                GameOutlinedButton(
                    onClick = onSelectAll,
                    modifier = Modifier.widthIn(min = 104.dp),
                    contentPadding = PaddingValues(horizontal = 9.dp, vertical = 0.dp)
                ) {
                    Text(
                        stringResource(R.string.gear_select_all),
                        style = MaterialTheme.typography.labelSmall,
                        maxLines = 1,
                        overflow = TextOverflow.Clip
                    )
                }
                if (selectedItemIds.isNotEmpty()) {
                    GameOutlinedButton(
                        onClick = onClearSelection,
                        modifier = Modifier.widthIn(min = 84.dp),
                        contentPadding = PaddingValues(horizontal = 9.dp, vertical = 0.dp)
                    ) {
                        Text(
                            stringResource(R.string.gear_cancel),
                            style = MaterialTheme.typography.labelSmall,
                            maxLines = 1,
                            overflow = TextOverflow.Clip
                        )
                    }
                }
            }

            if (!showingStash) {
                AutoSalvagePolicy(
                    state = state,
                    onDisable = onDisableAutoSalvage,
                    onSelectKeepRarity = onSelectKeepRarity,
                    onSalvageBelow = onSalvageBelow
                )
            }

            if (selectedItemIds.isNotEmpty()) {
                SelectionActionBar(
                    count = selectedItemIds.size,
                    visibleCount = visibleItemIds.size,
                    onSalvage = onBulkSalvage,
                    onClear = onClearSelection
                )
            }
        }
    }
}

@Composable
private fun AutoSalvagePolicy(
    state: GearUiState,
    onDisable: () -> Unit,
    onSelectKeepRarity: (Rarity) -> Unit,
    onSalvageBelow: () -> Unit
) {
    GameCard(
        modifier = Modifier.fillMaxWidth(),
        border = BorderStroke(1.dp, ResourceGold.copy(alpha = 0.38f)),
        colors = CardDefaults.cardColors(containerColor = ResourceGold.copy(alpha = 0.07f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(9.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .widthIn(min = 0.dp),
                    verticalArrangement = Arrangement.spacedBy(1.dp)
                ) {
                    Text(
                        stringResource(R.string.gear_auto_salvage_title),
                        style = MaterialTheme.typography.labelMedium,
                        color = ResourceGold
                    )
                    Text(
                        if (state.autoSalvageEnabled) {
                            stringResource(
                                R.string.gear_auto_salvage_summary_format,
                                rarityLabel(state.minimumKeepRarity)
                            )
                        } else {
                            stringResource(R.string.gear_auto_salvage_off_detail)
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Clip
                    )
                }
                GameStatusPill(
                    text = stringResource(
                        if (state.autoSalvageEnabled) {
                            R.string.gear_auto_salvage_on_short
                        } else {
                            R.string.gear_auto_salvage_off_short
                        }
                    ),
                    accent = if (state.autoSalvageEnabled) PositiveGreen else ObsidianOutline
                )
            }
            Text(
                stringResource(R.string.gear_auto_salvage_detail),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Clip
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                GameChoiceButton(
                    selected = !state.autoSalvageEnabled,
                    onClick = onDisable,
                    modifier = Modifier.widthIn(min = 58.dp),
                    contentPadding = PaddingValues(horizontal = 9.dp, vertical = 0.dp)
                ) {
                    Text(
                        stringResource(R.string.gear_auto_salvage_off_short),
                        style = MaterialTheme.typography.labelSmall,
                        maxLines = 1
                    )
                }
                Rarity.ordered().forEach { rarity ->
                    GameChoiceButton(
                        selected = state.autoSalvageEnabled && state.minimumKeepRarity == rarity,
                        onClick = { onSelectKeepRarity(rarity) },
                        modifier = Modifier.widthIn(min = 92.dp),
                        contentPadding = PaddingValues(horizontal = 9.dp, vertical = 0.dp)
                    ) {
                        Text(
                            stringResource(R.string.gear_keep_rarity, rarityLabel(rarity)),
                            style = MaterialTheme.typography.labelSmall,
                            maxLines = 1,
                            overflow = TextOverflow.Clip
                        )
                    }
                }
            }
            GameOutlinedButton(
                onClick = onSalvageBelow,
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp)
            ) {
                Text(
                    stringResource(
                        R.string.gear_salvage_below_action,
                        rarityLabel(state.minimumKeepRarity)
                    ),
                    style = MaterialTheme.typography.labelSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Clip
                )
            }
        }
    }
}

@Composable
private fun SelectionActionBar(count: Int, visibleCount: Int, onSalvage: () -> Unit, onClear: () -> Unit) {
    GameCard(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = ResonanceTeal.copy(alpha = 0.12f)),
        border = BorderStroke(1.dp, ResonanceTeal.copy(alpha = 0.42f))
    ) {
        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                stringResource(R.string.gear_selection_count_format, count, visibleCount),
                style = MaterialTheme.typography.labelLarge,
                color = ResonanceTeal
            )
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                GameOutlinedButton(onClick = onClear, modifier = Modifier.weight(1f)) { Text(stringResource(R.string.gear_cancel)) }
                GameButton(onClick = onSalvage, modifier = Modifier.weight(1f)) { Text(stringResource(R.string.gear_salvage_selected)) }
            }
        }
    }
}

@Composable
private fun ArmoryInventoryRow(item: GearItemUiState, selected: Boolean, bulkSelected: Boolean, onSelect: () -> Unit, onToggleSelected: () -> Unit) {
    val accent = rarityAccent(item.rarityId)
    GameCard(
        onClick = onSelect,
        modifier = Modifier.fillMaxWidth(),
        border = BorderStroke(if (selected) 2.dp else 1.dp, if (selected) ResourceGold else accent.copy(alpha = 0.62f)),
        colors = CardDefaults.cardColors(containerColor = if (selected) ResourceGold.copy(alpha = 0.10f) else ObsidianSurface1)
    ) {
        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(modifier = Modifier.size(56.dp).clip(RoundedCornerShape(14.dp)).background(accent.copy(alpha = 0.16f)), contentAlignment = Alignment.Center) {
                    Image(
                        painter = painterResource(item.iconAssetKey.drawableResId()),
                        contentDescription = stringResource(item.titleStringKey.stringResId()),
                        modifier = Modifier.size(44.dp),
                        contentScale = ContentScale.Fit
                    )
                }
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(stringResource(item.titleStringKey.stringResId()), style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Clip)
                    Text(listOfNotNull(item.equipmentSlot?.let { slotLabel(it) }, stringResource(item.rarityTitleStringKey.stringResId())).joinToString(" · "), style = MaterialTheme.typography.labelMedium, color = accent, maxLines = 1, overflow = TextOverflow.Clip)
                    item.effects.firstOrNull()?.let { effect -> Text(effectSummary(effect), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Clip) }
                }
                Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    when {
                        item.locked -> GameStatusPill(stringResource(R.string.gear_locked_state), ResourceGold)
                        item.equippedSlot != null -> GameStatusPill(stringResource(R.string.gear_status_equipped), ResonanceTeal)
                    selected -> GameStatusPill(stringResource(R.string.gear_inspecting), ResourceGold)
                    }
                    Text(stringResource(R.string.gear_salvage_value_format, item.salvageGoldDisplay), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Clip)
                }
            }
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    stringResource(if (selected) R.string.gear_tap_inspect_comparison else R.string.gear_tap_inspect),
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                GameChoiceButton(selected = bulkSelected, onClick = onToggleSelected, enabled = item.canSalvage, contentPadding = PaddingValues(horizontal = 10.dp)) {
                    Text(stringResource(if (bulkSelected) R.string.gear_selected else R.string.gear_select))
                }
            }
        }
    }
}

@Composable
private fun StashToolbar(selectedCount: Int, onSelectAll: () -> Unit, onSalvage: () -> Unit, onClear: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        GameOutlinedButton(onClick = onSelectAll, modifier = Modifier.weight(1f)) { Text(stringResource(R.string.gear_select_all)) }
        if (selectedCount > 0) {
            GameOutlinedButton(onClick = onClear, modifier = Modifier.weight(1f)) { Text(stringResource(R.string.gear_cancel)) }
            GameButton(onClick = onSalvage, modifier = Modifier.weight(1f)) { Text(stringResource(R.string.gear_salvage_selected)) }
        }
    }
}

@Composable
private fun StashInventoryRow(
    item: GearItemUiState,
    selected: Boolean,
    bulkSelected: Boolean,
    onSelect: () -> Unit,
    onToggleSelected: () -> Unit,
    onIntent: (GearUiIntent) -> Unit,
    onRequestSalvage: () -> Unit
) {
    val accent = rarityAccent(item.rarityId)
    GameCard(
        onClick = onSelect,
        modifier = Modifier.fillMaxWidth(),
        border = BorderStroke(if (selected) 2.dp else 1.dp, if (selected) ResourceGold else accent),
        colors = CardDefaults.cardColors(containerColor = WarningAmber.copy(alpha = 0.08f))
    ) {
        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Image(
                    painter = painterResource(item.iconAssetKey.drawableResId()),
                    contentDescription = stringResource(item.titleStringKey.stringResId()),
                    modifier = Modifier.size(54.dp),
                    contentScale = ContentScale.Fit
                )
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(stringResource(item.titleStringKey.stringResId()), style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Clip)
                    Text(stringResource(item.rarityTitleStringKey.stringResId()), style = MaterialTheme.typography.labelSmall, color = accent)
                    Text(stringResource(R.string.gear_salvage_value_format, item.salvageGoldDisplay), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                if (selected) GameStatusPill(stringResource(R.string.gear_inspecting), ResourceGold)
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                GameChoiceButton(selected = bulkSelected, onClick = onToggleSelected, modifier = Modifier.weight(1f)) {
                    Text(stringResource(if (bulkSelected) R.string.gear_selected else R.string.gear_select))
                }
                GameButton(onClick = { onIntent(GearUiIntent.ClaimOverflow(item.instanceId)) }, enabled = item.canClaimOverflow, modifier = Modifier.weight(1f)) {
                    Text(if (item.canClaimOverflow) stringResource(R.string.gear_claim_overflow) else stringResource(R.string.gear_claim_need_slot))
                }
                GameOutlinedButton(onClick = onRequestSalvage, enabled = item.canSalvageOverflow, modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.gear_salvage_overflow))
                }
            }
        }
    }
}

@Composable
private fun EmptyFocusPanel() {
    PremiumPanel(backgroundResId = R.drawable.panel_secondary_premium, modifier = Modifier.fillMaxWidth(), contentPadding = PaddingValues(14.dp)) {
        Text(stringResource(R.string.gear_inventory_empty), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun EmptyStoragePanel(message: String) {
    GameCard(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = ObsidianSurface1), border = BorderStroke(1.dp, ObsidianOutline.copy(alpha = 0.52f))) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(stringResource(R.string.gear_nothing_here), style = MaterialTheme.typography.labelSmall, color = ResourceGold)
            Text(message, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun GearFeedbackBanner(feedback: GearFeedbackUiState) {
    val isError = feedback.kind == GearFeedbackKind.COMMAND_REJECTED
    GameCard(
        modifier = Modifier.fillMaxWidth().eventFeedbackPulse(feedback.sequenceNumber),
        colors = CardDefaults.cardColors(containerColor = if (isError) MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.72f) else ResonanceTeal.copy(alpha = 0.12f)),
        border = BorderStroke(1.dp, if (isError) MaterialTheme.colorScheme.error else ResonanceTeal.copy(alpha = 0.4f))
    ) {
        Row(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(if (isError) "!" else "✓", style = MaterialTheme.typography.titleMedium, color = if (isError) MaterialTheme.colorScheme.error else ResonanceTeal)
            Text(gearFeedbackText(feedback), style = MaterialTheme.typography.bodyMedium, color = if (isError) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onSurface)
        }
    }
}

@Composable
private fun gearFeedbackText(feedback: GearFeedbackUiState): String = when (feedback.kind) {
    GearFeedbackKind.EQUIPPED -> stringResource(R.string.gear_feedback_equipped)
    GearFeedbackKind.UNEQUIPPED -> stringResource(R.string.gear_feedback_unequipped)
    GearFeedbackKind.LOCKED -> stringResource(R.string.gear_feedback_locked)
    GearFeedbackKind.UNLOCKED -> stringResource(R.string.gear_feedback_unlocked)
    GearFeedbackKind.SALVAGED -> stringResource(R.string.gear_feedback_salvaged_format, feedback.amountDisplay ?: "0")
    GearFeedbackKind.CAPACITY_EXPANDED -> stringResource(R.string.gear_feedback_capacity_format, feedback.previousCapacity ?: 0L, feedback.newCapacity ?: 0L, feedback.amountDisplay ?: "0")
    GearFeedbackKind.OVERFLOW_CLAIMED -> stringResource(R.string.gear_feedback_overflow_claimed)
    GearFeedbackKind.OVERFLOW_SALVAGED -> stringResource(R.string.gear_feedback_overflow_salvaged_format, feedback.amountDisplay ?: "0")
    GearFeedbackKind.ITEM_TO_OVERFLOW -> stringResource(R.string.gear_feedback_item_to_overflow)
    GearFeedbackKind.ENHANCED -> if (feedback.enhancementSucceeded == true) {
        stringResource(R.string.gear_feedback_enhancement_success, feedback.amountDisplay ?: "?")
    } else {
        stringResource(R.string.gear_feedback_enhancement_failed, feedback.amountDisplay ?: "?")
    }
    GearFeedbackKind.REFINED -> stringResource(
        R.string.gear_feedback_refined,
        feedback.amountDisplay ?: "?"
    )
    GearFeedbackKind.PROGRESSION_BLOCKED -> stringResource(R.string.gear_feedback_blocked)
    GearFeedbackKind.PROGRESSION_UNBLOCKED -> stringResource(R.string.gear_feedback_unblocked_format, feedback.availableStorageSlots ?: 0L)
    GearFeedbackKind.COMMAND_REJECTED -> rejectionMessage(feedback.rejectionCode)
}

@Composable
private fun rejectionMessage(code: CommandRejectionCode?): String = when (code) {
    CommandRejectionCode.INVALID_ARGUMENT -> stringResource(R.string.gear_rejection_invalid_argument)
    CommandRejectionCode.INVALID_STATE -> stringResource(R.string.gear_rejection_invalid_state)
    CommandRejectionCode.UNKNOWN_CONTENT -> stringResource(R.string.gear_rejection_unknown_content)
    CommandRejectionCode.LOCKED -> stringResource(R.string.gear_rejection_locked)
    CommandRejectionCode.NOT_OWNED -> stringResource(R.string.gear_rejection_not_owned)
    CommandRejectionCode.ALREADY_OWNED -> stringResource(R.string.gear_rejection_already_owned)
    CommandRejectionCode.INSUFFICIENT_RESOURCE -> stringResource(R.string.gear_rejection_insufficient_gold)
    CommandRejectionCode.CAPACITY_EXCEEDED -> stringResource(R.string.gear_rejection_capacity)
    CommandRejectionCode.COOLDOWN_ACTIVE -> stringResource(R.string.gear_rejection_generic)
    CommandRejectionCode.STALE_PREVIEW -> stringResource(R.string.gear_rejection_generic)
    CommandRejectionCode.UNSUPPORTED -> stringResource(R.string.gear_rejection_generic)
    CommandRejectionCode.NOT_READY -> stringResource(R.string.gear_rejection_not_ready)
    CommandRejectionCode.ALREADY_CLAIMED -> stringResource(R.string.gear_rejection_generic)
    null -> stringResource(R.string.gear_rejection_generic)
}

@Composable
private fun SalvageConfirmationDialog(item: GearItemUiState, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.gear_salvage_confirm_title)) },
        text = { Text(stringResource(R.string.gear_salvage_confirm_message, stringResource(item.titleStringKey.stringResId()), item.salvageGoldDisplay)) },
        confirmButton = { GameButton(onClick = onConfirm) { Text(stringResource(R.string.gear_salvage_confirm_action)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.gear_cancel)) } }
    )
}

@Composable
private fun effectSummary(effect: GearEffectUiState): String = when (effect.kind) {
    GearEffectKind.FLAT_ATTACK_POWER -> stringResource(R.string.gear_effect_attack_format, effect.amountDisplay)
    GearEffectKind.FLAT_ARMOR -> stringResource(R.string.gear_effect_armor_format, effect.amountDisplay)
    GearEffectKind.RESONANCE_CHARGE_BONUS -> stringResource(R.string.gear_effect_resonance_format, effect.affinityTitleStringKey?.let { stringResource(it.stringResId()) } ?: stringResource(R.string.gear_unknown_affinity), effect.amountDisplay)
    GearEffectKind.SKILL_TRAIT -> stringResource(R.string.gear_effect_skill_trait, listOfNotNull(effect.amountDisplay, effect.skillTitleStringKey?.let { stringResource(it.stringResId()) }).joinToString(" · "))
}

@Composable
private fun rarityLabel(rarity: Rarity): String = when (rarity) {
    Rarity.COMMON -> stringResource(R.string.content_rarity_common)
    Rarity.UNCOMMON -> stringResource(R.string.content_rarity_uncommon)
    Rarity.RARE -> stringResource(R.string.content_rarity_rare)
    Rarity.EPIC -> stringResource(R.string.content_rarity_epic)
    Rarity.LEGENDARY -> stringResource(R.string.content_rarity_legendary)
}

@Composable
private fun rarityAccent(rarityId: ContentId): Color = when (Rarity.values().firstOrNull { it.id == rarityId }) {
    Rarity.COMMON, null -> ObsidianOutline
    Rarity.UNCOMMON -> ResonanceTeal
    Rarity.RARE -> Color(0xFF6DA8FF)
    Rarity.EPIC -> ArcaneViolet
    Rarity.LEGENDARY -> ResourceGold
}

@Composable
private fun slotLabel(slot: EquipmentSlot): String = when (slot) {
    EquipmentSlot.WEAPON -> stringResource(R.string.gear_slot_weapon)
    EquipmentSlot.ARMOR -> stringResource(R.string.gear_slot_armor)
    EquipmentSlot.HELM -> stringResource(R.string.gear_slot_helm)
    EquipmentSlot.BOOTS -> stringResource(R.string.gear_slot_boots)
    EquipmentSlot.ACCESSORY -> stringResource(R.string.gear_slot_accessory)
    EquipmentSlot.CATALYST -> stringResource(R.string.gear_slot_catalyst)
}
