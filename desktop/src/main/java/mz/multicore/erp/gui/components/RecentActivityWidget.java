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
        setBorder(BorderFactory.createEmptyBorder(14, 16, 14, 16));

        // Header
        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);

        JLabel titleLabel = new JLabel("Atividade Recente");
        titleLabel.setFont(new Font(UIHelper.FONT, Font.BOLD, 14));
        titleLabel.setForeground(UIHelper.TEXT_LIGHT);

        JLabel iconLabel = new JLabel(UIHelper.icon("fas-stream", 14, UIHelper.ACCENT_BLUE));
        header.add(iconLabel, BorderLayout.WEST);
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
            JLabel empty = new JLabel("Sem atividade recente registada.");
            empty.setFont(new Font(UIHelper.FONT, Font.PLAIN, 12));
            empty.setForeground(UIHelper.TEXT_MUTED);
            empty.setBorder(BorderFactory.createEmptyBorder(20, 10, 20, 10));
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
        row.setBorder(BorderFactory.createEmptyBorder(2, 4, 2, 4));

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
        titleLbl.setFont(new Font(UIHelper.FONT, Font.BOLD, 12));
        titleLbl.setForeground(UIHelper.TEXT_LIGHT);

        JLabel detailLbl = new JLabel(item.detail() != null ? item.detail() : "");
        detailLbl.setFont(new Font(UIHelper.FONT, Font.PLAIN, 11));
        detailLbl.setForeground(UIHelper.TEXT_MUTED);

        textStack.add(titleLbl);
        textStack.add(detailLbl);
        row.add(textStack, BorderLayout.CENTER);

        // Right: Timestamp
        JLabel timeLbl = new JLabel(item.timeAgo() != null ? item.timeAgo() : "");
        timeLbl.setFont(new Font(UIHelper.FONT, Font.PLAIN, 10));
        timeLbl.setForeground(UIHelper.TEXT_MUTED);
        row.add(timeLbl, BorderLayout.EAST);

        return row;
    }
}
