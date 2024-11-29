package edu.nadn2tetris.tokenizer;

import java.io.Closeable;
import java.io.IOException;

public final class TokensCompiler implements Closeable {
    private StringBuilder builder = new StringBuilder();
    private final JackTokenizer tokenizer;

    public TokensCompiler(JackTokenizer tokenizer) {
        this.tokenizer = tokenizer;
    }

    public String generate() {
        builder.append("<tokens>\n");
        while (tokenizer.hasMoreTokens()) {
            tokenizer.advance();
            write();
        }

        builder.append("</tokens>\n");

        return builder.toString();
    }

    private void write() {
        switch (tokenizer.tokenType()) {
            case KEYWORD ->
                    builder.append("<keyword> %s </keyword>\n".formatted(tokenizer.keyword().toString().toLowerCase()));
            case SYMBOL -> builder.append("<symbol> %s </symbol>\n".formatted(tokenizer.symbol()));
            case STRING_CONST ->
                    builder.append("<stringConstant> %s </stringConstant>\n".formatted(tokenizer.stringVal()));
            case INT_CONST -> builder.append("<integerConstant> %d </integerConstant>\n".formatted(tokenizer.intVal()));
            case IDENTIFIER -> builder.append("<identifier> %s </identifier>\n".formatted(tokenizer.identifier()));
        }
    }

    @Override
    public void close() throws IOException {
        tokenizer.close();
    }
}
