package edu.nadn2tetris.ast.term;

import edu.nadn2tetris.ast.NodeKind;

public abstract class AbstractConstantTree<V> extends ATermSyntaxTree {
    public final V value;

    public AbstractConstantTree(NodeKind nodeKind, V value) {
        super(nodeKind);
        this.value = value;
    }
}
