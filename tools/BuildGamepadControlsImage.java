import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Line2D;
import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;

/** Builds the redistributable controller-layout image shown by the gamepad help dialog. */
public final class BuildGamepadControlsImage {
    private static final int WIDTH = 1280;
    private static final int HEIGHT = 640;
    private static final Color LABEL = new Color(255, 212, 82);
    private static final Color LINE = new Color(210, 226, 235, 230);

    private BuildGamepadControlsImage() {}

    public static void main(String[] args) throws Exception {
        File source = new File("assets/ui/gamepad_icon.png");
        File output = new File("assets/ui/gamepad_controls.png");
        BufferedImage controller = ImageIO.read(source);
        BufferedImage result = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = result.createGraphics();
        graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        graphics.setComposite(AlphaComposite.SrcOver);

        int controllerX = 245;
        int controllerY = 105;
        int controllerWidth = 790;
        int controllerHeight = 511;
        graphics.drawImage(
                controller,
                controllerX,
                controllerY,
                controllerWidth,
                controllerHeight,
                null);

        Font labelFont = new Font("DejaVu Sans", Font.BOLD, 30);
        Font compactFont = new Font("DejaVu Sans", Font.BOLD, 22);
        graphics.setStroke(new BasicStroke(3f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

        callout(graphics, labelFont, "LT", 255, 22, 445, 126, false);
        callout(graphics, labelFont, "LB", 435, 22, 474, 174, false);
        callout(graphics, labelFont, "RB", 775, 22, 806, 174, false);
        callout(graphics, labelFont, "RT", 955, 22, 835, 126, false);

        callout(graphics, labelFont, "L3", 34, 269, 447, 342, true);
        callout(graphics, compactFont, "D-PAD", 34, 435, 543, 445, true);
        callout(graphics, labelFont, "R3", 1167, 423, 747, 441, true);

        centeredLabel(graphics, compactFont, "VIEW", 585, 287);
        centeredLabel(graphics, compactFont, "MENU", 696, 287);
        centeredLabel(graphics, compactFont, "SHARE", 640, 374);

        graphics.dispose();
        ImageIO.write(result, "png", output);
    }

    private static void callout(
            Graphics2D graphics,
            Font font,
            String text,
            int x,
            int y,
            int targetX,
            int targetY,
            boolean sideLabel) {
        graphics.setFont(font);
        FontMetrics metrics = graphics.getFontMetrics();
        int paddingX = 13;
        int paddingY = 8;
        int width = metrics.stringWidth(text) + paddingX * 2;
        int height = metrics.getHeight() + paddingY * 2;

        int lineStartX = sideLabel
                ? (x < WIDTH / 2 ? x + width : x)
                : x + width / 2;
        int lineStartY = sideLabel ? y + height / 2 : y + height;
        graphics.setColor(LINE);
        graphics.draw(new Line2D.Float(lineStartX, lineStartY, targetX, targetY));

        graphics.setColor(new Color(15, 24, 31, 235));
        graphics.fillRoundRect(x, y, width, height, 14, 14);
        graphics.setColor(LABEL);
        graphics.drawRoundRect(x, y, width, height, 14, 14);
        graphics.drawString(text, x + paddingX, y + paddingY + metrics.getAscent());
    }

    private static void centeredLabel(
            Graphics2D graphics,
            Font font,
            String text,
            int centerX,
            int baselineY) {
        graphics.setFont(font);
        FontMetrics metrics = graphics.getFontMetrics();
        int width = metrics.stringWidth(text);
        graphics.setColor(new Color(4, 9, 13, 210));
        graphics.fillRoundRect(centerX - width / 2 - 8, baselineY - metrics.getAscent(), width + 16, metrics.getHeight(), 9, 9);
        graphics.setColor(LABEL);
        graphics.drawString(text, centerX - width / 2, baselineY);
    }
}
