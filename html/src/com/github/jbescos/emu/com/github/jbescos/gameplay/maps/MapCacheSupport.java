package com.github.jbescos.gameplay.maps;

import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Pixmap;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;

final class MapCacheSupport {
    private MapCacheSupport() {
    }

    static boolean isAvailable() {
        return false;
    }

    static boolean readBooleanSystemProperty(String name) {
        return false;
    }

    static Reader openReader(FileHandle cacheFile) throws IOException {
        throw new IOException("Compressed map caches are not available in WebGL");
    }

    static Writer openWriter(FileHandle cacheFile) throws IOException {
        throw new IOException("Compressed map caches are not available in WebGL");
    }

    static String calculateSha256(FileHandle file) {
        return null;
    }

    static void writePng(FileHandle file, Pixmap pixmap) {
    }
}
