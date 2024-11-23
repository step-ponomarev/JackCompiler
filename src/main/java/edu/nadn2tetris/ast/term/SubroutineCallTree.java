package edu.nadn2tetris.ast.term;

import java.util.Collections;
import java.util.List;

import edu.nadn2tetris.ast.AbstractSyntaxTree;
import edu.nadn2tetris.ast.ExpressionTree;
import edu.nadn2tetris.ast.NodeKind;

public final class SubroutineCallTree extends AbstractSyntaxTree {
    private final String identifierTree;
    private final List<ExpressionTree> expressionTreeList;

    public SubroutineCallTree(String identifierTree, List<ExpressionTree> expressionTreeList) {
        super(NodeKind.SUBROUTINE_CALL);
        this.identifierTree = identifierTree;
        this.expressionTreeList = expressionTreeList == null ? Collections.emptyList() : expressionTreeList;
    }
}
