package edu.nadn2tetris.ast.variables;

import java.util.List;

import edu.nadn2tetris.ast.AbstractSyntaxTree;
import edu.nadn2tetris.ast.NodeKind;
import edu.nadn2tetris.ast.Type;

public final class VarDeclarationTree extends AbstractSyntaxTree {
    private final List<String> names;
    private final Type type;
    public String classNameType;

    public VarDeclarationTree(List<String> names, Type type) {
        super(NodeKind.VAR_DECLARATION);
        this.names = names;
        this.type = type;
    }
}
