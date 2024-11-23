package edu.nadn2tetris.ast.term;

import edu.nadn2tetris.ast.AbstractSyntaxTree;
import edu.nadn2tetris.ast.NodeKind;

public final class UnaryOpTree extends AbstractSyntaxTree {
    public enum Op {
        NEG,
        NOT
    }

    public final Op op;

    public UnaryOpTree(Op op) {
        super(NodeKind.OP);
        this.op = op;
    }
}
