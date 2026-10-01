package com.cocopopo.app;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;

import static com.cocopopo.app.Gfx.*;

/** The playable locations: backgrounds, map icons and starting contents. */
final class Scenes {
    static final class Loc {
        final String id, name;
        final int color;
        Loc(String id, String name, int color) { this.id = id; this.name = name; this.color = color; }
    }

    static final Loc[] ALL = {
        new Loc("home", "Cozy Home", 0xFFFF8FA3),
        new Loc("school", "School", 0xFF5BC0F8),
        new Loc("hospital", "Hospital", 0xFF6DD3C8),
        new Loc("market", "Market", 0xFFFFC043),
        new Loc("cafe", "Café", 0xFFE09A62),
        new Loc("park", "Park", 0xFF7ED957),
        new Loc("beach", "Beach", 0xFF4FD0E6),
        new Loc("fair", "Funfair", 0xFFB67CFF),
    };

    static Loc get(String id) {
        for (Loc l : ALL) if (l.id.equals(id)) return l;
        return ALL[0];
    }

    /**
     * Starting contents. "prop:xFraction:y:scale" or "@presetIndex:xFraction:y:scale".
     */
    static String[] defaults(String id) {
        switch (id) {
            case "home": return new String[]{
                "rug:.17:520:1", "bed:.12:530:1", "dresser:.33:535:.95", "lamp:.41:535:.9", "plant:.02:545:.8",
                "tub:.66:540:.95", "toilet:.88:545:.85", "frame:.28:360:.9",
                "rug:.2:975:1", "sofa:.18:965:1", "tv:.38:960:.95", "plant:.05:970:.9",
                "fridge:.62:965:1", "stove:.75:962:.95", "table:.88:970:.85", "chair:.82:975:.8",
                "@0:.42:525:1", "@1:.3:968:1", "duck:.66:500:1"};
            case "school": return new String[]{
                "desk:.22:860:1", "desk:.42:860:1", "desk:.62:860:1", "desk:.22:1010:1", "desk:.42:1010:1", "desk:.62:1010:1",
                "shelf:.93:800:.9", "globe:.82:760:.8", "clock:.6:200:.8", "backpack:.08:830:.8",
                "@6:.76:800:1", "@2:.32:940:1", "@3:.52:945:1"};
            case "hospital": return new String[]{
                "hbed:.2:830:1", "hbed:.64:830:1", "ivstand:.36:820:1", "medkit:.88:800:.9", "plant:.94:820:.9", "wheelchair:.8:1000:1",
                "@5:.5:860:1", "@4:.14:850:1"};
            case "market": return new String[]{
                "cart:.2:950:1", "crate:.45:800:1", "crate:.6:800:1", "register:.86:790:1", "fridge:.07:770:.9",
                "apple:.15:700:1", "juice:.36:780:.8", "@7:.74:810:1", "@8:.34:930:1"};
            case "cafe": return new String[]{
                "table:.2:900:.85", "chair:.1:910:.75", "chair:.3:910:.75", "table:.5:1000:.85", "plant:.04:760:.9",
                "coffee:.16:830:1", "cake:.56:900:1", "cupcake:.64:760:.9", "register:.8:760:.95",
                "@7:.82:770:1", "@9:.36:975:1"};
            case "park": return new String[]{
                "tree:.08:700:1", "tree:.92:690:1.1", "swing:.3:760:1", "slide:.64:760:1", "bench:.5:960:1",
                "flower:.2:900:1", "flower:.24:915:.8", "bush:.78:930:1", "ball:.42:1020:.8", "rock:.9:950:.8", "car:.86:1050:1.5", "mushroom:.58:1010:1",
                "@1:.12:900:1", "@2:.7:930:1"};
            case "beach": return new String[]{
                "umbrella:.2:880:1", "palm:.06:740:1", "palm:.93:760:1", "sandcastle:.5:960:1", "surfboard:.78:900:.9",
                "rug:.22:990:.8", "ball:.65:1010:.8", "icecream:.4:870:.9",
                "@3:.34:960:1", "@9:.84:960:1"};
            default: return new String[]{
                "balloon:.1:900:1", "balloon:.14:910:.9", "balloon:.9:900:1", "popcorn:.3:900:1", "cotton:.7:900:1",
                "teddy:.55:900:1", "gift:.82:1010:.9",
                "@0:.35:960:1", "@1:.66:960:1"};
        }
    }

    // ================================================================== backgrounds
    static void drawBg(Canvas c, String id, float W, float H, float t) {
        Gfx.ol(5);
        Gfx.shade = true;
        drawBg2(c, id, W, H, t);
        Gfx.ol(0);
        Gfx.shade = false;
    }

    private static void drawBg2(Canvas c, String id, float W, float H, float t) {
        switch (id) {
            case "home": home(c, W, H, t); break;
            case "school": school(c, W, H, t); break;
            case "hospital": hospital(c, W, H, t); break;
            case "market": market(c, W, H, t); break;
            case "cafe": cafe(c, W, H, t); break;
            case "park": park(c, W, H, t); break;
            case "beach": beach(c, W, H, t); break;
            default: fair(c, W, H, t); break;
        }
    }

    private static float hash(int i) {
        double v = Math.sin(i * 127.1 + 311.7) * 43758.5453;
        return (float) (v - Math.floor(v));
    }

    static void cloud(Canvas c, float x, float y, float s, int col) {
        float o = Gfx.olw; Gfx.ol(0);
        ci(c, x, y, 34 * s, col); ci(c, x + 38 * s, y - 14 * s, 42 * s, col); ci(c, x + 84 * s, y, 32 * s, col);
        rr(c, x - 34 * s, y, 150 * s, 34 * s, 17 * s, col);
        Gfx.ol(o);
    }

    static void clouds(Canvas c, float W, float t, float y0, int n, int col) {
        for (int i = 0; i < n; i++) {
            float sp = 6 + hash(i) * 8;
            float x = ((hash(i + 9) * (W + 400) + t * sp) % (W + 400)) - 200;
            cloud(c, x, y0 + hash(i + 3) * 160, .8f + hash(i + 5) * .7f, col);
        }
    }

    static void window(Canvas c, float x, float y, float w, float h, int curtain) {
        rr(c, x - 12, y - 12, w + 24, h + 24, 16, 0xFFFFFFFF);
        rr(c, x, y, w, h, 8, 0xFFA8E2FF);
        ov(c, x + w * .3f, y + h * .3f, w * .18f, h * .08f, al(0xFFFFFFFF, 220));
        ov(c, x + w * .7f, y + h * .45f, w * .14f, h * .06f, al(0xFFFFFFFF, 200));
        rect(c, x + w / 2 - 4, y, 8, h, 0xFFFFFFFF);
        rect(c, x, y + h / 2 - 4, w, 8, 0xFFFFFFFF);
        if (curtain != 0) {
            poly(c, curtain, x - 14, y - 22, x + w * .3f, y - 22, x + w * .22f, y + h * .5f, x + w * .34f, y + h + 8, x - 14, y + h + 8);
            poly(c, curtain, x + w + 14, y - 22, x + w * .7f, y - 22, x + w * .78f, y + h * .5f, x + w * .66f, y + h + 8, x + w + 14, y + h + 8);
            rr(c, x - 24, y - 32, w + 48, 12, 6, dk(curtain, .25f));
        }
    }

    /** Wallpaper + floor; floorY is where the wall ends. */
    static void room(Canvas c, float x, float y, float w, float floorY, float bottom, int wall, int pattern, int floorCol) {
        rect(c, x, y, w, floorY - y, wall);
        if (pattern == 1) for (float px = x + 30; px < x + w; px += 60) rect(c, px, y, 22, floorY - y, al(0xFFFFFFFF, 55));
        if (pattern == 2) for (int i = 0; i < w / 56 + 1; i++) for (int j = 0; j < (floorY - y) / 56 + 1; j++)
            ci(c, x + 28 + i * 56 + (j % 2) * 28, y + 28 + j * 56, 6, al(0xFFFFFFFF, 80));
        if (pattern == 3) {
            for (float px = x; px < x + w; px += 70) rect(c, px, y, 3, floorY - y, al(0xFFFFFFFF, 140));
            for (float py = y; py < floorY; py += 70) rect(c, x, py, w, 3, al(0xFFFFFFFF, 140));
        }
        rect(c, x, floorY - 22, w, 22, dk(wall, .10f));
        grad(c, x, floorY, w, bottom - floorY, floorCol, dk(floorCol, .14f));
        for (float py = floorY + 34; py < bottom; py += 42) rect(c, x, py, w, 3, al(0xFF000000, 22));
        if (Gfx.olw > 0) {
            ln(c, x, floorY, x + w, floorY, 4, Gfx.olc);
            Gfx.stroke(Gfx.olc, Gfx.olw * 2);
            c.drawRect(x, y, x + w, bottom, Gfx.P);
        }
    }

    // ----------------------------------------------------------------- Home
    private static void home(Canvas c, float W, float H, float t) {
        grad(c, 0, 0, W, H, 0xFF93DAFF, 0xFFE9F8FF);
        clouds(c, W, t, 60, 5, 0xFFFFFFFF);
        rect(c, 0, 990, W, H - 990, 0xFF7ED67A);
        rect(c, 0, 990, W, 14, 0xFF63C45F);
        for (int i = 0; i < 14; i++) ci(c, hash(i) * W, 1010 + hash(i + 40) * 60, 5, i % 2 == 0 ? 0xFFFFFFFF : 0xFFFFE066);
        float x0 = W * .05f, x1 = W * .95f, mid = x0 + (x1 - x0) * .56f;
        // roof
        poly(c, 0xFFE0675A, x0 - 40, 132, x1 + 40, 132, x1 - 130, 14, x0 + 130, 14);
        for (int i = 0; i < 5; i++) ln(c, x0 - 30 + i * 8 + 0, 132 - i * 24, x1 + 30 - i * 8, 132 - i * 24, 4, al(0xFF000000, 26));
        rr(c, x0 - 56, 124, x1 - x0 + 112, 20, 10, 0xFFC24E44);
        // body
        rr(c, x0 - 14, 138, x1 - x0 + 28, 862, 18, 0xFFFFEBC9);
        // upper rooms
        float uy = 156, uf = 470, ub = 560;
        room(c, x0 + 6, uy, mid - x0 - 12, uf, ub, 0xFFDCCBFF, 2, 0xFFD9A066);
        room(c, mid + 12, uy, x1 - mid - 18, uf, ub, 0xFFC8F0E6, 3, 0xFFB7E3F2);
        // lower rooms
        float ly = 604, lf = 910, lb = 990;
        room(c, x0 + 6, ly, mid - x0 - 12, lf, lb, 0xFFFFDDB5, 1, 0xFFD39A62);
        room(c, mid + 12, ly, x1 - mid - 18, lf, lb, 0xFFFFF1B8, 0, 0xFFF3D9A4);
        // divider walls & slab
        rect(c, mid - 6, uy, 18, ub - uy, 0xFFFFEBC9);
        rect(c, mid - 6, ly, 18, lb - ly, 0xFFFFEBC9);
        rect(c, x0 - 14, ub, x1 - x0 + 28, ly - ub, 0xFFF2D3A2);
        rect(c, x0 - 14, ub + 18, x1 - x0 + 28, 6, al(0xFF000000, 26));
        // stairs hint between floors
        // windows & decor
        window(c, x0 + 150, 230, 190, 190, 0xFFFF8FA3);
        window(c, mid - 380, 640, 200, 200, 0xFF9BD6FF);
        window(c, x1 - 170, 250, 120, 190, 0);
        // bedroom stars + moon
        ci(c, x0 + 480, 250, 40, 0xFFFFE680); ci(c, x0 + 498, 238, 36, 0xFFDCCBFF);
        for (int i = 0; i < 5; i++) ci(c, x0 + 400 + i * 54, 330 + (i % 2) * 40, 6, 0xFFFFE680);
        // bathroom mirror
        rr(c, mid + 150, 230, 130, 170, 40, 0xFFFFFFFF);
        rr(c, mid + 160, 240, 110, 150, 32, 0xFFD2F1FF);
        // kitchen cabinets
        rr(c, x1 - 640, 640, 560, 110, 14, 0xFFF39AAA);
        for (int i = 0; i < 4; i++) { rr(c, x1 - 632 + i * 138, 648, 130, 94, 10, 0xFFFFB3C0); ci(c, x1 - 590 + i * 138, 700, 6, 0xFFFFFFFF); }
        // living room shelf lights
        for (int i = 0; i < 9; i++) ci(c, x0 + 40 + i * 60, 636 + (i % 2) * 14, 8, i % 3 == 0 ? 0xFFFF6F8F : (i % 3 == 1 ? 0xFFFFE066 : 0xFF6FC3FF));
        // ladder linking the two floors
        float lx = W * .5f;
        rr(c, lx - 50, 452, 100, 30, 10, 0xFF8A5A30);
        ln(c, lx - 34, 470, lx - 34, 985, 10, 0xFFC98A44); ln(c, lx + 34, 470, lx + 34, 985, 10, 0xFFC98A44);
        for (float ry = 500; ry < 980; ry += 52) ln(c, lx - 34, ry, lx + 34, ry, 8, 0xFFD9A066);
        // house frame outline
        rrs(c, x0 - 14, 138, x1 - x0 + 28, 862, 18, 0xFFE5C48E, 8);
    }

    // ----------------------------------------------------------------- School
    private static void school(Canvas c, float W, float H, float t) {
        room(c, 0, 0, W, 640, H, 0xFFBFE8D8, 1, 0xFFE0B27A);
        // bunting
        for (int i = 0; i < W / 70; i++) {
            float x = 20 + i * 70;
            poly(c, new int[]{0xFFFF6F8F, 0xFFFFD43B, 0xFF4FB3FF, 0xFF7ED957}[i % 4], x, 0, x + 56, 0, x + 28, 70);
        }
        // chalkboard
        float cx = W * .5f;
        rr(c, cx - 400, 130, 800, 360, 18, 0xFFC98A44);
        rr(c, cx - 384, 146, 768, 328, 10, 0xFF2F7A62);
        rr(c, cx - 400, 478, 800, 22, 8, 0xFFB07744);
        text(c, "A B C", cx - 200, 260, 90, al(0xFFFFFFFF, 235), Paint.Align.CENTER);
        text(c, "1 + 2 = 3", cx + 160, 250, 70, al(0xFFFFFFFF, 220), Paint.Align.CENTER);
        ln(c, cx - 340, 300, cx + 340, 300, 4, al(0xFFFFFFFF, 90));
        ci(c, cx - 200, 390, 38, al(0xFFFFE066, 230));
        for (int i = 0; i < 8; i++) { double a = i * Math.PI / 4; ln(c, cx - 200 + (float) Math.cos(a) * 50, 390 + (float) Math.sin(a) * 50, cx - 200 + (float) Math.cos(a) * 68, 390 + (float) Math.sin(a) * 68, 5, al(0xFFFFE066, 230)); }
        text(c, "Hello!", cx + 140, 400, 80, al(0xFFFF9EC4, 235), Paint.Align.CENTER);
        rr(c, cx - 380, 480, 60, 10, 5, 0xFFFFFFFF);
        // windows
        window(c, W * .84f, 160, 220, 260, 0xFFFFB86B);
        window(c, W * .08f, 160, 220, 260, 0xFFFFB86B);
        // door
        // flag
        ln(c, W * .27f, 110, W * .27f, 230, 6, 0xFF8A5A30);
        poly(c, 0xFFFF5C73, W * .27f, 114, W * .27f + 70, 134, W * .27f, 160);
    }

    // ----------------------------------------------------------------- Hospital
    private static void hospital(Canvas c, float W, float H, float t) {
        room(c, 0, 0, W, 660, H, 0xFFE4F6F4, 0, 0xFFE7EFF5);
        rect(c, 0, 0, W, 96, 0xFF9ADFD6);
        rect(c, 0, 90, W, 10, 0xFF7CCFC4);
        // checker floor
        for (int i = 0; i < W / 120 + 1; i++) for (int j = 0; j < (H - 660) / 70 + 1; j++)
            if ((i + j) % 2 == 0) rect(c, i * 120, 660 + j * 70, 120, 70, al(0xFFB9D3E3, 140));
        // cross sign
        float cx = W * .5f;
        ci(c, cx, 250, 92, 0xFFFFFFFF);
        rr(c, cx - 22, 190, 44, 120, 10, 0xFFFF5C73); rr(c, cx - 60, 228, 120, 44, 10, 0xFFFF5C73);
        window(c, W * .27f, 200, 200, 240, 0);
        window(c, W * .84f, 200, 200, 240, 0);
        // curtain rail and curtains
        rr(c, W * .03f, 150, W * .94f, 10, 5, 0xFFB0BED0);
        for (int k = 0; k < 2; k++) {
            float x = k == 0 ? W * .04f : W * .62f;
            float w = W * .17f;
            for (int i = 0; i < 6; i++) {
                float px = x + i * (w / 6);
                rr(c, px, 158, w / 6 + 2, 300, 16, i % 2 == 0 ? 0xFFFFC1D6 : 0xFFFFA9C6);
            }
        }
        // heart monitor
        rr(c, W * .38f, 330, 170, 120, 14, 0xFF4B4660); // monitor
        rr(c, W * .38f + 10, 340, 150, 100, 8, 0xFF1F2A3A);
        Path pa = new Path();
        pa.moveTo(W * .38f + 20, 392);
        pa.lineTo(W * .38f + 60, 392); pa.lineTo(W * .38f + 74, 360); pa.lineTo(W * .38f + 88, 424); pa.lineTo(W * .38f + 102, 392); pa.lineTo(W * .38f + 150, 392);
        pathS(c, pa, 0xFF7ED957, 5);
    }

    // ----------------------------------------------------------------- Market
    private static void market(Canvas c, float W, float H, float t) {
        room(c, 0, 0, W, 690, H, 0xFFFFF1CF, 0, 0xFFF2E3C2);
        for (int i = 0; i < W / 110 + 1; i++) for (int j = 0; j < (H - 690) / 70 + 1; j++)
            if ((i + j) % 2 == 0) rect(c, i * 110, 690 + j * 70, 110, 70, al(0xFFFFFFFF, 120));
        // awning
        for (int i = 0; i < W / 90 + 1; i++) {
            rect(c, i * 90, 0, 90, 56, i % 2 == 0 ? 0xFFFF5C73 : 0xFFFFFFFF);
            ov(c, i * 90 + 45, 56, 45, 26, i % 2 == 0 ? 0xFFFF5C73 : 0xFFFFFFFF);
        }
        rr(c, W * .5f - 250, 90, 500, 90, 26, 0xFF58B368);
        rrs(c, W * .5f - 250, 90, 500, 90, 26, 0xFFFFFFFF, 6);
        text(c, "MARKET", W * .5f, 160, 76, 0xFFFFFFFF, Paint.Align.CENTER);
        // shelves
        int[] cols = {0xFFFF6F8F, 0xFF6FC3FF, 0xFFFFC93C, 0xFF7ED957, 0xFFB67CFF, 0xFFFF9A3D};
        for (int u = 0; u < 3; u++) {
            float sx = W * (.2f + u * .26f), sw = W * .24f;
            rr(c, sx, 230, sw, 440, 14, 0xFFE9EEF5);
            for (int r = 0; r < 3; r++) {
                float sy = 240 + r * 140;
                rr(c, sx + 10, sy, sw - 20, 126, 8, 0xFFD2DBE7);
                rr(c, sx + 6, sy + 122, sw - 12, 12, 5, 0xFFB0BED0);
                float x = sx + 20;
                int k = 0;
                while (x < sx + sw - 50) {
                    int hh = 54 + (int) (hash(u * 40 + r * 9 + k) * 40);
                    int col = cols[(k + r + u) % 6];
                    if ((k + r) % 3 == 0) { ci(c, x + 22, sy + 122 - 22, 22, col); x += 48; }
                    else { rr(c, x, sy + 122 - hh, 40, hh, 8, col); rr(c, x + 6, sy + 122 - hh + 14, 28, 18, 4, al(0xFFFFFFFF, 190)); x += 48; }
                    k++;
                }
            }
        }
    }

    // ----------------------------------------------------------------- Cafe
    private static void cafe(Canvas c, float W, float H, float t) {
        room(c, 0, 0, W, 680, H, 0xFFFFE3C8, 2, 0xFFC38A5A);
        rect(c, 0, 400, W, 280, 0xFFD9A06E);
        for (float x = 0; x < W; x += 110) rect(c, x, 400, 4, 280, al(0xFF000000, 25));
        rect(c, 0, 396, W, 16, 0xFFB57B4E);
        // hanging lamps
        for (int i = 0; i < 4; i++) {
            float x = W * (.14f + i * .24f);
            ln(c, x, 0, x, 130, 5, 0xFF5B5470);
            pie(c, x, 190, 64, 60, 180, 180, 0xFFFFC857);
            rr(c, x - 64, 186, 128, 12, 6, 0xFFE8A91F);
            ov(c, x, 215, 70, 14, al(0xFFFFF2B0, 120));
        }
        // menu board
        rr(c, W * .04f, 240, 330, 250, 16, 0xFFB07744);
        rr(c, W * .04f + 14, 254, 302, 222, 8, 0xFF37403F);
        text(c, "MENU", W * .04f + 165, 320, 54, al(0xFFFFFFFF, 235), Paint.Align.CENTER);
        for (int i = 0; i < 3; i++) {
            ln(c, W * .04f + 40, 366 + i * 40, W * .04f + 200, 366 + i * 40, 5, al(0xFFFFFFFF, 160));
            text(c, "" + (2 + i) + ".5", W * .04f + 260, 378 + i * 40, 32, al(0xFFFFE066, 235), Paint.Align.CENTER);
        }
        // window with awning
        window(c, W * .3f, 240, 260, 200, 0);
        for (int i = 0; i < 6; i++) poly(c, i % 2 == 0 ? 0xFFFF6F8F : 0xFFFFFFFF, W * .3f - 30 + i * 53, 200, W * .3f - 30 + (i + 1) * 53, 200, W * .3f - 30 + (i + 1) * 53, 250, W * .3f - 30 + i * 53, 250);
        // counter
        float cx = W * .62f, cw = W * .34f;
        rr(c, cx, 440, cw, 250, 14, 0xFFFF9A7A);
        rr(c, cx - 14, 420, cw + 28, 36, 14, 0xFFFFE9C9);
        for (int i = 0; i < 5; i++) rr(c, cx + 20 + i * (cw - 40) / 5, 480, (cw - 60) / 5, 190, 8, 0xFFFFB59C);
        // pastry case
        rr(c, cx + 20, 330, 220, 90, 14, al(0xFFD2F1FF, 200));
        for (int i = 0; i < 4; i++) { ci(c, cx + 52 + i * 50, 394, 18, 0xFFF3A649); ci(c, cx + 52 + i * 50, 384, 10, i % 2 == 0 ? 0xFFFF9EC4 : 0xFFFFFFFF); }
        // coffee machine
        rr(c, cx + cw - 190, 340, 150, 84, 12, 0xFFB4BFCD);
        rr(c, cx + cw - 190, 340, 150, 24, 12, 0xFF6B6480);
        rr(c, cx + cw - 140, 380, 50, 44, 6, 0xFF4B4660);
    }

    // ----------------------------------------------------------------- Park
    private static void park(Canvas c, float W, float H, float t) {
        grad(c, 0, 0, W, 620, 0xFF8ADBFF, 0xFFE3F8FF);
        Gfx.ol(0);
        // sun
        float sx = W * .84f;
        for (int i = 0; i < 12; i++) {
            c.save(); c.translate(sx, 150); c.rotate(i * 30 + t * 6);
            rr(c, -10, -126, 20, 40, 10, al(0xFFFFE066, 170));
            c.restore();
        }
        ci(c, sx, 150, 70, 0xFFFFE066);
        clouds(c, W, t, 90, 5, 0xFFFFFFFF);
        ov(c, W * .25f, 640, W * .5f, 170, 0xFF9DE28B);
        ov(c, W * .8f, 650, W * .45f, 150, 0xFF86D87A);
        grad(c, 0, 600, W, H - 600, 0xFF84D974, 0xFF5FC25B);
        // path
        poly(c, 0xFFF3E1B5, W * .46f, 640, W * .54f, 640, W * .78f, H, W * .22f, H);
        Gfx.ol(5);
        // fence
        for (float x = 10; x < W; x += 54) rr(c, x, 566, 30, 90, 10, 0xFFFFFFFF);
        rr(c, 0, 590, W, 14, 6, 0xFFF0EDE6); rr(c, 0, 628, W, 14, 6, 0xFFF0EDE6);
        Gfx.ol(0);
        // pond
        float px = W * .86f;
        ov(c, px, 850, 250, 86, 0xFFBDEBFF);
        ov(c, px, 850, 230, 74, 0xFF6EC8F5);
        ov(c, px - 60, 840, 70, 18, al(0xFFFFFFFF, 90));
        ov(c, px + 70, 872, 34, 14, 0xFF4FBF6B); ov(c, px - 20, 880, 28, 11, 0xFF5BD07A);
        ci(c, px + 74, 866, 7, 0xFFFF8FC0);
        Gfx.ol(5);
        for (int i = 0; i < 22; i++) {
            float fx = hash(i + 1) * W, fy = 700 + hash(i + 70) * 360;
            ci(c, fx, fy, 6, new int[]{0xFFFFFFFF, 0xFFFFE066, 0xFFFF8FC0}[i % 3]);
        }
    }

    // ----------------------------------------------------------------- Beach
    private static void beach(Canvas c, float W, float H, float t) {
        Gfx.ol(0);
        grad(c, 0, 0, W, 470, 0xFF7FD8FF, 0xFFFFF4D6);
        for (int i = 0; i < 12; i++) {
            c.save(); c.translate(W * .8f, 150); c.rotate(i * 30 + t * 5);
            rr(c, -9, -122, 18, 36, 9, al(0xFFFFE066, 150));
            c.restore();
        }
        ci(c, W * .8f, 150, 68, 0xFFFFE066);
        clouds(c, W, t, 80, 4, 0xFFFFFFFF);
        grad(c, 0, 470, W, 270, 0xFF4DC9E6, 0xFF2AA3D4);
        Gfx.ol(5);
        // boat
        float bx = W * .3f + (float) Math.sin(t * .6) * 24, by = 520 + (float) Math.sin(t * 1.4) * 4;
        poly(c, 0xFFFFFFFF, bx, by - 110, bx, by - 8, bx + 74, by - 8);
        poly(c, 0xFFFF6F8F, bx - 8, by - 90, bx - 8, by - 8, bx - 56, by - 8);
        poly(c, 0xFF8A5A30, bx - 70, by - 6, bx + 90, by - 6, bx + 66, by + 22, bx - 46, by + 22);
        for (int i = 0; i < 7; i++) {
            float wy = 560 + i * 36, off = (t * 30 + i * 97) % 220;
            for (float x = -200 + off; x < W; x += 220) arc(c, x, wy, 50, 10, 200, 140, 5, al(0xFFFFFFFF, 140));
        }
        Gfx.ol(0);
        // sand
        float wob = (float) Math.sin(t * 1.2) * 14;
        Path p = new Path();
        p.moveTo(0, 740 + wob);
        for (int i = 0; i <= 8; i++) p.quadTo(W * (i + .5f) / 8, 700 + (i % 2) * 50 + wob, W * (i + 1) / 8, 740 + wob);
        p.lineTo(W, H); p.lineTo(0, H); p.close();
        path(c, p, 0xFFE8C98A);
        float off = 36;
        Path p2 = new Path();
        p2.moveTo(0, 740 + wob + off);
        for (int i = 0; i <= 8; i++) p2.quadTo(W * (i + .5f) / 8, 700 + (i % 2) * 50 + wob + off, W * (i + 1) / 8, 740 + wob + off);
        p2.lineTo(W, H); p2.lineTo(0, H); p2.close();
        path(c, p2, 0xFFF7DFA4);
        for (int i = 0; i < 18; i++) {
            float fx = hash(i + 5) * W, fy = 860 + hash(i + 33) * 200;
            ci(c, fx, fy, 4 + hash(i) * 4, al(0xFFC9A257, 140));
        }
        // starfish
        c.save(); c.translate(W * .46f, 1030); for (int i = 0; i < 5; i++) { c.rotate(72); rr(c, -7, -34, 14, 34, 7, 0xFFFF8F5C); } c.restore();
    }

    // ----------------------------------------------------------------- Funfair
    private static void fair(Canvas c, float W, float H, float t) {
        Gfx.ol(0);
        grad(c, 0, 0, W, 330, 0xFF5E4AE3, 0xFFC864D8);
        grad(c, 0, 330, W, 440, 0xFFC864D8, 0xFFFFB27A);
        for (int i = 0; i < 36; i++) ci(c, hash(i) * W, hash(i + 50) * 300, 2 + hash(i + 7) * 3, al(0xFFFFFFFF, 120 + (int) (100 * Math.sin(t * 2 + i))));
        Gfx.ol(5);
        // ferris wheel
        float cx = W * .5f, cy = 400, r = 270;
        ln(c, cx, cy, cx - 150, 900, 18, 0xFFE8EAF6); ln(c, cx, cy, cx + 150, 900, 18, 0xFFE8EAF6);
        cis(c, cx, cy, r, 0xFFFFFFFF, 14);
        cis(c, cx, cy, r * .55f, al(0xFFFFFFFF, 200), 8);
        float rot = t * 12;
        for (int i = 0; i < 12; i++) {
            double a = Math.toRadians(i * 30 + rot);
            float px = cx + (float) Math.cos(a) * r, py = cy + (float) Math.sin(a) * r;
            ln(c, cx, cy, px, py, 6, al(0xFFFFFFFF, 220));
            ln(c, px, py, px, py + 30, 5, 0xFFE8EAF6);
            rr(c, px - 30, py + 28, 60, 52, 16, new int[]{0xFFFF5C73, 0xFFFFD43B, 0xFF4FB3FF, 0xFF7ED957}[i % 4]);
            rr(c, px - 22, py + 36, 44, 22, 8, al(0xFFFFFFFF, 190));
        }
        ci(c, cx, cy, 34, 0xFFFFD43B); ci(c, cx, cy, 16, 0xFFFF5C73);
        Gfx.ol(0);
        // ground
        grad(c, 0, 760, W, H - 760, 0xFFFFE0B8, 0xFFFFC98C);
        rect(c, 0, 756, W, 12, 0xFFB86BD0);
        Gfx.ol(5);
        // stalls
        for (int k = 0; k < 2; k++) {
            float sx = k == 0 ? W * .1f : W * .9f;
            rr(c, sx - 130, 570, 260, 190, 10, k == 0 ? 0xFFFF8FA3 : 0xFF6FC3FF);
            for (int i = 0; i < 6; i++) poly(c, i % 2 == 0 ? 0xFFFF5C73 : 0xFFFFFFFF, sx - 150 + i * 50, 520, sx - 100 + i * 50, 520, sx - 100 + i * 50, 590, sx - 150 + i * 50, 590);
            poly(c, 0xFFFFD43B, sx - 150, 520, sx + 150, 520, sx, 470);
            rr(c, sx - 100, 640, 200, 30, 8, 0xFFFFFFFF);
        }
        Gfx.ol(0);
        // string lights
        Path p = new Path();
        p.moveTo(0, 30); p.quadTo(W * .25f, 130, W * .5f, 40); p.quadTo(W * .75f, 130, W, 30);
        pathS(c, p, 0xFF3A2F5A, 4);
        for (int i = 0; i < 22; i++) {
            float u = i / 21f;
            float y = (1 - u) * (1 - u) * 0 + 0; // placeholder to keep arithmetic simple
            float lx = W * u;
            float ly = 30 + (float) Math.sin(u * Math.PI * 4) * -0 + 46 * (float) Math.abs(Math.sin(u * Math.PI * 2));
            ci(c, lx, ly + 8, 10, new int[]{0xFFFFE066, 0xFFFF8FA3, 0xFF7FD8FF}[i % 3]);
        }
    }

    // ================================================================== map icons
    /** Small vignette drawn in a 200x200 box centred on (cx, cy). */
    static void drawIcon(Canvas c, String id, float cx, float cy, float s, float t) {
        c.save();
        c.translate(cx, cy + 80 * s);
        c.scale(s, s);
        switch (id) {
            case "home":
                rr(c, -80, -110, 160, 110, 10, 0xFFFFEBC9);
                poly(c, 0xFFE0675A, -100, -104, 100, -104, 0, -190);
                rr(c, -20, -70, 40, 70, 8, 0xFF8A5A30);
                rr(c, -70, -90, 36, 36, 6, 0xFFA8E2FF); rr(c, 34, -90, 36, 36, 6, 0xFFA8E2FF);
                rr(c, 40, -176, 24, 44, 4, 0xFFC24E44);
                break;
            case "school":
                rr(c, -90, -100, 180, 100, 8, 0xFFFFD98A);
                poly(c, 0xFFE0675A, -100, -100, 100, -100, 0, -150);
                rr(c, -20, -60, 40, 60, 8, 0xFF8A5A30);
                for (int i = 0; i < 2; i++) { rr(c, -78 + i * 118, -80, 36, 36, 6, 0xFFA8E2FF); }
                rr(c, -3, -200, 6, 54, 3, 0xFF8A5A30); poly(c, 0xFFFF5C73, 3, -200, 44, -186, 3, -172);
                break;
            case "hospital":
                rr(c, -86, -130, 172, 130, 10, 0xFFFFFFFF);
                rr(c, -86, -130, 172, 24, 10, 0xFF6DD3C8);
                rr(c, -14, -108, 28, 80, 6, 0xFFFF5C73); rr(c, -40, -82, 80, 28, 6, 0xFFFF5C73);
                rr(c, -20, -34, 40, 34, 6, 0xFFBDE9FF);
                rr(c, -74, -90, 22, 22, 4, 0xFFA8E2FF); rr(c, 52, -90, 22, 22, 4, 0xFFA8E2FF);
                break;
            case "market":
                rr(c, -86, -110, 172, 110, 8, 0xFFFFF1CF);
                for (int i = 0; i < 6; i++) poly(c, i % 2 == 0 ? 0xFFFF5C73 : 0xFFFFFFFF, -96 + i * 32, -150, -64 + i * 32, -150, -64 + i * 32, -100, -96 + i * 32, -100);
                rr(c, -70, -80, 140, 60, 8, 0xFFA8E2FF);
                ci(c, -34, -46, 14, 0xFFFF4D5E); ci(c, 0, -46, 14, 0xFFFF9A3D); ci(c, 34, -46, 14, 0xFF7ED957);
                break;
            case "cafe":
                rr(c, -86, -110, 172, 110, 10, 0xFFE9B889);
                for (int i = 0; i < 6; i++) poly(c, i % 2 == 0 ? 0xFF6DD3C8 : 0xFFFFFFFF, -96 + i * 32, -140, -64 + i * 32, -140, -64 + i * 32, -100, -96 + i * 32, -100);
                rr(c, -26, -78, 52, 50, 12, 0xFFFFFFFF);
                ln(c, 26, -66, 40, -58, 7, 0xFFFFFFFF); ln(c, 40, -58, 26, -42, 7, 0xFFFFFFFF);
                for (int i = 0; i < 2; i++) ln(c, -8 + i * 16, -86, -14 + i * 16 + (float) Math.sin(t * 3 + i) * 3, -112, 4, al(0xFFFFFFFF, 220));
                break;
            case "park":
                rr(c, -12, -90, 24, 90, 8, 0xFF9B6B43);
                ci(c, 0, -130, 56, 0xFF4FBF6B); ci(c, -40, -100, 36, 0xFF57CB74); ci(c, 40, -100, 36, 0xFF45B561);
                ci(c, -18, -150, 8, 0xFFFF5C73); ci(c, 28, -120, 8, 0xFFFF5C73);
                rr(c, 56, -40, 70, 12, 5, 0xFFC98A44); rr(c, 60, -28, 8, 28, 3, 0xFF6B6480); rr(c, 112, -28, 8, 28, 3, 0xFF6B6480);
                break;
            case "beach":
                ov(c, 0, -10, 110, 22, 0xFFF7DFA4);
                rr(c, -4, -150, 8, 150, 3, 0xFFF3F0E8);
                for (int i = 0; i < 6; i++) pie(c, 0, -110, 78, 60, 180 + i * 30, 30, i % 2 == 0 ? 0xFFFF5C73 : 0xFFFFFFFF);
                for (int i = 0; i < 2; i++) arc(c, -60 + i * 70, 6, 40, 8, 200, 140, 5, 0xFF4DC9E6);
                break;
            default:
                cis(c, 0, -100, 70, 0xFFFFFFFF, 8);
                for (int i = 0; i < 6; i++) {
                    double a = Math.toRadians(i * 60 + t * 20);
                    ln(c, 0, -100, (float) Math.cos(a) * 70, -100 + (float) Math.sin(a) * 70, 4, 0xFFFFFFFF);
                    ci(c, (float) Math.cos(a) * 70, -100 + (float) Math.sin(a) * 70, 11, new int[]{0xFFFF5C73, 0xFFFFD43B, 0xFF4FB3FF}[i % 3]);
                }
                ln(c, 0, -100, -46, 0, 10, 0xFFE8EAF6); ln(c, 0, -100, 46, 0, 10, 0xFFE8EAF6);
                ci(c, 0, -100, 10, 0xFFFFD43B);
                break;
        }
        c.restore();
    }
}
