package edu.nadn2tetris.ast.statement;

import edu.nadn2tetris.ast.AbstractSyntaxTree;
import edu.nadn2tetris.ast.NodeKind;

public abstract class StatementTree extends AbstractSyntaxTree {
    protected StatementTree(NodeKind nodeKind) {
        super(nodeKind);
    }
}
