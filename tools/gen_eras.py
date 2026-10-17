"""Erzeugt die Epochen der Zeitmaschine als Datenpaket: Dimensionen, Dimensionstypen, abgewandelte
Noise-Einstellungen (Meeresspiegel, eigene Kontinente), Urzeit-Biome und die Biom-Umdeutung pro Epoche.

Aufruf: python3 -I tools/gen_eras.py
Grundlage sind die Vanilla-Worldgen-Daten aus dem Minecraft-Jar im Gradle-Cache.
"""
import copy
import glob
import json
import re
import zipfile
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
DATA = ROOT / "src/main/resources/data/timemod"
NS = "timemod"


def vanilla_jar():
    pattern = str(Path.home() / ".gradle/caches/fabric-loom/minecraftMaven/net/minecraft/minecraft-common/*/minecraft-common-*.jar")
    jars = [j for j in glob.glob(pattern) if "intermediary" not in j]
    if not jars:
        raise SystemExit("Minecraft-Jar nicht gefunden – erst ./gradlew build ausführen")
    return zipfile.ZipFile(jars[0])


JAR = vanilla_jar()


def vanilla(path):
    return json.loads(JAR.read(f"data/minecraft/{path}"))


def write(path, data):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(data, indent=2, ensure_ascii=False) + "\n", encoding="utf-8")


# ------------------------------------------------------------------------------------------
# Epochen (muss zu era/Eras.java passen)
# ------------------------------------------------------------------------------------------

# (Jahre, Dimensionstyp, Noise-Einstellungen, Biom-Umdeutung)
ERAS = [
    (1, "era_future", "minecraft:overworld", None),
    (2, "era_future", "minecraft:overworld", None),
    (3, "era_future", "minecraft:overworld", None),
    (4, "era_future", "minecraft:overworld", None),
    (5, "era_future", "minecraft:overworld", None),
    (10, "era_future", "minecraft:overworld", None),
    (25, "era_future", "minecraft:overworld", None),
    (50, "era_future", "minecraft:overworld", None),
    (100, "era_future", "minecraft:overworld", None),
    (250, "era_future", "minecraft:overworld", None),
    (500, "era_future", "minecraft:overworld", None),
    (1000, "era_future", "minecraft:overworld", None),
    (-1, "era_past", "minecraft:overworld", None),
    (-2, "era_past", "minecraft:overworld", None),
    (-3, "era_past", "minecraft:overworld", None),
    (-4, "era_past", "minecraft:overworld", None),
    (-5, "era_past", "minecraft:overworld", None),
    (-10, "era_past", "minecraft:overworld", None),
    (-50, "era_past", "minecraft:overworld", None),
    (-100, "era_past", "minecraft:overworld", "wild"),
    (-500, "era_past", "minecraft:overworld", "wild"),
    (-1000, "era_past", "minecraft:overworld", "primeval"),
    (-5000, "era_past", "minecraft:overworld", "primeval"),
    (-20000, "era_ice", "timemod:ice_age", "ice"),
    (-100000, "era_ice", "timemod:pleistocene", "ice"),
    (-1000000, "era_steppe", "timemod:pleistocene", "steppe"),
    (-10000000, "era_deep", "timemod:miocene", "miocene"),
    (-66000000, "era_deep", "timemod:cretaceous", "cretaceous"),
    (-70000000, "era_deep", "timemod:cretaceous", "cretaceous"),
    (-100000000, "era_deep", "timemod:cretaceous", "cretaceous"),
    (-150000000, "era_deep", "timemod:jurassic", "jurassic"),
    (-200000000, "era_deep", "timemod:rift", "triassic"),
    (-245000000, "era_deep", "timemod:pangaea", "pangaea"),
]


def era_id(years):
    return ("p" if years > 0 else "m") + str(abs(years))


# ------------------------------------------------------------------------------------------
# Noise-Einstellungen
# ------------------------------------------------------------------------------------------

TERRAIN_FUNCTIONS = ["continents", "erosion", "ridges", "ridges_folded", "offset", "factor", "jaggedness", "depth", "sloped_cheese",
                     "base_3d_noise"]
CLIMATE_NOISES = ["continentalness", "erosion", "ridge", "temperature", "vegetation"]


def rename_noises(text, name):
    # Nur echte Noise-Verweise umbenennen: "minecraft:temperature" ist auch ein Oberflächen-Bedingungstyp
    for noise in CLIMATE_NOISES:
        text = text.replace(f'"noise": "minecraft:{noise}"', f'"noise": "{NS}:{name}_{noise}"')
    return text


def shift_surface(rule, delta):
    """Oberflächenregeln am Meeresspiegel (Sumpfwasser, Ufer) mit dem Meeresspiegel verschieben."""
    text = json.dumps(rule)
    for level in (60, 62, 63):
        text = text.replace(f'"absolute": {level}', f'"absolute": {level + delta}')
    return json.loads(text)


# Bausteine für Oberflächenregeln
def block(name, **properties):
    state = {"Name": name if ":" in name else f"minecraft:{name}"}
    if properties:
        state["Properties"] = properties
    return {"type": "minecraft:block", "result_state": state}


def when(condition, then):
    return {"type": "minecraft:condition", "if_true": condition, "then_run": then}


def first(*rules):
    return {"type": "minecraft:sequence", "sequence": list(rules)}


def in_biome(*names):
    return {"type": "minecraft:biome", "biome_is": [f"{NS}:{n}" for n in names]}


def depth(surface="floor", add_surface_depth=False, secondary=0):
    return {"type": "minecraft:stone_depth", "offset": 0, "add_surface_depth": add_surface_depth, "secondary_depth_range": secondary,
            "surface_type": surface}


def noise_above(threshold, noise="minecraft:surface", below=1.7976931348623157e308):
    return {"type": "minecraft:noise_threshold", "noise": noise, "min_threshold": threshold, "max_threshold": below}


def y_above(level):
    return {"type": "minecraft:y_above", "anchor": {"absolute": level}, "surface_depth_multiplier": 0, "add_stone_depth": False}


ON_FLOOR = depth()
UNDER_FLOOR = depth(add_surface_depth=True)
DEEP_UNDER_FLOOR = depth(add_surface_depth=True, secondary=6)
VERY_DEEP_UNDER_FLOOR = depth(add_surface_depth=True, secondary=30)
ON_CEILING = depth("ceiling")
ABOVE_WATER = {"type": "minecraft:water", "offset": -1, "surface_depth_multiplier": 0, "add_stone_depth": False}


def era_surface_rules():
    """Eigene Böden der Urzeit-Biome; Vanilla kennt sie nicht und würde überall Gras legen."""
    sand = first(when(ON_CEILING, block("sandstone")), block("sand"))
    return [
        # Pangäa: rote Wüste mit hellen Salzpfannen, darunter Sandstein und Terrakotta-Schichten
        when(in_biome("pangaea_desert"), first(
            when(UNDER_FLOOR, first(when(ON_CEILING, block("red_sandstone")), when(noise_above(0.3), block("sand")), block("red_sand"))),
            when(DEEP_UNDER_FLOOR, block("red_sandstone")),
            when(VERY_DEEP_UNDER_FLOOR, first(when(noise_above(0.0), block("orange_terracotta")), block("terracotta"))))),
        # Vulkanfelder: Basalt und Schwarzstein, glühende Magmarisse, graue Asche
        when(in_biome("volcanic_wastes"), first(
            when(ON_FLOOR, first(when(noise_above(0.26), block("magma_block")), when(noise_above(0.0), block("basalt", axis="y")),
                                 when(noise_above(-0.18), block("blackstone")), block("tuff"))),
            when(UNDER_FLOOR, first(when(noise_above(0.0), block("basalt", axis="y")), block("blackstone"))),
            when(DEEP_UNDER_FLOOR, block("tuff")))),
        # Ursumpf: Schlamm und Wassertümpel wie im Mangrovensumpf
        when(in_biome("primeval_swamp"), first(
            when(ON_FLOOR, first(when(y_above(60), when({"type": "minecraft:not", "invert": y_above(63)},
                                                          when(noise_above(0.0, "minecraft:surface_swamp"), block("water", level="0")))),
                                 block("mud"))),
            when(UNDER_FLOOR, block("mud")))),
        when(in_biome("primeval_shore"), first(when(UNDER_FLOOR, sand), when(DEEP_UNDER_FLOOR, block("sandstone")))),
        when(in_biome("primeval_ocean"), when(UNDER_FLOOR, sand)),
        when(in_biome("primeval_peaks"), when(UNDER_FLOOR, first(when(noise_above(-0.0125, "minecraft:calcite", 0.0125), block("calcite")),
                                                                  block("stone")))),
        when(in_biome("primeval_caves"), when(ON_FLOOR, block("stone"))),
        when(in_biome("conifer_forest"), when(ON_FLOOR, when(ABOVE_WATER, first(when(noise_above(0.2121), block("coarse_dirt")),
                                                                              when(noise_above(-0.1152), block("podzol")))))),
        when(in_biome("cycad_savanna", "miocene_savanna", "mammoth_steppe"), when(ON_FLOOR, when(ABOVE_WATER, when(noise_above(0.18),
                                                                                                                block("coarse_dirt"))))),
        when(in_biome("fern_jungle", "miocene_woodland"), when(ON_FLOOR, when(ABOVE_WATER, when(noise_above(0.24), block("podzol"))))),
    ]


def with_era_surfaces(rule):
    """Die Urzeit-Regeln vor die Vanilla-Regeln oberhalb der vorläufigen Oberfläche setzen."""
    for entry in rule["sequence"]:
        if entry.get("if_true", {}).get("type") == "minecraft:above_preliminary_surface":
            entry["then_run"]["sequence"][0:0] = era_surface_rules()
            return rule
    raise SystemExit("Oberflächenregel ohne above_preliminary_surface – Vanilla-Format geändert?")


def variant(name, sea_level, base="overworld", new_continents=True, continent_shift=0.0):
    """Eine abgewandelte Oberwelt: eigener Meeresspiegel, auf Wunsch eigene Kontinente und eigenes Klima."""
    source_dir = "overworld" if base == "overworld" else "overworld_amplified"
    settings = vanilla(f"worldgen/noise_settings/{base}.json")
    text = json.dumps(settings)
    if new_continents:
        # Gelände-Funktionen kopieren und umbenennen: andere Rauschwerte → andere Kontinente
        for fn in TERRAIN_FUNCTIONS:
            src = f"worldgen/density_function/{source_dir}/{fn}.json"
            try:
                data = vanilla(src)
            except KeyError:
                data = vanilla(f"worldgen/density_function/overworld/{fn}.json")
            body = json.dumps(data)
            for other in TERRAIN_FUNCTIONS:
                body = body.replace(f'"minecraft:{source_dir}/{other}"', f'"{NS}:{name}/{other}"')
                body = body.replace(f'"minecraft:overworld/{other}"', f'"{NS}:{name}/{other}"')
            data = json.loads(rename_noises(body, name))
            if fn == "continents" and continent_shift:
                data = {"type": "minecraft:add", "argument1": continent_shift, "argument2": data}
            write(DATA / f"worldgen/density_function/{name}/{fn}.json", data)
        for noise in CLIMATE_NOISES:
            write(DATA / f"worldgen/noise/{name}_{noise}.json", vanilla(f"worldgen/noise/{noise}.json"))
        for other in TERRAIN_FUNCTIONS:
            text = text.replace(f'"minecraft:{source_dir}/{other}"', f'"{NS}:{name}/{other}"')
            text = text.replace(f'"minecraft:overworld/{other}"', f'"{NS}:{name}/{other}"')
        text = rename_noises(text, name)
    settings = json.loads(text)
    delta = sea_level - settings["sea_level"]
    settings["sea_level"] = sea_level
    settings["surface_rule"] = shift_surface(with_era_surfaces(settings["surface_rule"]), delta)
    write(DATA / f"worldgen/noise_settings/{name}.json", settings)


def noise_settings():
    # Eiszeit: dieselben Kontinente, aber weniger Meer – Inseln werden zu Halbinseln, Landbrücken entstehen.
    # Minecrafts Schelfmeere sind flach: 13 Blöcke legen schon gut ein Drittel der Meere trocken.
    variant("ice_age", 50, new_continents=False)
    # Pleistozän außerhalb des Eiszeit-Höhepunkts: etwas weniger Meer als heute
    variant("pleistocene", 56, new_continents=False)
    variant("miocene", 64)
    # Kreide: Das Meer steigt in die Tiefländer – flache, warme Binnenmeere
    variant("cretaceous", 65)
    variant("jurassic", 64)
    # Trias/Jura-Grenze: Riftgräben und gewaltige Berge
    variant("rift", 63, base="amplified")
    # Pangäa: ein Superkontinent
    variant("pangaea", 63, continent_shift=0.45)


# ------------------------------------------------------------------------------------------
# Urzeit-Biome
# ------------------------------------------------------------------------------------------

FLOWERS = re.compile(r"minecraft:(flower_.*|patch_sunflower|patch_grass.*|patch_tall_grass.*|patch_pumpkin|patch_melon|patch_berry_.*|"
                     r"patch_dead_bush.*|bamboo.*|forest_flowers|patch_cactus.*)")
# Menschengemachtes hat in der Urzeit nichts verloren
BUILT = re.compile(r"minecraft:(monster_room.*|desert_well)")
NO_SPAWNERS = {k: [] for k in ("ambient", "axolotls", "creature", "misc", "monster", "underground_water_creature", "water_ambient",
                               "water_creature")}

# Name: (Vorlage, Farben, Temperatur, Gräser entfernen, zusätzliche Features – ohne Schritt: Vegetation)
BIOMES = {
    "fern_jungle": ("jungle", {"grass_color": 0x4F8A2B, "foliage_color": 0x3B7026, "sky_color": 0x8FB8DE, "fog_color": 0xC9D7B0,
                               "water_color": 0x3A7F6E, "water_fog_color": 0x0E2A22}, 0.95, True, ["minecraft:patch_large_fern"]),
    "primeval_swamp": ("mangrove_swamp", {"grass_color": 0x5D7B30, "foliage_color": 0x4E6E2A, "sky_color": 0x92B0C8, "fog_color": 0xA9B98A,
                                          "water_color": 0x4C5E3A, "water_fog_color": 0x1C2614}, 0.9, True, ["minecraft:patch_large_fern"]),
    "cycad_savanna": ("savanna", {"grass_color": 0x9C9A4A, "foliage_color": 0x8C9440, "sky_color": 0x9CBEDC, "fog_color": 0xE0C890,
                                  "water_color": 0x3E7A80, "water_fog_color": 0x0F2A2E}, 1.6, True, ["minecraft:patch_large_fern"]),
    "conifer_forest": ("old_growth_spruce_taiga", {"grass_color": 0x5F8A3A, "foliage_color": 0x47703A, "sky_color": 0x90B4D8,
                                                   "fog_color": 0xB8C8A0, "water_color": 0x3B6E70, "water_fog_color": 0x0C2426}, 0.6, True, []),
    "volcanic_wastes": ("badlands", {"grass_color": 0x8C7A3A, "foliage_color": 0x7A6A30, "sky_color": 0xB89080, "fog_color": 0xC08A70,
                                     "water_color": 0x5A4A30, "water_fog_color": 0x261A10}, 2.0, True, ["minecraft:spring_lava"]),
    "pangaea_desert": ("desert", {"sky_color": 0xE8B080, "fog_color": 0xF0C090, "water_color": 0x4A8A88, "water_fog_color": 0x123030},
                       2.0, True, []),
    "primeval_ocean": ("warm_ocean", {"sky_color": 0x8FB8DE, "fog_color": 0xC9D7B0, "water_color": 0x2E8A8A, "water_fog_color": 0x0A2E2E},
                       0.8, True, []),
    "primeval_deep_ocean": ("deep_lukewarm_ocean", {"sky_color": 0x8FB8DE, "fog_color": 0xC9D7B0, "water_color": 0x1E6A76,
                                                    "water_fog_color": 0x06202A}, 0.6, True, []),
    "primeval_shore": ("beach", {"sky_color": 0x8FB8DE, "fog_color": 0xC9D7B0, "water_color": 0x2E8A8A, "water_fog_color": 0x0A2E2E},
                       0.9, True, []),
    "primeval_river": ("river", {"grass_color": 0x4F8A2B, "foliage_color": 0x3B7026, "sky_color": 0x8FB8DE, "fog_color": 0xC9D7B0,
                                 "water_color": 0x3A7F6E, "water_fog_color": 0x0E2A22}, 0.8, True, []),
    "primeval_peaks": ("stony_peaks", {"sky_color": 0x9AA8C0, "fog_color": 0xB0B4B8}, 0.6, True, []),
    # Bernstein mit Insekten liegt im Gestein der Mammutsteppe (Schritt 6: Erze)
    "mammoth_steppe": ("plains", {"grass_color": 0xB0A868, "foliage_color": 0x9A9A55, "sky_color": 0x9EB8D8, "fog_color": 0xD8DCE0,
                                  "water_color": 0x3D5A8A, "water_fog_color": 0x0A1530}, 0.3, False, [(6, "timemod:amber_ore")]),
    "miocene_savanna": ("savanna", {"grass_color": 0xB3A95A, "foliage_color": 0x9C9A48, "sky_color": 0x9CBEDC, "fog_color": 0xE6D2A0,
                                    "water_color": 0x3E7A80, "water_fog_color": 0x0F2A2E}, 1.4, False, []),
    "miocene_woodland": ("sparse_jungle", {"grass_color": 0x6E9A3A, "foliage_color": 0x5A8A30, "sky_color": 0x9CBEDC, "fog_color": 0xD8D8B0,
                                           "water_color": 0x3E7A80, "water_fog_color": 0x0F2A2E}, 1.0, False, []),
    # Höhlen der Urzeit: ohne Sculk, Zombies und Axolotl
    "primeval_caves": ("dripstone_caves", {}, 0.8, False, []),
    "primeval_lush_caves": ("lush_caves", {}, 0.5, False, []),
}

STEPS = 11


def feature_order():
    """Pro Generierungsschritt eine gemeinsame Reihenfolge aller Vanilla-Features der Oberwelt.

    Minecraft bricht ab, wenn zwei Biome einer Dimension dieselben Features in widersprüchlicher Reihenfolge
    platzieren. Sortiert jedes Urzeit-Biom nach dieser Reihenfolge, kann das nicht passieren.
    """
    ranks = []
    for step in range(STEPS):
        edges, seen = {}, []
        for biome in OVERWORLD:
            features = vanilla(f"worldgen/biome/{biome}.json")["features"]
            row = features[step] if step < len(features) else []
            for feature in row:
                if feature not in edges:
                    edges[feature] = set()
                    seen.append(feature)
            for a, b in zip(row, row[1:]):
                edges[a].add(b)
        incoming = {f: 0 for f in seen}
        for targets in edges.values():
            for b in targets:
                incoming[b] += 1
        ready = [f for f in seen if incoming[f] == 0]
        order = []
        while ready:
            current = ready.pop(0)
            order.append(current)
            for b in sorted(edges[current], key=seen.index):
                incoming[b] -= 1
                if incoming[b] == 0:
                    ready.append(b)
        if len(order) != len(seen):
            raise SystemExit(f"Vanilla-Features in Schritt {step} sind nicht sortierbar")
        ranks.append({f: i for i, f in enumerate(order)})
    return ranks


def check_feature_cycles(lists):
    """Gegenprobe: Alle Biome zusammen dürfen keinen Reihenfolge-Zyklus enthalten."""
    for step in range(STEPS):
        edges = {}
        for features in lists:
            row = features[step] if step < len(features) else []
            for a, b in zip(row, row[1:]):
                edges.setdefault(a, set()).add(b)
        state = {}

        def visit(node):
            state[node] = 1
            for nxt in edges.get(node, ()):
                if state.get(nxt) == 1 or (nxt not in state and visit(nxt)):
                    return True
            state[node] = 2
            return False

        for node in list(edges):
            if node not in state and visit(node):
                raise SystemExit(f"Feature-Zyklus in Schritt {step} bei {node}")


def biomes():
    ranks = feature_order()
    written = []
    for name, (base, colors, temperature, strip, extra) in BIOMES.items():
        biome = copy.deepcopy(vanilla(f"worldgen/biome/{base}.json"))
        biome["temperature"] = temperature
        effects = biome["effects"]
        effects.update(colors)
        effects.pop("music", None)
        biome["spawners"] = copy.deepcopy(NO_SPAWNERS)
        biome["spawn_costs"] = {}
        features = biome["features"]
        while len(features) < STEPS:
            features.append([])
        for step, row in enumerate(features):
            row = [f for f in row if not BUILT.fullmatch(f) and not (strip and step == 9 and FLOWERS.fullmatch(f))]
            for wanted in extra:
                at, feature = wanted if isinstance(wanted, tuple) else (9, wanted)
                if at == step and feature not in row:
                    row.append(feature)
            features[step] = sorted(row, key=lambda f: (ranks[step].get(f, len(ranks[step])), f))
        written.append(features)
        write(DATA / f"worldgen/biome/{name}.json", biome)
    check_feature_cycles(written + [vanilla(f"worldgen/biome/{b}.json")["features"] for b in OVERWORLD])


# ------------------------------------------------------------------------------------------
# Biom-Umdeutung pro Epoche
# ------------------------------------------------------------------------------------------

OCEANS = ["ocean", "deep_ocean", "warm_ocean", "lukewarm_ocean", "deep_lukewarm_ocean", "cold_ocean", "deep_cold_ocean", "frozen_ocean",
          "deep_frozen_ocean"]
LAND = ["plains", "sunflower_plains", "snowy_plains", "ice_spikes", "desert", "swamp", "mangrove_swamp", "forest", "flower_forest",
        "birch_forest", "dark_forest", "old_growth_birch_forest", "old_growth_pine_taiga", "old_growth_spruce_taiga", "taiga",
        "snowy_taiga", "savanna", "savanna_plateau", "windswept_hills", "windswept_gravelly_hills", "windswept_forest",
        "windswept_savanna", "jungle", "sparse_jungle", "bamboo_jungle", "badlands", "eroded_badlands", "wooded_badlands", "meadow",
        "cherry_grove", "grove", "snowy_slopes", "frozen_peaks", "jagged_peaks", "stony_peaks", "mushroom_fields"]
SHORES = ["beach", "snowy_beach", "stony_shore"]
RIVERS = ["river", "frozen_river"]
CAVES = ["dripstone_caves", "lush_caves", "deep_dark"]
OVERWORLD = LAND + OCEANS + SHORES + RIVERS + CAVES


def m(**pairs):
    return {f"minecraft:{k}": (v if ":" in v else f"minecraft:{v}") for k, v in pairs.items()}


def deep(default_land, overrides, ocean="timemod:primeval_ocean", deep_ocean="timemod:primeval_deep_ocean"):
    remap = {}
    for biome in LAND:
        remap[f"minecraft:{biome}"] = default_land
    for biome in OCEANS:
        remap[f"minecraft:{biome}"] = deep_ocean if biome.startswith("deep_") else ocean
    for biome in SHORES:
        remap[f"minecraft:{biome}"] = "timemod:primeval_shore"
    for biome in RIVERS:
        remap[f"minecraft:{biome}"] = "timemod:primeval_river"
    remap["minecraft:dripstone_caves"] = "timemod:primeval_caves"
    remap["minecraft:deep_dark"] = "timemod:primeval_caves"
    remap["minecraft:lush_caves"] = "timemod:primeval_lush_caves"
    for biome, target in overrides.items():
        remap[f"minecraft:{biome}"] = target
    return remap


REMAPS = {
    # Ursprünglicher: Wälder statt Wiesen, alte Bäume
    "wild": m(plains="forest", meadow="forest", sunflower_plains="flower_forest", birch_forest="old_growth_birch_forest",
              taiga="old_growth_spruce_taiga"),
    "primeval": m(plains="forest", meadow="forest", sunflower_plains="flower_forest", birch_forest="old_growth_birch_forest",
                  taiga="old_growth_spruce_taiga", forest="dark_forest", windswept_forest="old_growth_pine_taiga"),
    # Höhepunkt der Eiszeit: Gletscher, Tundra, gefrorene Meere
    "ice": m(plains="snowy_plains", sunflower_plains="ice_spikes", meadow="snowy_plains", forest="snowy_taiga",
             flower_forest="snowy_taiga", birch_forest="snowy_taiga", dark_forest="snowy_taiga", old_growth_birch_forest="snowy_taiga",
             taiga="snowy_taiga", old_growth_pine_taiga="snowy_taiga", old_growth_spruce_taiga="snowy_taiga", swamp="snowy_plains",
             mangrove_swamp="snowy_plains", savanna="snowy_plains", savanna_plateau="snowy_slopes", windswept_savanna="snowy_slopes",
             desert="snowy_plains", badlands="snowy_slopes", eroded_badlands="snowy_slopes", wooded_badlands="snowy_taiga",
             jungle="taiga", sparse_jungle="taiga", bamboo_jungle="taiga", windswept_hills="snowy_slopes",
             windswept_gravelly_hills="snowy_slopes", windswept_forest="snowy_taiga", cherry_grove="grove", stony_peaks="frozen_peaks",
             jagged_peaks="frozen_peaks", river="frozen_river", beach="snowy_beach", ocean="frozen_ocean", cold_ocean="frozen_ocean",
             lukewarm_ocean="cold_ocean", warm_ocean="cold_ocean", deep_ocean="deep_frozen_ocean", deep_cold_ocean="deep_frozen_ocean",
             deep_lukewarm_ocean="deep_cold_ocean", mushroom_fields="snowy_plains"),
    # Pleistozän: Mammutsteppe, große Flüsse, kühl
    "steppe": {**m(plains="timemod:mammoth_steppe", sunflower_plains="timemod:mammoth_steppe", meadow="timemod:mammoth_steppe",
                   savanna="timemod:mammoth_steppe", savanna_plateau="timemod:mammoth_steppe", windswept_savanna="timemod:mammoth_steppe",
                   desert="timemod:mammoth_steppe", badlands="timemod:mammoth_steppe", eroded_badlands="timemod:mammoth_steppe",
                   wooded_badlands="taiga", swamp="timemod:mammoth_steppe", mangrove_swamp="timemod:mammoth_steppe",
                   forest="taiga", flower_forest="taiga", birch_forest="taiga", dark_forest="old_growth_spruce_taiga",
                   old_growth_birch_forest="taiga", jungle="old_growth_spruce_taiga", sparse_jungle="taiga", bamboo_jungle="taiga",
                   cherry_grove="grove", mushroom_fields="timemod:mammoth_steppe", warm_ocean="cold_ocean", lukewarm_ocean="cold_ocean",
                   deep_lukewarm_ocean="deep_cold_ocean")},
    # Miozän: Savannen, warme Urmeere
    "miocene": deep("timemod:miocene_savanna", {"forest": "timemod:miocene_woodland", "dark_forest": "timemod:miocene_woodland",
                                                "jungle": "timemod:miocene_woodland", "bamboo_jungle": "timemod:miocene_woodland",
                                                "taiga": "timemod:miocene_woodland", "stony_peaks": "timemod:primeval_peaks",
                                                "jagged_peaks": "timemod:primeval_peaks", "frozen_peaks": "timemod:primeval_peaks"}),
    # Kreide: Farnwälder, Sümpfe, Palmfarn-Savannen, Vulkane
    "cretaceous": deep("timemod:fern_jungle", {"swamp": "timemod:primeval_swamp", "mangrove_swamp": "timemod:primeval_swamp",
                                               "savanna": "timemod:cycad_savanna", "savanna_plateau": "timemod:cycad_savanna",
                                               "plains": "timemod:cycad_savanna", "desert": "timemod:cycad_savanna",
                                               "taiga": "timemod:conifer_forest", "snowy_taiga": "timemod:conifer_forest",
                                               "old_growth_spruce_taiga": "timemod:conifer_forest", "badlands": "timemod:volcanic_wastes",
                                               "eroded_badlands": "timemod:volcanic_wastes", "stony_peaks": "timemod:primeval_peaks",
                                               "jagged_peaks": "timemod:primeval_peaks", "frozen_peaks": "timemod:volcanic_wastes"}),
    # Jura: Farnwälder und Riesen-Nadelbäume, Lagunen
    "jurassic": deep("timemod:conifer_forest", {"jungle": "timemod:fern_jungle", "bamboo_jungle": "timemod:fern_jungle",
                                                "sparse_jungle": "timemod:fern_jungle", "forest": "timemod:fern_jungle",
                                                "swamp": "timemod:primeval_swamp", "mangrove_swamp": "timemod:primeval_swamp",
                                                "savanna": "timemod:cycad_savanna", "desert": "timemod:cycad_savanna",
                                                "stony_peaks": "timemod:primeval_peaks", "jagged_peaks": "timemod:primeval_peaks",
                                                "badlands": "timemod:volcanic_wastes"}),
    # Trias/Jura-Grenze: Vulkanismus und Trockenheit
    "triassic": deep("timemod:cycad_savanna", {"jungle": "timemod:fern_jungle", "swamp": "timemod:primeval_swamp",
                                               "badlands": "timemod:volcanic_wastes", "eroded_badlands": "timemod:volcanic_wastes",
                                               "windswept_hills": "timemod:volcanic_wastes", "stony_peaks": "timemod:volcanic_wastes",
                                               "jagged_peaks": "timemod:primeval_peaks", "frozen_peaks": "timemod:primeval_peaks",
                                               "taiga": "timemod:conifer_forest", "desert": "timemod:pangaea_desert"}),
    # Pangäa: riesige Wüste im Inneren, Monsun an den Küsten, Vulkanfelder
    "pangaea": deep("timemod:pangaea_desert", {"jungle": "timemod:fern_jungle", "swamp": "timemod:primeval_swamp",
                                               "mangrove_swamp": "timemod:primeval_swamp", "forest": "timemod:cycad_savanna",
                                               "dark_forest": "timemod:fern_jungle", "badlands": "timemod:volcanic_wastes",
                                               "eroded_badlands": "timemod:volcanic_wastes", "windswept_hills": "timemod:volcanic_wastes",
                                               "stony_peaks": "timemod:primeval_peaks", "taiga": "timemod:conifer_forest"}),
}


# ------------------------------------------------------------------------------------------
# Dimensionen
# ------------------------------------------------------------------------------------------

def dimension_types():
    base = vanilla("dimension_type/overworld.json")
    for name, effects in (("era_future", "minecraft:overworld"), ("era_past", "minecraft:overworld"), ("era_ice", f"{NS}:ice_age"),
                          ("era_steppe", f"{NS}:steppe"), ("era_deep", f"{NS}:deep_time")):
        dim = copy.deepcopy(base)
        dim["effects"] = effects
        dim["has_raids"] = name in ("era_future", "era_past")
        dim["natural"] = True
        write(DATA / f"dimension_type/{name}.json", dim)


def dimensions():
    for years, dim_type, settings, remap in ERAS:
        source = {"type": "minecraft:multi_noise", "preset": "minecraft:overworld"}
        if remap:
            source = {"type": f"{NS}:remapped", "base": source, "remap": REMAPS[remap]}
        write(DATA / f"dimension/year_{era_id(years)}.json", {
            "type": f"{NS}:{dim_type}",
            "generator": {"type": "minecraft:noise", "settings": settings, "biome_source": source},
        })


# ------------------------------------------------------------------------------------------
# Namen
# ------------------------------------------------------------------------------------------

# Jahre: (deutscher Name, englischer Name)
TITLES = {
    1000: ("Ein Jahrtausend später", "A Millennium Later"),
    500: ("Ein halbes Jahrtausend später", "Half a Millennium Later"),
    250: ("Ferne Zukunft", "Distant Future"),
    100: ("Ein Jahrhundert später", "A Century Later"),
    50: ("Ein halbes Jahrhundert später", "Half a Century Later"),
    25: ("Eine Generation später", "A Generation Later"),
    10: ("Ein Jahrzehnt später", "A Decade Later"),
    5: ("Nahe Zukunft", "Near Future"),
    4: ("Nahe Zukunft", "Near Future"),
    3: ("Nahe Zukunft", "Near Future"),
    2: ("Nahe Zukunft", "Near Future"),
    1: ("Nächstes Jahr", "Next Year"),
    0: ("Gegenwart", "Present"),
    -1: ("Letztes Jahr", "Last Year"),
    -2: ("Jüngste Vergangenheit", "Recent Past"),
    -3: ("Jüngste Vergangenheit", "Recent Past"),
    -4: ("Jüngste Vergangenheit", "Recent Past"),
    -5: ("Jüngste Vergangenheit", "Recent Past"),
    -10: ("Vor einem Jahrzehnt", "A Decade Ago"),
    -50: ("Vor einem halben Jahrhundert", "Half a Century Ago"),
    -100: ("Zeit der Pioniere", "Age of Pioneers"),
    -500: ("Spätmittelalter", "Late Middle Ages"),
    -1000: ("Frühmittelalter", "Early Middle Ages"),
    -5000: ("Jungsteinzeit", "Neolithic"),
    -20000: ("Höhepunkt der Eiszeit", "Last Glacial Maximum"),
    -100000: ("Letzte Kaltzeit", "Last Glacial Period"),
    -1000000: ("Pleistozän", "Pleistocene"),
    -10000000: ("Miozän", "Miocene"),
    -66000000: ("Ende der Kreidezeit", "End of the Cretaceous"),
    -70000000: ("Späte Kreidezeit", "Late Cretaceous"),
    -100000000: ("Kreidezeit", "Cretaceous"),
    -150000000: ("Jura", "Jurassic"),
    -200000000: ("Trias-Jura-Grenze", "Triassic–Jurassic Boundary"),
    -245000000: ("Trias · Pangäa", "Triassic · Pangaea"),
}


def span_text(years, separator, singular, plural):
    """Die Zeitspanne ohne Vorzeichen, etwa „1.000 Jahre“ (für „… später“, „… alt“)."""
    digits = f"{abs(years):,}".replace(",", separator)
    return f"{digits} {singular if abs(years) == 1 else plural}"


def year_text(years, separator, singular, plural):
    if years == 0:
        return None
    sign = "+" if years > 0 else "−"
    return sign + span_text(years, separator, singular, plural)


def lang():
    for code, idx, separator, singular, plural, today in (("de_de", 0, ".", "Jahr", "Jahre", "Heute"),
                                                          ("en_us", 1, ",", "year", "years", "Today")):
        path = ROOT / f"src/main/resources/assets/{NS}/lang/{code}.json"
        data = json.loads(path.read_text(encoding="utf-8")) if path.exists() else {}
        for years, names in TITLES.items():
            key = "present" if years == 0 else era_id(years)
            data[f"era.{NS}.{key}"] = names[idx]
            data[f"era.{NS}.{key}.year"] = year_text(years, separator, singular, plural) or today
            if years != 0:
                data[f"era.{NS}.{key}.span"] = span_text(years, separator, singular, plural)
        write(path, dict(sorted(data.items())))


def main():
    noise_settings()
    biomes()
    dimension_types()
    dimensions()
    if sorted(TITLES) != sorted([0] + [years for years, *_ in ERAS]):
        raise SystemExit("Namen und Epochen passen nicht zusammen")
    lang()
    print(f"{len(ERAS)} Epochen, {len(BIOMES)} Urzeit-Biome erzeugt")


if __name__ == "__main__":
    main()
