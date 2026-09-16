#!/usr/bin/env bash

set +e

SERIAL="$ANDROID_SERIAL"
if [ -z "$SERIAL" ]; then
  SERIAL="emulator-5554"
fi

PACKAGE="com.idlerpg.game"
ACTIVITY=".MainActivity"
SCENARIO="$SCENARIO"
if [ -z "$SCENARIO" ]; then
  SCENARIO="launch_crash"
fi

OUT="artifacts/emulator"
COMPONENT="$PACKAGE/$ACTIVITY"
TRACE_DEVICE="/data/local/tmp/idlerpg-launch.pftrace"
PERF_DATA_DEVICE="/data/local/tmp/idlerpg.perf.data"

mkdir -p "$OUT"

adb_cmd() {
  adb -s "$SERIAL" "$@"
}

printf 'scenario=%s\npackage=%s\ncomponent=%s\nserial=%s\n' \
  "$SCENARIO" "$PACKAGE" "$COMPONENT" "$SERIAL" > "$OUT/run-info.txt"

adb_cmd devices -l > "$OUT/00-adb-devices.txt" 2>&1
adb_cmd shell getprop > "$OUT/00-device-properties.txt" 2>&1
adb_cmd shell wm size > "$OUT/00-window-size.txt" 2>&1
adb_cmd shell wm density > "$OUT/00-window-density.txt" 2>&1
adb_cmd shell cmd package resolve-activity --brief "$PACKAGE" > "$OUT/00-resolved-activity.txt" 2>&1
adb_cmd shell dumpsys package "$PACKAGE" > "$OUT/00-package-state.txt" 2>&1

adb_cmd install -r "app/build/outputs/apk/debug/app-debug.apk" > "$OUT/00-install.txt" 2>&1
adb_cmd shell pm clear "$PACKAGE" > "$OUT/00-clear-data.txt" 2>&1
adb_cmd shell am force-stop "$PACKAGE" > "$OUT/00-force-stop.txt" 2>&1
adb_cmd shell dumpsys gfxinfo "$PACKAGE" reset > "$OUT/00-gfxinfo-reset.txt" 2>&1
adb_cmd logcat -c

adb_cmd shell rm -f "$TRACE_DEVICE"
adb_cmd shell perfetto -o "$TRACE_DEVICE" -t 20s --app "$PACKAGE" \
  sched freq idle am wm gfx view binder_driver hal dalvik \
  > "$OUT/02-perfetto-console.txt" 2>&1 &
PERFETTO_HOST_PID=$!
sleep 1

adb_cmd shell am start -W -n "$COMPONENT" > "$OUT/01-launch.txt" 2>&1
LAUNCH_CODE=$?
sleep 8

adb_cmd exec-out screencap -p > "$OUT/03-after-launch.png" 2> "$OUT/03-after-launch-screenshot.err"
adb_cmd exec-out uiautomator dump /dev/tty > "$OUT/03-after-launch-ui.xml" 2> "$OUT/03-after-launch-ui.err"
adb_cmd shell dumpsys activity activities > "$OUT/03-after-launch-activity.txt" 2>&1
adb_cmd shell dumpsys window windows > "$OUT/03-after-launch-window.txt" 2>&1
adb_cmd shell dumpsys input_method > "$OUT/03-after-launch-input-method.txt" 2>&1

PID_AFTER_LAUNCH="$(adb_cmd shell pidof -s "$PACKAGE" 2>/dev/null | tr -d '\r' | xargs)"
printf 'launch_exit_code=%s\npid_after_launch=%s\n' \
  "$LAUNCH_CODE" "$PID_AFTER_LAUNCH" > "$OUT/04-launch-state.txt"

if [ -n "$PID_AFTER_LAUNCH" ]; then
  adb_cmd shell rm -f "$PERF_DATA_DEVICE"
  adb_cmd shell simpleperf record --app "$PACKAGE" \
    -o "$PERF_DATA_DEVICE" -e cpu-clock -f 4000 -g --duration 12 \
    > "$OUT/07-simpleperf-console.txt" 2>&1
  adb_cmd pull "$PERF_DATA_DEVICE" "$OUT/idlerpg.perf.data" \
    > "$OUT/07-simpleperf-pull.txt" 2>&1
  adb_cmd shell simpleperf report -i "$PERF_DATA_DEVICE" \
    > "$OUT/07-simpleperf-report.txt" 2>&1
else
  printf 'Skipped because the app had no live process after launch.\n' \
    > "$OUT/07-simpleperf-console.txt"
fi

wait "$PERFETTO_HOST_PID" > "$OUT/02-perfetto-wait.txt" 2>&1
adb_cmd pull "$TRACE_DEVICE" "$OUT/idlerpg-launch.pftrace" \
  > "$OUT/02-perfetto-pull.txt" 2>&1

adb_cmd shell dumpsys gfxinfo "$PACKAGE" > "$OUT/08-gfxinfo.txt" 2>&1
adb_cmd shell dumpsys gfxinfo "$PACKAGE" framestats \
  > "$OUT/08-gfxinfo-framestats.txt" 2>&1
adb_cmd shell dumpsys meminfo "$PACKAGE" > "$OUT/08-meminfo.txt" 2>&1
adb_cmd shell dumpsys cpuinfo > "$OUT/08-cpuinfo.txt" 2>&1
adb_cmd logcat -d -v threadtime > "$OUT/09-logcat.txt" 2>&1
adb_cmd logcat -b crash -d > "$OUT/09-crash-buffer.txt" 2>&1
adb_cmd shell dumpsys activity activities > "$OUT/09-final-activity.txt" 2>&1
adb_cmd shell dumpsys window windows > "$OUT/09-final-window.txt" 2>&1
adb_cmd shell dumpsys package "$PACKAGE" > "$OUT/09-final-package-state.txt" 2>&1

if [ -n "$PID_AFTER_LAUNCH" ]; then
  LAUNCH_VERDICT="launch_did_not_crash"
elif grep -Eiq "$PACKAGE|FATAL EXCEPTION|AndroidRuntime" "$OUT/09-crash-buffer.txt"; then
  LAUNCH_VERDICT="launch_crash_evidence"
else
  LAUNCH_VERDICT="app_not_running_after_launch"
fi

{
  printf 'scenario=%s\n' "$SCENARIO"
  printf 'package=%s\n' "$PACKAGE"
  printf 'component=%s\n' "$COMPONENT"
  printf 'serial=%s\n' "$SERIAL"
  printf 'launch_exit_code=%s\n' "$LAUNCH_CODE"
  printf 'pid_after_launch=%s\n' "$PID_AFTER_LAUNCH"
  printf 'launch_verdict=%s\n' "$LAUNCH_VERDICT"
} > "$OUT/verdict.txt"

printf 'scenario=%s launch_verdict=%s\n' "$SCENARIO" "$LAUNCH_VERDICT"

if [ -n "$GITHUB_STEP_SUMMARY" ]; then
  {
    echo "## IdleRPG emulator QA"
    echo
    echo "- Scenario: $SCENARIO"
    echo "- Launch verdict: $LAUNCH_VERDICT"
    echo "- Evidence is uploaded under artifacts/emulator/."
  } >> "$GITHUB_STEP_SUMMARY"
fi

exit 0
