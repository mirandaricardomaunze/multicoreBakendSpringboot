package com.phcpro.gui;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class UiPanelDecompositionTest {
    private static final int MAX_PANEL_LINES = 1_000;
    private static final List<String> PRIORITY_PANELS = List.of(
            "ComercialPanel.java", "StockPanel.java", "POSPanel.java",
            "ComprasPanel.java", "HRPanel.java", "ConfigPanel.java");

    @Test
    void priorityPanelsRemainDecomposedByUseCase() throws IOException {
        Path gui = Path.of("src", "main", "java", "com", "phcpro", "gui");
        for (String fileName : PRIORITY_PANELS) {
            Path source = gui.resolve(fileName);
            assertThat(source).as("fonte do painel %s", fileName).exists();
            long lines;
            try (var stream = Files.lines(source)) {
                lines = stream.count();
            }
            assertThat(lines)
                    .as("%s deve permanecer abaixo de %d linhas", fileName, MAX_PANEL_LINES)
                    .isLessThanOrEqualTo(MAX_PANEL_LINES);
        }
    }
}
