package edu.nadn2tetris.tokenizer;

public final class JackTokenizer { 
    public boolean hasMoreTokens() {
        throw new UnsupportedOperationException();
    }
    
    public void advance() {
        throw new UnsupportedOperationException();
    }
    
    public TokenType tokenType() {
        throw new UnsupportedOperationException();
    }
    
    public Keyword keyword() {
        throw new UnsupportedOperationException();
    }
    
    public char symbol() {
        throw new UnsupportedOperationException();
    }
    
    public String identifier() {
        throw new UnsupportedOperationException();
    } 
    
    // [0, 32767] by spec
    public short intVal() {
        throw new UnsupportedOperationException();
    }
    
    public String stringVal() {
        throw new UnsupportedOperationException();
    }
}
