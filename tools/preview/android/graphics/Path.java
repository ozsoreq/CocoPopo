package android.graphics;
import java.awt.geom.*;
public class Path {
    public enum Direction { CW, CCW }
    public final Path2D.Float p = new Path2D.Float();
    public void reset() { p.reset(); }
    public void moveTo(float x, float y) { p.moveTo(x, y); }
    public void lineTo(float x, float y) { p.lineTo(x, y); }
    public void quadTo(float a, float b, float x, float y) { p.quadTo(a, b, x, y); }
    public void cubicTo(float a, float b, float c, float d, float x, float y) { p.curveTo(a, b, c, d, x, y); }
    public void close() { p.closePath(); }
    public void addRect(float l, float t, float r, float b, Direction d) { p.append(new Rectangle2D.Float(l, t, r - l, b - t), false); }
    public void addCircle(float x, float y, float r, Direction d) { p.append(new Ellipse2D.Float(x - r, y - r, 2 * r, 2 * r), false); }
    public void addOval(RectF r, Direction d) { p.append(new Ellipse2D.Float(r.left, r.top, r.width(), r.height()), false); }
    public void addRoundRect(RectF r, float rx, float ry, Direction d) { p.append(new RoundRectangle2D.Float(r.left, r.top, r.width(), r.height(), rx * 2, ry * 2), false); }
}
