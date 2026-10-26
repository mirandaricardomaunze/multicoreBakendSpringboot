package mz.multicore.erp.gui.components;

import mz.multicore.erp.architecture.quantity.PackagingComposition;
import mz.multicore.erp.gui.components.ShelfLabelsDialog.PackagingLevel;
import mz.multicore.erp.modules.comercial.dto.ProductDTO;

import javax.swing.*;
import java.awt.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * Renderizador gráfico 2D de etiquetas de prateleira, gôndola e embalagem multinível.
 */
public final class ShelfLabelCardRenderer {

    private ShelfLabelCardRenderer() {}

    public static JPanel createSingleLabelCard(ProductDTO product) {
        return createSingleLabelCard(product, PackagingLevel.UNIT, null, null);
    }

    public static JPanel createSingleLabelCard(ProductDTO product, PackagingLevel level) {
        return createSingleLabelCard(product, level, null, null);
    }

    public static JPanel createSingleLabelCard(ProductDTO product, PackagingLevel level, String batchNumber, String expiryDate) {
        if (level == null) level = PackagingLevel.UNIT;
        final PackagingLevel finalLevel = level;

        JPanel card = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(Color.WHITE);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                g2.setColor(new Color(203, 213, 225));
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 8, 8);
                g2.dispose();
            }
        };
        card.setLayout(new BorderLayout(4, 4));
        card.setOpaque(false);
        card.setPreferredSize(new Dimension(240, 130));
        card.setBorder(BorderFactory.createEmptyBorder(8, 10, 8, 10));

        // Top line: Brand and Category / Level
        JPanel topRow = new JPanel(new BorderLayout());
        topRow.setOpaque(false);
        JLabel brand = new JLabel("MULTICORE ERP");
        brand.setFont(new Font("Arial", Font.BOLD, 9));
        brand.setForeground(new Color(100, 116, 139));

        String levelBadge = switch (finalLevel) {
            case BOX -> "[CAIXA]";
            case PACKAGE -> "[EMBALAGEM]";
            case UNIT -> "[UNIDADE]";
        };

        String catName = (product.categoryName() != null ? product.categoryName() : "GERAL") + " " + levelBadge;
        JLabel cat = new JLabel(catName);
        cat.setFont(new Font("Arial", Font.BOLD, 8));
        cat.setForeground(new Color(79, 70, 229));
        topRow.add(brand, BorderLayout.WEST);
        topRow.add(cat, BorderLayout.EAST);
        card.add(topRow, BorderLayout.NORTH);

        // Center: Product Name and Barcode
        JPanel centerPanel = new JPanel();
        centerPanel.setOpaque(false);
        centerPanel.setLayout(new BoxLayout(centerPanel, BoxLayout.Y_AXIS));

        JLabel nameLabel = new JLabel("<html><b>" + product.name() + "</b></html>");
        nameLabel.setFont(new Font("Arial", Font.BOLD, 12));
        nameLabel.setForeground(new Color(15, 23, 42));

        String code = (product.barcode() != null && !product.barcode().isBlank())
                ? product.barcode() : (product.sku() != null ? product.sku() : "0000000000");

        JPanel barcodePanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setColor(Color.BLACK);
                int w = getWidth();
                int h = getHeight() - 12;
                int bars = Math.min(code.length() * 4, w - 10);
                for (int i = 5; i < bars + 5; i += 2) {
                    int barW = ((i * 7) % 3 == 0) ? 2 : 1;
                    g2.fillRect(i, 2, barW, h);
                }
                g2.setFont(new Font("Monospaced", Font.PLAIN, 9));
                g2.drawString(code, 6, getHeight() - 2);
                g2.dispose();
            }
        };
        barcodePanel.setOpaque(false);
        barcodePanel.setPreferredSize(new Dimension(140, 32));
        barcodePanel.setMaximumSize(new Dimension(200, 32));

        centerPanel.add(nameLabel);
        centerPanel.add(Box.createVerticalStrut(2));
        centerPanel.add(barcodePanel);

        if (product.packagesPerBox() > 1 || product.unitsPerPackage() > 1) {
            PackagingComposition comp = PackagingComposition.of(product.packagesPerBox(), product.unitsPerPackage());
            JLabel pkgHint = new JLabel("1 Cx = " + comp.packagesPerBox() + " emb. × " + comp.unitsPerPackage() + " un. (" + comp.unitsPerBox() + " un.)");
            pkgHint.setFont(new Font("Arial", Font.PLAIN, 8));
            pkgHint.setForeground(new Color(100, 116, 139));
            centerPanel.add(pkgHint);
        }

        if ((batchNumber != null && !batchNumber.isBlank()) || (expiryDate != null && !expiryDate.isBlank())) {
            StringBuilder lotInfo = new StringBuilder();
            if (batchNumber != null && !batchNumber.isBlank()) lotInfo.append("Lote: ").append(batchNumber.trim());
            if (expiryDate != null && !expiryDate.isBlank()) {
                if (!lotInfo.isEmpty()) lotInfo.append(" | ");
                lotInfo.append("Val: ").append(expiryDate.trim());
            }
            JLabel lotHint = new JLabel(lotInfo.toString());
            lotHint.setFont(new Font("Arial", Font.BOLD, 8));
            lotHint.setForeground(new Color(79, 70, 229));
            centerPanel.add(lotHint);
        }
        card.add(centerPanel, BorderLayout.CENTER);

        // Bottom: Calculated Price for this packaging level
        JPanel bottomRow = new JPanel(new BorderLayout());
        bottomRow.setOpaque(false);
        BigDecimal basePrice = product.unitPrice() != null ? product.unitPrice() : BigDecimal.ZERO;
        BigDecimal calculatedPrice = switch (finalLevel) {
            case BOX -> basePrice.multiply(BigDecimal.valueOf(product.unitsPerBox()));
            case PACKAGE -> basePrice.multiply(BigDecimal.valueOf(product.unitsPerPackage()));
            case UNIT -> basePrice;
        };

        JLabel priceLabel = new JLabel(String.format("%,.2f MT", calculatedPrice));
        priceLabel.setFont(new Font("Arial", Font.BOLD, 15));
        priceLabel.setForeground(new Color(16, 185, 129));

        JLabel dateLabel = new JLabel(LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
        dateLabel.setFont(new Font("Arial", Font.PLAIN, 8));
        dateLabel.setForeground(new Color(148, 163, 184));

        bottomRow.add(priceLabel, BorderLayout.WEST);
        bottomRow.add(dateLabel, BorderLayout.EAST);
        card.add(bottomRow, BorderLayout.SOUTH);
        return card;
    }
}
