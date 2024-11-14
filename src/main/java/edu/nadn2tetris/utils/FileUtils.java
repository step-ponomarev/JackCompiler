package edu.nadn2tetris.utils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;

public final class FileUtils {
    public static void removeDir(Path path) throws IOException {
        if (path == null || Files.notExists(path)) {
            return;
        }

        Files.walk(path)
                .sorted(Comparator.reverseOrder())
                .forEach(p -> {
                    try {
                        Files.delete(p);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                });
    }

    private FileUtils() {}
}
