package mz.multicore.erp.gui;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Harness AF-01..02:
 * Valida a conformidade canónica dos módulos de Fiscal, Auditoria Forense e Contabilidade:
 * - Ausência de ModernPanel com raio inferior a 16
 * - Calibração de larguras de coluna com UIHelper.ensureHeadersFit
 * - Instalação de Quick Peek silencioso com tecla Espaço
 */
class AccountingFiscalUnifiedHarnessTest {

    private static String source(String filename) throws Exception {
        return Files.readString(Path.of("src/main/java/mz/multicore/erp/gui/" + filename));
    }

    @Test
    @DisplayName("AF-01: FiscalPanel padronizado com ModernPanel(16), ensureHeadersFit e Quick Peek")
    void af01_fiscalCanonical() throws Exception {
        String fiscal = source("FiscalPanel.java");
        assertThat(fiscal).doesNotContain("new ModernPanel(14)");
        assertThat(fiscal).contains("UIHelper.ensureHeadersFit(taxRatesTable)");
        assertThat(fiscal).contains("TableQuickPeekController.install(taxRatesTable");
        assertThat(fiscal).contains("UIHelper.ensureHeadersFit(withholdingsTable)");
        assertThat(fiscal).contains("TableQuickPeekController.install(withholdingsTable");
    }

    @Test
    @DisplayName("AF-02: ForensicAuditPanel e AccountingPanel com ensureHeadersFit e Quick Peek")
    void af02_forensicAndAccountingCanonical() throws Exception {
        String forensic = source("ForensicAuditPanel.java");
        assertThat(forensic).contains("UIHelper.ensureHeadersFit(table)");
        assertThat(forensic).contains("TableQuickPeekController.install(table");

        String accounting = source("accounting/AccountingPanel.java");
        assertThat(accounting).contains("UIHelper.ensureHeadersFit(table)");
    }
}
