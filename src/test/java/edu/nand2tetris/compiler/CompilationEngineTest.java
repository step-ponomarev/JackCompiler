package edu.nand2tetris.compiler;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PipedOutputStream;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import edu.nadn2tetris.compiler.CompilationEngine;
import edu.nadn2tetris.tokenizer.JackTokenizer;

public final class CompilationEngineTest {
    @Test
    public void testSimpleCompileVarDeclaration() {
        final String line = "var char c;";
        final String expected = """
                <keyword> var </keyword>
                <keyword> char </keyword>
                <identifier> c </identifier>
                <symbol> ; </symbol>
                """;

        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (final JackTokenizer jackTokenizer = new JackTokenizer(new ByteArrayInputStream(line.getBytes()));
             final CompilationEngine compilationEngine = new CompilationEngine(jackTokenizer, baos);
        ) {
            jackTokenizer.advance();
            compilationEngine.compileVarDec();
            compilationEngine.flush();
            
            final BufferedReader reader = new BufferedReader(new InputStreamReader(new ByteArrayInputStream(baos.toByteArray())));
            final String collect = reader.lines().collect(Collectors.joining());
            reader.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
} 

