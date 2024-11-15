package edu.nadn2tetris.tokenizer.iterator;

import java.util.Objects;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

final class TokenUtils {
    private static final RowTokenizer ROW_TOKENIZER = new RowTokenizer();

    public static Stream<String> tokenStreamOf(Stream<String> stream) {
        return StreamSupport.stream(
                        new RowFilterSpliterator(stream.spliterator()),
                        false
                ).flatMap(ROW_TOKENIZER)
                .filter(Objects::nonNull);
    }

    private TokenUtils() {}
}