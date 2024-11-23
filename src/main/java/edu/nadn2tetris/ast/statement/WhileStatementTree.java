package edu.nadn2tetris.ast.statement;

import java.util.List;

import edu.nadn2tetris.ast.ExpressionTree;
import edu.nadn2tetris.ast.NodeKind;

public final class WhileStatementTree extends StatementTree {
    public ExpressionTree condition;
    public List<StatementTree> body;

    public WhileStatementTree() {
        super(NodeKind.WHILE_STATEMENT);
    }
}
