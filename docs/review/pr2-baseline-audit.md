# PR #2 baseline audit

Date: 2026-09-15
Branch: `feat/responsive-battle-layout`
Baseline audited: `18af045ee82711c1d9c8cd0ea312d5e5c099baf1`
Research contract: [idle RPG loop contract](../research/idle-rpg-loop-contract.md)

This review separates implementation standards from the requested gameplay contract. It is a static audit of all 319 production Kotlin files plus the existing unit/scenario suite. It is not a claim that static review can prove zero defects on every Android device.

## Standards findings

### S1 — duplicated active-enemy limit can drift from the domain contract

- **Evidence:** `app/src/main/kotlin/com/idlerpg/game/domain/system/combat/CombatSystem.kt:65-67` accepts `1..5`.
- **Evidence:** `app/src/main/kotlin/com/idlerpg/game/domain/system/world/WorldSystem.kt:224-230` accepts wave formations in `1..5`.
- **Contract source:** `EncounterDefinition.MAX_ACTIVE_ENEMIES` is `3`, and encounter construction already enforces the elite/boss single-enemy rule.
- **Risk:** direct combat or future callers can bypass authored encounter validation and create formations the game design does not support.
- **Resolution:** both systems now use the shared `EncounterDefinition.MAX_ACTIVE_ENEMIES` constant, and the multi-enemy scenario covers the three-enemy ceiling.

### S2 — world projection relies on a nullable attack id

- **Evidence:** `app/src/main/kotlin/com/idlerpg/game/presentation/projection/WorldProjector.kt:73-76` calls `enemyDefinition.attackDefinitionId!!`.
- **Evidence:** `app/src/main/kotlin/com/idlerpg/game/data/content/ContentValidator.kt:358-365` permits a null attack id.
- **Risk:** a content-valid enemy can crash the world screen when its first wave is projected. The combat loop intentionally supports enemies without an attack by skipping their scheduled action, so the projection and content contract disagree.
- **Resolution:** encounter-referenced enemies now require an authored attack during validation, and projection reports the validated-contract failure instead of asserting on a nullable id. The regression scenario covers the missing-attack path.

### S3 — low nonzero rates are displayed as zero

- **Evidence:** `app/src/main/kotlin/com/idlerpg/game/presentation/format/GameNumberFormatter.kt:23-28` truncates any rate with at most three integer digits to at most two decimals.
- **Example:** a stored rate of `0.001` becomes `0`.
- **Risk:** players lose meaningful progression information in the UI, especially for early or slow idle rates.
- **Resolution:** fractional rates now retain three readable decimal places, show `<0.001` below the display floor, and have regression coverage.

## Requested gameplay findings

### P1 — the route was capped and the final stage self-looped

- **Evidence:** `app/src/main/kotlin/com/idlerpg/game/data/content/TrainingHollowWorldContent.kt:10-12` caps the route at stage 72.
- **Evidence:** `TrainingHollowWorldContent.kt:74` points the last encounter to itself.
- **Risk:** the long-run idle loop has no authored post-72 push/farm runway; the user request explicitly calls for substantial stages and grinding.
- **Resolution:** extend the route to 120 deterministic stages, add milestone bosses at 84, 96, 108, and 120, and stop push automation at the final stage. Farm mode can still target the highest cleared stage. The three-enemy/one-elite-or-boss constraint remains enforced.

### P2 — gear/skill progression needs a longer runway

- **Evidence:** the current content has a small initial equipment/skill catalog, while the runtime already has deterministic loot, salvage, equipment, skill loadout, and upgrade systems.
- **Risk:** the systems read as prototype scaffolding because the player reaches the end of authored content before those decisions can mature.
- **Resolution:** authored content now includes 120 stages, eight milestone bosses, 15 skills, 11 item definitions, and five build-defining mid/late-game equipment pieces. Existing deterministic systems own the outcomes; no AI/LLM decision path was added.

### P3 — automation must preserve meaningful player decisions

- **Evidence:** existing design docs define the loop as battle → rewards → inspect/equip/salvage or upgrade → battle.
- **Risk:** presenting automation as the game itself creates the “AI-oriented” feel the user rejected.
- **Resolution:** the existing auto-battle mode remains a repetition reducer, while push/farm mode, skill queue/loadout, all-rarity salvage threshold, lock/equip/salvage actions, and capacity expansion remain explicit commands. The capacity harness now follows the actual safe-overflow contract rather than waiting for a hard block that cannot occur.

## Baseline verification

- The repository workflow command is `bash ./gradlew testDebugUnitTest assembleDebug`.
- The clean baseline PR workflow passed before this audit.
- No TODO/FIXME/XXX/HACK markers were found in production Kotlin.
- The full runtime/device path is not proven by this static pass; after fixes, CI and Android emulator/device checks are required.

## Implementation follow-up

- `ContentRegistry.contains` now includes authored status IDs, closing a lookup gap between `statusOrNull` and generic content membership.
- Inventory capacity remains lossless: normal slots fill first, retained loot moves to soft overflow, and overflow can be claimed or salvaged. The simulator’s capacity scenario now stops when both capacities are actually full.
- Battle nameplates can wrap title/health text to two lines, health separators are breakable, and compact HUD resource values scale down by length so long gold/Lumen values remain readable on narrow screens.
- The all-rarity auto-salvage policy is preserved: the player can choose Off, Common+, Uncommon+, Rare+, Epic+, or Legendary+, while build-defining traits are protected by the authored loot rule.
