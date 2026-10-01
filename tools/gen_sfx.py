#!/usr/bin/env python3
"""Generates the game's playful sound effects as WAV files (no external assets)."""
import math, random, struct, wave, os, sys

RATE = 22050
OUT = sys.argv[1] if len(sys.argv) > 1 else "godot/sfx"

def tone(dur, f0, f1, vol, vib_hz=0, vib_amt=0, harm=.15):
    n = int(dur * RATE); out = []; ph = 0.0
    for i in range(n):
        u = i / n
        f = f0 + (f1 - f0) * u + math.sin(i * 2 * math.pi * vib_hz / RATE) * vib_amt
        ph += 2 * math.pi * f / RATE
        env = min(1, i / (RATE * .004)) * math.exp(-u * 4)
        out.append((math.sin(ph) * .8 + math.sin(ph * 2) * harm) * env * vol)
    return out

def noise(dur, vol, shape, k0=.05, k1=.25, seed=3):
    r = random.Random(seed); n = int(dur * RATE); out = []; lp = 0.0
    for i in range(n):
        u = i / n; env = shape(u); k = k0 + k1 * env
        lp += ((r.random() * 2 - 1) - lp) * k
        out.append(lp * env * vol)
    return out

def pluck(freq, dur, vol=.7):
    n = int(dur * RATE); period = int(RATE / freq); r = random.Random(1)
    buf = [r.random() * 2 - 1 for _ in range(period)]; out = []
    for i in range(n):
        v = buf[i % period]; nxt = buf[(i + 1) % period]
        buf[i % period] = .996 * .5 * (v + nxt)
        out.append(v * vol * (1 - i / n))
    return out

def save(name, s):
    os.makedirs(OUT, exist_ok=True)
    with wave.open(os.path.join(OUT, name + ".wav"), "w") as w:
        w.setnchannels(1); w.setsampwidth(2); w.setframerate(RATE)
        w.writeframes(b"".join(struct.pack("<h", int(max(-1, min(1, v)) * 30000)) for v in s))

save("pop", tone(.09, 420, 980, .9))
save("drop", tone(.13, 220, 90, 1))
save("bite", noise(.12, .9, lambda u: (1 - u / .45) if u < .45 else (0 if u < .5 else max(0, 1 - (u - .5) / .4)), .35, 0))
save("toggle", tone(.04, 900, 900, .6) + tone(.06, 1350, 1350, .6))
save("tick", tone(.035, 1500, 1300, .45))
save("bounce", tone(.18, 260, 620, .8, 18, 40))
save("spark", tone(.06, 1047, 1047, .5) + tone(.06, 1319, 1319, .5) + tone(.12, 1568, 1568, .5))
save("whoosh", noise(.28, .9, lambda u: math.sin(math.pi * u), .05, .25, 7))
save("splash", noise(.45, .8, lambda u: (1 - u) ** 2, .15, .5, 11))
save("tada", tone(.08, 784, 784, .5) + tone(.08, 988, 988, .5) + tone(.22, 1175, 1175, .55))
save("strum", [a + b + c for a, b, c in zip(pluck(196, .9), pluck(247, .9), pluck(294, .9))])
save("squeak", tone(.12, 1200, 1800, .5, 30, 120))
save("wheee", tone(.6, 500, 1400, .5, 6, 30))
save("vroom", [v * .6 for v in tone(.5, 70, 160, 1, 9, 12, .6)])
save("yawn", tone(.7, 520, 260, .45, 5, 15))
save("swoosh_map", noise(.18, .6, lambda u: math.sin(math.pi * u), .1, .4, 5))
save("meow", tone(.18, 620, 900, .45, 7, 30, .35) + tone(.32, 900, 520, .45, 7, 30, .35))
save("woof", [a * .9 + b * .5 for a, b in zip(tone(.16, 240, 150, 1, 0, 0, .7), noise(.16, .8, lambda u: 1 - u, .3, .3, 21))]
     + [0.0] * 1500 + [a * .9 + b * .5 for a, b in zip(tone(.14, 260, 160, 1, 0, 0, .7), noise(.14, .8, lambda u: 1 - u, .3, .3, 22))])
save("sizzle", noise(.9, .5, lambda u: min(1, u * 8) * (1 - u) ** .5, .6, .35, 31))
save("blend", [a * .5 + b * .4 for a, b in zip(tone(.9, 150, 210, 1, 30, 20, .9), noise(.9, 1, lambda u: min(1, u * 6) * (1 - u * .4), .2, .3, 41))])
print("sounds written to", OUT)
