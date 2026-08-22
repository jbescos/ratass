import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;

/** Replaces selected square cells while preserving every other cell in an image atlas. */
public final class ReplaceImageAtlasCells {
    private ReplaceImageAtlasCells() {
    }

    public static void main(String[] args) throws Exception {
        if (args.length < 6 || (args.length - 4) % 2 != 0) {
            throw new IllegalArgumentException(
                    "Usage: ReplaceImageAtlasCells <input> <output> <columns> <cell-size>"
                            + " <cell-index> <image> [<cell-index> <image> ...]");
        }

        File inputFile = new File(args[0]);
        File outputFile = new File(args[1]);
        int columns = Integer.parseInt(args[2]);
        int cellSize = Integer.parseInt(args[3]);
        BufferedImage input = requireImage(inputFile);
        if (columns <= 0
                || cellSize <= 0
                || input.getWidth() != columns * cellSize
                || input.getHeight() % cellSize != 0) {
            throw new IllegalArgumentException("Input atlas dimensions do not match its grid");
        }

        BufferedImage output = new BufferedImage(
                input.getWidth(),
                input.getHeight(),
                BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = output.createGraphics();
        graphics.drawImage(input, 0, 0, null);
        graphics.setRenderingHint(
                RenderingHints.KEY_INTERPOLATION,
                RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        graphics.setRenderingHint(
                RenderingHints.KEY_RENDERING,
                RenderingHints.VALUE_RENDER_QUALITY);

        int cellCount = columns * (input.getHeight() / cellSize);
        for (int index = 4; index < args.length; index += 2) {
            int cellIndex = Integer.parseInt(args[index]);
            if (cellIndex < 0 || cellIndex >= cellCount) {
                throw new IllegalArgumentException("Cell index is outside atlas: " + cellIndex);
            }
            BufferedImage replacement = requireImage(new File(args[index + 1]));
            int targetX = cellIndex % columns * cellSize;
            int targetY = cellIndex / columns * cellSize;
            graphics.drawImage(
                    replacement,
                    targetX,
                    targetY,
                    targetX + cellSize,
                    targetY + cellSize,
                    0,
                    0,
                    replacement.getWidth(),
                    replacement.getHeight(),
                    null);
        }
        graphics.dispose();

        File parent = outputFile.getParentFile();
        if (parent != null && !parent.isDirectory() && !parent.mkdirs()) {
            throw new IllegalStateException("Cannot create output directory: " + parent);
        }
        if (!ImageIO.write(output, "png", outputFile)) {
            throw new IllegalStateException("No PNG writer is available");
        }
    }

    private static BufferedImage requireImage(File file) throws Exception {
        BufferedImage image = ImageIO.read(file);
        if (image == null) {
            throw new IllegalArgumentException("Cannot read image: " + file);
        }
        return image;
    }
}
