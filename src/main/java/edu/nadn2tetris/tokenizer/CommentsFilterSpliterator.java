package edu.nadn2tetris.tokenizer;

import java.util.Spliterator;
import java.util.Spliterators;
import java.util.function.Consumer;
import java.util.regex.Pattern;

final class CommentsFilterSpliterator extends Spliterators.AbstractSpliterator<String> {
    private final Spliterator<String> source;

    private final Pattern COMMENTS_LINE_PATTERN = Pattern.compile("^(//.*|/\\*.*\\*/)$");
    private final Pattern COMMENTS_START_BLOCK_PATTERN = Pattern.compile("^/\\*.*$");
    private final Pattern COMMENTS_END_BLOCK_PATTERN = Pattern.compile("^.*\\*/$");

    private boolean commentBlockStarted;

    public CommentsFilterSpliterator(Spliterator<String> source) {
        super(source.estimateSize(), source.characteristics());
        this.source = source;
    }

    @Override
    public boolean tryAdvance(Consumer<? super String> action) {
        return source.tryAdvance(row -> {
            boolean endComments = commentBlockStarted && COMMENTS_END_BLOCK_PATTERN.matcher(row).matches();
            if (commentBlockStarted && !endComments) {
                return;
            } else if (commentBlockStarted) {
                commentBlockStarted = false;
                return;
            }

            if (COMMENTS_LINE_PATTERN.matcher(row).matches()) {
                return;
            }

            if (COMMENTS_START_BLOCK_PATTERN.matcher(row).matches()) {
                commentBlockStarted = true;
                return;
            }

            action.accept(row);
        });
    }
}
