package com.cocopopo.app;

/** Something sitting in a scene: a character or a prop. */
final class Obj {
    boolean isChar;
    Look look;          // characters
    String prop;        // props
    float x, y;         // anchor (bottom centre) in virtual units
    float scale = 1;
    boolean flip;
    float bw, bh;       // local bounds

    // animation state
    float lift, tilt, hop, hopV, sq, sqv, pop;
    float emoteT = -1;
    int emote;
    float blinkT, nextBlink = 2;
    float phase;
    float vx;

    static Obj character(Look l, float x, float y) {
        Obj o = new Obj();
        o.isChar = true; o.look = l; o.x = x; o.y = y;
        o.bw = Avatar.W; o.bh = Avatar.H;
        o.phase = (float) (Math.random() * 6);
        return o;
    }

    static Obj prop(String id, float x, float y) {
        Obj o = new Obj();
        PropArt.Def d = PropArt.get(id);
        o.prop = d.id; o.x = x; o.y = y; o.bw = d.w; o.bh = d.h;
        o.phase = (float) (Math.random() * 6);
        return o;
    }

    boolean hit(float px, float py) {
        float lx = (px - x) / scale, ly = (py - y) / scale;
        return lx >= -bw / 2 && lx <= bw / 2 && ly >= -bh && ly <= 0;
    }

    Obj copyAt(float nx, float ny) {
        Obj o = isChar ? character(look.copy(), nx, ny) : prop(prop, nx, ny);
        o.scale = scale; o.flip = flip;
        return o;
    }

    String encode(float W) {
        String xs = String.format(java.util.Locale.US, "%.4f", x / W);
        String ys = String.format(java.util.Locale.US, "%.1f", y);
        String sc = String.format(java.util.Locale.US, "%.2f", scale);
        return (isChar ? "C:" + look.encode().replace(',', '.') : "P:" + prop) + ":" + xs + ":" + ys + ":" + sc + ":" + (flip ? 1 : 0);
    }

    static Obj decode(String s, float W) {
        try {
            String[] p = s.split(":");
            float x = Float.parseFloat(p[2]) * W, y = Float.parseFloat(p[3]);
            Obj o;
            if (p[0].equals("C")) o = character(Look.decode(p[1].replace('.', ',')), x, y);
            else o = prop(p[1], x, y);
            o.scale = Float.parseFloat(p[4]);
            o.flip = p[5].equals("1");
            return o;
        } catch (Exception e) {
            return null;
        }
    }
}
