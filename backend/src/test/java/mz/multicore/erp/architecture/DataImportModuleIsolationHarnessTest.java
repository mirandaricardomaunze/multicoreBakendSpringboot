package mz.multicore.erp.architecture;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * IMP-11 (docs/IMPORTACAO_DADOS_SPEC.md §3.3): the data import module writes only through the owner
 * services and never touches another module's repository — not by import, wildcard import or fully
 * qualified name. Stricter than {@link CrossModuleRepositoryScanner} (named imports only) because the
 * module is new and must stay clean.
 */
class DataImportModuleIsolationHarnessTest {

    private static final Path ROOT = CrossModuleRepositoryScanner.ROOT;
    private static final Pattern FOREIGN_REPOSITORY = Pattern.compile(
            "mz\\.multicore\\.erp\\.modules\\.(?!dataimport\\.)([a-z0-9]+)\\.repository\\b");

    @Test
    void dataImportModuleNeverReferencesAnotherModulesRepository() throws IOException {
        Path module = ROOT.resolve(Path.of("backend", "src", "main", "java",
                "mz", "multicore", "erp", "modules", "dataimport"));
        assertThat(module).isDirectory();

        List<String> offenders;
        try (Stream<Path> walk = Files.walk(module)) {
            offenders = walk.filter(p -> p.toString().endsWith(".java"))
                    .flatMap(DataImportModuleIsolationHarnessTest::foreignReferences)
                    .toList();
        }
        assertThat(offenders)
                .as("IMP-11: modules/dataimport must use the owner's Service/Lookup, never its Repository")
                .isEmpty();
    }

    private static Stream<String> foreignReferences(Path file) {
        try {
            Matcher m = FOREIGN_REPOSITORY.matcher(Files.readString(file, StandardCharsets.UTF_8));
            List<String> found = new ArrayList<>();
            while (m.find()) {
                found.add(ROOT.relativize(file.toAbsolutePath().normalize()).toString().replace('\\', '/')
                        + " -> " + m.group(1) + ".repository");
            }
            return found.stream();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
