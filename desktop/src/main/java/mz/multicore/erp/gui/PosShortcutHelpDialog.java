package mz.multicore.erp.gui;

import mz.multicore.erp.gui.components.ModernFormDialog;
import mz.multicore.erp.gui.components.ModernPanel;
import mz.multicore.erp.gui.components.UIHelper;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

/**
 * Diálogo de ajuda interativo que exibe a matriz completa de atalhos de teclado do POS.
 */
public final class PosShortcutHelpDialog {

    private PosShortcutHelpDialog() {}

    public static void show(Component parent) {
        JPanel root = new JPanel();
        root.setOpaque(false);
        root.setLayout(new BoxLayout(root, BoxLayout.Y_AXIS));
        root.setBorder(new EmptyBorder(10, 10, 10, 10));

        JPanel cardsPanel = new JPanel(new GridLayout(1, 3, 12, 12));
        cardsPanel.setOpaque(false);

        cardsPanel.add(buildCategoryCard("Artigos & Carrinho", "fas-boxes", UIHelper.ACCENT_BLUE, new String[][]{
                {"F2", "Pesquisar Artigo no Catálogo"},
                {"F3", "Focar Leitor Código Barras"},
                {"F5", "Aplicar Desconto de Linha"},
                {"F6", "Alterar Quantidade da Linha"},
                {"+ / -", "Aumentar / Diminuir Quantidade"},
                {"DELETE", "Remover Artigo Selecionado"}
        }));

        cardsPanel.add(buildCategoryCard("Caixa & Operações", "fas-cash-register", UIHelper.APPROVED_GREEN, new String[][]{
                {"F8", "Devoluções & Vales de Compras"},
                {"F9", "Movimentos de Caixa (Sangria/Suprimento)"},
                {"F10", "Finalizar Venda / Pagamento"},
                {"F12", "Fecho Cego de Caixa (Blind Drop)"}
        }));

        cardsPanel.add(buildCategoryCard("Geral & Navegação", "fas-desktop", UIHelper.ACCENT, new String[][]{
                {"F1", "Abrir este Guia de Atalhos"},
                {"F4", "Pesquisar / Selecionar Cliente"},
                {"F7", "Importar Cotação / Pró-forma no Carrinho"},
                {"F11", "Alternar Venda / Histórico"},
                {"ESC", "Cancelar / Limpar Carrinho"},
                {"CTRL+N", "Iniciar Nova Venda"}
        }));

        root.add(cardsPanel);

        new ModernFormDialog(UIHelper.mainWindow, "Atalhos Rápidos de Teclado (F1 a F12)",
                "fas-keyboard", "Guia operacional para alta velocidade no balcão de vendas", root)
                .setConfirmButton("Fechar", "fas-check")
                .showDialog();
    }

    private static JPanel buildCategoryCard(String title, String iconCode, Color accentColor, String[][] shortcuts) {
        ModernPanel card = new ModernPanel(14);
        card.setLayout(new BorderLayout(0, 10));
        card.setBackground(UIHelper.BG_CARD);
        card.setBorder(new EmptyBorder(12, 14, 14, 14));

        JPanel header = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        header.setOpaque(false);
        header.add(new JLabel(UIHelper.icon(iconCode, 15, accentColor)));
        JLabel titleLbl = new JLabel(title);
        titleLbl.setFont(new Font(UIHelper.FONT, Font.BOLD, 13));
        titleLbl.setForeground(UIHelper.TEXT_LIGHT);
        header.add(titleLbl);

        JPanel list = new JPanel(new GridLayout(shortcuts.length, 1, 0, 6));
        list.setOpaque(false);

        for (String[] row : shortcuts) {
            JPanel item = new JPanel(new BorderLayout(8, 0));
            item.setOpaque(false);

            JLabel badge = new JLabel(row[0], SwingConstants.CENTER);
            badge.setFont(new Font(Font.MONOSPACED, Font.BOLD, 11));
            badge.setForeground(Color.WHITE);
            badge.setBackground(UIHelper.ACCENT_BLUE);
            badge.setOpaque(true);
            badge.setPreferredSize(new Dimension(54, 22));
            badge.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(UIHelper.BORDER, 1),
                    BorderFactory.createEmptyBorder(2, 4, 2, 4)
            ));

            JLabel desc = new JLabel(row[1]);
            desc.setFont(new Font(UIHelper.FONT, Font.PLAIN, 12));
            desc.setForeground(UIHelper.TEXT_MUTED);

            item.add(badge, BorderLayout.WEST);
            item.add(desc, BorderLayout.CENTER);
            list.add(item);
        }

        card.add(header, BorderLayout.NORTH);
        card.add(list, BorderLayout.CENTER);
        return card;
    }
}
