package edu.nadn2tetris.tokenizer;

import java.util.Iterator;

final class RowTokenizerIterator implements Iterator<String> {
    private enum TokenType {
        LETTER,
        INT,
        STRING_CONST,
        SYMBOL;
    }

    private int i = 0;
    private final String row;

    public RowTokenizerIterator(String row) {
        this.row = row.trim();
    }

    @Override
    public boolean hasNext() {
        return i < row.length();
    }

    @Override
    public String next() {
        final TokenType tokenType = getTokenType();
        if (tokenType == null) {
            throw new IllegalStateException("Unsupported start symbol");
        }

        return switch (tokenType) {
            case INT -> getDigit();
            case LETTER -> getIdentificator();
            case SYMBOL -> getSymbol();
            case STRING_CONST -> getStringConst();
        };
    }

    private String getStringConst() {
        final int start = i;
        int end = start + 1;

        while (end < row.length()) {
            final char ch = row.charAt(end++);
            if (ch == '"') {
                break;
            }
        }
        i = end;

        return row.substring(start, end);
    }

    private String getSymbol() {
        return String.valueOf(row.charAt(i++));
    }

    private String getIdentificator() {
        final int start = i;
        int end = start + 1;

        while (end < row.length()) {
            final char ch = row.charAt(end);
            if (!Character.isLetterOrDigit(ch)) {
                break;
            }

            end++;
        }
        i = end;

        return row.substring(start, end);
    }

    private String getDigit() {
        final int start = i;
        int end = start + 1;

        while (end < row.length()) {
            final char ch = row.charAt(end);
            if (!Character.isDigit(ch)) {
                break;
            }

            end++;
        }
        i = end;

        return row.substring(start, end);
    }

    private TokenType getTokenType() {
        char firstChar = row.charAt(i);

        TokenType tokenType;
        while ((tokenType = getSymbolType(firstChar)) == null && hasNext()) {
            firstChar = row.charAt(++i);
        }

        return tokenType;
    }

    private static TokenType getSymbolType(char ch) {
        return Character.isLetter(ch)
                ? TokenType.LETTER
                : Character.isDigit(ch)
                ? TokenType.INT
                : ch == '"'
                ? TokenType.STRING_CONST
                : edu.nadn2tetris.common.TokenType.SYMBOL.pattern.matcher(String.valueOf(ch)).matches()
                ? TokenType.SYMBOL
                : null;
    }
}
