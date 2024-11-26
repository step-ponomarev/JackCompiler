package edu.nadn2tetris.ast.statement;

import java.util.List;

import edu.nadn2tetris.ast.NodeKind;
import edu.nadn2tetris.ast.term.ATermSyntaxTree;

public final class IfStatementTree extends StatementTree {
    public ATermSyntaxTree condition;
    public List<StatementTree> ifBody;
    public List<StatementTree> elseBody;

    public IfStatementTree() {
        super(NodeKind.IF_STATEMENT);
    }
}
