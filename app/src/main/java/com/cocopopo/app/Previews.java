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
            });
            g.layout(w, h);
            String[] a = name.substring(2).split("_");
            if (a[0].equals("scene")) { g.screen = Game.SCENE; g.enterScene(a[1]); }
            if (a[0].equals("editor")) { g.screen = Game.EDITOR; g.debugEditor(a.length > 1 ? Integer.parseInt(a[1]) : 0); }
            if (a[0].equals("tray")) { g.screen = Game.SCENE; g.enterScene(a[1]); g.debugTray(Integer.parseInt(a[2])); }
            for (int i = 0; i < 40; i++) g.update(.05f);
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
        };
        Game g = new Game(host);
        g.layout(w, h);
        StringBuilder log = new StringBuilder();
        float s = g.scale;
        for (int i = 0; i < 10; i++) frame(g, c);
        // tap "Cozy Home" tile
        tap(g, c, 480 * s, 520 * s);
        for (int i = 0; i < 40; i++) frame(g, c);
        log.append("screen=" + g.screen + " loc=" + g.loc + " objs=" + g.objs.size() + "\n");
        // open items tray, tap first card -> spawn
        tap(g, c, 250 * s, (Game.H - 92) * s);
        for (int i = 0; i < 30; i++) frame(g, c);
        int before = g.objs.size();
        tap(g, c, 520 * s, 900 * s);
        for (int i = 0; i < 10; i++) frame(g, c);
        log.append("spawned by tap: " + (g.objs.size() - before) + "\n");
        // drag a card upward into the scene
        before = g.objs.size();
        g.touch(0, 750 * s, 900 * s); frame(g, c);
        for (int i = 1; i <= 10; i++) { g.touch(1, 750 * s, (900 - i * 30) * s); frame(g, c); }
        g.touch(2, 750 * s, 600 * s); frame(g, c);
        log.append("spawned by drag: " + (g.objs.size() - before) + "\n");
        // scroll tray sideways
        g.touch(0, 900 * s, 900 * s); frame(g, c);
        for (int i = 1; i <= 10; i++) { g.touch(1, (900 - i * 40) * s, 905 * s); frame(g, c); }
        g.touch(2, 500 * s, 905 * s);
        for (int i = 0; i < 20; i++) frame(g, c);
        // drag an existing object
        Obj o = g.objs.get(0); for (Obj q : g.objs) if (q.isChar) { o = q; break; }
        g.back(); for (int i = 0; i < 30; i++) frame(g, c); o.y = 1060; o.x = 900; for (int i = 0; i < 3; i++) frame(g, c); float ox = o.x, oy = o.y;
        g.touch(0, o.x * s, (o.y - 100) * s); frame(g, c);
        for (int i = 1; i <= 8; i++) { g.touch(1, (o.x + 20) * s, (oy - 20 - i * 10) * s); frame(g, c); }
        g.touch(2, (o.x) * s, (oy - 100) * s);
        for (int i = 0; i < 20; i++) frame(g, c);
        log.append("dragged: " + (o.x != ox || o.y != oy) + " moved=" + g.debugMovedCount(ox, oy) + "\n");
        // camera + back to map
        g.pause();
        log.append("saved scene: " + (store.get("scene_home") != null) + "\n");
        g.back(); g.back();
        for (int i = 0; i < 40; i++) frame(g, c);
        log.append("screen after back=" + g.screen + "\n");
        // editor
        tap(g, c, 100 * s, 92 * s);
        for (int i = 0; i < 40; i++) frame(g, c);
        log.append("screen=" + g.screen + "\n");
        for (int tab = 0; tab < 5; tab++) { g.debugEditor(tab); for (int i = 0; i < 3; i++) frame(g, c); }
        tap(g, c, (g.W - 96) * s, 90 * s);
        for (int i = 0; i < 40; i++) frame(g, c);
        log.append("after done screen=" + g.screen + " lib=" + g.lib.size() + " libSaved=" + store.get("lib") + "\n");
        // every scene renders
        for (Scenes.Loc l : Scenes.ALL) { g.screen = Game.SCENE; g.enterScene(l.id); for (int i = 0; i < 5; i++) frame(g, c); }
        log.append("all scenes ok\n");
        // reload persisted home
        g.enterScene("home");
        log.append("reloaded objs=" + g.objs.size() + "\n");
        return log.toString();
    }

    private static void frame(Game g, Canvas c) { g.update(.03f); g.draw(c); }

    private static void tap(Game g, Canvas c, float x, float y) {
        g.touch(0, x, y); frame(g, c); g.touch(2, x, y); frame(g, c);
    }
}
