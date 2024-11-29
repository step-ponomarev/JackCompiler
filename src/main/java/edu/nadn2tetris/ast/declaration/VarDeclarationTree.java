package edu.nadn2tetris.ast.declaration;

import edu.nadn2tetris.ast.NodeKind;

public final class VarDeclarationTree extends ADeclarationTree {
    public VarDeclarationTree() {
        super(NodeKind.VAR_DECLARATION);
    }
}
