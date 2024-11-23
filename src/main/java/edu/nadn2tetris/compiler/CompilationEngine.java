package edu.nadn2tetris.compiler;

import java.io.BufferedWriter;
import java.io.Closeable;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Stack;

import edu.nadn2tetris.ast.AbstractSyntaxTree;
import edu.nadn2tetris.ast.ExpressionTree;
import edu.nadn2tetris.ast.term.ArraySyntaxTree;
import edu.nadn2tetris.ast.term.BinaryOpTree;
import edu.nadn2tetris.ast.term.IdentifierTree;
import edu.nadn2tetris.ast.term.IntegerConstantTree;
import edu.nadn2tetris.ast.term.KeywordConstantTree;
import edu.nadn2tetris.ast.term.StringConstantTree;
import edu.nadn2tetris.ast.term.SubroutineCallTree;
import edu.nadn2tetris.ast.term.UnaryOpTree;
import edu.nadn2tetris.common.Keyword;
import edu.nadn2tetris.common.TokenType;
import edu.nadn2tetris.table.IdentifierInfo;
import edu.nadn2tetris.table.SymbolTable;
import edu.nadn2tetris.tokenizer.JackTokenizer;
import edu.nadn2tetris.writer.Command;
import edu.nadn2tetris.writer.Segment;
import edu.nadn2tetris.writer.VMWriter;

public final class CompilationEngine implements Closeable {
    private static final String TAB_SYMBOL = "\s\s";

    private final JackTokenizer tokenizer;
    private final Writer bufferedWriter;
    private final StringBuilder xml = new StringBuilder();
    private final VMWriter vmWriter;

    private final Stack<Kind> kindNesting = new Stack<>();
    private final SymbolTable classSymbolTable = new SymbolTable();
    private final SymbolTable procedureSymbolTable = new SymbolTable();

    private edu.nadn2tetris.table.Kind declarationKind = null;
    private String declarationType = null;
    private int nestingLevel = 0;

    private final Set<Flag> flags;

    private final Stack<String> expression = new Stack<>();

    public CompilationEngine(JackTokenizer tokenizer, OutputStream out, Set<Flag> flags) {
        this.flags = flags == null ? new HashSet<>() : flags;

        this.tokenizer = tokenizer;

        final BufferedWriter bufferedWriter = new BufferedWriter(new OutputStreamWriter(out));
        this.vmWriter = this.flags.contains(Flag.GENERATE_CODE) ? new VMWriter(bufferedWriter) : null;
        this.bufferedWriter = this.flags.contains(Flag.GENERATE_CODE) ? null : bufferedWriter;
    }

    public void compileClass() {
        advance();
        openBlock(Kind.CLASS);

        handleTokenXml();
        advance();
        handleTokenXml();

        // {
        advance();
        handleTokenXml();

        advance();
        if (tokenizer.tokenType() == TokenType.SYMBOL && tokenizer.symbol() == '}') {
            handleTokenXml();
            return;
        }

        while (tokenizer.tokenType() == TokenType.KEYWORD && isClassVarDec(tokenizer.keyword())) {
            compileClassVarDec();
            advance();
        }

        while (tokenizer.tokenType() == TokenType.KEYWORD && isSubroutineDec(tokenizer.keyword())) {
            compileSubroutine();
            advance();
        }

        // }
        handleTokenXml();
        closeBlock(Kind.CLASS);
    }

    private boolean isSubroutineDec(Keyword keyword) {
        return keyword == Keyword.CONSTRUCTOR
                || keyword == Keyword.FUNCTION
                || keyword == Keyword.METHOD;
    }

    private boolean isClassVarDec(Keyword keyword) {
        return keyword == Keyword.STATIC || keyword == Keyword.FIELD;
    }

    public void compileClassVarDec() {
        openBlock(Kind.CLASS_VAR_DEC);
        handleTokenXml();

        advance();

        declarationType = getType(tokenizer);
        handleTokenXml();

        advance();

        declarationKind = tokenizer.keyword() == Keyword.STATIC ? edu.nadn2tetris.table.Kind.STATIC : edu.nadn2tetris.table.Kind.FIELD;
        handleTokenXml();

        if (!tokenizer.hasMoreTokens()) {
            declarationType = null;
            declarationKind = null;
            closeBlock(Kind.CLASS_VAR_DEC);
            return;
        }

        advance();
        if (tokenizer.tokenType() != TokenType.SYMBOL || tokenizer.symbol() != ',') {
            handleTokenXml();
            declarationType = null;
            declarationKind = null;
            closeBlock(Kind.CLASS_VAR_DEC);
            return;
        }

        //,
        handleTokenXml();

        advance();
        compileVarDec(true);

        declarationType = null;
        declarationKind = null;
        closeBlock(Kind.CLASS_VAR_DEC);
    }

    public void compileSubroutine() {
        openBlock(Kind.SUBROUTINE_DEC);
        handleTokenXml();

        advance();
        handleTokenXml();

        advance();
        handleTokenXml();

        //(
        advance();
        handleTokenXml();

        advance();
        compileParameterList();

        //)
        advance();
        handleTokenXml();

        advance();
        compileSubroutineBody();
        closeBlock(Kind.SUBROUTINE_DEC);
    }

    public void compileSubroutineBody() {
        openBlock(Kind.SUBROUTINE_BODY);
        //{
        handleTokenXml();

        advance();
        while (tokenizer.tokenType() == TokenType.KEYWORD && tokenizer.keyword() == Keyword.VAR) {
            compileVarDec();
            advance();
        }

        compileStatements();

        //}
        advance();
        handleTokenXml();
        closeBlock(Kind.SUBROUTINE_BODY);
    }

    public void compileParameterList() {
        boolean declareVariables = kindNesting.peek() == Kind.SUBROUTINE_DEC;
        openBlock(Kind.PARAMETER_LIST);
        compileParameterListNested(declareVariables);
        closeBlock(Kind.PARAMETER_LIST);
    }

    private void compileParameterListNested() {
        compileParameterListNested(false);
    }

    private void compileParameterListNested(boolean declareVariables) {
        if (!isType(tokenizer)) {
            return;
        }

        if (declareVariables) {
            declarationKind = edu.nadn2tetris.table.Kind.ARG;
            declarationType = getType(tokenizer);
        }

        handleTokenXml();

        advance();
        handleTokenXml();

        advance();
        if (tokenizer.tokenType() != TokenType.SYMBOL || tokenizer.symbol() != ',') {
            if (declareVariables) {
                declarationKind = null;
                declarationType = null;
            }
            return;
        }

        //,
        handleTokenXml();

        advance();
        compileParameterListNested(declareVariables);

        if (declareVariables) {
            declarationKind = null;
            declarationType = null;
        }
    }

    private static boolean isType(JackTokenizer tokenizer) {
        if (tokenizer.tokenType() == TokenType.KEYWORD) {
            return tokenizer.keyword() == Keyword.INT
                    || tokenizer.keyword() == Keyword.CHAR
                    || tokenizer.keyword() == Keyword.BOOLEAN;
        }

        return tokenizer.tokenType() == TokenType.IDENTIFIER;
    }

    private static String getType(JackTokenizer tokenizer) {
        if (tokenizer.tokenType() == TokenType.KEYWORD) {
            return tokenizer.keyword().name().toLowerCase();
        }

        return tokenizer.identifier();
    }

    public void compileVarDec() {
        openBlock(Kind.VAR_DEC);
        compileVarDec(false);
        closeBlock(Kind.VAR_DEC);
    }

    private void compileVarDec(boolean listDeclaration) {
        if (!listDeclaration) {
            declarationKind = edu.nadn2tetris.table.Kind.VAR;
            handleTokenXml();

            advance();
            handleTokenXml();
            declarationType = getType(tokenizer);

            advance();
        }

        handleTokenXml();

        advance();
        if (tokenizer.symbol() == ';') {
            handleTokenXml();
            declarationKind = null;
            declarationType = null;
            return;
        }

        handleTokenXml(); //,

        advance();
        compileVarDec(true);
        if (!listDeclaration) {
            declarationKind = null;
            declarationType = null;
        }
    }

    public void compileStatements() {
        openBlock(Kind.STATEMENTS);
        compileStatementsNested();
        closeBlock(Kind.STATEMENTS);
    }

    private void compileStatementsNested() {
        if (isNotStatement()) {
            return;
        }

        compileStatement();

        advance();
        if (tokenizer.tokenType() != TokenType.KEYWORD || isNotStatement()) {
            return;
        }

        compileStatementsNested();
    }

    private void compileStatement() {
        switch (tokenizer.keyword()) {
            case LET -> compileLet();
            case IF -> compileIf();
            case WHILE -> compileWhile();
            case DO -> compileDo();
            case RETURN -> compileReturn();
            default -> throw new IllegalStateException("Unsupported type " + tokenizer.keyword());
        }
    }

    public void compileLet() {
        openBlock(Kind.LET_STATEMENT);
        handleTokenXml();

        advance();
        handleTokenXml();

        advance();
        final boolean array = tokenizer.symbol() == '[';
        if (array) {
            handleTokenXml();
            advance();
            compileExpression();

            advance();
            handleTokenXml();

            // =
            advance();
        }

        // =
        handleTokenXml();

        advance();
        compileExpression();

        advance();
        handleTokenXml();
        closeBlock(Kind.LET_STATEMENT);
    }

    public void compileIf() {
        openBlock(Kind.IF_STATEMENT);
        handleTokenXml();

        advance();
        compileConditionalStatements();
        if (!tokenizer.hasMoreTokens()) {
            closeBlock(Kind.IF_STATEMENT);
            return;
        }

        advance();
        if (tokenizer.tokenType() != TokenType.KEYWORD || tokenizer.keyword() != Keyword.ELSE) {
            closeBlock(Kind.IF_STATEMENT);
            return;
        }

        handleTokenXml();

        advance();
        handleTokenXml();

        advance();
        compileStatements();

        advance();
        handleTokenXml();
        closeBlock(Kind.IF_STATEMENT);
    }

    public void compileWhile() {
        openBlock(Kind.WHILE_STATEMENT);
        handleTokenXml();
        advance();

        compileConditionalStatements();
        closeBlock(Kind.WHILE_STATEMENT);
    }

    private void compileConditionalStatements() {
        handleTokenXml();

        advance();
        compileExpression();

        advance();
        handleTokenXml();

        // {
        advance();
        handleTokenXml();

        advance();
        compileStatements();

        advance();

        handleTokenXml();
    }

    public void compileDo() {
        openBlock(Kind.DO_STATEMENT);
        handleTokenXml();

        advance();
        compileSubroutineCall();
        closeBlock(Kind.DO_STATEMENT);
    }

    public void compileReturn() {
        openBlock(Kind.RETURN_STATEMENT);
        handleTokenXml();

        advance();
        if (tokenizer.tokenType() == TokenType.SYMBOL && tokenizer.symbol() == ';') {
            handleTokenXml();
            return;
        }

        compileExpression();
        advance();
        handleTokenXml();
    }

    public ExpressionTree compileExpression() {
        final ExpressionTree expressionTree = new ExpressionTree();
        compileExpression(expressionTree);

        return expressionTree;
    }

    private ExpressionTree compileExpression(ExpressionTree expressionTree) {
        final AbstractSyntaxTree term = compileTerm();
        if (!tokenizer.hasMoreTokens()) {
            expressionTree.curr = term;
            return expressionTree;
        }

        advance();
        if (tokenizer.tokenType() != TokenType.SYMBOL || !isOp(tokenizer.symbol())) {
            expressionTree.curr = term;
            return expressionTree;
        }

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
            return new BinaryOpTree(convert(tokenizer.symbol()));
        }

        if (isUnaryOp(tokenizer.symbol())) {
            final UnaryOpTree.Op op = tokenizer.symbol() == '~' ? UnaryOpTree.Op.NOT : UnaryOpTree.Op.NEG;

            advance();
            return new UnaryOpTree(op);
        }

        return compileExpression();
    }

    private BinaryOpTree.Op convert(char ch) {
       return switch (ch) {
           case '+' -> BinaryOpTree.Op.ADD;
           case '-' -> BinaryOpTree.Op.SUB;
           case '*' -> BinaryOpTree.Op.MUL;
           case '/' -> BinaryOpTree.Op.DIV;
           case '&' -> BinaryOpTree.Op.AND;
           case '|' -> BinaryOpTree.Op.OR;
           case '<' -> BinaryOpTree.Op.LS;
           case '>' -> BinaryOpTree.Op.GT;
           case '=' -> BinaryOpTree.Op.EQ;
           default -> throw new IllegalStateException("Unexpected token: " + ch);
        };
    }

    private static boolean isUnaryOp(char op) {
        return op == '-' || op == '~';
    }

    private static boolean isOp(char op) {
        return op == '+'
                || op == '-'
                || op == '*'
                || op == '/'
                || op == '&'
                || op == '|'
                || op == '<'
                || op == '>'
                || op == '=';
    }

    private AbstractSyntaxTree compileTermIdentifier() {
        final String identifier = tokenizer.identifier();

        advance();
        final boolean varName = tokenizer.tokenType() != TokenType.SYMBOL || tokenizer.symbol() == ';';
        if (varName) {
            return new IdentifierTree(identifier);
        }

        if (tokenizer.symbol() == '[') {
            advance(); // [
            final ArraySyntaxTree arraySyntaxTree = new ArraySyntaxTree(identifier, compileExpression());
            advance(); // ]

            return arraySyntaxTree;
        }

        if (tokenizer.symbol() == '(') {
            advance(); // (
            final List<ExpressionTree> expressionList = compileExpressionList(new ArrayList<>());
            advance(); // )

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

    private void compileSubroutineCall() {
        handleTokenXml();

        advance();
        if (tokenizer.symbol() == '(') {
            handleTokenXml();

            advance();
            compileExpressionList(new ArrayList<>());

            advance();
            handleTokenXml();
        }

        //dot or ;
        advance();
        handleTokenXml();
        if (tokenizer.symbol() == ';') {
            return;
        }

        advance();
        compileSubroutineCallAfterDot();
    }

    private void compileSubroutineCallAfterDot(boolean writeLineEnd) {
        handleTokenXml();

        advance();
        handleTokenXml();

        advance();
        compileExpressionList(new ArrayList<>());

        // )
        advance();
        handleTokenXml();

        if (writeLineEnd) {
            advance();
            handleTokenXml();
        }
    }

    private void compileSubroutineCallAfterDot() {
        compileSubroutineCallAfterDot(true);
    }

    public List<ExpressionTree> compileExpressionList(List<ExpressionTree> expressionTreeList) {
        if (!tokenizer.hasMoreTokens()) {
            return Collections.emptyList();
        }

        if (tokenizer.tokenType() == TokenType.SYMBOL && tokenizer.symbol() == ')') {
            return Collections.emptyList();
        }

        expressionTreeList.add(compileExpression());

        advance();
        if (tokenizer.tokenType() == TokenType.SYMBOL && tokenizer.symbol() == ',') {
            advance();
            return compileExpressionList(expressionTreeList);
        }

        return expressionTreeList;
    }

    private void advance() {
        tokenizer.advance();
    }

    private void handleTokenXml() {
        switch (tokenizer.tokenType()) {
            case KEYWORD -> appendXml(wrapKeyword(tokenizer.keyword()));
            case IDENTIFIER -> handleIdentifier();
            case SYMBOL -> appendXml(wrapSymbol(tokenizer.symbol()));
            case INT_CONST -> appendXml(wrapIntConst(tokenizer.intVal()));
            case STRING_CONST -> appendXml(wrapStringConst(tokenizer.stringVal()));
            default -> throw new IllegalStateException("Unsupported token type: " + tokenizer.tokenType());
        }
    }

    private void handleIdentifier() {
        final String identifier = tokenizer.identifier();

        String identifierXml = "%s<identifier> %s </identifier>\n".formatted(TAB_SYMBOL.repeat(nestingLevel), identifier);
        if (!flags.contains(Flag.EXTENDED_IDENTIFIER)) {
            appendXml(identifierXml);
            return;
        }

        final boolean declaration = declarationKind != null && declarationType != null;
        if (declaration) {
            defineIdentifier(identifier);
        }

        final IdentifierInfo identifierInfo = getIdentifierInfo(identifier);
        if (identifierInfo == null) {
            appendXml(identifierXml);
            return;
        }

        final String category = declaration
                ? identifierInfo.kind.name().toLowerCase()
                : identifierInfo.kind == edu.nadn2tetris.table.Kind.STATIC || identifierInfo.kind == edu.nadn2tetris.table.Kind.FIELD
                ? edu.nadn2tetris.table.Kind.CLASS.name().toLowerCase()
                : edu.nadn2tetris.table.Kind.SUBROUTINE.name().toLowerCase();

        identifierXml = "%s<identifier category=\"%s\" index=\"%d\" declaration=\"%s\"> %s </identifier>\n".formatted(
                TAB_SYMBOL.repeat(nestingLevel),
                category,
                indexOf(identifier),
                declaration,
                identifier
        );

        appendXml(identifierXml);
    }

    private boolean isNotStatement() {
        if (tokenizer.tokenType() != TokenType.KEYWORD) {
            return true;
        }

        final Keyword keyword = tokenizer.keyword();
        return keyword != Keyword.LET
                && keyword != Keyword.IF
                && keyword != Keyword.WHILE
                && keyword != Keyword.DO
                && keyword != Keyword.RETURN;
    }

    private void openBlock(Kind kind) {
        appendXml("%s<%s>\n".formatted(TAB_SYMBOL.repeat(nestingLevel++), kind.tagName));
        kindNesting.add(kind);
    }

    private void closeBlock(Kind kind) {
        if (kind == Kind.SUBROUTINE_DEC || kind == Kind.CLASS) {
            procedureSymbolTable.reset();
        }

        if (kind == Kind.EXPRESSION && flags.contains(Flag.GENERATE_CODE)) {
            generateExpression();
        }

        kindNesting.pop();
        appendXml("%s</%s>\n".formatted(TAB_SYMBOL.repeat(--nestingLevel >= 0 ? nestingLevel : 0), kind.tagName));
    }

    //TODO: на данный момент это не будет работать для term op term op term...
    // может строить нормальное AST??
    private void generateExpression() {
        if (expression.size() == 1) { // term
            String identifier = expression.pop();
            final IdentifierInfo identifierInfo = getIdentifierInfo(identifier);
            final Segment seg = identifierInfo.kind == edu.nadn2tetris.table.Kind.CLASS ? Segment.THIS : Segment.LOCAL;

            vmWriter.writePush(seg, indexOf(identifier));
        } else { // term op term
            while (!expression.isEmpty()) {
                String identifier2 = expression.pop();
                Command command = Command.parse(expression.pop().charAt(0));
                pushIdentifier(expression.pop());
                pushIdentifier(identifier2);
                vmWriter.writeArithmetic(command);
            }
        }
    }

    private void pushIdentifier(String name) {
        final IdentifierInfo identifierInfo = getIdentifierInfo(name);
        final Segment seg = identifierInfo.kind == edu.nadn2tetris.table.Kind.CLASS ? Segment.THIS : Segment.LOCAL;
        vmWriter.writePush(seg, indexOf(name));
    }

    private String wrapKeyword(Keyword keyword) {
        return "%s<keyword> %s </keyword>\n".formatted(TAB_SYMBOL.repeat(nestingLevel), keyword.name().toLowerCase());
    }

    private short indexOf(String name) {
        IdentifierInfo identifierInfo = procedureSymbolTable.getIdentifierInfo(name);
        if (identifierInfo != null) {
            return procedureSymbolTable.indexOf(name);
        }

        identifierInfo = classSymbolTable.getIdentifierInfo(name);
        if (identifierInfo == null) {
            throw new IllegalStateException("Undefined identifier: " + name);
        }

        return classSymbolTable.indexOf(name);
    }

    private void defineIdentifier(String name) {
        if (declarationKind == null || declarationType == null) {
            throw new IllegalStateException("Invalid declaration");
        }

        final SymbolTable table = declarationKind == edu.nadn2tetris.table.Kind.FIELD || declarationKind == edu.nadn2tetris.table.Kind.STATIC
                ? classSymbolTable
                : procedureSymbolTable;

        table.define(name, declarationType, declarationKind);
    }

    private IdentifierInfo getIdentifierInfo(String name) {
        IdentifierInfo identifier = procedureSymbolTable.getIdentifierInfo(name);
        if (identifier == null) {
            identifier = classSymbolTable.getIdentifierInfo(name);
        }

        return identifier;
    }

    private String wrapSymbol(char symbol) {
        return "%s<symbol> %s </symbol>\n".formatted(TAB_SYMBOL.repeat(nestingLevel), symbol);
    }

    private String wrapIntConst(short intConst) {
        return "%s<integerConstant> %d </integerConstant>\n".formatted(TAB_SYMBOL.repeat(nestingLevel), intConst);
    }

    private String wrapStringConst(String stringConst) {
        return "%s<stringConstant> %s </stringConstant>\n".formatted(TAB_SYMBOL.repeat(nestingLevel), stringConst);
    }

    private void appendXml(String string) {
        if (flags.contains(Flag.GENERATE_CODE)) {
            return;
        }

        xml.append(string);
    }

    @Override
    public void close() throws IOException {
        this.tokenizer.close();

        if (this.bufferedWriter != null) {
            this.bufferedWriter.append(xml).close();
        }

        if (this.vmWriter != null) {
            this.vmWriter.close();
        }
    }

    private enum Kind {
        CLASS("class"),
        CLASS_VAR_DEC("classVarDec"),
        SUBROUTINE_DEC("subroutineDec"),
        SUBROUTINE_BODY("subroutineBody"),
        PARAMETER_LIST("parameterList"),
        VAR_DEC("varDec"),
        STATEMENTS("statements"),
        LET_STATEMENT("letStatement"),
        IF_STATEMENT("ifStatement"),
        WHILE_STATEMENT("whileStatement"),
        DO_STATEMENT("doStatement"),
        RETURN_STATEMENT("returnStatement"),
        EXPRESSION("expression"),
        EXPRESSION_LIST("expressionList"),
        STRING_CONSTANT("stringConstant"),
        INTEGER_CONSTANT("integerConstant"),
        TERM("term");

        public final String tagName;

        Kind(String tagName) {
            this.tagName = tagName;
        }
    }
}

