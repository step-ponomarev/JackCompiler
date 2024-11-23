package edu.nadn2tetris.ast.term;

import edu.nadn2tetris.ast.AbstractSyntaxTree;
import edu.nadn2tetris.ast.ExpressionTree;
import edu.nadn2tetris.ast.NodeKind;

public final class ArraySyntaxTree extends AbstractSyntaxTree {
    private final String identifierName;
    private final ExpressionTree expressionTree;

    public ArraySyntaxTree(String identifierName, ExpressionTree expressionTree) {
        super(NodeKind.ARRAY);
        this.identifierName = identifierName;
        this.expressionTree = expressionTree;
    }
}
