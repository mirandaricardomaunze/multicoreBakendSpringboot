package com.phcpro.gui;

import com.phcpro.gui.components.FormField;
import com.phcpro.gui.components.TableCellRenderers;
import com.phcpro.gui.components.UIHelper;
import org.junit.jupiter.api.Test;

import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.table.DefaultTableModel;
import java.awt.Component;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class FinalUiUniformityHarnessTest {

    @Test
    void roleRendererAndComboShowHumanLabelsWithoutChangingCode() {
        JTable table = new JTable(new DefaultTableModel(new Object[][]{{"MANAGER"}}, new Object[]{"Perfil"}));
        JLabel rendered = (JLabel) TableCellRenderers.role().getTableCellRendererComponent(
                table, "MANAGER", false, false, 0, 0);
        assertThat(rendered.getText()).isEqualTo("Gestor");

        JComboBox<String> combo = new JComboBox<>(new String[]{"EMPLOYEE", "MANAGER", "ADMIN"});
        UIHelper.styleComboBox(combo);
        UIHelper.humanizeRoleCombo(combo);
        combo.setSelectedItem("MANAGER");
        Component option = combo.getRenderer().getListCellRendererComponent(
                new javax.swing.JList<>(), "MANAGER", 1, false, false);
        assertThat(((JLabel) option).getText()).isEqualTo("Gestor");
        assertThat(combo.getSelectedItem()).isEqualTo("MANAGER");
    }

    @Test
    void legacyDialogFormUsesCanonicalFormField() {
        JTextField input = new JTextField();
        JPanel form = UIHelper.createDialogForm("Nome *:", input);
        FormField field = find(form, FormField.class);
        assertThat(field).isNotNull();
        assertThat(field.input()).isSameAs(input);
        assertThat(field.required()).isTrue();
    }

    @Test
    void businessPanelsHaveNoLocalColorsOrMixedLanguage() throws IOException {
        Path gui = Path.of("src", "main", "java", "com", "phcpro", "gui");
        try (var paths = Files.walk(gui)) {
            List<Path> files = paths.filter(p -> p.toString().endsWith(".java"))
                    .filter(p -> !p.toString().contains("components"))
                    .toList();
            for (Path file : files) {
                String source = Files.readString(file);
                assertThat(source.toLowerCase()).as(file.toString())
                        .doesNotContain("new color(", "atualizar", "cadastrar", "manager/admin");
            }
        }
    }

    @Test
    void densePlatformAndFiscalBarsUseMenus() throws IOException {
        Path gui = Path.of("src", "main", "java", "com", "phcpro", "gui");
        assertThat(Files.readString(gui.resolve("PlataformaPanel.java")))
                .contains("createActionMenuButton(\"Mais acções\")");
        assertThat(Files.readString(gui.resolve("FiscalPanel.java")))
                .contains("createActionMenuButton(\"Documentos\")");
    }

    private static <T> T find(Component root, Class<T> type) {
        if (type.isInstance(root)) return type.cast(root);
        if (root instanceof java.awt.Container container) {
            for (Component child : container.getComponents()) {
                T found = find(child, type);
                if (found != null) return found;
            }
        }
        return null;
    }
}
