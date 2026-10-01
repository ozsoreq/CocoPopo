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

    /** Body types: legLen factor, torso width, torso height, head scale. */
    static final float[][] BODY = {
        {1.00f, 1.00f, 1.00f, 1.00f},   // kid
        {0.55f, 0.94f, 0.80f, 1.02f},   // toddler
        {1.50f, 0.94f, 1.18f, 0.90f},   // teen
        {1.42f, 1.20f, 1.28f, 0.90f},   // adult
        {1.05f, 1.36f, 1.12f, 0.94f},   // round
        {1.20f, 1.10f, 1.12f, 0.92f},   // elder
    };
    static final String[] BODY_NAMES = {"Kid", "Toddler", "Teen", "Adult", "Round", "Elder"};

    private static float[] body(Look l) { return BODY[l.body % BODY.length]; }

    /** Hip height (negative, local). */
    static float hipY(Look l) { return -98 * body(l)[0]; }

    /** Top of the torso / base of the neck. */
    static float neckY(Look l) { return hipY(l) - 98 * body(l)[2]; }

    /** Approximate full height including hair. */
    static float height(Look l) { return -neckY(l) + 170 * body(l)[3]; }

    static void draw(Canvas c, Look l, Pose p) {
        float[] b = body(l);
        float hip = hipY(l), neck = neckY(l), hk = b[3];
        int skin = Look.SKIN[l.skin], skinD = dk(skin, 0.13f);
        int hair = Look.HAIR[l.hairColor];
        int topC = Look.CLOTH[l.topColor];
        int pants = Look.PANTS[l.bottom];
        int shoe = Look.SHOES[l.shoe % Look.SHOES.length];
        float breathe = (float) Math.sin(p.t * 2.4f) * 2.2f;
        float bob = -Math.abs((float) Math.sin(p.walkPh)) * 7 * p.walk - Math.abs((float) Math.sin(p.t * 8)) * 12 * p.dance;
        if (p.sit) bob = 0;

        // ---- hair behind everything (head space)
        c.save();
        headSpace(c, l, p, neck, hk, breathe + bob);
        hairBack(c, l, hair);
        c.restore();

        // ---- legs
        boolean dress = l.top == 3;
        int legC = dress || l.bstyle == 2 ? skin : pants;
        float sw = p.swing * 14;
        float spread = 25 * Math.min(b[1], 1.15f);
        float legLen = -24 - hip;
        for (int s = -1; s <= 1 && !p.noLegs; s += 2) {
            if (p.sit) {
                // seated, seen from the front: foreshortened thighs toward the viewer, shins hanging
                float kick = (float) Math.sin(p.t * 3 + s) * 3;
                float sh = Math.max(18, 30 * b[0]);
                ov(c, s * (spread + 3), hip + 6, 26, 22, legC == skin && l.bstyle != 2 ? legC : pants);
                rr(c, s * (spread + 3) - 17, hip + 10, 34, sh + kick, 14, l.bstyle == 0 && !dress ? pants : skin);
                float fy = hip + 18 + sh + kick;
                ov(c, s * (spread + 5), fy + 4, 27, 14, dk(shoe, .25f));
                ov(c, s * (spread + 5), fy, 26, 13, shoe);
                continue;
            }
            float lift = Math.max(0, (float) Math.sin(p.walkPh + (s > 0 ? Math.PI : 0))) * 18 * p.walk
                + Math.max(0, (float) Math.sin(p.t * 8 + (s > 0 ? Math.PI : 0))) * 10 * p.dance;
            c.save();
            c.translate(s * spread, hip - lift + bob * .3f);
            c.rotate(s * sw);
            rr(c, -19, 0, 38, legLen, 18, legC);
            if (!dress && l.bstyle == 1) rr(c, -20, 0, 40, Math.min(legLen, 34), 12, pants);
            if (!dress && l.bstyle == 0) rr(c, -19, legLen - 14, 38, 14, 6, dk(pants, 0.1f));
            c.restore();
            float fx = s * spread + s * sw * 1.3f;
            ov(c, fx + s * 3, -15 - lift, 31, 17, dk(shoe, .25f));
            ov(c, fx + s * 3, -20 - lift, 30, 16, shoe);
        }

        // ---- torso + arms (torso space: original coordinates, hips at -98)
        c.save();
        c.translate(0, hip + breathe * .5f + bob);
        c.scale(b[1], b[2]);
        c.translate(0, 98);
        if (!dress && l.bstyle == 2) skirt(c, pants);
        torso(c, l, skin, topC, pants);
        if (p.holdType == 1 && p.hold) { /* arms drawn in front of the item by the game */ }
        for (int s = -1; s <= 1; s += 2) {
            c.save();
            c.translate(s * 62, -178);
            float ang = armAngle(p, s);
            c.rotate(-ang);
            int sleeve = l.top == 5 ? 0xFFFFFFFF : (l.top == 3 ? skin : topC);
            rr(c, -15, -8, 30, 84, 15, sleeve);
            if (l.top != 3) rr(c, -15, 58, 30, 8, 4, dk(sleeve, 0.08f));
            mitten(c, s, skin);
            c.restore();
        }
        c.restore();

        // ---- neck + head
        c.save();
        headSpace(c, l, p, neck, hk, breathe + bob);
        rr(c, -19, -214, 38, 34, 12, skinD);
        head(c, l, p, skin, skinD, hair);
        c.restore();
    }

    /** Moves the canvas so head drawing code (neck at -196) lands on this body's neck. */
    private static void headSpace(Canvas c, Look l, Pose p, float neck, float hk, float dy) {
        c.translate(p.look * 7, neck + dy);
        if (p.dance > 0) c.rotate((float) Math.sin(p.t * 8) * 7 * p.dance);
        if (p.tilt != 0) c.rotate(p.tilt);
        c.scale(hk, hk);
        c.translate(0, 196);
    }

    static void mitten(Canvas c, int s, int skin) {
        ov(c, -s * 12, 72, 8, 10, skin);
        ov(c, 0, 82, 17, 19, skin);
    }

    static void skirt(Canvas c, int col) {
        PA.reset();
        PA.moveTo(-58, -110); PA.lineTo(58, -110); PA.lineTo(80, -54); PA.quadTo(0, -44, -80, -54); PA.close();
        path(c, PA, col);
        rr(c, -81, -62, 162, 12, 6, lt(col, .35f));
    }

    /** Arm rotation in degrees for side s (-1 left, +1 right). */
    static float armAngle(Pose p, int s) {
        float raise = p.arm;
        float a = 14 + 150 * raise + (float) Math.sin(p.t * 2.0f + s) * 4 * (1 - raise);
        a += (float) Math.sin(p.walkPh + (s > 0 ? Math.PI : 0)) * 22 * p.walk;
        if (p.dance > 0) a += (60 + (float) Math.sin(p.t * 8 + (s > 0 ? Math.PI : 0)) * 70) * p.dance;
        if (s > 0 && p.wave > 0) a = a * (1 - p.wave) + (150 + (float) Math.sin(p.t * 14) * 24) * p.wave;
        if (p.hug > 0) a = a * (1 - p.hug) + (-48 + (float) Math.sin(p.t * 6) * 4) * p.hug;
        if (p.hold && p.arm < .5f) {
            if (p.holdType == 1) a = -62;                                   // both hands in front
            else if (p.holdType == 2) { if (s > 0) a = 166 + (float) Math.sin(p.t * 3) * 4; }  // overhead
            else if (s > 0) a = p.strum > 0 ? 40 + (float) Math.sin(p.t * 30) * 12 : 48;
        }
        if (p.sit && !p.hold && p.wave <= .1f && p.dance <= .1f && p.arm < .3f) a = 6;
        return s * a;
    }

    /** Right-hand centre in avatar-local coordinates (or the two-hand grip point), matching draw(). */
    static float handX(Pose p, Look l) {
        if (p.hold && p.holdType == 1 && p.arm < .5f) return 0;
        double a = Math.toRadians(armAngle(p, 1));
        return (62 + 80 * (float) Math.sin(a)) * body(l)[1];
    }

    static float handY(Pose p, Look l) {
        float bob = p.sit ? 0 : -Math.abs((float) Math.sin(p.walkPh)) * 7 * p.walk - Math.abs((float) Math.sin(p.t * 8)) * 12 * p.dance;
        float hy;
        if (p.hold && p.holdType == 1 && p.arm < .5f) hy = -146;
        else hy = -178 + 80 * (float) Math.cos(Math.toRadians(armAngle(p, 1)));
        return hipY(l) + (hy + 98) * body(l)[2] + (float) Math.sin(p.t * 2.4f) * 1.1f + bob;
    }

    // ------------------------------------------------------------------ torso
    static void torso(Canvas c, Look l, int skin, int topC, int pants) {
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

    static void neckline(Canvas c, int skin, int topC) {
        ov(c, 0, -194, 26, 15, skin);
        arc(c, 0, -194, 26, 15, 0, 180, 5, dk(topC, 0.15f));
    }

    // ------------------------------------------------------------------ head
    // expressions (Pose.face)
    static final int F_NONE = 0, F_HAPPY = 1, F_LAUGH = 2, F_WOW = 3, F_YUM = 4, F_SAD = 5, F_SLEEPY = 6, F_LOVE = 7, F_SCARED = 8;

    private static void head(Canvas c, Look l, Pose p, int skin, int skinD, int hair) {
        headBase(c, l, skin, skinD);
        face(c, l, p, skin, hair);
        hairFront(c, l, hair);
        accessory(c, l, hair);
    }

    static void headBase(Canvas c, Look l, int skin, int skinD) {
        int shape = l.head % 3;
        float earX = shape == 2 ? 97 : 88;
        ci(c, -earX, -256, 17, skin);
        ci(c, earX, -256, 17, skin);
        ci(c, -earX, -256, 8, skinD);
        ci(c, earX, -256, 8, skinD);
        boolean sh = Gfx.shade;
        Gfx.shade = false;
        if (shape == 1) rr(c, -90, -346, 180, 164, 70, skin);
        else if (shape == 2) ov(c, 0, -258, 99, 78, skin);
        else ov(c, 0, -264, 90, 84, skin);
        Gfx.shade = sh;
    }

    static void face(Canvas c, Look l, Pose p, int skin, int hair) {
        int face = p.sleep ? F_SLEEPY : p.face;
        // cheeks
        int blush = face == F_LOVE || face == F_LAUGH ? 150 : 85;
        ov(c, -52, -236, 17, 11, al(0xFFFF6F8F, blush));
        ov(c, 52, -236, 17, 11, al(0xFFFF6F8F, blush));
        if (l.freckles == 1) for (int s = -1; s <= 1; s += 2)
            for (int i = 0; i < 3; i++) ci(c, s * (40 + i * 10), -244 + (i % 2) * 7, 2.6f, dk(skin, .3f));

        // eyes
        float blink = p.sleep ? 1 : p.blink;
        int style = l.eyes;
        if (face == F_LAUGH || face == F_YUM) style = 1;
        else if (face == F_SLEEPY) style = 3;
        float gx = Gfx.clamp(p.gazeX + p.look * .6f, -1, 1), gy = Gfx.clamp(p.gazeY, -1, 1);
        float browLift = 0, browTilt = 0;
        switch (face) {
            case F_WOW: browLift = -10; break;
            case F_SCARED: browLift = -12; browTilt = -12; break;
            case F_SAD: browLift = -4; browTilt = -18; break;
            case F_LAUGH: browLift = -6; break;
            case F_SLEEPY: browLift = 4; break;
            default: break;
        }
        for (int s = -1; s <= 1; s += 2) {
            float ex = s * 35 + p.look * 6, ey = -262;
            if (face == F_LOVE) {
                hp.reset();
                hp.moveTo(ex, ey + 12); hp.cubicTo(ex - 26, ey - 4, ex - 14, ey - 22, ex, ey - 9); hp.cubicTo(ex + 14, ey - 22, ex + 26, ey - 4, ex, ey + 12);
                hp.close();
                float o = Gfx.olw; Gfx.ol(0); path(c, hp, 0xFFFF4D6D); Gfx.ol(o);
            } else if (face == F_WOW || face == F_SCARED) {
                ov(c, ex, ey, 18, 21, 0xFFFFFFFF);
                ci(c, ex + gx * 5, ey + gy * 4, face == F_SCARED ? 5 : 7, INK);
            } else switch (style) {
                case 0: { // round dot
                    float ry = 14 * (1 - 0.85f * blink);
                    ov(c, ex + gx * 4, ey + gy * 3, 11, ry, INK);
                    if (blink < .5f) ci(c, ex + gx * 4 + 3.5f, ey - 5, 4, 0xFFFFFFFF);
                    break;
                }
                case 1: // happy arcs
                    arc(c, ex, ey + 6, 13, 13, 200, 140, 6, INK);
                    break;
                case 2: { // big sparkly
                    float ry = 19 * (1 - 0.85f * blink);
                    ov(c, ex + gx * 4, ey + gy * 3, 15, ry, INK);
                    if (blink < .5f) {
                        ci(c, ex + gx * 4 + 5, ey - 7, 6, 0xFFFFFFFF);
                        ci(c, ex + gx * 4 - 5, ey + 6, 3, 0xFFFFFFFF);
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
                case 4: // wink
                    if (s < 0) ov(c, ex, ey, 11, 14 * (1 - 0.85f * blink), INK);
                    else arc(c, ex, ey + 4, 12, 10, 200, 140, 6, INK);
                    if (s < 0 && blink < .5f) ci(c, ex + 3.5f, ey - 5, 4, 0xFFFFFFFF);
                    break;
                default: { // whites + pupils that look at things
                    ov(c, ex, ey, 17, 19, 0xFFFFFFFF);
                    ov(c, ex + gx * 6, ey + gy * 5 + 2, 10, 12, INK);
                    ci(c, ex + gx * 6 + 4, ey + gy * 5 - 3, 3.5f, 0xFFFFFFFF);
                    if (blink > .05f) {
                        float o = Gfx.olw; Gfx.ol(0);
                        rr(c, ex - 20, ey - 22, 40, 42 * blink, 10, skin);
                        Gfx.ol(o);
                        ln(c, ex - 16, ey - 22 + 40 * blink, ex + 16, ey - 22 + 40 * blink, 4, dk(skin, .35f));
                    }
                    break;
                }
            }
            // brows follow the expression
            c.save();
            c.translate(ex, ey - 26 + browLift);
            c.rotate(s * browTilt);
            arc(c, 0, 4, 14, 8, 205, 130, 5, dk(hair, 0.05f));
            c.restore();
        }

        // nose
        arc(c, 0, -246, 7, 5, 20, 140, 4, dk(skin, 0.22f));

        // mouth
        int m = p.mood >= 0 ? p.mood : l.mouth;
        switch (face) {
            case F_HAPPY: m = 1; break;
            case F_LAUGH: m = 5; break;
            case F_WOW: m = 3; break;
            case F_YUM: m = 4; break;
            case F_SAD: m = 6; break;
            case F_SLEEPY: m = 3; break;
            case F_LOVE: m = 0; break;
            case F_SCARED: m = 7; break;
            default: break;
        }
        if (p.chew) m = Math.sin(p.t * 16) > 0 ? 3 : 2;
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
                ov(c, 0, my + 2, p.sleep || face == F_SLEEPY ? 6 : 9, p.sleep || face == F_SLEEPY ? 7 : 12, MOUTH);
                break;
            case 4: { // tongue
                arc(c, 0, my - 6, 22, 14, 20, 140, 5, MOUTH);
                rr(c, 2, my + 4, 15, 20, 7, 0xFFFF7C8E);
                ln(c, 9, my + 6, 9, my + 16, 2, 0xFFE0566F);
                break;
            }
            case 6: // sad
                arc(c, 0, my + 8, 18, 12, 200, 140, 5, MOUTH);
                break;
            case 7: { // wobbly scared
                hp.reset();
                hp.moveTo(-18, my); hp.quadTo(-12, my - 8, -6, my); hp.quadTo(0, my + 8, 6, my); hp.quadTo(12, my - 8, 18, my);
                pathS(c, hp, MOUTH, 5);
                break;
            }
            default: { // big grin w/ teeth
                pie(c, 0, my - 12, 30, 32, 0, 180, MOUTH);
                rr(c, -26, my - 12, 52, 12, 4, 0xFFFFFFFF);
                break;
            }
        }

    }

    // ------------------------------------------------------------------ hair
    static void hairBack(Canvas c, Look l, int hair) {
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
            case 10: // top bun
                ov(c, 0, -268, 94, 86, hair);
                ci(c, 0, -372, 42, hair);
                rr(c, -30, -342, 60, 14, 7, 0xFFFF8FD0);
                break;
            case 11: // braids
                ov(c, 0, -268, 94, 86, hair);
                for (int s = -1; s <= 1; s += 2) {
                    for (int i = 0; i < 5; i++) ov(c, s * (94 - i * 2), -224 + i * 26, 18, 16, i % 2 == 0 ? hair : dk(hair, .08f));
                    rr(c, s * 86 - 14, -104, 28, 12, 6, 0xFF4FB3FF);
                }
                break;
            case 12: // curly top
                ov(c, 0, -268, 94, 86, hair);
                break;
            default: break;
        }
    }

    static void hairFront(Canvas c, Look l, int hair) {
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
            case 9: // mohawk
                rr(c, -18, -404, 36, 100, 18, hair);
                for (int i = 0; i < 3; i++) poly(c, hair, -18, -380 + i * 26, -34, -392 + i * 26, -18, -366 + i * 26);
                return;
            case 12: // curly top
                fringe(c, hair, 1);
                for (int i = 0; i < 7; i++) {
                    double a = Math.toRadians(-160 + i * 23.3);
                    ci(c, (float) Math.cos(a) * 82, -282 + (float) Math.sin(a) * 74, 30, hair);
                }
                return;
            case 10: fringe(c, hair, 0); return;
            case 11: fringe(c, hair, 3); return;
            case 1: fringe(c, hair, 0); break;
            case 4: fringe(c, hair, 2); break;
            case 3: fringe(c, hair, 3); break;
            default: fringe(c, hair, 1); break;
        }
    }

    /** Hair cap over the forehead. Variants change the bottom edge. */
    static void fringe(Canvas c, int hair, int v) {
        hp.reset();
        hp.moveTo(-94, -262);
        hp.cubicTo(-108, -382, 108, -382, 94, -262);
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
    static void accessory(Canvas c, Look l, int hair) {
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
            case 8: // party hat
                c.save(); c.rotate(12, 0, -340);
                poly(c, ac, -42, -336, 42, -336, 0, -440);
                for (int i = 0; i < 3; i++) ci(c, -14 + i * 14, -360 - i * 24, 6, 0xFFFFFFFF);
                ci(c, 0, -444, 14, 0xFFFFD43B);
                c.restore();
                break;
            case 9: // cat ears
                for (int s = -1; s <= 1; s += 2) {
                    poly(c, ac, s * 30, -332, s * 84, -330, s * 70, -398);
                    poly(c, 0xFFFF9EC4, s * 44, -338, s * 74, -338, s * 66, -376);
                }
                arc(c, 0, -268, 94, 90, 205, 130, 9, ac);
                break;
            case 10: // flower crown
                for (int i = 0; i < 7; i++) {
                    double a = Math.toRadians(-165 + i * 25);
                    float fx = (float) Math.cos(a) * 88, fy = -276 + (float) Math.sin(a) * 76;
                    int pc = i % 3 == 0 ? 0xFFFF8FC0 : (i % 3 == 1 ? 0xFFFFD43B : 0xFFB67CFF);
                    ci(c, fx, fy, 15, pc);
                    ci(c, fx, fy, 6, 0xFFFFFFFF);
                }
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
