package edu.nadn2tetris.ast.processor;

import java.io.Closeable;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

import edu.nadn2tetris.ast.AbstractSyntaxTree;
import edu.nadn2tetris.ast.ClassTree;
import edu.nadn2tetris.ast.NodeKind;
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
import edu.nadn2tetris.table.FileSymbolTable;
import edu.nadn2tetris.table.IdentifierInfo;
import edu.nadn2tetris.table.Kind;
import edu.nadn2tetris.writer.Command;
import edu.nadn2tetris.writer.Segment;
import edu.nadn2tetris.writer.VMWriter;

public final class ByteCodeAstGenerator implements AstGenerator<Boolean>, Closeable {
    private final FileSymbolTable symbolTable;
    private final VMWriter vmWriter;
    private final Map<String, Short> labelIndex = new HashMap<>();

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
            case CLASS -> compileClass(node);
            case SUBROUTINE_DECLARATION -> compileSubroutineDeclaration(node);
            default -> throw new IllegalStateException("Unexpected node kind: " + node.getNodeKind());
        }
    }

    private void compileClass(AbstractSyntaxTree node) {
        final ClassTree classNode = (ClassTree) node;
        if (classNode.blocks == null || classNode.blocks.isEmpty()) {
            return;
        }

        classNode.blocks.stream()
                .filter(n -> n.getNodeKind() == NodeKind.SUBROUTINE_DECLARATION)
                .forEach(this::handleSyntaxTree);
    }

    private void compileSubroutineDeclaration(AbstractSyntaxTree node) {
        final SubroutineDeclarationTree subroutineDeclarationTree = (SubroutineDeclarationTree) node;

        final String methodName = subroutineDeclarationTree.name;
        final List<ParameterTree> params = subroutineDeclarationTree.parameterList;
        vmWriter.writeFunction(methodName, symbolTable.getMethodVarCount(methodName) + (params == null ? 0 : params.size()));

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
            compileFunctionBody(methodName, body);
        }
    }

    private void compileStatement(AbstractSyntaxTree node, String methodName) {
        switch (node.getNodeKind()) {
            case LET_STATEMENT -> compileLet((LetStatementTree) node, methodName);
            case DO_STATEMENT -> compileDo((DoStatementTree) node, methodName);
            case IF_STATEMENT -> compileIfStatement((IfStatementTree) node, methodName);
            case WHILE_STATEMENT -> compileWhileStatement((WhileStatementTree) node, methodName);
            case RETURN_STATEMENT -> compileReturnStatement((ReturnStatementTree) node, methodName);
            default -> throw new IllegalStateException("Unexpected node kind: " + node.getNodeKind());
        }
    }

    private void compileWhileStatement(WhileStatementTree node, String methodName) {
        final String labelPrefix = methodName + "_while";
        final short suffix = getLabelIndex(labelPrefix);

        final String whileBodyStart = labelPrefix + "_start_" + suffix;
        final String whileBodyEnd = labelPrefix + "_end_" + suffix;
        final String conditionCheck = labelPrefix + "_check_" + suffix;

        // check conditional
        vmWriter.writeLabel(conditionCheck);
        compileExpression(node.condition, methodName);
        vmWriter.writeIf(whileBodyStart);
        vmWriter.writeGoto(whileBodyEnd);

        vmWriter.writeLabel(whileBodyStart);
        compileStatements(node.body, methodName);
        vmWriter.writeGoto(conditionCheck);

        vmWriter.writeLabel(whileBodyEnd);
    }

    private void compileIfStatement(IfStatementTree node, String methodName) {
        final String labelPrefix = methodName + "_if";
        final short ifSuffix = getLabelIndex(labelPrefix);
        final String ifLabel = labelPrefix + "_" + ifSuffix;

        compileExpression(node.condition, methodName); // result on stack
        vmWriter.writeIf(ifLabel);
        compileStatements(node.elseBody, methodName);

        final String endIfLabel = labelPrefix + "_end_" + ifSuffix;
        vmWriter.writeGoto(endIfLabel);

        vmWriter.writeLabel(ifLabel);
        compileStatements(node.ifBody, methodName);

        vmWriter.writeLabel(endIfLabel);
    }

    private void compileStatements(List<StatementTree> statementTrees, String methodName) {
        if (statementTrees == null || statementTrees.isEmpty()) {
            return;
        }

        for (StatementTree statementTree : statementTrees) {
            compileStatement(statementTree, methodName);
        }
    }

    private void compileReturnStatement(ReturnStatementTree node, String methodName) {
        if (node.expression != null) {
            compileExpression(node.expression, methodName);
        }
        vmWriter.writeReturn();
    }

    private void compileDo(DoStatementTree node, String methodName) {
        compileExpression(node.subroutineCallTree, methodName);
        vmWriter.writePop(Segment.TEMP, (short) 0);
    }

    private void compileLet(LetStatementTree node, String methodName) {
        final IdentifierInfo identifierInfo = getIdentifierInfo(methodName, node.identifierName);
        final Segment segment = getSegment(identifierInfo.kind);
        final boolean isArray = node.arraySyntaxTree != null;
        if (isArray) {
            vmWriter.writePush(segment, identifierInfo.index);
            compileExpression(node.arraySyntaxTree.expressionTree, methodName);
            vmWriter.writeArithmetic(Command.ADD);
            vmWriter.writePop(Segment.TEMP, (short) 0); // save addr
            compileExpression(node.expression, methodName);

            vmWriter.writePush(Segment.TEMP, (short) 0); // get arrayAddr
            vmWriter.writePop(Segment.POINTER, (short) 1);
            vmWriter.writePop(Segment.THAT, (short) 0); //save expression result
            return;
        }

        compileExpression(node.expression, methodName);
        vmWriter.writePop(segment, identifierInfo.index);
    }

    private void compileExpression(ATermSyntaxTree expression, String methodName) {
        final Stack<ATermSyntaxTree> terms = new Stack<>();
        terms.add(expression);

        final Set<AbstractSyntaxTree> visited = new HashSet<>();
        while (!terms.isEmpty()) {
            final ATermSyntaxTree currTerm = terms.pop();
            boolean handleCurrent = (currTerm.left == null || visited.contains(currTerm.left)) && (currTerm.right == null || visited.contains(currTerm.right));
            if (handleCurrent) {
                compileTerm(currTerm, methodName);
                visited.add(currTerm);
                continue;
            }

            if (!visited.contains(currTerm)) {
                terms.add(currTerm);
            }

            if (currTerm.right != null && !visited.contains(currTerm.right)) {
                terms.add(currTerm.right);
            }

            if (currTerm.left != null && !visited.contains(currTerm.left)) {
                terms.add(currTerm.left);
            }
        }
    }

    private void compileTerm(ATermSyntaxTree termSyntaxTree, String methodName) {
        if (termSyntaxTree.expression) {
            termSyntaxTree.expression = false;
            compileExpression(termSyntaxTree, methodName);
            termSyntaxTree.expression = true;
            return;
        }

        switch (termSyntaxTree.getNodeKind()) {
            case IDENTIFIER -> compileIdentifier((IdentifierTree) termSyntaxTree, methodName);
            case INTEGER_CONSTANT -> compileIntegerConstant((IntegerConstantTree) termSyntaxTree);
            case STRING_CONSTANT -> compileStringConstant((StringConstantTree) termSyntaxTree);
            case KEYWORD -> compileKeyword((KeywordConstantTree) termSyntaxTree);
            case OP -> compileOperation((OperatorTree) termSyntaxTree);
            case SUBROUTINE_CALL -> compileSubroutineCall((SubroutineCallTree) termSyntaxTree, methodName);
            case ARRAY -> compileArray((ArraySyntaxTree) termSyntaxTree, methodName);
        }
    }

    private void compileStringConstant(StringConstantTree termSyntaxTree) {
        vmWriter.writePush(Segment.CONSTANT, (short) termSyntaxTree.value.length());
        vmWriter.writeCall("String.new", 1);
        for (char ch : termSyntaxTree.value.toCharArray()) {
            vmWriter.writePush(Segment.CONSTANT, (short) ch);
            vmWriter.writeCall("String.appendChar", 2);
        }
    }

    private void compileArray(ArraySyntaxTree termSyntaxTree, String methodName) {
        final IdentifierInfo identifierInfo = getIdentifierInfo(methodName, termSyntaxTree.identifierName);
        vmWriter.writePush(getSegment(identifierInfo.kind), identifierInfo.index);
        compileExpression(termSyntaxTree.expressionTree, methodName);
        vmWriter.writeArithmetic(Command.ADD);
        vmWriter.writePop(Segment.POINTER, (short) 1); // set that addr
        vmWriter.writePush(Segment.THAT, (short) 0); // push that data
    }

    private void compileSubroutineCall(SubroutineCallTree termSyntaxTree, String methodName) {
        final String[] split = termSyntaxTree.identifierName.split("\\.");

        final String identifierName;
        final boolean systemCall = split.length > 1;
        if (systemCall) {
            final IdentifierInfo identifierInfo = symbolTable.get(methodName, split[0]);
            if (identifierInfo != null) { // method call
                identifierName = split[1];
                vmWriter.writePush(getSegment(identifierInfo.kind), identifierInfo.index);
            } else { //system.call
                identifierName = termSyntaxTree.identifierName;
            }
        } else { // this method call
            vmWriter.writePush(Segment.POINTER, (short) 0); // set this on stack
            identifierName = split[0];
        }

        final boolean hasArgs = termSyntaxTree.argList != null && !termSyntaxTree.argList.isEmpty();
        final int paramN = hasArgs ? termSyntaxTree.argList.size() : 0;
        if (hasArgs) {
            for (ATermSyntaxTree param : termSyntaxTree.argList) {
                compileExpression(param, methodName);
            }
        }

        vmWriter.writeCall(identifierName, systemCall ? paramN : paramN + 1);
    }

    private void compileKeyword(KeywordConstantTree termSyntaxTree) {
        if (termSyntaxTree.value == KeywordConstantTree.Keyword.THIS) {
            vmWriter.writePush(Segment.POINTER, (short) 0);
        }

        if (termSyntaxTree.value == KeywordConstantTree.Keyword.TRUE) {
            vmWriter.writePush(Segment.CONSTANT, (short) 0);
        }

        if (termSyntaxTree.value == KeywordConstantTree.Keyword.FALSE) {
            vmWriter.writePush(Segment.CONSTANT, (short) 1);
        }

        if (termSyntaxTree.value == KeywordConstantTree.Keyword.NULL) {
            vmWriter.writePush(Segment.CONSTANT, (short) 0); // TODO: Уточнить спецификацию
        }
    }

    private void compileOperation(OperatorTree termSyntaxTree) {
        if (termSyntaxTree.value == OperatorTree.Op.MUL) {
            compileMul();
            return;
        }

        if (termSyntaxTree.value == OperatorTree.Op.DIV) {
            compileDiv();
            return;
        }

        vmWriter.writeArithmetic(parse(termSyntaxTree.value));
    }

    private void compileDiv() {
        final String methodName = "div";
        final short index = getLabelIndex(methodName);

        vmWriter.writePop(Segment.TEMP, (short) 0); // left operant value
        vmWriter.writePop(Segment.TEMP, (short) 1); // right operant value

        vmWriter.writePush(Segment.TEMP, (short) 0);
        vmWriter.writePop(Segment.TEMP, (short) 2); // curr value

        vmWriter.writePush(Segment.CONSTANT, (short) 0);
        vmWriter.writePop(Segment.TEMP, (short) 3); // res

        final String conditionLabel = methodName + "_con_" + index;
        final String endLabel = methodName + "_end_" + index;
        final String start = methodName + "_start_" + index;

        vmWriter.writeLabel(conditionLabel);
        vmWriter.writePush(Segment.TEMP, (short) 2);
        vmWriter.writePush(Segment.CONSTANT, (short) 0);
        vmWriter.writeArithmetic(Command.GT);
        vmWriter.writeIf(start);
        vmWriter.writeGoto(endLabel);

        vmWriter.writeLabel(start);
        vmWriter.writePush(Segment.TEMP, (short) 2);
        vmWriter.writePush(Segment.TEMP, (short) 1);
        vmWriter.writeArithmetic(Command.SUB);
        vmWriter.writePop(Segment.TEMP, (short) 2);

        vmWriter.writePush(Segment.TEMP, (short) 3);
        vmWriter.writePush(Segment.CONSTANT, (short) 1);
        vmWriter.writeArithmetic(Command.ADD);
        vmWriter.writePop(Segment.TEMP, (short) 3);

        vmWriter.writeGoto(conditionLabel);

        vmWriter.writeLabel(endLabel);
        vmWriter.writePush(Segment.TEMP, (short) 3); // result on stack
    }

    private short getLabelIndex(String labelPrefix) {
        labelIndex.compute(labelPrefix, (k, i) -> {
            if (i == null) {
                return (short) 0;
            }

            return (short) (i + 1);
        });

        return labelIndex.get(labelPrefix);
    }

    private void compileMul() {
        final String methodName = "mul";
        labelIndex.compute(methodName, (k, i) -> {
            if (i == null) {
                return (short) 0;
            }

            return (short) (i + 1);
        });

        final short suffix = labelIndex.get(methodName);
        vmWriter.writePop(Segment.TEMP, (short) 0); // left operant value
        vmWriter.writePop(Segment.TEMP, (short) 1); // right operant value

        vmWriter.writePush(Segment.CONSTANT, (short) 0);
        vmWriter.writePop(Segment.TEMP, (short) 2); // curr iteration

        vmWriter.writePush(Segment.CONSTANT, (short) 0);
        vmWriter.writePop(Segment.TEMP, (short) 3); // res

        final String conditionLabel = methodName + "_con_" + suffix;
        final String endLabel = methodName + "_end_" + suffix;
        final String start = methodName + "_start_" + suffix;

        vmWriter.writeLabel(conditionLabel);
        vmWriter.writePush(Segment.TEMP, (short) 2);
        vmWriter.writePush(Segment.TEMP, (short) 1);
        vmWriter.writeArithmetic(Command.LT);
        vmWriter.writeIf(start);
        vmWriter.writeGoto(endLabel);

        vmWriter.writeLabel(start);
        vmWriter.writePush(Segment.TEMP, (short) 0);
        vmWriter.writePush(Segment.TEMP, (short) 0);
        vmWriter.writeArithmetic(Command.ADD);
        vmWriter.writePop(Segment.TEMP, (short) 3);

        vmWriter.writePush(Segment.CONSTANT, (short) 1);
        vmWriter.writePush(Segment.TEMP, (short) 2);
        vmWriter.writeArithmetic(Command.ADD);
        vmWriter.writePop(Segment.TEMP, (short) 2);

        vmWriter.writeGoto(conditionLabel);

        vmWriter.writeLabel(endLabel);
        vmWriter.writePush(Segment.TEMP, (short) 3); // result on stack
    }

    public Command parse(OperatorTree.Op op) {
        return switch (op) {
            case ADD -> Command.ADD;
            case SUB -> Command.SUB;
            case NOT -> Command.NOT;
            case EQ -> Command.EQ;
            case GT -> Command.GT;
            case LS -> Command.LT;
            case AND -> Command.AND;
            case OR -> Command.OR;
            default -> throw new IllegalStateException("Unexpected value: " + op);
        };
    }

    private void compileIntegerConstant(IntegerConstantTree termSyntaxTree) {
        vmWriter.writePush(Segment.CONSTANT, termSyntaxTree.value);
    }

    private void compileIdentifier(IdentifierTree termSyntaxTree, String methodName) {
        final IdentifierInfo identifierInfo = getIdentifierInfo(methodName, termSyntaxTree.varName);

        vmWriter.writePush(getSegment(identifierInfo.kind), identifierInfo.index);
    }

    private Segment getSegment(Kind kind) {
        if (kind == Kind.FIELD) {
            return Segment.POINTER;
        }

        if (kind == Kind.STATIC) {
            return Segment.STATIC;
        }

        if (kind == Kind.ARG) {
            return Segment.ARGUMENT;
        }

        if (kind == Kind.VAR) {
            return Segment.LOCAL;
        }

        throw new IllegalStateException("Unexpected kind: " + kind);
    }

    private void writeConstructorBody(SubroutineBodyTree bodyTree) {
        final String methodName = "new";

        final List<StatementTree> statements = bodyTree.nodes.stream()
                .filter(n -> n instanceof StatementTree)
                .map(n -> (StatementTree) n).toList();

        compileStatements(statements, methodName);

        vmWriter.writePush(Segment.POINTER, (short) 0);
        vmWriter.writeReturn();
    }

    private void compileFunctionBody(String methodName, SubroutineBodyTree bodyTree) {
        final List<StatementTree> statements = bodyTree.nodes.stream()
                .filter(n -> n instanceof StatementTree)
                .map(n -> (StatementTree) n).toList();

        compileStatements(statements, methodName);
    }

    private IdentifierInfo getIdentifierInfo(String methodName, String identifierName) {
        final IdentifierInfo identifierInfo = symbolTable.get(methodName, identifierName);
        if (identifierInfo == null) {
            throw new IllegalStateException("Undefined identifier: " + identifierName);
        }

        return identifierInfo;
    }

    @Override
    public void close() throws IOException {
        vmWriter.close();
    }
}
