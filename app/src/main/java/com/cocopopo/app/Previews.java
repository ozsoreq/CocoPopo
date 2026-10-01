package com.cocopopo.app;

import android.graphics.Canvas;

/** Entry point used by the desktop preview tool to render individual screens. */
public final class Previews {
    private Previews() {}

    public static void render(String name, Canvas c, int w, int h) {
        Gfx.rect(c, 0, 0, w, h, 0xFFEAF6FF);
        if (name.startsWith("props")) {
            int page = name.length() > 5 ? name.charAt(5) - '0' : 0;
            int per = 28;
            for (int i = 0; i < per; i++) {
                int idx = page * per + i;
                if (idx >= PropArt.ALL.size()) break;
                PropArt.Def d = PropArt.ALL.get(idx);
                int col = i % 7, row = i / 7;
                float cx = 140 + col * 270, cy = 240 + row * 260;
                Gfx.rr(c, cx - 125, cy - 215, 250, 240, 24, 0xFFFFFFFF);
                float sc = Math.min(200 / d.w, 190 / d.h);
                c.save(); c.translate(cx, cy); c.scale(sc, sc);
                Gfx.ol(5); PropArt.draw(c, d.id, 3); Gfx.ol(0);
                c.restore();
                Gfx.text(c, d.name, cx, cy + 50, 28, 0xFF555566, android.graphics.Paint.Align.CENTER);
            }
        }
        if (name.startsWith("bg_")) {
            String id = name.substring(3);
            Scenes.drawBg(c, id, w, h, 1.5f);
            for (String d : Scenes.defaults(id)) {
                String[] p = d.split(":");
                float x = Float.parseFloat(p[1]) * w, y = Float.parseFloat(p[2]), sc = Float.parseFloat(p[3]);
                Gfx.ol(p[0].startsWith("@") ? 6.5f : 5f); c.save(); c.translate(x, y); c.scale(sc, sc);
                if (p[0].startsWith("@")) Avatar.draw(c, Look.PRESETS[Integer.parseInt(p[0].substring(1))], new Pose());
                else PropArt.draw(c, p[0], 1.5f);
                c.restore();
            }
        }
        if (name.equals("map_icons")) {
            for (int i = 0; i < Scenes.ALL.length; i++) {
                Gfx.rr(c, 100 + (i % 4) * 450, 100 + (i / 4) * 450, 400, 400, 40, 0xFFFFFFFF);
                Scenes.drawIcon(c, Scenes.ALL[i].id, 300 + (i % 4) * 450, 300 + (i / 4) * 450, 1.5f, 1f);
            }
        }
        if (name.startsWith("g_")) {
            Game g = new Game(new Game.Host() {
                public String load(String k) { return null; }
                public void save(String k, String v) {}
                public void photo() {}
                public void haptic() {}
                public void quit() {}
            public void sound(int id) {}
            });
            g.layout(w, h);
            String[] a = name.substring(2).split("_");
            if (a[0].equals("scene")) { g.screen = Game.SCENE; g.enterScene(a[1]); }
            if (a[0].equals("editor")) { g.screen = Game.EDITOR; g.debugEditor(a.length > 1 ? Integer.parseInt(a[1]) : 0); }
            if (a[0].equals("life")) {
                g.screen = Game.SCENE; g.enterScene("home");
                g.debugLife();
            }
            if (a[0].equals("drag")) { g.screen = Game.SCENE; g.enterScene("home"); g.debugDrag(); g.draw(c); return; }
            if (a[0].equals("tray")) { g.screen = Game.SCENE; g.enterScene(a[1]); g.debugTray(Integer.parseInt(a[2])); }
            for (int i = 0; i < 40; i++) g.update(.0503f);
            g.draw(c);
            return;
        }
        if (name.equals("icon")) {
            Gfx.rr(c, 0, 0, w, h, 0, 0xFFFFB3C7);
            Gfx.ci(c, w / 2f, h / 2f + 40, 420, 0xFFFFD6E2);
            Gfx.ol(7); Avatar.drawAt(c, Look.PRESETS[0], w / 2f, h * .97f, 1.25f, 0); Gfx.ol(0);
            return;
        }
        if (name.equals("cast")) {
            for (int i = 0; i < Look.PRESETS.length; i++) {
                int col = i % 5, row = i / 5;
                Gfx.ol(5); Avatar.drawAt(c, Look.PRESETS[i], 200 + col * 380, 470 + row * 520, 1.15f, i); Gfx.ol(0);
            }
        }
    }

    /** Scripted headless playthrough used as a smoke test. Returns a log. */
    public static String smoke(Canvas c, int w, int h) {
        final java.util.HashMap<String, String> store = new java.util.HashMap<String, String>();
        Game.Host host = new Game.Host() {
            public String load(String k) { return store.get(k); }
            public void save(String k, String v) { if (v == null) store.remove(k); else store.put(k, v); }
            public void photo() {}
            public void haptic() {}
            public void quit() {}
            public void sound(int id) {}
        };
        Game g = new Game(host);
        g.layout(w, h);
        StringBuilder log = new StringBuilder();
        float s = g.scale;
        for (int i = 0; i < 10; i++) frame(g, c);
        // spin the world by dragging, let it settle, then press play
        g.touch(0, 900 * s, 700 * s); frame(g, c);
        for (int i = 1; i <= 10; i++) { g.touch(1, (900 - i * 45) * s, 700 * s); frame(g, c); }
        g.touch(2, 450 * s, 700 * s);
        for (int i = 0; i < 120; i++) frame(g, c);
        log.append("world spun, focus=" + g.debugFocus() + "\n");
        tap(g, c, g.W / 2 * s, 985 * s);
        for (int i = 0; i < 40; i++) frame(g, c);
        log.append("entered: screen=" + g.screen + " loc=" + g.loc + "\n");

        // a clean test room in the home
        g.screen = Game.SCENE; g.enterScene("home");
        g.objs.clear();
        Obj sofa = Obj.prop("sofa", 400, 965); g.objs.add(sofa);
        Obj bed = Obj.prop("bed", 400, 530); g.objs.add(bed);
        Obj table = Obj.prop("table", 1500, 965); g.objs.add(table);
        Obj apple = Obj.prop("apple", 1100, 965); g.objs.add(apple);
        Obj ball = Obj.prop("ball", 1250, 1040); g.objs.add(ball);
        Obj kid = Obj.character(Look.PRESETS[1].copy(), 800, 970); g.objs.add(kid);
        for (Obj o : g.objs) { o.pop = 1; o.idleT = 9999; }
        for (int i = 0; i < 5; i++) frame(g, c);

        drag(g, c, kid.x, kid.y - 150, 400 - 86 + 5, 960 - 150);
        for (int i = 0; i < 20; i++) frame(g, c);
        log.append("sit on sofa: " + (kid.state == Obj.SIT && kid.link == sofa) + "\n");

        drag(g, c, kid.x, kid.y - 150, 420, 480 - 150);
        for (int i = 0; i < 20; i++) frame(g, c);
        log.append("sleep in bed: " + (kid.state == Obj.LIE && kid.link == bed) + "\n");

        drag(g, c, kid.x - 200, kid.y - 60, 800, 970 - 150);
        for (int i = 0; i < 40; i++) frame(g, c);
        log.append("picked out of bed & fell to floor: state=" + kid.state + " y=" + (int) kid.y + "\n");

        drag(g, c, apple.x, apple.y - 40, kid.x, kid.y - 150);
        for (int i = 0; i < 10; i++) frame(g, c);
        log.append("holding apple: " + (kid.held == apple && apple.state == Obj.HELD) + "\n");

        // save + reload keeps the apple in hand
        g.pause();
        g.enterScene("home");
        Obj kid2 = null;
        for (Obj o : g.objs) if (o.isChar) { kid2 = o; o.idleT = 9999; }
        log.append("reload keeps holding: " + (kid2 != null && kid2.held != null) + "\n");
        for (int i = 0; i < 5; i++) frame(g, c);
        for (int b = 0; b < 3; b++) { tap(g, c, kid2.x, kid2.y - 300); for (int i = 0; i < 30; i++) frame(g, c); }
        log.append("ate apple: " + (kid2.held == null) + " objs=" + g.objs.size() + "\n");

        Obj tb = null, bl = null;
        for (Obj o : g.objs) { if ("table".equals(o.prop)) tb = o; if ("ball".equals(o.prop)) bl = o; }
        Obj cake = Obj.prop("cake", 1000, 965); cake.pop = 1; g.objs.add(cake);
        frame(g, c);
        drag(g, c, cake.x, cake.y - 40, tb.x + 20, tb.y - 400);
        for (int i = 0; i < 60; i++) frame(g, c);
        log.append("  dbg cake state=" + cake.state + " " + (int) cake.x + "," + (int) cake.y + " table " + (int) tb.x + "," + (int) tb.y + " link=" + (cake.link == null ? "-" : cake.link.isChar ? "char" : cake.link.prop) + "\n");
        log.append("cake on table: " + (cake.state == Obj.ON_TOP && cake.link == tb) + "\n");

        float bx0 = bl.x;
        tap(g, c, bl.x, bl.y - 40);
        for (int i = 0; i < 90; i++) frame(g, c);
        log.append("ball kicked & landed: moved=" + (Math.abs(bl.x - bx0) > 5) + " falling=" + bl.falling + "\n");

        // walk to tap
        tap(g, c, kid2.x, kid2.y - 300);
        frame(g, c);
        float kx = kid2.x;
        tap(g, c, kid2.x + 400, 1000);
        for (int i = 0; i < 20; i++) frame(g, c);
        log.append("walking: " + (kid2.walking || Math.abs(kid2.x - kx) > 50) + "\n");

        // ---- new verbs
        g.objs.clear();
        Obj tub = Obj.prop("tub", 1300, 540); Obj slide = Obj.prop("slide", 760, 1000); Obj cart = Obj.prop("cart", 1500, 1000);
        Obj sofa2 = Obj.prop("sofa", 520, 960); Obj car = Obj.prop("car", 1000, 1050); car.scale = 1.6f;
        Obj m1 = Obj.character(Look.PRESETS[0].copy(), 900, 540), m2 = Obj.character(Look.PRESETS[5].copy(), 1100, 1000);
        Obj duck = Obj.prop("duck", 1600, 1000), mush = Obj.prop("mushroom", 1750, 1000);
        Obj[] all = {tub, slide, cart, sofa2, car, m1, m2, duck, mush};
        for (Obj o : all) { o.pop = 1; o.idleT = 999; g.objs.add(o); }
        for (int i = 0; i < 5; i++) frame(g, c);
        drag(g, c, m1.x, m1.y - 150, tub.x, tub.y - 150);
        for (int i = 0; i < 10; i++) frame(g, c);
        log.append("bathe: " + (m1.state == Obj.BATHE) + "\n");
        drag(g, c, duck.x, duck.y - 40, cart.x, cart.y - 120);
        for (int i = 0; i < 10; i++) frame(g, c);
        log.append("duck in cart: " + cart.contents + "\n");
        tap(g, c, cart.x, cart.y - 90);
        for (int i = 0; i < 60; i++) frame(g, c);
        log.append("tap cart pops it out: " + cart.contents.size() + " objs=" + g.objs.size() + "\n");
        drag(g, c, m1.x, m1.y - 120, slide.x - 100, slide.y - 300);
        for (int i = 0; i < 6; i++) frame(g, c);
        boolean sliding = m1.state == Obj.SLIDE;
        for (int i = 0; i < 60; i++) frame(g, c);
        log.append("slide: " + sliding + " then free=" + (m1.state == Obj.FREE) + "\n");
        drag(g, c, m1.x, m1.y - 150, m2.x + 30, m2.y - 170);
        for (int i = 0; i < 5; i++) frame(g, c);
        log.append("hug: " + (m1.hugT > 0 && m2.hugT > 0) + "\n");
        for (int i = 0; i < 80; i++) frame(g, c);
        drag(g, c, m1.x, m1.y - 150, m2.x, m2.y + Avatar.neckY(m2.look) - 90);
        for (int i = 0; i < 10; i++) frame(g, c);
        log.append("shoulder ride: " + (m1.state == Obj.SIT && m1.link == m2) + "\n");
        // walk-and-use: select the lower character, tap the sofa
        log.append("  dbg rider at " + (int) m1.x + "," + (int) m1.y + " carrier " + (int) m2.x + "," + (int) m2.y + "\n");
        drag(g, c, m1.x, m1.y - 150, 900, 540 - 150);   // put m1 upstairs
        for (int i = 0; i < 40; i++) frame(g, c);
        log.append("  dbg after upstairs drag m1 " + (int) m1.x + "," + (int) m1.y + " st=" + m1.state + " m2 " + (int) m2.x + "," + (int) m2.y + " st=" + m2.state + "\n");
        tap(g, c, m1.x, m1.y - 200);
        frame(g, c);
        Obj selBefore = g.debugSel();

        tap(g, c, sofa2.x, sofa2.y - 60);
        log.append("  dbg selBefore=m1? " + (selBefore == m1) + " after tap sofa sel=" + (g.debugSel() == m1 ? "m1" : g.debugSel() == null ? "null" : (g.debugSel().isChar ? "char" : g.debugSel().prop)) + " pend=" + (m1.pend != null) + " walking=" + m1.walking + "\n");
        for (int i = 0; i < 600 && m1.state != Obj.SIT; i++) frame(g, c);
        log.append("  dbg m1 state=" + m1.state + " x=" + (int) m1.x + " y=" + (int) m1.y + " walking=" + m1.walking + " pend=" + (m1.pend != null) + " sel=" + (g.debugSel() == m1) + "\n");
        log.append("walk downstairs via ladder & sit on sofa: " + (m1.state == Obj.SIT && m1.link == sofa2) + "\n");
        drag(g, c, m2.x, m2.y - 150, car.x - 20, car.y - 140);
        for (int i = 0; i < 6; i++) frame(g, c);
        log.append("  dbg m2 state=" + m2.state + " link=" + (m2.link == null ? "-" : (m2.link.isChar ? "char" : m2.link.prop)) + " x=" + (int) m2.x + " y=" + (int) m2.y + "\n");
        log.append("ride car: " + (m2.state == Obj.SIT && m2.link == car) + "\n");
        tap(g, c, m2.x, m2.y - 200); frame(g, c);
        tap(g, c, 1700, 1040);
        for (int i = 0; i < 90; i++) frame(g, c);
        log.append("car drove: x=" + (int) car.x + " rider follows=" + (Math.abs(m2.x - car.x) < 100) + "\n");
        g.debugEditorAll(c);
        log.append("editor all tabs ok\n");

        // let life run for a while in every place
        for (Scenes.Loc l : Scenes.ALL) { g.enterScene(l.id); for (int i = 0; i < 400; i++) frame(g, c); }
        log.append("all scenes ran 12s of life ok\n");
        g.screen = Game.MAP; for (int i = 0; i < 30; i++) frame(g, c);
        tap(g, c, (g.W - 110) * s, 640 * s);
        for (int i = 0; i < 60; i++) frame(g, c);
        log.append("arrow spin focus=" + g.debugFocus() + "\n");
        return log.toString();
    }

    private static void drag(Game g, Canvas c, float x0, float y0, float x1, float y1) {
        float s = g.scale;
        g.draw(c);
        g.touch(0, x0 * s, y0 * s); frame(g, c);
        for (int i = 1; i <= 12; i++) { g.touch(1, (x0 + (x1 - x0) * i / 12) * s, (y0 + (y1 - y0) * i / 12) * s); frame(g, c); }
        for (int i = 0; i < 4; i++) { g.touch(1, x1 * s, y1 * s); frame(g, c); }
        try { Thread.sleep(100); } catch (InterruptedException e) { }
        g.touch(2, x1 * s, y1 * s); frame(g, c);
    }

    private static int fc;

    private static void frame(Game g, Canvas c) { g.update(.03f); if (fc++ % 6 == 0) g.draw(c); }

    private static void tap(Game g, Canvas c, float x, float y) {
        float s = g.scale;
        if (x > g.W * s * 0 && false) return;
        g.draw(c);
        g.touch(0, x, y); frame(g, c); g.touch(2, x, y); frame(g, c);
    }
}
