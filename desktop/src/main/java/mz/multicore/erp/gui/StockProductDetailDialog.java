package mz.multicore.erp.gui;

import mz.multicore.erp.gui.components.DialogChrome;
import mz.multicore.erp.gui.components.Spacing;
import mz.multicore.erp.gui.components.Typography;
import mz.multicore.erp.gui.components.ButtonStyles;
import mz.multicore.erp.gui.components.FeedbackType;
import mz.multicore.erp.gui.components.ModernButton;
import mz.multicore.erp.gui.components.UIHelper;
import mz.multicore.erp.modules.comercial.dto.ProductDTO;
import mz.multicore.erp.modules.inventory.dto.StockDTO;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.math.BigDecimal;

/**
 * Ficha executiva e detalhada do artigo (Product Detail View).
 * Apresenta a fotografia em alta definição, indicadores financeiros e de margem,
 * especificações logísticas, saldos por armazém e rastreabilidade de lotes com validade.
 */
public class StockProductDetailDialog {

    private final StockPanel owner;
    private final ProductDTO product;
    private final JDialog dialog;

    public StockProductDetailDialog(StockPanel owner, ProductDTO product) {
        this.owner = owner;
        this.product = product;
        this.dialog = new JDialog(UIHelper.mainWindow, "Ficha do Artigo — " + product.name(), Dialog.ModalityType.APPLICATION_MODAL);
        initDialog();
    }

    private void initDialog() {
        dialog.getContentPane().setBackground(UIHelper.BG_DARK);
        dialog.setIconImage(UIHelper.iconImage("fas-box-open", 24, UIHelper.MODULE_STOCK));

        // Corpo: identificação (SKU, referência, estado) + Hero com Imagem + KPIs + Abas de Detalhe
        JPanel body = new JPanel(new BorderLayout(0, 10));
        body.setOpaque(false);
        BigDecimal totalQty = computeTotalStockQuantity();
        JPanel top = new JPanel(new BorderLayout(0, Spacing.MD));
        top.setOpaque(false);
        top.add(buildTags(), BorderLayout.NORTH);
        top.add(StockProductDetailHeroBuilder.buildHeroSection(product, totalQty, dialog), BorderLayout.CENTER);
        body.add(top, BorderLayout.NORTH);
        body.add(StockProductDetailTabsBuilder.buildTabsSection(owner, product), BorderLayout.CENTER);

        dialog.setContentPane(DialogChrome.frame("fas-box-open", product.name(),
                product.categoryName() != null && !product.categoryName().isBlank() ? product.categoryName() : "Ficha do artigo",
                body, buildFooter()));
        dialog.pack();
        dialog.setMinimumSize(new Dimension(800, 520));
        UIHelper.applyStandardModalBounds(dialog, 960, 610);
    }

    public void showDialog() {
        dialog.setVisible(true);
    }

    /** SKU, referência, EAN e estado do stock, por baixo do cabeçalho comum. */
    private JComponent buildTags() {
        JPanel header = new JPanel(new BorderLayout(14, 0));
        header.setOpaque(false);

        JPanel tagRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        tagRow.setOpaque(false);
        tagRow.add(createPill("SKU: " + product.sku(), UIHelper.ACCENT_BLUE));
        if (product.reference() != null && !product.reference().isBlank()) tagRow.add(createPill("Ref: " + product.reference(), UIHelper.ACCENT));
        if (product.barcode() != null && !product.barcode().isBlank()) tagRow.add(createPill("EAN: " + product.barcode(), UIHelper.TEXT_MUTED));
        if (product.categoryName() != null && !product.categoryName().isBlank()) tagRow.add(createPill(product.categoryName(), UIHelper.ACCENT_PINK));
        header.add(tagRow, BorderLayout.CENTER);

        BigDecimal totalQty = computeTotalStockQuantity();
        BigDecimal minStock = product.minStock() != null ? product.minStock() : BigDecimal.ZERO;
        String statusText = totalQty.signum() <= 0 ? "Esgotado" : (totalQty.compareTo(minStock) < 0 ? "Stock Baixo" : "Em Stock");
        Color statusColor = totalQty.signum() <= 0 ? UIHelper.REJECTED_RED : (totalQty.compareTo(minStock) < 0 ? UIHelper.PENDING_YELLOW : UIHelper.APPROVED_GREEN);

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 4));
        right.setOpaque(false);
        right.add(createPill(statusText, statusColor));
        header.add(right, BorderLayout.EAST);
        return header;
    }

    private JPanel buildFooter() {
        JPanel leftActions = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        leftActions.setOpaque(false);

        ModernButton editBtn = ButtonStyles.secondary(UIHelper.createSecondaryButton("Editar Artigo"));
        editBtn.setIcon(UIHelper.icon("fas-edit", 14, UIHelper.PENDING_YELLOW));
        editBtn.addActionListener(e -> { dialog.dispose(); owner.editProductDialog(product.id()); });
        leftActions.add(editBtn);

        ModernButton labelBtn = ButtonStyles.secondary(UIHelper.createSecondaryButton("Gerar Etiqueta"));
        labelBtn.setIcon(UIHelper.icon("fas-barcode", 14, UIHelper.ACCENT));
        labelBtn.addActionListener(e -> { dialog.dispose(); owner.openLabelDialog(); });
        leftActions.add(labelBtn);
        ModernButton closeBtn = ButtonStyles.secondary(UIHelper.createSecondaryButton("Fechar"));
        closeBtn.setIcon(UIHelper.icon("fas-times", 14));
        closeBtn.addActionListener(e -> dialog.dispose());
        return DialogChrome.footerWithNote(leftActions, closeBtn);
    }

    private BigDecimal computeTotalStockQuantity() {
        if (owner == null || owner.stocksList == null) return BigDecimal.ZERO;
        BigDecimal total = BigDecimal.ZERO;
        for (StockDTO s : owner.stocksList) {
            if (s.productId() != null && s.productId().equals(product.id()) && s.quantity() != null) {
                total = total.add(s.quantity());
            }
        }
        return total;
    }

    private JComponent createPill(String text, Color color) {
        JLabel l = new JLabel(text);
        l.setFont(Typography.CAPTION_BOLD);
        l.setForeground(color);
        l.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(color, 1, true),
                new EmptyBorder(Spacing.XXS, Spacing.SM, Spacing.XXS, Spacing.SM)
        ));
        return l;
    }

    public static void showFor(StockPanel owner, Long productId) {
        if (productId == null) {
            owner.showStockNotice(FeedbackType.INFO, "Seleccione um artigo", "Seleccione um produto na tabela para ver a ficha detalhada.");
            return;
        }
        ProductDTO product = null;
        for (ProductDTO p : owner.catalogProducts) {
            if (p.id().equals(productId)) { product = p; break; }
        }
        if (product == null) {
            try {
                owner.catalogProducts = owner.comercialApiClient.getAllProducts();
                for (ProductDTO p : owner.catalogProducts) {
                    if (p.id().equals(productId)) { product = p; break; }
                }
            } catch (Exception ignored) {}
        }
        if (product != null) new StockProductDetailDialog(owner, product).showDialog();
    }
}
