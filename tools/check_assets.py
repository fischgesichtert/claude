#!/usr/bin/env python3
"""Prüft die Client-Assets: Jedes registrierte Item/jeder Block hat Modell, Texturen und Übersetzungen.

Die Registrierungsnamen werden aus dem Java-Code gelesen (register("name", ...)), damit nichts
vergessen wird, wenn neue Inhalte dazukommen.
"""
import json
import pathlib
import re
import sys

ROOT = pathlib.Path(__file__).resolve().parent.parent
JAVA = ROOT / "src/main/java/com/fischgesichtert/timemod/registry"
ASSET_DIRS = [ROOT / "src/main/resources/assets/timemod", ROOT / "src/client/resources/assets/timemod"]


def find(rel):
    for base in ASSET_DIRS:
        p = base / rel
        if p.exists():
            return p
    return None


def names(java_file):
    text = (JAVA / java_file).read_text(encoding="utf-8")
    return re.findall(r'(?:register|module)\("([a-z0-9_]+)"', text)


def texture_refs(model_path, seen=None):
    seen = seen or set()
    data = json.loads(model_path.read_text(encoding="utf-8"))
    refs = list((data.get("textures") or {}).values())
    parent = data.get("parent", "")
    if parent.startswith("timemod:") and parent not in seen:
        seen.add(parent)
        p = find("models/" + parent.split(":", 1)[1] + ".json")
        if p is None:
            yield f"Elternmodell fehlt: {parent} (in {model_path.name})"
        else:
            yield from texture_refs(p, seen)
    for ref in refs:
        if ref.startswith("#") or not ref.startswith("timemod:"):
            continue
        if find("textures/" + ref.split(":", 1)[1] + ".png") is None:
            yield f"Textur fehlt: {ref} (in {model_path.name})"


def main():
    problems = []
    items = names("ModItems.java")
    blocks = names("ModBlocks.java")
    lang = {}
    for code in ("de_de", "en_us"):
        p = find(f"lang/{code}.json")
        lang[code] = json.loads(p.read_text(encoding="utf-8")) if p else {}

    block_items = set(re.findall(r'= block\(ModBlocks\.([A-Z_]+)\)', (JAVA / "ModItems.java").read_text(encoding="utf-8")))
    all_items = set(items) | {b.lower() for b in block_items}

    for item in sorted(all_items):
        model = find(f"models/item/{item}.json")
        if model is None:
            problems.append(f"Item-Modell fehlt: {item}")
        else:
            problems.extend(texture_refs(model))
        for code, table in lang.items():
            if f"item.timemod.{item}" not in table and f"block.timemod.{item}" not in table:
                problems.append(f"Übersetzung fehlt ({code}): {item}")

    for block in blocks:
        state = find(f"blockstates/{block}.json")
        if state is None:
            problems.append(f"Blockstate fehlt: {block}")
            continue
        data = json.loads(state.read_text(encoding="utf-8"))
        models = set()
        for variant in (data.get("variants") or {}).values():
            for v in (variant if isinstance(variant, list) else [variant]):
                models.add(v["model"])
        for part in data.get("multipart") or []:
            apply = part["apply"]
            for v in (apply if isinstance(apply, list) else [apply]):
                models.add(v["model"])
        for m in models:
            if not m.startswith("timemod:"):
                continue
            p = find("models/" + m.split(":", 1)[1] + ".json")
            if p is None:
                problems.append(f"Blockmodell fehlt: {m}")
            else:
                problems.extend(texture_refs(p))
        for code, table in lang.items():
            if f"block.timemod.{block}" not in table:
                problems.append(f"Übersetzung fehlt ({code}): block {block}")

    sounds = find("sounds.json")
    if sounds:
        for event, entry in json.loads(sounds.read_text(encoding="utf-8")).items():
            for s in entry.get("sounds", []):
                name = s if isinstance(s, str) else s["name"]
                if name.startswith("timemod:") and find("sounds/" + name.split(":", 1)[1] + ".ogg") is None:
                    problems.append(f"Sounddatei fehlt: {name} ({event})")

    for p in problems:
        print("FEHLER", p)
    print(f"{len(all_items)} Items, {len(blocks)} Blöcke geprüft – {len(problems)} Probleme")
    return 1 if problems else 0


if __name__ == "__main__":
    sys.exit(main())
