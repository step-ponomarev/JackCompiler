package edu.nadn2tetris.ast.term;

import edu.nadn2tetris.ast.NodeKind;

public final class OperatorTree extends ATermSyntaxTree {
    private final Op op;

    public OperatorTree(Op op) {
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
        EQ,
        NOT
    }
}
