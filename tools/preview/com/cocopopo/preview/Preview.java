package com.cocopopo.preview;

import android.graphics.Canvas;
import com.cocopopo.app.Previews;
import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;

/** Desktop harness: renders game screens to PNG using the Java2D android.graphics shim. */
public class Preview {
    public static void main(String[] a) throws Exception {
        if (a.length > 0 && a[0].equals("smoke")) {
            BufferedImage im = new BufferedImage(1920, 1080, BufferedImage.TYPE_INT_ARGB);
            System.out.print(Previews.smoke(new Canvas(im.createGraphics()), 1920, 1080));
            return;
        }
        String out = a.length > 0 ? a[0] : "preview-out";
        new File(out).mkdirs();
        int w = 1920, h = 1080;
        String[] which = a.length > 1 ? java.util.Arrays.copyOfRange(a, 1, a.length) : new String[]{"cast"};
        for (String name : which) {
            if (name.equals("icon")) { w = 512; h = 512; }
            BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
            Canvas c = new Canvas(img.createGraphics());
            Previews.render(name, c, w, h);
            ImageIO.write(img, "png", new File(out, name + ".png"));
            System.out.println("wrote " + name);
        }
    }
}
