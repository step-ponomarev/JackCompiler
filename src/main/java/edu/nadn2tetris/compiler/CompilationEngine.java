package edu.nadn2tetris.compiler;

import java.io.Closeable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import edu.nadn2tetris.ast.AbstractSyntaxTree;
import edu.nadn2tetris.ast.ClassTree;
import edu.nadn2tetris.ast.term.ATermSyntaxTree;
import edu.nadn2tetris.ast.declaration.ClassVarDeclarationTree;
import edu.nadn2tetris.ast.declaration.VarDeclarationTree;
import edu.nadn2tetris.ast.statement.DoStatementTree;
import edu.nadn2tetris.ast.statement.IfStatementTree;
import edu.nadn2tetris.ast.statement.LetStatementTree;
import edu.nadn2tetris.ast.statement.ReturnStatementTree;
import edu.nadn2tetris.ast.statement.StatementTree;
import edu.nadn2tetris.ast.statement.WhileStatementTree;
import edu.nadn2tetris.ast.term.subroutine.SubroutineBodyTree;
import edu.nadn2tetris.ast.term.subroutine.SubroutineDeclarationTree;
import edu.nadn2tetris.ast.Type;
import edu.nadn2tetris.ast.term.ArraySyntaxTree;
import edu.nadn2tetris.ast.term.OperatorTree;
import edu.nadn2tetris.ast.term.IdentifierTree;
import edu.nadn2tetris.ast.term.IntegerConstantTree;
import edu.nadn2tetris.ast.term.KeywordConstantTree;
import edu.nadn2tetris.ast.term.ParameterTree;
import edu.nadn2tetris.ast.term.StringConstantTree;
import edu.nadn2tetris.ast.term.subroutine.SubroutineCallTree;
import edu.nadn2tetris.common.Keyword;
import edu.nadn2tetris.common.TokenType;
import edu.nadn2tetris.tokenizer.JackTokenizer;

/**
 * Creates AST tree
 */
public final class CompilationEngine implements Closeable {
    private final JackTokenizer tokenizer;
    private boolean hasBufferedToken;

    public CompilationEngine(JackTokenizer tokenizer) {
        this.tokenizer = tokenizer;
    }

    public AbstractSyntaxTree compileClass() {
        advance(); // to class

        if (tokenizer.tokenType() != TokenType.KEYWORD || tokenizer.keyword() != Keyword.CLASS) {
            throwUnexpectedToken(tokenizer);
        }
        advance(); // skip class

        final ClassTree classTree = new ClassTree();
        classTree.className = tokenizer.identifier();
        classTree.blocks = new ArrayList<>();
        advance(); // skip className

        advance(); // skip {
        // empty class
        while (tokenizer.hasMoreTokens()) {
            if (!isClassBlock(tokenizer)) {
                throwUnexpectedToken(tokenizer);
            }

            if (isClassVarDec(tokenizer.keyword())) {
                classTree.blocks.add(compileClassVarDec());
                advance();
            } else if (isSubroutineDec(tokenizer.keyword())) {
                classTree.blocks.add(compileSubroutine());
                advance();
            } else {
                throwUnexpectedToken(tokenizer);
            }
        }

        return classTree;
    }

    private static boolean isClassBlock(JackTokenizer tokenizer) {
        if (tokenizer.tokenType() != TokenType.KEYWORD) {
            return false;
        }

        if (isSubroutineDec(tokenizer.keyword())) {
            return true;
        }

        return isClassVarDec(tokenizer.keyword());
    }

    private static boolean isSubroutineDec(Keyword keyword) {
        return keyword == Keyword.CONSTRUCTOR || keyword == Keyword.FUNCTION || keyword == Keyword.METHOD;
    }

    private static boolean isClassVarDec(Keyword keyword) {
        return keyword == Keyword.STATIC || keyword == Keyword.FIELD;
    }

    private ClassVarDeclarationTree compileClassVarDec() {
        if (tokenizer.tokenType() != TokenType.KEYWORD || !isClassVarDec(tokenizer.keyword())) {
            throwUnexpectedToken(tokenizer);
        }

        final ClassVarDeclarationTree classVarDeclarationTree = new ClassVarDeclarationTree();
        classVarDeclarationTree.declarationType = tokenizer.keyword() == Keyword.STATIC ? ClassVarDeclarationTree.DeclarationType.STATIC : ClassVarDeclarationTree.DeclarationType.FIELD;

        advance(); // skip field or static
        classVarDeclarationTree.type = getType(tokenizer);
        if (classVarDeclarationTree.type == Type.CLASS) {
            classVarDeclarationTree.classNameType = tokenizer.identifier();
        }

        advance(); //skip type
        classVarDeclarationTree.varNames = getVarNamesList();

        return classVarDeclarationTree;
    }

    private SubroutineDeclarationTree compileSubroutine() {
        if (!isSubroutineDec(tokenizer.keyword())) {
            throwUnexpectedToken(tokenizer);
        }

        final SubroutineDeclarationTree subroutineDeclarationTree = new SubroutineDeclarationTree();
        subroutineDeclarationTree.subroutineType = getSubroutineDeclarationType(tokenizer.keyword());

        advance(); // skip constructor or method or function
        subroutineDeclarationTree.type = getType(tokenizer);
        if (subroutineDeclarationTree.type == Type.CLASS) {
            subroutineDeclarationTree.classNameType = tokenizer.identifier();
        }

        advance(); // skip type
        subroutineDeclarationTree.name = tokenizer.identifier();

        advance(); // skip name
        subroutineDeclarationTree.parameterList = compileParameterList();

        advance(); // skip )
        subroutineDeclarationTree.subroutineBodyTree = compileSubroutineBody();

        return subroutineDeclarationTree;
    }

    private static SubroutineDeclarationTree.SubroutineType getSubroutineDeclarationType(Keyword keyword) {
        return keyword == Keyword.CONSTRUCTOR
                ? SubroutineDeclarationTree.SubroutineType.CONSTRUCTOR
                : keyword == Keyword.METHOD
                ? SubroutineDeclarationTree.SubroutineType.METHOD
                : SubroutineDeclarationTree.SubroutineType.FUNCTION;
    }

    private SubroutineBodyTree compileSubroutineBody() {
        if (tokenizer.tokenType() != TokenType.SYMBOL || tokenizer.symbol() != '{') {
            throwUnexpectedToken(tokenizer);
        }

        advance(); // skip {
        // empty method
        if (tokenizer.tokenType() == TokenType.SYMBOL && tokenizer.symbol() == '}') {
            return new SubroutineBodyTree(new ArrayList<>());
        }

        final SubroutineBodyTree subroutineBodyTree = new SubroutineBodyTree();
        subroutineBodyTree.nodes = new ArrayList<>();
        // var declarations on top
        while (tokenizer.tokenType() == TokenType.KEYWORD && tokenizer.keyword() == Keyword.VAR) {
            subroutineBodyTree.nodes.add(compileVarDec());
            advance();
        }

        subroutineBodyTree.nodes.addAll(compileStatements());
        advance(); // to }

        return subroutineBodyTree;
    }

    private List<ParameterTree> compileParameterList() {
        if (tokenizer.tokenType() != TokenType.SYMBOL || tokenizer.symbol() != '(') {
            throwUnexpectedToken(tokenizer);
        }

        final List<ParameterTree> parameterTrees = new ArrayList<>();
        advance(); // (
        if (tokenizer.tokenType() == TokenType.SYMBOL && tokenizer.symbol() == ')') {
            return Collections.emptyList();
        } else {
            bufferToken();
        }

        while (tokenizer.tokenType() != TokenType.SYMBOL || tokenizer.symbol() != ')') {
            advance(); // skip ( or ,
            ParameterTree parameterTree = new ParameterTree();
            parameterTree.type = getType(tokenizer);
            if (parameterTree.type == Type.CLASS) {
                parameterTree.classNameType = tokenizer.identifier();
            }

            advance();
            parameterTree.name = tokenizer.identifier();
            advance();

            parameterTrees.add(parameterTree);
        }

        return parameterTrees;
    }

    private VarDeclarationTree compileVarDec() {
        if (tokenizer.tokenType() != TokenType.KEYWORD || tokenizer.keyword() != Keyword.VAR) {
            throw new IllegalStateException("Unexpected token: " + tokenizer.tokenType());
        }
        advance(); // skip var

        final VarDeclarationTree varDeclarationTree = new VarDeclarationTree();
        varDeclarationTree.type = getType(tokenizer);
        varDeclarationTree.varNames = new ArrayList<>();

        if (varDeclarationTree.type == Type.CLASS) {
            varDeclarationTree.classNameType = tokenizer.identifier();
        }

        advance();
        varDeclarationTree.varNames.addAll(getVarNamesList());

        return varDeclarationTree;
    }

    private List<String> getVarNamesList() {
        if (tokenizer.tokenType() != TokenType.IDENTIFIER) {
            throw new IllegalStateException("Unexpected token: " + tokenizer.tokenType());
        }

        final List<String> varNames = new ArrayList<>();
        varNames.add(tokenizer.identifier());
        advance();
        while (tokenizer.symbol() != ';') {
            advance(); // skip ,
            varNames.add(tokenizer.identifier());
            advance();
        }

        return varNames;
    }

    private List<StatementTree> compileStatements() {
        // empty statements block
        if (!isStatement(tokenizer)) {
            bufferToken();
            return Collections.emptyList();
        }

        final List<StatementTree> statementTrees = new ArrayList<>();
        while (isStatement(tokenizer)) {
            statementTrees.add(compileStatement());
            advance();
        }
        bufferToken();

        return statementTrees;
    }

    private StatementTree compileStatement() {
        if (!isStatement(tokenizer)) {
            throwUnexpectedToken(tokenizer);
        }

        return switch (tokenizer.keyword()) {
            case LET -> compileLet();
            case IF -> compileIf();
            case WHILE -> compileWhile();
            case DO -> compileDo();
            case RETURN -> compileReturn();
            default -> throw new IllegalStateException("Unsupported type " + tokenizer.keyword());
        };
    }

    private LetStatementTree compileLet() {
        if (tokenizer.tokenType() != TokenType.KEYWORD || tokenizer.keyword() != Keyword.LET) {
            throwUnexpectedToken(tokenizer);
        }

        advance(); // skip let
        final LetStatementTree letStatementTree = new LetStatementTree();
        letStatementTree.identifierName = tokenizer.identifier();

        advance(); // skip identifier
        final boolean array = tokenizer.symbol() == '[';
        if (array) {
            advance(); // skip [
            letStatementTree.arraySyntaxTree = new ArraySyntaxTree(letStatementTree.identifierName, compileExpression());

            advance(); // next
            advance(); // skip ]
        }

        advance(); // skip =
        letStatementTree.assigment = compileExpression();
        advance(); // next

        return letStatementTree;
    }

    private IfStatementTree compileIf() {
        if (tokenizer.tokenType() != TokenType.KEYWORD || tokenizer.keyword() != Keyword.IF) {
            throwUnexpectedToken(tokenizer);
        }

        advance(); // skip if
        advance(); // skip (

        IfStatementTree ifStatementTree = new IfStatementTree();
        ifStatementTree.condition = compileExpression();

        advance(); // to )
        advance(); // skip )
        advance(); // skip {
        ifStatementTree.ifBody = compileStatements();
        advance(); // to }
        advance(); // skip }

        if (tokenizer.tokenType() != TokenType.KEYWORD || tokenizer.keyword() != Keyword.ELSE) {
            bufferToken();
            return ifStatementTree;
        }

        advance(); // skip else
        advance(); // skip {
        ifStatementTree.elseBody = compileStatements();
        advance(); // skip }

        return ifStatementTree;
    }

    private WhileStatementTree compileWhile() {
        if (tokenizer.tokenType() != TokenType.KEYWORD || tokenizer.keyword() != Keyword.WHILE) {
            throw new IllegalStateException("Unexpected keyword: " + tokenizer.keyword());
        }

        final WhileStatementTree whileStatementTree = new WhileStatementTree();
        advance(); // skip while
        advance(); // skip (
        whileStatementTree.condition = compileExpression();
        advance(); // next
        advance(); // skip )
        advance(); // skip {

        whileStatementTree.body = compileStatements();
        advance(); // skip }

        return whileStatementTree;
    }

    private DoStatementTree compileDo() {
        if (tokenizer.tokenType() != TokenType.KEYWORD || tokenizer.keyword() != Keyword.DO) {
            throw new IllegalStateException("Unexpected keyword: " + tokenizer.keyword());
        }

        advance(); // skip DO
        final DoStatementTree doStatementTree = new DoStatementTree();
        doStatementTree.subroutineCallTree = compileSubroutineCall();
        advance();

        return doStatementTree;
    }

    private ReturnStatementTree compileReturn() {
        if (tokenizer.tokenType() != TokenType.KEYWORD || tokenizer.keyword() != Keyword.RETURN) {
            throw new IllegalStateException("Unexpected keyword: " + tokenizer.keyword());
        }

        advance(); // skip return
        if (tokenizer.tokenType() == TokenType.SYMBOL && tokenizer.symbol() == ';') {
            return new ReturnStatementTree();
        }

        final ReturnStatementTree returnStatementTree = new ReturnStatementTree();
        returnStatementTree.expression = compileExpression();
        advance(); // to ;

        return returnStatementTree;
    }

    private ATermSyntaxTree compileExpression() {
        final ATermSyntaxTree term = compileTerm();

        advance(); // to next term
        // unaryOp term
        if (term instanceof OperatorTree && term.right == null) {
            term.left = compileTerm();
            return term;
        }

        // term
        if (tokenizer.tokenType() != TokenType.SYMBOL || !isOp(tokenizer.symbol())) {
            bufferToken();
            return term;
        }

        final ATermSyntaxTree root = compileTerm();
        root.left = term;

        advance();
        root.right = compileTerm();

        return root;
    }

    private ATermSyntaxTree compileTerm() {
        final TokenType tokenType = tokenizer.tokenType();
        return switch (tokenType) {
            case IDENTIFIER -> compileTermIdentifier();
            case SYMBOL -> compileTermSymbol();
            default -> compileTermConstant();
        };
    }

    private ATermSyntaxTree compileTermConstant() {
        return switch (tokenizer.tokenType()) {
            case INT_CONST -> new IntegerConstantTree(tokenizer.intVal());
            case STRING_CONST -> new StringConstantTree(tokenizer.stringVal());
            case KEYWORD -> new KeywordConstantTree(convert(tokenizer.keyword()));
            default -> throw new IllegalStateException("Unexpected token " + tokenizer.tokenType());
        };
    }

    private static KeywordConstantTree.Keyword convert(Keyword keyword) {
        return switch (keyword) {
            case THIS -> KeywordConstantTree.Keyword.THIS;
            case FALSE -> KeywordConstantTree.Keyword.FALSE;
            case TRUE -> KeywordConstantTree.Keyword.TRUE;
            case NULL -> KeywordConstantTree.Keyword.NULL;
            default -> throw new IllegalArgumentException("Unsupported keyword " + keyword);
        };
    }

    private ATermSyntaxTree compileTermSymbol() {
        if (isOp(tokenizer.symbol())) {
            return new OperatorTree(convert(tokenizer.symbol()));
        }

        // ( expression )
        if (tokenizer.symbol() == '(') {
            advance();
            ATermSyntaxTree expressionTree = compileExpression();
            expressionTree.expression = true;
            advance();
            return expressionTree;
        }

        return compileExpression();
    }

    private OperatorTree.Op convert(char ch) {
        return switch (ch) {
            case '+' -> OperatorTree.Op.ADD;
            case '-' -> OperatorTree.Op.SUB;
            case '*' -> OperatorTree.Op.MUL;
            case '/' -> OperatorTree.Op.DIV;
            case '&' -> OperatorTree.Op.AND;
            case '|' -> OperatorTree.Op.OR;
            case '<' -> OperatorTree.Op.LS;
            case '>' -> OperatorTree.Op.GT;
            case '=' -> OperatorTree.Op.EQ;
            case '~' -> OperatorTree.Op.NOT;
            default -> throw new IllegalStateException("Unexpected token: " + ch);
        };
    }

    private static boolean isOp(char op) {
        return op == '+' || op == '-' || op == '*' || op == '/' || op == '&' || op == '|' || op == '<' || op == '>' || op == '=' || op == '~';
    }

    private ATermSyntaxTree compileTermIdentifier() {
        if (tokenizer.tokenType() != TokenType.IDENTIFIER) {
            throw new IllegalStateException("Unexpected token " + tokenizer.tokenType());
        }

        final String identifier = tokenizer.identifier();

        advance();
        final boolean varName = tokenizer.tokenType() != TokenType.SYMBOL || tokenizer.symbol() == ';';
        if (varName) {
            bufferToken();
            return new IdentifierTree(identifier);
        }

        if (tokenizer.symbol() == '[') {
            advance(); // [
            final ArraySyntaxTree arraySyntaxTree = new ArraySyntaxTree(identifier, compileExpression());
            advance(); // ]

            return arraySyntaxTree;
        }

        if (tokenizer.symbol() == '(') {
            final List<ATermSyntaxTree> expressionList = compileExpressionList();

            return new SubroutineCallTree(identifier, expressionList);
        }

        if (tokenizer.symbol() == '.') {
            String subroutineIdentifier = identifier + tokenizer.symbol();
            advance();
            subroutineIdentifier += tokenizer.identifier();

            advance();
            final List<ATermSyntaxTree> expressionTrees = compileExpressionList();

            return new SubroutineCallTree(subroutineIdentifier, expressionTrees);
        }

        bufferToken();
        return new IdentifierTree(identifier);
    }

    private SubroutineCallTree compileSubroutineCall() {
        final AbstractSyntaxTree abstractSyntaxTree = compileTermIdentifier();
        if (!(abstractSyntaxTree instanceof SubroutineCallTree)) {
            throw new IllegalStateException("SubroutineCallTree expected");
        }

        return (SubroutineCallTree) abstractSyntaxTree;
    }

    private List<ATermSyntaxTree> compileExpressionList() {
        if (tokenizer.tokenType() != TokenType.SYMBOL || tokenizer.symbol() == ')') {
            throw new IllegalStateException("Unexpected token " + tokenizer.tokenType());
        }

        advance(); // skip (
        if (tokenizer.tokenType() == TokenType.SYMBOL && tokenizer.symbol() == ')') {
            return Collections.emptyList();
        }

        final List<ATermSyntaxTree> expressionTreeList = new ArrayList<>();
        expressionTreeList.add(compileExpression());

        advance(); // to , or )
        while (tokenizer.tokenType() == TokenType.SYMBOL && tokenizer.symbol() == ',') {
            advance(); // skip ,
            expressionTreeList.add(compileExpression());
            advance(); // to , or )
        }

        return expressionTreeList;
    }

    private static Type getType(JackTokenizer tokenizer) {
        if (tokenizer.tokenType() == TokenType.KEYWORD) {
            return switch (tokenizer.keyword()) {
                case INT -> Type.INTEGER;
                case CHAR -> Type.CHAR;
                case BOOLEAN -> Type.BOOLEAN;
                case VOID -> Type.VOID;
                default -> throw new IllegalStateException("Unexpected keyword: " + tokenizer.keyword());
            };
        }

        return Type.CLASS;
    }

    private void bufferToken() {
        hasBufferedToken = true;
    }

    private void advance() {
        if (hasBufferedToken) {
            hasBufferedToken = false;
            return;
        }

        tokenizer.advance();
    }

    private static boolean isStatement(JackTokenizer tokenizer) {
        if (tokenizer.tokenType() != TokenType.KEYWORD) {
            return false;
        }

        return switch (tokenizer.keyword()) {
            case LET, RETURN, IF, WHILE, DO -> true;
            default -> false;
        };
    }

    private static void throwUnexpectedToken(JackTokenizer tokenizer) {
        throw new IllegalStateException("Unexpected token: " + getCurrentToken(tokenizer));
    }

    private static String getCurrentToken(JackTokenizer tokenizer) {
        return switch (tokenizer.tokenType()) {
            case INT_CONST -> String.valueOf(tokenizer.intVal());
            case SYMBOL -> String.valueOf(tokenizer.symbol());
            case IDENTIFIER -> tokenizer.identifier();
            case KEYWORD -> tokenizer.keyword().name();
            case STRING_CONST -> tokenizer.stringVal();
        };
    }

    @Override
    public void close() throws IOException {
        this.tokenizer.close();
    }
}
