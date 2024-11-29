package edu.nadn2tetris.ast.processor;

import edu.nadn2tetris.ast.AbstractSyntaxTree;

public final class ByteCodeAstGenerator implements AstGenerator<String> {
    private final StringBuilder builder = new StringBuilder();

    @Override
    public String generate(AbstractSyntaxTree root) {
        return "";
    }
}
