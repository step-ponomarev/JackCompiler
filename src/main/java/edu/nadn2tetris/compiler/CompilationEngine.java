package edu.nadn2tetris.compiler;

import java.io.Closeable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import edu.nadn2tetris.ast.AbstractSyntaxTree;
import edu.nadn2tetris.ast.ClassTree;
import edu.nadn2tetris.ast.ExpressionTree;
import edu.nadn2tetris.ast.variables.ClassVarDeclarationTree;
import edu.nadn2tetris.ast.variables.VarDeclarationTree;
import edu.nadn2tetris.ast.statement.DoStatementTree;
import edu.nadn2tetris.ast.statement.IfStatementTree;
import edu.nadn2tetris.ast.statement.LetStatementTree;
import edu.nadn2tetris.ast.statement.ReturnStatementTree;
import edu.nadn2tetris.ast.statement.StatementTree;
import edu.nadn2tetris.ast.statement.WhileStatementTree;
import edu.nadn2tetris.ast.subroutine.SubroutineBodyTree;
import edu.nadn2tetris.ast.subroutine.SubroutineDeclarationTree;
import edu.nadn2tetris.ast.Type;
import edu.nadn2tetris.ast.term.ArraySyntaxTree;
import edu.nadn2tetris.ast.term.OperatorTree;
import edu.nadn2tetris.ast.term.IdentifierTree;
import edu.nadn2tetris.ast.term.IntegerConstantTree;
import edu.nadn2tetris.ast.term.KeywordConstantTree;
import edu.nadn2tetris.ast.term.ParameterTree;
import edu.nadn2tetris.ast.term.StringConstantTree;
import edu.nadn2tetris.ast.subroutine.SubroutineCallTree;
import edu.nadn2tetris.common.Keyword;
import edu.nadn2tetris.common.TokenType;
import edu.nadn2tetris.tokenizer.JackTokenizer;

public final class CompilationEngine implements Closeable {
    private final JackTokenizer tokenizer;

    public CompilationEngine(JackTokenizer tokenizer) {
        this.tokenizer = tokenizer;
    }

    public ClassTree compileClass() {
        advance(); // start
        advance(); // skip class

        final ClassTree classTree = new ClassTree();
        classTree.className = tokenizer.identifier();
        advance(); // skip className

        final ArrayList<AbstractSyntaxTree> innerClassNodes = new ArrayList<>();

        advance();
        // empty class
        while (tokenizer.tokenType() != TokenType.SYMBOL || tokenizer.symbol() != '}') {
            if (tokenizer.tokenType() != TokenType.KEYWORD) {
                throw new IllegalStateException("Unexpected token: " + tokenizer.tokenType());
            }

            if (isClassVarDec(tokenizer.keyword())) {
                innerClassNodes.add(compileClassVarDec());
                advance();
            } else if (isSubroutineDec(tokenizer.keyword())) {
                innerClassNodes.add(compileSubroutine());
                advance();
            } else {
                throw new IllegalStateException("Unexpected token: " + tokenizer.tokenType());
            }
        }

        classTree.nodes = innerClassNodes;

        return classTree;
    }

    private boolean isSubroutineDec(Keyword keyword) {
        return keyword == Keyword.CONSTRUCTOR || keyword == Keyword.FUNCTION || keyword == Keyword.METHOD;
    }

    private boolean isClassVarDec(Keyword keyword) {
        return keyword == Keyword.STATIC || keyword == Keyword.FIELD;
    }

    public ClassVarDeclarationTree compileClassVarDec() {
        if (tokenizer.tokenType() != TokenType.KEYWORD || (tokenizer.keyword() != Keyword.STATIC && tokenizer.keyword() != Keyword.FIELD)) {
            throw new IllegalStateException("Unexpected token: " + tokenizer.tokenType());
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

    public SubroutineDeclarationTree compileSubroutine() {
        Keyword keyword = tokenizer.keyword();
        final SubroutineDeclarationTree.SubroutineType subroutineType = keyword == Keyword.CONSTRUCTOR ? SubroutineDeclarationTree.SubroutineType.CONSTRUCTOR : keyword == Keyword.METHOD ? SubroutineDeclarationTree.SubroutineType.METHOD : SubroutineDeclarationTree.SubroutineType.FUNCTION;


        advance();
        final SubroutineDeclarationTree subroutineDeclarationTree = new SubroutineDeclarationTree();
        subroutineDeclarationTree.returnType = getType(tokenizer);
        if (subroutineDeclarationTree.returnType == Type.CLASS) {
            subroutineDeclarationTree.returnClassName = tokenizer.identifier();
        }

        subroutineDeclarationTree.subroutineType = subroutineType;

        advance();
        subroutineDeclarationTree.name = tokenizer.identifier();

        advance();
        subroutineDeclarationTree.parameterList = compileParameterList();

        advance(); // skip )
        subroutineDeclarationTree.subroutineBodyTree = compileSubroutineBody();

        return subroutineDeclarationTree;
    }

    public SubroutineBodyTree compileSubroutineBody() {
        if (tokenizer.tokenType() != TokenType.SYMBOL || tokenizer.symbol() != '{') {
            throw new IllegalStateException("Unexpected token: " + tokenizer.tokenType());
        }

        advance(); // skip {
        // empty method
        if (tokenizer.tokenType() == TokenType.SYMBOL && tokenizer.symbol() == '}') {
            return new SubroutineBodyTree(new ArrayList<>());
        }

        final ArrayList<AbstractSyntaxTree> nodes = new ArrayList<>();
        final SubroutineBodyTree subroutineBodyTree = new SubroutineBodyTree(nodes);
        // var declarations on top
        while (tokenizer.tokenType() == TokenType.KEYWORD && tokenizer.keyword() == Keyword.VAR) {
            nodes.add(compileVarDec());
            advance();
        }

        nodes.addAll(compileStatements());

        advance(); // to }

        return subroutineBodyTree;
    }

    public List<ParameterTree> compileParameterList() {
        final List<ParameterTree> parameterTrees = new ArrayList<>();
        advance(); // ( or ,
        while (tokenizer.tokenType() != TokenType.SYMBOL || tokenizer.symbol() != ')') {
            ParameterTree parameterTree = new ParameterTree();
            parameterTree.type = getType(tokenizer);
            if (parameterTree.type == Type.CLASS) {
                parameterTree.className = tokenizer.identifier();
            }

            advance();
            parameterTree.name = tokenizer.identifier();
            advance();

            parameterTrees.add(parameterTree);
        }

        return parameterTrees;
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

    public VarDeclarationTree compileVarDec() {
        if (tokenizer.tokenType() != TokenType.KEYWORD || tokenizer.keyword() != Keyword.VAR) {
            throw new IllegalStateException("Unexpected token: " + tokenizer.tokenType());
        }
        advance(); // skip var

        final Type varType = getType(tokenizer);
        final ArrayList<String> names = new ArrayList<>();
        VarDeclarationTree varDeclarationTree = new VarDeclarationTree(names, varType);
        if (varType == Type.CLASS) {
            varDeclarationTree.classNameType = tokenizer.identifier();
        }

        advance();
        names.addAll(getVarNamesList());

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

    public List<StatementTree> compileStatements() {
        final List<StatementTree> statementTrees = new ArrayList<>();
        while (!isNotStatement()) {
            statementTrees.add(compileStatement());
            advance();
        }
        tokenizer.rollback();

        return statementTrees;
    }

    private StatementTree compileStatement() {
        return switch (tokenizer.keyword()) {
            case LET -> compileLet();
            case IF -> compileIf();
            case WHILE -> compileWhile();
            case DO -> compileDo();
            case RETURN -> compileReturn();
            default -> throw new IllegalStateException("Unsupported type " + tokenizer.keyword());
        };
    }

    public LetStatementTree compileLet() {
        if (tokenizer.tokenType() != TokenType.KEYWORD || tokenizer.keyword() != Keyword.LET) {
            throw new IllegalStateException("Unexpected keyword: " + tokenizer.keyword());
        }

        advance(); // skip let
        final LetStatementTree letStatementTree = new LetStatementTree();
        letStatementTree.identifierName = tokenizer.identifier();

        advance();
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

    public IfStatementTree compileIf() {
        if (tokenizer.tokenType() != TokenType.KEYWORD || tokenizer.keyword() != Keyword.IF) {
            throw new IllegalStateException("Unexpected keyword: " + tokenizer.keyword());
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
            tokenizer.rollback();
            return ifStatementTree;
        }

        advance(); // skip else
        advance(); // skip {
        ifStatementTree.elseBody = compileStatements();
        advance(); // skip }

        return ifStatementTree;
    }

    public WhileStatementTree compileWhile() {
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

    public DoStatementTree compileDo() {
        if (tokenizer.tokenType() != TokenType.KEYWORD || tokenizer.keyword() != Keyword.DO) {
            throw new IllegalStateException("Unexpected keyword: " + tokenizer.keyword());
        }

        advance(); // skip DO
        final DoStatementTree doStatementTree = new DoStatementTree();
        doStatementTree.subroutineCallTree = compileSubroutineCall();
        advance();

        return doStatementTree;
    }

    public ReturnStatementTree compileReturn() {
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

    public ExpressionTree compileExpression() {
        final ExpressionTree expressionTree = new ExpressionTree();
        compileExpression(expressionTree);

        return expressionTree;
    }

    private ExpressionTree compileExpression(ExpressionTree expressionTree) {
        final AbstractSyntaxTree term = compileTerm();

        // unaryOp term
        advance();
        if (term instanceof OperatorTree) {
            final ExpressionTree curr = new ExpressionTree();
            curr.curr = term;
            curr.left = compileExpression();

            expressionTree.curr = curr;
            return expressionTree;
        }

        if (tokenizer.tokenType() != TokenType.SYMBOL || !isOp(tokenizer.symbol())) {
            expressionTree.curr = term;
            tokenizer.rollback();
            return expressionTree;
        }

        // term op term
        expressionTree.left = new ExpressionTree(term);
        expressionTree.curr = compileTerm(); // op
        expressionTree.right = new ExpressionTree();

        advance();
        return compileExpression(expressionTree.right);
    }

    public AbstractSyntaxTree compileTerm() {
        final TokenType tokenType = tokenizer.tokenType();
        return switch (tokenType) {
            case IDENTIFIER -> compileTermIdentifier();
            case SYMBOL -> compileTermSymbol();
            default -> compileTermConstant();
        };
    }

    private AbstractSyntaxTree compileTermConstant() {
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

    private AbstractSyntaxTree compileTermSymbol() {
        if (isOp(tokenizer.symbol())) {
            return new OperatorTree(convert(tokenizer.symbol()));
        }

        // ( expression )
        if (tokenizer.symbol() == '(') {
            advance();
            ExpressionTree expressionTree = compileExpression();
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

    private AbstractSyntaxTree compileTermIdentifier() {
        if (tokenizer.tokenType() != TokenType.IDENTIFIER) {
            throw new IllegalStateException("Unexpected token " + tokenizer.tokenType());
        }

        final String identifier = tokenizer.identifier();

        advance();
        final boolean varName = tokenizer.tokenType() != TokenType.SYMBOL || tokenizer.symbol() == ';';
        if (varName) {
            tokenizer.rollback();
            return new IdentifierTree(identifier);
        }

        if (tokenizer.symbol() == '[') {
            advance(); // [
            final ArraySyntaxTree arraySyntaxTree = new ArraySyntaxTree(identifier, compileExpression());
            advance(); // ]

            return arraySyntaxTree;
        }

        if (tokenizer.symbol() == '(') {
            final List<ExpressionTree> expressionList = compileExpressionList(new ArrayList<>());

            return new SubroutineCallTree(identifier, expressionList);
        }

        if (tokenizer.symbol() == '.') {
            String subroutineIdentifier = identifier + tokenizer.symbol();
            advance();
            subroutineIdentifier += tokenizer.identifier();

            advance();
            final List<ExpressionTree> expressionTrees = compileExpressionList(new ArrayList<>());

            return new SubroutineCallTree(subroutineIdentifier, expressionTrees);
        }

        tokenizer.rollback();
        return new IdentifierTree(identifier);
    }

    private SubroutineCallTree compileSubroutineCall() {
        final AbstractSyntaxTree abstractSyntaxTree = compileTermIdentifier();
        if (!(abstractSyntaxTree instanceof SubroutineCallTree)) {
            throw new IllegalStateException("SubroutineCallTree expected");
        }

        return (SubroutineCallTree) abstractSyntaxTree;
    }

    public List<ExpressionTree> compileExpressionList(List<ExpressionTree> expressionTreeList) {
        if (tokenizer.tokenType() != TokenType.SYMBOL || tokenizer.symbol() == ')') {
            throw new IllegalStateException("Unexpected token " + tokenizer.tokenType());
        }

        advance(); // skip (
        if (tokenizer.tokenType() == TokenType.SYMBOL && tokenizer.symbol() == ')') {
            return Collections.emptyList();
        }

        expressionTreeList.add(compileExpression());

        advance();
        while (tokenizer.tokenType() == TokenType.SYMBOL && tokenizer.symbol() == ',') {
            advance(); // skip ,
            expressionTreeList.add(compileExpression());
            advance();
        }

        return expressionTreeList;
    }

    private void advance() {
        tokenizer.advance();
    }

    private boolean isNotStatement() {
        if (tokenizer.tokenType() != TokenType.KEYWORD) {
            return true;
        }

        final Keyword keyword = tokenizer.keyword();
        return keyword != Keyword.LET && keyword != Keyword.IF && keyword != Keyword.WHILE && keyword != Keyword.DO && keyword != Keyword.RETURN;
    }

    @Override
    public void close() throws IOException {
        this.tokenizer.close();
    }
}

