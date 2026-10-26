package mz.multicore.erp.gui.commercial;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class OutstandingAccountsPanelDecompositionHarnessTest {

    private static final Path COMMERCIAL_DIR = Path.of("src", "main", "java", "mz", "multicore", "erp", "gui", "commercial");

    @Test
    void out01_lineCeilingsRespected() throws IOException {
        Path panel = COMMERCIAL_DIR.resolve("OutstandingAccountsPanel.java");
        Path dialog = COMMERCIAL_DIR.resolve("OutstandingPaymentDialog.java");
        Path coordinator = COMMERCIAL_DIR.resolve("OutstandingAccountsOperationsCoordinator.java");

        assertThat(Files.lines(panel).count())
                .as("OutstandingAccountsPanel deve ser enxuto e manter responsabilidade visual")
                .isLessThanOrEqualTo(205);

        assertThat(Files.lines(dialog).count())
                .as("OutstandingPaymentDialog deve focar no formulário modal de pagamento")
                .isLessThanOrEqualTo(90);

        assertThat(Files.lines(coordinator).count())
                .as("OutstandingAccountsOperationsCoordinator coordena ações e visual sem inflar")
                .isLessThanOrEqualTo(115);
    }

    @Test
    void out02_cleanArchitectureAndZeroJOptionPane() throws IOException {
        for (String file : new String[]{"OutstandingAccountsPanel.java", "OutstandingPaymentDialog.java", "OutstandingAccountsOperationsCoordinator.java"}) {
            String content = Files.readString(COMMERCIAL_DIR.resolve(file));
            assertThat(content)
                    .as(file + " não deve usar JOptionPane legado")
                    .doesNotContain("JOptionPane.");
        }
    }

    @Test
    void out03_actionMenuAndLayoutInvariantsPreserved() throws IOException {
        String content = Files.readString(COMMERCIAL_DIR.resolve("OutstandingAccountsPanel.java"));
        assertThat(content)
                .contains("TableFilter.bar(")
                .contains("UIHelper.ensureHeadersFit(table)")
                .contains("TableQuickPeekController.install(table")
                .contains("UIHelper.tableCardTop(")
                .contains("ClientTablePagination.install(table)")
                .contains("ActionMenuButton")
                .contains("Radar Preventivo")
                .contains("Prestações")
                .contains("Devolver Fatura");
    }
}
