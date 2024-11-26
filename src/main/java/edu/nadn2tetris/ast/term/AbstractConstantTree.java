package edu.nadn2tetris.ast.term;

import edu.nadn2tetris.ast.NodeKind;

public abstract class AbstractConstantTree<V> extends ATermSyntaxTree {
    private final V value;

    public AbstractConstantTree(NodeKind nodeKind, V value) {
        super(nodeKind);
        this.value = value;
    }

    public V getValue() {
        return value;
    }
}
