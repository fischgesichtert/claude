"""Erzeugt alle Texturen von TIME als 16x16-Pixel-Art (reproduzierbar, ohne fremde Grafiken).

Aufruf: python3 -I tools/gen_textures.py [vorschau.png]
Stil: Messing (warm, glänzend), Chronit (violett mit türkisem Leuchten), dunkle Konturen.
"""
import math
import random
import sys
from pathlib import Path

from PIL import Image

ROOT = Path(__file__).resolve().parent.parent
TEX = ROOT / "src/main/resources/assets/timemod/textures"
GUI = ROOT / "src/client/resources/assets/timemod/textures/gui/container"

T = (0, 0, 0, 0)
OUT = (38, 22, 34, 255)

BRASS = [(96, 58, 18, 255), (150, 100, 34, 255), (200, 146, 48, 255), (236, 192, 86, 255), (255, 232, 150, 255)]
IRON = [(60, 62, 72, 255), (105, 108, 120, 255), (160, 164, 176, 255), (214, 218, 228, 255)]
CHRON = [(46, 22, 84, 255), (92, 48, 160, 255), (146, 92, 224, 255), (196, 160, 255, 255), (236, 222, 255, 255)]
GLOW = [(40, 140, 190, 255), (80, 210, 240, 255), (170, 245, 255, 255)]
FACE = [(214, 204, 176, 255), (240, 232, 208, 255)]
WOOD = [(70, 44, 26, 255), (104, 68, 40, 255), (138, 94, 58, 255), (168, 120, 78, 255)]
GREEN = [(40, 110, 50, 255), (80, 170, 70, 255), (150, 220, 110, 255)]
RED = [(130, 30, 30, 255), (200, 60, 50, 255)]
DEEP = [(44, 44, 52, 255), (58, 58, 66, 255), (72, 72, 80, 255), (88, 88, 96, 255)]
SAND = [(196, 164, 98, 255), (220, 190, 120, 255), (236, 210, 146, 255), (250, 232, 176, 255)]


class Sprite:
    def __init__(self, w=16, h=16):
        self.img = Image.new("RGBA", (w, h), T)
        self.px = self.img.load()
        self.w, self.h = w, h

    def set(self, x, y, c):
        if 0 <= x < self.w and 0 <= y < self.h:
            self.px[x, y] = c

    def get(self, x, y):
        return self.px[x, y] if 0 <= x < self.w and 0 <= y < self.h else T

    def rect(self, x0, y0, x1, y1, c):
        for y in range(y0, y1 + 1):
            for x in range(x0, x1 + 1):
                self.set(x, y, c)

    def line(self, x0, y0, x1, y1, c):
        dx, dy = abs(x1 - x0), -abs(y1 - y0)
        sx, sy = (1 if x0 < x1 else -1), (1 if y0 < y1 else -1)
        err = dx + dy
        while True:
            self.set(x0, y0, c)
            if x0 == x1 and y0 == y1:
                break
            e2 = 2 * err
            if e2 >= dy:
                err += dy
                x0 += sx
            if e2 <= dx:
                err += dx
                y0 += sy

    def disc(self, cx, cy, r, c):
        for y in range(self.h):
            for x in range(self.w):
                if math.hypot(x - cx, y - cy) <= r:
                    self.set(x, y, c)

    def ring(self, cx, cy, r0, r1, c):
        for y in range(self.h):
            for x in range(self.w):
                d = math.hypot(x - cx, y - cy)
                if r0 <= d <= r1:
                    self.set(x, y, c)

    def shade_disc(self, cx, cy, r, ramp):
        """Gefüllter Kreis mit Licht von links oben."""
        for y in range(self.h):
            for x in range(self.w):
                d = math.hypot(x - cx, y - cy)
                if d <= r:
                    light = ((cx - x) + (cy - y)) / (2 * max(r, 1))
                    i = int(round((len(ramp) - 1) * (0.5 + 0.5 * light)))
                    self.set(x, y, ramp[max(0, min(len(ramp) - 1, i))])

    def outline(self, c=OUT):
        filled = {(x, y) for y in range(self.h) for x in range(self.w) if self.px[x, y][3] > 0}
        for y in range(self.h):
            for x in range(self.w):
                if (x, y) in filled:
                    continue
                if any((x + dx, y + dy) in filled for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1))):
                    self.px[x, y] = c

    def save(self, path):
        path.parent.mkdir(parents=True, exist_ok=True)
        self.img.save(path)


# ------------------------------------------------------------------------------------------
# Wiederverwendbare Motive
# ------------------------------------------------------------------------------------------

def clock_face(s, cx, cy, r, face=FACE, rim=BRASS, hands=((0, -0.75), (0.55, 0.15)), glow=None):
    s.shade_disc(cx, cy, r, rim[1:])
    s.disc(cx, cy, r - 1.2, face[1])
    for y in range(s.h):
        for x in range(s.w):
            d = math.hypot(x - cx, y - cy)
            if r - 2.2 < d <= r - 1.2 and (x + y) % 3 == 0:
                s.set(x, y, face[0])
    for hx, hy in hands:
        steps = 6
        for i in range(1, steps + 1):
            f = i / steps
            s.set(int(round(cx + hx * (r - 1.5) * f)), int(round(cy + hy * (r - 1.5) * f)), glow or OUT)
    s.set(int(round(cx)), int(round(cy)), rim[4])


def gear(s, cx, cy, r, ramp=BRASS, teeth=8, hole=1.2):
    for y in range(s.h):
        for x in range(s.w):
            dx, dy = x - cx, y - cy
            d = math.hypot(dx, dy)
            a = math.atan2(dy, dx)
            tooth = (math.cos(a * teeth) > 0.2)
            limit = r if tooth else r - 1.3
            if hole < d <= limit:
                light = (-dx - dy) / (2 * r)
                i = int(round(1 + 2.5 * (0.5 + 0.5 * light)))
                s.set(x, y, ramp[max(0, min(len(ramp) - 1, i))])


def crystal(s, points, ramp=CHRON, edge=GLOW[2]):
    """Kristall als Polygon mit Schattierung von links nach rechts und heller Kante."""
    xs = [p[0] for p in points]
    ys = [p[1] for p in points]
    for y in range(min(ys), max(ys) + 1):
        for x in range(min(xs), max(xs) + 1):
            if inside(points, x + 0.5, y + 0.5):
                f = (x - min(xs)) / max(1, max(xs) - min(xs))
                i = 3 - int(f * 3)
                s.set(x, y, ramp[max(1, min(4, i))])
    for (x0, y0), (x1, y1) in zip(points, points[1:] + points[:1]):
        s.line(x0, y0, x1, y1, ramp[1])
    s.line(points[0][0], points[0][1], points[1][0], points[1][1], edge)


def inside(poly, x, y):
    c = False
    for (x0, y0), (x1, y1) in zip(poly, poly[1:] + poly[:1]):
        if (y0 > y) != (y1 > y) and x < (x1 - x0) * (y - y0) / (y1 - y0) + x0:
            c = not c
    return c


def noise_fill(s, ramp, seed, x0=0, y0=0, x1=15, y1=15, weights=None):
    rnd = random.Random(seed)
    for y in range(y0, y1 + 1):
        for x in range(x0, x1 + 1):
            i = rnd.choices(range(len(ramp)), weights=weights or [1] * len(ramp))[0]
            s.set(x, y, ramp[i])


# ------------------------------------------------------------------------------------------
# Items
# ------------------------------------------------------------------------------------------

def brass_ingot():
    s = Sprite()
    shape = [(2, 9), (9, 4), (14, 6), (7, 12)]
    for y in range(16):
        for x in range(16):
            if inside(shape, x + 0.5, y + 0.5):
                top = inside([(3, 9), (9, 5), (13, 6), (7, 10)], x + 0.5, y + 0.5)
                s.set(x, y, BRASS[3] if top else BRASS[2])
    s.line(3, 9, 9, 5, BRASS[4])
    s.line(7, 12, 14, 7, BRASS[1])
    s.outline()
    return s


def brass_nugget():
    s = Sprite()
    for cx, cy, r in ((7, 9, 2.2), (10, 8, 1.8), (8, 6, 1.6)):
        s.shade_disc(cx, cy, r, BRASS[1:])
    s.set(7, 5, BRASS[4])
    s.outline()
    return s


def brass_gear():
    s = Sprite()
    gear(s, 7.5, 7.5, 6.5, BRASS, teeth=8, hole=1.4)
    s.ring(7.5, 7.5, 2.6, 3.2, BRASS[1])
    s.outline()
    return s


def mainspring():
    s = Sprite()
    cx, cy = 7.5, 7.5
    for i in range(220):
        a = i / 220 * 5.2 * math.pi
        r = 0.8 + a * 0.38
        x, y = int(round(cx + math.cos(a) * r)), int(round(cy + math.sin(a) * r))
        s.set(x, y, IRON[3] if (x + y) % 2 else IRON[2])
    s.disc(cx, cy, 1.2, BRASS[3])
    s.set(14, 7, IRON[1])
    s.set(14, 8, IRON[1])
    s.outline()
    return s


def balance_wheel():
    s = Sprite()
    s.ring(7.5, 7.5, 4.6, 6.4, BRASS[3])
    s.ring(7.5, 7.5, 5.6, 6.4, BRASS[2])
    for a in (0, 2.09, 4.19):
        s.line(8, 8, int(round(7.5 + math.cos(a) * 5)), int(round(7.5 + math.sin(a) * 5)), BRASS[1])
    s.disc(7.5, 7.5, 1.4, (220, 60, 70, 255))
    s.outline()
    return s


def clockwork():
    s = Sprite()
    gear(s, 6, 6, 5, BRASS, teeth=8, hole=1.0)
    gear(s, 11.5, 11, 3.8, BRASS, teeth=6, hole=0.8)
    s.ring(6, 6, 1.8, 2.4, BRASS[1])
    for i in range(60):
        a = i / 60 * 3.2 * math.pi
        r = 0.5 + a * 0.28
        s.set(int(round(11.5 + math.cos(a) * r)), int(round(4 + math.sin(a) * r)), IRON[3])
    s.set(4, 11, (220, 60, 70, 255))
    s.set(5, 12, (220, 60, 70, 255))
    s.outline()
    return s


def raw_chronite():
    s = Sprite()
    blob = [(3, 8), (6, 4), (11, 3), (14, 7), (12, 12), (6, 13)]
    for y in range(16):
        for x in range(16):
            if inside(blob, x + 0.5, y + 0.5):
                s.set(x, y, CHRON[1] if (x * 7 + y * 3) % 5 == 0 else CHRON[2])
    for x, y in ((7, 6), (10, 5), (11, 9), (6, 10), (9, 8)):
        s.set(x, y, GLOW[1])
    s.set(8, 5, CHRON[4])
    s.outline()
    return s


def chronite_crystal():
    s = Sprite()
    crystal(s, [(7, 1), (11, 5), (10, 13), (6, 14), (4, 8)])
    crystal(s, [(11, 6), (14, 9), (12, 14), (10, 12)])
    s.set(7, 4, (255, 255, 255, 255))
    s.set(6, 6, GLOW[2])
    s.outline()
    return s


def time_dust():
    s = Sprite()
    rnd = random.Random(5)
    for y in range(7, 15):
        width = (y - 7) * 0.9 + 1.5
        for x in range(16):
            if abs(x - 7.5) <= width:
                s.set(x, y, rnd.choice([CHRON[2], CHRON[3], CHRON[2], GLOW[1]]))
    for x, y in ((4, 5), (11, 4), (8, 3), (13, 8), (2, 9)):
        s.set(x, y, GLOW[2])
    s.outline()
    return s


def chronite_seed():
    s = Sprite()
    s.rect(4, 12, 11, 14, DEEP[2])
    s.rect(5, 11, 10, 11, DEEP[3])
    crystal(s, [(7, 4), (9, 7), (8, 11), (6, 11), (5, 8)])
    s.set(7, 6, GLOW[2])
    s.outline()
    return s


def pocket_watch():
    s = Sprite()
    clock_face(s, 7.5, 9, 6.2, hands=((0, -0.8), (0.6, 0.1)))
    s.ring(7.5, 2.2, 1.2, 2.0, BRASS[3])
    s.set(7, 3, BRASS[2])
    s.set(8, 3, BRASS[2])
    s.outline()
    return s


def growth_hourglass():
    s = Sprite()
    s.rect(3, 1, 12, 2, WOOD[2])
    s.rect(3, 13, 12, 14, WOOD[2])
    s.line(3, 1, 12, 1, WOOD[3])
    for y in range(3, 13):
        w = abs(y - 7.5) * 0.8 + 0.6
        for x in range(16):
            if abs(x - 7.5) <= w:
                s.set(x, y, (200, 240, 255, 150))
    for y in range(3, 7):
        w = abs(y - 7.5) * 0.8 - 0.2
        for x in range(16):
            if abs(x - 7.5) <= w:
                s.set(x, y, GREEN[1])
    for y in range(10, 13):
        w = abs(y - 7.5) * 0.8 - 0.2
        for x in range(16):
            if abs(x - 7.5) <= w:
                s.set(x, y, GREEN[2] if y == 10 else GREEN[1])
    s.set(7, 8, GREEN[2])
    s.set(8, 9, GREEN[2])
    for y in range(3, 13):
        s.set(3, y, BRASS[2])
        s.set(12, y, BRASS[1])
    s.outline()
    return s


def brake_spring():
    s = Sprite()
    s.rect(4, 1, 11, 2, BRASS[3])
    s.rect(4, 13, 11, 14, BRASS[2])
    for i in range(10):
        y = 3 + i
        if i % 2 == 0:
            s.line(4, y, 11, y + 1, IRON[3])
        else:
            s.line(11, y, 4, y + 1, IRON[2])
    s.rect(12, 6, 14, 9, RED[1])
    s.set(12, 6, RED[0])
    s.outline()
    return s


def moment_lens():
    s = Sprite()
    s.ring(6.5, 6.5, 3.6, 5.4, BRASS[3])
    s.ring(6.5, 6.5, 4.6, 5.4, BRASS[2])
    s.disc(6.5, 6.5, 3.6, (120, 220, 245, 170))
    s.set(5, 4, (255, 255, 255, 220))
    s.set(4, 5, (255, 255, 255, 200))
    s.line(10, 10, 14, 14, WOOD[1])
    s.line(11, 10, 14, 13, WOOD[2])
    s.set(10, 10, BRASS[3])
    s.outline()
    return s


def recall_watch():
    s = Sprite()
    clock_face(s, 7.5, 8.5, 6.4, face=[(150, 210, 190, 255), (196, 240, 222, 255)], hands=((0, -0.8), (-0.6, 0.1)), glow=(40, 100, 90, 255))
    for i in range(14):
        a = math.pi * 1.1 + i / 14 * math.pi * 1.2
        s.set(int(round(7.5 + math.cos(a) * 4.3)), int(round(8.5 + math.sin(a) * 4.3)), (50, 160, 120, 255))
    s.set(3, 6, (50, 160, 120, 255))
    s.set(4, 5, (50, 160, 120, 255))
    s.ring(7.5, 1.6, 1.0, 1.8, BRASS[3])
    s.outline()
    return s


def stasis_watch():
    s = Sprite()
    clock_face(s, 7.5, 8.5, 6.6, face=[(60, 60, 70, 255), (90, 92, 104, 255)], rim=CHRON, hands=((0, -0.8), (0.0, 0.8)), glow=GLOW[2])
    for x, y in ((3, 3), (12, 3), (3, 14), (12, 14)):
        s.set(x, y, GLOW[1])
    s.rect(6, 0, 9, 1, CHRON[3])
    s.outline()
    return s


def time_accelerator():
    s = Sprite()
    s.rect(2, 3, 13, 12, BRASS[1])
    s.rect(3, 4, 12, 11, (30, 26, 48, 255))
    for ox in (4, 8):
        for i in range(4):
            s.set(ox + i, 5 + i, GLOW[1 if i < 3 else 2])
            s.set(ox + i, 10 - i, GLOW[1 if i < 3 else 2])
    gear(s, 13, 13, 2.6, BRASS, teeth=6, hole=0.6)
    s.line(2, 3, 13, 3, BRASS[3])
    s.outline()
    return s


def slow_pendulum():
    s = Sprite()
    s.rect(2, 1, 13, 2, WOOD[2])
    s.line(2, 1, 13, 1, WOOD[3])
    s.line(7, 3, 5, 10, BRASS[2])
    s.line(8, 3, 6, 10, BRASS[3])
    s.shade_disc(5, 12, 2.6, BRASS[1:])
    s.set(4, 11, BRASS[4])
    for i, x in enumerate((9, 10, 11)):
        s.set(x, 11 - i, (150, 200, 255, 160))
    s.outline()
    return s


def rewind_spool():
    s = Sprite()
    for cx in (4.5, 11.5):
        s.ring(cx, 7.5, 2.0, 3.6, (40, 34, 40, 255))
        s.ring(cx, 7.5, 2.8, 3.6, (70, 60, 66, 255))
        s.disc(cx, 7.5, 1.6, BRASS[3])
        s.set(int(cx), 7, BRASS[1])
    s.line(4, 11, 11, 11, (60, 50, 56, 255))
    s.rect(2, 12, 13, 14, CHRON[1])
    s.line(2, 12, 13, 12, CHRON[3])
    for i, x in enumerate((6, 7, 8)):
        s.set(x, 13, GLOW[2])
    s.set(5, 13, GLOW[1])
    s.outline()
    return s


def chrono_knife():
    s = Sprite()
    for i in range(8):
        s.set(5 + i, 10 - i, GLOW[1])
        s.set(6 + i, 10 - i, GLOW[2] if i < 6 else GLOW[1])
        s.set(5 + i, 9 - i, CHRON[3])
    s.set(13, 2, GLOW[2])
    s.line(2, 11, 6, 15, BRASS[2])
    s.line(3, 10, 3, 10, BRASS[3])
    s.line(2, 13, 4, 11, BRASS[3])
    s.set(1, 14, BRASS[1])
    s.set(2, 15, BRASS[1])
    s.line(3, 12, 1, 14, WOOD[1])
    s.outline()
    return s


def cell(ramp_glow, caps, big=False):
    s = Sprite()
    x0, x1 = (4, 11) if not big else (3, 12)
    s.rect(x0, 1, x1, 2, caps[3])
    s.rect(x0, 13, x1, 14, caps[2])
    s.rect(x0 + 1, 3, x1 - 1, 12, (190, 236, 250, 140))
    s.rect(x0 + 2, 5, x1 - 2, 11, ramp_glow[1])
    s.line(x0 + 2, 5, x0 + 2, 11, ramp_glow[2])
    s.set(x0 + 3, 7, (255, 255, 255, 255))
    s.rect(7, 0, 8, 0, caps[4])
    s.outline()
    return s


def pocket_time_cell():
    return cell(GLOW, BRASS)


def chronite_time_cell():
    return cell([CHRON[1], CHRON[2], CHRON[4]], BRASS, big=True)


# ------------------------------------------------------------------------------------------
# Blöcke
# ------------------------------------------------------------------------------------------

def brass_block():
    s = Sprite()
    noise_fill(s, [BRASS[2], BRASS[3], BRASS[2]], 11, weights=[3, 2, 3])
    for y in range(16):
        if y % 2 == 0:
            for x in range(16):
                if (x + y) % 5 == 0:
                    s.set(x, y, BRASS[3])
    s.rect(0, 0, 15, 0, BRASS[4])
    s.rect(0, 0, 0, 15, BRASS[4])
    s.rect(0, 15, 15, 15, BRASS[1])
    s.rect(15, 0, 15, 15, BRASS[1])
    for x, y in ((2, 2), (13, 2), (2, 13), (13, 13)):
        s.set(x, y, BRASS[4])
        s.set(x + 1, y + 1, BRASS[0])
    return s


def time_sand():
    s = Sprite()
    noise_fill(s, SAND, 21, weights=[2, 4, 3, 1])
    rnd = random.Random(4)
    for _ in range(7):
        s.set(rnd.randrange(16), rnd.randrange(16), GLOW[2] if rnd.random() < 0.5 else (255, 245, 210, 255))
    return s


def time_glass():
    s = Sprite()
    s.rect(0, 0, 15, 15, (150, 225, 245, 70))
    s.rect(0, 0, 15, 0, (200, 245, 255, 220))
    s.rect(0, 15, 15, 15, (90, 170, 200, 220))
    s.rect(0, 0, 0, 15, (200, 245, 255, 220))
    s.rect(15, 0, 15, 15, (90, 170, 200, 220))
    s.line(3, 4, 6, 1, (240, 255, 255, 170))
    s.line(3, 6, 8, 1, (240, 255, 255, 110))
    s.ring(10, 10, 2.6, 3.2, (120, 210, 240, 120))
    s.set(10, 8, (120, 210, 240, 150))
    s.set(11, 10, (120, 210, 240, 150))
    return s


def deepslate(seed):
    s = Sprite()
    noise_fill(s, DEEP, seed, weights=[2, 4, 3, 1])
    for y in (4, 9, 14):
        for x in range(16):
            if (x + y) % 7 != 0:
                s.set(x, y, DEEP[0] if (x % 3) else DEEP[1])
    return s


def deepslate_chronite_ore():
    s = deepslate(31)
    for pts in ([(3, 2), (5, 4), (4, 7), (2, 5)], [(10, 5), (13, 7), (12, 10), (9, 9)], [(5, 10), (7, 12), (6, 14), (4, 13)]):
        crystal(s, pts)
    for x, y in ((4, 4), (11, 7), (6, 12)):
        s.set(x, y, GLOW[2])
    return s


def deepslate_chronite_ore_phased():
    s = deepslate_chronite_ore()
    px = s.px
    for y in range(16):
        for x in range(16):
            r, g, b, a = px[x, y]
            if (r, g, b, a) in [c for c in DEEP]:
                px[x, y] = (r + 30, g + 20, b + 60, 70)
            else:
                px[x, y] = (min(255, r + 40), min(255, g + 40), min(255, b + 40), 200)
    return s


def chronite_sprout(stage):
    s = Sprite()
    heights = [4, 7, 10, 14]
    h = heights[stage]
    base = 15
    crystal(s, [(7, base - h), (9, base - h + 3), (9, base), (6, base), (6, base - h + 3)])
    if stage >= 1:
        crystal(s, [(3, base - h // 2 - 1), (5, base - h // 2 + 1), (5, base), (3, base)])
    if stage >= 2:
        crystal(s, [(12, base - h // 2), (13, base - h // 2 + 2), (13, base), (11, base)])
    if stage >= 3:
        crystal(s, [(10, base - h + 2), (11, base - h + 4), (10, base - 3), (9, base - 4)])
        s.set(7, base - h + 1, (255, 255, 255, 255))
    s.set(7, base - h + 2, GLOW[2])
    return s


def bench_top():
    s = Sprite()
    noise_fill(s, WOOD[1:], 41, weights=[2, 3, 2])
    for y in range(0, 16, 4):
        s.line(0, y, 15, y, WOOD[0])
    gear(s, 5, 5, 3.6, BRASS, teeth=6, hole=0.8)
    gear(s, 10.5, 10, 2.6, BRASS, teeth=6, hole=0.6)
    s.line(9, 3, 13, 3, IRON[3])
    s.line(13, 3, 13, 5, IRON[2])
    s.rect(0, 0, 15, 0, WOOD[3])
    return s


def bench_side():
    s = Sprite()
    noise_fill(s, WOOD[1:], 43, weights=[2, 3, 2])
    s.rect(0, 0, 15, 2, WOOD[3])
    s.rect(1, 5, 14, 9, WOOD[0])
    s.rect(2, 6, 13, 8, WOOD[2])
    s.rect(7, 7, 8, 7, BRASS[3])
    s.rect(1, 11, 14, 14, WOOD[0])
    s.rect(2, 12, 13, 13, WOOD[2])
    s.rect(7, 12, 8, 12, BRASS[3])
    return s


def bench_front():
    s = bench_side()
    s.rect(1, 3, 14, 10, WOOD[0])
    clock_face(s, 7.5, 6.5, 3.6, hands=((0, -0.8), (0.6, 0.0)))
    return s


def condenser_side():
    s = Sprite()
    noise_fill(s, [BRASS[1], BRASS[2]], 51, weights=[1, 3])
    s.rect(0, 0, 15, 1, BRASS[3])
    s.rect(0, 14, 15, 15, BRASS[0])
    for x in range(1, 15, 3):
        s.set(x, 7, GLOW[1])
    return s


def condenser_top():
    s = Sprite()
    noise_fill(s, [BRASS[2], BRASS[3]], 53, weights=[3, 1])
    s.ring(7.5, 7.5, 4.0, 5.2, GLOW[1])
    s.disc(7.5, 7.5, 2.5, (30, 26, 48, 255))
    s.set(7, 7, GLOW[2])
    return s


def condenser_base():
    s = Sprite()
    noise_fill(s, [BRASS[0], BRASS[1]], 55, weights=[2, 1])
    return s


def projector(part, active):
    s = Sprite()
    noise_fill(s, [(40, 30, 60, 255), (52, 40, 78, 255), (64, 50, 92, 255)], 61 + len(part), weights=[2, 3, 1])
    s.rect(0, 0, 15, 0, CHRON[2])
    s.rect(0, 15, 15, 15, (24, 18, 34, 255))
    lit = GLOW if active else [CHRON[0], CHRON[1], CHRON[2]]
    if part == "top":
        s.ring(7.5, 7.5, 3.5, 6.0, BRASS[2])
        s.disc(7.5, 7.5, 3.5, lit[1])
        s.disc(7.5, 7.5, 1.6, lit[2])
    elif part == "side":
        for y in (4, 8, 12):
            s.line(2, y, 13, y, lit[1] if active else CHRON[1])
        s.rect(6, 3, 9, 12, lit[0])
        s.rect(7, 4, 8, 11, lit[2] if active else lit[1])
    else:
        s.rect(3, 3, 12, 12, (30, 24, 40, 255))
    return s


# ------------------------------------------------------------------------------------------
# Stufe III: Module, Temporal-Werkbank, Chrono-Säule, Boss-Beute
# ------------------------------------------------------------------------------------------

PCB = [(24, 28, 40, 255), (36, 44, 60, 255), (52, 64, 84, 255)]

ICON_PALETTE = {
    "p": (240, 160, 170, 255), "P": (200, 110, 125, 255), "k": (30, 20, 30, 255), "n": (120, 50, 70, 255),
    "g": (96, 190, 80, 255), "G": (60, 140, 50, 255), "K": (24, 40, 24, 255),
    "e": (60, 200, 120, 255), "E": (150, 250, 190, 255),
    "h": (90, 60, 40, 255), "s": (220, 170, 130, 255), "b": (70, 110, 220, 255), "w": (245, 245, 255, 255),
    "f": (210, 210, 220, 255), "a": (150, 110, 70, 255), "A": (110, 80, 50, 255),
    "y": (250, 230, 90, 255), "Y": (200, 170, 40, 255), "o": (140, 220, 80, 255),
    "i": (150, 150, 165, 255), "I": (95, 95, 110, 255), "c": (80, 210, 240, 255), "C": (170, 245, 255, 255),
    "l": (110, 190, 255, 255), "L": (60, 120, 220, 255), "r": (230, 40, 40, 255), "R": (255, 120, 90, 255),
    "S": (130, 130, 140, 255), "d": (40, 40, 48, 255), "F": (255, 170, 40, 255), "m": (255, 210, 120, 255),
    "B": (180, 130, 60, 255), "q": (230, 200, 150, 255),
}

ICONS = {
    "animals": ["........", ".pp..pp.", ".pppppp.", ".pkppkp.", ".pppppp.", ".pPPPPp.", ".pPnnPp.", "..pppp.."],
    "monsters": ["gggggggg", "gKKggKKg", "gKKggKKg", "gggKKggg", "ggKKKKgg", "ggKKKKgg", "ggKggKgg", "gggggggg"],
    "villagers": ["...ee...", "..eEEe..", ".eEeeEe.", "eEeeeeEe", ".eEeeEe.", "..eEEe..", "...ee...", "........"],
    "players": ["hhhhhhhh", "hhhhhhhh", "hssssssh", "swbsswbs", "ssssssss", "sssAAsss", "ssAssAss", "ssssssss"],
    "projectiles": [".....fff", "......ff", ".....a.f", "....a...", "...a....", ".wa.....", "ww......", ".w......"],
    "items": ["..yyyy..", ".yYYYYy.", "yYooooYy", "yYowooYy", "yYooooYy", "yYooooYy", ".yYYYYy.", "..yyyy.."],
    "vehicles": ["........", "........", "iiiiiiii", "iIIIIIIi", "iIIIIIIi", ".iiiiii.", ".k....k.", "kSk..kSk"],
    "growth": ["...gg...", "..gGGg..", ".gGggGg.", "..gGGg..", "...aa...", "...a....", "..aa....", ".hhhhhh."],
    "environment": ["...w....", ".w.w.w..", "..www...", "wwwCwww.", "..www...", ".w.w.w..", "...w....", "........"],
    "fluids": ["...l....", "...l....", "..lll...", ".llLll..", ".lLLLl..", ".lLwLl..", "..lll...", "........"],
    "redstone": ["...rr...", "..rRRr..", "...rr...", "...aa...", "...aa...", "...aa...", "...AA...", "........"],
    "machines": ["SSSSSSSS", "SiiiiiiS", "SiddddiS", "SidFFdiS", "SiFmmFiS", "SiddddiS", "SiiiiiiS", "SSSSSSSS"],
    "radius": ["w..cc..w", ".w....w.", "...cc...", "c.cCCc.c", "c.cCCc.c", "...cc...", ".w....w.", "w..cc..w"],
    "duration": ["BBBBBBBB", ".BqqqqB.", "..BqqB..", "...BB...", "...BB...", "..B.qB..", ".BqqqqB.", "BBBBBBBB"],
    "strength": ["....yy..", "...yy...", "..yy....", ".yyyyy..", "....yy..", "...yy...", "..yy....", ".y......"],
    "efficiency": ["...ww...", ".wwwwww.", ".w....w.", ".wggggw.", ".wGGGGw.", ".wggggw.", ".wGGGGw.", ".wwwwww."],
}


def module_base(kind):
    s = Sprite()
    s.rect(2, 2, 13, 12, PCB[1])
    s.rect(3, 3, 12, 11, PCB[0])
    accent = {"filter": GLOW[1], "amp": BRASS[3], "blank": PCB[2]}[kind]
    s.rect(2, 2, 13, 2, accent)
    s.rect(2, 2, 2, 12, accent)
    s.rect(13, 3, 13, 12, PCB[2])
    s.rect(3, 12, 13, 12, PCB[2])
    # Kontakte unten (Messing)
    for x in range(3, 13, 2):
        s.set(x, 13, BRASS[3])
        s.set(x, 14, BRASS[2])
    # Leiterbahnen
    s.set(3, 5, GLOW[0])
    s.set(12, 9, GLOW[0])
    s.set(12, 4, BRASS[2])
    s.set(3, 10, BRASS[2])
    return s


def module(kind, icon=None):
    s = module_base(kind)
    if icon is None:
        # leerer Sockel
        s.rect(5, 5, 10, 10, PCB[1])
        s.rect(6, 6, 9, 9, PCB[0])
        for x, y in ((5, 5), (10, 5), (5, 10), (10, 10)):
            s.set(x, y, BRASS[2])
    else:
        rows = ICONS[icon]
        for y, row in enumerate(rows):
            for x, ch in enumerate(row):
                if ch != ".":
                    s.set(4 + x, 3 + y, ICON_PALETTE[ch])
    s.outline()
    return s


def clockwork_core():
    s = Sprite()
    gear(s, 7.5, 7.5, 7.0, BRASS, teeth=10, hole=0.0)
    s.shade_disc(7.5, 7.5, 4.2, [BRASS[1], BRASS[2], BRASS[3]])
    s.disc(7.5, 7.5, 2.8, GLOW[0])
    s.disc(7.5, 7.5, 1.8, GLOW[1])
    s.disc(7.2, 7.2, 0.9, GLOW[2])
    for a in range(0, 360, 90):
        x = 7.5 + 3.5 * math.cos(math.radians(a + 45))
        y = 7.5 + 3.5 * math.sin(math.radians(a + 45))
        s.set(int(round(x)), int(round(y)), BRASS[4])
    s.outline()
    return s


def phage_heart():
    s = Sprite()
    heart = [(7.5, 14), (1.5, 7), (1.5, 4), (4, 2), (7.5, 4.5), (11, 2), (13.5, 4), (13.5, 7)]
    for y in range(16):
        for x in range(16):
            if inside(heart, x + 0.5, y + 0.5):
                f = (x + y) / 30
                s.set(x, y, [CHRON[2], CHRON[1], CHRON[0]][min(2, int(f * 3))])
    # Adern und Zähne
    s.line(4, 4, 7, 9, RED[0])
    s.line(11, 4, 8, 9, RED[0])
    s.line(7, 9, 7, 12, RED[1])
    for x in range(5, 11, 2):
        s.set(x, 7, (240, 230, 220, 255))
    s.disc(7.5, 8.0, 1.2, GLOW[1])
    s.set(7, 8, GLOW[2])
    s.set(4, 3, CHRON[4])
    s.outline()
    return s


CHRONOS_FRAMES = 8
GOLD = [(120, 70, 10, 255), (190, 130, 30, 255), (236, 186, 60, 255), (255, 226, 120, 255), (255, 248, 210, 255)]


def chronos_watch():
    """Die Chronos-Uhr, animiert: kreisender Minutenzeiger, ein schräger Orbit-Ring mit zwei Lichtern."""
    strip = Sprite(16, 16 * CHRONOS_FRAMES)
    cx, cy = 7.5, 7.5
    tilt = math.radians(-18)
    for f in range(CHRONOS_FRAMES):
        s = Sprite()
        phase = f / CHRONOS_FRAMES * 2 * math.pi

        def ring_point(t):
            x, y = 7.6 * math.cos(t), 1.9 * math.sin(t)
            return cx + x * math.cos(tilt) - y * math.sin(tilt), cy + 1.2 + x * math.sin(tilt) + y * math.cos(tilt)

        def draw_ring(front, color):
            for i in range(120):
                t = i / 120 * 2 * math.pi
                if (math.sin(t) > 0) != front:
                    continue
                x, y = ring_point(t)
                s.set(int(round(x)), int(round(y)), color)

        draw_ring(False, CHRON[1])
        a = phase - math.pi / 2
        clock_face(s, cx, cy, 5.4, face=[(28, 16, 50, 255), (46, 28, 78, 255)], rim=GOLD,
                   hands=((0.42, -0.5), (math.cos(a) * 0.95, math.sin(a) * 0.95)), glow=GLOW[2])
        for k in range(4):
            kx = cx + 3.3 * math.cos(k / 4 * 2 * math.pi)
            ky = cy + 3.3 * math.sin(k / 4 * 2 * math.pi)
            s.set(int(round(kx)), int(round(ky)), GOLD[3])
        s.rect(7, 0, 8, 0, GOLD[3])
        s.set(7, 1, GOLD[1])
        s.set(8, 1, GOLD[2])
        s.outline()
        draw_ring(True, GLOW[1])
        for t0 in (phase, phase + math.pi):
            x, y = ring_point(t0)
            s.set(int(round(x)), int(round(y)), (255, 255, 255, 255) if math.sin(t0) > 0 else GLOW[2])
        strip.img.paste(s.img, (0, 16 * f))
    return strip


def heart_of_time():
    s = Sprite()
    heart = [(7.5, 14.5), (1.0, 7.5), (1.0, 4.0), (3.8, 1.6), (7.5, 4.0), (11.2, 1.6), (14.0, 4.0), (14.0, 7.5)]
    for y in range(16):
        for x in range(16):
            if inside(heart, x + 0.5, y + 0.5):
                light = ((7.5 - x) + (6 - y)) / 16
                i = int(round(1 + 2.6 * (0.5 + 0.5 * light)))
                s.set(x, y, GOLD[max(0, min(4, i))])
    for x, y in ((2, 7), (3, 8), (4, 9), (13, 7), (12, 8), (11, 9), (6, 12), (8, 12), (7, 13)):
        s.set(x, y, CHRON[2])
    clock_face(s, 7.5, 7.4, 3.7, face=[(28, 16, 50, 255), (50, 32, 86, 255)], rim=[GOLD[0], GOLD[1], GOLD[1], GOLD[2], GOLD[4]],
               hands=((0.0, -0.9), (0.7, 0.3)), glow=GLOW[2])
    for x, y in ((3, 3), (12, 3)):
        s.set(x, y, GOLD[4])
    s.outline()
    return s


def chronos_ring():
    """Band der schwebenden Ringe: Goldkanten, dunkles Glas, leuchtende Ziffernstriche (wiederholt sich)."""
    s = Sprite(64, 8)
    for x in range(64):
        s.set(x, 1, GOLD[2])
        s.set(x, 6, GOLD[1])
        for y in range(2, 6):
            s.set(x, y, (60, 30, 110, 150))
    for k in range(8):
        x0 = k * 8 + 3
        tall = k % 2 == 0
        for y in range(2 if tall else 3, 6 if tall else 5):
            s.set(x0, y, GLOW[2])
            if tall:
                s.set(x0 + 1, y, GLOW[1])
    for x in (0, 16, 32, 48):
        s.set(x, 0, GOLD[4])
        s.set(x, 7, GOLD[3])
    return s


def workbench_side(formed):
    s = Sprite()
    noise_fill(s, [(34, 26, 52, 255), (44, 34, 66, 255), (56, 44, 82, 255)], 71, weights=[2, 3, 1])
    s.rect(0, 0, 15, 1, BRASS[3])
    s.rect(0, 2, 15, 2, BRASS[1])
    s.rect(0, 14, 15, 15, BRASS[1])
    rune = GLOW[1] if formed else CHRON[1]
    for x in (3, 7, 11):
        s.line(x, 5, x + 2, 5, rune)
        s.line(x + 1, 6, x + 1, 10, rune)
        s.line(x, 11, x + 2, 11, rune)
    if formed:
        s.set(4, 8, GLOW[2])
        s.set(8, 8, GLOW[2])
        s.set(12, 8, GLOW[2])
    return s


def workbench_top(formed):
    s = Sprite()
    noise_fill(s, [(34, 26, 52, 255), (44, 34, 66, 255)], 73, weights=[3, 2])
    s.ring(7.5, 7.5, 6.0, 7.4, BRASS[2])
    s.ring(7.5, 7.5, 4.0, 4.7, GLOW[1] if formed else CHRON[1])
    for a in range(0, 360, 30):
        x = 7.5 + 5.3 * math.cos(math.radians(a))
        y = 7.5 + 5.3 * math.sin(math.radians(a))
        s.set(int(round(x)), int(round(y)), GLOW[2] if formed else CHRON[2])
    s.disc(7.5, 7.5, 2.2, GLOW[2] if formed else CHRON[2])
    s.disc(7.5, 7.5, 1.0, (255, 255, 255, 255) if formed else CHRON[3])
    return s


def workbench_stem():
    s = Sprite()
    noise_fill(s, [CHRON[0], CHRON[1], CHRON[2]], 75, weights=[2, 3, 1])
    for y in range(0, 16, 5):
        s.line(0, y, 15, y, BRASS[2])
    s.set(7, 7, GLOW[2])
    s.set(3, 2, GLOW[1])
    s.set(12, 11, GLOW[1])
    return s


def pillar_side():
    s = Sprite()
    s.rect(0, 0, 15, 15, BRASS[2])
    noise_fill(s, [BRASS[1], BRASS[2]], 81, 0, 0, 15, 15, weights=[1, 3])
    s.rect(0, 0, 15, 1, BRASS[3])
    s.rect(0, 14, 15, 15, BRASS[1])
    s.rect(3, 3, 12, 12, (24, 18, 36, 255))
    s.rect(5, 2, 10, 13, CHRON[1])
    s.rect(6, 3, 9, 12, CHRON[2])
    s.line(7, 4, 7, 11, GLOW[1])
    s.set(8, 6, GLOW[2])
    s.set(7, 9, GLOW[2])
    return s


def pillar_top():
    s = Sprite()
    noise_fill(s, [BRASS[2], BRASS[3]], 83, weights=[3, 1])
    s.disc(7.5, 7.5, 3.0, CHRON[1])
    s.disc(7.5, 7.5, 1.6, GLOW[1])
    s.set(7, 7, GLOW[2])
    return s


def pillar_core():
    s = Sprite()
    noise_fill(s, [CHRON[1], CHRON[2], CHRON[3]], 85, weights=[2, 3, 1])
    for x, y in ((3, 4), (10, 2), (6, 11), (13, 9)):
        s.set(x, y, GLOW[2])
    return s


# ------------------------------------------------------------------------------------------
# Stufe III: Chrono-Rüstung, Paradox-Zelle, Lebenszeit-Extraktor
# ------------------------------------------------------------------------------------------

ARM = [(26, 20, 38, 255), (42, 34, 60, 255), (62, 50, 88, 255), (88, 72, 120, 255)]
MAGENTA = [(120, 30, 110, 255), (200, 70, 190, 255), (255, 160, 240, 255)]


def shade_poly(s, poly, ramp=ARM):
    xs = [p[0] for p in poly]
    ys = [p[1] for p in poly]
    for y in range(min(ys), max(ys) + 1):
        for x in range(min(xs), max(xs) + 1):
            if inside(poly, x + 0.5, y + 0.5):
                f = ((x - min(xs)) / max(1, max(xs) - min(xs)) + (y - min(ys)) / max(1, max(ys) - min(ys))) / 2
                s.set(x, y, ramp[max(0, min(len(ramp) - 1, 3 - int(f * 3.2)))])


def chrono_helmet():
    s = Sprite()
    shade_poly(s, [(3, 4), (5, 2), (11, 2), (13, 4), (13, 11), (11, 11), (11, 7), (5, 7), (5, 11), (3, 11)])
    s.line(3, 7, 13, 7, BRASS[2])
    s.line(5, 2, 11, 2, BRASS[3])
    s.line(6, 8, 10, 8, GLOW[1])
    s.set(8, 4, GLOW[2])
    s.set(7, 4, GLOW[1])
    s.outline()
    return s


def chrono_chestplate():
    s = Sprite()
    shade_poly(s, [(1, 2), (6, 2), (7, 4), (9, 4), (10, 2), (15, 2), (15, 7), (12, 7), (12, 14), (4, 14), (4, 7), (1, 7)])
    s.line(4, 7, 4, 13, BRASS[1])
    s.line(12, 7, 12, 13, BRASS[1])
    s.line(1, 2, 6, 2, BRASS[3])
    s.line(10, 2, 15, 2, BRASS[3])
    s.rect(7, 7, 9, 9, BRASS[2])
    s.set(8, 8, GLOW[2])
    s.set(7, 8, GLOW[1])
    s.set(9, 8, GLOW[1])
    s.line(5, 11, 11, 11, CHRON[2])
    s.outline()
    return s


def chrono_leggings():
    s = Sprite()
    shade_poly(s, [(3, 2), (13, 2), (13, 14), (10, 14), (9, 6), (7, 6), (6, 14), (3, 14)])
    s.line(3, 2, 13, 2, BRASS[3])
    s.line(3, 3, 13, 3, BRASS[1])
    s.rect(4, 8, 5, 9, BRASS[2])
    s.rect(11, 8, 12, 9, BRASS[2])
    s.set(8, 4, GLOW[2])
    s.line(4, 12, 5, 12, GLOW[1])
    s.line(11, 12, 12, 12, GLOW[1])
    s.outline()
    return s


def chrono_boots():
    s = Sprite()
    shade_poly(s, [(2, 7), (6, 7), (6, 12), (7, 12), (7, 14), (1, 14), (1, 12), (2, 12)])
    shade_poly(s, [(10, 7), (14, 7), (14, 12), (15, 12), (15, 14), (9, 14), (9, 12), (10, 12)])
    s.line(2, 7, 6, 7, BRASS[3])
    s.line(10, 7, 14, 7, BRASS[3])
    s.line(1, 13, 7, 13, BRASS[1])
    s.line(9, 13, 15, 13, BRASS[1])
    s.set(4, 10, GLOW[2])
    s.set(12, 10, GLOW[2])
    s.outline()
    return s


def chronite_plate():
    s = Sprite()
    shade_poly(s, [(2, 4), (13, 2), (14, 11), (3, 13)], [CHRON[0], CHRON[1], CHRON[2], CHRON[3]])
    for x, y in ((3, 5), (12, 3), (13, 10), (4, 12)):
        s.set(x, y, BRASS[3])
    s.line(5, 6, 10, 5, GLOW[1])
    s.line(5, 9, 11, 8, CHRON[3])
    s.outline()
    return s


def chrono_upgrade_template():
    s = Sprite()
    s.rect(3, 1, 12, 14, (40, 34, 52, 255))
    s.rect(4, 2, 11, 13, (56, 48, 72, 255))
    s.rect(3, 1, 12, 1, (80, 70, 100, 255))
    s.rect(3, 14, 12, 14, (24, 20, 32, 255))
    clock_face(s, 7.5, 7.5, 3.3, face=[CHRON[1], CHRON[2]], rim=[GLOW[0], GLOW[0], GLOW[1], GLOW[2], GLOW[2]], hands=((0, -0.8), (0.6, 0.1)), glow=GLOW[2])
    s.set(5, 12, BRASS[3])
    s.set(10, 3, BRASS[3])
    s.outline()
    return s


def paradox_time_cell():
    s = cell([MAGENTA[0], MAGENTA[1], MAGENTA[2]], [CHRON[0], CHRON[1], CHRON[2], CHRON[3], CHRON[4]], big=True)
    for i, (x, y) in enumerate(((6, 6), (7, 7), (8, 6), (9, 8), (8, 9), (7, 10))):
        s.set(x, y, GLOW[2] if i % 2 else MAGENTA[2])
    return s


def extractor_side():
    s = Sprite()
    noise_fill(s, [(22, 18, 26, 255), (32, 26, 36, 255), (44, 36, 48, 255)], 91, weights=[2, 3, 1])
    s.rect(0, 0, 15, 1, BRASS[2])
    s.rect(0, 9, 15, 9, BRASS[1])
    for x in range(2, 15, 3):
        s.rect(x, 3, x, 7, (60, 14, 20, 255))
        s.set(x, 5, RED[1])
    return s


def extractor_top(active):
    s = Sprite()
    noise_fill(s, [(22, 18, 26, 255), (32, 26, 36, 255)], 93, weights=[3, 2])
    s.ring(7.5, 7.5, 5.6, 7.0, BRASS[1])
    glow = [MAGENTA[1], MAGENTA[2]] if active else [(70, 20, 50, 255), (100, 40, 80, 255)]
    # Sanduhr-Rune
    hour = [(4, 3), (11, 3), (8, 7.5), (11, 12), (4, 12), (7, 7.5)]
    for (x0, y0), (x1, y1) in zip(hour, hour[1:] + hour[:1]):
        s.line(int(x0), int(y0), int(x1), int(y1), glow[0])
    s.rect(6, 4, 9, 5, glow[1] if active else glow[0])
    s.rect(7, 10, 8, 11, glow[1])
    return s


def extractor_bottom():
    s = Sprite()
    noise_fill(s, [(18, 14, 22, 255), (26, 20, 30, 255)], 95)
    return s


def armor_layer(layer):
    s = Sprite(64, 32)

    def face(x0, y0, w, h, kind, seed):
        rnd = random.Random(seed)
        for y in range(h):
            for x in range(w):
                c = ARM[1 + (rnd.random() < 0.35) + (y == 0)]
                s.set(x0 + x, y0 + y, c)
        if kind == "front_head":
            s.line(x0 + 1, y0 + 3, x0 + w - 2, y0 + 3, GLOW[1])
            s.line(x0, y0, x0 + w - 1, y0, BRASS[3])
            s.set(x0 + w // 2, y0 + 1, GLOW[2])
        elif kind == "front_body":
            s.line(x0, y0, x0 + w - 1, y0, BRASS[3])
            s.rect(x0 + w // 2 - 1, y0 + 3, x0 + w // 2, y0 + 4, GLOW[1])
            s.set(x0 + w // 2 - 1, y0 + 3, GLOW[2])
            s.line(x0 + 1, y0 + 7, x0 + w - 2, y0 + 7, BRASS[1])
            s.line(x0, y0 + h - 1, x0 + w - 1, y0 + h - 1, BRASS[2])
        elif kind == "arm":
            s.line(x0, y0, x0 + w - 1, y0, BRASS[3])
            s.line(x0, y0 + 5, x0 + w - 1, y0 + 5, BRASS[1])
            s.set(x0 + w // 2, y0 + 2, GLOW[1])
        elif kind == "leg":
            s.line(x0, y0 + 4, x0 + w - 1, y0 + 4, BRASS[2])
            s.set(x0 + w // 2, y0 + 5, GLOW[1])
        elif kind == "boot":
            s.line(x0, y0, x0 + w - 1, y0, BRASS[3])
            s.set(x0 + w // 2, y0 + 1, GLOW[2])
        elif kind == "belt":
            s.line(x0, y0, x0 + w - 1, y0, BRASS[3])
            s.line(x0, y0 + 1, x0 + w - 1, y0 + 1, BRASS[1])
            s.set(x0 + w // 2, y0 + 2, GLOW[2])

    def cube(u, v, w, h, d, front_kind, side_kind, rows=None, seed=0):
        """Vanilla-Würfelabwicklung; rows=(von, bis) begrenzt auf einen Teil der Höhe."""
        y_from, y_to = rows if rows else (0, h)
        hh = y_to - y_from
        if rows is None:
            face(u + d, v, w, d, "top", seed)
            face(u + d + w, v, w, d, "top", seed + 1)
        face(u, v + d + y_from, d, hh, side_kind, seed + 2)
        face(u + d, v + d + y_from, w, hh, front_kind, seed + 3)
        face(u + d + w, v + d + y_from, d, hh, side_kind, seed + 4)
        face(u + d + w + d, v + d + y_from, w, hh, side_kind, seed + 5)

    if layer == 1:
        cube(0, 0, 8, 8, 8, "front_head", "side", seed=10)
        cube(16, 16, 8, 12, 4, "front_body", "side", seed=20)
        cube(40, 16, 4, 12, 4, "arm", "arm", seed=30)
        cube(0, 16, 4, 12, 4, "boot", "boot", rows=(7, 12), seed=40)
    else:
        cube(16, 16, 8, 12, 4, "belt", "belt", rows=(8, 12), seed=50)
        cube(0, 16, 4, 12, 4, "leg", "leg", seed=60)
    return s


# ------------------------------------------------------------------------------------------
# Strukturen: Boss-Altäre, Zeitkern-Fragment
# ------------------------------------------------------------------------------------------

BLACKSTONE = [(24, 20, 26, 255), (36, 30, 40, 255), (50, 42, 54, 255)]
SCULK = [(8, 30, 40, 255), (14, 52, 64, 255), (40, 140, 150, 255)]


def time_core_fragment():
    s = Sprite()
    shard = [(4, 3), (11, 2), (13, 8), (8, 14), (3, 9)]
    for y in range(16):
        for x in range(16):
            if inside(shard, x + 0.5, y + 0.5):
                d = math.hypot(x - 8, y - 7)
                s.set(x, y, GLOW[2] if d < 2 else (GLOW[1] if d < 3.5 else CHRON[2] if (x + y) % 3 else CHRON[1]))
    for (x0, y0), (x1, y1) in zip(shard, shard[1:] + shard[:1]):
        s.line(x0, y0, x1, y1, BRASS[2])
    s.set(8, 7, (255, 255, 255, 255))
    s.outline()
    return s


def altar_side(kind):
    s = Sprite()
    if kind == "clockmaker":
        noise_fill(s, BLACKSTONE, 101, weights=[2, 3, 1])
        s.rect(0, 0, 15, 1, BRASS[3])
        s.rect(0, 14, 15, 15, BRASS[1])
        gear(s, 7.5, 8, 4.2, BRASS, teeth=8, hole=1.4)
        s.set(7, 8, GLOW[2])
    else:
        noise_fill(s, DEEP, 103, weights=[2, 3, 2, 1])
        for y in range(16):
            for x in range(16):
                if (x * 7 + y * 3) % 11 == 0 or (y > 10 and (x + y) % 4 == 0):
                    s.set(x, y, SCULK[1])
        s.rect(0, 0, 15, 1, SCULK[0])
        s.line(7, 3, 7, 12, SCULK[2])
        s.line(8, 3, 8, 12, CHRON[2])
        s.set(7, 7, GLOW[2])
    return s


def altar_top(kind):
    s = Sprite()
    if kind == "clockmaker":
        noise_fill(s, BLACKSTONE, 105)
        clock_face(s, 7.5, 7.5, 6.0, face=[(200, 196, 180, 255), (232, 226, 210, 255)], hands=((0, -0.9), (-0.55, -0.45)), glow=OUT)
    else:
        noise_fill(s, SCULK[:2] + [DEEP[1]], 107, weights=[2, 2, 1])
        for x, y in ((3, 3), (12, 3), (3, 12), (12, 12)):
            s.rect(x - 1, y - 1, x + 1, y + 1, (10, 10, 14, 255))
            s.set(x, y, CHRON[1])
        s.disc(7.5, 7.5, 2.4, (10, 10, 14, 255))
        s.disc(7.5, 7.5, 1.3, MAGENTA[1])
        s.set(7, 7, GLOW[2])
    return s


def altar_bottom(kind):
    s = Sprite()
    noise_fill(s, BLACKSTONE if kind == "clockmaker" else DEEP, 109)
    return s


# ------------------------------------------------------------------------------------------
# Bosse (128x128, Würfel-Abwicklung wie im Modell)
# ------------------------------------------------------------------------------------------

STEEL = [(40, 42, 50, 255), (62, 64, 74, 255), (88, 90, 100, 255), (120, 122, 134, 255)]
CHITIN = [(18, 10, 24, 255), (30, 16, 40, 255), (44, 24, 58, 255), (60, 34, 80, 255)]
FLESH = [(70, 12, 40, 255), (100, 20, 60, 255), (140, 40, 90, 255)]


class Cube:
    """Flächen eines Modellwürfels in der Textur (Vanilla-Abwicklung)."""

    def __init__(self, u, v, w, h, d):
        self.faces = {
            "top": (u + d, v, w, d), "bottom": (u + d + w, v, w, d),
            "right": (u, v + d, d, h), "front": (u + d, v + d, w, h),
            "left": (u + d + w, v + d, d, h), "back": (u + 2 * d + w, v + d, w, h),
        }

    def each(self, names=None):
        for name, rect in self.faces.items():
            if names is None or name in names:
                yield name, rect


def fill_face(s, rect, ramp, seed, weights=None):
    x, y, w, h = rect
    noise_fill(s, ramp, seed, x, y, x + w - 1, y + h - 1, weights)


def clockmaker_textures():
    s = Sprite(128, 128)
    g = Sprite(128, 128)
    body = Cube(0, 0, 18, 20, 10)
    for name, rect in body.each():
        fill_face(s, rect, BRASS[1:4], 201 + len(name), [2, 3, 1])
        x, y, w, h = rect
        for sx in range(x + 5, x + w - 1, 6):
            s.line(sx, y, sx, y + h - 1, BRASS[0])
        for rx, ry in ((x + 1, y + 1), (x + w - 2, y + 1), (x + 1, y + h - 2), (x + w - 2, y + h - 2)):
            s.set(rx, ry, BRASS[4])
    fx, fy, fw, fh = body.faces["front"]
    s.rect(fx + 3, fy + 3, fx + 14, fy + 14, (24, 22, 30, 255))
    s.rect(fx + 2, fy + 2, fx + 15, fy + 2, BRASS[4])
    for gx, gy in ((fx + 1, fy + 17), (fx + 16, fy + 17)):
        s.set(gx, gy, GLOW[1])
        g.set(gx, gy, GLOW[2])
    head = Cube(56, 0, 12, 12, 12)
    for name, rect in head.each():
        fill_face(s, rect, BRASS[1:4], 211 + len(name), [2, 3, 2])
    hx, hy, hw, hh = head.faces["front"]
    s.rect(hx + 1, hy + 4, hx + hw - 2, hy + 5, (16, 14, 22, 255))
    for i in range(hx + 1, hx + hw - 1):
        g.set(i, hy + 4, GLOW[1])
    for i in range(3):
        s.line(hx + 3, hy + 8 + i, hx + hw - 4, hy + 8 + i, BRASS[0] if i % 2 == 0 else BRASS[1])
    s.rect(hx, hy + hh - 1, hx + hw - 1, hy + hh - 1, BRASS[0])
    for cube_uv in ((0, 30), (24, 30)):
        arm = Cube(cube_uv[0], cube_uv[1], 6, 22, 6)
        for name, rect in arm.each():
            fill_face(s, rect, STEEL, 221 + cube_uv[0] + len(name), [1, 3, 2, 1])
            x, y, w, h = rect
            if name not in ("top", "bottom"):
                s.rect(x, y, x + w - 1, y + 1, BRASS[3])
                s.rect(x, y + 10, x + w - 1, y + 11, BRASS[2])
                s.rect(x, y + 16, x + w - 1, y + h - 1, BRASS[2])
                s.set(x + w // 2, y + 5, GLOW[1])
                g.set(x + w // 2, y + 5, GLOW[2])
    for cube_uv in ((48, 30), (72, 30)):
        leg = Cube(cube_uv[0], cube_uv[1], 6, 20, 6)
        for name, rect in leg.each():
            fill_face(s, rect, STEEL, 231 + cube_uv[0] + len(name), [1, 3, 2, 1])
            x, y, w, h = rect
            if name not in ("top", "bottom"):
                s.rect(x, y + 8, x + w - 1, y + 10, BRASS[3])
                s.rect(x, y + h - 4, x + w - 1, y + h - 1, (30, 30, 36, 255))
    # Brustklappe mit Zifferblatt
    door = Cube(96, 24, 10, 10, 1)
    for name, rect in door.each():
        fill_face(s, rect, BRASS[2:4], 241, [2, 1])
    dx, dy, dw, dh = door.faces["front"]
    sub = Sprite(10, 10)
    clock_face(sub, 4.5, 4.5, 4.6, hands=((0, -0.9), (0.6, 0.2)))
    s.img.alpha_composite(sub.img, (dx, dy))
    # Kern (leuchtet), Monokel, Krone
    core = Cube(104, 0, 6, 6, 3)
    for name, rect in core.each():
        fill_face(s, rect, [GLOW[0], GLOW[1]], 251, [1, 2])
        fill_face(g, rect, [GLOW[1], GLOW[2], (255, 255, 255, 255)], 252, [2, 2, 1])
    mono = Cube(104, 10, 3, 3, 1)
    for name, rect in mono.each():
        fill_face(s, rect, [BRASS[3]], 253)
    mx, my, mw, mh = mono.faces["front"]
    s.set(mx + 1, my + 1, GLOW[2])
    g.set(mx + 1, my + 1, (255, 255, 255, 255))
    for xx in range(mx, mx + 3):
        for yy in range(my, my + 3):
            if (xx, yy) != (mx + 1, my + 1):
                g.set(xx, yy, GLOW[1])
    crown = Cube(112, 10, 2, 4, 2)
    for name, rect in crown.each():
        fill_face(s, rect, [BRASS[3], BRASS[4]], 255)
    # Zahnrad-Säge (Vorder- und Rückseite)
    saw = Cube(0, 60, 12, 12, 1)
    for name in ("front", "back"):
        x, y, w, h = saw.faces[name]
        sub = Sprite(12, 12)
        gear(sub, 5.5, 5.5, 5.8, BRASS, teeth=10, hole=1.0)
        sub.ring(5.5, 5.5, 2.4, 3.0, BRASS[0])
        s.img.alpha_composite(sub.img, (x, y))
    for name in ("top", "bottom", "left", "right"):
        fill_face(s, saw.faces[name], [BRASS[2]], 256)
    return s, g


def chronophage_textures():
    s = Sprite(128, 128)
    g = Sprite(128, 128)
    upper = Cube(0, 0, 24, 10, 24)
    for name, rect in upper.each():
        fill_face(s, rect, CHITIN, 301 + len(name), [2, 3, 2, 1])
        x, y, w, h = rect
        rnd = random.Random(310 + len(name))
        for _ in range(w * h // 40):
            vx, vy = x + rnd.randrange(w), y + rnd.randrange(h)
            s.set(vx, vy, MAGENTA[0])
            if rnd.random() < 0.3:
                g.set(vx, vy, MAGENTA[1])
    # Zifferblatt auf dem Panzer
    tx, ty, tw, th = upper.faces["top"]
    for yy in range(th):
        for xx in range(tw):
            d = math.hypot(xx - 11.5, yy - 11.5)
            a = math.degrees(math.atan2(yy - 11.5, xx - 11.5))
            if 9.6 <= d <= 10.6:
                s.set(tx + xx, ty + yy, BRASS[1])
            if 7.5 <= d <= 9.2 and abs(((a + 360 + 15) % 30) - 15) < 4:
                s.set(tx + xx, ty + yy, MAGENTA[1])
                g.set(tx + xx, ty + yy, MAGENTA[2])
    for i in range(1, 8):
        s.set(tx + 11, ty + 11 - i, BRASS[2])
        s.set(tx + 11 + i // 2, ty + 11 + i // 3, BRASS[2])
    fill_face(s, upper.faces["bottom"], FLESH, 320, [2, 2, 1])
    lower = Cube(0, 34, 22, 6, 23)
    for name, rect in lower.each():
        fill_face(s, rect, CHITIN, 331 + len(name), [2, 3, 2, 1])
    fill_face(s, lower.faces["top"], FLESH, 340, [2, 3, 1])
    core = Cube(96, 0, 8, 8, 8)
    for name, rect in core.each():
        x, y, w, h = rect
        for yy in range(h):
            for xx in range(w):
                d = math.hypot(xx - (w - 1) / 2, yy - (h - 1) / 2)
                c = (255, 255, 255, 255) if d < 1.2 else MAGENTA[2] if d < 2.4 else MAGENTA[1] if d < 3.4 else MAGENTA[0]
                s.set(x + xx, y + yy, c)
                g.set(x + xx, y + yy, c)
        s.line(x + w // 2, y + 1, x + w // 2, y + h // 2, (20, 10, 20, 255))
    tooth = Cube(96, 16, 2, 4, 2)
    for name, rect in tooth.each():
        fill_face(s, rect, [(230, 222, 200, 255), (200, 190, 168, 255)], 350)
    orb = Cube(96, 24, 3, 3, 3)
    for name, rect in orb.each():
        fill_face(s, rect, [GLOW[1], CHRON[2]], 360)
        fill_face(g, rect, [GLOW[2], CHRON[3]], 361)
    for u in (96, 104):
        tendril = Cube(u, 32, 2, 7, 2)
        for name, rect in tendril.each():
            fill_face(s, rect, CHITIN[1:], 370 + u, [2, 2, 1])
            x, y, w, h = rect
            if name not in ("top", "bottom") and u == 104:
                for xx in range(x, x + w):
                    g.set(xx, y + h - 1, MAGENTA[2])
                    s.set(xx, y + h - 1, MAGENTA[1])
    return s, g


# ------------------------------------------------------------------------------------------
# Handbuch „Die Chronik“
# ------------------------------------------------------------------------------------------

LEATHER = [(46, 22, 44, 255), (60, 30, 58, 255), (74, 38, 70, 255)]
PARCHMENT = (236, 222, 190, 255)


def chronik_item():
    s = Sprite()
    s.rect(2, 1, 13, 14, LEATHER[1])
    s.rect(3, 2, 12, 13, LEATHER[2])
    s.rect(2, 1, 3, 14, LEATHER[0])
    s.rect(13, 2, 14, 14, PARCHMENT)
    s.line(13, 2, 13, 14, (200, 184, 150, 255))
    clock_face(s, 8.0, 7.5, 3.6, hands=((0, -0.8), (0.5, 0.2)))
    for x, y in ((3, 1), (12, 1), (3, 14), (12, 14)):
        s.set(x, y, BRASS[3])
    s.outline()
    return s


def chronik_gui():
    w, h = 292, 180
    img = Image.new("RGBA", (512, 256), T)
    px = img.load()
    rnd = random.Random(501)
    for y in range(h):
        for x in range(w):
            corner = min(x, w - 1 - x) + min(y, h - 1 - y)
            if corner < 3:
                continue
            px[x, y] = LEATHER[rnd.choice((0, 1, 1, 2))]
    for (x0, y0) in ((0, 0), (w - 14, 0), (0, h - 14), (w - 14, h - 14)):
        for y in range(14):
            for x in range(14):
                if (x < 3 or y < 3 or x > 10 or y > 10) and px[x0 + x, y0 + y][3] > 0:
                    px[x0 + x, y0 + y] = BRASS[2] if (x + y) % 5 else BRASS[3]
    for page_x0, page_x1 in ((12, 145), (147, 280)):
        for y in range(8, h - 8):
            for x in range(page_x0, page_x1):
                spine = abs(x - 146) / 20.0
                shade = int(18 * max(0.0, 1.0 - spine))
                edge = 6 if (y < 10 or y > h - 11) else 0
                n = rnd.randint(-4, 4)
                r, g, b, a = PARCHMENT
                px[x, y] = (r - shade - edge + n, g - shade - edge + n, b - shade - edge + n, 255)
        # blasses Zifferblatt als Wasserzeichen
        cx = (page_x0 + page_x1) / 2
        cy = h / 2 + 6
        for y in range(h):
            for x in range(page_x0, page_x1):
                d = math.hypot(x - cx, y - cy)
                if 38 <= d <= 40 or (d < 36 and (int(math.degrees(math.atan2(y - cy, x - cx)) + 360) % 30) < 2 and d > 30):
                    r, g, b, a = px[x, y]
                    px[x, y] = (r - 10, g - 10, b - 6, 255)
        for x in range(page_x0 + 8, page_x1 - 8):
            r, g, b, a = px[x, 11]
            px[x, 11] = (min(255, r - 40), min(255, g - 55), b - 90, 255)
    for y in range(8, h - 8):
        px[146, y] = (120, 96, 70, 255)
    return img


# ------------------------------------------------------------------------------------------
# Oberfläche
# ------------------------------------------------------------------------------------------

def clockmaker_gui():
    img = Image.new("RGBA", (256, 256), T)
    px = img.load()
    bg, light, dark, slot = (198, 198, 198, 255), (255, 255, 255, 255), (85, 85, 85, 255), (139, 139, 139, 255)
    w, h = 176, 166
    for y in range(h):
        for x in range(w):
            px[x, y] = bg
    for x in range(w):
        px[x, 0] = (0, 0, 0, 255)
        px[x, h - 1] = (0, 0, 0, 255)
        px[x, 1] = light
        px[x, h - 2] = dark
    for y in range(h):
        px[0, y] = (0, 0, 0, 255)
        px[w - 1, y] = (0, 0, 0, 255)
        px[1, y] = light
        px[w - 2, y] = dark
    # Messing-Akzent oben
    for x in range(4, w - 4):
        for y in (3, 4):
            px[x, y] = BRASS[2] if (x // 6) % 2 else BRASS[3]

    def draw_slot(x0, y0, size=18):
        for y in range(y0, y0 + size):
            for x in range(x0, x0 + size):
                px[x, y] = slot
        for i in range(size):
            px[x0 + i, y0] = dark
            px[x0, y0 + i] = dark
            px[x0 + i, y0 + size - 1] = light
            px[x0 + size - 1, y0 + i] = light

    for row in range(3):
        for col in range(3):
            draw_slot(29 + col * 18, 16 + row * 18)
    draw_slot(119, 30, 26)
    # Pfeil aus Zahnrädern
    for i in range(16):
        for y in range(33, 41):
            if abs(y - 37) <= max(0, (i - 8)) // 1 and i >= 8 or (35 <= y <= 39 and i < 10):
                px[89 + i, y] = BRASS[2]
    for row in range(3):
        for col in range(9):
            draw_slot(7 + col * 18, 83 + row * 18)
    for col in range(9):
        draw_slot(7 + col * 18, 141)
    return img


def temporal_workbench_gui():
    img = Image.new("RGBA", (256, 256), T)
    px = img.load()
    bg, light, dark, slot = (198, 198, 198, 255), (255, 255, 255, 255), (85, 85, 85, 255), (139, 139, 139, 255)
    w, h = 176, 166
    for y in range(h):
        for x in range(w):
            px[x, y] = bg
    for x in range(w):
        px[x, 0] = (0, 0, 0, 255)
        px[x, h - 1] = (0, 0, 0, 255)
        px[x, 1] = light
        px[x, h - 2] = dark
    for y in range(h):
        px[0, y] = (0, 0, 0, 255)
        px[w - 1, y] = (0, 0, 0, 255)
        px[1, y] = light
        px[w - 2, y] = dark
    for x in range(4, w - 4):
        for y in (3, 4):
            px[x, y] = CHRON[2] if (x // 6) % 2 else GLOW[1]

    def draw_slot(x0, y0, size=18):
        for y in range(y0, y0 + size):
            for x in range(x0, x0 + size):
                px[x, y] = slot
        for i in range(size):
            px[x0 + i, y0] = dark
            px[x0, y0 + i] = dark
            px[x0 + i, y0 + size - 1] = light
            px[x0 + size - 1, y0 + i] = light

    # Ringe um den Werkzeugplatz
    for y in range(10, 64):
        for x in range(4, 50):
            d = math.hypot(x - 25, y - 43)
            if 15.5 <= d <= 16.5:
                px[x, y] = BRASS[2]
            elif 12.0 <= d <= 12.6 and (int(math.degrees(math.atan2(y - 43, x - 25))) // 15) % 2 == 0:
                px[x, y] = CHRON[2]
    draw_slot(16, 34)
    # Verbindungslinien zu den Modulplätzen
    for i, my in enumerate((16, 34, 52)):
        y = my + 8
        for x in range(35, 52):
            px[x, y] = GLOW[0] if i != 1 else GLOW[1]
        draw_slot(52, my)
    # Werte-Feld
    for y in range(16, 71):
        for x in range(76, 169):
            px[x, y] = (30, 26, 44, 255)
    for x in range(76, 169):
        px[x, 16] = BRASS[1]
        px[x, 70] = BRASS[3]
    for y in range(16, 71):
        px[76, y] = BRASS[1]
        px[168, y] = BRASS[3]
    for row in range(3):
        for col in range(9):
            draw_slot(7 + col * 18, 83 + row * 18)
    for col in range(9):
        draw_slot(7 + col * 18, 141)
    # Schloss-Kachel für gesperrte Modulplätze (bei 176,0)
    for y in range(18):
        for x in range(18):
            px[176 + x, y] = (60, 56, 70, 255)
    for i in range(18):
        px[176 + i, 0] = dark
        px[176, i] = dark
        px[176 + i, 17] = light
        px[193, i] = light
    for y in range(7, 13):
        for x in range(182, 188):
            px[x, y] = BRASS[2]
    for x in (183, 186):
        for y in range(4, 7):
            px[x, y] = IRON[2]
    px[184, 4] = IRON[2]
    px[185, 4] = IRON[2]
    px[184, 9] = (30, 20, 30, 255)
    px[185, 9] = (30, 20, 30, 255)
    px[184, 10] = (30, 20, 30, 255)
    return img


# ------------------------------------------------------------------------------------------

ITEMS = {
    "brass_ingot": brass_ingot, "brass_nugget": brass_nugget, "brass_gear": brass_gear, "mainspring": mainspring,
    "balance_wheel": balance_wheel, "clockwork": clockwork, "raw_chronite": raw_chronite, "chronite_crystal": chronite_crystal,
    "time_dust": time_dust, "chronite_seed": chronite_seed, "pocket_watch": pocket_watch, "growth_hourglass": growth_hourglass,
    "brake_spring": brake_spring, "moment_lens": moment_lens, "recall_watch": recall_watch, "stasis_watch": stasis_watch,
    "time_accelerator": time_accelerator, "slow_pendulum": slow_pendulum, "rewind_spool": rewind_spool, "chrono_knife": chrono_knife,
    "pocket_time_cell": pocket_time_cell, "chronite_time_cell": chronite_time_cell,
    "clockwork_core": clockwork_core, "phage_heart": phage_heart,
    "chrono_helmet": chrono_helmet, "chrono_chestplate": chrono_chestplate, "chrono_leggings": chrono_leggings, "chrono_boots": chrono_boots,
    "chronite_plate": chronite_plate, "time_core_fragment": time_core_fragment, "chronik": chronik_item, "chrono_upgrade_template": chrono_upgrade_template, "paradox_time_cell": paradox_time_cell,
    "chronos_watch": chronos_watch, "heart_of_time": heart_of_time,
    "blank_module": lambda: module("blank"),
    **{f"filter_{n}": (lambda n=n: module("filter", n)) for n in
       ("animals", "monsters", "villagers", "players", "projectiles", "items", "vehicles", "growth", "environment", "fluids", "redstone", "machines")},
    **{f"{n}_module": (lambda n=n: module("amp", n)) for n in ("radius", "duration", "strength", "efficiency")},
}

BLOCKS = {
    "brass_block": brass_block, "time_sand": time_sand, "time_glass": time_glass,
    "deepslate_chronite_ore": deepslate_chronite_ore, "deepslate_chronite_ore_phased": deepslate_chronite_ore_phased,
    "clockmaker_bench_top": bench_top, "clockmaker_bench_side": bench_side, "clockmaker_bench_front": bench_front,
    "time_condenser_side": condenser_side, "time_condenser_top": condenser_top, "time_condenser_base": condenser_base,
    "time_field_projector_top": lambda: projector("top", False), "time_field_projector_side": lambda: projector("side", False),
    "time_field_projector_bottom": lambda: projector("bottom", False),
    "time_field_projector_top_active": lambda: projector("top", True), "time_field_projector_side_active": lambda: projector("side", True),
    **{f"chronite_sprout_stage{i}": (lambda i=i: chronite_sprout(i)) for i in range(4)},
    "temporal_workbench_side": lambda: workbench_side(False), "temporal_workbench_side_formed": lambda: workbench_side(True),
    "temporal_workbench_top": lambda: workbench_top(False), "temporal_workbench_top_formed": lambda: workbench_top(True),
    "temporal_workbench_stem": workbench_stem,
    "chrono_pillar_side": pillar_side, "chrono_pillar_top": pillar_top, "chrono_pillar_core": pillar_core,
    "lifetime_extractor_side": extractor_side, "lifetime_extractor_top": lambda: extractor_top(False),
    "lifetime_extractor_top_active": lambda: extractor_top(True), "lifetime_extractor_bottom": extractor_bottom,
    **{f"{k}_altar_{part}": (lambda k=k, f=f: f(k)) for k in ("clockmaker", "phage")
       for part, f in (("side", altar_side), ("top", altar_top), ("bottom", altar_bottom))},
}


def main(preview=None):
    sprites = []
    for name, fn in ITEMS.items():
        s = fn()
        s.save(TEX / "item" / f"{name}.png")
        if s.h > 16:
            # Animierte Textur: Streifen aus Einzelbildern
            (TEX / "item" / f"{name}.png.mcmeta").write_text('{"animation": {"frametime": 3}}\n')
            sprites.append((name, s.img.crop((0, 0, 16, 16))))
            continue
        sprites.append((name, s.img))
    for name, fn in BLOCKS.items():
        s = fn()
        s.save(TEX / "block" / f"{name}.png")
        sprites.append((name, s.img))
    GUI.mkdir(parents=True, exist_ok=True)
    clockmaker_gui().save(GUI / "clockmaker_bench.png")
    temporal_workbench_gui().save(GUI / "temporal_workbench.png")
    chronik_gui().save(GUI.parent / "chronik.png")
    chronos_ring().save(TEX / "misc" / "chronos_ring.png")
    entity_dir = TEX / "entity"
    for name, fn in (("clockmaker", clockmaker_textures), ("chronophage", chronophage_textures)):
        tex, glow = fn()
        tex.save(entity_dir / f"{name}.png")
        glow.save(entity_dir / f"{name}_glow.png")
    armor_dir = ROOT / "src/main/resources/assets/minecraft/textures/models/armor"
    for layer in (1, 2):
        armor_layer(layer).save(armor_dir / f"timemod_chrono_layer_{layer}.png")
    print(f"{len(sprites)} Texturen geschrieben")
    if preview:
        cols = 10
        scale = 6
        rows = (len(sprites) + cols - 1) // cols
        sheet = Image.new("RGBA", (cols * 16 * scale + cols * 4, rows * 16 * scale + rows * 4), (60, 60, 70, 255))
        for i, (name, img) in enumerate(sprites):
            x = (i % cols) * (16 * scale + 4)
            y = (i // cols) * (16 * scale + 4)
            sheet.alpha_composite(img.resize((16 * scale, 16 * scale), Image.NEAREST), (x, y))
        sheet.save(preview)


if __name__ == "__main__":
    main(sys.argv[1] if len(sys.argv) > 1 else None)
