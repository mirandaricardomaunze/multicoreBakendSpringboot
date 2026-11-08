package mz.multicore.erp.gui.sector;

import mz.multicore.erp.gui.components.UIHelper;

import javax.swing.*;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.math.BigDecimal;

/**
 * Totals summary of a draft document, as large ERPs show it: lines, subtotal before VAT, VAT and the total in
 * evidence. Replaces the single "Total Rascunho: … (incl. IVA)" line, which mixed everything in one sentence.
 */
public final class DraftTotalsBar extends JPanel {

    /** Snapshot of a draft: non-empty lines and the amounts already computed by the editor. */
    public record Totals(int lines, BigDecimal subtotal, BigDecimal tax, BigDecimal total) {}

    private final JLabel lines = value(false);
    private final JLabel subtotal = value(false);
    private final JLabel tax = value(false);
    private final JLabel total = value(true);

    public DraftTotalsBar() {
        super(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        setOpaque(false);
        add(cell("Linhas", lines, false));
        add(separator());
        add(cell("Subtotal (sem IVA)", subtotal, false));
        add(separator());
        add(cell("IVA", tax, false));
        add(separator());
        add(cell("Total do rascunho", total, true));
        getAccessibleContext().setAccessibleName("Totais do documento em rascunho");
        show(new Totals(0, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO));
    }

    public void show(Totals totals) {
        lines.setText(String.valueOf(totals.lines()));
        subtotal.setText(UIHelper.formatMzn(nz(totals.subtotal())));
        tax.setText(UIHelper.formatMzn(nz(totals.tax())));
        total.setText(UIHelper.formatMzn(nz(totals.total())));
    }

    String totalText() {
        return total.getText();
    }

    private static JComponent cell(String caption, JLabel value, boolean emphasis) {
        JPanel cell = new JPanel(new BorderLayout(0, 1));
        cell.setOpaque(emphasis);
        if (emphasis) {
            cell.setBackground(UIHelper.withAlpha(UIHelper.APPROVED_GREEN, 28));
            cell.setBorder(new CompoundBorder(UIHelper.fieldBorder(UIHelper.APPROVED_GREEN, 1), new EmptyBorder(3, 12, 3, 12)));
        } else {
            cell.setBorder(new EmptyBorder(3, 12, 3, 12));
        }
        JLabel label = new JLabel(caption);
        label.setFont(new Font(UIHelper.FONT, Font.PLAIN, 11));
        label.setForeground(UIHelper.TEXT_MUTED);
        label.setHorizontalAlignment(SwingConstants.RIGHT);
        cell.add(label, BorderLayout.NORTH);
        cell.add(value, BorderLayout.CENTER);
        return cell;
    }

    private static JLabel value(boolean emphasis) {
        JLabel label = new JLabel("—");
        label.setFont(new Font(UIHelper.FONT, Font.BOLD, emphasis ? 17 : 13));
        label.setForeground(emphasis ? UIHelper.APPROVED_GREEN : UIHelper.TEXT_LIGHT);
        label.setHorizontalAlignment(SwingConstants.RIGHT);
        return label;
    }

    private static JComponent separator() {
        JSeparator separator = new JSeparator(SwingConstants.VERTICAL);
        separator.setPreferredSize(new Dimension(1, 30));
        separator.setForeground(UIHelper.BORDER);
        return separator;
    }

    private static BigDecimal nz(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}
