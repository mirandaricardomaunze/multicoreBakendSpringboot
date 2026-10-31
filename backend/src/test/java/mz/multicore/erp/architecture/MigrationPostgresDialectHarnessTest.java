package mz.multicore.erp.architecture;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Corre em todo o `mvn test`, sem PostgreSQL: a suite usa H2 sem Flyway, por isso a sintaxe de
 * outro dialecto nas migrações passaria despercebida. Ver POSTGRES_SCHEMA_PARITY_SPEC.
 */
class MigrationPostgresDialectHarnessTest {
    private static final Pattern FORBIDDEN = Pattern.compile(
            "(?i)\\b(AUTO_INCREMENT|DATETIME|CLOB|BLOB|TINYINT|LONGTEXT)\\b");

    private static Path migrations() {
        Path fromModule = Path.of("src/main/resources/db/migration");
        return Files.isDirectory(fromModule) ? fromModule : Path.of("backend").resolve(fromModule);
    }

    @Test
    void pg01_migracoesNaoUsamSintaxeForaDoPostgres() throws IOException {
        List<String> violations = new ArrayList<>();
        try (Stream<Path> files = Files.list(migrations())) {
            for (Path file : files.filter(f -> f.getFileName().toString().endsWith(".sql")).toList()) {
                List<String> lines = Files.readAllLines(file);
                for (int i = 0; i < lines.size(); i++) {
                    String code = lines.get(i).replaceFirst("--.*", "");
                    if (FORBIDDEN.matcher(code).find()) {
                        violations.add(file.getFileName() + ":" + (i + 1) + " " + code.trim());
                    }
                }
            }
        }
        assertThat(violations).isEmpty();
    }
}
