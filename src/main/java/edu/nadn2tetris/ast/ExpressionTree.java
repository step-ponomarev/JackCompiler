package edu.nadn2tetris.ast;

public final class ExpressionTree extends AbstractSyntaxTree {
    public AbstractSyntaxTree curr;
    public ExpressionTree parent;
    public ExpressionTree left;
    public ExpressionTree right;

    public ExpressionTree() {
        super(NodeKind.EXPRESSION);
    }

    public ExpressionTree(AbstractSyntaxTree curr) {
        super(NodeKind.EXPRESSION);
        this.curr = curr;
    }
}
