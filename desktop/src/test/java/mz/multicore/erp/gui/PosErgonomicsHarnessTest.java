package mz.multicore.erp.gui;

import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.desktop.client.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import javax.swing.*;
import java.awt.event.KeyEvent;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Harness automatizado para validação ergonómica e visual do POS (POS-01 a POS-04).
 */
public class PosErgonomicsHarnessTest {

    @Test
    @DisplayName("POS-01: A aba de venda ativa do POS não possui cartões KPI volumosos no topo")
    void testSalesTabHeaderIsCompactWithoutKpis() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            CurrentUserContext.setCurrentUser("ana", "ADMIN");
            CurrentUserContext.setCurrentCompanyId(1L);
            try {
                POSPanel pos = createTestPosPanel();
                assertNotNull(pos);

                JPanel salesTab = (JPanel) pos.viewCards.getComponent(0);
                assertNotNull(salesTab);
                assertEquals(2, salesTab.getComponentCount(), "salesTab deve conter apenas o topSelectsBar e o workspace");
                assertSame(pos.topSelectsBar, salesTab.getComponent(0), "O topo deve ser o topSelectsBar compacto");
            } finally {
                CurrentUserContext.clear();
            }
        });
    }

    @Test
    @DisplayName("POS-02: Os cartões de produto possuem dimensões compactas padronizadas para alta densidade")
    void testProductCardDimensions() {
        assertTrue(PosCatalogController.CARD_IMAGE_WIDTH <= 85, "CARD_IMAGE_WIDTH deve ser <= 85px");
        assertTrue(PosCatalogController.CARD_IMAGE_HEIGHT <= 45, "CARD_IMAGE_HEIGHT deve ser <= 45px");
        assertTrue(PosCatalogController.CARD_PADDING <= 6, "CARD_PADDING deve ser <= 6px");
        assertTrue(PosCatalogController.CARD_CONTENT_GAP <= 3, "CARD_CONTENT_GAP deve ser <= 3px");
    }

    @Test
    @DisplayName("POS-03: Atalhos de teclado operacionais (F2, F3, F4, F6, F9) registados no InputMap")
    void testOperationalKeyboardShortcuts() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            CurrentUserContext.setCurrentUser("ana", "ADMIN");
            CurrentUserContext.setCurrentCompanyId(1L);
            try {
                POSPanel pos = createTestPosPanel();
                InputMap inputMap = pos.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
                ActionMap actionMap = pos.getActionMap();

                assertNotNull(inputMap.get(KeyStroke.getKeyStroke(KeyEvent.VK_F2, 0)), "F2 deve estar registado");
                assertNotNull(actionMap.get("posProductSearch"), "Ação posProductSearch deve existir");

                assertNotNull(inputMap.get(KeyStroke.getKeyStroke(KeyEvent.VK_F3, 0)), "F3 deve estar registado");
                assertNotNull(actionMap.get("posBarcodeSearch"), "Ação posBarcodeSearch deve existir");

                assertNotNull(inputMap.get(KeyStroke.getKeyStroke(KeyEvent.VK_F4, 0)), "F4 deve estar registado");
                assertNotNull(actionMap.get("posClientSearch"), "Ação posClientSearch deve existir");

                assertNotNull(inputMap.get(KeyStroke.getKeyStroke(KeyEvent.VK_F6, 0)), "F6 deve estar registado");
                assertNotNull(actionMap.get("posEditQuantity"), "Ação posEditQuantity deve existir");

                assertNotNull(inputMap.get(KeyStroke.getKeyStroke(KeyEvent.VK_F9, 0)), "F9 deve estar registado");
                assertNotNull(actionMap.get("posCheckout"), "Ação posCheckout deve existir");
            } finally {
                CurrentUserContext.clear();
            }
        });
    }

    @Test
    @DisplayName("POS-04: Estrutura do bloco de totais e tabela do carrinho")
    void testCartTotalHierarchy() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            CurrentUserContext.setCurrentUser("ana", "ADMIN");
            CurrentUserContext.setCurrentCompanyId(1L);
            try {
                POSPanel pos = createTestPosPanel();
                assertNotNull(pos.totalLabel, "Etiqueta de total deve existir");
                assertNotNull(pos.subtotalValueLabel, "Etiqueta de subtotal deve existir");
                assertNotNull(pos.ivaValueLabel, "Etiqueta de IVA deve existir");
                assertEquals("0,00 MT", pos.totalLabel.getText());
            } finally {
                CurrentUserContext.clear();
            }
        });
    }

    private POSPanel createTestPosPanel() {
        POSApiClient posApi = Mockito.mock(POSApiClient.class);
        ComercialApiClient comercialApi = Mockito.mock(ComercialApiClient.class);
        InventoryApiClient invApi = Mockito.mock(InventoryApiClient.class);
        FinanceApiClient finApi = Mockito.mock(FinanceApiClient.class);
        PromotionApiClient promoApi = Mockito.mock(PromotionApiClient.class);
        mz.multicore.erp.modules.pos.scale.ScaleBarcodeParser scale = Mockito.mock(mz.multicore.erp.modules.pos.scale.ScaleBarcodeParser.class);

        return new POSPanel(posApi, comercialApi, invApi, finApi, promoApi, scale);
    }
}
