package edu.nadn2tetris.ast.term;

import edu.nadn2tetris.ast.NodeKind;

public final class IntegerConstantTree extends AbstractConstantTree<Short> {
    public IntegerConstantTree(short value) {
        super(NodeKind.INTEGER_CONSTANT, value);
    }
}
