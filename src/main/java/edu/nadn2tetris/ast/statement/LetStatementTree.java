package edu.nadn2tetris.ast.statement;

import edu.nadn2tetris.ast.ExpressionTree;
import edu.nadn2tetris.ast.NodeKind;
import edu.nadn2tetris.ast.term.ArraySyntaxTree;
import edu.nadn2tetris.ast.term.IdentifierTree;

public final class LetStatementTree extends StatementTree {
    public IdentifierTree identifierTree;
    public ArraySyntaxTree arraySyntaxTree;
    public ExpressionTree assigment;

    public LetStatementTree() {
        super(NodeKind.LET_STATEMENT);
    }
}
