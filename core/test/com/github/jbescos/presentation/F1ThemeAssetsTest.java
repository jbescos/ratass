package com.github.jbescos.presentation;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.imageio.ImageIO;
import org.junit.Test;

public class F1ThemeAssetsTest {
    @Test
    public void containsOnlyTenStandaloneCarSprites() throws IOException {
        Path themeRoot = findThemeRoot();
        List<Path> files;
        try (Stream<Path> paths = Files.walk(themeRoot)) {
            files = paths.filter(Files::isRegularFile).collect(Collectors.toList());
        }

        assertEquals(10, files.size());
        for (int index = 0; index < 10; index++) {
            Path car = themeRoot.resolve("cars").resolve(String.format("%02d.png", index));
            assertTrue(Files.isRegularFile(car));
            BufferedImage image = ImageIO.read(car.toFile());
            assertNotNull(image);
            assertEquals(150, image.getWidth());
            assertEquals(200, image.getHeight());
            assertTrue(image.getColorModel().hasAlpha());
            assertEquals(0, image.getRGB(0, 0) >>> 24);
        }
    }

    private static Path findThemeRoot() {
        Path fromModule = Paths.get("../assets/theme/f1");
        return Files.isDirectory(fromModule)
                ? fromModule
                : Paths.get("assets/theme/f1");
    }
}
