package mz.multicore.erp.gui.components;

import javax.swing.BoundedRangeModel;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.SwingConstants;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/** Seta discreta que avisa que há mais módulos abaixo. Ocupa sempre o mesmo espaço, para o menu não saltar. */
final class SidebarScrollHint extends JLabel {

    private static final int STEP = 120;

    SidebarScrollHint(JScrollPane scroll) {
        BoundedRangeModel bar = scroll.getVerticalScrollBar().getModel();
        setHorizontalAlignment(SwingConstants.CENTER);
        setPreferredSize(new Dimension(0, 16));
        setToolTipText("Mais módulos abaixo");
        bar.addChangeListener(e -> refresh(bar));
        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                bar.setValue(bar.getValue() + STEP);
            }
        });
        refresh(bar);
    }

    static boolean hasMoreBelow(int value, int extent, int maximum) {
        return value + extent < maximum;
    }

    private void refresh(BoundedRangeModel bar) {
        boolean more = hasMoreBelow(bar.getValue(), bar.getExtent(), bar.getMaximum());
        setIcon(more ? UIHelper.icon("fas-chevron-down", 10, UIHelper.ACCENT_BLUE) : null);
        setCursor(Cursor.getPredefinedCursor(more ? Cursor.HAND_CURSOR : Cursor.DEFAULT_CURSOR));
    }
}
