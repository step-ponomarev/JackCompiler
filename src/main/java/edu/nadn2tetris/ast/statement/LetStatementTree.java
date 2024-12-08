package edu.nadn2tetris.ast.statement;

import edu.nadn2tetris.ast.NodeKind;
import edu.nadn2tetris.ast.term.ATermSyntaxTree;
import edu.nadn2tetris.ast.term.ArraySyntaxTree;

public final class LetStatementTree extends StatementTree {
    public String identifierName;
    public ArraySyntaxTree arraySyntaxTree; // if var is array
    public ATermSyntaxTree expression;

    public LetStatementTree() {
        super(NodeKind.LET_STATEMENT);
    }
}
