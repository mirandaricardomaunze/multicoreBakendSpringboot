package mz.multicore.erp.gui;

import mz.multicore.erp.desktop.client.*;
import mz.multicore.erp.gui.components.UIHelper;
import mz.multicore.erp.modules.subscription.dto.MySubscriptionDTO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import javax.swing.*;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Harness: Decomposição de ConfigSubscriptionPanel (Fase 65)")
class ConfigSubscriptionPanelDecompositionHarnessTest {

    private static Path resolveGui(String name) {
        Path direct = Path.of("desktop/src/main/java/mz/multicore/erp/gui", name);
        if (Files.exists(direct)) return direct;
        Path nested = Path.of("src/main/java/mz/multicore/erp/gui", name);
        if (Files.exists(nested)) return nested;
        return direct;
    }

    @Test
    @DisplayName("CSP-01: Limites físicos estritos de linhas de código")
    void csp01_strictLineLimits() throws IOException {
        Path panelPath = resolveGui("ConfigSubscriptionPanel.java");
        Path rendererPath = resolveGui("ConfigSubscriptionCardRenderer.java");

        assertTrue(Files.exists(panelPath), "ConfigSubscriptionPanel.java deve existir");
        assertTrue(Files.exists(rendererPath), "ConfigSubscriptionCardRenderer.java deve existir");

        long panelLines = Files.lines(panelPath).count();
        long rendererLines = Files.lines(rendererPath).count();

        assertTrue(panelLines <= 75, "ConfigSubscriptionPanel.java excedeu 75 linhas (actual: " + panelLines + ")");
        assertTrue(rendererLines <= 95, "ConfigSubscriptionCardRenderer.java excedeu 95 linhas (actual: " + rendererLines + ")");
    }

    @Test
    @DisplayName("CSP-02: Zero diálogos legados JOptionPane")
    void csp02_zeroLegacyDialogs() throws IOException {
        List<String> files = List.of(
                "ConfigSubscriptionPanel.java",
                "ConfigSubscriptionCardRenderer.java"
        );
        for (String file : files) {
            Path p = resolveGui(file);
            assertTrue(Files.exists(p), file + " deve existir");
            String content = Files.readString(p);
            assertFalse(content.contains("JOptionPane.show"),
                    file + " não pode conter chamadas legadas a JOptionPane");
        }
    }

    @Test
    @DisplayName("CSP-03: Renderização do cartão de subscrição e formatação visual")
    void csp03_rendererLogicAndStates() {
        assertEquals("—", ConfigSubscriptionCardRenderer.daysText(null));
        assertEquals("Expirada há 3 dia(s)", ConfigSubscriptionCardRenderer.daysText(-3L));
        assertEquals("Expira hoje", ConfigSubscriptionCardRenderer.daysText(0L));
        assertEquals("15 dia(s)", ConfigSubscriptionCardRenderer.daysText(15L));

        assertEquals(UIHelper.TEXT_LIGHT, ConfigSubscriptionCardRenderer.daysColor(null));
        assertEquals(UIHelper.REJECTED_RED, ConfigSubscriptionCardRenderer.daysColor(-1L));
        assertEquals(UIHelper.PENDING_YELLOW, ConfigSubscriptionCardRenderer.daysColor(5L));
        assertEquals(UIHelper.APPROVED_GREEN, ConfigSubscriptionCardRenderer.daysColor(30L));

        assertEquals(UIHelper.APPROVED_GREEN, ConfigSubscriptionCardRenderer.statusColor("ACTIVE"));
        assertEquals(UIHelper.APPROVED_GREEN, ConfigSubscriptionCardRenderer.statusColor("TRIAL"));
        assertEquals(UIHelper.REJECTED_RED, ConfigSubscriptionCardRenderer.statusColor("EXPIRED"));
        assertEquals(UIHelper.REJECTED_RED, ConfigSubscriptionCardRenderer.statusColor("SUSPENDED"));
        assertEquals(UIHelper.TEXT_LIGHT, ConfigSubscriptionCardRenderer.statusColor("OTHER"));

        JPanel card = new JPanel();

        // Estado Vazio
        ConfigSubscriptionCardRenderer.renderEmpty(card);
        assertTrue(card.getComponentCount() > 0);

        // Estado Erro
        ConfigSubscriptionCardRenderer.renderError(card, "Falha de rede");
        assertTrue(card.getComponentCount() > 0);

        // Estado Assinatura Activa
        MySubscriptionDTO sub = new MySubscriptionDTO(
                "Empresa Teste Lda", true, "PRO", "Profissional", "ACTIVE", "Activo",
                LocalDate.now(), LocalDate.now().plusDays(30), 30L, new BigDecimal("3500.00")
        );
        ConfigSubscriptionCardRenderer.renderSubscription(card, sub);
        assertEquals(6, card.getComponentCount(), "Cartão de assinatura activa deve conter 6 campos informativos");
    }

    @Test
    @DisplayName("CSP-04: Estrutura do painel e botões de cabeçalho")
    void csp04_panelStructureAndButtons() {
        ConfigPanel mockOwner = Mockito.mock(ConfigPanel.class);
        ConfigSubscriptionPanel panel = new ConfigSubscriptionPanel(mockOwner);
        JPanel view = panel.buildPanel();
        assertNotNull(view);

        boolean foundRenew = false;
        boolean foundRefresh = false;

        for (java.awt.Component comp : view.getComponents()) {
            if (comp instanceof JPanel header) {
                for (java.awt.Component hChild : header.getComponents()) {
                    if (hChild instanceof JPanel actions) {
                        for (java.awt.Component btn : actions.getComponents()) {
                            if (btn instanceof JButton b) {
                                if ("Renovar / Activar Plano".equals(b.getText())) foundRenew = true;
                                if ("Actualizar".equals(b.getText())) foundRefresh = true;
                            }
                        }
                    }
                }
            }
        }

        assertTrue(foundRenew, "Botão 'Renovar / Activar Plano' deve existir no cabeçalho");
        assertTrue(foundRefresh, "Botão 'Actualizar' deve existir no cabeçalho");
    }
}
