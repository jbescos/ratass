package com.github.jbescos.gameplay.maps;

import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.PixmapIO;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

final class MapCacheSupport {
    private MapCacheSupport() {
    }

    static boolean isAvailable() {
        return true;
    }

    static boolean readBooleanSystemProperty(String name) {
        return Boolean.getBoolean(name);
    }

    static Reader openReader(FileHandle cacheFile) throws IOException {
        return new InputStreamReader(
                new GZIPInputStream(cacheFile.read()),
                StandardCharsets.UTF_8);
    }

    static Writer openWriter(FileHandle cacheFile) throws IOException {
        return new OutputStreamWriter(
                new GZIPOutputStream(cacheFile.write(false)),
                StandardCharsets.UTF_8);
    }

    static String calculateSha256(FileHandle file) {
        InputStream input = null;
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            input = new BufferedInputStream(file.read());
            byte[] buffer = new byte[8192];
            int count;
            while ((count = input.read(buffer)) >= 0) {
                if (count > 0) {
                    digest.update(buffer, 0, count);
                }
            }
            return toHex(digest.digest());
        } catch (RuntimeException ignored) {
            return null;
        } catch (IOException ignored) {
            return null;
        } catch (NoSuchAlgorithmException ignored) {
            return null;
        } finally {
            closeQuietly(input);
        }
    }

    static void writePng(FileHandle file, Pixmap pixmap) {
        PixmapIO.PNG writer =
                new PixmapIO.PNG(Math.max(1024, pixmap.getWidth() * pixmap.getHeight() * 4));
        try {
            writer.setFlipY(false);
            writer.setCompression(9);
            writer.write(file, pixmap);
        } catch (IOException ignored) {
            // Packaged/internal files are intentionally not writable.
        } finally {
            writer.dispose();
        }
    }

    private static String toHex(byte[] bytes) {
        char[] output = new char[bytes.length * 2];
        char[] digits = "0123456789abcdef".toCharArray();
        for (int i = 0; i < bytes.length; i++) {
            int value = bytes[i] & 0xff;
            output[i * 2] = digits[value >>> 4];
            output[i * 2 + 1] = digits[value & 0x0f];
        }
        return new String(output);
    }

    private static void closeQuietly(InputStream input) {
        if (input == null) {
            return;
        }
        try {
            input.close();
        } catch (IOException ignored) {
        }
    }
}
