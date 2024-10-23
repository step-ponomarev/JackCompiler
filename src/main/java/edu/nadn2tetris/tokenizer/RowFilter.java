package edu.nadn2tetris.tokenizer;

import java.util.function.Predicate;

public final class RowFilter implements Predicate<String> {
    @Override
    public boolean test(String s) {
        if (s.isBlank()) {
            return false;
        }

        return true;
    }
}
