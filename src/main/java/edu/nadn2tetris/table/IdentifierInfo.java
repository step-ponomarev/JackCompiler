package edu.nadn2tetris.table;

public final class IdentifierInfo {
    public final String name;
    public final String type;
    public final Kind kind;

    public IdentifierInfo(String name, String type, Kind kind) {
        this.name = name;
        this.type = type;
        this.kind = kind;
    }
}