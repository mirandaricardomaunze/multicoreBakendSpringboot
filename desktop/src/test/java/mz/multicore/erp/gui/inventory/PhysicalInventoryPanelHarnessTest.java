package mz.multicore.erp.gui.inventory;

import mz.multicore.erp.desktop.client.InventoryPhysicalCountingApiClient;
import mz.multicore.erp.modules.inventory.dto.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.swing.*;
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
