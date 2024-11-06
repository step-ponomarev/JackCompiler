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

            jackTokenizer.advance();
            Assertions.assertEquals(TokenType.SYMBOL, jackTokenizer.tokenType());
            Assertions.assertEquals(';', jackTokenizer.symbol());

            Assertions.assertFalse(jackTokenizer.hasMoreTokens());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void simpleIfTest() {
        final String line = """
                if (x < 0) {
                    x = 0;
                }
                """;

        try (final JackTokenizer jackTokenizer = new JackTokenizer(new ByteArrayInputStream(line.getBytes()))) {
            jackTokenizer.advance();
            Assertions.assertEquals(TokenType.KEYWORD, jackTokenizer.tokenType());
            Assertions.assertEquals(Keyword.IF, jackTokenizer.keyword());

            jackTokenizer.advance();
            Assertions.assertEquals(TokenType.SYMBOL, jackTokenizer.tokenType());
            Assertions.assertEquals('(', jackTokenizer.symbol());

            jackTokenizer.advance();
            Assertions.assertEquals(TokenType.IDENTIFIER, jackTokenizer.tokenType());
            Assertions.assertEquals("x", jackTokenizer.identifier());

            jackTokenizer.advance();
            Assertions.assertEquals(TokenType.SYMBOL, jackTokenizer.tokenType());
            Assertions.assertEquals('<', jackTokenizer.symbol());

            jackTokenizer.advance();
            Assertions.assertEquals(TokenType.INT_CONST, jackTokenizer.tokenType());
            Assertions.assertEquals(0, jackTokenizer.intVal());

            jackTokenizer.advance();
            Assertions.assertEquals(TokenType.SYMBOL, jackTokenizer.tokenType());
            Assertions.assertEquals(')', jackTokenizer.symbol());

            jackTokenizer.advance();
            Assertions.assertEquals(TokenType.SYMBOL, jackTokenizer.tokenType());
            Assertions.assertEquals('{', jackTokenizer.symbol());

            jackTokenizer.advance();
            Assertions.assertEquals(TokenType.IDENTIFIER, jackTokenizer.tokenType());
            Assertions.assertEquals("x", jackTokenizer.identifier());

            jackTokenizer.advance();
            Assertions.assertEquals(TokenType.SYMBOL, jackTokenizer.tokenType());
            Assertions.assertEquals('=', jackTokenizer.symbol());

            jackTokenizer.advance();
            Assertions.assertEquals(TokenType.INT_CONST, jackTokenizer.tokenType());
            Assertions.assertEquals(0, jackTokenizer.intVal());

            jackTokenizer.advance();
            Assertions.assertEquals(TokenType.SYMBOL, jackTokenizer.tokenType());
            Assertions.assertEquals(';', jackTokenizer.symbol());

            jackTokenizer.advance();
            Assertions.assertEquals(TokenType.SYMBOL, jackTokenizer.tokenType());
            Assertions.assertEquals('}', jackTokenizer.symbol());

            Assertions.assertFalse(jackTokenizer.hasMoreTokens());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void commentsBlockTest() {
        final String line = """
                /* smart comment
                ablut how it works
                */
                   
                var
                // simple comment
                var
                                
                /*** one line block */
                var
                """;

        try (final JackTokenizer jackTokenizer = new JackTokenizer(new ByteArrayInputStream(line.getBytes()))) {
            jackTokenizer.advance();
            Assertions.assertEquals(TokenType.KEYWORD, jackTokenizer.tokenType());
            Assertions.assertEquals(Keyword.VAR, jackTokenizer.keyword());
            
            jackTokenizer.advance();
            Assertions.assertEquals(TokenType.KEYWORD, jackTokenizer.tokenType());
            Assertions.assertEquals(Keyword.VAR, jackTokenizer.keyword());
            
            jackTokenizer.advance();
            Assertions.assertEquals(TokenType.KEYWORD, jackTokenizer.tokenType());
            Assertions.assertEquals(Keyword.VAR, jackTokenizer.keyword());

            Assertions.assertFalse(jackTokenizer.hasMoreTokens());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
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
