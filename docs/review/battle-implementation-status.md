# Battle redesign implementation status

This copy contains the Battle-first implementation and the supporting start/menu flow. The
canonical Kotlin simulation remains authoritative; Compose only projects state and dispatches
intents.

## Implemented

- Auto Battle is a top-right play/pause control backed by the existing Doctrine enabled state.
  Disabling Doctrine leaves the canonical Basic Attack fallback intact and does not change manual
  skill queue semantics.
- The global HUD now prioritizes Gold with a resource icon, level, Echo, and an authoritative
  segmented XP bar. Inventory is no longer a permanent HUD resource row.
- Battle shows stage, wave, and the actual Push/Farm mode, with a compact icon-led Retreat action.
- The battlefield keeps hero-left/enemies-right composition, projects current/max health for the
  hero and every visible enemy, shows threat timing when projected, and uses real combat impacts.
- Ordinary kill feedback is separated from completion state. Canonical Gold, XP, and item facts
  now appear as a transparent, staggered formation-side float that fades away; the persistent
  stage-completion surface is reserved for actual victory.
- The weighted battlefield grows with the available height and scales actor art by enemy count.
  The lower area is a finite vertical viewport, the Combat Readout is a responsive grid without
  horizontal scrolling, and the hero artwork no longer carries Attack/Armor/DPS labels. Four and
  five-enemy formations use two rows so actors are not squeezed into a single strip.
- Battle reward presentation is now a bounded, presentation-only feed. Canonical kill/reward
  facts are retained as transient items before Compose renders them one at a time. Ordinary floats
  use a 2.5 second fade; a defeated-actor snapshot is held for 560 ms (760 ms at victory), and an
  incoming replacement formation is withheld for that same presentation window when the runtime
  advances a wave in the same transition. These timings affect only rendering, never simulation
  time, attack speed, cooldowns, RNG, or rewards.
- The lower Battle area exposes authoritative Attack, HP, Armor, Crit, Crit DMG, DPS, Skill Power,
  and Healing Power projections with vector stat artwork. The loadout entry is a 48 dp minimum
  artwork-led glyph with a screen-reader description; the existing navigation remains intact.
- Status icons use projected asset, polarity, authored detail key, stack count, duration, and
  runtime potency. Tap or long-press opens a dismissible detail menu near the icon.
- Battle-only high-salience labels use Android's readable platform monospace family as a
  dependency-free terminal influence; body copy and long names keep the normal readable family.
- The Battle skill dock no longer contains the Basic Attack upgrade control or internal evaluator
  priority wording. The existing upgrade domain path is preserved elsewhere.
- New expeditions require a hero name. Existing saves without a name are preserved and blocked at
  a naming gate until the player supplies one. V6 saves migrate to V7 without replacing progress.
- Main menu states support Continue, New Expedition, Settings, and Android-appropriate Exit.

## Evidence

Verified by source inspection:

- Package, Gradle, Kotlin, Compose, SDK, persistence, deterministic simulation, Doctrine, combat
  projection, event presenter, and navigation were inspected before editing.
- The original uploaded ZIP remains untouched; this project is a separate copy.
- Auto Battle maps through `DoctrineUiIntent.SetDoctrineEnabled` to the canonical `ReplaceDoctrine`
  command.
- `GameEventPresenter` preserves every `EnemyKilled` instance ID in transient `killReward` data
  without changing the serialized domain event schema. Rewards remain aggregate because the
  canonical reward events do not carry enemy instance IDs; the UI does not falsely split them.
- Hero identity is stored in `MetaState`, survives Chronicle copies, and is encoded through an
  explicit V6-to-V7 migration.

Verified by static checks in this workspace:

- Kotlin delimiter smoke check: pass.
- Android resource-reference scan: pass.
- Drawable and values XML parsing: pass.
- `git diff --check`: pass.
- The current Battle change has a presentation reward feed keyed by canonical event sequence and
  stable UI tokens. Items are removed only after their float finishes (or when leaving Battle / a
  new combat sequence starts), so an unrendered canonical reward is not dropped or reordered.
- Presenter regression coverage was added for reward-only transitions and multiple real enemy
  kills; it is source-present but not executed here because Gradle could not start.

Not verified here:

- Kotlin/Compose compilation and unit-test execution. The Gradle wrapper could not download its
  configured Gradle 9.0.0 distribution in this environment (`java.net.SocketException: Network is
  unreachable`), so no task reached source compilation.
- APK assembly, emulator/device installation, screenshots, performance profiling, accessibility
  inspection, and real first-session playtesting. No Android SDK/ADB target is available here.

## Android Code Studio handoff

1. Import/unzip this project as an Android project without changing the package name.
2. Allow Android Code Studio to obtain the configured Gradle 9.0.0 distribution and project
   dependencies.
3. Run `:app:compileDebugKotlin` first, then `:app:testDebugUnitTest`.
4. Quick Run or assemble with `:app:assembleDebug` only after those steps pass.
5. Test a fresh install, name the hero, toggle Auto Battle on/off, queue a manual skill, complete
   an encounter, inspect its actual reward, return to Battle, and then test an existing unnamed
   save and a named save after process recreation.

This is an **unverified source candidate** until those Android Studio/device gates pass.
