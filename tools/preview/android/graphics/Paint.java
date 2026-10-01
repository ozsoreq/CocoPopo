package android.graphics;
public class Paint {
    public static final int ANTI_ALIAS_FLAG = 1;
    public enum Style { FILL, STROKE, FILL_AND_STROKE }
    public enum Align { LEFT, CENTER, RIGHT }
    public enum Cap { BUTT, ROUND, SQUARE }
    public enum Join { MITER, ROUND, BEVEL }
    public int color = 0xFF000000; public Style style = Style.FILL; public float strokeWidth = 1, textSize = 12;
    public Align align = Align.LEFT; public Cap cap = Cap.BUTT; public Join join = Join.MITER; public boolean bold = true;
    public Paint() {}
    public Paint(int flags) {}
    public void setColor(int c) { color = c; }
    public int getColor() { return color; }
    public void setAlpha(int a) { color = (color & 0xFFFFFF) | (a << 24); }
    public void setStyle(Style s) { style = s; }
    public void setStrokeWidth(float w) { strokeWidth = w; }
    public void setStrokeCap(Cap c) { cap = c; }
    public void setStrokeJoin(Join j) { join = j; }
    public void setTextSize(float s) { textSize = s; }
    public void setTextAlign(Align a) { align = a; }
    public void setTypeface(Typeface t) {}
    public void setAntiAlias(boolean b) {}
    public float measureText(String s) {
        java.awt.Font f = Canvas.font(textSize);
        return (float) f.getStringBounds(s, new java.awt.font.FontRenderContext(null, true, true)).getWidth();
    }
}
