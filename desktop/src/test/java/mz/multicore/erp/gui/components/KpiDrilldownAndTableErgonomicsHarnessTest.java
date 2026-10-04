package mz.multicore.erp.gui.components;

import org.junit.jupiter.api.Test;

import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import java.awt.Cursor;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class KpiDrilldownAndTableErgonomicsHarnessTest {

    @Test
    void testMakeInteractiveConfiguresHandCursorAndAccessibility() {
        ModernPanel card = KpiCard.createCard("Vendas", new JLabel("15.000 MT"), "Mês actual", "fas-chart-line", UIHelper.ACCENT_BLUE);
        assertNotNull(card);

        KpiCard.makeInteractive(card, "Filtrar vendas por este mês", () -> {});

        assertThat(card.getCursor().getType()).isEqualTo(Cursor.HAND_CURSOR);
        assertTrue(card.isFocusable(), "Card interativo deve aceitar foco para acessibilidade");
        assertThat(card.getToolTipText()).isEqualTo("Filtrar vendas por este mês");
        assertThat(card.getPreferredSize().height).isEqualTo(KpiCard.STANDARD_CARD_HEIGHT);
    }

    @Test
    void testInteractiveCardTriggersActionOnLeftClick() {
        AtomicBoolean clicked = new AtomicBoolean(false);
        ModernPanel card = KpiCard.createInteractiveCard(
                "Rupturas",
                new JLabel("5"),
                "Itens esgotados",
                "fas-boxes",
                UIHelper.REJECTED_RED,
                "Clique para ver rupturas",
                () -> clicked.set(true)
        );

        assertFalse(clicked.get());

        // Simular clique do botão esquerdo do rato
        MouseEvent click = new MouseEvent(
                card,
                MouseEvent.MOUSE_CLICKED,
                System.currentTimeMillis(),
                0,
                20,
                20,
                1,
                false,
                MouseEvent.BUTTON1
        );

        for (var listener : card.getMouseListeners()) {
            listener.mouseClicked(click);
        }

        assertTrue(clicked.get(), "O clique com o botão esquerdo deve disparar a acção de drilldown do KPI card");
    }

    @Test
    void testInteractiveCardTriggersActionOnEnterOrSpaceKey() {
        AtomicInteger keyCount = new AtomicInteger(0);
        ModernPanel card = KpiCard.createInteractiveMetricCard(
                "Sobras",
                new JLabel("12"),
                "Impacto positivo",
                "fas-arrow-up",
                UIHelper.ACCENT_CYAN,
                "Clique para filtrar",
                keyCount::incrementAndGet
        );

        assertEquals(0, keyCount.get());

        // Simular tecla Enter
        KeyEvent enterKey = new KeyEvent(card, KeyEvent.KEY_PRESSED, System.currentTimeMillis(), 0, KeyEvent.VK_ENTER, '\n');
        for (var listener : card.getKeyListeners()) {
            listener.keyPressed(enterKey);
        }
        assertEquals(1, keyCount.get(), "A tecla ENTER deve activar a acção de drilldown");

        // Simular tecla Espaço
        KeyEvent spaceKey = new KeyEvent(card, KeyEvent.KEY_PRESSED, System.currentTimeMillis(), 0, KeyEvent.VK_SPACE, ' ');
        for (var listener : card.getKeyListeners()) {
            listener.keyPressed(spaceKey);
        }
        assertEquals(2, keyCount.get(), "A tecla ESPAÇO deve activar a acção de drilldown");
    }

    @Test
    void testInstallRowDoubleClickHandlerTranslatesModelIndex() {
        DefaultTableModel model = new DefaultTableModel(new String[]{"ID", "Nome"}, 0);
        model.addRow(new Object[]{"101", "Artigo A"});
        model.addRow(new Object[]{"102", "Artigo B"});
        model.addRow(new Object[]{"103", "Artigo C"});

        JTable table = new JTable(model);
        TableRowSorter<DefaultTableModel> sorter = new TableRowSorter<>(model);
        table.setRowSorter(sorter);

        AtomicInteger doubleClickedModelRow = new AtomicInteger(-1);
        UIHelper.installRowDoubleClickHandler(table, doubleClickedModelRow::set);

        // Simular duplo clique na linha 1
        table.setRowSelectionInterval(1, 1);
        int rowY = table.getCellRect(1, 0, true).y + 2;
        MouseEvent doubleClick = new MouseEvent(
                table,
                MouseEvent.MOUSE_CLICKED,
                System.currentTimeMillis(),
                0,
                10,
                rowY,
                2, // clickCount = 2
                false,
                MouseEvent.BUTTON1
        );

        for (var listener : table.getMouseListeners()) {
            listener.mouseClicked(doubleClick);
        }

        assertThat(doubleClickedModelRow.get()).isEqualTo(1);
    }

    @Test
    void testInstallRowContextMenuSelectsRowAndProvidesPopup() {
        DefaultTableModel model = new DefaultTableModel(new String[]{"ID", "Nome"}, 0);
        model.addRow(new Object[]{"1", "Registo 1"});
        model.addRow(new Object[]{"2", "Registo 2"});

        JTable table = new JTable(model);
        table.setSize(300, 200);

        AtomicInteger rightClickedRow = new AtomicInteger(-1);
        UIHelper.installRowContextMenu(table, row -> {
            rightClickedRow.set(row);
            JPopupMenu menu = new JPopupMenu();
            menu.add(new JMenuItem("Detalhes"));
            return menu;
        });

        int targetY = table.getCellRect(1, 0, true).y + 2;
        // Simular clique direito popup trigger
        MouseEvent popupEvent = new MouseEvent(
                table,
                MouseEvent.MOUSE_PRESSED,
                System.currentTimeMillis(),
                MouseEvent.BUTTON3_DOWN_MASK,
                10,
                targetY,
                1,
                true, // isPopupTrigger
                MouseEvent.BUTTON3
        );

        for (var listener : table.getMouseListeners()) {
            listener.mousePressed(popupEvent);
        }

        assertThat(table.getSelectedRow()).isEqualTo(1);
        assertThat(rightClickedRow.get()).isEqualTo(1);
    }

    @Test
    void testDashboardKpiCardsInteractiveNavigation() {
        java.util.List<String> navigatedModules = new java.util.ArrayList<>();
        mz.multicore.erp.gui.DashboardPanel dashboard = new mz.multicore.erp.gui.DashboardPanel(
                null, null, null, null, null, null,
                null, null, null, null,
                navigatedModules::add
        );

        java.util.List<ModernPanel> kpiCards = new java.util.ArrayList<>();
        findKpiCards(dashboard, kpiCards);

        // Deve conter os 8 cartões de KPI do topo
        assertThat(kpiCards).isNotEmpty();

        // Todos os cartões de KPI interativos devem possuir HAND_CURSOR e tooltip descritiva
        for (ModernPanel card : kpiCards) {
            assertThat(card.getCursor().getType()).isEqualTo(Cursor.HAND_CURSOR);
            assertThat(card.getToolTipText()).isNotBlank();
        }

        // Simular clique no primeiro card (Saldo de Tesouraria -> financeiro)
        ModernPanel firstCard = kpiCards.get(0);
        MouseEvent click = new MouseEvent(
                firstCard,
                MouseEvent.MOUSE_CLICKED,
                System.currentTimeMillis(),
                0,
                10,
                10,
                1,
                false,
                MouseEvent.BUTTON1
        );
        for (var listener : firstCard.getMouseListeners()) {
            listener.mouseClicked(click);
        }

        assertThat(navigatedModules).contains("financeiro");
    }

    private static void findKpiCards(java.awt.Container root, java.util.List<ModernPanel> out) {
        for (java.awt.Component c : root.getComponents()) {
            if (c instanceof ModernPanel panel && panel.getToolTipText() != null && panel.getToolTipText().startsWith("Clique para abrir")) {
                out.add(panel);
            }
            if (c instanceof java.awt.Container child) {
                findKpiCards(child, out);
            }
        }
    }
}
