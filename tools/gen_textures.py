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


# ------------------------------------------------------------------------------------------

ITEMS = {
    "brass_ingot": brass_ingot, "brass_nugget": brass_nugget, "brass_gear": brass_gear, "mainspring": mainspring,
    "balance_wheel": balance_wheel, "clockwork": clockwork, "raw_chronite": raw_chronite, "chronite_crystal": chronite_crystal,
    "time_dust": time_dust, "chronite_seed": chronite_seed, "pocket_watch": pocket_watch, "growth_hourglass": growth_hourglass,
    "brake_spring": brake_spring, "moment_lens": moment_lens, "recall_watch": recall_watch, "stasis_watch": stasis_watch,
    "time_accelerator": time_accelerator, "slow_pendulum": slow_pendulum, "rewind_spool": rewind_spool, "chrono_knife": chrono_knife,
    "pocket_time_cell": pocket_time_cell, "chronite_time_cell": chronite_time_cell,
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
}


def main(preview=None):
    sprites = []
    for name, fn in ITEMS.items():
        s = fn()
        s.save(TEX / "item" / f"{name}.png")
        sprites.append((name, s.img))
    for name, fn in BLOCKS.items():
        s = fn()
        s.save(TEX / "block" / f"{name}.png")
        sprites.append((name, s.img))
    GUI.mkdir(parents=True, exist_ok=True)
    clockmaker_gui().save(GUI / "clockmaker_bench.png")
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
