package android.graphics;
import java.awt.*;
import java.awt.geom.*;
import java.util.ArrayDeque;

/** Desktop Java2D stand-in for android.graphics.Canvas (preview tooling only). */
public class Canvas {
    public final Graphics2D g;
    private final ArrayDeque<Object[]> stack = new ArrayDeque<>();
    private static String fontName = null;
    public Canvas(Graphics2D g) {
        this.g = g;
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        g.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
    }
    static Font font(float size) {
        if (fontName == null) {
            fontName = "SansSerif";
            for (String n : GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames())
                if (n.equals("Noto Sans") || n.equals("DejaVu Sans")) { fontName = n; if (n.equals("Noto Sans")) break; }
        }
        return new Font(fontName, Font.BOLD, 1).deriveFont(size);
    }
    public int save() { stack.push(new Object[]{g.getTransform(), g.getClip()}); return stack.size(); }
    public void restore() { Object[] o = stack.pop(); g.setTransform((AffineTransform) o[0]); g.setClip((Shape) o[1]); }
    public void translate(float x, float y) { g.translate(x, y); }
    public void scale(float x, float y) { g.scale(x, y); }
    public void rotate(float d) { g.rotate(Math.toRadians(d)); }
    public void rotate(float d, float px, float py) { g.rotate(Math.toRadians(d), px, py); }
    private void paint(Shape s, Paint p) {
        g.setColor(new java.awt.Color(p.color, true));
        if (p.style == Paint.Style.STROKE) {
            int cap = p.cap == Paint.Cap.ROUND ? BasicStroke.CAP_ROUND : p.cap == Paint.Cap.SQUARE ? BasicStroke.CAP_SQUARE : BasicStroke.CAP_BUTT;
            int join = p.join == Paint.Join.ROUND ? BasicStroke.JOIN_ROUND : p.join == Paint.Join.BEVEL ? BasicStroke.JOIN_BEVEL : BasicStroke.JOIN_MITER;
            g.setStroke(new BasicStroke(p.strokeWidth, cap, join));
            g.draw(s);
        } else g.fill(s);
    }
    public void drawColor(int c) { g.setColor(new java.awt.Color(c, true)); Shape cl = g.getClip(); g.fill(new Rectangle(-100000, -100000, 200000, 200000)); }
    public void drawRect(float l, float t, float r, float b, Paint p) { paint(new Rectangle2D.Float(l, t, r - l, b - t), p); }
    public void drawRect(RectF r, Paint p) { drawRect(r.left, r.top, r.right, r.bottom, p); }
    public void drawRoundRect(RectF r, float rx, float ry, Paint p) { paint(new RoundRectangle2D.Float(r.left, r.top, r.width(), r.height(), rx * 2, ry * 2), p); }
    public void drawRoundRect(float l, float t, float r, float b, float rx, float ry, Paint p) { paint(new RoundRectangle2D.Float(l, t, r - l, b - t, rx * 2, ry * 2), p); }
    public void drawCircle(float x, float y, float r, Paint p) { paint(new Ellipse2D.Float(x - r, y - r, 2 * r, 2 * r), p); }
    public void drawOval(RectF r, Paint p) { paint(new Ellipse2D.Float(r.left, r.top, r.width(), r.height()), p); }
    public void drawOval(float l, float t, float r, float b, Paint p) { paint(new Ellipse2D.Float(l, t, r - l, b - t), p); }
    public void drawLine(float x1, float y1, float x2, float y2, Paint p) { Paint q = new Paint(); q.color = p.color; q.style = Paint.Style.STROKE; q.strokeWidth = p.strokeWidth; q.cap = p.cap; paint(new Line2D.Float(x1, y1, x2, y2), q); }
    public void drawPath(Path path, Paint p) { paint(path.p, p); }
    public void drawArc(RectF r, float start, float sweep, boolean center, Paint p) {
        Arc2D.Float a = new Arc2D.Float(r.left, r.top, r.width(), r.height(), -start, -sweep, center ? Arc2D.PIE : Arc2D.OPEN);
        paint(a, p);
    }
    public void drawText(String s, float x, float y, Paint p) {
        Font f = font(p.textSize);
        java.awt.font.GlyphVector gv = f.createGlyphVector(g.getFontRenderContext(), s);
        float w = (float) gv.getLogicalBounds().getWidth();
        if (p.align == Paint.Align.CENTER) x -= w / 2; else if (p.align == Paint.Align.RIGHT) x -= w;
        Shape sh = gv.getOutline(x, y);
        paint(sh, p);
    }
    public void clipRect(float l, float t, float r, float b) { g.clip(new Rectangle2D.Float(l, t, r - l, b - t)); }
    public void clipRect(RectF r) { clipRect(r.left, r.top, r.right, r.bottom); }
    public void clipPath(Path p) { g.clip(p.p); }
}
