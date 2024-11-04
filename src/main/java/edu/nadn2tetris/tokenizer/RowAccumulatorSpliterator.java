package edu.nadn2tetris.tokenizer;

import java.util.Spliterator;
import java.util.Spliterators;
import java.util.function.Consumer;
import java.util.function.Predicate;

final class RowAccumulatorSpliterator extends Spliterators.AbstractSpliterator<String> {
    private final StringBuilder acc = new StringBuilder();
    private final Spliterator<String> source;

    private final Predicate<CharSequence> matcher;

    public RowAccumulatorSpliterator(Spliterator<String> source, Predicate<CharSequence> matcher) {
        super(source.estimateSize(), source.characteristics());
        this.source = source;
        this.matcher = matcher;
    }

    @Override
    public boolean tryAdvance(Consumer<? super String> action) {
        return source.tryAdvance(row -> {
            acc.append(row);
            if (!matcher.test(acc)) {
                return;
            }
            
            action.accept(acc.toString());
            acc.setLength(0);
        });
    }
}
