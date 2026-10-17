#!/usr/bin/env python3
"""Lädt verbreitete Fabric-Mods für Kompatibilitätstests von Modrinth.

Die Liste entspricht einem echten Spieler-Modpack (Lithium, ModernFix, Sodium, Iris, …).
Server-Mods landen in compat/mods/server, reine Client-Mods in compat/mods/client.
Gradle bindet sie mit -Pcompat in runGametest bzw. runDemoClient ein.

Aufruf: python3 -I tools/fetch-compat-mods.py
"""
import hashlib
import json
import pathlib
import sys
import urllib.parse
import urllib.request

ROOT = pathlib.Path(__file__).resolve().parent.parent
TARGET = ROOT / "compat" / "mods"
GAME_VERSION = "1.20.1"

# (Modrinth-Projekt, Version, Seite) – „server“ läuft auch im Client mit.
MODS = [
    ("lithium", "mc1.20.1-0.11.4-fabric", "server"),
    ("modernfix", "5.25.2+mc1.20.1", "server"),
    ("ferrite-core", "6.0.1", "server"),
    ("collective", "1.20.1-8.57-fabric+forge+neo", "server"),
    ("jade", "11.13.3+fabric", "server"),
    ("sodium", "mc1.20.1-0.5.13-fabric", "client"),
    ("iris", "1.7.6+1.20.1", "client"),
    ("indium", "1.0.36+mc1.20.1", "client"),
    ("immediatelyfast", "1.5.5+1.20.4-fabric", "client"),
    ("entityculling", "1.11.3", "client"),
    ("moreculling", "1.20.1-0.24.6", "client"),
    ("continuity", "3.0.0+1.20.1", "client"),
    ("entitytexturefeatures", "7.2.6-fabric-1.20.1", "client"),
    ("entity-model-features", "3.3.12-fabric-1.20.1", "client"),
    ("dynamic-fps", "3.11.4", "client"),
    ("sound-physics-remastered", "fabric-1.20.1-1.5.1", "client"),
    ("distanthorizons", "3.3.3-1.20.1", "client"),
]


def api(path):
    with urllib.request.urlopen("https://api.modrinth.com/v2/" + path, timeout=60) as response:
        return json.load(response)


def fetch(slug, version_number, side):
    query = urllib.parse.urlencode({"loaders": '["fabric"]', "game_versions": json.dumps([GAME_VERSION])})
    versions = api(f"project/{slug}/version?{query}")
    match = [v for v in versions if v["version_number"] == version_number]
    if not match:
        sys.exit(f"{slug}: Version {version_number} nicht gefunden")
    files = [f for f in match[0]["files"] if f["primary"]] or match[0]["files"]
    file = files[0]
    out = TARGET / side / file["filename"]
    if out.exists() and hashlib.sha512(out.read_bytes()).hexdigest() == file["hashes"]["sha512"]:
        print(f"vorhanden  {out.relative_to(ROOT)}")
        return
    out.parent.mkdir(parents=True, exist_ok=True)
    with urllib.request.urlopen(file["url"], timeout=300) as response:
        data = response.read()
    if hashlib.sha512(data).hexdigest() != file["hashes"]["sha512"]:
        sys.exit(f"{slug}: Prüfsumme stimmt nicht")
    out.write_bytes(data)
    print(f"geladen    {out.relative_to(ROOT)} ({len(data) / 1e6:.1f} MB)")


def main():
    for slug, version, side in MODS:
        fetch(slug, version, side)


if __name__ == "__main__":
    main()
