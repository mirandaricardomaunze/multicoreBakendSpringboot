package mz.multicore.erp.gui.components;

import javax.swing.JPanel;
import java.awt.Component;
import java.awt.GridLayout;

/**
 * KPI strip for screens whose main content is a table: every KPI card added here is re-laid as a
 * compact tile, so the summary takes one short row and the table keeps the vertical space.
 */
final class KpiStrip extends JPanel {

    KpiStrip(int columns, int hgap, int vgap) {
        super(new GridLayout(0, columns, hgap, vgap));
        setOpaque(false);
    }

    @Override
    protected void addImpl(Component comp, Object constraints, int index) {
        if (comp instanceof ModernPanel card) KpiCompactLayout.apply(card);
        super.addImpl(comp, constraints, index);
    }
}
