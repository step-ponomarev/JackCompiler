package edu.nadn2tetris;

import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import edu.nadn2tetris.ast.AbstractSyntaxTree;
import edu.nadn2tetris.ast.processor.ByteCodeAstGenerator;
import edu.nadn2tetris.ast.processor.SymbolTableAstGenerator;
import edu.nadn2tetris.ast.processor.XmlTreeAstGenerator;
import edu.nadn2tetris.ast.AstParser;
import edu.nadn2tetris.conf.Flag;
import edu.nadn2tetris.tokenizer.JackTokenizer;
import edu.nadn2tetris.tokenizer.TokensCompiler;
import edu.nadn2tetris.utils.FileUtils;
import edu.nadn2tetris.writer.VMWriter;

public final class JackCompiler {

    public static void main(String[] args) {
        if (args.length < 2) {
            throw new IllegalArgumentException("Invalid arguments count: " + args.length);
        }

        final Set<Flag> flags = new HashSet<>();
        for (short i = 2; i < args.length; i++) {
            flags.add(Flag.parse(args[i]));
        }

        final Path outDir = Paths.get(args[1]);
        try {
            final List<Path> sourceFiles = getSourceFiles(Paths.get(args[0]));
            if (Files.notExists(outDir)) {
                Files.createDirectory(outDir);
            }

            compile(sourceFiles, outDir, flags);
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

    private static void compile(List<Path> srcFiles, Path outDir, Set<Flag> flags) throws IOException {
        if (srcFiles == null || srcFiles.isEmpty()) {
            return;
        }

        for (Path src : srcFiles) {
            final Path outFile = outDir.resolve(src.getFileName().toString().replace(".jack", flags.contains(Flag.GENERATE_CODE) ? ".vm" : ".xml"));
            Files.deleteIfExists(outFile);
            Files.createFile(outFile);

            //TODO: режимы взаимо исключающие
            if (flags.contains(Flag.XML_MODE)) {
                compileXml(src, outFile);
                continue;
            }

            if (flags.contains(Flag.TOKENS)) {
                compileTokens(src, outFile);
                continue;
            }

            if (flags.contains(Flag.GENERATE_CODE)) {
                compileByteCode(src, outFile);
            }
        }
    }

    private static void compileByteCode(Path src, Path outFile) throws IOException {
        try (
                final AstParser engine = new AstParser(
                        new JackTokenizer(new FileInputStream(src.toFile()))
                )
        ) {
            final AbstractSyntaxTree classTree = engine.compileClass();
            try (final ByteCodeAstGenerator byteCodeGenerator = new ByteCodeAstGenerator(
                    new SymbolTableAstGenerator().generate(classTree),
                    new VMWriter(Files.newBufferedWriter(outFile))
            )) {
                byteCodeGenerator.generate(classTree);
            }
        }
    }

    private static void compileXml(Path src, Path outDir) throws IOException {
        try (
                final AstParser engine = new AstParser(
                        new JackTokenizer(new FileInputStream(src.toFile()))
                );
                final BufferedWriter writer = Files.newBufferedWriter(outDir);
        ) {
            final AbstractSyntaxTree abstractSyntaxTree = engine.compileClass();
            writer.write(new XmlTreeAstGenerator().generate(abstractSyntaxTree));
        }
    }

    private static void compileTokens(Path src, Path outDir) throws IOException {
        try (
                final TokensCompiler tokensCompiler = new TokensCompiler(
                        new JackTokenizer(new FileInputStream(src.toFile()))
                );
                final BufferedWriter writer = Files.newBufferedWriter(outDir);
        ) {
            writer.write(tokensCompiler.generate());
        }
    }
}