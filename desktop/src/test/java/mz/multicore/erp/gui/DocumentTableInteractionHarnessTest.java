package mz.multicore.erp.gui;

import mz.multicore.erp.gui.components.UIHelper;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Teste de Homologação e Harness para SPEC-DTI-001:
 * Duplo Clique e Atalhos de Teclado Universais nas Tabelas de Documentos.
 */
public class DocumentTableInteractionHarnessTest {

    @BeforeAll
    static void initHeadless() {
        System.setProperty("java.awt.headless", "true");
    }

    private static Path resolvePath(String relativePath) {
        Path p = Path.of(relativePath);
        if (Files.exists(p)) return p;
        p = Path.of("desktop", relativePath);
        if (Files.exists(p)) return p;
        return Path.of("..", relativePath);
    }

    private static String readSource(String relativePath) throws IOException {
        Path path = resolvePath(relativePath);
        assertTrue(Files.exists(path), "Ficheiro não encontrado: " + relativePath);
        return Files.readString(path);
    }

    @Test
    @DisplayName("DTI-00: SPEC-DTI-001 existe e define os requisitos obrigatórios")
    void dti00_specExistsAndCoversRequirements() throws IOException {
        String spec = readSource("docs/DOCUMENT_TABLE_INTERACTION_SPEC.md");
        assertThat(spec).contains("SPEC-DTI-001")
                .contains("DTI-01")
                .contains("DTI-02")
                .contains("DTI-03")
                .contains("DTI-04")
                .contains("installDoubleClick")
                .contains("installDocumentGridShortcuts");
    }

    @Test
    @DisplayName("DTI-01: Tabelas de listagem possuem duplo clique configurado")
    void dti01_listingTablesHaveDoubleClick() throws IOException {
        String ordersView = readSource("desktop/src/main/java/mz/multicore/erp/gui/CommercialOrdersView.java");
        assertThat(ordersView).contains("installDoubleClick(owner.ordersTable, owner::openSelectedOrderEditor)");

        String invoicesView = readSource("desktop/src/main/java/mz/multicore/erp/gui/CommercialInvoicesView.java");
        assertThat(invoicesView).contains("installDoubleClick(owner.invoicesTable, owner::printSelectedInvoice)");

        String quotationsPanel = readSource("desktop/src/main/java/mz/multicore/erp/gui/commercial/QuotationsPanel.java");
        assertThat(quotationsPanel).contains("openSelectedEditor()");

        String purchasePanel = readSource("desktop/src/main/java/mz/multicore/erp/gui/PurchaseOrdersPanel.java");
        assertThat(purchasePanel).contains("installDoubleClick(poListTable, this::openSelectedEditor)");

        String stockPanel = readSource("desktop/src/main/java/mz/multicore/erp/gui/StockPanel.java");
        assertThat(stockPanel).contains("transferActions.openSelectedEditor()");
    }

    @Test
    @DisplayName("DTI-02: Todas as grelhas de itens de documento instalam atalhos universais")
    void dti02_itemGridsInstallUniversalShortcuts() throws IOException {
        String ordersView = readSource("desktop/src/main/java/mz/multicore/erp/gui/CommercialOrdersView.java");
        assertThat(ordersView).contains("installDocumentGridShortcuts(owner.orderLinesTable");

        String invoicesView = readSource("desktop/src/main/java/mz/multicore/erp/gui/CommercialInvoicesView.java");
        assertThat(invoicesView).contains("installDocumentGridShortcuts(owner.linesTable");

        String quotationsForm = readSource("desktop/src/main/java/mz/multicore/erp/gui/commercial/QuotationEditorForm.java");
        assertThat(quotationsForm).contains("installDocumentGridShortcuts(linesTable");

        String purchasePanel = readSource("desktop/src/main/java/mz/multicore/erp/gui/PurchaseOrdersPanel.java");
        assertThat(purchasePanel).contains("installDocumentGridShortcuts(poLinesTable");

        String stockTransferForm = readSource("desktop/src/main/java/mz/multicore/erp/gui/StockTransferEditorForm.java");
        assertThat(stockTransferForm).contains("installDocumentGridShortcuts(linesTable");
    }

    @Test
    @DisplayName("DTI-03: Runtime de UIHelper.installDocumentGridShortcuts e protecção de edição")
    void dti03_runtimeGridShortcutsExecution() {
        DefaultTableModel model = new DefaultTableModel(new String[]{"Produto", "Qtd"}, 0);
        model.addRow(new Object[]{"Artigo 1", 10});
        JTable table = new JTable(model);
        table.setRowSelectionInterval(0, 0);

        AtomicInteger addCount = new AtomicInteger();
        AtomicInteger removeCount = new AtomicInteger();
        AtomicInteger saveCount = new AtomicInteger();

        UIHelper.installDocumentGridShortcuts(table, addCount::incrementAndGet, removeCount::incrementAndGet, saveCount::incrementAndGet);

        InputMap im = table.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
        ActionMap am = table.getActionMap();

        // 1. Verificar atalhos no InputMap
        assertEquals("gridAddLine", im.get(KeyStroke.getKeyStroke(KeyEvent.VK_INSERT, 0)));
        assertEquals("gridAddLine", im.get(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, InputEvent.CTRL_DOWN_MASK)));
        assertEquals("gridRemoveLine", im.get(KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0)));
        assertEquals("gridRemoveLine", im.get(KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, InputEvent.CTRL_DOWN_MASK)));
        assertEquals("gridSaveDoc", im.get(KeyStroke.getKeyStroke(KeyEvent.VK_S, InputEvent.CTRL_DOWN_MASK)));
        assertEquals("gridSaveDoc", im.get(KeyStroke.getKeyStroke(KeyEvent.VK_F10, 0)));

        // 2. Disparar acção de Adicionar
        Action addAction = am.get("gridAddLine");
        assertNotNull(addAction);
        addAction.actionPerformed(new ActionEvent(table, ActionEvent.ACTION_PERFORMED, "add"));
        assertEquals(1, addCount.get(), "Adicionar linha deve ter sido chamado 1 vez");

        // 3. Disparar acção de Gravar
        Action saveAction = am.get("gridSaveDoc");
        assertNotNull(saveAction);
        saveAction.actionPerformed(new ActionEvent(table, ActionEvent.ACTION_PERFORMED, "save"));
        assertEquals(1, saveCount.get(), "Gravar documento deve ter sido chamado 1 vez");

        // 4. Disparar acção de Remover quando NÃO está a editar célula
        Action removeAction = am.get("gridRemoveLine");
        assertNotNull(removeAction);
        removeAction.actionPerformed(new ActionEvent(table, ActionEvent.ACTION_PERFORMED, "remove"));
        assertEquals(1, removeCount.get(), "Remover linha deve executar quando !table.isEditing()");
    }

    @Test
    @DisplayName("DTI-04: Runtime de UIHelper.installDoubleClick dispara acção em linha seleccionada")
    void dti04_runtimeDoubleClickExecution() {
        DefaultTableModel model = new DefaultTableModel(new String[]{"ID", "Nome"}, 0);
        model.addRow(new Object[]{1L, "Documento Teste"});
        JTable table = new JTable(model);
        table.setRowSelectionInterval(0, 0);

        int initialListeners = table.getMouseListeners().length;
        AtomicBoolean clicked = new AtomicBoolean(false);
        UIHelper.installDoubleClick(table, () -> clicked.set(true));

        assertEquals(initialListeners + 1, table.getMouseListeners().length, "Deve haver 1 novo MouseListener instalado");
        var listeners = table.getMouseListeners();
        var listener = listeners[listeners.length - 1];

        // Disparar clique simples — NÃO deve disparar
        MouseEvent singleClick = new MouseEvent(table, MouseEvent.MOUSE_CLICKED, System.currentTimeMillis(),
                0, 10, 10, 1, false, MouseEvent.BUTTON1);
        listener.mouseClicked(singleClick);
        assertFalse(clicked.get(), "Clique simples não deve abrir o editor");

        // Disparar clique duplo — DEVE disparar
        MouseEvent doubleClick = new MouseEvent(table, MouseEvent.MOUSE_CLICKED, System.currentTimeMillis(),
                0, 10, 10, 2, false, MouseEvent.BUTTON1);
        listener.mouseClicked(doubleClick);
        assertTrue(clicked.get(), "Duplo clique deve abrir o editor");
    }
}
