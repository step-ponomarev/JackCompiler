package edu.nadn2tetris.ast.term;

import edu.nadn2tetris.ast.AbstractSyntaxTree;
import edu.nadn2tetris.ast.NodeKind;

public final class BinaryOpTree extends AbstractSyntaxTree {
    private final Op op;

    public BinaryOpTree(Op op) {
        super(NodeKind.OP);
        this.op = op;
    }

    public enum Op {
        ADD,
        SUB,
        MUL,
        DIV,
        AND,
        OR,
        LS,
        GT,
        EQ
    }
}
