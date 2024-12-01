package edu.nadn2tetris.ast.processor;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import edu.nadn2tetris.ast.AbstractSyntaxTree;
import edu.nadn2tetris.ast.ClassTree;
import edu.nadn2tetris.ast.NodeKind;
import edu.nadn2tetris.ast.declaration.ADeclarationTree;
import edu.nadn2tetris.ast.declaration.ClassVarDeclarationTree;
import edu.nadn2tetris.ast.declaration.VarDeclarationTree;
import edu.nadn2tetris.ast.term.ParameterTree;
import edu.nadn2tetris.ast.term.subroutine.SubroutineBodyTree;
import edu.nadn2tetris.ast.term.subroutine.SubroutineDeclarationTree;
import edu.nadn2tetris.table.FileSymbolTable;
import edu.nadn2tetris.table.Kind;
import edu.nadn2tetris.table.SymbolTable;

public final class SymbolTableAstGenerator implements AstGenerator<FileSymbolTable> {
    private static final Set<NodeKind> DECLARATION_KINDS = Set.of(NodeKind.CLASS_VAR_DECLARATION, NodeKind.SUBROUTINE_DECLARATION);

    private final SymbolTable classSymbolTable = new SymbolTable();
    private final Map<String, SymbolTable> subroutineSymbolTables = new HashMap<>();

    private String className;
    private String currSubroutineName;

    @Override
    public FileSymbolTable generate(AbstractSyntaxTree root) {
        fillSymbolTree(root);
        return new FileSymbolTable(classSymbolTable, subroutineSymbolTables);
    }

    private void fillSymbolTree(AbstractSyntaxTree root) {
        switch (root.getNodeKind()) {
            case CLASS -> {
                final ClassTree classTree = (ClassTree) root;
                this.className = classTree.className;
                final List<AbstractSyntaxTree> list = classTree.blocks.stream()
                        .filter(node -> DECLARATION_KINDS.contains(node.getNodeKind())).toList();
                for (AbstractSyntaxTree block : list) {
                    fillSymbolTree(block);
                }
            }
            case CLASS_VAR_DECLARATION -> {
                final ClassVarDeclarationTree classTree = (ClassVarDeclarationTree) root;
                final Kind kind = classTree.declarationType == ClassVarDeclarationTree.DeclarationType.STATIC
                        ? Kind.STATIC
                        : Kind.FIELD;
                process(classSymbolTable, classTree, kind);
            }
            case SUBROUTINE_DECLARATION -> {
                final SubroutineDeclarationTree subroutineDeclarationTree = (SubroutineDeclarationTree) root;
                this.currSubroutineName = subroutineDeclarationTree.name;

                final SymbolTable createdSymbolTable = new SymbolTable();
                if (subroutineDeclarationTree.subroutineType == SubroutineDeclarationTree.SubroutineType.METHOD) {
                    createdSymbolTable.define("this", className, Kind.ARG);
                }

                if (subroutineDeclarationTree.parameterList != null && !subroutineDeclarationTree.parameterList.isEmpty()) {
                    for (ParameterTree parameter : subroutineDeclarationTree.parameterList) {
                        createdSymbolTable.define(parameter.name, parameter.getType(), Kind.ARG);
                    }
                }
                subroutineSymbolTables.put(this.currSubroutineName, createdSymbolTable);

                fillSymbolTree(subroutineDeclarationTree.subroutineBodyTree);
            }
            case SUBROUTINE_BODY -> {
                final SubroutineBodyTree bodyTree = (SubroutineBodyTree) root;
                if (bodyTree.nodes != null && !bodyTree.nodes.isEmpty()) {
                    bodyTree.nodes.stream()
                            .filter(e -> e.getNodeKind() == NodeKind.VAR_DECLARATION)
                            .forEach(this::fillSymbolTree);
                }
            }
            case VAR_DECLARATION -> {
                if (currSubroutineName == null) {
                    throw new IllegalStateException("Current method not set");
                }
                process(subroutineSymbolTables.get(currSubroutineName), (VarDeclarationTree) root, Kind.VAR);
            }
            default -> throw new IllegalStateException("Unsupported node kind: " + root.getNodeKind());
        }
    }

    private static void process(SymbolTable table, ADeclarationTree declarationTree, Kind kind) {
        for (String name : declarationTree.varNames) {
            table.define(name, declarationTree.getType(), kind);
        }
    }
}
