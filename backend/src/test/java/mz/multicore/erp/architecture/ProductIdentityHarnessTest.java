package mz.multicore.erp.architecture;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertTrue;

class ProductIdentityHarnessTest {
    private static final Set<String> TEXT_EXTENSIONS = Set.of(
            ".java", ".md", ".xml", ".properties", ".yml", ".yaml",
            ".bat", ".json", ".sql", ".sh", ".example", ".txt");

    @Test
    void repositoryUsesOnlyMulticoreIdentity() throws IOException {
        String forbiddenIdentity = new String(new char[]{'p', 'h', 'c'});
        List<Path> violations;
        try (var paths = Files.walk(Path.of("."))) {
            violations = paths
                    .filter(path -> !isIgnored(path))
                    .filter(path -> contains(path.getFileName().toString(), forbiddenIdentity)
                            || containsForbiddenText(path, forbiddenIdentity))
                    .toList();
        }
        assertTrue(violations.isEmpty(), () -> "Identidade externa encontrada em: " + violations);
    }

    private static boolean isIgnored(Path path) {
        String normalized = path.normalize().toString().replace('\\', '/');
        return normalized.startsWith("./.git/") || normalized.startsWith("./target/")
                || normalized.equals(".git") || normalized.equals("target");
    }

    private static boolean containsForbiddenText(Path path, String forbiddenIdentity) {
        if (!Files.isRegularFile(path) || !isTextFile(path)) return false;
        try {
            return contains(Files.readString(path, StandardCharsets.UTF_8), forbiddenIdentity);
        } catch (IOException exception) {
            throw new IllegalStateException("Não foi possível verificar " + path, exception);
        }
    }

    private static boolean isTextFile(Path path) {
        String name = path.getFileName().toString();
        if (name.equals("Dockerfile")) return true;
        int dot = name.lastIndexOf('.');
        return dot >= 0 && TEXT_EXTENSIONS.contains(name.substring(dot).toLowerCase(Locale.ROOT));
    }

    private static boolean contains(String value, String token) {
        return value.toLowerCase(Locale.ROOT).contains(token);
    }
}
