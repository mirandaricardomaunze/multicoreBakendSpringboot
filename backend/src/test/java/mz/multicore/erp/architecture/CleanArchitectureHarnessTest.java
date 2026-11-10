package mz.multicore.erp.architecture;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Permanent clean-architecture harness (docs/CLEAN_ARCHITECTURE_RULES_SPEC.md, CA-01..CA-08).
 * Legacy violations live in frozen baselines that may only shrink; new code must comply.
 */
class CleanArchitectureHarnessTest {
    private static final Path ROOT = Files.isDirectory(Path.of("tasks"))
            ? Path.of("").toAbsolutePath().normalize()
            : Path.of("..").toAbsolutePath().normalize();
    private static final Path BACKEND = ROOT.resolve(Path.of("backend", "src", "main", "java"));
    private static final Path DESKTOP = ROOT.resolve(Path.of("desktop", "src", "main", "java"));
    private static final Path CONTRACTS = ROOT.resolve(Path.of("contracts", "src", "main", "java"));
    private static final Path BASELINES = ROOT.resolve(Path.of("backend", "src", "test", "resources", "architecture"));

    private static final int MAX_LINES = 600;
    /** Files already above MAX_LINES; each may not grow beyond the recorded size. */
    private static final Map<String, Integer> OVERSIZE_BASELINE = Map.of(
            "desktop/src/main/java/mz/multicore/erp/gui/MainFrame.java", 744);
    /** Controllers that still map an entity themselves. Must only shrink. */
    private static final Set<String> CONTROLLER_ENTITY_BASELINE = Set.of(
            "backend/src/main/java/mz/multicore/erp/modules/audit/controller/AuditController.java -> AuditLog",
            "backend/src/main/java/mz/multicore/erp/modules/users/controller/UserController.java -> AppUser");

    private static final Pattern MODULE_OF_FILE = Pattern.compile("/modules/([a-z0-9]+)/");
    private static final Pattern REPOSITORY_IMPORT =
            Pattern.compile("^import mz\\.multicore\\.erp\\.modules\\.([a-z0-9]+)\\.repository\\.(\\w+);", Pattern.MULTILINE);
    private static final Pattern MODEL_IMPORT =
            Pattern.compile("^import mz\\.multicore\\.erp\\.modules\\.[a-z0-9]+\\.model\\.(\\w+);", Pattern.MULTILINE);
    private static final Pattern FIELD_INJECTION = Pattern.compile(
            "@(Autowired|Inject|Resource)\\s*(?:\\([^)]*\\))?\\s+(?:@\\w+(?:\\([^)]*\\))?\\s+)*"
                    + "(?:private|protected|public)?\\s*(?:static\\s+)?[A-Z][\\w<>,.? ]*\\s+\\w+\\s*(?:=[^;]*)?;");

    @Test
    void ca01_controllersNeverTouchRepositories() {
        List<String> violations = javaFiles(BACKEND).stream()
                .filter(CleanArchitectureHarnessTest::isController)
                .filter(f -> REPOSITORY_IMPORT.matcher(read(f)).find())
                .map(CleanArchitectureHarnessTest::rel)
                .toList();
        assertThat(violations).as("CA-01 Controller -> Repository").isEmpty();
    }

    @Test
    void ca02_controllersNeverHandleEntities() {
        Set<String> entities = javaFiles(BACKEND).stream()
                .filter(f -> f.toString().replace('\\', '/').contains("/model/"))
                .filter(f -> read(f).contains("@Entity"))
                .map(f -> f.getFileName().toString().replace(".java", ""))
                .collect(Collectors.toSet());
        Set<String> found = new TreeSet<>();
        for (Path file : javaFiles(BACKEND)) {
            if (!isController(file)) {
                continue;
            }
            Matcher m = MODEL_IMPORT.matcher(read(file));
            while (m.find()) {
                if (entities.contains(m.group(1))) {
                    found.add(rel(file) + " -> " + m.group(1));
                }
            }
        }
        assertThat(found).as("CA-02 new Controller -> @Entity (use DTOs from the service)")
                .isSubsetOf(CONTROLLER_ENTITY_BASELINE);
    }

    @Test
    void ca03_constructorInjectionOnly() {
        List<String> violations = Stream.of(BACKEND, DESKTOP)
                .flatMap(root -> javaFiles(root).stream())
                .filter(f -> FIELD_INJECTION.matcher(read(f)).find())
                .map(CleanArchitectureHarnessTest::rel)
                .toList();
        assertThat(violations).as("CA-03 field injection").isEmpty();
    }

    @Test
    void ca04_httpStaysInControllers() {
        List<String> violations = javaFiles(BACKEND).stream()
                .filter(f -> {
                    String p = f.toString().replace('\\', '/');
                    return p.contains("/service/") || p.contains("/repository/") || p.contains("/model/");
                })
                .filter(f -> {
                    String s = read(f);
                    return s.contains("import org.springframework.web.bind.annotation.")
                            || s.contains("import org.springframework.http.ResponseEntity;")
                            || s.contains("import jakarta.servlet.");
                })
                .map(CleanArchitectureHarnessTest::rel)
                .toList();
        assertThat(violations).as("CA-04 HTTP types outside controllers").isEmpty();
    }

    @Test
    void ca05_repositoriesArePersistenceInterfaces() {
        List<String> violations = javaFiles(BACKEND).stream()
                .filter(f -> f.toString().replace('\\', '/').contains("/repository/"))
                .filter(f -> f.getFileName().toString().endsWith("Repository.java"))
                .filter(f -> !read(f).contains("interface "))
                .map(CleanArchitectureHarnessTest::rel)
                .toList();
        assertThat(violations).as("CA-05 repository must be an interface").isEmpty();
    }

    @Test
    void ca06_singleResponsibilitySizeLimit() {
        List<String> violations = new ArrayList<>();
        for (Path root : List.of(BACKEND, DESKTOP, CONTRACTS)) {
            for (Path file : javaFiles(root)) {
                int lines = lineCount(file);
                String rel = rel(file);
                int limit = OVERSIZE_BASELINE.getOrDefault(rel, MAX_LINES);
                if (lines > limit) {
                    violations.add(rel + " has " + lines + " lines (limit " + limit + ")");
                }
            }
        }
        assertThat(violations).as("CA-06 class too large: extract a collaborator (SRP)").isEmpty();
    }

    @Test
    void ca07_noNewCrossModuleRepositoryCoupling() throws IOException {
        Set<String> found = new TreeSet<>();
        for (Path file : javaFiles(BACKEND)) {
            Matcher owner = MODULE_OF_FILE.matcher(file.toString().replace('\\', '/'));
            String own = owner.find() ? owner.group(1) : "";
            Matcher m = REPOSITORY_IMPORT.matcher(read(file));
            while (m.find()) {
                if (!m.group(1).equals(own)) {
                    found.add(rel(file) + " -> " + m.group(1) + ".repository." + m.group(2));
                }
            }
        }
        Path baselineFile = BASELINES.resolve("cross-module-repository-baseline.txt");
        if (Boolean.getBoolean("architecture.baseline.write")) {
            Files.createDirectories(BASELINES);
            Files.writeString(baselineFile, String.join("\n", found) + "\n", StandardCharsets.UTF_8);
        }
        Set<String> baseline = Files.readAllLines(baselineFile, StandardCharsets.UTF_8).stream()
                .map(String::trim).filter(s -> !s.isEmpty() && !s.startsWith("#"))
                .collect(Collectors.toSet());
        Set<String> added = new TreeSet<>(found);
        added.removeAll(baseline);
        assertThat(added).as("CA-07 new cross-module repository coupling: call the owning module's service")
                .isEmpty();
    }

    @Test
    void ca08_contractsStayDependencyFree() {
        List<String> violations = javaFiles(CONTRACTS).stream()
                .filter(f -> {
                    String s = read(f);
                    return s.contains("import mz.multicore.erp.modules.") && s.contains(".service.")
                            || s.contains("import javax.swing") || s.contains("import jakarta.persistence");
                })
                .map(CleanArchitectureHarnessTest::rel)
                .toList();
        assertThat(violations).as("CA-08 contracts depend on nothing else").isEmpty();
    }

    private static boolean isController(Path file) {
        String p = file.toString().replace('\\', '/');
        return p.contains("/controller/") && file.getFileName().toString().endsWith(".java");
    }

    private static List<Path> javaFiles(Path root) {
        if (!Files.isDirectory(root)) {
            return List.of();
        }
        try (Stream<Path> walk = Files.walk(root)) {
            return walk.filter(p -> p.toString().endsWith(".java")).sorted().toList();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static String read(Path file) {
        try {
            return Files.readString(file, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static int lineCount(Path file) {
        try (Stream<String> lines = Files.lines(file, StandardCharsets.UTF_8)) {
            return (int) lines.count();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static String rel(Path file) {
        return ROOT.relativize(file.toAbsolutePath().normalize()).toString().replace('\\', '/');
    }
}
