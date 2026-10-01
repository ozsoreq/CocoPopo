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

    // life simulation
    static final int FREE = 0, SIT = 1, LIE = 2, HELD = 3, ON_TOP = 4, BATHE = 5, SLIDE = 6;
    int state;
    Obj link;           // seat / bed / holder / surface this object belongs to
    int slot;           // seat slot index
    float offX;         // offset on a surface
    Obj held;           // what a character is holding
    boolean falling;
    float vy, fvx;      // fall / toss velocity
    boolean walking;
    float tx, ty, walkPh, walkAmt;
    float idleT = 3 + (float) Math.random() * 6;
    int act;            // 0 none, 1 wave, 2 look, 3 dance
    float actT;
    float chewT;
    int bites;
    int pstate;         // prop state (lamp on, tv channel, ...)
    float wiggle;
    final Pose pose = new Pose();
    float peakY, dyo;
    float ds = 1;      // extra draw scale (held items)
    int tmpLink = -1;
    final java.util.ArrayList<String> contents = new java.util.ArrayList<String>();
    Obj pend;           // walk-and-use target
    int pendKind, pendSlot;
    final float[] wpx = new float[4], wpy = new float[4];
    int nwp;            // remaining waypoints after (tx, ty)
    int faceId;         // temporary expression
    float faceT, hugT, strumT, slideT, launchT, glance, glanceT;
    float shimmerT;

    void setFace(int f, float secs) { faceId = f; faceT = secs; }

    void sizeFromLook() {
        float[] b = Avatar.BODY[look.body % Avatar.BODY.length];
        bw = Avatar.W * Math.max(1, b[1]);
        bh = Avatar.height(look) + 10;
    }

    float sortY() {
        if (state == SIT && link != null && link.isChar) return link.y - 1; // shoulder rider sits behind the carrier's head
        if (state == SIT || state == LIE || state == ON_TOP || state == BATHE || state == SLIDE) return link != null ? link.y + 1 : y;
        if (state == HELD && link != null) return link.y + 1;
        return y;
    }

    static Obj character(Look l, float x, float y) {
        Obj o = new Obj();
        o.isChar = true; o.look = l; o.x = x; o.y = y;
        o.sizeFromLook();
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
        if (state == LIE) {
            float lx = (px - x) / scale * -slot, ly = (py - y) / scale;
            return lx >= -20 && lx <= 380 && ly >= -120 && ly <= 100;
        }
        float sc = scale * ds;
        float lx = (px - x) / sc, ly = (py - y) / sc;
        return lx >= -bw / 2 && lx <= bw / 2 && ly >= -bh && ly <= 0;
    }

    Obj copyAt(float nx, float ny) {
        Obj o = isChar ? character(look.copy(), nx, ny) : prop(prop, nx, ny);
        o.scale = scale; o.flip = flip;
        return o;
    }

    String encode(float W, String extra) {
        String xs = String.format(java.util.Locale.US, "%.4f", x / W);
        String ys = String.format(java.util.Locale.US, "%.1f", y);
        String sc = String.format(java.util.Locale.US, "%.2f", scale);
        return (isChar ? "C:" + look.encode().replace(',', '.') : "P:" + prop) + ":" + xs + ":" + ys + ":" + sc + ":" + (flip ? 1 : 0) + ":" + extra;
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
            if (p.length > 6) {
                String[] e = p[6].split(",");
                o.state = Integer.parseInt(e[0]);
                o.tmpLink = Integer.parseInt(e[1]);
                o.slot = Integer.parseInt(e[2]);
                o.pstate = Integer.parseInt(e[3]);
                o.bites = Integer.parseInt(e[4]);
                if (e.length > 5 && e[5].length() > 0) for (String it : e[5].split("\\+")) if (it.length() > 0) o.contents.add(it);
                if (o.state == SLIDE) o.state = FREE;
            }
            return o;
        } catch (Exception e) {
            return null;
        }
    }
}
