package edu.nadn2tetris.compiler;

import java.io.BufferedWriter;
import java.io.Closeable;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;

import edu.nadn2tetris.common.Keyword;
import edu.nadn2tetris.common.TokenType;
import edu.nadn2tetris.tokenizer.JackTokenizer;

// странцица 310 10.1.3 Синтаксический раздел
// Читаем первый символ от Tokenizer(a), решаем что за конструкция перед нами - рендерим конструкцию.
//TODO: 1) Инкапсулировать xml запсь(чтобы не дублировать wrap) +
//      2) Писать сразу в буффер или в один XML и переиспользовать публичные методы. +
//      3) Рекурсия(?) - пох
//      4) <class> <subroutineDec> <parameterList> <subroutineBody> <varDec> <statements>  <letStatement> <expression> <term> <expressionList>...
//      5) подумать над читабельностью

public final class CompilationEngine implements Closeable {
    private static final String TAB_SYMBOL = "\t";
    private final JackTokenizer tokenizer;
    private final BufferedWriter bufferedWriter;

    private boolean nextIsBuffered = false;
    private int nestingLevel = 0;

    public CompilationEngine(JackTokenizer tokenizer, OutputStream out) {
        this.tokenizer = tokenizer;
        this.bufferedWriter = new BufferedWriter(new OutputStreamWriter(out));
    }

    public void compileClass() {
        write();
        advance();
        write();

        // {
        advance();
        write();
        nestingLevel++;

        advance();
        if (tokenizer.tokenType() == TokenType.SYMBOL && tokenizer.symbol() == '}') {
            nestingLevel--;
            write();
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

        nestingLevel--;
        // }
        write();
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
        write();

        advance();
        write();

        advance();
        write();

        if (!tokenizer.hasMoreTokens()) {
            return;
        }

        advance();
        if (tokenizer.tokenType() != TokenType.SYMBOL || tokenizer.symbol() == ',') {
            return;
        }

        //,
        write();

        advance();
        compileVarDec();
    }

    public void compileSubroutine() {
        write();

        advance();
        write();

        advance();
        write();

        //(
        advance();
        write();

        advance();
        compileParameterList();

        //)
        advance();
        write();

        advance();
        compileSubroutineBody();
    }

    public void compileSubroutineBody() {
        //{
        write();
        nestingLevel++;

        advance();
        while (tokenizer.tokenType() == TokenType.KEYWORD && tokenizer.keyword() == Keyword.VAR) {
            compileVarDec();
            advance();
        }

        compileStatements();

        //}
        nestingLevel--;
        advance();
        write();
    }

    public void compileParameterList() {
        if (!isType(tokenizer)) {
            return;
        }

        write();

        advance();
        if (tokenizer.tokenType() != TokenType.SYMBOL || tokenizer.symbol() != ',') {
            return;
        }

        //,
        write();

        advance();
        compileParameterList();
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
        compileVarDec(false);
    }

    private void compileVarDec(boolean list) {
        if (!list) {
            write();

            advance();
            write();

            advance();
        }

        write();

        advance();
        if (tokenizer.symbol() == ';') {
            write();
            return;
        }

        write();

        advance();
        compileVarDec(true);
    }

    public void compileStatements() {
        if (!isStatement(tokenizer.keyword())) {
            return;
        }

        compileStatement();

        advance();
        if (tokenizer.tokenType() != TokenType.KEYWORD || !isStatement(tokenizer.keyword())) {
            return;
        }

        compileStatements();
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
        write();

        advance();
        write();

        advance();
        final boolean array = tokenizer.symbol() == '[';
        if (array) {
            write();
            advance();
            compileExpression();

            advance();
            write();

            // =
            advance();
        }

        // =
        write();

        advance();
        compileExpression();

        advance();
        write();
    }

    public void compileIf() {
        write();

        advance();
        compileConditionalStatements();
        if (!tokenizer.hasMoreTokens()) {
            return;
        }

        advance();
        if (tokenizer.tokenType() != TokenType.KEYWORD || tokenizer.keyword() != Keyword.ELSE) {
            return;
        }

        write();

        advance();
        write();
        nestingLevel++;

        advance();
        compileStatements();

        nestingLevel--;
        advance();

        write();
    }

    public void compileWhile() {
        write();
        advance();

        compileConditionalStatements();
    }

    private void compileConditionalStatements() {
        write();

        advance();
        compileExpression();

        advance();
        write();

        // {
        advance();
        write();
        nestingLevel++;

        advance();
        compileStatements();

        nestingLevel--;
        advance();

        write();
    }

    public void compileDo() {
        write();

        advance();
        compileSubroutineCall();

        advance();
        write();
    }

    public void compileReturn() {
        write();

        advance();
        if (tokenizer.tokenType() == TokenType.SYMBOL && tokenizer.symbol() == ';') {
            write();
            return;
        }

        compileExpression();
        advance();
        write();
    }

    public void compileExpression() {
        compileTerm();
        if (!tokenizer.hasMoreTokens()) {
            return;
        }

        advance();
        if (tokenizer.tokenType() == TokenType.SYMBOL && isOp(tokenizer.symbol())) {
            write();
            advance();
            compileTerm();
        }
    }

    public void compileTerm() {
        final TokenType tokenType = tokenizer.tokenType();
        switch (tokenType) {
            case IDENTIFIER -> compileTermIdentifier();
            case SYMBOL -> compileTermSymbol();
            default -> write();
        }
    }

    private void compileTermSymbol() {
        if (tokenizer.symbol() != '(') {
            return;
        }

        write();
        final boolean isOp = isUnaryOp(tokenizer.symbol());
        if (!isOp) {
            advance();
            compileExpression();

            advance();
            write();
            return;
        }

        write();
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
        write();

        //just identifier
        advance();
        if (tokenizer.tokenType() != TokenType.SYMBOL) {
            return;
        }

        //array, expression
        final boolean isArray = tokenizer.symbol() == '[';
        if (tokenizer.symbol() == '(' || isArray) {
            write();

            advance();
            compileExpressionList();

            advance();
            write();

            if (isArray) {
                return;
            }

            // .
            advance();
            write();
        } else if (tokenizer.symbol() == '.') {
            // subroutineCall
            write();
        } else {
            return;
        }

        advance();
        compileSubroutineCallAfterDot();
    }

    private void compileSubroutineCall() {
        write();

        advance();
        if (tokenizer.symbol() == '(') {
            advance();
            compileExpressionList();

            advance();
            write();

            advance();
        }

        //dot
        write();

        advance();
        compileSubroutineCallAfterDot();
    }

    //...'.'
    private void compileSubroutineCallAfterDot() {
        write();

        advance();
        write();

        advance();
        compileExpressionList();

        advance();
        write();
    }

    public void compileExpressionList() {
        compileExpression();
        if (!tokenizer.hasMoreTokens()) {
            return;
        }

        advance();
        if (tokenizer.tokenType() == TokenType.SYMBOL && tokenizer.symbol() == ',') {
            write();
            advance();
            compileExpressionList();
        }
    }

    private void advance() {
        if (nextIsBuffered) {
            return;
        }

        tokenizer.advance();
        nextIsBuffered = true;
    }

    private void write() {
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

        nextIsBuffered = false;
    }

    private static boolean isStatement(Keyword keyword) {
        return keyword == Keyword.LET
                || keyword == Keyword.IF
                || keyword == Keyword.WHILE
                || keyword == Keyword.DO
                || keyword == Keyword.RETURN;
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
        return "%s<intConst> %d </intConst>\n".formatted(TAB_SYMBOL.repeat(nestingLevel), intConst);
    }

    private String wrapStringConst(String stringConst) {
        return "%s<stringConst> %s </stringConst>\n".formatted(TAB_SYMBOL.repeat(nestingLevel), stringConst);
    }

    @Override
    public void close() throws IOException {
        this.tokenizer.close();
        this.bufferedWriter.close();
    }
}

