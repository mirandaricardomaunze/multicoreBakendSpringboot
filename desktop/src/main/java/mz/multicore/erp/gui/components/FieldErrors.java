package mz.multicore.erp.gui.components;

import javax.swing.BorderFactory;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JLayeredPane;
import javax.swing.JRootPane;
import javax.swing.JSpinner;
import javax.swing.SwingUtilities;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.JTextComponent;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.Rectangle;

/**
 * Shows a validation message on the field it belongs to (docs/VALIDACAO_CAMPOS_SPEC.md).
 *
 * <p>Inside a {@link FormField} the message goes to its own error line. Any other field gets the red
 * border plus a caption floating just under it, so no form layout has to change. Editing the field
 * clears both.
 */
public final class FieldErrors {

    static final String CAPTION_KEY = "fieldErrors.caption";
    private static final String CLEAR_INSTALLED = "fieldErrors.clearInstalled";

    private FieldErrors() {
    }

    /** Marks the field of a {@link FieldValidationException}; false when the error is not about a field. */
    public static boolean showIfField(Throwable error) {
        for (Throwable t = error; t != null; t = t.getCause()) {
            if (t instanceof FieldValidationException fve) {
                show(fve.field(), fve.getMessage());
                return true;
            }
            if (t.getCause() == t) break;
        }
        return false;
    }

    public static void show(JComponent field, String message) {
        String text = message == null || message.isBlank() ? "Valor inválido." : message;
        JComponent target = editable(field);
        installClearOnEdit(target);
        FormField owner = (FormField) SwingUtilities.getAncestorOfClass(FormField.class, field);
        if (owner != null) {
            owner.setError(text);
        } else {
            UIHelper.markFieldInvalid(target, text);
            showCaption(target, text);
        }
        target.scrollRectToVisible(new Rectangle(target.getSize()));
        target.requestFocusInWindow();
    }

    public static void clear(JComponent field) {
        JComponent target = editable(field);
        FormField owner = (FormField) SwingUtilities.getAncestorOfClass(FormField.class, target);
        if (owner != null) {
            owner.clearError();
        } else if (target.getClientProperty("validationError") != null) {
            UIHelper.clearFieldInvalid(target);
        }
        removeCaption(target);
    }

    /** The visible caption under a field, or null. Package-private for the harness. */
    static JLabel caption(JComponent field) {
        return editable(field).getClientProperty(CAPTION_KEY) instanceof JLabel label ? label : null;
    }

    /** A spinner reports the error on its text editor, which is what the operator types into. */
    private static JComponent editable(JComponent field) {
        if (field instanceof JSpinner spinner && spinner.getEditor() instanceof JSpinner.DefaultEditor editor) {
            return editor.getTextField();
        }
        return field;
    }

    private static void showCaption(JComponent field, String text) {
        removeCaption(field);
        JRootPane root = SwingUtilities.getRootPane(field);
        if (root == null) return;
        JLabel caption = new JLabel(text);
        caption.setFont(Typography.CAPTION);
        caption.setForeground(UIHelper.REJECTED_RED);
        caption.setOpaque(true);
        caption.setBackground(UIHelper.BG_DARK);
        caption.setBorder(BorderFactory.createEmptyBorder(1, 4, 1, 4));
        caption.getAccessibleContext().setAccessibleName("Erro: " + text);
        JLayeredPane layers = root.getLayeredPane();
        Point at = SwingUtilities.convertPoint(field.getParent(), field.getX(), field.getY() + field.getHeight() + 2, layers);
        Dimension size = caption.getPreferredSize();
        int width = Math.min(size.width, Math.max(field.getWidth(), 260));
        int x = Math.max(0, Math.min(at.x, layers.getWidth() - width));
        caption.setBounds(x, at.y, width, size.height);
        layers.add(caption, JLayeredPane.POPUP_LAYER);
        layers.repaint(caption.getBounds());
        field.putClientProperty(CAPTION_KEY, caption);
    }

    private static void removeCaption(JComponent field) {
        if (field.getClientProperty(CAPTION_KEY) instanceof JLabel caption) {
            Component parent = caption.getParent();
            if (parent != null) {
                Rectangle bounds = caption.getBounds();
                ((java.awt.Container) parent).remove(caption);
                parent.repaint(bounds.x, bounds.y, bounds.width, bounds.height);
            }
            field.putClientProperty(CAPTION_KEY, null);
        }
    }

    private static void installClearOnEdit(JComponent field) {
        if (Boolean.TRUE.equals(field.getClientProperty(CLEAR_INSTALLED))) return;
        field.putClientProperty(CLEAR_INSTALLED, Boolean.TRUE);
        if (field instanceof JTextComponent text) {
            text.getDocument().addDocumentListener(new DocumentListener() {
                @Override public void insertUpdate(DocumentEvent e) { clear(field); }
                @Override public void removeUpdate(DocumentEvent e) { clear(field); }
                @Override public void changedUpdate(DocumentEvent e) { clear(field); }
            });
        } else if (field instanceof JComboBox<?> combo) {
            combo.addActionListener(e -> clear(field));
        }
    }
}
