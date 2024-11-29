package edu.nadn2tetris.ast.processor;

import java.util.List;

import edu.nadn2tetris.ast.ATypedTree;
import edu.nadn2tetris.ast.AbstractSyntaxTree;
import edu.nadn2tetris.ast.ClassTree;
import edu.nadn2tetris.ast.NodeKind;
import edu.nadn2tetris.ast.Type;
import edu.nadn2tetris.ast.declaration.ClassVarDeclarationTree;
import edu.nadn2tetris.ast.declaration.VarDeclarationTree;
import edu.nadn2tetris.ast.statement.DoStatementTree;
import edu.nadn2tetris.ast.statement.IfStatementTree;
import edu.nadn2tetris.ast.statement.LetStatementTree;
import edu.nadn2tetris.ast.statement.ReturnStatementTree;
import edu.nadn2tetris.ast.statement.StatementTree;
import edu.nadn2tetris.ast.statement.WhileStatementTree;
import edu.nadn2tetris.ast.term.ATermSyntaxTree;
import edu.nadn2tetris.ast.term.ArraySyntaxTree;
import edu.nadn2tetris.ast.term.IdentifierTree;
import edu.nadn2tetris.ast.term.IntegerConstantTree;
import edu.nadn2tetris.ast.term.KeywordConstantTree;
import edu.nadn2tetris.ast.term.OperatorTree;
import edu.nadn2tetris.ast.term.ParameterTree;
import edu.nadn2tetris.ast.term.StringConstantTree;
import edu.nadn2tetris.ast.term.subroutine.SubroutineBodyTree;
import edu.nadn2tetris.ast.term.subroutine.SubroutineCallTree;
import edu.nadn2tetris.ast.term.subroutine.SubroutineDeclarationTree;
import edu.nadn2tetris.common.Keyword;

public final class XmlTreeAstGenerator implements AstGenerator<String> {
    private final String TAB_SYMBOL = "\s\s";

    private final StringBuilder xml = new StringBuilder();
    private int nestingLevel = 0;

    @Override
    public String generate(AbstractSyntaxTree root) {
        handleSyntaxTree(root);
        return xml.toString();
    }

    private void handleSyntaxTree(AbstractSyntaxTree node) {
        switch (node.getNodeKind()) {
            case CLASS -> writeClass(node);
            case CLASS_VAR_DECLARATION -> writeVarDeclaration(node);
            case SUBROUTINE_DECLARATION -> writeSubroutineDeclaration(node);
            case SUBROUTINE_BODY -> writeSubroutineBody(node);
            case VAR_DECLARATION -> writeVarDeclaration((VarDeclarationTree) node);
            case ARRAY, IDENTIFIER, INTEGER_CONSTANT, KEYWORD, STRING_CONSTANT, SUBROUTINE_CALL ->
                    writeTerm((ATermSyntaxTree) node);
            case OP -> writeOp((OperatorTree) node);
            case DO_STATEMENT -> writeDoStatement(node);
            case LET_STATEMENT -> writeLet((LetStatementTree) node);
            case IF_STATEMENT -> writeIfStatement((IfStatementTree) node);
            case WHILE_STATEMENT -> writeWhileStatement((WhileStatementTree) node);
            case RETURN_STATEMENT -> writeReturnStatement((ReturnStatementTree) node);
            default -> throw new IllegalStateException("Unexpected node kind: " + node.getNodeKind());
        }
    }

    private void writeOp(OperatorTree node) {
        final boolean unaryTree = node.right == null;
        if (unaryTree) {
            writeTerm(node);
            return;
        }

        write(parse(node.value));
    }

    private void writeReturnStatement(ReturnStatementTree node) {
        openBlock("returnStatement");
        write(Keyword.RETURN);
        if (node.expression != null) {
            writeExpression(node.expression);
        }

        write(';');

        closeBlock("returnStatement");
    }

    private void writeWhileStatement(WhileStatementTree node) {
        openBlock("whileStatement");
        write(Keyword.WHILE);
        write('(');
        writeExpression(node.condition);
        write(')');

        write('{');
        writeStatements(node.body);
        write('}');

        closeBlock("whileStatement");

    }

    private void writeIfStatement(IfStatementTree node) {
        openBlock("ifStatement");
        write(Keyword.IF);
        write('(');
        writeExpression(node.condition);
        write(')');

        write('{');
        writeStatements(node.ifBody);
        write('}');

        if (node.elseBody != null) {
            write(Keyword.ELSE);
            write('{');
            writeStatements(node.elseBody);
            write('}');
        }

        closeBlock("ifStatement");
    }

    private void writeStatements(List<StatementTree> ifBody) {
        openBlock("statements");
        for (StatementTree statement : ifBody) {
            handleSyntaxTree(statement);
        }
        closeBlock("statements");
    }

    private void writeLet(LetStatementTree node) {
        openBlock("letStatement");
        write(Keyword.LET);
        write(node.identifierName);
        if (node.arraySyntaxTree != null) {
            write('[');
            writeExpression(node.arraySyntaxTree.expressionTree);
            write(']');
        }

        write('=');
        writeExpression(node.assigment);
        write(';');
        closeBlock("letStatement");
    }

    private void writeVarDeclaration(VarDeclarationTree node) {
        openBlock("varDec");
        write(Keyword.VAR);
        writeType(node);
        writeDeclarationVars(node.varNames);
        write(';');
        closeBlock("varDec");
    }

    private void writeDoStatement(AbstractSyntaxTree node) {
        final DoStatementTree doStatementTree = (DoStatementTree) node;
        openBlock("doStatement");
        write(Keyword.DO);
        writeSubroutineCall(doStatementTree.subroutineCallTree);
        write(';');
        closeBlock("doStatement");
    }

    private void writeExpressionList(List<ATermSyntaxTree> expressions) {
        openBlock("expressionList");
        if (expressions == null || expressions.isEmpty()) {
            closeBlock("expressionList");
            return;
        }

        int i = 0;
        while (i < expressions.size() - 1) {
            writeExpression(expressions.get(i++));
            write(',');
        }
        writeExpression(expressions.get(i));

        closeBlock("expressionList");
    }

    private void writeExpression(ATermSyntaxTree expression) {
        openBlock("expression");
        //unary operation
        if (expression.getNodeKind() == NodeKind.OP && expression.right == null) {
            writeTerm(expression);
            closeBlock("expression");
            return;
        }

        if (expression.left != null) {
            writeTerm(expression.left);
        }

        handleSyntaxTree(expression);

        if (expression.right != null) {
            writeTerm(expression.right);
        }

        closeBlock("expression");
    }

    private void writeTerm(ATermSyntaxTree term) {
        if (term == null) {
            return;
        }

        openBlock("term");
        if (term.expression) {
            term.expression = false;
            write('(');
            writeExpression(term);
            write(')');
            closeBlock("term");
            term.expression = true;
            return;
        }

        switch (term.getNodeKind()) {
            case IDENTIFIER -> {
                final IdentifierTree identifierTree = (IdentifierTree) term;
                write(identifierTree.varName);
            }
            case INTEGER_CONSTANT -> {
                final IntegerConstantTree identifierTree = (IntegerConstantTree) term;
                write(identifierTree.value);
            }
            case STRING_CONSTANT -> {
                final StringConstantTree identifierTree = (StringConstantTree) term;
                wrapStringConst(identifierTree.value);
            }
            case KEYWORD -> {
                final KeywordConstantTree keywordConstantTree = (KeywordConstantTree) term;
                write(parse(keywordConstantTree.value));
            }
            case SUBROUTINE_CALL -> {
                final SubroutineCallTree subroutineCallTree = (SubroutineCallTree) term;
                writeSubroutineCall(subroutineCallTree);
            }
            case ARRAY -> writeArray((ArraySyntaxTree) term);
            case OP -> writeTermOp((OperatorTree) term);
            default -> throw new IllegalStateException("Unexpected node kind: " + term.getNodeKind());
        }

        closeBlock("term");
    }

    private void writeTermOp(OperatorTree term) {
        write(parse(term.value));
        writeTerm(term.left);
    }

    private void writeArray(ArraySyntaxTree array) {
        write(array.identifierName);
        write('[');
        writeExpression(array.expressionTree);
        write(']');
    }

    private char parse(OperatorTree.Op op) {
        return switch (op) {
            case SUB -> '-';
            case LS -> '<';
            case EQ -> '=';
            case GT -> '>';
            case OR -> '|';
            case ADD -> '+';
            case AND -> '&';
            case DIV -> '/';
            case MUL -> '*';
            case NOT -> '~';
        };
    }

    private static Keyword parse(KeywordConstantTree.Keyword keyword) {
        return switch (keyword) {
            case THIS -> Keyword.THIS;
            case NULL -> Keyword.NULL;
            case TRUE -> Keyword.TRUE;
            case FALSE -> Keyword.FALSE;
        };
    }

    private void writeSubroutineDeclaration(AbstractSyntaxTree node) {
        final SubroutineDeclarationTree subroutineDeclarationTree = (SubroutineDeclarationTree) node;

        openBlock("subroutineDec");
        write(parse(subroutineDeclarationTree.subroutineType));
        writeType(subroutineDeclarationTree);
        write(subroutineDeclarationTree.name);

        write('(');
        openBlock("parameterList");
        writeParameterList(subroutineDeclarationTree.parameterList);
        closeBlock("parameterList");
        write(')');

        handleSyntaxTree(subroutineDeclarationTree.subroutineBodyTree);
        closeBlock("subroutineDec");
    }

    private void writeSubroutineBody(AbstractSyntaxTree node) {
        openBlock("subroutineBody");
        write('{');


        final SubroutineBodyTree subroutineBodyTree = (SubroutineBodyTree) node;
        if (subroutineBodyTree.nodes != null) {
            int i = 0;
            while (
                    i < subroutineBodyTree.nodes.size()
                            && subroutineBodyTree.nodes.get(i).getNodeKind() == NodeKind.VAR_DECLARATION
            ) {
                handleSyntaxTree(subroutineBodyTree.nodes.get(i++));
            }

            openBlock("statements");
            for (; i < subroutineBodyTree.nodes.size(); i++) {
                handleSyntaxTree(subroutineBodyTree.nodes.get(i));
            }
            closeBlock("statements");
        }


        write('}');
        closeBlock("subroutineBody");
    }

    private void writeVarDeclaration(AbstractSyntaxTree node) {
        final ClassVarDeclarationTree classVarDeclarationTree = (ClassVarDeclarationTree) node;
        openBlock("classVarDec");
        write(classVarDeclarationTree.declarationType == ClassVarDeclarationTree.DeclarationType.STATIC ? Keyword.STATIC : Keyword.FIELD);

        writeType(classVarDeclarationTree);

        writeDeclarationVars(classVarDeclarationTree.varNames);
        write(';');

        closeBlock("classVarDec");
    }

    private void writeDeclarationVars(List<String> varNames) {
        int i = 0;
        while (i < varNames.size() - 1) {
            write(varNames.get(i++));
            write(',');
        }
        write(varNames.get(i));
    }

    private void writeSubroutineCall(SubroutineCallTree node) {
        //see edu/nadn2tetris/ast/term/subroutine/SubroutineCallTree.java:10
        final String[] identifiers = node.identifierTree.split("\\.");
        write(identifiers[0]);
        if (identifiers.length == 2) {
            write('.');
            write(identifiers[1]);
        }

        write('(');
        writeExpressionList(node.argList);
        write(')');
    }

    private void writeClass(AbstractSyntaxTree node) {
        openBlock("class");
        final ClassTree classNode = (ClassTree) node;
        write(Keyword.CLASS);
        write(classNode.className);
        write('{');
        for (AbstractSyntaxTree tree : classNode.blocks) {
            handleSyntaxTree(tree);
        }
        write('}');
        closeBlock("class");
    }

    private Keyword parse(SubroutineDeclarationTree.SubroutineType subroutineType) {
        if (subroutineType == SubroutineDeclarationTree.SubroutineType.CONSTRUCTOR) {
            return Keyword.CONSTRUCTOR;
        }

        if (subroutineType == SubroutineDeclarationTree.SubroutineType.METHOD) {
            return Keyword.METHOD;
        }

        return Keyword.FUNCTION;
    }

    private void openBlock(String block) {
        xml.append("%s<%s>\n".formatted(TAB_SYMBOL.repeat(nestingLevel++), block));
    }

    private void closeBlock(String block) {
        xml.append("%s</%s>\n".formatted(TAB_SYMBOL.repeat(--nestingLevel >= 0 ? nestingLevel : 0), block));
    }

    private void writeParameterList(List<ParameterTree> parameterList) {
        if (parameterList == null || parameterList.isEmpty()) {
            return;
        }

        int i = 0;
        while (i < parameterList.size() - 1) {
            ParameterTree parameterTree = parameterList.get(i++);
            writeType(parameterTree);
            write(parameterTree.name);
            write(',');
        }

        ParameterTree parameterTree = parameterList.get(i);
        writeType(parameterTree);
        write(parameterTree.name);
    }

    private void writeType(ATypedTree aTypedTree) {
        final Type type = aTypedTree.type;
        if (type == Type.CLASS) {
            write(aTypedTree.classNameType);
        } else {
            write(Keyword.parse(type.getCodeValue()));
        }
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
