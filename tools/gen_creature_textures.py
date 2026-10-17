"""Texturen der Wesen der Zukunft (ab M8d): Zeitwächter, Zeitsiegel, Zeigerklinge, Dämpfungsmodul.

Aufruf: python3 -I tools/gen_creature_textures.py [vorschau.png]   (läuft auch am Ende von gen_textures.py)
Baut auf den Werkzeugen von gen_textures.py auf (Sprite, Cube, Paletten).
"""
import math
import random
import sys
from pathlib import Path

from PIL import Image

sys.path.insert(0, str(Path(__file__).resolve().parent))
import gen_textures as g  # noqa: E402
from gen_textures import BRASS, FACE, GLOW, T, Cube, Sprite, fill_face  # noqa: E402


TEX = g.TEX
COAT = [(14, 17, 32, 255), (22, 27, 48, 255), (31, 38, 64, 255), (44, 54, 86, 255)]
SILVER = [(96, 104, 124, 255), (146, 154, 172, 255), (196, 204, 220, 255), (236, 240, 248, 255)]
GLOVE = [(20, 18, 26, 255), (34, 30, 42, 255)]
WAX = [(46, 20, 82, 255), (74, 34, 128, 255), (104, 56, 170, 255), (140, 96, 210, 255)]
WHITE = (255, 255, 255, 255)

def paste(s, img, at):
    s.img.paste(img, at)


# ------------------------------------------------------------------------------------------
# Zeitwächter (128×128, Abwicklung wie TimeWardenModel)
# ------------------------------------------------------------------------------------------

def coat_face(s, rect, seed, hem=False):
    fill_face(s, rect, COAT[1:], seed, [3, 4, 1])
    x, y, w, h = rect
    rnd = random.Random(seed)
    # Falten: senkrechte dunkle Linien mit hellem Saum daneben
    for fx in range(x + 1 + rnd.randrange(2), x + w - 1, 3):
        for yy in range(y + 1, y + h):
            if rnd.random() < 0.8:
                s.set(fx, yy, COAT[0])
                if fx + 1 < x + w:
                    s.set(fx + 1, yy, COAT[3] if rnd.random() < 0.35 else s.get(fx + 1, yy))
    if hem:
        s.rect(x, y + h - 1, x + w - 1, y + h - 1, SILVER[1])
        s.rect(x, y + h - 2, x + w - 1, y + h - 2, COAT[0])


def time_warden_textures():
    s = Sprite(128, 128)
    glow = Sprite(128, 128)

    # Kopf: dunkle Kapuze mit Silberpaspel (die Vorderseite verdeckt die Maske)
    head = Cube(0, 0, 8, 8, 8)
    for name, rect in head.each():
        fill_face(s, rect, COAT[1:], 501 + len(name), [2, 3, 1])
        x, y, w, h = rect
        if name in ("right", "left", "back"):
            s.rect(x, y, x + w - 1, y, SILVER[1])
        if name == "back":
            s.rect(x + 3, y + 2, x + 4, y + h - 1, COAT[0])
    hx, hy, hw, hh = head.faces["front"]
    s.rect(hx, hy, hx + hw - 1, hy + hh - 1, COAT[0])

    # Maske: ein Zifferblatt, „fünf vor zwölf“, die Augen leuchten bei zehn und zwei Uhr
    mask = Cube(32, 0, 8, 8, 1)
    for name, rect in mask.each():
        fill_face(s, rect, [BRASS[2], BRASS[3]], 511, [2, 1])
    mx, my, mw, mh = mask.faces["front"]
    for yy in range(8):
        for xx in range(8):
            d = math.hypot(xx - 3.5, yy - 3.5)
            if d <= 3.2:
                s.set(mx + xx, my + yy, FACE[1] if (xx + yy) % 5 else FACE[0])
            elif d <= 4.3:
                s.set(mx + xx, my + yy, BRASS[2] if xx + yy < 7 else BRASS[1])
    for ex, ey in ((1, 2), (6, 2)):
        s.set(mx + ex, my + ey, GLOW[1])
        glow.set(mx + ex, my + ey, GLOW[2])
    s.set(mx + 3, my + 4, (40, 30, 30, 255))
    for xx, yy in ((3, 3), (3, 2), (3, 1)):
        s.set(mx + xx, my + yy, (30, 24, 40, 255))
    s.set(mx + 2, my + 3, (30, 24, 40, 255))
    s.set(mx + 4, my + 6, (120, 110, 100, 255))
    s.set(mx + 3, my + 0, BRASS[4])
    glow.set(mx + 3, my + 1, GLOW[1])

    # Heiligenschein: Ring aus Licht mit römischen Ziffern, Mitte durchsichtig
    halo = Cube(64, 0, 16, 16, 0)
    ax, ay, aw, ah = halo.faces["front"]
    ring = Sprite(16, 16)
    ring_glow = Sprite(16, 16)
    for yy in range(16):
        for xx in range(16):
            d = math.hypot(xx - 7.5, yy - 7.5)
            if 6.6 <= d <= 7.6:
                ring.set(xx, yy, (150, 235, 255, 255))
                ring_glow.set(xx, yy, (170, 245, 255, 255))
            elif 5.2 <= d < 6.6:
                ang = math.atan2(yy - 7.5, xx - 7.5)
                hour = (ang + math.pi / 2) / (2 * math.pi) * 12
                if abs(hour - round(hour)) < 0.18:
                    ring.set(xx, yy, (236, 222, 255, 255))
                    ring_glow.set(xx, yy, (236, 222, 255, 255))
    # Die großen Ziffern XII, III, VI, IX als helle Punkte außen
    for hour in (0, 3, 6, 9):
        ang = hour / 12 * 2 * math.pi - math.pi / 2
        px, py = 7.5 + math.cos(ang) * 7.1, 7.5 + math.sin(ang) * 7.1
        ring.set(int(round(px)), int(round(py)), WHITE)
        ring_glow.set(int(round(px)), int(round(py)), WHITE)
    paste(s, ring.img, (ax, ay))
    paste(glow, ring_glow.img, (ax, ay))
    bx, by, _, _ = halo.faces["back"]
    paste(s, ring.img.transpose(Image.FLIP_LEFT_RIGHT), (bx, by))
    paste(glow, ring_glow.img.transpose(Image.FLIP_LEFT_RIGHT), (bx, by))

    # Rumpf: Mantel mit Silbernaht, Messingknöpfen und leuchtender Sanduhr auf der Brust
    body = Cube(0, 16, 8, 13, 4)
    for name, rect in body.each():
        coat_face(s, rect, 521 + len(name))
    fx, fy, fw, fh = body.faces["front"]
    s.rect(fx + 3, fy, fx + 4, fy + fh - 1, COAT[0])
    s.rect(fx + 3, fy, fx + 3, fy + fh - 1, SILVER[0])
    for by_ in range(fy + 6, fy + fh - 1, 2):
        s.set(fx + 2, by_, BRASS[3])
        s.set(fx + 5, by_, BRASS[3])
    hourglass = ["####", ".##.", ".##.", "####"]
    for row, line in enumerate(hourglass):
        for col, ch in enumerate(line):
            if ch == "#":
                s.set(fx + 2 + col, fy + 1 + row, GLOW[1] if row in (1, 2) else SILVER[2])
                if row in (1, 2):
                    glow.set(fx + 2 + col, fy + 1 + row, GLOW[2])
    s.rect(fx, fy + fh - 2, fx + fw - 1, fy + fh - 2, SILVER[0])  # Gürtel
    s.set(fx + 3, fy + fh - 2, BRASS[3])
    s.set(fx + 4, fy + fh - 2, BRASS[3])
    kx, ky, kw, kh = body.faces["back"]
    for yy in range(kh):
        for xx in range(kw):
            d = math.hypot(xx - 3.5, yy - 4.0)
            if 2.4 <= d <= 3.2:
                s.set(kx + xx, ky + yy, SILVER[1])
    s.set(kx + 3, ky + 2, SILVER[2])
    s.set(kx + 3, ky + 3, SILVER[2])
    s.set(kx + 4, ky + 4, SILVER[2])

    # Hoher Kragen
    collar = Cube(24, 16, 9, 3, 5)
    for name, rect in collar.each():
        fill_face(s, rect, COAT[2:], 531 + len(name), [3, 1])
        x, y, w, h = rect
        s.rect(x, y, x + w - 1, y, SILVER[2])
    cx, cy, cw, ch_ = collar.faces["front"]
    s.rect(cx + 3, cy, cx + 5, cy + ch_ - 1, COAT[0])
    s.set(cx + 4, cy + 1, BRASS[3])

    # Mantelschoß: Falten, Silbersaum, vorne geschlitzt
    coat = Cube(52, 16, 9, 11, 5)
    for name, rect in coat.each():
        coat_face(s, rect, 541 + len(name), hem=name not in ("top", "bottom"))
    ox, oy, ow, oh = coat.faces["front"]
    s.rect(ox + 4, oy + 1, ox + 4, oy + oh - 1, COAT[0])
    s.rect(ox + 4, oy + oh - 4, ox + 4, oy + oh - 1, (8, 10, 18, 255))
    s.rect(ox, oy, ox + ow - 1, oy, SILVER[0])

    # Arme: Ärmel mit Silberstulpe, dunkle Handschuhe
    for u in (80, 96):
        arm = Cube(u, 16, 4, 13, 4)
        for name, rect in arm.each():
            coat_face(s, rect, 551 + u + len(name))
            x, y, w, h = rect
            if name not in ("top", "bottom"):
                s.rect(x, y + h - 4, x + w - 1, y + h - 4, SILVER[2])
                s.rect(x, y + h - 3, x + w - 1, y + h - 3, SILVER[1])
                s.rect(x, y + h - 2, x + w - 1, y + h - 1, GLOVE[1])
                s.set(x + 1, y + h - 1, GLOVE[0])
            if name == "bottom":
                fill_face(s, rect, GLOVE, 560)

    # Beine: dunkle Hose, Stiefel mit Messingschnalle
    for u in (0, 16):
        leg = Cube(u, 36, 4, 14, 4)
        for name, rect in leg.each():
            fill_face(s, rect, COAT[:3], 571 + u + len(name), [2, 3, 1])
            x, y, w, h = rect
            if name not in ("top", "bottom"):
                s.rect(x, y + h - 5, x + w - 1, y + h - 1, GLOVE[0])
                s.rect(x, y + h - 5, x + w - 1, y + h - 5, GLOVE[1])
                if name == "front":
                    s.set(x + 1, y + h - 4, BRASS[3])
                    s.set(x + 2, y + h - 4, BRASS[2])
            if name == "bottom":
                fill_face(s, rect, [GLOVE[0]], 580)

    # Schulterstücke aus Messing mit graviertem Zahnkranz
    for u in (32, 52):
        pad = Cube(u, 36, 5, 3, 5)
        for name, rect in pad.each():
            fill_face(s, rect, BRASS[1:4], 591 + u + len(name), [2, 3, 1])
            x, y, w, h = rect
            if name == "top":
                for xx in range(w):
                    for yy in range(h):
                        if (xx + yy) % 2 == 0 and (xx in (0, w - 1) or yy in (0, h - 1)):
                            s.set(x + xx, y + yy, BRASS[4])
                s.set(x + w // 2, y + h // 2, GLOW[1])
                glow.set(x + w // 2, y + h // 2, GLOW[2])
            elif name != "bottom":
                s.rect(x, y + h - 1, x + w - 1, y + h - 1, BRASS[0])

    # Zeigerklinge: Minutenzeiger mit Spitze (vorne) und Gegengewicht-Ring (in der Hand)
    blade = Cube(72, 36, 0, 5, 20)
    rx, ry, rw, rh = blade.faces["right"]
    hand = Sprite(20, 5)
    hand_glow = Sprite(20, 5)
    steel = [(40, 44, 58, 255), (70, 76, 96, 255), (120, 128, 150, 255), (190, 198, 214, 255)]
    # Schaft (Mitte), zur Spitze hin breiter werdend
    for xx in range(20):
        if xx < 5:
            # Gegengewicht: Messingring
            for yy in range(5):
                d = math.hypot(xx - 2.0, yy - 2.0)
                if 1.0 <= d <= 2.3:
                    hand.set(xx, yy, BRASS[3] if yy < 2 else BRASS[2])
            continue
        if xx < 14:
            hand.set(xx, 2, steel[2])
            hand.set(xx, 1 if xx > 9 else 2, steel[3] if xx > 9 else steel[2])
            hand.set(xx, 3 if xx > 9 else 2, steel[1] if xx > 9 else steel[2])
        else:
            # Pfeilspitze
            half = (19 - xx) * 0.45 + 0.5
            for yy in range(5):
                if abs(yy - 2) <= half:
                    hand.set(xx, yy, steel[3] if yy < 2 else steel[2] if yy == 2 else steel[1])
    # Ornament: kleine Raute im Schaft und leuchtende Kante
    for xx, yy in ((9, 1), (9, 3), (8, 2), (10, 2)):
        hand.set(xx, yy, BRASS[3])
    hand.set(9, 2, GLOW[1])
    hand_glow.set(9, 2, GLOW[2])
    for xx in range(14, 20):
        hand_glow.set(xx, 2, GLOW[1])
        hand.set(xx, 2, GLOW[2])
    paste(s, hand.img, (rx, ry))
    paste(glow, hand_glow.img, (rx, ry))
    lx, ly, _, _ = blade.faces["left"]
    paste(s, hand.img.transpose(Image.FLIP_LEFT_RIGHT), (lx, ly))
    paste(glow, hand_glow.img.transpose(Image.FLIP_LEFT_RIGHT), (lx, ly))
    return s, glow



# ------------------------------------------------------------------------------------------
# Ruinenwächter (128×128, Abwicklung wie RuinGuardianModel)
# ------------------------------------------------------------------------------------------

STONE = [(64, 66, 64, 255), (88, 90, 86, 255), (110, 112, 106, 255), (132, 134, 126, 255)]
MOSS = [(46, 70, 30, 255), (64, 94, 36, 255), (84, 120, 46, 255), (110, 146, 60, 255)]
AMBERS = [(150, 70, 10, 255), (220, 130, 30, 255), (255, 196, 80, 255), (255, 236, 170, 255)]


def stone_bricks(s, rect, seed, moss=0.25):
    """Bemooste Steinquader: Fugen, Licht oben links, hier und da Moos."""
    x, y, w, h = rect
    rnd = random.Random(seed)
    fill_face(s, rect, STONE[1:], seed, [3, 4, 2])
    for yy in range(h):
        row = (y + yy) // 4
        for xx in range(w):
            px, py = x + xx, y + yy
            offset = 3 if row % 2 else 0
            if (py % 4 == 0) or ((px + offset) % 7 == 0):
                s.set(px, py, STONE[0])
            elif rnd.random() < moss * (0.4 + 0.6 * (1 - yy / max(h, 1))):
                s.set(px, py, MOSS[rnd.choice([0, 1, 1, 2])])


def iron_band(s, x0, y0, w, rows, seed):
    rnd = random.Random(seed)
    for yy in range(rows):
        for xx in range(w):
            c = g.RUST[1 + (1 if yy == 0 else 0) + (1 if rnd.random() < 0.2 else 0)]
            s.set(x0 + xx, y0 + yy, c)
    for xx in range(1, w - 1, 4):
        s.set(x0 + xx, y0 + rows // 2, g.RUST[4])


def ruin_guardian_textures():
    s = Sprite(128, 128)
    glow = Sprite(128, 128)
    head = Cube(0, 0, 8, 10, 8)
    for name, rect in head.each():
        stone_bricks(s, rect, 801 + len(name), moss=0.3 if name == "top" else 0.18)
    hx, hy, hw, hh = head.faces["front"]
    # Augenhöhle: dunkle Vertiefung rund um die Linse
    s.rect(hx + 2, hy + 2, hx + 5, hy + 5, (30, 30, 30, 255))
    s.rect(hx + 1, hy + 7, hx + 6, hy + 7, STONE[0])
    eye = Cube(34, 0, 3, 3, 1)
    for name, rect in eye.each():
        fill_face(s, rect, [g.RUST[1], g.RUST[2]], 811)
    ex, ey, _, _ = eye.faces["front"]
    for yy in range(3):
        for xx in range(3):
            c = AMBERS[3] if (xx, yy) == (1, 1) else AMBERS[2] if abs(xx - 1) + abs(yy - 1) == 1 else AMBERS[1]
            s.set(ex + xx, ey + yy, AMBERS[0] if (xx, yy) != (1, 1) else AMBERS[1])
            glow.set(ex + xx, ey + yy, c)
    body = Cube(0, 40, 18, 14, 11)
    for name, rect in body.each():
        stone_bricks(s, rect, 821 + len(name), moss=0.35 if name in ("top", "back") else 0.15)
        x, y, w, h = rect
        if name not in ("top", "bottom"):
            iron_band(s, x, y + 3, w, 2, 831 + len(name))
            iron_band(s, x, y + h - 4, w, 2, 841 + len(name))
    # Auf der Brust eine alte Stadtuhr: verblasstes Zifferblatt, ein abgebrochener Zeiger
    fx, fy, fw, fh = body.faces["front"]
    cx, cy = fx + fw // 2, fy + 7
    for yy in range(-3, 4):
        for xx in range(-3, 4):
            d = math.hypot(xx, yy)
            if d <= 2.4:
                s.set(cx + xx, cy + yy, (196, 188, 160, 255) if (xx + yy) % 3 else (176, 168, 140, 255))
            elif d <= 3.4:
                s.set(cx + xx, cy + yy, g.RUST[2] if xx + yy < 0 else g.RUST[1])
    for xx, yy in ((0, -1), (0, -2), (1, 0)):
        s.set(cx + xx, cy + yy, (40, 34, 30, 255))
    s.set(cx - 2, cy + 1, MOSS[2])
    s.set(cx + 2, cy - 2, MOSS[1])
    waist = Cube(0, 70, 10, 5, 7)
    for name, rect in waist.each():
        fill_face(s, rect, g.RUST[:4], 851 + len(name), [2, 3, 2, 1])
    moss = Cube(0, 90, 19, 2, 12)
    for name, rect in moss.each():
        fill_face(s, rect, MOSS, 861 + len(name), [2, 3, 3, 1])
    # Ranken (flach, mit durchsichtigem Grund), Vorder- und Rückseite gleich
    for u, w, h, seed in ((0, 5, 12, 871), (12, 4, 9, 872)):
        vine = Cube(u, 108, w, h, 0)
        fx, fy, _, _ = vine.faces["front"]
        sub = Sprite(w, h)
        rnd = random.Random(seed)
        for xx in range(0, w, 2):
            length = h - rnd.randrange(0, h // 2)
            for yy in range(length):
                sub.set(xx + (yy // 3) % 2 * (1 if xx + 1 < w else 0), yy, MOSS[rnd.choice([1, 2, 2, 3])])
        paste(s, sub.img, (fx, fy))
        bx, by, _, _ = vine.faces["back"]
        paste(s, sub.img.transpose(Image.FLIP_LEFT_RIGHT), (bx, by))
    # Schössling: kleiner Azaleenzweig mit rosa Blüten
    sprout = Cube(24, 108, 5, 6, 0)
    sub = Sprite(5, 6)
    for xx, yy in ((2, 5), (2, 4), (2, 3)):
        sub.set(xx, yy, (90, 62, 40, 255))
    for xx, yy in ((1, 2), (2, 2), (3, 2), (0, 1), (1, 1), (2, 1), (3, 1), (4, 1), (1, 0), (2, 0), (3, 0)):
        sub.set(xx, yy, MOSS[2] if (xx + yy) % 2 else MOSS[3])
    for xx, yy in ((1, 1), (3, 0), (4, 1)):
        sub.set(xx, yy, (226, 130, 190, 255))
    fx, fy, _, _ = sprout.faces["front"]
    paste(s, sub.img, (fx, fy))
    bx, by, _, _ = sprout.faces["back"]
    paste(s, sub.img.transpose(Image.FLIP_LEFT_RIGHT), (bx, by))
    for v, seed in ((21, 881), (58, 882)):
        arm = Cube(60, v, 4, 30, 6)
        for name, rect in arm.each():
            stone_bricks(s, rect, seed + len(name), moss=0.2)
            x, y, w, h = rect
            if name not in ("top", "bottom"):
                iron_band(s, x, y + 9, w, 2, seed + 10)
                iron_band(s, x, y + 19, w, 2, seed + 11)
                for yy in range(0, 5):
                    for xx in range(w):
                        if random.Random(seed + xx * 7 + yy).random() < 0.6 - yy * 0.1:
                            s.set(x + xx, y + yy, MOSS[1 + (xx + yy) % 2])
    for v, seed in ((0, 891), (14, 892)):
        fist = Cube(84, v, 6, 6, 8)
        for name, rect in fist.each():
            fill_face(s, rect, STONE, seed + len(name), [1, 3, 3, 2])
            x, y, w, h = rect
            for xx in range(x, x + w, 2):
                s.set(xx, y + h - 1, STONE[0])
    for v, seed in ((30, 901), (52, 902)):
        leg = Cube(84, v, 6, 16, 5)
        for name, rect in leg.each():
            stone_bricks(s, rect, seed + len(name), moss=0.15)
            x, y, w, h = rect
            if name not in ("top", "bottom"):
                iron_band(s, x, y + 6, w, 3, seed + 20)
                s.rect(x, y + h - 2, x + w - 1, y + h - 1, STONE[0])
    return s, glow


def guardian_core():
    """Wächterkern: eine Kugel aus rostigem Eisen und Moos, in der eine Bernsteinlinse glimmt."""
    s = Sprite()
    s.shade_disc(7.5, 7.5, 6.6, [g.RUST[0], g.RUST[1], g.RUST[2], g.RUST[3]])
    rnd = random.Random(911)
    for _ in range(18):
        x, y = rnd.randint(2, 13), rnd.randint(2, 13)
        if s.get(x, y) != T and math.hypot(x - 7.5, y - 7.5) > 3.0:
            s.set(x, y, MOSS[rnd.choice([1, 2])])
    s.disc(7.5, 7.5, 3.0, AMBERS[0])
    s.disc(7.5, 7.5, 2.0, AMBERS[1])
    s.disc(7.0, 7.0, 1.0, AMBERS[2])
    s.set(6, 6, AMBERS[3])
    for x in range(1, 15, 3):
        if s.get(x, 7) != T and abs(x - 7.5) > 3.5:
            s.set(x, 7, g.RUST[4])
    s.outline()
    return s


# ------------------------------------------------------------------------------------------
# Moosbär (128×64, Abwicklung des Eisbären-Modells)
# ------------------------------------------------------------------------------------------

FUR = [(58, 40, 26, 255), (78, 54, 34, 255), (98, 70, 44, 255), (122, 90, 58, 255)]


def moss_bear_textures(sheared):
    s = Sprite(128, 64)
    head = Cube(0, 0, 7, 7, 7)
    for name, rect in head.each():
        fill_face(s, rect, FUR, 921 + len(name), [2, 3, 3, 1])
    fx, fy, fw, fh = head.faces["front"]
    for xx in (1, 5):
        s.set(fx + xx, fy + 3, (20, 14, 10, 255))
        s.set(fx + xx, fy + 2, FUR[0])
    if not sheared:
        tx, ty, tw, th = head.faces["top"]
        for xx in range(tw):
            for yy in range(th):
                if (xx * 3 + yy * 5) % 4 != 0:
                    s.set(tx + xx, ty + yy, MOSS[1 + (xx + yy) % 3])
    mouth = Cube(0, 44, 5, 3, 3)
    for name, rect in mouth.each():
        fill_face(s, rect, FUR[2:], 931 + len(name))
    mx, my, mw, mh = mouth.faces["front"]
    s.rect(mx + 1, my, mx + 3, my, (24, 18, 14, 255))
    ear = Cube(26, 0, 2, 2, 1)
    for name, rect in ear.each():
        fill_face(s, rect, FUR[:2], 941)

    def mossy(cube, top_name, side_from_top, seed):
        rnd = random.Random(seed)
        for name, rect in cube.each():
            fill_face(s, rect, FUR, seed + len(name), [2, 3, 3, 1])
        if sheared:
            # Kurz geschorener Rücken: hellere, ungleichmäßige Stoppeln
            x, y, w, h = cube.faces[top_name]
            for yy in range(h):
                for xx in range(w):
                    if rnd.random() < 0.5:
                        s.set(x + xx, y + yy, FUR[3] if rnd.random() < 0.4 else FUR[2])
            return
        x, y, w, h = cube.faces[top_name]
        for yy in range(h):
            for xx in range(w):
                s.set(x + xx, y + yy, MOSS[rnd.choice([0, 1, 1, 2, 2, 3])])
        # Blumen, Pilze und Farn obenauf
        for _ in range(w * h // 18):
            px, py = x + rnd.randrange(w), y + rnd.randrange(h)
            s.set(px, py, rnd.choice([(230, 210, 80, 255), (226, 130, 190, 255), (200, 60, 50, 255), (240, 240, 230, 255)]))
        # Moos hängt an den Flanken herab
        for name, near_left in side_from_top:
            sx, sy, sw, sh = cube.faces[name]
            for yy in range(sh):
                depth = 3 + int(2 * math.sin(yy * 0.9 + seed)) + rnd.randrange(2)
                for k in range(depth):
                    xx = k if near_left else sw - 1 - k
                    s.set(sx + xx, sy + yy, MOSS[rnd.choice([0, 1, 2])])

    # Rumpf und Schulterbuckel sind um 90° gekippt: ihre Rückseite ist der Bärenrücken,
    # rechts liegt der Rücken links in der Fläche, links rechts
    mossy(Cube(0, 19, 14, 14, 11), "back", (("right", True), ("left", False)), 951)
    mossy(Cube(39, 0, 12, 12, 10), "back", (("right", True), ("left", False)), 961)
    for u, v, d in ((50, 22, 8), (50, 40, 6)):
        leg = Cube(u, v, 4, 10, d)
        for name, rect in leg.each():
            fill_face(s, rect, FUR, 971 + u + v + len(name), [2, 3, 2, 1])
            x, y, w, h = rect
            if name not in ("top", "bottom"):
                s.rect(x, y + h - 2, x + w - 1, y + h - 1, FUR[0])
    return s


# ------------------------------------------------------------------------------------------
# Chrono-Händler: Berufskleidung (64×64, Dorfbewohner-Abwicklung)
# ------------------------------------------------------------------------------------------

ROBE = [(34, 20, 60, 255), (48, 28, 84, 255), (66, 40, 112, 255), (88, 58, 140, 255)]
LEATHER = [(70, 44, 26, 255), (96, 62, 36, 255)]


def chrono_trader_overlay():
    s = Sprite(64, 64)
    # Kopf: ein Monokel vor dem rechten Auge
    head = Cube(0, 0, 8, 10, 8)
    fx, fy, _, _ = head.faces["front"]
    for xx, yy in ((1, 3), (2, 3), (3, 3), (1, 4), (3, 4), (1, 5), (2, 5), (3, 5)):
        s.set(fx + xx, fy + yy, BRASS[3] if yy == 3 else BRASS[2])
    for yy in range(6, 9):
        s.set(fx + 3, fy + yy, BRASS[1])
    # Hut-Schicht: Lederband mit Schutzbrille auf der Stirn
    hat = Cube(32, 0, 8, 10, 8)
    for name in ("front", "right", "left", "back"):
        x, y, w, h = hat.faces[name]
        for xx in range(w):
            s.set(x + xx, y + 1, LEATHER[1])
            s.set(x + xx, y + 2, LEATHER[0])
    hx, hy, _, _ = hat.faces["front"]
    for gx in (1, 5):
        for xx in range(gx - 1, gx + 2):
            for yy in range(0, 3):
                edge = xx in (gx - 1, gx + 1) or yy in (0, 2)
                s.set(hx + xx, hy + yy, BRASS[3] if edge else GLOW[1])
    s.set(hx + 3, hy + 1, BRASS[2])
    s.set(hx + 4, hy + 1, BRASS[2])
    # Mantel: tiefes Violett, Messingknöpfe, Zahnrad-Abzeichen, Uhrkette, türkiser Saum
    jacket = Cube(0, 38, 8, 20, 6)
    for name, rect in jacket.each():
        fill_face(s, rect, ROBE[1:], 981 + len(name), [3, 3, 1])
        x, y, w, h = rect
        if name not in ("top", "bottom"):
            s.rect(x, y + h - 1, x + w - 1, y + h - 1, GLOW[0])
            s.rect(x, y + h - 2, x + w - 1, y + h - 2, ROBE[0])
    jx, jy, jw, jh = jacket.faces["front"]
    for yy in range(jh - 2):
        s.set(jx + 3, jy + yy, ROBE[0])
    for yy in range(2, 13, 3):
        s.set(jx + 4, jy + yy, BRASS[3])
    gear_pts = [(1, 3), (0, 4), (2, 4), (1, 5), (1, 4)]
    for xx, yy in gear_pts:
        s.set(jx + 5 + xx, jy + yy, BRASS[3] if (xx, yy) != (1, 4) else GLOW[1])
    for xx, yy in ((1, 7), (2, 8), (3, 8), (2, 9)):
        s.set(jx + xx, jy + yy, BRASS[4])
    s.set(jx + 1, jy + 10, BRASS[3])
    body = Cube(16, 20, 8, 12, 6)
    for name, rect in body.each():
        fill_face(s, rect, ROBE[:3], 991 + len(name), [2, 3, 1])
    # Ärmel (verschränkte Arme)
    for cube, seed in ((Cube(44, 22, 4, 8, 4), 1001), (Cube(40, 38, 8, 4, 4), 1002)):
        for name, rect in cube.each():
            fill_face(s, rect, ROBE[1:], seed + len(name), [3, 3, 1])
            x, y, w, h = rect
            if name not in ("top", "bottom") and cube.faces["front"][2] == 4:
                s.rect(x, y + h - 2, x + w - 1, y + h - 2, BRASS[2])
    return s


# ------------------------------------------------------------------------------------------
# Chronometer (Block)
# ------------------------------------------------------------------------------------------

MAHOGANY = [(54, 26, 18, 255), (76, 38, 24, 255), (98, 50, 30, 255), (120, 64, 40, 255)]


def chronometer_top():
    s = Sprite()
    noise_fill_local(s, MAHOGANY[1:], 1011)
    for yy in range(16):
        for xx in range(16):
            d = math.hypot(xx - 7.5, yy - 7.5)
            if d <= 5.2:
                s.set(xx, yy, FACE[1] if (xx + yy) % 4 else FACE[0])
            elif d <= 6.4:
                light = ((7.5 - xx) + (7.5 - yy)) / 12
                s.set(xx, yy, BRASS[3] if light > 0.2 else BRASS[2] if light > -0.3 else BRASS[1])
    for hour in range(12):
        ang = hour / 12 * 2 * math.pi
        x = 7.5 + math.sin(ang) * 4.3
        y = 7.5 - math.cos(ang) * 4.3
        s.set(int(round(x)), int(round(y)), (40, 34, 40, 255))
    for i in range(1, 4):
        s.set(7, 7 - i, (30, 26, 34, 255))
    for i in range(1, 3):
        s.set(7 + i, 7 + i // 2, (30, 26, 34, 255))
    s.set(7, 7, BRASS[1])
    s.set(8, 10, GLOW[1])
    for x, y in ((1, 1), (14, 1), (1, 14), (14, 14)):
        s.set(x, y, BRASS[3])
    return s


def noise_fill_local(s, ramp, seed):
    g.noise_fill(s, ramp, seed, weights=([1] * (len(ramp) - 3) + [2, 3, 1])[-len(ramp):])
    for y in range(0, 16, 5):
        for x in range(16):
            if (x + y) % 3 == 0:
                s.set(x, y, ramp[0])


def chronometer_side():
    s = Sprite()
    noise_fill_local(s, MAHOGANY, 1021)
    s.rect(0, 0, 15, 0, BRASS[3])
    s.rect(0, 7, 15, 7, BRASS[2])
    for x, y in ((0, 0), (15, 0), (0, 15), (15, 15)):
        s.set(x, y, BRASS[4])
    # Messingschild mit eingraviertem Zahnrad
    s.rect(5, 10, 10, 13, BRASS[2])
    s.rect(5, 10, 10, 10, BRASS[3])
    s.set(7, 12, BRASS[0])
    s.set(8, 11, BRASS[0])
    s.set(8, 12, BRASS[4])
    return s


def chronometer_bottom():
    s = Sprite()
    noise_fill_local(s, MAHOGANY[:3], 1031)
    return s


# ------------------------------------------------------------------------------------------
# Wächter der letzten Stadt (256×128, Abwicklung wie CityColossusModel)
# ------------------------------------------------------------------------------------------

CALCITE = [(176, 172, 160, 255), (198, 194, 182, 255), (216, 212, 200, 255), (232, 229, 218, 255)]
PATINA = [(40, 92, 74, 255), (58, 122, 98, 255), (80, 152, 122, 255), (118, 184, 154, 255)]
BRONZE = [(96, 62, 30, 255), (134, 90, 44, 255), (170, 120, 60, 255)]
WARM = [(180, 110, 30, 255), (240, 170, 60, 255), (255, 220, 130, 255), (255, 246, 210, 255)]


def calcite_face(s, rect, seed, cracks=2, moss=0.06):
    fill_face(s, rect, CALCITE, seed, [1, 3, 4, 2])
    x, y, w, h = rect
    rnd = random.Random(seed)
    # Quaderfugen
    for yy in range(0, h, 6):
        for xx in range(w):
            if rnd.random() < 0.85:
                s.set(x + xx, y + yy, CALCITE[0])
    # Risse
    for _ in range(cracks):
        cx, cy = x + rnd.randrange(w), y + rnd.randrange(h)
        for _ in range(rnd.randint(3, 7)):
            s.set(cx, cy, (120, 116, 106, 255))
            cx += rnd.choice((-1, 0, 1))
            cy += 1
            if not (x <= cx < x + w and y <= cy < y + h):
                break
    for yy in range(h):
        for xx in range(w):
            if rnd.random() < moss * (1.0 - yy / max(h, 1)):
                s.set(x + xx, y + yy, MOSS[rnd.choice([1, 2])])


def patina_face(s, rect, seed):
    fill_face(s, rect, PATINA, seed, [1, 3, 3, 1])
    x, y, w, h = rect
    rnd = random.Random(seed)
    for _ in range(w * h // 14):
        s.set(x + rnd.randrange(w), y + rnd.randrange(h), BRONZE[rnd.choice([1, 2])])


def colossus_textures(bare):
    s = Sprite(256, 128)
    glow = Sprite(256, 128)
    body = Cube(0, 0, 24, 24, 14)
    for name, rect in body.each():
        calcite_face(s, rect, 1101 + len(name), cracks=6 if bare else 3, moss=0.12 if name == "top" else 0.04)
        x, y, w, h = rect
        if name not in ("top", "bottom"):
            for xx in range(w):
                s.set(x + xx, y + h - 3, PATINA[1])
                s.set(x + xx, y + h - 2, PATINA[2])
    if bare:
        # Ohne Platten liegt das Uhrwerk frei: Zahnräder in Messing, ein glühender Kern
        for name in ("front", "back", "right", "left"):
            x, y, w, h = body.faces[name]
            rnd = random.Random(1120 + len(name))
            for _ in range(3 if name in ("front", "back") else 2):
                cx, cy = x + 3 + rnd.randrange(max(1, w - 6)), y + 3 + rnd.randrange(max(1, h - 10))
                r = rnd.choice([2.2, 3.0, 3.6])
                for yy in range(-4, 5):
                    for xx in range(-4, 5):
                        d = math.hypot(xx, yy)
                        ang = math.atan2(yy, xx)
                        tooth = int((ang + math.pi) / (2 * math.pi) * 10) % 2 == 0
                        if d <= r - 0.8 or (d <= r + 0.4 and tooth):
                            s.set(cx + xx, cy + yy, BRASS[2] if (xx + yy) % 3 else BRASS[3])
                        if d <= 0.9:
                            s.set(cx + xx, cy + yy, GLOW[1])
                            glow.set(cx + xx, cy + yy, GLOW[2])
    # Die Uhr der Stadt
    clock = Cube(80, 0, 10, 10, 1)
    for name, rect in clock.each():
        patina_face(s, rect, 1131)
    fx, fy, fw, fh = clock.faces["front"]
    for yy in range(fh):
        for xx in range(fw):
            d = math.hypot(xx - 4.5, yy - 4.5)
            if d <= 3.9:
                s.set(fx + xx, fy + yy, FACE[1] if (xx * 3 + yy) % 5 else FACE[0])
            elif d <= 5.0:
                s.set(fx + xx, fy + yy, PATINA[2] if xx + yy < 9 else PATINA[1])
    for hour in range(12):
        ang = hour / 12 * 2 * math.pi
        xx, yy = int(round(4.5 + math.sin(ang) * 3.3)), int(round(4.5 - math.cos(ang) * 3.3))
        s.set(fx + xx, fy + yy, (40, 40, 44, 255))
        glow.set(fx + xx, fy + yy, WARM[0])
    for i in range(1, 4):
        s.set(fx + 4, fy + 4 - i, (30, 30, 34, 255))
        glow.set(fx + 4, fy + 4 - i, WARM[1])
    for i in range(1, 3):
        s.set(fx + 4 + i, fy + 4 + i // 2, (30, 30, 34, 255))
        glow.set(fx + 4 + i, fy + 4 + i // 2, WARM[1])
    # Kupferne Schulterplatten
    for v in (0, 24):
        plate = Cube(104, v, 9, 5, 16)
        for name, rect in plate.each():
            patina_face(s, rect, 1141 + v + len(name))
            x, y, w, h = rect
            if name == "top":
                for xx in range(0, w, 3):
                    for yy in range(h):
                        s.set(x + xx, y + yy, PATINA[0] if yy % 4 else PATINA[3])
    # Kopf: Glockenturm aus Kalkstein, Kupferdach in drei Stufen, Blitzableiter
    head = Cube(0, 40, 12, 12, 12)
    for name, rect in head.each():
        calcite_face(s, rect, 1151 + len(name), cracks=2)
        x, y, w, h = rect
        if name not in ("top", "bottom"):
            # Rundbogenfenster (dunkel; das leuchtende Fenster ist ein eigener Würfel vorne)
            for yy in range(3, 8):
                for xx in range(4, 8):
                    if not (yy == 3 and xx in (4, 7)):
                        s.set(x + xx, y + yy, (40, 36, 34, 255))
    for i, (u, v, w, h, d) in enumerate(((48, 40, 14, 2, 14), (104, 48, 10, 4, 10), (144, 48, 6, 3, 6))):
        roof = Cube(u, v, w, h, d)
        for name, rect in roof.each():
            patina_face(s, rect, 1161 + i * 10 + len(name))
    rod = Cube(168, 48, 2, 6, 2)
    for name, rect in rod.each():
        fill_face(s, rect, [BRONZE[1], BRONZE[2], PATINA[2]], 1191)
    window = Cube(176, 48, 6, 3, 1)
    for name, rect in window.each():
        fill_face(s, rect, [WARM[0], WARM[1]], 1192)
        fill_face(glow, rect, [WARM[1], WARM[2], WARM[3]], 1193)
    # Arme: Kalksteinsäulen mit Kupferbändern; rechts die Glocke, links die Faust
    for u, seed in ((0, 1201), (124, 1211)):
        arm = Cube(u, 64, 6, 26, 8)
        for name, rect in arm.each():
            calcite_face(s, rect, seed + len(name), cracks=2)
            x, y, w, h = rect
            if name not in ("top", "bottom"):
                for yy in (4, 5, 17, 18):
                    for xx in range(w):
                        s.set(x + xx, y + yy, PATINA[1 if yy % 2 else 2])
    for i, (u, w, h, d) in enumerate(((28, 6, 3, 6), (52, 8, 4, 8), (84, 10, 2, 10))):
        bell = Cube(u, 64, w, h, d)
        for name, rect in bell.each():
            patina_face(s, rect, 1221 + i * 10 + len(name))
            if name == "bottom" and i == 2:
                x, y, w2, h2 = rect
                s.rect(x + 1, y + 1, x + w2 - 2, y + h2 - 2, (24, 30, 28, 255))
    fist = Cube(152, 64, 8, 8, 10)
    for name, rect in fist.each():
        calcite_face(s, rect, 1251 + len(name), cracks=1)
    for u, seed in ((0, 1261), (32, 1271)):
        leg = Cube(u, 98, 8, 18, 8)
        for name, rect in leg.each():
            calcite_face(s, rect, seed + len(name), cracks=2, moss=0.1)
            x, y, w, h = rect
            if name not in ("top", "bottom"):
                for xx in range(w):
                    s.set(x + xx, y + h - 2, PATINA[1])
                    s.set(x + xx, y + h - 1, PATINA[0])
                    s.set(x + xx, y + 2, PATINA[2])
    return s, glow


def heart_of_the_last_city():
    """Herz der letzten Stadt: eine kleine grüne Glocke, in der ein warmes Licht brennt, mit einem Zifferblatt-Ring."""
    s = Sprite()
    rows = [(4, 5, 10), (5, 4, 11), (6, 4, 11), (7, 3, 12), (8, 3, 12), (9, 3, 12), (10, 2, 13), (11, 2, 13), (12, 1, 14), (13, 1, 14)]
    for y, x0, x1 in rows:
        for x in range(x0, x1 + 1):
            light = (8 - x) / 8
            s.set(x, y, PATINA[3] if light > 0.3 else PATINA[2] if light > -0.2 else PATINA[1])
    s.rect(6, 2, 9, 3, BRONZE[2])
    s.rect(7, 1, 8, 1, BRONZE[1])
    for x in range(1, 15):
        s.set(x, 13, PATINA[0])
    for yy in range(7, 12):
        for xx in range(6, 10):
            d = math.hypot(xx - 7.5, yy - 9.0)
            if d <= 1.9:
                s.set(xx, yy, WARM[2] if d < 1.0 else WARM[1])
    s.set(7, 8, WARM[3])
    s.set(5, 6, PATINA[3])
    s.set(7, 14, BRONZE[1])
    s.set(8, 14, BRONZE[1])
    s.outline()
    return s


def city_bell_plinth():
    s = Sprite()
    stone_bricks(s, (0, 0, 16, 16), 1301, moss=0.35)
    return s


def city_bell_wood():
    s = Sprite()
    noise_fill_local(s, [(52, 40, 30, 255), (70, 54, 40, 255), (88, 70, 52, 255)], 1311)
    for x in range(16):
        s.set(x, 0, (40, 30, 22, 255))
    return s


def city_bell_bronze():
    s = Sprite()
    patina_face(s, (0, 0, 16, 16), 1321)
    for y in range(16):
        s.set(0, y, PATINA[0])
        s.set(15, y, PATINA[0])
    return s

# ------------------------------------------------------------------------------------------
# Items
# ------------------------------------------------------------------------------------------

def time_seal():
    """Zeitsiegel: violettes Wachs mit eingeprägter Sanduhr, darunter ein Stück Siegelband."""
    s = Sprite()
    # Band
    for i in range(5):
        s.set(4 + i // 2, 11 + i, (120, 30, 40, 255))
        s.set(5 + i // 2, 11 + i, (160, 44, 52, 255))
        s.set(11 - i // 2, 11 + i, (160, 44, 52, 255))
        s.set(10 - i // 2, 11 + i, (120, 30, 40, 255))
    # Wachsscheibe mit unregelmäßigem Rand
    rnd = random.Random(61)
    for yy in range(16):
        for xx in range(16):
            d = math.hypot(xx - 7.5, yy - 7.0)
            edge = 6.2 + 0.6 * math.sin(math.atan2(yy - 7.0, xx - 7.5) * 7 + 1.3)
            if d <= edge:
                light = ((7.5 - xx) + (7.0 - yy)) / 12.0
                i = 1 + (1 if light > -0.15 else 0) + (1 if light > 0.35 else 0)
                if d > edge - 1.0:
                    i = max(0, i - 1)
                s.set(xx, yy, WAX[i])
    # Eingeprägter Ring und Sanduhr
    for yy in range(16):
        for xx in range(16):
            d = math.hypot(xx - 7.5, yy - 7.0)
            if 3.9 <= d <= 4.5 and s.get(xx, yy) != T:
                s.set(xx, yy, WAX[0])
    glass = ["#####", ".###.", "..#..", ".#.#.", "#####"]
    for row, line in enumerate(glass):
        for col, ch in enumerate(line):
            if ch == "#":
                s.set(5 + col, 5 + row, GLOW[1] if row in (1, 3) else WAX[0])
    s.set(7, 6, GLOW[2])
    s.set(4, 3, WAX[3])
    s.set(5, 3, WAX[3])
    s.outline()
    return s


def clock_hand_blade():
    """Zeigerklinge: diagonaler Minutenzeiger aus dunklem Stahl, Messingring als Griff, leuchtende Spitze."""
    s = Sprite()
    steel = [(40, 44, 58, 255), (70, 76, 96, 255), (120, 128, 150, 255), (190, 198, 214, 255)]
    # Klinge von links unten nach rechts oben
    for i in range(5, 14):
        x, y = i, 15 - i
        s.set(x, y, steel[2])
        s.set(x - 1, y, steel[1])
        s.set(x, y - 1, steel[3])
    # Pfeilspitze
    for x, y, c in ((14, 1, steel[3]), (13, 1, steel[3]), (14, 2, steel[3]), (15, 0, WHITE), (12, 1, steel[2]), (14, 3, steel[1]),
                    (13, 3, steel[2]), (12, 2, steel[3]), (11, 1, steel[1]), (14, 4, steel[1])):
        s.set(x, y, c)
    for x, y in ((13, 2), (12, 3), (11, 4)):
        s.set(x, y, GLOW[1])
    s.set(14, 1, GLOW[2])
    # Raute als Ornament
    for x, y in ((9, 6), (8, 7), (10, 7), (9, 8)):
        s.set(x, y, BRASS[3])
    s.set(9, 7, GLOW[1])
    # Griff: Messingring als Gegengewicht
    for yy in range(16):
        for xx in range(16):
            d = math.hypot(xx - 3.0, yy - 12.0)
            if 1.2 <= d <= 2.6:
                s.set(xx, yy, BRASS[3] if xx + (15 - yy) < 7 else BRASS[2])
    s.set(3, 12, T)
    s.set(1, 14, BRASS[1])
    s.set(0, 15, BRASS[1])
    s.outline()
    return s


DAMPING_ICON = ["...yy..r", "..yYYyr.", "..yYYr..", ".yYYrYy.", ".yYrYYy.", "yyryyyyy", ".r.yy...", "r......."]


def damping_module():
    g.ICONS["damping"] = DAMPING_ICON
    return g.module("amp", "damping")


ITEMS = {"time_seal": time_seal, "clock_hand_blade": clock_hand_blade, "damping_module": damping_module, "guardian_core": guardian_core,
         "heart_of_the_last_city": heart_of_the_last_city}
BLOCKS = {"chronometer_top": chronometer_top, "chronometer_side": chronometer_side, "chronometer_bottom": chronometer_bottom,
          "city_bell_plinth": city_bell_plinth, "city_bell_wood": city_bell_wood, "city_bell_bronze": city_bell_bronze}


def main(preview=None):
    sprites = []
    for name, fn in ITEMS.items():
        s = fn()
        s.save(TEX / "item" / f"{name}.png")
        sprites.append(s.img)
    for name, fn in BLOCKS.items():
        s = fn()
        s.save(TEX / "block" / f"{name}.png")
        sprites.append(s.img)
    tex, glow = time_warden_textures()
    tex.save(TEX / "entity" / "time_warden.png")
    glow.save(TEX / "entity" / "time_warden_glow.png")
    tex, glow = ruin_guardian_textures()
    tex.save(TEX / "entity" / "ruin_guardian.png")
    glow.save(TEX / "entity" / "ruin_guardian_glow.png")
    for bare, suffix in ((False, ""), (True, "_bare")):
        tex, glow = colossus_textures(bare)
        tex.save(TEX / "entity" / f"city_colossus{suffix}.png")
        glow.save(TEX / "entity" / f"city_colossus{suffix}_glow.png")
    moss_bear_textures(False).save(TEX / "entity" / "moss_bear.png")
    moss_bear_textures(True).save(TEX / "entity" / "moss_bear_sheared.png")
    for folder in ("villager", "zombie_villager"):
        path = TEX / "entity" / folder / "profession" / "chrono_trader.png"
        chrono_trader_overlay().save(path)
        (path.parent / "chrono_trader.png.mcmeta").write_text('{"villager": {"hat": "partial"}}\n')
    print(f"{len(sprites)} Item- und Block-Texturen und 4 Wesen geschrieben")
    if preview:
        scale = 6
        sheet = Image.new("RGBA", (128 * 4 + 16 * scale * len(sprites) + 40, 128 * 4), (60, 60, 70, 255))
        sheet.alpha_composite(tex.img.resize((512, 512), Image.NEAREST), (0, 0))
        for i, img in enumerate(sprites):
            sheet.alpha_composite(img.resize((16 * scale, 16 * scale), Image.NEAREST), (520 + i * (16 * scale + 8), 8))
        sheet.save(preview)


if __name__ == "__main__":
    main(sys.argv[1] if len(sys.argv) > 1 else None)
