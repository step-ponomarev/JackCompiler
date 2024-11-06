package edu.nadn2tetris.tokenizer;

import java.util.Spliterator;
import java.util.Spliterators;
import java.util.function.Function;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

final class RowTokenizer implements Function<String, Stream<String>> {
    @Override
    public Stream<String> apply(String row) {
        return StreamSupport.stream(
                Spliterators.spliteratorUnknownSize(
                        new RowTokenizerIterator(row),
                        Spliterator.ORDERED
                ),
                false
        );
    }
}
