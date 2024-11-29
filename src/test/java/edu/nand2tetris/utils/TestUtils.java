package edu.nand2tetris.utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

import org.junit.jupiter.api.Assertions;

public final class TestUtils {
    private TestUtils() {}

    public static void compareFiles(File firstFile, File secondFile) throws IOException {
        final BufferedReader testReader = new BufferedReader(new FileReader(firstFile));
        final BufferedReader compiledReader = new BufferedReader(new FileReader(secondFile));

        String first;
        String second;
        while ((first = testReader.readLine()) != null && (second = compiledReader.readLine()) != null) {
            Assertions.assertEquals(first, second);
        }
    }
}
