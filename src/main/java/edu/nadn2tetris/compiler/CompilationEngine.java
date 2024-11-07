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
public final class CompilationEngine implements Closeable {
    private final JackTokenizer tokenizer;
    private final BufferedWriter bufferedWriter;
    private final StringBuilder xml = new StringBuilder();
    private static final String TAB = "\n";

    // не нужно дегать advance, проверяем текущий снаружи
    private boolean nextChecked = false;
    private int level = 0;

    public CompilationEngine(JackTokenizer tokenizer, OutputStream outputStream) {
        this.tokenizer = tokenizer;
        this.bufferedWriter = new BufferedWriter(new OutputStreamWriter(outputStream));
    }

    public void compileClass() {
        throw new UnsupportedOperationException();
    }

    public void compileClassVarDec() {
        throw new UnsupportedOperationException();
    }

    public void compileSubroutine() {
        throw new UnsupportedOperationException();
    }

    public void compileParameterList() {
        throw new UnsupportedOperationException();
    }

    public void compileSubroutineBody() {
        throw new UnsupportedOperationException();
    }

    public void compileVarDec() {
        final StringBuilder varXml = new StringBuilder(wrapKeyword(Keyword.VAR));
        tokenizer.advance();
        TokenType tokenType = tokenizer.tokenType();
        if (tokenType != TokenType.IDENTIFIER && tokenType != TokenType.KEYWORD) {
            throw new IllegalStateException("Expected identifier or keyword, but got: " + tokenType);
        }
        if (tokenType == TokenType.IDENTIFIER) {
            varXml.append(wrapIdentifier(tokenizer.identifier()));
        } else {
            varXml.append(wrapKeyword(tokenizer.keyword()));
        }

        xml.append(writeVarDec(varXml));
    }

    /**
     * @return hasMore
     */
    private StringBuilder writeVarDec(StringBuilder varDecXml) {
        tokenizer.advance();
        TokenType tokenType = tokenizer.tokenType();
        if (tokenType != TokenType.IDENTIFIER) {
            throw new IllegalStateException("Expected identifier, but got: " + tokenType);
        }
        varDecXml.append(wrapIdentifier(tokenizer.identifier()));

        tokenizer.advance();
        tokenType = tokenizer.tokenType();
        if (tokenType != TokenType.SYMBOL) {
            throw new IllegalStateException("Expected symbol, but got: " + tokenType);
        }

        if (tokenizer.symbol() == ';') {
            return varDecXml.append(wrapSymbol(tokenizer.symbol()));
        }

        if (tokenizer.symbol() != ',') {
            throw new IllegalStateException("Unsupported symbol: " + tokenizer.symbol());
        }
        varDecXml.append(wrapSymbol(tokenizer.symbol()));

        return writeVarDec(varDecXml);
    }

    public void compileStatements() {
        throw new UnsupportedOperationException();
    }

    public void compileLet() {
        throw new UnsupportedOperationException();
    }

    public void compileIf() {
        throw new UnsupportedOperationException();
    }

    public void compileWhile() {
        throw new UnsupportedOperationException();
    }

    public void compileDo() {
        throw new UnsupportedOperationException();
    }

    public void compileReturn() {
        throw new UnsupportedOperationException();
    }

    public void compileExpression() {
        throw new UnsupportedOperationException();
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
            //TODO: add subruitineCall
            case IDENTIFIER -> writeTermIdentifier(termXml);
            case SYMBOL -> writeTermSymbol(termXml);
            default -> throw new IllegalStateException("Unsupported token type: " + tokenType);
        };
    }

    private StringBuilder writeTermSymbol(StringBuilder termXml) {
        if (tokenizer.tokenType() != TokenType.SYMBOL || (tokenizer.symbol() != '(' && !isOp(tokenizer.symbol()))) {
            throw new IllegalStateException("Unexpected token type: " + tokenizer.tokenType());
        }

        termXml.append(wrapSymbol(tokenizer.symbol()));
        final boolean isOp = isOp(tokenizer.symbol());
        if (!isOp) {
            tokenizer.advance();
            writeExpression(termXml);

            tokenizer.advance();
            if (tokenizer.tokenType() != TokenType.SYMBOL || tokenizer.symbol() != ')') {
                throw new IllegalStateException("Unexpected token type: " + tokenizer.tokenType());
            }
            return termXml.append(wrapSymbol(tokenizer.symbol()));
        }


        termXml.append(wrapSymbol(tokenizer.symbol()));
        tokenizer.advance();

        return writeTerm(termXml);
    }

    private static boolean isOp(char op) {
        return op == '+' || op == '-' || op == '*' || op == '/' || op == '&' || op == '|';
    }

    private StringBuilder writeTermIdentifier(StringBuilder termXml) {
        termXml.append(wrapIdentifier(tokenizer.identifier()));
        tokenizer.advance();
        if (tokenizer.tokenType() != TokenType.SYMBOL || tokenizer.symbol() != '(') {
            nextChecked = true;
            return termXml;
        }
        termXml.append(wrapSymbol(tokenizer.symbol()));

        tokenizer.advance();
        writeExpression(termXml);

        tokenizer.advance();
        if (tokenizer.tokenType() != TokenType.SYMBOL || tokenizer.symbol() != ')') {
            throw new IllegalStateException("Unexpected token type: " + tokenizer.tokenType());
        }
        termXml.append(wrapSymbol(tokenizer.symbol()));

        return termXml;
    }

    private StringBuilder writeExpression(StringBuilder expressionXml) {
        final StringBuilder stringBuilder = writeTerm(expressionXml);
            // term (op term)*
        
        return expressionXml;
    }

    public short compileExpressionList() {
        throw new UnsupportedOperationException();
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

