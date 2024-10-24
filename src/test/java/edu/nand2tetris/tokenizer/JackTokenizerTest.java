package edu.nand2tetris.tokenizer;

import java.io.ByteArrayInputStream;
import java.io.IOException;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import edu.nadn2tetris.tokenizer.JackTokenizer;
import edu.nadn2tetris.tokenizer.Keyword;
import edu.nadn2tetris.tokenizer.TokenType;

public final class JackTokenizerTest {
    @Test
    public void simpleTest() {
        final String line = "var char c;";

        try (final JackTokenizer jackTokenizer = new JackTokenizer(new ByteArrayInputStream(line.getBytes()))) {
            jackTokenizer.advance();
            Assertions.assertEquals(TokenType.KEYWORD, jackTokenizer.tokenType());
            Assertions.assertEquals(Keyword.VAR, jackTokenizer.keyword());

            jackTokenizer.advance();
            Assertions.assertEquals(TokenType.KEYWORD, jackTokenizer.tokenType());
            Assertions.assertEquals(Keyword.CHAR, jackTokenizer.keyword());

            jackTokenizer.advance();
            Assertions.assertEquals(TokenType.IDENTIFIER, jackTokenizer.tokenType());
            Assertions.assertEquals("c", jackTokenizer.identifier());

            Assertions.assertFalse(jackTokenizer.hasMoreTokens());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void invalidLineTest() {
        final String line = "var char c";
        Assertions.assertThrowsExactly(IllegalStateException.class, () -> {
            JackTokenizer jackTokenizer = new JackTokenizer(new ByteArrayInputStream(line.getBytes()));
            jackTokenizer.advance();
        });
    }

    @Test
    public void emptyLineTest() {
        final String line = "   ";

        try (final JackTokenizer jackTokenizer = new JackTokenizer(new ByteArrayInputStream(line.getBytes()))) {
            Assertions.assertFalse(jackTokenizer.hasMoreTokens());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
