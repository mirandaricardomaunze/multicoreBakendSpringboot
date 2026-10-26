package mz.multicore.erp.gui;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class PromotionsPanelDecompositionHarnessTest {

    private static final Path GUI_DIR = Path.of("src", "main", "java", "mz", "multicore", "erp", "gui");

    @Test
    void promo01_lineCeilingsRespected() throws IOException {
        Path panel = GUI_DIR.resolve("PromotionsPanel.java");
        Path dialog = GUI_DIR.resolve("PromotionFormDialog.java");
        Path coordinator = GUI_DIR.resolve("PromotionsOperationsCoordinator.java");

        assertThat(Files.lines(panel).count())
                .as("PromotionsPanel deve manter foco estrito de layout e tabela")
                .isLessThanOrEqualTo(175);

        assertThat(Files.lines(dialog).count())
                .as("PromotionFormDialog deve gerenciar modal de promoção")
                .isLessThanOrEqualTo(125);

        assertThat(Files.lines(coordinator).count())
                .as("PromotionsOperationsCoordinator gerencia benefícios e Quick Peek")
                .isLessThanOrEqualTo(65);
    }

    @Test
    void promo02_cleanArchitectureAndZeroJOptionPane() throws IOException {
        for (String file : new String[]{"PromotionsPanel.java", "PromotionFormDialog.java", "PromotionsOperationsCoordinator.java"}) {
            String content = Files.readString(GUI_DIR.resolve(file));
            assertThat(content)
                    .as(file + " não deve usar JOptionPane legado")
                    .doesNotContain("JOptionPane.");
        }
    }

    @Test
    void promo03_layoutAndInvariantsPreserved() throws IOException {
        String content = Files.readString(GUI_DIR.resolve("PromotionsPanel.java"));
        assertThat(content)
                .contains("TableFilter.bar(")
                .contains("UIHelper.ensureHeadersFit(table)")
                .contains("TableQuickPeekController.install(table")
                .contains("ClientTablePagination.install(table)")
                .contains("UIHelper.tableCardTop(\"Promoções de Loja\"")
                .doesNotContain("promoFilters = new JPanel(new GridBagLayout())");
    }
}
