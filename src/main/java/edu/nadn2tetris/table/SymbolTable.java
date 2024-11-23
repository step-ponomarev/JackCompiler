package edu.nadn2tetris.table;

import java.util.HashMap;
import java.util.Map;

public final class SymbolTable {
    private Map<String, IdentifierInfo> identifiers = new HashMap<>();
    private Map<Kind, Short> kindIndex = new HashMap<>();


    public void reset() {
        this.identifiers.clear();
        this.kindIndex.clear();
    }

    /**
     * @param name имя идентефикатора
     * @param type keyword or Class name
     * @param kind kind
     */
    public void define(String name, String type, Kind kind) {
        final short [] index = new short[1];
        kindIndex.compute(kind, (Kind key, Short prev) -> {
            index[0] = (short) (prev == null ? 0 : prev + 1);
            return index[0];
        });

        identifiers.put(name, new IdentifierInfo(name, type, kind, index[0]));
    }

    public short varCount(Kind kind) {
        if (kind == null) {
            return 0;
        }

        final Short count = kindIndex.get(kind);

        return count == null ? 0 : count;
    }

    public String typeOf(String name) {
        return getIdentifierInfo(name).type;
    }

    public short indexOf(String name) {
        return getIdentifierInfo(name).index;
    }

    public IdentifierInfo getIdentifierInfo(String name) {
        return identifiers.get(name);
    }
}
