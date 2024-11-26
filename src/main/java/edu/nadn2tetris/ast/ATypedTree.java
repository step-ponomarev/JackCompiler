package edu.nadn2tetris.ast;

public abstract class ATypedTree extends AbstractSyntaxTree {
    public Type type;
    public String classNameType;

    protected ATypedTree(NodeKind nodeKind) {
        super(nodeKind);
    }

    public String getType() {
        return type == Type.CLASS ? classNameType : type.getCode();
    }
}
