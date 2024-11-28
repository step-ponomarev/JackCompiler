package edu.nadn2tetris.ast.processor;

import edu.nadn2tetris.ast.AbstractSyntaxTree;

public final class XmlTreeAstProcessor implements AstProcessor<String> {
    final StringBuilder xml =  new StringBuilder();

    @Override
    public String process(AbstractSyntaxTree root) {


        return xml.toString();
    }

    private void handleSyntaxTree(AbstractSyntaxTree node) {
        switch (node.getNodeKind()) {
            case KEYWORD -> break;

            default -> throw new IllegalStateException("Unexpected node kind: " + node.getNodeKind());

        }
    }
}
