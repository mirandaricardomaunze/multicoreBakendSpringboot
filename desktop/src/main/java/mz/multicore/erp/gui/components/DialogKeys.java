package mz.multicore.erp.gui.components;

import javax.swing.AbstractButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JRootPane;
import javax.swing.KeyStroke;
import java.awt.AWTEvent;
import java.awt.Component;
import java.awt.Container;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.List;

/**
 * The same keys in every dialog (docs/TABELAS_CONSISTENCIA_SPEC.md, rule 11): Enter presses the one green button
 * ({@link ButtonStyles#main}) and Esc closes the dialog as if its window "X" were clicked. A dialog that already
 * set its own default button or Esc keeps it. Installed once for the whole application.
 */
public final class DialogKeys {

    /** Put on a main button that must never fire on Enter (e.g. accepting a licence). */
    public static final String NO_ENTER = "dialogKeys.noEnter";

    private static final KeyStroke ESCAPE = KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0);
    private static final String CLOSE_ACTION = "dialogKeys.close";
    private static boolean installed;

    private DialogKeys() {}

    /** Applies the keys to every dialog when it opens. */
    public static synchronized void install() {
        if (installed) return;
        installed = true;
        Toolkit.getDefaultToolkit().addAWTEventListener(event -> {
            if (event.getID() == WindowEvent.WINDOW_OPENED && event.getSource() instanceof JDialog dialog) {
                apply(dialog);
            }
        }, AWTEvent.WINDOW_EVENT_MASK);
    }

    static void apply(JDialog dialog) {
        JRootPane root = dialog.getRootPane();
        if (root.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).get(ESCAPE) == null) {
            root.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(ESCAPE, CLOSE_ACTION);
            root.getActionMap().put(CLOSE_ACTION, new javax.swing.AbstractAction() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    dialog.dispatchEvent(new WindowEvent(dialog, WindowEvent.WINDOW_CLOSING));
                }
            });
        }
        if (root.getDefaultButton() == null) {
            List<AbstractButton> mains = new ArrayList<>();
            collectMains(dialog.getContentPane(), mains);
            if (mains.size() == 1 && mains.get(0) instanceof javax.swing.JButton button) {
                root.setDefaultButton(button);
            }
        }
    }

    private static void collectMains(Container parent, List<AbstractButton> out) {
        for (Component child : parent.getComponents()) {
            if (child instanceof ModernButton button && ButtonStyles.isMain(button) && button.isVisible()
                    && button.getClientProperty(NO_ENTER) == null) {
                out.add(button);
            }
            if (child instanceof Container container) collectMains(container, out);
        }
    }
}
