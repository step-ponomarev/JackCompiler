package edu.nadn2tetris.ast.processor;

import edu.nadn2tetris.ast.AbstractSyntaxTree;
import edu.nadn2tetris.ast.ClassTree;
import edu.nadn2tetris.common.Keyword;

public final class XmlTreeAstProcessor implements AstProcessor<String> {
    private final String TAB_SYMBOL = "\t\t";
    private int nestingLevel = 0;

    final StringBuilder xml = new StringBuilder();

    @Override
    public String process(AbstractSyntaxTree root) {
        handleSyntaxTree(root);
        return xml.toString();
    }

    private void handleSyntaxTree(AbstractSyntaxTree node) {
        switch (node.getNodeKind()) {
            case CLASS -> {
                openBlock("class");
                final ClassTree classNode = (ClassTree) node;
                for (AbstractSyntaxTree tree : classNode.blocks) {
                    handleSyntaxTree(tree);
                }
                closeBlock("class");
            }
            case CLASS_VAR_DECLARATION -> {
                openBlock("classVarDeclaration");
                closeBlock("classVarDeclaration");
            }
            case SUBROUTINE_DECLARATION -> {

            }
            case SUBROUTINE_BODY -> {

            }
            case SUBROUTINE_CALL -> {

            }
            case VAR_DECLARATION -> {

            }
            case KEYWORD -> {
            }
            default -> throw new IllegalStateException("Unexpected node kind: " + node.getNodeKind());

        }
    }

    private void openBlock(String block) {
        xml.append("%s<%s>\n".formatted(TAB_SYMBOL.repeat(nestingLevel++), block));
    }

    private void closeBlock(String block) {
        xml.append("%s</%s>\n".formatted(TAB_SYMBOL.repeat(--nestingLevel >= 0 ? nestingLevel : 0), block));
    }

    private void write(Keyword keyword) {
        xml.append("%s<keyword> %s </keyword>\n".formatted(TAB_SYMBOL.repeat(nestingLevel), keyword.name().toLowerCase()));
    }

    private void write(String identifier) {
        xml.append("%s<identifier> %s </identifier>\n".formatted(TAB_SYMBOL.repeat(nestingLevel), identifier));
    }

    private void write(char symbol) {
        xml.append("%s<symbol> %s </symbol>\n".formatted(TAB_SYMBOL.repeat(nestingLevel), symbol));
    }

    private void write(short intConst) {
        xml.append("%s<integerConstant> %d </integerConstant>\n".formatted(TAB_SYMBOL.repeat(nestingLevel), intConst));
    }

    private void wrapStringConst(String stringConst) {
        xml.append("%s<stringConstant> %s </stringConstant>\n".formatted(TAB_SYMBOL.repeat(nestingLevel), stringConst));
    }
}
