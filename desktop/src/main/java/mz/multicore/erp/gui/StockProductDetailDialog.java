package mz.multicore.erp.gui;

import mz.multicore.erp.gui.components.KpiCard;
import mz.multicore.erp.gui.components.ModernButton;
import mz.multicore.erp.gui.components.ModernPanel;
import mz.multicore.erp.gui.components.TableCellRenderers;
import mz.multicore.erp.gui.components.UIHelper;
import mz.multicore.erp.modules.comercial.dto.ProductDTO;
import mz.multicore.erp.modules.inventory.dto.ProductBatchDTO;
import mz.multicore.erp.modules.inventory.dto.StockDTO;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * Ficha executiva e detalhada do artigo (Product Detail View).
 * Apresenta a fotografia em alta definição, indicadores financeiros e de margem,
 * especificações logísticas, saldos por armazém e rastreabilidade de lotes com validade.
 */
public class StockProductDetailDialog {

    private final StockPanel owner;
    private final ProductDTO product;
    private final JDialog dialog;
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    public StockProductDetailDialog(StockPanel owner, ProductDTO product) {
        this.owner = owner;
        this.product = product;
        this.dialog = new JDialog(UIHelper.mainWindow, "Ficha do Artigo — " + product.name(), Dialog.ModalityType.APPLICATION_MODAL);
        initDialog();
    }

    private void initDialog() {
        dialog.getContentPane().setBackground(UIHelper.BG_DARK);
        dialog.setIconImage(UIHelper.iconImage("fas-box-open", 24, UIHelper.MODULE_STOCK));

        JPanel mainPanel = new JPanel(new BorderLayout(0, 16));
        mainPanel.setBackground(UIHelper.BG_DARK);
        mainPanel.setBorder(new EmptyBorder(20, 24, 20, 24));

        // 1. Cabeçalho Executivo
        mainPanel.add(buildHeader(), BorderLayout.NORTH);

        // 2. Corpo: Hero com Imagem + KPIs + Abas de Detalhe
        JPanel body = new JPanel(new BorderLayout(0, 14));
        body.setOpaque(false);
        body.add(buildHeroSection(), BorderLayout.NORTH);
        body.add(buildTabsSection(), BorderLayout.CENTER);
        mainPanel.add(body, BorderLayout.CENTER);

        // 3. Rodapé com Ações
        mainPanel.add(buildFooter(), BorderLayout.SOUTH);

        dialog.setContentPane(mainPanel);
        dialog.pack();
        dialog.setSize(new Dimension(840, 680));
        dialog.setLocationRelativeTo(owner);
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
        if (product.reference() != null && !product.reference().isBlank()) {
            tagRow.add(createPill("Ref: " + product.reference(), UIHelper.ACCENT));
        }
        if (product.barcode() != null && !product.barcode().isBlank()) {
            tagRow.add(createPill("EAN: " + product.barcode(), UIHelper.TEXT_MUTED));
        }
        if (product.categoryName() != null && !product.categoryName().isBlank()) {
            tagRow.add(createPill(product.categoryName(), UIHelper.ACCENT_PINK));
        }

        left.add(tagRow);
        header.add(left, BorderLayout.CENTER);

        // Badge de Status Global de Stock
        BigDecimal totalQty = computeTotalStockQuantity();
        BigDecimal minStock = product.minStock() != null ? product.minStock() : BigDecimal.ZERO;
        String statusText;
        Color statusColor;
        if (totalQty.signum() <= 0) {
            statusText = "Esgotado";
            statusColor = UIHelper.REJECTED_RED;
        } else if (totalQty.compareTo(minStock) < 0) {
            statusText = "Stock Baixo";
            statusColor = UIHelper.PENDING_YELLOW;
        } else {
            statusText = "Em Stock";
            statusColor = UIHelper.APPROVED_GREEN;
        }

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 4));
        right.setOpaque(false);
        right.add(createPill(statusText, statusColor));
        header.add(right, BorderLayout.EAST);

        return header;
    }

    private JComponent buildHeroSection() {
        JPanel hero = new JPanel(new BorderLayout(18, 0));
        hero.setOpaque(false);

        // A. Card de Fotografia em Alta Resolução (Esquerda)
        hero.add(buildImageCard(), BorderLayout.WEST);

        // B. Métricas e Indicadores Chave (Direita)
        hero.add(buildKpiGrid(), BorderLayout.CENTER);

        return hero;
    }

    private JComponent buildImageCard() {
        ModernPanel card = new ModernPanel(14);
        card.setLayout(new BorderLayout(0, 8));
        card.setPreferredSize(new Dimension(210, 210));
        card.setBorder(new EmptyBorder(10, 10, 10, 10));

        JLabel imgLabel = new JLabel("", SwingConstants.CENTER);
        imgLabel.setHorizontalAlignment(SwingConstants.CENTER);
        imgLabel.setVerticalAlignment(SwingConstants.CENTER);

        if (product.image() != null && product.image().length > 0) {
            ImageIcon icon = UIHelper.imageIconFromBytes(product.image(), 185, 185);
            if (icon != null) {
                imgLabel.setIcon(icon);
                imgLabel.setToolTipText("Fotografia oficial do artigo");
                imgLabel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                imgLabel.addMouseListener(new java.awt.event.MouseAdapter() {
                    @Override
                    public void mouseClicked(java.awt.event.MouseEvent e) {
                        openFullImageModal();
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

    private void renderPlaceholderImage(JLabel label) {
        label.setIcon(UIHelper.icon("fas-box-open", 54, UIHelper.TEXT_MUTED));
        label.setText("Sem fotografia");
        label.setHorizontalTextPosition(SwingConstants.CENTER);
        label.setVerticalTextPosition(SwingConstants.BOTTOM);
        label.setForeground(UIHelper.TEXT_MUTED);
        label.setFont(new Font(UIHelper.FONT, Font.PLAIN, 12));
    }

    private void openFullImageModal() {
        if (product.image() == null || product.image().length == 0) return;
        ImageIcon fullIcon = UIHelper.imageIconFromBytes(product.image(), 480, 480);
        if (fullIcon == null) return;

        JDialog viewDlg = new JDialog(dialog, product.name() + " — Fotografia", Dialog.ModalityType.APPLICATION_MODAL);
        viewDlg.getContentPane().setBackground(UIHelper.BG_DARK);
        JLabel fullLabel = new JLabel(fullIcon);
        fullLabel.setBorder(new EmptyBorder(20, 20, 20, 20));
        viewDlg.add(fullLabel);
        viewDlg.pack();
        viewDlg.setLocationRelativeTo(dialog);
        viewDlg.setVisible(true);
    }

    private JComponent buildKpiGrid() {
        JPanel grid = KpiCard.createGrid(2, 10, 10);

        // 1. Preço de Venda
        BigDecimal sellPrice = product.unitPrice() != null ? product.unitPrice() : BigDecimal.ZERO;
        String vatInfo = product.taxRateLabel() != null ? product.taxRateLabel() : "IVA Geral";
        grid.add(KpiCard.createCard("Preço de Venda", formatMoney(sellPrice), vatInfo, "fas-tag", UIHelper.APPROVED_GREEN));

        // 2. Stock Consolidado
        BigDecimal totalQty = computeTotalStockQuantity();
        int unitsPerBox = product.unitsPerBox() > 0 ? product.unitsPerBox() : 1;
        String boxInfo = mz.multicore.erp.architecture.quantity.PackagingQuantity.label(totalQty,
                mz.multicore.erp.architecture.quantity.PackagingComposition.of(
                        product.packagesPerBox(), product.unitsPerPackage()))
                + " (" + unitsPerBox + " un/cx)";
        grid.add(KpiCard.createCard("Stock Disponível", String.format("%,.0f un", totalQty.doubleValue()), boxInfo, "fas-boxes", UIHelper.ACCENT_BLUE));

        // 3. Preço de Custo & Margem
        BigDecimal costPrice = product.purchasePrice() != null ? product.purchasePrice() : BigDecimal.ZERO;
        String marginText = "—";
        if (sellPrice.signum() > 0 && costPrice.signum() > 0) {
            BigDecimal margin = sellPrice.subtract(costPrice).divide(sellPrice, 4, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100));
            marginText = String.format("Margem: %.1f%%", margin.doubleValue());
        }
        grid.add(KpiCard.createCard("Preço de Custo", formatMoney(costPrice), marginText, "fas-shopping-cart", UIHelper.PENDING_YELLOW));

        // 4. Preço ao Grosso
        if (product.wholesalePrice() != null && product.wholesalePrice().signum() > 0) {
            String qtyMin = product.wholesaleMinQty() != null ? "A partir de " + product.wholesaleMinQty() + " un" : "Preço de revenda";
            grid.add(KpiCard.createCard("Preço Grosso", formatMoney(product.wholesalePrice()), qtyMin, "fas-layer-group", UIHelper.ACCENT_CYAN));
        } else {
            grid.add(KpiCard.createCard("Preço Grosso", "Não activo", "Venda exclusiva a retalho", "fas-layer-group", UIHelper.TEXT_MUTED));
        }

        return grid;
    }



    private JComponent buildTabsSection() {
        JTabbedPane tabs = new JTabbedPane();
        UIHelper.styleTabbedPaneMulticore(tabs);

        tabs.addTab("Especificações & Logística", UIHelper.icon("fas-info-circle", 15, UIHelper.ACCENT_BLUE), buildSpecsTab());
        tabs.addTab("Saldos por Armazém",        UIHelper.icon("fas-warehouse", 15, UIHelper.ACCENT_CYAN), buildWarehousesTab());
        tabs.addTab("Lotes & Validades",         UIHelper.icon("fas-calendar-times", 15, UIHelper.ACCENT_ORANGE), buildBatchesTab());

        return tabs;
    }

    private JPanel buildSpecsTab() {
        JPanel p = new JPanel(new GridLayout(0, 2, 16, 8));
        p.setOpaque(false);
        p.setBorder(new EmptyBorder(12, 12, 12, 12));

        p.add(specRow("Tipo de Venda:", "WEIGHT".equalsIgnoreCase(product.saleType()) ? "Venda ao Peso (Balança / Kg)" : "Venda por Unidade"));
        p.add(specRow("Rastreio de Stock:", product.stockTracked() ? "Activo (Controlo rigoroso)" : "Inactivo (Serviço/Sem stock)"));
        p.add(specRow("Stock Mínimo de Alerta:", product.minStock() != null ? product.minStock() + " unidades" : "0"));
        p.add(specRow("Embalagens por Caixa:", String.valueOf(product.packagesPerBox())));
        p.add(specRow("Unidades por Embalagem:", String.valueOf(product.unitsPerPackage())));
        p.add(specRow("Total de Unidades por Caixa:", String.valueOf(product.unitsPerBox())));
        p.add(specRow("Peso Líquido Unitário:", product.netUnitWeightKg() != null ? product.netUnitWeightKg() + " kg" : "—"));
        p.add(specRow("Peso Bruto Unitário:", product.grossUnitWeightKg() != null ? product.grossUnitWeightKg() + " kg" : "—"));
        p.add(specRow("Peso Total da Caixa:", product.grossBoxWeightKg().signum() > 0 ? product.grossBoxWeightKg() + " kg" : "—"));
        p.add(specRow("Descrição / Notas:", product.description() != null && !product.description().isBlank() ? product.description() : "Sem notas adicionais."));

        JPanel wrap = new JPanel(new BorderLayout());
        wrap.setOpaque(false);
        wrap.add(p, BorderLayout.NORTH);
        return wrap;
    }

    private JPanel buildWarehousesTab() {
        String[] cols = {"Armazém", "Unidades em Stock", "Caixas Calculadas", "Stock Mínimo", "Estado"};
        DefaultTableModel model = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };

        if (owner != null && owner.stocksList != null) {
            for (StockDTO s : owner.stocksList) {
                if (s.productId() != null && s.productId().equals(product.id())) {
                    BigDecimal qty = s.quantity() != null ? s.quantity() : BigDecimal.ZERO;
                    int upb = s.unitsPerBox() > 0 ? s.unitsPerBox() : (product.unitsPerBox() > 0 ? product.unitsPerBox() : 1);
                    long boxes = qty.divide(BigDecimal.valueOf(upb), 0, RoundingMode.DOWN).longValue();
                    String st = qty.signum() <= 0 ? "Esgotado" : (qty.compareTo(s.minStock() != null ? s.minStock() : BigDecimal.ZERO) < 0 ? "Baixo" : "Disponível");
                    model.addRow(new Object[]{s.warehouseName(), qty, boxes, s.minStock(), st});
                }
            }
        }

        JTable table = new JTable(model);
        UIHelper.styleTable(table);
        table.getColumnModel().getColumn(4).setCellRenderer(TableCellRenderers.status());

        JPanel p = new JPanel(new BorderLayout());
        p.setOpaque(false);
        p.setBorder(new EmptyBorder(8, 6, 6, 6));
        JScrollPane scroll = new JScrollPane(table);
        UIHelper.styleScrollPane(scroll);
        p.add(scroll, BorderLayout.CENTER);
        return p;
    }

    private JPanel buildBatchesTab() {
        String[] cols = {"Lote", "Armazém", "Validade", "Dias Restantes", "Quantidade", "Estado"};
        DefaultTableModel model = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };

        if (owner != null && owner.batchesList != null) {
            List<ProductBatchDTO> batches = owner.batchesList;
            if (batches != null) {
                LocalDate today = LocalDate.now();
                for (ProductBatchDTO b : batches) {
                    if (b.productId() != null && b.productId().equals(product.id())) {
                        String expStr = b.expirationDate() != null ? b.expirationDate().format(DATE_FMT) : "—";
                        String daysStr = "—";
                        String state = "Válido";
                        if (b.expirationDate() != null) {
                            long days = ChronoUnit.DAYS.between(today, b.expirationDate());
                            daysStr = days + " d";
                            if (days < 0) state = "Expirado";
                            else if (days <= 30) state = "Crítico";
                        }
                        model.addRow(new Object[]{b.batchNumber(), b.warehouseName(), expStr, daysStr, b.quantity(), state});
                    }
                }
            }
        }

        JTable table = new JTable(model);
        UIHelper.styleTable(table);
        table.getColumnModel().getColumn(5).setCellRenderer(TableCellRenderers.status());

        JPanel p = new JPanel(new BorderLayout());
        p.setOpaque(false);
        p.setBorder(new EmptyBorder(8, 6, 6, 6));
        JScrollPane scroll = new JScrollPane(table);
        UIHelper.styleScrollPane(scroll);
        p.add(scroll, BorderLayout.CENTER);
        return p;
    }

    private JPanel specRow(String key, String value) {
        JPanel r = new JPanel(new BorderLayout(8, 0));
        r.setOpaque(false);

        JLabel k = new JLabel(key);
        k.setFont(new Font(UIHelper.FONT, Font.BOLD, 12));
        k.setForeground(UIHelper.TEXT_MUTED);

        JLabel v = new JLabel(value);
        v.setFont(new Font(UIHelper.FONT, Font.PLAIN, 12));
        v.setForeground(UIHelper.TEXT_LIGHT);

        r.add(k, BorderLayout.WEST);
        r.add(v, BorderLayout.CENTER);
        return r;
    }

    private JPanel buildFooter() {
        JPanel footer = new JPanel(new BorderLayout());
        footer.setOpaque(false);
        footer.setBorder(new EmptyBorder(10, 0, 0, 0));

        JPanel leftActions = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        leftActions.setOpaque(false);

        ModernButton editBtn = UIHelper.createSecondaryButton("Editar Artigo");
        editBtn.setIcon(UIHelper.icon("fas-edit", 14, UIHelper.PENDING_YELLOW));
        editBtn.addActionListener(e -> {
            dialog.dispose();
            owner.editProductDialog(product.id());
        });
        leftActions.add(editBtn);

        ModernButton labelBtn = UIHelper.createSecondaryButton("Gerar Etiqueta");
        labelBtn.setIcon(UIHelper.icon("fas-barcode", 14, UIHelper.ACCENT));
        labelBtn.addActionListener(e -> {
            dialog.dispose();
            owner.openLabelDialog();
        });
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

    private static String formatMoney(BigDecimal amount) {
        if (amount == null) return "0,00 MT";
        return String.format(java.util.Locale.forLanguageTag("pt-MZ"), "%,.2f MT", amount);
    }
}
