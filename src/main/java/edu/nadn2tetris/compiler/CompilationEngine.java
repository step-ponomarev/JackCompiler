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
//TODO: 1) Инкапсулировать xml запсь(чтобы не дублировать wrap)
//      2) Писать сразу в буффер
//      3) Рекурсия(?)
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
        classXml.append(wrapKeyword(Keyword.CLASS));

        advance();
        classXml.append(
                wrapIdentifier(tokenizer.identifier())
        );

        // {
        advance();
        classXml.append(
                wrapSymbol(tokenizer.symbol())
        );
        level++;


        advance();
        if (tokenizer.tokenType() == TokenType.SYMBOL && tokenizer.symbol() == '}') {
            level--;
            xml.append(wrapSymbol(tokenizer.symbol()));
            return classXml;
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
        classXml.append(
                tokenizer.symbol()
        );

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
        varDexXml.append(
                wrapKeyword(tokenizer.keyword())
        );

        advance();
        varDexXml.append(
                tokenizer.tokenType() == TokenType.IDENTIFIER
                        ? wrapIdentifier(tokenizer.identifier())
                        : wrapKeyword(tokenizer.keyword())
        );

        advance();
        varDexXml.append(
                tokenizer.identifier()
        );

        if (!tokenizer.hasMoreTokens()) {
            return varDexXml;
        }

        advance();
        if (tokenizer.tokenType() != TokenType.SYMBOL || tokenizer.symbol() == ',') {
            nextChecked = true;
            return varDexXml;
        }

        //,
        varDexXml.append(
                tokenizer.symbol()
        );

        advance();
        return writeVarDec(varDexXml);
    }

    public void compileSubroutine() {
        xml.append(
                writeSubroutineDec(new StringBuilder())
        );
    }

    private StringBuilder writeSubroutineDec(StringBuilder subroutineDecXml) {
        subroutineDecXml.append(
                wrapKeyword(tokenizer.keyword())
        );

        advance();
        if (isType(tokenizer)) {
            writeType(subroutineDecXml);
        } else {
            subroutineDecXml.append(wrapKeyword(Keyword.VOID));
        }

        advance();
        subroutineDecXml.append(
                wrapIdentifier(tokenizer.identifier())
        );

        //(
        advance();
        subroutineDecXml.append(
                wrapSymbol(tokenizer.symbol())
        );

        advance();
        writeParameterList(subroutineDecXml);

        //)
        advance();
        subroutineDecXml.append(
                wrapSymbol(tokenizer.symbol())
        );

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
        subroutineXml.append(
                wrapSymbol(tokenizer.symbol())
        );
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
        subroutineXml.append(
                wrapSymbol(tokenizer.symbol())
        );

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
        parameterListXml.append(
                wrapSymbol(tokenizer.symbol())
        );

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
            varDecXml.append(wrapKeyword(tokenizer.keyword()));

            advance();
            writeType(varDecXml);

            advance();
        }
        
        varDecXml.append(wrapIdentifier(tokenizer.identifier()));

        advance();
        if (tokenizer.symbol() == ';') {
            return varDecXml.append(wrapSymbol(tokenizer.symbol()));
        }

        varDecXml.append(wrapSymbol(tokenizer.symbol()));

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
        letXml.append(wrapKeyword(Keyword.LET));

        advance();
        letXml.append(wrapIdentifier(tokenizer.identifier()));

        advance();
        final boolean array = tokenizer.symbol() == '[';
        if (array) {
            letXml.append(wrapSymbol(tokenizer.symbol()));
            advance();
            writeExpression(letXml);

            advance();
            letXml.append(wrapSymbol(tokenizer.symbol()));

            // =
            advance();
        }

        // =
        letXml.append(wrapSymbol(tokenizer.symbol()));

        advance();
        writeExpression(letXml);

        advance();
        return letXml.append(
                wrapSymbol(tokenizer.symbol())
        );
    }

    public void compileIf() {
        xml.append(
                writeIf(new StringBuilder())
        );
    }

    private StringBuilder writeIf(StringBuilder ifXml) {
        ifXml.append(wrapKeyword(Keyword.IF));

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

        ifXml.append(
                wrapKeyword(tokenizer.keyword())
        );

        advance();
        ifXml.append(wrapSymbol(tokenizer.symbol()));
        level++;

        advance();
        writeStatements(ifXml);

        level--;
        advance();

        return ifXml.append(wrapSymbol(tokenizer.symbol()));
    }

    public void compileWhile() {
        xml.append(
                writeWhile(new StringBuilder())
        );
    }

    private StringBuilder writeWhile(StringBuilder whileXml) {
        whileXml.append(wrapKeyword(Keyword.WHILE));
        advance();

        return writeConditionalStatements(whileXml);
    }

    private StringBuilder writeConditionalStatements(StringBuilder localXml) {
        localXml.append(wrapSymbol(tokenizer.symbol()));

        advance();
        writeExpression(localXml);

        advance();
        localXml.append(wrapSymbol(tokenizer.symbol()));

        advance();
        localXml.append(wrapSymbol(tokenizer.symbol()));
        level++;

        advance();
        writeStatements(localXml);

        level--;
        advance();

        return localXml.append(wrapSymbol(tokenizer.symbol()));
    }

    public void compileDo() {
        advance();
        xml.append(
                writeDo(new StringBuilder())
        );
    }

    private StringBuilder writeDo(StringBuilder doXml) {
        doXml.append(wrapKeyword(Keyword.DO));
        advance();
        return writeSubroutineCall(doXml);
    }

    public void compileReturn() {
        xml.append(
                writeReturn(new StringBuilder())
        );
    }

    private StringBuilder writeReturn(StringBuilder returnXml) {
        returnXml.append(wrapKeyword(Keyword.RETURN));

        advance();
        if (tokenizer.tokenType() == TokenType.SYMBOL && tokenizer.symbol() == ';') {
            return returnXml.append(wrapSymbol(tokenizer.symbol()));
        }

        writeExpression(returnXml);
        advance();
        return returnXml.append(wrapSymbol(tokenizer.symbol()));
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
        termXml.append(wrapSymbol(tokenizer.symbol()));
        final boolean isOp = isUnaryOp(tokenizer.symbol());
        if (!isOp) {
            advance();
            writeExpression(termXml);

            advance();
            return termXml.append(wrapSymbol(tokenizer.symbol()));
        }


        termXml.append(wrapSymbol(tokenizer.symbol()));
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
        identefierXml.append(wrapIdentifier(tokenizer.identifier()));

        advance();
        if (tokenizer.tokenType() != TokenType.SYMBOL) {
            nextChecked = true;
            return identefierXml;
        }

        final boolean isArray = tokenizer.symbol() == '[';
        if (tokenizer.symbol() == '(' || isArray) {
            identefierXml.append(wrapSymbol(tokenizer.symbol()));

            advance();
            writeExpressionList(identefierXml);

            advance();
            identefierXml.append(wrapSymbol(tokenizer.symbol()));
        } else {
            nextChecked = true;
            return identefierXml;
        }

        if (isArray) {
            return identefierXml;
        }

        // subroutineCall
        if (!tokenizer.hasMoreTokens()) {
            return identefierXml;
        }

        advance();
        if (tokenizer.tokenType() != TokenType.SYMBOL || tokenizer.symbol() != '.') {
            nextChecked = true;
            return identefierXml;
        }

        identefierXml.append(wrapSymbol(tokenizer.symbol()));

        advance();

        return writeSubroutineCallAfterDot(identefierXml);
    }

    private StringBuilder writeSubroutineCall(StringBuilder subroutineXml) {
        subroutineXml.append(wrapIdentifier(tokenizer.identifier()));

        boolean idintifier = false;
        advance();
        if (tokenizer.symbol() == '(') {
            advance();
            writeExpressionList(subroutineXml);

            advance();
            subroutineXml.append(wrapSymbol(tokenizer.symbol()));
            wrapSymbol(tokenizer.symbol());
        } else {
            idintifier = true;
        }

        if (!idintifier) {
            advance();
        }
        //dot
        subroutineXml.append(wrapSymbol(tokenizer.symbol()));

        return writeSubroutineCallAfterDot(subroutineXml);
    }

    //...'.'
    private StringBuilder writeSubroutineCallAfterDot(StringBuilder subroutineXml) {
        subroutineXml.append(wrapIdentifier(tokenizer.identifier()));

        advance();
        subroutineXml.append(wrapSymbol(tokenizer.symbol()));

        advance();
        writeExpressionList(subroutineXml);

        advance();
        subroutineXml.append(wrapSymbol(tokenizer.symbol()));

        return subroutineXml;
    }

    private StringBuilder writeExpression(StringBuilder expressionXml) {
        final StringBuilder expression = writeTerm(expressionXml);
        if (!tokenizer.hasMoreTokens()) {
            return expression;
        }

        advance();
        if (tokenizer.tokenType() == TokenType.SYMBOL && isOp(tokenizer.symbol())) {
            expression.append(wrapSymbol(tokenizer.symbol()));
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
        final StringBuilder expression = writeExpression(expressionXml);
        if (!tokenizer.hasMoreTokens()) {
            return expression;
        }

        advance();
        if (tokenizer.tokenType() == TokenType.SYMBOL && tokenizer.symbol() == ',') {
            expression.append(wrapSymbol(tokenizer.symbol()));
            advance();
            return writeExpressionList(expressionXml);
        } else {
            this.nextChecked = true;
        }

        return expressionXml;
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
        return "%s<stringConst> %d </stringConst>\n".formatted(TAB.repeat(level), stringConst);
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

