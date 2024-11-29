package edu.nadn2tetris.ast.statement;

import edu.nadn2tetris.ast.NodeKind;
import edu.nadn2tetris.ast.term.subroutine.SubroutineCallTree;

public final class DoStatementTree extends StatementTree {
    public SubroutineCallTree subroutineCallTree;

    public DoStatementTree() {
        super(NodeKind.DO_STATEMENT);
    }
}
