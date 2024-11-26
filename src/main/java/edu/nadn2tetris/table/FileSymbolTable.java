package edu.nadn2tetris.table;

import java.util.Map;

public final class FileSymbolTable {
    private final SymbolTable classSymbolTable;
    private final Map<String, SymbolTable> subroutineSymbolTables;

    public FileSymbolTable(SymbolTable classSymbolTable, Map<String, SymbolTable> subroutineSymbolTables) {
        this.classSymbolTable = classSymbolTable;
        this.subroutineSymbolTables = subroutineSymbolTables;
    }

    public IdentifierInfo get(String methodName, String identifier) {
        final SymbolTable table = subroutineSymbolTables.get(methodName);
        if (table == null) {
            return classSymbolTable.getIdentifierInfo(identifier);
        }

        final IdentifierInfo identifierInfo = table.getIdentifierInfo(identifier);

        return identifierInfo == null ? classSymbolTable.getIdentifierInfo(identifier) : identifierInfo;
    }
}