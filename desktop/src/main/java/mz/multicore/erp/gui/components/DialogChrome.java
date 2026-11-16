package mz.multicore.erp.gui.components;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;
import java.awt.BorderLayout;
import java.awt.FlowLayout;

/**
 * The one frame of every dialog (docs/TABELAS_CONSISTENCIA_SPEC.md, rule 14), the same one {@link ModernFormDialog}
 * uses: the shared header (icon, title, subtitle, divider), {@code Spacing.XL} margin, {@code Spacing.LG} between
 * parts, and a footer with its buttons on the right under a divider. A dialog only supplies its body and buttons.
 */
public final class DialogChrome {

    /** Marks a footer built here, so the harness can find dialogs that still build their own. */
    static final String FOOTER = "dialogChrome.footer";

    private DialogChrome() {}

    /** Header, body and footer inside the standard margin; the footer may be null. */
    public static JPanel frame(String iconCode, String title, String subtitle, JComponent body, JComponent footer) {
        JPanel root = new JPanel(new BorderLayout(0, Spacing.LG));
        root.setBackground(UIHelper.BG_DARK);
        root.setBorder(new EmptyBorder(Spacing.XL, Spacing.XL, Spacing.XL, Spacing.XL));
        root.add(UIHelper.buildPremiumHeader(iconCode, title, subtitle), BorderLayout.NORTH);
        if (body != null) root.add(body, BorderLayout.CENTER);
        if (footer != null) root.add(footer, BorderLayout.SOUTH);
        return root;
    }

    /** Buttons on the right, under a divider; the main (green) button goes last. */
    public static JPanel footer(JComponent... buttons) {
        return footerWithNote(null, buttons);
    }

    /** As {@link #footer(JComponent...)}, with a note, check box or status on the left (may be null). */
    public static JPanel footerWithNote(JComponent left, JComponent... buttons) {
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, Spacing.SM, 0));
        actions.setOpaque(false);
        for (JComponent button : buttons) {
            if (button != null) actions.add(button);
        }
        JPanel footer = new JPanel(new BorderLayout(Spacing.LG, 0));
        footer.setOpaque(false);
        footer.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, UIHelper.GRID),
                new EmptyBorder(Spacing.LG, 0, 0, 0)));
        if (left != null) footer.add(left, BorderLayout.CENTER);
        footer.add(actions, BorderLayout.EAST);
        footer.putClientProperty(FOOTER, Boolean.TRUE);
        return footer;
    }
}
