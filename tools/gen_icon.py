"""Erzeugt das Mod-Icon (assets/timemod/icon.png) als Pixel-Art im 32x32-Raster, hochskaliert auf 128x128."""
import math
import sys
from pathlib import Path

from PIL import Image

N = 32
OUT = Path(__file__).resolve().parent.parent / "src/main/resources/assets/timemod/icon.png"

BG_IN = (38, 16, 64)
BG_OUT = (12, 6, 24)
RIM_DARK = (120, 74, 18)
RIM = (214, 158, 46)
RIM_LIGHT = (255, 226, 120)
FACE = (238, 230, 205)
FACE_SHADE = (205, 192, 160)
TICK = (70, 52, 40)
HAND = (30, 22, 30)
GLOW = (90, 230, 255)


def lerp(a, b, t):
    return tuple(int(round(a[i] + (b[i] - a[i]) * t)) for i in range(3))


def main():
    img = Image.new("RGBA", (N, N))
    px = img.load()
    c = (N - 1) / 2
    for y in range(N):
        for x in range(N):
            d = math.hypot(x - c, y - c)
            # Hintergrund: violetter Strudel
            ang = math.atan2(y - c, x - c)
            swirl = 0.5 + 0.5 * math.sin(ang * 3 + d * 0.55)
            base = lerp(BG_IN, BG_OUT, min(1.0, d / (N * 0.72)))
            base = lerp(base, (70, 30, 110), swirl * 0.25 * max(0.0, 1 - d / 16))
            px[x, y] = base + (255,)
            if 12.5 < d <= 14.6:
                # Leuchtring
                px[x, y] = lerp(base, GLOW, 0.55 * (1 - abs(d - 13.5) / 1.1)) + (255,)
            if d <= 12.5:
                if d > 11.0:
                    light = 0.5 + 0.5 * math.cos(ang + math.pi * 0.75)
                    col = lerp(RIM_DARK, RIM_LIGHT, light)
                    if d > 11.8:
                        col = lerp(col, RIM_DARK, 0.4)
                    px[x, y] = col + (255,)
                else:
                    shade = max(0.0, (x - c + y - c) / (2 * 11))
                    px[x, y] = lerp(FACE, FACE_SHADE, shade) + (255,)
    # Stundenmarken
    for h in range(12):
        a = h / 12 * 2 * math.pi
        r = 9.4 if h % 3 else 9.0
        for rr in ([r, r + 0.9] if h % 3 == 0 else [r + 0.4]):
            x = int(round(c + math.sin(a) * rr))
            y = int(round(c - math.cos(a) * rr))
            px[x, y] = TICK + (255,)
    # Zeiger (10:10), mit Leuchtspitzen
    def hand(angle_deg, length, color, tip):
        a = math.radians(angle_deg)
        steps = int(length * 3)
        for i in range(steps + 1):
            t = i / steps * length
            x = int(round(c + math.sin(a) * t))
            y = int(round(c - math.cos(a) * t))
            px[x, y] = (tip if t > length - 1.2 else color) + (255,)
    hand(-60, 6.0, HAND, GLOW)
    hand(60, 8.5, HAND, GLOW)
    px[int(c), int(c)] = RIM_LIGHT + (255,)
    px[int(c) + 1, int(c)] = RIM + (255,)
    # Krone oben
    for x in range(14, 18):
        for y in range(1, 3):
            px[x, y] = (RIM_LIGHT if y == 1 else RIM) + (255,)
    px[15, 3] = RIM_DARK + (255,)
    px[16, 3] = RIM_DARK + (255,)
    OUT.parent.mkdir(parents=True, exist_ok=True)
    img.resize((128, 128), Image.NEAREST).save(OUT)
    print("geschrieben:", OUT)


if __name__ == "__main__":
    sys.exit(main())
