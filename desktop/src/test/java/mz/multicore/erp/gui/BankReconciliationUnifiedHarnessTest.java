package mz.multicore.erp.gui;

import mz.multicore.erp.desktop.client.BankReconciliationApiClient;
import mz.multicore.erp.desktop.client.FinanceApiClient;
import mz.multicore.erp.gui.components.ActionMenuButton;
import mz.multicore.erp.gui.components.ModernPanel;
import mz.multicore.erp.gui.components.QuickPeekPanel;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class BankReconciliationUnifiedHarnessTest {

    @BeforeAll
    static void initHeadless() {
        System.setProperty("java.awt.headless", "true");
    }

    @Mock
    private BankReconciliationApiClient apiClient;

    @Mock
    private FinanceApiClient financeApiClient;

    @Test
    @DisplayName("TRECON-01: Seletores e acções estão consolidados no card ModernPanel(16) sem barras exteriores")
    void trecon01_selectorsAndActionsContainedInsideCard() {
        BankReconciliationPanel panel = new BankReconciliationPanel(apiClient, financeApiClient);
        assertNotNull(panel);

        Container card = panel.itemsTable.getParent();
        while (card != null && !(card instanceof ModernPanel)) {
            card = card.getParent();
        }
        assertNotNull(card, "A tabela de movimentos deve residir dentro de um ModernPanel");

        assertTrue(isDescendant(card, panel.accountCombo),
                "O combo de seleção de conta deve estar contido dentro do card da tabela");
        assertTrue(isDescendant(card, panel.statementCombo),
                "O combo de seleção de extracto deve estar contido dentro do card da tabela");
        assertTrue(isDescendant(card, panel.operationsMenu),
                "O menu de operações deve estar contido dentro do card da tabela");

        // Verifica que o conteúdo principal não possui barras flutuantes exteriores soltas
        JScrollPane scrollPane = findComponent(panel, JScrollPane.class);
        assertNotNull(scrollPane, "Scroll pane do painel principal deve existir");
        Container viewContainer = (Container) scrollPane.getViewport().getView();
        assertNotNull(viewContainer);
        JPanel mainContent = (JPanel) viewContainer.getComponent(0);
        assertNotNull(mainContent);

        // Deve conter: Feedback, KpiGrid, Strut e TableCard (exatamente 4 componentes)
        assertEquals(4, mainContent.getComponentCount(),
                "mainContent deve conter apenas feedback, kpis, espaçador e tableCard (sem barras flutuantes exteriores)");
    }

    @Test
    @DisplayName("TRECON-02: ActionMenuButton de operações possui no máximo 5 acções")
    void trecon02_operationsMenuHasFiveOrFewerActions() {
        BankReconciliationPanel panel = new BankReconciliationPanel(apiClient, financeApiClient);
        ActionMenuButton menu = panel.operationsMenu;

        assertNotNull(menu, "ActionMenuButton 'Operações' deve existir");
        assertTrue(menu.actionCount() <= 5, "ActionMenuButton não pode ter mais de 5 acções");
        assertEquals(5, menu.actionCount(), "Operações bancárias essenciais consolidadas");

        assertEquals("Auto-Conciliar", menu.actionAt(0).getText());
        assertEquals("Conciliar Manualmente", menu.actionAt(1).getText());
        assertEquals("Lançar Encargo Bancário", menu.actionAt(2).getText());
        assertEquals("Desfazer Conciliação", menu.actionAt(3).getText());
        assertEquals("Emitir Relatório (PDF)", menu.actionAt(4).getText());
    }

    @Test
    @DisplayName("TRECON-03: Quick Peek silencioso com tecla SPACE instalado na tabela")
    void trecon03_quickPeekInstalledOnItemsTable() throws Exception {
        BankReconciliationPanel panel = new BankReconciliationPanel(apiClient, financeApiClient);

        Container card = panel.itemsTable.getParent();
        while (card != null && !(card instanceof ModernPanel)) {
            card = card.getParent();
        }
        assertNotNull(card);

        QuickPeekPanel quickPeek = findComponent(card, QuickPeekPanel.class);
        assertNotNull(quickPeek, "QuickPeekPanel drawer deve existir dentro do card da tabela");
        assertFalse(quickPeek.isVisible(), "QuickPeekPanel deve iniciar silencioso/fechado");

        // Adiciona uma linha de teste para que a tabela tenha conteúdo inspecionável
        SwingUtilities.invokeAndWait(() -> {
            DefaultTableModel model = (DefaultTableModel) panel.itemsTable.getModel();
            model.addRow(new Object[]{"04/10/2026", "Depósito BIM", "REF-001", BigDecimal.valueOf(5000), "PENDENTE", "—"});
        });

        // Simula pressão da tecla SPACE na tabela
        boolean spaceConsumed = false;
        for (KeyListener kl : panel.itemsTable.getKeyListeners()) {
            KeyEvent spacePress = new KeyEvent(panel.itemsTable, KeyEvent.KEY_PRESSED,
                    System.currentTimeMillis(), 0, KeyEvent.VK_SPACE, ' ');
            kl.keyPressed(spacePress);
            if (spacePress.isConsumed()) {
                spaceConsumed = true;
            }
        }

        assertTrue(spaceConsumed, "itemsTable deve consumir evento da tecla SPACE para abrir o QuickPeek");
        assertTrue(quickPeek.isVisible(), "QuickPeekPanel deve ficar visível após tecla SPACE");

        // Simula SPACE novamente para fechar
        for (KeyListener kl : panel.itemsTable.getKeyListeners()) {
            KeyEvent spacePress2 = new KeyEvent(panel.itemsTable, KeyEvent.KEY_PRESSED,
                    System.currentTimeMillis(), 0, KeyEvent.VK_SPACE, ' ');
            kl.keyPressed(spacePress2);
        }
        assertFalse(quickPeek.isVisible(), "QuickPeekPanel deve fechar após segundo SPACE");
    }

    @Test
    @DisplayName("TRECON-04: KPIs interativos com drilldown e atualização de filtros")
    void trecon04_interactiveKpiDrilldown() {
        BankReconciliationPanel panel = new BankReconciliationPanel(apiClient, financeApiClient);

        JScrollPane scrollPane = findComponent(panel, JScrollPane.class);
        assertNotNull(scrollPane);
        Container viewContainer = (Container) scrollPane.getViewport().getView();
        JPanel mainContent = (JPanel) viewContainer.getComponent(0);
        JPanel kpiGrid = (JPanel) mainContent.getComponent(1);

        // Componentes do grid: 0=Saldo Extracto, 1=Saldo Sistema, 2=Diferença, 3=Movimentos Pendentes
        assertEquals(4, kpiGrid.getComponentCount());

        JPanel cardBank = (JPanel) kpiGrid.getComponent(0);
        JPanel cardPending = (JPanel) kpiGrid.getComponent(3);

        panel.statusFilter.setSelectedItem("Todos os estados");
        assertEquals("Todos os estados", panel.statusFilter.getSelectedItem());

        // Clicar em Movimentos Pendentes deve filtrar para "PENDENTE"
        for (MouseListener ml : cardPending.getMouseListeners()) {
            ml.mouseClicked(new MouseEvent(cardPending, MouseEvent.MOUSE_CLICKED,
                    System.currentTimeMillis(), 0, 10, 10, 1, false, MouseEvent.BUTTON1));
        }
        assertEquals("PENDENTE", panel.statusFilter.getSelectedItem(),
                "Clicar no KPI 'Movimentos Pendentes' deve ativar filtro PENDENTE");

        // Clicar em Saldo no Extracto deve redefinir filtro para 'Todos os estados'
        for (MouseListener ml : cardBank.getMouseListeners()) {
            ml.mouseClicked(new MouseEvent(cardBank, MouseEvent.MOUSE_CLICKED,
                    System.currentTimeMillis(), 0, 10, 10, 1, false, MouseEvent.BUTTON1));
        }
        assertEquals("Todos os estados", panel.statusFilter.getSelectedItem(),
                "Clicar no KPI 'Saldo no Extracto' deve repor filtro inicial");
    }

    @Test
    @DisplayName("TRECON-05: Decomposição e limite estrito de linhas (<= 1000 linhas)")
    void trecon05_strictMaxLinesUnder1000() throws Exception {
        Path path = Path.of("src/main/java/mz/multicore/erp/gui/BankReconciliationPanel.java");
        if (!Files.exists(path)) {
            path = Path.of("desktop/src/main/java/mz/multicore/erp/gui/BankReconciliationPanel.java");
        }
        assertTrue(Files.exists(path), "BankReconciliationPanel.java deve existir");

        long lineCount = Files.lines(path).count();
        assertTrue(lineCount <= 1000,
                "BankReconciliationPanel.java não pode exceder 1000 linhas. Actual: " + lineCount);
    }

    private static boolean isDescendant(Container parent, Component target) {
        if (target == null) return false;
        Component current = target;
        while (current != null) {
            if (current == parent) return true;
            current = current.getParent();
        }
        return false;
    }

    @SuppressWarnings("unchecked")
    private static <T extends Component> T findComponent(Container parent, Class<T> clazz) {
        if (clazz.isInstance(parent)) {
            return (T) parent;
        }
        for (Component child : parent.getComponents()) {
            if (clazz.isInstance(child)) {
                return (T) child;
            }
            if (child instanceof Container container) {
                T found = findComponent(container, clazz);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }
}
