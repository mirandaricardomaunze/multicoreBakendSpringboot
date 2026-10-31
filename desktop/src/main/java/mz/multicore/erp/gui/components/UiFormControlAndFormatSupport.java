package mz.multicore.erp.gui.components;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.text.JTextComponent;
import java.awt.*;
import java.awt.event.ActionListener;
import java.math.BigDecimal;
import java.util.function.Function;

/**
 * Fachada estável dos helpers de formulário usados por {@link UIHelper}.
 * A composição visual vive em {@link UiFormComponentStyler} e a recolha/filtragem
 * de texto em {@link UiTextInputSupport}.
 */
final class UiFormControlAndFormatSupport {
    private UiFormControlAndFormatSupport() {}

    static ModernButton createButton(String text, Icon icon, Color baseColor, ActionListener listener) {
        return UiFormComponentStyler.createButton(text, icon, baseColor, listener);
    }
    static ModernButton createRefreshButton(Runnable action) { return UiFormComponentStyler.createRefreshButton(action); }
    static ModernButton createExportButton(JTable table, String name) { return UiFormComponentStyler.createExportButton(table, name); }
    static JPanel tableCardTop(String title, JComponent filters, JComponent... actions) {
        return UiFormComponentStyler.tableCardTop(title, filters, actions);
    }
    static ModernButton createIconButton(String name, String icon) { return UiFormComponentStyler.createIconButton(name, icon); }
    static JButton createIconButton(String code, int size, String tooltip, Runnable action) {
        return UiFormComponentStyler.createIconButton(code, size, tooltip, action);
    }
    static ModernButton createAddLineButton() { return UiFormComponentStyler.createAddLineButton(); }
    static ModernButton createProductLookupButton(Runnable action) { return UiFormComponentStyler.createProductLookupButton(action); }
    static JPanel createFilterGroup(String label, JComponent component) { return UiFormComponentStyler.createFilterGroup(label, component); }
    static JComponent makeDialogScrollable(JPanel content, int maxHeight) { return UiFormComponentStyler.makeDialogScrollable(content, maxHeight); }
    static void styleTextField(JTextField field) { UiFormComponentStyler.styleTextField(field); }
    static void markFieldInvalid(JComponent component, String message) { UiFormComponentStyler.markFieldInvalid(component, message); }
    static void clearFieldInvalid(JComponent component) { UiFormComponentStyler.clearFieldInvalid(component); }
    static void setReadOnly(JTextComponent component, boolean readOnly) { UiFormComponentStyler.setReadOnly(component, readOnly); }
    static Border fieldBorder(Color line, int thickness) { return UiFormComponentStyler.fieldBorder(line, thickness); }
    static void onTextChange(JTextField field, Runnable action) { UiFormComponentStyler.onTextChange(field, action); }
    static String promptRequiredText(String title, String icon, String subtitle, String label) {
        return UiTextInputSupport.promptRequiredText(title, icon, subtitle, label);
    }
    static BigDecimal promptAmount(String title, String icon, String subtitle, String label, BigDecimal min) {
        return UiTextInputSupport.promptAmount(title, icon, subtitle, label, min);
    }
    static String humanRole(String role) { return UiFormComponentStyler.humanRole(role); }
    static void humanizeRoleCombo(JComboBox<String> combo) { UiFormComponentStyler.humanizeRoleCombo(combo); }
    static void stylePasswordField(JPasswordField field) { UiFormComponentStyler.stylePasswordField(field); }
    static void styleTextArea(JTextArea area) { UiFormComponentStyler.styleTextArea(area); }
    static <T> ListCellRenderer<T> labelRenderer(Function<T, String> label) { return UiFormComponentStyler.labelRenderer(label); }
    static void styleComboBox(JComboBox<?> combo) { UiFormComponentStyler.styleComboBox(combo); }
    static <T> JComboBox<T> createComboBox(T[] items, Function<T, String> label) { return UiFormComponentStyler.createComboBox(items, label); }
    static void styleSpinner(JSpinner spinner) { UiFormComponentStyler.styleSpinner(spinner); }
    static JLabel createWarningBanner(String message) { return UiFormComponentStyler.createWarningBanner(message); }
    static JSpinner createYearSpinner(int year) { return UiFormComponentStyler.createYearSpinner(year); }
    static String humanStatus(String status) { return UiFormComponentStyler.humanStatus(status); }
    static JPanel createDialogForm(int columns, Object... values) { return UiFormComponentStyler.createDialogForm(columns, values); }
    static void installDigitsOnlyFilter(JTextComponent component, int maxLength) {
        UiTextInputSupport.installDigitsOnlyFilter(component, maxLength);
    }
    static void installUppercaseFilter(JTextComponent component, int maxLength) {
        UiTextInputSupport.installUppercaseFilter(component, maxLength);
    }
}
