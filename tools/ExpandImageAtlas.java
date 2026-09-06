import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;

/** Reflows an atlas to a new grid while preserving row-major cell indexes. */
public final class ExpandImageAtlas {
    private ExpandImageAtlas() {
    }

    public static void main(String[] args) throws Exception {
        if (args.length != 9) {
            throw new IllegalArgumentException(
                    "Usage: ExpandImageAtlas <input> <output> <old-columns> <cell-size>"
                            + " <new-columns> <new-rows> <replacement-index>"
                            + " <replacement-image> <alpha:true|false>");
        }
        BufferedImage input = requireImage(new File(args[0]));
        int oldColumns = Integer.parseInt(args[2]);
        int cellSize = Integer.parseInt(args[3]);
        int newColumns = Integer.parseInt(args[4]);
        int newRows = Integer.parseInt(args[5]);
        int replacementIndex = Integer.parseInt(args[6]);
        boolean alpha = Boolean.parseBoolean(args[8]);
        if (oldColumns <= 0
                || cellSize <= 0
                || input.getWidth() != oldColumns * cellSize
                || input.getHeight() % cellSize != 0
                || newColumns <= 0
                || newRows <= 0) {
            throw new IllegalArgumentException("Atlas dimensions do not match the grids");
        }
        int oldCellCount = oldColumns * (input.getHeight() / cellSize);
        int newCellCount = newColumns * newRows;
        if (newCellCount < oldCellCount
                || replacementIndex < 0
                || replacementIndex >= newCellCount) {
            throw new IllegalArgumentException("New atlas has insufficient capacity");
        }

        BufferedImage output = new BufferedImage(
                newColumns * cellSize,
                newRows * cellSize,
                alpha ? BufferedImage.TYPE_INT_ARGB : BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = output.createGraphics();
        graphics.setRenderingHint(
                RenderingHints.KEY_INTERPOLATION,
                RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        graphics.setRenderingHint(
                RenderingHints.KEY_RENDERING,
                RenderingHints.VALUE_RENDER_QUALITY);
        for (int index = 0; index < oldCellCount; index++) {
            drawCell(
                    graphics,
                    input,
                    index % oldColumns * cellSize,
                    index / oldColumns * cellSize,
                    index % newColumns * cellSize,
                    index / newColumns * cellSize,
                    cellSize);
        }
        BufferedImage replacement = requireImage(new File(args[7]));
        graphics.drawImage(
                replacement,
                replacementIndex % newColumns * cellSize,
                replacementIndex / newColumns * cellSize,
                replacementIndex % newColumns * cellSize + cellSize,
                replacementIndex / newColumns * cellSize + cellSize,
                0,
                0,
                replacement.getWidth(),
                replacement.getHeight(),
                null);
        graphics.dispose();

        File outputFile = new File(args[1]);
        File parent = outputFile.getParentFile();
        if (parent != null && !parent.isDirectory() && !parent.mkdirs()) {
            throw new IllegalStateException("Cannot create output directory: " + parent);
        }
        if (!ImageIO.write(output, "png", outputFile)) {
            throw new IllegalStateException("No PNG writer is available");
        }
    }

    private static void drawCell(
            Graphics2D graphics,
            BufferedImage input,
            int sourceX,
            int sourceY,
            int targetX,
            int targetY,
            int cellSize) {
        graphics.drawImage(
                input,
                targetX,
                targetY,
                targetX + cellSize,
                targetY + cellSize,
                sourceX,
                sourceY,
                sourceX + cellSize,
                sourceY + cellSize,
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
