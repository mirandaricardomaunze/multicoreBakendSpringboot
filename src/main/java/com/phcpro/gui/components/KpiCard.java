package com.phcpro.gui.components;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;

/**
 * Cartão de KPI profissional (gradiente + ícone), partilhado entre os painéis de visão geral
 * (Dashboard, RH, …). Cabeçalho com título e ícone, valor em destaque ao centro e uma linha de
 * detalhe opcional em baixo. Uma fonte de verdade para o aspecto dos KPIs.
 */
public final class KpiCard {

    private KpiCard() {
    }

    /** Label de valor em destaque (branco, negrito) com o tamanho indicado. */
    public static JLabel valueLabel(String text, int size) {
        JLabel l = new JLabel(text);
        l.setFont(new Font("Segoe UI", Font.BOLD, size));
        l.setForeground(Color.WHITE);
        return l;
    }

    public static ModernPanel create(String title, String iconCode, Color titleColor,
                                     JLabel valueLabel, JLabel subLabel,
                                     Color gradientStart, Color gradientEnd) {
        ModernPanel card = new ModernPanel(12, gradientStart, gradientEnd);
        card.setLayout(new BorderLayout(6, 6));
        card.setBorder(new EmptyBorder(12, 14, 12, 14));

        JPanel titleRow = new JPanel(new BorderLayout(6, 0));
        titleRow.setOpaque(false);
        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 11));
        titleLabel.setForeground(titleColor);
        titleRow.add(titleLabel, BorderLayout.CENTER);
        titleRow.add(new JLabel(UIHelper.icon(iconCode, 18, titleColor)), BorderLayout.EAST);

        card.add(titleRow, BorderLayout.NORTH);
        card.add(valueLabel, BorderLayout.CENTER);
        if (subLabel != null) {
            card.add(subLabel, BorderLayout.SOUTH);
        }
        return card;
    }
}
