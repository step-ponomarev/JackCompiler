package edu.nadn2tetris.common;

import java.util.regex.Pattern;

public enum Keyword {
    CLASS(Pattern.compile("^class$")),
    CONSTRUCTOR(Pattern.compile("^constructor$")),
    FUNCTION(Pattern.compile("^function$")),
    METHOD(Pattern.compile("^method$")),
    FIELD(Pattern.compile("^field$")),
    STATIC(Pattern.compile("^static$")),
    VAR(Pattern.compile("^var$")),
    INT(Pattern.compile("^int$")),
    CHAR(Pattern.compile("^char$")),

    BOOLEAN(Pattern.compile("^boolean$")),
    VOID(Pattern.compile("^void$")),
    TRUE(Pattern.compile("^true$")),
    FALSE(Pattern.compile("^false$")),
    NULL(Pattern.compile("^null$")),
    THIS(Pattern.compile("^this$")),
    LET(Pattern.compile("^let$")),
    DO(Pattern.compile("^do$")),
    IF(Pattern.compile("^if$")),
    ELSE(Pattern.compile("^else$")),
    WHILE(Pattern.compile("^while$")),
    RETURN(Pattern.compile("^return$"));

    private final Pattern pattern;

    Keyword(Pattern pattern) {
        this.pattern = pattern;
    }

    public static Keyword parse(String keyword) {
        if (keyword == null) {
            return null;
        }

        for (Keyword val : Keyword.values()) {
            if (val.pattern.matcher(keyword).matches()) {
                return val;
            }
        }

        return null;
    }

    public static boolean isKeyword(String keyword) {
        return parse(keyword) != null;
    }
    
    public Pattern getPattern() {
        return this.pattern;
    }
}
