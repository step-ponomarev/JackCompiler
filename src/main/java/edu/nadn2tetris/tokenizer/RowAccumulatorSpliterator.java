package edu.nadn2tetris.tokenizer;

import java.util.Spliterator;
import java.util.Spliterators;
import java.util.function.Consumer;
import java.util.regex.Pattern;

final class RowAccumulatorSpliterator extends Spliterators.AbstractSpliterator<String> {
    private final StringBuilder acc = new StringBuilder();
    private final Spliterator<String> source;
    private final Pattern[] patterns;

    public RowAccumulatorSpliterator(Spliterator<String> source, Pattern[] patterns) {
        super(source.estimateSize(), source.characteristics());
        this.source = source;
        this.patterns = patterns;
    }

    @Override
    public boolean tryAdvance(Consumer<? super String> action) {
        return source.tryAdvance(row -> {
            acc.append(row);


            for (Pattern pattern : patterns) {
                if (!pattern.matcher(acc).matches()) {
                    continue;
                }

                action.accept(acc.toString());
                acc.setLength(0);
            }
        });
    }
}
