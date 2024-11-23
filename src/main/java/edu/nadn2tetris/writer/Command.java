package edu.nadn2tetris.writer;

public enum Command {
    ADD('+'),
    SUB('-'),
    NEG('-'),
    EQ('='),
    GT('>'),
    LT('<'),
    AND('&'),
    OR('|'),
    NOT('~');

    private final char op;

    Command(char op) {
        this.op = op;
    }

    public static Command parse(char ch) {
        for (Command command : values()) {
            if (command.op == ch) {
                return command;
            }
        }

        return null;
    }
}
