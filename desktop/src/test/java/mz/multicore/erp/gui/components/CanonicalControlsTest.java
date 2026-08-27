package mz.multicore.erp.gui.components;

import org.junit.jupiter.api.Test;

import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JTable;
import javax.swing.ListCellRenderer;
import javax.swing.SwingConstants;
import javax.swing.table.TableCellRenderer;
import java.awt.Component;
import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;

class CanonicalControlsTest {

    @Test
    void styleComboBox_rendererExistente_preservaRepresentacao() {
        JComboBox<String> combo = new JComboBox<>(new String[]{"ADMIN"});
        JLabel marker = new JLabel("Administrador");
        combo.setRenderer((list, value, index, selected, focus) -> marker);

        UIHelper.styleComboBox(combo);
        @SuppressWarnings("unchecked")
        ListCellRenderer<Object> renderer = (ListCellRenderer<Object>) combo.getRenderer();
        Component rendered = renderer.getListCellRendererComponent(new JList<>(), "ADMIN", 0, false, false);

        assertSame(marker, rendered);
        assertEquals("Administrador", ((JLabel) rendered).getText());
    }

    @Test
    void humanStatus_parcialmentePaga_traduz() {
        assertEquals("Parcialmente paga", UIHelper.humanStatus("PARTIALLY_PAID"));
    }

    @Test
    void iconButton_nomeAcessivel_configuraTooltipEAcessibilidade() {
        ModernButton button = UIHelper.createIconButton("Actualizar dados", "fas-sync");
        assertEquals("Actualizar dados", button.getToolTipText());
        assertEquals("Actualizar dados", button.getAccessibleContext().getAccessibleName());
        assertNotNull(button.getIcon());
    }

    @Test
    void moneyRenderer_bigDecimal_alinhaEFormata() {
        JTable table = new JTable(1, 1);
        TableCellRenderer renderer = TableCellRenderers.money();
        JLabel label = (JLabel) renderer.getTableCellRendererComponent(
                table, new BigDecimal("1234.5"), false, false, 0, 0);
        assertEquals("1 234,50 MT", label.getText());
        assertEquals(SwingConstants.RIGHT, label.getHorizontalAlignment());
    }

    @Test
    void quantityRenderer_bigDecimal_alinhaEFormata() {
        JTable table = new JTable(1, 1);
        JLabel label = (JLabel) TableCellRenderers.quantity().getTableCellRendererComponent(
                table, new BigDecimal("2.5"), false, false, 0, 0);
        assertEquals("2,500", label.getText());
        assertEquals(SwingConstants.RIGHT, label.getHorizontalAlignment());
    }

    @Test
    void statusRenderer_desconhecido_mantemTextoSeguro() {
        JTable table = new JTable(1, 1);
        JLabel label = (JLabel) TableCellRenderers.status().getTableCellRendererComponent(
                table, "CUSTOM_STATE", false, false, 0, 0);
        assertEquals("CUSTOM STATE", label.getText());
        assertEquals(UIHelper.TEXT_MUTED, label.getForeground());
    }
}
