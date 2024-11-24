package edu.nadn2tetris.ast.subroutine;

import java.util.Collections;
import java.util.List;

import edu.nadn2tetris.ast.AbstractSyntaxTree;
import edu.nadn2tetris.ast.ExpressionTree;
import edu.nadn2tetris.ast.NodeKind;

public final class SubroutineCallTree extends AbstractSyntaxTree {
    private final String identifierTree;
    private final List<ExpressionTree> argList;

    public SubroutineCallTree(String identifierTree, List<ExpressionTree> argList) {
        super(NodeKind.SUBROUTINE_CALL);
        this.identifierTree = identifierTree;
        this.argList = argList == null ? Collections.emptyList() : argList;
    }
}
