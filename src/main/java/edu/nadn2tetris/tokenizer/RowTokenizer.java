package edu.nadn2tetris.tokenizer;

import java.util.Iterator;
import java.util.Objects;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public final class RowTokenizer implements Function<String, Stream<String>> {
    private static final Pattern[] ROW_PATTERNS = new Pattern[] {
            createVarDecPattern()
    };

    @Override
    public Stream<String> apply(String row) {
        final Matcher matcher = getRowMatcher(row);
        if (matcher == null) {
            throw new IllegalStateException("Unsupported row: " + row);
        }

        final int groupCount = matcher.groupCount();
        final Iterator<String> iterator = new Iterator<>() {
            private int i = 1;

            @Override
            public boolean hasNext() {
                return i <= groupCount;
            }

            @Override
            public String next() {
                final String token = matcher.group(i++);
                if (token == null) {
                    return null;
                }

                if (TokenType.parse(token) == null) {
                    throw new IllegalStateException("Invalid token: " + token);
                }

                return token.trim();
            }
        };

        return StreamSupport.stream(
                Spliterators.spliteratorUnknownSize(iterator, Spliterator.ORDERED),
                false
        ).filter(Objects::nonNull);
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
