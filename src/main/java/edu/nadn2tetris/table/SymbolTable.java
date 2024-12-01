package edu.nadn2tetris.table;

import java.util.HashMap;
import java.util.Map;

import edu.nadn2tetris.ast.Type;

public final class SymbolTable {
    private final Map<String, IdentifierInfo> identifiers = new HashMap<>();
    private final Map<Kind, Short> kindIndex = new HashMap<>();

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

    public short size() {
        return (short) identifiers.size();
    }

    public IdentifierInfo getIdentifierInfo(String name) {
        return identifiers.get(name);
    }

    public short countOf(Kind kind) {
        Short i = kindIndex.get(kind);
        if (i == null) {
            return 0;
        }

        return (short) (i + 1);
    }
}
