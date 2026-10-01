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
    }

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
    private static final int M_NONE = 0, M_DRAG = 1, M_TRAY = 2, M_PANEL = 3, M_PREVIEW = 4;
    private int mode;
    private Obj dragObj;
    private float dragDX, dragDY;

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
            float ka = (a.isChar ? 0 : (PropArt.flat(a.prop) ? -100000 : 0)) + a.y;
            float kb = (b.isChar ? 0 : (PropArt.flat(b.prop) ? -100000 : 0)) + b.y;
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
        }
        sel = null;
    }

    private void saveScene() {
        StringBuilder sb = new StringBuilder();
        for (Obj o : objs) { if (sb.length() > 0) sb.append('|'); sb.append(o.encode(W)); }
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
            for (Obj o : objs) updateObj(o, dt, o == dragObj && moved);
        }
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
    }

    // ================================================================== draw
    void draw(Canvas canvas) {
        c = canvas;
        nb = 0;
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
        drawScene(false);
        c.restore();
    }

    // ------------------------------------------------------------------ buttons
    private boolean isDown(int id) { return down && pressedId == id && !scrolling; }

    private void btn(int id, float cx, float cy, float r, int col, int icon) {
        float s = isDown(id) ? .9f : 1;
        ci(c, cx, cy + 7 * s, r * s, dk(col, .28f));
        ci(c, cx, cy, r * s, col);
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

    // ================================================================== MAP
    private float tileX(int i) { float sp = Math.min(W * .21f, 340); return W / 2 + (i % 4 - 1.5f) * sp; }
    private float tileY(int i) { return 480 + (i / 4) * 365; }

    private void drawMap() {
        grad(c, 0, 0, W, H, 0xFF86E0F2, 0xFF4EBFE6);
        for (int r = 0; r < 12; r++) {
            float off = (t * 24 + r * 131) % 260;
            for (float x = -260 + off; x < W + 100; x += 260) arc(c, x, 90 + r * 90, 56, 12, 200, 140, 6, al(0xFFFFFFFF, 90));
        }
        float sp = Math.min(W * .21f, 340), iw = sp * 4 + 150;
        ov(c, W / 2, 690, iw / 2 + 40, 470, 0xFFF9E7B0);
        ov(c, W / 2, 676, iw / 2, 450, 0xFF98DD84);
        ov(c, W / 2, 660, iw / 2 - 36, 424, 0xFFABE996);
        // decorations
        for (int i = 0; i < 2; i++) {
            float tx = W / 2 + (i == 0 ? -1 : 1) * (iw / 2 - 30);
            c.save(); c.translate(tx, 330); c.scale(.42f, .42f); PropArt.draw(c, "tree", t); c.restore();
            c.save(); c.translate(tx * .96f + (i == 0 ? 20 : -20), 1010); c.scale(.5f, .5f); PropArt.draw(c, "bush", t); c.restore();
        }
        for (int i = 0; i < 16; i++) {
            float fx = W / 2 + (rndf(i) - .5f) * iw * .9f, fy = 300 + rndf(i + 30) * 700;
            ci(c, fx, fy, 6, new int[]{0xFFFFFFFF, 0xFFFFE066, 0xFFFF8FC0}[i % 3]);
        }
        Scenes.clouds(c, W, t, 40, 4, al(0xFFFFFFFF, 235));

        // title
        String title = "CocoPopo";
        int[] cols = {0xFFFF5C8A, 0xFFFF9A3D, 0xFFFFC93C, 0xFF3CC5AF, 0xFF4FB3FF, 0xFF8E7BFF, 0xFFFF5C8A, 0xFFFF9A3D};
        float total = tw(title, 124) + 8 * 6;
        float cx = W / 2 - total / 2;
        for (int i = 0; i < title.length(); i++) {
            String ch = title.substring(i, i + 1);
            float cw = tw(ch, 124);
            float yy = 176 + (float) Math.sin(t * 3 + i * .7f) * 7;
            textOut(c, ch, cx + cw / 2 + 5, yy + 10, 124, al(0xFF2B2735, 60), al(0xFF2B2735, 60), 16);
            textOut(c, ch, cx + cw / 2, yy, 124, cols[i % cols.length], 0xFFFFFFFF, 16);
            cx += cw + 6;
        }
        text(c, "Tap a place to play!", W / 2, 260, 40, 0xFF1F6F94, Paint.Align.CENTER);

        for (int i = 0; i < Scenes.ALL.length; i++) {
            Scenes.Loc lc = Scenes.ALL[i];
            float x = tileX(i), y = tileY(i) + (float) Math.sin(t * 2 + i) * 5;
            float s = isDown(B_LOC + i) ? .94f : 1;
            c.save(); c.translate(x, y); c.scale(s, s);
            rr(c, -125, -108, 250, 250, 46, dk(lc.color, .32f));
            rr(c, -125, -122, 250, 250, 46, lc.color);
            rr(c, -112, -109, 224, 224, 36, lt(lc.color, .45f));
            Scenes.drawIcon(c, lc.id, 0, -8, 1.05f, t);
            float lw = tw(lc.name, 34) + 50;
            rr(c, -lw / 2, 142, lw, 56, 28, 0xFFFFFFFF);
            text(c, lc.name, 0, 181, 34, dk(lc.color, .5f), Paint.Align.CENTER);
            c.restore();
            reg(B_LOC + i, x, y + 40, 250, 340);
        }
        btn(B_NEWCHAR, 100, 92, 56, 0xFFFF6F8F, Icons.PERSON_PLUS);
        textOut(c, "Dress-up", 100, 186, 30, 0xFFFFFFFF, 0xFFE0567A, 8);
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
        if (!ui) return;

        if (sel != null && objs.contains(sel) && !(down && mode == M_DRAG && moved)) drawPopup();
        else if (sel != null && !objs.contains(sel)) sel = null;

        btn(B_HOME, 100, 92, 52, 0xFFFF8F5C, Icons.HOME);
        btn(B_CAM, W - 100, 92, 52, 0xFF4FB3FF, Icons.CAMERA);
        btn(B_CLEAR, W - 226, 92, 52, 0xFFFFB02E, Icons.BROOM);
        drawTray();
    }

    private void drawObj(Obj o) {
        float sc = o.scale * Gfx.back(o.pop);
        boolean shadow = !(o.prop != null && (o.prop.equals("rug") || o.prop.equals("frame") || o.prop.equals("clock")));
        if (shadow && sc > 0) {
            float k = 1 - o.lift * .22f;
            ov(c, o.x, o.y, o.bw * .36f * sc * k, 15 * sc * k, al(0xFF000000, (int) (46 - o.lift * 14)));
        }
        float yoff = -o.lift * 36 * o.scale - o.hop;
        if (o.prop != null && o.prop.equals("balloon")) yoff -= 8 + (float) Math.sin(t * 1.6f + o.phase) * 8;
        c.save();
        c.translate(o.x, o.y + yoff);
        c.rotate(o.tilt);
        float sq = o.sq;
        c.scale(sc * (1 - sq * .5f) * (o.flip ? -1 : 1) * (1 + o.lift * .05f), sc * (1 + sq));
        if (o.isChar) {
            Pose p = new Pose();
            p.t = t + o.phase;
            float em = o.emoteT >= 0 && o.emoteT < .8f ? 1 : 0;
            p.arm = Math.max(o.lift, em);
            p.swing = o.lift * (float) Math.sin(t * 16 + o.phase) * .8f;
            p.blink = o.blinkT > 0 ? (float) Math.sin(Math.PI * (1 - o.blinkT / .14f)) : 0;
            if (o.emoteT >= 0) p.mood = new int[]{1, 5, 0, 3, 2, 2, 1}[o.emote % 7];
            Avatar.draw(c, o.look, p);
        } else {
            PropArt.draw(c, o.prop, t + o.phase);
        }
        c.restore();
        if (o.isChar && o.emoteT >= 0) {
            float a = Gfx.back(o.emoteT * 4);
            float fo = o.emoteT > 1.6f ? Math.max(0, (2 - o.emoteT) / .4f) : 1;
            Icons.emote(c, o.emote, o.x, o.y + yoff - o.bh * o.scale - 50 - (float) Math.sin(t * 5) * 4, o.scale * .9f * a * fo, t);
        }
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
        rr(c, x0, top, pw, ph, 54, 0xFFFFFFFF);
        float tipX = Gfx.clamp(sel.x, x0 + 40, x0 + pw - 40);
        poly(c, 0xFFFFFFFF, tipX - 16, top + ph - 4, tipX + 16, top + ph - 4, tipX, top + ph + 16);
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
            rr(c, -40, top, W + 80, TRAY_H + 60, 48, 0xFFFFF7E6);
            rr(c, -40, top, W + 80, 18, 8, 0xFFFFD98A);
            float chipY = top + 52;
            if (trayTab == 2) {
                float x = CARD_X0;
                for (int i = 0; i < PropArt.CATS.length; i++) {
                    boolean on = i == trayCat;
                    float w = tw(PropArt.CATS[i], 32) + 52;
                    float s = isDown(B_CHIP + i) ? .94f : 1;
                    rr(c, x, chipY - 28 * s, w, 56 * s, 28, on ? 0xFF6C7BFF : 0xFFFFFFFF);
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
        rr(c, -CARD / 2, -CARD / 2, CARD, CARD, 30, 0xFFFFFFFF);
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
                Avatar.draw(c, l, IDLE);
                c.restore();
                String nm = i - 1 < lib.size() ? "Mine " + (i) : Look.PRESET_NAMES[i - 1 - lib.size()];
                text(c, nm, 0, 82, 28, 0xFF6B6480, Paint.Align.CENTER);
            }
        } else {
            PropArt.Def d = itemAt(i);
            if (d != null) {
                float k = Math.min(150 / d.w, 124 / d.h);
                c.save(); c.translate(0, 52); c.scale(k, k); PropArt.draw(c, d.id, t); c.restore();
                text(c, d.name, 0, 84, 26, 0xFF6B6480, Paint.Align.CENTER);
            }
        }
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
        } else if (screen == EDITOR) {
            if (x > W * .44f) mode = M_PANEL;
            else if (Math.abs(x - W * .2f) < 190 && y > 250 && y < 960) mode = M_PREVIEW;
        }
    }

    private void onMove(float x, float y) {
        if (!down) return;
        float dx = x - sx, dy = y - sy;
        boolean far = dx * dx + dy * dy > 18 * 18;
        if (mode == M_DRAG && dragObj != null) {
            if (far && !moved) { moved = true; sel = dragObj; host.haptic(); }
            if (moved) {
                dragObj.vx = x - lx > 0 ? (x - lx) * 60 : (x - lx) * 60;
                dragObj.x = Gfx.clamp(x + dragDX, 30, W - 30);
                dragObj.y = Gfx.clamp(y + dragDY, 150, H - 6);
            }
        } else if (mode == M_TRAY) {
            if (!trayDecided && far) {
                trayDecided = true;
                if (trayCard >= 0 && dy < -20 && Math.abs(dy) > Math.abs(dx) * .8f) {
                    Obj o = spawnCard(trayCard, x, y);
                    if (o != null) {
                        mode = M_DRAG; dragObj = o; moved = true;
                        dragDX = 0; dragDY = 0; o.lift = 1;
                        return;
                    }
                }
                scrolling = true;
            }
            if (trayDecided && mode == M_TRAY) { trayScroll -= x - lx; trayVel = -(x - lx) * 40; clampTray(); }
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
                if (!moved) {
                    sel = o;
                    o.hopV = 520; host.haptic();
                    if (o.isChar) { o.emote = rnd.nextInt(7); o.emoteT = 0; }
                } else {
                    o.sqv = -2.4f;
                    sel = o;
                }
            } else if (mode == M_TRAY) {
                if (!trayDecided && trayCard >= 0 && cardAt(x, y) == trayCard) {
                    if (trayTab == 1 && trayCard == 0) go(EDITOR, null, 1, null);
                    else spawnCard(trayCard, W * .5f + (rnd.nextFloat() - .5f) * 500, 520 + rnd.nextFloat() * 120);
                }
            } else if (mode == M_NONE && !moved) {
                sel = null;
            }
        } else if (screen == EDITOR && mode == M_PREVIEW && !scrolling) {
            edHopV = 700; edEmote = rnd.nextInt(7); edEmoteT = 0;
            edMood = new int[]{1, 5, 0, 3, 2, 2, 1}[edEmote];
        }
        mode = M_NONE;
        dragObj = null;
    }

    // ------------------------------------------------------------------ button actions
    private void onButton(int id) {
        host.haptic();
        if (id >= B_LOC && id < B_LOC + 20) { go(SCENE, Scenes.ALL[id - B_LOC].id, 0, null); return; }
        if (id >= E_OPT) { applyOption((id - E_OPT) / 100, (id - E_OPT) % 100); return; }
        if (id >= B_CHIP && id < B_CHIP + 10) { trayCat = id - B_CHIP; trayScroll = 0; return; }
        switch (id) {
            case B_NEWCHAR: go(EDITOR, null, 0, null); break;
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
        ov(c, px, 944, 230, 52, 0xFFE9CFA3);
        ov(c, px, 930, 230, 52, 0xFFFFE9C2);
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
        Avatar.draw(c, work, p);
        c.restore();
        if (edEmoteT >= 0) Icons.emote(c, edEmote, px, 930 - 380 * cs - 50, 1.1f * Gfx.back(edEmoteT * 4), t);

        // panel
        float x0 = W * .44f, x1 = W - 40, y0 = 230, y1 = H - 40;
        rr(c, x0, y0 + 8, x1 - x0, y1 - y0, 40, al(0xFF000000, 30));
        rr(c, x0, y0, x1 - x0, y1 - y0, 40, 0xFFFFFFFF);
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
                ci(c, cx, cy + 4, 38, al(0xFF000000, 30));
                ci(c, cx, cy, 38, cols[i]);
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
            rr(c, -size / 2, -size / 2, size, size, 28, on ? 0xFFE5E8FF : 0xFFF5F2FA);
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
            Avatar.draw(c, l, IDLE);
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
