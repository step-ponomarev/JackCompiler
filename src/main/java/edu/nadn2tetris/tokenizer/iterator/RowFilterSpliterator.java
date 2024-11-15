package edu.nadn2tetris.tokenizer.iterator;

import java.util.Spliterator;
import java.util.Spliterators;
import java.util.function.Consumer;
import java.util.regex.Pattern;

final class RowFilterSpliterator extends Spliterators.AbstractSpliterator<String> {
    private static final Pattern COMMENTS_LINE_PATTERN = Pattern.compile("^(//.*|/\\*.*\\*/)$");
    private static final Pattern COMMENTS_START_BLOCK_PATTERN = Pattern.compile("^/\\*.*$");
    private static final Pattern COMMENTS_END_BLOCK_PATTERN = Pattern.compile("^.*\\*/$");
    private static final Pattern EMPTY_LINE_PATTERN = Pattern.compile("^\\s*$");
    private boolean commentBlockStarted;
    private final Spliterator<String> sourceSpliterator;

    public RowFilterSpliterator(Spliterator<String> sourceSpliterator) {
        super(sourceSpliterator.estimateSize(), sourceSpliterator.characteristics());
        this.sourceSpliterator = sourceSpliterator;
    }

    @Override
    public boolean tryAdvance(Consumer<? super String> action) {
        return sourceSpliterator.tryAdvance(r -> {
            if (EMPTY_LINE_PATTERN.matcher(r).matches()) {
                return;
            }

            if (COMMENTS_LINE_PATTERN.matcher(r).matches()) {
                return;
            }

            if (commentBlockStarted) {
                if (COMMENTS_END_BLOCK_PATTERN.matcher(r).matches()) {
                    commentBlockStarted = false;
                }
                return;
            }

            if (COMMENTS_START_BLOCK_PATTERN.matcher(r).matches()) {
                commentBlockStarted = true;
                return;
            }

            action.accept(
                    r.replaceAll("(//|/\\*).*", "").trim()
            );
        });
    }
}
