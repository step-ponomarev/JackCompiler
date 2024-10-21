package edu.nadn2tetris.tokenizer;

import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public enum TokenType {
    KEYWORD(createKeywordPatten()),
    //    SYMBOL(Pattern.compile()),
    IDENTIFIER(Pattern.compile("^[_a-zA-Z]+[_a-zA-Z\\d]*$")),
    INT_CONST(Pattern.compile("^\\d{1,5}$")),  // [0, 32767] by spec
    STRING_CONST(Pattern.compile("^\".*\"$"));

    private final Pattern pattern;

    TokenType(Pattern pattern) {
        this.pattern = pattern;
    }

    public static TokenType parse(String value) {
        if (value == null) {
            return null;
        }

        for (TokenType val : TokenType.values()) {
            if (val.pattern.matcher(value).matches()) {
                return val;
            }
        }

        return null;
    }

    private static Pattern createKeywordPatten() {
        final String keywordsUnion = Stream.of(Keyword.values())
                .map(p -> p.getPattern().pattern().substring(1, p.getPattern().pattern().length() - 1))
                .collect(Collectors.joining("|"));

        return Pattern.compile("^(" + keywordsUnion + ")$");
    }
}
