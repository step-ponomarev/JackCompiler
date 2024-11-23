package edu.nadn2tetris.ast.subroutine;

import java.util.List;

import edu.nadn2tetris.ast.AbstractSyntaxTree;
import edu.nadn2tetris.ast.NodeKind;
import edu.nadn2tetris.ast.Type;
import edu.nadn2tetris.ast.term.ParameterTree;

public final class SubroutineDeclarationTree extends AbstractSyntaxTree {
    public enum SubroutineType {
        FUNCTION,
        METHOD,
        CONSTRUCTOR
    }

    public SubroutineType subroutineType;
    public String name;
    public Type returnType;

    public List<ParameterTree> parameterList;
    public SubroutineBodyTree subroutineBodyTree;

    public String returnClassName;

    public SubroutineDeclarationTree() {
        super(NodeKind.SUBROUTINE_DECLARATION);
    }
}
