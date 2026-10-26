package mz.multicore.erp.gui.components;

import mz.multicore.erp.modules.comercial.dto.ProductDTO;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
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
            new String[]{"Folha A4 (Grelha 3x8 - 24 por folha)", "Bobina Térmica (80mm)", "Etiquetas Individuais (50x30mm)"}, s -> s);
    private final JComboBox<PackagingLevel> levelSelector = UIHelper.createComboBox(PackagingLevel.values(), PackagingLevel::label);
    private final JSpinner copiesSpinner = new JSpinner(new SpinnerNumberModel(1, 1, 500, 1));
    private final JTextField searchField = TableFilter.searchField("Pesquisar produto...");
    private final JTextField batchField = TableFilter.searchField("Lote / OP (ex.: OP-2026-001)");
    private final JTextField expiryField = TableFilter.searchField("Validade (ex.: 12/2026)");

    public ShelfLabelsDialog(Window owner, List<ProductDTO> products) { this(owner, products, null, 1); }

    public ShelfLabelsDialog(Window owner, List<ProductDTO> products, String defaultBatch, int defaultCopies) {
        super(owner, "Etiquetas de Prateleira e Códigos de Barras", ModalityType.APPLICATION_MODAL);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setMinimumSize(new Dimension(800, 500));
        UIHelper.applyStandardModalBounds(this, 960, 610);
        getContentPane().setBackground(UIHelper.BG_DARK);
        if (products != null) allProducts.addAll(products);
        if (defaultBatch != null && !defaultBatch.isBlank()) batchField.setText(defaultBatch.trim());
        if (defaultCopies > 1) copiesSpinner.setValue(Math.min(defaultCopies, 500));

        JPanel root = new JPanel(new BorderLayout(0, 16));
        root.setBackground(UIHelper.BG_DARK);
        root.setBorder(BorderFactory.createEmptyBorder(18, 20, 18, 20));

        root.add(UIHelper.buildPremiumHeader(
                "fas-barcode", "Gerador de Etiquetas, Lotes e Códigos de Barras",
                "Emissão de etiquetas de prateleira, embalagem multinível e rastreabilidade de lotes de fabrico."
        ), BorderLayout.NORTH);

        JPanel contentGrid = new JPanel(new BorderLayout(16, 0)); contentGrid.setOpaque(false);
        contentGrid.add(buildLeftPanel(), BorderLayout.WEST); contentGrid.add(buildRightPanel(), BorderLayout.CENTER);
        root.add(contentGrid, BorderLayout.CENTER); root.add(buildFooter(), BorderLayout.SOUTH);

        add(root);
        getRootPane().registerKeyboardAction(e -> dispose(), KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_IN_FOCUSED_WINDOW);
        updatePreview();
    }

    private JPanel buildLeftPanel() {
        JPanel leftPanel = new ModernPanel(12, UIHelper.BG_CARD, UIHelper.BG_CARD);
        leftPanel.setLayout(new BorderLayout(0, 8)); leftPanel.setBorder(BorderFactory.createEmptyBorder(10, 12, 10, 12));
        leftPanel.setPreferredSize(new Dimension(320, 0));

        JLabel leftTitle = new JLabel("Artigos Disponíveis (" + allProducts.size() + ")");
        leftTitle.setFont(new Font(UIHelper.FONT, Font.BOLD, 13)); leftTitle.setForeground(UIHelper.TEXT_LIGHT);
        JPanel leftTop = new JPanel(new BorderLayout(0, 6)); leftTop.setOpaque(false);
        leftTop.add(leftTitle, BorderLayout.NORTH); leftTop.add(searchField, BorderLayout.SOUTH);
        leftPanel.add(leftTop, BorderLayout.NORTH);

        populateList("");
        UIHelper.onTextChange(searchField, () -> populateList(searchField.getText()));
        UIHelper.onTextChange(batchField, this::updatePreview);
        UIHelper.onTextChange(expiryField, this::updatePreview);

        productList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        productList.setCellRenderer((list, value, index, isSelected, cellHasFocus) -> {
            JPanel item = new JPanel(new BorderLayout(4, 2)); item.setOpaque(true);
            item.setBackground(isSelected ? UIHelper.ACCENT_BLUE : UIHelper.BG_CARD);
            item.setBorder(BorderFactory.createEmptyBorder(4, 6, 4, 6));

            JLabel nameLbl = new JLabel(value != null ? value.name() : "");
            nameLbl.setFont(new Font(UIHelper.FONT, Font.PLAIN, 12));
            nameLbl.setForeground(isSelected ? Color.WHITE : UIHelper.TEXT_LIGHT);

            String packText = (value != null && (value.packagesPerBox() > 1 || value.unitsPerPackage() > 1))
                    ? "Cx:" + value.unitsPerBox() + " | Emb:" + value.unitsPerPackage() : "";
            JLabel metaLbl = new JLabel(packText);
            metaLbl.setFont(new Font(UIHelper.FONT, Font.PLAIN, 10));
            metaLbl.setForeground(isSelected ? new Color(224, 231, 255) : UIHelper.TEXT_MUTED);

            item.add(nameLbl, BorderLayout.CENTER); item.add(metaLbl, BorderLayout.EAST);
            return item;
        });

        if (!listModel.isEmpty()) productList.setSelectedIndex(0);
        productList.addListSelectionListener(e -> updatePreview());

        JScrollPane leftScroll = new JScrollPane(productList);
        leftScroll.setBorder(BorderFactory.createLineBorder(UIHelper.isLight() ? new Color(226, 232, 240) : new Color(51, 65, 85), 1));
        leftPanel.add(leftScroll, BorderLayout.CENTER);
        return leftPanel;
    }

    private JPanel buildRightPanel() {
        JPanel rightPanel = new ModernPanel(12, UIHelper.BG_CARD, UIHelper.BG_CARD);
        rightPanel.setLayout(new BorderLayout(0, 10)); rightPanel.setBorder(BorderFactory.createEmptyBorder(10, 14, 10, 14));

        JPanel previewHeader = new JPanel(new GridLayout(2, 1, 0, 6)); previewHeader.setOpaque(false);
        JPanel row1 = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0)); row1.setOpaque(false);
        JLabel levelLbl = new JLabel("Nível:"); levelLbl.setFont(new Font(UIHelper.FONT, Font.BOLD, 12)); levelLbl.setForeground(UIHelper.TEXT_MUTED);
        levelSelector.setPreferredSize(new Dimension(170, UIHelper.FORM_CONTROL_HEIGHT)); levelSelector.addActionListener(e -> updatePreview());
        JLabel layoutLbl = new JLabel("Formato:"); layoutLbl.setFont(new Font(UIHelper.FONT, Font.BOLD, 12)); layoutLbl.setForeground(UIHelper.TEXT_MUTED);
        layoutSelector.setPreferredSize(new Dimension(200, UIHelper.FORM_CONTROL_HEIGHT));
        JLabel copiesLbl = new JLabel("Cópias:"); copiesLbl.setFont(new Font(UIHelper.FONT, Font.BOLD, 12)); copiesLbl.setForeground(UIHelper.TEXT_MUTED);
        UIHelper.styleSpinner(copiesSpinner); copiesSpinner.setPreferredSize(new Dimension(65, UIHelper.FORM_CONTROL_HEIGHT));
        row1.add(levelLbl); row1.add(levelSelector); row1.add(layoutLbl); row1.add(layoutSelector); row1.add(copiesLbl); row1.add(copiesSpinner);

        JPanel row2 = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0)); row2.setOpaque(false);
        JLabel batchLbl = new JLabel("Lote/OP:"); batchLbl.setFont(new Font(UIHelper.FONT, Font.BOLD, 12)); batchLbl.setForeground(UIHelper.TEXT_MUTED);
        batchField.setPreferredSize(new Dimension(200, UIHelper.FORM_CONTROL_HEIGHT));
        JLabel expLbl = new JLabel("Validade:"); expLbl.setFont(new Font(UIHelper.FONT, Font.BOLD, 12)); expLbl.setForeground(UIHelper.TEXT_MUTED);
        expiryField.setPreferredSize(new Dimension(160, UIHelper.FORM_CONTROL_HEIGHT));
        row2.add(batchLbl); row2.add(batchField); row2.add(expLbl); row2.add(expiryField);

        previewHeader.add(row1); previewHeader.add(row2);
        rightPanel.add(previewHeader, BorderLayout.NORTH);

        previewContainer.setOpaque(false); previewContainer.setLayout(new FlowLayout(FlowLayout.CENTER, 10, 10));
        JScrollPane rightScroll = new JScrollPane(previewContainer);
        rightScroll.setBorder(BorderFactory.createEmptyBorder()); rightScroll.setOpaque(false); rightScroll.getViewport().setOpaque(false);
        rightPanel.add(rightScroll, BorderLayout.CENTER);
        return rightPanel;
    }

    private JPanel buildFooter() {
        JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0)); footer.setOpaque(false);
        ModernButton cancelBtn = UIHelper.createSecondaryButton("Fechar (ESC)");
        cancelBtn.setIcon(UIHelper.icon("fas-times", 14)); cancelBtn.addActionListener(e -> dispose());
        ModernButton printBtn = UIHelper.createPrimaryButton("Imprimir Etiquetas");
        printBtn.setIcon(UIHelper.icon("fas-print", 14)); printBtn.addActionListener(e -> printLabels());
        footer.add(cancelBtn); footer.add(printBtn);
        return footer;
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
        if (!listModel.isEmpty() && productList.getSelectedIndex() < 0) productList.setSelectedIndex(0);
    }

    private void updatePreview() {
        previewContainer.removeAll();
        List<ProductDTO> selected = productList.getSelectedValuesList();
        if (selected.isEmpty() && !listModel.isEmpty()) selected = List.of(listModel.get(0));

        PackagingLevel level = (PackagingLevel) levelSelector.getSelectedItem();
        if (level == null) level = PackagingLevel.UNIT;
        String batch = batchField.getText().trim();
        String exp = expiryField.getText().trim();

        for (ProductDTO p : selected) {
            previewContainer.add(ShelfLabelCardRenderer.createSingleLabelCard(p, level, batch, exp));
        }
        previewContainer.revalidate(); previewContainer.repaint();
    }

    private void printLabels() {
        PackagingLevel level = (PackagingLevel) levelSelector.getSelectedItem();
        int copies = (Integer) copiesSpinner.getValue();
        ShelfLabelPrintEngine.printLabels(this, allProducts, productList.getSelectedValuesList(),
                level, copies, batchField.getText().trim(), expiryField.getText().trim());
    }

    public static void show(Window owner, List<ProductDTO> products) { showWithBatch(owner, products, null, 1); }

    public static void showWithBatch(Window owner, List<ProductDTO> products, String batchNumber, int defaultCopies) {
        SwingUtilities.invokeLater(() -> new ShelfLabelsDialog(owner, products, batchNumber, defaultCopies).setVisible(true));
    }

    public static JPanel createSingleLabelCard(ProductDTO product) { return ShelfLabelCardRenderer.createSingleLabelCard(product); }

    public static JPanel createSingleLabelCard(ProductDTO product, PackagingLevel level) {
        return ShelfLabelCardRenderer.createSingleLabelCard(product, level);
    }

    public static JPanel createSingleLabelCard(ProductDTO product, PackagingLevel level, String batchNumber, String expiryDate) {
        return ShelfLabelCardRenderer.createSingleLabelCard(product, level, batchNumber, expiryDate);
    }
}
