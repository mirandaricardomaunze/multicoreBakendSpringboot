package mz.multicore.erp.gui;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class StockCommercialUiHarnessTest {

    private static String source(String relative) throws IOException {
        return Files.readString(Path.of("src", "main", "java", "mz", "multicore", "erp", "gui").resolve(relative));
    }

    @Test
    void stockFiltersUseCanonicalHeight() throws IOException {
        String stock = source("StockPanel.java");
        String batches = source("StockBatchesPanel.java");
        assertThat(stock).contains("UIHelper.FORM_CONTROL_HEIGHT").doesNotContain("Dimension(240, 35)", "Dimension(200, 35)");
        assertThat(batches).contains("UIHelper.FORM_CONTROL_HEIGHT").doesNotContain("Dimension(220, 35)", "Dimension(200, 35)");
    }

    @Test
    void commercialCriticalActionsRemainExplicit() throws IOException {
        String invoices = source("CommercialInvoicesView.java");
        String orders = source("CommercialOrdersView.java");
        assertThat(invoices).contains("ActionMenuButton", "Anular Fatura", "Liquidar (RC)");
        assertThat(orders).contains("ActionMenuButton", "Faturar Encomenda", "Converter em Guia", "Cancelar Encomenda");
    }

    @Test
    void stockAndApprovalCriticalActionsRemainExplicit() throws IOException {
        String stock = source("StockPanel.java");
        String notes = source(Path.of("commercial", "CommercialNotesPanel.java").toString());
        String guides = source(Path.of("commercial", "DeliveryGuidesPanel.java").toString());
        assertThat(stock).contains("ActionMenuButton", "Registar Produto", "Inventário Físico", "Trancar Stock");
        assertThat(notes).contains("ActionMenuButton", "Aprovar", "Rejeitar");
        assertThat(guides).contains("ActionMenuButton", "Aprovar", "Rejeitar", "Cancelar");
    }

    @Test
    void modalDeclaresCanonicalKeyboardBindings() throws IOException {
        String dialog = source(Path.of("components", "ModernFormDialog.java").toString());
        assertThat(dialog).contains("KeyEvent.VK_S", "CTRL_DOWN_MASK", "KeyEvent.VK_ESCAPE");
    }
}
