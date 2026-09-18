"""Capture real Compose screens using UI-tree targets; record misses explicitly."""
import json
import re
import subprocess
import time
import xml.etree.ElementTree as ET
from pathlib import Path

OUT = Path('artifacts/ui-smoke')
OUT.mkdir(parents=True, exist_ok=True)
PACKAGE = 'com.idlerpg.game'
outcomes = []


def adb(*args, check=True):
    return subprocess.run(['adb', *args], capture_output=True, check=check).stdout


def capture(label):
    (OUT / (label + '.png')).write_bytes(adb('exec-out', 'screencap', '-p'))
    adb('shell', 'uiautomator', 'dump', '/sdcard/contract-ui.xml')
    raw = adb('exec-out', 'cat', '/sdcard/contract-ui.xml')
    (OUT / (label + '.xml')).write_bytes(raw)
    return ET.fromstring(raw)


def tap(pattern, label, required=False):
    root = capture(label + '-before')
    for node in root.iter('node'):
        if node.get('enabled') != 'true':
            continue
        if not any(re.search(pattern, node.get(key, '')) for key in ['text', 'content-desc', 'class']):
            continue
        bounds = list(map(int, re.findall(r'\d+', node.get('bounds', ''))))
        if len(bounds) == 4 and bounds[2] > bounds[0] and bounds[3] > bounds[1]:
            adb('shell', 'input', 'tap', str((bounds[0]+bounds[2])//2), str((bounds[1]+bounds[3])//2))
            time.sleep(2)
            outcomes.append({'step': label, 'result': 'target_tapped'})
            capture(label)
            return True
    outcomes.append({'step': label, 'result': 'target_missing', 'required': required})
    if required:
        raise RuntimeError('Required UI target missing: ' + pattern)
    return False


try:
    adb('install', '-r', 'app/build/outputs/apk/debug/app-debug.apk')
    adb('logcat', '-c')
    adb('shell', 'am', 'start', '-W', '-n', PACKAGE + '/.MainActivity')
    time.sleep(5)
    tap('^Start Expedition$', '01-start', True)
    tap('android.widget.EditText', '02-name', True)
    adb('shell', 'input', 'text', 'Mel')
    adb('shell', 'input', 'keyevent', '4')
    tap('^Continue$', '03-continue', True)
    time.sleep(5)
    capture('04-battle')
    for name in ['Adventure', 'Build', 'Growth']:
        tap('^' + name + '$', '05-' + name.lower(), True)
    tap('How is this calculated', '06-power-details')
    # Scroll the actual content area, away from navigation and system edges.
    root = capture('07-growth-scroll-target')
    for node in root.iter('node'):
        if node.get('scrollable') == 'true':
            b = list(map(int, re.findall(r'\d+', node.get('bounds', ''))))
            if len(b) == 4 and b[3] - b[1] > 300:
                x = (b[0]+b[2])//2
                adb('shell', 'input', 'swipe', str(x), str(b[3]-80), str(x), str(b[1]+80), '500')
                break
    capture('08-growth-lower')
    tap('Allocate points', '09-permanent-growth')
    tap('^Build$', '10-build', True)
    tap('^Skills$', '11-skills', True)
    adb('shell', 'input', 'keyevent', '4')
    tap('^Battle$', '12-battle', True)
    adb('shell', 'settings', 'put', 'system', 'font_scale', '1.5')
    time.sleep(3)
    capture('13-battle-large-text')
    tap('^Growth$', '14-growth-large-text', True)
    adb('shell', 'settings', 'put', 'system', 'font_scale', '1.0')
    assert adb('shell', 'pidof', PACKAGE).strip(), 'App process missing'
finally:
    (OUT / 'outcomes.json').write_text(json.dumps(outcomes, indent=2))
    (OUT / 'crash.log').write_bytes(adb('logcat', '-d', '-b', 'crash', check=False))
    (OUT / 'logcat.txt').write_bytes(adb('logcat', '-d', check=False))
