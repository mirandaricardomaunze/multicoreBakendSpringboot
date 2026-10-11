package mz.multicore.erp.gui;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Harness COMM-OP-01..03:
 * Valida a conformidade canónica dos Documentos Comerciais Operacionais:
 * - Cotações, Recibos e Guias usam TableFilter.bar, ensureHeadersFit e TableQuickPeekController.
 * - Faturas e Pedidos contêm calibração ensureHeadersFit.
 */
class CommercialOperacionalUnifiedHarnessTest {

    private static String source(String filename) throws Exception {
        return Files.readString(Path.of("src/main/java/mz/multicore/erp/gui/" + filename));
    }

    @Test
    @DisplayName("COMM-OP-01: QuotationsPanel segue padrão canónico com TableFilter.bar, ensureHeadersFit e Quick Peek")
    void commOp01_quotationsCanonical() throws Exception {
        String s = source("commercial/QuotationsPanel.java");
        assertThat(s).contains("TableFilter.bar(");
        assertThat(s).contains("UIHelper.ensureHeadersFit(table)");
        assertThat(s).contains("TableQuickPeekController.install(table");
    }

    @Test
    @DisplayName("COMM-OP-02: ReceiptsPanel e DeliveryGuidesPanel seguem padrão canónico com ensureHeadersFit e Quick Peek")
    void commOp02_receiptsAndGuidesCanonical() throws Exception {
        String receipts = source("commercial/ReceiptsPanel.java");
        assertThat(receipts).contains("TableFilter.bar(");
        assertThat(receipts).contains("UIHelper.ensureHeadersFit(table)");
        assertThat(receipts).contains("TableQuickPeekController.install(table");

        String guides = source("commercial/DeliveryGuidesPanel.java");
        assertThat(guides).contains("TableFilter.bar(");
        assertThat(guides).contains("UIHelper.ensureHeadersFit(table)");
        assertThat(guides).contains("TableQuickPeekController.install(table");
    }

    @Test
    @DisplayName("COMM-OP-03: CommercialInvoicesView e CommercialOrdersView contêm calibração ensureHeadersFit")
    void commOp03_invoicesAndOrdersCalibrated() throws Exception {
        String invoices = source("CommercialInvoicesView.java");
        assertThat(invoices).contains("UIHelper.ensureHeadersFit(owner.invoicesTable)");

        String orders = source("CommercialOrdersView.java");
        assertThat(orders).contains("UIHelper.ensureHeadersFit(owner.ordersTable)");
    }
}
