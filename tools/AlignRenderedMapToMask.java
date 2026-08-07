import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;

/**
 * Rebuilds the visible road surface from the authoritative gameplay mask.
 *
 * <p>This is an offline artwork tool. It deliberately has no dependency on the
 * game runtime so presentation corrections cannot affect map geometry or RL.
 */
public final class AlignRenderedMapToMask {
    private static final int INFINITE_DISTANCE = 1_000_000;
    private static final int ORTHOGONAL_COST = 3;
    private static final int DIAGONAL_COST = 4;
    private static final float SHOULDER_PIXELS = 4.5f;
    private static final float GENERATED_MARKER_CLEANUP_PIXELS = 24f;

    private AlignRenderedMapToMask() {
    }

    public static void main(String[] args) throws IOException {
        if (args.length != 3) {
            System.err.println("Usage: AlignRenderedMapToMask <artwork.png> <mask.png> <output.png>");
            System.exit(2);
        }

        File artworkFile = new File(args[0]);
        File maskFile = new File(args[1]);
        BufferedImage artwork = requireImage(artworkFile);
        BufferedImage mask = requireImage(maskFile);
        if (artwork.getWidth() != mask.getWidth() || artwork.getHeight() != mask.getHeight()) {
            throw new IllegalArgumentException(
                    "Artwork and mask dimensions differ: artwork="
                            + artwork.getWidth() + "x" + artwork.getHeight()
                            + " mask=" + mask.getWidth() + "x" + mask.getHeight());
        }

        BufferedImage aligned = align(artwork, mask);
        File outputFile = new File(args[2]);
        File parent = outputFile.getParentFile();
        if (parent != null && !parent.isDirectory() && !parent.mkdirs()) {
            throw new IOException("Could not create output directory: " + parent);
        }
        if (!ImageIO.write(aligned, "png", outputFile)) {
            throw new IOException("No PNG writer is available");
        }
        System.out.println(
                "aligned=" + outputFile
                        + " size=" + aligned.getWidth() + "x" + aligned.getHeight()
                        + " road_pixels=" + countRoadPixels(mask));
    }

    static BufferedImage align(BufferedImage artwork, BufferedImage mask) {
        int width = mask.getWidth();
        int height = mask.getHeight();
        boolean[] road = new boolean[width * height];
        boolean[] greenStartLine = new boolean[road.length];
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int index = y * width + x;
                int rgb = mask.getRGB(x, y);
                int red = (rgb >>> 16) & 0xff;
                int green = (rgb >>> 8) & 0xff;
                int blue = rgb & 0xff;
                road[index] = Math.max(red, Math.max(green, blue)) >= 96;
                greenStartLine[index] = green >= 96 && green > red * 3 / 2 && green > blue * 3 / 2;
            }
        }

        int[] distanceToRoad = chamferDistance(road, width, height, true);
        int[] distanceToOutside = chamferDistance(road, width, height, false);
        BufferedImage output = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int index = y * width + x;
                int source = artwork.getRGB(x, y);
                int result;
                if (road[index]) {
                    result = renderRoadPixel(
                            x, y, distanceToOutside[index], greenStartLine[index]);
                } else {
                    result = renderOutsidePixel(x, y, source, distanceToRoad[index]);
                }
                output.setRGB(x, y, result);
            }
        }
        return output;
    }

    private static int renderRoadPixel(
            int x,
            int y,
            int distanceToOutside,
            boolean greenStartLine) {
        if (greenStartLine) {
            int variation = coordinateNoise(x, y, 5);
            return rgb(38 + variation, 226 + variation, 91 + variation);
        }

        float edgeDistance = distanceToOutside / (float) ORTHOGONAL_COST;
        if (edgeDistance <= 1.35f) {
            int variation = coordinateNoise(x, y, 4);
            return rgb(210 + variation, 211 + variation, 207 + variation);
        }

        int fineNoise = coordinateNoise(x, y, 5);
        int broadNoise = coordinateNoise(x / 7, y / 7, 4);
        int texture = fineNoise + broadNoise;
        return rgb(48 + texture, 51 + texture, 54 + texture);
    }

    private static int renderOutsidePixel(int x, int y, int source, int distanceToRoad) {
        float distance = distanceToRoad / (float) ORTHOGONAL_COST;
        boolean generatedGreenSpill = isGreenMarker(source);
        if (distance > SHOULDER_PIXELS && !generatedGreenSpill) {
            return source & 0x00ffffff;
        }
        if (distance > GENERATED_MARKER_CLEANUP_PIXELS) {
            return source & 0x00ffffff;
        }
        int noise = coordinateNoise(x, y, 6) + coordinateNoise(x / 5, y / 5, 4);
        return rgb(107 + noise, 104 + noise, 94 + noise);
    }

    private static boolean isGreenMarker(int rgb) {
        int red = (rgb >>> 16) & 0xff;
        int green = (rgb >>> 8) & 0xff;
        int blue = rgb & 0xff;
        return green >= 96 && green > red * 3 / 2 && green > blue * 3 / 2;
    }

    private static int[] chamferDistance(boolean[] road, int width, int height, boolean sourceIsRoad) {
        int[] distance = new int[road.length];
        for (int i = 0; i < road.length; i++) {
            distance[i] = road[i] == sourceIsRoad ? 0 : INFINITE_DISTANCE;
        }

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int index = y * width + x;
                int best = distance[index];
                if (x > 0) {
                    best = Math.min(best, distance[index - 1] + ORTHOGONAL_COST);
                }
                if (y > 0) {
                    best = Math.min(best, distance[index - width] + ORTHOGONAL_COST);
                    if (x > 0) {
                        best = Math.min(best, distance[index - width - 1] + DIAGONAL_COST);
                    }
                    if (x + 1 < width) {
                        best = Math.min(best, distance[index - width + 1] + DIAGONAL_COST);
                    }
                }
                distance[index] = best;
            }
        }

        for (int y = height - 1; y >= 0; y--) {
            for (int x = width - 1; x >= 0; x--) {
                int index = y * width + x;
                int best = distance[index];
                if (x + 1 < width) {
                    best = Math.min(best, distance[index + 1] + ORTHOGONAL_COST);
                }
                if (y + 1 < height) {
                    best = Math.min(best, distance[index + width] + ORTHOGONAL_COST);
                    if (x > 0) {
                        best = Math.min(best, distance[index + width - 1] + DIAGONAL_COST);
                    }
                    if (x + 1 < width) {
                        best = Math.min(best, distance[index + width + 1] + DIAGONAL_COST);
                    }
                }
                distance[index] = best;
            }
        }
        return distance;
    }

    private static BufferedImage requireImage(File file) throws IOException {
        BufferedImage image = ImageIO.read(file);
        if (image == null) {
            throw new IOException("Could not decode image: " + file);
        }
        return image;
    }

    private static long countRoadPixels(BufferedImage mask) {
        long count = 0;
        for (int y = 0; y < mask.getHeight(); y++) {
            for (int x = 0; x < mask.getWidth(); x++) {
                int rgb = mask.getRGB(x, y);
                int red = (rgb >>> 16) & 0xff;
                int green = (rgb >>> 8) & 0xff;
                int blue = rgb & 0xff;
                if (Math.max(red, Math.max(green, blue)) >= 96) {
                    count++;
                }
            }
        }
        return count;
    }

    private static int coordinateNoise(int x, int y, int amplitude) {
        int value = x * 0x1f123bb5 ^ y * 0x5f356495;
        value ^= value >>> 15;
        value *= 0x2c1b3c6d;
        value ^= value >>> 12;
        int range = amplitude * 2 + 1;
        return Math.floorMod(value, range) - amplitude;
    }

    private static int rgb(int red, int green, int blue) {
        return (clampByte(red) << 16) | (clampByte(green) << 8) | clampByte(blue);
    }

    private static int clampByte(int value) {
        return Math.max(0, Math.min(255, value));
    }

}
