package edu.nadn2tetris.compiler;

import java.util.Objects;

public enum Flag {
    XML_MODE("--xml"),
    GENERATE_CODE("--code");

    private final String flag;

    Flag(String flag) {
        this.flag = flag;
    }

    public static Flag parse(String flag) {
        for (Flag f : Flag.values()) {
            if (Objects.equals(f.flag, flag)) {
                return f;
            }
        }

        throw new IllegalArgumentException("Unsupported flag: " + flag);
    }
}
