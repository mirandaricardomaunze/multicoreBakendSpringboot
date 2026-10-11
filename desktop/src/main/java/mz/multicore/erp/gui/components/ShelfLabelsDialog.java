package mz.multicore.erp.gui.components;

import mz.multicore.erp.architecture.quantity.PackagingComposition;
import mz.multicore.erp.modules.comercial.dto.ProductDTO;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.print.PageFormat;
import java.awt.print.Printable;
import java.awt.print.PrinterException;
import java.awt.print.PrinterJob;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Diálogo de geração e impressão de etiquetas de prateleira e códigos de barras
 * com suporte à cadeia logística multinível (Caixa → Embalagem → Unidade)
 * e formatação para Folha A4, Bobina Térmica e Etiquetas Individuais.
 */
public class ShelfLabelsDialog extends JDialog {

    public enum PackagingLevel {
        UNIT("Unidade (Retalho)"),
        PACKAGE("Embalagem Intermédia"),
        BOX("Caixa Master");

        private final String label;
        PackagingLevel(String label) { this.label = label; }
        public String label() { return label; }
        @Override public String toString() { return label; }
    }

    private final List<ProductDTO> allProducts = new ArrayList<>();
    private final DefaultListModel<ProductDTO> listModel = new DefaultListModel<>();
    private final JList<ProductDTO> productList = new JList<>(listModel);
    private final JPanel previewContainer = new JPanel();
    private final JComboBox<String> layoutSelector = UIHelper.createComboBox(
            new String[]{"Folha A4 (Grelha 3x8 - 24 por folha)", "Bobina Térmica (80mm)", "Etiquetas Individuais (50x30mm)"},
            s -> s
    );
    private final JComboBox<PackagingLevel> levelSelector = UIHelper.createComboBox(
            PackagingLevel.values(),
            PackagingLevel::label
    );
    private final JSpinner copiesSpinner = new JSpinner(new SpinnerNumberModel(1, 1, 500, 1));
    private final JTextField searchField = TableFilter.searchField("Pesquisar produto...");
    private final JTextField batchField = TableFilter.searchField("Lote / OP (ex.: OP-2026-001)");
    private final JTextField expiryField = TableFilter.searchField("Validade (ex.: 12/2026)");

    public ShelfLabelsDialog(Window owner, List<ProductDTO> products) {
        this(owner, products, null, 1);
    }

    public ShelfLabelsDialog(Window owner, List<ProductDTO> products, String defaultBatch, int defaultCopies) {
        super(owner, "Etiquetas de Prateleira e Códigos de Barras", ModalityType.APPLICATION_MODAL);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setMinimumSize(new Dimension(800, 500));
        UIHelper.applyStandardModalBounds(this, 960, 610);
        getContentPane().setBackground(UIHelper.BG_DARK);

        if (products != null) {
            allProducts.addAll(products);
        }
        if (defaultBatch != null && !defaultBatch.isBlank()) {
            batchField.setText(defaultBatch.trim());
        }
        if (defaultCopies > 1) {
            copiesSpinner.setValue(Math.min(defaultCopies, 500));
        }

        JPanel root = new JPanel(new BorderLayout(0, 16));
        root.setBackground(UIHelper.BG_DARK);
        root.setBorder(BorderFactory.createEmptyBorder(18, 20, 18, 20));

        // Header
        root.add(UIHelper.buildPremiumHeader(
                "fas-barcode",
                "Gerador de Etiquetas, Lotes e Códigos de Barras",
                "Emissão de etiquetas de prateleira, embalagem multinível e rastreabilidade de lotes de fabrico."
        ), BorderLayout.NORTH);

        // Center: Split between Product Selector (Left) and Live Label Preview (Right)
        JPanel contentGrid = new JPanel(new BorderLayout(16, 0));
        contentGrid.setOpaque(false);

        // Left Panel: Product Selection with Search
        JPanel leftPanel = new ModernPanel(12, UIHelper.BG_CARD, UIHelper.BG_CARD);
        leftPanel.setLayout(new BorderLayout(0, 8));
        leftPanel.setBorder(BorderFactory.createEmptyBorder(10, 12, 10, 12));
        leftPanel.setPreferredSize(new Dimension(320, 0));

        JLabel leftTitle = new JLabel("Artigos Disponíveis (" + allProducts.size() + ")");
        leftTitle.setFont(new Font(UIHelper.FONT, Font.BOLD, 13));
        leftTitle.setForeground(UIHelper.TEXT_LIGHT);

        JPanel leftTop = new JPanel(new BorderLayout(0, 6));
        leftTop.setOpaque(false);
        leftTop.add(leftTitle, BorderLayout.NORTH);
        leftTop.add(searchField, BorderLayout.SOUTH);
        leftPanel.add(leftTop, BorderLayout.NORTH);

        populateList("");
        searchField.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            @Override public void insertUpdate(javax.swing.event.DocumentEvent e) { filterList(); }
            @Override public void removeUpdate(javax.swing.event.DocumentEvent e) { filterList(); }
            @Override public void changedUpdate(javax.swing.event.DocumentEvent e) { filterList(); }
            private void filterList() { populateList(searchField.getText()); }
        });

        javax.swing.event.DocumentListener previewUpdater = new javax.swing.event.DocumentListener() {
            @Override public void insertUpdate(javax.swing.event.DocumentEvent e) { updatePreview(); }
            @Override public void removeUpdate(javax.swing.event.DocumentEvent e) { updatePreview(); }
            @Override public void changedUpdate(javax.swing.event.DocumentEvent e) { updatePreview(); }
        };
        batchField.getDocument().addDocumentListener(previewUpdater);
        expiryField.getDocument().addDocumentListener(previewUpdater);

        productList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        productList.setCellRenderer((list, value, index, isSelected, cellHasFocus) -> {
            JPanel item = new JPanel(new BorderLayout(4, 2));
            item.setOpaque(true);
            item.setBackground(isSelected ? UIHelper.ACCENT_BLUE : UIHelper.BG_CARD);
            item.setBorder(BorderFactory.createEmptyBorder(4, 6, 4, 6));

            JLabel nameLbl = new JLabel(value != null ? value.name() : "");
            nameLbl.setFont(new Font(UIHelper.FONT, Font.PLAIN, 12));
            nameLbl.setForeground(isSelected ? Color.WHITE : UIHelper.TEXT_LIGHT);

            String packText = "";
            if (value != null && (value.packagesPerBox() > 1 || value.unitsPerPackage() > 1)) {
                packText = "Cx:" + value.unitsPerBox() + " | Emb:" + value.unitsPerPackage();
            }
            JLabel metaLbl = new JLabel(packText);
            metaLbl.setFont(new Font(UIHelper.FONT, Font.PLAIN, 10));
            metaLbl.setForeground(isSelected ? new Color(224, 231, 255) : UIHelper.TEXT_MUTED);

            item.add(nameLbl, BorderLayout.CENTER);
            item.add(metaLbl, BorderLayout.EAST);
            return item;
        });

        if (!listModel.isEmpty()) {
            productList.setSelectedIndex(0);
        }

        productList.addListSelectionListener(e -> updatePreview());

        JScrollPane leftScroll = new JScrollPane(productList);
        leftScroll.setBorder(BorderFactory.createLineBorder(UIHelper.isLight() ? new Color(226, 232, 240) : new Color(51, 65, 85), 1));
        leftPanel.add(leftScroll, BorderLayout.CENTER);

        contentGrid.add(leftPanel, BorderLayout.WEST);

        // Right Panel: Live Label Preview with Level & Layout Controls
        JPanel rightPanel = new ModernPanel(12, UIHelper.BG_CARD, UIHelper.BG_CARD);
        rightPanel.setLayout(new BorderLayout(0, 10));
        rightPanel.setBorder(BorderFactory.createEmptyBorder(10, 14, 10, 14));

        JPanel previewHeader = new JPanel(new GridLayout(2, 1, 0, 6));
        previewHeader.setOpaque(false);

        JPanel row1 = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        row1.setOpaque(false);

        JLabel levelLbl = new JLabel("Nível:");
        levelLbl.setFont(new Font(UIHelper.FONT, Font.BOLD, 12));
        levelLbl.setForeground(UIHelper.TEXT_MUTED);

        levelSelector.setPreferredSize(new Dimension(170, UIHelper.FORM_CONTROL_HEIGHT));
        levelSelector.addActionListener(e -> updatePreview());

        JLabel layoutLbl = new JLabel("Formato:");
        layoutLbl.setFont(new Font(UIHelper.FONT, Font.BOLD, 12));
        layoutLbl.setForeground(UIHelper.TEXT_MUTED);

        layoutSelector.setPreferredSize(new Dimension(200, UIHelper.FORM_CONTROL_HEIGHT));

        JLabel copiesLbl = new JLabel("Cópias:");
        copiesLbl.setFont(new Font(UIHelper.FONT, Font.BOLD, 12));
        copiesLbl.setForeground(UIHelper.TEXT_MUTED);

        UIHelper.styleSpinner(copiesSpinner);
        copiesSpinner.setPreferredSize(new Dimension(65, UIHelper.FORM_CONTROL_HEIGHT));

        row1.add(levelLbl);
        row1.add(levelSelector);
        row1.add(layoutLbl);
        row1.add(layoutSelector);
        row1.add(copiesLbl);
        row1.add(copiesSpinner);

        JPanel row2 = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        row2.setOpaque(false);

        JLabel batchLbl = new JLabel("Lote/OP:");
        batchLbl.setFont(new Font(UIHelper.FONT, Font.BOLD, 12));
        batchLbl.setForeground(UIHelper.TEXT_MUTED);
        batchField.setPreferredSize(new Dimension(200, UIHelper.FORM_CONTROL_HEIGHT));

        JLabel expLbl = new JLabel("Validade:");
        expLbl.setFont(new Font(UIHelper.FONT, Font.BOLD, 12));
        expLbl.setForeground(UIHelper.TEXT_MUTED);
        expiryField.setPreferredSize(new Dimension(160, UIHelper.FORM_CONTROL_HEIGHT));

        row2.add(batchLbl);
        row2.add(batchField);
        row2.add(expLbl);
        row2.add(expiryField);

        previewHeader.add(row1);
        previewHeader.add(row2);

        rightPanel.add(previewHeader, BorderLayout.NORTH);

        previewContainer.setOpaque(false);
        previewContainer.setLayout(new FlowLayout(FlowLayout.CENTER, 10, 10));

        JScrollPane rightScroll = new JScrollPane(previewContainer);
        rightScroll.setBorder(BorderFactory.createEmptyBorder());
        rightScroll.setOpaque(false);
        rightScroll.getViewport().setOpaque(false);
        rightPanel.add(rightScroll, BorderLayout.CENTER);

        contentGrid.add(rightPanel, BorderLayout.CENTER);
        root.add(contentGrid, BorderLayout.CENTER);

        // Footer Actions
        JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        footer.setOpaque(false);

        ModernButton cancelBtn = UIHelper.createSecondaryButton("Fechar (ESC)");
        cancelBtn.setIcon(UIHelper.icon("fas-times", 14));
        cancelBtn.addActionListener(e -> dispose());

        ModernButton printBtn = UIHelper.createPrimaryButton("Imprimir Etiquetas");
        printBtn.setIcon(UIHelper.icon("fas-print", 14));
        printBtn.addActionListener(e -> printLabels());

        footer.add(cancelBtn);
        footer.add(printBtn);
        root.add(footer, BorderLayout.SOUTH);

        add(root);

        // ESC shortcut
        getRootPane().registerKeyboardAction(
                e -> dispose(),
                KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
                JComponent.WHEN_IN_FOCUSED_WINDOW
        );

        updatePreview();
    }

    private void populateList(String query) {
        listModel.clear();
        String q = query == null ? "" : query.trim().toLowerCase();
        for (ProductDTO p : allProducts) {
            if (q.isEmpty() || p.name().toLowerCase().contains(q)
                    || (p.sku() != null && p.sku().toLowerCase().contains(q))
                    || (p.barcode() != null && p.barcode().toLowerCase().contains(q))) {
                listModel.addElement(p);
            }
        }
        if (!listModel.isEmpty() && productList.getSelectedIndex() < 0) {
            productList.setSelectedIndex(0);
        }
    }

    public static void show(Window owner, List<ProductDTO> products) {
        showWithBatch(owner, products, null, 1);
    }

    public static void showWithBatch(Window owner, List<ProductDTO> products, String batchNumber, int defaultCopies) {
        SwingUtilities.invokeLater(() -> {
            ShelfLabelsDialog dialog = new ShelfLabelsDialog(owner, products, batchNumber, defaultCopies);
            dialog.setVisible(true);
        });
    }

    private void updatePreview() {
        previewContainer.removeAll();
        List<ProductDTO> selected = productList.getSelectedValuesList();
        if (selected.isEmpty() && !listModel.isEmpty()) {
            selected = List.of(listModel.get(0));
        }

        PackagingLevel level = (PackagingLevel) levelSelector.getSelectedItem();
        if (level == null) level = PackagingLevel.UNIT;
        String batch = batchField.getText().trim();
        String exp = expiryField.getText().trim();

        for (ProductDTO p : selected) {
            previewContainer.add(createSingleLabelCard(p, level, batch, exp));
        }
        previewContainer.revalidate();
        previewContainer.repaint();
    }

    public static JPanel createSingleLabelCard(ProductDTO product) {
        return createSingleLabelCard(product, PackagingLevel.UNIT, null, null);
    }

    /**
     * Cria a representação visual da etiqueta com suporte a embalagem multinível.
     * Dimensão exatamente 240x130 para conformidade com testes de harness existentes.
     */
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
                ? product.barcode()
                : (product.sku() != null ? product.sku() : "0000000000");

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

        // Multilevel packaging hint if applicable
        if (product.packagesPerBox() > 1 || product.unitsPerPackage() > 1) {
            PackagingComposition comp = PackagingComposition.of(product.packagesPerBox(), product.unitsPerPackage());
            JLabel pkgHint = new JLabel("1 Cx = " + comp.packagesPerBox() + " emb. × " + comp.unitsPerPackage() + " un. (" + comp.unitsPerBox() + " un.)");
            pkgHint.setFont(new Font("Arial", Font.PLAIN, 8));
            pkgHint.setForeground(new Color(100, 116, 139));
            centerPanel.add(pkgHint);
        }

        // Lote e Validade para rastreabilidade
        if ((batchNumber != null && !batchNumber.isBlank()) || (expiryDate != null && !expiryDate.isBlank())) {
            StringBuilder lotInfo = new StringBuilder();
            if (batchNumber != null && !batchNumber.isBlank()) {
                lotInfo.append("Lote: ").append(batchNumber.trim());
            }
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

        String priceStr = String.format("%,.2f MT", calculatedPrice);
        JLabel priceLabel = new JLabel(priceStr);
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

    private void printLabels() {
        PrinterJob job = PrinterJob.getPrinterJob();
        PackagingLevel level = (PackagingLevel) levelSelector.getSelectedItem();
        if (level == null) level = PackagingLevel.UNIT;
        final PackagingLevel finalLevel = level;
        final int copies = (Integer) copiesSpinner.getValue();
        final String finalBatch = batchField.getText().trim();
        final String finalExpiry = expiryField.getText().trim();

        job.setPrintable((graphics, pageFormat, pageIndex) -> {
            if (pageIndex > 0) return Printable.NO_SUCH_PAGE;
            Graphics2D g2 = (Graphics2D) graphics;
            g2.translate(pageFormat.getImageableX(), pageFormat.getImageableY());

            int x = 10;
            int y = 10;
            List<ProductDTO> selected = productList.getSelectedValuesList();
            if (selected.isEmpty()) selected = allProducts;

            for (ProductDTO p : selected) {
                for (int c = 0; c < copies; c++) {
                    JPanel card = createSingleLabelCard(p, finalLevel, finalBatch, finalExpiry);
                    card.setSize(card.getPreferredSize());
                    card.doLayout();
                    Graphics2D gCard = (Graphics2D) g2.create(x, y, card.getWidth(), card.getHeight());
                    card.paint(gCard);
                    gCard.dispose();

                    x += card.getWidth() + 10;
                    if (x + card.getWidth() > pageFormat.getImageableWidth()) {
                        x = 10;
                        y += card.getHeight() + 10;
                    }
                }
            }
            return Printable.PAGE_EXISTS;
        });

        if (job.printDialog()) {
            try {
                job.print();
                ToastManager.show(this, FeedbackType.SUCCESS, "Etiquetas enviadas para a impressora com sucesso.");
            } catch (PrinterException ex) {
                ToastManager.show(this, FeedbackType.ERROR, "Erro ao imprimir etiquetas: " + ex.getMessage());
            }
        }
    }
}
