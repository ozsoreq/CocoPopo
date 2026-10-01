package com.cocopopo.app;

import android.graphics.Canvas;
import android.graphics.Path;

import static com.cocopopo.app.Gfx.*;

/** Chunky glyphs for round buttons. Each is drawn inside roughly a 50-unit radius, in one colour. */
final class Icons {
    static final int HOME = 0, CAMERA = 1, BROOM = 2, PEOPLE = 3, CUBE = 4, PLUS = 5, MINUS = 6, CHECK = 7, DICE = 8,
        CLOSE = 9, TRASH = 10, EDIT = 11, SMILE = 12, FLIP = 13, COPY = 14, BACK = 15, STAR = 16, SHIRT = 17,
        FACE = 18, HAIR = 19, SPARK = 20, PERSON_PLUS = 21, SAVE = 22;

    private static final Path p = new Path();

    private Icons() {}

    static void draw(Canvas c, int id, float cx, float cy, float s, int col) {
        c.save();
        c.translate(cx, cy);
        c.scale(s, s);
        float w = 9;
        switch (id) {
            case HOME:
                poly(c, col, -30, -2, 0, -30, 30, -2);
                rr(c, -20, -4, 40, 30, 6, col);
                rr(c, -6, 8, 12, 18, 4, 0x55000000);
                break;
            case CAMERA:
                rr(c, -30, -16, 60, 42, 10, col);
                rr(c, -12, -26, 24, 14, 5, col);
                ci(c, 0, 5, 13, 0x66000000);
                ci(c, 0, 5, 8, col);
                break;
            case BROOM:
                ln(c, 14, -30, -4, 4, 8, col);
                poly(c, col, -22, 4, 6, 4, 14, 30, -30, 30);
                for (int i = 0; i < 3; i++) ln(c, -18 + i * 10, 12, -20 + i * 11, 28, 2.5f, 0x66000000);
                break;
            case PEOPLE:
                ci(c, 0, -14, 14, col);
                pie(c, 0, 32, 28, 28, 180, 180, col);
                break;
            case PERSON_PLUS:
                ci(c, -6, -14, 12, col);
                rr(c, -26, 2, 40, 28, 14, col);
                ln(c, 26, -20, 26, 2, 7, col); ln(c, 15, -9, 37, -9, 7, col);
                break;
            case CUBE:
                poly(c, col, 0, -30, 28, -16, 0, -2, -28, -16);
                poly(c, dk(col, .12f), -28, -12, -2, 2, -2, 32, -28, 18);
                poly(c, dk(col, .28f), 28, -12, 2, 2, 2, 32, 28, 18);
                break;
            case PLUS:
                ln(c, -20, 0, 20, 0, w, col); ln(c, 0, -20, 0, 20, w, col);
                break;
            case MINUS:
                ln(c, -20, 0, 20, 0, w, col);
                break;
            case CHECK:
                ln(c, -22, 2, -6, 18, 11, col); ln(c, -6, 18, 24, -16, 11, col);
                break;
            case DICE:
                rr(c, -26, -26, 52, 52, 12, col);
                ci(c, -12, -12, 5, 0x77000000); ci(c, 12, 12, 5, 0x77000000); ci(c, 0, 0, 5, 0x77000000);
                ci(c, 12, -12, 5, 0x77000000); ci(c, -12, 12, 5, 0x77000000);
                break;
            case CLOSE:
                ln(c, -18, -18, 18, 18, w, col); ln(c, 18, -18, -18, 18, w, col);
                break;
            case TRASH:
                rr(c, -20, -12, 40, 40, 8, col);
                rr(c, -26, -22, 52, 8, 4, col);
                rr(c, -8, -30, 16, 10, 4, col);
                for (int i = -1; i <= 1; i++) ln(c, i * 10, -2, i * 10, 20, 4, 0x66000000);
                break;
            case EDIT:
                c.save(); c.rotate(45);
                rr(c, -9, -30, 18, 46, 4, col);
                poly(c, col, -9, 20, 9, 20, 0, 34);
                c.restore();
                break;
            case SMILE:
                cis(c, 0, 0, 24, col, 7);
                ci(c, -9, -7, 4.5f, col); ci(c, 9, -7, 4.5f, col);
                arc(c, 0, 2, 13, 11, 20, 140, 6, col);
                break;
            case FLIP:
                ln(c, -26, -8, 24, -8, 7, col); poly(c, col, 26, -8, 10, -22, 10, 6);
                ln(c, 26, 14, -24, 14, 7, col); poly(c, col, -26, 14, -10, 0, -10, 28);
                break;
            case COPY:
                rr(c, -26, -26, 34, 38, 8, col);
                rr(c, -8, -10, 34, 38, 8, dk(col, .1f));
                break;
            case BACK:
                ln(c, 14, -22, -14, 0, 11, col); ln(c, -14, 0, 14, 22, 11, col);
                break;
            case STAR: {
                p.reset();
                for (int i = 0; i < 10; i++) {
                    double a = -Math.PI / 2 + i * Math.PI / 5;
                    float r = i % 2 == 0 ? 30 : 13;
                    float x = (float) Math.cos(a) * r, y = (float) Math.sin(a) * r;
                    if (i == 0) p.moveTo(x, y); else p.lineTo(x, y);
                }
                p.close();
                path(c, p, col);
                break;
            }
            case SHIRT:
                poly(c, col, -14, -26, 14, -26, 32, -10, 22, 4, 14, -2, 14, 28, -14, 28, -14, -2, -22, 4, -32, -10);
                break;
            case FACE:
                ci(c, 0, 0, 26, col);
                ci(c, -9, -5, 4.5f, 0x88000000); ci(c, 9, -5, 4.5f, 0x88000000);
                arc(c, 0, 4, 12, 9, 20, 140, 5, 0x88000000);
                break;
            case HAIR:
                pie(c, 0, 6, 30, 30, 180, 180, col);
                rr(c, -30, 4, 14, 24, 7, col); rr(c, 16, 4, 14, 24, 7, col);
                break;
            case SPARK: {
                p.reset();
                p.moveTo(0, -30); p.quadTo(4, -4, 30, 0); p.quadTo(4, 4, 0, 30); p.quadTo(-4, 4, -30, 0); p.quadTo(-4, -4, 0, -30);
                p.close();
                path(c, p, col);
                ci(c, 22, -22, 6, col);
                break;
            }
            case SAVE:
                p.reset();
                p.moveTo(0, 26); p.cubicTo(-40, -4, -26, -30, 0, -14); p.cubicTo(26, -30, 40, -4, 0, 26);
                p.close();
                path(c, p, col);
                break;
            default: break;
        }
        c.restore();
    }

    /** Speech-bubble emotes shown above characters. */
    static void emote(Canvas c, int kind, float x, float y, float s, float t) {
        c.save();
        c.translate(x, y);
        c.scale(s, s);
        Gfx.ol(4);
        poly(c, 0xFFFFFFFF, -14, 20, 14, 20, 0, 42);
        rr(c, -62, -48, 124, 82, 36, 0xFFFFFFFF);
        Gfx.ol(0);
        switch (kind % 7) {
            case 0: // heart
                p.reset();
                p.moveTo(0, 20); p.cubicTo(-46, -6, -26, -34, 0, -14); p.cubicTo(26, -34, 46, -6, 0, 20);
                p.close();
                path(c, p, 0xFFFF5C73);
                break;
            case 1: icon(c, STAR, 0, -7, .9f, 0xFFFFC93C); break;
            case 2: // music note
                ln(c, 10, -26, 10, 10, 7, 0xFF6C7BFF);
                ln(c, 10, -26, 28, -18, 7, 0xFF6C7BFF);
                ov(c, -2, 10, 14, 10, 0xFF6C7BFF);
                break;
            case 3: text(c, "!", 0, 14, 64, 0xFFFF9A3D, android.graphics.Paint.Align.CENTER); break;
            case 4: text(c, "?", 0, 14, 64, 0xFF4FB3FF, android.graphics.Paint.Align.CENTER); break;
            case 5: text(c, "z z z", 0, 10, 40, 0xFF8A93C8, android.graphics.Paint.Align.CENTER); break;
            default: icon(c, SPARK, 0, -7, .85f, 0xFFB67CFF); break;
        }
        c.restore();
    }

    private static void icon(Canvas c, int id, float x, float y, float s, int col) { draw(c, id, x, y, s, col); }
}
