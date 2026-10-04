package mz.multicore.erp.gui.components;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Harness automatizado para o SPEC docs/QUICK_PEEK_SILENCIOSO_SPEC.md
 * e HARNESS docs/QUICK_PEEK_SILENCIOSO_HARNESS.md.
 */
class TableQuickPeekHarnessTest {

    private JTable createSampleTable() {
        String[] cols = {"Referência", "Cliente / Descrição", "Estado", "Total"};
        Object[][] data = {
                {"FT 2026/001", "Empresa Alpha Lda", "Pago", "12 500,00 MT"},
                {"FT 2026/002", "Beta Soluções", "Pendente", "45 200,00 MT"},
                {"FT 2026/003", "Gama Trading", "Cancelado", "3 100,00 MT"}
        };
        DefaultTableModel model = new DefaultTableModel(data, cols) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        return new JTable(model);
    }

    @Test
    @DisplayName("PEEK-01: Inicialização Silenciosa — Drawer permanece invisível e sem ruído visual")
    void testSilentInitialization() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            JTable table = createSampleTable();
            JPanel cardContainer = new JPanel();

            TableQuickPeekController controller = TableQuickPeekController.install(table, cardContainer);

            assertFalse(controller.isOpen(), "O drawer deve iniciar fechado (invisível)");
            assertFalse(controller.getPeekPanel().isVisible(), "O painel lateral não deve estar visível no arranque");
            assertEquals(table, controller.getTable());
        });
    }

    @Test
    @DisplayName("PEEK-02: Acionamento por Tecla Espaço (SPACE) abre o painel e popula campos")
    void testSpaceKeyOpensAndPopulatesDrawer() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            JTable table = createSampleTable();
            JPanel cardContainer = new JPanel();
            TableQuickPeekController controller = TableQuickPeekController.install(table, cardContainer);

            // Seleciona a segunda linha (Beta Soluções)
            table.setRowSelectionInterval(1, 1);

            // Simula clique da barra de espaço
            KeyEvent spaceEvent = new KeyEvent(table, KeyEvent.KEY_PRESSED, System.currentTimeMillis(), 0, KeyEvent.VK_SPACE, ' ');
            for (var kl : table.getKeyListeners()) {
                kl.keyPressed(spaceEvent);
            }

            assertTrue(controller.isOpen(), "A barra de espaço deve abrir o Quick Peek");
            assertTrue(controller.getPeekPanel().isVisible());

            QuickPeekPanel panel = controller.getPeekPanel();
            assertEquals("FT 2026/002", panel.getTitleLabel().getText());
            assertEquals("PENDENTE", panel.getStatusBadge().getText());
            assertTrue(panel.getStatusBadge().isVisible());
        });
    }

    @Test
    @DisplayName("PEEK-03: Fecho por Tecla ESC e alternância por Espaço")
    void testEscAndSpaceClosesDrawer() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            JTable table = createSampleTable();
            JPanel cardContainer = new JPanel();
            TableQuickPeekController controller = TableQuickPeekController.install(table, cardContainer);

            table.setRowSelectionInterval(0, 0);
            controller.open();
            assertTrue(controller.isOpen());

            // Pressionar ESC fecha
            KeyEvent escEvent = new KeyEvent(table, KeyEvent.KEY_PRESSED, System.currentTimeMillis(), 0, KeyEvent.VK_ESCAPE, (char) 27);
            for (var kl : table.getKeyListeners()) {
                kl.keyPressed(escEvent);
            }
            assertFalse(controller.isOpen(), "A tecla ESC deve fechar o Quick Peek");

            // Pressionar Espaço abre novamente
            KeyEvent spaceEvent = new KeyEvent(table, KeyEvent.KEY_PRESSED, System.currentTimeMillis(), 0, KeyEvent.VK_SPACE, ' ');
            for (var kl : table.getKeyListeners()) {
                kl.keyPressed(spaceEvent);
            }
            assertTrue(controller.isOpen());

            // Pressionar Espaço novamente fecha (toggle)
            for (var kl : table.getKeyListeners()) {
                kl.keyPressed(spaceEvent);
            }
            assertFalse(controller.isOpen(), "Pressionar Espaço com o drawer aberto deve fechá-lo");
        });
    }

    @Test
    @DisplayName("PEEK-04: Navegação Reativa por Setas (UP / DOWN) atualiza os dados em tempo real")
    void testReactiveRowNavigationUpdatesDrawer() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            JTable table = createSampleTable();
            JPanel cardContainer = new JPanel();
            TableQuickPeekController controller = TableQuickPeekController.install(table, cardContainer);

            table.setRowSelectionInterval(0, 0);
            controller.open();
            assertEquals("FT 2026/001", controller.getPeekPanel().getTitleLabel().getText());
            assertEquals("PAGO", controller.getPeekPanel().getStatusBadge().getText());

            // Simula descida de linha com a seta (mudança de seleção)
            table.setRowSelectionInterval(2, 2);

            // O conteúdo do Quick Peek deve ter sido atualizado reativamente para Gama Trading
            assertEquals("FT 2026/003", controller.getPeekPanel().getTitleLabel().getText());
            assertEquals("CANCELADO", controller.getPeekPanel().getStatusBadge().getText());
        });
    }

    @Test
    @DisplayName("PEEK-05: Fallback Gracioso sem seleção seleciona linha 0 automaticamente")
    void testGracefulFallbackWhenNoRowSelected() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            JTable table = createSampleTable();
            table.clearSelection();
            assertEquals(-1, table.getSelectedRow());

            JPanel cardContainer = new JPanel();
            TableQuickPeekController controller = TableQuickPeekController.install(table, cardContainer);

            controller.toggle();
            assertTrue(controller.isOpen());
            assertEquals(0, table.getSelectedRow(), "Deve ter auto-selecionado a primeira linha");
            assertEquals("FT 2026/001", controller.getPeekPanel().getTitleLabel().getText());
        });
    }

    @Test
    @DisplayName("PEEK-06: Botão Ver Completo dispara callback com o índice correto de modelo")
    void testOpenFullButtonCallback() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            JTable table = createSampleTable();
            JPanel cardContainer = new JPanel();
            TableQuickPeekController controller = TableQuickPeekController.install(table, cardContainer);

            AtomicInteger openedModelRow = new AtomicInteger(-1);
            controller.setOnOpenFullCallback(openedModelRow::set);

            table.setRowSelectionInterval(1, 1);
            controller.open();

            controller.getPeekPanel().getOpenFullButton().doClick();
            assertEquals(1, openedModelRow.get(), "O callback de abertura completa deve receber a linha 1");
        });
    }

    @Test
    @DisplayName("PEEK-07: Integração com TableQuickFilterBar adiciona botão discreto de alternância")
    void testTableQuickFilterBarIntegration() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            JTable table = createSampleTable();
            JPanel cardContainer = new JPanel();
            TableQuickPeekController controller = TableQuickPeekController.install(table, cardContainer);

            TableQuickFilterBar filterBar = new TableQuickFilterBar(table);
            filterBar.attachQuickPeek(controller);

            assertFalse(controller.isOpen());

            // Encontra e clica no botão adicionado na barra
            JButton peekBtn = findButtonWithTooltip(filterBar, "Espreitar");
            assertThat(peekBtn).isNotNull();

            peekBtn.doClick();
            assertTrue(controller.isOpen(), "O clique no botão da barra de filtros deve abrir o Quick Peek");

            peekBtn.doClick();
            assertFalse(controller.isOpen(), "O segundo clique deve fechar o Quick Peek");
        });
    }

    private JButton findButtonWithTooltip(Container container, String tooltipSubstring) {
        for (Component c : container.getComponents()) {
            if (c instanceof JButton btn && btn.getToolTipText() != null && btn.getToolTipText().contains(tooltipSubstring)) {
                return btn;
            }
            if (c instanceof Container sub) {
                JButton found = findButtonWithTooltip(sub, tooltipSubstring);
                if (found != null) return found;
            }
        }
        return null;
    }
}
