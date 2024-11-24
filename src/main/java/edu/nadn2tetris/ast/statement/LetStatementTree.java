package edu.nadn2tetris.ast.statement;

import edu.nadn2tetris.ast.ExpressionTree;
import edu.nadn2tetris.ast.NodeKind;
import edu.nadn2tetris.ast.term.ArraySyntaxTree;

public final class LetStatementTree extends StatementTree {
    public String identifierName;
    public ArraySyntaxTree arraySyntaxTree;
    public ExpressionTree assigment;

    public LetStatementTree() {
        super(NodeKind.LET_STATEMENT);
    }
}
