package edu.nand2tetris.compiler;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

import edu.nadn2tetris.compiler.CompilationEngine;
import edu.nadn2tetris.tokenizer.JackTokenizer;

public final class CompilationEngineTest {
    @Test
    public void testSimpleCompileVarDeclaration() {
        final String line = "var char c;";
        final String expected = """
                <keyword> var </keyword>
                <keyword> char </keyword>
                <identifier> c </identifier>
                <symbol> ; </symbol>
                """;

        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (final JackTokenizer jackTokenizer = new JackTokenizer(new ByteArrayInputStream(line.getBytes()));
             final CompilationEngine compilationEngine = new CompilationEngine(jackTokenizer, baos);
        ) {
            jackTokenizer.advance();
            compilationEngine.compileVarDec();
            compilationEngine.flush();
            
            final BufferedReader reader = new BufferedReader(new InputStreamReader(new ByteArrayInputStream(baos.toByteArray())));
            final String collect = reader.lines().collect(Collectors.joining());
            reader.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
    
    @Test
    public void testCompileIf() {
        final String line = """
                if (x < 0) {
                    if (x < 0) {
                       while (x < 0) {
                            let x = 1;
                        }
                    }
                    
                    let x = 0;
                }
                """;


        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (final JackTokenizer jackTokenizer = new JackTokenizer(new ByteArrayInputStream(line.getBytes()));
             final CompilationEngine compilationEngine = new CompilationEngine(jackTokenizer, baos);
        ) {
            jackTokenizer.advance();
            compilationEngine.compileIf();
            compilationEngine.flush();
            
            final BufferedReader reader = new BufferedReader(new InputStreamReader(new ByteArrayInputStream(baos.toByteArray())));
            String Str = reader.lines().collect(Collectors.joining(""));
            System.out.println(Str);
            reader.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
    
    @Test
    public void testWhileCompilation() {
        final String line = """
                while (i < length) {
                    let a[i] = Keyboard.readInt("ENTER THE NEXT NUMBER: ");
                    let i = i + 1;
                }
                """;

        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (final JackTokenizer jackTokenizer = new JackTokenizer(new ByteArrayInputStream(line.getBytes()));
             final CompilationEngine compilationEngine = new CompilationEngine(jackTokenizer, baos);
        ) {
            jackTokenizer.advance();
            compilationEngine.compileWhile();
            compilationEngine.flush();
            
            final BufferedReader reader = new BufferedReader(new InputStreamReader(new ByteArrayInputStream(baos.toByteArray())));
            String Str = reader.lines().collect(Collectors.joining(""));
            System.out.println(Str);
            reader.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
    
    @Test
    public void testMainCompilation() {
        final String line = """
                class Main {
                    function void main() {
                        var Array a;
                        var int length;
                        var int i, sum;
                	
                	let length = Keyboard.readInt("HOW MANY NUMBERS? ");
                	let a = Array.new(length);
                	let i = 0;
                	
                	while (i < length) {
                	    let a[i] = Keyboard.readInt("ENTER THE NEXT NUMBER: ");
                	    let i = i + 1;
                	}
                	
                	let i = 0;
                	let sum = 0;
                	
                	while (i < length) {
                	    let sum = sum + a[i];
                	    let i = i + 1;
                	}
                	
                	do Output.printString("THE AVERAGE IS: ");
                	do Output.printInt(sum / length);
                	do Output.println();
                	
                	return;
                    }
                }
                """;

        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (final JackTokenizer jackTokenizer = new JackTokenizer(new ByteArrayInputStream(line.getBytes()));
             final CompilationEngine compilationEngine = new CompilationEngine(jackTokenizer, baos);
        ) {
            jackTokenizer.advance();
            compilationEngine.compileClass();
            compilationEngine.flush();
            
            final BufferedReader reader = new BufferedReader(new InputStreamReader(new ByteArrayInputStream(baos.toByteArray())));
            String Str = reader.lines().collect(Collectors.joining(""));
            System.out.println(Str);
            reader.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
} 

