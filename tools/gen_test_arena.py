"""Erzeugt die Gametest-Arena (SNBT): Steinboden, Rest Luft. Zwei Hälften für Vergleichstests."""
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
OUT = ROOT / "src/gametest/resources/data/timemod-gametest/gametest/structures/arena.snbt"
SX, SY, SZ = 40, 14, 16


def main():
    lines = ["{", "    DataVersion: 3465,", f"    size: [{SX}, {SY}, {SZ}],", "    data: ["]
    entries = []
    for x in range(SX):
        for y in range(SY):
            for z in range(SZ):
                state = "minecraft:smooth_stone" if y == 0 else "minecraft:air"
                entries.append(f'        {{pos: [{x}, {y}, {z}], state: "{state}"}}')
    lines.append(",\n".join(entries))
    lines += ["    ],", "    entities: [],", "    palette: [", '        "minecraft:smooth_stone",', '        "minecraft:air"', "    ]", "}"]
    OUT.parent.mkdir(parents=True, exist_ok=True)
    OUT.write_text("\n".join(lines) + "\n")
    print("geschrieben:", OUT, len(entries), "Blöcke")


if __name__ == "__main__":
    main()
