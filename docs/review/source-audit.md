# Source and verification audit

This audit records what was established before the redesign changes. It is intentionally
separate from the implementation design spec so that source evidence is not confused with
playtest evidence.

## Verified through source inspection

- The project is a native Kotlin/Jetpack Compose Android application with package
  `com.idlerpg.game`.
- The checked-in build configuration uses AGP 8.13.0, Kotlin 2.1.0, Gradle 9.0.0, compile SDK
  36, target SDK 34, min SDK 21, and Java/Kotlin target 17.
- Combat is deterministic and event-driven. The runtime models encounters with multiple waves,
  enemy roles, skills, cooldowns, queued manual actions, Doctrine automation, resonance,
  adaptation, boss phases, victory, defeat, and retreat.
- Existing progression includes Gold, XP, equipment and loot, skill loadouts/evolutions,
  inventory locking/salvage/overflow, Push/Farm, offline simulation, quests, achievements,
  discoveries, Chronicle/Legacy, and save migrations/recovery.
- Existing authored artwork is present in `app/src/main/res/drawable-nodpi`; this pass reuses it
  and does not introduce unlicensed external artwork.
- The ordinary Compose UI scan found no player-facing AI chain-of-thought, evaluator transcript,
  or developer commentary. Deterministic evaluator names and useful source comments remain in
  the domain/runtime code.
- The source supports a complete presentation path for Battle, Build/Gear, Skills, Auto Battle,
  Adventure/World, and Growth/Progress. The redesign keeps those systems and makes their states
  more explicit rather than adding new currencies or unrelated systems.

## Verified by static checks in this workspace

- `git diff --check` passes.
- `app/src/main/res/values/strings.xml` parses as XML.
- No duplicate string resource names were found.
- Every direct `R.string.*` reference in the main Kotlin source resolves to a declared string.
- Changed Kotlin files have balanced delimiters under a lexical source scan.
- A JUnit 4 dependency and executable contract test entry were added for fresh-save navigation
  and canonical reward projection.

## Not verified here

- Gradle compilation, unit-test execution, APK assembly, installation, launch, screenshots,
  accessibility services, performance profiling, and device playtesting.
- Fresh-install versus existing-save migration behavior at runtime; offline return and duplicate
  reward behavior across force-stop/background cycles; rapid repeated taps; process recreation;
  compact screens; enlarged text; reduced motion; haptics; and long-name rendering.

The verification blocker is environmental: the Gradle wrapper could not download Gradle 9.0.0
from `services.gradle.org` because the workspace returned `java.net.SocketException: Network is
unreachable`. No APK is included or claimed as built.

## Assumptions requiring Android Code Studio verification

- The existing generated Gradle/dependency setup resolves normally when Android Code Studio has
  access to its configured repositories and SDK 36.
- The event-derived reward panel remains visible for the full victory transition in the production
  runtime and its item-instance focus resolves to the retained inventory item, not overflow or an
  auto-salvaged item.
- Saved-state restoration of navigation and nested detail routes should be checked on process
  recreation; the canonical game save remains the source of gameplay truth.
