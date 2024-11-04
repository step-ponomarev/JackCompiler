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
    public static final Pattern EXPRESSION_LIST_PATTERN = createExpresionListPattern();
    public static final Pattern RETURNM_PATTERN = createReturnPattern();
    public static final Pattern DO_PATTERN = createDoPattern();
    public static final Pattern WHILE_PATTERN = createWhilePattern();
    public static final Pattern IF_PATTERN = createIfPattern();
    public static final Pattern LET_PATTERN = createLetPattern();
    public static final Pattern STATEMENT_PATTERN = createStatementPattern();
    public static final Pattern STATEMENTS_PATTERN = createStatementsPattern();
    public static final Pattern SUBROUTINE_BODY_PATTERN = createSubroutineBodyPattern();

    public static final Pattern TYPE_PATTERN = createTypePattern();

    public static final Pattern PARAMETER_LIST_PATTERN = parameterListPattern();
    public static final Pattern SUBROUTINE_DECLARATION_PATTERN = createSubroutineDeclarationPattern();
    public static final Pattern CLASS_VAR_DECLARATION_PATTERN = createClassVarDeclarationPattern();
    public static final Pattern CLASS_PATTERN = createClassPattern();


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
        return Pattern.compile("^((%s) ((%s) (%s))*)$".formatted(
                        TERM_PATTERN.pattern().substring(1, TERM_PATTERN.pattern().length() - 1),
                        OPERATOR_PATTERN.pattern().substring(1, OPERATOR_PATTERN.pattern().length() - 1),
                        TERM_PATTERN.pattern().substring(1, TERM_PATTERN.pattern().length() - 1)
                )
        );
    }

    private static Pattern createDoPattern() {
        return Pattern.compile("^((do) (%s) (;))$".formatted(
                        SUBROUTINE_CALL_PATTERN.pattern().substring(1, SUBROUTINE_CALL_PATTERN.pattern().length() - 1)
                )
        );
    }

    private static Pattern createIfPattern() {
        return Pattern.compile("^((if) (\\()(%s)(\\)) ({)(.+)(}) ((else) ({)(.+)(}))?)$".formatted(
                        SUBROUTINE_CALL_PATTERN.pattern().substring(1, SUBROUTINE_CALL_PATTERN.pattern().length() - 1)
                )
        );
    }

    private static Pattern createWhilePattern() {
        return Pattern.compile("^((while) (\\()(%s)(\\)) ({)(.+)(}))$".formatted(
                        EXPRESSION_PATTERN.pattern().substring(1, EXPRESSION_PATTERN.pattern().length() - 1)
                )
        );
    }

    private static Pattern createExpresionListPattern() {
        return Pattern.compile("^(((%s) ((,) (%s))*)?)$".formatted(
                        EXPRESSION_PATTERN.pattern().substring(1, EXPRESSION_PATTERN.pattern().length() - 1),
                        EXPRESSION_PATTERN.pattern().substring(1, EXPRESSION_PATTERN.pattern().length() - 1)
                )
        );
    }

    private static Pattern createReturnPattern() {
        return Pattern.compile("^((return) (%s)? (;))$".formatted(
                        EXPRESSION_PATTERN.pattern().substring(1, EXPRESSION_PATTERN.pattern().length() - 1)
                )
        );
    }

    private static Pattern createLetPattern() {
        return Pattern.compile("^((let) (%s) ((\\[)%s(\\]))? (=) (%s)(;))$".formatted(
                        VAR_NAME_PATTERN.pattern().substring(1, VAR_NAME_PATTERN.pattern().length() - 1),
                        EXPRESSION_PATTERN.pattern().substring(1, EXPRESSION_PATTERN.pattern().length() - 1),
                        EXPRESSION_PATTERN.pattern().substring(1, EXPRESSION_PATTERN.pattern().length() - 1)
                )
        );
    }

    private static Pattern createStatementPattern() {
        return Pattern.compile("^((%s)|(%s)|(%s)|(%s)|(%s))$".formatted(
                        LET_PATTERN.pattern().substring(1, LET_PATTERN.pattern().length() - 1),
                        IF_PATTERN.pattern().substring(1, IF_PATTERN.pattern().length() - 1),
                        WHILE_PATTERN.pattern().substring(1, WHILE_PATTERN.pattern().length() - 1),
                        DO_PATTERN.pattern().substring(1, DO_PATTERN.pattern().length() - 1),
                        RETURNM_PATTERN.pattern().substring(1, RETURNM_PATTERN.pattern().length() - 1)
                )
        );
    }

    private static Pattern createStatementsPattern() {
        return Pattern.compile("^((%s)*)$".formatted(
                        STATEMENT_PATTERN.pattern().substring(1, STATEMENT_PATTERN.pattern().length() - 1)
                )
        );
    }

    private static Pattern createSubroutineBodyPattern() {
        return Pattern.compile("^(({)(%s)* (%s)(}))$".formatted(
                        VAR_DECLARATION_PATTERN.pattern().substring(1, VAR_NAME_PATTERN.pattern().length() - 1),
                        STATEMENTS_PATTERN.pattern().substring(1, STATEMENTS_PATTERN.pattern().length() - 1)
                )
        );
    }

    private static Pattern createVarDeclarationPattern() {
        final String identifierPattern = VAR_NAME_PATTERN.pattern().substring(1, VAR_NAME_PATTERN.pattern().length() - 1);
        return Pattern.compile("^((var) (int|char|boolean|%s) (%s) ((,) (%s))*(;))$"
                .formatted(
                        identifierPattern,
                        identifierPattern,
                        identifierPattern
                )
        );
    }

    private static Pattern createTypePattern() {
        return Pattern.compile("^((int)|(char)|(boolean)|(%s)))$".formatted(
                        CLASS_NAME_PATTERN.pattern().substring(1, CLASS_NAME_PATTERN.pattern().length() - 1)
                )
        );
    }

    private static Pattern parameterListPattern() {
        return Pattern.compile("^(((%s) (%s)) ((,) (%s), (%s))*)?$".formatted(
                        TYPE_PATTERN.pattern().substring(1, TYPE_PATTERN.pattern().length() - 1),
                        VAR_NAME_PATTERN.pattern().substring(1, VAR_NAME_PATTERN.pattern().length() - 1),
                        TYPE_PATTERN.pattern().substring(1, TYPE_PATTERN.pattern().length() - 1),
                        VAR_NAME_PATTERN.pattern().substring(1, VAR_NAME_PATTERN.pattern().length() - 1)
                )
        );
    }

    private static Pattern createSubroutineDeclarationPattern() {
        return Pattern.compile("^(((constructor)|(function)|(method)) ((void)|(%s)) (%s) (\\()(%s)(\\))) (%s))$".formatted(
                        TYPE_PATTERN.pattern().substring(1, TYPE_PATTERN.pattern().length() - 1),
                        SUBROUTINE_NAME_PATTERN.pattern().substring(1, SUBROUTINE_NAME_PATTERN.pattern().length() - 1),
                        PARAMETER_LIST_PATTERN.pattern().substring(1, PARAMETER_LIST_PATTERN.pattern().length() - 1),
                        SUBROUTINE_BODY_PATTERN.pattern().substring(1, SUBROUTINE_BODY_PATTERN.pattern().length() - 1)
                )
        );
    }

    private static Pattern createClassVarDeclarationPattern() {
        return Pattern.compile("^(((static)|(field)) (%s) (%s) ((,) (%s))*(;))$".formatted(
                        TYPE_PATTERN.pattern().substring(1, TYPE_PATTERN.pattern().length() - 1),
                        VAR_NAME_PATTERN.pattern().substring(1, VAR_NAME_PATTERN.pattern().length() - 1),
                        VAR_NAME_PATTERN.pattern().substring(1, VAR_NAME_PATTERN.pattern().length() - 1)
                )
        );
    }

    private static Pattern createClassPattern() {
        return Pattern.compile("^((class) (%s) ({)(%s)* (%s)*(}))$".formatted(
                        CLASS_NAME_PATTERN.pattern().substring(1, CLASS_NAME_PATTERN.pattern().length() - 1),
                        CLASS_VAR_DECLARATION_PATTERN.pattern().substring(1, CLASS_VAR_DECLARATION_PATTERN.pattern().length() - 1),
                        SUBROUTINE_DECLARATION_PATTERN.pattern().substring(1, SUBROUTINE_DECLARATION_PATTERN.pattern().length() - 1)
                )
        );
    }
}