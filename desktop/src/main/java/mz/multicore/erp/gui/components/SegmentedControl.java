package mz.multicore.erp.gui.components;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JPanel;
import javax.swing.JToggleButton;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.util.ArrayList;
import java.util.List;
import java.util.function.IntConsumer;

/**
 * A row of mutually exclusive options shown together, the active one filled
 * (docs/ECRAS_FORA_DA_REGRA_SPEC.md): "Claro | Escuro | Alto contraste". Replaces buttons that cycle
 * through hidden states ("Tema: Claro (Clique p/ Alto Contraste)"), where the operator could not see the
 * choices nor pick one directly.
 */
public final class SegmentedControl extends JPanel {

    private final List<JToggleButton> options = new ArrayList<>();

    private SegmentedControl(String accessibleName, String[] labels, int selected, IntConsumer onSelect) {
        super(new FlowLayout(FlowLayout.LEFT, 0, 0));
        setOpaque(false);
        setBorder(BorderFactory.createEmptyBorder(Spacing.XXS / 2, Spacing.XXS / 2, Spacing.XXS / 2, Spacing.XXS / 2));
        getAccessibleContext().setAccessibleName(accessibleName);
        ButtonGroup group = new ButtonGroup();
        for (int i = 0; i < labels.length; i++) {
            JToggleButton option = new Segment(labels[i]);
            option.setSelected(i == selected);
            int index = i;
            option.addActionListener(e -> {
                if (index != selectedIndex()) return;
                onSelect.accept(index);
            });
            option.addItemListener(e -> repaint());
            group.add(option);
            options.add(option);
            add(option);
        }
    }

    public static SegmentedControl of(String accessibleName, String[] labels, int selected, IntConsumer onSelect) {
        return new SegmentedControl(accessibleName, labels, selected, onSelect);
    }

    public int selectedIndex() {
        for (int i = 0; i < options.size(); i++) if (options.get(i).isSelected()) return i;
        return -1;
    }

    List<JToggleButton> options() {
        return options;
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(UIHelper.FIELD_BG);
        g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, UIHelper.CONTROL_ARC, UIHelper.CONTROL_ARC);
        g2.setColor(ThemeColors.outline());
        g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, UIHelper.CONTROL_ARC, UIHelper.CONTROL_ARC);
        g2.dispose();
        super.paintComponent(g);
    }

    private static final class Segment extends JToggleButton {
        Segment(String label) {
            super(label);
            setFont(Typography.BODY_BOLD);
            setFocusPainted(false);
            setBorderPainted(false);
            setContentAreaFilled(false);
            setOpaque(false);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setBorder(BorderFactory.createEmptyBorder(0, Spacing.MD, 0, Spacing.MD));
        }

        @Override
        public Dimension getPreferredSize() {
            Dimension d = super.getPreferredSize();
            return new Dimension(d.width, UIHelper.FORM_CONTROL_HEIGHT - 2);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            if (isSelected()) {
                g2.setColor(UIHelper.ACCENT);
                g2.fillRoundRect(2, 2, getWidth() - 4, getHeight() - 4, UIHelper.CONTROL_ARC - 2, UIHelper.CONTROL_ARC - 2);
            } else if (getModel().isRollover()) {
                g2.setColor(ThemeColors.hoverOverlay());
                g2.fillRoundRect(2, 2, getWidth() - 4, getHeight() - 4, UIHelper.CONTROL_ARC - 2, UIHelper.CONTROL_ARC - 2);
            }
            g2.dispose();
            setForeground(isSelected() ? Color.WHITE : UIHelper.TEXT_LIGHT);
            super.paintComponent(g);
        }
    }
}
