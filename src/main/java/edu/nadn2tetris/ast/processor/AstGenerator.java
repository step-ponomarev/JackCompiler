package edu.nadn2tetris.ast.processor;

import edu.nadn2tetris.ast.AbstractSyntaxTree;

public interface AstGenerator<T> {
    T generate(AbstractSyntaxTree root);
}
