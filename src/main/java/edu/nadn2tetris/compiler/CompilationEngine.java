package edu.nadn2tetris.compiler;

import java.io.BufferedWriter;
import java.io.Closeable;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;

import edu.nadn2tetris.common.Keyword;
import edu.nadn2tetris.common.TokenType;
import edu.nadn2tetris.tokenizer.JackTokenizer;

public final class CompilationEngine implements Closeable {
    private static final String TAB_SYMBOL = "\s\s";
    private final JackTokenizer tokenizer;
    private final BufferedWriter bufferedWriter;

    private boolean tokenIsBuffered = false;
    private int nestingLevel = 0;

    public CompilationEngine(JackTokenizer tokenizer, OutputStream out) {
        this.tokenizer = tokenizer;
        this.bufferedWriter = new BufferedWriter(new OutputStreamWriter(out));
    }

    public void compileClass() {
        advance();
        openBlock(StatementType.CLASS);

        writeToken();
        advance();
        writeToken();

        // {
        advance();
        writeToken();

        advance();
        if (tokenizer.tokenType() == TokenType.SYMBOL && tokenizer.symbol() == '}') {
            writeToken();
            return;
        }

        while (tokenizer.tokenType() == TokenType.KEYWORD && isClassVarDec(tokenizer.keyword())) {
            compileClassVarDec();
            advance();
        }

        while (tokenizer.tokenType() == TokenType.KEYWORD && isSubroutineDec(tokenizer.keyword())) {
            compileSubroutine();
            advance();
        }

        // }
        writeToken();
        closeBlock(StatementType.CLASS);
    }

    private boolean isSubroutineDec(Keyword keyword) {
        return keyword == Keyword.CONSTRUCTOR
                || keyword == Keyword.FUNCTION
                || keyword == Keyword.METHOD;
    }

    private boolean isClassVarDec(Keyword keyword) {
        return keyword == Keyword.STATIC || keyword == Keyword.FIELD;
    }

    public void compileClassVarDec() {
        writeToken();

        advance();
        writeToken();

        advance();
        writeToken();

        if (!tokenizer.hasMoreTokens()) {
            return;
        }

        advance();
        if (tokenizer.tokenType() != TokenType.SYMBOL || tokenizer.symbol() == ',') {
            return;
        }

        //,
        writeToken();

        advance();
        compileVarDec();
    }

    public void compileSubroutine() {
        openBlock(StatementType.SUBROUTINE_DEC);
        writeToken();

        advance();
        writeToken();

        advance();
        writeToken();

        //(
        advance();
        writeToken();

        advance();
        compileParameterList();

        //)
        advance();
        writeToken();

        advance();
        compileSubroutineBody();
        closeBlock(StatementType.SUBROUTINE_DEC);
    }

    public void compileSubroutineBody() {
        openBlock(StatementType.SUBROUTINE_BODY);
        //{
        writeToken();

        advance();
        while (tokenizer.tokenType() == TokenType.KEYWORD && tokenizer.keyword() == Keyword.VAR) {
            compileVarDec();
            advance();
        }

        compileStatements();

        //}
        advance();
        writeToken();
        closeBlock(StatementType.SUBROUTINE_BODY);
    }

    public void compileParameterList() {
        openBlock(StatementType.PARAMETER_LIST);
        compileParameterListNested();
        closeBlock(StatementType.PARAMETER_LIST);
    }

    private void compileParameterListNested() {
        if (!isType(tokenizer)) {
            return;
        }

        writeToken();

        advance();
        if (tokenizer.tokenType() != TokenType.SYMBOL || tokenizer.symbol() != ',') {
            return;
        }

        //,
        writeToken();

        advance();
        compileParameterListNested();
    }

    private static boolean isType(JackTokenizer tokenizer) {
        if (tokenizer.tokenType() == TokenType.KEYWORD) {
            return tokenizer.keyword() == Keyword.INT
                    || tokenizer.keyword() == Keyword.CHAR
                    || tokenizer.keyword() == Keyword.BOOLEAN;
        }

        return tokenizer.tokenType() == TokenType.IDENTIFIER;
    }

    public void compileVarDec() {
        openBlock(StatementType.VAR_DEC);
        compileVarDec(false);
        closeBlock(StatementType.VAR_DEC);
    }

    private void compileVarDec(boolean list) {
        if (!list) {
            writeToken();

            advance();
            writeToken();

            advance();
        }

        writeToken();

        advance();
        if (tokenizer.symbol() == ';') {
            writeToken();
            return;
        }

        writeToken();

        advance();
        compileVarDec(true);
    }

    public void compileStatements() {
        openBlock(StatementType.STATEMENTS);
        compileStatementsNested();
        closeBlock(StatementType.STATEMENTS);
    }

    private void compileStatementsNested() {
        if (!isStatement(tokenizer.keyword())) {
            return;
        }

        compileStatement();

        advance();
        if (tokenizer.tokenType() != TokenType.KEYWORD || !isStatement(tokenizer.keyword())) {
            return;
        }

        compileStatementsNested();
    }

    private void compileStatement() {
        switch (tokenizer.keyword()) {
            case LET -> compileLet();
            case IF -> compileIf();
            case WHILE -> compileWhile();
            case DO -> compileDo();
            case RETURN -> compileReturn();
            default -> throw new IllegalStateException("Unsupported type " + tokenizer.keyword());
        }
    }

    public void compileLet() {
        openBlock(StatementType.LET_STATEMENT);
        writeToken();

        advance();
        writeToken();

        advance();
        final boolean array = tokenizer.symbol() == '[';
        if (array) {
            writeToken();
            advance();
            compileExpression();

            advance();
            writeToken();

            // =
            advance();
        }

        // =
        writeToken();

        advance();
        compileExpression();

        advance();
        writeToken();
        closeBlock(StatementType.LET_STATEMENT);
    }

    public void compileIf() {
        openBlock(StatementType.IF_STATEMENT);
        writeToken();

        advance();
        compileConditionalStatements();
        if (!tokenizer.hasMoreTokens()) {
            return;
        }

        advance();
        if (tokenizer.tokenType() != TokenType.KEYWORD || tokenizer.keyword() != Keyword.ELSE) {
            return;
        }

        writeToken();

        advance();
        writeToken();

        advance();
        compileStatements();

        advance();
        writeToken();
        closeBlock(StatementType.IF_STATEMENT);
    }

    public void compileWhile() {
        openBlock(StatementType.WHILE_STATEMENT);
        writeToken();
        advance();

        compileConditionalStatements();
        closeBlock(StatementType.WHILE_STATEMENT);
    }

    private void compileConditionalStatements() {
        writeToken();

        advance();
        compileExpression();

        advance();
        writeToken();

        // {
        advance();
        writeToken();

        advance();
        compileStatements();

        advance();

        writeToken();
    }

    public void compileDo() {
        openBlock(StatementType.DO_STATEMENT);
        writeToken();

        advance();
        compileSubroutineCall();

        advance();
        writeToken();
        closeBlock(StatementType.DO_STATEMENT);
    }

    public void compileReturn() {
        openBlock(StatementType.RETURN_STATEMENT);
        writeToken();

        advance();
        if (tokenizer.tokenType() == TokenType.SYMBOL && tokenizer.symbol() == ';') {
            writeToken();
            closeBlock(StatementType.RETURN_STATEMENT);
            return;
        }

        compileExpression();
        advance();
        writeToken();
        closeBlock(StatementType.RETURN_STATEMENT);
    }

    public void compileExpression() {
        openBlock(StatementType.EXPRESSION);
        compileTerm();
        if (!tokenizer.hasMoreTokens()) {
            closeBlock(StatementType.EXPRESSION);
            return;
        }

        advance();
        if (tokenizer.tokenType() == TokenType.SYMBOL && isOp(tokenizer.symbol())) {
            writeToken();
            advance();
            compileTerm();
        }
        closeBlock(StatementType.EXPRESSION);
    }

    public void compileTerm() {
        openBlock(StatementType.TERM);
        final TokenType tokenType = tokenizer.tokenType();
        switch (tokenType) {
            case IDENTIFIER -> compileTermIdentifier();
            case SYMBOL -> compileTermSymbol();
            default -> writeToken();
        }
        closeBlock(StatementType.TERM);
    }

    private void compileTermSymbol() {
        if (tokenizer.symbol() != '(') {
            return;
        }

        writeToken();
        final boolean isOp = isUnaryOp(tokenizer.symbol());
        if (!isOp) {
            advance();
            compileExpression();

            advance();
            writeToken();
            return;
        }

        writeToken();
        advance();

        compileTerm();
    }

    private static boolean isUnaryOp(char op) {
        return op == '-' || op == '~';
    }

    private static boolean isOp(char op) {
        return op == '+'
                || op == '-'
                || op == '*'
                || op == '/'
                || op == '&'
                || op == '|'
                || op == '<'
                || op == '>'
                || op == '=';
    }

    private void compileTermIdentifier() {
        writeToken();

        //just identifier
        advance();
        if (tokenizer.tokenType() != TokenType.SYMBOL) {
            return;
        }

        //array, expression
        final boolean isArray = tokenizer.symbol() == '[';
        if (tokenizer.symbol() == '(' || isArray) {
            writeToken();

            advance();
            compileExpression();

            advance();
            writeToken();

            if (isArray) {
                return;
            }

            // .
            advance();
            writeToken();
        } else if (tokenizer.symbol() == '.') {
            // subroutineCall
            writeToken();
        } else {
            return;
        }

        advance();
        compileSubroutineCallAfterDot();
    }

    private void compileSubroutineCall() {
        writeToken();

        advance();
        if (tokenizer.symbol() == '(') {
            advance();
            compileExpressionList();

            advance();
            writeToken();

            advance();
        }

        //dot
        writeToken();

        advance();
        compileSubroutineCallAfterDot();
    }

    private void compileSubroutineCallAfterDot() {
        writeToken();

        advance();
        writeToken();

        advance();
        compileExpressionList();

        advance();
        writeToken();
    }

    public void compileExpressionList() {
        openBlock(StatementType.EXPRESSION_LIST);
        compileExpressionListNested();
        closeBlock(StatementType.EXPRESSION_LIST);
    }

    private void compileExpressionListNested() {
        if (!tokenizer.hasMoreTokens()) {
            return;
        }

        if (tokenizer.tokenType() == TokenType.SYMBOL && tokenizer.symbol() == ')') {
            return;
        }

        compileExpression();

        advance();
        if (tokenizer.tokenType() == TokenType.SYMBOL && tokenizer.symbol() == ',') {
            writeToken();
            advance();
            compileExpressionListNested();
        }
    }

    private void advance() {
        if (tokenIsBuffered) {
            return;
        }

        tokenizer.advance();
        tokenIsBuffered = true;
    }

    private void writeToken() {
        try {
            switch (tokenizer.tokenType()) {
                case KEYWORD -> bufferedWriter.write(wrapKeyword(tokenizer.keyword()));
                case IDENTIFIER -> bufferedWriter.write(wrapIdentifier(tokenizer.identifier()));
                case SYMBOL -> bufferedWriter.write(wrapSymbol(tokenizer.symbol()));
                case INT_CONST -> bufferedWriter.write(wrapIntConst(tokenizer.intVal()));
                case STRING_CONST -> bufferedWriter.write(wrapStringConst(tokenizer.stringVal()));
                default -> throw new IllegalStateException("Unsupported token type: " + tokenizer.tokenType());
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        tokenIsBuffered = false;
    }

    private static boolean isStatement(Keyword keyword) {
        return keyword == Keyword.LET
                || keyword == Keyword.IF
                || keyword == Keyword.WHILE
                || keyword == Keyword.DO
                || keyword == Keyword.RETURN;
    }

    private void openBlock(StatementType statementType) {
        try {
            bufferedWriter.write("%s<%s>\n".formatted(TAB_SYMBOL.repeat(nestingLevel++), statementType.tagName));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void closeBlock(StatementType statementType) {
        try {
            bufferedWriter.write("%s</%s>\n".formatted(TAB_SYMBOL.repeat(--nestingLevel >= 0 ? nestingLevel : 0), statementType.tagName));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private String wrapKeyword(Keyword keyword) {
        return "%s<keyword> %s </keyword>\n".formatted(TAB_SYMBOL.repeat(nestingLevel), keyword.name().toLowerCase());
    }

    private String wrapIdentifier(String identifier) {
        return "%s<identifier> %s </identifier>\n".formatted(TAB_SYMBOL.repeat(nestingLevel), identifier);
    }

    private String wrapSymbol(char symbol) {
        return "%s<symbol> %s </symbol>\n".formatted(TAB_SYMBOL.repeat(nestingLevel), symbol);
    }

    private String wrapIntConst(short intConst) {
        return "%s<integerConstant> %d </integerConstant>\n".formatted(TAB_SYMBOL.repeat(nestingLevel), intConst);
    }

    private String wrapStringConst(String stringConst) {
        return "%s<stringConstant> %s </stringConstant>\n".formatted(TAB_SYMBOL.repeat(nestingLevel), stringConst);
    }

    @Override
    public void close() throws IOException {
        this.tokenizer.close();
        this.bufferedWriter.close();
    }

    enum StatementType {
        CLASS("class"),
        SUBROUTINE_DEC("subroutineDec"),
        SUBROUTINE_BODY("subroutineBody"),
        PARAMETER_LIST("parameterList"),
        VAR_DEC("varDec"),
        STATEMENTS("statements"),
        LET_STATEMENT("letStatement"),
        IF_STATEMENT("ifStatement"),
        WHILE_STATEMENT("whileStatement"),
        DO_STATEMENT("doStatement"),
        RETURN_STATEMENT("returnStatement"),
        EXPRESSION("expression"),
        EXPRESSION_LIST("expressionList"),
        STRING_CONSTANT("stringConstant"),
        INTEGER_CONSTANT("integerConstant"),
        TERM("term");

        public String tagName;

        StatementType(String tagName) {
            this.tagName = tagName;
        }
    }
}

