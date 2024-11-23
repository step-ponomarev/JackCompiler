package edu.nadn2tetris.ast.subroutine;

import java.util.List;

import edu.nadn2tetris.ast.AbstractSyntaxTree;
import edu.nadn2tetris.ast.NodeKind;

public final class SubroutineBodyTree extends AbstractSyntaxTree {
    final List<AbstractSyntaxTree> nodes;

    public SubroutineBodyTree(List<AbstractSyntaxTree> nodes) {
        super(NodeKind.SUBROUTINE_BODY);
        this.nodes = nodes;
    }
}
