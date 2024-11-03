package edu.nadn2tetris.tokenizer;

import java.util.regex.Pattern;

public class StatementPatterns {
    public static final Pattern KEYWORD_CONSTANT_PATTERN = Pattern.compile("^(true|false|null|this)$");
    public static final Pattern UNARY_OPERATOR_PATTERN = Pattern.compile("^[-~]$");
    public static final Pattern OPERATOR_PATTERN = Pattern.compile("^[+\\-\\*\\/\\&\\|\\<\\>\\=\\~]$");
    public static final Pattern INTEGER_CONSTANT_PATTERN = TokenType.INT_CONST.pattern;

    public static final Pattern STRING_CONSTANT_PATTERN = TokenType.STRING_CONST.pattern;

    public static final Pattern CLASS_NAME_PATTERN = TokenType.IDENTIFIER.pattern;
    public static final Pattern SUBROUTINE_NAME_PATTERN = TokenType.IDENTIFIER.pattern;
    public static final Pattern VAR_NAME_PATTERN = TokenType.IDENTIFIER.pattern;
    public static final Pattern VAR_DECLARATION_PATTERN = createVarDeclarationPattern();
    public static final Pattern SUBROUTINE_CALL_PATTERN = createSubroutimeCallPattern();
    public static final Pattern TERM_PATTERN = createTermPattern();
    public static final Pattern EXPRESSION_PATTERN = createExpresionPattern();

    private static Pattern createSubroutimeCallPattern() {

        return Pattern.compile("^((%s)(\\()(.+)(\\))|((%s)|(%s))(\\.)(%s))$"
                .formatted(
                        VAR_NAME_PATTERN.pattern().substring(1, VAR_NAME_PATTERN.pattern().length() - 1),
                        VAR_NAME_PATTERN.pattern().substring(1, VAR_NAME_PATTERN.pattern().length() - 1),
                        VAR_NAME_PATTERN.pattern().substring(1, VAR_NAME_PATTERN.pattern().length() - 1),
                        VAR_NAME_PATTERN.pattern().substring(1, VAR_NAME_PATTERN.pattern().length() - 1)
                )
        );
    }

    private static Pattern createTermPattern() {
        final String ARRAY_PATTERN = "(%s)(\\[)(.+)(\\])".formatted(VAR_NAME_PATTERN.pattern().substring(1, VAR_NAME_PATTERN.pattern().length() - 1));
        final String EXPRESSION_PATTERN = "(\\()(.+)(\\))";
        final String UNARY_OP_PATTERN = "(%s) (.+)".formatted(UNARY_OPERATOR_PATTERN.pattern().substring(1, UNARY_OPERATOR_PATTERN.pattern().length() - 1));

        return Pattern.compile("^((%s)|(%s)|(%s)|(%s)|(%s)|(%s)|(%s))$"
                .formatted(
                        INTEGER_CONSTANT_PATTERN.pattern().substring(1, INTEGER_CONSTANT_PATTERN.pattern().length() - 1),
                        STRING_CONSTANT_PATTERN.pattern().substring(1, STRING_CONSTANT_PATTERN.pattern().length() - 1),
                        KEYWORD_CONSTANT_PATTERN.pattern().substring(1, KEYWORD_CONSTANT_PATTERN.pattern().length() - 1),
                        ARRAY_PATTERN,
                        EXPRESSION_PATTERN,
                        UNARY_OP_PATTERN,
                        SUBROUTINE_CALL_PATTERN.pattern().substring(1, SUBROUTINE_CALL_PATTERN.pattern().length() - 1)
                )
        );
    }


    private static Pattern createExpresionPattern() {
        return Pattern.compile("^(%s) (\\()(%s) (%s)(\\))$".formatted(
                        TERM_PATTERN.pattern().substring(1, TERM_PATTERN.pattern().length() - 1),
                        OPERATOR_PATTERN.pattern().substring(1, OPERATOR_PATTERN.pattern().length() - 1),
                        TERM_PATTERN.pattern().substring(1, TERM_PATTERN.pattern().length() - 1)
                )
        );
    }

    private static Pattern createVarDeclarationPattern() {
        final String identifierPattern = VAR_NAME_PATTERN.pattern().substring(1, VAR_NAME_PATTERN.pattern().length() - 1);
        return Pattern.compile("^(var)\\s+(int|char|boolean|%s)\\s+(%s)\\s*((,)\\s*(%s))*(\\s*;)$"
                .formatted(
                        identifierPattern,
                        identifierPattern,
                        identifierPattern
                )
        );
    }
}