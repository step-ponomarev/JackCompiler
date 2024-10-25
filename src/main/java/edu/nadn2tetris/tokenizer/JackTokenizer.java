package edu.nadn2tetris.tokenizer;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Iterator;

public final class JackTokenizer implements Closeable {
    private final BufferedReader reader;

    private TokenType tokenType;
    private Keyword keyword;

    private char symbol;
    private short intVal;
    private String identifier;
    private String stringVal;
    private String[] rowTokens;
    private final Iterator<String> rowIterator;
    int i = 0;

    public JackTokenizer(InputStream is) throws IOException {
        this.reader = new BufferedReader(new InputStreamReader(is));
        this.rowIterator = this.reader.lines().filter(new RowFilter()).iterator();
    }

    public boolean hasMoreTokens() throws IOException {
        return (rowTokens != null && i < rowTokens.length) || rowIterator.hasNext();
    }

    public void advance() {
        try {
            if (!this.hasMoreTokens()) {
                throw new IllegalStateException("End of file!");
            }

            if (rowTokens == null || i == rowTokens.length) {
                rowTokens = readNextRow();
            }

            String currToken = rowTokens[i++];
            final boolean lastToken = i == rowTokens.length;
            if (lastToken) {
                currToken = currToken.substring(0, currToken.length() - 1);
            }

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
        } catch (IOException e) {
            throw new RuntimeException(e);
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

    private String[] readNextRow() throws IOException {
        i = 0;
        /*
            TODO: Поддержать конструкции вида
                if (x < y) {
                    ...
                }
                // решается посимвольным чтением(Но не эффективно)
                // в данном виде у нас не распарсятся отдельные токены (x как "x", "("  
         */
        String currentLine = rowIterator.next();
        if (currentLine.charAt(currentLine.length() - 1) != ';') {
            throw new IllegalStateException("Expected \";\"");
        }

        return currentLine.split("\\s");
    }

    @Override
    public void close() throws IOException {
        this.reader.close();
    }
}
