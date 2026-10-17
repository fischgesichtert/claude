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


ITEMS = {"time_seal": time_seal, "clock_hand_blade": clock_hand_blade, "damping_module": damping_module}


def main(preview=None):
    sprites = []
    for name, fn in ITEMS.items():
        s = fn()
        s.save(TEX / "item" / f"{name}.png")
        sprites.append(s.img)
    tex, glow = time_warden_textures()
    tex.save(TEX / "entity" / "time_warden.png")
    glow.save(TEX / "entity" / "time_warden_glow.png")
    print(f"{len(sprites)} Item-Texturen und 1 Wesen geschrieben")
    if preview:
        scale = 6
        sheet = Image.new("RGBA", (128 * 4 + 16 * scale * len(sprites) + 40, 128 * 4), (60, 60, 70, 255))
        sheet.alpha_composite(tex.img.resize((512, 512), Image.NEAREST), (0, 0))
        for i, img in enumerate(sprites):
            sheet.alpha_composite(img.resize((16 * scale, 16 * scale), Image.NEAREST), (520 + i * (16 * scale + 8), 8))
        sheet.save(preview)


if __name__ == "__main__":
    main(sys.argv[1] if len(sys.argv) > 1 else None)
