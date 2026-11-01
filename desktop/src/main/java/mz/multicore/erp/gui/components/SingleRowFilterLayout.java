package mz.multicore.erp.gui.components;

import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JTextField;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Insets;
import java.util.ArrayList;
import java.util.List;

/** Filtros sempre numa linha: se não couberem, encolhem pesquisas e selectores em vez de sair da janela. */
final class SingleRowFilterLayout extends FlowLayout {

    private static final int MIN_FLEXIBLE_WIDTH = 90;

    SingleRowFilterLayout() {
        super(LEFT, 8, 0);
    }

    @Override
    public void layoutContainer(Container target) {
        synchronized (target.getTreeLock()) {
            List<Component> visible = new ArrayList<>();
            for (Component component : target.getComponents()) {
                if (component.isVisible()) visible.add(component);
            }
            int[] preferred = visible.stream().mapToInt(c -> c.getPreferredSize().width).toArray();
            boolean[] flexible = new boolean[preferred.length];
            boolean[] redundant = new boolean[preferred.length];
            for (int i = 0; i < flexible.length; i++) {
                flexible[i] = isFlexible(visible.get(i));
                redundant[i] = visible.get(i) instanceof JLabel && i + 1 < flexible.length
                        && visible.get(i + 1) instanceof JComboBox<?>;
            }

            Insets insets = target.getInsets();
            int[] widths = fit(preferred, flexible, redundant, target.getWidth() - insets.left - insets.right, getHgap());
            int rowHeight = target.getHeight() - (insets.top + insets.bottom);
            int x = insets.left;
            for (int i = 0; i < widths.length; i++) {
                Dimension size = visible.get(i).getPreferredSize();
                visible.get(i).setBounds(x, insets.top + Math.max(0, (rowHeight - size.height) / 2), widths[i], size.height);
                if (widths[i] > 0) x += widths[i] + getHgap();
            }
        }
    }

    /** As datas ficam de fora: cortadas, deixam de se conseguir ler. */
    private static boolean isFlexible(Component component) {
        return component instanceof JComboBox<?>
                || (component instanceof JTextField && !(component instanceof DateField));
    }

    /** Sem espaço, sai primeiro a etiqueta do selector: "Todos os artigos" já diz o que filtra. */
    static int[] fit(int[] preferred, boolean[] flexible, boolean[] redundant, int available, int gap) {
        int[] widths = preferred.clone();
        int overflow = gap * Math.max(0, widths.length - 1) - available;
        for (int width : widths) overflow += width;
        for (int i = 0; i < widths.length && overflow > 0; i++) {
            if (!redundant[i]) continue;
            overflow -= widths[i] + gap;
            widths[i] = 0;
        }
        while (overflow > 0) {
            int shrinkable = 0;
            for (int i = 0; i < widths.length; i++) {
                if (flexible[i] && widths[i] > MIN_FLEXIBLE_WIDTH) shrinkable++;
            }
            if (shrinkable == 0) break;
            int share = Math.max(1, overflow / shrinkable);
            for (int i = 0; i < widths.length && overflow > 0; i++) {
                if (!flexible[i] || widths[i] <= MIN_FLEXIBLE_WIDTH) continue;
                int cut = Math.min(Math.min(share, overflow), widths[i] - MIN_FLEXIBLE_WIDTH);
                widths[i] -= cut;
                overflow -= cut;
            }
        }
        return widths;
    }
}
