package com.cocopopo.app;

import android.graphics.Canvas;
import android.graphics.Path;

import java.util.ArrayList;
import java.util.List;

import static com.cocopopo.app.Gfx.*;

/** Catalog of draggable props plus their vector artwork. Local origin = bottom centre. */
final class PropArt {
    static final String[] CATS = {"Home", "Food", "Toys", "Outdoors", "Stuff"};

    /** cat|id|name|width|height */
    private static final String[] DEFS = {
        "0|bed|Bed|370|240", "0|sofa|Sofa|350|190", "0|armchair|Armchair|200|190", "0|table|Table|310|160",
        "0|chair|Chair|120|190", "0|shelf|Bookshelf|230|350", "0|lamp|Lamp|100|310", "0|tv|TV|270|230",
        "0|fridge|Fridge|180|350", "0|tub|Bathtub|350|200", "0|toilet|Toilet|130|170", "0|dresser|Dresser|240|210",
        "0|stove|Stove|200|230", "0|desk|School desk|240|180", "0|plant|Plant|130|210", "0|rug|Rug|380|110",
        "1|cake|Cake|150|150", "1|pizza|Pizza|140|110", "1|burger|Burger|130|120", "1|icecream|Ice cream|90|170",
        "1|donut|Donut|120|90", "1|apple|Apple|90|100", "1|juice|Juice|80|130", "1|cupcake|Cupcake|100|120",
        "2|ball|Ball|110|110", "2|teddy|Teddy|140|180", "2|balloon|Balloon|100|290", "2|car|Toy car|210|110",
        "2|blocks|Blocks|160|140", "2|guitar|Guitar|110|250", "2|duck|Duck|120|110", "2|rocket|Rocket|110|250",
        "3|tree|Tree|290|450", "3|palm|Palm|270|450", "3|flower|Flower|90|160", "3|bush|Bush|240|140",
        "3|mushroom|Mushroom|110|120", "3|rock|Rock|160|110", "3|umbrella|Umbrella|280|300", "3|sandcastle|Sand castle|190|160",
        "3|swing|Swing|320|340", "3|slide|Slide|320|330",
        "4|backpack|Backpack|120|150", "4|books|Books|140|90", "4|globe|Globe|120|160", "4|bench|Bench|320|180",
        "4|frame|Picture|130|160", "4|gift|Gift|130|130", "4|trophy|Trophy|120|170", "4|camera|Camera|130|100",
        "0|hbed|Hospital bed|380|240", "4|ivstand|IV stand|110|350", "4|crate|Fruit crate|180|120", "4|register|Register|200|180", "1|coffee|Coffee|80|90", "3|surfboard|Surf board|110|300", "1|popcorn|Popcorn|110|160", "1|cotton|Cotton candy|100|210", "4|medkit|Med kit|130|110", "4|cart|Cart|200|190", "4|wheelchair|Wheelchair|170|210", "4|clock|Clock|120|120",
    };

    static final class Def {
        int cat; String id, name; float w, h;
    }

    static final List<Def> ALL = new ArrayList<Def>();

    static {
        for (String s : DEFS) {
            String[] p = s.split("\\|");
            Def d = new Def();
            d.cat = Integer.parseInt(p[0]); d.id = p[1]; d.name = p[2];
            d.w = Float.parseFloat(p[3]); d.h = Float.parseFloat(p[4]);
            ALL.add(d);
        }
    }

    static Def get(String id) {
        for (Def d : ALL) if (d.id.equals(id)) return d;
        return ALL.get(0);
    }

    /** Flat things (rugs) are drawn underneath everything else. */
    static boolean flat(String id) { return id.equals("rug"); }

    private static final int WOOD = 0xFFD39A62, WOOD_D = 0xFFB07744, WHITE = 0xFFFFFFFF, INK = 0xFF2B2230;
    private static final Path pp = new Path();

    private PropArt() {}

    static void draw(Canvas c, String id, float t) {
        switch (id) {
            case "bed":
                rr(c, -176, -70, 24, 70, 8, WOOD_D); rr(c, 150, -70, 24, 70, 8, WOOD_D);
                rr(c, -186, -236, 34, 200, 14, WOOD);
                rr(c, -170, -112, 346, 52, 14, WOOD);
                rr(c, -156, -148, 332, 56, 22, 0xFFF4F1FF);
                rr(c, -40, -156, 218, 70, 26, 0xFF8E9BFF);
                rr(c, -40, -156, 218, 20, 10, 0xFFA9B4FF);
                for (int i = 0; i < 5; i++) ci(c, -10 + i * 42, -112, 6, 0xFFC8CEFF);
                rr(c, -148, -176, 100, 48, 24, WHITE);
                rr(c, -148, -150, 100, 22, 11, 0xFFE6E8F5);
                break;
            case "sofa":
                rr(c, -170, -190, 340, 126, 44, 0xFFFF8FA3);
                rr(c, -162, -112, 324, 70, 26, 0xFFFFA9B8);
                rr(c, -176, -140, 54, 114, 26, 0xFFE87389);
                rr(c, 122, -140, 54, 114, 26, 0xFFE87389);
                rr(c, -110, -150, 106, 56, 22, 0xFFFFC1CB);
                rr(c, 4, -150, 106, 56, 22, 0xFFFFC1CB);
                rr(c, -140, -26, 20, 26, 6, WOOD_D); rr(c, 120, -26, 20, 26, 6, WOOD_D);
                break;
            case "armchair":
                rr(c, -96, -190, 192, 130, 46, 0xFF7ED3C0);
                rr(c, -86, -108, 172, 66, 24, 0xFF9BE2D2);
                rr(c, -100, -136, 44, 108, 22, 0xFF55B8A4);
                rr(c, 56, -136, 44, 108, 22, 0xFF55B8A4);
                rr(c, -72, -28, 16, 28, 5, WOOD_D); rr(c, 56, -28, 16, 28, 5, WOOD_D);
                break;
            case "table":
                rr(c, -140, -130, 22, 130, 6, WOOD_D); rr(c, 118, -130, 22, 130, 6, WOOD_D);
                rr(c, -155, -156, 310, 34, 14, WOOD);
                rr(c, -155, -138, 310, 12, 6, WOOD_D);
                // cloth + vase
                rr(c, -100, -168, 200, 14, 7, 0xFFFFE9A8);
                rr(c, -14, -204, 28, 38, 10, 0xFF6FC3FF);
                ci(c, 0, -218, 12, 0xFFFF6F8F); ci(c, -12, -210, 9, 0xFFFFC93C); ci(c, 12, -210, 9, 0xFFFF9A3D);
                break;
            case "chair":
                rr(c, -48, -190, 20, 110, 9, WOOD);
                rr(c, -54, -96, 108, 24, 10, WOOD);
                rr(c, -48, -76, 16, 76, 5, WOOD_D); rr(c, 32, -76, 16, 76, 5, WOOD_D);
                rr(c, -44, -170, 14, 14, 5, WOOD_D);
                break;
            case "shelf":
                rr(c, -112, -350, 224, 350, 14, WOOD);
                rr(c, -98, -336, 196, 322, 8, 0xFFB98350);
                int[] bc = {0xFFFF6F8F, 0xFF6FC3FF, 0xFFFFC93C, 0xFF7ED957, 0xFFB67CFF, 0xFFFF9A3D};
                for (int r = 0; r < 4; r++) {
                    float y = -336 + (r + 1) * 80;
                    rr(c, -102, y - 6, 204, 12, 4, WOOD);
                    float x = -92;
                    for (int i = 0; i < 6; i++) {
                        float bw = 14 + ((i * 7 + r * 5) % 4) * 4, bh = 38 + ((i * 5 + r * 3) % 4) * 8;
                        if (r == 2 && i >= 3) { ci(c, x + 20, y - 24, 18, 0xFFFFFFFF); break; }
                        rr(c, x, y - 6 - bh, bw, bh, 3, bc[(i + r) % 6]);
                        x += bw + 3;
                    }
                }
                break;
            case "lamp":
                ov(c, 0, -6, 38, 10, 0xFF5B5470);
                rr(c, -5, -250, 10, 244, 4, 0xFF6B6480);
                poly(c, 0xFFFFD86B, -50, -250, 50, -250, 34, -310, -34, -310);
                rr(c, -50, -258, 100, 12, 6, 0xFFFFB93D);
                ov(c, 0, -240, 40, 8, al(0xFFFFF2B0, 110));
                break;
            case "tv":
                rr(c, -120, -76, 240, 76, 12, WOOD);
                rr(c, -100, -62, 90, 22, 6, WOOD_D); rr(c, 10, -62, 90, 22, 6, WOOD_D);
                rr(c, -112, -226, 224, 150, 16, 0xFF2B2735);
                rr(c, -102, -216, 204, 130, 10, 0xFF8FD8FF);
                ci(c, 50, -170, 20, 0xFFFFE066);
                poly(c, 0xFF7ED957, -102, -86, -30, -150, 20, -86);
                poly(c, 0xFF5BBF4C, -40, -86, 30, -136, 102, -86);
                rr(c, -20, -80, 40, 6, 3, 0xFF4B4660);
                break;
            case "fridge":
                rr(c, -88, -350, 176, 350, 20, 0xFFE3F4FF);
                rr(c, -88, -350, 176, 350, 20, 0xFFE3F4FF);
                ln(c, -84, -240, 84, -240, 6, 0xFFB7D6EA);
                rr(c, 56, -310, 12, 56, 6, 0xFF8AA6BC); rr(c, 56, -226, 12, 80, 6, 0xFF8AA6BC);
                rr(c, -60, -300, 40, 40, 6, 0xFFFFD86B); rr(c, -52, -170, 30, 30, 6, 0xFFFF8FA3);
                rr(c, -80, -12, 40, 12, 4, 0xFFB7D6EA); rr(c, 40, -12, 40, 12, 4, 0xFFB7D6EA);
                break;
            case "tub":
                rr(c, -160, -26, 26, 26, 8, 0xFFB0BED0); rr(c, 134, -26, 26, 26, 8, 0xFFB0BED0);
                rr(c, -176, -120, 352, 104, 50, 0xFFC3E8FA);
                rr(c, -164, -124, 328, 26, 13, 0xFF8FD3F2);
                rr(c, 130, -190, 14, 76, 7, 0xFFB0BED0);
                rr(c, 100, -196, 56, 14, 7, 0xFFB0BED0);
                for (int i = 0; i < 6; i++) ci(c, -120 + i * 38, -128 - (i % 3) * 8, 14 - (i % 2) * 4, al(WHITE, 235));
                break;
            case "toilet":
                rr(c, -42, -170, 84, 70, 16, 0xFFE6EEF7);
                rr(c, -44, -108, 118, 30, 14, 0xFFF4F8FC);
                rr(c, -34, -84, 84, 50, 22, 0xFFE6EEF7);
                rr(c, -20, -34, 60, 34, 10, 0xFFDCE6F0);
                rr(c, -30, -160, 20, 12, 6, 0xFFB0BED0);
                break;
            case "dresser":
                rr(c, -120, -200, 240, 190, 16, WOOD);
                rr(c, -110, -190, 220, 80, 10, 0xFFE0AC78);
                rr(c, -110, -102, 220, 80, 10, 0xFFE0AC78);
                ci(c, 0, -150, 8, 0xFF8A5A30); ci(c, 0, -62, 8, 0xFF8A5A30);
                rr(c, -104, -12, 22, 12, 4, WOOD_D); rr(c, 82, -12, 22, 12, 4, WOOD_D);
                break;
            case "stove":
                rr(c, -100, -220, 200, 220, 16, 0xFFDDE4EE);
                rr(c, -100, -220, 200, 30, 12, 0xFFB4BFCD);
                rr(c, -82, -150, 164, 110, 12, 0xFF4B4660);
                rr(c, -70, -138, 140, 84, 8, 0xFF8FA0B8);
                for (int i = 0; i < 3; i++) ci(c, -50 + i * 50, -180, 9, 0xFF6B6480);
                rr(c, -82, -228, 70, 14, 7, 0xFFFFFFFF);
                break;
            case "desk":
                rr(c, -100, -130, 14, 130, 5, 0xFF8A93A8); rr(c, 86, -130, 14, 130, 5, 0xFF8A93A8);
                rr(c, -120, -150, 240, 26, 10, 0xFFFFC66B);
                rr(c, -120, -134, 240, 10, 5, 0xFFE0A24A);
                rr(c, -70, -176, 60, 28, 4, 0xFF6FC3FF); rr(c, -70, -176, 60, 8, 4, 0xFF4B9FE0);
                rr(c, 20, -166, 50, 10, 4, 0xFFFF6F8F);
                break;
            case "plant":
                for (int i = -2; i <= 2; i++) {
                    c.save(); c.translate(0, -90); c.rotate(i * 26);
                    ov(c, 0, -50, 20, 54, i % 2 == 0 ? 0xFF4FBF6B : 0xFF6FD98A);
                    c.restore();
                }
                poly(c, 0xFFE8765E, -42, -92, 42, -92, 32, 0, -32, 0);
                rr(c, -46, -102, 92, 20, 8, 0xFFF0907A);
                break;
            case "rug":
                ov(c, 0, -55, 190, 55, 0xFFFFB3C6);
                ov(c, 0, -55, 160, 42, 0xFFFFD6E0);
                ov(c, 0, -55, 120, 30, 0xFFFFB3C6);
                ov(c, 0, -55, 80, 18, 0xFFFFE9EF);
                break;

            // ---------------- food
            case "cake":
                ov(c, 0, -10, 70, 12, 0xFFFFFFFF);
                rr(c, -56, -70, 112, 58, 14, 0xFFFFC1D6);
                rr(c, -56, -70, 112, 18, 9, 0xFFFFFFFF);
                for (int i = 0; i < 4; i++) rr(c, -44 + i * 26, -64, 14, 30, 7, 0xFFFFFFFF);
                rr(c, -3, -112, 6, 40, 3, 0xFF6FC3FF);
                ov(c, 0, -122, 6, 11, 0xFFFFB93D);
                ci(c, -34, -76, 7, 0xFFFF4D6D); ci(c, 34, -76, 7, 0xFFFF4D6D);
                break;
            case "pizza":
                ov(c, 0, -52, 62, 50, 0xFFFFC866);
                ov(c, 0, -52, 52, 42, 0xFFFF9B54);
                ov(c, 0, -52, 46, 36, 0xFFFFD27A);
                ci(c, -20, -62, 9, 0xFFE8454F); ci(c, 18, -66, 9, 0xFFE8454F); ci(c, 4, -42, 9, 0xFFE8454F); ci(c, -26, -42, 7, 0xFF58B368);
                poly(c, 0xFFFFEAB3, 0, -52, 60, -80, 60, -24);
                break;
            case "burger":
                ov(c, 0, -80, 56, 34, 0xFFF3A649);
                ci(c, -20, -92, 3, 0xFFFFF3C4); ci(c, 10, -98, 3, 0xFFFFF3C4); ci(c, 28, -88, 3, 0xFFFFF3C4);
                rr(c, -60, -62, 120, 16, 8, 0xFF5BBF4C);
                rr(c, -56, -50, 112, 12, 6, 0xFFFF5C5C);
                rr(c, -58, -42, 116, 22, 11, 0xFF7B4B2E);
                rr(c, -56, -24, 112, 22, 11, 0xFFF3A649);
                break;
            case "icecream":
                poly(c, 0xFFE9B36C, -34, -90, 34, -90, 0, 0);
                for (int i = 0; i < 3; i++) ln(c, -22 + i * 18, -84, -4 + i * 6, -30, 3, 0xFFC98A44);
                ci(c, 0, -104, 36, 0xFFFF9EC4);
                ci(c, 0, -142, 30, 0xFF9AE3FF);
                ci(c, 6, -168, 7, 0xFFFF4D6D);
                break;
            case "donut":
                ov(c, 0, -42, 56, 40, 0xFFE9B36C);
                ov(c, 0, -48, 56, 38, 0xFFFF8FB5);
                ov(c, 0, -48, 18, 12, 0xFFFFF0F5);
                for (int i = 0; i < 6; i++) rr(c, -40 + i * 15, -70 + (i % 3) * 14, 10, 4, 2, i % 2 == 0 ? 0xFFFFFFFF : 0xFF6FC3FF);
                break;
            case "apple":
                ci(c, -18, -42, 34, 0xFFFF4D5E); ci(c, 18, -42, 34, 0xFFFF4D5E);
                rr(c, -3, -92, 6, 22, 3, 0xFF7B4B2E);
                ov(c, 18, -86, 18, 8, 0xFF5BBF4C);
                ov(c, -22, -54, 7, 12, al(WHITE, 110));
                break;
            case "juice":
                rr(c, -28, -108, 56, 106, 10, 0xFFFFA23D);
                rr(c, -28, -108, 56, 30, 10, 0xFFFFE066);
                ci(c, 0, -46, 16, 0xFFFF7A1F);
                ln(c, 12, -108, 26, -128, 6, 0xFFFF4D6D);
                break;
            case "cupcake":
                ci(c, 0, -62, 34, 0xFFFF9EC4);
                ci(c, 0, -90, 24, 0xFFFFC1D8);
                ci(c, 0, -112, 8, 0xFFFF4D5E);
                poly(c, 0xFFE9B36C, -38, -52, 38, -52, 28, 0, -28, 0);
                for (int i = -1; i <= 1; i++) ln(c, i * 14, -48, i * 10, -6, 3, 0xFFC98A44);
                break;

            // ---------------- toys
            case "ball":
                ci(c, 0, -52, 50, WHITE);
                for (int i = 0; i < 6; i++) pie(c, 0, -52, 50, 50, i * 60, 30, i % 2 == 0 ? 0xFFFF5C73 : 0xFFFFD43B);
                ci(c, 0, -52, 9, WHITE);
                break;
            case "teddy":
                ci(c, -34, -150, 18, 0xFFB57B4E); ci(c, 34, -150, 18, 0xFFB57B4E);
                ci(c, -34, -150, 9, 0xFFE8B48A); ci(c, 34, -150, 9, 0xFFE8B48A);
                ov(c, 0, -60, 48, 56, 0xFFC68A5A);
                ov(c, 0, -52, 30, 34, 0xFFE8B48A);
                ci(c, -42, -20, 20, 0xFFC68A5A); ci(c, 42, -20, 20, 0xFFC68A5A);
                ci(c, -52, -80, 17, 0xFFC68A5A); ci(c, 52, -80, 17, 0xFFC68A5A);
                ov(c, 0, -122, 44, 38, 0xFFC68A5A);
                ov(c, 0, -112, 20, 15, 0xFFE8B48A);
                ci(c, -16, -128, 5, INK); ci(c, 16, -128, 5, INK);
                ov(c, 0, -116, 6, 4, INK);
                poly(c, 0xFFFF5C73, -20, -86, 0, -78, -20, -70);
                poly(c, 0xFFFF5C73, 20, -86, 0, -78, 20, -70);
                break;
            case "balloon":
                path(c, balloonPath(), 0xFFFF5C73);
                ov(c, -18, -226, 8, 16, al(WHITE, 130));
                poly(c, 0xFFE8465E, -7, -126, 7, -126, 0, -138);
                pp.reset(); pp.moveTo(0, -126); pp.quadTo(-26, -90, 8, -56); pp.quadTo(30, -26, 0, 0);
                pathS(c, pp, 0xFF8C93A8, 3);
                break;
            case "car":
                rr(c, -100, -62, 200, 40, 16, 0xFFFF6F61);
                rr(c, -58, -100, 112, 46, 20, 0xFFFF6F61);
                rr(c, -46, -92, 40, 30, 10, 0xFFCDEBFF); rr(c, 2, -92, 40, 30, 10, 0xFFCDEBFF);
                ci(c, -56, -22, 22, INK); ci(c, 56, -22, 22, INK);
                ci(c, -56, -22, 10, 0xFFDDE4EE); ci(c, 56, -22, 10, 0xFFDDE4EE);
                ci(c, 94, -44, 6, 0xFFFFE066);
                break;
            case "blocks":
                rr(c, -66, -62, 62, 62, 8, 0xFFFF5C73);
                rr(c, 4, -62, 62, 62, 8, 0xFF4FB3FF);
                rr(c, -32, -124, 62, 62, 8, 0xFFFFD43B);
                text(c, "A", -35, -22, 40, WHITE, android.graphics.Paint.Align.CENTER);
                text(c, "B", 35, -22, 40, WHITE, android.graphics.Paint.Align.CENTER);
                text(c, "C", -1, -82, 40, WHITE, android.graphics.Paint.Align.CENTER);
                break;
            case "guitar":
                rr(c, -8, -250, 16, 130, 5, 0xFF8A5A30);
                rr(c, -14, -266, 28, 28, 8, 0xFF5B3A1E);
                ci(c, 0, -70, 44, 0xFFE8A24A); ci(c, 0, -122, 32, 0xFFE8A24A);
                ci(c, 0, -90, 15, 0xFF5B3A1E);
                rr(c, -20, -62, 40, 8, 4, 0xFF8A5A30);
                for (int i = -1; i <= 1; i++) ln(c, i * 3, -250, i * 3, -64, 1.5f, 0xFFFFF3C4);
                break;
            case "duck":
                ov(c, 0, -40, 48, 36, 0xFFFFD43B);
                ci(c, 24, -84, 26, 0xFFFFD43B);
                ov(c, 52, -76, 16, 8, 0xFFFF9A3D);
                ci(c, 30, -92, 4, INK);
                ov(c, -8, -38, 24, 16, 0xFFF3BE1F);
                poly(c, 0xFFFFD43B, -50, -50, -66, -70, -34, -56);
                break;
            case "rocket":
                poly(c, 0xFFFF5C73, -46, -90, -86, -30, -40, -50);
                poly(c, 0xFFFF5C73, 46, -90, 86, -30, 40, -50);
                ov(c, 0, -130, 44, 110, 0xFFF0F4FA);
                path(c, rocketNose(), 0xFFFF5C73);
                ci(c, 0, -150, 22, 0xFF4B4660); ci(c, 0, -150, 16, 0xFF8FD8FF);
                rr(c, -44, -64, 88, 16, 8, 0xFFFF5C73);
                poly(c, 0xFFFFB93D, -26, -26, 26, -26, 0, 4);
                poly(c, 0xFFFFE066, -14, -26, 14, -26, 0, -8);
                break;

            // ---------------- outdoors
            case "tree":
                rr(c, -24, -170, 48, 170, 14, 0xFF9B6B43);
                rr(c, -24, -100, 14, 100, 7, 0xFF86582F);
                ci(c, 0, -320, 100, 0xFF4FBF6B);
                ci(c, -84, -250, 68, 0xFF57CB74); ci(c, 84, -250, 68, 0xFF45B561);
                ci(c, -30, -380, 62, 0xFF66DA82); ci(c, 60, -340, 54, 0xFF57CB74);
                ci(c, -40, -230, 11, 0xFFFF5C73); ci(c, 50, -290, 11, 0xFFFF5C73); ci(c, 0, -360, 11, 0xFFFF5C73);
                break;
            case "palm": {
                pp.reset(); pp.moveTo(-18, 0); pp.quadTo(-6, -220, 34, -330); pp.lineTo(60, -322); pp.quadTo(24, -210, 22, 0); pp.close();
                path(c, pp, 0xFFC29363);
                for (int i = 0; i < 6; i++) ln(c, -4 + i * 0, -40 - i * 44, 24 + (i % 2) * 2, -40 - i * 44 + 8, 6, 0xFFA87A4E);
                for (int i = 0; i < 6; i++) {
                    c.save(); c.translate(46, -332); c.rotate(-150 + i * 60);
                    pp.reset(); pp.moveTo(0, 0); pp.quadTo(60, -50, 130, 10); pp.quadTo(70, -14, 0, 14); pp.close();
                    path(c, pp, i % 2 == 0 ? 0xFF3FAF5F : 0xFF57C77A);
                    c.restore();
                }
                ci(c, 36, -318, 14, 0xFF8A5A30); ci(c, 58, -312, 14, 0xFF8A5A30);
                break;
            }
            case "flower":
                rr(c, -4, -110, 8, 110, 4, 0xFF4FBF6B);
                ov(c, 18, -40, 22, 9, 0xFF4FBF6B);
                for (int i = 0; i < 6; i++) {
                    c.save(); c.translate(0, -126); c.rotate(i * 60);
                    ov(c, 0, -22, 14, 22, 0xFFFF8FC0);
                    c.restore();
                }
                ci(c, 0, -126, 14, 0xFFFFD43B);
                break;
            case "bush":
                ci(c, -70, -56, 56, 0xFF4FBF6B); ci(c, 70, -56, 56, 0xFF4FBF6B);
                ci(c, 0, -76, 64, 0xFF5BD07A); ci(c, 0, -50, 54, 0xFF4FBF6B);
                ci(c, -30, -90, 8, 0xFFFF8FA3); ci(c, 44, -70, 8, 0xFFFF8FA3); ci(c, -70, -50, 8, 0xFFFFD43B);
                break;
            case "mushroom":
                rr(c, -22, -62, 44, 62, 16, 0xFFFFF1DC);
                pie(c, 0, -62, 52, 52, 180, 180, 0xFFFF5C5C);
                ci(c, -24, -84, 8, WHITE); ci(c, 14, -98, 9, WHITE); ci(c, 30, -76, 6, WHITE);
                break;
            case "rock":
                ov(c, 0, -40, 78, 40, 0xFFA7B0C0);
                ov(c, 22, -56, 42, 28, 0xFFBFC7D5);
                ov(c, -30, -22, 30, 14, 0xFF8E98AB);
                break;
            case "umbrella":
                rr(c, -5, -280, 10, 280, 4, 0xFFF3F0E8);
                for (int i = 0; i < 6; i++) pie(c, 0, -216, 140, 90, 180 + i * 30, 30, i % 2 == 0 ? 0xFFFF5C73 : 0xFFFFFFFF);
                ci(c, 0, -300, 8, 0xFFF3F0E8);
                break;
            case "sandcastle":
                rr(c, -80, -70, 160, 70, 8, 0xFFF3D493);
                rr(c, -80, -100, 36, 36, 4, 0xFFF3D493); rr(c, -18, -100, 36, 36, 4, 0xFFF3D493); rr(c, 44, -100, 36, 36, 4, 0xFFF3D493);
                rr(c, -48, -128, 96, 66, 8, 0xFFEAC77A);
                for (int i = 0; i < 3; i++) rr(c, -48 + i * 36, -142, 24, 18, 3, 0xFFEAC77A);
                rr(c, -14, -50, 28, 50, 14, 0xFFC9A257);
                ln(c, 0, -142, 0, -170, 3, 0xFF8A5A30);
                poly(c, 0xFFFF5C73, 0, -170, 28, -160, 0, -150);
                break;
            case "swing":
                ln(c, -140, 0, -80, -330, 12, 0xFFC98A44); ln(c, -20, 0, -80, -330, 12, 0xFFC98A44);
                ln(c, 140, 0, 80, -330, 12, 0xFFC98A44); ln(c, 20, 0, 80, -330, 12, 0xFFC98A44);
                ln(c, -80, -330, 80, -330, 14, 0xFF86582F);
                float sx = (float) Math.sin(t * 2) * 10;
                ln(c, -40, -326, -40 + sx, -90, 4, 0xFF6B6480); ln(c, 40, -326, 40 + sx, -90, 4, 0xFF6B6480);
                rr(c, -56 + sx, -96, 112, 20, 10, 0xFFFF6F8F);
                break;
            case "slide":
                rr(c, -150, -300, 14, 300, 6, 0xFF8A93A8); rr(c, -80, -300, 14, 300, 6, 0xFF8A93A8);
                rr(c, -164, -300, 100, 16, 6, 0xFFFFC66B);
                pp.reset(); pp.moveTo(-70, -290); pp.quadTo(60, -190, 150, -30); pp.lineTo(150, 0); pp.lineTo(120, 0); pp.quadTo(30, -150, -70, -250); pp.close();
                path(c, pp, 0xFF4FB3FF);
                rr(c, -164, -340, 8, 60, 4, 0xFF8A93A8);
                break;

            // ---------------- stuff
            case "backpack":
                rr(c, -48, -140, 96, 140, 30, 0xFF4FB3FF);
                rr(c, -34, -64, 68, 48, 14, 0xFF2E8FDB);
                rr(c, -20, -168, 40, 36, 14, 0xFF2E8FDB);
                rr(c, -50, -120, 12, 60, 6, 0xFF2E8FDB);
                break;
            case "books":
                rr(c, -66, -26, 132, 26, 6, 0xFFFF6F8F);
                rr(c, -56, -52, 112, 26, 6, 0xFF6FC3FF);
                rr(c, -62, -78, 124, 26, 6, 0xFFFFC93C);
                for (int i = 0; i < 3; i++) rr(c, -52, -22 - i * 26, 100, 5, 2, al(WHITE, 190));
                break;
            case "globe":
                rr(c, -30, -12, 60, 12, 6, 0xFF8A5A30);
                rr(c, -4, -42, 8, 32, 3, 0xFF8A5A30);
                ci(c, 0, -100, 54, 0xFF4FB3FF);
                ov(c, -16, -112, 20, 14, 0xFF7ED957); ov(c, 22, -86, 14, 22, 0xFF7ED957); ov(c, -6, -76, 10, 8, 0xFF7ED957);
                arc(c, 0, -100, 62, 62, 120, 100, 5, 0xFFFFC93C);
                break;
            case "bench":
                rr(c, -150, -94, 300, 22, 8, 0xFFC98A44);
                rr(c, -150, -170, 300, 22, 8, 0xFFC98A44);
                rr(c, -150, -138, 300, 22, 8, 0xFFD39A55);
                rr(c, -130, -170, 18, 170, 6, 0xFF6B6480); rr(c, 112, -170, 18, 170, 6, 0xFF6B6480);
                break;
            case "frame":
                rr(c, -62, -150, 124, 150, 10, 0xFFC98A44);
                rr(c, -50, -138, 100, 126, 6, 0xFFBDE9FF);
                poly(c, 0xFF7ED957, -50, -12, -10, -90, 24, -50, 50, -80, 50, -12);
                ci(c, 24, -112, 12, 0xFFFFE066);
                break;
            case "gift":
                rr(c, -56, -90, 112, 90, 8, 0xFFB67CFF);
                rr(c, -62, -112, 124, 30, 8, 0xFF9B5CF0);
                rr(c, -10, -112, 20, 112, 3, 0xFFFFD43B);
                ci(c, -18, -126, 16, 0xFFFFD43B); ci(c, 18, -126, 16, 0xFFFFD43B); ci(c, 0, -118, 9, 0xFFE0B020);
                break;
            case "trophy":
                rr(c, -36, -22, 72, 22, 6, 0xFF8A5A30);
                rr(c, -8, -60, 16, 40, 4, 0xFFFFC93C);
                path(c, trophyCup(), 0xFFFFC93C);
                cis(c, -44, -112, 14, 0xFFFFC93C, 8); cis(c, 44, -112, 14, 0xFFFFC93C, 8);
                ci(c, 0, -112, 12, 0xFFFFE680);
                break;
            case "camera":
                rr(c, -56, -76, 112, 70, 14, 0xFF4B4660);
                rr(c, -56, -76, 112, 24, 12, 0xFFFF6F8F);
                ci(c, 0, -38, 24, 0xFF2B2735); ci(c, 0, -38, 15, 0xFF6FC3FF); ci(c, -5, -43, 4, WHITE);
                rr(c, -20, -90, 40, 16, 6, 0xFF4B4660);
                break;
            case "hbed":
                ln(c, -150, -62, -150, 0, 9, 0xFFB0BED0); ln(c, 150, -62, 150, 0, 9, 0xFFB0BED0);
                ci(c, -150, -4, 9, 0xFF6B6480); ci(c, 150, -4, 9, 0xFF6B6480);
                rr(c, -176, -190, 14, 130, 7, 0xFFB0BED0);
                rr(c, -170, -110, 340, 36, 10, 0xFFB0BED0);
                rr(c, -164, -140, 330, 44, 18, 0xFFFFFFFF);
                rr(c, -20, -150, 188, 60, 22, 0xFF6DD3C8);
                rr(c, -20, -150, 188, 16, 8, 0xFF8FE6DC);
                rr(c, -150, -170, 90, 40, 20, 0xFFEAF4FB);
                break;
            case "ivstand":
                ln(c, 0, 0, 0, -330, 7, 0xFFB0BED0);
                ln(c, -40, -6, 40, -6, 8, 0xFFB0BED0);
                ci(c, -40, -4, 7, 0xFF6B6480); ci(c, 40, -4, 7, 0xFF6B6480);
                rr(c, -26, -330, 52, 14, 7, 0xFFB0BED0);
                rr(c, -22, -312, 44, 70, 14, al(0xFFBDE9FF, 230));
                rr(c, -22, -270, 44, 28, 14, 0xFF7FD0F5);
                pp.reset(); pp.moveTo(0, -242); pp.quadTo(-22, -190, 6, -150); pp.quadTo(24, -120, 0, -100);
                pathS(c, pp, 0xFF9AA6BC, 3);
                break;
            case "crate":
                rr(c, -86, -70, 172, 70, 10, 0xFFD39A62);
                ci(c, -50, -78, 24, 0xFFFF4D5E); ci(c, 0, -84, 26, 0xFFFF9A3D); ci(c, 50, -78, 24, 0xFF7ED957); ci(c, -24, -100, 22, 0xFFFFD43B); ci(c, 28, -102, 22, 0xFFFF4D5E);
                rr(c, -90, -50, 180, 50, 8, 0xFFC08450);
                ln(c, -90, -26, 90, -26, 4, 0xFFA8693A);
                break;
            case "register":
                rr(c, -100, -110, 200, 110, 14, 0xFF7BC4F5);
                rr(c, -104, -122, 208, 22, 10, 0xFFFFFFFF);
                rr(c, -30, -172, 78, 56, 10, 0xFF4B4660);
                rr(c, -22, -164, 62, 26, 5, 0xFF8FE6B0);
                rr(c, -40, -130, 100, 18, 6, 0xFF6B6480);
                ci(c, -70, -140, 16, 0xFFFF6F8F);
                break;
            case "coffee":
                rr(c, -30, -52, 60, 52, 14, WHITE);
                ln(c, 30, -40, 44, -34, 8, WHITE); ln(c, 44, -34, 30, -18, 8, WHITE);
                ov(c, 0, -52, 30, 8, 0xFF8A5A30);
                pp.reset(); pp.moveTo(-8, -64); pp.quadTo(-18, -76, -6, -86); pp.moveTo(8, -64); pp.quadTo(-2, -76, 10, -90);
                pathS(c, pp, 0xFFB0BED0, 4);
                ov(c, 0, -2, 44, 8, 0xFFE0E8F2);
                break;
            case "surfboard":
                pp.reset(); pp.moveTo(0, -300); pp.cubicTo(60, -230, 60, -60, 0, -4); pp.cubicTo(-60, -60, -60, -230, 0, -300); pp.close();
                path(c, pp, 0xFFFF6F8F);
                ln(c, 0, -290, 0, -14, 7, WHITE);
                ln(c, -34, -150, 34, -150, 8, 0xFFFFD43B);
                break;
            case "popcorn":
                for (int i = 0; i < 7; i++) ci(c, -34 + (i % 4) * 22, -112 - (i / 4) * 22 - (i % 2) * 8, 20, i % 2 == 0 ? 0xFFFFF3C4 : 0xFFFFE9A0);
                poly(c, 0xFFFFFFFF, -48, -100, 48, -100, 38, 0, -38, 0);
                for (int i = -1; i <= 1; i += 1) poly(c, 0xFFFF5C73, i * 28 - 8, -100, i * 28 + 8, -100, i * 22 + 6, 0, i * 22 - 6, 0);
                break;
            case "cotton":
                ln(c, 0, -80, 0, 0, 8, 0xFFF3E1B5);
                ci(c, -30, -134, 30, 0xFFFF9EC4); ci(c, 30, -134, 30, 0xFFFF9EC4);
                ci(c, 0, -158, 38, 0xFFFFB4D3); ci(c, 0, -118, 34, 0xFFFFC6DD); ci(c, -14, -170, 12, 0xFFFFD6E8);
                break;
            case "medkit":
                rr(c, -52, -90, 104, 90, 14, WHITE);
                rr(c, -52, -90, 104, 20, 10, 0xFFFF5C73);
                rr(c, -14, -70, 28, 60, 6, 0xFFFF5C73); rr(c, -34, -50, 68, 20, 6, 0xFFFF5C73);
                rr(c, -20, -106, 40, 22, 9, 0xFFCDD6E4);
                break;
            case "cart":
                rr(c, -80, -100, 150, 72, 12, 0xFFC4CCDA);
                for (int i = 0; i < 4; i++) ln(c, -60 + i * 40, -96, -52 + i * 38, -34, 3, 0xFF8E98AB);
                ln(c, -80, -100, -96, -150, 9, 0xFF6B6480); ln(c, -110, -150, -80, -150, 9, 0xFF6B6480);
                ln(c, -60, -28, -60, -14, 6, 0xFF6B6480);
                ci(c, -50, -12, 14, INK); ci(c, 44, -12, 14, INK);
                rr(c, -30, -138, 40, 38, 8, 0xFFFF8F5C); ci(c, 34, -116, 16, 0xFF7ED957);
                break;
            case "wheelchair":
                rr(c, -56, -150, 18, 80, 8, 0xFF4B4660);
                rr(c, -56, -86, 90, 18, 8, 0xFF6FC3FF);
                rr(c, -54, -150, 70, 66, 14, 0xFF6FC3FF);
                cis(c, -10, -50, 48, 0xFF4B4660, 9);
                cis(c, -10, -50, 34, 0xFFBFC7D5, 3);
                ci(c, 70, -16, 16, 0xFF4B4660);
                ln(c, 34, -80, 62, -26, 8, 0xFF4B4660);
                break;
            case "clock":
                ci(c, 0, -60, 54, 0xFF8A5A30);
                ci(c, 0, -60, 46, WHITE);
                for (int i = 0; i < 12; i++) {
                    double a = i * Math.PI / 6;
                    ln(c, (float) Math.sin(a) * 38, -60 - (float) Math.cos(a) * 38, (float) Math.sin(a) * 42, -60 - (float) Math.cos(a) * 42, 3, 0xFF6B6480);
                }
                double ma = t * 0.5, ha = t * 0.04;
                ln(c, 0, -60, (float) Math.sin(ma) * 34, -60 - (float) Math.cos(ma) * 34, 4, INK);
                ln(c, 0, -60, (float) Math.sin(ha) * 22, -60 - (float) Math.cos(ha) * 22, 6, INK);
                ci(c, 0, -60, 5, 0xFFFF5C73);
                break;
            default:
                rr(c, -40, -80, 80, 80, 12, 0xFFB0BED0);
                break;
        }
    }

    private static Path balloonPath() {
        pp.reset();
        pp.moveTo(0, -128);
        pp.cubicTo(-70, -150, -64, -290, 0, -290);
        pp.cubicTo(64, -290, 70, -150, 0, -128);
        pp.close();
        return pp;
    }

    private static Path rocketNose() {
        pp.reset();
        pp.moveTo(-40, -176);
        pp.cubicTo(-30, -220, -10, -236, 0, -242);
        pp.cubicTo(10, -236, 30, -220, 40, -176);
        pp.quadTo(0, -194, -40, -176);
        pp.close();
        return pp;
    }

    private static Path trophyCup() {
        pp.reset();
        pp.moveTo(-44, -150);
        pp.lineTo(44, -150);
        pp.quadTo(44, -70, 0, -60);
        pp.quadTo(-44, -70, -44, -150);
        pp.close();
        return pp;
    }
}
