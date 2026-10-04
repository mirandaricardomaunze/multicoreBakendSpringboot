package mz.multicore.erp.gui.inventory;

import mz.multicore.erp.desktop.client.InventoryPhysicalCountingApiClient;
import mz.multicore.erp.modules.inventory.dto.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.swing.*;
import java.awt.BorderLayout;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class PhysicalInventoryPanelHarnessTest {

    private PhysicalInventoryPanel panel;
    private MockApiClient mockApiClient;

    @BeforeEach
    void setUp() {
        System.setProperty("java.awt.headless", "true");
        mockApiClient = new MockApiClient();
        panel = new PhysicalInventoryPanel(mockApiClient);
    }

    @Test
    void testPanelInitializationAndLayout() {
        assertNotNull(panel);
        assertTrue(panel.getComponentCount() > 0);
        assertNotNull(panel.getItemsTable());
        assertNotNull(panel.getItemsTable().getRowSorter(), "TableFilter should install a RowSorter on itemsTable");
    }

    @Test
    void testTableIsEnclosedInModernPanelCard() {
        JTable table = panel.getItemsTable();
        assertNotNull(table);
        boolean foundModernPanelAncestor = false;
        java.awt.Component parent = table.getParent();
        while (parent != null) {
            if (parent instanceof mz.multicore.erp.gui.components.ModernPanel) {
                foundModernPanelAncestor = true;
                break;
            }
            parent = parent.getParent();
        }
        assertTrue(foundModernPanelAncestor, "itemsTable must be enclosed within a ModernPanel card");
    }

    @Test
    void testKpiCardsArePlacedAtTop() {
        BorderLayout mainLayout = (BorderLayout) panel.getLayout();
        JPanel centerPanel = (JPanel) mainLayout.getLayoutComponent(BorderLayout.CENTER);
        assertNotNull(centerPanel, "Center panel should exist");

        BorderLayout centerLayout = (BorderLayout) centerPanel.getLayout();
        java.awt.Component northComp = centerLayout.getLayoutComponent(BorderLayout.NORTH);
        java.awt.Component southComp = centerLayout.getLayoutComponent(BorderLayout.SOUTH);

        assertNotNull(northComp, "KPI cards grid must be placed at BorderLayout.NORTH (encima)");
        assertNull(southComp, "BorderLayout.SOUTH should be empty");
    }

    @Test
    void testTopBarActionCountAndMenuContainment() {
        java.util.List<javax.swing.AbstractButton> buttons = new java.util.ArrayList<>();
        collectButtons(panel, buttons);
        boolean hasActionMenu = buttons.stream()
                .anyMatch(mz.multicore.erp.gui.components.ActionMenuButton.class::isInstance);
        long buttonCount = buttons.stream().filter(button -> button.isShowing() || button.isVisible()).count();

        assertTrue(buttonCount <= 3,
                "A barra de ações do cabeçalho deve ter no máximo 3 botões visíveis, mas tem: " + buttonCount);
        assertTrue(hasActionMenu, "As ações do ciclo da sessão devem estar agrupadas em ActionMenuButton");
    }

    private static void collectButtons(java.awt.Container root, java.util.List<javax.swing.AbstractButton> buttons) {
        for (java.awt.Component component : root.getComponents()) {
            if (component instanceof javax.swing.AbstractButton button) buttons.add(button);
            if (component instanceof java.awt.Container child) collectButtons(child, buttons);
        }
    }

    private static class MockApiClient extends InventoryPhysicalCountingApiClient {
        public MockApiClient() {
            super(null);
        }

        @Override
        public List<InventorySessionDTO> listSessions() {
            InventoryItemDTO item = new InventoryItemDTO(
                    1L, 10L, "ARR-001", "Arroz 5kg", "560123456789",
                    BigDecimal.TEN, BigDecimal.valueOf(12), BigDecimal.valueOf(2),
                    BigDecimal.valueOf(300), BigDecimal.valueOf(600), "Sobra"
            );
            InventorySessionDTO session = new InventorySessionDTO(
                    100L, "INV-2026/001", "Inventário Teste", InventoryStatus.IN_PROGRESS,
                    false, LocalDateTime.now(), null, 1L, 1, 1,
                    BigDecimal.valueOf(600), BigDecimal.ZERO, BigDecimal.valueOf(600),
                    List.of(item), null
            );
            return List.of(session);
        }

        @Override
        public InventorySessionDTO getSessionById(Long id) {
            return listSessions().get(0);
        }
    }
}
