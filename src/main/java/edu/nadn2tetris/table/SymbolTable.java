package edu.nadn2tetris.table;

import java.util.HashMap;
import java.util.Map;

import edu.nadn2tetris.ast.Type;

public final class SymbolTable {
    private Map<String, IdentifierInfo> identifiers = new HashMap<>();
    private Map<Kind, Short> kindIndex = new HashMap<>();

    /**
     * @param name имя идентефикатора
     * @param type keyword or Class name
     * @param kind kind
     */
    public void define(String name, Type type, Kind kind) {
        define(name, type.getCodeValue(), kind);
    }

    public void define(String name, String type, Kind kind) {
        final short[] index = new short[1];
        kindIndex.compute(kind, (Kind key, Short prev) -> {
            index[0] = (short) (prev == null ? 0 : prev + 1);
            return index[0];
        });


        final IdentifierInfo.Builder infoBuilder = IdentifierInfo.builder()
                .setName(name)
                .setKind(kind)
                .setIndex(index[0]);

        final Type varType = Type.parse(type);
        if (varType == Type.CLASS) {
            infoBuilder.setClassType(type);
        } else {
            infoBuilder.setType(varType);
        }

        identifiers.put(name, infoBuilder.build());
    }

    public IdentifierInfo getIdentifierInfo(String name) {
        return identifiers.get(name);
    }
}
