package mz.multicore.erp.gui;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Harness STMT-01..03:
 * Valida a conformidade canónica de Contas Correntes e Cash Flow Previsional:
 * - Card único ModernPanel(16) e ausência de ModernPanel(12) / ModernPanel(10) nos cartões
 * - Integração de seletores e filtros via UIHelper.tableCardTop
 * - Instalação de Quick Peek silencioso com tecla Espaço
 * - Calibração de larguras de coluna com UIHelper.ensureHeadersFit
 */
class StatementsCashFlowUnifiedHarnessTest {

    private static String source(String filename) throws Exception {
        return Files.readString(Path.of("src/main/java/mz/multicore/erp/gui/" + filename));
    }

    @Test
    @DisplayName("STMT-01: CustomerStatementPanel segue padrão ModernPanel(16), tableCardTop, Quick Peek e ensureHeadersFit")
    void stmt01_customerStatementCanonical() throws Exception {
        String s = source("CustomerStatementPanel.java");

        assertThat(s).contains("new ModernPanel(16)");
        assertThat(s).doesNotContain("new ModernPanel(12)");
        assertThat(s).contains("UIHelper.tableCardTop(");
        assertThat(s).contains("TableQuickPeekController.install(table");
        assertThat(s).contains("UIHelper.ensureHeadersFit(table)");
    }

    @Test
    @DisplayName("STMT-02: SupplierStatementPanel segue padrão ModernPanel(16), tableCardTop, Quick Peek e ensureHeadersFit")
    void stmt02_supplierStatementCanonical() throws Exception {
        String s = source("SupplierStatementPanel.java");

        assertThat(s).contains("new ModernPanel(16)");
        assertThat(s).doesNotContain("new ModernPanel(12)");
        assertThat(s).contains("UIHelper.tableCardTop(");
        assertThat(s).contains("TableQuickPeekController.install(table");
        assertThat(s).contains("UIHelper.ensureHeadersFit(table)");
    }

    @Test
    @DisplayName("STMT-03: CashFlowForecastPanel usa ModernPanel(16) e calibra colunas da matriz de liquidez")
    void stmt03_cashFlowCanonical() throws Exception {
        String s = source("CashFlowForecastPanel.java");

        assertThat(s).doesNotContain("new ModernPanel(10)");
        assertThat(s).contains("new ModernPanel(16)");
        assertThat(s).contains("UIHelper.ensureHeadersFit(bucketsTable)");
    }
}
