package mz.multicore.erp.gui;

import mz.multicore.erp.gui.components.KpiCard;
import mz.multicore.erp.gui.components.ModernPanel;
import mz.multicore.erp.gui.components.UIHelper;
import mz.multicore.erp.modules.comercial.dto.ProductDTO;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Construtor do bloco Hero e grelha de KPIs para a ficha detalhada de artigo.
 */
public final class StockProductDetailHeroBuilder {

    private StockProductDetailHeroBuilder() {}

    public static JComponent buildHeroSection(ProductDTO product, BigDecimal totalQty, JDialog parentDialog) {
        JPanel hero = new JPanel(new BorderLayout(10, 0));
        hero.setOpaque(false);
        hero.add(buildImageCard(product, parentDialog), BorderLayout.WEST);
        hero.add(buildKpiGrid(product, totalQty), BorderLayout.CENTER);
        return hero;
    }

    public static JComponent buildImageCard(ProductDTO product, JDialog parentDialog) {
        ModernPanel card = new ModernPanel(16);
        card.setLayout(new BorderLayout(0, 4));
        card.setPreferredSize(new Dimension(96, KpiCard.STANDARD_CARD_HEIGHT));
        card.setBorder(new EmptyBorder(6, 6, 6, 6));

        JLabel imgLabel = new JLabel("", SwingConstants.CENTER);
        imgLabel.setHorizontalAlignment(SwingConstants.CENTER);
        imgLabel.setVerticalAlignment(SwingConstants.CENTER);

        if (product.image() != null && product.image().length > 0) {
            ImageIcon icon = UIHelper.imageIconFromBytes(product.image(), 80, 80);
            if (icon != null) {
                imgLabel.setIcon(icon);
                imgLabel.setToolTipText("Fotografia oficial do artigo • Clique para ampliar");
                imgLabel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                imgLabel.addMouseListener(new java.awt.event.MouseAdapter() {
                    @Override
                    public void mouseClicked(java.awt.event.MouseEvent e) {
                        openFullImageModal(product, parentDialog);
                    }
                });
            } else {
                renderPlaceholderImage(imgLabel);
            }
        } else {
            renderPlaceholderImage(imgLabel);
        }

        card.add(imgLabel, BorderLayout.CENTER);
        return card;
    }

    private static void renderPlaceholderImage(JLabel label) {
        label.setIcon(UIHelper.icon("fas-box-open", 28, UIHelper.TEXT_MUTED));
        label.setText("Sem foto");
        label.setHorizontalTextPosition(SwingConstants.CENTER);
        label.setVerticalTextPosition(SwingConstants.BOTTOM);
        label.setForeground(UIHelper.TEXT_MUTED);
        label.setFont(new Font(UIHelper.FONT, Font.PLAIN, 10));
    }

    public static void openFullImageModal(ProductDTO product, JDialog parentDialog) {
        if (product.image() == null || product.image().length == 0) return;
        ImageIcon fullIcon = UIHelper.imageIconFromBytes(product.image(), 480, 480);
        if (fullIcon == null) return;

        JDialog viewDlg = new JDialog(parentDialog, product.name() + " — Fotografia", Dialog.ModalityType.APPLICATION_MODAL);
        viewDlg.getContentPane().setBackground(UIHelper.BG_DARK);
        JLabel fullLabel = new JLabel(fullIcon);
        fullLabel.setBorder(new EmptyBorder(20, 20, 20, 20));
        viewDlg.add(fullLabel);
        viewDlg.pack();
        viewDlg.setLocationRelativeTo(parentDialog);
        viewDlg.setVisible(true);
    }

    public static JComponent buildKpiGrid(ProductDTO product, BigDecimal totalQty) {
        JPanel grid = KpiCard.createGrid(4, 8, 8);

        BigDecimal sellPrice = product.unitPrice() != null ? product.unitPrice() : BigDecimal.ZERO;
        String vatInfo = product.taxRateLabel() != null ? product.taxRateLabel() : "IVA Geral";
        grid.add(KpiCard.createCard("Preço de Venda", formatMoney(sellPrice), vatInfo, "fas-tag", UIHelper.APPROVED_GREEN));

        int unitsPerBox = product.unitsPerBox() > 0 ? product.unitsPerBox() : 1;
        String boxInfo = mz.multicore.erp.architecture.quantity.PackagingQuantity.label(totalQty,
                mz.multicore.erp.architecture.quantity.PackagingComposition.of(
                        product.packagesPerBox(), product.unitsPerPackage()))
                + " (" + unitsPerBox + " un/cx)";
        grid.add(KpiCard.createCard("Stock Disponível", String.format("%,.0f un", totalQty.doubleValue()), boxInfo, "fas-boxes", UIHelper.ACCENT_BLUE));

        BigDecimal costPrice = product.purchasePrice() != null ? product.purchasePrice() : BigDecimal.ZERO;
        String marginText = "—";
        if (sellPrice.signum() > 0 && costPrice.signum() > 0) {
            BigDecimal margin = sellPrice.subtract(costPrice).divide(sellPrice, 4, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100));
            marginText = String.format("Margem: %.1f%%", margin.doubleValue());
        }
        grid.add(KpiCard.createCard("Preço de Custo", formatMoney(costPrice), marginText, "fas-shopping-cart", UIHelper.PENDING_YELLOW));

        if (product.wholesalePrice() != null && product.wholesalePrice().signum() > 0) {
            String qtyMin = product.wholesaleMinQty() != null ? "A partir de " + product.wholesaleMinQty() + " un" : "Preço de revenda";
            grid.add(KpiCard.createCard("Preço Grosso", formatMoney(product.wholesalePrice()), qtyMin, "fas-layer-group", UIHelper.ACCENT_CYAN));
        } else {
            grid.add(KpiCard.createCard("Preço Grosso", "Não activo", "Venda exclusiva a retalho", "fas-layer-group", UIHelper.TEXT_MUTED));
        }

        return grid;
    }

    public static String formatMoney(BigDecimal amount) {
        if (amount == null) return "0,00 MT";
        return UIHelper.formatMzn(amount);
    }
}
