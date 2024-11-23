package edu.nadn2tetris.ast.statement;

import java.util.List;

import edu.nadn2tetris.ast.ExpressionTree;
import edu.nadn2tetris.ast.NodeKind;

public final class IfStatementTree extends StatementTree {
    public ExpressionTree condition;
    public List<StatementTree> ifBody;
    public List<StatementTree> elseBody;

    public IfStatementTree() {
        super(NodeKind.IF_STATEMENT);
    }
}
