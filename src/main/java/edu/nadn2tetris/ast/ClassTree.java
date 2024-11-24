package edu.nadn2tetris.ast;

import java.util.List;

public final class ClassTree extends AbstractSyntaxTree {
    public List<AbstractSyntaxTree> blocks;
    public String className;

    public ClassTree() {
        super(NodeKind.CLASS);
    }
}
