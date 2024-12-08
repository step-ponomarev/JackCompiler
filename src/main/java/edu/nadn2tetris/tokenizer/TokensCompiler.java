package edu.nadn2tetris.tokenizer;

import java.io.Closeable;
import java.io.IOException;

public final class TokensCompiler implements Closeable {
    private StringBuilder xml = new StringBuilder();
    private final JackTokenizer tokenizer;

    public TokensCompiler(JackTokenizer tokenizer) {
        this.tokenizer = tokenizer;
    }

    public String generate() {
        xml.append("<tokens>\n");
        while (tokenizer.hasMoreTokens()) {
            tokenizer.advance();
            write();
        }

        xml.append("</tokens>\n");

        return xml.toString();
    }

    private void write() {
        switch (tokenizer.tokenType()) {
            case KEYWORD ->
                    xml.append("<keyword> %s </keyword>\n".formatted(tokenizer.keyword().toString().toLowerCase()));
            case SYMBOL -> xml.append("<symbol> %s </symbol>\n".formatted(tokenizer.symbol()));
            case STRING_CONST ->
                    xml.append("<stringConstant> %s </stringConstant>\n".formatted(tokenizer.stringVal()));
            case INT_CONST -> xml.append("<integerConstant> %d </integerConstant>\n".formatted(tokenizer.intVal()));
            case IDENTIFIER -> xml.append("<identifier> %s </identifier>\n".formatted(tokenizer.identifier()));
        }
    }

    @Override
    public void close() throws IOException {
        tokenizer.close();
    }
}
