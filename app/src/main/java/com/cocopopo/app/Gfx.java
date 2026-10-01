package com.cocopopo.app;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Typeface;

/** Tiny immediate-mode drawing helpers shared by every screen (flat "toy" look). */
final class Gfx {
    static final Paint P = new Paint(Paint.ANTI_ALIAS_FLAG);
    static final RectF R = new RectF();
    static final Path PA = new Path();

    static {
        P.setStrokeCap(Paint.Cap.ROUND);
        P.setStrokeJoin(Paint.Join.ROUND);
        P.setTypeface(Typeface.DEFAULT_BOLD);
    }

    private Gfx() {}

    static int rgb(int hex) { return 0xFF000000 | hex; }

    /** Darken by fraction f (0..1). */
    static int dk(int c, float f) {
        int r = (int) (((c >> 16) & 255) * (1 - f));
        int g = (int) (((c >> 8) & 255) * (1 - f));
        int b = (int) ((c & 255) * (1 - f));
        return (c & 0xFF000000) | (r << 16) | (g << 8) | b;
    }

    /** Lighten towards white by fraction f (0..1). */
    static int lt(int c, float f) {
        int r = (c >> 16) & 255, g = (c >> 8) & 255, b = c & 255;
        r += (255 - r) * f; g += (255 - g) * f; b += (255 - b) * f;
        return (c & 0xFF000000) | (r << 16) | (g << 8) | b;
    }

    static int al(int c, int a) { return (c & 0x00FFFFFF) | (a << 24); }

    static int mix(int a, int b, float t) {
        int ar = (a >> 16) & 255, ag = (a >> 8) & 255, ab = a & 255;
        int br = (b >> 16) & 255, bg = (b >> 8) & 255, bb = b & 255;
        return 0xFF000000 | ((int) (ar + (br - ar) * t) << 16) | ((int) (ag + (bg - ag) * t) << 8) | (int) (ab + (bb - ab) * t);
    }

    /** Outline state: width in local units (0 = off). Applies to filled shapes that are big & opaque enough. */
    static float olw = 0;
    static int olc = 0xFF2A1F2E;
    static boolean shade = true;

    static void ol(float w) { olw = w; }

    private static boolean wantOL(float minDim, int col) {
        if (olw <= 0 || minDim < 22 || (col >>> 24) != 255) return false;
        int lum = (int) (((col >> 16) & 255) * .3f + ((col >> 8) & 255) * .6f + (col & 255) * .1f);
        return lum > 48;
    }

    private static void olStroke() {
        P.setStyle(Paint.Style.STROKE);
        P.setColor(olc);
        P.setStrokeWidth(olw * 2);
    }

    /** Soft highlight/shadow so flat shapes read as slightly puffy, like Toca's shading. */
    private static void shadeRR(Canvas c, float x, float y, float w, float h, float r, int col) {
        if (!shade || olw <= 0 || Math.min(w, h) < 46) return;
        float in = Math.min(w, h) * .09f;
        // light crown
        fill(lt(col, .16f));
        R.set(x + in, y + in * .7f, x + w - in, y + h * .46f);
        float rr = Math.min(r, Math.min(R.width(), R.height()) / 2);
        c.drawRoundRect(R, rr, rr, P);
        // soft shadow along the bottom
        fill(al(dk(col, .3f), 70));
        R.set(x + in * .6f, y + h - Math.min(h * .2f, 22), x + w - in * .6f, y + h - in * .5f);
        c.drawRoundRect(R, rr, rr, P);
    }

    static void fill(int col) { P.setStyle(Paint.Style.FILL); P.setColor(col); }

    static void stroke(int col, float w) { P.setStyle(Paint.Style.STROKE); P.setColor(col); P.setStrokeWidth(w); }

    /** Round rect by top-left corner. */
    static void rr(Canvas c, float x, float y, float w, float h, float r, int col) {
        float m = Math.min(w, h) / 2;
        if (r > m) r = m;
        boolean o = wantOL(Math.min(w, h), col);
        if (o) {
            olStroke();
            R.set(x, y, x + w, y + h);
            c.drawRoundRect(R, r, r, P);
        }
        fill(col);
        R.set(x, y, x + w, y + h);
        c.drawRoundRect(R, r, r, P);
        if (o) shadeRR(c, x, y, w, h, r, col);
    }

    static void rrs(Canvas c, float x, float y, float w, float h, float r, int col, float sw) {
        stroke(col, sw);
        R.set(x, y, x + w, y + h);
        float m = Math.min(w, h) / 2;
        if (r > m) r = m;
        c.drawRoundRect(R, r, r, P);
    }

    /** Outlined plain rectangle (walls, slabs). */
    static void rectOL(Canvas c, float x, float y, float w, float h, int col) {
        if (olw > 0) { olStroke(); c.drawRect(x, y, x + w, y + h, P); }
        fill(col);
        c.drawRect(x, y, x + w, y + h, P);
    }

    static void rect(Canvas c, float x, float y, float w, float h, int col) {
        fill(col);
        c.drawRect(x, y, x + w, y + h, P);
    }

    static void ci(Canvas c, float x, float y, float r, int col) {
        boolean o = wantOL(r * 2, col);
        if (o) { olStroke(); c.drawCircle(x, y, r, P); }
        fill(col);
        c.drawCircle(x, y, r, P);
        if (o && shade && r >= 26) {
            fill(lt(col, .16f));
            R.set(x - r * .62f, y - r * .8f, x + r * .15f, y - r * .22f);
            c.drawOval(R, P);
        }
    }

    static void cis(Canvas c, float x, float y, float r, int col, float sw) {
        stroke(col, sw);
        c.drawCircle(x, y, r, P);
    }

    static void ov(Canvas c, float x, float y, float rx, float ry, int col) {
        boolean o = wantOL(Math.min(rx, ry) * 2, col);
        if (o) { olStroke(); R.set(x - rx, y - ry, x + rx, y + ry); c.drawOval(R, P); }
        fill(col);
        R.set(x - rx, y - ry, x + rx, y + ry);
        c.drawOval(R, P);
    }

    static void ovs(Canvas c, float x, float y, float rx, float ry, int col, float sw) {
        stroke(col, sw);
        R.set(x - rx, y - ry, x + rx, y + ry);
        c.drawOval(R, P);
    }

    static void ln(Canvas c, float x1, float y1, float x2, float y2, float w, int col) {
        if (olw > 0 && w >= 9 && (col >>> 24) == 255) {
            stroke(olc, w + olw * 2);
            c.drawLine(x1, y1, x2, y2, P);
        }
        stroke(col, w);
        c.drawLine(x1, y1, x2, y2, P);
    }

    static void arc(Canvas c, float cx, float cy, float rx, float ry, float start, float sweep, float w, int col) {
        stroke(col, w);
        R.set(cx - rx, cy - ry, cx + rx, cy + ry);
        c.drawArc(R, start, sweep, false, P);
    }

    static void pie(Canvas c, float cx, float cy, float rx, float ry, float start, float sweep, int col) {
        if (wantOL(Math.min(rx, ry) * 1.2f, col)) {
            olStroke();
            R.set(cx - rx, cy - ry, cx + rx, cy + ry);
            c.drawArc(R, start, sweep, true, P);
        }
        fill(col);
        R.set(cx - rx, cy - ry, cx + rx, cy + ry);
        c.drawArc(R, start, sweep, true, P);
    }

    static void poly(Canvas c, int col, float... p) {
        PA.reset();
        PA.moveTo(p[0], p[1]);
        float x0 = p[0], x1 = p[0], y0 = p[1], y1 = p[1];
        for (int i = 2; i + 1 < p.length; i += 2) {
            PA.lineTo(p[i], p[i + 1]);
            x0 = Math.min(x0, p[i]); x1 = Math.max(x1, p[i]); y0 = Math.min(y0, p[i + 1]); y1 = Math.max(y1, p[i + 1]);
        }
        PA.close();
        if (wantOL(Math.max(x1 - x0, y1 - y0) * .7f, col)) { olStroke(); c.drawPath(PA, P); }
        fill(col);
        c.drawPath(PA, P);
    }

    static void path(Canvas c, Path p, int col) {
        if (wantOL(40, col)) { olStroke(); c.drawPath(p, P); }
        fill(col);
        c.drawPath(p, P);
    }

    static void pathS(Canvas c, Path p, int col, float w) {
        stroke(col, w);
        c.drawPath(p, P);
    }

    static void text(Canvas c, String s, float x, float y, float size, int col, Paint.Align a) {
        P.setStyle(Paint.Style.FILL);
        P.setTextSize(size);
        P.setTextAlign(a);
        P.setColor(col);
        c.drawText(s, x, y, P);
    }

    static void textOut(Canvas c, String s, float x, float y, float size, int col, int outline, float ow) {
        P.setTextSize(size);
        P.setTextAlign(Paint.Align.CENTER);
        P.setStyle(Paint.Style.STROKE);
        P.setStrokeWidth(ow);
        P.setColor(outline);
        c.drawText(s, x, y, P);
        P.setStyle(Paint.Style.FILL);
        P.setColor(col);
        c.drawText(s, x, y, P);
    }

    static float tw(String s, float size) {
        P.setTextSize(size);
        return P.measureText(s);
    }

    /** Vertical gradient faked with strips (keeps drawing code portable and cheap). */
    static void grad(Canvas c, float x, float y, float w, float h, int top, int bottom) {
        int n = 24;
        float sh = h / n;
        for (int i = 0; i < n; i++) {
            fill(mix(top, bottom, i / (float) (n - 1)));
            c.drawRect(x, y + i * sh, x + w, y + (i + 1) * sh + 1, P);
        }
    }

    static float clamp(float v, float lo, float hi) { return v < lo ? lo : (v > hi ? hi : v); }

    static float ease(float t) { t = clamp(t, 0, 1); return t * t * (3 - 2 * t); }

    /** Overshooting ease used for pop-in animations. */
    static float back(float t) {
        t = clamp(t, 0, 1) - 1;
        return t * t * (2.7f * t + 1.7f) + 1;
    }
}
