package edu.nadn2tetris.ast.term.subroutine;

import java.util.List;

import edu.nadn2tetris.ast.ATypedTree;
import edu.nadn2tetris.ast.NodeKind;
import edu.nadn2tetris.ast.term.ParameterTree;

public final class SubroutineDeclarationTree extends ATypedTree {
    public enum SubroutineType {
        FUNCTION,
        METHOD,
        CONSTRUCTOR
    }

    public SubroutineType subroutineType;
    public String name;

    public List<ParameterTree> parameterList;
    public SubroutineBodyTree subroutineBodyTree;

    public SubroutineDeclarationTree() {
        super(NodeKind.SUBROUTINE_DECLARATION);
    }
}
