# Functionality polish audit

This pass focused on public gameplay seams where a valid player action could previously
lose a reward, reject a legitimate progression, or crash autonomous play.

## Gameplay fixes

- `RewardSystem.grant` no longer treats the visible inventory/overflow soft cap as a hard
  precondition. `InventorySystem.addGeneratedItem` already preserves kept items in overflow,
  including beyond the display capacity, so combat and claims now follow that contract.
- `QuestSystem` and `AchievementSystem` count `ItemSentToOverflow` as item acquisition. A kept
  item is still acquired when it cannot fit in normal inventory.
- Quest and achievement reward claims no longer fail just because the soft storage display is
  full; loot is delivered through the same deterministic overflow path as combat loot.
- `WorldSystem.handleConfigureAutomation` validates explicit and persisted farm targets against
  authored content, the active region, region membership, and clear state. Invalid legacy values
  are rejected or cleared instead of being stored for a later scheduler crash.
- `EncounterSystem.automationTarget` validates scheduler targets before autonomous starts and
  safely falls back to the current authored encounter for malformed legacy automation data.
- `InventoryCapacitySystem.handleExpand` rejects arithmetic overflow in legacy purchase
  counters/cost calculations without spending currency or mutating state. Storage headroom
  projections saturate instead of overflowing a `Long`.
- Empty overflow bulk-salvage selections now return `INVALID_ARGUMENT`, matching normal bulk
  salvage behavior.
- `GameRuntimeController` keeps the single writer alive when fresh-game deployment or naming
  fails, preserving the prior menu/save state or the naming gate instead of publishing a false
  playable state.

## Regression coverage added

- Guaranteed rewards spill into overflow when both normal and visible overflow capacity are full.
- Overflow item acquisition advances the authored quest objective.
- Extreme capacity purchase counters reject atomically.
- Unknown automation targets are rejected and malformed persisted targets recover safely.
- Empty overflow bulk-salvage is rejected consistently.

## Verification limitation

The project’s Gradle wrapper requires Gradle 9.0.0. This environment has no cached Gradle
distribution and cannot reach `services.gradle.org`, so `./gradlew test --offline --no-daemon
--console=plain` stops before compilation. ADB is also unavailable here, so emulator QA could
not be run in this workspace.
