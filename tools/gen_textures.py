"""Erzeugt die Item-Texturen von TIME als 16x16-Pixel-Art (reproduzierbar, ohne externe Assets)."""
import math
from pathlib import Path

from PIL import Image

ROOT = Path(__file__).resolve().parent.parent
ITEM_DIR = ROOT / "src/main/resources/assets/timemod/textures/item"

OUTLINE = (34, 20, 40, 255)
BRASS_D = (122, 78, 22, 255)
BRASS = (200, 146, 48, 255)
BRASS_L = (255, 222, 120, 255)
ROD_D = (44, 30, 62, 255)
ROD = (86, 58, 118, 255)
ROD_L = (132, 98, 170, 255)
GLOW_D = (40, 150, 200, 255)
GLOW = (90, 230, 255, 255)
GLOW_L = (210, 252, 255, 255)
FACE = (236, 228, 200, 255)


def chrono_debug_wand():
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    px = img.load()
    # Stab diagonal von unten links nach oben rechts
    for i in range(2, 11):
        x, y = i, 15 - i
        px[x, y] = ROD
        px[x - 1, y] = ROD_D
        px[x, y - 1] = ROD_L if i % 3 else BRASS
    # Messingringe
    for i in (4, 7):
        x, y = i, 15 - i
        px[x, y] = BRASS_L
        px[x - 1, y] = BRASS
        px[x, y - 1] = BRASS
    # Griffende
    px[1, 14] = BRASS_D
    px[2, 14] = BRASS
    px[1, 13] = BRASS
    # Uhr-Kugel oben rechts (Mittelpunkt 11.5, 4.5)
    cx, cy, r = 11.5, 4.5, 3.6
    for y in range(16):
        for x in range(16):
            d = math.hypot(x - cx, y - cy)
            if d <= r:
                if d > r - 0.9:
                    px[x, y] = BRASS if (x + y) % 2 else BRASS_L
                elif d > r - 1.7:
                    px[x, y] = GLOW
                else:
                    px[x, y] = FACE
            elif d <= r + 0.75 and px[x, y][3] == 0:
                px[x, y] = OUTLINE
    # Zeiger
    px[11, 4] = OUTLINE
    px[11, 3] = OUTLINE
    px[12, 4] = OUTLINE
    # Funkeln
    for x, y, c in ((15, 0, GLOW_L), (8, 1, GLOW), (15, 8, GLOW_D)):
        px[x, y] = c
    return img


def main():
    ITEM_DIR.mkdir(parents=True, exist_ok=True)
    textures = {"chrono_debug_wand": chrono_debug_wand()}
    for name, img in textures.items():
        img.save(ITEM_DIR / f"{name}.png")
        print("geschrieben:", name)


if __name__ == "__main__":
    main()
