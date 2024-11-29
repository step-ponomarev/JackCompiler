package edu.nadn2tetris.ast.term;

import edu.nadn2tetris.ast.NodeKind;

public final class ArraySyntaxTree extends ATermSyntaxTree {
    public final String identifierName;
    public final ATermSyntaxTree expressionTree;

    public ArraySyntaxTree(String identifierName, ATermSyntaxTree expressionTree) {
        super(NodeKind.ARRAY);
        this.identifierName = identifierName;
        this.expressionTree = expressionTree;
    }
}
