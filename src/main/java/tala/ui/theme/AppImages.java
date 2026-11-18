package tala.ui.theme;

import javax.imageio.ImageIO;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * AppImages
 * ------------------------------------------------------------
 * Loads the TALA logo (images/logo.png) once and gives it out in the sizes we need:
 *   - the logo in the purple top bar
 *   - the icon in the window's title bar and the Windows taskbar
 *
 * To change the logo, replace  src/main/resources/images/logo.png
 * with your own square picture (512 x 512 looks sharp everywhere).
 */
public final class AppImages {

    private static BufferedImage logo;
    private static boolean alreadyTried = false;

    private AppImages() { }

    /** The full-size logo, or null if the file cannot be found */
    public static BufferedImage getLogo() {
        if (!alreadyTried) {
            alreadyTried = true;
            logo = loadLogo();
        }
        return logo;
    }

    /** The logo shrunk smoothly to size x size pixels (null if there is no logo) */
    public static BufferedImage getLogo(int size) {
        BufferedImage full = getLogo();
        if (full == null) {
            return null;
        }
        // SCALE_SMOOTH gives a much cleaner small picture than simple resizing
        Image smooth = full.getScaledInstance(size, size, Image.SCALE_SMOOTH);
        BufferedImage result = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        java.awt.Graphics2D g = result.createGraphics();
        g.drawImage(smooth, 0, 0, null);
        g.dispose();
        return result;
    }

    /**
     * Several sizes for the window icon. Windows picks the best one
     * for the title bar (small) and the taskbar (bigger).
     */
    public static List<Image> getWindowIcons() {
        List<Image> icons = new ArrayList<>();
        if (getLogo() != null) {
            for (int size : new int[] {16, 20, 24, 32, 40, 48, 64, 128, 256}) {
                icons.add(getLogo(size));
            }
        }
        return icons;
    }

    /**
     * Looks for the picture in two places:
     *   1. inside the program's classpath (Maven, NetBeans and run.bat put it there)
     *   2. as a normal file next to the project, in case the classpath has no copy
     */
    private static BufferedImage loadLogo() {
        try (InputStream in = AppImages.class.getResourceAsStream("/images/logo.png")) {
            if (in != null) {
                return ImageIO.read(in);
            }
        } catch (Exception ignored) {
            // try the files below
        }
        String[] files = {"src/main/resources/images/logo.png", "images/logo.png", "src/images/logo.png"};
        for (String path : files) {
            try {
                File file = new File(path);
                if (file.exists()) {
                    return ImageIO.read(file);
                }
            } catch (Exception ignored) {
                // try the next place
            }
        }
        return null;   // no logo found: the app draws a yellow "T" instead
    }
}
