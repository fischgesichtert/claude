"""Schneidet die Einzelbilder der Demo (run-demo/demo-frames) zu einem beschrifteten Video zusammen.

Aufruf: python3 -I tools/make_demo_video.py <ausgabe.mp4> [ausgabe.gif]
Jede Szene: Titelzeile + Bildsequenz (20 Bilder pro Sekunde = echtes Spieltempo).
"""
import shutil
import subprocess
import sys
import tempfile
from pathlib import Path

from PIL import Image, ImageDraw, ImageFont

ROOT = Path(__file__).resolve().parent.parent
FRAMES = ROOT / "run-demo/demo-frames"
FONT = "/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf"

SCENES = [
    ("seq_1_normal", "Normale Zeit", "Referenz: so läuft alles normal"),
    ("seq_2_vorspulen_4x", "4× vorgespult", "gleiche Wege, gleiche KI – nur viermal so oft gerechnet"),
    ("seq_3_zeitlupe", "Zeitlupe 0,25×", "nur jeder vierte Tick, weich interpoliert"),
    ("seq_4_zeitstopp_aussen", "Zeitstopp – von außen", "die Zone wird zur grauen Kugel, Rauch hängt in der Luft"),
    ("seq_5_zeitstopp_innen", "Zeitstopp – mittendrin", "die ganze Welt verliert ihre Farbe"),
    ("seq_6_global_5x", "Globale Zeit 5×", "Sonne, Wolken und alle Tiere rasen"),
]


def caption(img, title, subtitle, font_big, font_small):
    draw = ImageDraw.Draw(img, "RGBA")
    w, _ = img.size
    draw.rectangle([0, 0, w, 74], fill=(10, 8, 24, 170))
    draw.text((22, 8), title, font=font_big, fill=(255, 226, 120, 255))
    draw.text((24, 46), subtitle, font=font_small, fill=(220, 230, 255, 255))
    draw.text((w - 120, 14), "TIME", font=font_big, fill=(120, 230, 255, 255))
    return img


def main(out_mp4, out_gif=None):
    font_big = ImageFont.truetype(FONT, 30)
    font_small = ImageFont.truetype(FONT.replace("-Bold", ""), 18)
    with tempfile.TemporaryDirectory() as tmp:
        tmp = Path(tmp)
        n = 0
        for folder, title, subtitle in SCENES:
            src = FRAMES / folder
            if not src.exists():
                continue
            files = sorted(src.glob("*.png"))
            for i, f in enumerate(files):
                img = Image.open(f).convert("RGB")
                caption(img, title, subtitle, font_big, font_small)
                img.save(tmp / f"{n:05d}.png")
                n += 1
            # kurzer Halt am Szenenende
            for _ in range(6):
                shutil.copy(tmp / f"{n - 1:05d}.png", tmp / f"{n:05d}.png")
                n += 1
        subprocess.run(["ffmpeg", "-hide_banner", "-loglevel", "error", "-y", "-framerate", "20", "-i", str(tmp / "%05d.png"),
                        "-c:v", "libx264", "-pix_fmt", "yuv420p", "-crf", "20", "-preset", "medium", "-movflags", "+faststart", out_mp4], check=True)
        if out_gif:
            subprocess.run(["ffmpeg", "-hide_banner", "-loglevel", "error", "-y", "-i", out_mp4,
                            "-vf", "fps=12,scale=640:-1:flags=lanczos,split[a][b];[a]palettegen=max_colors=128[p];[b][p]paletteuse=dither=bayer:bayer_scale=4",
                            out_gif], check=True)
    print("fertig:", out_mp4, n, "Bilder")


if __name__ == "__main__":
    main(*sys.argv[1:])
