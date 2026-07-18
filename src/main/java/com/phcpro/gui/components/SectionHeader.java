package com.phcpro.gui.components;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

/**
 * Cabeçalho de secção reutilizável — estilo PHC.
 * Título bold a 13 px + ícone opcional à esquerda + linha separadora subtil na parte inferior.
 * Substitui os {@code JLabel} soltos no topo de cada secção dos painéis.
 *
 * <p>Uso:</p>
 * <pre>
 *   panel.add(new SectionHeader("Faturas Emitidas", "fas-file-invoice"));
 * </pre>
 */
public class SectionHeader extends JPanel {

    /**
     * @param title    texto do cabeçalho
     * @param iconCode código Ikonli (ex.: {@code "fas-file-invoice"}); {@code null} para sem ícone
     */
    public SectionHeader(String title, String iconCode) {
        setLayout(new FlowLayout(FlowLayout.LEFT, 6, 0));
        setOpaque(false);
        setBorder(new EmptyBorder(0, 0, 6, 0));

        if (iconCode != null && !iconCode.isBlank()) {
            JLabel icon = new JLabel(UIHelper.icon(iconCode, 14, UIHelper.ACCENT));
            add(icon);
        }

        JLabel label = new JLabel(title);
        label.setFont(new Font(UIHelper.FONT, Font.BOLD, 13));
        label.setForeground(UIHelper.TEXT_LIGHT);
        add(label);
    }

    /** Sem ícone. */
    public SectionHeader(String title) {
        this(title, null);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        // Linha separadora subtil na base do componente
        Graphics2D g2 = (Graphics2D) g.create();
        try {
            g2.setColor(UIHelper.GRID);
            g2.fillRect(0, getHeight() - 1, getWidth(), 1);
        } finally {
            g2.dispose();
        }
    }
}
