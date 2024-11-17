package edu.nadn2tetris.compiler;

import java.io.BufferedWriter;
import java.io.Closeable;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.Stack;

import edu.nadn2tetris.common.Keyword;
import edu.nadn2tetris.common.TokenType;
import edu.nadn2tetris.table.IdentifierInfo;
import edu.nadn2tetris.table.Kind;
import edu.nadn2tetris.table.SymbolTable;
import edu.nadn2tetris.tokenizer.JackTokenizer;

public final class CompilationEngine implements Closeable {
    private static final String TAB_SYMBOL = "\s\s";

    private final JackTokenizer tokenizer;
    private final BufferedWriter bufferedWriter;

    private final Stack<StatementType> statementTypeNesting = new Stack<>();
    private final SymbolTable classSymbolTable = new SymbolTable();
    private final SymbolTable procedureSymbolTable = new SymbolTable();

    private Kind declarationKind = null;
    private String declarationType = null;

    private boolean extendedIdentifierInfo = true;

    private boolean tokenIsBuffered = false;
    private int nestingLevel = 0;

    public CompilationEngine(JackTokenizer tokenizer, OutputStream out) {
        this.tokenizer = tokenizer;
        this.bufferedWriter = new BufferedWriter(new OutputStreamWriter(out));
    }

    public void compileClass() {
        advance();
        openBlock(StatementType.CLASS);

        handleToken();
        advance();
        handleToken();

        // {
        advance();
        handleToken();

        advance();
        if (tokenizer.tokenType() == TokenType.SYMBOL && tokenizer.symbol() == '}') {
            handleToken();
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
        handleToken();
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
        openBlock(StatementType.CLASS_VAR_DEC);
        handleToken();

        advance();
        handleToken();
        declarationType = getType(tokenizer);

        advance();
        handleToken();

        if (!tokenizer.hasMoreTokens()) {
            closeBlock(StatementType.CLASS_VAR_DEC);
            return;
        }

        advance();
        if (tokenizer.tokenType() != TokenType.SYMBOL || tokenizer.symbol() != ',') {
            handleToken();
            closeBlock(StatementType.CLASS_VAR_DEC);
            return;
        }

        //,
        handleToken();

        advance();
        compileVarDec(true);
        closeBlock(StatementType.CLASS_VAR_DEC);
    }

    public void compileSubroutine() {
        openBlock(StatementType.SUBROUTINE_DEC);
        handleToken();

        advance();
        handleToken();

        advance();
        handleToken();

        //(
        advance();
        handleToken();

        advance();
        compileParameterList();

        //)
        advance();
        handleToken();

        advance();
        compileSubroutineBody();
        closeBlock(StatementType.SUBROUTINE_DEC);
    }

    public void compileSubroutineBody() {
        openBlock(StatementType.SUBROUTINE_BODY);
        //{
        handleToken();

        advance();
        while (tokenizer.tokenType() == TokenType.KEYWORD && tokenizer.keyword() == Keyword.VAR) {
            compileVarDec();
            advance();
        }

        compileStatements();

        //}
        advance();
        handleToken();
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

        declarationType = getType(tokenizer);
        handleToken();

        advance();
        handleToken();

        advance();
        if (tokenizer.tokenType() != TokenType.SYMBOL || tokenizer.symbol() != ',') {
            return;
        }

        //,
        handleToken();

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

    private static String getType(JackTokenizer tokenizer) {
        if (tokenizer.tokenType() == TokenType.KEYWORD) {
            return tokenizer.keyword().name().toLowerCase();
        }

        return tokenizer.identifier();
    }

    public void compileVarDec() {
        openBlock(StatementType.VAR_DEC);
        compileVarDec(false);
        closeBlock(StatementType.VAR_DEC);
    }

    private void compileVarDec(boolean list) {
        if (!list) {
            handleToken();

            advance();
            handleToken();
            declarationType = getType(tokenizer);

            advance();
        }

        handleToken();

        advance();
        if (tokenizer.symbol() == ';') {
            handleToken();
            return;
        }

        handleToken();

        advance();
        compileVarDec(true);
    }

    public void compileStatements() {
        openBlock(StatementType.STATEMENTS);
        compileStatementsNested();
        closeBlock(StatementType.STATEMENTS);
    }

    private void compileStatementsNested() {
        if (isNotStatement()) {
            return;
        }

        compileStatement();

        advance();
        if (tokenizer.tokenType() != TokenType.KEYWORD || isNotStatement()) {
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
        handleToken();

        advance();
        handleToken();

        advance();
        final boolean array = tokenizer.symbol() == '[';
        if (array) {
            handleToken();
            advance();
            compileExpression();

            advance();
            handleToken();

            // =
            advance();
        }

        // =
        handleToken();

        advance();
        compileExpression();

        advance();
        handleToken();
        closeBlock(StatementType.LET_STATEMENT);
    }

    public void compileIf() {
        openBlock(StatementType.IF_STATEMENT);
        handleToken();

        advance();
        compileConditionalStatements();
        if (!tokenizer.hasMoreTokens()) {
            closeBlock(StatementType.IF_STATEMENT);
            return;
        }

        advance();
        if (tokenizer.tokenType() != TokenType.KEYWORD || tokenizer.keyword() != Keyword.ELSE) {
            closeBlock(StatementType.IF_STATEMENT);
            return;
        }

        handleToken();

        advance();
        handleToken();

        advance();
        compileStatements();

        advance();
        handleToken();
        closeBlock(StatementType.IF_STATEMENT);
    }

    public void compileWhile() {
        openBlock(StatementType.WHILE_STATEMENT);
        handleToken();
        advance();

        compileConditionalStatements();
        closeBlock(StatementType.WHILE_STATEMENT);
    }

    private void compileConditionalStatements() {
        handleToken();

        advance();
        compileExpression();

        advance();
        handleToken();

        // {
        advance();
        handleToken();

        advance();
        compileStatements();

        advance();

        handleToken();
    }

    public void compileDo() {
        openBlock(StatementType.DO_STATEMENT);
        handleToken();

        advance();
        compileSubroutineCall();
        closeBlock(StatementType.DO_STATEMENT);
    }

    public void compileReturn() {
        openBlock(StatementType.RETURN_STATEMENT);
        handleToken();

        advance();
        if (tokenizer.tokenType() == TokenType.SYMBOL && tokenizer.symbol() == ';') {
            handleToken();
            closeBlock(StatementType.RETURN_STATEMENT);
            return;
        }

        compileExpression();
        advance();
        handleToken();
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
            handleToken();
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
            default -> handleToken();
        }
        closeBlock(StatementType.TERM);
    }

    private void compileTermSymbol() {
        // unaryOp expression
        if (isUnaryOp(tokenizer.symbol())) {
            handleToken();

            advance();
            compileTerm();
            return;
        }

        // (expression)
        handleToken();

        advance();
        compileExpression();

        advance();
        handleToken();
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
        handleToken(); // identifier

        advance();
        final boolean varName = tokenizer.tokenType() != TokenType.SYMBOL;
        if (varName) {
            return;
        }

        boolean arrayOrExpressionInBrackets = tokenizer.symbol() == '[' || tokenizer.symbol() == '(';
        if (arrayOrExpressionInBrackets) {
            handleToken();

            advance();
            compileExpression();

            advance();
            handleToken();
        }

        advance();
        final boolean subroutineCall = tokenizer.tokenType() == TokenType.SYMBOL && tokenizer.symbol() == '.';
        if (!subroutineCall) {
            return;
        }

        handleToken();

        advance();
        compileSubroutineCallAfterDot(false);
    }

    private void compileSubroutineCall() {
        handleToken();

        advance();
        if (tokenizer.symbol() == '(') {
            handleToken();

            advance();
            compileExpressionList();

            advance();
            handleToken();
        }

        //dot or ;
        advance();
        handleToken();
        if (tokenizer.symbol() == ';') {
            return;
        }

        advance();
        compileSubroutineCallAfterDot();
    }

    private void compileSubroutineCallAfterDot(boolean writeLineEnd) {
        handleToken();

        advance();
        handleToken();

        advance();
        compileExpressionList();

        // )
        advance();
        handleToken();

        if (writeLineEnd) {
            advance();
            handleToken();
        }
    }

    private void compileSubroutineCallAfterDot() {
        compileSubroutineCallAfterDot(true);
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
            handleToken();
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

    private void handleToken() {
        try {
            switch (tokenizer.tokenType()) {
                case KEYWORD -> bufferedWriter.write(wrapKeyword(tokenizer.keyword()));
                case IDENTIFIER -> handleIdentifier();
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

    private void handleIdentifier() throws IOException {
        String identifier = wrapIdentifier(tokenizer.identifier());

        bufferedWriter.write(identifier);
    }

    private boolean isNotStatement() {
        if (tokenizer.tokenType() != TokenType.KEYWORD) {
            return true;
        }

        final Keyword keyword = tokenizer.keyword();
        return keyword != Keyword.LET
                && keyword != Keyword.IF
                && keyword != Keyword.WHILE
                && keyword != Keyword.DO
                && keyword != Keyword.RETURN;
    }

    private void openBlock(StatementType statementType) {
        try {
            if (statementType == StatementType.CLASS_VAR_DEC) {
                declarationKind = tokenizer.keyword() == Keyword.STATIC ? Kind.STATIC : Kind.FIELD;
            } else if (statementType == StatementType.VAR_DEC) {
                declarationKind = Kind.VAR;
            } else if (statementType == StatementType.PARAMETER_LIST && statementTypeNesting.peek() == StatementType.SUBROUTINE_DEC) {
                declarationKind = Kind.ARG;
            }

            bufferedWriter.write("%s<%s>\n".formatted(TAB_SYMBOL.repeat(nestingLevel++), statementType.tagName));
            statementTypeNesting.add(statementType);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void closeBlock(StatementType statementType) {
        try {
            bufferedWriter.write("%s</%s>\n".formatted(TAB_SYMBOL.repeat(--nestingLevel >= 0 ? nestingLevel : 0), statementType.tagName));

            if (statementType == StatementType.SUBROUTINE_DEC || statementType == StatementType.CLASS) {
                procedureSymbolTable.reset();
            }

            declarationKind = null;
            statementTypeNesting.pop();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private String wrapKeyword(Keyword keyword) {
        return "%s<keyword> %s </keyword>\n".formatted(TAB_SYMBOL.repeat(nestingLevel), keyword.name().toLowerCase());
    }

    //TODO: Отсюда нунжно вынести
    //      1) Метод имеет побочки
    //      2) Задевает кейсы, которые не нужно задевать, например объявление с объектным типом
    private String wrapIdentifier(String identifier) {
        if (!extendedIdentifierInfo) {
            return "%s<identifier> %s </identifier>\n".formatted(TAB_SYMBOL.repeat(nestingLevel), identifier);
        }

        final boolean declaration = declarationKind != null && declarationType != null;
        SymbolTable table = declarationKind == Kind.FIELD || declarationKind == Kind.STATIC
                ? classSymbolTable
                : procedureSymbolTable;

        if (declaration) {
            table.define(identifier, declarationType, declarationKind);
        }

        final IdentifierInfo identifierInfo = getIdentifierInfo(identifier);
        if (identifierInfo == null) {
            return "%s<identifier> %s </identifier>\n".formatted(TAB_SYMBOL.repeat(nestingLevel), identifier);
        }

        table = identifierInfo.kind == Kind.FIELD || identifierInfo.kind == Kind.STATIC
                ? classSymbolTable
                : procedureSymbolTable;

        final String res = "%s<identifier category=\"%s\" index=\"%d\" declaration=\"%s\"> %s </identifier>\n".formatted(
                TAB_SYMBOL.repeat(nestingLevel),
                declaration
                        ? identifierInfo.kind.name().toLowerCase()
                        : identifierInfo.kind == Kind.STATIC || identifierInfo.kind == Kind.FIELD
                        ? Kind.CLASS.name().toLowerCase()
                        : Kind.SUBROUTINE.name().toLowerCase(),
                table.indexOf(identifier),
                declaration,
                identifier
        );

        // записали, занулили
        declarationType = null;
        declarationKind = null;

        return res;
    }

    private IdentifierInfo getIdentifierInfo(String name) {
        IdentifierInfo identifier = procedureSymbolTable.getIdentifierInfo(name);
        if (identifier == null) {
            identifier = classSymbolTable.getIdentifierInfo(name);
        }

        return identifier;
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

    public void setExtendedIdentifierInfo(boolean extended) {
        this.extendedIdentifierInfo = extended;
    }

    @Override
    public void close() throws IOException {
        this.tokenizer.close();
        this.bufferedWriter.close();
    }

    enum StatementType {
        CLASS("class"),
        CLASS_VAR_DEC("classVarDec"),
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

        public final String tagName;

        StatementType(String tagName) {
            this.tagName = tagName;
        }
    }
}

