package mz.multicore.erp.gui;

import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.desktop.client.*;
import mz.multicore.erp.gui.components.ModernButton;
import mz.multicore.erp.gui.components.UIHelper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import javax.swing.*;
import java.awt.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Testes automatizados para verificação do padrão de ergonomia profissional do POS:
 * 1. Banner de sessão de caixa (sessionBanner) em vez de etiqueta solta.
 * 2. Ícones e cores contextuais por categoria no catálogo.
 * 3. Botões de quantidade no carrinho limpos (sem duplicação de símbolo/ícone).
 * 4. Terminologia institucional para venda a crédito.
 * 5. Tipografia destacada para o TOTAL A PAGAR (24px bold, azul de destaque).
 * 6. Pesos calibrados da barra de cabeçalho.
 */
public class PosProfessionalErgonomicsHarnessTest {

    @Test
    @DisplayName("POS-PROF-01: O estado da sessão é exibido num sessionBanner estilizado")
    void testSessionStateIsRenderedAsBanner() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            CurrentUserContext.setCurrentUser("operador.pos", "CAIXA");
            CurrentUserContext.setCurrentCompanyId(1L);
            try {
                POSPanel pos = createTestPosPanel();
                assertNotNull(pos.sessionBanner, "sessionBanner deve existir");
                assertSame(pos.statusLabel.getParent(), pos.sessionBanner, "statusLabel deve estar dentro do sessionBanner");
                assertEquals(UIHelper.ROW_ALT, pos.sessionBanner.getBackground(), "Caixa fechado deve usar fundo ROW_ALT");
            } finally {
                CurrentUserContext.clear();
            }
        });
    }

    @Test
    @DisplayName("POS-PROF-02: Ícones e cores contextuais por categoria no catálogo de produtos")
    void testCategoryContextualIconsAndColors() {
        assertEquals("fas-glass-martini-alt", PosCatalogController.categoryIcon("Bebidas"));
        assertEquals("fas-glass-martini-alt", PosCatalogController.categoryIcon("Refrigerantes e Sucos"));
        assertEquals("fas-pump-soap", PosCatalogController.categoryIcon("Higiene e Limpeza"));
        assertEquals("fas-bread-slice", PosCatalogController.categoryIcon("Padaria e Pastelaria"));
        assertEquals("fas-shopping-basket", PosCatalogController.categoryIcon("Alimentos e Mercearia"));
        assertEquals("fas-box", PosCatalogController.categoryIcon("Diversos"));
        assertEquals("fas-box", PosCatalogController.categoryIcon(null));

        assertEquals(UIHelper.ACCENT_BLUE, PosCatalogController.categoryColor("Bebidas"));
        assertEquals(UIHelper.PENDING_YELLOW, PosCatalogController.categoryColor("Padaria"));
        assertEquals(UIHelper.APPROVED_GREEN, PosCatalogController.categoryColor("Limpeza"));
        assertEquals(UIHelper.PENDING_YELLOW, PosCatalogController.categoryColor("Alimentos"));
        assertEquals(UIHelper.ACCENT_BLUE, PosCatalogController.categoryColor("Diversos"));
        assertEquals(UIHelper.ACCENT_BLUE, PosCatalogController.categoryColor(null));
    }

    @Test
    @DisplayName("POS-PROF-03: Botões de quantidade no carrinho não têm ícone duplicado com o texto")
    void testQuantityButtonsHaveCleanLabels() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            CurrentUserContext.setCurrentUser("operador.pos", "CAIXA");
            CurrentUserContext.setCurrentCompanyId(1L);
            try {
                POSPanel pos = createTestPosPanel();
                assertNotNull(pos.cartCenter);

                // Localiza os botões de ajuste de quantidade na árvore do carrinho
                Container cartCard = pos.cartCenter.getParent();
                assertNotNull(cartCard);

                boolean foundMinus = false;
                boolean foundPlus = false;
                boolean foundEditQty = false;

                for (Component c : getAllComponents(cartCard)) {
                    if (c instanceof ModernButton btn) {
                        String txt = btn.getText();
                        if ("−".equals(txt)) {
                            foundMinus = true;
                            assertNull(btn.getIcon(), "Botão de decremento '−' não deve ter ícone duplicado");
                        } else if ("+".equals(txt)) {
                            foundPlus = true;
                            assertNull(btn.getIcon(), "Botão de incremento '+' não deve ter ícone duplicado");
                        } else if (txt != null && txt.contains("Quantidade (F6)")) {
                            foundEditQty = true;
                        }
                    }
                }

                assertTrue(foundMinus, "Botão '−' deve estar presente no carrinho");
                assertTrue(foundPlus, "Botão '+' deve estar presente no carrinho");
                assertTrue(foundEditQty, "Botão 'Quantidade (F6)' deve estar presente no carrinho");
            } finally {
                CurrentUserContext.clear();
            }
        });
    }

    @Test
    @DisplayName("POS-PROF-04: Opção de crédito usa terminologia institucional")
    void testCreditOptionUsesProfessionalWording() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            CurrentUserContext.setCurrentUser("operador.pos", "CAIXA");
            CurrentUserContext.setCurrentCompanyId(1L);
            try {
                POSPanel pos = createTestPosPanel();
                assertNotNull(pos.creditCheck);
                assertEquals("Venda a Crédito (Conta Corrente)", pos.creditCheck.getText());
                assertFalse(pos.creditCheck.getText().toLowerCase().contains("fiado"), "Não deve conter o termo informal 'fiado'");
            } finally {
                CurrentUserContext.clear();
            }
        });
    }

    @Test
    @DisplayName("POS-PROF-05: Tipografia destacada do Total a Pagar (>= 22px bold com cor ACCENT_BLUE)")
    void testTotalDisplayTypography() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            CurrentUserContext.setCurrentUser("operador.pos", "CAIXA");
            CurrentUserContext.setCurrentCompanyId(1L);
            try {
                POSPanel pos = createTestPosPanel();
                assertNotNull(pos.totalLabel);
                assertTrue(pos.totalLabel.getFont().getSize() >= 22, "Fonte do total deve ser >= 22px para clareza");
                assertTrue(pos.totalLabel.getFont().isBold(), "Fonte do total deve ser bold");
                assertEquals(UIHelper.ACCENT_BLUE, pos.totalLabel.getForeground(), "Total deve ter cor de destaque ACCENT_BLUE");
            } finally {
                CurrentUserContext.clear();
            }
        });
    }

    @Test
    @DisplayName("POS-PROF-06: Pesos do cabeçalho distribuem 100% da largura em 5 campos")
    void testHeaderFieldWeights() {
        assertEquals(5, PosLayout.HEADER_FIELD_WEIGHTS.length, "Deve haver 5 colunas de cabeçalho");
        double sum = 0.0;
        for (double w : PosLayout.HEADER_FIELD_WEIGHTS) {
            sum += w;
            assertTrue(w > 0.15, "Cada coluna deve ter peso mínimo de 15%");
        }
        assertEquals(1.0, sum, 0.001, "A soma dos pesos do cabeçalho deve ser 1.0 (100%)");
    }

    private java.util.List<Component> getAllComponents(Container container) {
        java.util.List<Component> list = new java.util.ArrayList<>();
        for (Component c : container.getComponents()) {
            list.add(c);
            if (c instanceof Container childContainer) {
                list.addAll(getAllComponents(childContainer));
            }
        }
        return list;
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
