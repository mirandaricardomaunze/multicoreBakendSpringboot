package mz.multicore.erp.gui;

import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.desktop.client.*;
import mz.multicore.erp.gui.components.ActionMenuButton;
import mz.multicore.erp.gui.components.ModernButton;
import mz.multicore.erp.gui.components.UIHelper;
import mz.multicore.erp.modules.pos.dto.TillSessionDTO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Harness automatizado para verificação de layout anti-sobreposição no cabeçalho do POS:
 * 1. O container superior (topBar) utiliza GridBagLayout para impedir intersecção de bounds.
 * 2. As ações secundárias de Cotação, Fidelidade e Fechos Z estão agregadas em ActionMenuButton (Operações).
 * 3. Quando a sessão de caixa é aberta, os botões não colidem em resoluções padrão (>= 1024px).
 * 4. Ausência de emojis Unicode em qualquer botão ou texto do cabeçalho.
 * 5. Decomposição estrita de POSPanel.java <= 1000 linhas.
 */
public class PosHeaderLayoutHarnessTest {

    @Test
    @DisplayName("OVERLAP-01: topBar usa GridBagLayout anti-colisão em vez de BorderLayout")
    void testTopBarUsesGridBagLayout() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            CurrentUserContext.setCurrentUser("operador.pos", "CAIXA");
            CurrentUserContext.setCurrentCompanyId(1L);
            try {
                POSPanel pos = createTestPosPanel();
                assertNotNull(pos.sessionBanner);
                Container sessionBar = pos.sessionBanner.getParent();
                assertNotNull(sessionBar, "sessionBar deve ser o container pai de sessionBanner");

                Component topBarComponent = sessionBar.getComponent(0);
                assertTrue(topBarComponent instanceof JPanel, "topBar deve ser um JPanel");
                JPanel topBar = (JPanel) topBarComponent;

                assertTrue(topBar.getLayout() instanceof GridBagLayout,
                        "topBar deve usar GridBagLayout para garantir separação física de colunas sem sobreposição");
            } finally {
                CurrentUserContext.clear();
            }
        });
    }

    @Test
    @DisplayName("OVERLAP-02: ActionMenuButton agrega Cotação, Fidelidade e Fechos Z com <= 5 ações")
    void testOperationsMenuGroupsSecondaryActions() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            CurrentUserContext.setCurrentUser("operador.pos", "CAIXA");
            CurrentUserContext.setCurrentCompanyId(1L);
            try {
                POSPanel pos = createTestPosPanel();
                assertNotNull(pos.operationsMenu, "operationsMenu deve estar instanciado");
                assertEquals(3, pos.operationsMenu.actionCount(), "Deve possuir exactamente 3 acções agrupadas");
                assertTrue(pos.operationsMenu.actionCount() <= 5, "Não pode exceder o limite estrito de 5 acções");

                assertEquals("Importar Cotação (F7)", pos.operationsMenu.actionAt(0).getText());
                assertEquals("Programa de Fidelidade", pos.operationsMenu.actionAt(1).getText());
                assertEquals("Histórico de Fechos (Z)", pos.operationsMenu.actionAt(2).getText());
            } finally {
                CurrentUserContext.clear();
            }
        });
    }

    @Test
    @DisplayName("OVERLAP-03: Botões do cabeçalho não se sobrepõem quando a caixa está aberta")
    void testNoOverlapWhenSessionIsOpen() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            CurrentUserContext.setCurrentUser("operador.pos", "CAIXA");
            CurrentUserContext.setCurrentCompanyId(1L);
            try {
                POSPanel pos = createTestPosPanel();

                // Simula abertura de sessão com fundo de caixa activo
                TillSessionDTO session = new TillSessionDTO(
                        10L, "operador.pos", 1L, BigDecimal.valueOf(500),
                        null, null, null, LocalDateTime.now(), null, "OPEN");
                pos.applySessionState(Optional.of(session));

                // Configura tamanho operacional padrão de desktop (1024px de largura útil)
                pos.setSize(1024, 768);
                pos.doLayout();

                Container sessionBar = pos.sessionBanner.getParent();
                JPanel topBar = (JPanel) sessionBar.getComponent(0);
                topBar.setSize(1024, 40);
                topBar.doLayout();

                Component segmented = topBar.getComponent(0);
                Component sessionActions = topBar.getComponent(2); // col 0 = segmented, col 1 = glue, col 2 = sessionActions

                segmented.doLayout();
                sessionActions.doLayout();

                int segmentedMaxX = segmented.getX() + segmented.getWidth();
                int sessionActionsMinX = sessionActions.getX();

                assertTrue(segmentedMaxX <= sessionActionsMinX,
                        String.format("segmented (maxX=%d) colide com sessionActions (minX=%d)!", segmentedMaxX, sessionActionsMinX));
            } finally {
                CurrentUserContext.clear();
            }
        });
    }

    @Test
    @DisplayName("OVERLAP-04: Ausência de caracteres e emojis Unicode proibidos nos botões")
    void testNoUnicodeEmojisInPosHeader() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            CurrentUserContext.setCurrentUser("operador.pos", "CAIXA");
            CurrentUserContext.setCurrentCompanyId(1L);
            try {
                POSPanel pos = createTestPosPanel();
                Container sessionBar = pos.sessionBanner.getParent();
                JPanel topBar = (JPanel) sessionBar.getComponent(0);

                assertNoForbiddenUnicode(topBar);
            } finally {
                CurrentUserContext.clear();
            }
        });
    }

    @Test
    @DisplayName("OVERLAP-05: POSPanel.java permanece rigorosamente <= 1000 linhas")
    void testPosPanelLineCountStrictlyUnder1000() throws IOException {
        Path path = Path.of("src", "main", "java", "mz", "multicore", "erp", "gui", "POSPanel.java");
        if (!Files.exists(path)) {
            path = Path.of("desktop", "src", "main", "java", "mz", "multicore", "erp", "gui", "POSPanel.java");
        }
        assertTrue(Files.exists(path), "Arquivo POSPanel.java deve existir");
        long lines = Files.lines(path).count();
        assertTrue(lines <= 1000,
                "POSPanel.java tem " + lines + " linhas; o limite estrito arquitetural é <= 1000 linhas");
    }

    private void assertNoForbiddenUnicode(Container container) {
        for (Component c : container.getComponents()) {
            if (c instanceof AbstractButton btn) {
                String text = btn.getText();
                if (text != null) {
                    assertFalse(text.contains("⭐") || text.contains("★") || text.contains("✅") || text.contains("❌"),
                            "Botão não pode usar emojis/caracteres especiais Unicode: " + text);
                }
            }
            if (c instanceof Container child) {
                assertNoForbiddenUnicode(child);
            }
        }
    }

    private POSPanel createTestPosPanel() {
        POSApiClient posApi = Mockito.mock(POSApiClient.class);
        ComercialApiClient comercialApi = Mockito.mock(ComercialApiClient.class);
        InventoryApiClient invApi = Mockito.mock(InventoryApiClient.class);
        FinanceApiClient finApi = Mockito.mock(FinanceApiClient.class);
        PromotionApiClient promoApi = Mockito.mock(PromotionApiClient.class);
        mz.multicore.erp.modules.pos.scale.ScaleBarcodeParser scale =
                Mockito.mock(mz.multicore.erp.modules.pos.scale.ScaleBarcodeParser.class);

        return new POSPanel(posApi, comercialApi, invApi, finApi, promoApi, scale);
    }
}
