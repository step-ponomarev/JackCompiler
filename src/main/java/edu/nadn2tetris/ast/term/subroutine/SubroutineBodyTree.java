package edu.nadn2tetris.ast.term.subroutine;

import java.util.List;

import edu.nadn2tetris.ast.AbstractSyntaxTree;
import edu.nadn2tetris.ast.NodeKind;

public final class SubroutineBodyTree extends AbstractSyntaxTree {
    public List<AbstractSyntaxTree> nodes;

    public SubroutineBodyTree(List<AbstractSyntaxTree> nodes) {
        super(NodeKind.SUBROUTINE_BODY);
        this.nodes = nodes;
    }

    public SubroutineBodyTree() {
        super(NodeKind.SUBROUTINE_BODY);
    }
}
