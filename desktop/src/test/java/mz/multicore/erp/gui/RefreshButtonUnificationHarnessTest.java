package mz.multicore.erp.gui;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/** Harness de docs/BOTAO_ACTUALIZAR_UNIFICADO_SPEC.md. */
class RefreshButtonUnificationHarnessTest {

    private static final Path GUI = Path.of("src", "main", "java", "mz", "multicore", "erp", "gui");
    private static final Path FACTORY = GUI.resolve(Path.of("components", "UiFormComponentStyler.java"));
    private static final Pattern HAND_BUILT_REFRESH =
            Pattern.compile("create(Primary|Secondary|Success)Button\\(\"Actualizar\"\\)|new ModernButton\\(\"Actualizar\"");

    @Test
    void refreshButtonsComeOnlyFromTheSharedFactory() throws IOException {
        List<String> offenders;
        try (Stream<Path> files = Files.walk(GUI)) {
            offenders = files.filter(p -> p.toString().endsWith(".java"))
                    .filter(p -> !p.endsWith(FACTORY))
                    .filter(RefreshButtonUnificationHarnessTest::buildsRefreshByHand)
                    .map(p -> GUI.relativize(p).toString())
                    .toList();
        }
        assertThat(offenders)
                .as("Botões \"Actualizar\" devem usar UIHelper.createRefreshButton(...)")
                .isEmpty();
    }

    @Test
    void physicalInventoryKeepsRefreshInHeaderAndFiltersOnOneRow() throws IOException {
        String panel = Files.readString(GUI.resolve(Path.of("inventory", "PhysicalInventoryPanel.java")));
        String view = Files.readString(GUI.resolve(Path.of("inventory", "PhysicalInventoryViewCoordinator.java")));

        assertThat(panel).contains("refreshBtn, printPdfBtn, sessionActionsMenu, newSessionBtn");
        assertThat(view).contains("TableFilter.bar(").doesNotContain("new GridLayout(2, 1");
    }

    private static boolean buildsRefreshByHand(Path file) {
        try {
            return HAND_BUILT_REFRESH.matcher(Files.readString(file)).find();
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }
}
