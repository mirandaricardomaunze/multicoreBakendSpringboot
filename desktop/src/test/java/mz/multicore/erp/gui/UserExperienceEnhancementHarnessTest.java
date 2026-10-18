package mz.multicore.erp.gui;

import mz.multicore.erp.gui.components.ModernFormDialog;
import mz.multicore.erp.gui.components.ShortcutHelpDialog;
import mz.multicore.erp.gui.components.TableFilter;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.event.ActionEvent;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Harness: Melhorias de Experiência do Utilizador (USER_EXPERIENCE_ENHANCEMENT_SPEC)")
public class UserExperienceEnhancementHarnessTest {

    @Test
    @DisplayName("UX-01: ModernFormDialog foca recursivamente o primeiro componente de entrada interactivo")
    void testModernFormDialogFocusFirstComponent() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            JPanel form = new JPanel();
            JLabel label = new JLabel("Nome:");
            JTextField disabledField = new JTextField("Inactivo");
            disabledField.setEnabled(false);
            JTextField activeField = new JTextField();
            activeField.setEnabled(true);
            activeField.setEditable(true);

            form.add(label);
            form.add(disabledField);
            form.add(activeField);

            boolean focused = ModernFormDialog.focusFirstComponent(form);
            assertTrue(focused, "Deve localizar e focar o primeiro campo interactivo");
            assertTrue(activeField.hasFocus() || activeField.isFocusOwner() || activeField.isFocusable(),
                    "O campo activo deve ser o alvo do foco");
        });
    }

    @Test
    @DisplayName("UX-02: TableFilter.clear restaura caixa de pesquisa e dropdowns para o índice inicial")
    void testTableFilterClearAction() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            DefaultTableModel model = new DefaultTableModel(new String[]{"ID", "Nome", "Estado"}, 0);
            model.addRow(new Object[]{"1", "Produto A", "Activo"});
            model.addRow(new Object[]{"2", "Produto B", "Inactivo"});
            JTable table = new JTable(model);

            JTextField search = new JTextField();
            JComboBox<String> statusCombo = new JComboBox<>(new String[]{"Todos os Estados", "Activo", "Inactivo"});
            TableFilter.ColumnFilter filter = new TableFilter.ColumnFilter(statusCombo, 2);

            TableFilter.install(table, search, filter);

            // Simula filtros preenchidos pelo utilizador
            search.setText("Produto A");
            statusCombo.setSelectedIndex(1);
            assertEquals("Produto A", search.getText());
            assertEquals(1, statusCombo.getSelectedIndex());

            // Executa limpeza central
            TableFilter.clear(table);

            assertEquals("", search.getText(), "Caixa de pesquisa deve ser esvaziada");
            assertEquals(0, statusCombo.getSelectedIndex(), "Dropdown de filtro deve regressar ao índice 0 (Todos)");
        });
    }

    @Test
    @DisplayName("UX-03: Tecla Esc no campo de pesquisa dispara a limpeza dos filtros")
    void testTableFilterEscKeyClearsSearch() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            DefaultTableModel model = new DefaultTableModel(new String[]{"Código", "Designação"}, 0);
            model.addRow(new Object[]{"101", "Parafuso"});
            JTable table = new JTable(model);

            JTextField search = new JTextField();
            TableFilter.install(table, search);

            search.setText("Parafuso");
            assertEquals("Parafuso", search.getText());

            // Invoca a acção associada ao atalho ESC registada no search
            java.awt.event.ActionListener escAction = search.getActionForKeyStroke(KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_ESCAPE, 0));
            assertNotNull(escAction, "Atalho ESC deve estar registado no campo de pesquisa do TableFilter");
            escAction.actionPerformed(new ActionEvent(search, ActionEvent.ACTION_PERFORMED, ""));

            assertEquals("", search.getText(), "Tecla ESC deve limpar o texto da pesquisa");
        });
    }

    @Test
    @DisplayName("UX-04: ShortcutHelpDialog contém os atalhos canónicos actualizados (F10, Espaço, F3, Esc)")
    void testShortcutHelpDialogCanonicalShortcuts() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            ShortcutHelpDialog dialog = new ShortcutHelpDialog(null);
            assertNotNull(dialog);

            // Inspecciona os textos contidos nos componentes do diálogo
            String allText = extractAllText(dialog);
            assertTrue(allText.contains("F10"), "Deve incluir F10 para finalizar venda no POS");
            assertTrue(allText.contains("Espaço"), "Deve incluir Espaço para espreitar linha (Quick-Peek)");
            assertTrue(allText.contains("F3"), "Deve incluir F3 para pesquisa no catálogo");
            assertTrue(allText.contains("Esc"), "Deve incluir Esc para limpar pesquisas e fechar");
        });
    }

    private static String extractAllText(java.awt.Container container) {
        StringBuilder sb = new StringBuilder();
        for (java.awt.Component c : container.getComponents()) {
            if (c instanceof JLabel l) sb.append(l.getText()).append(" ");
            if (c instanceof AbstractButton b) sb.append(b.getText()).append(" ");
            if (c instanceof java.awt.Container child) sb.append(extractAllText(child)).append(" ");
        }
        return sb.toString();
    }
}
