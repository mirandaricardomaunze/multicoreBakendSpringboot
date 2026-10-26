package mz.multicore.erp.gui;

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

        JPanel mainPanel = new JPanel(new BorderLayout(0, 12));
        mainPanel.setBackground(UIHelper.BG_DARK);
        mainPanel.setBorder(new EmptyBorder(14, 20, 14, 20));

        // 1. Cabeçalho Executivo
        mainPanel.add(buildHeader(), BorderLayout.NORTH);

        // 2. Corpo: Hero com Imagem + KPIs + Abas de Detalhe
        JPanel body = new JPanel(new BorderLayout(0, 10));
        body.setOpaque(false);
        BigDecimal totalQty = computeTotalStockQuantity();
        body.add(StockProductDetailHeroBuilder.buildHeroSection(product, totalQty, dialog), BorderLayout.NORTH);
        body.add(StockProductDetailTabsBuilder.buildTabsSection(owner, product), BorderLayout.CENTER);
        mainPanel.add(body, BorderLayout.CENTER);

        // 3. Rodapé com Ações
        mainPanel.add(buildFooter(), BorderLayout.SOUTH);

        dialog.setContentPane(mainPanel);
        dialog.pack();
        dialog.setMinimumSize(new Dimension(800, 520));
        UIHelper.applyStandardModalBounds(dialog, 960, 610);
    }

    public void showDialog() {
        dialog.setVisible(true);
    }

    private JComponent buildHeader() {
        JPanel header = new JPanel(new BorderLayout(14, 0));
        header.setOpaque(false);

        JPanel left = new JPanel();
        left.setOpaque(false);
        left.setLayout(new BoxLayout(left, BoxLayout.Y_AXIS));

        JLabel titleLbl = new JLabel(product.name());
        titleLbl.setFont(new Font(UIHelper.FONT, Font.BOLD, 20));
        titleLbl.setForeground(UIHelper.TEXT_LIGHT);
        left.add(titleLbl);

        left.add(Box.createVerticalStrut(4));

        JPanel tagRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        tagRow.setOpaque(false);
        tagRow.add(createPill("SKU: " + product.sku(), UIHelper.ACCENT_BLUE));
        if (product.reference() != null && !product.reference().isBlank()) tagRow.add(createPill("Ref: " + product.reference(), UIHelper.ACCENT));
        if (product.barcode() != null && !product.barcode().isBlank()) tagRow.add(createPill("EAN: " + product.barcode(), UIHelper.TEXT_MUTED));
        if (product.categoryName() != null && !product.categoryName().isBlank()) tagRow.add(createPill(product.categoryName(), UIHelper.ACCENT_PINK));
        left.add(tagRow);
        header.add(left, BorderLayout.CENTER);

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
        JPanel footer = new JPanel(new BorderLayout());
        footer.setOpaque(false);
        footer.setBorder(new EmptyBorder(10, 0, 0, 0));

        JPanel leftActions = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        leftActions.setOpaque(false);

        ModernButton editBtn = UIHelper.createSecondaryButton("Editar Artigo");
        editBtn.setIcon(UIHelper.icon("fas-edit", 14, UIHelper.PENDING_YELLOW));
        editBtn.addActionListener(e -> { dialog.dispose(); owner.editProductDialog(product.id()); });
        leftActions.add(editBtn);

        ModernButton labelBtn = UIHelper.createSecondaryButton("Gerar Etiqueta");
        labelBtn.setIcon(UIHelper.icon("fas-barcode", 14, UIHelper.ACCENT));
        labelBtn.addActionListener(e -> { dialog.dispose(); owner.openLabelDialog(); });
        leftActions.add(labelBtn);
        footer.add(leftActions, BorderLayout.WEST);

        ModernButton closeBtn = UIHelper.createSecondaryButton("Fechar");
        closeBtn.setIcon(UIHelper.icon("fas-times", 14));
        closeBtn.addActionListener(e -> dialog.dispose());
        footer.add(closeBtn, BorderLayout.EAST);
        return footer;
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
        l.setFont(new Font(UIHelper.FONT, Font.BOLD, 11));
        l.setForeground(color);
        l.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(color, 1, true),
                new EmptyBorder(2, 8, 2, 8)
        ));
        return l;
    }

    public static void showFor(StockPanel owner, Long productId) {
        if (productId == null) {
            owner.showStockNotice(FeedbackType.INFO, "Selecione um artigo", "Selecione um produto na tabela para ver a ficha detalhada.");
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
