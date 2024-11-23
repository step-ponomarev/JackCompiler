package edu.nadn2tetris.ast;

public abstract class AbstractSyntaxTree {
    private final NodeKind nodeKind;

    protected AbstractSyntaxTree(NodeKind nodeKind) {
        this.nodeKind = nodeKind;
    }

    public NodeKind getNodeKind() {
        return nodeKind;
    }
}
