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

public final class JackAnalyzer {

    public static void main(String[] args) throws IOException {
        if (args.length < 2) {
            throw new IllegalArgumentException("Invalid arguments count: " + args.length);
        }

        final Path path = Paths.get(args[0]);
        final List<Path> srcFils = new ArrayList<>();
        if (!Files.isDirectory(path)) {
            srcFils.add(path);
        } else {
            srcFils.addAll(
                    Files.walk(path).filter(f -> f.getFileName().toString().endsWith(".jack")).toList()
            );
        }

        final Path outDir = Paths.get(args[1]);
        if (Files.notExists(outDir)) {
            Files.createDirectory(outDir);
        }

        for (Path src : srcFils) {
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