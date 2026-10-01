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
            sb.append(o.encode(W, (li < 0 ? 0 : o.state) + "," + li + "," + o.slot + "," + o.pstate + "," + o.bites));
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

        if (dragging) {
            o.walking = false; o.falling = false;
            if (o.isChar) updateChar(o, dt, true);
            return;
        }

        // things attached to other things follow them
        if (o.state != Obj.FREE && (o.link == null || !objs.contains(o.link))) {
            if (o.state == Obj.HELD && o.link != null) o.link.held = null;
            o.state = Obj.FREE; o.link = null;
            startFall(o, 0, 0);
        }
        Obj l = o.link;
        switch (o.state) {
            case Obj.SIT: {
                float[] st = Life.seat(l.prop);
                if (st == null || o.slot + 1 >= st.length) { o.state = Obj.FREE; break; }
                float dir = l.flip ? -1 : 1;
                o.x = l.x + st[1 + o.slot] * l.scale * dir;
                if (l.prop.equals("swing")) o.x += (float) Math.sin((t + l.phase) * 2) * 10 * l.scale;
                o.y = l.y + st[0] * l.scale + 98 * o.scale - 4 - l.hop;
                break;
            }
            case Obj.LIE:
                o.x = l.x + 158 * l.scale * o.slot;
                o.y = l.y + (Life.bed(l.prop) + 34) * l.scale - l.hop;
                break;
            case Obj.ON_TOP:
                o.x = l.x + o.offX * l.scale;
                o.y = l.y + Life.surface(l.prop) * l.scale - l.lift * 36 * l.scale - l.hop;
                break;
            case Obj.HELD: {
                Pose hp = l.pose;
                float dir = l.flip ? -1 : 1;
                float hs = holdScale(o);
                o.x = l.x + Avatar.handX(hp) * l.scale * dir;
                o.y = l.y + l.dyo + Avatar.handY(hp) * l.scale + o.bh * hs * .45f;
                break;
            }
            default: break;
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
        p.arm = Math.max(Math.max(o.lift, em), o.falling ? 1 : 0);
        p.swing = Math.max(o.lift, o.falling ? .6f : 0) * (float) Math.sin(t * 16 + o.phase) * .8f;
        p.blink = o.blinkT > 0 ? (float) Math.sin(Math.PI * (1 - o.blinkT / .14f)) : 0;
        p.mood = o.emoteT >= 0 ? new int[]{1, 5, 0, 3, 2, 2, 1}[o.emote % 7] : (o.falling || dragging ? 3 : -1);
        p.sit = o.state == Obj.SIT;
        p.sleep = o.state == Obj.LIE;
        p.hold = o.held != null;
        p.chew = o.chewT > 0;
        if (o.actT > 0) { o.actT -= dt; if (o.actT <= 0) o.act = 0; }
        float k = Math.min(1, dt * 8);
        p.wave += ((o.act == 1 ? 1 : 0) - p.wave) * k;
        p.look += ((o.act == 2 ? (float) Math.sin(t * 2.4f + o.phase) : 0) - p.look) * k;
        p.dance += ((o.act == 3 ? 1 : 0) - p.dance) * k;

        if (o.walking && o.state == Obj.FREE && !dragging) {
            float dx = o.tx - o.x, dy = o.ty - o.y;
            float d = (float) Math.sqrt(dx * dx + dy * dy);
            if (d < 6) o.walking = false;
            else {
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
            if (o.idleT <= 0) { o.idleT = 4 + rnd.nextFloat() * 8; pickIdle(o); }
            if (o.state == Obj.LIE && o.emoteT < 0 && rnd.nextFloat() < dt * .3f) { o.emote = 5; o.emoteT = 0; }
        }
    }

    /** Characters do little things on their own so the world feels alive. */
    private void pickIdle(Obj o) {
        int r = rnd.nextInt(10);
        if (o.state == Obj.LIE) return;
        if (o.state == Obj.SIT) {
            if (r < 4) { o.act = 2; o.actT = 2.5f; }
            else if (r < 7) { o.act = 1; o.actT = 1.8f; }
            else { o.emote = rnd.nextInt(7); o.emoteT = 0; }
            return;
        }
        if (o.state != Obj.FREE) return;
        if (r < 4) {
            float[] f = Life.floors(loc);
            int b = Life.band(loc, o.y);
            float dist = (120 + rnd.nextFloat() * 260) * o.scale * (rnd.nextBoolean() ? 1 : -1);
            o.tx = Gfx.clamp(o.x + dist, 70, W - 70);
            o.ty = Gfx.clamp(o.y + (rnd.nextFloat() - .5f) * 80, f[b] + 10, f[b + 1] - 6);
            o.walking = true;
        } else if (r < 6) { o.act = 1; o.actT = 1.8f; }
        else if (r < 8) { o.act = 2; o.actT = 2.6f; }
        else if (r < 9) { o.act = 3; o.actT = 2.6f; }
        else { o.hopV = 560; o.emote = rnd.nextInt(7); o.emoteT = 0; }
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
            plife[parts] = type == 1 ? .7f : .8f + rnd.nextFloat() * .5f;
            psz[parts] = type == 1 ? 30 : 6 + rnd.nextFloat() * 8;
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
    }

    private void reg(int id, float cx, float cy, float w, float h) {
        if (nb >= bid.length) return;
        bid[nb] = id; bx[nb] = cx; by[nb] = cy; bw[nb] = w; bh[nb] = h; nb++;
    }

    private int hitBtn(float x, float y) {
        for (int i = nb - 1; i >= 0; i--)
            if (Math.abs(x - bx[i]) <= bw[i] / 2 && Math.abs(y - by[i]) <= bh[i] / 2) return bid[i];
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
        order.clear();
        order.addAll(objs);
        Collections.sort(order, byDepth);
        if (dragObj != null && order.remove(dragObj)) order.add(dragObj);
        for (Obj o : order) drawObj(o);
        drawParts();
        if (!ui) return;

        if (sel != null && objs.contains(sel) && !(down && mode == M_DRAG && moved)) drawPopup();
        else if (sel != null && !objs.contains(sel)) sel = null;

        btn(B_HOME, 100, 92, 52, 0xFFFF8F5C, Icons.HOME);
        btn(B_CAM, W - 100, 92, 52, 0xFF4FB3FF, Icons.CAMERA);
        btn(B_CLEAR, W - 226, 92, 52, 0xFFFFB02E, Icons.BROOM);
        drawTray();
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
        float hx = holder.x + Avatar.handX(holder.pose) * holder.scale * dir, hy = holder.y + holder.dyo + Avatar.handY(holder.pose) * holder.scale;
        Gfx.ol(6.5f * holder.scale);
        ci(c, hx, hy, 17 * holder.scale, Look.SKIN[holder.look.skin]);
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
                if (dragObj.held != null) dragObj.dyo = -dragObj.lift * 36 * dragObj.scale;
            }
        } else if (mode == M_TRAY) {
            if (!trayDecided && far) {
                trayDecided = true;
                if (trayCard >= 0 && dy < -20 && Math.abs(dy) > Math.abs(dx) * .8f) {
                    Obj o = spawnCard(trayCard, x, y);
                    if (o != null) {
                        host.sound(S_POP);
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
                sel = o;
                if (!moved) tapObj(o);
                else drop(o);
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
    private void pickUp(Obj o) {
        host.sound(S_POP);
        o.walking = false; o.falling = false; o.act = 0;
        if (o.state == Obj.HELD && o.link != null) o.link.held = null;
        o.state = Obj.FREE; o.link = null;
    }

    private void drop(Obj o) {
        if (o.isChar) {
            if (trySeat(o) || tryBed(o)) { o.sqv = -2.4f; host.sound(S_DROP); return; }
            startFall(o, 0, 0);
        } else {
            if (tryGive(o)) return;
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

    private boolean trySeat(Obj o) {
        Obj best = null; int bestSlot = 0; float bestD = 1e9f;
        for (Obj s : objs) {
            if (s.isChar) continue;
            float[] st = Life.seat(s.prop);
            if (st == null) continue;
            float seatY = s.y + st[0] * s.scale;
            if (o.y < seatY - 150 || o.y > s.y + 50) continue;
            for (int j = 0; j + 1 < st.length; j++) {
                float sx = s.x + st[1 + j] * s.scale * (s.flip ? -1 : 1);
                float d = Math.abs(o.x - sx);
                if (d < 85 * s.scale && d < bestD && !seatTaken(s, j)) { best = s; bestSlot = j; bestD = d; }
            }
        }
        if (best == null) return false;
        o.state = Obj.SIT; o.link = best; o.slot = bestSlot; o.walking = false; o.flip = false;
        return true;
    }

    private boolean tryBed(Obj o) {
        for (Obj b : objs) {
            float m = b.isChar ? 0 : Life.bed(b.prop);
            if (m == 0) continue;
            if (Math.abs(o.x - b.x) > b.bw * b.scale * .5f || o.y < b.y + m * b.scale - 200 || o.y > b.y + 60) continue;
            boolean taken = false;
            for (Obj q : objs) if (q.link == b && q.state == Obj.LIE) taken = true;
            if (taken) continue;
            o.state = Obj.LIE; o.link = b; o.slot = b.flip ? -1 : 1; o.walking = false;
            o.emote = 5; o.emoteT = 0;
            return true;
        }
        return false;
    }

    private boolean tryGive(Obj item) {
        if (!Life.holdable(item)) return false;
        float cx = item.x, cy = item.y - item.bh * item.scale * .5f;
        for (int i = order.size() - 1; i >= 0; i--) {
            Obj ch = order.get(i);
            if (!ch.isChar || ch.held != null || ch.state == Obj.LIE || !ch.hit(cx, cy)) continue;
            ch.held = item; item.state = Obj.HELD; item.link = ch; item.falling = false;
            ch.hopV = 380; ch.emote = 1; ch.emoteT = 0;
            host.sound(S_SPARK);
            return true;
        }
        return false;
    }

    private void tapObj(Obj o) {
        host.haptic();
        if (o.isChar) {
            if (o.held != null && Life.food(o.held.prop)) { eat(o); return; }
            o.hopV = o.state == Obj.FREE ? 560 : 0;
            o.wiggle = o.state == Obj.FREE ? 0 : .6f;
            o.emote = rnd.nextInt(7); o.emoteT = 0;
            host.sound(S_TICK);
            return;
        }
        int n = Life.states(o.prop);
        if (o.prop.equals("gift") && o.pstate == 0) {
            o.pstate = 1; o.wiggle = 1;
            String[] pool = {"teddy", "ball", "duck", "rocket", "car", "cupcake", "donut", "balloon", "trophy", "icecream"};
            Obj g = Obj.prop(pool[rnd.nextInt(pool.length)], o.x, o.y - 100 * o.scale);
            g.scale = o.scale; g.pop = 0;
            objs.add(g);
            startFall(g, (rnd.nextFloat() - .5f) * 700, -1500);
            burst(o.x, o.y - 100 * o.scale, 14, 0xFFFFD43B, 2, 600);
            host.sound(S_SPARK);
            return;
        }
        if (n > 0) {
            o.pstate = (o.pstate + 1) % n; o.wiggle = .7f;
            host.sound(S_TOGGLE);
            if (o.prop.equals("lamp") && o.pstate == 1) burst(o.x, o.y - 280 * o.scale, 8, 0xFFFFE066, 2, 300);
            return;
        }
        if (o.prop.equals("camera")) { flash = .6f; host.sound(S_TOGGLE); return; }
        if (Life.tossable(o) && o.state == Obj.FREE && !o.falling) {
            startFall(o, (rnd.nextFloat() - .5f) * 900, -1300);
            host.sound(S_BOUNCE);
            return;
        }
        o.hopV = 420; o.wiggle = .5f;
        host.sound(S_TICK);
    }

    private void eat(Obj o) {
        Obj f = o.held;
        o.chewT = .9f;
        f.bites++;
        float dir = o.flip ? -1 : 1;
        int col = f.prop.equals("juice") || f.prop.equals("coffee") ? 0xFF8FD8FF : 0xFFE9B36C;
        burst(o.x + 30 * dir * o.scale, o.y + o.dyo - 225 * o.scale, 8, col, 0, 300);
        host.sound(S_BITE);
        if (f.bites >= 3) {
            objs.remove(f); o.held = null;
            o.emote = 0; o.emoteT = 0;
            if (sel == f) sel = o;
        }
    }

    /** Tapping empty floor sends the selected character walking there. */
    private void tapFloor(float x, float y) {
        Obj ch = sel;
        if (ch == null || !ch.isChar || ch.state == Obj.HELD) { sel = null; return; }
        float[] f = Life.floors(loc);
        int bt = -1;
        for (int i = 0; i < f.length; i += 2) if (y >= f[i] - 20 && y <= f[i + 1]) bt = i;
        if (bt < 0) { sel = null; return; }
        if (ch.state == Obj.SIT || ch.state == Obj.LIE) {
            Obj l = ch.link;
            ch.state = Obj.FREE; ch.link = null;
            ch.y = Life.floorBelow(loc, l.y - 4);
        }
        if (Life.band(loc, ch.y) != bt) { sel = null; return; }
        ch.tx = Gfx.clamp(x, 60, W - 60);
        ch.ty = Gfx.clamp(y, f[bt] + 8, f[bt + 1] - 4);
        ch.walking = true; ch.act = 0; ch.idleT = 6 + rnd.nextFloat() * 6;
        burst(ch.tx, ch.ty, 1, 0xFFFFFFFF, 1, 0);
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
            case 0: work.skin = i; break;
            case 1: work.hairStyle = i; break;
            case 2: work.hairColor = i; break;
            case 3: work.eyes = i; break;
            case 4: work.mouth = i; break;
            case 5: work.top = i; break;
            case 6: work.topColor = i; break;
            case 7: work.bottom = i; break;
            case 8: work.acc = i; break;
            case 9: work.accColor = i; break;
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
                label("Skin colour", left);
                swatches(0, Look.SKIN, work.skin, left, cw);
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
                label("Trousers", left);
                swatches(7, Look.PANTS, work.bottom, left, cw);
                break;
            default:
                label("Accessory", left);
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
            switch (g) {
                case 1: l.hairStyle = i; cy = -272; k = .66f; break;
                case 3: l.eyes = i; cy = -262; k = 1.08f; break;
                case 4: l.mouth = i; cy = -230; k = 1.3f; break;
                case 5: l.top = i; cy = -128; k = .6f; break;
                default: l.acc = i; cy = -290; k = .62f; break;
            }
            c.scale(k, k);
            c.translate(0, -cy);
            Gfx.ol(5f / k);
            Avatar.draw(c, l, IDLE);
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
        for (Obj o : objs) { o.pop = 1; o.idleT = 99; }
    }

    int debugFocus() { return mapFocus; }
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
