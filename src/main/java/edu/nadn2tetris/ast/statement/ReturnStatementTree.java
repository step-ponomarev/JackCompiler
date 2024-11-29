package edu.nadn2tetris.ast.statement;

import edu.nadn2tetris.ast.NodeKind;
import edu.nadn2tetris.ast.term.ATermSyntaxTree;

public final class ReturnStatementTree extends StatementTree {
    public ATermSyntaxTree expression;

    public ReturnStatementTree() {
        super(NodeKind.RETURN_STATEMENT);
    }
}
