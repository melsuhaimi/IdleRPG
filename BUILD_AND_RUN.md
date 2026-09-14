# IdleRPG — build and run

This project is an Android Jetpack Compose app with application id `com.idlerpg.game`.

## Android Studio

1. Open this folder as an existing Gradle project.
2. Use JDK 17 and install Android SDK 36 when Android Studio asks for missing components.
3. Let Gradle sync finish, then run the `app` configuration on an emulator or connected device.

The production launcher is `MainActivity`. The simulator harness is not registered in the
production manifest.

## Command line

From the project root:

```bash
./gradlew test assembleDebug
```

On Windows, use `gradlew.bat test assembleDebug`.

The debug APK is written to:

```text
app/build/outputs/apk/debug/app-debug.apk
```

The redesign keeps the deterministic game engine, save format, content IDs, domain commands,
and existing art assets intact. Presentation now exposes the real Battle → reward → upgrade
loop, including event-derived Gold/XP/item facts and the existing Gold-funded attack upgrade.

## Verification status for this handoff

Static source checks and `git diff --check` were run. A JUnit 4 entry point covers the fresh-save
Skills route and the battle reward projection contract. This environment could not execute
Gradle because the wrapper needed to download Gradle 9.0.0 and the network returned
`Network is unreachable`; no APK, compilation result, emulator launch, screenshot, or device
playtest is claimed here.

After opening the project in Android Code Studio, run:

```bash
./gradlew :app:test
./gradlew :app:assembleDebug
```

Then Quick Run a fresh install and an existing save. Verify Battle → reward details → Upgrade,
Skills entry/exit with Back, Equipment comparison/equip, Adventure Push/Farm, Auto Battle,
offline return, save recovery, compact layout, enlarged text, reduced motion, and haptics.
