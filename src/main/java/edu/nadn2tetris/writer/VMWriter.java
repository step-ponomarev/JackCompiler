package edu.nadn2tetris.writer;

import java.io.Closeable;
import java.io.IOException;

public final class VMWriter implements Closeable {
    public void writePush(Segment segment, short index) {
        throw new UnsupportedOperationException();
    }

    public void writePop(Segment segment, short index) {
        throw new UnsupportedOperationException();
    }

    public void writeArithmetic(Command command) {
        throw new UnsupportedOperationException();
    }

    public void writeLabel(String label) {
        throw new UnsupportedOperationException();
    }

    public void writeGoto(String label) {
        throw new UnsupportedOperationException();
    }

    public void writeId(String label) {
        throw new UnsupportedOperationException();
    }

    public void writeCall(String label) {
        throw new UnsupportedOperationException();
    }

    public void writeFunction(String label) {
        throw new UnsupportedOperationException();
    }

    public void writeReturn() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void close() throws IOException {
        throw new UnsupportedOperationException();
    }
}
