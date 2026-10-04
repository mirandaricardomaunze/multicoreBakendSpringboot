package mz.multicore.erp.gui.components;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Componente canónico e reutilizável de apresentação de fichas de detalhe executivas no Multicore ERP.
 * Oferece uma estrutura universal com:
 * 1. Cabeçalho rico com Avatar/Ícone, Títulos e Badge Pill de Estado
 * 2. Faixa horizontal de KPIs com cartões modernos (ModernPanel)
 * 3. Seção central em abas com ícones temáticos vibrantes
 * 4. Rodapé flexível de ações (Imprimir, Editar, Exportar, Fechar)
 */
public class ExecutiveDetailDialog {

    public enum StatusSeverity {
        SUCCESS(UIHelper.APPROVED_GREEN, Color.WHITE),
        WARNING(UIHelper.PENDING_YELLOW, Color.BLACK),
        DANGER(UIHelper.REJECTED_RED, Color.WHITE),
        INFO(UIHelper.ACCENT_BLUE, Color.WHITE),
        NEUTRAL(UIHelper.BG_CARD, UIHelper.TEXT_MUTED);

        public final Color bg;
        public final Color fg;

        StatusSeverity(Color bg, Color fg) {
            this.bg = bg;
            this.fg = fg;
        }
    }

    private final Window parent;
    private final JDialog dialog;

    // Cabeçalho
    private JLabel headerIconLabel;
    private JLabel titleLabel;
    private JLabel subtitleLabel;
    private JLabel statusBadgeLabel;

    // KPIs
    private final List<JComponent> kpiCards = new ArrayList<>();
    private JPanel kpiRowPanel;

    // Abas
    private final JTabbedPane tabbedPane;

    // Rodapé
    private final JPanel leftActionsPanel;
    private final JPanel rightActionsPanel;

    public ExecutiveDetailDialog(Window parent, String dialogTitle) {
        this.parent = parent;
        this.dialog = GraphicsEnvironment.isHeadless() ? null : new JDialog(parent, dialogTitle, Dialog.ModalityType.APPLICATION_MODAL);
        this.tabbedPane = new JTabbedPane();
        this.leftActionsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        this.rightActionsPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));

        initUi();
    }

    public static ExecutiveDetailDialog create(Component owner, String title) {
        Window win = owner instanceof Window ? (Window) owner : (owner != null ? SwingUtilities.getWindowAncestor(owner) : null);
        return new ExecutiveDetailDialog(win, title);
    }

    private void initUi() {
        if (dialog != null) {
            dialog.setSize(940, 660);
            dialog.setMinimumSize(new Dimension(800, 540));
            dialog.getContentPane().setBackground(UIHelper.BG_DARK);
        }

        JPanel root = new JPanel(new BorderLayout(0, 14));
        root.setBackground(UIHelper.BG_DARK);
        root.setBorder(new EmptyBorder(18, 20, 16, 20));

        // 1. Top Section: Header + KPIs
        JPanel topContainer = new JPanel(new BorderLayout(0, 14));
        topContainer.setOpaque(false);
        topContainer.add(buildHeader(), BorderLayout.NORTH);

        kpiRowPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
        kpiRowPanel.setOpaque(false);
        topContainer.add(kpiRowPanel, BorderLayout.CENTER);

        root.add(topContainer, BorderLayout.NORTH);

        // 2. Center: Abas
        UIHelper.styleTabbedPaneMulticore(tabbedPane);
        root.add(tabbedPane, BorderLayout.CENTER);

        // 3. Bottom: Rodapé de Ações
        root.add(buildFooter(), BorderLayout.SOUTH);

        if (dialog != null) {
            dialog.setContentPane(root);
        }
    }

    private JPanel buildHeader() {
        JPanel header = new JPanel(new BorderLayout(14, 0));
        header.setOpaque(false);

        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
        left.setOpaque(false);

        headerIconLabel = new JLabel();
        left.add(headerIconLabel);

        JPanel titleBlock = new JPanel(new GridLayout(2, 1, 0, 2));
        titleBlock.setOpaque(false);

        titleLabel = new JLabel("—");
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 18));
        titleLabel.setForeground(UIHelper.TEXT_LIGHT);

        subtitleLabel = new JLabel("—");
        subtitleLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        subtitleLabel.setForeground(UIHelper.TEXT_MUTED);

        titleBlock.add(titleLabel);
        titleBlock.add(subtitleLabel);
        left.add(titleBlock);

        statusBadgeLabel = new JLabel("—", SwingConstants.CENTER);
        statusBadgeLabel.setOpaque(true);
        statusBadgeLabel.setFont(new Font("Segoe UI", Font.BOLD, 12));
        statusBadgeLabel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UIHelper.BORDER, 1),
                new EmptyBorder(6, 16, 6, 16)
        ));

        header.add(left, BorderLayout.WEST);
        header.add(statusBadgeLabel, BorderLayout.EAST);
        return header;
    }

    private JPanel buildFooter() {
        JPanel footer = new JPanel(new BorderLayout());
        footer.setOpaque(false);

        leftActionsPanel.setOpaque(false);
        rightActionsPanel.setOpaque(false);

        // Botão padrão de fechar se nenhum outro botão for definido à direita
        ModernButton defaultCloseBtn = UIHelper.createSecondaryButton("Fechar");
        defaultCloseBtn.setIcon(UIHelper.icon("fas-times", 14));
        defaultCloseBtn.addActionListener(e -> dispose());
        rightActionsPanel.add(defaultCloseBtn);

        footer.add(leftActionsPanel, BorderLayout.WEST);
        footer.add(rightActionsPanel, BorderLayout.EAST);
        return footer;
    }

    // ─── Fluent API ─────────────────────────────────────────────────────────────

    public ExecutiveDetailDialog setTitle(String title) {
        titleLabel.setText(title);
        if (dialog != null) {
            dialog.setTitle(title);
        }
        return this;
    }

    public ExecutiveDetailDialog setSubtitle(String subtitle) {
        subtitleLabel.setText(subtitle != null && !subtitle.isBlank() ? subtitle : " ");
        return this;
    }

    public ExecutiveDetailDialog setHeaderIcon(String fontAwesome, int size, Color color) {
        headerIconLabel.setIcon(UIHelper.icon(fontAwesome, size, color));
        return this;
    }

    public ExecutiveDetailDialog setHeaderAvatar(String initials, Color bgColor) {
        int d = 42;
        headerIconLabel.setIcon(new Icon() {
            @Override
            public void paintIcon(Component c, Graphics g, int x, int y) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(bgColor != null ? bgColor : UIHelper.ACCENT);
                g2.fillOval(x, y, d, d);
                g2.setColor(Color.WHITE);
                g2.setFont(new Font("Segoe UI", Font.BOLD, 16));
                FontMetrics fm = g2.getFontMetrics();
                String text = initials != null ? initials.toUpperCase() : "—";
                int tx = x + (d - fm.stringWidth(text)) / 2;
                int ty = y + ((d - fm.getHeight()) / 2) + fm.getAscent();
                g2.drawString(text, tx, ty);
                g2.dispose();
            }

            @Override public int getIconWidth() { return d; }
            @Override public int getIconHeight() { return d; }
        });
        return this;
    }

    public ExecutiveDetailDialog setStatusBadge(String text, StatusSeverity severity) {
        StatusSeverity sev = severity != null ? severity : StatusSeverity.NEUTRAL;
        return setStatusBadge(text, sev.bg, sev.fg);
    }

    public ExecutiveDetailDialog setStatusBadge(String text, Color bg, Color fg) {
        statusBadgeLabel.setText(text != null ? text.toUpperCase() : "—");
        statusBadgeLabel.setBackground(bg != null ? bg : UIHelper.BG_CARD);
        statusBadgeLabel.setForeground(fg != null ? fg : UIHelper.TEXT_LIGHT);
        return this;
    }

    public ExecutiveDetailDialog addKpi(String label, String value, String subtext, Color accentColor, String iconName) {
        ModernPanel card = new ModernPanel(12);
        card.setLayout(new BorderLayout(8, 4));
        card.setBorder(new EmptyBorder(10, 14, 10, 14));
        card.setPreferredSize(new Dimension(190, 68));

        JPanel header = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        header.setOpaque(false);
        if (iconName != null && !iconName.isBlank()) {
            header.add(new JLabel(UIHelper.icon(iconName, 13, accentColor != null ? accentColor : UIHelper.ACCENT)));
        }
        JLabel lbl = new JLabel(label);
        lbl.setFont(new Font("Segoe UI", Font.BOLD, 11));
        lbl.setForeground(UIHelper.TEXT_MUTED);
        header.add(lbl);

        JLabel val = new JLabel(value != null ? value : "—");
        val.setFont(new Font("Segoe UI", Font.BOLD, 15));
        val.setForeground(UIHelper.TEXT_LIGHT);

        JLabel sub = new JLabel(subtext != null ? subtext : " ");
        sub.setFont(new Font("Segoe UI", Font.PLAIN, 10));
        sub.setForeground(accentColor != null ? accentColor : UIHelper.TEXT_MUTED);

        JPanel body = new JPanel(new GridLayout(2, 1, 0, 1));
        body.setOpaque(false);
        body.add(val);
        body.add(sub);

        card.add(header, BorderLayout.NORTH);
        card.add(body, BorderLayout.CENTER);

        kpiCards.add(card);
        kpiRowPanel.removeAll();
        for (JComponent c : kpiCards) {
            kpiRowPanel.add(c);
        }
        kpiRowPanel.revalidate();
        return this;
    }

    public ExecutiveDetailDialog addTab(String title, String iconName, Color tabAccent, JComponent content) {
        Icon icon = (iconName != null && !iconName.isBlank())
                ? UIHelper.icon(iconName, 15, tabAccent != null ? tabAccent : UIHelper.ACCENT)
                : null;
        tabbedPane.addTab(title, icon, content);
        return this;
    }

    public ExecutiveDetailDialog addLeftAction(ModernButton button) {
        leftActionsPanel.add(button);
        return this;
    }

    public ExecutiveDetailDialog addRightAction(ModernButton button) {
        // Se adicionado um botão customizado à direita, limpa o padrão se for a primeira adição
        if (rightActionsPanel.getComponentCount() == 1 && rightActionsPanel.getComponent(0) instanceof ModernButton mb
                && "Fechar".equalsIgnoreCase(mb.getText())) {
            rightActionsPanel.removeAll();
        }
        rightActionsPanel.add(button);
        return this;
    }

    public JDialog getDialog() {
        return dialog;
    }

    public void showDialog() {
        if (dialog == null || GraphicsEnvironment.isHeadless() || "true".equalsIgnoreCase(System.getProperty("java.awt.headless"))
                || "true".equalsIgnoreCase(System.getProperty("multicore.test.headless"))) {
            return;
        }
        dialog.setLocationRelativeTo(parent);
        dialog.setVisible(true);
    }

    public void dispose() {
        if (dialog != null) {
            dialog.dispose();
        }
    }
}
