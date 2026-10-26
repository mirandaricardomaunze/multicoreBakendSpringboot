package mz.multicore.erp.gui;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class HRLiabilitiesPanelDecompositionHarnessTest {

    private static final Path GUI_DIR = Path.of("src", "main", "java", "mz", "multicore", "erp", "gui");

    @Test
    void hrLiab01_lineCeilingsRespected() throws IOException {
        Path panel = GUI_DIR.resolve("HRLiabilitiesPanel.java");
        Path policyDialog = GUI_DIR.resolve("HrPolicyConfigDialog.java");
        Path deliveryDialog = GUI_DIR.resolve("HrLiabilityDeliveryDialog.java");
        Path coordinator = GUI_DIR.resolve("HRLiabilitiesOperationsCoordinator.java");

        assertThat(Files.lines(panel).count())
                .as("HRLiabilitiesPanel deve manter foco estrito de layout e view")
                .isLessThanOrEqualTo(185);

        assertThat(Files.lines(policyDialog).count())
                .as("HrPolicyConfigDialog deve encapsular modal de políticas legais")
                .isLessThanOrEqualTo(95);

        assertThat(Files.lines(deliveryDialog).count())
                .as("HrLiabilityDeliveryDialog deve substituir JOptionPane")
                .isLessThanOrEqualTo(55);

        assertThat(Files.lines(coordinator).count())
                .as("HRLiabilitiesOperationsCoordinator gerencia operações e visual")
                .isLessThanOrEqualTo(85);
    }

    @Test
    void hrLiab02_cleanArchitectureAndZeroJOptionPane() throws IOException {
        for (String file : new String[]{"HRLiabilitiesPanel.java", "HrPolicyConfigDialog.java", "HrLiabilityDeliveryDialog.java", "HRLiabilitiesOperationsCoordinator.java"}) {
            String content = Files.readString(GUI_DIR.resolve(file));
            assertThat(content)
                    .as(file + " não deve conter chamadas legadas a JOptionPane")
                    .doesNotContain("JOptionPane.");
        }
    }

    @Test
    void hrLiab03_layoutAndContractInvariantsPreserved() throws IOException {
        String content = Files.readString(GUI_DIR.resolve("HRLiabilitiesPanel.java"));
        assertThat(content)
                .contains("UIHelper.ensureHeadersFit(table)")
                .contains("TableQuickPeekController.install(table")
                .contains("UIHelper.tableCardTop(\"Retenções por Entregar\", bar,")
                .contains("refreshBtn, actionsBtn, deliverBtn)")
                .contains("Apurar Período")
                .contains("Valores Legais");
    }
}
