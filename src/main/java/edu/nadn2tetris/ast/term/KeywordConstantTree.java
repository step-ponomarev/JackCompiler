package edu.nadn2tetris.ast.term;

import edu.nadn2tetris.ast.NodeKind;

public final class KeywordConstantTree extends AbstractConstantTree<KeywordConstantTree.Keyword> {
    public enum Keyword {
        TRUE,
        FALSE,
        NULL,
        THIS
    }

    public KeywordConstantTree(Keyword value) {
        super(NodeKind.KEYWORD, value);
    }
}
