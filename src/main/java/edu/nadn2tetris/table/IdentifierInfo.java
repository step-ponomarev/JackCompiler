package edu.nadn2tetris.table;

import edu.nadn2tetris.ast.Type;

public final class IdentifierInfo {
    public final String name;
    public final Type type;
    public final String classType;
    public final Kind kind;
    public final short index;

    public static class Builder {
        private String name;
        private Type type;
        private String classType;
        private Kind kind;
        private short index;

        private Builder() {}

        public IdentifierInfo build() {
            if (name == null || type == null || kind == null) {
                throw new IllegalStateException("Cannot build an identifier");
            }

            return new IdentifierInfo(name, type, classType, kind, index);
        }

        public Builder setName(String name) {
            this.name = name;
            return this;
        }

        public Builder setType(Type type) {
            this.type = type;
            this.classType = null;
            return this;
        }

        public Builder setClassType(String type) {
            this.type = Type.CLASS;
            this.classType = type;
            return this;
        }

        public Builder setKind(Kind kind) {
            this.kind = kind;
            return this;
        }

        public Builder setIndex(short index) {
            this.index = index;
            return this;
        }
    }

    private IdentifierInfo(String name, Type type, String classType, Kind kind, short index) {
        this.name = name;
        this.type = type;
        this.classType = classType;
        this.kind = kind;
        this.index = index;
    }

    public static Builder builder() {
        return new Builder();
    }
}