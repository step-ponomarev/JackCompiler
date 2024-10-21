package edu.nadn2tetris.tokenizer;

public enum TokenType {
    KEYWORD,
    SYMBOL,
    IDENTIFIER,
    INT_CONST,  // [0, 32767] by spec
    STRING_CONST
}
