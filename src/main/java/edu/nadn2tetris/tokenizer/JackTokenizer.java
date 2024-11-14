package edu.nadn2tetris.tokenizer;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Iterator;
import java.util.Objects;
import java.util.stream.Stream;

import edu.nadn2tetris.common.Keyword;
import edu.nadn2tetris.common.TokenType;
import edu.nadn2tetris.tokenizer.filter.RowFilter;

public final class JackTokenizer implements Closeable {
    private static final RowTokenizer ROW_TOKENIZER = new RowTokenizer();
    private final BufferedReader reader;
    private TokenType tokenType;
    private Keyword keyword;
    private char symbol;
    private short intVal;
    private String identifier;
    private String stringVal;
    private final Iterator<String> tokens;

    public JackTokenizer(InputStream is) {
        this.reader = new BufferedReader(new InputStreamReader(is));
        final Stream<String> lines = this.reader.lines();
        this.tokens = this.reader.lines()
                .flatMap(new RowFilter(lines.spliterator()))
                .flatMap(ROW_TOKENIZER)
                .filter(Objects::nonNull)
                .iterator();
    }

    public boolean hasMoreTokens() {
        return tokens.hasNext();
    }

    public void advance() {
        if (!this.hasMoreTokens()) {
            throw new IllegalStateException("End of file!");
        }

        final String currToken = this.tokens.next();
        tokenType = TokenType.parse(currToken);
        if (tokenType == null) {
            throw new RuntimeException("Unsupported currToken: " + currToken);
        }

        switch (tokenType) {
            case SYMBOL -> this.symbol = currToken.charAt(0);
            case KEYWORD -> this.keyword = Keyword.parse(currToken);
            case INT_CONST -> this.intVal = Short.parseShort(currToken);
            case IDENTIFIER -> this.identifier = currToken;
            case STRING_CONST -> this.stringVal = currToken.substring(1, currToken.length() - 1);
            default -> throw new IllegalArgumentException("Unsupported currToken type: " + tokenType);
        }
    }

    public TokenType tokenType() {
        return tokenType;
    }

    public Keyword keyword() {
        return keyword;
    }

    public char symbol() {
        return symbol;
    }

    public String identifier() {
        return identifier;
    }

    // [0, 32767] by spec
    public short intVal() {
        return intVal;
    }

    public String stringVal() {
        return stringVal;
    }

    @Override
    public void close() throws IOException {
        this.reader.close();
    }
}
