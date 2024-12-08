package edu.nadn2tetris.writer;

public enum Segment {
    CONSTANT,
    ARGUMENT,
    LOCAL,
    STATIC,
    THIS, // this data
    THAT, // that data
    POINTER, // addr segment 0 - this, 1 - that
    TEMP
}
