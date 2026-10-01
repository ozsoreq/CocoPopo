package com.cocopopo.app;

import android.graphics.Canvas;
import android.graphics.Paint;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Random;

import static com.cocopopo.app.Gfx.*;

/** All game logic and rendering. Platform-independent: the Android view just feeds it time, touches and a Canvas. */
public final class Game {
    interface Host {
        String load(String key);
        void save(String key, String value);
        void photo();
        void haptic();
        void quit();
        void sound(int id);
    }

    static final int S_POP = 0, S_DROP = 1, S_BITE = 2, S_TOGGLE = 3, S_TICK = 4, S_BOUNCE = 5, S_SPARK = 6, S_WHOOSH = 7;

    static final float H = 1080;
    static boolean DEBUG = false;
    static final int MAP = 0, SCENE = 1, EDITOR = 2;

    // button ids
    private static final int B_HOME = 1, B_CAM = 2, B_CLEAR = 3, B_TAB_CHAR = 4, B_TAB_ITEM = 5, B_NEWCHAR = 6,
        B_CHIP = 10, P_EDIT = 20, P_EMOTE = 21, P_FLIP = 22, P_BIG = 23, P_SMALL = 24, P_COPY = 25, P_DEL = 26,
        E_BACK = 30, E_DONE = 31, E_DICE = 32, E_SAVE = 33, E_TAB = 40, B_LOC = 100, E_OPT = 1000;

    final Host host;
    final Random rnd = new Random();
    float W = 1920, scale = 1, t;
    int screen = MAP;
    private Canvas c;

    // ---- buttons registered while drawing (immediate-mode UI)
    private final int[] bid = new int[300];
    private final float[] bx = new float[300], by = new float[300], bw = new float[300], bh = new float[300];
    private final boolean[] bround = new boolean[300];
    private int nb;

    // ---- input
    private boolean down, moved, scrolling;
    private float sx, sy, lx, ly;
    private int pressedId = -1;
    private static final int M_NONE = 0, M_DRAG = 1, M_TRAY = 2, M_PANEL = 3, M_PREVIEW = 4, M_MAP = 5;
    private int mode;
    private Obj dragObj;
    private float dragDX, dragDY, dragVX, dragVY;
    private long lastMoveNs;

    // ---- transition / feedback
    private float fade;
    private int fadeDir;
    private int pScreen, pEditMode;
    private String pLoc;
    private Obj pTarget;
    private String toast = "";
    private float toastT;
    private float flash;
    private float clearConfirm;

    // ---- scene
    String loc = "home";
    final List<Obj> objs = new ArrayList<Obj>();
    private final List<Obj> order = new ArrayList<Obj>();
    private Obj sel;
    private int trayTab;           // 0 closed, 1 characters, 2 items
    private float trayA, trayScroll, trayVel;
    private int trayCat;
    private int trayCard = -1;     // card under finger when tray touch began
    private boolean trayDecided;
    final List<Look> lib = new ArrayList<Look>();

    // ---- editor
    private Look work = Look.PRESETS[0].copy();
    private Obj editTarget;
    private int editMode;          // 0 new from map, 1 new from tray, 2 edit scene object
    private int edTab;
    private float edScroll, edMax;
    private float edHop, edHopV, edBlink, edBlinkNext = 2, edEmoteT = -1;
    private int edEmote, edMood = -1;
    private static final Pose IDLE = new Pose();

    private final Comparator<Obj> byDepth = new Comparator<Obj>() {
        public int compare(Obj a, Obj b) {
            float ka = (a.isChar ? 0 : (PropArt.flat(a.prop) ? -100000 : 0)) + a.sortY();
            float kb = (b.isChar ? 0 : (PropArt.flat(b.prop) ? -100000 : 0)) + b.sortY();
            return Float.compare(ka, kb);
        }
    };

    Game(Host host) {
        this.host = host;
        String s = host.load("lib");
        if (s != null && s.length() > 0) for (String p : s.split(";")) lib.add(Look.decode(p));
    }

    void layout(int pxW, int pxH) {
        scale = pxH / H;
        W = pxW / scale;
    }

    // ================================================================== lifecycle
    void pause() {
        if (screen == SCENE) saveScene();
    }

    boolean back() {
        if (fadeDir != 0) return true;
        if (screen == EDITOR) { leaveEditor(false); return true; }
        if (screen == SCENE) {
            if (trayTab != 0) { trayTab = 0; return true; }
            go(MAP, null, 0, null);
            return true;
        }
        return false;
    }

    private void go(int scr, String l, int em, Obj target) {
        if (fadeDir != 0) return;
        pScreen = scr; pLoc = l; pEditMode = em; pTarget = target;
        fadeDir = 1;
    }

    private void applyTransition() {
        if (screen == SCENE && pScreen != SCENE) saveScene();
        screen = pScreen;
        down = false; mode = M_NONE; pressedId = -1; dragObj = null;
        if (screen == SCENE) {
            if (pLoc != null) enterScene(pLoc);
            trayTab = 0; trayA = 0; sel = null;
        } else if (screen == EDITOR) {
            editMode = pEditMode; editTarget = pTarget;
            work = pTarget != null ? pTarget.look.copy() : Look.random(rnd);
            if (pTarget == null) { work.acc = 0; }
            edTab = 0; edScroll = 0; edEmoteT = -1; edMood = -1;
        }
        afterEditorReturn();
    }

    void enterScene(String id) {
        loc = id;
        objs.clear();
        String s = host.load("scene_" + id);
        if (s == null) {
            for (String d : Scenes.defaults(id)) {
                String[] p = d.split(":");
                float x = Float.parseFloat(p[1]) * W, y = Float.parseFloat(p[2]);
                Obj o = p[0].startsWith("@") ? Obj.character(Look.PRESETS[Integer.parseInt(p[0].substring(1))].copy(), x, y) : Obj.prop(p[0], x, y);
                o.scale = Float.parseFloat(p[3]);
                o.pop = 1;
                objs.add(o);
            }
        } else if (s.length() > 1) {
            for (String r : s.split("\\|")) {
                Obj o = Obj.decode(r, W);
                if (o != null) { o.pop = 1; objs.add(o); }
            }
            for (Obj o : objs) {
                if (o.tmpLink >= 0 && o.tmpLink < objs.size() && o.state != Obj.FREE) {
                    o.link = objs.get(o.tmpLink);
                    if (o.state == Obj.HELD) o.link.held = o;
                } else if (o.state != Obj.FREE) o.state = Obj.FREE;
            }
        }
        sel = null;
        parts = 0;
    }

    private void saveScene() {
        StringBuilder sb = new StringBuilder();
        for (Obj o : objs) {
            if (sb.length() > 0) sb.append('|');
            int li = o.link != null ? objs.indexOf(o.link) : -1;
            StringBuilder ct = new StringBuilder();
            for (String it : o.contents) { if (ct.length() > 0) ct.append('+'); ct.append(it); }
            sb.append(o.encode(W, (li < 0 ? 0 : o.state) + "," + li + "," + o.slot + "," + o.pstate + "," + o.bites + "," + ct));
        }
        host.save("scene_" + loc, sb.length() == 0 ? "-" : sb.toString());
    }

    private void saveLib() {
        StringBuilder sb = new StringBuilder();
        for (Look l : lib) { if (sb.length() > 0) sb.append(';'); sb.append(l.encode()); }
        host.save("lib", sb.toString());
    }

    private void say(String s) { toast = s; toastT = 2.2f; }

    // ================================================================== update
    void update(float dt) {
        if (dt > .05f) dt = .05f;
        t += dt;
        if (toastT > 0) toastT -= dt;
        if (flash > 0) flash -= dt * 2.5f;
        if (clearConfirm > 0) clearConfirm -= dt;
        if (fadeDir == 1) { fade += dt * 5; if (fade >= 1) { fade = 1; applyTransition(); fadeDir = -1; } }
        else if (fadeDir == -1) { fade -= dt * 4; if (fade <= 0) { fade = 0; fadeDir = 0; } }

        if (screen == SCENE) {
            float target = trayTab != 0 ? 1 : 0;
            trayA += (target - trayA) * Math.min(1, dt * 12);
            if (Math.abs(target - trayA) < .002f) trayA = target;
            if (!(down && mode == M_TRAY)) {
                trayScroll += trayVel * dt;
                trayVel *= (float) Math.pow(.02, dt);
                clampTray();
            }
            for (int i = 0; i < objs.size(); i++) { Obj o = objs.get(i); updateObj(o, dt, o == dragObj && moved); }
            updateParts(dt);
            sortOrder();
            shimmer -= dt;
            if (shimmer <= 0) {
                shimmer = 6 + rnd.nextFloat() * 4;
                java.util.ArrayList<Obj> cands = new java.util.ArrayList<Obj>();
                for (Obj q : objs) if (!q.isChar && q.state == Obj.FREE && (Life.states(q.prop) > 0 || Life.capacity(q.prop) > 0 || Life.seat(q.prop) != null
                        || Life.bed(q.prop) != 0 || Life.tub(q.prop) || Life.slide(q.prop) || q.prop.equals("tree") || q.prop.equals("bush") || q.prop.equals("palm"))) cands.add(q);
                if (!cands.isEmpty()) {
                    Obj q = cands.get(rnd.nextInt(cands.size()));
                    burst(q.x, q.y - q.bh * q.scale * .55f, 7, 0xFFFFF3A0, 2, 220);
                }
            }
        }
        if (screen == MAP) updateMap(dt);
        if (screen == EDITOR) {
            edHopV -= 2600 * dt; edHop += edHopV * dt;
            if (edHop < 0) { edHop = 0; edHopV = 0; }
            edBlinkNext -= dt;
            if (edBlinkNext <= 0) { edBlink = .14f; edBlinkNext = 2 + rnd.nextFloat() * 3; }
            if (edBlink > 0) edBlink -= dt;
            if (edEmoteT >= 0) { edEmoteT += dt; if (edEmoteT > 2) { edEmoteT = -1; edMood = -1; } }
        }
    }

    private void updateObj(Obj o, float dt, boolean dragging) {
        o.lift += ((dragging ? 1 : 0) - o.lift) * Math.min(1, dt * 14);
        float tt = dragging ? Gfx.clamp(o.vx * .04f, -14, 14) : 0;
        o.tilt += (tt - o.tilt) * Math.min(1, dt * 12);
        o.vx *= .8f;
        o.hopV -= 2600 * dt; o.hop += o.hopV * dt;
        if (o.hop < 0) {
            if (o.hopV < -500) o.sqv = 2.2f;
            o.hop = 0; o.hopV = 0;
        }
        o.sqv += (-o.sq * 420 - o.sqv * 18) * dt; o.sq += o.sqv * dt;
        if (o.pop < 1) o.pop = Math.min(1, o.pop + dt * 3);
        if (o.emoteT >= 0) { o.emoteT += dt; if (o.emoteT > 2) o.emoteT = -1; }
        o.nextBlink -= dt;
        if (o.nextBlink <= 0) { o.blinkT = .14f; o.nextBlink = 2 + rnd.nextFloat() * 3; }
        if (o.blinkT > 0) o.blinkT -= dt;
        o.wiggle = Math.max(0, o.wiggle - dt * 2.5f);
        if (o.chewT > 0) o.chewT -= dt;
        if (o.faceT > 0) o.faceT -= dt;
        if (o.hugT > 0) o.hugT -= dt;
        if (o.strumT > 0) o.strumT -= dt;
        if (o.launchT > 0) { o.launchT -= dt; if (o.launchT <= 0) { o.sqv = -3; host.sound(S_DROP); } }

        if (dragging) {
            o.walking = false; o.falling = false; o.pend = null; o.nwp = 0;
            if (o.isChar) updateChar(o, dt, true);
            return;
        }

        // things attached to other things follow them
        if (o.state != Obj.FREE && (o.link == null || !objs.contains(o.link) || (o.link.isChar && o.link.state == Obj.HELD))) {
            if (o.state == Obj.HELD && o.link != null) o.link.held = null;
            o.state = Obj.FREE; o.link = null;
            startFall(o, 0, 0);
        }
        Obj l = o.link;
        switch (o.state) {
            case Obj.SIT: {
                if (l.isChar) { // riding on shoulders
                    o.x = l.x;
                    o.y = l.y + l.dyo + (Avatar.neckY(l.look) - 135 * Avatar.BODY[l.look.body % 6][3]) * l.scale - Avatar.hipY(o.look) * o.scale;
                    break;
                }
                float[] st = Life.seat(l.prop);
                if (st == null || o.slot + 1 >= st.length) { o.state = Obj.FREE; break; }
                float dir = l.flip ? -1 : 1;
                o.x = l.x + st[1 + o.slot] * l.scale * dir;
                if (l.prop.equals("swing")) o.x += (float) Math.sin((t + l.phase) * 2) * 10 * l.scale;
                o.y = l.y + st[0] * l.scale - Avatar.hipY(o.look) * o.scale - 4 - l.hop - l.lift * 36 * l.scale;
                break;
            }
            case Obj.LIE:
                o.x = l.x + 158 * l.scale * o.slot;
                o.y = l.y + (Life.bed(l.prop) + 34) * l.scale - l.hop;
                break;
            case Obj.BATHE:
                o.x = l.x - 30 * l.scale * (l.flip ? -1 : 1);
                o.y = l.y - 74 * l.scale - Avatar.hipY(o.look) * o.scale - l.hop;
                if (rnd.nextFloat() < dt * 3) burst(o.x + (rnd.nextFloat() - .5f) * 200 * l.scale, l.y - 130 * l.scale, 1, 0xFFFFFFFF, 3, 120);
                break;
            case Obj.SLIDE: {
                o.slideT += dt / 1.1f;
                float u = Math.min(1, o.slideT), dir = l.flip ? -1 : 1;
                float px0 = -70, py0 = -290, cx0 = 60, cy0 = -190, px1 = 150, py1 = -20;
                float bx = (1 - u) * (1 - u) * px0 + 2 * (1 - u) * u * cx0 + u * u * px1;
                float by = (1 - u) * (1 - u) * py0 + 2 * (1 - u) * u * cy0 + u * u * py1;
                o.x = l.x + bx * l.scale * dir;
                o.y = l.y + by * l.scale - Avatar.hipY(o.look) * o.scale * .6f;
                o.flip = l.flip;
                if (u >= 1) {
                    o.state = Obj.FREE; o.link = null;
                    o.x = l.x + 200 * l.scale * dir; o.y = l.y;
                    o.sqv = -3; o.setFace(Avatar.F_LAUGH, 1.5f); o.emote = 6; o.emoteT = 0;
                    host.sound(S_DROP);
                }
                break;
            }
            case Obj.ON_TOP:
                o.x = l.x + o.offX * l.scale;
                o.y = l.y + Life.surface(l.prop) * l.scale - l.lift * 36 * l.scale - l.hop;
                break;
            case Obj.HELD: {
                Pose hp = l.pose;
                float dir = l.flip ? -1 : 1;
                float hs = holdScale(o);
                o.x = l.x + Avatar.handX(hp, l.look) * l.scale * dir;
                o.y = l.y + l.dyo + Avatar.handY(hp, l.look) * l.scale + o.bh * hs * (hp.holdType == 2 ? .1f : .45f);
                break;
            }
            default: break;
        }

        // vehicles drive to where they were sent
        if (!o.isChar && o.walking) {
            float dx = o.tx - o.x, dy = o.ty - o.y;
            float d = (float) Math.sqrt(dx * dx + dy * dy);
            if (d < 6) o.walking = false;
            else {
                float step = Math.min(d, 420 * dt);
                o.x += dx / d * step; o.y += dy / d * step;
                if (Math.abs(dx) > 4) o.flip = o.prop.equals("car") ? dx < 0 : dx > 0;
                o.hop = Math.abs((float) Math.sin(t * 18)) * 3;
            }
        }

        if (o.falling) {
            o.vy += 3200 * dt;
            o.x += o.fvx * dt;
            o.y += o.vy * dt;
            o.peakY = Math.min(o.peakY, o.y);
            if (o.x < 40) { o.x = 40; o.fvx = Math.abs(o.fvx) * .6f; }
            if (o.x > W - 40) { o.x = W - 40; o.fvx = -Math.abs(o.fvx) * .6f; }
            float rest = restFor(o);
            if (o.y >= rest && o.vy > 0) {
                o.y = rest;
                if (Life.tossable(o) && o.vy > 800) {
                    o.vy = -o.vy * (o.prop.equals("ball") ? .62f : .38f);
                    o.fvx *= .7f;
                    o.peakY = o.y;
                    host.sound(S_BOUNCE);
                } else {
                    if (o.isChar && o.vy > 1900) { o.setFace(Avatar.F_WOW, 1.2f); o.emote = 1; o.emoteT = 0; }
                    o.falling = false; o.vy = 0; o.fvx = 0;
                    o.sqv = -2.6f;
                    if (restLink != null) { o.state = Obj.ON_TOP; o.link = restLink; o.offX = (o.x - restLink.x) / restLink.scale; }
                    host.sound(S_DROP);
                }
            }
        }
        if (o.isChar) updateChar(o, dt, false);
    }

    private Obj restLink;
    private float shimmer = 3;

    /** Resting height for a falling object: a table top (small items) or the floor. */
    private float restFor(Obj o) {
        restLink = null;
        float rest = Life.floorBelow(loc, o.peakY);
        if (o.isChar || !Life.tossable(o)) return rest;
        for (Obj s : objs) {
            if (s == o || s.isChar) continue;
            float top = Life.surface(s.prop);
            if (top == 0) continue;
            float topY = s.y + top * s.scale;
            if (Math.abs(o.x - s.x) < s.bw * s.scale * .42f && topY >= o.peakY - 8 && topY < rest) { rest = topY; restLink = s; }
        }
        return rest;
    }

    void startFall(Obj o, float vx, float vy) {
        if (o.isChar || !(Life.wall(o.prop) || Life.floats(o.prop))) {
            o.falling = true;
            o.fvx = vx; o.vy = vy;
            o.peakY = o.y;
        }
    }

    float holdScale(Obj item) { return Math.min(1, 120 / Math.max(item.bw, item.bh)) * item.scale * (1 - item.bites * .22f); }

    private void updateChar(Obj o, float dt, boolean dragging) {
        Pose p = o.pose;
        p.t = t + o.phase;
        float em = o.emoteT >= 0 && o.emoteT < .8f ? 1 : 0;
        p.arm = Math.max(Math.max(o.lift, em), o.falling || o.state == Obj.SLIDE ? 1 : 0);
        if (o.walking && o.nwp >= 0 && Math.abs(o.ty - o.y) > Math.abs(o.tx - o.x) * 2) p.arm = Math.max(p.arm, .7f); // climbing
        p.swing = Math.max(o.lift, o.falling ? .6f : 0) * (float) Math.sin(t * 16 + o.phase) * .8f;
        p.blink = o.blinkT > 0 ? (float) Math.sin(Math.PI * (1 - o.blinkT / .14f)) : 0;
        p.mood = -1;
        p.sit = o.state == Obj.SIT || o.state == Obj.BATHE || o.state == Obj.SLIDE;
        p.sleep = o.state == Obj.LIE;
        p.noLegs = o.state == Obj.BATHE;
        p.hold = o.held != null;
        p.holdType = o.held != null ? Life.holdType(o.held.prop) : 0;
        p.chew = o.chewT > 0;
        p.strum += ((o.strumT > 0 ? 1 : 0) - p.strum) * Math.min(1, dt * 10);
        p.hug += ((o.hugT > 0 ? 1 : 0) - p.hug) * Math.min(1, dt * 8);
        if (o.actT > 0) { o.actT -= dt; if (o.actT <= 0) o.act = 0; }
        float k = Math.min(1, dt * 8);
        p.wave += ((o.act == 1 ? 1 : 0) - p.wave) * k;
        p.look += ((o.act == 2 ? (float) Math.sin(t * 2.4f + o.phase) : 0) - p.look) * k;
        p.dance += ((o.act == 3 ? 1 : 0) - p.dance) * k;

        // expression
        int face = Avatar.F_NONE;
        if (o.faceT > 0) face = o.faceId;
        else if (dragging) face = (t + o.phase) % 1.4f < .7f ? Avatar.F_LAUGH : Avatar.F_WOW;
        else if (o.falling) face = Avatar.F_SCARED;
        else if (o.hugT > 0) face = Avatar.F_LOVE;
        else if (o.chewT > 0) face = Avatar.F_YUM;
        else if (o.state == Obj.BATHE) face = Avatar.F_HAPPY;
        else if (o.emoteT >= 0) face = new int[]{Avatar.F_LOVE, Avatar.F_WOW, Avatar.F_HAPPY, Avatar.F_WOW, Avatar.F_NONE, Avatar.F_SLEEPY, Avatar.F_LAUGH}[o.emote % 7];
        else if (o.strumT > 0) face = Avatar.F_LAUGH;

        // gaze: what is this character looking at?
        float gx = 0, gy = 0;
        Obj tv = watching(o);
        if (o.held != null) { gx = (o.flip ? -1 : 1) * .7f; gy = .6f; }
        else if (tv != null) {
            gx = tv.x < o.x ? -1 : 1; gy = -.2f;
            if (tv.pstate == 2 && face == Avatar.F_NONE && ((int) (t + o.phase) % 5) == 0) face = Avatar.F_LAUGH;
            if (o.state == Obj.SIT && face == Avatar.F_NONE) face = Avatar.F_HAPPY;
        } else if (o.walking) gx = o.tx < o.x ? -.8f : .8f;
        else {
            o.glanceT -= dt;
            if (o.glanceT <= 0) { o.glance = (rnd.nextFloat() - .5f) * 1.6f; o.glanceT = 1 + rnd.nextFloat() * 3; }
            gx = o.glance;
        }
        p.gazeX += (gx - p.gazeX) * Math.min(1, dt * 6);
        p.gazeY += (gy - p.gazeY) * Math.min(1, dt * 6);
        if (tv != null && o.act == 0) p.look += ((tv.x < o.x ? -.45f : .45f) - p.look) * k;
        p.face = face;

        if (o.walking && o.state == Obj.FREE && !dragging) {
            float dx = o.tx - o.x, dy = o.ty - o.y;
            float d = (float) Math.sqrt(dx * dx + dy * dy);
            if (d < 6) {
                o.x = o.tx; o.y = o.ty;
                if (o.nwp > 0) {
                    o.tx = o.wpx[0]; o.ty = o.wpy[0];
                    for (int i = 1; i < o.nwp; i++) { o.wpx[i - 1] = o.wpx[i]; o.wpy[i - 1] = o.wpy[i]; }
                    o.nwp--;
                } else {
                    o.walking = false;
                    if (o.pend != null) { Obj tg = o.pend; o.pend = null; arrive(o, tg); }
                }
            } else {
                float step = Math.min(d, 270 * o.scale * dt);
                o.x += dx / d * step; o.y += dy / d * step;
                if (Math.abs(dx) > 4) o.flip = dx < 0;
                o.walkPh += dt * 11;
            }
        } else o.walking = false;
        o.walkAmt += ((o.walking ? 1 : 0) - o.walkAmt) * Math.min(1, dt * 10);
        p.walk = o.walkAmt;
        p.walkPh = o.walkPh;
        o.dyo = -o.lift * 36 * o.scale - o.hop;

        if (!dragging && !o.falling && o != dragObj) {
            o.idleT -= dt;
            if (o.idleT <= 0) { o.idleT = 4 + rnd.nextFloat() * 7; if (!o.walking) pickIdle(o); }
            if (o.state == Obj.LIE && o.emoteT < 0 && rnd.nextFloat() < dt * .3f) { o.emote = 5; o.emoteT = 0; }
        }
    }

    /** A TV that is on and close enough for this character to watch, or null. */
    private Obj watching(Obj o) {
        if (o.state != Obj.FREE && o.state != Obj.SIT) return null;
        Obj best = null; float bd = 900;
        for (Obj q : objs) {
            if (q.isChar || !q.prop.equals("tv") || q.pstate == 1) continue;
            float d = Math.abs(q.x - o.x) + Math.abs(q.y - o.y) * 2;
            if (d < bd && Math.abs(q.x - o.x) > 60) { bd = d; best = q; }
        }
        return best;
    }

    /** Characters do little things on their own so the world feels alive. */
    private void pickIdle(Obj o) {
        int r = rnd.nextInt(20);
        switch (o.state) {
            case Obj.LIE:
                if (r < 2) standUp(o, Avatar.F_HAPPY);
                return;
            case Obj.BATHE:
                if (r < 6) { standUp(o, Avatar.F_HAPPY); burst(o.x, o.y - 200 * o.scale, 10, 0xFFFFE066, 2, 400); host.sound(S_SPARK); }
                else { o.emote = 6; o.emoteT = 0; }
                return;
            case Obj.SIT:
                if (o.link != null && o.link.isChar) { if (r < 6) { o.emote = 6; o.emoteT = 0; } return; }
                if (r < 3) { standUp(o, Avatar.F_NONE); return; }
                if (o.held != null && Life.food(o.held.prop) && r < 9) { eat(o); return; }
                if (r < 10) { o.act = 2; o.actT = 2.5f; }
                else if (r < 15) { o.act = 1; o.actT = 1.8f; }
                else { o.emote = rnd.nextInt(7); o.emoteT = 0; }
                return;
            case Obj.FREE: break;
            default: return;
        }
        if (o.held != null && Life.food(o.held.prop) && r < 8) { eat(o); return; }
        if (o.held != null && !Life.food(o.held.prop) && r < 3) { putDown(o); return; }
        if (r < 7 && autoUse(o)) return;
        if (r < 12) {
            float[] f = Life.floors(loc);
            int b = Life.band(loc, o.y);
            float dist = (120 + rnd.nextFloat() * 260) * o.scale * (rnd.nextBoolean() ? 1 : -1);
            o.tx = Gfx.clamp(o.x + dist, 70, W - 70);
            o.ty = Gfx.clamp(o.y + (rnd.nextFloat() - .5f) * 80, f[b] + 10, f[b + 1] - 6);
            o.walking = true; o.nwp = 0;
        } else if (r < 14) { o.act = 1; o.actT = 1.8f; }
        else if (r < 16) { o.act = 2; o.actT = 2.6f; }
        else if (r < 18) { o.act = 3; o.actT = 2.6f; }
        else { o.hopV = 560; o.emote = rnd.nextInt(7); o.emoteT = 0; }
    }

    /** Pick something nearby to use on its own: a free seat, a toy or snack, the tub. */
    private boolean autoUse(Obj o) {
        int band = Life.band(loc, o.y);
        Obj best = null; float bestScore = 0;
        for (Obj q : objs) {
            if (q == o || q.isChar || q.state == Obj.HELD) continue;
            float d = Math.abs(q.x - o.x);
            if (d > 750 || Life.band(loc, q.y) != band) continue;
            float score = 0;
            if (Life.seat(q.prop) != null && freeSlot(q, o) >= 0) score = 2;
            else if (o.held == null && Life.holdable(q) && !Life.floats(q.prop)) score = Life.food(q.prop) ? 3 : 1.5f;
            else if (Life.tub(q.prop) && occupant(q) == null) score = 1;
            else if (Life.slide(q.prop)) score = 1.2f;
            if (score <= 0) continue;
            score *= .5f + rnd.nextFloat();
            if (score > bestScore) { bestScore = score; best = q; }
        }
        if (best == null) return false;
        return walkUse(o, best);
    }

    private void standUp(Obj o, int face) {
        Obj l = o.link;
        o.state = Obj.FREE; o.link = null;
        if (l != null) {
            o.x = Gfx.clamp(l.x + (rnd.nextBoolean() ? 1 : -1) * l.bw * l.scale * .4f, 60, W - 60);
            o.y = Life.floorBelow(loc, l.y - 4);
            if (l.isChar) o.y = l.y + 10;
        }
        o.hopV = 420;
        if (face != Avatar.F_NONE) o.setFace(face, 1.2f);
    }

    private void putDown(Obj o) {
        Obj it = o.held;
        if (it == null) return;
        o.held = null; it.state = Obj.FREE; it.link = null;
        it.x = o.x + (o.flip ? -1 : 1) * 90 * o.scale;
        startFall(it, 0, 0);
    }

    // ------------------------------------------------------------------ particles
    private final float[] px = new float[240], py = new float[240], pvx = new float[240], pvy = new float[240], plife = new float[240], psz = new float[240];
    private final int[] pcol = new int[240], ptype = new int[240];
    private int parts;

    private void burst(float x, float y, int n, int col, int type, float speed) {
        for (int i = 0; i < n && parts < px.length; i++, parts++) {
            double a = rnd.nextFloat() * Math.PI * 2;
            float sp = speed * (.4f + rnd.nextFloat() * .6f);
            px[parts] = x; py[parts] = y;
            pvx[parts] = (float) Math.cos(a) * sp; pvy[parts] = (float) Math.sin(a) * sp - speed * .6f;
            plife[parts] = type == 1 ? .7f : (type == 3 ? 1.6f + rnd.nextFloat() : .8f + rnd.nextFloat() * .5f);
            psz[parts] = type == 1 ? 30 : (type == 3 ? 8 + rnd.nextFloat() * 14 : 6 + rnd.nextFloat() * 8);
            pcol[parts] = col; ptype[parts] = type;
        }
    }

    private void updateParts(float dt) {
        for (int i = 0; i < parts; i++) {
            plife[i] -= dt;
            if (plife[i] <= 0) {
                parts--;
                px[i] = px[parts]; py[i] = py[parts]; pvx[i] = pvx[parts]; pvy[i] = pvy[parts];
                plife[i] = plife[parts]; psz[i] = psz[parts]; pcol[i] = pcol[parts]; ptype[i] = ptype[parts];
                i--;
                continue;
            }
            if (ptype[i] == 0) pvy[i] += 1800 * dt;
            if (ptype[i] == 2) { pvy[i] -= 200 * dt; pvx[i] *= .96f; }
            if (ptype[i] == 3) { pvy[i] = pvy[i] * .9f - 80 * dt; pvx[i] = pvx[i] * .9f + (float) Math.sin(t * 6 + i) * 4; }
            px[i] += pvx[i] * dt; py[i] += pvy[i] * dt;
        }
    }

    private void drawParts() {
        for (int i = 0; i < parts; i++) {
            int a = (int) (255 * Math.min(1, plife[i] * 3));
            if (ptype[i] == 1) {
                float r = psz[i] + (.7f - plife[i]) * 90;
                ovs(c, px[i], py[i], r, r * .32f, al(0xFFFFFFFF, a), 6);
            } else if (ptype[i] == 2) {
                c.save(); c.translate(px[i], py[i]); c.rotate(t * 200 + i * 40);
                poly(c, al(pcol[i], a), 0, -psz[i] * 1.6f, psz[i] * .5f, 0, 0, psz[i] * 1.6f, -psz[i] * .5f, 0);
                c.restore();
            } else if (ptype[i] == 3) {
                ci(c, px[i], py[i], psz[i], al(0xFFFFFFFF, a / 3));
                cis(c, px[i], py[i], psz[i], al(0xFFFFFFFF, a), 3);
                ci(c, px[i] - psz[i] * .35f, py[i] - psz[i] * .35f, psz[i] * .22f, al(0xFFFFFFFF, a));
            } else ci(c, px[i], py[i], psz[i] * .5f, al(pcol[i], a));
        }
    }

    // ================================================================== draw
    void draw(Canvas canvas) {
        c = canvas;
        nb = 0;
        Gfx.ol(0);
        Gfx.shade = false;
        c.save();
        c.scale(scale, scale);
        switch (screen) {
            case MAP: drawMap(); break;
            case SCENE: drawScene(true); break;
            default: drawEditor(); break;
        }
        if (toastT > 0) {
            float a = Gfx.clamp(toastT * 3, 0, 1);
            float w = tw(toast, 38) + 80;
            c.save(); c.translate(W / 2, 80 + (1 - a) * -40);
            rr(c, -w / 2, -34, w, 68, 34, al(0xFF2B2735, (int) (230 * a)));
            text(c, toast, 0, 13, 38, al(0xFFFFFFFF, (int) (255 * a)), Paint.Align.CENTER);
            c.restore();
        }
        if (flash > 0) rect(c, 0, 0, W, H, al(0xFFFFFFFF, (int) (255 * Math.min(1, flash))));
        if (fade > 0) {
            rect(c, 0, 0, W, H, al(0xFFFFF1C9, (int) (255 * fade)));
            float r = 160 * (1 - fade);
            if (r > 4) { ci(c, W / 2, H / 2, r, al(0xFFFF8FA3, (int) (255 * (1 - fade)))); }
        }
        c.restore();
    }

    /** Scene without any UI, used for the in-game camera. */
    void drawPhoto(Canvas canvas) {
        c = canvas;
        nb = 0;
        c.save();
        c.scale(scale, scale);
        sel = null;
        Gfx.ol(0);
        drawScene(false);
        c.restore();
    }

    // ------------------------------------------------------------------ buttons
    private boolean isDown(int id) { return down && pressedId == id && !scrolling; }

    private void btn(int id, float cx, float cy, float r, int col, int icon) {
        float s = isDown(id) ? .9f : 1;
        ci(c, cx, cy + 8 * s, r * s, 0xFF2A1F2E);
        Gfx.ol(4.5f);
        ci(c, cx, cy, r * s, col);
        Gfx.ol(0);
        arc(c, cx, cy, r * s * .8f, r * s * .8f, 200, 70, r * .08f, al(0xFFFFFFFF, 120));
        Icons.draw(c, icon, cx, cy, r / 50f * s, 0xFFFFFFFF);
        reg(id, cx, cy, r * 2, r * 2);
        bround[nb - 1] = true;
    }

    private void reg(int id, float cx, float cy, float w, float h) {
        if (nb >= bid.length) return;
        bid[nb] = id; bx[nb] = cx; by[nb] = cy; bw[nb] = w; bh[nb] = h; bround[nb] = false; nb++;
    }

    private int hitBtn(float x, float y) {
        for (int i = nb - 1; i >= 0; i--) {
            float dx = x - bx[i], dy = y - by[i], r = bw[i] / 2 + 6;
            if (bround[i] ? dx * dx + dy * dy <= r * r : Math.abs(dx) <= bw[i] / 2 && Math.abs(dy) <= bh[i] / 2) return bid[i];
        }
        return -1;
    }

    // ================================================================== MAP (rotating world)
    private static final float PR = 980;
    private float mapAng, mapVel, spinTo = Float.NaN;
    private int mapFocus;
    private final Pose mp = new Pose();
    private static final int B_LEFT = 7, B_RIGHT = 8, B_PLAY = 9;

    private float step() { return 360f / Scenes.ALL.length; }
    private float pcx() { return W / 2; }
    private float pcy() { return 650 + PR; }

    /** Angle of location i relative to the top of the world, in -180..180. */
    private float rel(float planetAngle) {
        float a = planetAngle - mapAng;
        a = ((a % 360) + 540) % 360 - 180;
        return a;
    }

    private void updateMap(float dt) {
        if (!(down && mode == M_MAP)) {
            if (!Float.isNaN(spinTo)) {
                mapAng += (spinTo - mapAng) * Math.min(1, dt * 7);
                if (Math.abs(spinTo - mapAng) < .2f) { mapAng = spinTo; spinTo = Float.NaN; }
            } else {
                mapAng += mapVel * dt;
                mapVel *= (float) Math.pow(.06, dt);
                if (Math.abs(mapVel) < 40) {
                    mapVel = 0;
                    float target = Math.round(mapAng / step()) * step();
                    mapAng += (target - mapAng) * Math.min(1, dt * 8);
                }
            }
        }
        int n = Scenes.ALL.length;
        int f = ((Math.round(mapAng / step()) % n) + n) % n;
        if (f != mapFocus) { mapFocus = f; host.sound(S_TICK); }
    }

    private void spinBy(int d) {
        float base = Float.isNaN(spinTo) ? Math.round(mapAng / step()) * step() : spinTo;
        spinTo = base + d * step();
        mapVel = 0;
        host.sound(S_WHOOSH);
    }

    private void standOn(float planetAngle, float lift) {
        float r = rel(planetAngle);
        c.rotate(r, pcx(), pcy());
        c.translate(pcx(), pcy() - PR - lift);
    }

    private void drawMap() {
        float cx = pcx(), cy = pcy();
        grad(c, 0, 0, W, H, 0xFF6FCBFF, 0xFFD9F5FF);
        // sun
        for (int i = 0; i < 12; i++) {
            c.save(); c.translate(W - 210, 150); c.rotate(i * 30 + t * 8);
            rr(c, -10, -128, 20, 42, 10, al(0xFFFFE066, 170));
            c.restore();
        }
        ci(c, W - 210, 150, 72, 0xFFFFE066);
        Scenes.clouds(c, W, t, 60, 5, al(0xFFFFFFFF, 235));

        // atmosphere + planet
        ci(c, cx, cy, PR + 90, al(0xFFFFFFFF, 50));
        ci(c, cx, cy, PR + 46, al(0xFFFFFFFF, 70));
        c.save();
        c.rotate(-mapAng, cx, cy);
        Gfx.ol(7);
        ci(c, cx, cy, PR, 0xFF94DE78);
        Gfx.ol(0);
        ci(c, cx, cy, PR - 60, 0xFF7FCF66);
        cis(c, cx, cy, PR - 28, 0xFFF7E4B0, 26);
        for (int k = 0; k < 8; k++) {
            float a = k * 45 + 22.5f;
            c.save(); c.rotate(a, cx, cy);
            Gfx.ol(5);
            ov(c, cx, cy - PR + 210 + (k % 2) * 90, 120 - (k % 3) * 20, 46, 0xFF6EC8F5);
            Gfx.ol(0);
            ov(c, cx - 30, cy - PR + 200 + (k % 2) * 90, 40, 10, al(0xFFFFFFFF, 120));
            for (int j = 0; j < 6; j++) ci(c, cx + (j - 2.5f) * 46, cy - PR + 120 + (j % 2) * 30, 7, j % 2 == 0 ? 0xFFFFFFFF : 0xFFFFE066);
            c.restore();
        }
        c.restore();

        Gfx.shade = true;
        // trees & bushes between places
        for (int k = 0; k < Scenes.ALL.length; k++) {
            float a = k * step() + step() / 2;
            if (Math.abs(rel(a)) > 75) continue;
            c.save(); standOn(a - 9, -6); c.scale(.62f, .62f); Gfx.ol(8); PropArt.draw(c, k % 2 == 0 ? "tree" : "palm", t); c.restore();
            c.save(); standOn(a + 8, -4); c.scale(.6f, .6f); Gfx.ol(8); PropArt.draw(c, "bush", t); c.restore();
        }
        // little people walking around the world
        for (int k = 0; k < 5; k++) {
            int dir = k % 2 == 0 ? 1 : -1;
            float a = k * 72 + 14 + t * 2.6f * dir;
            if (Math.abs(rel(a)) > 70) continue;
            mp.reset();
            mp.t = t + k; mp.walk = 1; mp.walkPh = t * 9 + k; mp.blink = 0;
            c.save(); standOn(a, -4); c.scale(.3f * (dir < 0 ? -1 : 1), .3f); Gfx.ol(7);
            Avatar.draw(c, Look.PRESETS[(k * 3) % Look.PRESETS.length], mp);
            c.restore();
        }
        // buildings
        for (int i = 0; i < Scenes.ALL.length; i++) {
            float r = rel(i * step());
            if (Math.abs(r) > 75) continue;
            float f = Math.max(0, 1 - Math.abs(r) / 30);
            float s = 1.45f + .45f * Gfx.ease(f);
            float bob = (float) Math.abs(Math.sin(t * 3)) * 10 * f;
            c.save();
            standOn(i * step(), -8);
            Gfx.ol(0);
            ov(c, 0, 4, 130 * s / 1.45f, 20, al(0xFF000000, 40));
            Gfx.ol(5.5f / s);
            Scenes.drawIcon(c, Scenes.ALL[i].id, 0, -80 * s - bob, s, t);
            c.restore();
        }
        Gfx.ol(0);
        Gfx.shade = false;

        // focused place name
        Scenes.Loc lc = Scenes.ALL[mapFocus];
        float nf = Math.max(0, 1 - Math.abs(rel(mapFocus * step())) / 12);
        if (nf > 0) {
            float lw = tw(lc.name, 64) + 90;
            c.save(); c.translate(W / 2, 252); c.scale(Gfx.back(nf), Gfx.back(nf));
            rr(c, -lw / 2, -44 + 8, lw, 88, 44, 0xFF2A1F2E);
            Gfx.ol(5);
            rr(c, -lw / 2, -44, lw, 88, 44, lc.color);
            Gfx.ol(0);
            textOut(c, lc.name, 0, 22, 64, 0xFFFFFFFF, dk(lc.color, .45f), 10);
            c.restore();
        }

        // title
        String title = "CocoPopo";
        int[] cols = {0xFFFF5C8A, 0xFFFF9A3D, 0xFFFFC93C, 0xFF3CC5AF, 0xFF4FB3FF, 0xFF8E7BFF, 0xFFFF5C8A, 0xFFFF9A3D};
        float total = tw(title, 92) + 8 * 4;
        float tx = W / 2 - total / 2;
        for (int i = 0; i < title.length(); i++) {
            String ch = title.substring(i, i + 1);
            float cw = tw(ch, 92);
            float yy = 112 + (float) Math.sin(t * 3 + i * .7f) * 6;
            textOut(c, ch, tx + cw / 2, yy, 92, cols[i % cols.length], 0xFF2A1F2E, 14);
            tx += cw + 4;
        }

        btn(B_NEWCHAR, 100, 92, 56, 0xFFFF6F8F, Icons.PERSON_PLUS);
        textOut(c, "Dress-up", 100, 186, 30, 0xFFFFFFFF, 0xFF2A1F2E, 8);
        btn(B_LEFT, 110, 640, 56, 0xFF4FB3FF, Icons.BACK);
        c.save(); c.translate(W - 110, 640); c.scale(-1, 1);
        btn(B_RIGHT, 0, 0, 56, 0xFF4FB3FF, Icons.BACK);
        c.restore();
        bx[nb - 1] = W - 110; by[nb - 1] = 640;
        float pulse = 1 + (float) Math.sin(t * 5) * .05f;
        btn(B_PLAY, W / 2, 985, 70 * pulse, 0xFF3CC57B, Icons.PLAY);
    }

    private boolean tapBuilding(float x, float y) {
        float cx = pcx(), cy = pcy();
        for (int i = 0; i < Scenes.ALL.length; i++) {
            float r = rel(i * step());
            if (Math.abs(r) > 70) continue;
            double a = Math.toRadians(r);
            float bx0 = cx + (float) Math.sin(a) * (PR + 150), by0 = cy - (float) Math.cos(a) * (PR + 150);
            if ((x - bx0) * (x - bx0) + (y - by0) * (y - by0) < 190 * 190) {
                if (i == mapFocus && Math.abs(r) < 6) go(SCENE, Scenes.ALL[i].id, 0, null);
                else { spinTo = mapAng + r; mapVel = 0; host.sound(S_WHOOSH); }
                return true;
            }
        }
        return false;
    }

    private float rndf(int i) { double v = Math.sin(i * 12.9898 + 4.1) * 43758.5453; return (float) (v - Math.floor(v)); }

    // ================================================================== SCENE
    private void drawScene(boolean ui) {
        Scenes.drawBg(c, loc, W, H, t);
        sortOrder();
        for (Obj o : order) drawObj(o);
        drawParts();
        drawBadge();
        if (!ui) return;

        if (sel != null && objs.contains(sel) && !(down && mode == M_DRAG && moved)) drawPopup();
        else if (sel != null && !objs.contains(sel)) sel = null;

        btn(B_HOME, 100, 92, 52, 0xFFFF8F5C, Icons.HOME);
        btn(B_CAM, W - 100, 92, 52, 0xFF4FB3FF, Icons.CAMERA);
        btn(B_CLEAR, W - 226, 92, 52, 0xFFFFB02E, Icons.BROOM);
        drawTray();
    }

    private void sortOrder() {
        order.clear();
        order.addAll(objs);
        Collections.sort(order, byDepth);
        if (dragObj != null && order.remove(dragObj)) order.add(dragObj);
        // things riding/held by the dragged object stay on top with it
        for (int i = 0; i < objs.size(); i++) {
            Obj q = objs.get(i);
            if (dragObj != null && q.link == dragObj && order.remove(q)) order.add(q);
        }
    }

    private void drawObj(Obj o) {
        if (o.state == Obj.HELD && o.link != null) return; // drawn by its holder
        float sc = o.scale * Gfx.back(o.pop);
        boolean shadow = !(o.prop != null && (o.prop.equals("rug") || Life.wall(o.prop))) && o.state != Obj.LIE && o.state != Obj.SIT;
        if (shadow && sc > 0) {
            float ground = o.falling ? Life.floorBelow(loc, o.peakY) : o.y;
            float k = (1 - o.lift * .22f) * (o.falling ? Math.max(.4f, 1 - (ground - o.y) / 500) : 1);
            ov(c, o.x, ground, o.bw * .36f * sc * k, 15 * sc * k, al(0xFF000000, (int) (46 - o.lift * 14)));
        }
        Gfx.ol(o.isChar ? 6.5f : 5f);
        Gfx.shade = true;
        float yoff = -o.lift * 36 * o.scale - o.hop;
        if (o.prop != null && o.prop.equals("balloon")) yoff -= 8 + (float) Math.sin(t * 1.6f + o.phase) * 8;
        if (o.launchT > 0) {
            float u = 3 - o.launchT;
            yoff -= u < 1.5f ? u * u * 900 : (3 - u) * (3 - u) * 900;
            if (rnd.nextFloat() < .6f) burst(o.x, o.y + yoff, 1, 0xFFFFB93D, 0, 200);
        }
        boolean glow = o == tgObj && dragObj != null && moved;
        if (glow) {
            int keep = Gfx.olc;
            Gfx.olc = al(0xFFFFFFFF, 220);
            float gw = 13 + (float) Math.sin(t * 10) * 4;
            c.save();
            c.translate(o.x, o.y + yoff);
            c.scale(o.scale * (o.flip && o.state != Obj.LIE ? -1 : 1), o.scale);
            Gfx.ol(gw / o.scale);
            boolean sh = Gfx.shade; Gfx.shade = false;
            if (o.isChar && o.state != Obj.LIE) Avatar.draw(c, o.look, o.pose); else if (!o.isChar) PropArt.draw(c, o.prop, t + o.phase, o.pstate);
            Gfx.shade = sh;
            c.restore();
            Gfx.olc = keep;
            Gfx.ol(o.isChar ? 6.5f : 5f);
        }
        c.save();
        c.translate(o.x, o.y + yoff);
        c.rotate(o.tilt + (float) Math.sin(t * 30) * 6 * o.wiggle);
        float sq = o.sq;
        if (o.state == Obj.LIE) {
            c.rotate(-90 * o.slot);
            c.scale(sc * .8f * (o.slot < 0 ? -1 : 1), sc * .8f);
        } else c.scale(sc * (1 - sq * .5f) * (o.flip ? -1 : 1) * (1 + o.lift * .05f), sc * (1 + sq));
        if (o.isChar) {
            Avatar.draw(c, o.look, o.pose);
        } else {
            PropArt.draw(c, o.prop, t + o.phase, o.pstate);
        }
        c.restore();
        if (o.state == Obj.LIE && o.link != null) {
            Obj b = o.link;
            c.save();
            c.translate(b.x, b.y - b.hop);
            c.scale(b.scale * (o.slot < 0 ? -1 : 1), b.scale);
            PropArt.blanket(c, b.prop);
            c.restore();
        }
        if ((o.state == Obj.BATHE || (o.state == Obj.SIT && o.link != null && !o.link.isChar && Life.coversSitter(o.link.prop))) && o.link != null) {
            Obj b = o.link;
            c.save();
            c.translate(b.x, b.y - b.hop - b.lift * 36 * b.scale);
            c.scale(b.scale * (b.flip ? -1 : 1), b.scale);
            Gfx.ol(5);
            PropArt.front(c, b.prop, t);
            c.restore();
            if (Life.tub(b.prop) && !b.contents.isEmpty()) drawContents(b, -b.hop);
        }
        if (!o.isChar && !o.contents.isEmpty()) drawContents(o, yoff);
        if (o.isChar && o.held != null) drawHeld(o, o.held);
        Gfx.ol(0);
        Gfx.shade = false;
        if (o.isChar && o.emoteT >= 0) {
            float a = Gfx.back(o.emoteT * 4);
            float fo = o.emoteT > 1.6f ? Math.max(0, (2 - o.emoteT) / .4f) : 1;
            float headY = o.state == Obj.LIE ? o.y - 90 * o.scale : (o.state == Obj.SIT ? o.y + yoff - (o.bh - 60) * o.scale : o.y + yoff - o.bh * o.scale);
            float headX = o.state == Obj.LIE ? o.x - 211 * o.scale * o.slot : o.x;
            Icons.emote(c, o.emote, headX, headY - 50 - (float) Math.sin(t * 5) * 4, o.scale * .9f * a * fo, t);
        }
    }

    /** Peek at what is inside a container. */
    private void drawContents(Obj o, float yoff) {
        if (o.prop.equals("fridge") && o.pstate == 0) return;
        if (o.prop.equals("gift") && o.pstate == 0) return;
        int n = Math.min(3, o.contents.size());
        float base;
        switch (o.prop) {
            case "cart": base = -96; break;
            case "crate": base = -70; break;
            case "backpack": base = -130; break;
            case "tub": base = -126; break;
            case "gift": base = -80; break;
            case "fridge": base = -250; break;
            case "shelf": base = -262; break;
            default: base = -o.bh * .6f;
        }
        Gfx.ol(4);
        for (int i = 0; i < n; i++) {
            String id = o.contents.get(o.contents.size() - 1 - i);
            PropArt.Def d = PropArt.get(id);
            float k = Math.min(.5f, 70 / Math.max(d.w, d.h)) * o.scale;
            float bob = o.prop.equals("tub") ? (float) Math.sin(t * 3 + i) * 5 : 0;
            c.save();
            float off = o.prop.equals("tub") ? 110 : 0;
            c.translate(o.x + ((i - (n - 1) / 2f) * 46 + off) * o.scale * (o.flip ? -1 : 1), o.y + yoff + base * o.scale + bob);
            c.scale(k, k);
            PropArt.draw(c, id, t, 0);
            c.restore();
        }
        Gfx.ol(5);
    }

    private void drawBadge() {
        if (tgObj == null || dragObj == null || !moved) return;
        Obj o = tgObj;
        int icon; int col;
        switch (tgKind) {
            case K_SIT: icon = Icons.CHAIR; col = 0xFF4FB3FF; break;
            case K_LIE: icon = Icons.ZZZ; col = 0xFF8E7BFF; break;
            case K_GIVE: icon = Icons.HAND; col = 0xFFFF9A3D; break;
            case K_IN: icon = Icons.INBOX; col = 0xFF58B368; break;
            case K_BATHE: icon = Icons.BUBBLES; col = 0xFF3CC5DF; break;
            case K_SLIDE: icon = Icons.SLIDEDOWN; col = 0xFFFF6F8F; break;
            case K_HUG: icon = Icons.SAVE; col = 0xFFFF5C8A; break;
            case K_SHOULDER: icon = Icons.UP; col = 0xFFFFB02E; break;
            case K_BOUNCE: icon = Icons.UP; col = 0xFFB67CFF; break;
            default: return;
        }
        float bx = o.x + o.bw * o.scale * .5f + 20, by = o.y + o.dyo - o.bh * o.scale - 10;
        if (Math.abs(bx - dragObj.x) < 120) bx = o.x - o.bw * o.scale * .5f - 20;
        bx = Gfx.clamp(bx, 60, W - 60);
        if (by < 70) by = 70;
        float s = 1 + (float) Math.sin(t * 8) * .08f;
        ci(c, bx, by + 6, 46 * s, 0xFF2A1F2E);
        Gfx.ol(4.5f);
        ci(c, bx, by, 46 * s, 0xFFFFFFFF);
        Gfx.ol(0);
        Icons.draw(c, icon, bx, by, .82f * s, col);
    }

    private void drawHeld(Obj holder, Obj it) {
        float hs = holdScale(it);
        it.ds = hs / it.scale;
        c.save();
        c.translate(it.x, it.y);
        c.rotate(holder.tilt);
        c.scale(hs * (holder.flip ? -1 : 1), hs);
        Gfx.ol(5f / Math.max(.3f, hs) * Math.min(1, holder.scale));
        PropArt.draw(c, it.prop, t + it.phase, it.pstate);
        c.restore();
        // fingers wrap around the item
        float dir = holder.flip ? -1 : 1;
        int skin = Look.SKIN[holder.look.skin];
        Gfx.ol(6.5f * holder.scale);
        if (holder.pose.holdType == 1 && holder.pose.arm < .5f) {
            float hy = holder.y + holder.dyo + Avatar.handY(holder.pose, holder.look) * holder.scale;
            for (int sd = -1; sd <= 1; sd += 2) ov(c, holder.x + sd * 30 * holder.scale, hy + 10 * holder.scale, 17 * holder.scale, 19 * holder.scale, skin);
        } else {
            float hx = holder.x + Avatar.handX(holder.pose, holder.look) * holder.scale * dir;
            float hy = holder.y + holder.dyo + Avatar.handY(holder.pose, holder.look) * holder.scale;
            ov(c, hx, hy, 17 * holder.scale, 19 * holder.scale, skin);
        }
        Gfx.ol(6.5f);
    }

    private void drawPopup() {
        boolean ch = sel.isChar;
        int[] ids = ch ? new int[]{P_EDIT, P_EMOTE, P_FLIP, P_BIG, P_SMALL, P_COPY, P_DEL} : new int[]{P_FLIP, P_BIG, P_SMALL, P_COPY, P_DEL};
        int[] icons = ch ? new int[]{Icons.EDIT, Icons.SMILE, Icons.FLIP, Icons.PLUS, Icons.MINUS, Icons.COPY, Icons.TRASH}
                         : new int[]{Icons.FLIP, Icons.PLUS, Icons.MINUS, Icons.COPY, Icons.TRASH};
        int[] cols = ch ? new int[]{0xFF6C7BFF, 0xFFFFB02E, 0xFF3CC5AF, 0xFF58B368, 0xFF58B368, 0xFF4FB3FF, 0xFFFF5C73}
                        : new int[]{0xFF3CC5AF, 0xFF58B368, 0xFF58B368, 0xFF4FB3FF, 0xFFFF5C73};
        int n = ids.length;
        float pw = n * 92 + 24, ph = 108;
        float top = sel.y - sel.bh * sel.scale - ph - 24;
        if (top < 150) top = Math.min(H - 340 - ph, sel.y + 24);
        float x0 = Gfx.clamp(sel.x - pw / 2, 20, W - 20 - pw);
        rr(c, x0, top + 6, pw, ph, 54, al(0xFF000000, 40));
        Gfx.ol(5);
        rr(c, x0, top, pw, ph, 54, 0xFFFFFFFF);
        float tipX = Gfx.clamp(sel.x, x0 + 40, x0 + pw - 40);
        poly(c, 0xFFFFFFFF, tipX - 16, top + ph - 6, tipX + 16, top + ph - 6, tipX, top + ph + 18);
        Gfx.ol(0);
        rect(c, tipX - 12, top + ph - 8, 24, 8, 0xFFFFFFFF);
        for (int i = 0; i < n; i++) btn(ids[i], x0 + 12 + 46 + i * 92, top + ph / 2, 38, cols[i], icons[i]);
    }

    // ------------------------------------------------------------------ tray
    private static final float TRAY_H = 330, CARD = 200, CARD_GAP = 22, CARD_X0 = 420;

    private float trayTop() { return H - TRAY_H * Gfx.ease(trayA); }

    private int cardCount() {
        if (trayTab == 1) return 1 + lib.size() + Look.PRESETS.length;
        int n = 0;
        for (PropArt.Def d : PropArt.ALL) if (d.cat == trayCat) n++;
        return n;
    }

    private PropArt.Def itemAt(int idx) {
        int n = 0;
        for (PropArt.Def d : PropArt.ALL) if (d.cat == trayCat) { if (n == idx) return d; n++; }
        return null;
    }

    private void clampTray() {
        float max = Math.max(0, cardCount() * (CARD + CARD_GAP) - (W - CARD_X0 - 30));
        if (trayScroll < 0) { trayScroll = 0; trayVel = 0; }
        if (trayScroll > max) { trayScroll = max; trayVel = 0; }
    }

    private void drawTray() {
        float top = trayTop();
        if (trayA > 0.01f) {
            rr(c, -40, top + 8, W + 80, TRAY_H + 60, 48, al(0xFF000000, 45));
            Gfx.ol(5);
            rr(c, -40, top, W + 80, TRAY_H + 60, 48, 0xFFFFF7E6);
            Gfx.ol(0);
            rr(c, -40, top, W + 80, 18, 8, 0xFFFFD98A);
            float chipY = top + 52;
            if (trayTab == 2) {
                float x = CARD_X0;
                for (int i = 0; i < PropArt.CATS.length; i++) {
                    boolean on = i == trayCat;
                    float w = tw(PropArt.CATS[i], 32) + 52;
                    float s = isDown(B_CHIP + i) ? .94f : 1;
                    Gfx.ol(4);
                    rr(c, x, chipY - 28 * s, w, 56 * s, 28, on ? 0xFF6C7BFF : 0xFFFFFFFF);
                    Gfx.ol(0);
                    text(c, PropArt.CATS[i], x + w / 2, chipY + 11, 32, on ? 0xFFFFFFFF : 0xFF6B6480, Paint.Align.CENTER);
                    reg(B_CHIP + i, x + w / 2, chipY, w, 64);
                    x += w + 14;
                }
            } else {
                text(c, "Characters", CARD_X0, chipY + 12, 38, 0xFFC24E6E, Paint.Align.LEFT);
            }
            c.save();
            c.clipRect(CARD_X0 - 14, top + 84, W, H);
            int n = cardCount();
            for (int i = 0; i < n; i++) {
                float x = CARD_X0 - trayScroll + i * (CARD + CARD_GAP);
                if (x > W || x + CARD < CARD_X0 - 20) continue;
                drawCard(i, x, top + 94);
            }
            c.restore();
        }
        float by0 = H - 92;
        btn(B_TAB_CHAR, 110, by0, 56, 0xFFFF6F8F, Icons.PEOPLE);
        btn(B_TAB_ITEM, 250, by0, 56, 0xFF6C7BFF, Icons.CUBE);
        if (trayTab != 0) cis(c, trayTab == 1 ? 110 : 250, by0, 66, 0xFFFFFFFF, 7);
    }

    private void drawCard(int i, float x, float y) {
        boolean pressed = trayCard == i && down && mode == M_TRAY && !trayDecided;
        float s = pressed ? .94f : 1;
        c.save();
        c.translate(x + CARD / 2, y + CARD / 2);
        c.scale(s, s);
        rr(c, -CARD / 2, -CARD / 2 + 8, CARD, CARD, 30, al(0xFF000000, 30));
        Gfx.ol(4.5f);
        rr(c, -CARD / 2, -CARD / 2, CARD, CARD, 30, 0xFFFFFFFF);
        Gfx.ol(0);
        Gfx.shade = true;
        if (trayTab == 1) {
            if (i == 0) {
                ci(c, 0, -10, 52, 0xFFFFE3EA);
                Icons.draw(c, Icons.PERSON_PLUS, 0, -10, 1.3f, 0xFFFF6F8F);
                text(c, "New", 0, 76, 30, 0xFFC24E6E, Paint.Align.CENTER);
            } else {
                Look l = i - 1 < lib.size() ? lib.get(i - 1) : Look.PRESETS[i - 1 - lib.size()];
                c.save();
                c.clipRect(-CARD / 2, -CARD / 2, CARD / 2, CARD / 2 - 34);
                c.translate(0, 92);
                c.scale(.5f, .5f);
                Gfx.ol(7);
                Avatar.draw(c, l, IDLE);
                Gfx.ol(0);
                c.restore();
                String nm = i - 1 < lib.size() ? "Mine " + (i) : Look.PRESET_NAMES[i - 1 - lib.size()];
                text(c, nm, 0, 82, 28, 0xFF6B6480, Paint.Align.CENTER);
            }
        } else {
            PropArt.Def d = itemAt(i);
            if (d != null) {
                float k = Math.min(150 / d.w, 124 / d.h);
                c.save(); c.translate(0, 52); c.scale(k, k); Gfx.ol(4 / k); PropArt.draw(c, d.id, t); Gfx.ol(0); c.restore();
                text(c, d.name, 0, 84, 26, 0xFF6B6480, Paint.Align.CENTER);
            }
        }
        Gfx.shade = false;
        c.restore();
    }

    private int cardAt(float x, float y) {
        float top = trayTop();
        if (y < top + 84 || y > top + TRAY_H || x < CARD_X0 - 10) return -1;
        float rel = x - CARD_X0 + trayScroll;
        int i = (int) Math.floor(rel / (CARD + CARD_GAP));
        float within = rel - i * (CARD + CARD_GAP);
        if (within > CARD || i < 0 || i >= cardCount()) return -1;
        return i;
    }

    private Obj spawnCard(int i, float x, float y) {
        Obj o;
        if (trayTab == 1) {
            if (i == 0) return null;
            Look l = i - 1 < lib.size() ? lib.get(i - 1) : Look.PRESETS[i - 1 - lib.size()];
            o = Obj.character(l.copy(), x, y);
        } else {
            PropArt.Def d = itemAt(i);
            if (d == null) return null;
            o = Obj.prop(d.id, x, y);
        }
        objs.add(o);
        sel = o;
        o.pop = 0;
        host.haptic();
        return o;
    }

    // ================================================================== touch
    void touch(int act, float px, float py) {
        float x = px / scale, y = py / scale;
        if (fadeDir == 1) return;
        switch (act) {
            case 0: onDown(x, y); break;
            case 1: onMove(x, y); break;
            default: onUp(x, y, act == 3); break;
        }
    }

    private void onDown(float x, float y) {
        down = true; moved = false; scrolling = false;
        dragVX = dragVY = 0; lastMoveNs = System.nanoTime();
        sx = lx = x; sy = ly = y;
        mode = M_NONE; trayDecided = false; trayCard = -1;
        pressedId = hitBtn(x, y);
        if (pressedId >= 0) return;
        if (screen == SCENE) {
            if (trayA > .5f && y > trayTop() + 70) {
                mode = M_TRAY; trayVel = 0;
                trayCard = cardAt(x, y);
                return;
            }
            for (int i = order.size() - 1; i >= 0; i--) {
                Obj o = order.get(i);
                if (o.hit(x, y)) {
                    mode = M_DRAG; dragObj = o;
                    dragDX = o.x - x; dragDY = o.y - y;
                    return;
                }
            }
        } else if (screen == MAP) {
            mode = M_MAP; mapVel = 0; spinTo = Float.NaN;
        } else if (screen == EDITOR) {
            if (x > W * .44f) mode = M_PANEL;
            else if (Math.abs(x - W * .2f) < 190 && y > 250 && y < 960) mode = M_PREVIEW;
        }
    }

    private void onMove(float x, float y) {
        if (!down) return;
        float dx = x - sx, dy = y - sy;
        boolean far = dx * dx + dy * dy > 18 * 18;
        long now = System.nanoTime();
        float mdt = Math.max(.004f, (now - lastMoveNs) / 1e9f);
        lastMoveNs = now;
        if (mode == M_DRAG && dragObj != null) {
            if (far && !moved) { moved = true; sel = dragObj; host.haptic(); pickUp(dragObj); }
            if (moved) {
                dragVX = dragVX * .5f + (x - lx) / mdt * .5f;
                dragVY = dragVY * .5f + (y - ly) / mdt * .5f;
                dragObj.vx = x - lx > 0 ? (x - lx) * 60 : (x - lx) * 60;
                dragObj.x = Gfx.clamp(x + dragDX, 30, W - 30);
                dragObj.y = Gfx.clamp(y + dragDY, 150, H - 6);
                fingerX = x; fingerY = y;
                Obj before = tgObj;
                resolve(dragObj, x, y);
                if (tgObj != null && tgObj != before) { host.haptic(); host.sound(S_TICK); }
                if (dragObj.held != null) dragObj.dyo = -dragObj.lift * 36 * dragObj.scale;
            }
        } else if (mode == M_TRAY) {
            if (!trayDecided && far) {
                trayDecided = true;
                if (trayCard >= 0 && dy < -20 && Math.abs(dy) > Math.abs(dx) * .8f) {
                    Obj o = spawnCard(trayCard, x, y);
                    if (o != null) {
                        host.sound(S_POP);
                        fingerX = x; fingerY = y;
                        mode = M_DRAG; dragObj = o; moved = true;
                        dragDX = 0; dragDY = 0; o.lift = 1;
                        return;
                    }
                }
                scrolling = true;
            }
            if (trayDecided && mode == M_TRAY) { trayScroll -= x - lx; trayVel = -(x - lx) * 40; clampTray(); }
        } else if (mode == M_MAP) {
            if (far) moved = true;
            float dDeg = -(x - lx) / (PR * (float) Math.PI / 180) * 1.15f;
            mapAng += dDeg;
            mapVel = mapVel * .5f + dDeg / mdt * .5f;
        } else if (mode == M_PANEL) {
            if (far) scrolling = true;
            if (scrolling) { edScroll = Gfx.clamp(edScroll - (y - ly), 0, edMax); pressedId = -1; }
        }
        if (far && pressedId >= 0 && hitBtn(x, y) != pressedId) pressedId = -1;
        lx = x; ly = y;
    }

    private void onUp(float x, float y, boolean cancel) {
        if (!down) return;
        down = false;
        int hit = cancel ? -1 : hitBtn(x, y);
        int id = pressedId;
        pressedId = -1;
        if (id >= 0 && hit == id) { onButton(id); mode = M_NONE; dragObj = null; return; }

        if (screen == SCENE) {
            if (mode == M_DRAG && dragObj != null) {
                Obj o = dragObj;
                dragObj = null;
                Obj prev = sel;
                if (!moved) {
                    if (!o.isChar && prev != null && prev.isChar && prev != o && prev.state != Obj.HELD
                            && o.state != Obj.HELD && objs.contains(prev) && walkUse(prev, o)) {
                        sel = prev;
                        burst(o.x, o.y - o.bh * o.scale * .5f, 6, 0xFFFFE066, 2, 260);
                        host.sound(S_TICK);
                    } else { sel = o; tapObj(o); }
                } else { sel = o; drop(o); }
            } else if (mode == M_TRAY) {
                if (!trayDecided && trayCard >= 0 && cardAt(x, y) == trayCard) {
                    if (trayTab == 1 && trayCard == 0) go(EDITOR, null, 1, null);
                    else {
                        Obj o = spawnCard(trayCard, W * .5f + (rnd.nextFloat() - .5f) * 500, 420 + rnd.nextFloat() * 100);
                        if (o != null) { host.sound(S_POP); startFall(o, 0, 0); }
                    }
                }
            } else if (mode == M_NONE && !moved) {
                tapFloor(x, y);
            }
        } else if (screen == MAP && mode == M_MAP && !moved) {
            mapVel = 0;
            tapBuilding(x, y);
        } else if (screen == EDITOR && mode == M_PREVIEW && !scrolling) {
            edHopV = 700; edEmote = rnd.nextInt(7); edEmoteT = 0;
            edMood = new int[]{1, 5, 0, 3, 2, 2, 1}[edEmote];
        }
        mode = M_NONE;
        dragObj = null;
    }

    // ------------------------------------------------------------------ interactions
    static final int K_NONE = 0, K_SIT = 1, K_LIE = 2, K_GIVE = 3, K_IN = 4, K_BATHE = 5, K_SLIDE = 6, K_HUG = 7,
        K_SHOULDER = 8, K_BOUNCE = 9, K_PICK = 10, K_USE = 11;

    // current drop target while dragging (also drives the glow + badge)
    private Obj tgObj;
    private int tgKind, tgSlot;
    private float tgScore;
    private float fingerX, fingerY, feetX = -1e9f, feetY;

    private void consider(Obj obj, int kind, int slot, float cx, float cy, float rx, float ry, float px, float py, float bx, float by) {
        float d1 = ((px - cx) / rx) * ((px - cx) / rx) + ((py - cy) / ry) * ((py - cy) / ry);
        float d2 = ((bx - cx) / rx) * ((bx - cx) / rx) + ((by - cy) / ry) * ((by - cy) / ry);
        float d = Math.min(d1, d2);
        if (feetX > -1e8f) d = Math.min(d, ((feetX - cx) / rx) * ((feetX - cx) / rx) + ((feetY - cy) / ry) * ((feetY - cy) / ry));
        if (d <= 1 && d < tgScore) { tgScore = d; tgObj = obj; tgKind = kind; tgSlot = slot; }
    }

    /** Finds what the dragged thing would interact with if released now (forgiving, finger- and body-based). */
    private void resolve(Obj d, float fx, float fy) {
        tgObj = null; tgKind = K_NONE; tgScore = 1.0001f;
        if (d == null) return;
        feetX = -1e9f;
        if (d.isChar) {
            float bx = d.x, by = d.y - (d.bh - 60) * d.scale * .5f;
            feetX = d.x; feetY = d.y;
            for (Obj q : objs) {
                if (q == d) continue;
                float qs = Math.max(q.scale, .85f);
                if (q.isChar) {
                    if (q.state == Obj.HELD || q.state == Obj.LIE || q.state == Obj.BATHE || q.state == Obj.SLIDE) continue;
                    if (q.link == d) continue;
                    float headY = q.y + q.dyo + (Avatar.neckY(q.look) - 90) * q.scale;
                    float fk = feetX; feetX = -1e9f;
                    if (!hasRider(q)) consider(q, K_SHOULDER, 0, q.x, headY, 95 * qs, 80 * qs, fx, fy, bx, by - 60 * d.scale);
                    if (q.state == Obj.FREE) consider(q, K_HUG, 0, q.x, q.y - q.bh * q.scale * .4f, 120 * qs, 130 * qs, fx, fy, bx, by);
                    feetX = fk;
                    continue;
                }
                float[] st = Life.seat(q.prop);
                if (st != null) {
                    for (int j = 0; j + 1 < st.length; j++) {
                        if (seatTaken(q, j)) continue;
                        float sx = q.x + st[1 + j] * q.scale * (q.flip ? -1 : 1);
                        consider(q, K_SIT, j, sx, q.y + (st[0] - 40) * q.scale, 120 * qs, 130 * qs, fx, fy, bx, by);
                    }
                }
                float m = Life.bed(q.prop);
                if (m != 0 && occupant(q) == null)
                    consider(q, K_LIE, 0, q.x, q.y + (m - 30) * q.scale, q.bw * q.scale * .6f, 150 * qs, fx, fy, bx, by);
                if (Life.tub(q.prop) && occupant(q) == null)
                    consider(q, K_BATHE, 0, q.x, q.y - 110 * q.scale, q.bw * q.scale * .55f, 140 * qs, fx, fy, bx, by);
                if (Life.slide(q.prop))
                    consider(q, K_SLIDE, 0, q.x - 100 * q.scale * (q.flip ? -1 : 1), q.y - 300 * q.scale, 140 * qs, 140 * qs, fx, fy, bx, by);
                if (Life.bouncy(q.prop) && q.state == Obj.FREE)
                    consider(q, K_BOUNCE, 0, q.x, q.y - 80 * q.scale, 110 * qs, 110 * qs, fx, fy, bx, by + d.bh * d.scale * .4f);
            }
            feetX = -1e9f;
        } else {
            float bx = d.x, by = d.y - d.bh * d.scale * .5f;
            for (Obj q : objs) {
                if (q == d) continue;
                float qs = Math.max(q.scale, .85f);
                if (q.isChar) {
                    if (q.held == null && Life.holdable(d) && q.state != Obj.LIE && q.state != Obj.HELD && q.state != Obj.SLIDE)
                        consider(q, K_GIVE, 0, q.x, q.y + q.dyo - q.bh * q.scale * .45f, 130 * qs, q.bh * q.scale * .5f, fx, fy, bx, by);
                    continue;
                }
                int cap = Life.capacity(q.prop);
                if (cap > 0 && q.contents.size() < cap && Life.holdable(d) && Life.capacity(d.prop) == 0
                        && !(q.prop.equals("gift") && q.pstate == 0)) {
                    float zy = Life.tub(q.prop) ? q.y - 120 * q.scale : q.y - q.bh * q.scale * .55f;
                    consider(q, K_IN, 0, q.x, zy, q.bw * q.scale * .5f, Math.max(90, q.bh * q.scale * .5f), fx, fy, bx, by);
                }
            }
        }
    }

    private boolean hasRider(Obj c2) {
        for (Obj q : objs) if (q.isChar && q.link == c2 && q.state == Obj.SIT) return true;
        return false;
    }

    private Obj occupant(Obj prop) {
        for (Obj q : objs) if (q.isChar && q.link == prop && (q.state == Obj.LIE || q.state == Obj.BATHE)) return q;
        return null;
    }

    private int freeSlot(Obj seat, Obj who) {
        float[] st = Life.seat(seat.prop);
        if (st == null) return -1;
        int best = -1; float bd = 1e9f;
        for (int j = 0; j + 1 < st.length; j++) {
            if (seatTaken(seat, j)) continue;
            float d = Math.abs(seat.x + st[1 + j] * seat.scale * (seat.flip ? -1 : 1) - who.x);
            if (d < bd) { bd = d; best = j; }
        }
        return best;
    }

    private void pickUp(Obj o) {
        host.sound(S_POP);
        o.walking = false; o.falling = false; o.act = 0; o.pend = null; o.nwp = 0;
        if (o.state == Obj.HELD && o.link != null) o.link.held = null;
        o.state = Obj.FREE; o.link = null;
    }

    private void drop(Obj o) {
        resolve(o, fingerX, fingerY);
        Obj tg = tgObj; int kind = tgKind, slot = tgSlot;
        tgObj = null; tgKind = K_NONE;
        if (tg != null && perform(o, tg, kind, slot)) return;
        if ((System.nanoTime() - lastMoveNs) / 1e9f > .08f) { dragVX = 0; dragVY = 0; }
        if (o.isChar) startFall(o, 0, 0);
        else {
            boolean toss = Life.tossable(o);
            startFall(o, toss ? Gfx.clamp(dragVX, -2600, 2600) * .8f : 0, toss ? Gfx.clamp(dragVY, -2600, 1200) * .8f : 0);
            if (!o.falling) { o.sqv = -2.4f; host.sound(S_DROP); }
            else if (Math.abs(dragVX) + Math.abs(dragVY) > 1500) host.sound(S_WHOOSH);
        }
    }

    private boolean seatTaken(Obj s, int slot) {
        for (Obj q : objs) if (q.isChar && q.link == s && q.state == Obj.SIT && q.slot == slot) return true;
        return false;
    }

    /** Carries out an interaction between a character/item and a target. */
    private boolean perform(Obj o, Obj tg, int kind, int slot) {
        o.walking = false; o.falling = false; o.nwp = 0;
        switch (kind) {
            case K_SIT:
                if (seatTaken(tg, slot)) return false;
                o.state = Obj.SIT; o.link = tg; o.slot = slot; o.flip = false;
                o.sqv = -2.4f; o.setFace(Avatar.F_HAPPY, 1.2f);
                host.sound(S_DROP);
                if (Life.vehicle(tg.prop)) { o.emote = 1; o.emoteT = 0; }
                return true;
            case K_LIE:
                if (occupant(tg) != null) return false;
                o.state = Obj.LIE; o.link = tg; o.slot = tg.flip ? -1 : 1;
                o.emote = 5; o.emoteT = 0;
                host.sound(S_DROP);
                return true;
            case K_BATHE:
                if (occupant(tg) != null) return false;
                o.state = Obj.BATHE; o.link = tg; o.flip = false;
                o.setFace(Avatar.F_LAUGH, 1.5f);
                burst(tg.x, tg.y - 130 * tg.scale, 14, 0xFFFFFFFF, 3, 260);
                host.sound(S_WHOOSH);
                return true;
            case K_SLIDE:
                o.state = Obj.SLIDE; o.link = tg; o.slideT = 0;
                o.setFace(Avatar.F_LAUGH, 1.6f);
                host.sound(S_WHOOSH);
                return true;
            case K_HUG: {
                float side = o.x >= tg.x ? 1 : -1;
                o.x = Gfx.clamp(tg.x + side * 118 * Math.max(o.scale, tg.scale), 60, W - 60);
                o.y = tg.y;
                o.flip = side > 0; tg.flip = side < 0;
                o.hugT = 1.8f; tg.hugT = 1.8f;
                o.emote = 0; o.emoteT = 0; tg.emote = 0; tg.emoteT = 0;
                tg.walking = false; tg.act = 0;
                burst((o.x + tg.x) / 2, o.y - 260 * o.scale, 6, 0xFFFF6F8F, 2, 260);
                host.sound(S_SPARK);
                return true;
            }
            case K_SHOULDER:
                o.state = Obj.SIT; o.link = tg; o.slot = 0; o.flip = tg.flip;
                o.setFace(Avatar.F_LAUGH, 1.5f); tg.setFace(Avatar.F_WOW, 1);
                host.sound(S_SPARK);
                return true;
            case K_BOUNCE:
                o.x = tg.x; o.y = tg.y - 100 * tg.scale;
                o.falling = true; o.fvx = (rnd.nextFloat() - .5f) * 300; o.vy = -2100; o.peakY = o.y;
                tg.sqv = -4;
                o.setFace(Avatar.F_LAUGH, 1.5f);
                host.sound(S_BOUNCE);
                return true;
            case K_GIVE:
            case K_PICK: {
                Obj ch = kind == K_GIVE ? tg : o, it = kind == K_GIVE ? o : tg;
                if (ch.held != null || !Life.holdable(it)) return false;
                if (it.state == Obj.HELD && it.link != null) it.link.held = null;
                ch.held = it; it.state = Obj.HELD; it.link = ch; it.falling = false;
                ch.hopV = 380;
                host.sound(S_SPARK);
                if (Life.food(it.prop)) eat(ch);
                else { ch.emote = 1; ch.emoteT = 0; ch.setFace(Avatar.F_HAPPY, 1.2f); }
                return true;
            }
            case K_IN:
                if (tg.contents.size() >= Life.capacity(tg.prop)) return false;
                tg.contents.add(o.prop);
                objs.remove(o);
                if (sel == o) sel = tg;
                tg.wiggle = .8f; tg.sqv = -2;
                if (tg.prop.equals("fridge")) tg.pstate = 1;
                burst(tg.x, tg.y - tg.bh * tg.scale * .6f, 5, 0xFFFFE066, 2, 250);
                host.sound(S_POP);
                return true;
            case K_USE:
                tapObj(tg);
                return true;
            default:
                return false;
        }
    }

    /** What a character would naturally do with this prop when sent to it. */
    private int useKind(Obj ch, Obj q) {
        if (q.isChar) return K_NONE;
        if (Life.seat(q.prop) != null && freeSlot(q, ch) >= 0) return K_SIT;
        if (Life.bed(q.prop) != 0 && occupant(q) == null) return K_LIE;
        if (Life.tub(q.prop) && occupant(q) == null) return K_BATHE;
        if (Life.slide(q.prop)) return K_SLIDE;
        if (Life.bouncy(q.prop) && q.state == Obj.FREE) return K_BOUNCE;
        if (Life.holdable(q) && ch.held == null && q.state != Obj.HELD) return K_PICK;
        if (Life.states(q.prop) > 0 || Life.capacity(q.prop) > 0 || q.prop.equals("tree") || q.prop.equals("palm")
            || q.prop.equals("bush") || q.prop.equals("rocket") || q.prop.equals("camera")) return K_USE;
        return K_NONE;
    }

    /** Sends a character walking to a prop (via the ladder if needed) to use it on arrival. */
    private boolean walkUse(Obj ch, Obj q) {
        int kind = useKind(ch, q);
        if (DEBUG) System.out.println("walkUse kind=" + kind + " q=" + q.prop + " chState=" + ch.state);
        if (kind == K_NONE || ch.state == Obj.HELD) return false;
        if (ch.state != Obj.FREE) standUp(ch, Avatar.F_NONE);
        float dir = ch.x < q.x ? -1 : 1;
        float ux = q.x + dir * Math.min(q.bw * q.scale * .5f + 40, 220);
        if (kind == K_SIT) {
            float[] st = Life.seat(q.prop);
            ux = q.x + st[1 + freeSlot(q, ch)] * q.scale * (q.flip ? -1 : 1);
        }
        float baseY = q.state == Obj.ON_TOP && q.link != null ? q.link.y : q.y;
        float uy = Life.floorBelow(loc, baseY - 4) + 6;
        planWalk(ch, Gfx.clamp(ux, 60, W - 60), uy);
        ch.pend = q; ch.pendKind = kind;
        ch.act = 0; ch.idleT = 8;
        return true;
    }

    /** Route to (x, y); uses the ladder when the destination is on another floor. */
    private void planWalk(Obj ch, float x, float y) {
        float[] f = Life.floors(loc);
        int from = Life.band(loc, ch.y), to = Life.band(loc, y);
        ch.nwp = 0;
        if (from != to && Life.ladder(loc) >= 0) {
            float lx = Life.ladder(loc) * W;
            ch.tx = lx; ch.ty = f[from] + 30;
            ch.wpx[0] = lx; ch.wpy[0] = f[to] + 30;
            ch.wpx[1] = x; ch.wpy[1] = y;
            ch.nwp = 2;
        } else { ch.tx = x; ch.ty = y; }
        ch.walking = true;
    }

    private void arrive(Obj ch, Obj q) {
        if (!objs.contains(q)) return;
        int kind = useKind(ch, q);
        int slot = kind == K_SIT ? freeSlot(q, ch) : 0;
        perform(ch, q, kind, slot);
    }

    private void tapObj(Obj o) {
        host.haptic();
        if (o.isChar) {
            if (o.held != null && Life.food(o.held.prop)) { eat(o); return; }
            if (o.held != null && o.held.prop.equals("guitar")) { strum(o); return; }
            if (o.held != null && o.held.prop.equals("camera")) { cameraFlash(o.x, o.y - 300 * o.scale); return; }
            if (o.state == Obj.LIE) { standUp(o, Avatar.F_HAPPY); o.emote = 6; o.emoteT = 0; host.sound(S_TICK); return; }
            o.hopV = o.state == Obj.FREE ? 560 : 0;
            o.wiggle = o.state == Obj.FREE ? 0 : .6f;
            o.emote = rnd.nextInt(7); o.emoteT = 0;
            host.sound(S_TICK);
            return;
        }
        float top = o.y - o.bh * o.scale;
        // containers give back what was put in
        if (!o.contents.isEmpty() && !(o.prop.equals("fridge") && o.pstate == 0)) {
            String id = o.contents.remove(o.contents.size() - 1);
            Obj g = Obj.prop(id, o.x, o.y - o.bh * o.scale * .6f);
            g.pop = 0; objs.add(g);
            startFall(g, (rnd.nextFloat() - .5f) * 700, -1300);
            o.wiggle = .7f;
            if (o.prop.equals("gift")) o.pstate = 1;
            host.sound(S_POP);
            return;
        }
        switch (o.prop) {
            case "gift":
                if (o.pstate == 0) {
                    o.pstate = 1; o.wiggle = 1;
                    String[] pool = {"teddy", "ball", "duck", "rocket", "car", "cupcake", "donut", "balloon", "trophy", "icecream"};
                    spawnFrom(pool[rnd.nextInt(pool.length)], o.x, o.y - 100 * o.scale, o.scale);
                    burst(o.x, o.y - 100 * o.scale, 14, 0xFFFFD43B, 2, 600);
                    react(o.x, 600, Avatar.F_WOW, 1.4f);
                    host.sound(S_SPARK);
                    return;
                }
                break;
            case "tree":
                o.wiggle = 1;
                spawnFrom("apple", o.x + (rnd.nextFloat() - .5f) * 140 * o.scale, o.y - 300 * o.scale, o.scale);
                host.sound(S_TICK);
                return;
            case "palm":
                o.wiggle = 1;
                spawnFrom("coconut", o.x + 40 * o.scale, o.y - 320 * o.scale, o.scale);
                host.sound(S_TICK);
                return;
            case "bush":
                o.wiggle = 1;
                burst(o.x, o.y - 80 * o.scale, 6, 0xFF5BD07A, 0, 300);
                if (rnd.nextInt(3) == 0) {
                    String[] pool = {"ball", "duck", "flower", "teddy", "mushroom"};
                    spawnFrom(pool[rnd.nextInt(pool.length)], o.x, o.y - 120 * o.scale, o.scale);
                    host.sound(S_SPARK);
                } else host.sound(S_TICK);
                return;
            case "balloon":
                if (o.state == Obj.FREE) {
                    burst(o.x, o.y - 220 * o.scale, 18, 0xFFFF5C73, 2, 700);
                    objs.remove(o); if (sel == o) sel = null;
                    react(o.x, 500, Avatar.F_WOW, 1);
                    host.sound(S_BOUNCE);
                    return;
                }
                break;
            case "rocket":
                if (o.launchT <= 0) { o.launchT = 3; o.wiggle = .5f; host.sound(S_WHOOSH); return; }
                break;
            case "camera":
                cameraFlash(o.x, o.y - 60 * o.scale);
                return;
            case "tub":
                burst(o.x, o.y - 130 * o.scale, 16, 0xFFFFFFFF, 3, 300);
                host.sound(S_WHOOSH);
                return;
            case "guitar":
                strum(null);
                return;
            case "shelf":
                spawnFrom("books", o.x, o.y - 200 * o.scale, o.scale);
                host.sound(S_POP);
                return;
            default: break;
        }
        int n = Life.states(o.prop);
        if (n > 0) {
            o.pstate = (o.pstate + 1) % n; o.wiggle = .7f;
            host.sound(S_TOGGLE);
            if (o.prop.equals("lamp")) {
                if (o.pstate == 1) burst(o.x, o.y - 280 * o.scale, 8, 0xFFFFE066, 2, 300);
                else react(o.x, 700, Avatar.F_SLEEPY, 1.6f);
            }
            if (o.prop.equals("tv") && o.pstate == 2) react(o.x, 900, Avatar.F_LAUGH, 1.2f);
            return;
        }
        if (Life.tossable(o) && o.state == Obj.FREE && !o.falling) {
            startFall(o, (rnd.nextFloat() - .5f) * 900, -1300);
            host.sound(S_BOUNCE);
            return;
        }
        o.hopV = 420; o.wiggle = .5f;
        host.sound(S_TICK);
    }

    private void spawnFrom(String id, float x, float y, float sc) {
        Obj g = Obj.prop(id, x, y);
        g.scale = Math.min(1.2f, Math.max(.8f, sc)); g.pop = 0;
        objs.add(g);
        startFall(g, (rnd.nextFloat() - .5f) * 600, -900);
    }

    /** Nearby characters show a feeling. */
    private void react(float x, float radius, int face, float secs) {
        for (Obj q : objs) if (q.isChar && Math.abs(q.x - x) < radius && q.state != Obj.LIE) q.setFace(face, secs);
    }

    private void strum(Obj player) {
        if (player != null) { player.strumT = 2.6f; player.emote = 2; player.emoteT = 0; }
        for (Obj q : objs)
            if (q.isChar && q != player && q.state == Obj.FREE && player != null && Math.abs(q.x - player.x) < 600) { q.act = 3; q.actT = 2.6f; }
        host.sound(S_SPARK);
    }

    private void cameraFlash(float x, float y) {
        flash = .6f;
        for (Obj q : objs) if (q.isChar && q.state != Obj.LIE) { q.setFace(rnd.nextBoolean() ? Avatar.F_WOW : Avatar.F_LAUGH, 1.5f); if (q.held == null) { q.act = 1; q.actT = 1.4f; } }
        host.sound(S_TOGGLE);
    }

    private void eat(Obj o) {
        Obj f = o.held;
        if (f == null) return;
        o.chewT = .9f;
        f.bites++;
        float dir = o.flip ? -1 : 1;
        int col = Life.drink(f.prop) ? 0xFF8FD8FF : 0xFFE9B36C;
        burst(o.x + 30 * dir * o.scale, o.y + o.dyo - 225 * o.scale, 8, col, 0, 300);
        host.sound(S_BITE);
        if (f.bites >= 3) {
            objs.remove(f); o.held = null;
            o.emote = 0; o.emoteT = 0; o.setFace(Avatar.F_LOVE, 1.4f);
            if (sel == f) sel = o;
        }
    }

    /** Tapping empty floor sends the selected character (or its vehicle) there. */
    private void tapFloor(float x, float y) {
        Obj ch = sel;
        if (ch == null || !ch.isChar || ch.state == Obj.HELD) { sel = null; return; }
        float[] f = Life.floors(loc);
        int bt = -1;
        for (int i = 0; i < f.length; i += 2) if (y >= f[i] - 20 && y <= f[i + 1]) bt = i;
        if (bt < 0) { sel = null; return; }
        float tx = Gfx.clamp(x, 60, W - 60), ty = Gfx.clamp(y, f[bt] + 8, f[bt + 1] - 4);
        if (ch.state == Obj.SIT && ch.link != null && !ch.link.isChar && Life.vehicle(ch.link.prop)) {
            Obj v = ch.link;
            if (Life.band(loc, v.y) == bt) { v.tx = tx; v.ty = ty; v.walking = true; burst(tx, ty, 1, 0xFFFFFFFF, 1, 0); host.sound(S_WHOOSH); return; }
        }
        if (ch.state != Obj.FREE) standUp(ch, Avatar.F_NONE);
        if (Life.band(loc, ch.y) != bt && Life.ladder(loc) < 0) { sel = null; return; }
        planWalk(ch, tx, ty);
        ch.pend = null; ch.act = 0; ch.idleT = 6 + rnd.nextFloat() * 6;
        burst(tx, ty, 1, 0xFFFFFFFF, 1, 0);
        host.sound(S_TICK);
    }

    // ------------------------------------------------------------------ button actions
    private void onButton(int id) {
        host.haptic();
        if (id >= B_LOC && id < B_LOC + 20) { go(SCENE, Scenes.ALL[id - B_LOC].id, 0, null); return; }
        if (id >= E_OPT) { applyOption((id - E_OPT) / 100, (id - E_OPT) % 100); return; }
        if (id >= B_CHIP && id < B_CHIP + 10) { trayCat = id - B_CHIP; trayScroll = 0; return; }
        switch (id) {
            case B_NEWCHAR: go(EDITOR, null, 0, null); break;
            case B_LEFT: spinBy(-1); break;
            case B_RIGHT: spinBy(1); break;
            case B_PLAY: go(SCENE, Scenes.ALL[mapFocus].id, 0, null); break;
            case B_HOME: go(MAP, null, 0, null); break;
            case B_CAM: flash = 1; sel = null; host.photo(); break;
            case B_CLEAR:
                if (clearConfirm > 0) {
                    clearConfirm = 0;
                    host.save("scene_" + loc, null);
                    enterScene(loc);
                    say("Room reset!");
                } else { clearConfirm = 3; say("Tap again to reset this place"); }
                break;
            case B_TAB_CHAR: trayTab = trayTab == 1 ? 0 : 1; trayScroll = 0; trayVel = 0; break;
            case B_TAB_ITEM: trayTab = trayTab == 2 ? 0 : 2; trayScroll = 0; trayVel = 0; break;
            case P_EDIT: if (sel != null && sel.isChar) go(EDITOR, null, 2, sel); break;
            case P_EMOTE: if (sel != null) { sel.emote = (sel.emote + 1 + rnd.nextInt(6)) % 7; sel.emoteT = 0; sel.hopV = 700; } break;
            case P_FLIP: if (sel != null) sel.flip = !sel.flip; break;
            case P_BIG: if (sel != null) sel.scale = Math.min(2.4f, sel.scale * 1.15f); break;
            case P_SMALL: if (sel != null) sel.scale = Math.max(.4f, sel.scale / 1.15f); break;
            case P_COPY:
                if (sel != null) { Obj o = sel.copyAt(Math.min(W - 40, sel.x + 90), Math.min(H - 10, sel.y + 20)); o.pop = 0; objs.add(o); sel = o; }
                break;
            case P_DEL: if (sel != null) { objs.remove(sel); sel = null; } break;
            case E_BACK: leaveEditor(false); break;
            case E_DONE: leaveEditor(true); break;
            case E_DICE: {
                int keepAcc = rnd.nextInt(3) == 0 ? 0 : work.acc;
                work = Look.random(rnd);
                if (keepAcc == 0 && rnd.nextInt(2) == 0) work.acc = 0;
                edHopV = 600;
                break;
            }
            case E_SAVE: lib.add(work.copy()); saveLib(); say("Saved to your characters!"); break;
            default:
                if (id >= E_TAB && id < E_TAB + 10) { edTab = id - E_TAB; edScroll = 0; }
                break;
        }
    }

    private void leaveEditor(boolean apply) {
        if (apply) {
            if (editMode == 2 && editTarget != null) { editTarget.look = work.copy(); editTarget.hopV = 600; }
            else {
                lib.add(work.copy()); saveLib();
                if (editMode == 1) pendingSpawn = work.copy();
            }
        }
        go(editMode == 0 ? MAP : SCENE, null, 0, null);
    }

    private Look pendingSpawn;

    private void applyOption(int g, int i) {
        switch (g) {
            case 0: work.skin = i % Look.SKIN.length; break;
            case 1: work.hairStyle = i % Look.N_HAIR; break;
            case 2: work.hairColor = i % Look.HAIR.length; break;
            case 3: work.eyes = i % Look.N_EYES; break;
            case 4: work.mouth = i % Look.N_MOUTH; break;
            case 5: work.top = i % Look.N_TOP; break;
            case 6: work.topColor = i % Look.CLOTH.length; break;
            case 7: work.bottom = i % Look.PANTS.length; break;
            case 8: work.acc = i % Look.N_ACC; break;
            case 9: work.accColor = i % Look.CLOTH.length; break;
            case 10: work.body = i % Look.N_BODY; break;
            case 11: work.head = i % Look.N_HEAD; break;
            case 12: work.bstyle = i % Look.N_BSTYLE; break;
            case 13: work.shoe = i % Look.SHOES.length; break;
            case 14: work.freckles = i % 2; break;
            default: break;
        }
        edHopV = 380;
    }

    // ================================================================== EDITOR
    private float cursor;

    private void drawEditor() {
        grad(c, 0, 0, W, H, 0xFFFFE8F1, 0xFFFFF6DE);
        ci(c, W * .2f, 600, 430, al(0xFFFFFFFF, 140));
        ci(c, W * .2f, 600, 320, al(0xFFFFFFFF, 160));
        for (int i = 0; i < 10; i++) ci(c, rndf(i) * W * .42f, 150 + rndf(i + 9) * 800, 8 + rndf(i + 4) * 10, al(0xFFFF8FA3, 90));
        // podium
        float px = W * .2f;
        Gfx.ol(5);
        ov(c, px, 944, 230, 52, 0xFFE9CFA3);
        ov(c, px, 930, 230, 52, 0xFFFFE9C2);
        Gfx.ol(0);
        ov(c, px, 930, 190, 40, 0xFFFFF6E2);
        // character
        float cs = 1.62f;
        ov(c, px, 930, 90, 18, al(0xFF000000, 40));
        c.save();
        c.translate(px, 930 - edHop);
        c.scale(cs, cs * (1 + (edHop > 0 ? .02f : 0)));
        Pose p = new Pose();
        p.t = t;
        p.arm = edEmoteT >= 0 && edEmoteT < .8f ? 1 : 0;
        p.blink = edBlink > 0 ? (float) Math.sin(Math.PI * (1 - edBlink / .14f)) : 0;
        p.mood = edMood;
        Gfx.ol(4.2f);
        Gfx.shade = true;
        Avatar.draw(c, work, p);
        Gfx.ol(0);
        Gfx.shade = false;
        c.restore();
        if (edEmoteT >= 0) Icons.emote(c, edEmote, px, 930 - 380 * cs - 50, 1.1f * Gfx.back(edEmoteT * 4), t);

        // panel
        float x0 = W * .44f, x1 = W - 40, y0 = 230, y1 = H - 40;
        rr(c, x0, y0 + 8, x1 - x0, y1 - y0, 40, al(0xFF000000, 30));
        Gfx.ol(5);
        rr(c, x0, y0, x1 - x0, y1 - y0, 40, 0xFFFFFFFF);
        Gfx.ol(0);
        int[] tabIcons = {Icons.FACE, Icons.HAIR, Icons.SMILE, Icons.SHIRT, Icons.STAR};
        int[] tabCols = {0xFFFFA85C, 0xFF9B6CDC, 0xFF3CC5AF, 0xFF4FB3FF, 0xFFFF6F8F};
        float tw0 = (x1 - x0 - 130) / 5;
        for (int i = 0; i < 5; i++) {
            float cx = x0 + tw0 * (i + .5f);
            if (i == edTab) ci(c, cx, 160, 66, al(tabCols[i], 90));
            btn(E_TAB + i, cx, 160, i == edTab ? 52 : 44, tabCols[i], tabIcons[i]);
        }

        c.save();
        c.clipRect(x0 + 6, y0 + 8, x1 - 6, y1 - 8);
        cursor = y0 + 30 - edScroll;
        float start = cursor;
        float cw = x1 - x0 - 60;
        float left = x0 + 30;
        switch (edTab) {
            case 0:
                label("Body", left);
                cards(10, Look.N_BODY, work.body, left, cw);
                label("Head shape", left);
                cards(11, Look.N_HEAD, work.head, left, cw);
                label("Skin colour", left);
                swatches(0, Look.SKIN, work.skin, left, cw);
                label("Freckles", left);
                cards(14, 2, work.freckles, left, cw);
                break;
            case 1:
                label("Hairstyle", left);
                cards(1, Look.N_HAIR, work.hairStyle, left, cw);
                label("Hair colour", left);
                swatches(2, Look.HAIR, work.hairColor, left, cw);
                break;
            case 2:
                label("Eyes", left);
                cards(3, Look.N_EYES, work.eyes, left, cw);
                label("Mouth", left);
                cards(4, Look.N_MOUTH, work.mouth, left, cw);
                break;
            case 3:
                label("Top", left);
                cards(5, Look.N_TOP, work.top, left, cw);
                label("Top colour", left);
                swatches(6, Look.CLOTH, work.topColor, left, cw);
                label("Bottoms", left);
                cards(12, Look.N_BSTYLE, work.bstyle, left, cw);
                label("Bottoms colour", left);
                swatches(7, Look.PANTS, work.bottom, left, cw);
                label("Shoes", left);
                swatches(13, Look.SHOES, work.shoe, left, cw);
                break;
            default:
                label("Hats & accessories", left);
                cards(8, Look.N_ACC, work.acc, left, cw);
                label("Accessory colour", left);
                swatches(9, Look.CLOTH, work.accColor, left, cw);
                break;
        }
        edMax = Math.max(0, cursor - start - (y1 - y0 - 120));
        if (edScroll > edMax) edScroll = edMax;
        c.restore();

        btn(E_BACK, 96, 92, 52, 0xFFFF8F5C, Icons.BACK);
        btn(E_DONE, W - 96, 90, 58, 0xFF3CC57B, Icons.CHECK);
        btn(E_DICE, px - 150, H - 80, 54, 0xFFFFB02E, Icons.DICE);
        btn(E_SAVE, px + 150, H - 80, 54, 0xFFFF6F8F, Icons.SAVE);
        text(c, editMode == 2 ? "Edit character" : "New character", W * .2f, 110, 48, 0xFFC24E6E, Paint.Align.CENTER);
    }

    private boolean visibleY(float y, float h) { return y + h > 232 && y < H - 42; }

    private void label(String s, float x) {
        text(c, s, x, cursor + 34, 36, 0xFF6B6480, Paint.Align.LEFT);
        cursor += 58;
    }

    private void swatches(int g, int[] cols, int sel, float x0, float cw) {
        float step = 92;
        int per = Math.max(1, (int) (cw / step));
        for (int i = 0; i < cols.length; i++) {
            float cx = x0 + 44 + (i % per) * step, cy = cursor + 44 + (i / per) * step;
            if (visibleY(cy - 44, 88)) {
                                Gfx.ol(4);
                ci(c, cx, cy, 38, cols[i]);
                Gfx.ol(0);
                if (cols[i] == 0xFFFFFFFF) cis(c, cx, cy, 37, 0xFFE0E0EC, 3);
                if (i == sel) { cis(c, cx, cy, 46, 0xFF6C7BFF, 7); }
                float s = 1;
                reg(E_OPT + g * 100 + i, cx, cy, 80, 80);
            }
        }
        cursor += ((cols.length + per - 1) / per) * step + 24;
    }

    private void cards(int g, int n, int sel, float x0, float cw) {
        float size = 138, gap = 18;
        int per = Math.max(1, (int) ((cw + gap) / (size + gap)));
        for (int i = 0; i < n; i++) {
            float x = x0 + (i % per) * (size + gap), y = cursor + (i / per) * (size + gap);
            if (!visibleY(y, size)) continue;
            boolean on = i == sel;
            float s = isDown(E_OPT + g * 100 + i) ? .94f : 1;
            c.save();
            c.translate(x + size / 2, y + size / 2);
            c.scale(s, s);
            Gfx.ol(4);
            rr(c, -size / 2, -size / 2, size, size, 28, on ? 0xFFE5E8FF : 0xFFF5F2FA);
            Gfx.ol(0);
            c.save();
            c.clipRect(-size / 2 + 4, -size / 2 + 4, size / 2 - 4, size / 2 - 4);
            Look l = work.copy();
            float cy, k;
            Pose tp = IDLE;
            switch (g) {
                case 1: l.hairStyle = i; l.acc = 0; cy = -290; k = .5f; break;
                case 3: l.eyes = i; cy = -262; k = 1.08f; break;
                case 4: l.mouth = i; cy = -230; k = 1.3f; break;
                case 5: l.top = i; cy = -128; k = .6f; break;
                case 10: l.body = i; cy = -Avatar.height(l) / 2 - 8; k = 118 / (Avatar.height(l) + 20); break;
                case 11: l.head = i; l.acc = 0; cy = -270; k = .56f; break;
                case 12: l.bstyle = i; cy = -80; k = .66f; break;
                case 14: l.freckles = i; cy = -244; k = 1.3f; break;
                default: l.acc = i; cy = -300; k = .5f; break;
            }
            if (g == 1 || g == 11 || g == 3 || g == 4 || g == 14 || g == 8) cy = cy - (Avatar.neckY(l) + 196) * 0;
            c.scale(k, k);
            // head-based crops follow the body's neck position
            if (g != 5 && g != 10 && g != 12) cy += (Avatar.neckY(l) + 196) * 1 - 0 + (1 - Avatar.BODY[l.body % 6][3]) * 0;
            c.translate(0, -cy);
            Gfx.ol(5f / k);
            Avatar.draw(c, l, tp);
            Gfx.ol(0);
            c.restore();
            if (on) rrs(c, -size / 2, -size / 2, size, size, 28, 0xFF6C7BFF, 6);
            c.restore();
            reg(E_OPT + g * 100 + i, x + size / 2, y + size / 2, size, size);
        }
        cursor += ((n + per - 1) / per) * (size + gap) + 20;
    }

    // ---- preview/debug helpers
    void debugEditor(int tab) { edTab = tab; work = Look.PRESETS[0].copy(); }
    int debugMovedCount(float ox, float oy) { int n = 0; for (Obj q : objs) if (q.lift > 0 || (Math.abs(q.x - ox) > 5 && Math.abs(q.x - ox) < 60)) n++; return n; }
    void debugLife() {
        objs.clear();
        Obj sofa = Obj.prop("sofa", W * .2f, 965); objs.add(sofa);
        Obj bed = Obj.prop("bed", W * .14f, 530); objs.add(bed);
        Obj lamp = Obj.prop("lamp", W * .33f, 535); lamp.pstate = 1; objs.add(lamp);
        Obj tv = Obj.prop("tv", W * .4f, 962); tv.pstate = 2; objs.add(tv);
        Obj fr = Obj.prop("fridge", W * .63f, 965); fr.pstate = 1; objs.add(fr);
        Obj st = Obj.prop("stove", W * .75f, 962); st.pstate = 1; objs.add(st);
        Obj tb = Obj.prop("table", W * .88f, 970); tb.scale = .85f; objs.add(tb);
        Obj cake = Obj.prop("cake", 0, 0); cake.state = Obj.ON_TOP; cake.link = tb; cake.offX = -40; objs.add(cake);
        Obj gift = Obj.prop("gift", W * .84f, 545); gift.pstate = 1; objs.add(gift);
        Obj a = Obj.character(Look.PRESETS[1].copy(), 0, 0); a.state = Obj.SIT; a.link = sofa; a.slot = 0; objs.add(a);
        Obj b = Obj.character(Look.PRESETS[0].copy(), 0, 0); b.state = Obj.LIE; b.link = bed; b.slot = 1; objs.add(b);
        Obj d = Obj.character(Look.PRESETS[2].copy(), W * .48f, 540); d.act = 3; d.actT = 99; objs.add(d);
        Obj e = Obj.character(Look.PRESETS[9].copy(), W * .66f, 545); objs.add(e);
        Obj ic = Obj.prop("icecream", 0, 0); ic.state = Obj.HELD; ic.link = e; e.held = ic; objs.add(ic);
        Obj f = Obj.character(Look.PRESETS[3].copy(), W * .5f, 985); f.walking = true; f.tx = W * .9f; f.ty = 985; objs.add(f);
        Obj s2 = Obj.character(Look.PRESETS[4].copy(), 0, 0); s2.state = Obj.SIT; s2.link = sofa; s2.slot = 1; s2.act = 1; s2.actT = 99; objs.add(s2);
        Obj tub = Obj.prop("tub", W * .68f, 545); tub.contents.add("duck"); objs.add(tub);
        Obj bather = Obj.character(Look.PRESETS[8].copy(), 0, 0); bather.state = Obj.BATHE; bather.link = tub; objs.add(bather);
        Obj dad = Obj.character(Look.PRESETS[6].copy(), W * .5f, 985); objs.add(dad);
        Obj tod = Obj.character(Look.PRESETS[3].copy(), 0, 0); tod.state = Obj.SIT; tod.link = dad; objs.add(tod);
        objs.remove(f); objs.remove(e); objs.remove(ic); objs.remove(gift);
        Obj e2 = Obj.character(Look.PRESETS[9].copy(), W * .88f, 545); objs.add(e2);
        Obj ted = Obj.prop("teddy", 0, 0); ted.state = Obj.HELD; ted.link = e2; e2.held = ted; objs.add(ted);
        for (Obj o : objs) { o.pop = 1; o.idleT = 99; }
        a.setFace(Avatar.F_LAUGH, 99); b.setFace(Avatar.F_SLEEPY, 99); d.setFace(Avatar.F_HAPPY, 99);
    }

    Obj debugSel() { return sel; }

    void debugDrag() {
        objs.clear();
        Obj sofa = Obj.prop("sofa", W * .3f, 965); objs.add(sofa);
        Obj kid = Obj.character(Look.PRESETS[1].copy(), W * .32f, 830); objs.add(kid);
        Obj tub = Obj.prop("tub", W * .7f, 545); objs.add(tub);
        Obj kid2 = Obj.character(Look.PRESETS[7].copy(), W * .55f, 985); objs.add(kid2);
        Obj cake = Obj.prop("cake", W * .66f, 760); objs.add(cake);
        for (Obj o : objs) { o.pop = 1; o.idleT = 99; }
        dragObj = kid; moved = true; down = true; mode = M_DRAG; kid.lift = 1;
        resolve(kid, kid.x, kid.y - 150);
    }

    int debugFocus() { return mapFocus; }
    void debugEditorAll(Canvas cv) {
        int keep = screen;
        screen = EDITOR;
        for (int tab = 0; tab < 5; tab++) { edTab = tab; for (int i = 0; i < 2; i++) { update(.03f); draw(cv); } }
        for (int g = 0; g < 15; g++) for (int i = 0; i < 13; i++) applyOption(g, i);
        draw(cv);
        screen = keep;
    }

    void debugTray(int tab) { trayTab = tab; trayA = 1; trayCat = 0; }

    // ================================================================== called after the editor transition finishes
    void afterEditorReturn() {
        if (pendingSpawn != null && screen == SCENE) {
            Obj o = Obj.character(pendingSpawn, W * .5f, 560);
            o.pop = 0; objs.add(o); sel = o;
            pendingSpawn = null;
        }
    }
}
