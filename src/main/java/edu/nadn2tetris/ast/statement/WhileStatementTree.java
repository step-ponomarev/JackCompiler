package edu.nadn2tetris.ast.statement;

import java.util.List;

import edu.nadn2tetris.ast.NodeKind;
import edu.nadn2tetris.ast.term.ATermSyntaxTree;

public final class WhileStatementTree extends StatementTree {
    public ATermSyntaxTree condition;
    public List<StatementTree> body;

    public WhileStatementTree() {
        super(NodeKind.WHILE_STATEMENT);
    }
}
