package edu.nadn2tetris.tokenizer.filter;

import java.util.Spliterator;
import java.util.function.Function;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public final class RowFilter implements Function<String, Stream<String>> {
    private final Spliterator<String> sourceSpliterator;

    public RowFilter(Spliterator<String> sourceSpliterator) {
        this.sourceSpliterator = sourceSpliterator;
    }

    @Override
    public Stream<String> apply(String row) {
        return StreamSupport.stream(
                new RowFilterSpliterator(sourceSpliterator),
                false
        );
    }
}