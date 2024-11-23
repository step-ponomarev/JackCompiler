package edu.nadn2tetris.table;

public final class IdentifierInfo {
    public final String name;
    public final String type;
    public final Kind kind;
    public final short index;

    public IdentifierInfo(String name, String type, Kind kind, short index) {
        this.name = name;
        this.type = type;
        this.kind = kind;
        this.index = index;
    }
}