package mz.multicore.erp.gui;

import mz.multicore.erp.gui.components.KpiCard;
import mz.multicore.erp.gui.components.ModernPanel;
import mz.multicore.erp.gui.components.UIHelper;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import javax.swing.JLabel;

import static org.junit.jupiter.api.Assertions.*;

class KpiCardStandardizationTest {

    @BeforeAll
    static void initHeadless() {
        System.setProperty("java.awt.headless", "true");
    }

    @Test
    void testKpiCardMetricCardCreationWithLabel() {
        // KPI-01: Verifies createMetricCard works with dynamic JLabel
        JLabel val = new JLabel("1.250,00 MT");
        ModernPanel card = KpiCard.createMetricCard("TOTAL FACTURADO", val, "Vendas no mês", "fas-receipt", UIHelper.ACCENT_BLUE);
        assertNotNull(card);
        assertEquals(2, card.getComponentCount());
    }

    @Test
    void testKpiCardMetricCardCreationWithString() {
        // KPI-01: Verifies createMetricCard works with String value
        ModernPanel card = KpiCard.createMetricCard("ALERTAS", "3 Activos", "Subtítulo de teste", "fas-bell", UIHelper.PENDING_YELLOW);
        assertNotNull(card);
        assertEquals(2, card.getComponentCount());
    }

    @Test
    void testCreditRiskPanelInstantiationWithStandardCards() {
        // KPI-02: CreditRiskPanel instantiates cleanly with standardized KPI cards
        CreditRiskPanel panel = new CreditRiskPanel(null);
        assertNotNull(panel);
    }

    @Test
    void testStockWastePanelInstantiationWithStandardCards() {
        // KPI-03: StockWastePanel instantiates cleanly with standardized KPI cards
        StockWastePanel panel = new StockWastePanel(null, null, null);
        assertNotNull(panel);
    }

    @Test
    void testCustomerStatementPanelInstantiationWithStandardCards() {
        // KPI-04: CustomerStatementPanel instantiates cleanly with standardized KPI cards
        CustomerStatementPanel panel = new CustomerStatementPanel(null, null);
        assertNotNull(panel);
    }

    @Test
    void testSupplierStatementPanelInstantiationWithStandardCards() {
        // KPI-05: SupplierStatementPanel instantiates cleanly with standardized KPI cards
        SupplierStatementPanel panel = new SupplierStatementPanel(null, null);
        assertNotNull(panel);
    }
}
