package mz.multicore.erp.architecture;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Module decoupling waves (docs/MODULE_DECOUPLING_SPEC.md). Each wave is enforced as soon as its goal
 * is DONE, so a goal cannot be closed while the coupling it targets still exists. Progress for all
 * waves is written to backend/target/module-decoupling-report.md.
 */
class ModuleDecouplingHarnessTest {
    private static final Path ROOT = CrossModuleRepositoryScanner.ROOT;
    private static final Set<String> PRODUCT_REPOSITORIES = Set.of(
            "comercial.repository.ProductRepository", "comercial.repository.ProductCategoryRepository");

    private record Wave(String goalId, String scope, Predicate<String> target) {
    }

    private static final List<Wave> WAVES = List.of(
            new Wave("GOAL-001", "company.repository.*", t -> t.startsWith("company.")),
            new Wave("GOAL-002", "Product/ProductCategory", PRODUCT_REPOSITORIES::contains),
            new Wave("GOAL-003", "comercial.repository.*", t -> t.startsWith("comercial.")),
            new Wave("GOAL-004", "inventory.repository.*", t -> t.startsWith("inventory.")),
            new Wave("GOAL-005", "todos os modulos", t -> true));

    @Test
    void doneWavesHaveNoCrossModuleRepositoryAccess() throws IOException {
        Set<String> links = CrossModuleRepositoryScanner.links();
        StringBuilder report = new StringBuilder("# Module decoupling progress\n\n")
                .append("| Goal | Scope | Status | Remaining links |\n|---|---|---|---|\n");

        for (Wave wave : WAVES) {
            List<String> remaining = links.stream()
                    .filter(l -> wave.target().test(CrossModuleRepositoryScanner.target(l)))
                    .toList();
            String status = goalStatus(wave.goalId()).orElse("absent");
            report.append("| ").append(wave.goalId()).append(" | ").append(wave.scope()).append(" | ")
                    .append(status).append(" | ").append(remaining.size()).append(" |\n");
            if ("DONE".equals(status)) {
                assertThat(remaining)
                        .as("%s is DONE but %s is still read from other modules (use the owner's *Lookup/Service)",
                                wave.goalId(), wave.scope())
                        .isEmpty();
            }
        }
        Path out = ROOT.resolve(Path.of("backend", "target", "module-decoupling-report.md"));
        Files.createDirectories(out.getParent());
        Files.writeString(out, report.toString(), StandardCharsets.UTF_8);
    }

    private static Optional<String> goalStatus(String goalId) throws IOException {
        Path goals = ROOT.resolve(Path.of("tasks", "goals"));
        if (!Files.isDirectory(goals)) {
            return Optional.empty();
        }
        try (Stream<Path> files = Files.list(goals)) {
            Optional<Path> file = files.filter(p -> p.getFileName().toString().startsWith(goalId + "-")).findFirst();
            if (file.isEmpty()) {
                return Optional.empty();
            }
            return Files.readAllLines(file.get(), StandardCharsets.UTF_8).stream()
                    .filter(line -> line.startsWith("status:"))
                    .map(line -> line.substring("status:".length()).trim())
                    .findFirst();
        }
    }
}
