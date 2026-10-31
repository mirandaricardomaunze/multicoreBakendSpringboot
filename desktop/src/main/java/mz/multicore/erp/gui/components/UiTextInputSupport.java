package mz.multicore.erp.gui.components;

import javax.swing.*;
import javax.swing.text.AbstractDocument;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DocumentFilter;
import javax.swing.text.JTextComponent;
import java.math.BigDecimal;
import java.util.function.UnaryOperator;

/** Recolha de texto/montantes em modal canónico e filtros de introdução (dígitos, maiúsculas). */
final class UiTextInputSupport {

    private UiTextInputSupport() {}

    static String promptRequiredText(String title, String iconCode, String subtitle, String label) {
        JTextArea field = new JTextArea(3, 24);
        field.setLineWrap(true);
        field.setWrapStyleWord(true);
        JPanel form = UiFormControlAndFormatSupport.createDialogForm(2, label, field);
        String[] holder = new String[1];
        ModernFormDialog dlg = new ModernFormDialog(UIHelper.mainWindow, title, iconCode, subtitle, form)
                .setConfirmButton("Confirmar", "fas-check");
        dlg.setOnSave(() -> {
            String v = field.getText().trim();
            if (v.isEmpty()) throw new IllegalArgumentException("Este campo é obrigatório.");
            holder[0] = v;
        });
        return dlg.showDialog() ? holder[0] : null;
    }

    static BigDecimal promptAmount(String title, String iconCode, String subtitle, String label, BigDecimal min) {
        JTextField field = new JTextField();
        JPanel form = UiFormControlAndFormatSupport.createDialogForm(2, label, field);
        BigDecimal[] holder = new BigDecimal[1];
        ModernFormDialog dlg = new ModernFormDialog(UIHelper.mainWindow, title, iconCode, subtitle, form)
                .setConfirmButton("Confirmar", "fas-check");
        dlg.setOnSave(() -> {
            BigDecimal v;
            try {
                v = new BigDecimal(field.getText().trim().replace(',', '.'));
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Introduza um valor numérico válido.");
            }
            if (min != null && v.compareTo(min) < 0) {
                throw new IllegalArgumentException("O valor deve ser maior ou igual a " + min + ".");
            }
            holder[0] = v;
        });
        return dlg.showDialog() ? holder[0] : null;
    }

    static void installDigitsOnlyFilter(JTextComponent comp, int maxLength) {
        installTransformingFilter(comp, maxLength, s -> s.replaceAll("[^0-9]", ""));
    }

    static void installUppercaseFilter(JTextComponent comp, int maxLength) {
        installTransformingFilter(comp, maxLength, String::toUpperCase);
    }

    /** Aplica {@code transform} ao texto introduzido e trunca ao limite {@code maxLength} (0 = sem limite). */
    private static void installTransformingFilter(JTextComponent comp, int maxLength, UnaryOperator<String> transform) {
        if (comp == null || !(comp.getDocument() instanceof AbstractDocument doc)) return;
        doc.setDocumentFilter(new DocumentFilter() {
            @Override
            public void insertString(FilterBypass fb, int offset, String string, AttributeSet attr) throws BadLocationException {
                if (string == null) return;
                super.insertString(fb, offset, clip(transform.apply(string), fb.getDocument().getLength()), attr);
            }

            @Override
            public void replace(FilterBypass fb, int offset, int length, String text, AttributeSet attrs) throws BadLocationException {
                if (text == null) return;
                super.replace(fb, offset, length, clip(transform.apply(text), fb.getDocument().getLength() - length), attrs);
            }

            private String clip(String value, int currentLen) {
                if (maxLength > 0 && currentLen + value.length() > maxLength) {
                    return value.substring(0, Math.max(0, maxLength - currentLen));
                }
                return value;
            }
        });
    }
}
