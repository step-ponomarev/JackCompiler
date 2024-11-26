package edu.nadn2tetris.ast.term;

import edu.nadn2tetris.ast.NodeKind;

public final class IdentifierTree extends ATermSyntaxTree {
    public final String varName;

    public IdentifierTree(String varName) {
        super(NodeKind.IDENTIFIER);
        this.varName = varName;
    }
}
