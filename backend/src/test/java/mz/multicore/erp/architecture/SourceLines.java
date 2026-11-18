package mz.multicore.erp.architecture;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Size of a source file as the decomposition harnesses mean it: lines of code, not documentation. Comment
 * lines are left out so that documenting a class never pushes it over its size limit.
 */
public final class SourceLines {

    private SourceLines() {
    }

    /** Counts the lines of the file that are not comment lines ({@code //}, {@code /*}, {@code *}). */
    public static int code(Path file) {
        try {
            return (int) Files.readAllLines(file).stream()
                    .map(String::trim)
                    .filter(line -> !line.startsWith("//") && !line.startsWith("/*") && !line.startsWith("*"))
                    .count();
        } catch (IOException e) {
            throw new UncheckedIOException("Could not read " + file, e);
        }
    }
}
