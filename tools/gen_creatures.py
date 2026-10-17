"""Erzeugt die Daten der Wesen der Zukunft (ab M8d): Item-Modelle, Übersetzungen, Beute, Rezepte,
Tags und Klang-Einträge für Paradox-Druck, Zeitwächter und alles, was dazugehört.

Aufruf: python3 -I tools/gen_creatures.py   (läuft auch am Ende von gen_assets.py)
"""
import json
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
RES = ROOT / "src/main/resources"
ASSETS = RES / "assets/timemod"
DATA = RES / "data"
NS = "timemod"


def write(path: Path, obj):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(obj, indent=2, ensure_ascii=False) + "\n", encoding="utf-8")


def t(name):
    return f"{NS}:{name}"


# (id, Modelltyp, de, en, Beschreibung de, Beschreibung en)
ITEMS = [
    ("time_seal", "generated", "Zeitsiegel", "Time Seal",
     "Das Dienstsiegel eines Zeitwächters. Aufgedrückt senkt es deinen Paradox-Druck um 20 %.",
     "A Time Warden's official seal. Pressed on, it lowers your paradox pressure by 20%."),
    ("clock_hand_blade", "handheld", "Zeigerklinge", "Clock Hand Blade",
     "Der Minutenzeiger einer Turmuhr, zur Waffe geschliffen. Wen sie trifft, dessen Zeit gerät ins Stocken.",
     "The minute hand of a tower clock, honed into a weapon. Whoever it strikes, their time stutters."),
    ("damping_module", "generated", "Dämpfungsmodul", "Damping Module",
     "Halbiert den Paradox-Druck, den das Werkzeug verursacht.", "Halves the paradox pressure the tool causes."),
    ("time_warden_spawn_egg", "spawn_egg", "Zeitwächter-Spawn-Ei", "Time Warden Spawn Egg", None, None),
    # M8e: Wesen der Zukunft
    ("guardian_core", "generated", "Wächterkern", "Guardian Core",
     "Das alte Herz eines Ruinenwächters. Es pocht noch – im Takt einer Glocke, die niemand mehr läutet.",
     "The old heart of a Ruin Guardian. It still beats – in time with a bell that nobody rings anymore."),
    ("ruin_guardian_spawn_egg", "spawn_egg", "Ruinenwächter-Spawn-Ei", "Ruin Guardian Spawn Egg", None, None),
    ("moss_bear_spawn_egg", "spawn_egg", "Moosbär-Spawn-Ei", "Moss Bear Spawn Egg", None, None),
]

# Blöcke: (id, de, en, Beschreibung de, Beschreibung en)
BLOCKS = [
    ("chronometer", "Chronometer", "Chronometer",
     "Präzisionsuhr der Zukunft – Arbeitsplatz der Chrono-Händler. Findet ein Dorfbewohner eines, handelt er mit der Zeit.",
     "A precision clock of the future – the Chrono Trader's workstation. A villager who finds one starts trading in time."),
]

SOUNDS = {
    "paradox_warning": ("Die Zeit wird unruhig", "Time grows restless", 32),
    "warden_appear": ("Zeitwächter erscheint", "Time Warden appears", 48),
    "warden_vanish": ("Zeitwächter verschwindet", "Time Warden vanishes", 32),
    "warden_blink": ("Zeitwächter springt", "Time Warden blinks", 24),
    "warden_ambient": ("Zeitwächter tickt", "Time Warden ticks", 16),
    "warden_hurt": ("Zeitwächter wird getroffen", "Time Warden hurts", 16),
    "warden_death": ("Zeitwächter zerspringt", "Time Warden shatters", 24),
    "warden_arrest": ("Zeitarrest", "Time arrest", 24),
    "warden_bolt": ("Stasis-Zeiger fliegt", "Stasis bolt flies", 24),
    "warden_bolt_hit": ("Stasis-Zeiger trifft", "Stasis bolt hits", 16),
    "seal_stamp": ("Zeitsiegel wird aufgedrückt", "Time seal pressed", 16),
    "guardian_wake": ("Ruinenwächter erwacht", "Ruin Guardian awakens", 32),
}

LANG = {
    "de_de": {
        "entity.timemod.time_warden": "Zeitwächter",
        "entity.timemod.stasis_bolt": "Stasis-Zeiger",
        "hud.timemod.paradox": "Paradox-Druck",
        "message.timemod.paradox.unease": "Die Zeit um dich wird unruhig …",
        "message.timemod.paradox.noticed": "Etwas jenseits der Zeit hat dich bemerkt.",
        "message.timemod.paradox.hunted": "Die Zeitwächter jagen dich!",
        "message.timemod.paradox.calm": "Die Zeitlinie hat sich beruhigt.",
        "message.timemod.paradox.warden": "Ein Zeitwächter ist erschienen.",
        "message.timemod.warden.arrest": "Zeitarrest!",
        "message.timemod.seal.calm": "Deine Zeitlinie ist ruhig – es gibt nichts zu versiegeln.",
        "message.timemod.seal.used": "Versiegelt. Paradox-Druck: %s %%",
        "message.timemod.pocket_watch.paradox": "Paradox %s %%",
        "commands.timemod.paradox.get": "Paradox-Druck von %s: %s %% (Wächter zugleich: %s)",
        "commands.timemod.paradox.set": "Paradox-Druck von %s auf %s %% gesetzt",
        "commands.timemod.paradox.warden.failed": "Hier findet kein Zeitwächter Platz.",
        "gamerule.doTimeWardens": "Zeitwächter jagen Spieler mit hohem Paradox-Druck",
        "entity.timemod.ruin_guardian": "Ruinenwächter",
        "entity.timemod.moss_bear": "Moosbär",
        "entity.minecraft.villager.chrono_trader": "Chrono-Händler",
    },
    "en_us": {
        "entity.timemod.time_warden": "Time Warden",
        "entity.timemod.stasis_bolt": "Stasis Bolt",
        "hud.timemod.paradox": "Paradox pressure",
        "message.timemod.paradox.unease": "Time grows restless around you …",
        "message.timemod.paradox.noticed": "Something beyond time has noticed you.",
        "message.timemod.paradox.hunted": "The Time Wardens are hunting you!",
        "message.timemod.paradox.calm": "The timeline has calmed down.",
        "message.timemod.paradox.warden": "A Time Warden has appeared.",
        "message.timemod.warden.arrest": "Time arrest!",
        "message.timemod.seal.calm": "Your timeline is calm – there is nothing to seal.",
        "message.timemod.seal.used": "Sealed. Paradox pressure: %s%%",
        "message.timemod.pocket_watch.paradox": "paradox %s%%",
        "commands.timemod.paradox.get": "Paradox pressure of %s: %s%% (wardens at once: %s)",
        "commands.timemod.paradox.set": "Set paradox pressure of %s to %s%%",
        "commands.timemod.paradox.warden.failed": "There is no room for a Time Warden here.",
        "gamerule.doTimeWardens": "Time Wardens hunt players with high paradox pressure",
        "entity.timemod.ruin_guardian": "Ruin Guardian",
        "entity.timemod.moss_bear": "Moss Bear",
        "entity.minecraft.villager.chrono_trader": "Chrono Trader",
    },
}


def item_models():
    for item_id, kind, *_ in ITEMS:
        if kind == "spawn_egg":
            write(ASSETS / f"models/item/{item_id}.json", {"parent": "minecraft:item/template_spawn_egg"})
            continue
        parent = "minecraft:item/handheld" if kind == "handheld" else "minecraft:item/generated"
        write(ASSETS / f"models/item/{item_id}.json", {"parent": parent, "textures": {"layer0": t(f"item/{item_id}")}})


def block_assets():
    for block_id, *_ in BLOCKS:
        write(ASSETS / f"models/item/{block_id}.json", {"parent": t(f"block/{block_id}")})
    write(ASSETS / "blockstates/chronometer.json", {"variants": {
        f"facing={facing}": {"model": t("block/chronometer"), **({"y": rot} if rot else {})}
        for facing, rot in (("north", 0), ("east", 90), ("south", 180), ("west", 270))}})
    side = t("block/chronometer_side")
    write(ASSETS / "models/block/chronometer.json", {
        "parent": "minecraft:block/block",
        "textures": {"particle": side, "side": side, "top": t("block/chronometer_top"), "bottom": t("block/chronometer_bottom")},
        "elements": [
            {"from": [1, 0, 1], "to": [15, 8, 15], "faces": {
                "down": {"uv": [1, 1, 15, 15], "texture": "#bottom", "cullface": "down"},
                "up": {"uv": [1, 1, 15, 15], "texture": "#bottom"},
                "north": {"uv": [1, 8, 15, 16], "texture": "#side"},
                "south": {"uv": [1, 8, 15, 16], "texture": "#side"},
                "west": {"uv": [1, 8, 15, 16], "texture": "#side"},
                "east": {"uv": [1, 8, 15, 16], "texture": "#side"}}},
            {"from": [2, 8, 2], "to": [14, 9, 14], "faces": {
                "up": {"uv": [2, 2, 14, 14], "texture": "#top"},
                "north": {"uv": [2, 0, 14, 1], "texture": "#side"},
                "south": {"uv": [2, 0, 14, 1], "texture": "#side"},
                "west": {"uv": [2, 0, 14, 1], "texture": "#side"},
                "east": {"uv": [2, 0, 14, 1], "texture": "#side"}}},
            {"from": [7, 8, 0], "to": [9, 10, 1], "faces": {
                "north": {"uv": [7, 0, 9, 2], "texture": "#side"},
                "up": {"uv": [7, 0, 9, 1], "texture": "#side"},
                "east": {"uv": [0, 0, 1, 2], "texture": "#side"},
                "west": {"uv": [0, 0, 1, 2], "texture": "#side"}}},
        ],
    })


def sounds():
    path = ASSETS / "sounds.json"
    data = json.loads(path.read_text(encoding="utf-8"))
    for name, (_de, _en, distance) in SOUNDS.items():
        data[name] = {"subtitle": f"subtitles.{NS}.{name}", "sounds": [{"name": t(name), "attenuation_distance": distance}]}
    write(path, data)


def loot_tables():
    def chance(base, per_level):
        return [{"condition": "minecraft:killed_by_player"},
                {"condition": "minecraft:random_chance_with_looting", "chance": base, "looting_multiplier": per_level}]

    write(DATA / f"{NS}/loot_tables/entities/time_warden.json", {"type": "minecraft:entity", "pools": [
        {"rolls": 1, "entries": [{"type": "minecraft:item", "name": t("time_dust"), "functions": [
            {"function": "minecraft:set_count", "count": {"type": "minecraft:uniform", "min": 1, "max": 3}},
            {"function": "minecraft:looting_enchant", "count": {"type": "minecraft:uniform", "min": 0, "max": 1}}]}]},
        {"rolls": 1, "conditions": chance(0.35, 0.1), "entries": [{"type": "minecraft:item", "name": t("time_seal")}]},
        {"rolls": 1, "conditions": chance(0.05, 0.02), "entries": [{"type": "minecraft:item", "name": t("clock_hand_blade")}]},
    ]})
    write(DATA / f"{NS}/loot_tables/entities/stasis_bolt.json", {"type": "minecraft:entity", "pools": []})

    def count(lo, hi, looting=1):
        fns = [{"function": "minecraft:set_count", "count": {"type": "minecraft:uniform", "min": lo, "max": hi}}]
        if looting:
            fns.append({"function": "minecraft:looting_enchant", "count": {"type": "minecraft:uniform", "min": 0, "max": looting}})
        return fns

    write(DATA / f"{NS}/loot_tables/entities/ruin_guardian.json", {"type": "minecraft:entity", "pools": [
        {"rolls": 1, "entries": [{"type": "minecraft:item", "name": t("rusty_iron"), "functions": count(1, 3)}]},
        {"rolls": 1, "entries": [{"type": "minecraft:item", "name": "minecraft:iron_nugget", "functions": count(2, 6, 2)}]},
        {"rolls": 1, "entries": [{"type": "minecraft:item", "name": "minecraft:moss_block", "functions": count(0, 2)}]},
        {"rolls": 1, "conditions": [{"condition": "minecraft:killed_by_player"}],
         "entries": [{"type": "minecraft:item", "name": t("guardian_core")}]},
    ]})
    write(DATA / f"{NS}/loot_tables/entities/moss_bear.json", {"type": "minecraft:entity", "pools": [
        {"rolls": 1, "entries": [{"type": "minecraft:item", "name": "minecraft:moss_block", "functions": count(0, 2)}]},
        {"rolls": 1, "entries": [{"type": "minecraft:item", "name": "minecraft:sweet_berries", "functions": count(0, 2)}]},
    ]})
    write(DATA / f"{NS}/loot_tables/blocks/chronometer.json", {"type": "minecraft:block", "pools": [
        {"rolls": 1, "conditions": [{"condition": "minecraft:survives_explosion"}],
         "entries": [{"type": "minecraft:item", "name": t("chronometer")}]}]})


def recipes():
    write(DATA / f"{NS}/recipes/damping_module.json", {
        "type": t("clockmaker"),
        "pattern": [" S ", "WMW", " S "],
        "key": {"S": {"item": t("time_seal")}, "W": {"tag": "minecraft:wool"}, "M": {"item": t("blank_module")}},
        "result": {"item": t("damping_module"), "count": 1},
    })


def tags():
    # Wer außerhalb der Zeit steht: Zeitzonen wirken nicht auf sie
    write(DATA / f"{NS}/tags/entity_types/time_immune.json", {"replace": False, "values": [
        "minecraft:ender_dragon", "minecraft:wither", "minecraft:warden", t("time_warden")]})


def lang():
    for code, idx in (("de_de", 0), ("en_us", 1)):
        path = ASSETS / f"lang/{code}.json"
        data = json.loads(path.read_text(encoding="utf-8")) if path.exists() else {}
        for item_id, _kind, de, en, desc_de, desc_en in ITEMS:
            data[f"item.{NS}.{item_id}"] = de if idx == 0 else en
            desc = desc_de if idx == 0 else desc_en
            if desc:
                data[f"item.{NS}.{item_id}.desc"] = desc
        for name, (de, en, _distance) in SOUNDS.items():
            data[f"subtitles.{NS}.{name}"] = de if idx == 0 else en
        for block_id, de, en, desc_de, desc_en in BLOCKS:
            data[f"block.{NS}.{block_id}"] = de if idx == 0 else en
            data[f"block.{NS}.{block_id}.desc"] = desc_de if idx == 0 else desc_en
        data.update(LANG[code])
        write(path, dict(sorted(data.items())))


def main():
    item_models()
    block_assets()
    sounds()
    loot_tables()
    recipes()
    tags()
    lang()
    print("Wesen der Zukunft erzeugt.")


if __name__ == "__main__":
    main()
