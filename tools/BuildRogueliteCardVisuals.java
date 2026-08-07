import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.awt.geom.Path2D;
import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;

/** Builds the runtime card-shell and category-icon atlases from artwork sources. */
public final class BuildRogueliteCardVisuals {
    private static final int CARD_COLUMNS = 5;
    private static final int CARD_ROWS = 2;
    private static final int CARD_WIDTH = 384;
    private static final int CARD_HEIGHT = 512;
    private static final int ICON_SIZE = 256;
    private static final int ICON_PADDING = 18;
    private static final int TIER_COUNT = 3;
    private static final String[] ICON_NAMES = {
        "driver", "tuning", "technique", "powerup", "revenge", "warning"
    };
    private static final int[] CATEGORY_COLORS = {
        0xe5b94c, 0xdf6248, 0x3ba4c4, 0x73c878, 0xd84f92
    };
    private static final int[] TIER_COLORS = {0xb98a5b, 0xb9ced9, 0xf0c84b};

    private BuildRogueliteCardVisuals() {
    }

    public static void main(String[] args) throws IOException {
        if (args.length != 5) {
            System.err.println(
                    "Usage: BuildRogueliteCardVisuals "
                            + "<shell-source.png> <icon-directory> "
                            + "<shell-atlas.png> <type-icon-atlas.png> "
                            + "<tier-icon-atlas.png>");
            System.exit(2);
        }

        BufferedImage shellSource = requireImage(new File(args[0]));
        File iconDirectory = new File(args[1]);
        BufferedImage iconAtlas = buildIconAtlas(iconDirectory);
        BufferedImage tierIconAtlas = buildTierIconAtlas(iconDirectory);
        BufferedImage shellAtlas = buildShellAtlas(shellSource);
        writePng(shellAtlas, new File(args[2]));
        writePng(iconAtlas, new File(args[3]));
        writePng(tierIconAtlas, new File(args[4]));
    }

    private static BufferedImage buildTierIconAtlas(File iconDirectory) throws IOException {
        BufferedImage atlas =
                new BufferedImage(ICON_SIZE * TIER_COUNT, ICON_SIZE, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = atlas.createGraphics();
        configureHighQuality(graphics);
        for (int tier = 1; tier <= TIER_COUNT; tier++) {
            BufferedImage icon = buildTierIcon(tier, TIER_COLORS[tier - 1]);
            graphics.drawImage(icon, (tier - 1) * ICON_SIZE, 0, null);
            writePng(icon, new File(iconDirectory, "tier" + tier + ".png"));
        }
        graphics.dispose();
        return atlas;
    }

    private static BufferedImage buildTierIcon(int tier, int tierRgb) {
        BufferedImage icon =
                new BufferedImage(ICON_SIZE, ICON_SIZE, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = icon.createGraphics();
        configureHighQuality(graphics);

        Path2D.Float shield = new Path2D.Float();
        shield.moveTo(128f, 17f);
        shield.lineTo(209f, 47f);
        shield.lineTo(198f, 163f);
        shield.lineTo(128f, 232f);
        shield.lineTo(58f, 163f);
        shield.lineTo(47f, 47f);
        shield.closePath();

        Color tierColor = new Color(tierRgb);
        graphics.setColor(new Color(0, 0, 0, 150));
        graphics.setStroke(new BasicStroke(18f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        graphics.draw(shield);
        graphics.setColor(new Color(12, 17, 23, 245));
        graphics.fill(shield);
        graphics.setColor(tierColor);
        graphics.setStroke(new BasicStroke(10f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        graphics.draw(shield);
        graphics.setColor(new Color(205, 218, 224, 180));
        graphics.setStroke(new BasicStroke(3f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        graphics.draw(shield);

        Font font = new Font(Font.SANS_SERIF, Font.BOLD, 82);
        graphics.setFont(font);
        FontMetrics metrics = graphics.getFontMetrics(font);
        String label = "T" + tier;
        int textX = (ICON_SIZE - metrics.stringWidth(label)) / 2;
        int textY = 143;
        graphics.setColor(new Color(0, 0, 0, 210));
        graphics.drawString(label, textX + 3, textY + 3);
        graphics.setColor(tierColor.brighter());
        graphics.drawString(label, textX, textY);

        float barWidth = 22f;
        float barGap = 10f;
        float totalWidth = tier * barWidth + Math.max(0, tier - 1) * barGap;
        float barX = (ICON_SIZE - totalWidth) * 0.5f;
        graphics.setColor(tierColor);
        for (int i = 0; i < tier; i++) {
            float x = barX + i * (barWidth + barGap);
            Path2D.Float rankMark = new Path2D.Float();
            rankMark.moveTo(x, 178f);
            rankMark.lineTo(x + barWidth * 0.5f, 168f);
            rankMark.lineTo(x + barWidth, 178f);
            rankMark.lineTo(x + barWidth * 0.5f, 188f);
            rankMark.closePath();
            graphics.fill(rankMark);
        }
        graphics.dispose();
        return icon;
    }

    private static BufferedImage buildIconAtlas(File iconDirectory) throws IOException {
        BufferedImage atlas =
                new BufferedImage(
                        ICON_SIZE * ICON_NAMES.length,
                        ICON_SIZE,
                        BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = atlas.createGraphics();
        configureHighQuality(graphics);
        for (int i = 0; i < ICON_NAMES.length; i++) {
            File iconFile = new File(iconDirectory, ICON_NAMES[i] + ".png");
            BufferedImage source =
                    "warning".equals(ICON_NAMES[i])
                            ? buildWarningIcon()
                            : requireImage(iconFile);
            BufferedImage normalized = normalizeIcon(source);
            graphics.drawImage(normalized, i * ICON_SIZE, 0, null);
            writePng(normalized, iconFile);
        }
        graphics.dispose();
        return atlas;
    }

    private static BufferedImage buildWarningIcon() {
        BufferedImage icon =
                new BufferedImage(ICON_SIZE, ICON_SIZE, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = icon.createGraphics();
        configureHighQuality(graphics);

        Path2D.Float triangle = new Path2D.Float();
        triangle.moveTo(128f, 19f);
        triangle.lineTo(238f, 221f);
        triangle.lineTo(18f, 221f);
        triangle.closePath();

        graphics.setColor(new Color(0, 0, 0, 185));
        graphics.setStroke(
                new BasicStroke(22f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        graphics.draw(triangle);
        graphics.setColor(new Color(13, 16, 18, 248));
        graphics.fill(triangle);
        graphics.setColor(new Color(255, 190, 44));
        graphics.setStroke(
                new BasicStroke(12f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        graphics.draw(triangle);
        graphics.setColor(new Color(255, 232, 150, 205));
        graphics.setStroke(
                new BasicStroke(3f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        graphics.draw(triangle);

        graphics.setColor(new Color(0, 0, 0, 210));
        graphics.setStroke(
                new BasicStroke(24f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        graphics.drawLine(131, 82, 131, 151);
        graphics.fillOval(118, 172, 26, 26);
        graphics.setColor(new Color(255, 202, 55));
        graphics.setStroke(
                new BasicStroke(15f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        graphics.drawLine(128, 79, 128, 148);
        graphics.fillOval(119, 169, 18, 18);
        graphics.dispose();
        return icon;
    }

    private static BufferedImage normalizeIcon(BufferedImage source) {
        int[] bounds = findVisibleBounds(source);
        int visibleWidth = Math.max(1, bounds[2] - bounds[0] + 1);
        int visibleHeight = Math.max(1, bounds[3] - bounds[1] + 1);
        int available = ICON_SIZE - ICON_PADDING * 2;
        float scale = Math.min(
                available / (float) visibleWidth,
                available / (float) visibleHeight);
        int drawWidth = Math.max(1, Math.round(visibleWidth * scale));
        int drawHeight = Math.max(1, Math.round(visibleHeight * scale));
        int drawX = (ICON_SIZE - drawWidth) / 2;
        int drawY = (ICON_SIZE - drawHeight) / 2;

        BufferedImage output =
                new BufferedImage(ICON_SIZE, ICON_SIZE, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = output.createGraphics();
        configureHighQuality(graphics);
        graphics.setComposite(AlphaComposite.Src);
        graphics.drawImage(
                source,
                drawX,
                drawY,
                drawX + drawWidth,
                drawY + drawHeight,
                bounds[0],
                bounds[1],
                bounds[2] + 1,
                bounds[3] + 1,
                null);
        graphics.dispose();
        return output;
    }

    private static int[] findVisibleBounds(BufferedImage image) {
        int minX = image.getWidth();
        int minY = image.getHeight();
        int maxX = -1;
        int maxY = -1;
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                int alpha = (image.getRGB(x, y) >>> 24) & 0xff;
                if (alpha < 12) {
                    continue;
                }
                minX = Math.min(minX, x);
                minY = Math.min(minY, y);
                maxX = Math.max(maxX, x);
                maxY = Math.max(maxY, y);
            }
        }
        if (maxX < minX || maxY < minY) {
            throw new IllegalArgumentException("Icon source has no visible pixels");
        }
        return new int[] {minX, minY, maxX, maxY};
    }

    private static BufferedImage buildShellAtlas(BufferedImage source) {
        BufferedImage atlas =
                new BufferedImage(
                        CARD_WIDTH * CARD_COLUMNS,
                        CARD_HEIGHT * CARD_ROWS,
                        BufferedImage.TYPE_INT_RGB);
        Graphics2D atlasGraphics = atlas.createGraphics();
        configureHighQuality(atlasGraphics);
        for (int row = 0; row < CARD_ROWS; row++) {
            for (int column = 0; column < CARD_COLUMNS; column++) {
                BufferedImage cell = scaleShell(source);
                recolorCyanAccents(cell, CATEGORY_COLORS[column]);
                drawCardLayoutSockets(cell, CATEGORY_COLORS[column]);
                if (row == 1) {
                    dimEmptySlot(cell);
                }
                atlasGraphics.drawImage(
                        cell,
                        column * CARD_WIDTH,
                        row * CARD_HEIGHT,
                        null);
            }
        }
        atlasGraphics.dispose();
        return atlas;
    }

    private static BufferedImage scaleShell(BufferedImage source) {
        BufferedImage output =
                new BufferedImage(CARD_WIDTH, CARD_HEIGHT, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = output.createGraphics();
        configureHighQuality(graphics);
        graphics.drawImage(source, 0, 0, CARD_WIDTH, CARD_HEIGHT, null);
        graphics.dispose();
        return output;
    }

    private static void recolorCyanAccents(BufferedImage image, int categoryRgb) {
        int targetRed = (categoryRgb >>> 16) & 0xff;
        int targetGreen = (categoryRgb >>> 8) & 0xff;
        int targetBlue = categoryRgb & 0xff;
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                int rgb = image.getRGB(x, y);
                int red = (rgb >>> 16) & 0xff;
                int green = (rgb >>> 8) & 0xff;
                int blue = rgb & 0xff;
                int cyanDominance = Math.min(green, blue) - red;
                if (cyanDominance < 14 || Math.max(green, blue) < 62) {
                    continue;
                }
                float strength = clamp(cyanDominance / 90f, 0f, 1f);
                float intensity = Math.max(green, blue) / 255f;
                int coloredRed = highlight(targetRed, intensity);
                int coloredGreen = highlight(targetGreen, intensity);
                int coloredBlue = highlight(targetBlue, intensity);
                image.setRGB(
                        x,
                        y,
                        rgb(
                                blend(red, coloredRed, strength),
                                blend(green, coloredGreen, strength),
                                blend(blue, coloredBlue, strength)));
            }
        }
    }

    private static int highlight(int component, float intensity) {
        float highlight = clamp((intensity - 0.62f) / 0.38f, 0f, 1f);
        return Math.round(component + (255 - component) * highlight);
    }

    private static void drawCardLayoutSockets(BufferedImage image, int categoryRgb) {
        Graphics2D graphics = image.createGraphics();
        configureHighQuality(graphics);
        drawSocket(graphics, 16, 15, 64, 64, 12, categoryRgb, 3f);
        drawSocket(graphics, 86, 17, 212, 58, 12, categoryRgb, 3f);
        drawSocket(graphics, 304, 15, 64, 64, 12, categoryRgb, 3f);
        drawSocket(graphics, 80, 82, 224, 224, 18, categoryRgb, 4f);
        drawSocket(graphics, 22, 306, 340, 166, 16, categoryRgb, 3f);
        drawSocket(graphics, 126, 474, 132, 34, 10, categoryRgb, 3f);
        graphics.dispose();
    }

    private static void drawSocket(
            Graphics2D graphics,
            int x,
            int y,
            int width,
            int height,
            int arc,
            int categoryRgb,
            float strokeWidth) {
        graphics.setColor(new Color(4, 7, 10, 248));
        graphics.fillRoundRect(x, y, width, height, arc, arc);
        graphics.setColor(new Color(categoryRgb | 0xff000000, true));
        graphics.setStroke(
                new BasicStroke(
                        strokeWidth,
                        BasicStroke.CAP_ROUND,
                        BasicStroke.JOIN_ROUND));
        graphics.drawRoundRect(x, y, width, height, arc, arc);
        graphics.setColor(new Color(112, 124, 132, 205));
        graphics.setStroke(new BasicStroke(1.5f));
        graphics.drawRoundRect(
                x + 5,
                y + 5,
                Math.max(1, width - 10),
                Math.max(1, height - 10),
                Math.max(4, arc - 4),
                Math.max(4, arc - 4));
    }

    private static void dimEmptySlot(BufferedImage image) {
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                int rgb = image.getRGB(x, y);
                int red = (rgb >>> 16) & 0xff;
                int green = (rgb >>> 8) & 0xff;
                int blue = rgb & 0xff;
                image.setRGB(
                        x,
                        y,
                        rgb(
                                Math.round(red * 0.68f),
                                Math.round(green * 0.68f),
                                Math.round(blue * 0.68f)));
            }
        }
    }

    private static void configureHighQuality(Graphics2D graphics) {
        graphics.setRenderingHint(
                RenderingHints.KEY_INTERPOLATION,
                RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        graphics.setRenderingHint(
                RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);
        graphics.setRenderingHint(
                RenderingHints.KEY_RENDERING,
                RenderingHints.VALUE_RENDER_QUALITY);
    }

    private static BufferedImage requireImage(File file) throws IOException {
        BufferedImage image = ImageIO.read(file);
        if (image == null) {
            throw new IOException("Could not decode image: " + file);
        }
        return image;
    }

    private static void writePng(BufferedImage image, File file) throws IOException {
        File parent = file.getParentFile();
        if (parent != null && !parent.isDirectory() && !parent.mkdirs()) {
            throw new IOException("Could not create output directory: " + parent);
        }
        if (!ImageIO.write(image, "png", file)) {
            throw new IOException("No PNG writer is available");
        }
    }

    private static int blend(int from, int to, float amount) {
        return Math.round(from + (to - from) * clamp(amount, 0f, 1f));
    }

    private static int rgb(int red, int green, int blue) {
        return (clampByte(red) << 16) | (clampByte(green) << 8) | clampByte(blue);
    }

    private static int clampByte(int value) {
        return Math.max(0, Math.min(255, value));
    }

    private static float clamp(float value, float minimum, float maximum) {
        return Math.max(minimum, Math.min(maximum, value));
    }
}
