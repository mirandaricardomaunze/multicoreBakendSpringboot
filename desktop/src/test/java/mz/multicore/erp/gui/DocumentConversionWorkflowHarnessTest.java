package mz.multicore.erp.gui;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Harness de validação estrutural do fluxo de conversão entre documentos:
 * Cotação -> Encomenda -> Guia de Remessa / Fatura.
 */
class DocumentConversionWorkflowHarnessTest {

    private static Path resolveSourcePath(String relativePath) {
        Path p = Path.of(relativePath);
        if (Files.exists(p)) return p;
        p = Path.of("desktop", relativePath);
        if (Files.exists(p)) return p;
        return Path.of("..", "desktop", relativePath);
    }

    @Test
    @DisplayName("DCW-01: QuotationsPanel possui conversão para Factura e para Encomenda sem emojis")
    void testQuotationsConversionActions() throws IOException {
        Path path = resolveSourcePath("src/main/java/mz/multicore/erp/gui/commercial/QuotationsPanel.java");
        assertTrue(Files.exists(path), "QuotationsPanel.java deve existir");
        String content = Files.readString(path);

        assertTrue(content.contains("Converter em Factura"), "Deve conter opção de conversão em fatura");
        assertTrue(content.contains("convertToInvoice"), "Deve conter método convertToInvoice");
        assertFalse(content.contains("⭐") || content.contains("✅") || content.contains("❌"), "Sem emojis");
    }

    @Test
    @DisplayName("DCW-02: CommercialOrdersView possui menu de processo com Guia, Transferência e Facturação")
    void testCommercialOrdersConversionActions() throws IOException {
        Path path = resolveSourcePath("src/main/java/mz/multicore/erp/gui/CommercialOrdersView.java");
        assertTrue(Files.exists(path), "CommercialOrdersView.java deve existir");
        String content = Files.readString(path);

        assertTrue(content.contains("Converter em Guia"), "Deve conter conversão em Guia");
        assertTrue(content.contains("Converter em Transferência"), "Deve conter conversão em Transferência");
        assertTrue(content.contains("Faturar Encomenda"), "Deve conter faturamento de encomenda");
        assertTrue(content.contains("fas-truck"), "Deve usar ícone vetorial de transporte");
        assertTrue(content.contains("fas-file-invoice-dollar"), "Deve usar ícone de faturação");
    }

    @Test
    @DisplayName("DCW-03: ComercialPanel implementa métodos de conversão de encomenda")
    void testComercialPanelConversionHandlers() throws IOException {
        Path path = resolveSourcePath("src/main/java/mz/multicore/erp/gui/ComercialPanel.java");
        assertTrue(Files.exists(path), "ComercialPanel.java deve existir");
        String content = Files.readString(path);

        assertTrue(content.contains("convertSelectedOrderToGuide"), "Deve implementar convertSelectedOrderToGuide");
        assertTrue(content.contains("convertSelectedOrderToTransfer"), "Deve implementar convertSelectedOrderToTransfer");
        assertTrue(content.contains("billSelectedOrder"), "Deve implementar billSelectedOrder");
    }
}
