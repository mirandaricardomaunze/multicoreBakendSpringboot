package mz.multicore.erp.gui.components;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Painel lateral ("drawer") de inspeção rápida e silenciosa (Quick Peek) para tabelas.
 * Permite visualizar os detalhes essenciais da linha selecionada sem abrir diálogos modais.
 */
public class QuickPeekPanel extends JPanel {

    public record PeekItem(String label, String value, boolean isHighlight) {}

    private final JLabel titleLabel;
    private final JLabel subtitleLabel;
    private final JLabel iconLabel;
    private final JPanel badgeContainer;
    private StatusBadge statusBadge;
    private final JPanel fieldsContainer;
    private final ModernButton openFullBtn;
    private final JButton closeBtn;
    private final JLabel hintLabel;

    private Consumer<Void> onOpenFullAction;
    private Runnable onCloseAction;

    public QuickPeekPanel() {
        setLayout(new BorderLayout());
        setPreferredSize(new Dimension(340, 0));
        setMinimumSize(new Dimension(280, 0));
        setOpaque(true);
        updateThemeColors();

        // 1. Cabeçalho
        JPanel header = new JPanel(new BorderLayout(8, 0));
        header.setOpaque(false);
        header.setBorder(new EmptyBorder(14, 16, 12, 16));

        JPanel titleBox = new JPanel(new BorderLayout(8, 2));
        titleBox.setOpaque(false);

        iconLabel = new JLabel(UIHelper.icon("fas-receipt", 18, UIHelper.ACCENT_BLUE));
        titleBox.add(iconLabel, BorderLayout.WEST);

        JPanel textStack = new JPanel();
        textStack.setLayout(new BoxLayout(textStack, BoxLayout.Y_AXIS));
        textStack.setOpaque(false);

        titleLabel = new JLabel("Detalhes do Registo");
        titleLabel.setFont(new Font(UIHelper.FONT, Font.BOLD, 13));
        titleLabel.setForeground(UIHelper.TEXT_LIGHT);
        textStack.add(titleLabel);

        subtitleLabel = new JLabel("Visualização rápida (Espaço)");
        subtitleLabel.setFont(new Font(UIHelper.FONT, Font.PLAIN, 11));
        subtitleLabel.setForeground(UIHelper.TEXT_MUTED);
        textStack.add(subtitleLabel);

        titleBox.add(textStack, BorderLayout.CENTER);
        header.add(titleBox, BorderLayout.CENTER);

        // Controlos à direita (Status Badge + Botão Fechar)
        JPanel headerRight = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        headerRight.setOpaque(false);

        badgeContainer = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        badgeContainer.setOpaque(false);
        badgeContainer.setVisible(false);
        headerRight.add(badgeContainer);

        closeBtn = new JButton(UIHelper.icon("fas-times", 12, UIHelper.TEXT_MUTED));
        closeBtn.setPreferredSize(new Dimension(26, 26));
        closeBtn.setFocusable(false);
        closeBtn.setBorderPainted(false);
        closeBtn.setContentAreaFilled(false);
        closeBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        closeBtn.setToolTipText("Fechar visualização rápida (ESC)");
        closeBtn.getAccessibleContext().setAccessibleName("Fechar visualização rápida");
        closeBtn.addActionListener(e -> {
            if (onCloseAction != null) {
                onCloseAction.run();
            } else {
                setVisible(false);
            }
        });
        headerRight.add(closeBtn);

        header.add(headerRight, BorderLayout.EAST);
        add(header, BorderLayout.NORTH);

        // 2. Corpo com Scroll e Grelha de Atributos
        fieldsContainer = new JPanel();
        fieldsContainer.setLayout(new BoxLayout(fieldsContainer, BoxLayout.Y_AXIS));
        fieldsContainer.setOpaque(false);
        fieldsContainer.setBorder(new EmptyBorder(4, 16, 12, 16));

        JScrollPane scrollPane = new JScrollPane(fieldsContainer);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.setOpaque(false);
        scrollPane.getViewport().setOpaque(false);
        scrollPane.getVerticalScrollBar().setUnitIncrement(14);
        scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        add(scrollPane, BorderLayout.CENTER);

        // 3. Rodapé com Ação Completa e Dica de Teclado
        JPanel footer = new JPanel(new BorderLayout(0, 6));
        footer.setOpaque(false);
        footer.setBorder(new EmptyBorder(10, 16, 14, 16));

        openFullBtn = UIHelper.createSecondaryButton("Ver Detalhes Completos");
        openFullBtn.setIcon(UIHelper.icon("fas-external-link-alt", 12, Color.WHITE));
        openFullBtn.addActionListener(e -> {
            if (onOpenFullAction != null) {
                onOpenFullAction.accept(null);
            }
        });
        footer.add(openFullBtn, BorderLayout.CENTER);

        hintLabel = new JLabel("Pressione Espaço ou ESC para alternar", SwingConstants.CENTER);
        hintLabel.setFont(new Font(UIHelper.FONT, Font.PLAIN, 10));
        hintLabel.setForeground(UIHelper.TEXT_MUTED);
        footer.add(hintLabel, BorderLayout.SOUTH);

        add(footer, BorderLayout.SOUTH);
    }

    public void updateThemeColors() {
        setBackground(UIHelper.BG_CARD);
        setBorder(BorderFactory.createMatteBorder(0, 1, 0, 0, UIHelper.BORDER));
    }

    public void setHeaderIcon(String iconCode, Color color) {
        iconLabel.setIcon(UIHelper.icon(iconCode, 18, color != null ? color : UIHelper.ACCENT_BLUE));
    }

    public void setTitle(String title) {
        titleLabel.setText(title != null && !title.isBlank() ? title : "Detalhes do Registo");
    }

    public void setSubtitle(String subtitle) {
        subtitleLabel.setText(subtitle != null ? subtitle : "");
    }

    public void setStatus(String statusText, Color color) {
        badgeContainer.removeAll();
        if (statusText != null && !statusText.isBlank()) {
            statusBadge = new StatusBadge(statusText, color != null ? color : UIHelper.ACCENT_BLUE);
            badgeContainer.add(statusBadge);
            badgeContainer.setVisible(true);
        } else {
            statusBadge = null;
            badgeContainer.setVisible(false);
        }
        badgeContainer.revalidate();
        badgeContainer.repaint();
    }

    public void setItems(List<PeekItem> items) {
        fieldsContainer.removeAll();
        if (items != null) {
            for (PeekItem item : items) {
                if (item.value() == null || item.value().isBlank()) continue;

                JPanel row = new JPanel(new BorderLayout(4, 2));
                row.setOpaque(false);
                row.setBorder(new EmptyBorder(6, 0, 6, 0));

                JLabel lbl = new JLabel(item.label());
                lbl.setFont(new Font(UIHelper.FONT, Font.PLAIN, 11));
                lbl.setForeground(UIHelper.TEXT_MUTED);
                row.add(lbl, BorderLayout.NORTH);

                JLabel val = new JLabel(item.value());
                if (item.isHighlight()) {
                    val.setFont(new Font(UIHelper.FONT, Font.BOLD, 15));
                    val.setForeground(UIHelper.ACCENT_BLUE);
                } else {
                    val.setFont(new Font(UIHelper.FONT, Font.BOLD, 12));
                    val.setForeground(UIHelper.TEXT_LIGHT);
                }
                row.add(val, BorderLayout.CENTER);

                fieldsContainer.add(row);
                fieldsContainer.add(new JSeparator(SwingConstants.HORIZONTAL));
            }
        }
        fieldsContainer.revalidate();
        fieldsContainer.repaint();
    }

    public void setOnOpenFullAction(Consumer<Void> action) {
        this.onOpenFullAction = action;
        openFullBtn.setVisible(action != null);
    }

    public void setOnCloseAction(Runnable action) {
        this.onCloseAction = action;
    }

    public JLabel getTitleLabel() {
        return titleLabel;
    }

    public StatusBadge getStatusBadge() {
        return statusBadge;
    }

    public ModernButton getOpenFullButton() {
        return openFullBtn;
    }

    public JButton getCloseButton() {
        return closeBtn;
    }
}
