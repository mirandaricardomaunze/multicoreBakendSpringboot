package mz.multicore.erp.gui;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class FinanceiroPanelDecompositionHarnessTest {

    private static final Path GUI_ROOT = Path.of("src", "main", "java", "mz", "multicore", "erp", "gui");

    @Test
    @DisplayName("FIN-01: Tetos de linhas de código estritamente respeitados na decomposição")
    void fin01_lineCeilingsRespected() throws Exception {
        Path panelPath = GUI_ROOT.resolve("FinanceiroPanel.java");
        Path receiptDialogPath = GUI_ROOT.resolve("TreasuryReceiptDialog.java");
        Path coordinatorPath = GUI_ROOT.resolve("TreasuryOperationsCoordinator.java");

        List<String> panelLines = Files.readAllLines(panelPath);
        List<String> dialogLines = Files.readAllLines(receiptDialogPath);
        List<String> coordLines = Files.readAllLines(coordinatorPath);

        assertThat(panelLines.size())
                .as("FinanceiroPanel.java deve possuir <= 230 linhas após decomposição")
                .isLessThanOrEqualTo(230);

        assertThat(dialogLines.size())
                .as("TreasuryReceiptDialog.java deve possuir <= 85 linhas")
                .isLessThanOrEqualTo(85);

        assertThat(coordLines.size())
                .as("TreasuryOperationsCoordinator.java deve possuir <= 100 linhas")
                .isLessThanOrEqualTo(100);
    }

    @Test
    @DisplayName("FIN-02: Preservação de contratos de filtro temporal e cards canónicos")
    void fin02_canonicalLayoutAndFiltersPreserved() throws Exception {
        String source = Files.readString(GUI_ROOT.resolve("FinanceiroPanel.java"));

        assertThat(source).contains("PeriodFilter");
        assertThat(source).contains("periodCombo");
        assertThat(source).contains("UIHelper.tableCardTop(\"Contas de Tesouraria & Saldos\"");
        assertThat(source).contains("UIHelper.tableCardTop(\"Histórico de Fluxo de Caixa\"");
        assertThat(source).contains("UIHelper.ensureHeadersFit(accountsTable)");
        assertThat(source).contains("UIHelper.ensureHeadersFit(movementsTable)");
        assertThat(source).contains("TableQuickPeekController.install(");
        assertThat(source).doesNotContain("JOptionPane.");
    }

    @Test
    @DisplayName("FIN-03: Preservação de construtores e métodos de navegação pública")
    void fin03_navigationAndConstructorsPreserved() throws Exception {
        String source = Files.readString(GUI_ROOT.resolve("FinanceiroPanel.java"));

        assertThat(source).contains("public void showReconciliationTab()");
        assertThat(source).contains("public void showForecastTab()");
        assertThat(source).contains("public CashFlowForecastPanel getForecastPanel()");
        assertThat(source).contains("public void onPanelSelected()");
        assertThat(source).contains("public void refreshData()");
        assertThat(source).contains("CashFlowForecastApiClient forecastApiClient");
        assertThat(source).contains("BankReconciliationApiClient bankReconciliationApiClient");
    }
}
