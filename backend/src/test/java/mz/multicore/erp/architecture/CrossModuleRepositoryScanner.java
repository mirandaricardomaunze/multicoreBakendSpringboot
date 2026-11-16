package mz.multicore.erp.architecture;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * Finds backend source files that import a repository owned by another module.
 * Shared by CleanArchitectureHarnessTest (CA-07) and ModuleDecouplingHarnessTest (MD-*).
 */
final class CrossModuleRepositoryScanner {
    static final Path ROOT = Files.isDirectory(Path.of("tasks"))
            ? Path.of("").toAbsolutePath().normalize()
            : Path.of("..").toAbsolutePath().normalize();
    private static final Path BACKEND = ROOT.resolve(Path.of("backend", "src", "main", "java"));
    private static final Pattern MODULE_OF_FILE = Pattern.compile("/modules/([a-z0-9]+)/");
    private static final Pattern REPOSITORY_IMPORT =
            Pattern.compile("^import mz\\.multicore\\.erp\\.modules\\.([a-z0-9]+)\\.repository\\.(\\w+);", Pattern.MULTILINE);

    private CrossModuleRepositoryScanner() {
    }

    /** Link format: {@code backend/src/main/java/.../File.java -> module.repository.ClassName}. */
    static Set<String> links() {
        Set<String> found = new TreeSet<>();
        try (Stream<Path> walk = Files.walk(BACKEND)) {
            for (Path file : walk.filter(p -> p.toString().endsWith(".java")).toList()) {
                String unix = file.toString().replace('\\', '/');
                Matcher owner = MODULE_OF_FILE.matcher(unix);
                String own = owner.find() ? owner.group(1) : "";
                Matcher m = REPOSITORY_IMPORT.matcher(Files.readString(file, StandardCharsets.UTF_8));
                while (m.find()) {
                    if (!m.group(1).equals(own)) {
                        found.add(rel(file) + " -> " + m.group(1) + ".repository." + m.group(2));
                    }
                }
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return found;
    }

    /** Target part of a link, e.g. {@code company.repository.CompanyRepository}. */
    static String target(String link) {
        return link.substring(link.indexOf(" -> ") + 4);
    }

    private static String rel(Path file) {
        return ROOT.relativize(file.toAbsolutePath().normalize()).toString().replace('\\', '/');
    }
}
