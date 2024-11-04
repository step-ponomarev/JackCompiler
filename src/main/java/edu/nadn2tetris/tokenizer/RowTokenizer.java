package edu.nadn2tetris.tokenizer;

import java.util.Iterator;
import java.util.Objects;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

final class RowTokenizer implements Function<String, Stream<String>> {
    @Override
    public Stream<String> apply(String row) {
        final Matcher matcher = StatementPatterns.getMatcher(row);
        if (matcher == null) {
            throw new IllegalStateException("Unsupported row: " + row);
        }

        //TODO: либо разобраться с группами.
        //TODO: либо написать отдельный обработчик для каждого случая
        final int groupCount = matcher.groupCount();
        final Iterator<String> iterator = new Iterator<>() {
            private int i = 1;

            @Override
            public boolean hasNext() {
                return i <= groupCount;
            }

            @Override
            public String next() {
                String token = matcher.group(i++);
                if (token == null) {
                    return null;
                }
                
                //TODO: обрабатывать посимвольно, а не через группы
                //если прочитали букву, то читаем пока корректно, проверяем не кейворд ли это
                // и тд.
                TokenType tokenType = TokenType.parse(token);
                while (tokenType == null && hasNext()) {
                    token = matcher.group(i++);
                    if (token == null) {
                        return null;
                    }
                    tokenType = TokenType.parse(token);
                }

                if (tokenType == null && !hasNext()) {
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
}
