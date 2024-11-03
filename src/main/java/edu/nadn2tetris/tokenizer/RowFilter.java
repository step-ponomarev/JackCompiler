package edu.nadn2tetris.tokenizer;

import java.util.function.Predicate;

final class RowFilter implements Predicate<String> {
    @Override
    public boolean test(String s) {
        //TODO: support comments
        if (s.isBlank()) {
            return false;
        }

        return true;
    }
}
