# UI polish pass

This pass keeps the existing gameplay, navigation, presentation models, asset IDs, and Compose
screen contracts intact. It changes shared presentation primitives and one battle-layout rule so
the game reads more like a finished mobile RPG instead of a collection of utility panels.

## Second, deeper polish pass

The first pass was too incremental. This revision is a real presentation pass across the shell and
the main playable destinations.

- Supporting cards now have a layered obsidian surface, top highlight, stronger edge treatment,
  and a more deliberate 18dp radius.
- Focal panels have a larger framed silhouette, brighter artwork treatment, and a stronger top
  highlight so they read as game panels rather than generic Material cards.
- Section headers now carry an explicit gold-to-slate rule, making the screen hierarchy visible
  before the player reads the body copy.
- HUD Gold and Legacy are now resource capsules; XP remains a segmented progress rail instead of
  looking like another plain text row.
- The shell backdrop is more atmospheric with authored artwork plus a bottom vignette, while the
  bottom navigation has an active gradient field and a framed top rule.
- Battle now has a framed encounter header and a bordered theater stage. Combat stats are arranged
  as icon-led console cells so Attack, HP, Armor, Crit, Crit DMG, DPS, Skill PWR, and Heal PWR can
  be scanned quickly.
- Build now uses a real horizontally scrollable mode rail for Equipment, Skills, and Auto Battle;
  the armory hero is larger and the equipment nodes have more presence.
- Adventure uses a clear selected Push/Farm control, and the map banner/region rhythm has more
  breathing room.
- Growth, Skills, and Auto Battle screens now use the same 16dp spacing rhythm as the redesigned
  cards and headers.

## Third, hierarchy polish pass

This pass strengthens the visual hierarchy across the whole playable shell instead of treating
every card as the same kind of surface.

- `GameCard` and `PremiumPanel` now accept an optional semantic accent rail. The accent identifies
  what the surface means: gold for progression and rewards, teal for active expedition/build
  state, violet for arcane and skill state, and red for danger or boss state.
- World region cards, encounter cards, stage nodes, adaptation/mutation rows, and feedback states
  now carry contextual accents instead of relying on identical neutral cards.
- Gear, skills, doctrine, and progress cards now communicate selected, enabled, locked, rarity,
  and focus states through the same accent system.
- The top HUD has a framed resource rail with gold/echo side accents and an authored top rule.
- Navigation selection now has a restrained outline in addition to its halo, icon tint, and rule.
- Battle telemetry and the command dock now have deliberate top rails, while each combat metric and
  skill tile has its own accent edge for faster scanning.
- The world hero banner and main menu/error surfaces now use the same focal framing language.

## What changed

### Shared visual language

`app/src/main/kotlin/com/idlerpg/game/ui/component/premium/GameUi.kt`

- Section eyebrows use `ResourceGold`, matching the game's reward and progression language.
- Metric chips now use a 10dp radius, a denser outline, 48dp minimum height, and semibold values.
- Status pills are 36dp tall with a more visible state dot and semibold label.
- Default progress bars are 10dp tall, making health, XP, and progression state readable at a
  glance.
- Filled and outlined game buttons use a 12dp radius while preserving the 48dp touch target.

`app/src/main/kotlin/com/idlerpg/game/ui/component/premium/GameCard.kt`

- Secondary cards use a consistent 16dp radius, a slightly opaque obsidian surface, and a clearer
  1dp outline. Premium focal panels continue to override the border when they need an accent.

### Shell and navigation

`app/src/main/kotlin/com/idlerpg/game/ui/component/hud/GlobalHud.kt`

- The global HUD and its level badge now use the same softer corner language as the main panels.

`app/src/main/kotlin/com/idlerpg/game/ui/navigation/IdleRpgNavigation.kt`

- The bottom navigation dock uses a 16dp container, 64dp item height, 34dp selected halo, 24dp
  icons, and a 28x3dp selected rule. Selection remains communicated by gold plus the rule, so the
  battle artwork stays visually dominant.

### Battle screen composition

`app/src/main/kotlin/com/idlerpg/game/ui/screen/battle/BattleScreen.kt`

- The event-banner reservation is now 8dp when no event is active, while active events retain
  their compact or regular banner height. This gives the hero/enemy formation more breathing room
  during the common idle-combat state without changing combat timing or event rendering.

## Deliberately unchanged

- No gameplay reducer, simulation, save format, intent, route, content ID, or asset key changed.
- No combat values or progression formulas changed.
- Existing accessibility semantics and reduced-motion handling remain in place.

## Verification status

The source was checked after each edit for the expected imports, call sites, and unchanged public
component signatures. A Gradle compile was attempted, but the wrapper could not download its
Gradle 9.0.0 distribution in the sandbox because outbound network access was unavailable before
compilation began. The workspace also has no `adb` binary, so device rendering and APK assembly
remain unverified in this environment.

## Compile-fix follow-up

The polish candidate was corrected after AndroidIDE reported two missing Compose layout imports:

- `GameUi.kt` now imports `androidx.compose.foundation.layout.width` for the metric-chip accent rail.
- `GearScreen.kt` now imports `androidx.compose.foundation.layout.widthIn` for the build-mode buttons.

A later AndroidIDE compile identified one more missing layout import introduced by the accent rail:

- `GameCard.kt` now imports `androidx.compose.foundation.layout.fillMaxHeight` for the vertical
  semantic accent rail.
