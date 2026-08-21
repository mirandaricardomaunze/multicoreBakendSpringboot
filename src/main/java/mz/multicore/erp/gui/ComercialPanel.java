package mz.multicore.erp.gui;

import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.gui.components.DocumentEditorHost;
import mz.multicore.erp.gui.components.ModernButton;
import mz.multicore.erp.gui.components.ModernFormDialog;
import mz.multicore.erp.gui.components.ModernPanel;
import mz.multicore.erp.gui.components.TableFilter;
import mz.multicore.erp.gui.components.TableCellRenderers;
import mz.multicore.erp.gui.components.MoneyField;
import mz.multicore.erp.gui.components.QuantityField;
import mz.multicore.erp.gui.components.PackageQuantityEditor;
import mz.multicore.erp.gui.components.DecimalField;
import mz.multicore.erp.gui.components.UIHelper;
import mz.multicore.erp.gui.commercial.CommercialMovementsPanel;
import mz.multicore.erp.gui.commercial.OutstandingAccountsPanel;
import mz.multicore.erp.gui.commercial.CommercialNotesPanel;
import mz.multicore.erp.gui.commercial.DeliveryGuidesPanel;
import mz.multicore.erp.gui.commercial.QuotationsPanel;
import mz.multicore.erp.gui.commercial.ReceiptsPanel;
import mz.multicore.erp.gui.commercial.BillOrderDialog;
import mz.multicore.erp.gui.commercial.CancelOrderDialog;
import mz.multicore.erp.gui.commercial.OrderDetailsDialog;
import mz.multicore.erp.desktop.client.ComercialApiClient;
import mz.multicore.erp.desktop.client.CreditNoteApiClient;
import mz.multicore.erp.desktop.client.DebitNoteApiClient;
import mz.multicore.erp.desktop.client.FinanceApiClient;
import mz.multicore.erp.desktop.client.InventoryApiClient;
import mz.multicore.erp.desktop.client.MovimentosApiClient;
import mz.multicore.erp.desktop.client.POSApiClient;
import mz.multicore.erp.desktop.client.PromotionApiClient;
import mz.multicore.erp.modules.comercial.dto.*;
import mz.multicore.erp.modules.comercial.model.InvoiceStatus;
import mz.multicore.erp.modules.inventory.dto.WarehouseDTO;
import mz.multicore.erp.modules.financeira.dto.TreasuryAccountDTO;

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
    private final JTabbedPane commercialTabs;

    // TAB 1: FATURAÇÃO ELEMENTS
    JComboBox<String> clientCombo;
    JComboBox<String> warehouseCombo;
    JComboBox<String> productCombo;
    QuantityField quantityField;
    QuantityField invoiceBoxesField;
    QuantityField invoiceLooseUnitsField;
    PackageQuantityEditor invoicePackageEditor;
    DecimalField discountField;
    JTextField batchField;
    JTextField serialField;
    DefaultTableModel linesTableModel;
    JTable linesTable;
    JLabel totalLabel;

    DefaultTableModel invoicesTableModel;
    mz.multicore.erp.gui.components.TablePager invoicesPager;
    JTable invoicesTable;

    // TAB 2: RECIBOS ELEMENTS

    // TAB 3: REGISTAR CLIENTE ELEMENTS

    // TAB 4: ENCOMENDAS ELEMENTS
    JComboBox<mz.multicore.erp.modules.comercial.model.OrderKind> orderKindCombo;
    JComboBox<String> orderClientCombo;
    JTextField orderClientWalkInField;
    JComboBox<String> orderWarehouseCombo;
    /** Armazém que recebe, só visível na reposição interna. Ver docs/REPOSICAO_INTERNA_SPEC.md. */
    JComboBox<String> orderDestinationCombo;
    JComboBox<String> orderProductCombo;
    QuantityField orderQuantityField;
    QuantityField orderBoxesField;
    QuantityField orderLooseUnitsField;
    PackageQuantityEditor orderPackageEditor;
    JLabel orderQuantityLabel;
    DecimalField orderDiscountField;
    JTextField orderBatchField;
    JTextField orderSerialField;
    DefaultTableModel orderLinesTableModel;
    JTable orderLinesTable;
    JLabel orderTotalLabel;
    JLabel orderLoadLabel;

    DefaultTableModel ordersTableModel;
    JTable ordersTable;

    /** Colunas da tabela de encomendas lidas pelas acções. Nomeadas para o índice não andar solto. */
    static final int ORDERS_COL_ID = 0;
    static final int ORDERS_COL_STATUS = 3;
    static final int ORDERS_COL_KIND = 6;
    static final int ORDERS_COL_ORIGIN = 7;
    static final int ORDERS_COL_DELIVERY = 8;

    // TAB 5: GUIAS DE REMESSA ELEMENTS

    // Seeding lists for selections
    List<ClientDTO> clientsList = new ArrayList<>();
    final List<ProductDTO> productsList = new ArrayList<>();
    List<WarehouseDTO> warehousesList = new ArrayList<>();
    
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
    private mz.multicore.erp.modules.comercial.dto.InvoiceDTO lastCreatedInvoice;
    JPanel orderFormContent;                // conteúdo do editor de nova encomenda
    OrderDTO lastCreatedOrder;
    CardLayout encomendasCards;             // alterna lista <-> editor na aba Encomendas
    JPanel encomendasHost;
    CardLayout faturacaoCards;              // alterna lista <-> editor na aba Faturação
    JPanel faturacaoHost;
    private final CommercialMovementsPanel movementsPanel;
    private final OutstandingAccountsPanel outstandingAccountsPanel;
    private final CommercialNotesPanel notesPanel;
    private final DeliveryGuidesPanel deliveryGuidesPanel;
    private final QuotationsPanel quotationsPanel;
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
            PromotionApiClient promotionApiClient,
            /** Atalho para as transferências entre armazéns (Stock); {@code null} desliga-o. */
            Runnable openWarehouseTransfers
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
        this.deliveryGuidesPanel = new DeliveryGuidesPanel(comercialApiClient, this::loadOrdersTable,
                openWarehouseTransfers);
        this.receiptsPanel = new ReceiptsPanel(comercialApiClient, this::loadInvoicesTable);
        this.quotationsPanel = new QuotationsPanel(comercialApiClient, () -> List.copyOf(clientsList),
                () -> List.copyOf(productsList), () -> List.copyOf(warehousesList), this::loadOrdersTable);
        this.billOrderDialog = new BillOrderDialog(this, comercialApiClient,
                this::loadInvoicesTable, this::loadOrdersTable);
        this.cancelOrderDialog = new CancelOrderDialog(this, comercialApiClient, this::loadOrdersTable);
        this.orderDetailsDialog = new OrderDetailsDialog(this, comercialApiClient, this::loadOrdersTable);

        setLayout(new BorderLayout());
        setBackground(UIHelper.BG_DARK);
        setBorder(new EmptyBorder(10, 10, 10, 10));

        commercialTabs = new JTabbedPane();
        JTabbedPane tabbedPane = commercialTabs;
        UIHelper.styleTabbedPaneMulticore(tabbedPane);

        // A cotação vem antes da faturação porque é aí que o ciclo comercial começa:
        // cotação → encomenda → guia/fatura.
        tabbedPane.addTab("Cotações (CT)", UIHelper.icon("fas-file-signature", 16, UIHelper.TEXT_LIGHT),
                quotationsPanel);

        JPanel tabFaturacao = createFaturacaoTab();
        tabbedPane.addTab("Faturação (FT)", UIHelper.icon("fas-file-invoice", 16, UIHelper.TEXT_LIGHT), tabFaturacao);

        tabbedPane.addTab("Recibos (RC)", UIHelper.icon("fas-receipt", 16, UIHelper.TEXT_LIGHT), receiptsPanel);

        JPanel tabEncomendas = createEncomendasTab();
        tabbedPane.addTab("Pedidos & Separação", UIHelper.icon("fas-clipboard-list", 16, UIHelper.TEXT_LIGHT), tabEncomendas);

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

    public void showCustomerOrders() {
        for (int index = 0; index < commercialTabs.getTabCount(); index++) {
            if (!commercialTabs.getTitleAt(index).startsWith("Pedidos")) continue;
            commercialTabs.setSelectedIndex(index);
            loadOrdersTable();
            return;
        }
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
        quotationsPanel.refresh();
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
        productsList.clear();
        productsList.addAll(metadata.products());

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
        if (orderDestinationCombo != null) orderDestinationCombo.removeAllItems();
        warehousesList = loaded;

        for (WarehouseDTO w : warehousesList) {
            warehouseCombo.addItem(w.name());
            orderWarehouseCombo.addItem(w.name());
            if (orderDestinationCombo != null) orderDestinationCombo.addItem(w.name());
        }
    }


    /**
     * Helper de venda ao grosso: se o operador indicar um nº de caixas e houver produto seleccionado,
     * preenche a Qtd em UNIDADES = caixas × unidades/caixa. O cálculo de dinheiro continua por unidade
     * (a caixa é só conversão). Campo vazio não mexe na Qtd (permite entrada directa em unidades).
     */
    void refreshInvoicePackaging() {
        if (invoicePackageEditor == null) return;
        int idx = productCombo.getSelectedIndex();
        if (idx < 0 || idx >= productsList.size()) return;
        invoicePackageEditor.setUnitsPerBox(productsList.get(idx).unitsPerBox());
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
        invoicePackageEditor.reset();
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

    /** Recarrega a listagem de faturas na primeira página (a tabela vem paginada do servidor). */
    public void loadInvoicesTable() {
        if (invoicesPager != null) invoicesPager.reload();
    }

    /** Carrega uma página de faturas. Ligado ao {@link TablePager} da aba de Faturação. */
    void loadInvoicesPage(int page, int size) {
        Long companyId = CurrentUserContext.getCurrentCompanyId();
        UIHelper.loadAsync(this, () -> comercialApiClient.getInvoicePage(companyId, page, size),
                response -> {
                    invoicesPager.apply(response);
                    applyInvoices(response.items());
                },
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
                    invoice.totalAmount(), invoice.outstandingAmount()
            });
        }
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

    void saveOrderFromEditor() {
        CommercialOrderSubmission.save(this, comercialApiClient);
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
                qty + " (" + OrderPackageQuantityBinding.label(product, qty) + ")",
                BigDecimal.ZERO, "0%", "0%",
                product.unitPrice() + " MT",
                discount + "%",
                lotSer,
                total + " MT"
        });
        OrderPackageQuantityBinding.refreshDraftLogistics(this);

        // Accumulate totals
        draftOrderSubtotal = draftOrderSubtotal.add(subTotal);
        draftOrderTax = draftOrderTax.add(tax);
        draftOrderTotal = draftOrderTotal.add(total);

        orderTotalLabel.setText(String.format("Total Rascunho: %,.2f MT (incl. IVA)", draftOrderTotal));

        // Clear details
        orderPackageEditor.reset();
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

    /** Via escolhida no editor — ver {@link CommercialOrderSubmission#selectedKind}. */
    mz.multicore.erp.modules.comercial.model.OrderKind selectedOrderKind() {
        return CommercialOrderSubmission.selectedKind(this);
    }

    /** Limpa o rascunho da encomenda (linhas, totais e selecção) antes de abrir o modal. */
    private void resetOrderDraft() {
        draftOrderLines.clear();
        draftOrderSubtotal = BigDecimal.ZERO;
        draftOrderTax = BigDecimal.ZERO;
        draftOrderTotal = BigDecimal.ZERO;
        if (orderLinesTableModel != null) orderLinesTableModel.setRowCount(0);
        if (orderTotalLabel != null) orderTotalLabel.setText("Total Rascunho: 0.00 MT (incl. IVA)");
        if (orderLoadLabel != null) orderLoadLabel.setText("Carga: 0.000 kg");
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

        if (!"SEPARATED".equalsIgnoreCase(statusStr)) {
            JOptionPane.showMessageDialog(this, "Apenas pedidos no estado SEPARATED podem ser faturados. Estado atual: " + statusStr, "Erro", JOptionPane.ERROR_MESSAGE);
            return;
        }

        UIHelper.runWithProgress(this, "A faturar pedido separado…",
                () -> comercialApiClient.billFulfillmentOrder(orderId, CustomerOrderFulfillmentActions.terminalName()), invoice -> {
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

        Long orderId = (Long) ordersTableModel.getValueAt(row, ORDERS_COL_ID);
        String status = String.valueOf(ordersTableModel.getValueAt(row, ORDERS_COL_STATUS));

        // A via decide que guia sai — remessa ao cliente ou transferência entre armazéns. Mesmo
        // princípio da impressão: o documento é um facto da encomenda, não do estado em que está.
        Object kindCell = ordersTableModel.getValueAt(row, ORDERS_COL_KIND);
        if (kindCell instanceof mz.multicore.erp.modules.comercial.model.OrderKind kind
                && kind.usesWarehouseTransfer()) {
            UIHelper.loadAsync(this, () -> comercialApiClient.getOrderById(orderId),
                    order -> InternalReplenishmentActions.showConvertDialog(this, comercialApiClient, order),
                    error -> showCommercialLoadError("encomenda", error));
            return;
        }

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
                    impressoes,
                    order.kind(),
                    order.quotationNumber() == null ? "—" : order.quotationNumber(),
                    formatExpectedDelivery(order)
            });
        }
    }

    /** Data prometida, com o atraso que o <b>servidor</b> calculou — o painel não compara datas. */
    private static String formatExpectedDelivery(OrderDTO order) {
        if (order.expectedDeliveryDate() == null) return "—";
        String date = order.expectedDeliveryDate()
                .format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy"));
        return order.deliveryOverdue() ? date + " (em atraso)" : date;
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
                pdf -> mz.multicore.erp.modules.printing.PdfFileSaver.saveAndOpen(pdf, "fatura-" + invoiceNum),
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
                pdf -> mz.multicore.erp.modules.printing.PdfFileSaver.saveAndOpen(pdf, "guia-remessa-" + invoiceNum),
                error -> showCommercialError("gerar guia em PDF", error));
    }

    void exportInvoicesTable() {
        try {
            mz.multicore.erp.modules.company.model.Company company = currentCompany();
            byte[] pdf = mz.multicore.erp.modules.printing.TablePdfExporter.renderFromSwing(company, "Faturas Emitidas", invoicesTable);
            mz.multicore.erp.modules.printing.PdfFileSaver.saveAndOpen(pdf, "faturas-export");
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Erro ao exportar: " + ex.getMessage(), "Erro", JOptionPane.ERROR_MESSAGE);
        }
    }

    void printSelectedOrder() {
        CustomerOrderFulfillmentActions.printSelected(this, comercialApiClient);
    }

    void completeSelectedOrderSeparation() {
        CustomerOrderFulfillmentActions.completeSeparation(this, comercialApiClient);
    }

    void showSelectedOrderEvents() {
        CustomerOrderFulfillmentActions.showEvents(this, comercialApiClient);
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
            mz.multicore.erp.modules.company.model.Company company = currentCompany();
            byte[] pdf = mz.multicore.erp.modules.printing.TablePdfExporter.renderFromSwing(company, "Encomendas", ordersTable);
            mz.multicore.erp.modules.printing.PdfFileSaver.saveAndOpen(pdf, "encomendas-export");
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Erro ao exportar: " + ex.getMessage(), "Erro", JOptionPane.ERROR_MESSAGE);
        }
    }

    private mz.multicore.erp.modules.company.model.Company currentCompany() {
        // Cliente-fino: o exportador de tabelas só precisa do id da empresa activa (cabeçalho do PDF).
        mz.multicore.erp.modules.company.model.Company company = new mz.multicore.erp.modules.company.model.Company();
        company.setId(CurrentUserContext.getCurrentCompanyId());
        return company;
    }

    private void showCommercialLoadError(String area, Throwable error) {
        JOptionPane.showMessageDialog(this, "Não foi possível carregar " + area + ": " + error.getMessage(),
                "Erro", JOptionPane.ERROR_MESSAGE);
    }

    void showCommercialError(String action, Throwable error) {
        JOptionPane.showMessageDialog(this, "Não foi possível " + action + ": " + error.getMessage(),
                "Erro", JOptionPane.ERROR_MESSAGE);
    }

    private record CommercialMetadata(List<ClientDTO> clients, List<ProductDTO> products) {}
}
