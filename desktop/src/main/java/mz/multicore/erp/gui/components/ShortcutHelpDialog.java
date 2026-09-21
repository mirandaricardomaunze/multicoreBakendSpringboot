package mz.multicore.erp.gui.components;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.Window;
import java.awt.event.KeyEvent;
import java.util.List;

/**
 * Diálogo visual que exibe o guia de atalhos rápidos do sistema e do POS.
 * Acessível via tecla F1 ou menu de ajuda.
 */
public class ShortcutHelpDialog extends JDialog {

    public record ShortcutItem(String key, String description) {}

    public ShortcutHelpDialog(Window owner) {
        super(owner, "Atalhos de Teclado (F1)", ModalityType.APPLICATION_MODAL);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setSize(700, 480);
        setMinimumSize(new Dimension(580, 400));
        setLocationRelativeTo(owner);
        getContentPane().setBackground(UIHelper.BG_DARK);

        JPanel root = new JPanel(new BorderLayout(0, 16));
        root.setBackground(UIHelper.BG_DARK);
        root.setBorder(BorderFactory.createEmptyBorder(20, 24, 20, 24));

        // Header
        root.add(UIHelper.buildPremiumHeader(
                "fas-keyboard",
                "Guia de Atalhos de Teclado",
                "Opere o Multicore ERP e o POS em alta velocidade apenas com o teclado."
        ), BorderLayout.NORTH);

        // Content: 2 Cards (Sistema & Navegação | POS & Balcão)
        JPanel cardsPanel = new JPanel(new GridLayout(1, 2, 16, 0));
        cardsPanel.setOpaque(false);

        List<ShortcutItem> generalShortcuts = List.of(
                new ShortcutItem("Ctrl + K", "Pesquisa Global / Paleta de Comandos"),
                new ShortcutItem("Ctrl + B", "Alternar Menu Lateral (Recolher/Expandir)"),
                new ShortcutItem("F1", "Abrir este Guia de Atalhos"),
                new ShortcutItem("F11", "Alternar Modo Ecrã Completo (Fullscreen)"),
                new ShortcutItem("Esc", "Fechar Janelas, Diálogos ou Menus")
        );

        List<ShortcutItem> posShortcuts = List.of(
                new ShortcutItem("F2", "Pesquisar Artigo no Catálogo"),
                new ShortcutItem("F3", "Focar Leitor de Código de Barras"),
                new ShortcutItem("F4", "Selecionar ou Trocar Cliente"),
                new ShortcutItem("F6", "Alterar Quantidade da Linha Selecionada"),
                new ShortcutItem("F9", "Finalizar Venda e Abrir Pagamento"),
                new ShortcutItem("Delete", "Remover Artigo Selecionado do Carrinho")
        );

        cardsPanel.add(buildCategoryCard("Sistema & Navegação", "fas-compass", UIHelper.ACCENT_BLUE, generalShortcuts));
        cardsPanel.add(buildCategoryCard("Ponto de Venda (POS)", "fas-cash-register", UIHelper.APPROVED_GREEN, posShortcuts));

        JScrollPane scrollPane = new JScrollPane(cardsPanel);
        scrollPane.setOpaque(false);
        scrollPane.getViewport().setOpaque(false);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        root.add(scrollPane, BorderLayout.CENTER);

        // Footer Actions
        JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        footer.setOpaque(false);
        ModernButton closeBtn = UIHelper.createSecondaryButton("Fechar (ESC)");
        closeBtn.setIcon(UIHelper.icon("fas-times", 14));
        closeBtn.addActionListener(e -> dispose());
        footer.add(closeBtn);
        root.add(footer, BorderLayout.SOUTH);

        add(root);

        // Global ESC
        getRootPane().registerKeyboardAction(
                e -> dispose(),
                KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
                JComponent.WHEN_IN_FOCUSED_WINDOW
        );
    }

    public static void show(Window owner) {
        SwingUtilities.invokeLater(() -> {
            ShortcutHelpDialog dialog = new ShortcutHelpDialog(owner);
            dialog.setVisible(true);
        });
    }

    private JPanel buildCategoryCard(String title, String iconCode, Color accent, List<ShortcutItem> shortcuts) {
        ModernPanel card = new ModernPanel(12, UIHelper.BG_CARD, UIHelper.BG_CARD);
        card.setLayout(new BorderLayout(0, 12));
        card.setBorder(BorderFactory.createEmptyBorder(14, 16, 14, 16));

        // Card Header
        JPanel cardHeader = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        cardHeader.setOpaque(false);
        JLabel iconLabel = new JLabel(UIHelper.icon(iconCode, 16, accent));
        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(new Font(UIHelper.FONT, Font.BOLD, 14));
        titleLabel.setForeground(UIHelper.TEXT_LIGHT);
        cardHeader.add(iconLabel);
        cardHeader.add(titleLabel);
        card.add(cardHeader, BorderLayout.NORTH);

        // List of shortcuts
        JPanel listPanel = new JPanel();
        listPanel.setOpaque(false);
        listPanel.setLayout(new BoxLayout(listPanel, BoxLayout.Y_AXIS));

        for (ShortcutItem s : shortcuts) {
            JPanel row = new JPanel(new BorderLayout(8, 0));
            row.setOpaque(false);
            row.setBorder(BorderFactory.createEmptyBorder(5, 2, 5, 2));

            // Key pill
            JLabel keyLabel = new JLabel(s.key());
            keyLabel.setFont(new Font(UIHelper.FONT, Font.BOLD, 11));
            keyLabel.setForeground(UIHelper.TEXT_LIGHT);
            keyLabel.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(UIHelper.isLight() ? new Color(203, 213, 225) : new Color(71, 85, 105), 1),
                    BorderFactory.createEmptyBorder(2, 6, 2, 6)
            ));

            // Description
            JLabel descLabel = new JLabel(s.description());
            descLabel.setFont(new Font(UIHelper.FONT, Font.PLAIN, 12));
            descLabel.setForeground(UIHelper.TEXT_MUTED);

            row.add(keyLabel, BorderLayout.WEST);
            row.add(descLabel, BorderLayout.CENTER);

            listPanel.add(row);
            listPanel.add(Box.createVerticalStrut(3));
        }

        card.add(listPanel, BorderLayout.CENTER);
        return card;
    }
}
