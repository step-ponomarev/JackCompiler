package edu.nadn2tetris.ast.term;

import edu.nadn2tetris.ast.NodeKind;

public final class StringConstantTree extends AbstractConstantTree<String> {
    public StringConstantTree(String value) {
        super(NodeKind.STRING_CONSTANT, value);
    }
}
