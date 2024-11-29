package edu.nadn2tetris.ast.declaration;

import edu.nadn2tetris.ast.NodeKind;

public final class ClassVarDeclarationTree extends ADeclarationTree {
    public DeclarationType declarationType;

    public enum DeclarationType {
        STATIC,
        FIELD
    }

    public ClassVarDeclarationTree() {
        super(NodeKind.CLASS_VAR_DECLARATION);
    }
}
