package mz.multicore.erp.gui.components;

import mz.multicore.erp.modules.comercial.dto.ProductDTO;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultListModel;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.RenderingHints;
import java.awt.Window;
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
 * Diálogo de geração e impressão de etiquetas de prateleira com código de barras e preço em Meticais.
 */
public class ShelfLabelsDialog extends JDialog {

    private final List<ProductDTO> allProducts = new ArrayList<>();
    private final DefaultListModel<ProductDTO> listModel = new DefaultListModel<>();
    private final JList<ProductDTO> productList = new JList<>(listModel);
    private final JPanel previewContainer = new JPanel();
    private final JComboBox<String> layoutSelector = new JComboBox<>(new String[]{"Folha A4 (Grelha 3x8 - 24 por folha)", "Bobina Térmica (80mm)"});

    public ShelfLabelsDialog(Window owner, List<ProductDTO> products) {
        super(owner, "Etiquetas de Prateleira e Códigos de Barras", ModalityType.APPLICATION_MODAL);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setSize(850, 580);
        setMinimumSize(new Dimension(720, 480));
        setLocationRelativeTo(owner);
        getContentPane().setBackground(UIHelper.BG_DARK);

        if (products != null) {
            allProducts.addAll(products);
        }

        JPanel root = new JPanel(new BorderLayout(0, 16));
        root.setBackground(UIHelper.BG_DARK);
        root.setBorder(BorderFactory.createEmptyBorder(18, 20, 18, 20));

        // Header
        root.add(UIHelper.buildPremiumHeader(
                "fas-barcode",
                "Gerador de Etiquetas de Prateleira",
                "Selecione os artigos para pré-visualizar e imprimir etiquetas de preço e código de barras."
        ), BorderLayout.NORTH);

        // Center: Split between Product Selector (Left) and Live Label Preview (Right)
        JPanel contentGrid = new JPanel(new BorderLayout(16, 0));
        contentGrid.setOpaque(false);

        // Left Panel: Product Selection
        JPanel leftPanel = new ModernPanel(12, UIHelper.BG_CARD, UIHelper.BG_CARD);
        leftPanel.setLayout(new BorderLayout(0, 8));
        leftPanel.setBorder(BorderFactory.createEmptyBorder(10, 12, 10, 12));
        leftPanel.setPreferredSize(new Dimension(300, 0));

        JLabel leftTitle = new JLabel("Artigos Selecionados (" + allProducts.size() + ")");
        leftTitle.setFont(new Font(UIHelper.FONT, Font.BOLD, 13));
        leftTitle.setForeground(UIHelper.TEXT_LIGHT);
        leftPanel.add(leftTitle, BorderLayout.NORTH);

        for (ProductDTO p : allProducts) {
            listModel.addElement(p);
        }

        productList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        productList.setCellRenderer((list, value, index, isSelected, cellHasFocus) -> {
            JLabel lbl = new JLabel(value != null ? value.name() + " (" + String.format("%.2f MT", value.unitPrice()) + ")" : "");
            lbl.setFont(new Font(UIHelper.FONT, Font.PLAIN, 12));
            lbl.setForeground(isSelected ? Color.WHITE : UIHelper.TEXT_LIGHT);
            lbl.setOpaque(isSelected);
            lbl.setBackground(isSelected ? UIHelper.ACCENT_BLUE : UIHelper.BG_CARD);
            lbl.setBorder(BorderFactory.createEmptyBorder(4, 6, 4, 6));
            return lbl;
        });

        if (!listModel.isEmpty()) {
            productList.setSelectedIndex(0);
        }

        productList.addListSelectionListener(e -> updatePreview());

        JScrollPane leftScroll = new JScrollPane(productList);
        leftScroll.setBorder(BorderFactory.createLineBorder(UIHelper.isLight() ? new Color(226, 232, 240) : new Color(51, 65, 85), 1));
        leftPanel.add(leftScroll, BorderLayout.CENTER);

        contentGrid.add(leftPanel, BorderLayout.WEST);

        // Right Panel: Live Label Preview
        JPanel rightPanel = new ModernPanel(12, UIHelper.BG_CARD, UIHelper.BG_CARD);
        rightPanel.setLayout(new BorderLayout(0, 10));
        rightPanel.setBorder(BorderFactory.createEmptyBorder(10, 14, 10, 14));

        JPanel previewHeader = new JPanel(new BorderLayout());
        previewHeader.setOpaque(false);
        JLabel rightTitle = new JLabel("Pré-visualização da Etiqueta");
        rightTitle.setFont(new Font(UIHelper.FONT, Font.BOLD, 13));
        rightTitle.setForeground(UIHelper.TEXT_LIGHT);

        UIHelper.styleComboBox(layoutSelector);
        layoutSelector.setPreferredSize(new Dimension(240, 28));

        previewHeader.add(rightTitle, BorderLayout.WEST);
        previewHeader.add(layoutSelector, BorderLayout.EAST);
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

    public static void show(Window owner, List<ProductDTO> products) {
        SwingUtilities.invokeLater(() -> {
            ShelfLabelsDialog dialog = new ShelfLabelsDialog(owner, products);
            dialog.setVisible(true);
        });
    }

    private void updatePreview() {
        previewContainer.removeAll();
        List<ProductDTO> selected = productList.getSelectedValuesList();
        if (selected.isEmpty() && !allProducts.isEmpty()) {
            selected = List.of(allProducts.get(0));
        }

        for (ProductDTO p : selected) {
            previewContainer.add(createSingleLabelCard(p));
        }
        previewContainer.revalidate();
        previewContainer.repaint();
    }

    /** Cria a representação visual de uma etiqueta de prateleira profissional. */
    public static JPanel createSingleLabelCard(ProductDTO product) {
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

        // Top line: Brand and Category
        JPanel topRow = new JPanel(new BorderLayout());
        topRow.setOpaque(false);
        JLabel brand = new JLabel("MULTICORE ERP");
        brand.setFont(new Font("Arial", Font.BOLD, 9));
        brand.setForeground(new Color(100, 116, 139));

        String catName = product.categoryName() != null ? product.categoryName() : "GERAL";
        JLabel cat = new JLabel(catName);
        cat.setFont(new Font("Arial", Font.PLAIN, 9));
        cat.setForeground(new Color(100, 116, 139));
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
        barcodePanel.setPreferredSize(new Dimension(140, 36));
        barcodePanel.setMaximumSize(new Dimension(200, 36));

        centerPanel.add(nameLabel);
        centerPanel.add(Box.createVerticalStrut(4));
        centerPanel.add(barcodePanel);
        card.add(centerPanel, BorderLayout.CENTER);

        // Bottom: Price in bold
        JPanel bottomRow = new JPanel(new BorderLayout());
        bottomRow.setOpaque(false);

        String priceStr = String.format("%,.2f MT", product.unitPrice() != null ? product.unitPrice() : BigDecimal.ZERO);
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
        job.setPrintable((graphics, pageFormat, pageIndex) -> {
            if (pageIndex > 0) return Printable.NO_SUCH_PAGE;
            Graphics2D g2 = (Graphics2D) graphics;
            g2.translate(pageFormat.getImageableX(), pageFormat.getImageableY());

            int x = 10;
            int y = 10;
            List<ProductDTO> selected = productList.getSelectedValuesList();
            if (selected.isEmpty()) selected = allProducts;

            for (ProductDTO p : selected) {
                JPanel card = createSingleLabelCard(p);
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
