import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import javax.imageio.ImageIO;

/** Converts the generated chroma-key effect strip into a compact alpha atlas. */
public final class BuildAbilityEffectAtlas {
    private static final int OUTPUT_CELL_SIZE = 256;
    private static final int SOURCE_CELL_INSET = 3;

    private BuildAbilityEffectAtlas() {
    }

    public static void main(String[] args) throws Exception {
        if (args.length < 7) {
            throw new IllegalArgumentException(
                    "Usage: BuildAbilityEffectAtlas <source> <output>"
                            + " <crop-x> <crop-y> <crop-width> <crop-height> <columns>"
                            + " [--replace-column <index> <transparent.png>]"
                            + " [--hollow-column <output-index>]"
                            + " [transparent-extra.png ...]");
        }

        File sourceFile = new File(args[0]);
        File outputFile = new File(args[1]);
        int cropX = Integer.parseInt(args[2]);
        int cropY = Integer.parseInt(args[3]);
        int cropWidth = Integer.parseInt(args[4]);
        int cropHeight = Integer.parseInt(args[5]);
        int columns = Integer.parseInt(args[6]);
        List<ColumnReplacement> replacements = new ArrayList<>();
        List<File> extras = new ArrayList<>();
        List<Integer> hollowColumns = new ArrayList<>();
        parseAdditionalSources(args, columns, replacements, extras, hollowColumns);

        BufferedImage source = ImageIO.read(sourceFile);
        validateCrop(source, cropX, cropY, cropWidth, cropHeight, columns);

        BufferedImage output =
                new BufferedImage(
                        OUTPUT_CELL_SIZE * (columns + extras.size()),
                        OUTPUT_CELL_SIZE,
                        BufferedImage.TYPE_INT_ARGB);
        for (int column = 0; column < columns; column++) {
            int sourceX0 = cropX + Math.round(column * cropWidth / (float) columns);
            int sourceX1 = cropX + Math.round((column + 1) * cropWidth / (float) columns);
            int cellX = sourceX0 + SOURCE_CELL_INSET;
            int cellY = cropY + SOURCE_CELL_INSET;
            int cellWidth = Math.max(1, sourceX1 - sourceX0 - SOURCE_CELL_INSET * 2);
            int cellHeight = Math.max(1, cropHeight - SOURCE_CELL_INSET * 2);

            BufferedImage scaled =
                    new BufferedImage(
                            OUTPUT_CELL_SIZE,
                            OUTPUT_CELL_SIZE,
                            BufferedImage.TYPE_INT_RGB);
            Graphics2D graphics = scaled.createGraphics();
            graphics.setRenderingHint(
                    RenderingHints.KEY_INTERPOLATION,
                    RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            graphics.drawImage(
                    source,
                    0,
                    0,
                    OUTPUT_CELL_SIZE,
                    OUTPUT_CELL_SIZE,
                    cellX,
                    cellY,
                    cellX + cellWidth,
                    cellY + cellHeight,
                    null);
            graphics.dispose();

            removeGreenScreen(scaled, output, column * OUTPUT_CELL_SIZE);
        }

        Graphics2D overlayGraphics = output.createGraphics();
        overlayGraphics.setRenderingHint(
                RenderingHints.KEY_INTERPOLATION,
                RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        overlayGraphics.setRenderingHint(
                RenderingHints.KEY_RENDERING,
                RenderingHints.VALUE_RENDER_QUALITY);
        for (int extra = 0; extra < extras.size(); extra++) {
            BufferedImage image = readTransparentSource(extras.get(extra));
            int targetX = (columns + extra) * OUTPUT_CELL_SIZE;
            drawCell(overlayGraphics, image, targetX);
        }
        for (ColumnReplacement replacement : replacements) {
            BufferedImage image = readTransparentSource(replacement.file);
            drawCell(overlayGraphics, image, replacement.column * OUTPUT_CELL_SIZE);
        }
        overlayGraphics.dispose();
        for (int column : hollowColumns) {
            if (column < 0 || column >= columns + extras.size()) {
                throw new IllegalArgumentException(
                        "Hollow column is outside output atlas: " + column);
            }
            clearCenterForCar(output, column * OUTPUT_CELL_SIZE);
        }

        File parent = outputFile.getParentFile();
        if (parent != null) {
            parent.mkdirs();
        }
        ImageIO.write(output, "png", outputFile);
    }

    private static void parseAdditionalSources(
            String[] args,
            int columns,
            List<ColumnReplacement> replacements,
            List<File> extras,
            List<Integer> hollowColumns) {
        for (int index = 7; index < args.length;) {
            if ("--hollow-column".equals(args[index])) {
                if (index + 1 >= args.length) {
                    throw new IllegalArgumentException(
                            "--hollow-column requires an output column index");
                }
                hollowColumns.add(Integer.parseInt(args[index + 1]));
                index += 2;
            } else if (!"--replace-column".equals(args[index])) {
                extras.add(new File(args[index++]));
            } else {
                if (index + 2 >= args.length) {
                    throw new IllegalArgumentException(
                            "--replace-column requires an index and image path");
                }
                int column = Integer.parseInt(args[index + 1]);
                if (column < 0 || column >= columns) {
                    throw new IllegalArgumentException(
                            "Replacement column is outside source atlas");
                }
                replacements.add(new ColumnReplacement(column, new File(args[index + 2])));
                index += 3;
            }
        }
    }

    private static void clearCenterForCar(BufferedImage image, int targetX) {
        float center = (OUTPUT_CELL_SIZE - 1) * 0.5f;
        float transparentRadius = OUTPUT_CELL_SIZE * 0.23f;
        float featherRadius = OUTPUT_CELL_SIZE * 0.30f;
        for (int y = 0; y < OUTPUT_CELL_SIZE; y++) {
            for (int x = 0; x < OUTPUT_CELL_SIZE; x++) {
                float dx = x - center;
                float dy = y - center;
                float distance = (float) Math.sqrt(dx * dx + dy * dy);
                if (distance >= featherRadius) {
                    continue;
                }
                int argb = image.getRGB(targetX + x, y);
                int alpha = (argb >>> 24) & 0xff;
                float alphaScale = smoothStep(
                        transparentRadius,
                        featherRadius,
                        distance);
                int adjustedAlpha = Math.round(alpha * alphaScale);
                image.setRGB(targetX + x, y, (argb & 0x00ffffff) | (adjustedAlpha << 24));
            }
        }
    }

    private static BufferedImage readTransparentSource(File file) throws Exception {
        BufferedImage image = ImageIO.read(file);
        if (image == null) {
            throw new IllegalArgumentException(
                    "Transparent source is not a readable image: " + file);
        }
        return image;
    }

    private static void drawCell(Graphics2D graphics, BufferedImage image, int targetX) {
        graphics.drawImage(
                image,
                targetX,
                0,
                targetX + OUTPUT_CELL_SIZE,
                OUTPUT_CELL_SIZE,
                0,
                0,
                image.getWidth(),
                image.getHeight(),
                null);
    }

    private static void validateCrop(
            BufferedImage source,
            int cropX,
            int cropY,
            int cropWidth,
            int cropHeight,
            int columns) {
        if (source == null) {
            throw new IllegalArgumentException("Source is not a readable image");
        }
        if (columns <= 0
                || cropX < 0
                || cropY < 0
                || cropWidth <= 0
                || cropHeight <= 0
                || cropX + cropWidth > source.getWidth()
                || cropY + cropHeight > source.getHeight()) {
            throw new IllegalArgumentException("Invalid source crop");
        }
    }

    private static void removeGreenScreen(
            BufferedImage source,
            BufferedImage target,
            int targetX) {
        for (int y = 0; y < OUTPUT_CELL_SIZE; y++) {
            for (int x = 0; x < OUTPUT_CELL_SIZE; x++) {
                int rgb = source.getRGB(x, y);
                int red = (rgb >>> 16) & 0xff;
                int green = (rgb >>> 8) & 0xff;
                int blue = rgb & 0xff;

                int nonKeyColor = Math.max(red, blue);
                float alpha = smoothStep(18f, 138f, nonKeyColor);
                if (green - nonKeyColor < 55) {
                    alpha = Math.max(alpha, smoothStep(34f, 120f, colorSpread(red, green, blue)));
                }
                int alphaByte = Math.round(alpha * 255f);
                if (alphaByte == 0) {
                    target.setRGB(targetX + x, y, 0);
                    continue;
                }

                int despilledGreen = Math.min(green, Math.round(nonKeyColor * 1.06f + 8f));
                target.setRGB(
                        targetX + x,
                        y,
                        (alphaByte << 24)
                                | (red << 16)
                                | (despilledGreen << 8)
                                | blue);
            }
        }
    }

    private static int colorSpread(int red, int green, int blue) {
        int max = Math.max(red, Math.max(green, blue));
        int min = Math.min(red, Math.min(green, blue));
        return max - min;
    }

    private static float smoothStep(float low, float high, float value) {
        float normalized = Math.max(0f, Math.min(1f, (value - low) / (high - low)));
        return normalized * normalized * (3f - 2f * normalized);
    }

    private static final class ColumnReplacement {
        private final int column;
        private final File file;

        private ColumnReplacement(int column, File file) {
            this.column = column;
            this.file = file;
        }
    }
}
