package edu.nadn2tetris.ast.term;

import edu.nadn2tetris.ast.AbstractSyntaxTree;
import edu.nadn2tetris.ast.NodeKind;

public abstract class ATermSyntaxTree extends AbstractSyntaxTree {
    public ATermSyntaxTree left;
    public ATermSyntaxTree right;
    public boolean expression; // (expression)

    protected ATermSyntaxTree(NodeKind nodeKind) {
        super(nodeKind);
    }
}
