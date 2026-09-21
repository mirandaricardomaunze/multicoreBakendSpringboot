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
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Widget de Ranking dos Produtos Mais Vendidos para o Dashboard.
 */
public class TopProductsWidget extends ModernPanel {

    public record RankedProduct(
            String name,
            BigDecimal revenue,
            int quantity,
            double percentage
    ) {}

    private final JPanel listContainer = new JPanel();
    private final List<RankedProduct> products = new ArrayList<>();

    public TopProductsWidget() {
        super(12, UIHelper.BG_CARD, UIHelper.BG_CARD);
        setLayout(new BorderLayout(0, 10));
        setBorder(BorderFactory.createEmptyBorder(14, 16, 14, 16));

        // Header
        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);

        JLabel titleLabel = new JLabel("Top 5 Produtos Mais Vendidos");
        titleLabel.setFont(new Font(UIHelper.FONT, Font.BOLD, 14));
        titleLabel.setForeground(UIHelper.TEXT_LIGHT);

        JLabel iconLabel = new JLabel(UIHelper.icon("fas-award", 14, UIHelper.PENDING_YELLOW));
        header.add(iconLabel, BorderLayout.WEST);
        header.add(titleLabel, BorderLayout.CENTER);

        add(header, BorderLayout.NORTH);

        // List
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

    public void setProducts(List<RankedProduct> items) {
        products.clear();
        if (items != null) {
            products.addAll(items);
        }
        render();
    }

    public List<RankedProduct> getProducts() {
        return List.copyOf(products);
    }

    private void render() {
        listContainer.removeAll();
        if (products.isEmpty()) {
            JLabel empty = new JLabel("Sem dados de vendas registados.");
            empty.setFont(new Font(UIHelper.FONT, Font.PLAIN, 12));
            empty.setForeground(UIHelper.TEXT_MUTED);
            empty.setBorder(BorderFactory.createEmptyBorder(20, 10, 20, 10));
            listContainer.add(empty);
        } else {
            for (int i = 0; i < products.size(); i++) {
                RankedProduct p = products.get(i);
                listContainer.add(createProductRow(i + 1, p));
                if (i < products.size() - 1) {
                    listContainer.add(Box.createVerticalStrut(8));
                }
            }
        }
        listContainer.revalidate();
        listContainer.repaint();
    }

    private JPanel createProductRow(int rank, RankedProduct p) {
        JPanel row = new JPanel(new BorderLayout(8, 4));
        row.setOpaque(false);

        // Top line: Rank + Name (Left) and Revenue + Qty (Right)
        JPanel textRow = new JPanel(new BorderLayout());
        textRow.setOpaque(false);

        JLabel nameLbl = new JLabel(rank + ". " + p.name());
        nameLbl.setFont(new Font(UIHelper.FONT, Font.BOLD, 12));
        nameLbl.setForeground(UIHelper.TEXT_LIGHT);

        String revStr = String.format("%,.2f MT (%d un)", p.revenue() != null ? p.revenue() : BigDecimal.ZERO, p.quantity());
        JLabel revLbl = new JLabel(revStr);
        revLbl.setFont(new Font(UIHelper.FONT, Font.PLAIN, 11));
        revLbl.setForeground(UIHelper.APPROVED_GREEN);

        textRow.add(nameLbl, BorderLayout.WEST);
        textRow.add(revLbl, BorderLayout.EAST);
        row.add(textRow, BorderLayout.NORTH);

        // Progress bar line
        JPanel bar = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int w = getWidth();
                int h = getHeight();
                g2.setColor(UIHelper.isLight() ? new Color(226, 232, 240) : new Color(51, 65, 85));
                g2.fillRoundRect(0, 0, w, h, 4, 4);

                int filledW = (int) Math.round((p.percentage() / 100.0) * w);
                if (filledW > 0) {
                    g2.setColor(rank == 1 ? UIHelper.PENDING_YELLOW : UIHelper.ACCENT_BLUE);
                    g2.fillRoundRect(0, 0, filledW, h, 4, 4);
                }
                g2.dispose();
            }
        };
        bar.setOpaque(false);
        bar.setPreferredSize(new Dimension(100, 6));
        row.add(bar, BorderLayout.SOUTH);

        return row;
    }
}
