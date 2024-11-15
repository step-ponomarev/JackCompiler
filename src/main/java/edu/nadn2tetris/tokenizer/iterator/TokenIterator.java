package edu.nadn2tetris.tokenizer.iterator;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.IOException;
import java.io.Reader;
import java.util.Iterator;
import java.util.NoSuchElementException;

public final class TokenIterator implements Iterator<String>, Closeable {
    private final Iterator<String> tokens;
    private final Reader reader;

    public TokenIterator(BufferedReader rowReader) {
        this.reader = rowReader;
        this.tokens = TokenUtils.tokenStreamOf(rowReader.lines()).iterator();
    }

    @Override
    public boolean hasNext() {
        return tokens.hasNext();
    }

    @Override
    public String next() {
        if (!hasNext()) {
            throw new NoSuchElementException();
        }

        return tokens.next();
    }

    @Override
    public void close() throws IOException {
        this.reader.close();
    }
}
