package edu.nadn2tetris.ast.statement;

import edu.nadn2tetris.ast.ExpressionTree;
import edu.nadn2tetris.ast.NodeKind;

public final class ReturnStatementTree extends StatementTree {
    public ExpressionTree expression;

    public ReturnStatementTree() {
        super(NodeKind.RETURN_STATEMENT);
    }
}
