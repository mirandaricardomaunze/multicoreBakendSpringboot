package mz.multicore.erp.gui.components;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Harness de validação estrutural da decomposição de KpiCard (Fase 92).
 * Valida limites estritos de linhas, ausência de JOptionPane, e delegação para helpers especializados.
 */
class KpiCardDecompositionHarnessTest {

    private static String readSource(String relativePath) {
        try {
            return Files.readString(Path.of(relativePath));
        } catch (IOException e) {
            throw new RuntimeException("Falha ao ler " + relativePath, e);
        }
    }

    private static long countLines(String relativePath) {
        try {
            return Files.lines(Path.of(relativePath)).count();
        } catch (IOException e) {
            throw new RuntimeException("Falha ao contar linhas em " + relativePath, e);
        }
    }

    @Test
    @DisplayName("KPID-01: Limites físicos estritos de linhas de código")
    void kpid01_limitesFisicosLinhasCodigo() {
        long cardLines = countLines("src/main/java/mz/multicore/erp/gui/components/KpiCard.java");
        long paletteLines = countLines("src/main/java/mz/multicore/erp/gui/components/KpiCardPalette.java");
        long badgeLines = countLines("src/main/java/mz/multicore/erp/gui/components/TrendBadge.java");
        long helperLines = countLines("src/main/java/mz/multicore/erp/gui/components/KpiCardInteractiveHelper.java");

        assertThat(cardLines)
                .as("KpiCard.java deve ter <= 250 linhas (reduzido de 579 linhas)")
                .isLessThanOrEqualTo(250);

        assertThat(paletteLines)
                .as("KpiCardPalette.java deve ter <= 180 linhas")
                .isLessThanOrEqualTo(180);

        assertThat(badgeLines)
                .as("TrendBadge.java deve ter <= 115 linhas")
                .isLessThanOrEqualTo(115);

        assertThat(helperLines)
                .as("KpiCardInteractiveHelper.java deve ter <= 50 linhas")
                .isLessThanOrEqualTo(50);
    }

    @Test
    @DisplayName("KPID-02: Zero diálogos legados JOptionPane")
    void kpid02_zeroLegadoJOptionPane() {
        for (String file : List.of(
                "src/main/java/mz/multicore/erp/gui/components/KpiCard.java",
                "src/main/java/mz/multicore/erp/gui/components/KpiCardPalette.java",
                "src/main/java/mz/multicore/erp/gui/components/TrendBadge.java",
                "src/main/java/mz/multicore/erp/gui/components/KpiCardInteractiveHelper.java"
        )) {
            String src = readSource(file);
            assertThat(src)
                    .as(file + " não deve conter chamadas a JOptionPane")
                    .doesNotContain("JOptionPane.");
        }
    }

    @Test
    @DisplayName("KPID-03: Delegação canónica para helpers especializados")
    void kpid03_delegacaoCanonicaParaHelpers() {
        String card = readSource("src/main/java/mz/multicore/erp/gui/components/KpiCard.java");

        assertThat(card)
                .as("KpiCard deve delegar resolução de paleta para KpiCardPalette")
                .contains("KpiCardPalette.resolvePalette(");

        assertThat(card)
                .as("KpiCard deve delegar interatividade para KpiCardInteractiveHelper")
                .contains("KpiCardInteractiveHelper.makeInteractive(");

        assertThat(card)
                .as("KpiCard.TrendBadge deve herdar de TrendBadge")
                .contains("class TrendBadge extends mz.multicore.erp.gui.components.TrendBadge");
    }

    @Test
    @DisplayName("KPID-04: Contratos e dimensões padrão preservados")
    void kpid04_contratosPreservados() {
        String card = readSource("src/main/java/mz/multicore/erp/gui/components/KpiCard.java");

        assertThat(card)
                .contains("STANDARD_CARD_HEIGHT = 96")
                .contains("STANDARD_CARD_MIN_WIDTH = 140")
                .contains("createGrid(")
                .contains("createCard(")
                .contains("createMetricCard(")
                .contains("createInteractiveCard(")
                .contains("createInteractiveMetricCard(");
    }
}
