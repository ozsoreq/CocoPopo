package com.cocopopo.app;

import java.util.Random;

/** Tiny procedural sound effects (bloops, pops and boings) so the game needs no audio files. */
final class Synth {
    static final int RATE = 22050;

    private Synth() {}

    static short[] make(int id) {
        switch (id) {
            case Game.S_POP: return tone(.09f, 420, 980, .9f, 0, 0);
            case Game.S_DROP: return tone(.13f, 220, 90, 1f, 0, 0);
            case Game.S_BITE: return crunch();
            case Game.S_TOGGLE: return concat(tone(.04f, 900, 900, .6f, 0, 0), tone(.06f, 1350, 1350, .6f, 0, 0));
            case Game.S_TICK: return tone(.035f, 1500, 1300, .45f, 0, 0);
            case Game.S_BOUNCE: return tone(.18f, 260, 620, .8f, 18, 40);
            case Game.S_SPARK: return concat(concat(tone(.06f, 1047, 1047, .5f, 0, 0), tone(.06f, 1319, 1319, .5f, 0, 0)), tone(.12f, 1568, 1568, .5f, 0, 0));
            default: return whoosh();
        }
    }

    /** Sine sweep from f0 to f1 with a soft attack, exponential decay and optional vibrato. */
    private static short[] tone(float dur, float f0, float f1, float vol, float vibHz, float vibAmt) {
        int n = (int) (dur * RATE);
        short[] out = new short[n];
        double ph = 0;
        for (int i = 0; i < n; i++) {
            float u = i / (float) n;
            double f = f0 + (f1 - f0) * u + Math.sin(i * 2 * Math.PI * vibHz / RATE) * vibAmt;
            ph += 2 * Math.PI * f / RATE;
            float env = Math.min(1, i / (RATE * .004f)) * (float) Math.exp(-u * 4);
            double v = Math.sin(ph) * .8 + Math.sin(ph * 2) * .15;
            out[i] = (short) (v * env * vol * 26000);
        }
        return out;
    }

    private static short[] crunch() {
        Random r = new Random(3);
        int n = (int) (.12f * RATE);
        short[] out = new short[n];
        float lp = 0;
        for (int i = 0; i < n; i++) {
            float u = i / (float) n;
            float burst = (u < .45f ? 1 - u / .45f : Math.max(0, 1 - (u - .5f) / .4f)) * (u > .45f && u < .5f ? 0 : 1);
            lp += ((r.nextFloat() * 2 - 1) - lp) * .35f;
            out[i] = (short) (lp * burst * 22000);
        }
        return out;
    }

    private static short[] whoosh() {
        Random r = new Random(7);
        int n = (int) (.28f * RATE);
        short[] out = new short[n];
        float lp = 0;
        for (int i = 0; i < n; i++) {
            float u = i / (float) n;
            float env = (float) Math.sin(Math.PI * u);
            float k = .05f + .25f * env;
            lp += ((r.nextFloat() * 2 - 1) - lp) * k;
            out[i] = (short) (lp * env * 26000);
        }
        return out;
    }

    private static short[] concat(short[] a, short[] b) {
        short[] o = new short[a.length + b.length];
        System.arraycopy(a, 0, o, 0, a.length);
        System.arraycopy(b, 0, o, a.length, b.length);
        return o;
    }
}
