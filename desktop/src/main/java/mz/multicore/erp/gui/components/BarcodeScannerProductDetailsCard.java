package mz.multicore.erp.gui.components;

import mz.multicore.erp.gui.components.BarcodeScannerCollectorDialog.CollectorMode;
import mz.multicore.erp.modules.comercial.dto.ProductDTO;
import mz.multicore.erp.modules.manufacturing.dto.BillOfMaterialsDTO;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionListener;

/**
 * Componente visual do artigo identificado no diálogo de leitura por código de barras.
 * Gerencia a apresentação das propriedades do artigo, estado da BOM e seletor de quantidade.
 */
public class BarcodeScannerProductDetailsCard {

    private final ModernPanel card = new ModernPanel(14);
    private final JLabel lblProductName = new JLabel("Aguardando leitura de código...");
    private final JLabel lblProductSku = new JLabel("SKU: —");
    private final JLabel lblProductBarcode = new JLabel("Código de Barras: —");
    private final JLabel lblProductDetails = new JLabel("Preço / Categoria: —");
    private final JLabel lblBomStatus = new JLabel(" ");
    private final JSpinner spinQuantity = new JSpinner(new SpinnerNumberModel(1, 1, 100000, 1));
    private final ModernButton btnAction;

    private ProductDTO lastScannedProduct;
    private BillOfMaterialsDTO matchedBom;

    public BarcodeScannerProductDetailsCard(CollectorMode mode, ActionListener actionListener) {
        card.setLayout(new BorderLayout(0, 10));
        card.setBorder(new EmptyBorder(12, 14, 12, 14));

        JLabel cardTitle = new JLabel("Artigo Identificado");
        cardTitle.setFont(new Font(UIHelper.FONT, Font.BOLD, 13));
        cardTitle.setForeground(UIHelper.TEXT_LIGHT);
        card.add(cardTitle, BorderLayout.NORTH);

        JPanel details = new JPanel();
        details.setOpaque(false);
        details.setLayout(new BoxLayout(details, BoxLayout.Y_AXIS));

        lblProductName.setFont(new Font(UIHelper.FONT, Font.BOLD, 15));
        lblProductName.setForeground(UIHelper.TEXT_LIGHT);
        lblProductSku.setFont(new Font(UIHelper.FONT, Font.PLAIN, 12));
        lblProductSku.setForeground(UIHelper.TEXT_MUTED);
        lblProductBarcode.setFont(new Font(UIHelper.FONT, Font.PLAIN, 12));
        lblProductBarcode.setForeground(UIHelper.TEXT_MUTED);
        lblProductDetails.setFont(new Font(UIHelper.FONT, Font.PLAIN, 12));
        lblProductDetails.setForeground(UIHelper.TEXT_MUTED);
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

        spinQuantity.setPreferredSize(new Dimension(80, UIHelper.FORM_CONTROL_HEIGHT));
        UIHelper.styleSpinner(spinQuantity);

        boolean isMfg = mode == CollectorMode.MANUFACTURING;
        btnAction = UIHelper.createSuccessButton(isMfg ? "Produzir Lote" : "Confirmar Leitura");
        btnAction.setIcon(UIHelper.icon(isMfg ? "fas-industry" : "fas-check", 14));
        btnAction.setEnabled(false);
        if (actionListener != null) btnAction.addActionListener(actionListener);

        actionRow.add(new JLabel("Qtd:"));
        actionRow.add(spinQuantity);
        actionRow.add(btnAction);

        details.add(Box.createVerticalStrut(8));
        details.add(actionRow);
        card.add(details, BorderLayout.CENTER);
    }

    public JPanel getPanel() { return card; }

    public void displayProduct(ProductDTO p, String scannedCode, CollectorMode mode) {
        this.lastScannedProduct = p;
        lblProductName.setText(p.name());
        lblProductSku.setText("SKU: " + (p.sku() != null ? p.sku() : "—"));
        lblProductBarcode.setText("Código de Barras: " + (p.barcode() != null ? p.barcode() : scannedCode));
        lblProductDetails.setText("Preço Venda: " + (p.unitPrice() != null ? p.unitPrice() + " MT" : "N/D") + " | Unidade: " + p.saleType());

        if (mode != CollectorMode.MANUFACTURING) {
            lblBomStatus.setText("Artigo verificado com sucesso.");
            lblBomStatus.setForeground(UIHelper.APPROVED_GREEN);
            btnAction.setEnabled(true);
        }
    }

    public void setBomStatus(String text, Color color, boolean actionEnabled) {
        lblBomStatus.setText(text);
        lblBomStatus.setForeground(color);
        btnAction.setEnabled(actionEnabled);
    }

    public void clearDisplay() {
        lastScannedProduct = null;
        matchedBom = null;
        lblProductName.setText("Aguardando leitura de código...");
        lblProductSku.setText("SKU: —");
        lblProductBarcode.setText("Código de Barras: —");
        lblProductDetails.setText("Preço / Categoria: —");
        lblBomStatus.setText(" ");
        btnAction.setEnabled(false);
    }

    public int getQuantity() { return (Integer) spinQuantity.getValue(); }
    public ProductDTO getLastScannedProduct() { return lastScannedProduct; }
    public BillOfMaterialsDTO getMatchedBom() { return matchedBom; }
    public void setMatchedBom(BillOfMaterialsDTO bom) { this.matchedBom = bom; }
    public void setActionEnabled(boolean enabled) { btnAction.setEnabled(enabled); }
}
