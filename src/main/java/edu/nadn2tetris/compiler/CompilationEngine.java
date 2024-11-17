package edu.nadn2tetris.compiler;

import java.io.BufferedWriter;
import java.io.Closeable;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.HashSet;
import java.util.Set;
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
    private final StringBuilder xml = new StringBuilder();

    private final Stack<StatementType> statementTypeNesting = new Stack<>();
    private final SymbolTable classSymbolTable = new SymbolTable();
    private final SymbolTable procedureSymbolTable = new SymbolTable();

    private Kind declarationKind = null;
    private String declarationType = null;

    private boolean tokenIsBuffered = false;
    private int nestingLevel = 0;

    private final Set<Flag> flags;

    public CompilationEngine(JackTokenizer tokenizer, OutputStream out, Set<Flag> flags) {
        this.tokenizer = tokenizer;
        this.bufferedWriter = new BufferedWriter(new OutputStreamWriter(out));
        this.flags = flags == null ? new HashSet<>() : flags;
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

        declarationType = getType(tokenizer);
        handleToken();

        advance();

        declarationKind = tokenizer.keyword() == Keyword.STATIC ? Kind.STATIC : Kind.FIELD;
        handleToken();

        if (!tokenizer.hasMoreTokens()) {
            declarationType = null;
            declarationKind = null;
            closeBlock(StatementType.CLASS_VAR_DEC);
            return;
        }

        advance();
        if (tokenizer.tokenType() != TokenType.SYMBOL || tokenizer.symbol() != ',') {
            handleToken();
            declarationType = null;
            declarationKind = null;
            closeBlock(StatementType.CLASS_VAR_DEC);
            return;
        }

        //,
        handleToken();

        advance();
        compileVarDec(true);

        declarationType = null;
        declarationKind = null;
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
        boolean declareVariables = statementTypeNesting.peek() == StatementType.SUBROUTINE_DEC;
        openBlock(StatementType.PARAMETER_LIST);
        compileParameterListNested(declareVariables);
        closeBlock(StatementType.PARAMETER_LIST);
    }

    private void compileParameterListNested() {
        compileParameterListNested(false);
    }

    private void compileParameterListNested(boolean declareVariables) {
        if (!isType(tokenizer)) {
            return;
        }

        if (declareVariables) {
            declarationKind = Kind.ARG;
            declarationType = getType(tokenizer);
        }

        handleToken();

        advance();
        handleToken();

        advance();
        if (tokenizer.tokenType() != TokenType.SYMBOL || tokenizer.symbol() != ',') {
            if (declareVariables) {
                declarationKind = null;
                declarationType = null;
            }
            return;
        }

        //,
        handleToken();

        advance();
        compileParameterListNested(declareVariables);

        if (declareVariables) {
            declarationKind = null;
            declarationType = null;
        }
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
            declarationKind = Kind.VAR;
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
            declarationKind = null;
            declarationType = null;
            return;
        }

        handleToken(); //,

        advance();
        compileVarDec(true);
        if (!list) {
            declarationKind = null;
            declarationType = null;
        }
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
        switch (tokenizer.tokenType()) {
            case KEYWORD -> xml.append(wrapKeyword(tokenizer.keyword()));
            case IDENTIFIER -> handleIdentifier();
            case SYMBOL -> xml.append(wrapSymbol(tokenizer.symbol()));
            case INT_CONST -> xml.append(wrapIntConst(tokenizer.intVal()));
            case STRING_CONST -> xml.append(wrapStringConst(tokenizer.stringVal()));
            default -> throw new IllegalStateException("Unsupported token type: " + tokenizer.tokenType());
        }

        tokenIsBuffered = false;
    }

    private void handleIdentifier() {
        final String identifier = tokenizer.identifier();

        String identifierXml = "%s<identifier> %s </identifier>\n".formatted(TAB_SYMBOL.repeat(nestingLevel), identifier);
        if (!flags.contains(Flag.EXTENDED_IDENTIFIER)) {
            xml.append(identifierXml);
            return;
        }

        final boolean declaration = declarationKind != null && declarationType != null;
        if (declaration) {
            defineIdentifier(identifier);
        }

        final IdentifierInfo identifierInfo = getIdentifierInfo(identifier);
        if (identifierInfo == null) {
            xml.append(identifierXml);
            return;
        }

        identifierXml = "%s<identifier category=\"%s\" index=\"%d\" declaration=\"%s\"> %s </identifier>\n".formatted(
                TAB_SYMBOL.repeat(nestingLevel),
                declaration
                        ? identifierInfo.kind.name().toLowerCase()
                        : identifierInfo.kind == Kind.STATIC || identifierInfo.kind == Kind.FIELD
                        ? Kind.CLASS.name().toLowerCase()
                        : Kind.SUBROUTINE.name().toLowerCase(),
                indexOf(identifier),
                declaration,
                identifier
        );

        xml.append(identifierXml);
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
        xml.append("%s<%s>\n".formatted(TAB_SYMBOL.repeat(nestingLevel++), statementType.tagName));
        statementTypeNesting.add(statementType);
    }

    private void closeBlock(StatementType statementType) {
        if (statementType == StatementType.SUBROUTINE_DEC || statementType == StatementType.CLASS) {
            procedureSymbolTable.reset();
        }

        statementTypeNesting.pop();
        xml.append("%s</%s>\n".formatted(TAB_SYMBOL.repeat(--nestingLevel >= 0 ? nestingLevel : 0), statementType.tagName));
    }

    private String wrapKeyword(Keyword keyword) {
        return "%s<keyword> %s </keyword>\n".formatted(TAB_SYMBOL.repeat(nestingLevel), keyword.name().toLowerCase());
    }

    private short indexOf(String name) {
        IdentifierInfo identifierInfo = procedureSymbolTable.getIdentifierInfo(name);
        if (identifierInfo != null) {
            return procedureSymbolTable.indexOf(name);
        }

        identifierInfo = classSymbolTable.getIdentifierInfo(name);
        if (identifierInfo == null) {
            throw new IllegalStateException("Undefined identifier: " + name);
        }

        return classSymbolTable.indexOf(name);
    }

    private void defineIdentifier(String name) {
        if (declarationKind == null || declarationType == null) {
            throw new IllegalStateException("Invalid declaration");
        }

        final SymbolTable table = declarationKind == Kind.FIELD || declarationKind == Kind.STATIC
                ? classSymbolTable
                : procedureSymbolTable;

        table.define(name, declarationType, declarationKind);
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

    @Override
    public void close() throws IOException {
        this.tokenizer.close();
        this.bufferedWriter.append(xml).close();
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

