package edu.nadn2tetris.ast.variables;

import java.util.List;

import edu.nadn2tetris.ast.AbstractSyntaxTree;
import edu.nadn2tetris.ast.NodeKind;
import edu.nadn2tetris.ast.Type;

public final class ClassVarDeclarationTree extends AbstractSyntaxTree {
    public DeclarationType declarationType;
    public Type type;
    public String classNameType;
    public List<String> varNames;

    public enum DeclarationType {
        STATIC,
        FIELD
    }

    public ClassVarDeclarationTree() {
        super(NodeKind.CLASS_VAR_DECLARATION);
    }
}
