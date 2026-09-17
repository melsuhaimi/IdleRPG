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
  SCENARIO="hourly_full_feature_qa"
fi
SESSION_SECONDS="${SESSION_SECONDS:-3600}"

OUT="artifacts/emulator"
COMPONENT="$PACKAGE/$ACTIVITY"
TRACE_DEVICE="/data/misc/perfetto-traces/idlerpg-hourly.pftrace"
PERF_DATA_DEVICE="/data/local/tmp/idlerpg-hourly.perf.data"
EVENT_LOG="$OUT/feature-events.tsv"
CURRENT_UI=""
FEATURE_ACTION_COUNT=0
SESSION_START=0
PERFETTO_HOST_PID=""
SESSION_COMPLETE_AT=0
GAME_STARTED=0
COMBAT_ACTIVE=0
FINAL_COMBAT_ACTIVE=0

mkdir -p "$OUT"
: > "$EVENT_LOG"
: > "$OUT/feature-outcomes.tsv"

adb_cmd() {
  adb -s "$SERIAL" "$@"
}

record_event() {
  printf '%s\t%s\t%s\n' "$(date -u +%Y-%m-%dT%H:%M:%SZ)" "$1" "$2" >> "$EVENT_LOG"
}

record_outcome() {
  printf '%s\t%s\t%s\n' "$1" "$2" "$3" >> "$OUT/feature-outcomes.tsv"
}

ui_dump() {
  local label="$1"
  adb_cmd exec-out uiautomator dump /dev/tty > "$OUT/$label-ui.xml" 2> "$OUT/$label-ui.err"
}

take_screenshot() {
  local label="$1"
  adb_cmd exec-out screencap -p > "$OUT/$label.png" 2> "$OUT/$label-screenshot.err"
}

dump_ui_action_inventory() {
  local label="$1"
  local xml_path="$OUT/$label-ui.xml"
  python3 - "$xml_path" "$OUT/$label-actions.tsv" <<'PY'
import sys
import xml.etree.ElementTree as ET

xml_path, output_path = sys.argv[1:3]
try:
    raw = open(xml_path, encoding="utf-8", errors="replace").read()
    start = raw.find("<?xml")
    end = raw.find("</hierarchy>")
    if start < 0 or end < 0:
        raise ValueError("hierarchy xml not found")
    root = ET.fromstring(raw[start:end + len("</hierarchy>")])
    with open(output_path, "w", encoding="utf-8") as output:
        output.write("text\tcontent_desc\tclass\tresource_id\tclickable\tenabled\tbounds\n")
        for node in root.iter("node"):
            attrs = node.attrib
            if not (attrs.get("text") or attrs.get("content-desc")):
                continue
            output.write("\t".join([
                attrs.get("text", "").replace("\t", " "),
                attrs.get("content-desc", "").replace("\t", " "),
                attrs.get("class", ""),
                attrs.get("resource-id", ""),
                attrs.get("clickable", ""),
                attrs.get("enabled", ""),
                attrs.get("bounds", ""),
            ]) + "\n")
except Exception as exc:
    with open(output_path, "w", encoding="utf-8") as output:
        output.write("ui_action_inventory_error\t" + repr(exc) + "\n")
PY
}

capture_checkpoint() {
  local label="$1"
  CURRENT_UI="$OUT/$label-ui.xml"
  record_event "checkpoint" "$label"
  take_screenshot "$label"
  ui_dump "$label"
  dump_ui_action_inventory "$label"
  adb_cmd shell dumpsys activity activities > "$OUT/$label-activity.txt" 2>&1
  adb_cmd shell dumpsys window windows > "$OUT/$label-window.txt" 2>&1
  adb_cmd shell dumpsys input_method > "$OUT/$label-input-method.txt" 2>&1
  adb_cmd shell pidof -s "$PACKAGE" > "$OUT/$label-pid.txt" 2>&1
  adb_cmd logcat -d -v threadtime -t 250 > "$OUT/$label-logcat-tail.txt" 2>&1
}

find_ui_center() {
  local xml_path="$1"
  local pattern="$2"
  python3 - "$xml_path" "$pattern" <<'PY'
import re
import sys
import xml.etree.ElementTree as ET

path, pattern = sys.argv[1:3]
try:
    raw = open(path, encoding="utf-8", errors="replace").read()
    start = raw.find("<?xml")
    end = raw.find("</hierarchy>")
    if start < 0 or end < 0:
        raise ValueError("hierarchy xml not found")
    root = ET.fromstring(raw[start:end + len("</hierarchy>")])
    matcher = re.compile(pattern, re.IGNORECASE)
    fallback = None
    for node in root.iter("node"):
        if node.attrib.get("enabled", "true").lower() == "false":
            continue
        values = [node.attrib.get(key, "") for key in ("text", "content-desc", "class", "resource-id")]
        if not any(matcher.search(value) for value in values):
            continue
        bounds = [int(value) for value in re.findall(r"\d+", node.attrib.get("bounds", ""))]
        if len(bounds) != 4:
            continue
        candidate = ((bounds[0] + bounds[2]) // 2, (bounds[1] + bounds[3]) // 2)
        if fallback is None:
            fallback = candidate
        if node.attrib.get("clickable", "false").lower() == "true":
            print(candidate[0], candidate[1])
            break
    else:
        if fallback is not None:
            print(fallback[0], fallback[1])
except Exception:
    pass
PY
}

find_ui_center_by_desc() {
  local xml_path="$1"
  local pattern="$2"
  python3 - "$xml_path" "$pattern" <<'PY'
import re
import sys
import xml.etree.ElementTree as ET

path, pattern = sys.argv[1:3]
try:
    raw = open(path, encoding="utf-8", errors="replace").read()
    start = raw.find("<?xml")
    end = raw.find("</hierarchy>")
    if start < 0 or end < 0:
        raise ValueError("hierarchy xml not found")
    root = ET.fromstring(raw[start:end + len("</hierarchy>")])
    matcher = re.compile(pattern, re.IGNORECASE)
    fallback = None
    for node in root.iter("node"):
        if node.attrib.get("enabled", "true").lower() == "false":
            continue
        value = node.attrib.get("content-desc", "")
        if not matcher.search(value):
            continue
        bounds = [int(item) for item in re.findall(r"\d+", node.attrib.get("bounds", ""))]
        if len(bounds) != 4:
            continue
        candidate = ((bounds[0] + bounds[2]) // 2, (bounds[1] + bounds[3]) // 2)
        if fallback is None:
            fallback = candidate
        if node.attrib.get("clickable", "false").lower() == "true":
            print(candidate[0], candidate[1])
            break
    else:
        if fallback is not None:
            print(fallback[0], fallback[1])
except Exception:
    pass
PY
}

tap_ui_text() {
  local label="$1"
  local pattern="$2"
  local xml_path="${3:-$CURRENT_UI}"
  local coords
  coords="$(find_ui_center "$xml_path" "$pattern")"
  if [ -z "$coords" ]; then
    printf 'target_not_found pattern=%s xml=%s\n' "$pattern" "$xml_path" > "$OUT/$label-tap.txt"
    return 1
  fi
  read -r x y <<< "$coords"
  {
    printf 'pattern=%s\n' "$pattern"
    printf 'bounds_center=%s,%s\n' "$x" "$y"
    printf 'action=input_tap\n'
  } > "$OUT/$label-tap.txt"
  adb_cmd shell input tap "$x" "$y" >> "$OUT/$label-tap.txt" 2>&1
}

tap_ui_desc() {
  local label="$1"
  local pattern="$2"
  local xml_path="${3:-$CURRENT_UI}"
  local coords
  coords="$(find_ui_center_by_desc "$xml_path" "$pattern")"
  if [ -z "$coords" ]; then
    printf 'content_desc_not_found pattern=%s xml=%s\n' "$pattern" "$xml_path" > "$OUT/$label-tap.txt"
    return 1
  fi
  read -r x y <<< "$coords"
  {
    printf 'content_desc_pattern=%s\n' "$pattern"
    printf 'bounds_center=%s,%s\n' "$x" "$y"
    printf 'action=input_tap\n'
  } > "$OUT/$label-tap.txt"
  adb_cmd shell input tap "$x" "$y" >> "$OUT/$label-tap.txt" 2>&1
}

try_tap() {
  local label="$1"
  local pattern="$2"
  local xml_path="${3:-$CURRENT_UI}"
  if tap_ui_text "$label" "$pattern" "$xml_path"; then
    FEATURE_ACTION_COUNT=$((FEATURE_ACTION_COUNT + 1))
    record_outcome "$label" "text" "tapped"
    record_event "tap" "$label"
    sleep 1
    return 0
  fi
  record_outcome "$label" "text" "not-found-or-disabled"
  return 1
}

try_desc() {
  local label="$1"
  local pattern="$2"
  local xml_path="${3:-$CURRENT_UI}"
  if tap_ui_desc "$label" "$pattern" "$xml_path"; then
    FEATURE_ACTION_COUNT=$((FEATURE_ACTION_COUNT + 1))
    record_outcome "$label" "content-desc" "tapped"
    record_event "tap-desc" "$label"
    sleep 1
    return 0
  fi
  record_outcome "$label" "content-desc" "not-found-or-disabled"
  return 1
}

scroll_ui_down() {
  local label="$1"
  local xml_path="${2:-$CURRENT_UI}"
  local coords
  coords="$(python3 - "$xml_path" <<'PY'
import re
import sys
import xml.etree.ElementTree as ET

path = sys.argv[1]
try:
    raw = open(path, encoding="utf-8", errors="replace").read()
    start = raw.find("<?xml")
    end = raw.find("</hierarchy>")
    root = ET.fromstring(raw[start:end + len("</hierarchy>")])
    candidates = []
    for node in root.iter("node"):
        if node.attrib.get("scrollable", "false").lower() != "true":
            continue
        bounds = [int(value) for value in re.findall(r"\d+", node.attrib.get("bounds", ""))]
        if len(bounds) == 4 and bounds[3] - bounds[1] > 300:
            candidates.append(bounds)
    if candidates:
        left, top, right, bottom = max(candidates, key=lambda item: item[3] - item[1])
        print((left + right) // 2, top, bottom)
except Exception:
    pass
PY
)"
  if [ -z "$coords" ]; then
    record_outcome "$label" "scroll" "scroll-container-not-found"
    return 1
  fi
  read -r x top bottom <<< "$coords"
  local start_y=$((bottom - 140))
  local end_y=$((top + 140))
  if [ "$start_y" -le "$end_y" ]; then
    record_outcome "$label" "scroll" "scroll-container-too-short"
    return 1
  fi
  adb_cmd shell input swipe "$x" "$start_y" "$x" "$end_y" 700 > "$OUT/$label-scroll.txt" 2>&1
  record_outcome "$label" "scroll" "down"
  record_event "scroll" "$label"
  sleep 1
  return 0
}

scroll_ui_up() {
  local label="$1"
  local xml_path="${2:-$CURRENT_UI}"
  local coords
  coords="$(python3 - "$xml_path" <<'PY'
import re
import sys
import xml.etree.ElementTree as ET

path = sys.argv[1]
try:
    raw = open(path, encoding="utf-8", errors="replace").read()
    start = raw.find("<?xml")
    end = raw.find("</hierarchy>")
    root = ET.fromstring(raw[start:end + len("</hierarchy>")])
    candidates = []
    for node in root.iter("node"):
        if node.attrib.get("scrollable", "false").lower() != "true":
            continue
        bounds = [int(value) for value in re.findall(r"\d+", node.attrib.get("bounds", ""))]
        if len(bounds) == 4 and bounds[3] - bounds[1] > 300:
            candidates.append(bounds)
    if candidates:
        left, top, right, bottom = max(candidates, key=lambda item: item[3] - item[1])
        print((left + right) // 2, top, bottom)
except Exception:
    pass
PY
)"
  if [ -z "$coords" ]; then
    record_outcome "$label" "scroll" "scroll-container-not-found"
    return 1
  fi
  read -r x top bottom <<< "$coords"
  local start_y=$((top + 140))
  local end_y=$((bottom - 140))
  if [ "$end_y" -le "$start_y" ]; then
    record_outcome "$label" "scroll" "scroll-container-too-short"
    return 1
  fi
  adb_cmd shell input swipe "$x" "$start_y" "$x" "$end_y" 700 > "$OUT/$label-scroll.txt" 2>&1
  record_outcome "$label" "scroll" "up"
  record_event "scroll" "$label"
  sleep 1
  return 0
}

scroll_until_text_down() {
  local label="$1"
  local pattern="$2"
  local max_steps="${3:-12}"
  local step=0
  while [ "$step" -lt "$max_steps" ]; do
    if grep -Eiq "$pattern" "$CURRENT_UI"; then
      record_outcome "$label" "scroll-until-down" "found-after-$step"
      return 0
    fi
    step=$((step + 1))
    scroll_ui_down "$label-step-$step"
    capture_checkpoint "$label-step-$step"
  done
  if grep -Eiq "$pattern" "$CURRENT_UI"; then
    record_outcome "$label" "scroll-until-down" "found-after-$step"
    return 0
  fi
  record_outcome "$label" "scroll-until-down" "not-found-after-$max_steps"
  return 1
}

scroll_until_text_up() {
  local label="$1"
  local pattern="$2"
  local max_steps="${3:-12}"
  local step=0
  while [ "$step" -lt "$max_steps" ]; do
    if grep -Eiq "$pattern" "$CURRENT_UI"; then
      record_outcome "$label" "scroll-until-up" "found-after-$step"
      return 0
    fi
    step=$((step + 1))
    scroll_ui_up "$label-step-$step"
    capture_checkpoint "$label-step-$step"
  done
  if grep -Eiq "$pattern" "$CURRENT_UI"; then
    record_outcome "$label" "scroll-until-up" "found-after-$step"
    return 0
  fi
  record_outcome "$label" "scroll-until-up" "not-found-after-$max_steps"
  return 1
}

cat > "$OUT/feature-plan.txt" <<'EOF'
IdleRPG one-hour feature QA plan
- cold launch, process, activity, menu, new-game/name flow
- accessibility and motion preferences
- Battle: live auto combat, Push/Farm, auto toggle, manual skill queue/clear, loadout entry, retreat/redeploy
- Skills: inspect, equip/unequip, reorder, evolution disclosure/selection
- Adventure: region/stage cards, encounter details, Push/Farm, start/retreat, adaptation and mutation panels
- Build: filters, sort, auto-salvage, inspect/compare, equip/unequip, lock/unlock, enhance/refine, salvage dialogs, stash, capacity
- Auto Battle: presets, confirmation, enable/disable, rule editor, condition/action controls, rule reorder/toggle/edit/remove
- Growth: overview, stats/upgrades, mastery, quests/claims, achievements/claims, Codex, Legacy, Rebirth, Prestige preview/cancel
- persistence relaunch after the measured 3600-second gameplay session
- screenshots/UI trees at every feature checkpoint and every 5 minutes during endurance
EOF

feature_settings() {
  capture_checkpoint "05-menu"
  if try_tap "06-settings-open" '^Settings$'; then
    capture_checkpoint "06-settings"
    try_tap "06-settings-reduced-motion" '^Reduce motion$'
    capture_checkpoint "06-settings-reduced-motion"
    try_tap "06-settings-haptics" '^Combat haptics$'
    capture_checkpoint "06-settings-haptics"
    try_tap "06-settings-close" '^Close$'
    capture_checkpoint "07-menu-after-settings"
  fi
}

feature_skill_loadout() {
  capture_checkpoint "16-skill-loadout"
  scroll_ui_down "16-skill-loadout-scroll-down"
  capture_checkpoint "17-skill-loadout-lower"
  if try_desc "18-skill-inspect-life-drain" '^Life Drain$'; then
    capture_checkpoint "18-skill-life-drain-inspected"
  else
    try_desc "18b-skill-inspect-arcane-blast" '^Arcane Blast$'
    capture_checkpoint "18b-skill-arcane-blast-inspected"
  fi
  try_tap "18-skill-evolution-toggle" '^Skill evolutions$'
  capture_checkpoint "18-skill-evolutions"
  try_tap "19-skill-select-evolution" '^Select evolution$'
  capture_checkpoint "19-skill-evolution-selected"
  try_tap "20-skill-equip" '^Equip$'
  capture_checkpoint "20-skill-equipped"
  try_tap "21-skill-unequip" '^Unequip$'
  capture_checkpoint "21-skill-unequipped"
  try_tap "22-skill-re-equip" '^Equip$'
  capture_checkpoint "22-skill-re-equipped"
  try_tap "23-skill-move-earlier" '^Earlier$'
  capture_checkpoint "23-skill-moved-earlier"
  try_tap "24-skill-move-later" '^Later$'
  capture_checkpoint "24-skill-moved-later"
  scroll_ui_up "25-skill-loadout-scroll-up"
  capture_checkpoint "25-skill-loadout-top"
  try_tap "26-skill-loadout-back" '^Back to Battle$'
  capture_checkpoint "26-battle-after-loadout"
}

feature_battle_controls() {
  capture_checkpoint "11-battle-top"
  scroll_ui_down "12-battle-scroll-down"
  capture_checkpoint "12-battle-lower"
  try_tap "13-battle-expand-skills" '^Show skills$'
  capture_checkpoint "13-battle-skills-expanded"
  try_desc "14-battle-queue-skill" 'Queue skill'
  capture_checkpoint "14-battle-skill-queued"
  try_tap "15-battle-clear-queue" '^Clear queue$'
  capture_checkpoint "15-battle-queue-cleared"
  if try_tap "16-open-skill-loadout" '^(Manage loadout|Skills)$'; then
    feature_skill_loadout
  else
    scroll_ui_up "16-battle-scroll-up"
    capture_checkpoint "16-battle-top-restored"
    if try_tap "17-open-skill-loadout" '^(Manage loadout|Skills)$'; then
      feature_skill_loadout
    fi
  fi
  try_desc "27-battle-auto-toggle-off" '^Auto Battle is on'
  capture_checkpoint "27-battle-auto-off"
  try_desc "28-battle-auto-toggle-on" '^Auto Battle is off'
  capture_checkpoint "28-battle-auto-on"
  try_desc "29-battle-retreat" '^Retreat from the current encounter$'
  capture_checkpoint "29-battle-retreated"
  if try_tap "30-battle-redeploy" '^Start Adventure$'; then
    capture_checkpoint "30-battle-redeployed"
  fi
}

feature_adventure() {
  if ! try_desc "31-nav-adventure" '^Adventure$'; then
    return
  fi
  sleep 2
  capture_checkpoint "31-adventure-top"
  scroll_until_text_down "32-adventure-selected-card" 'View details' 12
  try_tap "32c-adventure-view-details" '^View details$'
  capture_checkpoint "32c-adventure-details"
  try_tap "33c-adventure-hide-details" '^Hide details$'
  capture_checkpoint "33c-adventure-details-hidden"
  scroll_until_text_up "33b-adventure-scroll-top" 'Run mode' 12
  try_tap "34-adventure-push" '^Push$'
  capture_checkpoint "34-adventure-push"
  try_tap "35-adventure-farm" '^Farm$'
  capture_checkpoint "35-adventure-farm"
  try_tap "36-adventure-select-region" '^Select$'
  capture_checkpoint "36-adventure-region-selected"
  scroll_ui_down "37-adventure-scroll-down"
  capture_checkpoint "37-adventure-lower"
  if try_desc "38-adventure-stage-1-card" '^Stage 1, .*Ready to start$'; then
    capture_checkpoint "38-adventure-stage-1-result"
    scroll_until_text_down "38b-adventure-selected-card" 'Start battle|Farm this stage' 12
    if try_tap "38d-adventure-start-battle" '^Start battle$'; then
      capture_checkpoint "38e-adventure-battle-started"
    else
      try_tap "38f-adventure-farm-stage" '^Farm this stage$'
      capture_checkpoint "38g-adventure-farm-stage"
    fi
  fi
  record_outcome "40-adventure-retreat" "text" "deferred-to-battle-retreat-test"
  capture_checkpoint "40-adventure-active-or-selected"
  scroll_until_text_up "41-adventure-scroll-up" 'Run mode' 12
}

feature_build() {
  if ! try_desc "42-nav-build" '^Build$'; then
    return
  fi
  sleep 2
  capture_checkpoint "42-build-top"
  if try_tap "42-build-skills-tab" '^Skills$'; then
    sleep 2
    feature_skill_loadout
    if try_desc "42b-nav-build" '^Build$'; then
      sleep 1
      capture_checkpoint "42b-build-after-skills"
    fi
  fi
  try_tap "43-build-show-details" '^(Show details|Inspect stats and affixes)$'
  capture_checkpoint "43-build-details"
  try_tap "44-build-hide-details" '^Hide details$'
  capture_checkpoint "44-build-details-hidden"
  scroll_ui_down "45-build-scroll-down"
  capture_checkpoint "45-build-lower"
  if try_tap "46-build-rarity-filter" '^All rarities$'; then
    capture_checkpoint "46-build-rarity-filter-open"
    adb_cmd shell input keyevent 4 > "$OUT/46-build-rarity-filter-back.txt" 2>&1
    capture_checkpoint "46-build-rarity-filter-closed"
  else
    capture_checkpoint "46-build-rarity-filter-not-found"
  fi
  if try_tap "47-build-slot-filter" '^All slots$'; then
    capture_checkpoint "47-build-slot-filter-open"
    adb_cmd shell input keyevent 4 > "$OUT/47-build-slot-filter-back.txt" 2>&1
    capture_checkpoint "47-build-slot-filter-closed"
  else
    capture_checkpoint "47-build-slot-filter-not-found"
  fi
  try_tap "48-build-sort-rarity" '^Sort: rarity$'
  capture_checkpoint "48-build-sorted-rarity"
  try_tap "49-build-sort-newest" '^Sort: newest$'
  capture_checkpoint "49-build-sorted-newest"
  try_tap "50-build-auto-salvage" '^Auto-salvage: (on|off)'
  capture_checkpoint "50-build-auto-salvage-toggled"
  try_tap "51-build-select-all" '^Select all$'
  capture_checkpoint "51-build-selected-all"
  try_tap "52-build-salvage-selected" '^Salvage selected$'
  capture_checkpoint "52-build-salvage-selected-dialog"
  try_tap "53-build-cancel-salvage" '^Cancel$'
  capture_checkpoint "53-build-salvage-cancelled"
  try_tap "54-build-expand-capacity" '^Expand capacity$'
  capture_checkpoint "54-build-capacity"
  try_tap "55-build-equip" '^Equip$'
  capture_checkpoint "55-build-equipped"
  try_tap "56-build-unequip" '^Unequip$'
  capture_checkpoint "56-build-unequipped"
  try_tap "57-build-lock" '^Lock$'
  capture_checkpoint "57-build-locked"
  try_tap "58-build-unlock" '^Unlock$'
  capture_checkpoint "58-build-unlocked"
  try_tap "59-build-enhance" '^Enhance$'
  capture_checkpoint "59-build-enhanced"
  try_tap "60-build-refine" '^Refine$'
  capture_checkpoint "60-build-refined"
  try_tap "61-build-claim-stash" '^Claim item$'
  capture_checkpoint "61-build-claim-stash"
  try_tap "62-build-salvage-stash" '^Salvage stash$'
  capture_checkpoint "62-build-salvage-stash"
  scroll_ui_up "63-build-scroll-up"
  capture_checkpoint "63-build-top-restored"
  if try_desc "63b-nav-build" '^Build$'; then
    sleep 1
    capture_checkpoint "63b-build-restored"
  fi
}

feature_doctrine() {
  if grep -Eiq "Action Priority|Auto Battle Rules|No Auto Battle rules" "$CURRENT_UI"; then
    capture_checkpoint "64-doctrine-top"
  else
    if ! try_desc "64-nav-doctrine" '^Auto Battle$'; then
      if ! try_tap "64-build-auto-battle-tab" '^Auto Battle$'; then
        return
      fi
    fi
    sleep 2
    capture_checkpoint "64-doctrine-top"
  fi
  try_tap "65-doctrine-balanced" '^Balanced$'
  capture_checkpoint "65-doctrine-balanced"
  try_tap "66-doctrine-aggressive" '^Aggressive$'
  capture_checkpoint "66-doctrine-aggressive-confirmation"
  try_tap "67-doctrine-replace-rules" '^Replace rules$'
  capture_checkpoint "67-doctrine-aggressive-applied"
  try_tap "68-doctrine-survival" '^Survival$'
  capture_checkpoint "68-doctrine-survival"
  try_tap "69-doctrine-toggle" '^Auto Battle (enabled|disabled)$'
  capture_checkpoint "69-doctrine-toggled"
  scroll_ui_down "70-doctrine-scroll-down"
  capture_checkpoint "70-doctrine-lower"
  try_tap "71-doctrine-add-rule" '^Add rule$'
  capture_checkpoint "71-doctrine-editor"
  scroll_ui_down "72-doctrine-editor-scroll-down"
  capture_checkpoint "72-doctrine-editor-lower"
  try_tap "72-doctrine-condition" '^(Hero HP %|Enemy count)$'
  capture_checkpoint "72-doctrine-condition-added"
  try_tap "73-doctrine-condition-picker" '^(<|≤|=|≥|>)$'
  capture_checkpoint "73-doctrine-condition-picker"
  try_tap "74-doctrine-action-picker" '^(Basic Attack|Use skill)$'
  capture_checkpoint "74-doctrine-action-picker"
  try_tap "75-doctrine-save-rule" '^Save rule$'
  capture_checkpoint "75-doctrine-rule-saved"
  try_tap "76-doctrine-cancel-editor" '^Cancel$'
  capture_checkpoint "76-doctrine-editor-cancelled"
  try_tap "76-doctrine-disable-rule" '^Disable$'
  capture_checkpoint "76-doctrine-rule-disabled"
  try_tap "77-doctrine-enable-rule" '^Enable$'
  capture_checkpoint "77-doctrine-rule-enabled"
  try_tap "78-doctrine-edit-rule" '^Edit$'
  capture_checkpoint "78-doctrine-rule-editor"
  try_tap "79-doctrine-cancel-edit" '^Cancel$'
  capture_checkpoint "79-doctrine-edit-cancelled"
  try_tap "80-doctrine-move-earlier" '^Earlier$'
  capture_checkpoint "80-doctrine-moved-earlier"
  try_tap "81-doctrine-move-later" '^Later$'
  capture_checkpoint "81-doctrine-moved-later"
  try_tap "82-doctrine-remove-rule" '^Remove$'
  capture_checkpoint "82-doctrine-rule-removed"
  scroll_ui_up "83-doctrine-scroll-up"
  capture_checkpoint "83-doctrine-top-restored"
}

feature_progress() {
  if ! try_desc "84-nav-growth" '^Growth$'; then
    return
  fi
  sleep 2
  capture_checkpoint "84-growth-overview"
  local index=0
  local destination=""
  for destination in "Overview" "Stats & Upgrades" "Skill Mastery" "Quests" "Achievements" "Codex" "Legacy" "Prestige"; do
    index=$((index + 1))
    if try_desc "85-growth-tab-$index" "^$destination$"; then
      sleep 1
      capture_checkpoint "85-growth-$index-top"
      try_tap "86-growth-$index-claim" '^Claim reward$'
      capture_checkpoint "86-growth-$index-claim-result"
      try_tap "87-growth-$index-upgrade" '^(x[0-9]+|Upgrade|Buy upgrade|Review Rebirth|\+1 Normal|\+1 Legacy|Reset Normal.*|Reset Legacy.*)$'
      capture_checkpoint "87-growth-$index-upgrade-result"
      try_tap "88-growth-$index-preview" '^Preview Prestige$'
      capture_checkpoint "88-growth-$index-preview-result"
      try_tap "89-growth-$index-preview-cancel" '^Cancel$'
      capture_checkpoint "89-growth-$index-preview-cancelled"
      scroll_ui_down "90-growth-$index-scroll-down"
      capture_checkpoint "90-growth-$index-lower"
      try_tap "91-growth-$index-lower-claim" '^Claim reward$'
      capture_checkpoint "91-growth-$index-lower-claim-result"
      try_tap "92-growth-$index-lower-upgrade" '^(x[0-9]+|Upgrade|Buy upgrade|Review Rebirth|\+1 Normal|\+1 Legacy|Reset Normal.*|Reset Legacy.*)$'
      capture_checkpoint "92-growth-$index-lower-upgrade-result"
      scroll_ui_up "93-growth-$index-scroll-up"
      capture_checkpoint "93-growth-$index-top-restored"
    fi
  done
  try_tap "94-growth-rebirth-review" '^Review Rebirth$'
  capture_checkpoint "94-growth-rebirth-review"
  try_tap "95-growth-rebirth-cancel" '^Cancel$'
  capture_checkpoint "95-growth-rebirth-cancelled"
}

ensure_active_battle() {
  local attempt
  COMBAT_ACTIVE=0
  for attempt in 1 2 3; do
    if try_desc "96-nav-battle-$attempt" '^Battle$'; then
      sleep 2
      capture_checkpoint "96-battle-after-feature-pass-$attempt"
      if ! grep -q "No active enemy" "$CURRENT_UI"; then
        COMBAT_ACTIVE=1
        record_event "state" "active_combat_attempt_$attempt"
        return 0
      fi
    fi
    if ! try_desc "96b-nav-adventure-$attempt" '^Adventure$'; then
      break
    fi
    sleep 2
    capture_checkpoint "96c-adventure-top-$attempt"
    scroll_ui_down "96d-adventure-scroll-down-$attempt"
    capture_checkpoint "96e-adventure-lower-$attempt"
    if try_desc "96f-adventure-stage-1-$attempt" '^Stage 1, .*Ready to start$'; then
      capture_checkpoint "96g-adventure-stage-1-result-$attempt"
      scroll_until_text_down "96h-adventure-selected-card-$attempt" 'Start battle|Farm this stage' 12
      if try_tap "96j-adventure-start-battle-$attempt" '^Start battle$'; then
        capture_checkpoint "96k-adventure-started-$attempt"
      else
        try_tap "96l-adventure-farm-stage-$attempt" '^Farm this stage$'
        capture_checkpoint "96m-adventure-farm-stage-$attempt"
      fi
    else
      try_desc "96n-adventure-stage-2-$attempt" '^Stage 2, .*Last state: retreated$'
      capture_checkpoint "96o-adventure-stage-2-result-$attempt"
      scroll_until_text_down "96p-adventure-selected-card-$attempt" 'Start battle|Farm this stage' 12
      if try_tap "96r-adventure-start-battle-$attempt" '^Start battle$'; then
        capture_checkpoint "96s-adventure-started-$attempt"
      else
        try_tap "96t-adventure-farm-stage-$attempt" '^Farm this stage$'
        capture_checkpoint "96u-adventure-farm-stage-$attempt"
      fi
    fi
  done
  record_event "state" "no_active_combat"
  return 1
}

return_to_battle() {
  ensure_active_battle
  try_desc "97-battle-auto-on" '^Auto Battle is off'
  capture_checkpoint "97-battle-auto-restored"
  if [ "$COMBAT_ACTIVE" -eq 1 ]; then
    record_outcome "hourly_combat_guard" "state" "active"
  else
    record_outcome "hourly_combat_guard" "state" "inactive"
  fi
}

printf 'scenario=%s\npackage=%s\ncomponent=%s\nserial=%s\nsession_seconds=%s\n' \
  "$SCENARIO" "$PACKAGE" "$COMPONENT" "$SERIAL" "$SESSION_SECONDS" > "$OUT/run-info.txt"

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

adb_cmd shell am start -W -n "$COMPONENT" > "$OUT/01-launch.txt" 2>&1
LAUNCH_CODE=$?
sleep 8

capture_checkpoint "03-after-launch"
adb_cmd shell dumpsys input_method > "$OUT/03-after-launch-input-method.txt" 2>&1

PID_AFTER_LAUNCH="$(adb_cmd shell pidof -s "$PACKAGE" 2>/dev/null | tr -d '\r' | xargs)"
printf 'launch_exit_code=%s\npid_after_launch=%s\n' \
  "$LAUNCH_CODE" "$PID_AFTER_LAUNCH" > "$OUT/04-launch-state.txt"

PLAYTEST_STATE="not_run"
if [ -n "$PID_AFTER_LAUNCH" ]; then
  PLAYTEST_STATE="menu_observed"
  feature_settings

  if try_tap "08-start-expedition" '^Start Expedition$'; then
    sleep 4
    capture_checkpoint "08-name-dialog"
    if try_tap "09-hero-name-field" 'android.widget.EditText'; then
      adb_cmd shell input text "Mel" > "$OUT/09-hero-name-input.txt" 2>&1
      adb_cmd shell input keyevent 4 >> "$OUT/09-hero-name-input.txt" 2>&1
      capture_checkpoint "09-before-continue"
      if try_tap "10-continue" '^Continue$'; then
        GAME_STARTED=1
      else
        adb_cmd shell input keyevent 4 >> "$OUT/09-hero-name-input.txt" 2>&1
        capture_checkpoint "09-before-continue-retry"
        if try_tap "10-continue-retry" '^Continue$'; then
          GAME_STARTED=1
        fi
      fi
    fi
  fi

  sleep 5
  capture_checkpoint "10-battle-start"
  if [ "$GAME_STARTED" -eq 1 ]; then
    PLAYTEST_STATE="feature_pass_started"
  else
    PLAYTEST_STATE="feature_pass_not_reached"
  fi
  SESSION_START="$(date +%s)"
  SESSION_DEADLINE=$((SESSION_START + SESSION_SECONDS))
  adb_cmd shell dumpsys gfxinfo "$PACKAGE" reset > "$OUT/session-gfxinfo-reset.txt" 2>&1

  adb_cmd shell rm -f "$TRACE_DEVICE" > "$OUT/02-perfetto-remove.txt" 2>&1
  adb_cmd shell perfetto -o "$TRACE_DEVICE" -t 120s --app "$PACKAGE" \
    sched freq idle am wm gfx view binder_driver hal dalvik \
    > "$OUT/02-perfetto-console.txt" 2>&1 &
  PERFETTO_HOST_PID=$!
  sleep 1

  feature_battle_controls
  feature_adventure
  feature_build
  feature_doctrine
  feature_progress
  return_to_battle
  PLAYTEST_STATE="endurance_running"
  capture_checkpoint "99-feature-pass-complete"

  NEXT_CAPTURE=$((SESSION_START + 300))
  while :; do
    NOW="$(date +%s)"
    if [ "$NOW" -ge "$SESSION_DEADLINE" ]; then
      break
    fi
    if [ "$NOW" -ge "$NEXT_CAPTURE" ]; then
      ELAPSED=$((NOW - SESSION_START))
      MINUTE=$((ELAPSED / 60))
      capture_checkpoint "hourly-$(printf '%03d' "$MINUTE")m"
      if grep -q "No active enemy" "$CURRENT_UI"; then
        record_event "warning" "hourly-$(printf '%03d' "$MINUTE")m-no-active-enemy"
      elif grep -Eiq "AUTO-COMBAT // LIVE|THREAT //|WAVE [0-9]+/[0-9]+|Retreat from the current encounter" "$CURRENT_UI"; then
        record_event "state" "hourly-$(printf '%03d' "$MINUTE")m-active-battle-screen"
      else
        record_event "state" "hourly-$(printf '%03d' "$MINUTE")m-not-on-battle-screen"
      fi
      adb_cmd shell dumpsys gfxinfo "$PACKAGE" > "$OUT/hourly-$(printf '%03d' "$MINUTE")m-gfxinfo.txt" 2>&1
      adb_cmd shell dumpsys meminfo "$PACKAGE" > "$OUT/hourly-$(printf '%03d' "$MINUTE")m-meminfo.txt" 2>&1
      NEXT_CAPTURE=$((NEXT_CAPTURE + 300))
    fi
    REMAINING=$((SESSION_DEADLINE - NOW))
    if [ "$REMAINING" -gt 15 ]; then
      sleep 15
    else
      sleep "$REMAINING"
    fi
  done

  ELAPSED="$(($(date +%s) - SESSION_START))"
  if [ "$ELAPSED" -lt "$SESSION_SECONDS" ]; then
    sleep "$((SESSION_SECONDS - ELAPSED))"
  fi
  try_desc "99b-final-nav-battle" '^Battle$'
  sleep 2
  capture_checkpoint "hourly-060m-final"
  SESSION_COMPLETE_AT="$(date +%s)"
  if grep -q "No active enemy" "$CURRENT_UI"; then
    FINAL_COMBAT_ACTIVE=0
    PLAYTEST_STATE="one_hour_complete_no_active_combat"
  elif grep -Eiq "AUTO-COMBAT // LIVE|THREAT //|WAVE [0-9]+/[0-9]+|Retreat from the current encounter" "$CURRENT_UI"; then
    FINAL_COMBAT_ACTIVE=1
    PLAYTEST_STATE="one_hour_complete_active"
  else
    FINAL_COMBAT_ACTIVE=0
    PLAYTEST_STATE="one_hour_complete_no_active_combat"
  fi

  adb_cmd shell am force-stop "$PACKAGE" > "$OUT/100-force-stop-after-hour.txt" 2>&1
  sleep 2
  adb_cmd shell am start -W -n "$COMPONENT" > "$OUT/101-relaunch-after-hour.txt" 2>&1
  sleep 8
  capture_checkpoint "101-after-hour-relaunch"
  if try_tap "102-offline-summary-continue" '^Continue$'; then
    capture_checkpoint "102-after-offline-summary"
  fi
fi

if [ -n "$PID_AFTER_LAUNCH" ]; then
  adb_cmd shell rm -f "$PERF_DATA_DEVICE" > "$OUT/103-simpleperf-remove.txt" 2>&1
  adb_cmd shell simpleperf record --app "$PACKAGE" \
    -o "$PERF_DATA_DEVICE" -e cpu-clock -f 4000 -g --duration 30 \
    > "$OUT/103-simpleperf-console.txt" 2>&1
  adb_cmd pull "$PERF_DATA_DEVICE" "$OUT/idlerpg-hourly.perf.data" \
    > "$OUT/103-simpleperf-pull.txt" 2>&1
  adb_cmd shell simpleperf report -i "$PERF_DATA_DEVICE" \
    > "$OUT/103-simpleperf-report.txt" 2>&1
else
  printf 'Skipped because the app had no live process after launch.\n' \
    > "$OUT/103-simpleperf-console.txt"
fi

if [ -n "$PERFETTO_HOST_PID" ]; then
  wait "$PERFETTO_HOST_PID" > "$OUT/02-perfetto-wait.txt" 2>&1
  adb_cmd pull "$TRACE_DEVICE" "$OUT/idlerpg-hourly.pftrace" \
    > "$OUT/02-perfetto-pull.txt" 2>&1
else
  printf 'Perfetto was not started because the app had no live process.\n' \
    > "$OUT/02-perfetto-console.txt"
fi

adb_cmd shell dumpsys gfxinfo "$PACKAGE" > "$OUT/104-gfxinfo.txt" 2>&1
adb_cmd shell dumpsys gfxinfo "$PACKAGE" framestats > "$OUT/104-gfxinfo-framestats.txt" 2>&1
adb_cmd shell dumpsys meminfo "$PACKAGE" > "$OUT/104-meminfo.txt" 2>&1
adb_cmd shell dumpsys cpuinfo > "$OUT/104-cpuinfo.txt" 2>&1
adb_cmd logcat -d -v threadtime > "$OUT/105-logcat.txt" 2>&1
adb_cmd logcat -b crash -d > "$OUT/105-crash-buffer.txt" 2>&1
adb_cmd shell dumpsys activity activities > "$OUT/106-final-activity.txt" 2>&1
adb_cmd shell dumpsys window windows > "$OUT/106-final-window.txt" 2>&1
adb_cmd shell dumpsys package "$PACKAGE" > "$OUT/106-final-package-state.txt" 2>&1
grep -E -i "$PACKAGE|FATAL EXCEPTION|ANR in|AndroidRuntime|SIGSEGV|OutOfMemoryError" \
  "$OUT/105-logcat.txt" "$OUT/105-crash-buffer.txt" > "$OUT/107-crash-scan.txt" 2>&1

if [ -n "$PID_AFTER_LAUNCH" ]; then
  LAUNCH_VERDICT="launch_did_not_crash"
elif grep -Eiq "$PACKAGE|FATAL EXCEPTION|AndroidRuntime" "$OUT/105-crash-buffer.txt"; then
  LAUNCH_VERDICT="launch_crash_evidence"
else
  LAUNCH_VERDICT="app_not_running_after_launch"
fi

SESSION_END="$(date +%s)"
SESSION_ELAPSED=0
if [ "$SESSION_COMPLETE_AT" -gt 0 ] && [ "$SESSION_START" -gt 0 ]; then
  SESSION_ELAPSED=$((SESSION_COMPLETE_AT - SESSION_START))
elif [ "$SESSION_START" -gt 0 ]; then
  SESSION_ELAPSED=$((SESSION_END - SESSION_START))
fi

CHECKPOINT_COUNT="$(find "$OUT" -name '*-ui.xml' | wc -l | tr -d ' ')"
SCREENSHOT_COUNT="$(find "$OUT" -name '*.png' | wc -l | tr -d ' ')"

{
  printf 'scenario=%s\n' "$SCENARIO"
  printf 'package=%s\n' "$PACKAGE"
  printf 'component=%s\n' "$COMPONENT"
  printf 'serial=%s\n' "$SERIAL"
  printf 'launch_exit_code=%s\n' "$LAUNCH_CODE"
  printf 'pid_after_launch=%s\n' "$PID_AFTER_LAUNCH"
  printf 'launch_verdict=%s\n' "$LAUNCH_VERDICT"
  printf 'playtest_state=%s\n' "$PLAYTEST_STATE"
  printf 'session_target_seconds=%s\n' "$SESSION_SECONDS"
  printf 'session_elapsed_seconds=%s\n' "$SESSION_ELAPSED"
  printf 'feature_action_count=%s\n' "$FEATURE_ACTION_COUNT"
  printf 'combat_active_at_feature_pass=%s\n' "$COMBAT_ACTIVE"
  printf 'combat_active_at_final=%s\n' "$FINAL_COMBAT_ACTIVE"
  printf 'checkpoint_count=%s\n' "$CHECKPOINT_COUNT"
  printf 'screenshot_count=%s\n' "$SCREENSHOT_COUNT"
} > "$OUT/verdict.txt"

printf 'scenario=%s launch_verdict=%s playtest_state=%s elapsed=%ss checkpoints=%s screenshots=%s\n' \
  "$SCENARIO" "$LAUNCH_VERDICT" "$PLAYTEST_STATE" "$SESSION_ELAPSED" "$CHECKPOINT_COUNT" "$SCREENSHOT_COUNT"

if [ -n "$GITHUB_STEP_SUMMARY" ]; then
  {
    echo "## IdleRPG one-hour emulator QA"
    echo
    echo "- Scenario: $SCENARIO"
    echo "- Launch verdict: $LAUNCH_VERDICT"
    echo "- Playtest state: $PLAYTEST_STATE"
    echo "- Session target: $SESSION_SECONDS seconds"
    echo "- Session elapsed: $SESSION_ELAPSED seconds"
    echo "- Feature actions: $FEATURE_ACTION_COUNT"
    echo "- Combat active at feature pass: $COMBAT_ACTIVE"
    echo "- Combat active at final: $FINAL_COMBAT_ACTIVE"
    echo "- UI checkpoints: $CHECKPOINT_COUNT"
    echo "- Screenshots: $SCREENSHOT_COUNT"
    echo "- Evidence is uploaded under artifacts/emulator/."
  } >> "$GITHUB_STEP_SUMMARY"
fi

exit 0
