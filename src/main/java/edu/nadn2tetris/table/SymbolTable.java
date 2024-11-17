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
        identifiers.put(name, new IdentifierInfo(name, type, kind));
        kindIndex.compute(kind, (Kind key, Short prev) -> {
            if (prev == null) {
                return (short) 0;
            }

            return (short) (prev + 1);
        });
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
        return varCount(
                getIdentifierInfo(
                        name
                ).kind
        );
    }

    public IdentifierInfo getIdentifierInfo(String name) {
        return identifiers.get(name);
    }
}
