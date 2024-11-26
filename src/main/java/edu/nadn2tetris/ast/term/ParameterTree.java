package edu.nadn2tetris.ast.term;

import edu.nadn2tetris.ast.ATypedTree;
import edu.nadn2tetris.ast.NodeKind;

public final class ParameterTree extends ATypedTree {
    public String name;

    public ParameterTree() {
        super(NodeKind.PARAMETER);
    }
}
