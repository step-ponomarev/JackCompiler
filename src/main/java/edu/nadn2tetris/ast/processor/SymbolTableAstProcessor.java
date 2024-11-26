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

public final class SymbolTableAstProcessor implements AstProcessor<FileSymbolTable> {
    private static final Set<NodeKind> DECLARATION_KINDS = Set.of(NodeKind.CLASS_VAR_DECLARATION, NodeKind.SUBROUTINE_DECLARATION);

    private final SymbolTable classSymbolTable = new SymbolTable();
    private final Map<String, SymbolTable> subroutineSymbolTables = new HashMap<>();
    private String currSubRoutine;

    @Override
    public FileSymbolTable process(AbstractSyntaxTree root) {
        switch (root.getNodeKind()) {
            case CLASS -> {
                final ClassTree classTree = (ClassTree) root;
                final List<AbstractSyntaxTree> list = classTree.blocks.stream()
                        .filter(node -> DECLARATION_KINDS.contains(node.getNodeKind())).toList();
                for (AbstractSyntaxTree block : list) {
                    process(block);
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
                this.currSubRoutine = subroutineDeclarationTree.name;
                if (subroutineDeclarationTree.parameterList != null) {
                    final SymbolTable createdSymbolTable = new SymbolTable();
                    for (ParameterTree parameter : subroutineDeclarationTree.parameterList) {
                        createdSymbolTable.define(parameter.name, parameter.getType(), Kind.ARG);
                    }

                    subroutineSymbolTables.put(this.currSubRoutine, createdSymbolTable);
                }

                process(subroutineDeclarationTree.subroutineBodyTree);
            }
            case SUBROUTINE_BODY -> {
                final SubroutineBodyTree bodyTree = (SubroutineBodyTree) root;
                if (bodyTree.nodes != null) {
                    bodyTree.nodes.stream()
                            .filter(e -> e.getNodeKind() == NodeKind.VAR_DECLARATION)
                            .forEach(this::process);
                }
            }
            case VAR_DECLARATION -> {
                if (currSubRoutine == null) {
                    throw new IllegalStateException("Current method not set");
                }
                process(subroutineSymbolTables.get(currSubRoutine), (VarDeclarationTree) root, Kind.VAR);
            }
        }

        return new FileSymbolTable(classSymbolTable, subroutineSymbolTables);
    }

    private static void process(SymbolTable table, ADeclarationTree declarationTree, Kind kind) {
        for (String name : declarationTree.varNames) {
            table.define(name, declarationTree.getType(), kind);
        }
    }
}
