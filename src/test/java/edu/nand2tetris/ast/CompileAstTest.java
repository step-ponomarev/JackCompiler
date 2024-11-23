package edu.nand2tetris.ast;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import org.junit.jupiter.api.Test;

import edu.nadn2tetris.ast.ExpressionTree;
import edu.nadn2tetris.compiler.CompilationEngine;
import edu.nadn2tetris.tokenizer.JackTokenizer;

public final class CompileAstTest {
    @Test
    public void compileExpressionTest() {
        final String line = "k + 12 * 24 - 1 + 22 - 43";

        try (final JackTokenizer jackTokenizer = new JackTokenizer(new ByteArrayInputStream(line.getBytes()))) {
            jackTokenizer.advance();

            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            CompilationEngine engine = new CompilationEngine(jackTokenizer, byteArrayOutputStream, null);
            ExpressionTree expressionTree = engine.compileExpression();
            System.out.println(expressionTree);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
