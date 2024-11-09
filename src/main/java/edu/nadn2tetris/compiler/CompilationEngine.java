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
//      2) Писать сразу в буффер
//      3) Рекурсия(?)
//      4) <class> <subroutineDec> <parameterList> <subroutineBody> <varDec> <statements>  <letStatement> <expression> <term> <expressionList>...
public final class CompilationEngine implements Closeable {
    private static final String TAB = "\t";
    private final JackTokenizer tokenizer;
    private final BufferedWriter bufferedWriter;
    private final StringBuilder xml = new StringBuilder();

    // не нужно дегать advance, проверяем текущий снаружи
    private boolean nextChecked = false;
    private int level = 0;

    public CompilationEngine(JackTokenizer tokenizer, OutputStream outputStream) {
        this.tokenizer = tokenizer;
        this.bufferedWriter = new BufferedWriter(new OutputStreamWriter(outputStream));
    }

    public void compileClass() {
        xml.append(
                writeClass(new StringBuilder())
        );
    }

    private StringBuilder writeClass(StringBuilder classXml) {
        append(classXml);

        advance();
        append(classXml);

        // {
        advance();
        append(classXml);
        level++;

        advance();
        if (tokenizer.tokenType() == TokenType.SYMBOL && tokenizer.symbol() == '}') {
            level--;
            return append(classXml);
        }

        while (tokenizer.tokenType() == TokenType.KEYWORD && isClassVarDec(tokenizer.keyword())) {
            writeClassVarDec(classXml);
            advance();
        }

        while (tokenizer.tokenType() == TokenType.KEYWORD && isSubroutineDec(tokenizer.keyword())) {
            writeSubroutineDec(classXml);
            advance();
        }

        level--;
        // }
        append(classXml);

        return classXml;
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
        xml.append(
                writeClassVarDec(
                        new StringBuilder()
                )
        );
    }

    private StringBuilder writeClassVarDec(StringBuilder varDexXml) {
        append(varDexXml);

        advance();
        append(varDexXml);

        advance();
        append(varDexXml);

        if (!tokenizer.hasMoreTokens()) {
            return varDexXml;
        }

        advance();
        if (tokenizer.tokenType() != TokenType.SYMBOL || tokenizer.symbol() == ',') {
            nextChecked = true;
            return varDexXml;
        }

        //,
        append(varDexXml);

        advance();
        return writeVarDec(varDexXml);
    }

    public void compileSubroutine() {
        xml.append(
                writeSubroutineDec(new StringBuilder())
        );
    }

    private StringBuilder writeSubroutineDec(StringBuilder subroutineDecXml) {
        append(subroutineDecXml);

        advance();
        if (isType(tokenizer)) {
            writeType(subroutineDecXml);
        } else {
            append(subroutineDecXml);
        }

        advance();
        append(subroutineDecXml);

        //(
        advance();
        append(subroutineDecXml);

        advance();
        writeParameterList(subroutineDecXml);

        //)
        advance();
        append(subroutineDecXml);

        advance();
        return writeSubroutineBody(subroutineDecXml);
    }

    public void compileSubroutineBody() {
        xml.append(
                writeSubroutineBody(new StringBuilder())
        );
    }

    private StringBuilder writeSubroutineBody(StringBuilder subroutineXml) {
        //{
        append(subroutineXml);
        level++;

        advance();
        while (tokenizer.tokenType() == TokenType.KEYWORD && tokenizer.keyword() == Keyword.VAR) {
            writeVarDec(subroutineXml);
            advance();
        }

        writeStatements(subroutineXml);

        //}
        level--;
        advance();
        append(subroutineXml);

        return subroutineXml;
    }

    public void compileParameterList() {
        xml.append(
                writeParameterList(new StringBuilder())
        );
    }

    private StringBuilder writeParameterList(StringBuilder parameterListXml) {
        if (!isType(tokenizer)) {
            nextChecked = true;
            return parameterListXml;
        }

        writeType(parameterListXml);

        advance();
        if (tokenizer.tokenType() != TokenType.SYMBOL || tokenizer.symbol() != ',') {
            nextChecked = true;
            return parameterListXml;
        }

        //,
        append(parameterListXml);

        advance();
        return writeParameterList(parameterListXml);
    }

    private StringBuilder writeType(StringBuilder type) {
        return type.append(
                tokenizer.tokenType() == TokenType.KEYWORD
                        ? wrapKeyword(tokenizer.keyword())
                        : wrapIdentifier(tokenizer.identifier())
        );
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
        xml.append(writeVarDec(new StringBuilder()));
    }


    private StringBuilder writeVarDec(StringBuilder varDecXml) {
        return writeVarDec(varDecXml, false);
    }

    /**
     * @return hasMore
     */
    private StringBuilder writeVarDec(StringBuilder varDecXml, boolean list) {
        if (!list) {
            append(varDecXml);

            advance();
            writeType(varDecXml);

            advance();
        }

        append(varDecXml);

        advance();
        if (tokenizer.symbol() == ';') {
            return append(varDecXml);
        }

        append(varDecXml);

        advance();
        return writeVarDec(varDecXml, true);
    }

    public void compileStatements() {
        xml.append(
                writeStatements(new StringBuilder())
        );
    }

    private StringBuilder writeStatements(StringBuilder statementsXml) {
        if (!isStatement(tokenizer.keyword())) {
            return statementsXml;
        }

        writeStatement(statementsXml);

        advance();
        if (tokenizer.tokenType() != TokenType.KEYWORD || !isStatement(tokenizer.keyword())) {
            nextChecked = true;
            return statementsXml;
        }


        return writeStatements(statementsXml);
    }

    private StringBuilder writeStatement(StringBuilder statementXml) {
        return switch (tokenizer.keyword()) {
            case LET -> writeLet(statementXml);
            case IF -> writeIf(statementXml);
            case WHILE -> writeWhile(statementXml);
            case DO -> writeDo(statementXml);
            case RETURN -> writeReturn(statementXml);
            default -> throw new IllegalStateException("Unsupported type " + tokenizer.keyword());
        };
    }

    private boolean isStatement(Keyword keyword) {
        return keyword == Keyword.LET
                || keyword == Keyword.IF
                || keyword == Keyword.WHILE
                || keyword == Keyword.DO
                || keyword == Keyword.RETURN;
    }

    public void compileLet() {
        xml.append(
                writeLet(new StringBuilder())
        );
    }

    private StringBuilder writeLet(StringBuilder letXml) {
        append(letXml);

        advance();
        append(letXml);

        advance();
        final boolean array = tokenizer.symbol() == '[';
        if (array) {
            append(letXml);
            advance();
            writeExpression(letXml);

            advance();
            append(letXml);

            // =
            advance();
        }

        // =
        append(letXml);

        advance();
        writeExpression(letXml);

        advance();
        return append(letXml);
    }

    public void compileIf() {
        xml.append(
                writeIf(new StringBuilder())
        );
    }

    private StringBuilder writeIf(StringBuilder ifXml) {
        append(ifXml);

        advance();
        writeConditionalStatements(ifXml);
        if (!tokenizer.hasMoreTokens()) {
            return ifXml;
        }

        advance();
        if (tokenizer.tokenType() != TokenType.KEYWORD || tokenizer.keyword() != Keyword.ELSE) {
            nextChecked = true;
            return ifXml;
        }

        append(ifXml);

        advance();
        append(ifXml);
        level++;

        advance();
        writeStatements(ifXml);

        level--;
        advance();

        return append(ifXml);
    }

    public void compileWhile() {
        xml.append(
                writeWhile(new StringBuilder())
        );
    }

    private StringBuilder writeWhile(StringBuilder whileXml) {
        append(whileXml);
        advance();

        return writeConditionalStatements(whileXml);
    }

    private StringBuilder writeConditionalStatements(StringBuilder localXml) {
        append(localXml);

        advance();
        writeExpression(localXml);

        advance();
        append(localXml);

        // {
        advance();
        append(localXml);
        level++;

        advance();
        writeStatements(localXml);

        level--;
        advance();

        return append(localXml);
    }

    public void compileDo() {
        advance();
        xml.append(
                writeDo(new StringBuilder())
        );
    }

    private StringBuilder writeDo(StringBuilder doXml) {
        append(doXml);

        advance();
        writeSubroutineCall(doXml);

        advance();
        return append(doXml);
    }

    public void compileReturn() {
        xml.append(
                writeReturn(new StringBuilder())
        );
    }

    private StringBuilder writeReturn(StringBuilder returnXml) {
        append(returnXml);

        advance();
        if (tokenizer.tokenType() == TokenType.SYMBOL && tokenizer.symbol() == ';') {
            return append(returnXml);
        }

        writeExpression(returnXml);
        advance();
        return append(returnXml);
    }

    public void compileExpression() {
        xml.append(
                writeExpression(new StringBuilder())
        );
    }

    public void compileTerm() {
        xml.append(
                writeTerm(new StringBuilder())
        );
    }

    private StringBuilder writeTerm(StringBuilder termXml) {
        final TokenType tokenType = tokenizer.tokenType();
        return switch (tokenType) {
            case INT_CONST -> termXml.append(wrapIntConst(tokenizer.intVal()));
            case STRING_CONST -> termXml.append(wrapStringConst(tokenizer.stringVal()));
            case KEYWORD -> termXml.append(wrapKeyword(tokenizer.keyword()));
            case IDENTIFIER -> writeTermIdentifier(termXml);
            case SYMBOL -> writeTermSymbol(termXml);
        };
    }

    private StringBuilder writeTermSymbol(StringBuilder termXml) {
        if (tokenizer.symbol() != '(') {
            nextChecked = true;
            return termXml;
        }

        append(termXml);
        final boolean isOp = isUnaryOp(tokenizer.symbol());
        if (!isOp) {
            advance();
            writeExpression(termXml);

            advance();
            return append(termXml);
        }


        append(termXml);
        advance();

        return writeTerm(termXml);
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

    private StringBuilder writeTermIdentifier(StringBuilder identefierXml) {
        append(identefierXml);

        //just identifier
        advance();
        if (tokenizer.tokenType() != TokenType.SYMBOL) {
            nextChecked = true;
            return identefierXml;
        }

        //array, expression
        final boolean isArray = tokenizer.symbol() == '[';
        if (tokenizer.symbol() == '(' || isArray) {
            append(identefierXml);

            advance();
            writeExpressionList(identefierXml);

            advance();
            append(identefierXml);

            if (isArray) {
                return identefierXml;
            }

            // .
            advance();
            append(identefierXml);
        } else if (tokenizer.symbol() == '.') {
            // subroutineCall
            append(identefierXml);
        } else {
            nextChecked = true;
            return identefierXml;
        }

        advance();
        return writeSubroutineCallAfterDot(identefierXml);
    }

    private StringBuilder writeSubroutineCall(StringBuilder subroutineXml) {
        append(subroutineXml);

        advance();
        if (tokenizer.symbol() == '(') {
            advance();
            writeExpressionList(subroutineXml);

            advance();
            append(subroutineXml);

            advance();
        }

        //dot
        append(subroutineXml);

        advance();
        return writeSubroutineCallAfterDot(subroutineXml);
    }

    //...'.'
    private StringBuilder writeSubroutineCallAfterDot(StringBuilder subroutineXml) {
        append(subroutineXml);

        advance();
        append(subroutineXml);

        advance();
        writeExpressionList(subroutineXml);

        advance();
        append(subroutineXml);

        return subroutineXml;
    }

    private StringBuilder writeExpression(StringBuilder expressionXml) {
        writeTerm(expressionXml);
        if (!tokenizer.hasMoreTokens()) {
            return expressionXml;
        }

        advance();
        if (tokenizer.tokenType() == TokenType.SYMBOL && isOp(tokenizer.symbol())) {
            append(expressionXml);
            advance();
            return writeTerm(expressionXml);
        } else {
            this.nextChecked = true;
        }

        return expressionXml;
    }

    private void advance() {
        if (nextChecked) {
            nextChecked = false;
            return;
        }

        tokenizer.advance();
    }

    public void compileExpressionList() {
        xml.append(
                writeExpressionList(new StringBuilder())
        );
    }

    private StringBuilder writeExpressionList(StringBuilder expressionXml) {
        writeExpression(expressionXml);
        if (!tokenizer.hasMoreTokens()) {
            return expressionXml;
        }

        advance();
        if (tokenizer.tokenType() == TokenType.SYMBOL && tokenizer.symbol() == ',') {
            append(expressionXml);
            advance();
            return writeExpressionList(expressionXml);
        } else {
            this.nextChecked = true;
        }

        return expressionXml;
    }

    private StringBuilder append(StringBuilder builder) {
        return switch (tokenizer.tokenType()) {
            case KEYWORD -> builder.append(wrapKeyword(tokenizer.keyword()));
            case IDENTIFIER -> builder.append(wrapIdentifier(tokenizer.identifier()));
            case SYMBOL -> builder.append(wrapSymbol(tokenizer.symbol()));
            case INT_CONST -> builder.append(wrapIntConst(tokenizer.intVal()));
            case STRING_CONST -> builder.append(wrapStringConst(tokenizer.stringVal()));
            default -> throw new IllegalStateException("Unsupported token type: " + tokenizer.tokenType());
        };
    }

    private String wrapKeyword(Keyword keyword) {
        return "%s<keyword> %s </keyword>\n".formatted(TAB.repeat(level), keyword.name().toLowerCase());
    }

    private String wrapIdentifier(String identifier) {
        return "%s<identifier> %s </identifier>\n".formatted(TAB.repeat(level), identifier);
    }

    private String wrapSymbol(char symbol) {
        return "%s<symbol> %s </symbol>\n".formatted(TAB.repeat(level), symbol);
    }

    private String wrapIntConst(short intConst) {
        return "%s<intConst> %d </intConst>\n".formatted(TAB.repeat(level), intConst);
    }

    private String wrapStringConst(String stringConst) {
        return "%s<stringConst> %s </stringConst>\n".formatted(TAB.repeat(level), stringConst);
    }

    public void flush() throws IOException {
        this.bufferedWriter.write(this.xml.toString());
        this.xml.setLength(0);

        this.bufferedWriter.flush();
    }

    @Override
    public void close() throws IOException {
        this.tokenizer.close();
        this.bufferedWriter.close();
    }
}

