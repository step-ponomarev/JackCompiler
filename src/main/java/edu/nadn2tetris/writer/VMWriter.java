package edu.nadn2tetris.writer;

import java.io.Closeable;
import java.io.IOException;
import java.io.Writer;

public final class VMWriter implements Closeable {
    private final Writer writer;
    private final StringBuilder code = new StringBuilder();

    private static final String PUSH_TEMPLATE = "push %s %d\n";
    private static final String POP_TEMPLATE = "pop %s %d\n";
    private static final String LABEL_TEMPLATE = "label %s\n";

    public VMWriter(Writer writer) {
        if (writer == null) {
            throw new IllegalArgumentException("Writer cannot be null");
        }
        this.writer = writer;
    }

    public void writePush(Segment segment, short index) {
        code.append(PUSH_TEMPLATE.formatted(segment.name().toLowerCase(), index));
    }

    public void writePop(Segment segment, short index) {
        code.append(POP_TEMPLATE.formatted(segment.name().toLowerCase(), index));
    }

    public void writeArithmetic(Command command) {
        code.append(command.name().toLowerCase()).append("\n");
    }

    public void writeLabel(String label) {
        code.append(LABEL_TEMPLATE.formatted(label));
    }

    public void writeGoto(String label) {
        code.append("goto %s\n".formatted(label));
    }

    public void writeIf(String label) {
        code.append("if-goto %s\n".formatted(label));
    }

    public void writeCall(String label, int nArgs) {
        code.append("call %s %d\n".formatted(label, nArgs));
    }

    public void writeFunction(String label, int nArgs) {
        code.append("function %s %d\n".formatted(label, nArgs));
    }

    public void writeReturn() {
        code.append("return\n");
    }

    @Override
    public void close() throws IOException {
        this.writer.append(code).close();
    }
}
