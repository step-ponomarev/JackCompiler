package edu.nand2tetris;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import edu.nadn2tetris.JackAnalyzer;
import edu.nadn2tetris.utils.FileUtils;

import static edu.nand2tetris.compiler.CompilationEngineTest.RES_DIR;

public final class JackAnalyzerTest {
    private final static Path OUT_DIR = RES_DIR.resolve("out");

    @AfterEach
    public void cleanUp() throws IOException {
        FileUtils.removeDir(OUT_DIR);
    }

    @Test
    public void testArray() throws IOException {
        final Path srcFile = RES_DIR.resolve("src/ArrayTest/Main.jack");
        final Path testFile = RES_DIR.resolve("test/ArrayTest/Main.xml");

        JackAnalyzer.main(new String[]{srcFile.toString(), OUT_DIR.toString()});

        compareFiles(testFile.toFile(), OUT_DIR.resolve("Main.xml").toFile());
    }

    @Test
    public void testExpressionLessSquare() throws IOException {
        final Path srcFile = RES_DIR.resolve("src/ExpressionLessSquare");
        final Path testFiles = RES_DIR.resolve("test/ExpressionLessSquare");

        JackAnalyzer.main(new String[]{srcFile.toString(), OUT_DIR.toString()});

        final Map<String, Path> testFilesMap = Files.walk(testFiles)
                .collect(
                        Collectors.toMap(p -> p.getFileName().toString(), UnaryOperator.identity())
                );

        final Map<String, Path> compiledFilesMap = Files.walk(OUT_DIR)
                .collect(
                        Collectors.toMap(p -> p.getFileName().toString(), UnaryOperator.identity())
                );

        for (Map.Entry<String, Path> testFile : testFilesMap.entrySet()) {
            Path compiledFile = compiledFilesMap.get(testFile.getKey());
            compareFiles(testFile.getValue().toFile(), compiledFile.toFile());
        }
    }

    private static void compareFiles(File firstFile, File secondFile) throws IOException {
        final BufferedReader testReader = new BufferedReader(new FileReader(firstFile));
        final BufferedReader compiledReader = new BufferedReader(new FileReader(secondFile));

        String first;
        String second;
        while ((first = testReader.readLine()) != null && (second = compiledReader.readLine()) != null) {
            Assertions.assertEquals(first, second);
        }
    }
}
