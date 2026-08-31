import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Path2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;

/** Builds the four-state checkbox icon atlas used by menus and loadout controls. */
public final class BuildCheckboxIconAtlas {
    private static final int CELL_SIZE = 64;
    private static final int STATE_COUNT = 4;

    private BuildCheckboxIconAtlas() {
    }

    public static void main(String[] args) throws IOException {
        if (args.length != 1) {
            System.err.println("Usage: BuildCheckboxIconAtlas <output.png>");
            System.exit(2);
        }
        BufferedImage atlas = new BufferedImage(
                CELL_SIZE,
                CELL_SIZE * STATE_COUNT,
                BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = atlas.createGraphics();
        configureHighQuality(graphics);
        drawState(graphics, 0, false, true);
        drawState(graphics, 1, true, true);
        drawState(graphics, 2, false, false);
        drawState(graphics, 3, true, false);
        graphics.dispose();

        File output = new File(args[0]);
        File parent = output.getParentFile();
        if (parent != null && !parent.isDirectory() && !parent.mkdirs()) {
            throw new IOException("Could not create " + parent);
        }
        if (!ImageIO.write(atlas, "png", output)) {
            throw new IOException("No PNG writer available");
        }
    }

    private static void drawState(
            Graphics2D graphics,
            int row,
            boolean checked,
            boolean enabled) {
        int y = row * CELL_SIZE;
        RoundRectangle2D.Float shadow = new RoundRectangle2D.Float(7f, y + 9f, 50f, 50f, 12f, 12f);
        RoundRectangle2D.Float plate = new RoundRectangle2D.Float(7f, y + 6f, 50f, 50f, 12f, 12f);

        graphics.setColor(new Color(0, 0, 0, enabled ? 150 : 80));
        graphics.fill(shadow);
        graphics.setColor(enabled ? new Color(12, 22, 27, 248) : new Color(24, 29, 31, 215));
        graphics.fill(plate);
        graphics.setStroke(new BasicStroke(4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        graphics.setColor(enabled ? new Color(195, 220, 224, 255) : new Color(91, 100, 103, 220));
        graphics.draw(plate);

        if (!checked) {
            return;
        }
        Path2D.Float check = new Path2D.Float();
        check.moveTo(17f, y + 31f);
        check.lineTo(27f, y + 41f);
        check.lineTo(48f, y + 19f);
        graphics.setStroke(new BasicStroke(8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        graphics.setColor(new Color(0, 0, 0, enabled ? 180 : 90));
        graphics.translate(1.5, 2.0);
        graphics.draw(check);
        graphics.translate(-1.5, -2.0);
        graphics.setColor(enabled ? new Color(73, 235, 137, 255) : new Color(105, 122, 112, 230));
        graphics.draw(check);
    }

    private static void configureHighQuality(Graphics2D graphics) {
        graphics.setRenderingHint(
                RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);
        graphics.setRenderingHint(
                RenderingHints.KEY_RENDERING,
                RenderingHints.VALUE_RENDER_QUALITY);
        graphics.setRenderingHint(
                RenderingHints.KEY_STROKE_CONTROL,
                RenderingHints.VALUE_STROKE_PURE);
    }
}
