package mz.multicore.erp.gui;

import mz.multicore.erp.gui.components.ClientTablePagination;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.Component;
import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Harness de teste para a barra dinâmica de totais e contagens no rodapé das tabelas.
 */
class DynamicTableTotalsFooterHarnessTest {

    @Test
    @DisplayName("DTT-01: ClientTablePagination calcula e formata totais na selecção a 2 casas decimais")
    void testDynamicFooterTotalsCalculation() {
        DefaultTableModel model = new DefaultTableModel(
                new String[]{"ID", "Item", "Qtd", "Total MT"}, 0
        );
        model.addRow(new Object[]{1L, "Artigo Alpha", new BigDecimal("10.00"), "1 500,00 MT"});
        model.addRow(new Object[]{2L, "Artigo Beta", new BigDecimal("5.50"), "750,00 MT"});
        model.addRow(new Object[]{3L, "Artigo Gamma", new BigDecimal("2.00"), "250,00 MT"});

        JTable table = new JTable(model);
        JPanel pager = ClientTablePagination.install(table);

        // Encontra o label statusLeft no painel do pager
        JLabel statusLabel = findStatusLabel(pager);
        assertTrue(statusLabel != null, "statusLabel deve existir no rodapé");

        // Sem selecção: deve indicar 3 registos
        assertTrue(statusLabel.getText().contains("3 registo(s)"), "Deve indicar 3 registo(s)");

        // Selecciona linhas 0 e 1 (1.500 + 750 = 2.250,00 MT)
        table.setRowSelectionInterval(0, 1);

        String text = statusLabel.getText();
        assertTrue(text.contains("2 sel."), "Deve indicar 2 seleccionados");
        assertTrue(text.contains("2 250,00 MT"), "Deve somar e formatar a 2 casas decimais: 2 250,00 MT");
    }

    private JLabel findStatusLabel(JPanel panel) {
        for (Component c : panel.getComponents()) {
            if (c instanceof JPanel p) {
                for (Component child : p.getComponents()) {
                    if (child instanceof JLabel l && (l.getText().contains("registo") || l.getText().contains("Sem registos"))) {
                        return l;
                    }
                }
            }
        }
        return null;
    }
}
