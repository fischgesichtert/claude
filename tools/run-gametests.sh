#!/usr/bin/env bash
# Führt alle Gametests aus und zeigt pro Test Ergebnis bzw. Fehlermeldung.
# Mit GRADLE_ARGS=-Pcompat laufen sie zusammen mit dem Modpack aus tools/fetch-compat-mods.py.
set -u
cd "$(dirname "$0")/.."
mkdir -p build
timeout 1800 ./gradlew runGametest --no-daemon --console=plain ${GRADLE_ARGS:-} > build/gametest.log 2>&1
status=$?
python3 - <<'PY'
import html, re, pathlib
p = pathlib.Path("build/gametest/report.xml")
if not p.exists():
    print("kein Report – siehe build/gametest.log")
else:
    s = p.read_text()
    for m in re.finditer(r'<testcase classname="[^"]*" name="([^"]+)" time="([^"]+)"(/>|>\s*<failure message="([^"]*)")', s):
        name, t, _, msg = m.groups()
        print(("FEHLER " if msg else "OK     ") + name + f" ({t}s)")
        if msg:
            print("       " + html.unescape(msg).replace("\n", "\n       ")[:1500])
PY
grep -E "Exception|ERROR" build/gametest.log | grep -v "yggdrasil\|server.properties\|authlib\|UnknownHost\|Failed to load properties" | head -20
exit $status
