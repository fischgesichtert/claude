"""Synthetisiert alle Klänge von TIME (keine fremden Aufnahmen) und schreibt sie als .ogg.

Aufruf:  python3 -I tools/gen_sounds.py
Benötigt numpy und ffmpeg (libvorbis).
"""
import subprocess
import sys
import tempfile
import wave
from pathlib import Path

import numpy as np

SR = 44100
ROOT = Path(__file__).resolve().parent.parent
OUT = ROOT / "src/main/resources/assets/timemod/sounds"
RNG = np.random.default_rng(20261017)


# ---------------------------------------------------------------------------------------------
# Bausteine
# ---------------------------------------------------------------------------------------------

def t(seconds):
    return np.arange(int(seconds * SR)) / SR


def silence(seconds):
    return np.zeros(int(seconds * SR))


def mix(*signals):
    """Addiert Signale unterschiedlicher Länge (kürzere werden mit Stille aufgefüllt)."""
    n = max(len(sig) for sig in signals)
    out = np.zeros(n)
    for sig in signals:
        out[: len(sig)] += sig
    return out


def place(buf, sig, at):
    start = int(at * SR)
    end = min(len(buf), start + len(sig))
    if end > start:
        buf[start:end] += sig[: end - start]
    return buf


def env_ad(n, attack, decay, curve=4.0):
    """Hüllkurve: linearer Anstieg, exponentielles Abklingen."""
    a = max(1, int(attack * SR))
    e = np.ones(n)
    e[:a] = np.linspace(0, 1, a)
    rest = np.arange(n - a) / SR
    e[a:] = np.exp(-curve * rest / max(decay, 1e-4))
    return e


def sweep(f0, f1, seconds, shape="exp"):
    """Sinus mit gleitender Frequenz (phasenkontinuierlich)."""
    n = int(seconds * SR)
    if shape == "exp":
        f = f0 * (f1 / f0) ** (np.arange(n) / n)
    else:
        f = np.linspace(f0, f1, n)
    phase = 2 * np.pi * np.cumsum(f) / SR
    return np.sin(phase)


def fft_filter(sig, low=None, high=None, slope=12.0):
    """Nullphasiges Filtern im Frequenzbereich (Hoch-/Tiefpass mit weicher Flanke)."""
    n = len(sig)
    spec = np.fft.rfft(sig)
    freqs = np.fft.rfftfreq(n, 1 / SR)
    gain = np.ones_like(freqs)
    if low is not None:  # Tiefpass bei 'low'
        gain *= 1 / np.sqrt(1 + (freqs / low) ** (slope / 3))
    if high is not None:  # Hochpass bei 'high'
        with np.errstate(divide="ignore"):
            gain *= 1 / np.sqrt(1 + (high / np.maximum(freqs, 1e-3)) ** (slope / 3))
    return np.fft.irfft(spec * gain, n)


def svf_sweep(sig, f_start, f_end, q=4.0, mode="band"):
    """Zustandsvariablen-Filter mit gleitender Mittenfrequenz (für Wusch-Geräusche)."""
    n = len(sig)
    freqs = f_start * (f_end / f_start) ** (np.arange(n) / max(n - 1, 1))
    out = np.zeros(n)
    low = band = 0.0
    damp = 1.0 / q
    for i in range(n):
        f = 2 * np.sin(np.pi * min(freqs[i], SR / 6) / SR)
        high = sig[i] - low - damp * band
        band += f * high
        low += f * band
        out[i] = band if mode == "band" else (low if mode == "low" else high)
    return out


def reverb(sig, seconds=2.0, mix=0.35, brightness=6000):
    """Faltungshall mit exponentiell abklingendem, gefiltertem Rauschen."""
    n = int(seconds * SR)
    ir = RNG.standard_normal(n) * np.exp(-6.9 * np.arange(n) / n)
    ir = fft_filter(ir, low=brightness)
    ir /= np.sqrt(np.sum(ir ** 2))
    wet = np.fft.irfft(np.fft.rfft(sig, len(sig) + n) * np.fft.rfft(ir, len(sig) + n), len(sig) + n)
    dry = np.concatenate([sig, np.zeros(n)])
    return (1 - mix) * dry + mix * wet


def saturate(sig, drive=2.0):
    return np.tanh(sig * drive) / np.tanh(drive)


def normalize(sig, peak_db=-1.0):
    peak = np.max(np.abs(sig)) or 1.0
    return sig / peak * (10 ** (peak_db / 20))


def fade(sig, fade_in=0.005, fade_out=0.05):
    sig = sig.copy()
    a = int(fade_in * SR)
    b = int(fade_out * SR)
    if a:
        sig[:a] *= np.linspace(0, 1, a)
    if b:
        sig[-b:] *= np.linspace(1, 0, b)
    return sig


def trim(sig, threshold_db=-60):
    thr = 10 ** (threshold_db / 20) * np.max(np.abs(sig))
    idx = np.where(np.abs(sig) > thr)[0]
    return sig[: idx[-1] + int(0.05 * SR)] if len(idx) else sig


# ---------------------------------------------------------------------------------------------
# Klangquellen
# ---------------------------------------------------------------------------------------------

def bell(freq, seconds, partials=((1.0, 1.0, 1.0), (2.76, 0.6, 0.55), (5.40, 0.35, 0.35), (8.93, 0.2, 0.22), (13.34, 0.12, 0.15))):
    """Metallische Glocke mit unharmonischen Teiltönen und leichter Schwebung."""
    x = t(seconds)
    out = np.zeros_like(x)
    for ratio, amp, decay_ratio in partials:
        f = freq * ratio
        detune = 1 + RNG.uniform(-0.002, 0.002)
        out += amp * (np.sin(2 * np.pi * f * x) + 0.5 * np.sin(2 * np.pi * f * detune * x)) * np.exp(-x / (seconds * 0.35 * decay_ratio))
    return out * env_ad(len(x), 0.002, seconds, curve=1.5)


def tick(freq=3200, body=900, length=0.06, level=1.0):
    """Uhrwerk-Klick: heller Anschlag plus kurzer Holz-/Metallkörper."""
    x = t(length)
    click = RNG.standard_normal(len(x)) * np.exp(-x / 0.0025)
    click = fft_filter(click, high=freq * 0.6)
    tone = np.sin(2 * np.pi * body * x) * np.exp(-x / 0.012)
    ping = np.sin(2 * np.pi * freq * x) * np.exp(-x / 0.006) * 0.4
    return level * (0.7 * click + 0.5 * tone + ping)


def boom(seconds=1.6, f0=95, f1=28):
    """Tiefer Einschlag mit absinkender Tonhöhe und Sättigung."""
    x = t(seconds)
    body = sweep(f0, f1, seconds) * env_ad(len(x), 0.004, seconds * 0.6, curve=3.0)
    sub = np.sin(2 * np.pi * 42 * x) * env_ad(len(x), 0.01, seconds * 0.8, curve=3.5) * 0.6
    hit = fft_filter(RNG.standard_normal(len(x)), low=1800) * np.exp(-x / 0.02) * 0.8
    return saturate(body + sub + hit, 2.5)


def heartbeat(level=1.0):
    """Herzschlag (lub-dub)."""
    out = silence(0.9)
    for at, f, amp in ((0.0, 55, 1.0), (0.22, 48, 0.75)):
        x = t(0.18)
        thump = np.sin(2 * np.pi * f * x * (1 - 0.35 * x / 0.18)) * env_ad(len(x), 0.006, 0.12, curve=3.0)
        place(out, thump * amp, at)
    return fft_filter(out, low=300) * level


def noise(seconds):
    return RNG.standard_normal(int(seconds * SR))


# ---------------------------------------------------------------------------------------------
# Die eigentlichen Klänge
# ---------------------------------------------------------------------------------------------

def time_stop():
    """„Die Welt hält an“: rückwärts anschwellender Hall → Einschlag → Glocke → verhallendes Ticken."""
    total = silence(4.2)
    # 1) Rückwärts-Hall: ein Glockenschlag mit Hall, umgedreht, endet genau beim Einschlag
    swell_src = mix(bell(392, 0.4), 0.4 * tick(level=1.5))
    swell = reverb(np.concatenate([swell_src, silence(0.1)]), seconds=1.3, mix=0.9)[::-1]
    swell = swell[-int(0.85 * SR):]
    swell *= np.linspace(0.2, 1.0, len(swell)) ** 2
    place(total, normalize(swell, -6), 0.0)
    hit_at = 0.85
    # 2) Einschlag
    place(total, normalize(boom(1.8), -2), hit_at)
    # 3) Gong/Glocke tief, lange ausklingend
    gong = mix(bell(146.8, 3.0), 0.5 * bell(220.0, 2.4))
    place(total, normalize(reverb(gong, 2.5, 0.45), -7), hit_at)
    # 4) Hohes Schimmern (Zeit „friert“ ein)
    x = t(2.4)
    shimmer = sum(np.sin(2 * np.pi * f * x + RNG.uniform(0, 6.28)) for f in (2093, 2637, 3136, 4186)) * np.exp(-x / 0.8)
    shimmer *= 0.5 + 0.5 * np.sin(2 * np.pi * 7 * x)
    place(total, normalize(shimmer, -20), hit_at + 0.05)
    # 5) Verhallendes Uhr-Ticken in der Stille
    for i, at in enumerate((1.55, 2.05, 2.55, 3.05)):
        place(total, reverb(tick(level=1.0), 1.0, 0.5) * (0.5 * 0.7 ** i), hit_at + at - 0.85)
    return trim(normalize(fade(total, 0.01, 0.4), -1))


def time_resume():
    """„Die Zeit läuft weiter“: Aufzieh-Ratschen, rückwärts gesaugter Hall, dann Wusch."""
    total = silence(2.2)
    # Aufziehen: beschleunigende Ratsche
    at = 0.0
    gap = 0.11
    for i in range(9):
        place(total, tick(freq=2600 + i * 120, body=700 + i * 40, level=0.55), at)
        at += gap
        gap *= 0.86
    # Rückwärts gesaugter Hall mit Klick am Ende
    src = mix(tick(level=2.0), bell(523, 0.3) * 0.6)
    rev = reverb(np.concatenate([src, silence(0.05)]), 1.0, 0.9)[::-1]
    rev = rev[-int(0.6 * SR):] * np.linspace(0.1, 1, int(0.6 * SR)) ** 2
    place(total, normalize(rev, -5), 0.35)
    # Freigabe: Wusch nach oben + tiefer Puls
    w = svf_sweep(noise(1.1), 300, 6000, q=3) * env_ad(int(1.1 * SR), 0.02, 0.8, curve=2.0)
    place(total, normalize(w, -6), 0.92)
    place(total, normalize(boom(0.9, 70, 40), -8), 0.95)
    return trim(normalize(fade(total, 0.01, 0.3), -1))


def clock_tick():
    return normalize(fade(tick(), 0.0005, 0.01), -3)


def clock_wind():
    out = silence(0.8)
    at, gap = 0.0, 0.07
    for i in range(10):
        place(out, tick(freq=2400 + 50 * i, body=650, level=0.8), at)
        at += gap
    return trim(normalize(fade(out, 0.001, 0.05), -3))


def heartbeat_sound():
    return normalize(fade(heartbeat(), 0.001, 0.05), -3)


def stop_ambience():
    """Loop für die Stille im Zeitstopp: tiefes Brummen, Herzschlag, fernes Ticken (4 s)."""
    length = 4.0
    x = t(length)
    drone = (np.sin(2 * np.pi * 55 * x) + 0.5 * np.sin(2 * np.pi * 55.4 * x) + 0.25 * np.sin(2 * np.pi * 110.3 * x))
    drone = fft_filter(drone, low=200) * 0.25
    out = drone.copy()
    for at in (0.2, 2.2):
        place(out, heartbeat(0.9), at)
    for at in (1.0, 3.0):
        place(out, reverb(tick(level=0.25), 0.8, 0.6)[: int(0.9 * SR)], at)
    out = out[: len(x)]
    # nahtlos loopen: Anfang und Ende überblenden
    xf = int(0.05 * SR)
    out[:xf] = out[:xf] * np.linspace(0, 1, xf) + out[-xf:] * np.linspace(1, 0, xf)
    return normalize(out[:-xf], -4)


def fast_forward():
    """Vorspulen: steigender Wusch mit flatterndem Band-Klang."""
    n = int(1.3 * SR)
    w = svf_sweep(noise(1.3), 200, 7000, q=5) * env_ad(n, 0.05, 1.0, curve=2.5)
    x = t(1.3)
    flutter = sweep(300, 2400, 1.3) * (0.5 + 0.5 * np.sin(2 * np.pi * 24 * x)) * env_ad(n, 0.05, 1.0, curve=3) * 0.25
    return trim(normalize(fade(w + flutter, 0.01, 0.2), -2))


def slow_motion():
    """Zeitlupe: tief absinkender Wusch mit dumpfem Nachhall."""
    n = int(1.8 * SR)
    w = svf_sweep(noise(1.8), 4000, 120, q=4, mode="band") * env_ad(n, 0.04, 1.4, curve=2.0)
    low = sweep(160, 40, 1.8) * env_ad(n, 0.05, 1.4, curve=2.5) * 0.6
    return trim(normalize(fade(reverb(w + low, 1.8, 0.4), 0.01, 0.3), -2))


def rewind():
    """Zurückspulen: Band-Rückspulen mit zirpendem, wackelndem Ton."""
    length = 1.6
    x = t(length)
    wobble = 1 + 0.35 * np.sin(2 * np.pi * 9 * x) + 0.15 * np.sin(2 * np.pi * 23 * x)
    f = 900 * wobble * (1 + 0.8 * x / length)
    saw = 2 * ((np.cumsum(f) / SR) % 1.0) - 1
    chirp = fft_filter(saw, low=5000, high=300) * 0.4
    hiss = svf_sweep(noise(length), 6000, 1500, q=2) * 0.5
    out = (chirp + hiss) * env_ad(len(x), 0.03, length, curve=1.2)
    return trim(normalize(fade(out, 0.01, 0.15), -2))


def zone_hum():
    """Leises Summen einer aktiven Zeitzone (Loop, 2 s)."""
    x = t(2.0)
    hum = sum(a * np.sin(2 * np.pi * f * x) for f, a in ((110, 1.0), (165.2, 0.5), (220.4, 0.3), (330, 0.15)))
    hum *= 0.75 + 0.25 * np.sin(2 * np.pi * 1.0 * x)
    return normalize(fft_filter(hum, low=900), -10)


# ---------------------------------------------------------------------------------------------
# Bosse
# ---------------------------------------------------------------------------------------------

def clockmaker_roar():
    """Uhrmacher: mechanisches Brüllen – tiefe Dampfhorn-Stimme, rasselnde Zahnräder, Glocke."""
    x = t(2.2)
    pitch = 70 + 18 * np.sin(2 * np.pi * 0.7 * x) - 15 * x
    phase = 2 * np.pi * np.cumsum(pitch) / SR
    voice = sum(a * np.sin(k * phase) for k, a in ((1, 1.0), (2, 0.7), (3, 0.5), (5, 0.3), (7, 0.2)))
    voice = svf_sweep(saturate(voice, 3.0), 300, 1200, q=3.0) * env_ad(len(x), 0.08, 1.6, curve=2.0)
    rattle = silence(2.2)
    for i in range(26):
        place(rattle, tick(freq=2600 + 400 * RNG.random(), body=500, level=0.5), 0.05 + i * 0.075 + RNG.uniform(0, 0.02))
    out = mix(normalize(voice, -3), normalize(rattle, -10), normalize(bell(98, 2.0), -12))
    return normalize(trim(reverb(out, 1.6, 0.3)), -1)


def gear_saw():
    """Zahnrad-Säge: kreischende, sich drehende Metallscheibe."""
    x = t(0.9)
    whine = sweep(900, 1500, 0.9) * (0.6 + 0.4 * np.sin(2 * np.pi * 38 * x))
    grit = fft_filter(noise(0.9), high=2500) * 0.35 * (0.5 + 0.5 * np.sin(2 * np.pi * 38 * x))
    out = (whine + grit) * env_ad(len(x), 0.02, 0.7, curve=2.5)
    return normalize(fade(out, 0.01, 0.15), -3)


def phage_roar():
    """Chronophage: tiefes, gurgelndes Grollen, zeitverzerrt (rückwärts anschwellend)."""
    x = t(2.6)
    growl = noise(2.6) * (0.6 + 0.4 * np.sin(2 * np.pi * 23 * x))
    growl = svf_sweep(growl, 120, 480, q=5.0) * 2.5
    tone = sweep(55, 38, 2.6) * 0.6
    body = saturate(growl + tone, 2.0) * env_ad(len(x), 0.4, 2.0, curve=2.0)
    swell = reverb(body[: int(0.9 * SR)], 1.0, 0.8)[::-1][-int(0.6 * SR):] * 0.5
    out = np.concatenate([swell, body])
    return normalize(trim(reverb(out, 2.2, 0.35, brightness=2500)), -1)


def phage_open():
    """Das Maul öffnet sich: Uhrwerk spult hoch, endet in einem hohlen Seufzen."""
    x = t(1.6)
    rise = sweep(200, 1400, 1.6) * np.linspace(0.2, 1.0, len(x)) ** 2
    ticks = silence(1.6)
    for i in range(22):
        place(ticks, tick(level=0.6), 1.5 * (i / 22) ** 0.6)
    breath = svf_sweep(noise(1.6), 300, 2000, q=2.0) * np.linspace(0, 1, len(x)) * 0.5
    return normalize(fade(mix(normalize(rise, -6), normalize(ticks, -8), breath), 0.02, 0.2), -2)


def phage_bite():
    """Zuschnappen: zwei Kiefer schlagen zusammen."""
    out = silence(0.5)
    place(out, normalize(boom(0.4, 160, 60), -4), 0.0)
    place(out, tick(freq=1800, body=300, level=1.2), 0.02)
    crack = fft_filter(noise(0.08), high=1500) * np.exp(-t(0.08) / 0.01)
    place(out, crack, 0.03)
    return normalize(out, -1)


def phage_drain():
    """Zeit wird gesaugt: rückwärts laufende Glocke, absinkender Ton."""
    src = mix(bell(660, 1.2), bell(990, 1.0) * 0.5)
    reversed_bell = src[::-1] * 0.8
    sink = sweep(1200, 180, 1.2) * env_ad(int(1.2 * SR), 0.05, 1.0, curve=2.0) * 0.5
    return normalize(fade(mix(reversed_bell, sink), 0.01, 0.1), -2)


def boss_awaken():
    """Erwachen eines Bosses: Rückwärtsschwall, Einschlag, tiefer Glockenschlag."""
    total = silence(4.0)
    swell = reverb(np.concatenate([bell(196, 0.5), silence(0.1)]), 1.5, 0.9)[::-1][-int(1.0 * SR):]
    swell *= np.linspace(0.1, 1.0, len(swell)) ** 2
    place(total, normalize(swell, -5), 0.0)
    place(total, normalize(boom(2.0, 80, 24), -1), 1.0)
    place(total, normalize(bell(65.4, 2.8), -6), 1.05)
    return normalize(trim(total), -1)


def paradox_voice():
    """Paradox: verstimmter, rückwärts anschwellender Akkord, darüber ein Flüstern – die eigene Stimme, verzerrt."""
    x = t(2.4)
    chord = sum(np.sin(2 * np.pi * f * x * (1 + 0.004 * np.sin(2 * np.pi * 0.7 * x + i))) for i, f in enumerate((220.0, 261.6, 329.6, 415.3)))
    chord *= env_ad(len(x), 0.6, 1.8, curve=1.8)
    swell = reverb(chord[: int(1.0 * SR)], 1.2, 0.9)[::-1][-int(0.7 * SR):] * np.linspace(0.1, 1, int(0.7 * SR)) ** 2
    whisper = svf_sweep(noise(2.4), 2400, 900, q=6.0) * (0.5 + 0.5 * np.sin(2 * np.pi * 5.5 * x)) * env_ad(len(x), 0.3, 1.6, curve=2.0)
    body = mix(normalize(chord, -6), normalize(whisper, -12))
    out = np.concatenate([normalize(swell, -8), body])
    return normalize(trim(reverb(out, 2.0, 0.4, brightness=4000)), -1)


def paradox_blink():
    """Zeitsprung: kurzer Wusch nach oben, am Ende ein rückwärts gezogener Glockenton."""
    w = svf_sweep(noise(0.4), 500, 7000, q=4.0) * env_ad(int(0.4 * SR), 0.02, 0.3, curve=3.0)
    ping = bell(1318.5, 0.5)[::-1] * 0.6
    out = silence(0.7)
    place(out, normalize(ping, -8), 0.0)
    place(out, normalize(w, -3), 0.42)
    return normalize(fade(out, 0.005, 0.08), -1)


def echo_shatter():
    """Ein Echo zerspringt: viele kurze, hohe Glasklänge und ein heller Rauschstoß."""
    out = silence(0.8)
    for i in range(9):
        f = RNG.uniform(1800, 5200)
        place(out, bell(f, 0.35) * RNG.uniform(0.3, 0.8), RNG.uniform(0.0, 0.12))
    burst = fft_filter(noise(0.15), low=2500) * np.exp(-t(0.15) / 0.04)
    place(out, burst * 0.6, 0.0)
    return normalize(fade(reverb(out, 0.8, 0.25, brightness=9000), 0.002, 0.1), -2)



def gate_charge():
    """Das Tor lädt sich auf (10,5 s): Zahnräder setzen sich in Gang, ein Ton steigt, das Ticken rast, dann saugt die Luft ein."""
    length = 10.5
    total = silence(length)
    x = t(10.0)
    ramp = np.linspace(0, 1, len(x))
    # Tiefes Brummen, das sich hochschraubt
    drone = sweep(48, 196, 10.0) + 0.5 * sweep(96, 392, 10.0) + 0.25 * sweep(144.5, 588, 10.0)
    drone = fft_filter(drone, low=900) * (0.25 + 0.75 * ramp ** 1.5)
    place(total, normalize(drone, -9), 0.0)
    # Heller Ton, der zum Ende hin ansteigt
    whine = sweep(180, 2600, 10.0) * ramp ** 2.2 * (0.8 + 0.2 * np.sin(2 * np.pi * 6 * x))
    place(total, normalize(whine, -16), 0.0)
    # Mahlende Zahnräder
    grind = svf_sweep(noise(10.0), 250, 3200, q=3.0) * ramp ** 1.3 * (0.6 + 0.4 * np.abs(np.sin(2 * np.pi * 9 * x * (1 + 2 * ramp))))
    place(total, normalize(grind, -14), 0.0)
    # Immer schnelleres Ticken
    at, gap, i = 0.2, 0.62, 0
    while at < 9.9:
        place(total, tick(freq=2200 + 1600 * at / 10, body=600 + 500 * at / 10, level=0.35 + 0.5 * at / 10), at)
        at += gap
        gap = max(0.028, gap * 0.9)
        i += 1
    # Zum Schluss saugt die Luft ein (rückwärts gespielter Hall)
    src = mix(bell(523.25, 0.4), 0.6 * bell(784.0, 0.35), tick(level=2.0))
    suck = reverb(np.concatenate([src, silence(0.05)]), 1.4, 0.95)[::-1][-int(1.4 * SR):]
    suck *= np.linspace(0.05, 1.0, len(suck)) ** 2
    place(total, normalize(suck, -4), length - 1.45)
    return normalize(fade(total, 0.3, 0.02), -1)


def gate_open():
    """Das Portal öffnet sich: Einschlag, ein Akkord aus Glocken, helles Schimmern, Wind nach außen."""
    total = silence(4.0)
    place(total, normalize(boom(2.0, 110, 30), -2), 0.0)
    chord = mix(bell(261.63, 3.2), 0.8 * bell(329.63, 3.0), 0.7 * bell(392.0, 2.8), 0.4 * bell(523.25, 2.6))
    place(total, normalize(reverb(chord, 2.6, 0.45), -5), 0.02)
    x = t(3.0)
    shimmer = sum(np.sin(2 * np.pi * f * x + RNG.uniform(0, 6.28)) for f in (1568, 2093, 2637, 3136, 4186)) * np.exp(-x / 1.1)
    shimmer *= 0.6 + 0.4 * np.sin(2 * np.pi * 11 * x)
    place(total, normalize(shimmer, -18), 0.05)
    wind = svf_sweep(noise(2.0), 5000, 400, q=2.5) * env_ad(int(2.0 * SR), 0.05, 1.6, curve=2.0)
    place(total, normalize(wind, -9), 0.0)
    return normalize(trim(fade(total, 0.002, 0.4)), -1)


def gate_close():
    """Das Portal fällt in sich zusammen: Wusch nach innen, dumpfer Schlag, eine rückwärts verklingende Glocke."""
    total = silence(1.8)
    w = svf_sweep(noise(0.9), 4500, 300, q=3.0) * np.linspace(0.2, 1.0, int(0.9 * SR)) ** 2
    place(total, normalize(w, -5), 0.0)
    place(total, normalize(boom(0.9, 70, 32), -4), 0.85)
    place(total, normalize(bell(392.0, 0.6)[::-1], -14), 0.3)
    return normalize(trim(fade(total, 0.01, 0.2)), -1)


def gate_arrive():
    """Ankunft: ein weicher Wusch, eine helle Glocke, Schimmern, das verweht."""
    total = silence(2.0)
    w = svf_sweep(noise(0.7), 600, 4500, q=3.0) * env_ad(int(0.7 * SR), 0.15, 0.5, curve=2.5)
    place(total, normalize(w, -8), 0.0)
    place(total, normalize(reverb(bell(659.25, 1.4), 1.6, 0.4), -7), 0.18)
    x = t(1.5)
    shimmer = sum(np.sin(2 * np.pi * f * x) for f in (2637, 3520, 4186)) * np.exp(-x / 0.5)
    place(total, normalize(shimmer, -20), 0.2)
    return normalize(trim(fade(total, 0.01, 0.3)), -2)


def console_click():
    """Eine Taste der Konsole: zwei mechanische Klicks und ein winziger Glockenton."""
    out = silence(0.25)
    place(out, tick(freq=2900, body=1300, length=0.05, level=1.0), 0.0)
    place(out, tick(freq=2000, body=900, length=0.05, level=0.6), 0.035)
    place(out, bell(1760.0, 0.18) * 0.25, 0.04)
    return normalize(fade(out, 0.001, 0.04), -4)


def time_stream():
    """Flug durch den Zeitstrom (8 s): rauschender Tunnel, tiefes Pulsieren, rückwärts laufende Uhren, ferne Glocken."""
    length = 8.0
    total = silence(length)
    x = t(length)
    swell = np.minimum(1.0, x / 1.2) * np.minimum(1.0, (length - x) / 1.8)
    # Rauschen, dessen Klangfarbe auf und ab schwingt
    rush = svf_sweep(noise(length), 350, 2600, q=2.2) * (0.6 + 0.4 * np.sin(2 * np.pi * 0.35 * x)) * swell
    place(total, normalize(rush, -9), 0.0)
    wind = svf_sweep(noise(length), 3200, 700, q=3.0) * swell * 0.6
    place(total, normalize(wind, -14), 0.0)
    # Tiefes, pulsierendes Brummen
    drone = (np.sin(2 * np.pi * 55 * x + 0.8 * np.sin(2 * np.pi * 0.5 * x)) + 0.6 * np.sin(2 * np.pi * 82.4 * x)
             + 0.3 * np.sin(2 * np.pi * 110.7 * x)) * (0.7 + 0.3 * np.sin(2 * np.pi * 2.2 * x)) * swell
    place(total, normalize(fft_filter(drone, low=600), -10), 0.0)
    # Rückwärts tickende Uhren, mal schneller, mal langsamer
    at = 0.4
    while at < length - 1.2:
        click = tick(freq=1800 + 900 * np.sin(at), body=700, level=0.45)[::-1]
        place(total, click * float(swell[min(len(swell) - 1, int(at * SR))]), at)
        at += 0.09 + 0.07 * (1 + np.sin(at * 1.7))
    # Ferne Glocken, rückwärts angehaucht
    for i, (when, freq) in enumerate(((1.6, 783.99), (3.4, 659.25), (5.0, 523.25), (6.2, 1046.5))):
        ring = reverb(bell(freq, 0.9), 1.4, 0.6)[::-1]
        place(total, normalize(ring, -20 - 2 * i), when - 0.9)
    return normalize(fade(total, 0.3, 1.2), -2)

SOUNDS = {
    "time_stop": time_stop,
    "time_resume": time_resume,
    "clock_tick": clock_tick,
    "clock_wind": clock_wind,
    "heartbeat": heartbeat_sound,
    "stop_ambience": stop_ambience,
    "fast_forward": fast_forward,
    "slow_motion": slow_motion,
    "rewind": rewind,
    "zone_hum": zone_hum,
    "clockmaker_roar": clockmaker_roar,
    "gear_saw": gear_saw,
    "phage_roar": phage_roar,
    "phage_open": phage_open,
    "phage_bite": phage_bite,
    "phage_drain": phage_drain,
    "boss_awaken": boss_awaken,
    "paradox_voice": paradox_voice,
    "paradox_blink": paradox_blink,
    "echo_shatter": echo_shatter,
    "gate_charge": gate_charge,
    "gate_open": gate_open,
    "gate_close": gate_close,
    "gate_arrive": gate_arrive,
    "console_click": console_click,
    "time_stream": time_stream,
}


def write_ogg(name, sig):
    pcm = np.clip(sig, -1, 1)
    pcm16 = (pcm * 32767).astype("<i2")
    OUT.mkdir(parents=True, exist_ok=True)
    with tempfile.TemporaryDirectory() as tmp:
        wav_path = Path(tmp) / f"{name}.wav"
        with wave.open(str(wav_path), "wb") as w:
            w.setnchannels(1)
            w.setsampwidth(2)
            w.setframerate(SR)
            w.writeframes(pcm16.tobytes())
        subprocess.run(["ffmpeg", "-hide_banner", "-loglevel", "error", "-y", "-i", str(wav_path),
                        "-c:a", "libvorbis", "-q:a", "6", str(OUT / f"{name}.ogg")], check=True)


def main(names):
    for name, fn in SOUNDS.items():
        if names and name not in names:
            continue
        sig = fn()
        write_ogg(name, sig)
        print(f"{name}: {len(sig) / SR:.2f} s")


if __name__ == "__main__":
    main(set(sys.argv[1:]))
