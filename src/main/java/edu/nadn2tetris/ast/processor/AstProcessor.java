package edu.nadn2tetris.ast.processor;

import edu.nadn2tetris.ast.AbstractSyntaxTree;

public interface AstProcessor<T> {
    T process(AbstractSyntaxTree root);
}
