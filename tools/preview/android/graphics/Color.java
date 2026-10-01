package android.graphics;
public class Color {
    public static final int WHITE = 0xFFFFFFFF, BLACK = 0xFF000000, TRANSPARENT = 0;
    public static int argb(int a, int r, int g, int b) { return (a << 24) | (r << 16) | (g << 8) | b; }
    public static int rgb(int r, int g, int b) { return 0xFF000000 | (r << 16) | (g << 8) | b; }
    public static int parseColor(String s) { return (int) Long.parseLong(s.substring(1), 16) | 0xFF000000; }
}
