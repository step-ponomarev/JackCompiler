package edu.nadn2tetris.ast.term.subroutine;

import java.util.Collections;
import java.util.List;

import edu.nadn2tetris.ast.NodeKind;
import edu.nadn2tetris.ast.term.ATermSyntaxTree;

public final class SubroutineCallTree extends ATermSyntaxTree {
    private final String identifierTree;
    private final List<ATermSyntaxTree> argList;

    public SubroutineCallTree(String identifierTree, List<ATermSyntaxTree> argList) {
        super(NodeKind.SUBROUTINE_CALL);
        this.identifierTree = identifierTree;
        this.argList = argList == null ? Collections.emptyList() : argList;
    }
}
