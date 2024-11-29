package edu.nadn2tetris.ast.processor;

import java.io.Closeable;
import java.io.IOException;
import java.util.List;

import javax.swing.Spring;

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
import edu.nadn2tetris.table.FileSymbolTable;
import edu.nadn2tetris.table.IdentifierInfo;
import edu.nadn2tetris.table.Kind;
import edu.nadn2tetris.writer.Segment;
import edu.nadn2tetris.writer.VMWriter;

public final class ByteCodeAstGenerator implements AstGenerator<Boolean>, Closeable {
    private final FileSymbolTable symbolTable;
    private final VMWriter vmWriter;

    public ByteCodeAstGenerator(FileSymbolTable symbolTable, VMWriter vmWriter) {
        this.symbolTable = symbolTable;
        this.vmWriter = vmWriter;
    }

    @Override
    public Boolean generate(AbstractSyntaxTree root) {
        handleSyntaxTree(root);
        return true;
    }

    private void handleSyntaxTree(AbstractSyntaxTree node) {
        switch (node.getNodeKind()) {
            case CLASS -> writeClass(node);
            case SUBROUTINE_DECLARATION -> writeSubroutineDeclaration(node);
            case LET_STATEMENT -> writeLet((LetStatementTree) node);
            case DO_STATEMENT -> writeDoStatement(node);
            case IF_STATEMENT -> writeIfStatement((IfStatementTree) node);
            case WHILE_STATEMENT -> writeWhileStatement((WhileStatementTree) node);
            case RETURN_STATEMENT -> writeReturnStatement((ReturnStatementTree) node);
            default -> throw new IllegalStateException("Unexpected node kind: " + node.getNodeKind());
        }
    }

    private void writeClass(AbstractSyntaxTree node) {
        final ClassTree classNode = (ClassTree) node;
        if (classNode.blocks == null || classNode.blocks.isEmpty()) {
            return;
        }

        classNode.blocks.stream()
                .filter(n -> n.getNodeKind() == NodeKind.SUBROUTINE_DECLARATION)
                .forEach(this::handleSyntaxTree);
    }

    private void writeSubroutineDeclaration(AbstractSyntaxTree node) {
        final SubroutineDeclarationTree subroutineDeclarationTree = (SubroutineDeclarationTree) node;

        final String methodName = subroutineDeclarationTree.name;
        final List<ParameterTree> params = subroutineDeclarationTree.parameterList;
        vmWriter.writeFunction(methodName, params == null ? 0 : params.size());

        final SubroutineDeclarationTree.SubroutineType functionType = subroutineDeclarationTree.subroutineType;
        if (functionType == SubroutineDeclarationTree.SubroutineType.METHOD) {
            vmWriter.writePush(Segment.ARGUMENT, (short) 0);
            vmWriter.writePop(Segment.POINTER, (short) 0);
        }

        final SubroutineBodyTree body = subroutineDeclarationTree.subroutineBodyTree;
        if (functionType == SubroutineDeclarationTree.SubroutineType.CONSTRUCTOR) {
            vmWriter.writePush(Segment.CONSTANT, symbolTable.getClassFieldsCount());
            vmWriter.writeCall("Memory.alloc", 1);
            vmWriter.writePop(Segment.POINTER, (short) 0);
            writeConstructorBody(body);
        } else {
            writeFunctionBody(methodName, body);
        }
    }

    private void handleStatement(AbstractSyntaxTree node, String methodName) {
        switch (node.getNodeKind()) {
            case LET_STATEMENT -> writeLet((LetStatementTree) node, methodName);
            case DO_STATEMENT -> writeDoStatement(node);
            case IF_STATEMENT -> writeIfStatement((IfStatementTree) node);
            case WHILE_STATEMENT -> writeWhileStatement((WhileStatementTree) node);
            case RETURN_STATEMENT -> writeReturnStatement((ReturnStatementTree) node);
            default -> throw new IllegalStateException("Unexpected node kind: " + node.getNodeKind());
        }
    }

    private void writeLet(LetStatementTree node, String methodName) {
        final IdentifierInfo identifierInfo = symbolTable.get(methodName, node.identifierName);
        if (identifierInfo == null) {
            throw new IllegalStateException("Undefined identifier: " + node.identifierName);
        }

        identifierInfo.kind == Kind.FIELD;
    }

    private void writeConstructorBody(SubroutineBodyTree bodyTree) {
        final String methodName = "new";

        writeStatements(methodName, bodyTree.nodes);
        vmWriter.writePush(Segment.POINTER, (short) 0);
        vmWriter.writeReturn();
    }

    private void writeFunctionBody(String methodName, SubroutineBodyTree bodyTree) {
        writeStatements(methodName, bodyTree.nodes);
    }

    private void writeStatements(String methodName, List<AbstractSyntaxTree> statements) {
        if (statements == null || statements.isEmpty()) {
            return;
        }

        for (AbstractSyntaxTree node : statements) {
            handleStatement(node, methodName);
        }
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

    private Keyword parse(SubroutineDeclarationTree.SubroutineType subroutineType) {
        if (subroutineType == SubroutineDeclarationTree.SubroutineType.CONSTRUCTOR) {
            return Keyword.CONSTRUCTOR;
        }

        if (subroutineType == SubroutineDeclarationTree.SubroutineType.METHOD) {
            return Keyword.METHOD;
        }

        return Keyword.FUNCTION;
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
        bytecode.append("%s<keyword> %s </keyword>\n".formatted(TAB_SYMBOL.repeat(nestingLevel), keyword.name().toLowerCase()));
    }

    private void write(String identifier) {
        bytecode.append("%s<identifier> %s </identifier>\n".formatted(TAB_SYMBOL.repeat(nestingLevel), identifier));
    }

    private void write(char symbol) {
        bytecode.append("%s<symbol> %s </symbol>\n".formatted(TAB_SYMBOL.repeat(nestingLevel), symbol));
    }

    private void write(short intConst) {
        bytecode.append("%s<integerConstant> %d </integerConstant>\n".formatted(TAB_SYMBOL.repeat(nestingLevel), intConst));
    }

    private void wrapStringConst(String stringConst) {
        bytecode.append("%s<stringConstant> %s </stringConstant>\n".formatted(TAB_SYMBOL.repeat(nestingLevel), stringConst));
    }

    @Override
    public void close() throws IOException {
        vmWriter.close();
    }
}
