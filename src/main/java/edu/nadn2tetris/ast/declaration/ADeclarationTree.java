package edu.nadn2tetris.ast.declaration;

import java.util.List;

import edu.nadn2tetris.ast.ATypedTree;
import edu.nadn2tetris.ast.NodeKind;

public abstract class ADeclarationTree extends ATypedTree {
    public List<String> varNames;

    protected ADeclarationTree(NodeKind nodeKind) {
        super(nodeKind);
    }
}
