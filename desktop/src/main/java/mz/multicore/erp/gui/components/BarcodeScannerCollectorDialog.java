package mz.multicore.erp.gui.components;

import mz.multicore.erp.desktop.client.ComercialApiClient;
import mz.multicore.erp.desktop.client.InventoryApiClient;
import mz.multicore.erp.desktop.client.ProductionApiClient;
import mz.multicore.erp.modules.comercial.dto.ProductDTO;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

/**
 * Diálogo interativo para leitura ágil via scanner de código de barras / leitor óptico USB ou Bluetooth.
 * Opera nos modos CHÃO DE FÁBRICA (apontamento e ordem rápida) e ARMAZÉM (auditoria e conferência física).
 */
public class BarcodeScannerCollectorDialog {

    public enum CollectorMode {
        MANUFACTURING("Chão de Fábrica & Fabrico Industrial", "fas-industry"),
        INVENTORY("Armazém & Auditoria de Stocks", "fas-boxes");

        final String title; final String icon;
        CollectorMode(String title, String icon) { this.title = title; this.icon = icon; }
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
    private BarcodeScannerProductDetailsCard detailsCard;
    private DefaultTableModel historyModel;

    public BarcodeScannerCollectorDialog(
            Window parent, CollectorMode mode,
            ComercialApiClient comercialApiClient, ProductionApiClient productionApiClient,
            InventoryApiClient inventoryApiClient, Runnable onActionCompleted
    ) {
        this.parent = parent; this.mode = mode;
        this.comercialApiClient = comercialApiClient;
        this.productionApiClient = productionApiClient;
        this.inventoryApiClient = inventoryApiClient;
        this.onActionCompleted = onActionCompleted;
    }

    public void showDialog() {
        dialog = new JDialog(parent, "Modo Coletor — " + mode.title, Dialog.ModalityType.APPLICATION_MODAL);
        dialog.getContentPane().setBackground(UIHelper.BG_DARK);
        dialog.setIconImage(UIHelper.iconImage("fas-barcode", 24, UIHelper.ACCENT_BLUE));
        UIHelper.applyStandardModalBounds(dialog, 840, 600);
        dialog.setMinimumSize(new Dimension(740, 500)); dialog.setLocationRelativeTo(parent);

        JPanel main = new JPanel(new BorderLayout(0, 12)); main.setOpaque(false);
        main.setBorder(new EmptyBorder(16, 18, 16, 18));

        JPanel header = new JPanel(new BorderLayout()); header.setOpaque(false);
        JLabel titleLabel = UIHelper.createHeading("Modo Coletor — " + mode.title);
        titleLabel.setIcon(UIHelper.icon(mode.icon, 20, UIHelper.ACCENT_BLUE));
        JLabel tip = new JLabel("Aponte o scanner laser/óptico para o código de barras ou prima Enter");
        tip.setFont(new Font(UIHelper.FONT, Font.PLAIN, 12)); tip.setForeground(UIHelper.TEXT_MUTED);
        header.add(titleLabel, BorderLayout.WEST); header.add(tip, BorderLayout.SOUTH);
        main.add(header, BorderLayout.NORTH);

        JPanel center = new JPanel(new BorderLayout(0, 10)); center.setOpaque(false);
        center.add(buildScanBar(), BorderLayout.NORTH);

        detailsCard = new BarcodeScannerProductDetailsCard(mode, e -> executeAction());
        JPanel contentSplit = new JPanel(new GridLayout(1, 2, 12, 0)); contentSplit.setOpaque(false);
        contentSplit.add(detailsCard.getPanel()); contentSplit.add(buildHistoryCard());
        center.add(contentSplit, BorderLayout.CENTER); main.add(center, BorderLayout.CENTER);

        JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT)); footer.setOpaque(false);
        ModernButton closeBtn = ButtonStyles.secondary(UIHelper.createSecondaryButton("Fechar Coletor"));
        closeBtn.addActionListener(e -> dialog.dispose());
        footer.add(closeBtn); main.add(footer, BorderLayout.SOUTH);

        dialog.getContentPane().add(main);
        dialog.getRootPane().registerKeyboardAction(e -> dialog.dispose(),
                KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_IN_FOCUSED_WINDOW);
        SwingUtilities.invokeLater(() -> scanField.requestFocusInWindow());
        dialog.setVisible(true);
    }

    private JPanel buildScanBar() {
        ModernPanel box = new ModernPanel(12); box.setLayout(new BorderLayout(8, 6)); box.setBorder(new EmptyBorder(10, 14, 10, 14));
        scanField = new JTextField(); UIHelper.styleTextField(scanField);
        scanField.setFont(new Font(UIHelper.FONT, Font.BOLD, 16));
        scanField.putClientProperty("JTextField.placeholderText", "Ler código de barras ou digitar SKU e premir Enter...");
        scanField.addKeyListener(new KeyAdapter() {
            @Override public void keyPressed(KeyEvent e) { if (e.getKeyCode() == KeyEvent.VK_ENTER) processScan(scanField.getText().trim()); }
        });
        ModernButton searchBtn = ButtonStyles.main(UIHelper.createPrimaryButton("Processar")); searchBtn.setIcon(UIHelper.icon("fas-search", 14));
        searchBtn.addActionListener(e -> processScan(scanField.getText().trim()));
        JPanel inputRow = new JPanel(new BorderLayout(8, 0)); inputRow.setOpaque(false);
        inputRow.add(new JLabel(UIHelper.icon("fas-barcode", 24, UIHelper.ACCENT_BLUE)), BorderLayout.WEST);
        inputRow.add(scanField, BorderLayout.CENTER); inputRow.add(searchBtn, BorderLayout.EAST);
        box.add(inputRow, BorderLayout.CENTER); box.add(feedback, BorderLayout.SOUTH);
        return box;
    }

    private JPanel buildHistoryCard() {
        ModernPanel card = new ModernPanel(14); card.setLayout(new BorderLayout(0, 8)); card.setBorder(new EmptyBorder(12, 14, 12, 14));
        JLabel cardTitle = new JLabel("Sessão Actual de Leituras"); cardTitle.setFont(new Font(UIHelper.FONT, Font.BOLD, 13));
        cardTitle.setForeground(UIHelper.TEXT_LIGHT); card.add(cardTitle, BorderLayout.NORTH);
        historyModel = new DefaultTableModel(new String[]{"Hora", "Código / SKU", "Produto", "Qtd"}, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        JTable historyTable = new JTable(historyModel); UIHelper.styleTable(historyTable);
        JScrollPane scroll = new JScrollPane(historyTable); UIHelper.styleScrollPane(scroll);
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

        BarcodeScannerExecutionEngine.searchProduct(
                code, comercialApiClient,
                product -> displayProduct(product, code),
                errMsg -> { feedback.show(FeedbackType.ERROR, errMsg); detailsCard.clearDisplay(); },
                () -> { scanField.setText(""); scanField.requestFocusInWindow(); }
        );
    }

    private void displayProduct(ProductDTO p, String scannedCode) {
        detailsCard.displayProduct(p, scannedCode, mode);
        if (mode == CollectorMode.MANUFACTURING) BarcodeScannerExecutionEngine.checkBomAndEnable(p, productionApiClient, detailsCard);
        historyModel.insertRow(0, new Object[]{LocalTime.now().format(TIME_FMT), scannedCode, p.name(), detailsCard.getQuantity()});
    }

    private void executeAction() {
        ProductDTO p = detailsCard.getLastScannedProduct();
        if (p == null) return;
        int qty = detailsCard.getQuantity();

        if (mode == CollectorMode.MANUFACTURING) {
            if (detailsCard.getMatchedBom() == null) {
                feedback.show(FeedbackType.WARNING, "O produto selecionado não possui fórmula/BOM de fabrico.");
                return;
            }
            detailsCard.setActionEnabled(false);
            BarcodeScannerExecutionEngine.launchProductionOrder(
                    detailsCard.getMatchedBom(), qty, inventoryApiClient, productionApiClient,
                    () -> { feedback.show(FeedbackType.SUCCESS, "Ordem de Fabrico lançada e iniciada com sucesso!"); if (onActionCompleted != null) onActionCompleted.run(); },
                    err -> feedback.show(FeedbackType.ERROR, "Falha ao lançar ordem: " + err),
                    () -> { detailsCard.setActionEnabled(true); scanField.requestFocusInWindow(); }
            );
        } else {
            feedback.show(FeedbackType.SUCCESS, "Leitura de inventário registada (" + qty + " un).");
            if (onActionCompleted != null) onActionCompleted.run();
            scanField.requestFocusInWindow();
        }
    }
}
