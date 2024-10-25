package edu.nadn2tetris.tokenizer;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

public final class RowTokenizer implements Function<String, Stream<String>> {
    private static final Pattern[] ROW_PATTERNS = new Pattern[]{
            createVarDecPattern()
    };

    @Override
    public Stream<String> apply(String row) {
        final Matcher matcher = getRowMatcher(row);
        if (matcher == null) {
            throw new IllegalStateException("Unsupported row: " + row);
        }

        final List<String> tokens = new ArrayList<>(matcher.groupCount());
        for (int i = 1; i <= matcher.groupCount(); i++) {
            final String token = matcher.group(i);
            if (token == null) {
                continue;
            }

            if (TokenType.parse(token) == null) {
                throw new IllegalStateException("Invalid token: " + token);
            }

            tokens.add(token);
        }

        return tokens.stream();
    }

    private static Matcher getRowMatcher(String row) {
        for (Pattern pattern : ROW_PATTERNS) {
            final Matcher matcher = pattern.matcher(row);
            if (!matcher.matches()) {
                continue;
            }

            return matcher;
        }

        return null;
    }

    // 'var' type varName (',' varName)* ';
    private static Pattern createVarDecPattern() {
        final String identifierPattern = TokenType.IDENTIFIER.pattern.pattern().substring(1, TokenType.IDENTIFIER.pattern.pattern().length() - 1);
        return Pattern
                .compile("^(var)\\s+(int|char|boolean|%s)\\s+(%s)\\s*((,)\\s*(%s))*(\\s*;)$"
                        .formatted(identifierPattern, identifierPattern, identifierPattern)
                );
    }
}
