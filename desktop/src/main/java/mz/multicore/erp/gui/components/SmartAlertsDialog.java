package mz.multicore.erp.gui.components;

import mz.multicore.erp.gui.NotificationFeed.NotificationItem;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Centro de alertas inteligentes e ações proativas operacionais.
 */
public class SmartAlertsDialog extends JDialog {

    public enum Urgency {
        CRITICAL("Crítico", UIHelper.REJECTED_RED, "fas-exclamation-circle"),
        WARNING("Atenção", UIHelper.PENDING_YELLOW, "fas-exclamation-triangle"),
        INFO("Informativo", UIHelper.ACCENT_BLUE, "fas-info-circle");

        private final String label;
        private final Color color;
        private final String icon;

        Urgency(String label, Color color, String icon) {
            this.label = label;
            this.color = color;
            this.icon = icon;
        }

        public String getLabel() { return label; }
        public Color getColor() { return color; }
        public String getIcon() { return icon; }
    }

    public record SmartAlert(
            Urgency urgency,
            String category,
            String title,
            String detail,
            String actionLabel,
            String targetModule
    ) {
        public static SmartAlert fromNotification(NotificationItem item) {
            Urgency u = Urgency.INFO;
            String action = "Abrir Módulo";
            if (item.priority() >= 3 || (item.title() != null && item.title().toLowerCase().contains("vencido"))) {
                u = Urgency.CRITICAL;
                action = "Resolver Imediatamente";
            } else if (item.priority() == 2 || (item.title() != null && item.title().toLowerCase().contains("baixo"))) {
                u = Urgency.WARNING;
                action = "Gerir Stock / Compras";
            }
            if ("desempenho".equals(item.moduleCard())) {
                action = "Acompanhar Meta";
            } else if ("risco_credito".equals(item.moduleCard())) {
                action = "Cobrar / Ver Risco";
            } else if ("stock_waste".equals(item.moduleCard())) {
                action = "Aprovar Quebras";
            }
            return new SmartAlert(u, item.type(), item.title(), item.detail(), action, item.moduleCard());
        }
    }

    private final List<SmartAlert> allAlerts;
    private final Consumer<String> onNavigate;
    private JPanel listContainer;
    private Urgency currentFilter = null; // null = todos

    public SmartAlertsDialog(Window owner, List<SmartAlert> alerts, Consumer<String> onNavigate) {
        super(owner, "Centro de Alertas Inteligentes & Ações Proativas", ModalityType.APPLICATION_MODAL);
        this.allAlerts = alerts != null ? alerts : List.of();
        this.onNavigate = onNavigate;

        setSize(680, 580);
        setLocationRelativeTo(owner);
        initComponents();
        refreshList();
    }

    private void initComponents() {
        JPanel root = new JPanel(new BorderLayout(0, 14));
        root.setBackground(UIHelper.BG_DARK);
        root.setBorder(new EmptyBorder(18, 20, 18, 20));

        // Topo
        JPanel header = new JPanel(new BorderLayout(0, 8));
        header.setOpaque(false);
        JLabel title = UIHelper.createHeading("Alertas Inteligentes & Ações Proativas");
        title.setIcon(UIHelper.icon("fas-bell", 18));
        JLabel subtitle = new JLabel("Situações operacionais que exigem atenção com ações diretas.");
        subtitle.setFont(new Font(UIHelper.FONT, Font.PLAIN, 12));
        subtitle.setForeground(UIHelper.TEXT_MUTED);

        header.add(title, BorderLayout.NORTH);
        header.add(subtitle, BorderLayout.SOUTH);
        root.add(header, BorderLayout.NORTH);

        // Barra de Filtros
        JPanel filterBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        filterBar.setOpaque(false);

        ModernButton allBtn = UIHelper.createSecondaryButton("Todos (" + allAlerts.size() + ")");
        long critCount = allAlerts.stream().filter(a -> a.urgency() == Urgency.CRITICAL).count();
        ModernButton critBtn = UIHelper.createSecondaryButton("Críticos (" + critCount + ")");
        critBtn.setIcon(UIHelper.icon("fas-exclamation-circle", 12, UIHelper.REJECTED_RED));
        long warnCount = allAlerts.stream().filter(a -> a.urgency() == Urgency.WARNING).count();
        ModernButton warnBtn = UIHelper.createSecondaryButton("Atenção (" + warnCount + ")");
        warnBtn.setIcon(UIHelper.icon("fas-exclamation-triangle", 12, UIHelper.PENDING_YELLOW));

        allBtn.addActionListener(e -> { currentFilter = null; refreshList(); });
        critBtn.addActionListener(e -> { currentFilter = Urgency.CRITICAL; refreshList(); });
        warnBtn.addActionListener(e -> { currentFilter = Urgency.WARNING; refreshList(); });

        filterBar.add(allBtn);
        filterBar.add(critBtn);
        filterBar.add(warnBtn);

        // Lista de Cartões
        listContainer = new JPanel();
        listContainer.setLayout(new BoxLayout(listContainer, BoxLayout.Y_AXIS));
        listContainer.setOpaque(false);

        JScrollPane scroll = new JScrollPane(listContainer);
        UIHelper.styleScrollPane(scroll);
        scroll.setBorder(BorderFactory.createEmptyBorder());

        JPanel centerPanel = new JPanel(new BorderLayout(0, 10));
        centerPanel.setOpaque(false);
        centerPanel.add(filterBar, BorderLayout.NORTH);
        centerPanel.add(scroll, BorderLayout.CENTER);
        root.add(centerPanel, BorderLayout.CENTER);

        // Rodapé
        JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        footer.setOpaque(false);
        ModernButton closeBtn = UIHelper.createSecondaryButton("Fechar");
        closeBtn.addActionListener(e -> dispose());
        footer.add(closeBtn);
        root.add(footer, BorderLayout.SOUTH);

        setContentPane(root);
    }

    private void refreshList() {
        listContainer.removeAll();
        List<SmartAlert> filtered = allAlerts.stream()
                .filter(a -> currentFilter == null || a.urgency() == currentFilter)
                .toList();

        if (filtered.isEmpty()) {
            JLabel empty = new JLabel("Nenhum alerta para a categoria selecionada.");
            empty.setFont(new Font(UIHelper.FONT, Font.ITALIC, 13));
            empty.setForeground(UIHelper.TEXT_MUTED);
            empty.setBorder(new EmptyBorder(30, 10, 10, 10));
            listContainer.add(empty);
        } else {
            for (SmartAlert alert : filtered) {
                listContainer.add(createAlertCard(alert));
                listContainer.add(Box.createVerticalStrut(8));
            }
        }

        listContainer.revalidate();
        listContainer.repaint();
    }

    private JPanel createAlertCard(SmartAlert alert) {
        ModernPanel card = new ModernPanel(12);
        card.setLayout(new BorderLayout(12, 0));
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 4, 0, 0, alert.urgency().getColor()),
                BorderFactory.createEmptyBorder(10, 14, 10, 14)
        ));

        // Lado Esquerdo / Centro: Textos
        JPanel textPanel = new JPanel();
        textPanel.setLayout(new BoxLayout(textPanel, BoxLayout.Y_AXIS));
        textPanel.setOpaque(false);

        JPanel tagLine = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        tagLine.setOpaque(false);

        JLabel catTag = new JLabel("[" + alert.category() + "]");
        catTag.setFont(new Font(UIHelper.FONT, Font.BOLD, 11));
        catTag.setForeground(UIHelper.ACCENT_BLUE);

        JLabel urgTag = new JLabel(alert.urgency().getLabel());
        urgTag.setIcon(UIHelper.icon(alert.urgency().getIcon(), 11, alert.urgency().getColor()));
        urgTag.setFont(new Font(UIHelper.FONT, Font.BOLD, 11));
        urgTag.setForeground(alert.urgency().getColor());

        tagLine.add(catTag);
        tagLine.add(urgTag);

        JLabel titleLbl = new JLabel(alert.title());
        titleLbl.setFont(new Font(UIHelper.FONT, Font.BOLD, 13));
        titleLbl.setForeground(UIHelper.TEXT_LIGHT);
        titleLbl.setBorder(new EmptyBorder(3, 0, 2, 0));

        JLabel detailLbl = new JLabel(alert.detail());
        detailLbl.setFont(new Font(UIHelper.FONT, Font.PLAIN, 12));
        detailLbl.setForeground(UIHelper.TEXT_MUTED);

        textPanel.add(tagLine);
        textPanel.add(titleLbl);
        textPanel.add(detailLbl);
        card.add(textPanel, BorderLayout.CENTER);

        // Lado Direito: Botão de Ação
        JPanel actionBox = new JPanel(new GridBagLayout());
        actionBox.setOpaque(false);
        ModernButton actionBtn = UIHelper.createPrimaryButton(alert.actionLabel());
        actionBtn.setFont(new Font(UIHelper.FONT, Font.BOLD, 11));
        actionBtn.setIcon(UIHelper.icon("fas-arrow-right", 11));
        actionBtn.addActionListener(e -> {
            dispose();
            if (onNavigate != null && alert.targetModule() != null) {
                onNavigate.accept(alert.targetModule());
            }
        });
        actionBox.add(actionBtn);
        card.add(actionBox, BorderLayout.EAST);

        return card;
    }
}
