package edu.nadn2tetris.ast;

import java.util.Objects;

public enum Type {
    BOOLEAN("boolean"),
    INTEGER("int"),
    CHAR("char"),
    VOID("void"),
    CLASS(null);

    private final String code;

    Type(String code) {
        this.code = code;
    }

    public String getCodeValue() {
        return code;
    }

    public static Type parse(String name) {
        if (name == null) {
            throw new IllegalArgumentException("Cannot parse null type");
        }

        for (Type type : Type.values()) {
            if (Objects.equals(type.code, name)) {
                return type;
            }
        }

        return CLASS;
    }
}