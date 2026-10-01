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

        // ---------------- character parts, white based (tinted in Godot)
        Look l = Look.PRESETS[0].copy();
        l.accColor = 9; // white
        int skinD = Gfx.dk(W, .13f);
        // torso space: origin = hips
        for (int top = 0; top < Look.N_TOP; top++) {
            final int tp = top;
            export("torso_" + top, c -> { Gfx.ol(6.5f); Gfx.shade = true; c.translate(0, 98); Look q = l.copy(); q.top = tp == 1 || tp == 4 ? 0 : tp; Avatar.torso(c, q, W, W, W); });
        }
        export("torso_1s", c -> { Gfx.ol(0); c.translate(0, 98); for (int i = 0; i < 4; i++) Gfx.rr(c, -52, -170 + i * 24, 104, 11, 5, W); });
        export("torso_4b", c -> { Gfx.ol(6.5f); Gfx.shade = true; c.translate(0, 98);
            Gfx.rr(c, -44, -156, 88, 76, 22, W); Gfx.rr(c, -44, -196, 16, 56, 7, W); Gfx.rr(c, 28, -196, 16, 56, 7, W);
            Gfx.rr(c, -16, -130, 32, 26, 8, Gfx.dk(W, .14f)); });
        export("buttons_4", c -> { Gfx.ol(0); c.translate(0, 98); Gfx.ci(c, -36, -150, 6, 0xFFFFD43B); Gfx.ci(c, 36, -150, 6, 0xFFFFD43B); });
        export("skirt", c -> { Gfx.ol(6.5f); Gfx.shade = true; c.translate(0, 98); Avatar.skirt(c, W); });
        // arms: origin = shoulder
        export("sleeve", c -> { Gfx.ol(6.5f); Gfx.shade = true; Gfx.rr(c, -15, -8, 30, 84, 15, W); Gfx.rr(c, -15, 58, 30, 8, 4, Gfx.dk(W, .08f)); });
        export("arm_bare", c -> { Gfx.ol(6.5f); Gfx.shade = true; Gfx.rr(c, -15, -8, 30, 84, 15, W); });
        export("mitten_r", c -> { Gfx.ol(6.5f); Gfx.shade = true; Avatar.mitten(c, 1, W); });
        export("mitten_l", c -> { Gfx.ol(6.5f); Gfx.shade = true; Avatar.mitten(c, -1, W); });
        // legs: origin = hip joint, kid length 74
        export("leg_pants", c -> { Gfx.ol(6.5f); Gfx.shade = true; Gfx.rr(c, -19, 0, 38, 74, 18, W); Gfx.rr(c, -19, 60, 38, 14, 6, Gfx.dk(W, .1f)); });
        export("leg_bare", c -> { Gfx.ol(6.5f); Gfx.shade = true; Gfx.rr(c, -19, 0, 38, 74, 18, W); });
        export("shorts", c -> { Gfx.ol(6.5f); Gfx.shade = true; Gfx.rr(c, -20, 0, 40, 34, 12, W); });
        // shoe: origin = shoe centre
        export("shoe", c -> { Gfx.ol(6.5f); Gfx.shade = true; Gfx.ov(c, 0, 5, 31, 17, Gfx.dk(W, .25f)); Gfx.ov(c, 0, 0, 30, 16, W); });
        // head space: origin = neck (head code is written with the neck at y = -196)
        export("neck", c -> { Gfx.ol(6.5f); c.translate(0, 196); Gfx.rr(c, -19, -214, 38, 34, 12, skinD); });
        for (int h = 0; h < Look.N_HEAD; h++) {
            final int hh = h;
            export("head_" + h, c -> { Gfx.ol(6.5f); c.translate(0, 196); Look q = l.copy(); q.head = hh; Avatar.headBase(c, q, W, skinD); });
        }
        for (int hs = 0; hs < Look.N_HAIR; hs++) {
            final int st = hs;
            export("hairb_" + hs, c -> { Gfx.ol(6.5f); Gfx.shade = true; c.translate(0, 196); Look q = l.copy(); q.hairStyle = st; Avatar.hairBack(c, q, W); });
            export("hairf_" + hs, c -> { Gfx.ol(6.5f); Gfx.shade = true; c.translate(0, 196); Look q = l.copy(); q.hairStyle = st; Avatar.hairFront(c, q, W); });
        }
        for (int ac = 1; ac < Look.N_ACC; ac++) {
            final int k = ac;
            export("acc_" + ac, c -> { Gfx.ol(6.5f); Gfx.shade = true; c.translate(0, 196); Look q = l.copy(); q.acc = k; Avatar.accessory(c, q, W); });
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
