"""Erzeugt alle JSON-Dateien von TIME: Blockzustände, Modelle, Beutetabellen, Rezepte, Tags,
Weltgenerierung und Übersetzungen. Eine Quelle der Wahrheit statt hunderter Hand-Dateien.

Aufruf: python3 -I tools/gen_assets.py
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


# ------------------------------------------------------------------------------------------
# Inhalte
# ------------------------------------------------------------------------------------------

# (id, Modelltyp, de, en, Beschreibung de, Beschreibung en)
ITEMS = [
    ("pocket_watch", "generated", "Taschenuhr", "Pocket Watch",
     "Zeigt Uhrzeit, Tag, Mondphase und den Zeitfluss an deiner Position.", "Shows time, day, moon phase and the flow of time where you stand."),
    ("growth_hourglass", "generated", "Wachstums-Sanduhr", "Growth Hourglass",
     "Ein Block (Pflanze, Setzling, Ofen, Maschine) läuft 30 s lang 4× so schnell.", "One block (plant, sapling, furnace, machine) runs 4× as fast for 30 s."),
    ("brake_spring", "generated", "Bremsfeder", "Brake Spring",
     "Ein Wesen oder Geschoss läuft 10 s lang mit halber Zeit.", "A creature or projectile runs at half time for 10 s."),
    ("moment_lens", "generated", "Moment-Linse", "Moment Lens",
     "Hält einen Moment fest: ein Wesen, ein Geschoss im Flug oder einen Block – 3 s lang.", "Freezes a moment: a creature, a projectile in flight or a block – for 3 s."),
    ("recall_watch", "generated", "Rückblick-Uhr", "Recall Watch",
     "Springt 5 Sekunden zurück: deinen Weg rückwärts, dein Leben wie damals.", "Jumps 5 seconds back: your path in reverse, your health as it was."),
    ("stasis_watch", "generated", "Stillstand-Uhr", "Stasis Watch",
     "Hält die Zeit im Umkreis von 12 Blöcken an. Nur du bewegst dich.", "Stops time within 12 blocks. Only you can move."),
    ("time_accelerator", "generated", "Zeitraffer", "Time Accelerator",
     "Eine Zone mit 2–4× Zeit (Radius 6, 30 s).", "A zone with 2–4× time (radius 6, 30 s)."),
    ("slow_pendulum", "generated", "Zeitlupen-Pendel", "Slow-Motion Pendulum",
     "Eine Zone mit 0,5× oder 0,25× Zeit (Radius 6, 30 s).", "A zone with 0.5× or 0.25× time (radius 6, 30 s)."),
    ("rewind_spool", "generated", "Rückspul-Spule", "Rewind Spool",
     "Spult den Umkreis von 16 Blöcken bis zu 10 Sekunden zurück.", "Rewinds everything within 16 blocks up to 10 seconds."),
    ("chrono_knife", "handheld", "Chrono-Wurfmesser", "Chrono Knife",
     "Im Zeitstopp geworfen, hängen sie in der Luft – bis die Zeit weiterläuft.", "Thrown in stopped time they hang in the air – until time moves on."),
    ("pocket_time_cell", "generated", "Taschen-Zeitzelle", "Pocket Time Cell", None, None),
    ("chronite_time_cell", "generated", "Chronit-Zeitzelle", "Chronite Time Cell", None, None),
    ("brass_ingot", "generated", "Messingbarren", "Brass Ingot", None, None),
    ("brass_nugget", "generated", "Messingklumpen", "Brass Nugget", None, None),
    ("brass_gear", "generated", "Messing-Zahnrad", "Brass Gear", None, None),
    ("mainspring", "generated", "Uhrfeder", "Mainspring", None, None),
    ("balance_wheel", "generated", "Unruh", "Balance Wheel", None, None),
    ("clockwork", "generated", "Uhrwerk", "Clockwork", None, None),
    ("raw_chronite", "generated", "Rohes Chronit", "Raw Chronite", None, None),
    ("chronite_crystal", "generated", "Chronit-Kristall", "Chronite Crystal", None, None),
    ("time_dust", "generated", "Zeitstaub", "Time Dust", None, None),
    ("chronite_seed", "generated", "Chronit-Keim", "Chronite Seed", None, None),
    ("chrono_debug_wand", "handheld", "Chrono-Debugstab", "Chrono Debug Wand", None, None),
]

BLOCKS = {
    "brass_block": ("Messingblock", "Block of Brass"),
    "clockmaker_bench": ("Uhrmacher-Werkbank", "Clockmaker's Bench"),
    "time_sand": ("Zeitsand", "Time Sand"),
    "time_glass": ("Zeitglas", "Time Glass"),
    "deepslate_chronite_ore": ("Tiefenschiefer-Chronit-Erz", "Deepslate Chronite Ore"),
    "chronite_sprout": ("Chronit-Keim", "Chronite Sprout"),
    "time_condenser": ("Zeitkondensator", "Time Condenser"),
    "time_field_projector": ("Zeitfeld-Projektor", "Time Field Projector"),
}

LANG_EXTRA = {
    "de_de": {
        "itemGroup.timemod.time": "TIME",
        "container.timemod.clockmaker_bench": "Uhrmacher-Werkbank",
        "tooltip.timemod.tension": "Federspannung: %s %%",
        "tooltip.timemod.wind_hint": "Schleichen + Rechtsklick halten: aufziehen",
        "tooltip.timemod.chronon": "Chronon: %s / %s",
        "tooltip.timemod.mode_hint": "Schleichen + Rechtsklick: Faktor wechseln",
        "message.timemod.wind_up": "Die Feder ist entspannt – Schleichen + Rechtsklick halten zum Aufziehen",
        "message.timemod.no_chronon": "Zu wenig Chronon (%s nötig)",
        "message.timemod.factor": "Zeitfaktor: %s",
        "message.timemod.rewind.nothing": "Hier gibt es noch nichts zum Zurückspulen",
        "message.timemod.condenser.status": "Kondensator: %s / %s Chronon",
        "message.timemod.projector.status": "Projektor: %s× · Radius %s · %s Chronon · %s Ch/s",
        "message.timemod.pocket_watch.time": "Tag %s · %s Uhr · %s",
        "message.timemod.pocket_watch.flow": "Zeitfluss %s×",
        "message.timemod.pocket_watch.stopped": "Die Zeit steht still",
        "moon.timemod.full": "Vollmond",
        "moon.timemod.waning_gibbous": "abnehmender Mond",
        "moon.timemod.last_quarter": "letztes Viertel",
        "moon.timemod.waning_crescent": "abnehmende Sichel",
        "moon.timemod.new": "Neumond",
        "moon.timemod.waxing_crescent": "zunehmende Sichel",
        "moon.timemod.first_quarter": "erstes Viertel",
        "moon.timemod.waxing_gibbous": "zunehmender Mond",
        "entity.timemod.chrono_knife": "Chrono-Wurfmesser",
    },
    "en_us": {
        "itemGroup.timemod.time": "TIME",
        "container.timemod.clockmaker_bench": "Clockmaker's Bench",
        "tooltip.timemod.tension": "Spring tension: %s %%",
        "tooltip.timemod.wind_hint": "Sneak + hold right-click: wind up",
        "tooltip.timemod.chronon": "Chronon: %s / %s",
        "tooltip.timemod.mode_hint": "Sneak + right-click: change factor",
        "message.timemod.wind_up": "The spring is slack – sneak + hold right-click to wind it up",
        "message.timemod.no_chronon": "Not enough Chronon (%s needed)",
        "message.timemod.factor": "Time factor: %s",
        "message.timemod.rewind.nothing": "There is nothing to rewind here yet",
        "message.timemod.condenser.status": "Condenser: %s / %s Chronon",
        "message.timemod.projector.status": "Projector: %s× · radius %s · %s Chronon · %s Ch/s",
        "message.timemod.pocket_watch.time": "Day %s · %s · %s",
        "message.timemod.pocket_watch.flow": "time flow %s×",
        "message.timemod.pocket_watch.stopped": "time stands still",
        "moon.timemod.full": "full moon",
        "moon.timemod.waning_gibbous": "waning gibbous",
        "moon.timemod.last_quarter": "last quarter",
        "moon.timemod.waning_crescent": "waning crescent",
        "moon.timemod.new": "new moon",
        "moon.timemod.waxing_crescent": "waxing crescent",
        "moon.timemod.first_quarter": "first quarter",
        "moon.timemod.waxing_gibbous": "waxing gibbous",
        "entity.timemod.chrono_knife": "Chrono Knife",
    },
}


# ------------------------------------------------------------------------------------------
# Modelle und Blockzustände
# ------------------------------------------------------------------------------------------

def item_models():
    for item_id, kind, *_ in ITEMS:
        parent = "minecraft:item/handheld" if kind == "handheld" else "minecraft:item/generated"
        write(ASSETS / f"models/item/{item_id}.json", {"parent": parent, "textures": {"layer0": t(f"item/{item_id}")}})
    for block_id in BLOCKS:
        if block_id == "chronite_sprout":
            continue
        write(ASSETS / f"models/item/{block_id}.json", {"parent": t(f"block/{block_id}")})


def cube_all(name, texture=None):
    model = {"parent": "minecraft:block/cube_all", "textures": {"all": t(f"block/{texture or name}")}}
    write(ASSETS / f"models/block/{name}.json", model)


def simple_state(name, model=None):
    write(ASSETS / f"blockstates/{name}.json", {"variants": {"": {"model": t(f"block/{model or name}")}}})


def block_models():
    cube_all("brass_block")
    simple_state("brass_block")
    cube_all("time_sand")
    simple_state("time_sand")
    cube_all("time_glass")
    simple_state("time_glass")

    # Chronit-Erz: fest und „durchgeflackert“
    cube_all("deepslate_chronite_ore")
    cube_all("deepslate_chronite_ore_phased")
    write(ASSETS / "blockstates/deepslate_chronite_ore.json", {"variants": {
        "phased=false": {"model": t("block/deepslate_chronite_ore")},
        "phased=true": {"model": t("block/deepslate_chronite_ore_phased")},
    }})

    # Uhrmacher-Werkbank
    write(ASSETS / "models/block/clockmaker_bench.json", {
        "parent": "minecraft:block/orientable_with_bottom",
        "textures": {
            "top": t("block/clockmaker_bench_top"),
            "front": t("block/clockmaker_bench_front"),
            "side": t("block/clockmaker_bench_side"),
            "bottom": "minecraft:block/spruce_planks",
        },
    })
    write(ASSETS / "blockstates/clockmaker_bench.json", {"variants": {
        f"facing={d}": {"model": t("block/clockmaker_bench"), **({"y": y} if y else {})}
        for d, y in (("north", 0), ("east", 90), ("south", 180), ("west", 270))
    }})

    # Chronit-Keim: vier Wachstumsstufen als Kreuzmodell
    write(ASSETS / "blockstates/chronite_sprout.json", {"variants": {
        f"age={a}": {"model": t(f"block/chronite_sprout_stage{a}")} for a in range(4)
    }})
    for a in range(4):
        write(ASSETS / f"models/block/chronite_sprout_stage{a}.json", {
            "parent": "minecraft:block/cross",
            "textures": {"cross": t(f"block/chronite_sprout_stage{a}")},
        })

    # Zeitkondensator: Messingsockel, Glaskammer, Messingdeckel
    write(ASSETS / "models/block/time_condenser.json", {
        "parent": "minecraft:block/block",
        "textures": {
            "particle": t("block/time_condenser_side"),
            "base": t("block/time_condenser_base"),
            "side": t("block/time_condenser_side"),
            "top": t("block/time_condenser_top"),
            "glass": t("block/time_glass"),
        },
        "elements": [
            {"from": [1, 0, 1], "to": [15, 3, 15], "faces": {
                "down": {"texture": "#base"}, "up": {"texture": "#top"},
                "north": {"texture": "#side"}, "south": {"texture": "#side"}, "east": {"texture": "#side"}, "west": {"texture": "#side"}}},
            {"from": [2, 3, 2], "to": [14, 11, 14], "faces": {
                "north": {"texture": "#glass"}, "south": {"texture": "#glass"}, "east": {"texture": "#glass"}, "west": {"texture": "#glass"}}},
            {"from": [1, 11, 1], "to": [15, 12, 15], "faces": {
                "down": {"texture": "#base"}, "up": {"texture": "#top"},
                "north": {"texture": "#side"}, "south": {"texture": "#side"}, "east": {"texture": "#side"}, "west": {"texture": "#side"}}},
            *[{"from": [x, 3, z], "to": [x + 1, 11, z + 1], "faces": {
                "north": {"texture": "#base"}, "south": {"texture": "#base"}, "east": {"texture": "#base"}, "west": {"texture": "#base"}}}
              for x, z in ((1, 1), (14, 1), (1, 14), (14, 14))],
        ],
    })
    simple_state("time_condenser")

    # Zeitfeld-Projektor: an/aus
    for suffix in ("", "_active"):
        write(ASSETS / f"models/block/time_field_projector{suffix}.json", {
            "parent": "minecraft:block/cube_bottom_top",
            "textures": {
                "top": t(f"block/time_field_projector_top{suffix}"),
                "side": t(f"block/time_field_projector_side{suffix}"),
                "bottom": t("block/time_field_projector_bottom"),
            },
        })
    write(ASSETS / "blockstates/time_field_projector.json", {"variants": {
        "active=false": {"model": t("block/time_field_projector")},
        "active=true": {"model": t("block/time_field_projector_active")},
    }})


# ------------------------------------------------------------------------------------------
# Beutetabellen
# ------------------------------------------------------------------------------------------

def survives_explosion():
    return [{"condition": "minecraft:survives_explosion"}]


def self_drop(name):
    write(DATA / f"{NS}/loot_tables/blocks/{name}.json", {
        "type": "minecraft:block",
        "pools": [{"rolls": 1, "entries": [{"type": "minecraft:item", "name": t(name)}], "conditions": survives_explosion()}],
    })


SILK = {"condition": "minecraft:match_tool", "predicate": {"enchantments": [{"enchantment": "minecraft:silk_touch", "levels": {"min": 1}}]}}


def loot_tables():
    for name in ("brass_block", "clockmaker_bench", "time_sand", "time_condenser", "time_field_projector"):
        self_drop(name)
    write(DATA / f"{NS}/loot_tables/blocks/time_glass.json", {
        "type": "minecraft:block",
        "pools": [{"rolls": 1, "entries": [{"type": "minecraft:item", "name": t("time_glass")}], "conditions": [SILK]}],
    })
    write(DATA / f"{NS}/loot_tables/blocks/deepslate_chronite_ore.json", {
        "type": "minecraft:block",
        "pools": [{"rolls": 1, "entries": [{"type": "minecraft:alternatives", "children": [
            {"type": "minecraft:item", "name": t("deepslate_chronite_ore"), "conditions": [SILK]},
            {"type": "minecraft:item", "name": t("raw_chronite"), "functions": [
                {"function": "minecraft:apply_bonus", "enchantment": "minecraft:fortune", "formula": "minecraft:ore_drops"},
                {"function": "minecraft:explosion_decay"}]},
        ]}]}],
    })
    write(DATA / f"{NS}/loot_tables/blocks/chronite_sprout.json", {
        "type": "minecraft:block",
        "pools": [{"rolls": 1, "entries": [{"type": "minecraft:alternatives", "children": [
            {"type": "minecraft:item", "name": t("chronite_crystal"),
             "conditions": [{"condition": "minecraft:block_state_property", "block": t("chronite_sprout"), "properties": {"age": "3"}}],
             "functions": [{"function": "minecraft:set_count", "count": {"type": "minecraft:uniform", "min": 2, "max": 4}},
                           {"function": "minecraft:apply_bonus", "enchantment": "minecraft:fortune", "formula": "minecraft:uniform_bonus_count",
                            "parameters": {"bonusMultiplier": 1}},
                           {"function": "minecraft:explosion_decay"}]},
            {"type": "minecraft:item", "name": t("chronite_seed")},
        ]}]}],
    })


# ------------------------------------------------------------------------------------------
# Rezepte
# ------------------------------------------------------------------------------------------

def key_of(mapping):
    out = {}
    for k, v in mapping.items():
        out[k] = {"tag": v[1:]} if v.startswith("#") else {"item": v}
    return out


def shaped(name, pattern, keys, result, count=1, kind="minecraft:crafting_shaped"):
    recipe = {"type": kind, "pattern": pattern, "key": key_of(keys), "result": {"item": result, "count": count}}
    if kind == "minecraft:crafting_shaped":
        recipe["category"] = "misc"
    write(DATA / f"{NS}/recipes/{name}.json", recipe)


def clockmaker(name, pattern, keys, result, count=1):
    shaped(name, pattern, keys, result, count, kind=t("clockmaker"))


def shapeless(name, ingredients, result, count=1):
    write(DATA / f"{NS}/recipes/{name}.json", {
        "type": "minecraft:crafting_shapeless", "category": "misc",
        "ingredients": [{"tag": i[1:]} if i.startswith("#") else {"item": i} for i in ingredients],
        "result": {"item": result, "count": count},
    })


def recipes():
    # Grundstoffe (normale Werkbank / Ofen)
    shapeless("brass_ingot", ["minecraft:copper_ingot"] * 3 + ["minecraft:gold_ingot"], t("brass_ingot"), 4)
    shapeless("brass_nugget", [t("brass_ingot")], t("brass_nugget"), 9)
    shaped("brass_ingot_from_nuggets", ["NNN", "NNN", "NNN"], {"N": t("brass_nugget")}, t("brass_ingot"))
    shaped("brass_block", ["III", "III", "III"], {"I": t("brass_ingot")}, t("brass_block"))
    shapeless("brass_ingot_from_block", [t("brass_block")], t("brass_ingot"), 9)
    shaped("clockmaker_bench", ["BBB", "PCP", "PPP"], {"B": t("brass_ingot"), "P": "#minecraft:planks", "C": "minecraft:crafting_table"}, t("clockmaker_bench"))
    write(DATA / f"{NS}/recipes/time_glass.json", {
        "type": "minecraft:smelting", "category": "blocks", "ingredient": {"item": t("time_sand")},
        "result": t("time_glass"), "experience": 0.3, "cookingtime": 200})
    write(DATA / f"{NS}/recipes/time_dust_from_raw_chronite.json", {
        "type": "minecraft:blasting", "category": "misc", "ingredient": {"item": t("raw_chronite")},
        "result": t("time_dust"), "experience": 1.0, "cookingtime": 200})
    shapeless("chronite_seed", [t("raw_chronite"), t("time_sand"), "minecraft:bone_meal"], t("chronite_seed"))
    shapeless("time_dust", [t("chronite_crystal")], t("time_dust"), 4)
    shaped("time_condenser", ["BGB", "GCG", "BFB"], {"B": t("brass_ingot"), "G": t("time_glass"), "C": t("clockwork"), "F": "minecraft:blast_furnace"}, t("time_condenser"))
    shaped("chrono_knife", ["C", "I", "B"], {"C": t("chronite_crystal"), "I": "minecraft:iron_ingot", "B": t("brass_ingot")}, t("chrono_knife"), 4)

    # Präzisionsteile (Uhrmacher-Werkbank)
    clockmaker("brass_gear", [" N ", "NIN", " N "], {"N": t("brass_nugget"), "I": t("brass_ingot")}, t("brass_gear"), 2)
    clockmaker("mainspring", [" I ", "INI", " I "], {"I": "minecraft:iron_nugget", "N": t("brass_nugget")}, t("mainspring"))
    clockmaker("balance_wheel", [" G ", "GNG", " G "], {"G": "minecraft:gold_nugget", "N": t("brass_nugget")}, t("balance_wheel"))
    clockmaker("clockwork", ["GMG", "RBR", "GGG"], {"G": t("brass_gear"), "M": t("mainspring"), "R": "minecraft:redstone", "B": t("balance_wheel")}, t("clockwork"))

    # Stufe I
    clockmaker("pocket_watch", [" N ", "GCG", " N "], {"N": t("brass_nugget"), "G": "minecraft:glass_pane", "C": t("clockwork")}, t("pocket_watch"))
    clockmaker("growth_hourglass", ["BXB", "GCG", "BSB"], {"B": t("brass_ingot"), "X": "minecraft:bone_meal", "G": "minecraft:glass", "C": t("clockwork"), "S": "#minecraft:sand"}, t("growth_hourglass"))
    clockmaker("brake_spring", ["I I", "MCM", "I I"], {"I": "minecraft:iron_ingot", "M": t("mainspring"), "C": t("clockwork")}, t("brake_spring"))
    clockmaker("moment_lens", [" G ", "GCG", " A "], {"G": "minecraft:glass_pane", "C": t("clockwork"), "A": "minecraft:amethyst_shard"}, t("moment_lens"))
    clockmaker("recall_watch", ["NEN", "ECE", "NWN"], {"N": t("brass_nugget"), "E": "minecraft:ender_pearl", "C": t("clockwork"), "W": t("pocket_watch")}, t("recall_watch"))

    # Stufe II
    clockmaker("pocket_time_cell", ["NGN", "GCG", "NGN"], {"N": t("brass_nugget"), "G": t("time_glass"), "C": t("chronite_crystal")}, t("pocket_time_cell"))
    clockmaker("chronite_time_cell", ["BCB", "CTC", "BCB"], {"B": t("brass_ingot"), "C": t("chronite_crystal"), "T": t("pocket_time_cell")}, t("chronite_time_cell"))
    clockmaker("stasis_watch", ["CDC", "DWD", "CTC"], {"C": t("chronite_crystal"), "D": "minecraft:diamond", "W": t("pocket_watch"), "T": t("pocket_time_cell")}, t("stasis_watch"))
    clockmaker("time_accelerator", ["CRC", "GKG", "CTC"], {"C": t("chronite_crystal"), "R": "minecraft:redstone_block", "G": t("brass_gear"), "K": t("clockwork"), "T": t("pocket_time_cell")}, t("time_accelerator"))
    clockmaker("slow_pendulum", ["CBC", "SKS", "CTC"], {"C": t("chronite_crystal"), "B": t("brass_ingot"), "S": "minecraft:slime_ball", "K": t("clockwork"), "T": t("pocket_time_cell")}, t("slow_pendulum"))
    clockmaker("rewind_spool", ["CEC", "SKS", "CTC"], {"C": t("chronite_crystal"), "E": "minecraft:echo_shard", "S": "minecraft:string", "K": t("clockwork"), "T": t("pocket_time_cell")}, t("rewind_spool"))
    clockmaker("time_field_projector", ["CDC", "GEG", "BBB"], {"C": t("chronite_crystal"), "D": "minecraft:diamond", "G": t("time_glass"), "E": "minecraft:ender_eye", "B": t("brass_block")}, t("time_field_projector"))


# ------------------------------------------------------------------------------------------
# Tags und Weltgenerierung
# ------------------------------------------------------------------------------------------

def tags():
    def tag(path, values):
        write(DATA / path, {"replace": False, "values": values})
    tag("minecraft/tags/blocks/mineable/pickaxe.json", [t("brass_block"), t("deepslate_chronite_ore"), t("chronite_sprout"), t("time_condenser"), t("time_field_projector")])
    tag("minecraft/tags/blocks/mineable/shovel.json", [t("time_sand")])
    tag("minecraft/tags/blocks/mineable/axe.json", [t("clockmaker_bench")])
    tag("minecraft/tags/blocks/needs_iron_tool.json", [t("brass_block"), t("time_condenser"), t("time_field_projector")])
    tag("minecraft/tags/blocks/needs_diamond_tool.json", [t("deepslate_chronite_ore")])
    tag("c/tags/items/brass_ingots.json", [t("brass_ingot")])
    tag("c/tags/items/brass_nuggets.json", [t("brass_nugget")])
    tag("c/tags/items/brass_blocks.json", [t("brass_block")])
    tag("c/tags/items/raw_chronite_ores.json", [t("raw_chronite")])
    tag("c/tags/blocks/chronite_ores.json", [t("deepslate_chronite_ore")])


def worldgen():
    base = DATA / f"{NS}/worldgen"
    write(base / "configured_feature/chronite_ore.json", {
        "type": "minecraft:ore",
        "config": {
            "size": 4, "discard_chance_on_air_exposure": 0.5,
            "targets": [{"target": {"predicate_type": "minecraft:tag_match", "tag": "minecraft:deepslate_ore_replaceables"},
                         "state": {"Name": t("deepslate_chronite_ore"), "Properties": {"phased": "false"}}}],
        },
    })
    write(base / "configured_feature/time_sand.json", {
        "type": "minecraft:ore",
        "config": {
            "size": 24, "discard_chance_on_air_exposure": 0.0,
            "targets": [
                {"target": {"predicate_type": "minecraft:block_match", "block": "minecraft:sand"}, "state": {"Name": t("time_sand")}},
                {"target": {"predicate_type": "minecraft:block_match", "block": "minecraft:sandstone"}, "state": {"Name": t("time_sand")}},
            ],
        },
    })

    def placed(name, feature, count, low, high):
        write(base / f"placed_feature/{name}.json", {
            "feature": t(feature),
            "placement": [
                {"type": "minecraft:count", "count": count},
                {"type": "minecraft:in_square"},
                {"type": "minecraft:height_range", "height": {"type": "minecraft:uniform", "min_inclusive": low, "max_inclusive": high}},
                {"type": "minecraft:biome"},
            ],
        })
    placed("chronite_ore", "chronite_ore", 2, {"absolute": -64}, {"absolute": -12})
    placed("chronite_ore_deep_dark", "chronite_ore", 6, {"absolute": -64}, {"absolute": -20})
    placed("time_sand", "time_sand", 3, {"absolute": 40}, {"absolute": 62})


# ------------------------------------------------------------------------------------------
# Übersetzungen
# ------------------------------------------------------------------------------------------

def lang():
    for code, idx in (("de_de", 0), ("en_us", 1)):
        path = ASSETS / f"lang/{code}.json"
        data = json.loads(path.read_text(encoding="utf-8")) if path.exists() else {}
        for item_id, _kind, de, en, desc_de, desc_en in ITEMS:
            data[f"item.{NS}.{item_id}"] = de if idx == 0 else en
            desc = desc_de if idx == 0 else desc_en
            if desc:
                data[f"item.{NS}.{item_id}.desc"] = desc
        for block_id, names in BLOCKS.items():
            data[f"block.{NS}.{block_id}"] = names[idx]
        data.update(LANG_EXTRA[code])
        write(path, dict(sorted(data.items())))


def main():
    item_models()
    block_models()
    loot_tables()
    recipes()
    tags()
    worldgen()
    lang()
    print("Assets erzeugt.")


if __name__ == "__main__":
    main()
