package mz.multicore.erp.gui.components;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.util.ArrayList;
import java.util.List;

/**
 * Widget de Feed de Atividades Recentes para o Dashboard.
 * Exibe a linha do tempo das últimas operações realizadas na empresa.
 */
public class RecentActivityWidget extends ModernPanel {

    public record ActivityEntry(
            String id,
            String title,
            String detail,
            String timeAgo,
            String iconCode,
            Color accentColor
    ) {}

    private final JPanel listContainer = new JPanel();
    private final List<ActivityEntry> entries = new ArrayList<>();

    public RecentActivityWidget() {
        super(12, UIHelper.BG_CARD, UIHelper.BG_CARD);
        setLayout(new BorderLayout(0, 10));
        setBorder(BorderFactory.createEmptyBorder(Spacing.LG, Spacing.LG, Spacing.LG, Spacing.LG));

        // Header
        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);

        JLabel titleLabel = new JLabel("Actividade Recente");
        titleLabel.setFont(Typography.EMPHASIS_BOLD);
        titleLabel.setForeground(UIHelper.TEXT_LIGHT);

        header.add(titleLabel, BorderLayout.CENTER);

        add(header, BorderLayout.NORTH);

        // Timeline List
        listContainer.setOpaque(false);
        listContainer.setLayout(new BoxLayout(listContainer, BoxLayout.Y_AXIS));

        JScrollPane scroll = new JScrollPane(listContainer);
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.setPreferredSize(new Dimension(320, 190));
        scroll.getVerticalScrollBar().setUnitIncrement(14);
        add(scroll, BorderLayout.CENTER);
    }

    public void setActivities(List<ActivityEntry> newEntries) {
        entries.clear();
        if (newEntries != null) {
            entries.addAll(newEntries);
        }
        render();
    }

    public List<ActivityEntry> getActivities() {
        return List.copyOf(entries);
    }

    private void render() {
        listContainer.removeAll();
        if (entries.isEmpty()) {
            JLabel empty = new JLabel("Sem actividade recente registada.");
            empty.setFont(Typography.BODY);
            empty.setForeground(UIHelper.TEXT_MUTED);
            empty.setBorder(BorderFactory.createEmptyBorder(Spacing.XL, Spacing.SM, Spacing.XL, Spacing.SM));
            listContainer.add(empty);
        } else {
            for (int i = 0; i < entries.size(); i++) {
                ActivityEntry item = entries.get(i);
                listContainer.add(createTimelineRow(item, i == entries.size() - 1));
                if (i < entries.size() - 1) {
                    listContainer.add(Box.createVerticalStrut(6));
                }
            }
        }
        listContainer.revalidate();
        listContainer.repaint();
    }

    private JPanel createTimelineRow(ActivityEntry item, boolean isLast) {
        JPanel row = new JPanel(new BorderLayout(10, 0));
        row.setOpaque(false);
        row.setBorder(BorderFactory.createEmptyBorder(Spacing.XXS, Spacing.XS, Spacing.XXS, Spacing.XS));

        // Left Icon Badge
        Color color = item.accentColor() != null ? item.accentColor() : UIHelper.ACCENT_BLUE;
        JPanel iconBadge = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0)) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), 30));
                g2.fillOval(0, 0, getWidth(), getHeight());
                g2.dispose();
            }
        };
        iconBadge.setOpaque(false);
        iconBadge.setPreferredSize(new Dimension(28, 28));
        iconBadge.add(new JLabel(UIHelper.icon(item.iconCode() != null ? item.iconCode() : "fas-circle", 12, color)));
        row.add(iconBadge, BorderLayout.WEST);

        // Center: Title + Detail
        JPanel textStack = new JPanel();
        textStack.setOpaque(false);
        textStack.setLayout(new BoxLayout(textStack, BoxLayout.Y_AXIS));

        JLabel titleLbl = new JLabel(item.title());
        titleLbl.setFont(Typography.BODY_BOLD);
        titleLbl.setForeground(UIHelper.TEXT_LIGHT);

        JLabel detailLbl = new JLabel(item.detail() != null ? item.detail() : "");
        detailLbl.setFont(Typography.CAPTION);
        detailLbl.setForeground(UIHelper.TEXT_MUTED);

        textStack.add(titleLbl);
        textStack.add(detailLbl);
        row.add(textStack, BorderLayout.CENTER);

        // Right: Timestamp
        JLabel timeLbl = new JLabel(item.timeAgo() != null ? item.timeAgo() : "");
        timeLbl.setFont(Typography.CAPTION);
        timeLbl.setForeground(UIHelper.TEXT_MUTED);
        row.add(timeLbl, BorderLayout.EAST);

        return row;
    }
}
