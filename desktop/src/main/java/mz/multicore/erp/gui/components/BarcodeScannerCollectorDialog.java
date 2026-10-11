package mz.multicore.erp.gui.components;

import mz.multicore.erp.desktop.client.ComercialApiClient;
import mz.multicore.erp.desktop.client.InventoryApiClient;
import mz.multicore.erp.desktop.client.ProductionApiClient;
import mz.multicore.erp.modules.comercial.dto.ProductDTO;
import mz.multicore.erp.modules.manufacturing.dto.BillOfMaterialsDTO;
import mz.multicore.erp.modules.manufacturing.dto.CreateProductionOrderRequest;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Diálogo interativo para leitura ágil via scanner de código de barras / leitor óptico USB ou Bluetooth.
 * Opera nos modos CHÃO DE FÁBRICA (apontamento e ordem rápida) e ARMAZÉM (auditoria e conferência física).
 */
public class BarcodeScannerCollectorDialog {

    public enum CollectorMode {
        MANUFACTURING("Chão de Fábrica & Fabrico Industrial", "fas-industry"),
        INVENTORY("Armazém & Auditoria de Stocks", "fas-boxes");

        final String title;
        final String icon;

        CollectorMode(String title, String icon) {
            this.title = title;
            this.icon = icon;
        }
    }

    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm:ss");

    private final Window parent;
    private final CollectorMode mode;
    private final ComercialApiClient comercialApiClient;
    private final ProductionApiClient productionApiClient;
    private final InventoryApiClient inventoryApiClient;
    private final Runnable onActionCompleted;

    private JDialog dialog;
    private JTextField scanField;
    private final InlineFeedbackPanel feedback = new InlineFeedbackPanel();

    // Painel de Detalhe do Produto
    private JLabel lblProductName;
    private JLabel lblProductSku;
    private JLabel lblProductBarcode;
    private JLabel lblProductDetails;
    private JLabel lblBomStatus;
    private JSpinner spinQuantity;
    private ModernButton btnAction;

    // Histórico de Leituras
    private DefaultTableModel historyModel;
    private JTable historyTable;

    private ProductDTO lastScannedProduct;
    private BillOfMaterialsDTO matchedBom;

    public BarcodeScannerCollectorDialog(
            Window parent,
            CollectorMode mode,
            ComercialApiClient comercialApiClient,
            ProductionApiClient productionApiClient,
            InventoryApiClient inventoryApiClient,
            Runnable onActionCompleted
    ) {
        this.parent = parent;
        this.mode = mode;
        this.comercialApiClient = comercialApiClient;
        this.productionApiClient = productionApiClient;
        this.inventoryApiClient = inventoryApiClient;
        this.onActionCompleted = onActionCompleted;
    }

    public void showDialog() {
        dialog = new JDialog(parent, "Modo Coletor — " + mode.title, Dialog.ModalityType.APPLICATION_MODAL);
        dialog.getContentPane().setBackground(UIHelper.BG_DARK);
        dialog.setIconImage(UIHelper.iconImage("fas-barcode", 24, UIHelper.ACCENT_BLUE));
        dialog.setSize(840, 600);
        dialog.setMinimumSize(new Dimension(740, 500));
        dialog.setLocationRelativeTo(parent);

        JPanel main = new JPanel(new BorderLayout(0, 12));
        main.setOpaque(false);
        main.setBorder(new EmptyBorder(16, 18, 16, 18));

        // Cabeçalho
        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        JLabel titleLabel = UIHelper.createHeading("Modo Coletor — " + mode.title);
        titleLabel.setIcon(UIHelper.icon(mode.icon, 20, UIHelper.ACCENT_BLUE));
        header.add(titleLabel, BorderLayout.WEST);

        JLabel tip = new JLabel("Aponte o scanner laser/óptico para o código de barras ou prima Enter");
        tip.setFont(new Font(UIHelper.FONT, Font.PLAIN, 12));
        tip.setForeground(UIHelper.TEXT_MUTED);
        header.add(tip, BorderLayout.SOUTH);
        main.add(header, BorderLayout.NORTH);

        // Centro: Campo de Leitura + Detalhes + Histórico
        JPanel center = new JPanel(new BorderLayout(0, 10));
        center.setOpaque(false);

        center.add(buildScanBar(), BorderLayout.NORTH);

        JPanel contentSplit = new JPanel(new GridLayout(1, 2, 12, 0));
        contentSplit.setOpaque(false);
        contentSplit.add(buildDetailsCard());
        contentSplit.add(buildHistoryCard());

        center.add(contentSplit, BorderLayout.CENTER);
        main.add(center, BorderLayout.CENTER);

        // Rodapé com fechar
        JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        footer.setOpaque(false);
        ModernButton closeBtn = UIHelper.createSecondaryButton("Fechar Coletor");
        closeBtn.addActionListener(e -> dialog.dispose());
        footer.add(closeBtn);
        main.add(footer, BorderLayout.SOUTH);

        dialog.getContentPane().add(main);

        // Foco automático imediato no campo de scan
        SwingUtilities.invokeLater(() -> scanField.requestFocusInWindow());

        dialog.setVisible(true);
    }

    private JPanel buildScanBar() {
        ModernPanel box = new ModernPanel(12);
        box.setLayout(new BorderLayout(8, 6));
        box.setBorder(new EmptyBorder(10, 14, 10, 14));

        JLabel icon = new JLabel(UIHelper.icon("fas-barcode", 24, UIHelper.ACCENT_BLUE));

        scanField = new JTextField();
        UIHelper.styleTextField(scanField);
        scanField.setFont(new Font(UIHelper.FONT, Font.BOLD, 16));
        scanField.putClientProperty("JTextField.placeholderText", "Ler código de barras ou digitar SKU e premir Enter...");

        scanField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    processScan(scanField.getText().trim());
                }
            }
        });

        ModernButton searchBtn = UIHelper.createPrimaryButton("Processar");
        searchBtn.setIcon(UIHelper.icon("fas-search", 14));
        searchBtn.addActionListener(e -> processScan(scanField.getText().trim()));

        JPanel inputRow = new JPanel(new BorderLayout(8, 0));
        inputRow.setOpaque(false);
        inputRow.add(icon, BorderLayout.WEST);
        inputRow.add(scanField, BorderLayout.CENTER);
        inputRow.add(searchBtn, BorderLayout.EAST);

        box.add(inputRow, BorderLayout.CENTER);
        box.add(feedback, BorderLayout.SOUTH);
        return box;
    }

    private JPanel buildDetailsCard() {
        ModernPanel card = new ModernPanel(14);
        card.setLayout(new BorderLayout(0, 10));
        card.setBorder(new EmptyBorder(12, 14, 12, 14));

        JLabel cardTitle = new JLabel("Artigo Identificado");
        cardTitle.setFont(new Font(UIHelper.FONT, Font.BOLD, 13));
        cardTitle.setForeground(UIHelper.TEXT_LIGHT);
        card.add(cardTitle, BorderLayout.NORTH);

        JPanel details = new JPanel();
        details.setOpaque(false);
        details.setLayout(new BoxLayout(details, BoxLayout.Y_AXIS));

        lblProductName = new JLabel("Aguardando leitura de código...");
        lblProductName.setFont(new Font(UIHelper.FONT, Font.BOLD, 15));
        lblProductName.setForeground(UIHelper.TEXT_LIGHT);

        lblProductSku = new JLabel("SKU: —");
        lblProductSku.setFont(new Font(UIHelper.FONT, Font.PLAIN, 12));
        lblProductSku.setForeground(UIHelper.TEXT_MUTED);

        lblProductBarcode = new JLabel("Código de Barras: —");
        lblProductBarcode.setFont(new Font(UIHelper.FONT, Font.PLAIN, 12));
        lblProductBarcode.setForeground(UIHelper.TEXT_MUTED);

        lblProductDetails = new JLabel("Preço / Categoria: —");
        lblProductDetails.setFont(new Font(UIHelper.FONT, Font.PLAIN, 12));
        lblProductDetails.setForeground(UIHelper.TEXT_MUTED);

        lblBomStatus = new JLabel(" ");
        lblBomStatus.setFont(new Font(UIHelper.FONT, Font.BOLD, 12));

        details.add(lblProductName);
        details.add(Box.createVerticalStrut(4));
        details.add(lblProductSku);
        details.add(lblProductBarcode);
        details.add(lblProductDetails);
        details.add(Box.createVerticalStrut(6));
        details.add(lblBomStatus);

        JPanel actionRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        actionRow.setOpaque(false);
        actionRow.setBorder(new EmptyBorder(8, 0, 0, 0));

        spinQuantity = new JSpinner(new SpinnerNumberModel(1, 1, 100000, 1));
        spinQuantity.setPreferredSize(new Dimension(80, UIHelper.FORM_CONTROL_HEIGHT));
        UIHelper.styleSpinner(spinQuantity);

        btnAction = UIHelper.createSuccessButton(mode == CollectorMode.MANUFACTURING ? "Produzir Lote" : "Confirmar Leitura");
        btnAction.setIcon(UIHelper.icon(mode == CollectorMode.MANUFACTURING ? "fas-industry" : "fas-check", 14));
        btnAction.setEnabled(false);
        btnAction.addActionListener(e -> executeAction());

        actionRow.add(new JLabel("Qtd:"));
        actionRow.add(spinQuantity);
        actionRow.add(btnAction);

        details.add(Box.createVerticalStrut(8));
        details.add(actionRow);

        card.add(details, BorderLayout.CENTER);
        return card;
    }

    private JPanel buildHistoryCard() {
        ModernPanel card = new ModernPanel(14);
        card.setLayout(new BorderLayout(0, 8));
        card.setBorder(new EmptyBorder(12, 14, 12, 14));

        JLabel cardTitle = new JLabel("Sessão Actual de Leituras");
        cardTitle.setFont(new Font(UIHelper.FONT, Font.BOLD, 13));
        cardTitle.setForeground(UIHelper.TEXT_LIGHT);
        card.add(cardTitle, BorderLayout.NORTH);

        String[] cols = {"Hora", "Código / SKU", "Produto", "Qtd"};
        historyModel = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int r, int c) { return false; }
        };
        historyTable = new JTable(historyModel);
        UIHelper.styleTable(historyTable);

        JScrollPane scroll = new JScrollPane(historyTable);
        UIHelper.styleScrollPane(scroll);
        card.add(scroll, BorderLayout.CENTER);

        return card;
    }

    private void processScan(String code) {
        feedback.clear();
        if (code == null || code.isBlank()) {
            feedback.show(FeedbackType.WARNING, "Introduza ou digitalize um código de barras.");
            scanField.requestFocusInWindow();
            return;
        }

        SwingWorker<ProductDTO, Void> worker = new SwingWorker<>() {
            @Override
            protected ProductDTO doInBackground() {
                try {
                    ProductDTO p = comercialApiClient.findProductByBarcode(code);
                    if (p != null) return p;
                } catch (Exception ignored) {}

                // Fallback: pesquisar por SKU se não bater por código de barras
                try {
                    List<ProductDTO> all = comercialApiClient.getAllProducts();
                    for (ProductDTO p : all) {
                        if (code.equalsIgnoreCase(p.sku()) || code.equalsIgnoreCase(p.barcode())) {
                            return p;
                        }
                    }
                } catch (Exception ignored) {}
                return null;
            }

            @Override
            protected void done() {
                try {
                    ProductDTO product = get();
                    if (product != null) {
                        displayProduct(product, code);
                    } else {
                        feedback.show(FeedbackType.ERROR, "Artigo não encontrado para o código: " + code);
                        clearDisplay();
                    }
                } catch (Exception ex) {
                    feedback.show(FeedbackType.ERROR, "Erro ao pesquisar: " + ex.getMessage());
                } finally {
                    scanField.setText("");
                    scanField.requestFocusInWindow();
                }
            }
        };
        worker.execute();
    }

    private void displayProduct(ProductDTO p, String scannedCode) {
        this.lastScannedProduct = p;
        lblProductName.setText(p.name());
        lblProductSku.setText("SKU: " + (p.sku() != null ? p.sku() : "—"));
        lblProductBarcode.setText("Código de Barras: " + (p.barcode() != null ? p.barcode() : scannedCode));
        lblProductDetails.setText("Preço Venda: " + (p.unitPrice() != null ? p.unitPrice() + " MT" : "N/D") + " | Unidade: " + p.saleType());

        if (mode == CollectorMode.MANUFACTURING) {
            checkBomAndEnable(p);
        } else {
            lblBomStatus.setText("Artigo verificado com sucesso.");
            lblBomStatus.setForeground(UIHelper.APPROVED_GREEN);
            btnAction.setEnabled(true);
        }

        // Adicionar ao histórico de leituras
        int qty = (Integer) spinQuantity.getValue();
        historyModel.insertRow(0, new Object[]{
                LocalTime.now().format(TIME_FMT),
                scannedCode,
                p.name(),
                qty
        });
    }

    private void checkBomAndEnable(ProductDTO p) {
        lblBomStatus.setText("A verificar Ficha Técnica (BOM)...");
        lblBomStatus.setForeground(UIHelper.PENDING_YELLOW);
        btnAction.setEnabled(false);

        SwingWorker<BillOfMaterialsDTO, Void> worker = new SwingWorker<>() {
            @Override
            protected BillOfMaterialsDTO doInBackground() {
                try {
                    List<BillOfMaterialsDTO> boms = productionApiClient.listBoms();
                    for (BillOfMaterialsDTO b : boms) {
                        if (b.finishedProductId().equals(p.id())) {
                            return b;
                        }
                    }
                } catch (Exception ignored) {}
                return null;
            }

            @Override
            protected void done() {
                try {
                    matchedBom = get();
                    if (matchedBom != null) {
                        lblBomStatus.setText("Ficha Técnica Activa: " + matchedBom.bomCode() + " (" + (matchedBom.items() != null ? matchedBom.items().size() : 0) + " itens)");
                        lblBomStatus.setForeground(UIHelper.APPROVED_GREEN);
                        btnAction.setEnabled(true);
                    } else {
                        lblBomStatus.setText("Sem Ficha Técnica (BOM) associada a este produto.");
                        lblBomStatus.setForeground(UIHelper.REJECTED_RED);
                        btnAction.setEnabled(false);
                    }
                } catch (Exception ex) {
                    lblBomStatus.setText("Falha ao verificar BOM.");
                    lblBomStatus.setForeground(UIHelper.REJECTED_RED);
                }
            }
        };
        worker.execute();
    }

    private void executeAction() {
        if (lastScannedProduct == null) return;
        int qty = (Integer) spinQuantity.getValue();

        if (mode == CollectorMode.MANUFACTURING) {
            if (matchedBom == null) {
                feedback.show(FeedbackType.WARNING, "O produto selecionado não possui fórmula/BOM de fabrico.");
                return;
            }

            btnAction.setEnabled(false);
            SwingWorker<Void, Void> worker = new SwingWorker<>() {
                @Override
                protected Void doInBackground() throws Exception {
                    Long sourceWarehouseId = 1L;
                    Long targetWarehouseId = 1L;
                    if (inventoryApiClient != null) {
                        try {
                            Long companyId = mz.multicore.erp.architecture.security.CurrentUserContext.findCurrentCompanyId();
                            if (companyId != null) {
                                var whs = inventoryApiClient.getWarehousesByCompany(companyId);
                                if (!whs.isEmpty()) {
                                    sourceWarehouseId = whs.get(0).id();
                                    targetWarehouseId = whs.get(0).id();
                                }
                            }
                        } catch (Exception ignored) {}
                    }
                    var req = new CreateProductionOrderRequest(
                            matchedBom.id(),
                            new BigDecimal(qty),
                            sourceWarehouseId,
                            targetWarehouseId,
                            LocalDate.now().plusDays(1)
                    );
                    var order = productionApiClient.createOrder(req);
                    // Iniciar automaticamente
                    productionApiClient.startOrder(order.id());
                    return null;
                }

                @Override
                protected void done() {
                    try {
                        get();
                        feedback.show(FeedbackType.SUCCESS, "Ordem de Fabrico lançada e iniciada com sucesso!");
                        if (onActionCompleted != null) onActionCompleted.run();
                    } catch (Exception ex) {
                        feedback.show(FeedbackType.ERROR, "Falha ao lançar ordem: " + ex.getMessage());
                    } finally {
                        btnAction.setEnabled(true);
                        scanField.requestFocusInWindow();
                    }
                }
            };
            worker.execute();
        } else {
            feedback.show(FeedbackType.SUCCESS, "Leitura de inventário registada (" + qty + " un).");
            if (onActionCompleted != null) onActionCompleted.run();
            scanField.requestFocusInWindow();
        }
    }

    private void clearDisplay() {
        lastScannedProduct = null;
        matchedBom = null;
        lblProductName.setText("Aguardando leitura de código...");
        lblProductSku.setText("SKU: —");
        lblProductBarcode.setText("Código de Barras: —");
        lblProductDetails.setText("Preço / Categoria: —");
        lblBomStatus.setText(" ");
        btnAction.setEnabled(false);
    }
}
