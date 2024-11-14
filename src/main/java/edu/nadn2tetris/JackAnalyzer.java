package edu.nadn2tetris;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import edu.nadn2tetris.compiler.CompilationEngine;
import edu.nadn2tetris.tokenizer.JackTokenizer;
import edu.nadn2tetris.utils.FileUtils;

public final class JackAnalyzer {

    public static void main(String[] args) {
        if (args.length < 2) {
            throw new IllegalArgumentException("Invalid arguments count: " + args.length);
        }

        final Path outDir = Paths.get(args[1]);
        try {
            final List<Path> sourceFiles = getSourceFiles(Paths.get(args[0]));
            if (Files.notExists(outDir)) {
                Files.createDirectory(outDir);
            }

            compile(sourceFiles, outDir);
        } catch (IOException e) {
            try {
                FileUtils.removeDir(outDir);
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }
            throw new RuntimeException(e);
        }
    }

    private static List<Path> getSourceFiles(Path srcPath) throws IOException {
        if (srcPath == null || Files.notExists(srcPath)) {
            throw new IllegalArgumentException("Illegal src path: " + srcPath);
        }

        final List<Path> sourceFiles = new ArrayList<>();
        if (!Files.isDirectory(srcPath)) {
            sourceFiles.add(srcPath);
        } else {
            sourceFiles.addAll(
                    Files.walk(srcPath).filter(f -> f.getFileName().toString().endsWith(".jack")).toList()
            );
        }

        return sourceFiles;
    }

    private static void compile(List<Path> srcFiles, Path outDir) throws IOException {
        if (srcFiles == null || srcFiles.isEmpty()) {
            return;
        }

        for (Path src : srcFiles) {
            final Path outFile = outDir.resolve(src.getFileName().toString().replace(".jack", ".xml"));
            Files.deleteIfExists(outFile);
            Files.createFile(outFile);

            try (
                    final CompilationEngine engine = new CompilationEngine(
                            new JackTokenizer(new FileInputStream(src.toFile())),
                            new FileOutputStream(outFile.toFile())
                    )
            ) {
                engine.compileClass();
            }
        }
    }
}