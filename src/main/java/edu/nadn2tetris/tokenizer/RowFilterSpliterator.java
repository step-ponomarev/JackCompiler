package edu.nadn2tetris.tokenizer;

import java.util.function.Predicate;
import java.util.regex.Pattern;

final class RowFilterSpliterator implements Predicate<String> {
    private static final Pattern COMMENTS_LINE_PATTERN = Pattern.compile("^(//.*|/\\*.*\\*/)$");
    private static final Pattern COMMENTS_START_BLOCK_PATTERN = Pattern.compile("^/\\*.*$");
    private static final Pattern COMMENTS_END_BLOCK_PATTERN = Pattern.compile("^.*\\*/$");
    private static final Pattern EMPTY_LINE_PATTERN = Pattern.compile("^\\s*$");
    private boolean commentBlockStarted;

    @Override
    public boolean test(String row) {
        boolean endComments = commentBlockStarted && COMMENTS_END_BLOCK_PATTERN.matcher(row).matches();
        if (commentBlockStarted) {
            if (endComments) {
                commentBlockStarted = false;
            }

            return false;
        }

        if (EMPTY_LINE_PATTERN.matcher(row).matches()) {
            return false;
        }

        if (COMMENTS_LINE_PATTERN.matcher(row).matches()) {
            return false;
        }

        if (COMMENTS_START_BLOCK_PATTERN.matcher(row).matches()) {
            commentBlockStarted = true;
            return false;
        }

        return true;
    }
}
