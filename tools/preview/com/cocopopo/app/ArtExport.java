package com.cocopopo.app;

import android.graphics.Canvas;
import java.io.File;
import java.io.FileWriter;
import java.util.LinkedHashMap;
import java.util.Map;

/** Exports the code-drawn art as SVG files (plus pivot manifest) for the Godot version of the game. */
public final class ArtExport {
    interface Draw { void draw(Canvas c); }

    private static final Map<String, float[]> manifest = new LinkedHashMap<>();
    private static File dir;
    private static final int W = 0xFFFFFFFF;

    static void export(String name, Draw d) throws Exception {
        StringBuilder body = new StringBuilder();
        Canvas c = new Canvas(body);
        Gfx.olc = 0xFF2A1F2E;
        d.draw(c);
        Gfx.ol(0);
        if (c.bounds == null) return;
        double pad = 14;
        double x = Math.floor(c.bounds.getMinX() - pad), y = Math.floor(c.bounds.getMinY() - pad);
        double w = Math.ceil(c.bounds.getMaxX() + pad) - x, h = Math.ceil(c.bounds.getMaxY() + pad) - y;
        String svg = "<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"" + (int) w + "\" height=\"" + (int) h
            + "\" viewBox=\"" + (int) x + " " + (int) y + " " + (int) w + " " + (int) h + "\">\n" + body + "</svg>\n";
        try (FileWriter fw = new FileWriter(new File(dir, name + ".svg"))) { fw.write(svg); }
        manifest.put(name, new float[]{(float) x, (float) y, (float) w, (float) h});
    }

    /** Rounded, slightly pear-shaped torso (hips at y = -98 in torso space). */
    static void torsoShape(Canvas c, int col) {
        android.graphics.Path p = Gfx.PA;
        p.reset();
        p.moveTo(-34, -197);
        p.lineTo(34, -197);
        p.cubicTo(52, -197, 58, -186, 57, -168);
        p.cubicTo(58, -140, 64, -112, 62, -96);
        p.cubicTo(61, -86, 52, -82, 40, -82);
        p.lineTo(-40, -82);
        p.cubicTo(-52, -82, -61, -86, -62, -96);
        p.cubicTo(-64, -112, -58, -140, -57, -168);
        p.cubicTo(-58, -186, -52, -197, -34, -197);
        p.close();
        Gfx.path(c, p, col);
    }

    static void pocket(Canvas c, int col) {
        android.graphics.Path p = Gfx.PA;
        p.reset();
        p.moveTo(-32, -126); p.quadTo(0, -134, 32, -126); p.lineTo(28, -100); p.quadTo(0, -96, -28, -100); p.close();
        Gfx.path(c, p, col);
    }

    /** A tapered limb from y0 down to y1 (top half-width w0, bottom half-width w1), rounded at both ends. */
    static void limb(Canvas c, float w0, float w1, float y0, float y1, int col) {
        android.graphics.Path p = Gfx.PA;
        p.reset();
        p.moveTo(-w0, y0 + w0 * .6f);
        p.quadTo(-w0, y0, 0, y0);
        p.quadTo(w0, y0, w0, y0 + w0 * .6f);
        p.lineTo(w1, y1 - w1 * .8f);
        p.quadTo(w1, y1, 0, y1);
        p.quadTo(-w1, y1, -w1, y1 - w1 * .8f);
        p.close();
        Gfx.path(c, p, col);
    }

    public static void main(String[] a) throws Exception {
        dir = new File(a.length > 0 ? a[0] : "godot/art");
        dir.mkdirs();
        Scenes.exportMode = true;

        // ---------------- props (+ states, fronts, blankets)
        for (PropArt.Def d : PropArt.ALL) {
            final String id = d.id;
            export("prop_" + id, c -> { Gfx.ol(5); Gfx.shade = true; PropArt.draw(c, id, 1.2f, 0); });
            int n = Math.max(Life.states(id), 1);
            for (int st = 1; st < n; st++) {
                final int s = st;
                export("prop_" + id + "_" + st, c -> { Gfx.ol(5); Gfx.shade = true; PropArt.draw(c, id, 1.2f, s); });
            }
        }
        for (String id : new String[]{"tub", "car"}) export("front_" + id, c -> { Gfx.ol(5); Gfx.shade = true; PropArt.front(c, id, 0); });
        for (String id : new String[]{"bed", "hbed"}) export("blanket_" + id, c -> { Gfx.ol(5); Gfx.shade = true; PropArt.blanket(c, id); });

        // ---------------- backgrounds and map buildings
        for (Scenes.Loc l : Scenes.ALL) {
            export("bg_" + l.id, c -> { Gfx.shade = false; Scenes.drawBg(c, l.id, 2560, 1080, 0); });
            export("place_" + l.id, c -> { Gfx.ol(5.5f); Gfx.shade = true; Scenes.drawIcon(c, l.id, 0, -80, 1, 0); });
        }
        export("cloud", c -> { Gfx.ol(0); Scenes.cloud(c, 0, 0, 1, W); });

        // ---------------- UI glyphs + emote bubbles
        for (int i = 0; i <= 30; i++) { final int id = i; export("icon_" + i, c -> Icons.draw(c, id, 0, 0, 1, W)); }
        for (int i = 0; i < 7; i++) { final int k = i; export("emote_" + i, c -> Icons.emote(c, k, 0, 0, 1, 0)); }

        // ---------------- character parts, white based (tinted in Godot), v2 "smooth" design:
        // thinner outlines, tapered limbs, rounded torso; no banded shading (Godot adds soft gradient shading).
        final float OL = 4.5f;
        Look l = Look.PRESETS[0].copy();
        l.accColor = 9; // white
        int skinD = Gfx.dk(W, .13f);
        // torso space: origin = hips
        for (int top = 0; top < Look.N_TOP; top++) {
            final int tp = top;
            export("torso_" + top, c -> {
                Gfx.ol(OL); Gfx.shade = false; c.translate(0, 98);
                if (tp == 3 || tp == 5) { Look q = l.copy(); q.top = tp; Avatar.torso(c, q, W, W, W); return; }
                if (tp == 2) Gfx.ov(c, 0, -190, 52, 26, Gfx.dk(W, .16f));       // hood behind
                torsoShape(c, W);
                if (tp == 2) {
                    Gfx.ol(0);
                    pocket(c, Gfx.dk(W, .1f));
                    Gfx.ln(c, -13, -176, -15, -150, 4, Gfx.dk(W, .25f));
                    Gfx.ln(c, 13, -176, 15, -150, 4, Gfx.dk(W, .25f));
                    Gfx.ol(OL);
                }
                Gfx.ol(0);
                Gfx.ov(c, 0, -194, 24, 13, W);                                     // neck opening (covered by the neck)
                Gfx.arc(c, 0, -194, 24, 13, 0, 180, 4, Gfx.dk(W, .18f));
            });
        }
        export("torso_1s", c -> { Gfx.ol(0); c.translate(0, 98); for (int i = 0; i < 4; i++) Gfx.rr(c, -50, -172 + i * 24, 100, 10, 5, W); });
        export("torso_4b", c -> { Gfx.ol(OL); Gfx.shade = false; c.translate(0, 98);
            Gfx.rr(c, -42, -156, 84, 74, 24, W); Gfx.rr(c, -42, -194, 14, 52, 7, W); Gfx.rr(c, 28, -194, 14, 52, 7, W);
            Gfx.ol(0); Gfx.rr(c, -15, -130, 30, 24, 9, Gfx.dk(W, .12f)); });
        export("buttons_4", c -> { Gfx.ol(0); c.translate(0, 98); Gfx.ci(c, -35, -150, 5.5f, 0xFFFFD43B); Gfx.ci(c, 35, -150, 5.5f, 0xFFFFD43B); });
        export("skirt", c -> { Gfx.ol(OL); Gfx.shade = false; c.translate(0, 98); Avatar.skirt(c, W); });
        // arms: origin = shoulder; tapered sleeve
        export("sleeve", c -> { Gfx.ol(OL); Gfx.shade = false; limb(c, 16, 13, -8, 76, W); Gfx.ol(0); Gfx.rr(c, -13, 62, 26, 7, 3.5f, Gfx.dk(W, .08f)); });
        export("arm_bare", c -> { Gfx.ol(OL); Gfx.shade = false; limb(c, 14, 12, -8, 76, W); });
        export("mitten_r", c -> { Gfx.ol(OL); Gfx.shade = false; Avatar.mitten(c, 1, W); });
        export("mitten_l", c -> { Gfx.ol(OL); Gfx.shade = false; Avatar.mitten(c, -1, W); });
        // legs: origin = hip joint, kid length 74; tapered
        export("leg_pants", c -> { Gfx.ol(OL); Gfx.shade = false; limb(c, 20, 16, 0, 78, W); Gfx.ol(0); Gfx.rr(c, -16, 64, 32, 7, 3.5f, Gfx.dk(W, .08f)); });
        export("leg_bare", c -> { Gfx.ol(OL); Gfx.shade = false; limb(c, 17, 14, 0, 78, W); });
        export("shorts", c -> { Gfx.ol(OL); Gfx.shade = false; limb(c, 21, 19, 0, 34, W); });
        // shoe: origin = shoe centre; rounded sneaker
        export("shoe", c -> {
            Gfx.ol(OL); Gfx.shade = false;
            Gfx.ov(c, 0, 6, 31, 12, Gfx.dk(W, .28f));
            Gfx.PA.reset();
            Gfx.PA.moveTo(-27, 6); Gfx.PA.cubicTo(-29, -16, 27, -18, 29, 4); Gfx.PA.quadTo(29, 8, 24, 8); Gfx.PA.lineTo(-23, 8); Gfx.PA.quadTo(-28, 8, -27, 6); Gfx.PA.close();
            Gfx.path(c, Gfx.PA, W);
            Gfx.ol(0);
            Gfx.ov(c, -8, -6, 9, 4, 0x55FFFFFF);
            Gfx.ln(c, -2, -8, 8, -9, 2.5f, Gfx.dk(W, .2f));
        });
        // head space: origin = neck (head code is written with the neck at y = -196)
        export("neck", c -> { Gfx.ol(OL); Gfx.shade = false; c.translate(0, 196); Gfx.rr(c, -16, -210, 32, 30, 12, skinD); });
        for (int h = 0; h < Look.N_HEAD; h++) {
            final int hh = h;
            export("head_" + h, c -> { Gfx.ol(OL); Gfx.shade = false; c.translate(0, 196); Look q = l.copy(); q.head = hh; Avatar.headBase(c, q, W, skinD); });
        }
        for (int hs = 0; hs < Look.N_HAIR; hs++) {
            final int st = hs;
            export("hairb_" + hs, c -> { Gfx.ol(OL); Gfx.shade = false; c.translate(0, 196); Look q = l.copy(); q.hairStyle = st; Avatar.hairBack(c, q, W); });
            export("hairf_" + hs, c -> { Gfx.ol(OL); Gfx.shade = false; c.translate(0, 196); Look q = l.copy(); q.hairStyle = st; Avatar.hairFront(c, q, W); });
        }
        for (int ac = 1; ac < Look.N_ACC; ac++) {
            final int k = ac;
            export("acc_" + ac, c -> { Gfx.ol(OL); Gfx.shade = false; c.translate(0, 196); Look q = l.copy(); q.acc = k; Avatar.accessory(c, q, W); });
        }

        // ---------------- manifest
        StringBuilder js = new StringBuilder("{\n");
        int i = 0;
        for (Map.Entry<String, float[]> e : manifest.entrySet()) {
            float[] v = e.getValue();
            js.append("  \"").append(e.getKey()).append("\": [").append(v[0]).append(", ").append(v[1]).append(", ").append(v[2]).append(", ").append(v[3]).append("]");
            js.append(++i < manifest.size() ? ",\n" : "\n");
        }
        js.append("}\n");
        try (FileWriter fw = new FileWriter(new File(dir, "manifest.json"))) { fw.write(js.toString()); }
        System.out.println("exported " + manifest.size() + " svgs to " + dir);
    }
}
