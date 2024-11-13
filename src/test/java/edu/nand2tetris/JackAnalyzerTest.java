package edu.nand2tetris;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.stream.Stream;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import edu.nadn2tetris.JackAnalyzer;

import static edu.nand2tetris.compiler.CompilationEngineTest.RES_DIR;

public final class JackAnalyzerTest {

    @Test
    public void testSimpleFileCompilation() throws IOException {
        final Path srcFile = RES_DIR.resolve("src/ArrayTest/Main.jack");
        final Path testFile = RES_DIR.resolve("test/ArrayTest/Main.xml");
        final Path outDir = RES_DIR.resolve("out");

        JackAnalyzer.main(new String[]{srcFile.toString(), outDir.toString()});

        final BufferedReader compiledReader = new BufferedReader(new FileReader(outDir.resolve("Main.xml").toFile()));
        final BufferedReader testReader = new BufferedReader(new InputStreamReader(new FileInputStream(testFile.toFile())));

        String first;
        String second;
        while ((first = testReader.readLine()) != null && (second = compiledReader.readLine()) != null) {
            Assertions.assertEquals(first, second);
        }

        try (Stream<Path> paths = Files.walk(outDir)) {
            paths.sorted(Comparator.reverseOrder()) // Сортируем в обратном порядке для удаления файлов до директорий
                    .forEach(path -> {
                                try {
                                    Files.delete(path); // Удаляем файл или директорию
                                } catch (IOException e) {
                                    throw new RuntimeException(e);
                                }
                            }
                    );
        }
    }
}
