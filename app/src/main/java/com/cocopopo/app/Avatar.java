package com.cocopopo.app;

import android.graphics.Canvas;
import android.graphics.Path;

import static com.cocopopo.app.Gfx.*;

/**
 * Draws a chunky big-headed character. Local origin is the point between the feet,
 * the character extends upward (negative y). Total height is roughly 360 units.
 */
final class Avatar {
    static final float W = 190, H = 380;
    private static final int SHOE = 0xFFF7F4F0, SOLE = 0xFF9AA2B8, MOUTH = 0xFF7A2B3A, INK = 0xFF2B2230;
    private static final Path hp = new Path();

    private Avatar() {}

    static void draw(Canvas c, Look l, Pose p) {
        int skin = Look.SKIN[l.skin], skinD = dk(skin, 0.13f);
        int hair = Look.HAIR[l.hairColor];
        int topC = Look.CLOTH[l.topColor];
        int pants = Look.PANTS[l.bottom];
        float breathe = (float) Math.sin(p.t * 2.4f) * 2.2f;

        // ---- hair behind everything
        hairBack(c, l, hair);

        // ---- legs
        boolean dress = l.top == 3;
        float sw = p.swing * 14;
        for (int s = -1; s <= 1; s += 2) {
            c.save();
            c.translate(s * 25, -98);
            c.rotate(s * sw);
            rr(c, -19, 0, 38, 74, 18, dress ? skin : pants);
            if (!dress) rr(c, -19, 0, 38, 14, 6, dk(pants, 0.1f));
            c.restore();
            // shoes
            float fx = s * 25 + s * sw * 1.3f;
            ov(c, fx + s * 3, -15, 31, 17, SOLE);
            ov(c, fx + s * 3, -20, 30, 16, SHOE);
        }

        c.save();
        c.translate(0, breathe * 0.5f);

        // ---- torso
        torso(c, l, skin, topC, pants);

        // ---- arms
        float raise = p.arm;
        for (int s = -1; s <= 1; s += 2) {
            c.save();
            c.translate(s * 62, -178);
            float ang = s * (14 + 150 * raise) + (float) Math.sin(p.t * 2.0f + s) * 4 * (1 - raise);
            c.rotate(-ang);
            int sleeve = l.top == 5 ? 0xFFFFFFFF : (l.top == 3 ? skin : topC);
            rr(c, -15, -8, 30, 84, 15, sleeve);
            if (l.top == 3) { /* sleeveless dress */ } else rr(c, -15, 58, 30, 8, 4, dk(sleeve, 0.08f));
            ci(c, 0, 80, 17, skin);
            c.restore();
        }
        c.restore();

        // ---- neck + head
        c.save();
        c.translate(0, breathe);
        rr(c, -19, -214, 38, 34, 12, skinD);
        head(c, l, p, skin, skinD, hair);
        c.restore();
    }

    // ------------------------------------------------------------------ torso
    private static void torso(Canvas c, Look l, int skin, int topC, int pants) {
        switch (l.top) {
            case 0: // tee
                rr(c, -58, -196, 116, 112, 38, topC);
                neckline(c, skin, topC);
                break;
            case 1: // stripes
                rr(c, -58, -196, 116, 112, 38, topC);
                for (int i = 0; i < 4; i++) rr(c, -52, -170 + i * 24, 104, 11, 5, lt(topC, 0.55f));
                neckline(c, skin, topC);
                break;
            case 2: // hoodie
                ov(c, 0, -192, 56, 28, dk(topC, 0.18f));
                rr(c, -60, -196, 120, 116, 38, topC);
                rr(c, -34, -128, 68, 36, 14, dk(topC, 0.12f));
                ov(c, 0, -190, 26, 15, skin);
                ln(c, -14, -178, -16, -150, 5, 0xFFFFFFFF);
                ln(c, 14, -178, 16, -150, 5, 0xFFFFFFFF);
                break;
            case 3: { // dress
                PA.reset();
                PA.moveTo(-52, -190);
                PA.quadTo(0, -208, 52, -190);
                PA.lineTo(60, -150);
                PA.lineTo(84, -66);
                PA.quadTo(0, -52, -84, -66);
                PA.lineTo(-60, -150);
                PA.close();
                path(c, PA, topC);
                rr(c, -85, -76, 170, 16, 8, lt(topC, 0.4f));
                rr(c, -57, -156, 114, 12, 6, dk(topC, 0.14f));
                ov(c, 0, -192, 24, 14, skin);
                break;
            }
            case 4: // overalls
                rr(c, -58, -196, 116, 112, 38, lt(topC, 0.5f));
                rr(c, -44, -156, 88, 76, 22, pants);
                rr(c, -44, -196, 16, 56, 7, pants);
                rr(c, 28, -196, 16, 56, 7, pants);
                ci(c, -36, -150, 6, 0xFFFFD43B);
                ci(c, 36, -150, 6, 0xFFFFD43B);
                rr(c, -16, -130, 32, 26, 8, dk(pants, 0.14f));
                ov(c, 0, -192, 24, 13, skin);
                break;
            default: // doctor coat
                rr(c, -58, -196, 116, 112, 38, 0xFF6DD3C8);
                rr(c, -58, -196, 116, 124, 36, 0xFFFFFFFF);
                poly(c, 0xFF6DD3C8, -14, -196, 14, -196, 0, -140);
                ln(c, 0, -140, 0, -76, 4, 0xFFD5DDE8);
                poly(c, skin, -16, -198, 16, -198, 0, -172);
                ci(c, 30, -150, 9, 0xFFFF5C73);
                ln(c, 30, -158, 30, -142, 4, 0xFFFFFFFF);
                ln(c, 22, -150, 38, -150, 4, 0xFFFFFFFF);
                break;
        }
    }

    private static void neckline(Canvas c, int skin, int topC) {
        ov(c, 0, -194, 26, 15, skin);
        arc(c, 0, -194, 26, 15, 0, 180, 5, dk(topC, 0.15f));
    }

    // ------------------------------------------------------------------ head
    private static void head(Canvas c, Look l, Pose p, int skin, int skinD, int hair) {
        ci(c, -88, -256, 17, skin);
        ci(c, 88, -256, 17, skin);
        ci(c, -88, -256, 8, skinD);
        ci(c, 88, -256, 8, skinD);
        ov(c, 0, -264, 90, 84, skin);

        // cheeks
        ov(c, -52, -236, 17, 11, al(0xFFFF6F8F, 85));
        ov(c, 52, -236, 17, 11, al(0xFFFF6F8F, 85));

        // eyes
        float blink = p.blink;
        for (int s = -1; s <= 1; s += 2) {
            float ex = s * 35, ey = -262;
            switch (l.eyes) {
                case 0: { // round
                    float ry = 14 * (1 - 0.85f * blink);
                    ov(c, ex, ey, 11, ry, INK);
                    if (blink < .5f) ci(c, ex + 3.5f, ey - 5, 4, 0xFFFFFFFF);
                    break;
                }
                case 1: // happy arcs
                    arc(c, ex, ey + 6, 13, 13, 200, 140, 6, INK);
                    break;
                case 2: { // big sparkly
                    float ry = 19 * (1 - 0.85f * blink);
                    ov(c, ex, ey, 15, ry, INK);
                    if (blink < .5f) {
                        ci(c, ex + 5, ey - 7, 6, 0xFFFFFFFF);
                        ci(c, ex - 5, ey + 6, 3, 0xFFFFFFFF);
                    }
                    ln(c, ex + s * 12, ey - 12, ex + s * 20, ey - 17, 4, INK);
                    break;
                }
                case 3: { // sleepy
                    ov(c, ex, ey + 2, 12, 12 * (1 - 0.7f * blink), INK);
                    rr(c, ex - 15, ey - 14, 30, 14, 5, skin);
                    ln(c, ex - 14, ey - 1, ex + 14, ey - 1, 4, dk(skin, 0.35f));
                    break;
                }
                default: // wink / star-ish
                    if (s < 0) ov(c, ex, ey, 11, 14 * (1 - 0.85f * blink), INK);
                    else arc(c, ex, ey + 4, 12, 10, 200, 140, 6, INK);
                    if (s < 0 && blink < .5f) ci(c, ex + 3.5f, ey - 5, 4, 0xFFFFFFFF);
                    break;
            }
            // brows
            if (l.hairColor >= 0) arc(c, ex, ey - 22, 14, 8, 205, 130, 5, dk(hair, 0.05f));
        }

        // nose
        arc(c, 0, -246, 7, 5, 20, 140, 4, dk(skin, 0.22f));

        // mouth
        int m = p.mood >= 0 ? p.mood : l.mouth;
        float my = -222;
        switch (m) {
            case 0: // soft smile
                arc(c, 0, my - 6, 20, 14, 30, 120, 5, MOUTH);
                break;
            case 1: { // open smile
                pie(c, 0, my - 8, 24, 24, 0, 180, MOUTH);
                pie(c, 0, my + 2, 13, 11, 0, 180, 0xFFFF7C8E);
                rect(c, -17, my - 8, 34, 6, 0xFFFFFFFF);
                break;
            }
            case 2: // neutral
                ln(c, -12, my, 12, my, 5, MOUTH);
                break;
            case 3: // oh
                ov(c, 0, my + 2, 9, 12, MOUTH);
                break;
            case 4: { // tongue
                arc(c, 0, my - 6, 22, 14, 20, 140, 5, MOUTH);
                rr(c, 2, my + 4, 15, 20, 7, 0xFFFF7C8E);
                ln(c, 9, my + 6, 9, my + 16, 2, 0xFFE0566F);
                break;
            }
            default: { // big grin w/ teeth
                pie(c, 0, my - 12, 30, 32, 0, 180, MOUTH);
                rr(c, -26, my - 12, 52, 12, 4, 0xFFFFFFFF);
                break;
            }
        }

        hairFront(c, l, hair);
        accessory(c, l, hair);
    }

    // ------------------------------------------------------------------ hair
    private static void hairBack(Canvas c, Look l, int hair) {
        int hd = dk(hair, 0.1f);
        switch (l.hairStyle) {
            case 1: ov(c, 0, -268, 94, 86, hair); break;
            case 2: rr(c, -99, -318, 198, 138, 52, hair); break;
            case 3: rr(c, -100, -318, 200, 220, 56, hair); break;
            case 4: {
                ov(c, 0, -268, 94, 86, hair);
                c.save(); c.translate(100, -236); c.rotate(-25);
                ov(c, 6, 36, 26, 58, hair);
                c.restore();
                ci(c, 96, -262, 14, 0xFFFF5C73);
                break;
            }
            case 5:
                ci(c, -72, -300, 52, hair); ci(c, 72, -300, 52, hair);
                ci(c, 0, -332, 62, hair);
                ci(c, -100, -250, 44, hair); ci(c, 100, -250, 44, hair);
                ci(c, -98, -205, 30, hair); ci(c, 98, -205, 30, hair);
                break;
            case 6: ov(c, 0, -268, 94, 86, hair); break;
            case 7:
                ov(c, 0, -268, 94, 86, hair);
                ci(c, -64, -352, 36, hair); ci(c, 64, -352, 36, hair);
                ci(c, -64, -352, 15, hd); ci(c, 64, -352, 15, hd);
                break;
            case 8:
                ov(c, 0, -268, 94, 86, hair);
                for (int s = -1; s <= 1; s += 2) {
                    c.save(); c.translate(s * 104, -228); c.rotate(s * 14);
                    ov(c, 0, 44, 28, 56, hair);
                    c.restore();
                    rr(c, s * 104 - 16, -250, 32, 16, 8, s < 0 ? 0xFFFF5C73 : 0xFFFF5C73);
                }
                break;
            default: break;
        }
    }

    private static void hairFront(Canvas c, Look l, int hair) {
        switch (l.hairStyle) {
            case 0: return;
            case 5: // afro hairline
                ov(c, 0, -330, 80, 34, hair);
                ci(c, -48, -318, 30, hair); ci(c, 48, -318, 30, hair); ci(c, 0, -312, 30, hair);
                return;
            case 6: // spiky
                fringe(c, hair, 1);
                for (int i = -2; i <= 2; i++) {
                    float x = i * 34;
                    poly(c, hair, x - 22, -330 + Math.abs(i) * 12, x, -392 + Math.abs(i) * 14, x + 22, -330 + Math.abs(i) * 12);
                }
                return;
            case 1: fringe(c, hair, 0); break;
            case 4: fringe(c, hair, 2); break;
            case 3: fringe(c, hair, 3); break;
            default: fringe(c, hair, 1); break;
        }
    }

    /** Hair cap over the forehead. Variants change the bottom edge. */
    private static void fringe(Canvas c, int hair, int v) {
        hp.reset();
        hp.moveTo(-92, -262);
        hp.cubicTo(-104, -362, 104, -362, 92, -262);
        switch (v) {
            case 0: // short, high
                hp.quadTo(70, -296, 44, -308);
                hp.quadTo(0, -296, -44, -310);
                hp.quadTo(-72, -296, -92, -262);
                break;
            case 1: // scalloped
                hp.quadTo(76, -280, 62, -302);
                hp.quadTo(46, -274, 26, -304);
                hp.quadTo(8, -272, -14, -304);
                hp.quadTo(-32, -274, -52, -302);
                hp.quadTo(-70, -280, -92, -262);
                break;
            case 2: // side swept
                hp.quadTo(86, -284, 70, -300);
                hp.quadTo(20, -270, -40, -302);
                hp.quadTo(-70, -296, -92, -262);
                break;
            default: // centre parted
                hp.quadTo(88, -272, 56, -304);
                hp.quadTo(30, -296, 0, -320);
                hp.quadTo(-30, -296, -56, -304);
                hp.quadTo(-88, -272, -92, -262);
                break;
        }
        hp.close();
        path(c, hp, hair);
    }

    // ------------------------------------------------------------------ accessories
    private static void accessory(Canvas c, Look l, int hair) {
        int ac = Look.CLOTH[l.accColor];
        switch (l.acc) {
            case 1: // round glasses
                cis(c, -35, -262, 22, 0xFF3A3346, 5);
                cis(c, 35, -262, 22, 0xFF3A3346, 5);
                ln(c, -13, -264, 13, -264, 5, 0xFF3A3346);
                break;
            case 2: // sunglasses
                rr(c, -62, -278, 56, 32, 14, 0xFF2B2735);
                rr(c, 6, -278, 56, 32, 14, 0xFF2B2735);
                ln(c, -8, -268, 8, -268, 5, 0xFF2B2735);
                ln(c, -50, -270, -36, -274, 4, al(0xFFFFFFFF, 140));
                break;
            case 3: // cap
                hp.reset();
                hp.moveTo(-90, -290);
                hp.cubicTo(-96, -380, 96, -380, 90, -290);
                hp.close();
                path(c, hp, ac);
                rr(c, -96, -302, 192, 18, 9, dk(ac, 0.18f));
                rr(c, 20, -300, 100, 14, 7, dk(ac, 0.18f));
                ci(c, 0, -362, 8, lt(ac, 0.3f));
                break;
            case 4: // beanie
                hp.reset();
                hp.moveTo(-92, -292);
                hp.cubicTo(-100, -392, 100, -392, 92, -292);
                hp.close();
                path(c, hp, ac);
                rr(c, -98, -306, 196, 30, 14, dk(ac, 0.15f));
                for (int i = -4; i <= 4; i++) ln(c, i * 20, -302, i * 20, -282, 3, dk(ac, 0.3f));
                ci(c, 0, -392, 17, lt(ac, 0.5f));
                break;
            case 5: // crown
                poly(c, 0xFFFFC93C, -58, -330, -58, -388, -29, -358, 0, -400, 29, -358, 58, -388, 58, -330);
                rr(c, -60, -340, 120, 16, 6, 0xFFE8A91F);
                ci(c, 0, -378, 7, 0xFFFF5C73);
                ci(c, -38, -368, 5, 0xFF4FB3FF);
                ci(c, 38, -368, 5, 0xFF7ED957);
                break;
            case 6: // bow
                c.save(); c.translate(-54, -340); c.rotate(-18);
                poly(c, ac, 0, 0, -40, -24, -40, 24);
                poly(c, ac, 0, 0, 40, -24, 40, 24);
                ci(c, 0, 0, 12, dk(ac, 0.2f));
                c.restore();
                break;
            case 7: // headphones
                arc(c, 0, -268, 100, 96, 190, 160, 11, 0xFF3A3346);
                rr(c, -112, -284, 30, 56, 14, ac);
                rr(c, 82, -284, 30, 56, 14, ac);
                break;
            default: break;
        }
    }

    /** Draw the character at a given scale with idle animation, helper for UI previews. */
    static void drawAt(Canvas c, Look l, float x, float y, float scale, float t) {
        Pose p = new Pose();
        p.t = t;
        c.save();
        c.translate(x, y);
        c.scale(scale, scale);
        draw(c, l, p);
        c.restore();
    }
}
