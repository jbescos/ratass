import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;

/** Adds tuning and technique expansion artwork to the roguelite card-art atlas. */
public final class BuildExpandedCardArtAtlas {
    private static final int ATLAS_COLUMNS = 6;
    private static final int ATLAS_ROWS = 19;
    private static final int CELL_SIZE = 250;
    private static final int SOURCE_COLUMNS = 6;
    private static final int SOURCE_ROWS = 4;

    // Target atlas index followed by the semantically matching expansion-sheet index.
    private static final int[][] TUNING_ARTWORK = {
        {62, 0}, {63, 1}, {64, 23}, {65, 3}, {66, 4}, {67, 15}, {68, 5},
        {69, 14}, {70, 18}, {71, 2}, {72, 19}, {73, 20}, {74, 16}, {75, 6},
        {76, 7}, {77, 8}, {78, 9}, {79, 10}, {80, 11}, {81, 12}, {82, 13}
    };
    private static final int[] TECHNIQUE_ARTWORK_INDICES = {
        6, 16, 84, 85, 86, 87, 88, 89, 90, 91, 97, 98, 99, 100, 101, 102,
        108, 109, 110, 111, 112, 113
    };
    private static final String[] TECHNIQUE_ARTWORK = {
        "straight_focus.png",
        "apex_focus.png",
        "rally_expert.png",
        "sprint_focus.png",
        "corner_master.png",
        "draft_master.png",
        "straight_master.png",
        "drift_master.png",
        "rally_master.png",
        "slide_focus.png",
        "apex_expert.png",
        "sprint_expert.png",
        "slide_expert.png",
        "apex_master.png",
        "sprint_master.png",
        "slide_master.png",
        "traction_focus.png",
        "traction_expert.png",
        "traction_master.png",
        "agility_focus.png",
        "agility_expert.png",
        "agility_master.png"
    };
    private static final int[] EXTRA_CARD_ARTWORK_INDICES = {
        8, 92, 93, 94, 95, 96, 103, 104, 105, 106, 107
    };
    private static final String[] EXTRA_CARD_ARTWORK = {
        "time_ripple.png",
        "repulsor_wave.png",
        "hunter_barrage.png",
        "grudge_spark.png",
        "vengeance_core.png",
        "nemesis_engine.png",
        "hunter_storm.png",
        "ace_hotline.png",
        "priority_hotline.png",
        "chrono_shift.png",
        "temporal_dominion.png"
    };

    private BuildExpandedCardArtAtlas() {
    }

    public static void main(String[] args) throws IOException {
        if (args.length != 4) {
            System.err.println(
                    "Usage: BuildExpandedCardArtAtlas "
                            + "<existing-atlas.png> <tuning-source.png> "
                            + "<technique-source-dir> <output-atlas.png>");
            System.exit(2);
        }

        BufferedImage existing = requireImage(new File(args[0]));
        BufferedImage source = requireImage(new File(args[1]));
        BufferedImage output =
                new BufferedImage(
                        ATLAS_COLUMNS * CELL_SIZE,
                        ATLAS_ROWS * CELL_SIZE,
                        BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = output.createGraphics();
        graphics.setColor(Color.BLACK);
        graphics.fillRect(0, 0, output.getWidth(), output.getHeight());
        graphics.drawImage(existing, 0, 0, null);
        graphics.setRenderingHint(
                RenderingHints.KEY_INTERPOLATION,
                RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        graphics.setRenderingHint(
                RenderingHints.KEY_RENDERING,
                RenderingHints.VALUE_RENDER_QUALITY);

        int sourceCellWidth = source.getWidth() / SOURCE_COLUMNS;
        int sourceCellHeight = source.getHeight() / SOURCE_ROWS;
        for (int i = 0; i < TUNING_ARTWORK.length; i++) {
            int targetIndex = TUNING_ARTWORK[i][0];
            int sourceIndex = TUNING_ARTWORK[i][1];
            int targetX = targetIndex % ATLAS_COLUMNS * CELL_SIZE;
            int targetY = targetIndex / ATLAS_COLUMNS * CELL_SIZE;
            int sourceX = sourceIndex % SOURCE_COLUMNS * sourceCellWidth;
            int sourceY = sourceIndex / SOURCE_COLUMNS * sourceCellHeight;
            graphics.drawImage(
                    source,
                    targetX,
                    targetY,
                    targetX + CELL_SIZE,
                    targetY + CELL_SIZE,
                    sourceX,
                    sourceY,
                    sourceX + sourceCellWidth,
                    sourceY + sourceCellHeight,
                    null);
        }
        File techniqueSourceDirectory = new File(args[2]);
        for (int i = 0; i < TECHNIQUE_ARTWORK.length; i++) {
            int targetIndex = TECHNIQUE_ARTWORK_INDICES[i];
            BufferedImage techniqueArt = requireImage(
                    new File(techniqueSourceDirectory, TECHNIQUE_ARTWORK[i]));
            int targetX = targetIndex % ATLAS_COLUMNS * CELL_SIZE;
            int targetY = targetIndex / ATLAS_COLUMNS * CELL_SIZE;
            graphics.drawImage(
                    techniqueArt,
                    targetX,
                    targetY,
                    targetX + CELL_SIZE,
                    targetY + CELL_SIZE,
                    0,
                    0,
                    techniqueArt.getWidth(),
                    techniqueArt.getHeight(),
                    null);
        }
        for (int i = 0; i < EXTRA_CARD_ARTWORK.length; i++) {
            int targetIndex = EXTRA_CARD_ARTWORK_INDICES[i];
            BufferedImage extraArt = requireImage(
                    new File(techniqueSourceDirectory, EXTRA_CARD_ARTWORK[i]));
            int targetX = targetIndex % ATLAS_COLUMNS * CELL_SIZE;
            int targetY = targetIndex / ATLAS_COLUMNS * CELL_SIZE;
            graphics.drawImage(
                    extraArt,
                    targetX,
                    targetY,
                    targetX + CELL_SIZE,
                    targetY + CELL_SIZE,
                    0,
                    0,
                    extraArt.getWidth(),
                    extraArt.getHeight(),
                    null);
        }
        graphics.dispose();

        File outputFile = new File(args[3]);
        File parent = outputFile.getParentFile();
        if (parent != null && !parent.isDirectory() && !parent.mkdirs()) {
            throw new IOException("Cannot create output directory: " + parent);
        }
        if (!ImageIO.write(output, "png", outputFile)) {
            throw new IOException("No PNG writer is available");
        }
    }

    private static BufferedImage requireImage(File file) throws IOException {
        BufferedImage image = ImageIO.read(file);
        if (image == null) {
            throw new IOException("Cannot read image: " + file);
        }
        return image;
    }
}
