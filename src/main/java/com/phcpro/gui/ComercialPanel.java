package com.phcpro.gui;

import com.phcpro.architecture.security.CurrentUserContext;
import com.phcpro.gui.components.DocumentEditorHost;
import com.phcpro.gui.components.ModernButton;
import com.phcpro.gui.components.ModernFormDialog;
import com.phcpro.gui.components.ModernPanel;
import com.phcpro.gui.components.TableFilter;
import com.phcpro.gui.components.TableCellRenderers;
import com.phcpro.gui.components.MoneyField;
import com.phcpro.gui.components.QuantityField;
import com.phcpro.gui.components.DecimalField;
import com.phcpro.gui.components.UIHelper;
import com.phcpro.gui.commercial.CommercialMovementsPanel;
import com.phcpro.gui.commercial.OutstandingAccountsPanel;
import com.phcpro.gui.commercial.CommercialNotesPanel;
import com.phcpro.gui.commercial.DeliveryGuidesPanel;
import com.phcpro.gui.commercial.ReceiptsPanel;
import com.phcpro.gui.commercial.BillOrderDialog;
import com.phcpro.gui.commercial.CancelOrderDialog;
import com.phcpro.gui.commercial.OrderDetailsDialog;
import com.phcpro.desktop.client.ComercialApiClient;
import com.phcpro.desktop.client.CreditNoteApiClient;
import com.phcpro.desktop.client.DebitNoteApiClient;
import com.phcpro.desktop.client.FinanceApiClient;
import com.phcpro.desktop.client.InventoryApiClient;
import com.phcpro.desktop.client.MovimentosApiClient;
import com.phcpro.desktop.client.POSApiClient;
import com.phcpro.desktop.client.PromotionApiClient;
import com.phcpro.modules.comercial.dto.*;
import com.phcpro.modules.comercial.model.InvoiceStatus;
import com.phcpro.modules.inventory.dto.WarehouseDTO;
import com.phcpro.modules.financeira.dto.TreasuryAccountDTO;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class ComercialPanel extends JPanel {

    private final ComercialApiClient comercialApiClient;
    private final InventoryApiClient inventoryApiClient;
    private final FinanceApiClient financeApiClient;

    // TAB 1: FATURAÇÃO ELEMENTS
    JComboBox<String> clientCombo;
    JComboBox<String> warehouseCombo;
    JComboBox<String> productCombo;
    QuantityField quantityField;
    QuantityField invoiceBoxesField;
    DecimalField discountField;
    JTextField batchField;
    JTextField serialField;
    DefaultTableModel linesTableModel;
    JTable linesTable;
    JLabel totalLabel;

    DefaultTableModel invoicesTableModel;
    JTable invoicesTable;

    // TAB 2: RECIBOS ELEMENTS

    // TAB 3: REGISTAR CLIENTE ELEMENTS

    // TAB 4: ENCOMENDAS ELEMENTS
    JComboBox<String> orderClientCombo;
    JTextField orderClientWalkInField;
    JComboBox<String> orderWarehouseCombo;
    JComboBox<String> orderProductCombo;
    QuantityField orderQuantityField;
    QuantityField orderBoxesField;
    DecimalField orderDiscountField;
    JTextField orderBatchField;
    JTextField orderSerialField;
    DefaultTableModel orderLinesTableModel;
    JTable orderLinesTable;
    JLabel orderTotalLabel;

    DefaultTableModel ordersTableModel;
    JTable ordersTable;

    // TAB 5: GUIAS DE REMESSA ELEMENTS

    // Seeding lists for selections
    private List<ClientDTO> clientsList = new ArrayList<>();
    private List<ProductDTO> productsList = new ArrayList<>();
    private List<WarehouseDTO> warehousesList = new ArrayList<>();
    
    // In-memory line items of the invoice currently being drafted
    final List<CreateInvoiceLineRequest> draftLines = new ArrayList<>();
    private BigDecimal draftSubtotal = BigDecimal.ZERO;
    private BigDecimal draftTax = BigDecimal.ZERO;
    private BigDecimal draftTotal = BigDecimal.ZERO;

    // In-memory line items of the order currently being drafted
    final List<CreateInvoiceLineRequest> draftOrderLines = new ArrayList<>();
    private BigDecimal draftOrderSubtotal = BigDecimal.ZERO;
    private BigDecimal draftOrderTax = BigDecimal.ZERO;
    private BigDecimal draftOrderTotal = BigDecimal.ZERO;


    private final POSApiClient posApiClient;
    private final MovimentosApiClient movimentosApiClient;

    JPanel invoiceFormContent;              // conteúdo do modal de nova fatura
    private com.phcpro.modules.comercial.dto.InvoiceDTO lastCreatedInvoice;
    JPanel orderFormContent;                // conteúdo do editor de nova encomenda
    private OrderDTO lastCreatedOrder;
    CardLayout encomendasCards;             // alterna lista <-> editor na aba Encomendas
    JPanel encomendasHost;
    CardLayout faturacaoCards;              // alterna lista <-> editor na aba Faturação
    JPanel faturacaoHost;
    private final CommercialMovementsPanel movementsPanel;
    private final OutstandingAccountsPanel outstandingAccountsPanel;
    private final CommercialNotesPanel notesPanel;
    private final DeliveryGuidesPanel deliveryGuidesPanel;
    private final ReceiptsPanel receiptsPanel;
    private final BillOrderDialog billOrderDialog;
    private final CancelOrderDialog cancelOrderDialog;
    private final OrderDetailsDialog orderDetailsDialog;

    public ComercialPanel(
            ComercialApiClient comercialApiClient,
            InventoryApiClient inventoryApiClient,
            FinanceApiClient financeApiClient,
            CreditNoteApiClient creditNoteApiClient,
            DebitNoteApiClient debitNoteApiClient,
            POSApiClient posApiClient,
            MovimentosApiClient movimentosApiClient,
            PromotionApiClient promotionApiClient
    ) {
        this.comercialApiClient = comercialApiClient;
        this.inventoryApiClient = inventoryApiClient;
        this.financeApiClient = financeApiClient;
        this.posApiClient = posApiClient;
        this.movimentosApiClient = movimentosApiClient;
        this.movementsPanel = new CommercialMovementsPanel(movimentosApiClient);
        this.outstandingAccountsPanel = new OutstandingAccountsPanel(comercialApiClient, financeApiClient, posApiClient);
        this.notesPanel = new CommercialNotesPanel(comercialApiClient, creditNoteApiClient, debitNoteApiClient,
                () -> List.copyOf(warehousesList));
        this.deliveryGuidesPanel = new DeliveryGuidesPanel(comercialApiClient, this::loadOrdersTable);
        this.receiptsPanel = new ReceiptsPanel(comercialApiClient, this::loadInvoicesTable);
        this.billOrderDialog = new BillOrderDialog(this, comercialApiClient,
                this::loadInvoicesTable, this::loadOrdersTable);
        this.cancelOrderDialog = new CancelOrderDialog(this, comercialApiClient, this::loadOrdersTable);
        this.orderDetailsDialog = new OrderDetailsDialog(this, comercialApiClient, this::loadOrdersTable);

        setLayout(new BorderLayout());
        setBackground(UIHelper.BG_DARK);
        setBorder(new EmptyBorder(10, 10, 10, 10));

        JTabbedPane tabbedPane = new JTabbedPane();
        UIHelper.styleTabbedPanePHC(tabbedPane);

        // TAB 1: FATURAÇÃO
        JPanel tabFaturacao = createFaturacaoTab();
        tabbedPane.addTab("Faturação (FT)", UIHelper.icon("fas-file-invoice", 16, UIHelper.TEXT_LIGHT), tabFaturacao);

        // TAB 2: RECIBOS
        tabbedPane.addTab("Recibos (RC)", UIHelper.icon("fas-receipt", 16, UIHelper.TEXT_LIGHT), receiptsPanel);

        // Registo/gestão de clientes vive na área "Clientes" do menu de topo, não aqui.

        // TAB 4: ENCOMENDAS (EC)
        JPanel tabEncomendas = createEncomendasTab();
        tabbedPane.addTab("Encomendas (EC)", UIHelper.icon("fas-file-signature", 16, UIHelper.TEXT_LIGHT), tabEncomendas);

        // TAB 5: GUIAS DE REMESSA (GR)
        tabbedPane.addTab("Guias de Remessa (GR)", UIHelper.icon("fas-truck", 16, UIHelper.TEXT_LIGHT),
                deliveryGuidesPanel);

        // TAB 6: NOTAS DE CRÉDITO (NC)
        tabbedPane.addTab("Notas de Crédito (NC)", UIHelper.icon("fas-undo-alt", 16, UIHelper.TEXT_LIGHT), notesPanel.creditTab());

        // TAB 7: NOTAS DE DÉBITO (ND)
        tabbedPane.addTab("Notas de Débito (ND)", UIHelper.icon("fas-plus-circle", 16, UIHelper.TEXT_LIGHT), notesPanel.debitTab());

        // TAB 8: CONTAS CORRENTES (FIADOS)
        tabbedPane.addTab("Contas Correntes", UIHelper.icon("fas-hand-holding-usd", 16, UIHelper.TEXT_LIGHT), outstandingAccountsPanel);

        // TAB 9: PROMOÇÕES
        tabbedPane.addTab("Promoções", UIHelper.icon("fas-tags", 16, UIHelper.TEXT_LIGHT),
                new PromotionsPanel(promotionApiClient, comercialApiClient));

        // TAB 10: MOVIMENTOS (vista unificada de todos os documentos comerciais)
        tabbedPane.addTab("Movimentos", UIHelper.icon("fas-list-alt", 16, UIHelper.TEXT_LIGHT), movementsPanel);

        add(tabbedPane, BorderLayout.CENTER);

        onPanelSelected();
    }


    private JPanel createFaturacaoTab() {
        return CommercialInvoicesView.create(this);
    }
    void openInvoiceEditor() {
        if (clientsList.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Nenhum cliente disponível.", "Erro", JOptionPane.ERROR_MESSAGE);
            return;
        }
        if (warehousesList.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Nenhum armazém disponível para a empresa atual.",
                    "Erro", JOptionPane.ERROR_MESSAGE);
            return;
        }
        resetInvoiceDraft();
        lastCreatedInvoice = null;
        if (faturacaoCards != null && faturacaoHost != null) {
            faturacaoCards.show(faturacaoHost, "editor");
        }
    }

    void backToInvoicesList() {
        if (faturacaoCards != null && faturacaoHost != null) {
            faturacaoCards.show(faturacaoHost, "list");
        }
    }

    /** Guardar a partir do editor: valida+cria, informa, recarrega a lista e volta. Erro mantém o editor. */
    void saveInvoiceFromEditor() {
        try {
            CreateInvoiceRequest request = buildInvoiceRequest();
            UIHelper.runWithProgress(this, "A emitir fatura…", () -> comercialApiClient.createInvoice(request), created -> {
            lastCreatedInvoice = created;
            resetInvoiceDraft();
            if (created.status() == InvoiceStatus.PENDING_DISCOUNT_APPROVAL) {
                JOptionPane.showMessageDialog(this, "Fatura " + created.invoiceNumber() + " emitida!\n"
                        + "Bloqueada para Aprovação de Desconto (superior a 10%).\n"
                        + "Valor: " + created.totalAmount() + " MT.", "Bloqueio de Desconto", JOptionPane.WARNING_MESSAGE);
            } else {
                JOptionPane.showMessageDialog(this, "Fatura " + created.invoiceNumber() + " emitida com sucesso!\n"
                        + "Valor: " + created.totalAmount() + " MT.", "Sucesso", JOptionPane.INFORMATION_MESSAGE);
            }
            loadInvoicesTable();
            backToInvoicesList();
            }, error -> showCommercialError("emitir fatura", error));
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage() == null ? "Falha ao emitir fatura." : ex.getMessage(),
                    "Erro", JOptionPane.ERROR_MESSAGE);
        }
    }

    public void onPanelSelected() {
        loadClientsAndProducts();
        loadWarehouses();
        loadInvoicesTable();
        receiptsPanel.refresh();
        loadOrdersTable();
        deliveryGuidesPanel.refresh();
        notesPanel.refresh();
        outstandingAccountsPanel.refresh();
        movementsPanel.refresh();
    }

    private void loadClientsAndProducts() {
        UIHelper.loadAsync(this, () -> new CommercialMetadata(comercialApiClient.getAllClients(),
                        comercialApiClient.getAllProducts()), this::applyClientsAndProducts,
                error -> showCommercialLoadError("clientes e produtos", error));
    }

    private void applyClientsAndProducts(CommercialMetadata metadata) {
        clientCombo.removeAllItems();
        productCombo.removeAllItems();
        orderClientCombo.removeAllItems();
        orderProductCombo.removeAllItems();
        clientsList = metadata.clients();
        productsList = metadata.products();

        // Encomendas aceitam venda sem cliente registado — primeiro item do combo.
        orderClientCombo.addItem("— Consumidor Final (sem registo) —");

        for (ClientDTO c : clientsList) {
            clientCombo.addItem(c.name() + " (" + c.taxId() + ")");
            orderClientCombo.addItem(c.name() + " (" + c.taxId() + ")");
        }

        for (ProductDTO p : productsList) {
            productCombo.addItem(productLabel(p) + " - " + p.unitPrice() + " MT");
            orderProductCombo.addItem(productLabel(p) + " - " + p.unitPrice() + " MT");
        }
    }

    private String productLabel(ProductDTO p) {
        String code = p.reference() != null && !p.reference().isBlank() ? p.reference() : p.sku();
        if (p.barcode() != null && !p.barcode().isBlank()) {
            return code + " | " + p.barcode() + " - " + p.name();
        }
        return code + " - " + p.name();
    }

    private void loadWarehouses() {
        Long companyId = CurrentUserContext.getCurrentCompanyId();
        UIHelper.loadAsync(this, () -> inventoryApiClient.getWarehousesByCompany(companyId), this::applyWarehouses,
                error -> showCommercialLoadError("armazéns", error));
    }

    private void applyWarehouses(List<WarehouseDTO> loaded) {
        warehouseCombo.removeAllItems();
        orderWarehouseCombo.removeAllItems();
        warehousesList = loaded;

        for (WarehouseDTO w : warehousesList) {
            warehouseCombo.addItem(w.name());
            orderWarehouseCombo.addItem(w.name());
        }
    }


    /**
     * Helper de venda ao grosso: se o operador indicar um nº de caixas e houver produto seleccionado,
     * preenche a Qtd em UNIDADES = caixas × unidades/caixa. O cálculo de dinheiro continua por unidade
     * (a caixa é só conversão). Campo vazio não mexe na Qtd (permite entrada directa em unidades).
     */
    void applyInvoiceBoxes() {
        if (invoiceBoxesField == null) return;
        String raw = invoiceBoxesField.getText().trim();
        if (raw.isEmpty()) return;
        int idx = productCombo.getSelectedIndex();
        if (idx < 0 || idx >= productsList.size()) return;
        int upb = Math.max(1, productsList.get(idx).unitsPerBox());
        try {
            int boxes = Integer.parseInt(raw);
            if (boxes <= 0) return;
            quantityField.setText(String.valueOf(boxes * upb));
        } catch (NumberFormatException ignore) {
            // texto inválido → não altera a Qtd
        }
    }

    void addDraftLine() {
        if (productsList.isEmpty()) return;

        int selectedProdIdx = productCombo.getSelectedIndex();
        if (selectedProdIdx < 0) return;

        ProductDTO product = productsList.get(selectedProdIdx);

        int qty;
        try {
            qty = quantityField.value().intValueExact();
            if (qty <= 0) throw new NumberFormatException();
        } catch (RuntimeException e) {
            JOptionPane.showMessageDialog(this, "A quantidade deve ser um número inteiro superior a zero.", "Erro", JOptionPane.ERROR_MESSAGE);
            return;
        }

        BigDecimal discount = BigDecimal.ZERO;
        try {
            discount = discountField.value();
            if (discount.compareTo(BigDecimal.ZERO) < 0 || discount.compareTo(BigDecimal.valueOf(100)) > 0) {
                throw new NumberFormatException();
            }
        } catch (RuntimeException e) {
            JOptionPane.showMessageDialog(this, "O desconto deve ser um número decimal entre 0 e 100.", "Erro", JOptionPane.ERROR_MESSAGE);
            return;
        }

        // Lote é decidido por FEFO no backend — batchField mostra apenas previsão.
        String previewBatch = batchField.getText().trim();
        String batch = null;

        String serial = serialField.getText().trim();
        if (serial.isEmpty()) serial = null;

        // A taxa é a do artigo (o backend resolve-a na mesma; isto só mantém a pré-visualização
        // do rascunho igual ao que vai ser cobrado).
        BigDecimal taxRate = product.effectiveTaxRate();

        CreateInvoiceLineRequest lineRequest = new CreateInvoiceLineRequest(
                product.id(),
                qty,
                taxRate,
                discount,
                batch,
                serial
        );
        draftLines.add(lineRequest);

        // Add to GUI table
        BigDecimal subTotal = product.unitPrice().multiply(BigDecimal.valueOf(qty));
        if (discount.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal discAmt = subTotal.multiply(discount.divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP));
            subTotal = subTotal.subtract(discAmt);
        }
        BigDecimal tax = subTotal.multiply(taxRate);
        BigDecimal total = subTotal.add(tax).setScale(2, RoundingMode.HALF_UP);

        String lotSer = "";
        if (!previewBatch.isEmpty() && !"Sem stock".equals(previewBatch)) {
            lotSer += "FEFO: " + previewBatch + " ";
        }
        if (serial != null) lotSer += "S: " + serial;
        if (lotSer.isEmpty()) lotSer = "-";

        linesTableModel.addRow(new Object[]{
                product.name(),
                qty,
                product.unitPrice() + " MT",
                discount + "%",
                lotSer,
                total + " MT"
        });

        // Accumulate totals
        draftSubtotal = draftSubtotal.add(subTotal);
        draftTax = draftTax.add(tax);
        draftTotal = draftTotal.add(total);

        totalLabel.setText(String.format("Total Rascunho: %,.2f MT (incl. IVA)", draftTotal));

        // Clear details
        quantityField.setText("1");
        invoiceBoxesField.setText("");
        discountField.setText("0");
        serialField.setText("");
        refreshInvoiceFEFOHint();
    }

    /** Abre o formulário de nova fatura num modal responsivo (com scroll). */
    /** Validação + emissão. Lança {@link RuntimeException} em erro para manter o editor aberto. */
    private CreateInvoiceRequest buildInvoiceRequest() {
        if (draftLines.isEmpty()) {
            throw new RuntimeException("Adicione pelo menos um item à fatura.");
        }
        int clientIdx = clientCombo.getSelectedIndex();
        int whIdx = warehouseCombo.getSelectedIndex();
        if (clientIdx < 0 || whIdx < 0) {
            throw new RuntimeException("Selecione cliente e armazém.");
        }
        ClientDTO client = clientsList.get(clientIdx);
        WarehouseDTO warehouse = warehousesList.get(whIdx);
        Long companyId = CurrentUserContext.getCurrentCompanyId();
        return new CreateInvoiceRequest(client.id(), companyId, warehouse.id(), new ArrayList<>(draftLines));
    }

    private void resetInvoiceDraft() {
        draftLines.clear();
        draftSubtotal = BigDecimal.ZERO;
        draftTax = BigDecimal.ZERO;
        draftTotal = BigDecimal.ZERO;
        if (linesTableModel != null) linesTableModel.setRowCount(0);
        if (totalLabel != null) totalLabel.setText("Total Rascunho: 0.00 MT (incl. IVA)");
    }

    void cancelSelectedInvoice() {
        int row = TableFilter.selectedModelRow(invoicesTable);
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Selecione uma fatura na tabela para anular.", "Aviso", JOptionPane.WARNING_MESSAGE);
            return;
        }

        Long invoiceId = (Long) invoicesTableModel.getValueAt(row, 0);
        String invoiceNum = (String) invoicesTableModel.getValueAt(row, 1);

        String reason = UIHelper.promptRequiredText("Anular Fatura", "fas-ban",
                "Fatura " + invoiceNum, "Motivo da anulação:");
        if (reason == null) return;

        UIHelper.runWithProgress(this, "A anular fatura…", () -> {
            comercialApiClient.cancelInvoice(invoiceId, reason);
            return null;
        }, ignored -> {
            JOptionPane.showMessageDialog(this, "Fatura " + invoiceNum + " anulada com sucesso!", "Sucesso", JOptionPane.INFORMATION_MESSAGE);
            loadInvoicesTable();
            receiptsPanel.refresh();
        }, error -> showCommercialError("anular fatura", error));
    }

    void paySelectedInvoice() {
        int row = TableFilter.selectedModelRow(invoicesTable);
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Selecione uma fatura na tabela para liquidar.", "Aviso", JOptionPane.WARNING_MESSAGE);
            return;
        }

        Long invoiceId = (Long) invoicesTableModel.getValueAt(row, 0);
        String invoiceNum = (String) invoicesTableModel.getValueAt(row, 1);
        String statusStr = (String) invoicesTableModel.getValueAt(row, 3);
        BigDecimal invoiceTotal = (BigDecimal) invoicesTableModel.getValueAt(row, 5);

        // Uma fatura parcialmente paga continua a receber recibos até o saldo chegar a zero.
        if (!"APPROVED".equalsIgnoreCase(statusStr) && !"PARTIALLY_PAID".equalsIgnoreCase(statusStr)) {
            JOptionPane.showMessageDialog(this, "Apenas faturas por cobrar podem receber recibo. Estado atual: " + statusStr, "Erro", JOptionPane.ERROR_MESSAGE);
            return;
        }

        // O valor sugerido é o que falta receber, não o total — com pagamentos parciais são
        // coisas diferentes, e o backend recusa um recibo acima do saldo.
        UIHelper.loadAsync(this, financeApiClient::getAllAccounts,
                accounts -> receiptsPanel.openPayment(invoiceId, invoiceNum, invoiceTotal, accounts),
                error -> showCommercialLoadError("contas de tesouraria", error));
    }

    public void loadInvoicesTable() {
        Long companyId = CurrentUserContext.getCurrentCompanyId();
        UIHelper.loadAsync(this, () -> comercialApiClient.getInvoicesByCompany(companyId), this::applyInvoices,
                error -> showCommercialLoadError("faturas", error));
    }

    private void applyInvoices(List<InvoiceDTO> invoices) {
        invoicesTableModel.setRowCount(0);
        for (InvoiceDTO invoice : invoices) {
            invoicesTableModel.addRow(new Object[]{
                    invoice.id(),
                    invoice.invoiceNumber(),
                    invoice.clientName(),
                    invoice.status().name(),
                    invoice.totalAmount(), outstandingOf(invoice)
            });
        }
    }

    /** Saldo por liquidar da fatura — espelha {@code Invoice.outstandingAmount()} do backend. */
    private static BigDecimal outstandingOf(InvoiceDTO invoice) {
        BigDecimal total = invoice.totalAmount() == null ? BigDecimal.ZERO : invoice.totalAmount();
        BigDecimal paid = invoice.amountPaid() == null ? BigDecimal.ZERO : invoice.amountPaid();
        BigDecimal remaining = total.subtract(paid);
        return remaining.compareTo(BigDecimal.ZERO) > 0 ? remaining : BigDecimal.ZERO;
    }

    private JPanel createEncomendasTab() {
        return CommercialOrdersView.create(this);
    }
    /** Abre o editor de nova encomenda (painel completo, substitui o modal). */
    void openOrderEditor() {
        if (warehousesList.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Nenhum armazém disponível para a empresa atual.",
                    "Erro", JOptionPane.ERROR_MESSAGE);
            return;
        }
        resetOrderDraft();
        lastCreatedOrder = null;
        if (encomendasCards != null && encomendasHost != null) {
            encomendasCards.show(encomendasHost, "editor");
        }
    }

    void backToOrdersList() {
        if (encomendasCards != null && encomendasHost != null) {
            encomendasCards.show(encomendasHost, "list");
        }
    }

    /** Guardar a partir do editor: valida+cria, informa, recarrega a lista e volta. Erro mantém o editor. */
    void saveOrderFromEditor() {
        try {
            com.phcpro.modules.comercial.dto.CreateOrderRequest request = buildOrderRequest();
            UIHelper.runWithProgress(this, "A criar encomenda…", () -> comercialApiClient.createOrder(request), created -> {
            lastCreatedOrder = created;
            String estadoMsg = "PENDING_APPROVAL".equals(created.status())
                    ? "Submetida para aprovação (valor: " + created.totalAmount() + " MT)."
                    : "Estado: " + created.status() + " (valor: " + created.totalAmount() + " MT).";
            JOptionPane.showMessageDialog(this, "Encomenda " + created.orderNumber() + " criada!\n" + estadoMsg,
                    "Sucesso", JOptionPane.INFORMATION_MESSAGE);
            loadOrdersTable();
            backToOrdersList();
            }, error -> showCommercialError("criar encomenda", error));
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage() == null ? "Falha ao criar encomenda." : ex.getMessage(),
                    "Erro", JOptionPane.ERROR_MESSAGE);
        }
    }

    /** Helper de venda ao grosso na encomenda: nº de caixas → Qtd em unidades. Ver {@link #applyInvoiceBoxes()}. */
    void applyOrderBoxes() {
        if (orderBoxesField == null) return;
        String raw = orderBoxesField.getText().trim();
        if (raw.isEmpty()) return;
        int idx = orderProductCombo.getSelectedIndex();
        if (idx < 0 || idx >= productsList.size()) return;
        int upb = Math.max(1, productsList.get(idx).unitsPerBox());
        try {
            int boxes = Integer.parseInt(raw);
            if (boxes <= 0) return;
            orderQuantityField.setText(String.valueOf(boxes * upb));
        } catch (NumberFormatException ignore) {
            // texto inválido → não altera a Qtd
        }
    }

    void addDraftOrderLine() {
        if (productsList.isEmpty()) return;

        int selectedProdIdx = orderProductCombo.getSelectedIndex();
        if (selectedProdIdx < 0) return;

        ProductDTO product = productsList.get(selectedProdIdx);

        int qty;
        try {
            qty = orderQuantityField.value().intValueExact();
            if (qty <= 0) throw new NumberFormatException();
        } catch (RuntimeException e) {
            JOptionPane.showMessageDialog(this, "A quantidade deve ser um número inteiro superior a zero.", "Erro", JOptionPane.ERROR_MESSAGE);
            return;
        }

        BigDecimal discount = BigDecimal.ZERO;
        try {
            discount = orderDiscountField.value();
            if (discount.compareTo(BigDecimal.ZERO) < 0 || discount.compareTo(BigDecimal.valueOf(100)) > 0) {
                throw new NumberFormatException();
            }
        } catch (RuntimeException e) {
            JOptionPane.showMessageDialog(this, "O desconto deve ser um número decimal entre 0 e 100.", "Erro", JOptionPane.ERROR_MESSAGE);
            return;
        }

        // Lote é decidido por FEFO no backend — orderBatchField mostra apenas previsão.
        String previewBatch = orderBatchField.getText().trim();
        String batch = null;

        String serial = orderSerialField.getText().trim();
        if (serial.isEmpty()) serial = null;

        // A taxa é a do artigo (o backend resolve-a na mesma; isto só mantém a pré-visualização
        // do rascunho igual ao que vai ser cobrado).
        BigDecimal taxRate = product.effectiveTaxRate();

        CreateInvoiceLineRequest lineRequest = new CreateInvoiceLineRequest(
                product.id(),
                qty,
                taxRate,
                discount,
                batch,
                serial
        );
        draftOrderLines.add(lineRequest);

        // Add to GUI table
        BigDecimal subTotal = product.unitPrice().multiply(BigDecimal.valueOf(qty));
        if (discount.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal discAmt = subTotal.multiply(discount.divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP));
            subTotal = subTotal.subtract(discAmt);
        }
        BigDecimal tax = subTotal.multiply(taxRate);
        BigDecimal total = subTotal.add(tax).setScale(2, RoundingMode.HALF_UP);

        String lotSer = "";
        if (!previewBatch.isEmpty() && !"Sem stock".equals(previewBatch)) {
            lotSer += "FEFO: " + previewBatch + " ";
        }
        if (serial != null) lotSer += "S: " + serial;
        if (lotSer.isEmpty()) lotSer = "-";

        orderLinesTableModel.addRow(new Object[]{
                product.name(),
                qty,
                product.unitPrice() + " MT",
                discount + "%",
                lotSer,
                total + " MT"
        });

        // Accumulate totals
        draftOrderSubtotal = draftOrderSubtotal.add(subTotal);
        draftOrderTax = draftOrderTax.add(tax);
        draftOrderTotal = draftOrderTotal.add(total);

        orderTotalLabel.setText(String.format("Total Rascunho: %,.2f MT (incl. IVA)", draftOrderTotal));

        // Clear details
        orderQuantityField.setText("1");
        orderBoxesField.setText("");
        orderDiscountField.setText("0");
        orderSerialField.setText("");
        refreshOrderFEFOHint();
    }

    /**
     * Pré-visualiza o lote/validade que vai sair (FEFO) no ecrã de faturas, com base no produto e
     * armazém escolhidos. Quando a linha for confirmada, o backend volta a aplicar FEFO em
     * transacção — esta consulta serve só para mostrar a previsão ao utilizador.
     */
    void refreshInvoiceFEFOHint() {
        renderFEFOHint(productCombo, warehouseCombo, batchField, productsList);
    }

    void refreshOrderFEFOHint() {
        renderFEFOHint(orderProductCombo, orderWarehouseCombo, orderBatchField, productsList);
    }

    private void renderFEFOHint(JComboBox<String> productBox, JComboBox<String> warehouseBox,
                                  JTextField targetField, List<ProductDTO> sourceProducts) {
        if (targetField == null) return;
        int prodIdx = productBox.getSelectedIndex();
        int whIdx = warehouseBox.getSelectedIndex();
        if (prodIdx < 0 || whIdx < 0
                || sourceProducts == null || prodIdx >= sourceProducts.size()
                || warehousesList.isEmpty() || whIdx >= warehousesList.size()) {
            targetField.setText("");
            return;
        }
        ProductDTO product = sourceProducts.get(prodIdx);
        WarehouseDTO warehouse = warehousesList.get(whIdx);
        UIHelper.loadAsync(this, () -> inventoryApiClient.findNextFEFO(product.id(), warehouse.id()), result ->
                result.ifPresentOrElse(
                    b -> {
                        String lote = b.batchNumber() == null ? "—" : b.batchNumber();
                        String val = b.expirationDate() == null
                                ? "—"
                                : b.expirationDate().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy"));
                        targetField.setText(lote + "  •  " + val);
                    },
                    () -> targetField.setText("Sem stock")), error -> targetField.setText(""));
    }

    /** Validação + emissão da encomenda. Lança {@link RuntimeException} em erro para manter o editor aberto. */
    private com.phcpro.modules.comercial.dto.CreateOrderRequest buildOrderRequest() {
        if (warehousesList.isEmpty()) {
            throw new RuntimeException("Nenhum armazém disponível para a empresa atual.");
        }
        if (draftOrderLines.isEmpty()) {
            throw new RuntimeException("Adicione pelo menos um item à encomenda.");
        }
        int clientIdx = orderClientCombo.getSelectedIndex();
        int whIdx = orderWarehouseCombo.getSelectedIndex();
        if (whIdx < 0) {
            throw new RuntimeException("Selecione o armazém.");
        }

        // O índice 0 do combo é "Consumidor Final"; índices >0 mapeiam para clientsList[idx-1].
        Long clientId = null;
        String walkInName = null;
        if (clientIdx > 0 && (clientIdx - 1) < clientsList.size()) {
            clientId = clientsList.get(clientIdx - 1).id();
        } else {
            String typed = orderClientWalkInField == null ? "" : orderClientWalkInField.getText().trim();
            if (!typed.isEmpty()) walkInName = typed;
        }

        WarehouseDTO warehouse = warehousesList.get(whIdx);
        Long companyId = CurrentUserContext.getCurrentCompanyId();
        return new com.phcpro.modules.comercial.dto.CreateOrderRequest(
                clientId, walkInName, companyId, warehouse.id(), new ArrayList<>(draftOrderLines));
    }

    /** Limpa o rascunho da encomenda (linhas, totais e selecção) antes de abrir o modal. */
    private void resetOrderDraft() {
        draftOrderLines.clear();
        draftOrderSubtotal = BigDecimal.ZERO;
        draftOrderTax = BigDecimal.ZERO;
        draftOrderTotal = BigDecimal.ZERO;
        if (orderLinesTableModel != null) orderLinesTableModel.setRowCount(0);
        if (orderTotalLabel != null) orderTotalLabel.setText("Total Rascunho: 0.00 MT (incl. IVA)");
        if (orderClientWalkInField != null) orderClientWalkInField.setText("");
        if (orderClientCombo != null && orderClientCombo.getItemCount() > 0) orderClientCombo.setSelectedIndex(0);
    }

    void billSelectedOrder() {
        int row = TableFilter.selectedModelRow(ordersTable);
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Selecione uma encomenda na tabela para faturar.", "Aviso", JOptionPane.WARNING_MESSAGE);
            return;
        }

        Long orderId = (Long) ordersTableModel.getValueAt(row, 0);
        String orderNum = (String) ordersTableModel.getValueAt(row, 1);
        String statusStr = (String) ordersTableModel.getValueAt(row, 3);

        if (!"PENDING".equalsIgnoreCase(statusStr)) {
            JOptionPane.showMessageDialog(this, "Apenas encomendas no estado PENDING podem ser faturadas. Estado atual: " + statusStr, "Erro", JOptionPane.ERROR_MESSAGE);
            return;
        }

        UIHelper.runWithProgress(this, "A faturar encomenda…", () -> comercialApiClient.billOrder(orderId), invoice -> {
            JOptionPane.showMessageDialog(this, "Encomenda " + orderNum + " faturada com sucesso!\n" +
                    "Fatura " + invoice.invoiceNumber() + " gerada com o mesmo número de sequência.", "Sucesso", JOptionPane.INFORMATION_MESSAGE);

            loadOrdersTable();
            loadInvoicesTable();
        }, error -> showCommercialError("faturar encomenda", error));
    }

    void convertSelectedOrderToGuide() {
        int row = TableFilter.selectedModelRow(ordersTable);
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Selecione uma encomenda na tabela para converter em guia.",
                    "Aviso", JOptionPane.WARNING_MESSAGE);
            return;
        }

        Long orderId = (Long) ordersTableModel.getValueAt(row, 0);
        String status = String.valueOf(ordersTableModel.getValueAt(row, 3));
        if (!"PENDING".equals(status)) {
            JOptionPane.showMessageDialog(this,
                    "Apenas encomendas aprovadas no estado PENDING podem ser convertidas em guia. Estado atual: " + status,
                    "Erro", JOptionPane.ERROR_MESSAGE);
            return;
        }

        UIHelper.loadAsync(this, () -> comercialApiClient.getOrderById(orderId), this::showConvertOrderToGuideDialog,
                error -> showCommercialLoadError("encomenda", error));
    }

    private void showConvertOrderToGuideDialog(OrderDTO order) {

        JTextField orderField = new JTextField(order.orderNumber());
        JTextField clientField = new JTextField(order.clientName());
        JTextField totalField = new JTextField(String.format("%,.2f MT", order.totalAmount()));
        JTextField responsibleField = new JTextField();
        JTextField vehicleField = new JTextField();
        JTextArea notesArea = new JTextArea(3, 28);
        for (JTextField field : List.of(orderField, clientField, totalField, responsibleField, vehicleField)) {
            UIHelper.styleTextField(field);
        }
        orderField.setEditable(false);
        clientField.setEditable(false);
        totalField.setEditable(false);
        responsibleField.putClientProperty("JTextField.placeholderText", "Nome de quem acompanha a expedição");
        vehicleField.putClientProperty("JTextField.placeholderText", "Matrícula ou identificação da viatura");
        UIHelper.styleTextArea(notesArea);
        notesArea.setLineWrap(true);
        notesArea.setWrapStyleWord(true);
        JScrollPane notesScroll = new JScrollPane(notesArea);
        UIHelper.styleScrollPane(notesScroll);

        JPanel form = UIHelper.createDialogForm(
                "Encomenda:", orderField,
                "Cliente:", clientField,
                "Total:", totalField,
                "Responsável pelo transporte:", responsibleField,
                "Viatura / Matrícula:", vehicleField,
                "Observações:", notesScroll
        );

        DeliveryGuideDTO[] created = new DeliveryGuideDTO[1];
        ModernFormDialog dialog = new ModernFormDialog(SwingUtilities.getWindowAncestor(this),
                "Converter em Guia", "fas-truck",
                "Dados de transporte da Guia de Remessa", form)
                .setConfirmButton("Criar Guia", "fas-truck")
                .setOnSaveAsync(() -> {
                    String responsible = blankToNull(responsibleField.getText());
                    String vehicle = blankToNull(vehicleField.getText());
                    String notes = blankToNull(notesArea.getText());
                    return () -> created[0] = comercialApiClient.createDeliveryGuide(
                            order.id(), responsible, vehicle, notes);
                });

        if (dialog.showDialog() && created[0] != null) {
            JOptionPane.showMessageDialog(this,
                    "Guia " + created[0].guideNumber() + " criada e submetida para aprovação.\n"
                            + "O stock só será movimentado quando a guia for aprovada.",
                    "Sucesso", JOptionPane.INFORMATION_MESSAGE);
            loadOrdersTable();
            deliveryGuidesPanel.refresh();
        }
    }

    private static String blankToNull(String value) {
        if (value == null || value.isBlank()) return null;
        return value.trim();
    }

    public void loadOrdersTable() {
        Long companyId = CurrentUserContext.getCurrentCompanyId();
        UIHelper.loadAsync(this, () -> comercialApiClient.getOrdersByCompany(companyId), this::applyOrders,
                error -> showCommercialLoadError("encomendas", error));
    }

    private void applyOrders(List<OrderDTO> orders) {
        ordersTableModel.setRowCount(0);
        java.time.format.DateTimeFormatter dtfShort =
                java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy");
        for (OrderDTO order : orders) {
            String clientLabel = order.clientName();
            if (order.walkInName() != null && !order.walkInName().isBlank()) {
                clientLabel += " — " + order.walkInName();
            }
            // Coluna combinada: "—" se nunca; "N × dd/MM/yyyy" se impressa.
            // Operador + hora completos ficam no diálogo "Ver Detalhes".
            String impressoes;
            if (order.printCount() <= 0) {
                impressoes = "—";
            } else if (order.printedAt() != null) {
                impressoes = order.printCount() + " × " + order.printedAt().format(dtfShort);
            } else {
                impressoes = String.valueOf(order.printCount());
            }
            ordersTableModel.addRow(new Object[]{
                    order.id(),
                    order.orderNumber(),
                    clientLabel,
                    order.status(),
                    order.totalAmount(),
                    impressoes
            });
        }
    }

    void openBillFromOrderDialog() {
        billOrderDialog.open();
    }
    void openCancelOrderDialog() {
        cancelOrderDialog.open();
    }
    void printSelectedInvoice() {
        int row = TableFilter.selectedModelRow(invoicesTable);
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Selecione uma fatura na tabela.", "Aviso", JOptionPane.WARNING_MESSAGE);
            return;
        }
        Long invoiceId = (Long) invoicesTableModel.getValueAt(row, 0);
        String invoiceNum = String.valueOf(invoicesTableModel.getValueAt(row, 1));
        UIHelper.runWithProgress(this, "A gerar fatura em PDF…", () -> comercialApiClient.renderInvoice(invoiceId),
                pdf -> com.phcpro.modules.printing.PdfFileSaver.saveAndOpen(pdf, "fatura-" + invoiceNum),
                error -> showCommercialError("gerar fatura em PDF", error));
    }

    void printSelectedGuide() {
        int row = TableFilter.selectedModelRow(invoicesTable);
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Selecione uma fatura na tabela.", "Aviso", JOptionPane.WARNING_MESSAGE);
            return;
        }
        Long invoiceId = (Long) invoicesTableModel.getValueAt(row, 0);
        String invoiceNum = String.valueOf(invoicesTableModel.getValueAt(row, 1));
        UIHelper.runWithProgress(this, "A gerar guia em PDF…", () -> comercialApiClient.renderGuide(invoiceId),
                pdf -> com.phcpro.modules.printing.PdfFileSaver.saveAndOpen(pdf, "guia-remessa-" + invoiceNum),
                error -> showCommercialError("gerar guia em PDF", error));
    }

    void exportInvoicesTable() {
        try {
            com.phcpro.modules.company.model.Company company = currentCompany();
            byte[] pdf = com.phcpro.modules.printing.TablePdfExporter.renderFromSwing(company, "Faturas Emitidas", invoicesTable);
            com.phcpro.modules.printing.PdfFileSaver.saveAndOpen(pdf, "faturas-export");
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Erro ao exportar: " + ex.getMessage(), "Erro", JOptionPane.ERROR_MESSAGE);
        }
    }

    void printSelectedOrder() {
        int row = TableFilter.selectedModelRow(ordersTable);
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Selecione uma encomenda na tabela.", "Aviso", JOptionPane.WARNING_MESSAGE);
            return;
        }
        Long orderId = (Long) ordersTableModel.getValueAt(row, 0);
        orderDetailsDialog.print(orderId);
    }

    void openSelectedOrderDetails() {
        int row = TableFilter.selectedModelRow(ordersTable);
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Selecione uma encomenda na tabela.", "Aviso", JOptionPane.WARNING_MESSAGE);
            return;
        }
        orderDetailsDialog.open((Long) ordersTableModel.getValueAt(row, 0));
    }

    void exportOrdersTable() {
        try {
            com.phcpro.modules.company.model.Company company = currentCompany();
            byte[] pdf = com.phcpro.modules.printing.TablePdfExporter.renderFromSwing(company, "Encomendas", ordersTable);
            com.phcpro.modules.printing.PdfFileSaver.saveAndOpen(pdf, "encomendas-export");
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Erro ao exportar: " + ex.getMessage(), "Erro", JOptionPane.ERROR_MESSAGE);
        }
    }

    private com.phcpro.modules.company.model.Company currentCompany() {
        // Cliente-fino: o exportador de tabelas só precisa do id da empresa activa (cabeçalho do PDF).
        com.phcpro.modules.company.model.Company company = new com.phcpro.modules.company.model.Company();
        company.setId(CurrentUserContext.getCurrentCompanyId());
        return company;
    }

    private void showCommercialLoadError(String area, Throwable error) {
        JOptionPane.showMessageDialog(this, "Não foi possível carregar " + area + ": " + error.getMessage(),
                "Erro", JOptionPane.ERROR_MESSAGE);
    }

    private void showCommercialError(String action, Throwable error) {
        JOptionPane.showMessageDialog(this, "Não foi possível " + action + ": " + error.getMessage(),
                "Erro", JOptionPane.ERROR_MESSAGE);
    }

    private record CommercialMetadata(List<ClientDTO> clients, List<ProductDTO> products) {}
}
