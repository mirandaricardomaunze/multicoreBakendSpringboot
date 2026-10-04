package mz.multicore.erp.gui.components;

import mz.multicore.erp.gui.commercial.CustomerDetailDialog;
import mz.multicore.erp.gui.commercial.OrderDetailsDialog;
import mz.multicore.erp.gui.commercial.SupplierDetailDialog;
import mz.multicore.erp.modules.comercial.dto.ClientDTO;
import mz.multicore.erp.modules.comercial.dto.OrderDTO;
import mz.multicore.erp.modules.comercial.dto.OrderLineDTO;
import mz.multicore.erp.modules.purchases.dto.SupplierDTO;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.swing.*;
import java.awt.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Harness: ExecutiveDetailDialog e Modais Canónicos Reutilizáveis")
class ExecutiveDetailDialogHarnessTest {

    @BeforeAll
    static void setupHeadless() {
        System.setProperty("java.awt.headless", "true");
        System.setProperty("multicore.test.headless", "true");
    }

    @Test
    @DisplayName("D-01: ExecutiveDetailDialog instancia componentes sem erros em headless")
    void testDialogBasicStructure() {
        ExecutiveDetailDialog dialog = new ExecutiveDetailDialog(
                null,
                "Ficha de Teste"
        );

        dialog.setTitle("Entidade Teste Lda")
              .setSubtitle("Código #999 • Registo Geral")
              .setHeaderAvatar("TS", UIHelper.ACCENT)
              .setStatusBadge("ACTIVO", ExecutiveDetailDialog.StatusSeverity.SUCCESS)
              .addKpi("Total", "1.500,00 MT", "Valor global", UIHelper.APPROVED_GREEN, "fas-receipt")
              .addKpi("Pendente", "0,00 MT", "Liquidado", UIHelper.TEXT_MUTED, "fas-check-circle");

        JPanel p1 = new JPanel();
        p1.add(new JLabel("Aba 1"));
        dialog.addTab("Dados Gerais", "fas-info-circle", UIHelper.ACCENT, p1);

        JPanel p2 = new JPanel();
        p2.add(new JLabel("Aba 2"));
        dialog.addTab("Histórico", "fas-history", UIHelper.ACCENT_CYAN, p2);

        dialog.addRightAction(new ModernButton("Ação Teste", UIHelper.ACCENT_BLUE, UIHelper.ACCENT_BLUE.darker()));

        assertNotNull(dialog);
        if (dialog.getDialog() != null) {
            assertEquals("Entidade Teste Lda", dialog.getDialog().getTitle());
            assertTrue(dialog.getDialog().getWidth() >= 800);
        }
    }

    @Test
    @DisplayName("D-02: Todas as severidades de Badge funcionam corretamente")
    void testBadgeSeverities() {
        ExecutiveDetailDialog dialog = new ExecutiveDetailDialog(null, "Badge Test");

        for (ExecutiveDetailDialog.StatusSeverity severity : ExecutiveDetailDialog.StatusSeverity.values()) {
            assertDoesNotThrow(() -> dialog.setStatusBadge("STATUS_" + severity.name(), severity));
        }
    }

    @Test
    @DisplayName("D-03: Header com ícone semântico funciona perfeitamente")
    void testHeaderIcon() {
        ExecutiveDetailDialog dialog = new ExecutiveDetailDialog(null, "Icon Header Test");

        assertDoesNotThrow(() -> {
            dialog.setHeaderIcon("fas-cube", 28, UIHelper.ACCENT_ORANGE);
            dialog.setTitle("Artigo Especial");
            dialog.setSubtitle("REF-12345");
        });
    }

    @Test
    @DisplayName("D-04: OrderDetailsDialog monta ficha executiva de Encomenda")
    void testOrderDetailsDialogConstruction() {
        OrderLineDTO line1 = new OrderLineDTO(
                1L, 10L, "Portátil HP G8", "PROD-01",
                new BigDecimal("2.00"), new BigDecimal("45000.00"), new BigDecimal("16.00"),
                new BigDecimal("104400.00"), BigDecimal.ZERO, "LOTE-01", "SN-9988",
                1, new BigDecimal("2.10"), new BigDecimal("2.50"), new BigDecimal("5.00"),
                BigDecimal.ZERO, BigDecimal.ZERO
        );

        OrderDTO order = new OrderDTO(
                100L,
                "ENC-2026/001",
                1L,
                "CLIENTE-ALPHA",
                "400123456",
                null,
                new BigDecimal("90000.00"),
                new BigDecimal("14400.00"),
                new BigDecimal("104400.00"),
                "PENDENTE",
                null,
                List.of(line1),
                LocalDateTime.now(),
                null,
                0,
                null
        );

        JPanel dummyOwner = new JPanel();
        assertDoesNotThrow(() -> {
            OrderDetailsDialog dlg = new OrderDetailsDialog(dummyOwner, null, null);
            assertNotNull(dlg);
            // Testa renderização sem lançar exceções gráficas
            dlg.show(order);
        });
    }

    @Test
    @DisplayName("D-05: CustomerDetailDialog monta ficha executiva de Cliente")
    void testCustomerDetailDialogConstruction() {
        ClientDTO client = new ClientDTO(
                55L,
                "Comercial Distribuidora Moçambique SA",
                "400123456",
                "comercial@cdm.co.mz",
                "Av. 24 de Julho 1000",
                30,
                new BigDecimal("500000.00")
        );

        JPanel dummyOwner = new JPanel();
        assertDoesNotThrow(() -> {
            CustomerDetailDialog dlg = new CustomerDetailDialog(dummyOwner, client, () -> {}, () -> {});
            assertNotNull(dlg);
            dlg.show();
        });
    }

    @Test
    @DisplayName("D-06: SupplierDetailDialog monta ficha executiva de Fornecedor")
    void testSupplierDetailDialogConstruction() {
        SupplierDTO supplier = new SupplierDTO(
                12L,
                "Indústrias Matola SARL",
                "400987654",
                "contacto@indmatola.co.mz",
                "Zona Industrial, Matola",
                "+258 82 999 8888",
                "Carlos Silva (Gestor de Contas)",
                true,
                1L
        );

        JPanel dummyOwner = new JPanel();
        assertDoesNotThrow(() -> {
            SupplierDetailDialog dlg = new SupplierDetailDialog(dummyOwner, supplier, () -> {}, () -> {});
            assertNotNull(dlg);
            dlg.show();
        });
    }

    @Test
    @DisplayName("D-07: RecordDetailsDialog abre ExecutiveDetailDialog para qualquer JTable genérica")
    void testRecordDetailsDialogFallback() {
        String[] columns = new String[]{"Código", "Designação", "Preço Total", "Estado"};
        Object[][] data = new Object[][]{
                {"PROD-99", "Cimento Campeão 50kg", "450,00 MT", "ACTIVO"}
        };
        JTable table = new JTable(data, columns);
        table.setRowSelectionInterval(0, 0);

        assertDoesNotThrow(() -> RecordDetailsDialog.show(table));
    }
}
