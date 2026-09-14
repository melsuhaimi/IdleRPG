package com.idlerpg.game.ui.simulation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/** Canonical developer-only on-device verification surface for Foundations 1-18 + FBE. */
@Composable
fun SimulationHarnessScreen(
    viewModel: SimulationHarnessViewModel
) {
    val uiState by viewModel.uiState.collectAsState()
    val enabled = !uiState.running

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            text = "Idle RPG Backend Simulator",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )

        Text(
            text = "Canonical developer surface. Controls dispatch GameCommand or advance GameRuntime; Compose never calculates gameplay outcomes.",
            style = MaterialTheme.typography.bodyMedium
        )

        KeyValue("Seed", uiState.seed.toString())
        KeyValue("Profile", uiState.profile.label)
        KeyValue("Status", uiState.status)
        KeyValue("Last command", uiState.lastCommandResult)
        uiState.canonicalRegression?.let { KeyValue("Canonical 24h", it) }
        uiState.progressText?.let { KeyValue("Progress", it) }
        uiState.error?.let {
            Text(
                text = "Error: $it",
                color = MaterialTheme.colorScheme.error,
                fontWeight = FontWeight.Bold
            )
        }

        SectionTitle("Profiles")
        OutlinedButton(
            onClick = viewModel::resetVerificationProfile,
            enabled = enabled,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Reset verification profile (10,000 slots)")
        }
        OutlinedButton(
            onClick = viewModel::resetNormalCapacityProfile,
            enabled = enabled,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Reset normal-capacity profile (60 + 20)")
        }
        OutlinedButton(
            onClick = viewModel::resetCurrentProfile,
            enabled = enabled,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Reset current profile")
        }

        SectionTitle("Time controls")
        SimulatorButton("+1 sec", enabled, viewModel::advanceOneSecond)
        SimulatorButton("+10 sec", enabled, viewModel::advanceTenSeconds)
        SimulatorButton("+1 min", enabled, viewModel::advanceOneMinute)
        SimulatorButton("+10 min", enabled, viewModel::advanceTenMinutes)
        SimulatorButton("+1 hour", enabled, viewModel::advanceOneHour)
        SimulatorButton("+24 hours", enabled, viewModel::advanceTwentyFourHours)

        SectionTitle("Canonical FBE regression")
        Text(
            text = "This scenario runs the 30-stage Training Hollow for 24 hours twice from seed 404, claims First Hunt, then proves identical canonical state and metrics while checking meaningful loot remains below kill count and inventory stays bounded.",
            style = MaterialTheme.typography.bodySmall
        )
        SimulatorButton(
            "Run canonical +24h regression",
            enabled,
            viewModel::runCanonicalTwentyFourHourRegression
        )

        SectionTitle("FBE-01 — Manual skill + loadout")
        SimulatorButton("Equip Heavy Strike", enabled, viewModel::equipHeavyStrike)
        SimulatorButton("Queue Heavy Strike", enabled, viewModel::queueHeavyStrike)
        OutlinedSimulatorButton("Clear queued skill", enabled, viewModel::clearQueuedSkill)

        SectionTitle("FBE-02 — Manual claims")
        Text(
            text = "First Hunt becomes ready after 3 Slime kills. Claim is a separate command.",
            style = MaterialTheme.typography.bodySmall
        )
        SimulatorButton("Claim First Hunt", enabled, viewModel::claimFirstHunt)
        Text(
            text = "The Forged Flame scenario uses an explicitly labelled debug unlock/loadout fixture, then real Doctrine + Resonance + Convergence + achievement reactions. Reward remains manual.",
            style = MaterialTheme.typography.bodySmall
        )
        SimulatorButton(
            "Run Forged Flame completion scenario",
            enabled,
            viewModel::runForgedFlameCompletionScenario
        )
        SimulatorButton(
            "Claim Forged Flame achievement",
            enabled,
            viewModel::claimForgedFlameAchievement
        )

        SectionTitle("FBE-03 — Echo shop")
        SimulatorButton(
            "Earn 5 Echo via Chronicle scenario",
            enabled,
            viewModel::earnFiveEchoViaChronicle
        )
        SimulatorButton(
            "Purchase Adaptation Forecast (5 Echo)",
            enabled,
            viewModel::purchaseAdaptationForecast
        )

        SectionTitle("FBE-04 — Inventory capacity + overflow")
        Text(
            text = "The block scenario resets to normal capacity and advances canonical combat until 60 normal slots + 20 overflow slots are full. The active encounter finishes; only the next encounter is blocked.",
            style = MaterialTheme.typography.bodySmall
        )
        SimulatorButton(
            "Run normal-capacity block scenario",
            enabled,
            viewModel::runNormalCapacityBlockScenario
        )
        SimulatorButton(
            "Expand inventory capacity (+20)",
            enabled,
            viewModel::expandInventoryCapacity
        )
        SimulatorButton(
            "Claim first overflow item",
            enabled,
            viewModel::claimFirstOverflowItem
        )
        OutlinedSimulatorButton(
            "Salvage first overflow item",
            enabled,
            viewModel::salvageFirstOverflowItem
        )

        SectionTitle("Canonical state snapshot")
        KeyValue("Simulation time", SimulationHarnessViewModel.formatDuration(uiState.snapshot.simulationMillis))
        KeyValue("Gold wallet", uiState.snapshot.gold.toString())
        KeyValue("Player level", uiState.snapshot.playerLevel.toString())
        KeyValue("Current XP", uiState.snapshot.playerExperience.toString())
        KeyValue(
            "Normal inventory",
            "${uiState.snapshot.inventoryItems} / ${uiState.snapshot.inventoryCapacity}"
        )
        KeyValue(
            "Overflow",
            "${uiState.snapshot.overflowItems} / ${uiState.snapshot.overflowCapacity}"
        )
        KeyValue("Inventory blocked", uiState.snapshot.inventoryBlocked.toString())
        KeyValue("Next expansion Gold cost", uiState.snapshot.nextExpansionGoldCost)
        KeyValue("Active region", uiState.snapshot.activeRegion)
        KeyValue("Encounter index", uiState.snapshot.encounterIndex?.toString() ?: "none")
        KeyValue("Enemy", uiState.snapshot.enemyDefinitionId)
        KeyValue("Enemy HP", uiState.snapshot.enemyHealth.toString())
        KeyValue("Enemy mutations", uiState.snapshot.enemyMutations)
        KeyValue("Resonance charges", uiState.snapshot.resonanceCharges)
        KeyValue("Adaptation tiers", uiState.snapshot.adaptationTiers)
        KeyValue("Equipped skills", uiState.snapshot.equippedSkills)
        KeyValue("Queued skill", uiState.snapshot.queuedSkill)
        KeyValue(
            "First Hunt",
            "complete=${uiState.snapshot.firstHuntCompletionCount}, claimed=${uiState.snapshot.firstHuntClaimedCount}, ready=${uiState.snapshot.firstHuntReadyToClaim}"
        )
        KeyValue(
            "Forged Flame achievement",
            "complete=${uiState.snapshot.forgedFlameAchievementCompleted}, claimed=${uiState.snapshot.forgedFlameRewardClaimed}, ready=${uiState.snapshot.forgedFlameReadyToClaim}"
        )
        KeyValue("Chronicle", uiState.snapshot.chronicleNumber.toString())
        KeyValue("Echo available", uiState.snapshot.echoAvailable.toString())
        KeyValue("Echo spent", uiState.snapshot.echoSpent.toString())
        KeyValue("Purchased Echo offers", uiState.snapshot.purchasedEchoOffers)
        KeyValue("Adaptation Forecast unlocked", uiState.snapshot.adaptationForecastUnlocked.toString())

        SectionTitle("Observed events since profile reset")
        KeyValue("Enemies defeated", uiState.metrics.enemiesDefeated.toString())
        KeyValue("Encounters cleared", uiState.metrics.encountersCleared.toString())
        KeyValue("Gold granted", uiState.metrics.goldGranted.toString())
        KeyValue("XP granted", uiState.metrics.experienceGranted.toString())
        KeyValue("Items found", uiState.metrics.itemsFound.toString())
        KeyValue("Items added normally", uiState.metrics.normalItemsAdded.toString())
        KeyValue("Items sent to overflow", uiState.metrics.overflowItemsAdded.toString())
        KeyValue("Convergences", uiState.metrics.convergencesTriggered.toString())
        KeyValue("Adaptation tier changes", uiState.metrics.adaptationTierChanges.toString())
        KeyValue("Quest claims", uiState.metrics.questClaims.toString())
        KeyValue("Achievement claims", uiState.metrics.achievementClaims.toString())
        KeyValue("Echo purchases", uiState.metrics.echoPurchases.toString())
        KeyValue("Capacity expansions", uiState.metrics.capacityExpansions.toString())
        KeyValue("Progression blocks", uiState.metrics.progressionBlocks.toString())

        SectionTitle("Recent important events")
        if (uiState.recentLog.isEmpty()) {
            Text("No important events yet.")
        } else {
            uiState.recentLog.asReversed().forEach { line ->
                Text(
                    text = line,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }

        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun SimulatorButton(
    label: String,
    enabled: Boolean,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(label)
    }
}

@Composable
private fun OutlinedSimulatorButton(
    label: String,
    enabled: Boolean,
    onClick: () -> Unit
) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(label)
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(top = 8.dp)
    )
}

@Composable
private fun KeyValue(label: String, value: String) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Text(
            text = "$label:",
            fontWeight = FontWeight.SemiBold
        )
        Text(text = value)
    }
}
