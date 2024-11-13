package edu.nand2tetris.compiler;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import edu.nadn2tetris.compiler.CompilationEngine;
import edu.nadn2tetris.tokenizer.JackTokenizer;

public final class CompilationEngineTest {
    public static final Path RES_DIR = Paths.get("src", "test", "resources");

    @Test
    public void testMainCompilation() {
        final Path src = RES_DIR.resolve("src/ArrayTest/Main.jack");
        final Path test = RES_DIR.resolve("test/ArrayTest/Main.xml");

        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (final JackTokenizer jackTokenizer = new JackTokenizer(new FileInputStream(src.toFile()));
             final CompilationEngine compilationEngine = new CompilationEngine(jackTokenizer, out);
        ) {
            jackTokenizer.advance();
            compilationEngine.compileClass();
            compilationEngine.close();

            final BufferedReader compiledReader = new BufferedReader(new InputStreamReader(new ByteArrayInputStream(out.toByteArray())));
            final BufferedReader testReader = new BufferedReader(new InputStreamReader(new FileInputStream(test.toFile())));

            String first;
            String second;
            while ((first = testReader.readLine()) != null && (second = compiledReader.readLine()) != null) {
                Assertions.assertEquals(first, second);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
} 

