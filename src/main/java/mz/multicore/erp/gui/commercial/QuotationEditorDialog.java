package mz.multicore.erp.gui.commercial;

import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.desktop.client.ComercialApiClient;
import mz.multicore.erp.gui.components.DecimalField;
import mz.multicore.erp.gui.components.ModernButton;
import mz.multicore.erp.gui.components.ModernFormDialog;
import mz.multicore.erp.gui.components.QuantityField;
import mz.multicore.erp.gui.components.UIHelper;
import mz.multicore.erp.modules.comercial.dto.ClientDTO;
import mz.multicore.erp.modules.comercial.dto.CreateQuotationLineRequest;
import mz.multicore.erp.modules.comercial.dto.CreateQuotationRequest;
import mz.multicore.erp.modules.comercial.dto.ProductDTO;
import mz.multicore.erp.modules.comercial.dto.QuotationDTO;
import mz.multicore.erp.modules.comercial.model.QuotationValidity;
import mz.multicore.erp.modules.inventory.dto.WarehouseDTO;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

/**
 * Formulário de emissão de cotação: cabeçalho (cliente, armazém, validade, condições) e construtor
 * de linhas.
 *
 * <p>O rascunho mostra preços apenas como <b>pré-visualização</b>. Quem aprecia é o servidor, a
 * partir do artigo — e o pedido enviado ({@link CreateQuotationLineRequest}) nem sequer tem campo
 * de preço ou de IVA por onde este ecrã pudesse discordar. Ver docs/COTACAO_SPEC.md §3.
 */
public final class QuotationEditorDialog {

    private final Component parent;
    private final ComercialApiClient apiClient;
    private final List<ClientDTO> clients;
    private final List<ProductDTO> products;
    private final List<WarehouseDTO> warehouses;

    private final List<CreateQuotationLineRequest> draftLines = new ArrayList<>();
    private BigDecimal draftTotal = BigDecimal.ZERO;

    private JComboBox<String> clientCombo;
    private JTextField walkInField;
    private JComboBox<String> warehouseCombo;
    private QuantityField validityField;
    private JTextField paymentTermsField;
    private JTextField deliveryTermsField;
    private JTextArea notesArea;

    private JComboBox<String> productCombo;
    private QuantityField quantityField;
    private DecimalField discountField;
    private DefaultTableModel linesModel;
    private JTable linesTable;
    private JLabel totalLabel;

    public QuotationEditorDialog(Component parent, ComercialApiClient apiClient,
                                 List<ClientDTO> clients, List<ProductDTO> products,
                                 List<WarehouseDTO> warehouses) {
        this.parent = parent;
        this.apiClient = apiClient;
        this.clients = clients;
        this.products = products;
        this.warehouses = warehouses;
    }

    /** Abre o formulário. Devolve a cotação criada, ou {@code null} se o operador desistiu. */
    public QuotationDTO open() {
        if (warehouses.isEmpty()) {
            JOptionPane.showMessageDialog(parent, "Nenhum armazém disponível para a empresa atual.",
                    "Erro", JOptionPane.ERROR_MESSAGE);
            return null;
        }
        if (products.isEmpty()) {
            JOptionPane.showMessageDialog(parent, "Nenhum produto disponível para cotar.",
                    "Erro", JOptionPane.ERROR_MESSAGE);
            return null;
        }

        JPanel content = new JPanel(new BorderLayout(0, 12));
        content.setOpaque(false);
        content.add(buildHeaderForm(), BorderLayout.NORTH);
        content.add(buildLinesSection(), BorderLayout.CENTER);

        QuotationDTO[] created = new QuotationDTO[1];
        ModernFormDialog dialog = new ModernFormDialog(SwingUtilities.getWindowAncestor((JComponent) parent),
                "Nova Cotação", "fas-file-signature",
                "Proposta de preço ao cliente — não move stock nem cria dívida", content)
                .setConfirmButton("Emitir Cotação", "fas-file-signature")
                .setSize(1000, 760)
                .setOnSaveAsync(() -> {
                    CreateQuotationRequest request = buildRequest();
                    return () -> created[0] = apiClient.createQuotation(request);
                });

        return dialog.showDialog() ? created[0] : null;
    }

    private JPanel buildHeaderForm() {
        clientCombo = new JComboBox<>();
        clientCombo.addItem("— Consumidor Final (sem registo) —");
        for (ClientDTO c : clients) {
            clientCombo.addItem(c.name() + " (" + c.taxId() + ")");
        }
        UIHelper.styleComboBox(clientCombo);

        walkInField = new JTextField();
        UIHelper.styleTextField(walkInField);
        walkInField.putClientProperty("JTextField.placeholderText", "Nome do comprador, se não for cliente registado");

        warehouseCombo = new JComboBox<>();
        for (WarehouseDTO w : warehouses) {
            warehouseCombo.addItem(w.name());
        }
        UIHelper.styleComboBox(warehouseCombo);

        validityField = new QuantityField(String.valueOf(QuotationValidity.DEFAULT_DAYS), true);
        UIHelper.styleTextField(validityField);

        paymentTermsField = new JTextField();
        UIHelper.styleTextField(paymentTermsField);
        paymentTermsField.putClientProperty("JTextField.placeholderText", "Ex.: 50% na encomenda, 50% na entrega");

        deliveryTermsField = new JTextField();
        UIHelper.styleTextField(deliveryTermsField);
        deliveryTermsField.putClientProperty("JTextField.placeholderText", "Ex.: 5 dias úteis após confirmação");

        notesArea = new JTextArea(3, 28);
        UIHelper.styleTextArea(notesArea);
        notesArea.setLineWrap(true);
        notesArea.setWrapStyleWord(true);
        JScrollPane notesScroll = new JScrollPane(notesArea);
        UIHelper.styleScrollPane(notesScroll);

        return UIHelper.createDialogForm(
                "Cliente:", clientCombo,
                "Comprador (opcional):", walkInField,
                "Armazém:", warehouseCombo,
                "Validade (dias):", validityField,
                "Condições de pagamento:", paymentTermsField,
                "Prazo de entrega:", deliveryTermsField,
                "Observações:", notesScroll
        );
    }

    private JPanel buildLinesSection() {
        JPanel section = new JPanel(new BorderLayout(0, 10));
        section.setOpaque(false);

        productCombo = new JComboBox<>();
        for (ProductDTO p : products) {
            productCombo.addItem(productLabel(p) + " - " + p.unitPrice() + " MT");
        }
        UIHelper.styleComboBox(productCombo);

        quantityField = new QuantityField("1", true);
        UIHelper.styleTextField(quantityField);
        discountField = new DecimalField("0", 2, false);
        UIHelper.styleTextField(discountField);

        ModernButton addBtn = UIHelper.createPrimaryButton("Adicionar Linha");
        addBtn.setIcon(UIHelper.icon("fas-plus", 14));
        addBtn.addActionListener(e -> addLine());

        ModernButton removeBtn = UIHelper.createDangerButton("Remover Linha");
        removeBtn.setIcon(UIHelper.icon("fas-trash", 14));
        removeBtn.addActionListener(e -> removeSelectedLine());

        JPanel lineForm = UIHelper.createDialogForm(
                "Produto:", productCombo,
                "Quantidade:", quantityField,
                "Desconto (%):", discountField
        );
        JPanel lineActions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        lineActions.setOpaque(false);
        lineActions.add(removeBtn);
        lineActions.add(addBtn);

        JPanel top = new JPanel(new BorderLayout(0, 8));
        top.setOpaque(false);
        top.add(lineForm, BorderLayout.CENTER);
        top.add(lineActions, BorderLayout.SOUTH);
        section.add(top, BorderLayout.NORTH);

        linesModel = new DefaultTableModel(
                new String[]{"Produto", "Qtd", "Preço Unit.", "Desc.", "Total (c/ IVA)"}, 0) {
            @Override public boolean isCellEditable(int row, int column) { return false; }
        };
        linesTable = new JTable(linesModel);
        UIHelper.styleTable(linesTable);
        JScrollPane scroll = new JScrollPane(linesTable);
        UIHelper.styleScrollPane(scroll);
        scroll.setPreferredSize(new Dimension(900, 220));
        section.add(scroll, BorderLayout.CENTER);

        totalLabel = new JLabel("Total Rascunho: 0.00 MT (incl. IVA)");
        totalLabel.setBorder(new EmptyBorder(6, 0, 0, 0));
        JPanel totalRow = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        totalRow.setOpaque(false);
        totalRow.add(totalLabel);
        section.add(totalRow, BorderLayout.SOUTH);
        return section;
    }

    private void addLine() {
        int idx = productCombo.getSelectedIndex();
        if (idx < 0 || idx >= products.size()) return;
        ProductDTO product = products.get(idx);

        BigDecimal qty;
        try {
            qty = quantityField.value();
            if (qty.signum() <= 0) throw new NumberFormatException();
        } catch (RuntimeException e) {
            JOptionPane.showMessageDialog(parent, "A quantidade deve ser um número superior a zero.",
                    "Erro", JOptionPane.ERROR_MESSAGE);
            return;
        }

        BigDecimal discount;
        try {
            discount = discountField.value();
            if (discount.signum() < 0 || discount.compareTo(BigDecimal.valueOf(100)) > 0) {
                throw new NumberFormatException();
            }
        } catch (RuntimeException e) {
            JOptionPane.showMessageDialog(parent, "O desconto deve ser um número entre 0 e 100.",
                    "Erro", JOptionPane.ERROR_MESSAGE);
            return;
        }

        draftLines.add(new CreateQuotationLineRequest(product.id(), qty, discount));

        // Pré-visualização apenas: a conta que vale é a do servidor (LineCalculator sobre o preço
        // efectivo e a taxa do artigo).
        BigDecimal unitPrice = product.unitPrice();
        BigDecimal net = unitPrice.multiply(qty);
        if (discount.signum() > 0) {
            net = net.subtract(net.multiply(discount.divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP)));
        }
        BigDecimal total = net.add(net.multiply(product.effectiveTaxRate())).setScale(2, RoundingMode.HALF_UP);

        linesModel.addRow(new Object[]{
                product.name(), qty, unitPrice + " MT", discount + "%", total + " MT"});
        draftTotal = draftTotal.add(total);
        totalLabel.setText(String.format("Total Rascunho: %,.2f MT (incl. IVA)", draftTotal));

        quantityField.setText("1");
        discountField.setText("0");
    }

    private void removeSelectedLine() {
        int row = linesTable.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(parent, "Selecione uma linha para remover.",
                    "Aviso", JOptionPane.WARNING_MESSAGE);
            return;
        }
        int modelRow = linesTable.convertRowIndexToModel(row);
        draftLines.remove(modelRow);
        linesModel.removeRow(modelRow);
        recomputeDraftTotal();
    }

    private void recomputeDraftTotal() {
        draftTotal = BigDecimal.ZERO;
        for (int row = 0; row < linesModel.getRowCount(); row++) {
            String cell = String.valueOf(linesModel.getValueAt(row, 4)).replace(" MT", "").trim();
            try {
                draftTotal = draftTotal.add(new BigDecimal(cell));
            } catch (NumberFormatException ignored) {
                // célula manipulada não pode derrubar o rascunho — o total real vem do servidor
            }
        }
        totalLabel.setText(String.format("Total Rascunho: %,.2f MT (incl. IVA)", draftTotal));
    }

    /** Valida no EDT e monta o pedido. Lança {@link RuntimeException} para manter o modal aberto. */
    private CreateQuotationRequest buildRequest() {
        if (draftLines.isEmpty()) {
            throw new RuntimeException("Adicione pelo menos uma linha à cotação.");
        }
        int whIdx = warehouseCombo.getSelectedIndex();
        if (whIdx < 0) {
            throw new RuntimeException("Selecione o armazém.");
        }
        int validity;
        try {
            validity = validityField.value().intValueExact();
            if (validity <= 0) throw new NumberFormatException();
        } catch (RuntimeException e) {
            throw new RuntimeException("A validade deve ser um número inteiro de dias superior a zero.");
        }

        // Índice 0 do combo é "Consumidor Final" — sem cliente registado.
        int clientIdx = clientCombo.getSelectedIndex();
        Long clientId = clientIdx > 0 ? clients.get(clientIdx - 1).id() : null;

        return new CreateQuotationRequest(
                clientId,
                blankToNull(walkInField.getText()),
                CurrentUserContext.getCurrentCompanyId(),
                warehouses.get(whIdx).id(),
                validity,
                blankToNull(paymentTermsField.getText()),
                blankToNull(deliveryTermsField.getText()),
                blankToNull(notesArea.getText()),
                new ArrayList<>(draftLines));
    }

    private static String productLabel(ProductDTO p) {
        String code = p.reference() != null && !p.reference().isBlank() ? p.reference() : p.sku();
        return code + " - " + p.name();
    }

    private static String blankToNull(String value) {
        return (value == null || value.isBlank()) ? null : value.trim();
    }
}
