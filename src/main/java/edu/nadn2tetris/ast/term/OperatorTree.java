package edu.nadn2tetris.ast.term;

import edu.nadn2tetris.ast.NodeKind;

public final class OperatorTree extends AbstractConstantTree<OperatorTree.Op> {
    public OperatorTree(Op op) {
        super(NodeKind.OP, op);
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
        NOT,
        NEG
    }
}
