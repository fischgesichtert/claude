"""Erzeugt alles für die Zukunft (M8): Verwitterungsregeln, Natur-Tag, Modelle der neuen Blöcke, Beute,
Rezepte, Weltgen-Feature und Texte.

Aufruf: python3 -I tools/gen_future.py   (läuft auch am Ende von gen_assets.py)
Die Regeln liegen danach in data/timemod/weathering/ und lassen sich per Datenpaket überschreiben.
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


def mc(*names):
    return ["minecraft:" + n for n in names]


COLORS = ["white", "orange", "magenta", "light_blue", "yellow", "lime", "pink", "gray", "light_gray", "cyan", "purple", "blue",
          "brown", "green", "red", "black"]
WOODS = ["oak", "spruce", "birch", "jungle", "acacia", "dark_oak", "mangrove", "cherry", "bamboo", "crimson", "warped"]
TREE_WOODS = ["oak", "spruce", "birch", "jungle", "acacia", "dark_oak", "mangrove", "cherry"]


def family(base, *parts):
    """z. B. family("stone_brick", "stairs", "slab", "wall") → stone_brick_stairs, …"""
    return [f"{base}_{p}" for p in parts]


# ------------------------------------------------------------------------------------------
# Was Menschen gebaut haben – und was die Jahre daraus machen
# ------------------------------------------------------------------------------------------

WOOD_BLOCKS = (
    [f"{w}_{p}" for w in WOODS for p in ("planks", "stairs", "slab", "fence", "fence_gate", "door", "trapdoor", "sign", "wall_sign",
                                          "hanging_sign", "wall_hanging_sign")]
    + [f"{w}_wood" for w in TREE_WOODS] + [f"stripped_{w}_wood" for w in TREE_WOODS] + [f"stripped_{w}_log" for w in TREE_WOODS]
    + ["crimson_hyphae", "warped_hyphae", "stripped_crimson_hyphae", "stripped_warped_hyphae", "stripped_crimson_stem",
       "stripped_warped_stem", "bamboo_block", "stripped_bamboo_block", "bamboo_mosaic", "bamboo_mosaic_stairs", "bamboo_mosaic_slab",
       "crafting_table", "bookshelf", "ladder", "lectern", "composter", "loom", "cartography_table", "fletching_table", "smithing_table",
       "note_block", "jukebox", "scaffolding", "beehive", "grindstone"]
)

STONE_BLOCKS = (
    ["cobblestone", "mossy_cobblestone", "stone_bricks", "mossy_stone_bricks", "cracked_stone_bricks", "chiseled_stone_bricks",
     "smooth_stone", "smooth_stone_slab", "stone_stairs", "stone_slab", "bricks", "polished_granite", "polished_diorite",
     "polished_andesite", "cut_sandstone", "cut_sandstone_slab", "chiseled_sandstone", "smooth_sandstone", "cut_red_sandstone",
     "cut_red_sandstone_slab", "chiseled_red_sandstone", "smooth_red_sandstone", "mud_bricks", "packed_mud", "cobbled_deepslate",
     "polished_deepslate", "deepslate_bricks", "cracked_deepslate_bricks", "deepslate_tiles", "cracked_deepslate_tiles",
     "chiseled_deepslate", "polished_blackstone", "polished_blackstone_bricks", "cracked_polished_blackstone_bricks",
     "chiseled_polished_blackstone", "nether_bricks", "cracked_nether_bricks", "chiseled_nether_bricks", "red_nether_bricks",
     "nether_brick_fence", "quartz_block", "quartz_pillar", "quartz_bricks", "chiseled_quartz_block", "smooth_quartz",
     "purpur_block", "purpur_pillar", "end_stone_bricks", "prismarine", "prismarine_bricks", "dark_prismarine", "furnace",
     "blast_furnace", "smoker", "stonecutter", "polished_basalt"]
    + family("cobblestone", "stairs", "slab", "wall") + family("mossy_cobblestone", "stairs", "slab", "wall")
    + family("stone_brick", "stairs", "slab", "wall") + family("mossy_stone_brick", "stairs", "slab", "wall")
    + family("brick", "stairs", "slab", "wall")
    + [f"{s}_{p}" for s in ("granite", "diorite", "andesite", "sandstone", "red_sandstone", "blackstone", "cobbled_deepslate",
                            "polished_deepslate", "deepslate_brick", "deepslate_tile", "polished_blackstone", "polished_blackstone_brick",
                            "nether_brick", "red_nether_brick", "end_stone_brick", "mud_brick", "prismarine")
       for p in ("stairs", "slab", "wall")]
    + [f"{s}_{p}" for s in ("polished_granite", "polished_diorite", "polished_andesite", "smooth_sandstone", "smooth_red_sandstone",
                            "quartz", "smooth_quartz", "purpur", "prismarine_brick", "dark_prismarine") for p in ("stairs", "slab")]
    + [f"{c}_concrete" for c in COLORS]
)

GLASS_BLOCKS = ["glass", "tinted_glass", "glass_pane"] + [f"{c}_stained_glass" for c in COLORS] + [f"{c}_stained_glass_pane" for c in COLORS]

COPPER_BLOCKS = [f"{stage}{b}" for stage in ("", "exposed_", "weathered_", "oxidized_")
                 for b in ("cut_copper", "cut_copper_stairs", "cut_copper_slab")] + \
                ["copper_block", "exposed_copper", "weathered_copper", "oxidized_copper"]

IRON_BLOCKS = ["iron_door", "iron_trapdoor", "hopper", "cauldron", "water_cauldron", "lava_cauldron", "powder_snow_cauldron", "chain",
               "brewing_stand"]

REDSTONE_BLOCKS = ["redstone_wire", "repeater", "comparator", "redstone_torch", "redstone_wall_torch", "lever", "tripwire_hook", "tripwire",
                   "daylight_detector", "observer", "piston", "sticky_piston", "piston_head", "dispenser", "dropper", "target", "rail",
                   "powered_rail", "detector_rail", "activator_rail", "calibrated_sculk_sensor"]

POTTED = ["potted_" + n for n in (
    "oak_sapling", "spruce_sapling", "birch_sapling", "jungle_sapling", "acacia_sapling", "dark_oak_sapling", "mangrove_propagule",
    "cherry_sapling", "fern", "dandelion", "poppy", "blue_orchid", "allium", "azure_bluet", "red_tulip", "orange_tulip", "white_tulip",
    "pink_tulip", "oxeye_daisy", "cornflower", "lily_of_the_valley", "wither_rose", "red_mushroom", "brown_mushroom", "cactus", "bamboo",
    "crimson_fungus", "warped_fungus", "crimson_roots", "warped_roots", "azalea_bush", "flowering_azalea_bush", "torchflower")]

CROPS = ["wheat", "carrots", "potatoes", "beetroots", "melon_stem", "attached_melon_stem", "pumpkin_stem", "attached_pumpkin_stem",
         "torchflower_crop", "pitcher_crop"]

# TIME-eigene Blöcke mit Bedeutung: Sie stehen auch nach tausend Jahren (Regel ohne Zerfall)
TIMELESS = ["time_glass", "chronos_gate_frame", "chronos_console", "chronos_portal", "home_anchor", "time_capsule", "amber_block",
            "amber_ore", "clockmaker_altar", "phage_altar", "paradox_altar", "temporal_workbench", "chrono_pillar", "lifetime_extractor",
            "time_condenser", "time_field_projector", "deepslate_chronite_ore", "chronite_sprout", "brass_block", "time_sand"]


def rule(name, blocks, priority=10, sounds=None, remove=None, convert=None, rubble=None, span=None, cover=None):
    data = {"priority": priority, "blocks": blocks}
    if sounds:
        data["sounds"] = sounds
    if remove:
        data["remove"] = remove
    if convert:
        data["convert"] = convert
    if rubble:
        data["rubble"] = rubble
    if span:
        data["span"] = span
    if cover:
        data["cover"] = cover
    write(DATA / f"{NS}/weathering/{name}.json", data)


def weathering_rules():
    base = DATA / f"{NS}/weathering"
    for old in base.glob("*.json"):
        old.unlink()
    wood_span = {"1": 7, "50": 5, "250": 3, "500": 1}
    rule("wood", mc(*WOOD_BLOCKS), sounds=["wood", "bamboo_wood", "cherry_wood", "nether_wood", "bamboo", "ladder", "scaffolding"],
         remove={"5": 0.0, "25": 0.04, "50": 0.15, "100": 0.3, "250": 0.55, "500": 0.8, "1000": 1.0}, span=wood_span)
    # Truhen halten länger: Man soll nach Jahrhunderten noch hineinsehen können
    rule("containers", mc("chest", "trapped_chest", "barrel", "chiseled_bookshelf"), priority=20,
         remove={"100": 0.0, "250": 0.1, "500": 0.25, "1000": 0.6}, span=wood_span)
    rule("glass", mc(*GLASS_BLOCKS), sounds=["glass"],
         remove={"5": 0.0, "25": 0.05, "50": 0.15, "100": 0.3, "250": 0.6, "500": 0.85, "1000": 0.97}, span={"1": 3})
    rule("stone", mc(*STONE_BLOCKS), sounds=["deepslate_bricks", "deepslate_tiles", "polished_deepslate", "nether_bricks", "mud_bricks"],
         convert=[{"to": "@mossy", "chance": {"25": 0.05, "50": 0.15, "100": 0.3, "250": 0.45, "500": 0.6, "1000": 0.7}},
                  {"to": "@cracked", "chance": {"50": 0.05, "100": 0.15, "500": 0.35, "1000": 0.5}}],
         remove={"100": 0.0, "250": 0.03, "500": 0.12, "1000": 0.3},
         rubble={"block": "minecraft:cobblestone", "chance": 0.35}, span={"1": 12, "100": 9, "500": 6, "1000": 4})
    rule("glazed_terracotta", mc(*[f"{c}_glazed_terracotta" for c in COLORS]), remove={"500": 0.0, "1000": 0.1}, span={"1": 10})
    rule("copper", mc(*COPPER_BLOCKS), sounds=["copper"],
         convert=[{"to": "@oxidize", "steps": {"1": 0, "3": 1, "25": 2, "100": 3}, "chance": 1.0}], span={"1": 10})
    rule("iron_block", mc("iron_block") + [t("rusty_iron_block")],
         convert=[{"to": t("rusty_iron_block"), "chance": {"10": 0.1, "25": 0.4, "50": 0.85, "100": 1.0}}],
         remove={"250": 0.0, "500": 0.15, "1000": 0.5}, span={"1": 12, "1000": 6})
    rule("iron_bars", mc("iron_bars") + [t("rusty_iron_bars")],
         convert=[{"to": t("rusty_iron_bars"), "chance": {"10": 0.1, "25": 0.4, "50": 0.85, "100": 1.0}}],
         remove={"100": 0.0, "250": 0.2, "500": 0.5, "1000": 0.9}, span={"1": 6})
    rule("iron", mc(*IRON_BLOCKS), sounds=["metal", "chain", "anvil"],
         remove={"25": 0.0, "50": 0.05, "100": 0.15, "250": 0.4, "500": 0.7, "1000": 1.0}, span={"1": 10})
    # Ambosse: erst angeschlagen, dann beschädigt, dann Schrott
    anvil_remove = {"250": 0.0, "500": 0.2, "1000": 0.6}
    rule("anvil", mc("anvil"), convert=[{"to": "minecraft:damaged_anvil", "chance": {"250": 0.3, "500": 0.7, "1000": 1.0}},
                                         {"to": "minecraft:chipped_anvil", "chance": {"25": 0.3, "100": 1.0}}], remove=anvil_remove)
    rule("chipped_anvil", mc("chipped_anvil"), convert=[{"to": "minecraft:damaged_anvil", "chance": {"100": 0.3, "500": 1.0}}],
         remove=anvil_remove)
    rule("damaged_anvil", mc("damaged_anvil"), remove=anvil_remove)
    rule("wool", ["#minecraft:wool", "#minecraft:wool_carpets", "#minecraft:beds", "#minecraft:banners"], sounds=["wool"],
         remove={"5": 0.0, "10": 0.05, "50": 0.5, "100": 0.85, "500": 1.0}, span={"1": 4})
    # Licht: Fackeln und Laternen sind nach einem Jahr aus, später ganz fort
    rule("torches", mc("torch", "soul_torch") + [t("burnt_torch")], priority=30,
         convert=[{"to": t("burnt_torch"), "chance": 1.0}], remove={"50": 0.0, "100": 0.1, "250": 0.5, "500": 1.0}, span={"1": 4})
    rule("wall_torches", mc("wall_torch", "soul_wall_torch") + [t("burnt_wall_torch")], priority=30,
         convert=[{"to": t("burnt_wall_torch"), "chance": 1.0}], remove={"50": 0.0, "100": 0.1, "250": 0.5, "500": 1.0}, span={"1": 4})
    rule("lanterns", mc("lantern", "soul_lantern") + [t("dull_lantern")], priority=30,
         convert=[{"to": t("dull_lantern"), "chance": 1.0}], remove={"100": 0.0, "250": 0.1, "500": 0.5, "1000": 1.0}, span={"1": 4})
    rule("campfires", mc("campfire", "soul_campfire"), priority=30, convert=[{"to": "@unlit", "chance": 1.0}],
         remove={"25": 0.0, "50": 0.3, "100": 0.7, "250": 1.0})
    rule("candles", ["#minecraft:candles"], priority=30, convert=[{"to": "@unlit", "chance": 1.0}], remove={"25": 0.0, "50": 0.5, "250": 1.0})
    rule("lamps", mc("redstone_lamp"), priority=30, convert=[{"to": "@unlit", "chance": 1.0}], remove={"500": 0.0, "1000": 0.3})
    rule("organic", mc("hay_block", "slime_block", "honey_block", "honeycomb_block", "dried_kelp_block", "cake", "jack_o_lantern",
                       "carved_pumpkin") + ["#minecraft:candle_cakes"], priority=30,
         remove={"1": 0.3, "5": 0.6, "25": 0.9, "50": 1.0}, span={"1": 4})
    # Felder: erst Erde, dann Wiese, dann Wald
    rule("farmland", mc("farmland"), priority=30,
         convert=[{"to": "minecraft:grass_block", "chance": {"10": 0.1, "25": 0.4, "50": 1.0}}, {"to": "minecraft:dirt", "chance": 1.0}],
         cover=[{"block": "minecraft:oak_sapling", "chance": {"250": 0.0, "500": 0.02, "1000": 0.05}},
                {"block": "minecraft:tall_grass", "chance": {"25": 0.05, "100": 0.15, "500": 0.3}},
                {"block": "minecraft:poppy", "chance": {"25": 0.02, "250": 0.06, "500": 0.08}},
                {"block": "minecraft:dandelion", "chance": {"25": 0.02, "250": 0.06, "500": 0.08}},
                {"block": "minecraft:cornflower", "chance": {"250": 0.0, "500": 0.04}},
                {"block": "minecraft:grass", "chance": {"1": 0.35, "50": 0.6}}])
    rule("paths", mc("dirt_path"), priority=30, convert=[{"to": "minecraft:grass_block", "chance": {"5": 0.1, "25": 0.6, "50": 1.0}}])
    rule("crops", mc(*CROPS), priority=30, remove={"1": 0.5}, convert=[{"to": "minecraft:grass", "chance": 1.0}])
    rule("potted_plants", mc(*POTTED), priority=30, convert=[{"to": "minecraft:potted_dead_bush", "chance": {"1": 0.3, "5": 0.6, "25": 1.0}}])
    rule("redstone", mc(*REDSTONE_BLOCKS) + ["#minecraft:buttons", "#minecraft:pressure_plates"], priority=25,
         remove={"10": 0.0, "25": 0.1, "50": 0.4, "250": 0.8, "500": 1.0}, span={"1": 6})
    rule("timeless", [t(n) for n in TIMELESS], priority=100)
    write(base / "reclaim.json", {
        "vines": {"10": 0.01, "50": 0.05, "250": 0.15, "1000": 0.3},
        "moss": {"25": 0.02, "100": 0.08, "500": 0.2, "1000": 0.3},
        "cobwebs": {"2": 0.01, "25": 0.06, "250": 0.08, "1000": 0.05},
        "sand": {"25": 0.02, "250": 0.15, "1000": 0.35},
        "snow": {"1": 0.15, "100": 0.35, "1000": 0.6},
        "ruin_trees": {"100": 0.0, "250": 0.004, "1000": 0.012},
        "field_trees": {"250": 0.0, "500": 0.01, "1000": 0.03},
        "sign_fade": {"1": 0.02, "5": 0.08, "50": 0.45, "100": 0.75, "250": 1.0},
        "bones": {"10": 0.0, "25": 0.8, "1000": 0.6},
        "descendants": {"10": 0.0, "25": 0.6, "1000": 0.8},
    })


# Was die Welt selbst gemacht hat: bekommt nie eine Rückfall-Regel über den Klang
NATURAL = [
    "#minecraft:base_stone_overworld", "#minecraft:base_stone_nether", "#minecraft:dirt", "#minecraft:sand", "#minecraft:logs",
    "#minecraft:leaves", "#minecraft:flowers", "#minecraft:saplings", "#minecraft:replaceable", "#minecraft:ice", "#minecraft:snow",
    "#minecraft:terracotta", "#minecraft:coral_blocks", "#minecraft:corals", "#minecraft:wall_corals", "#minecraft:coal_ores",
    "#minecraft:iron_ores", "#minecraft:copper_ores", "#minecraft:gold_ores", "#minecraft:redstone_ores", "#minecraft:lapis_ores",
    "#minecraft:diamond_ores", "#minecraft:emerald_ores", "#minecraft:nylium", "#minecraft:wart_blocks", "#minecraft:cave_vines",
    {"id": "#c:ores", "required": False}, {"id": "#c:stones", "required": False},
] + mc("gravel", "clay", "bedrock", "water", "lava", "obsidian", "crying_obsidian", "magma_block", "sandstone", "red_sandstone",
       "calcite", "tuff", "dripstone_block", "pointed_dripstone", "amethyst_block", "budding_amethyst", "small_amethyst_bud",
       "medium_amethyst_bud", "large_amethyst_bud", "amethyst_cluster", "moss_block", "moss_carpet", "mud", "muddy_mangrove_roots",
       "mangrove_roots", "brown_mushroom_block", "red_mushroom_block", "mushroom_stem", "brown_mushroom", "red_mushroom", "cactus",
       "sugar_cane", "bamboo", "bamboo_sapling", "kelp", "kelp_plant", "seagrass", "tall_seagrass", "sea_pickle", "lily_pad", "vine",
       "glow_lichen", "sculk", "sculk_vein", "sculk_catalyst", "sculk_shrieker", "sculk_sensor", "spore_blossom", "azalea",
       "flowering_azalea", "big_dripleaf", "big_dripleaf_stem", "small_dripleaf", "hanging_roots", "rooted_dirt", "pumpkin", "melon",
       "bee_nest", "sweet_berry_bush", "cocoa", "nether_wart", "packed_ice", "blue_ice", "powder_snow", "snow_block", "end_stone",
       "basalt", "smooth_basalt", "blackstone", "soul_sand", "soul_soil", "netherrack", "glowstone", "shroomlight", "crimson_fungus",
       "warped_fungus", "crimson_roots", "warped_roots", "twisting_vines", "twisting_vines_plant", "weeping_vines",
       "weeping_vines_plant", "nether_sprouts", "ancient_debris", "suspicious_sand", "suspicious_gravel", "infested_stone",
       "infested_cobblestone", "infested_stone_bricks", "infested_mossy_stone_bricks", "infested_cracked_stone_bricks",
       "infested_chiseled_stone_bricks", "infested_deepslate", "spawner", "dead_bush", "frogspawn", "turtle_egg", "sniffer_egg",
       "chorus_plant", "chorus_flower", "sponge", "wet_sponge")


# ------------------------------------------------------------------------------------------
# Neue Blöcke: erloschene Fackel und Laterne, Rost, Knochenhaufen
# ------------------------------------------------------------------------------------------

def block_assets():
    def model(name, obj):
        write(ASSETS / f"models/block/{name}.json", obj)

    def state(name, obj):
        write(ASSETS / f"blockstates/{name}.json", obj)

    def item(name, obj):
        write(ASSETS / f"models/item/{name}.json", obj)

    model("burnt_torch", {"parent": "minecraft:block/template_torch", "textures": {"torch": t("block/burnt_torch")}})
    model("burnt_wall_torch", {"parent": "minecraft:block/template_torch_wall", "textures": {"torch": t("block/burnt_torch")}})
    state("burnt_torch", {"variants": {"": {"model": t("block/burnt_torch")}}})
    state("burnt_wall_torch", {"variants": {
        f"facing={d}": {"model": t("block/burnt_wall_torch"), **({"y": y} if y else {})}
        for d, y in (("east", 0), ("north", 270), ("south", 90), ("west", 180))}})
    item("burnt_torch", {"parent": "minecraft:item/generated", "textures": {"layer0": t("block/burnt_torch")}})

    model("dull_lantern", {"parent": "minecraft:block/template_lantern", "textures": {"lantern": t("block/dull_lantern")}})
    model("dull_lantern_hanging", {"parent": "minecraft:block/template_hanging_lantern", "textures": {"lantern": t("block/dull_lantern")}})
    state("dull_lantern", {"variants": {"hanging=false": {"model": t("block/dull_lantern")},
                                        "hanging=true": {"model": t("block/dull_lantern_hanging")}}})
    item("dull_lantern", {"parent": "minecraft:item/generated", "textures": {"layer0": t("item/dull_lantern")}})

    model("rusty_iron_block", {"parent": "minecraft:block/cube_all", "textures": {"all": t("block/rusty_iron_block")}})
    state("rusty_iron_block", {"variants": {"": {"model": t("block/rusty_iron_block")}}})
    item("rusty_iron_block", {"parent": t("block/rusty_iron_block")})

    bars = t("block/rusty_iron_bars")
    for part in ("post_ends", "post", "cap", "cap_alt", "side", "side_alt"):
        model(f"rusty_iron_bars_{part}", {"parent": f"minecraft:block/iron_bars_{part}",
                                          "textures": {"particle": bars, "bars": bars, "edge": bars}})
    none = {"east": "false", "north": "false", "south": "false", "west": "false"}

    def only(direction):
        return {**none, direction: "true"}

    m = lambda part: t(f"block/rusty_iron_bars_{part}")  # noqa: E731
    state("rusty_iron_bars", {"multipart": [
        {"apply": {"model": m("post_ends")}},
        {"apply": {"model": m("post")}, "when": none},
        {"apply": {"model": m("cap")}, "when": only("north")},
        {"apply": {"model": m("cap"), "y": 90}, "when": only("east")},
        {"apply": {"model": m("cap_alt")}, "when": only("south")},
        {"apply": {"model": m("cap_alt"), "y": 90}, "when": only("west")},
        {"apply": {"model": m("side")}, "when": {"north": "true"}},
        {"apply": {"model": m("side"), "y": 90}, "when": {"east": "true"}},
        {"apply": {"model": m("side_alt")}, "when": {"south": "true"}},
        {"apply": {"model": m("side_alt"), "y": 90}, "when": {"west": "true"}},
    ]})
    item("rusty_iron_bars", {"parent": "minecraft:item/generated", "textures": {"layer0": bars}})

    model("bone_pile", {"parent": "minecraft:block/carpet", "textures": {"wool": t("block/bone_pile")}})
    state("bone_pile", {"variants": {"": {"model": t("block/bone_pile")}}})
    item("bone_pile", {"parent": t("block/bone_pile")})


def loot_tables():
    def self_drop(name):
        write(DATA / f"{NS}/loot_tables/blocks/{name}.json", {
            "type": "minecraft:block",
            "pools": [{"rolls": 1, "entries": [{"type": "minecraft:item", "name": t(name)}],
                       "conditions": [{"condition": "minecraft:survives_explosion"}]}],
        })

    for name in ("burnt_torch", "dull_lantern", "rusty_iron_block", "rusty_iron_bars"):
        self_drop(name)
    write(DATA / f"{NS}/loot_tables/blocks/bone_pile.json", {
        "type": "minecraft:block",
        "pools": [{"rolls": 1, "entries": [{"type": "minecraft:item", "name": "minecraft:bone", "functions": [
            {"function": "minecraft:set_count", "count": {"type": "minecraft:uniform", "min": 1, "max": 3}},
            {"function": "minecraft:explosion_decay"}]}]}],
    })


def recipes():
    base = DATA / f"{NS}/recipes"
    write(base / "torch_from_burnt_torch.json", {
        "type": "minecraft:crafting_shapeless", "category": "misc",
        "ingredients": [{"item": t("burnt_torch")}, {"tag": "minecraft:coals"}], "result": {"item": "minecraft:torch"}})
    write(base / "lantern_from_dull_lantern.json", {
        "type": "minecraft:crafting_shapeless", "category": "misc",
        "ingredients": [{"item": t("dull_lantern")}, {"item": "minecraft:torch"}], "result": {"item": "minecraft:lantern"}})
    write(base / "rusty_iron_block.json", {
        "type": "minecraft:crafting_shaped", "category": "building", "pattern": ["RRR", "RRR", "RRR"],
        "key": {"R": {"item": t("rusty_iron")}}, "result": {"item": t("rusty_iron_block")}})
    write(base / "rusty_iron_from_block.json", {
        "type": "minecraft:crafting_shapeless", "category": "misc",
        "ingredients": [{"item": t("rusty_iron_block")}], "result": {"item": t("rusty_iron"), "count": 9}})
    write(base / "iron_nugget_from_rusty_iron_bars.json", {
        "type": "minecraft:blasting", "category": "misc", "ingredient": {"item": t("rusty_iron_bars")},
        "result": "minecraft:iron_nugget", "experience": 0.1, "cookingtime": 100})
    write(base / "bone_meal_from_bone_pile.json", {
        "type": "minecraft:crafting_shapeless", "category": "misc",
        "ingredients": [{"item": t("bone_pile")}], "result": {"item": "minecraft:bone_meal", "count": 3}})


def worldgen():
    base = DATA / f"{NS}/worldgen"
    write(base / "configured_feature/weathering.json", {"type": t("weathering"), "config": {}})
    write(base / "placed_feature/weathering.json", {"feature": t("weathering"), "placement": []})


def tags():
    write(DATA / f"{NS}/tags/blocks/weathering/natural.json", {"replace": False, "values": NATURAL})


LANG = {
    "de_de": {
        "block.timemod.burnt_torch": "Erloschene Fackel",
        "block.timemod.burnt_torch.desc": "Was nach Jahren von einer Fackel bleibt. Mit Kohle wird wieder eine Fackel daraus.",
        "block.timemod.burnt_wall_torch": "Erloschene Wandfackel",
        "block.timemod.dull_lantern": "Erloschene Laterne",
        "block.timemod.dull_lantern.desc": "Seit Jahrzehnten ohne Öl. Mit einer Fackel brennt sie wieder.",
        "block.timemod.rusty_iron_block": "Rostiger Eisenblock",
        "block.timemod.rusty_iron_bars": "Rostige Gitterstäbe",
        "block.timemod.bone_pile": "Knochenhaufen",
        "block.timemod.bone_pile.desc": "Was von den Tieren blieb, die hier einmal lebten.",
        "tooltip.timemod.echo": "Zeit-Echo – zerfällt bei Berührung",
        "message.timemod.echo.dissolved": "Ein Zeit-Echo zerfällt zu Staub – nur die Zeitkapsel trägt Dinge durch die Zeit",
    },
    "en_us": {
        "block.timemod.burnt_torch": "Burnt-Out Torch",
        "block.timemod.burnt_torch.desc": "What remains of a torch after years. Add coal to make a torch again.",
        "block.timemod.burnt_wall_torch": "Burnt-Out Wall Torch",
        "block.timemod.dull_lantern": "Dull Lantern",
        "block.timemod.dull_lantern.desc": "Out of oil for decades. A torch lights it again.",
        "block.timemod.rusty_iron_block": "Rusty Iron Block",
        "block.timemod.rusty_iron_bars": "Rusty Iron Bars",
        "block.timemod.bone_pile": "Bone Pile",
        "block.timemod.bone_pile.desc": "What remains of the animals that once lived here.",
        "tooltip.timemod.echo": "Time echo – crumbles at a touch",
        "message.timemod.echo.dissolved": "A time echo crumbles to dust – only the time capsule carries things through time",
    },
}


def lang():
    for code, entries in LANG.items():
        path = ASSETS / f"lang/{code}.json"
        data = json.loads(path.read_text(encoding="utf-8")) if path.exists() else {}
        data.update(entries)
        write(path, dict(sorted(data.items())))


def main():
    weathering_rules()
    tags()
    block_assets()
    loot_tables()
    recipes()
    worldgen()
    lang()
    print("Zukunft erzeugt.")


if __name__ == "__main__":
    main()
