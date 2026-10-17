#!/usr/bin/env bash
# Führt nur die genannten Gametest-Klassen aus (z. B. TownTests ClimateTests) – schneller beim Entwickeln.
set -u
cd "$(dirname "$0")/.."
MOD=src/gametest/resources/fabric.mod.json
cp "$MOD" build/fabric.mod.json.bak
trap 'cp build/fabric.mod.json.bak "$MOD"' EXIT
python3 - "$@" <<'PY'
import json, sys
path = "src/gametest/resources/fabric.mod.json"
data = json.load(open(path))
wanted = sys.argv[1:]
data["entrypoints"]["fabric-gametest"] = [e for e in data["entrypoints"]["fabric-gametest"] if e.rsplit(".", 1)[1] in wanted]
json.dump(data, open(path, "w"), indent=2)
PY
bash tools/run-gametests.sh
