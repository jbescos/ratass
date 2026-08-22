import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;

/** Builds a hollow aura by placing an image at four points around the car. */
public final class BuildOrbitAura {
    private static final int OUTPUT_SIZE = 256;

    private BuildOrbitAura() {
    }

    public static void main(String[] args) throws Exception {
        if (args.length != 4) {
            throw new IllegalArgumentException(
                    "Usage: BuildOrbitAura <icon> <background-or-dash> <icon-size> <output>");
        }
        BufferedImage icon = requireImage(new File(args[0]));
        BufferedImage background = "-".equals(args[1])
                ? null : requireImage(new File(args[1]));
        int iconSize = Integer.parseInt(args[2]);
        if (iconSize <= 0 || iconSize > OUTPUT_SIZE / 2) {
            throw new IllegalArgumentException("Icon size is outside the supported range");
        }

        BufferedImage output = new BufferedImage(
                OUTPUT_SIZE,
                OUTPUT_SIZE,
                BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = output.createGraphics();
        graphics.setRenderingHint(
                RenderingHints.KEY_INTERPOLATION,
                RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        graphics.setRenderingHint(
                RenderingHints.KEY_RENDERING,
                RenderingHints.VALUE_RENDER_QUALITY);
        if (background != null) {
            graphics.drawImage(background, 0, 0, OUTPUT_SIZE, OUTPUT_SIZE, null);
        }

        int radius = Math.round(OUTPUT_SIZE * 0.36f);
        drawCentered(graphics, icon, OUTPUT_SIZE / 2, OUTPUT_SIZE / 2 - radius, iconSize);
        drawCentered(graphics, icon, OUTPUT_SIZE / 2 + radius, OUTPUT_SIZE / 2, iconSize);
        drawCentered(graphics, icon, OUTPUT_SIZE / 2, OUTPUT_SIZE / 2 + radius, iconSize);
        drawCentered(graphics, icon, OUTPUT_SIZE / 2 - radius, OUTPUT_SIZE / 2, iconSize);
        graphics.dispose();

        File outputFile = new File(args[3]);
        File parent = outputFile.getParentFile();
        if (parent != null && !parent.isDirectory() && !parent.mkdirs()) {
            throw new IllegalStateException("Cannot create output directory: " + parent);
        }
        if (!ImageIO.write(output, "png", outputFile)) {
            throw new IllegalStateException("No PNG writer is available");
        }
    }

    private static void drawCentered(
            Graphics2D graphics,
            BufferedImage image,
            int centerX,
            int centerY,
            int size) {
        int half = size / 2;
        graphics.drawImage(
                image,
                centerX - half,
                centerY - half,
                centerX - half + size,
                centerY - half + size,
                0,
                0,
                image.getWidth(),
                image.getHeight(),
                null);
    }

    private static BufferedImage requireImage(File file) throws Exception {
        BufferedImage image = ImageIO.read(file);
        if (image == null) {
            throw new IllegalArgumentException("Cannot read image: " + file);
        }
        return image;
    }
}
