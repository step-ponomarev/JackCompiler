package edu.nadn2tetris.ast.term;

import edu.nadn2tetris.ast.AbstractSyntaxTree;
import edu.nadn2tetris.ast.NodeKind;
import edu.nadn2tetris.ast.Type;

public final class ParameterTree extends AbstractSyntaxTree {
    public String name;
    public Type type;
    public String className;

    public ParameterTree() {
        super(NodeKind.PARAMETER);
    }
}
