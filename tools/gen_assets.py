"""Erzeugt alle JSON-Dateien von TIME: Blockzustände, Modelle, Beutetabellen, Rezepte, Tags,
Weltgenerierung und Übersetzungen. Eine Quelle der Wahrheit statt hunderter Hand-Dateien.

Aufruf: python3 -I tools/gen_assets.py
"""
import json
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
from chronik_text import ADVANCEMENTS, CHAPTERS  # noqa: E402

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
    # Stufe III
    ("clockwork_core", "generated", "Uhrwerkskern", "Clockwork Core",
     "Das tickende Herz des Uhrmachers. Es schlägt noch.", "The ticking heart of the Clockmaker. It still beats."),
    ("phage_heart", "generated", "Phagenherz", "Phage Heart",
     "Hat unzählige Sekunden gefressen – und gibt sie nur widerwillig her.", "It devoured countless seconds – and gives them back only grudgingly."),
    ("chronik", "generated", "Die Chronik", "The Chronicle", "Das Handbuch der Zeit. Rechtsklick zum Lesen.", "The handbook of time. Right-click to read."),
    ("clockmaker_spawn_egg", "spawn_egg", "Uhrmacher-Spawn-Ei", "Clockmaker Spawn Egg", None, None),
    ("chronophage_spawn_egg", "spawn_egg", "Chronophagen-Spawn-Ei", "Chronophage Spawn Egg", None, None),
    ("time_core_fragment", "generated", "Zeitkern-Fragment", "Time Core Fragment",
     "Ein Splitter aus dem Herzen der Zeit. Vier davon wecken etwas in den Chrono-Ruinen.", "A shard from the heart of time. Four of them wake something in the Chrono Ruins."),
    ("chronite_plate", "generated", "Chronit-Platte", "Chronite Plate",
     "Gepresstes Chronit mit Netherit-Kern – Rohstoff der Chrono-Rüstung.", "Pressed chronite around a netherite core – base of Chrono armor."),
    ("chrono_upgrade_template", "generated", "Schmiedevorlage", "Smithing Template", None, None),
    ("chrono_helmet", "generated", "Chrono-Helm", "Chrono Helmet", None, None),
    ("chrono_chestplate", "generated", "Chrono-Brustpanzer", "Chrono Chestplate", None, None),
    ("chrono_leggings", "generated", "Chrono-Beinschutz", "Chrono Leggings", None, None),
    ("chrono_boots", "generated", "Chrono-Stiefel", "Chrono Boots", None, None),
    ("paradox_time_cell", "generated", "Paradox-Zeitzelle", "Paradox Time Cell", None, None),
    ("blank_module", "generated", "Leeres Modul", "Blank Module",
     "Grundplatine für Filter und Verstärker.", "Base board for filters and amplifiers."),
    ("filter_animals", "generated", "Filtermodul: Tiere", "Filter Module: Animals",
     "Das Werkzeug wirkt nur auf Tiere.", "The tool only affects animals."),
    ("filter_monsters", "generated", "Filtermodul: Monster", "Filter Module: Monsters",
     "Das Werkzeug wirkt nur auf Monster.", "The tool only affects monsters."),
    ("filter_villagers", "generated", "Filtermodul: Dorfbewohner", "Filter Module: Villagers",
     "Das Werkzeug wirkt nur auf Dorfbewohner und Händler.", "The tool only affects villagers and traders."),
    ("filter_players", "generated", "Filtermodul: Spieler", "Filter Module: Players",
     "Das Werkzeug wirkt auf andere Spieler (nie auf dich).", "The tool affects other players (never you)."),
    ("filter_projectiles", "generated", "Filtermodul: Geschosse", "Filter Module: Projectiles",
     "Pfeile, Feuerbälle, TNT und fallende Blöcke.", "Arrows, fireballs, TNT and falling blocks."),
    ("filter_items", "generated", "Filtermodul: Gegenstände", "Filter Module: Items",
     "Herumliegende Gegenstände und Erfahrungskugeln.", "Dropped items and experience orbs."),
    ("filter_vehicles", "generated", "Filtermodul: Fahrzeuge", "Filter Module: Vehicles",
     "Loren und Boote.", "Minecarts and boats."),
    ("filter_growth", "generated", "Filtermodul: Wachstum", "Filter Module: Growth",
     "Pflanzen, Bäume, Gras, Amethyst, Eier.", "Plants, trees, grass, amethyst, eggs."),
    ("filter_environment", "generated", "Filtermodul: Umwelt", "Filter Module: Environment",
     "Schnee, Eis, Feuer, Laub und Verwitterung.", "Snow, ice, fire, leaves and weathering."),
    ("filter_fluids", "generated", "Filtermodul: Flüssigkeiten", "Filter Module: Fluids",
     "Fließendes Wasser und Lava.", "Flowing water and lava."),
    ("filter_redstone", "generated", "Filtermodul: Redstone", "Filter Module: Redstone",
     "Redstone, Kolben, geplante Block-Ticks – und gebaute Blöcke beim Zurückspulen.", "Redstone, pistons, scheduled block ticks – and built blocks when rewinding."),
    ("filter_machines", "generated", "Filtermodul: Maschinen", "Filter Module: Machines",
     "Öfen, Trichter, Spawner und Maschinen anderer Mods.", "Furnaces, hoppers, spawners and machines from other mods."),
    ("radius_module", "generated", "Verstärker: Radius", "Amplifier: Radius",
     "+50 % Radius (kostet nach Fläche mehr).", "+50% radius (costs more by area)."),
    ("duration_module", "generated", "Verstärker: Dauer", "Amplifier: Duration",
     "+50 % Dauer – bei der Spule: weiter zurück.", "+50% duration – for the spool: further back."),
    ("strength_module", "generated", "Verstärker: Stärke", "Amplifier: Strength",
     "Schaltet stärkere Faktoren frei (bis 10× bzw. 0,05×) – bei der Spule: schneller.", "Unlocks stronger factors (up to 10× or 0.05×) – for the spool: faster."),
    ("efficiency_module", "generated", "Verstärker: Effizienz", "Amplifier: Efficiency",
     "−25 % Chronon-Verbrauch.", "−25% Chronon cost."),
    # Stufe IV
    ("heart_of_time", "generated", "Herz der Zeit", "Heart of Time",
     "Schlägt im Takt aller Uhren der Welt.", "It beats in time with every clock in the world."),
    ("chronos_watch", "generated", "Chronos-Uhr", "Chronos Watch", None, None),
    ("paradox_spawn_egg", "spawn_egg", "Paradox-Spawn-Ei", "Paradox Spawn Egg", None, None),
    ("time_loop", "generated", "Zeitschleife", "Time Loop",
     "Stirbst du, spult die Welt um dich 10 Sekunden zurück – mit dir.", "If you die, the world around you rewinds 10 seconds – with you."),
    ("infinity_time_cell", "generated", "Unendlichkeits-Zeitzelle", "Infinity Time Cell", None, None),
    # Stufe V: die Zeitmaschine
    ("fossil_fragment", "generated", "Fossil-Fragment", "Fossil Fragment",
     "Mit dem Pinsel aus verdächtigem Sand und Kies freigelegt – ein Gruß aus der Tiefe der Zeit.",
     "Brushed out of suspicious sand and gravel – a greeting from the depths of time."),
    ("era_focus", "generated", "Epochen-Fokus", "Era Focus",
     "In der Chronos-Konsole öffnet er Steinzeit, Eiszeit und Erdneuzeit.", "In the Chronos Console it opens the Stone Age, the Ice Age and the Cenozoic."),
    ("amber", "generated", "Bernstein", "Amber",
     "Versteinertes Harz. Er brennt im Ofen – nicht umsonst heißt er „Brennstein“.", "Fossilized resin. It burns in a furnace."),
    ("insect_amber", "generated", "Bernstein mit Insekt", "Amber with Insect",
     "Seit einer Million Jahren gefangen. Vielleicht steckt noch Blut in ihr …", "Trapped for a million years. Maybe there is still blood inside …"),
    ("dna_sample", "generated", "DNS-Probe", "DNA Sample",
     "Urzeitliches Erbgut aus dem Bauch einer Mücke.", "Prehistoric genes from a mosquito's belly."),
    ("time_seed", "generated", "Zeitsaat", "Time Seed",
     "Auf einen Setzling in der Vergangenheit: In der Gegenwart steht dort ein Baum, so alt wie die Reise weit war.",
     "On a sapling in the past: in the present a tree stands there, as old as the journey was far."),
    ("rusty_iron", "generated", "Rostklumpen", "Rusty Iron",
     "Eisen nach Jahrhunderten. Im Schmelzofen bleibt etwas davon übrig.", "Iron after centuries. A blast furnace saves a little of it."),
    ("mesozoic_focus", "generated", "Fokus: Mesozoikum", "Focus: Mesozoic",
     "Richtet die Chronos-Konsole auf das Zeitalter der Dinosaurier aus.", "Aims the Chronos Console at the age of the dinosaurs."),
]

# Chronos-Uhr: Modi, Meisterschaft, Radialmenü und HUD
LANG_CHRONOS = {
    "de_de": {
        "chronos.timemod.mode.stop": "Zeitstopp",
        "chronos.timemod.mode.fast": "Zeitraffer",
        "chronos.timemod.mode.slow": "Zeitlupe",
        "chronos.timemod.mode.rewind": "Zurückspulen",
        "chronos.timemod.level.0": "Lehrling",
        "chronos.timemod.level.1": "Geselle",
        "chronos.timemod.level.2": "Uhrmacher",
        "chronos.timemod.level.3": "Zeitwanderer",
        "chronos.timemod.level.4": "Zeitmeister",
        "chronos.timemod.level.5": "Herr der Zeit",
        "tooltip.timemod.chronos.cost": "Kosten: %s Chronon",
        "tooltip.timemod.chronos.mastery": "Meisterschaft: %s (%s %%)",
        "tooltip.timemod.chronos.hint": "R: Radialmenü · Schleichen + Mausrad: Wert · Schleichen + Benutzen: Zonen aufheben",
        "tooltip.timemod.chronos.global": "überall",
        "tooltip.timemod.chronos.radius": "Radius %s",
        "tooltip.timemod.chronos.stop": "%s · %s · %s s",
        "tooltip.timemod.chronos.zone": "%s %s · %s · %s s",
        "tooltip.timemod.chronos.rewind": "%s · %s · bis %s s zurück",
        "message.timemod.chronos.mastery": "Chronos-Uhr: Meisterschaft „%s“ – neue Grenzen freigeschaltet!",
        "message.timemod.chronos.cancelled": "Deine Zeitzonen sind aufgehoben.",
        "key.categories.timemod": "TIME",
        "key.timemod.chronos_menu": "Chronos-Radialmenü",
        "chronos.timemod.menu.value.stop": "Dauer",
        "chronos.timemod.menu.value.fast": "Faktor",
        "chronos.timemod.menu.value.slow": "Faktor",
        "chronos.timemod.menu.value.rewind": "Tiefe",
        "chronos.timemod.menu.radius": "Radius",
        "chronos.timemod.menu.duration": "Zonendauer",
        "chronos.timemod.menu.global": "Global",
        "chronos.timemod.menu.global_locked": "Global ab „Zeitwanderer“",
        "chronos.timemod.menu.self": "Selbst mitspulen",
        "chronos.timemod.menu.filters": "Filter",
        "chronos.timemod.menu.default": "Standard",
        "chronos.timemod.menu.full": "Spult alles zurück, samt Inventaren",
        "chronos.timemod.menu.partial": "Bewegt und heilt nur – Tote bleiben tot",
        "chronos.timemod.menu.global_only": "nur global",
        "chronos.timemod.menu.hint": "Klicken: wählen · Mausrad: Wert · Umschalt + Mausrad: Radius",
        "chronos.timemod.hud.cost": "%s Chronon",
        "chronos.timemod.hud.now": "jetzt",
        "chronos.timemod.hud.memory": "Erinnerung: %s s",
        "chronos.timemod.hud.no_memory": "Die Welt zeichnet nicht auf",
        "chronos.timemod.hud.scrubbing": "Spule zurück … loslassen zum Anhalten",
        "chronos.timemod.hud.rewinding": "Spule zurück …",
        "chronos.timemod.hud.cooldown": "lädt nach",
        "entity.timemod.paradox": "Paradox",
        "entity.timemod.paradox.echo_of": "Paradox – Echo von %s",
        "entity.timemod.paradox_echo": "Echo",
        "boss.timemod.paradox.awakens": "Paradox erwacht",
        "boss.timemod.paradox.awakens.hint": "Es kennt jede deiner Bewegungen – denn es sind deine",
        "boss.timemod.paradox.later": "„Ich bin du – nur später.“",
        "boss.timemod.paradox.defeated": "„Dann … bist du jetzt … ich.“",
        "message.timemod.altar.paradox": "Ein Spiegel ohne Spiegelbild. Er hungert nach einem Paradox …",
        "message.timemod.altar.paradox.charge": "Der Spiegel verlangt eine Paradox-Zeitzelle mit mindestens %s Chronon.",
        "message.timemod.paradox.mirror": "Paradox spiegelt deine Zeit!",
        "subtitles.timemod.paradox_voice": "Paradox spricht",
        "subtitles.timemod.paradox_blink": "Paradox springt durch die Zeit",
        "subtitles.timemod.echo_shatter": "Ein Echo zerspringt",
        "tooltip.timemod.loop.cost": "Rettet dich für %s Chronon",
        "tooltip.timemod.loop.ready": "Bereit",
        "tooltip.timemod.loop.cooldown": "Lädt nach: noch %s min",
        "message.timemod.loop.title": "Nicht heute.",
        "message.timemod.loop.subtitle": "Die Zeitschleife verweigert deinen Tod",
    },
    "en_us": {
        "chronos.timemod.mode.stop": "Time stop",
        "chronos.timemod.mode.fast": "Fast forward",
        "chronos.timemod.mode.slow": "Slow motion",
        "chronos.timemod.mode.rewind": "Rewind",
        "chronos.timemod.level.0": "Apprentice",
        "chronos.timemod.level.1": "Journeyman",
        "chronos.timemod.level.2": "Clockmaker",
        "chronos.timemod.level.3": "Time Walker",
        "chronos.timemod.level.4": "Time Master",
        "chronos.timemod.level.5": "Lord of Time",
        "tooltip.timemod.chronos.cost": "Cost: %s chronon",
        "tooltip.timemod.chronos.mastery": "Mastery: %s (%s%%)",
        "tooltip.timemod.chronos.hint": "R: radial menu · Sneak + wheel: value · Sneak + use: cancel zones",
        "tooltip.timemod.chronos.global": "everywhere",
        "tooltip.timemod.chronos.radius": "radius %s",
        "tooltip.timemod.chronos.stop": "%s · %s · %s s",
        "tooltip.timemod.chronos.zone": "%s %s · %s · %s s",
        "tooltip.timemod.chronos.rewind": "%s · %s · up to %s s back",
        "message.timemod.chronos.mastery": "Chronos Watch: mastery \"%s\" – new limits unlocked!",
        "message.timemod.chronos.cancelled": "Your time zones have been lifted.",
        "key.categories.timemod": "TIME",
        "key.timemod.chronos_menu": "Chronos radial menu",
        "chronos.timemod.menu.value.stop": "Duration",
        "chronos.timemod.menu.value.fast": "Factor",
        "chronos.timemod.menu.value.slow": "Factor",
        "chronos.timemod.menu.value.rewind": "Depth",
        "chronos.timemod.menu.radius": "Radius",
        "chronos.timemod.menu.duration": "Zone duration",
        "chronos.timemod.menu.global": "Global",
        "chronos.timemod.menu.global_locked": "Global from \"Time Walker\"",
        "chronos.timemod.menu.self": "Rewind yourself",
        "chronos.timemod.menu.filters": "Filters",
        "chronos.timemod.menu.default": "Default",
        "chronos.timemod.menu.full": "Rewinds everything, inventories included",
        "chronos.timemod.menu.partial": "Only moves and heals – the dead stay dead",
        "chronos.timemod.menu.global_only": "global only",
        "chronos.timemod.menu.hint": "Click: select · Wheel: value · Shift + wheel: radius",
        "chronos.timemod.hud.cost": "%s chronon",
        "chronos.timemod.hud.now": "now",
        "chronos.timemod.hud.memory": "Memory: %s s",
        "chronos.timemod.hud.no_memory": "The world is not recording",
        "chronos.timemod.hud.scrubbing": "Rewinding … release to stop",
        "chronos.timemod.hud.rewinding": "Rewinding …",
        "chronos.timemod.hud.cooldown": "recharging",
        "entity.timemod.paradox": "Paradox",
        "entity.timemod.paradox.echo_of": "Paradox – Echo of %s",
        "entity.timemod.paradox_echo": "Echo",
        "boss.timemod.paradox.awakens": "Paradox awakens",
        "boss.timemod.paradox.awakens.hint": "It knows every move you make – because they are yours",
        "boss.timemod.paradox.later": "\"I am you – just later.\"",
        "boss.timemod.paradox.defeated": "\"Then … you are … me now.\"",
        "message.timemod.altar.paradox": "A mirror without a reflection. It hungers for a paradox …",
        "message.timemod.altar.paradox.charge": "The mirror demands a Paradox time cell holding at least %s chronon.",
        "message.timemod.paradox.mirror": "Paradox mirrors your time!",
        "subtitles.timemod.paradox_voice": "Paradox speaks",
        "subtitles.timemod.paradox_blink": "Paradox blinks through time",
        "subtitles.timemod.echo_shatter": "An echo shatters",
        "tooltip.timemod.loop.cost": "Saves you for %s chronon",
        "tooltip.timemod.loop.ready": "Ready",
        "tooltip.timemod.loop.cooldown": "Recharging: %s min left",
        "message.timemod.loop.title": "Not today.",
        "message.timemod.loop.subtitle": "The time loop refuses your death",
    },
}

BLOCKS = {
    "brass_block": ("Messingblock", "Block of Brass"),
    "clockmaker_bench": ("Uhrmacher-Werkbank", "Clockmaker's Bench"),
    "time_sand": ("Zeitsand", "Time Sand"),
    "time_glass": ("Zeitglas", "Time Glass"),
    "deepslate_chronite_ore": ("Tiefenschiefer-Chronit-Erz", "Deepslate Chronite Ore"),
    "chronite_sprout": ("Chronit-Keim", "Chronite Sprout"),
    "time_condenser": ("Zeitkondensator", "Time Condenser"),
    "time_field_projector": ("Zeitfeld-Projektor", "Time Field Projector"),
    "temporal_workbench": ("Temporal-Werkbank", "Temporal Workbench"),
    "chrono_pillar": ("Chrono-Säule", "Chrono Pillar"),
    "lifetime_extractor": ("Lebenszeit-Extraktor", "Lifetime Extractor"),
    "clockmaker_altar": ("Uhrwerk-Altar", "Clockwork Altar"),
    "phage_altar": ("Phagen-Altar", "Phage Altar"),
    "paradox_altar": ("Spiegel-Altar", "Mirror Altar"),
    "chronos_gate_frame": ("Chronos-Torrahmen", "Chronos Gate Frame"),
    "chronos_console": ("Chronos-Konsole", "Chronos Console"),
    "chronos_portal": ("Zeitportal", "Time Portal"),
    "home_anchor": ("Heimkehr-Anker", "Homeward Anchor"),
    "time_capsule": ("Zeitkapsel", "Time Capsule"),
    "amber_ore": ("Bernsteinerz", "Amber Ore"),
    "amber_block": ("Bernsteinblock", "Block of Amber"),
}

# Chronos-Tor: Konsole, Meldungen, Fokus
LANG_GATE = {
    "de_de": {
        "container.timemod.chronos_console": "Chronos-Konsole",
        "gui.timemod.console.cost": "Kosten: %s Chronon",
        "gui.timemod.console.charge": "Aufladen",
        "gui.timemod.console.charging": "Lädt … %s %%",
        "gui.timemod.console.open": "Offen %s",
        "gui.timemod.console.status.charging": "Das Tor lädt sich auf …",
        "gui.timemod.console.status.open": "Das Tor ist offen – geh hindurch!",
        "gui.timemod.console.status.no_time": "Hier gibt es keine Zeit, in die man reisen könnte",
        "gui.timemod.console.status.missing": "Kein Tor in der Nähe (7×7-Ring)",
        "gui.timemod.console.status.blocked": "Die Öffnung des Tors ist versperrt",
        "gui.timemod.console.status.focus": "Benötigt: %s",
        "gui.timemod.console.status.ready": "Tor bereit",
        "gui.timemod.console.jump_back": "Ein Zeitalter zurück (Umschalt + ←)",
        "gui.timemod.console.back": "Eine Stufe zurück (←)",
        "gui.timemod.console.forward": "Eine Stufe vor (→)",
        "gui.timemod.console.jump_forward": "Ein Zeitalter vor (Umschalt + →)",
        "gui.timemod.console.buffer": "Chronon-Speicher: %s / %s",
        "message.timemod.gate.busy": "Das Tor arbeitet bereits",
        "message.timemod.gate.no_time": "Nether und End haben keine Geschichte, in die man reisen könnte",
        "message.timemod.gate.missing": "Kein vollständiger Torring in der Nähe",
        "message.timemod.gate.blocked": "Etwas versperrt die Öffnung des Tors",
        "message.timemod.gate.focus": "Für dieses Zeitalter braucht die Konsole den passenden Fokus",
        "message.timemod.gate.chronon": "Es fehlen noch %s Chronon",
        "tooltip.timemod.focus.unlocks": "Öffnet: %s",
        "era.timemod.age.mesozoic": "Erdmittelalter (Dinosaurier)",
        "era.timemod.age.cenozoic": "Erdneuzeit",
        "era.timemod.age.ice_age": "Eiszeit",
        "era.timemod.age.stone_age": "Steinzeit",
        "era.timemod.age.history": "Geschichte",
        "era.timemod.age.present": "Gegenwart",
        "era.timemod.age.future": "Zukunft",
        "subtitles.timemod.gate_charge": "Zeittor lädt sich auf",
        "subtitles.timemod.gate_open": "Zeittor öffnet sich",
        "subtitles.timemod.gate_close": "Zeittor schließt sich",
        "subtitles.timemod.gate_arrive": "Ankunft aus einer anderen Zeit",
        "subtitles.timemod.console_click": "Konsole klickt",
        "subtitles.timemod.time_stream": "Zeitstrom rauscht",
        "stream.timemod.title": "Zeitstrom",
        "stream.timemod.year": "Jahr",
        "message.timemod.anchor.here": "Dieser Anker führt hierher – du bist schon da",
        "message.timemod.anchor.wait": "Der Zeitstrom ist noch unruhig – warte einen Moment",
        "block.timemod.home_anchor.desc": "Führt zurück vor das Tor, durch das du kamst. Selbst gebaut: in die Gegenwart.",
        "commands.timemod.travel.done": "Reise ins Jahr %s",
        "commands.timemod.travel.unknown": "Kein Jahr der Zeitmaschine: %s",
        "container.timemod.time_capsule": "Zeitkapsel",
        "container.timemod.time_capsule.aged": "Zeitkapsel · %s alt",
        "container.timemod.time_capsule.hint": "Schleichen + Benutzen versiegelt sie",
        "container.timemod.time_capsule.hint_here": "Versiegeln lässt sie sich nur in der Vergangenheit",
        "container.timemod.time_capsule.sender": "Versiegelt von %s",
        "container.timemod.time_capsule.years": "%s Jahre",
        "block.timemod.time_capsule.desc": "Befülle sie in der Vergangenheit und versiegle sie (Schleichen + Benutzen) – in der Gegenwart wartet sie auf dich.",
        "message.timemod.capsule.sealed": "Versiegelt – ihr Inhalt reist durch die Jahre",
        "message.timemod.capsule.not_past": "Nur in der Vergangenheit kann eine Kapsel die Jahre bis heute überdauern",
        "message.timemod.capsule.empty": "Die Kapsel ist leer",
        "message.timemod.capsule.sent": "Versiegelt! In der Gegenwart liegt sie nun an dieser Stelle",
        "message.timemod.seed.not_past": "Zeitsaat wirkt nur in der Vergangenheit",
        "message.timemod.seed.already": "Dieser Setzling trägt schon Zeitsaat",
        "message.timemod.seed.planted": "Die Zeitsaat keimt – %s später steht hier ein mächtiger Baum",
    },
    "en_us": {
        "container.timemod.chronos_console": "Chronos Console",
        "gui.timemod.console.cost": "Cost: %s Chronon",
        "gui.timemod.console.charge": "Charge",
        "gui.timemod.console.charging": "Charging … %s%%",
        "gui.timemod.console.open": "Open %s",
        "gui.timemod.console.status.charging": "The gate is charging …",
        "gui.timemod.console.status.open": "The gate is open – step through!",
        "gui.timemod.console.status.no_time": "There is no time to travel to here",
        "gui.timemod.console.status.missing": "No gate nearby (7×7 ring)",
        "gui.timemod.console.status.blocked": "The gate's opening is blocked",
        "gui.timemod.console.status.focus": "Requires: %s",
        "gui.timemod.console.status.ready": "Gate ready",
        "gui.timemod.console.jump_back": "One age back (Shift + ←)",
        "gui.timemod.console.back": "One step back (←)",
        "gui.timemod.console.forward": "One step forward (→)",
        "gui.timemod.console.jump_forward": "One age forward (Shift + →)",
        "gui.timemod.console.buffer": "Chronon storage: %s / %s",
        "message.timemod.gate.busy": "The gate is already working",
        "message.timemod.gate.no_time": "The Nether and the End have no history to travel to",
        "message.timemod.gate.missing": "No complete gate ring nearby",
        "message.timemod.gate.blocked": "Something blocks the gate's opening",
        "message.timemod.gate.focus": "This age needs the right focus in the console",
        "message.timemod.gate.chronon": "%s Chronon still missing",
        "tooltip.timemod.focus.unlocks": "Opens: %s",
        "era.timemod.age.mesozoic": "Mesozoic (Dinosaurs)",
        "era.timemod.age.cenozoic": "Cenozoic",
        "era.timemod.age.ice_age": "Ice Age",
        "era.timemod.age.stone_age": "Stone Age",
        "era.timemod.age.history": "History",
        "era.timemod.age.present": "Present",
        "era.timemod.age.future": "Future",
        "subtitles.timemod.gate_charge": "Time gate charges",
        "subtitles.timemod.gate_open": "Time gate opens",
        "subtitles.timemod.gate_close": "Time gate closes",
        "subtitles.timemod.gate_arrive": "Arrival from another time",
        "subtitles.timemod.console_click": "Console clicks",
        "subtitles.timemod.time_stream": "Time stream rushes",
        "stream.timemod.title": "Time Stream",
        "stream.timemod.year": "Year",
        "message.timemod.anchor.here": "This anchor leads here – you are already there",
        "message.timemod.anchor.wait": "The time stream is still restless – wait a moment",
        "block.timemod.home_anchor.desc": "Leads back to the gate you came through. Self-built: to the present.",
        "commands.timemod.travel.done": "Journey to the year %s",
        "commands.timemod.travel.unknown": "Not a year of the time machine: %s",
        "container.timemod.time_capsule": "Time Capsule",
        "container.timemod.time_capsule.aged": "Time capsule · %s old",
        "container.timemod.time_capsule.hint": "Sneak + use to seal it",
        "container.timemod.time_capsule.hint_here": "It can only be sealed in the past",
        "container.timemod.time_capsule.sender": "Sealed by %s",
        "container.timemod.time_capsule.years": "%s years",
        "block.timemod.time_capsule.desc": "Fill it in the past and seal it (sneak + use) – it waits for you in the present.",
        "message.timemod.capsule.sealed": "Sealed – its contents travel through the years",
        "message.timemod.capsule.not_past": "Only in the past can a capsule outlast the years until today",
        "message.timemod.capsule.empty": "The capsule is empty",
        "message.timemod.capsule.sent": "Sealed! In the present it now lies at this very spot",
        "message.timemod.seed.not_past": "Time seeds only work in the past",
        "message.timemod.seed.already": "This sapling already carries a time seed",
        "message.timemod.seed.planted": "The time seed sprouts – %s later a mighty tree stands here",
    },
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
        "container.timemod.temporal_workbench": "Temporal-Werkbank",
        "gui.timemod.workbench.hint": "Lege ein Zeitwerkzeug ein, um Module einzubauen.",
        "message.timemod.workbench.incomplete": "Die Werkbank schläft: Sie braucht 4 Chrono-Säulen (je 2 hoch) diagonal im Abstand von 2 Blöcken.",
        "message.timemod.workbench.busy": "Jemand arbeitet gerade an dieser Werkbank.",
        "tooltip.timemod.module.install": "Einbau an der Temporal-Werkbank",
        "tooltip.timemod.modules": "Module:",
        "tooltip.timemod.affects": "Wirkt auf: %s",
        "tooltip.timemod.stats": "Radius %s · %s s · %s Chronon",
        "tooltip.timemod.rewind_stats": "Radius %s · %s s zurück · %s× · %s Chronon",
        "filter.timemod.animals": "Tiere",
        "filter.timemod.monsters": "Monster",
        "filter.timemod.villagers": "Dorfbewohner",
        "filter.timemod.players": "Spieler",
        "filter.timemod.projectiles": "Geschosse",
        "filter.timemod.items": "Gegenstände",
        "filter.timemod.vehicles": "Fahrzeuge",
        "filter.timemod.growth": "Wachstum",
        "filter.timemod.environment": "Umwelt",
        "filter.timemod.fluids": "Flüssigkeiten",
        "filter.timemod.redstone": "Redstone",
        "filter.timemod.machines": "Maschinen",
        "filter.timemod.daytime": "Tag/Nacht",
        "filter.timemod.weather": "Wetter",
        "item.timemod.chrono_upgrade_template.applies_to": "Netherit-Rüstung",
        "item.timemod.chrono_upgrade_template.ingredients": "Chronit-Platte",
        "item.timemod.chrono_upgrade_template.title": "Chrono-Aufwertung",
        "item.timemod.chrono_upgrade_template.base_slot": "Netherit-Rüstungsteil einlegen",
        "item.timemod.chrono_upgrade_template.additions_slot": "Chronit-Platte einlegen",
        "tooltip.timemod.chrono_armor.piece": "Jedes Teil spart 5 % Chronon",
        "tooltip.timemod.chrono_armor.helmet": "Zeitsicht: zeigt alle Zeitzonen",
        "tooltip.timemod.chrono_armor.set": "Volles Set: Du bewegst dich in fremdem Zeitstopp",
        "message.timemod.extractor.status": "Extraktor: %s / %s Chronon",
        "death.attack.timemod.old_age": "%1$s ist an Altersschwäche gestorben",
        "death.attack.timemod.old_age.player": "%1$s ist an Altersschwäche gestorben, während %2$s zusah",
        "message.timemod.altar.clockmaker": "Der Altar ruht. Ein Uhrwerk könnte ihn wecken …",
        "message.timemod.altar.phage": "Vier leere Sockel. Es fehlen Zeitkern-Fragmente …",
        "item.timemod.workshop_map": "Karte zur Zeitlosen Werkstatt",
        "entity.timemod.clockmaker": "Der Uhrmacher",
        "entity.timemod.chronophage": "Der Chronophage",
        "entity.timemod.gear_saw": "Zahnrad-Säge",
        "entity.timemod.time_leech": "Zeitegel",
        "boss.timemod.clockmaker.awakens": "Der Uhrmacher erwacht",
        "boss.timemod.clockmaker.awakens.hint": "Nur in Zeitlupe zeigt er seine Schwachstelle",
        "boss.timemod.chronophage.awakens": "Der Chronophage erwacht",
        "boss.timemod.chronophage.awakens.hint": "Halte die Zeit an, wenn sein Maul offen steht",
        "message.timemod.altar.busy": "Der Altar ist noch an seinen Wächter gebunden.",
        "message.timemod.altar.phage.more": "Der Altar verlangt %s Zeitkern-Fragmente.",
        "subtitles.timemod.clockmaker_roar": "Uhrmacher brüllt",
        "subtitles.timemod.gear_saw": "Zahnrad-Säge kreischt",
        "subtitles.timemod.phage_roar": "Chronophage grollt",
        "subtitles.timemod.phage_open": "Chronophage öffnet sein Maul",
        "subtitles.timemod.phage_bite": "Chronophage beißt",
        "subtitles.timemod.phage_drain": "Zeit wird gefressen",
        "subtitles.timemod.boss_awaken": "Etwas erwacht",
        "book.timemod.astronomer.title": "Notizen des Astronomen",
        "book.timemod.astronomer.page1": "Seit Jahren beobachte ich den Himmel. Doch nicht immer ist es der Himmel von heute. Manchmal leuchten Sterne, die längst erloschen sind – oder solche, die erst noch entstehen werden.",
        "book.timemod.astronomer.page2": "Tief unter der Erde, wo der Wächter schläft, hat jemand die Zeit selbst angezapft. Die Ruinen dort summen noch. Vier Splitter, sagt man, wecken ihren Hunger.",
        "book.timemod.astronomer.page3": "Der Uhrmacher baute sich eine Werkstatt ohne Tür. Er wollte nie wieder gestört werden. Doch sein Werk tickt noch – und wer ihm ein Uhrwerk bringt, bekommt eine Antwort.",
        "book.timemod.astronomer.page4": "Es gibt Tore: Ringe aus Chronit, die sich drehen, bis das Jahr nachgibt. Ich sah Wälder, die es noch nicht gibt, Städte aus Glas – und Echsen, so groß wie Türme.",
        "book.timemod.astronomer.page5": "Wenn du dies liest: Zieh deine Uhr auf. Und traue nie einer Zeit, die zu still ist.",
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
        "container.timemod.temporal_workbench": "Temporal Workbench",
        "gui.timemod.workbench.hint": "Insert a time tool to install modules.",
        "message.timemod.workbench.incomplete": "The workbench sleeps: it needs 4 Chrono Pillars (2 high each) placed diagonally 2 blocks away.",
        "message.timemod.workbench.busy": "Someone is already working at this workbench.",
        "tooltip.timemod.module.install": "Install at a Temporal Workbench",
        "tooltip.timemod.modules": "Modules:",
        "tooltip.timemod.affects": "Affects: %s",
        "tooltip.timemod.stats": "Radius %s · %s s · %s Chronon",
        "tooltip.timemod.rewind_stats": "Radius %s · %s s back · %s× · %s Chronon",
        "filter.timemod.animals": "animals",
        "filter.timemod.monsters": "monsters",
        "filter.timemod.villagers": "villagers",
        "filter.timemod.players": "players",
        "filter.timemod.projectiles": "projectiles",
        "filter.timemod.items": "items",
        "filter.timemod.vehicles": "vehicles",
        "filter.timemod.growth": "growth",
        "filter.timemod.environment": "environment",
        "filter.timemod.fluids": "fluids",
        "filter.timemod.redstone": "redstone",
        "filter.timemod.machines": "machines",
        "filter.timemod.daytime": "day/night",
        "filter.timemod.weather": "weather",
        "item.timemod.chrono_upgrade_template.applies_to": "Netherite Armor",
        "item.timemod.chrono_upgrade_template.ingredients": "Chronite Plate",
        "item.timemod.chrono_upgrade_template.title": "Chrono Upgrade",
        "item.timemod.chrono_upgrade_template.base_slot": "Add netherite armor",
        "item.timemod.chrono_upgrade_template.additions_slot": "Add Chronite Plate",
        "tooltip.timemod.chrono_armor.piece": "Each piece saves 5% Chronon",
        "tooltip.timemod.chrono_armor.helmet": "Time sight: shows all time zones",
        "tooltip.timemod.chrono_armor.set": "Full set: you move in others' stopped time",
        "message.timemod.extractor.status": "Extractor: %s / %s Chronon",
        "death.attack.timemod.old_age": "%1$s died of old age",
        "death.attack.timemod.old_age.player": "%1$s died of old age while %2$s watched",
        "message.timemod.altar.clockmaker": "The altar rests. A clockwork might wake it …",
        "message.timemod.altar.phage": "Four empty sockets. Time core fragments are missing …",
        "item.timemod.workshop_map": "Map to the Timeless Workshop",
        "entity.timemod.clockmaker": "The Clockmaker",
        "entity.timemod.chronophage": "The Chronophage",
        "entity.timemod.gear_saw": "Gear Saw",
        "entity.timemod.time_leech": "Time Leech",
        "boss.timemod.clockmaker.awakens": "The Clockmaker awakens",
        "boss.timemod.clockmaker.awakens.hint": "Only in slow motion does he show his weak spot",
        "boss.timemod.chronophage.awakens": "The Chronophage awakens",
        "boss.timemod.chronophage.awakens.hint": "Stop time while its maw is open",
        "message.timemod.altar.busy": "The altar is still bound to its guardian.",
        "message.timemod.altar.phage.more": "The altar demands %s time core fragments.",
        "subtitles.timemod.clockmaker_roar": "Clockmaker roars",
        "subtitles.timemod.gear_saw": "Gear saw screeches",
        "subtitles.timemod.phage_roar": "Chronophage rumbles",
        "subtitles.timemod.phage_open": "Chronophage opens its maw",
        "subtitles.timemod.phage_bite": "Chronophage bites",
        "subtitles.timemod.phage_drain": "Time is devoured",
        "subtitles.timemod.boss_awaken": "Something awakens",
        "book.timemod.astronomer.title": "The Astronomer's Notes",
        "book.timemod.astronomer.page1": "For years I have watched the sky. But it is not always today's sky. Sometimes stars shine that died long ago – or ones that are yet to be born.",
        "book.timemod.astronomer.page2": "Deep below, where the Warden sleeps, someone tapped into time itself. The ruins still hum. Four shards, they say, wake its hunger.",
        "book.timemod.astronomer.page3": "The Clockmaker built himself a workshop without a door. He never wanted to be disturbed again. Yet his work still ticks – bring it a clockwork, and it will answer.",
        "book.timemod.astronomer.page4": "There are gates: rings of chronite that turn until the year gives way. I saw forests that do not exist yet, cities of glass – and lizards as tall as towers.",
        "book.timemod.astronomer.page5": "If you read this: wind your watch. And never trust a time that is too quiet.",
    },
}


# ------------------------------------------------------------------------------------------
# Modelle und Blockzustände
# ------------------------------------------------------------------------------------------

def item_models():
    for item_id, kind, *_ in ITEMS:
        if kind == "spawn_egg":
            write(ASSETS / f"models/item/{item_id}.json", {"parent": "minecraft:item/template_spawn_egg"})
            continue
        parent = "minecraft:item/handheld" if kind == "handheld" else "minecraft:item/generated"
        write(ASSETS / f"models/item/{item_id}.json", {"parent": parent, "textures": {"layer0": t(f"item/{item_id}")}})
    for block_id in BLOCKS:
        if block_id in ("chronite_sprout", "chronos_portal", "time_capsule"):
            continue
        write(ASSETS / f"models/item/{block_id}.json", {"parent": t(f"block/{block_id}")})


def cube_all(name, texture=None):
    model = {"parent": "minecraft:block/cube_all", "textures": {"all": t(f"block/{texture or name}")}}
    write(ASSETS / f"models/block/{name}.json", model)


def simple_state(name, model=None):
    write(ASSETS / f"blockstates/{name}.json", {"variants": {"": {"model": t(f"block/{model or name}")}}})


def gate_models():
    for name in ("chronos_gate_frame", "amber_ore", "amber_block"):
        cube_all(name)
        simple_state(name)
    # Konsole: Fuß, Säule, Pult – die Anzeige zeigt zum Spieler
    side = {"texture": "#side"}
    write(ASSETS / "models/block/chronos_console.json", {
        "parent": "minecraft:block/block",
        "textures": {"particle": t("block/chronos_console_side"), "top": t("block/chronos_console_top"),
                     "side": t("block/chronos_console_side"), "bottom": t("block/chronos_console_bottom")},
        "elements": [
            {"from": [1, 0, 1], "to": [15, 2, 15], "faces": {"down": {"texture": "#bottom"}, "up": {"texture": "#bottom"},
                                                            "north": side, "south": side, "east": side, "west": side}},
            {"from": [3, 2, 3], "to": [13, 9, 13], "faces": {"north": side, "south": side, "east": side, "west": side}},
            {"from": [0, 9, 0], "to": [16, 14, 16], "faces": {"down": {"texture": "#bottom"}, "up": {"texture": "#top", "rotation": 180},
                                                             "north": side, "south": side, "east": side, "west": side}},
        ],
    })
    write(ASSETS / "blockstates/chronos_console.json", {"variants": {
        f"facing={d}": {"model": t("block/chronos_console"), **({"y": y} if y else {})}
        for d, y in (("north", 0), ("east", 90), ("south", 180), ("west", 270))
    }})
    # Portal: dünne, eingefärbte Scheibe wie das Netherportal
    for name, box, faces in (("chronos_portal_ns", ([0, 0, 6], [16, 16, 10]), ("north", "south")),
                             ("chronos_portal_ew", ([6, 0, 0], [10, 16, 16]), ("east", "west"))):
        write(ASSETS / f"models/block/{name}.json", {
            "ambientocclusion": False,
            "textures": {"particle": t("block/chronos_portal"), "portal": t("block/chronos_portal")},
            "elements": [{"from": box[0], "to": box[1], "shade": False,
                          "faces": {f: {"uv": [0, 0, 16, 16], "texture": "#portal", "tintindex": 0} for f in faces}}],
        })
    # Heimkehr-Anker: Sockel, leuchtender Schaft, Deckplatte
    side = {"texture": "#side"}
    stem = {"texture": "#stem"}
    write(ASSETS / "models/block/home_anchor.json", {
        "parent": "minecraft:block/block",
        "textures": {"particle": t("block/home_anchor_side"), "side": t("block/home_anchor_side"), "top": t("block/home_anchor_top"),
                     "stem": t("block/home_anchor_stem")},
        "elements": [
            {"from": [2, 0, 2], "to": [14, 3, 14], "faces": {"down": {"texture": "#side"}, "up": {"texture": "#top"},
                                                            "north": side, "south": side, "east": side, "west": side}},
            {"from": [5, 3, 5], "to": [11, 11, 11], "faces": {"north": stem, "south": stem, "east": stem, "west": stem}},
            {"from": [3, 11, 3], "to": [13, 14, 13], "faces": {"down": {"texture": "#side"}, "up": {"texture": "#top"},
                                                              "north": side, "south": side, "east": side, "west": side}},
        ],
    })
    simple_state("home_anchor")
    # Zeitkapsel: offen, versiegelt (in der Vergangenheit), gefunden (in der Gegenwart)
    for stage in ("open", "sealed", "ancient"):
        write(ASSETS / f"models/block/time_capsule_{stage}.json", {
            "parent": "minecraft:block/block",
            "textures": {"particle": t(f"block/time_capsule_side_{stage}"), "side": t(f"block/time_capsule_side_{stage}"),
                         "top": t(f"block/time_capsule_top_{stage}")},
            "elements": [{"from": [2, 0, 2], "to": [14, 14, 14], "faces": {
                "down": {"texture": "#top"}, "up": {"texture": "#top"}, "north": {"texture": "#side"}, "south": {"texture": "#side"},
                "east": {"texture": "#side"}, "west": {"texture": "#side"}}}],
        })
    write(ASSETS / "blockstates/time_capsule.json", {"variants": {
        f"stage={stage}": {"model": t(f"block/time_capsule_{stage}")} for stage in ("open", "sealed", "ancient")}})
    write(ASSETS / "models/item/time_capsule.json", {"parent": t("block/time_capsule_open")})
    write(ASSETS / "blockstates/chronos_portal.json", {"variants": {
        "axis=x": {"model": t("block/chronos_portal_ns")},
        "axis=z": {"model": t("block/chronos_portal_ew")},
    }})


def block_models():
    gate_models()
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

    # Temporal-Werkbank: Sockel, Säule, Platte – erwacht leuchten die Runen
    for suffix in ("", "_formed"):
        side = t(f"block/temporal_workbench_side{suffix}")
        top = t(f"block/temporal_workbench_top{suffix}")
        stem = t("block/temporal_workbench_stem")
        faces = lambda tex_side, tex_top: {"down": {"texture": "#bottom"}, "up": {"texture": tex_top},
                                           "north": {"texture": tex_side}, "south": {"texture": tex_side},
                                           "east": {"texture": tex_side}, "west": {"texture": tex_side}}
        write(ASSETS / f"models/block/temporal_workbench{suffix}.json", {
            "parent": "minecraft:block/block",
            "textures": {"particle": side, "side": side, "top": top, "stem": stem, "bottom": t("block/time_field_projector_bottom")},
            "elements": [
                {"from": [0, 0, 0], "to": [16, 4, 16], "faces": faces("#side", "#bottom")},
                {"from": [3, 4, 3], "to": [13, 10, 13], "faces": faces("#stem", "#stem")},
                {"from": [1, 10, 1], "to": [15, 13, 15], "faces": faces("#side", "#top")},
            ],
        })
    write(ASSETS / "blockstates/temporal_workbench.json", {"variants": {
        "formed=false": {"model": t("block/temporal_workbench")},
        "formed=true": {"model": t("block/temporal_workbench_formed")},
    }})

    # Chrono-Säule
    write(ASSETS / "models/block/chrono_pillar.json", {
        "parent": "minecraft:block/block",
        "textures": {"particle": t("block/chrono_pillar_side"), "side": t("block/chrono_pillar_side"), "top": t("block/chrono_pillar_top")},
        "elements": [{"from": [3, 0, 3], "to": [13, 16, 13], "faces": {
            "down": {"texture": "#top", "cullface": "down"}, "up": {"texture": "#top", "cullface": "up"},
            "north": {"texture": "#side"}, "south": {"texture": "#side"}, "east": {"texture": "#side"}, "west": {"texture": "#side"}}}],
    })
    simple_state("chrono_pillar")

    # Lebenszeit-Extraktor: Sockel mit Sanduhr-Rune, die beim Entziehen leuchtet
    for suffix in ("", "_active"):
        write(ASSETS / f"models/block/lifetime_extractor{suffix}.json", {
            "parent": "minecraft:block/block",
            "textures": {"particle": t("block/lifetime_extractor_side"), "side": t("block/lifetime_extractor_side"),
                         "top": t(f"block/lifetime_extractor_top{suffix}"), "bottom": t("block/lifetime_extractor_bottom")},
            "elements": [
                {"from": [0, 0, 0], "to": [16, 6, 16], "faces": {
                    "down": {"texture": "#bottom", "cullface": "down"}, "up": {"texture": "#bottom"},
                    "north": {"texture": "#side"}, "south": {"texture": "#side"}, "east": {"texture": "#side"}, "west": {"texture": "#side"}}},
                {"from": [2, 6, 2], "to": [14, 10, 14], "faces": {
                    "up": {"texture": "#top"},
                    "north": {"texture": "#side"}, "south": {"texture": "#side"}, "east": {"texture": "#side"}, "west": {"texture": "#side"}}},
            ],
        })
    for altar in ("clockmaker_altar", "phage_altar", "paradox_altar"):
        sides = {"north": {"texture": "#side"}, "south": {"texture": "#side"}, "east": {"texture": "#side"}, "west": {"texture": "#side"}}
        write(ASSETS / f"models/block/{altar}.json", {
            "parent": "minecraft:block/block",
            "textures": {"particle": t(f"block/{altar}_side"), "side": t(f"block/{altar}_side"), "top": t(f"block/{altar}_top"),
                         "bottom": t(f"block/{altar}_bottom")},
            "elements": [
                {"from": [1, 0, 1], "to": [15, 4, 15], "faces": {"down": {"texture": "#bottom"}, "up": {"texture": "#bottom"}, **sides}},
                {"from": [3, 4, 3], "to": [13, 12, 13], "faces": sides},
                {"from": [2, 12, 2], "to": [14, 14, 14], "faces": {"down": {"texture": "#bottom"}, "up": {"texture": "#top"}, **sides}},
            ],
        })
        simple_state(altar)
    write(ASSETS / "blockstates/lifetime_extractor.json", {"variants": {
        "active=false": {"model": t("block/lifetime_extractor")},
        "active=true": {"model": t("block/lifetime_extractor_active")},
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


def chest_loot(name, pools):
    write(DATA / f"{NS}/loot_tables/chests/{name}.json", {"type": "minecraft:chest", "pools": pools})


def entry(item, weight=1, lo=1, hi=1, extra=None):
    e = {"type": "minecraft:item", "name": item, "weight": weight}
    functions = []
    if lo != 1 or hi != 1:
        functions.append({"function": "minecraft:set_count", "count": {"type": "minecraft:uniform", "min": lo, "max": hi}})
    functions.extend(extra or [])
    if functions:
        e["functions"] = functions
    return e


def workshop_map(weight):
    return {"type": "minecraft:item", "name": "minecraft:map", "weight": weight, "functions": [
        {"function": "minecraft:exploration_map", "destination": t("on_workshop_maps"), "decoration": "target_x",
         "zoom": 2, "search_radius": 64, "skip_existing_chunks": False},
        {"function": "minecraft:set_name", "name": {"translate": "item.timemod.workshop_map", "italic": False}},
    ]}


def structure_loot():
    chest_loot("clock_tower", [
        {"rolls": {"type": "minecraft:uniform", "min": 4, "max": 7}, "entries": [
            entry(t("brass_ingot"), 10, 1, 4), entry(t("brass_nugget"), 12, 3, 9), entry(t("brass_gear"), 6, 1, 3),
            entry(t("mainspring"), 3), entry(t("balance_wheel"), 3), entry(t("time_sand"), 6, 2, 6), entry("minecraft:iron_ingot", 6, 1, 3),
            entry("minecraft:bread", 6, 1, 3), entry("minecraft:clock", 2), entry(t("chronite_seed"), 1), entry("minecraft:string", 5, 2, 5),
        ]},
    ])
    chest_loot("clock_tower_top", [
        {"rolls": 1, "entries": [workshop_map(1)]},
        {"rolls": {"type": "minecraft:uniform", "min": 2, "max": 4}, "entries": [
            entry(t("clockwork"), 5), entry(t("pocket_watch"), 2), entry(t("brass_block"), 2), entry(t("time_dust"), 6, 1, 4),
            entry(t("brass_gear"), 8, 2, 4), entry("minecraft:golden_apple", 1), entry("minecraft:amethyst_shard", 5, 1, 4),
        ]},
    ])
    chest_loot("observatory", [
        {"rolls": 1, "entries": [entry("minecraft:spyglass", 3), workshop_map(1), entry("minecraft:compass", 2)]},
        {"rolls": {"type": "minecraft:uniform", "min": 3, "max": 6}, "entries": [
            entry(t("chronite_crystal"), 5, 1, 3), entry("minecraft:ender_pearl", 5, 1, 2), entry(t("time_dust"), 8, 2, 5),
            entry("minecraft:book", 8, 1, 3), entry("minecraft:paper", 8, 2, 6), entry("minecraft:amethyst_shard", 6, 2, 5),
            entry("minecraft:lapis_lazuli", 6, 3, 8), entry(t("raw_chronite"), 4, 1, 3), entry("minecraft:diamond", 1),
        ]},
    ])
    chest_loot("chrono_ruins", [
        {"rolls": 1, "entries": [entry(t("time_core_fragment"), 1, 2, 2)]},
        {"rolls": {"type": "minecraft:uniform", "min": 3, "max": 6}, "entries": [
            entry("minecraft:echo_shard", 6, 1, 3), entry(t("chronite_crystal"), 8, 2, 5), entry(t("raw_chronite"), 8, 2, 6),
            entry("minecraft:sculk_catalyst", 3), entry("minecraft:diamond", 3, 1, 2), entry("minecraft:netherite_scrap", 1),
            entry(t("blank_module"), 2), entry(t("time_dust"), 6, 2, 6),
            entry("minecraft:book", 3, 1, 1, [{"function": "minecraft:enchant_randomly", "enchantments": ["minecraft:swift_sneak"]}]),
        ]},
    ])
    chest_loot("timeless_workshop", [
        {"rolls": {"type": "minecraft:uniform", "min": 4, "max": 7}, "entries": [
            entry(t("clockwork"), 8, 1, 2), entry(t("brass_block"), 4, 1, 2), entry(t("chronite_crystal"), 6, 1, 4),
            entry(t("mainspring"), 6, 1, 3), entry(t("balance_wheel"), 6, 1, 3), entry(t("pocket_time_cell"), 3),
            entry(t("time_dust"), 8, 2, 6), entry(t("blank_module"), 2), entry("minecraft:diamond", 2, 1, 2),
            entry("minecraft:netherite_scrap", 1),
        ]},
    ])


def citadel_loot():
    chest_loot("timeless_citadel", [
        {"rolls": 1, "entries": [entry(t("paradox_time_cell"), 1), entry(t("chronite_time_cell"), 3), entry(t("chronite_plate"), 2)]},
        {"rolls": {"type": "minecraft:uniform", "min": 4, "max": 7}, "entries": [
            entry(t("chronite_crystal"), 10, 2, 6), entry(t("time_dust"), 8, 3, 8), entry(t("time_core_fragment"), 3, 1, 2),
            entry("minecraft:ender_pearl", 6, 1, 4), entry("minecraft:diamond", 4, 1, 3), entry("minecraft:echo_shard", 4, 1, 3),
            entry("minecraft:golden_apple", 3), entry("minecraft:enchanted_golden_apple", 1), entry("minecraft:netherite_scrap", 2),
            entry(t("chrono_knife"), 5, 4, 12), entry(t("strength_module"), 2), entry(t("efficiency_module"), 2),
            entry("minecraft:book", 3, 1, 1, [{"function": "minecraft:enchant_with_levels", "levels": 30, "treasure": True}]),
        ]},
    ])


def boss_loot():
    write(DATA / f"{NS}/loot_tables/entities/clockmaker.json", {"type": "minecraft:entity", "pools": [
        {"rolls": 1, "entries": [entry(t("clockwork_core"))]},
        {"rolls": 1, "entries": [entry(t("brass_block"), 1, 2, 4)]},
        {"rolls": 1, "entries": [entry(t("clockwork"), 1, 1, 3)]},
        {"rolls": 1, "entries": [entry(t("chronite_crystal"), 1, 2, 5)]},
    ]})
    write(DATA / f"{NS}/loot_tables/entities/chronophage.json", {"type": "minecraft:entity", "pools": [
        {"rolls": 1, "entries": [entry(t("phage_heart"))]},
        {"rolls": 1, "entries": [entry("minecraft:echo_shard", 1, 2, 5)]},
        {"rolls": 1, "entries": [entry(t("time_dust"), 1, 6, 12)]},
        {"rolls": 1, "entries": [entry(t("time_core_fragment"), 1, 1, 2)]},
    ]})
    write(DATA / f"{NS}/loot_tables/entities/paradox.json", {"type": "minecraft:entity", "pools": [
        {"rolls": 1, "entries": [entry(t("heart_of_time"))]},
        {"rolls": 1, "entries": [entry(t("chronite_crystal"), 1, 4, 8)]},
        {"rolls": 1, "entries": [entry(t("time_core_fragment"), 1, 1, 2)]},
        {"rolls": 1, "entries": [entry("minecraft:echo_shard", 1, 2, 4)]},
    ]})
    write(DATA / f"{NS}/loot_tables/entities/paradox_echo.json", {"type": "minecraft:entity", "pools": []})


def loot_tables():
    structure_loot()
    citadel_loot()
    boss_loot()
    for name in ("brass_block", "clockmaker_bench", "time_sand", "time_condenser", "time_field_projector", "temporal_workbench", "chrono_pillar",
                 "lifetime_extractor", "chronos_gate_frame", "amber_block", "time_capsule"):
        self_drop(name)
    # Die Konsole behält ihren Chronon-Speicher
    write(DATA / f"{NS}/loot_tables/blocks/chronos_console.json", {
        "type": "minecraft:block",
        "pools": [{"rolls": 1, "conditions": survives_explosion(), "entries": [{"type": "minecraft:item", "name": t("chronos_console"),
                   "functions": [{"function": "minecraft:copy_nbt", "source": "block_entity",
                                  "ops": [{"source": "Chronon", "target": "BlockEntityTag.Chronon", "op": "replace"}]}]}]}],
    })
    # Bernstein: ein bis zwei Brocken, selten mit Insekt
    write(DATA / f"{NS}/loot_tables/blocks/amber_ore.json", {
        "type": "minecraft:block",
        "pools": [
            {"rolls": 1, "entries": [{"type": "minecraft:alternatives", "children": [
                {"type": "minecraft:item", "name": t("amber_ore"), "conditions": [SILK]},
                {"type": "minecraft:item", "name": t("amber"), "functions": [
                    {"function": "minecraft:set_count", "count": {"type": "minecraft:uniform", "min": 1, "max": 2}},
                    {"function": "minecraft:apply_bonus", "enchantment": "minecraft:fortune", "formula": "minecraft:ore_drops"},
                    {"function": "minecraft:explosion_decay"}]},
            ]}]},
            {"rolls": 1, "conditions": [{"condition": "minecraft:inverted", "term": SILK},
                                        {"condition": "minecraft:table_bonus", "enchantment": "minecraft:fortune",
                                         "chances": [0.1, 0.14, 0.19, 0.25]}],
             "entries": [{"type": "minecraft:item", "name": t("insect_amber")}]},
        ],
    })
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
    shapeless("chronik", ["minecraft:book", t("brass_nugget")], t("chronik"))
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

    # Stufe III: Temporaltechnik (der Uhrwerkskern des ersten Bosses ist der Schlüssel)
    clockmaker("chrono_pillar", ["BCB", "BGB", "BCB"], {"B": t("brass_ingot"), "C": t("chronite_crystal"), "G": t("time_glass")}, t("chrono_pillar"), 2)
    clockmaker("temporal_workbench", ["CKC", "NWN", "BBB"], {"C": t("chronite_crystal"), "K": t("clockwork_core"), "N": "minecraft:netherite_ingot",
                                                             "W": t("clockmaker_bench"), "B": t("brass_block")}, t("temporal_workbench"))
    clockmaker("blank_module", ["BQB", "CSC", "BRB"], {"B": t("brass_ingot"), "Q": "minecraft:quartz", "C": t("chronite_crystal"),
                                                       "S": "minecraft:netherite_scrap", "R": "minecraft:redstone"}, t("blank_module"), 2)
    for name, item in (("filter_animals", "minecraft:leather"), ("filter_monsters", "minecraft:rotten_flesh"), ("filter_villagers", "minecraft:emerald"),
                       ("filter_players", "minecraft:armor_stand"), ("filter_projectiles", "minecraft:arrow"), ("filter_items", "minecraft:hopper"),
                       ("filter_vehicles", "minecraft:minecart"), ("filter_growth", "minecraft:bone_meal"), ("filter_environment", "minecraft:snowball"),
                       ("filter_fluids", "minecraft:bucket"), ("filter_redstone", "minecraft:repeater"), ("filter_machines", "minecraft:furnace")):
        clockmaker(name, [" X ", "XMX", " X "], {"X": item, "M": t("blank_module")}, t(name))
    clockmaker("chronite_plate", ["CCC", "CNC", "CCC"], {"C": t("chronite_crystal"), "N": "minecraft:netherite_scrap"}, t("chronite_plate"))
    clockmaker("chrono_upgrade_template", ["DCD", "DHD", "DDD"], {"D": "minecraft:diamond", "C": t("chronite_crystal"), "H": t("phage_heart")},
               t("chrono_upgrade_template"), 2)
    clockmaker("chrono_upgrade_template_copy", ["DTD", "DCD", "DDD"], {"D": "minecraft:diamond", "T": t("chrono_upgrade_template"),
                                                                       "C": t("chronite_crystal")}, t("chrono_upgrade_template"), 2)
    for piece in ("helmet", "chestplate", "leggings", "boots"):
        write(DATA / f"{NS}/recipes/chrono_{piece}_smithing.json", {
            "type": "minecraft:smithing_transform",
            "template": {"item": t("chrono_upgrade_template")},
            "base": {"item": f"minecraft:netherite_{piece}"},
            "addition": {"item": t("chronite_plate")},
            "result": {"item": t(f"chrono_{piece}")},
        })
    clockmaker("paradox_time_cell", ["PEP", "CXC", "PEP"], {"P": t("chronite_plate"), "E": "minecraft:echo_shard", "C": t("chronite_crystal"),
                                                            "X": t("chronite_time_cell")}, t("paradox_time_cell"))
    # Stufe IV
    clockmaker("time_loop", ["ETE", "RXS", "ECE"], {"E": "minecraft:echo_shard", "T": "minecraft:totem_of_undying", "R": t("recall_watch"),
                                                 "X": t("heart_of_time"), "S": t("rewind_spool"), "C": t("paradox_time_cell")}, t("time_loop"))
    clockmaker("infinity_time_cell", ["PNP", "CXC", "PNP"], {"P": t("paradox_time_cell"), "N": "minecraft:nether_star", "C": t("chronite_plate"),
                                                          "X": t("heart_of_time")}, t("infinity_time_cell"))
    clockmaker("chronos_watch", ["WNT", "KHF", "PMR"], {"W": t("stasis_watch"), "N": "minecraft:nether_star", "T": t("time_accelerator"),
                                                     "K": t("clockwork_core"), "H": t("heart_of_time"), "F": t("phage_heart"),
                                                     "P": t("slow_pendulum"), "M": t("strength_module"), "R": t("rewind_spool")}, t("chronos_watch"))
    # Stufe V: die Zeitmaschine
    clockmaker("chronos_gate_frame", ["IGI", "CTC", "IGI"], {"I": t("brass_ingot"), "G": t("brass_gear"), "C": t("chronite_crystal"),
                                                          "T": t("time_glass")}, t("chronos_gate_frame"), 4)
    clockmaker("chronos_console", ["TPT", "KHK", "BBB"], {"T": t("time_glass"), "P": t("chronite_plate"), "K": t("clockwork"),
                                                       "H": t("heart_of_time"), "B": t("brass_block")}, t("chronos_console"))
    clockmaker("home_anchor", ["CEC", "GKG", "BBB"], {"C": t("chronite_crystal"), "E": "minecraft:ender_pearl", "G": t("time_glass"),
                                                   "K": t("clockwork"), "B": t("brass_ingot")}, t("home_anchor"))
    clockmaker("time_capsule", ["BGB", "CXC", "BBB"], {"B": t("brass_ingot"), "G": t("time_glass"), "C": t("chronite_crystal"),
                                                    "X": "minecraft:chest"}, t("time_capsule"))
    clockmaker("time_seed", [" D ", "CSC", " M "], {"D": t("time_dust"), "C": t("chronite_crystal"), "S": "minecraft:wheat_seeds",
                                                 "M": "minecraft:bone_meal"}, t("time_seed"), 2)
    write(DATA / f"{NS}/recipes/iron_nugget_from_rusty_iron.json", {
        "type": "minecraft:blasting", "category": "misc", "ingredient": {"item": t("rusty_iron")},
        "result": "minecraft:iron_nugget", "experience": 0.1, "cookingtime": 100})
    clockmaker("era_focus", ["FBF", "CTC", "FBF"], {"F": t("fossil_fragment"), "B": t("brass_ingot"), "C": t("chronite_crystal"),
                                                 "T": t("time_glass")}, t("era_focus"))
    clockmaker("mesozoic_focus", ["DXD", "XFX", "DXD"], {"D": t("dna_sample"), "X": t("chronite_crystal"), "F": t("era_focus")}, t("mesozoic_focus"))
    shapeless("dna_sample", [t("insect_amber"), "minecraft:glass_bottle"], t("dna_sample"))
    shaped("amber_block", ["AAA", "AAA", "AAA"], {"A": t("amber")}, t("amber_block"))
    shapeless("amber_from_block", [t("amber_block")], t("amber"), 9)
    clockmaker("lifetime_extractor", ["CHC", "SXS", "BBB"], {"C": t("chronite_crystal"), "H": t("phage_heart"), "S": "minecraft:soul_lantern",
                                                             "X": t("time_condenser"), "B": "minecraft:polished_blackstone"}, t("lifetime_extractor"))
    clockmaker("radius_module", ["EDE", "DMD", "EDE"], {"E": "minecraft:ender_pearl", "D": t("time_dust"), "M": t("blank_module")}, t("radius_module"))
    clockmaker("duration_module", ["CDC", "DMD", "CDC"], {"C": "minecraft:clock", "D": t("time_dust"), "M": t("blank_module")}, t("duration_module"))
    clockmaker("strength_module", ["BDB", "DMD", "BDB"], {"B": "minecraft:blaze_powder", "D": t("time_dust"), "M": t("blank_module")}, t("strength_module"))
    clockmaker("efficiency_module", [" E ", "DMD", " E "], {"E": "minecraft:echo_shard", "D": t("time_dust"), "M": t("blank_module")}, t("efficiency_module"))


# ------------------------------------------------------------------------------------------
# Tags und Weltgenerierung
# ------------------------------------------------------------------------------------------

def tags():
    def tag(path, values):
        write(DATA / path, {"replace": False, "values": values})
    # Eine einzige Liste: ein zweiter Aufruf für dieselbe Datei würde die erste überschreiben
    tag("minecraft/tags/blocks/mineable/pickaxe.json", [t(n) for n in (
        "brass_block", "deepslate_chronite_ore", "chronite_sprout", "time_condenser", "time_field_projector", "temporal_workbench",
        "chrono_pillar", "lifetime_extractor", "chronos_gate_frame", "chronos_console", "amber_ore", "amber_block", "home_anchor",
        "time_capsule", "dull_lantern", "rusty_iron_block", "rusty_iron_bars")])
    tag("minecraft/tags/blocks/mineable/shovel.json", [t("time_sand")])
    tag("minecraft/tags/blocks/mineable/axe.json", [t("clockmaker_bench")])
    tag("minecraft/tags/blocks/needs_iron_tool.json", [t("brass_block"), t("time_condenser"), t("time_field_projector"), t("chrono_pillar")])
    tag("minecraft/tags/blocks/needs_diamond_tool.json", [t("deepslate_chronite_ore"), t("temporal_workbench"), t("lifetime_extractor"),
                                                          t("chronos_gate_frame"), t("chronos_console")])
    tag("minecraft/tags/blocks/needs_stone_tool.json", [t("amber_ore")])
    # Hält der Wither, hält das Tor: Rahmen, Konsole und Portal lassen sich nicht wegsprengen
    tag("minecraft/tags/blocks/wither_immune.json", [t("chronos_portal")])
    # Was die Zeit übersteht und was sie schnell frisst (Datenpakete können beides erweitern)
    tag(f"{NS}/tags/items/timeless.json", ["minecraft:" + n for n in (
        "diamond", "diamond_block", "emerald", "emerald_block", "gold_ingot", "gold_nugget", "gold_block", "raw_gold", "raw_gold_block",
        "netherite_ingot", "netherite_block", "netherite_scrap", "ancient_debris", "amethyst_shard", "amethyst_block", "quartz",
        "nether_star", "echo_shard", "heart_of_the_sea", "nautilus_shell", "prismarine_shard", "prismarine_crystals", "obsidian",
        "crying_obsidian", "golden_apple", "enchanted_golden_apple", "totem_of_undying", "trident")] + [t(n) for n in (
        "chronite_crystal", "heart_of_time", "clockwork_core", "phage_heart", "time_core_fragment", "amber", "insect_amber", "amber_block",
        "fossil_fragment", "dna_sample", "era_focus", "mesozoic_focus", "pocket_time_cell", "chronite_time_cell", "paradox_time_cell",
        "infinity_time_cell", "chronik")])
    tag(f"{NS}/tags/items/organic.json", ["minecraft:" + n for n in (
        "leather", "paper", "string", "feather", "stick", "wheat", "wheat_seeds", "sugar", "rabbit_hide", "map", "filled_map", "name_tag",
        "painting", "item_frame", "glow_item_frame", "armor_stand", "bowl", "bow", "crossbow", "fishing_rod", "shield", "torch",
        "wooden_sword", "wooden_pickaxe", "wooden_axe", "wooden_shovel", "wooden_hoe", "leather_helmet", "leather_chestplate",
        "leather_leggings", "leather_boots", "saddle", "lead", "book", "writable_book", "written_book", "bamboo", "sugar_cane",
        "cactus", "vine", "lily_pad", "hay_block", "slime_ball", "honeycomb", "ink_sac", "glow_ink_sac", "egg", "bone_meal")])
    tag("minecraft/tags/blocks/dragon_immune.json", [t("chronos_portal"), t("chronos_gate_frame")])
    tag("minecraft/tags/items/trimmable_armor.json", [t(f"chrono_{p}") for p in ("helmet", "chestplate", "leggings", "boots")])
    tag("c/tags/items/brass_ingots.json", [t("brass_ingot")])
    tag(f"{NS}/tags/items/modules.json", [t(n) for n in (
        "filter_animals", "filter_monsters", "filter_villagers", "filter_players", "filter_projectiles", "filter_items", "filter_vehicles",
        "filter_growth", "filter_environment", "filter_fluids", "filter_redstone", "filter_machines",
        "radius_module", "duration_module", "strength_module", "efficiency_module")])
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
    write(base / "configured_feature/amber_ore.json", {
        "type": "minecraft:ore",
        "config": {"size": 7, "discard_chance_on_air_exposure": 0.0,
                   "targets": [{"target": {"predicate_type": "minecraft:tag_match", "tag": "minecraft:stone_ore_replaceables"},
                                "state": {"Name": t("amber_ore")}}]},
    })
    placed("amber_ore", "amber_ore", 14, {"absolute": 30}, {"absolute": 100})

    # Strukturen der Stufe III
    def structure(name, step, adaptation, biomes, spacing, separation, salt):
        write(base / f"structure/{name}.json", {
            "type": t(name), "biomes": f"#{NS}:has_structure/{name}", "spawn_overrides": {},
            "step": step, "terrain_adaptation": adaptation,
        })
        write(base / f"structure_set/{name}.json", {
            "structures": [{"structure": t(name), "weight": 1}],
            "placement": {"type": "minecraft:random_spread", "salt": salt, "spacing": spacing, "separation": separation},
        })
        write(DATA / f"{NS}/tags/worldgen/biome/has_structure/{name}.json", {"replace": False, "values": biomes})
    structure("clock_tower", "surface_structures", "beard_thin",
              ["minecraft:plains", "minecraft:sunflower_plains", "minecraft:meadow", "minecraft:forest", "minecraft:birch_forest",
               "minecraft:flower_forest", "minecraft:old_growth_birch_forest", "minecraft:savanna", "minecraft:taiga",
               "minecraft:dark_forest", "minecraft:cherry_grove"], 36, 12, 1017001)
    structure("observatory", "surface_structures", "beard_thin",
              ["minecraft:jagged_peaks", "minecraft:frozen_peaks", "minecraft:stony_peaks", "minecraft:snowy_slopes", "minecraft:grove",
               "minecraft:meadow", "minecraft:cherry_grove", "minecraft:windswept_hills", "minecraft:windswept_gravelly_hills",
               "minecraft:windswept_forest"], 24, 8, 1017002)
    structure("chrono_ruins", "underground_structures", "none", ["minecraft:deep_dark"], 20, 6, 1017003)
    structure("timeless_workshop", "underground_structures", "none", ["#minecraft:is_overworld"], 40, 14, 1017004)
    write(DATA / f"{NS}/tags/worldgen/structure/on_workshop_maps.json", {"replace": False, "values": [t("timeless_workshop")]})
    # Stufe IV: schwebt im äußeren End
    structure("timeless_citadel", "surface_structures", "none",
              ["minecraft:end_highlands", "minecraft:end_midlands", "minecraft:end_barrens", "minecraft:small_end_islands"], 36, 12, 1017005)


# ------------------------------------------------------------------------------------------
# Übersetzungen
# ------------------------------------------------------------------------------------------

def advancements():
    for adv_id, parent, icon, frame, criterion, *_ in ADVANCEMENTS:
        kind, value = criterion
        if kind == "item":
            trigger = {"trigger": "minecraft:inventory_changed", "conditions": {"items": [{"items": [value]}]}}
        elif kind == "items":
            trigger = {"trigger": "minecraft:inventory_changed", "conditions": {"items": [{"items": [v]} for v in value]}}
        elif kind == "tag":
            trigger = {"trigger": "minecraft:inventory_changed", "conditions": {"items": [{"tag": value}]}}
        elif kind == "structure":
            trigger = {"trigger": "minecraft:location", "conditions": {"player": [
                {"condition": "minecraft:entity_properties", "entity": "this", "predicate": {"location": {"structure": value}}}]}}
        elif kind == "impossible":
            trigger = {"trigger": "minecraft:impossible"}
        else:
            trigger = {"trigger": "minecraft:player_killed_entity", "conditions": {"entity": [
                {"condition": "minecraft:entity_properties", "entity": "this", "predicate": {"type": value}}]}}
        display = {
            "icon": {"item": icon},
            "title": {"translate": f"advancements.{NS}.{adv_id}.title"},
            "description": {"translate": f"advancements.{NS}.{adv_id}.description"},
            "frame": frame, "show_toast": True, "announce_to_chat": True, "hidden": False,
        }
        adv = {"display": display, "criteria": {"done": trigger}}
        if parent is None:
            display["background"] = "timemod:textures/block/deepslate_chronite_ore.png"
            display["announce_to_chat"] = False
            display["show_toast"] = False
        else:
            adv["parent"] = t(parent)
        write(DATA / f"{NS}/advancements/{adv_id}.json", adv)


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
        data.update(LANG_CHRONOS[code])
        data.update(LANG_GATE[code])
        for key, (de_title, de_text, en_title, en_text) in CHAPTERS.items():
            data[f"chronik.{NS}.{key}.title"] = de_title if idx == 0 else en_title
            data[f"chronik.{NS}.{key}.text"] = de_text if idx == 0 else en_text
        data[f"chronik.{NS}.title"] = "Die Chronik" if idx == 0 else "The Chronicle"
        data[f"chronik.{NS}.subtitle"] = "Inhalt" if idx == 0 else "Contents"
        for adv_id, *_rest, de_title, de_desc, en_title, en_desc in ADVANCEMENTS:
            data[f"advancements.{NS}.{adv_id}.title"] = de_title if idx == 0 else en_title
            data[f"advancements.{NS}.{adv_id}.description"] = de_desc if idx == 0 else en_desc
        write(path, dict(sorted(data.items())))


def main():
    item_models()
    block_models()
    loot_tables()
    recipes()
    tags()
    worldgen()
    advancements()
    lang()
    import gen_future  # noqa: E402 – die Zukunft (M8) hat ihren eigenen Generator
    gen_future.main()
    print("Assets erzeugt.")


if __name__ == "__main__":
    main()
