package mz.multicore.erp.gui;

import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.gui.components.DocumentEditorHost;
import mz.multicore.erp.gui.components.ModernButton;
import mz.multicore.erp.gui.components.ModernFormDialog;
import mz.multicore.erp.gui.components.ModernPanel;
import mz.multicore.erp.gui.components.TableFilter;
import mz.multicore.erp.gui.components.TableCellRenderers;
import mz.multicore.erp.gui.components.MoneyField;
import mz.multicore.erp.gui.components.ProductSearchComboBox;
import mz.multicore.erp.gui.components.DecimalField;
import mz.multicore.erp.gui.components.UIHelper;
import mz.multicore.erp.gui.components.CustomerCreditValidator;
import mz.multicore.erp.gui.components.FeedbackType;
import mz.multicore.erp.gui.components.InlineFeedbackPanel;
import mz.multicore.erp.gui.components.ToastManager;
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
import mz.multicore.erp.gui.components.PrintPreviewDialog;
import mz.multicore.erp.desktop.client.PrintApiClient;
import mz.multicore.erp.gui.components.TableExportAction;

public class ComercialPanel extends JPanel {

    final ComercialApiClient comercialApiClient;
    private final PrintApiClient printApiClient;
    private final InventoryApiClient inventoryApiClient;
    private final FinanceApiClient financeApiClient;
    private final JTabbedPane commercialTabs;
    private final InlineFeedbackPanel feedback = new InlineFeedbackPanel();

    // TAB 1: FATURAÇÃO ELEMENTS
    JComboBox<String> clientCombo;
    JComboBox<String> warehouseCombo;
    ProductSearchComboBox productCombo;
    DecimalField discountField;
    JTextField serialField;
    DefaultTableModel linesTableModel;
    JTable linesTable;
    JLabel totalLabel;
    boolean invoiceGridEditable;
    boolean syncingInvoiceGrid;

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
    ProductSearchComboBox orderProductCombo;
    DecimalField orderDiscountField;
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
    final List<BigDecimal> draftOrderUnitPrices = new ArrayList<>();
    BigDecimal draftOrderSubtotal = BigDecimal.ZERO;
    BigDecimal draftOrderTax = BigDecimal.ZERO;
    BigDecimal draftOrderTotal = BigDecimal.ZERO;


    private final POSApiClient posApiClient;
    private final MovimentosApiClient movimentosApiClient;

    JPanel invoiceFormContent;              // conteúdo do modal de nova fatura
    private mz.multicore.erp.modules.comercial.dto.InvoiceDTO lastCreatedInvoice;
    JPanel orderFormContent;                // conteúdo do editor de nova encomenda
    DocumentEditorHost orderEditor;
    OrderDTO editingOrder;
    private boolean orderEditorDirty;
    boolean orderGridEditable;
    boolean syncingOrderGrid;
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
    final OrderDetailsDialog orderDetailsDialog;
    private final PromotionsPanel promotionsPanel;

    public ComercialPanel(
            ComercialApiClient comercialApiClient,
            InventoryApiClient inventoryApiClient,
            FinanceApiClient financeApiClient,
            CreditNoteApiClient creditNoteApiClient,
            DebitNoteApiClient debitNoteApiClient,
            POSApiClient posApiClient,
            MovimentosApiClient movimentosApiClient,
            PromotionApiClient promotionApiClient,
            PrintApiClient printApiClient,
            /** Atalho para as transferências entre armazéns (Stock); {@code null} desliga-o. */
            Runnable openWarehouseTransfers
    ) {
        this.comercialApiClient = comercialApiClient;
        this.printApiClient = printApiClient;
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
        this.promotionsPanel = new PromotionsPanel(promotionApiClient, comercialApiClient);

        setLayout(new BorderLayout());
        setBackground(UIHelper.BG_DARK);
        setBorder(new EmptyBorder(10, 10, 10, 10));

        commercialTabs = new JTabbedPane();
        JTabbedPane tabbedPane = commercialTabs;
        UIHelper.styleTabbedPaneMulticore(tabbedPane);

        // Rótulos sem o código da série: com dez separadores em SCROLL_TAB_LAYOUT, o que não cabe
        // desaparece atrás das setas. Guarda em CommercialTabStripFitsTest. Ordem = ciclo comercial.
        tabbedPane.addTab("Cotações", UIHelper.icon("fas-file-signature", 16, UIHelper.ACCENT_BLUE),
                quotationsPanel);

        JPanel tabFaturacao = createFaturacaoTab();
        tabbedPane.addTab("Faturação", UIHelper.icon("fas-file-invoice", 16, UIHelper.MODULE_COMERCIAL), tabFaturacao);

        tabbedPane.addTab("Recibos", UIHelper.icon("fas-receipt", 16, UIHelper.APPROVED_GREEN), receiptsPanel);

        JPanel tabEncomendas = createEncomendasTab();
        tabbedPane.addTab("Pedidos & Separação", UIHelper.icon("fas-clipboard-list", 16, UIHelper.ACCENT_CYAN), tabEncomendas);

        // TAB 5: GUIAS DE REMESSA (GR)
        tabbedPane.addTab("Guias", UIHelper.icon("fas-truck", 16, UIHelper.ACCENT_SKY),
                deliveryGuidesPanel);

        // TAB 6: NOTAS DE CRÉDITO (NC)
        tabbedPane.addTab("Notas de Crédito", UIHelper.icon("fas-undo-alt", 16, UIHelper.PENDING_YELLOW), notesPanel.creditTab());

        // TAB 7: NOTAS DE DÉBITO (ND)
        tabbedPane.addTab("Notas de Débito", UIHelper.icon("fas-plus-circle", 16, UIHelper.ACCENT_ORANGE), notesPanel.debitTab());

        // TAB 8: CONTAS CORRENTES (FIADOS)
        tabbedPane.addTab("Contas Correntes", UIHelper.icon("fas-hand-holding-usd", 16, UIHelper.REJECTED_RED), outstandingAccountsPanel);

        // TAB 9: PROMOÇÕES
        tabbedPane.addTab("Promoções", UIHelper.icon("fas-tags", 16, UIHelper.ACCENT_PINK), promotionsPanel);

        // TAB 10: MOVIMENTOS (vista unificada de todos os documentos comerciais)
        tabbedPane.addTab("Movimentos", UIHelper.icon("fas-list-alt", 16, UIHelper.ACCENT), movementsPanel);

        add(feedback, BorderLayout.NORTH);
        add(tabbedPane, BorderLayout.CENTER);
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
            showCommercialNotice(FeedbackType.WARNING, "Cliente necessário", "Registe um cliente antes de emitir a fatura.");
            return;
        }
        if (warehousesList.isEmpty()) {
            showCommercialNotice(FeedbackType.WARNING, "Armazém necessário",
                    "Registe um armazém para a empresa actual antes de emitir a fatura.");
            return;
        }
        resetInvoiceDraft();
        lastCreatedInvoice = null;
        invoiceGridEditable = true;
        if (faturacaoCards != null && faturacaoHost != null) {
            faturacaoCards.show(faturacaoHost, "editor");
        }
    }

    void backToInvoicesList() {
        if (faturacaoCards != null && faturacaoHost != null) {
            faturacaoCards.show(faturacaoHost, "list");
        }
        invoiceGridEditable = false;
    }

    /** Guardar a partir do editor: valida+cria, informa, recarrega a lista e volta. Erro mantém o editor. */
    void saveInvoiceFromEditor() {
        stopInvoiceCellEditing();
        try {
            int clientIdx = clientCombo.getSelectedIndex();
            if (clientIdx >= 0 && clientIdx < clientsList.size()) {
                ClientDTO client = clientsList.get(clientIdx);
                CustomerCreditValidator.CreditAssessment assessment =
                        CustomerCreditValidator.validateCreditPreFlight(client, draftTotal);
                if (!assessment.isApproved()) {
                    showCommercialNotice(FeedbackType.WARNING, "Crédito não autorizado", assessment.details());
                    return;
                }
            }
            CreateInvoiceRequest request = buildInvoiceRequest();
            UIHelper.runWithProgress(this, "A emitir fatura…", () -> comercialApiClient.createInvoice(request), created -> {
            lastCreatedInvoice = created;
            resetInvoiceDraft();
            if (created.status() == InvoiceStatus.PENDING_DISCOUNT_APPROVAL) {
                showCommercialNotice(FeedbackType.WARNING, "Fatura aguarda aprovação",
                        "Fatura " + created.invoiceNumber() + " emitida com desconto superior a 10%. Valor: "
                                + created.totalAmount() + " MT.");
            } else {
                ToastManager.success(this, "Fatura " + created.invoiceNumber() + " emitida · " + created.totalAmount() + " MT.");
            }
            loadInvoicesTable();
            backToInvoicesList();
            }, error -> showCommercialError("emitir fatura", error));
        } catch (Exception ex) {
            showCommercialNotice(FeedbackType.ERROR, "Não foi possível emitir a fatura",
                    ex.getMessage() == null ? "Tente novamente." : ex.getMessage());
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
        promotionsPanel.reload();
    }

    private void loadClientsAndProducts() {
        UIHelper.loadAsync(this, () -> new CommercialMetadata(comercialApiClient.getAllClients(),
                        comercialApiClient.getAllProducts()), this::applyClientsAndProducts,
                error -> showCommercialLoadError("clientes e produtos", error));
    }

    private void applyClientsAndProducts(CommercialMetadata metadata) {
        clientCombo.removeAllItems();
        orderClientCombo.removeAllItems();
        clientsList = metadata.clients();
        productsList.clear();
        productsList.addAll(metadata.products());

        // Encomendas aceitam venda sem cliente registado — primeiro item do combo.
        orderClientCombo.addItem("Consumidor Final (sem registo)");

        for (ClientDTO c : clientsList) {
            clientCombo.addItem(c.name() + " (" + c.taxId() + ")");
            orderClientCombo.addItem(c.name() + " (" + c.taxId() + ")");
        }

        productCombo.setProducts(productsList);
        orderProductCombo.setProducts(productsList);
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


    void addDraftLine() {
        if (!invoiceGridEditable) return;
        stopInvoiceCellEditing();
        draftLines.add(new CreateInvoiceLineRequest(
                0L, BigDecimal.ONE, BigDecimal.ZERO, BigDecimal.ZERO, null, null));
        refreshInvoiceGrid();
        int modelRow = linesTableModel.getRowCount() - 1;
        int rowCount = linesTable.getRowCount();
        if (modelRow >= 0 && rowCount > 0) {
            int viewRow = Math.min(Math.max(0, modelRow), rowCount - 1);
            try {
                if (modelRow < rowCount) {
                    viewRow = linesTable.convertRowIndexToView(modelRow);
                }
            } catch (Exception ignored) {
                viewRow = rowCount - 1;
            }
            if (viewRow >= 0 && viewRow < rowCount) {
                final int targetRow = viewRow;
                try {
                    linesTable.setRowSelectionInterval(targetRow, targetRow);
                    linesTable.scrollRectToVisible(linesTable.getCellRect(targetRow, 0, true));
                } catch (Exception ignored) {
                }
                SwingUtilities.invokeLater(() -> {
                    try {
                        if (targetRow < linesTable.getRowCount()) {
                            linesTable.editCellAt(targetRow, 0);
                            if (linesTable.getEditorComponent() != null) {
                                linesTable.getEditorComponent().requestFocusInWindow();
                            }
                        }
                    } catch (Exception ignored) {
                    }
                });
            }
        }
    }

    void syncInvoiceLineFromGrid(int row, int column) {
        if (row < 0 || row >= draftLines.size()) return;
        ProductDTO product = linesTableModel.getValueAt(row, 0) instanceof ProductDTO selected ? selected : null;
        int unitsPerPackage = product == null ? 1 : Math.max(1, product.unitsPerPackage());
        BigDecimal unitsPerBox = BigDecimal.valueOf(product == null ? 1L
                : (long) Math.max(1, product.packagesPerBox()) * unitsPerPackage);
        BigDecimal quantity;
        if (column == 2) quantity = decimalGrid(linesTableModel.getValueAt(row, 2))
                .multiply(BigDecimal.valueOf(unitsPerPackage));
        else if (column == 3) quantity = decimalGrid(linesTableModel.getValueAt(row, 3)).multiply(unitsPerBox);
        else quantity = decimalGrid(linesTableModel.getValueAt(row, 1));
        BigDecimal discount = decimalGrid(linesTableModel.getValueAt(row, 7));
        String serial = String.valueOf(linesTableModel.getValueAt(row, 8)).trim();
        if (serial.isEmpty() || "—".equals(serial)) serial = null;
        draftLines.set(row, new CreateInvoiceLineRequest(product == null ? 0L : product.id(), quantity,
                product == null ? BigDecimal.ZERO : product.effectiveTaxRate(), discount, null, serial));
        refreshInvoiceGrid();
    }

    void removeSelectedInvoiceDraftLine() {
        int row = linesTable.getSelectedRow();
        if (row < 0) {
            showCommercialNotice(FeedbackType.WARNING, "Seleccione uma linha", "Escolha um item para remover.");
            return;
        }
        draftLines.remove(linesTable.convertRowIndexToModel(row));
        refreshInvoiceGrid();
    }

    private void refreshInvoiceGrid() {
        syncingInvoiceGrid = true;
        try {
            linesTableModel.setRowCount(0);
        draftSubtotal = BigDecimal.ZERO;
        draftTax = BigDecimal.ZERO;
        draftTotal = BigDecimal.ZERO;
        for (CreateInvoiceLineRequest line : draftLines) {
            ProductDTO product = productsList.stream().filter(item -> item.id().equals(line.productId()))
                    .findFirst().orElse(null);
            BigDecimal price = product == null || product.unitPrice() == null ? BigDecimal.ZERO : product.unitPrice();
            BigDecimal discount = line.discountPercentage() == null ? BigDecimal.ZERO : line.discountPercentage();
            BigDecimal subtotal = price.multiply(line.quantity())
                    .multiply(BigDecimal.ONE.subtract(discount.movePointLeft(2)));
            BigDecimal tax = subtotal.multiply(product == null ? line.taxRate() : product.effectiveTaxRate());
            BigDecimal total = subtotal.add(tax).setScale(2, RoundingMode.HALF_UP);
            int unitsPerPackage = product == null ? 1 : Math.max(1, product.unitsPerPackage());
            BigDecimal unitsPerBox = BigDecimal.valueOf(product == null ? 1L
                    : (long) Math.max(1, product.packagesPerBox()) * unitsPerPackage);
            BigDecimal packages = line.quantity().divide(BigDecimal.valueOf(unitsPerPackage), 2, RoundingMode.HALF_UP);
            BigDecimal boxes = line.quantity().divide(unitsPerBox, 2, RoundingMode.HALF_UP);
            BigDecimal percentage = line.quantity().multiply(BigDecimal.valueOf(100))
                    .divide(unitsPerBox, 1, RoundingMode.HALF_UP);
            linesTableModel.addRow(new Object[]{product, line.quantity(), packages, boxes,
                    percentage.stripTrailingZeros().toPlainString() + "%", price,
                    product == null ? "0%" : product.effectiveTaxRate().movePointRight(2) + "%",
                    discount, line.serialNumber() == null ? "" : line.serialNumber(), total});
            draftSubtotal = draftSubtotal.add(subtotal);
            draftTax = draftTax.add(tax);
            draftTotal = draftTotal.add(total);
        }
        } finally {
            syncingInvoiceGrid = false;
        }
        totalLabel.setText(String.format("Total Rascunho: %,.2f MT (incl. IVA)", draftTotal));
    }

    /** Abre o formulário de nova fatura num modal responsivo (com scroll). */
    /** Validação + emissão. Lança {@link RuntimeException} em erro para manter o editor aberto. */
    private CreateInvoiceRequest buildInvoiceRequest() {
        stopInvoiceCellEditing();
        if (draftLines.isEmpty()) {
            throw new RuntimeException("Adicione pelo menos um item à fatura.");
        }
        for (int row = 0; row < draftLines.size(); row++) {
            CreateInvoiceLineRequest line = draftLines.get(row);
            boolean productExists = productsList.stream().anyMatch(product -> product.id().equals(line.productId()));
            if (!productExists) throw new RuntimeException("Seleccione o produto da linha " + (row + 1) + ".");
            if (line.quantity().signum() <= 0) throw new RuntimeException("A quantidade da linha " + (row + 1) + " deve ser positiva.");
            BigDecimal discount = line.discountPercentage() == null ? BigDecimal.ZERO : line.discountPercentage();
            if (discount.signum() < 0 || discount.compareTo(BigDecimal.valueOf(100)) > 0) {
                throw new RuntimeException("O desconto da linha " + (row + 1) + " deve ficar entre 0 e 100.");
            }
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

    private boolean inStopInvoiceCellEditing = false;

    private void stopInvoiceCellEditing() {
        if (inStopInvoiceCellEditing) return;
        inStopInvoiceCellEditing = true;
        try {
            if (linesTable != null && linesTable.isEditing() && linesTable.getCellEditor() != null) {
                if (!linesTable.getCellEditor().stopCellEditing()) {
                    linesTable.getCellEditor().cancelCellEditing();
                }
            }
        } finally {
            inStopInvoiceCellEditing = false;
        }
    }

    private static BigDecimal decimalGrid(Object value) {
        if (value instanceof BigDecimal number) return number;
        if (value == null || String.valueOf(value).isBlank()) return BigDecimal.ZERO;
        try { return new BigDecimal(String.valueOf(value).trim().replace(',', '.')); }
        catch (NumberFormatException ignored) { return BigDecimal.ZERO; }
    }

    void cancelSelectedInvoice() {
        int row = TableFilter.selectedModelRow(invoicesTable);
        if (row < 0) {
            showCommercialNotice(FeedbackType.WARNING, "Seleccione uma fatura", "Escolha na tabela a fatura que pretende anular.");
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
            ToastManager.success(this, "Fatura " + invoiceNum + " anulada com sucesso.");
            loadInvoicesTable();
            receiptsPanel.refresh();
        }, error -> showCommercialError("anular fatura", error));
    }

    void paySelectedInvoice() {
        int row = TableFilter.selectedModelRow(invoicesTable);
        if (row < 0) {
            showCommercialNotice(FeedbackType.WARNING, "Seleccione uma fatura", "Escolha na tabela a fatura que pretende liquidar.");
            return;
        }

        Long invoiceId = (Long) invoicesTableModel.getValueAt(row, 0);
        String invoiceNum = (String) invoicesTableModel.getValueAt(row, 1);
        String statusStr = (String) invoicesTableModel.getValueAt(row, 4);
        BigDecimal invoiceTotal = (BigDecimal) invoicesTableModel.getValueAt(row, 6);

        // Uma fatura parcialmente paga continua a receber recibos até o saldo chegar a zero.
        if (!"APPROVED".equalsIgnoreCase(statusStr) && !"PARTIALLY_PAID".equalsIgnoreCase(statusStr)) {
            showCommercialNotice(FeedbackType.WARNING, "Fatura não liquidável",
                    "Apenas faturas por cobrar podem receber recibo. Estado actual: " + UIHelper.humanStatus(statusStr) + ".");
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

    private static final java.time.format.DateTimeFormatter INVOICE_DATE_FMT =
            java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private void applyInvoices(List<InvoiceDTO> invoices) {
        invoicesTableModel.setRowCount(0);
        for (InvoiceDTO invoice : invoices) {
            String dateStr = invoice.createdAt() != null ? invoice.createdAt().format(INVOICE_DATE_FMT) : "—";
            invoicesTableModel.addRow(new Object[]{
                    invoice.id(),
                    invoice.invoiceNumber(),
                    invoice.clientName(),
                    dateStr,
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
            showCommercialNotice(FeedbackType.WARNING, "Armazém necessário", "Registe um armazém para a empresa actual.");
            return;
        }
        resetOrderDraft();
        editingOrder = null;
        lastCreatedOrder = null;
        orderGridEditable = true;
        orderKindCombo.setEnabled(true);
        orderEditor.setEditorTitle("Nova Encomenda");
        orderEditor.setSaveText("Guardar encomenda");
        orderEditor.setSaveEnabled(true);
        orderEditorDirty = false;
        if (encomendasCards != null && encomendasHost != null) {
            encomendasCards.show(encomendasHost, "editor");
        }
    }

    void openSelectedOrderEditor() {
        CommercialOrderEditorActions.openSelected(this);
    }

    void backToOrdersList() {
        if (encomendasCards != null && encomendasHost != null) {
            encomendasCards.show(encomendasHost, "list");
        }
        editingOrder = null;
        orderGridEditable = false;
        orderEditorDirty = false;
    }

    void saveOrderFromEditor() {
        CommercialOrderSubmission.save(this, comercialApiClient);
    }

    boolean isOrderEditorDirty() {
        return orderEditorDirty;
    }

    void markOrderEditorDirty() {
        orderEditorDirty = true;
    }

    void clearOrderEditorDirty() {
        orderEditorDirty = false;
    }

    void addDraftOrderLine() {
        CommercialOrderEditorActions.addBlankLine(this);
    }

    void removeSelectedDraftOrderLine() {
        CommercialOrderEditorActions.removeSelectedLine(this);
    }

    void refreshOrderDraftTable() {
        CommercialOrderEditorActions.refreshTable(this);
    }

    /** Via escolhida no editor — ver {@link CommercialOrderSubmission#selectedKind}. */
    mz.multicore.erp.modules.comercial.model.OrderKind selectedOrderKind() {
        return CommercialOrderSubmission.selectedKind(this);
    }

    /** Limpa o rascunho da encomenda (linhas, totais e selecção) antes de abrir o editor. */
    void resetOrderDraft() {
        draftOrderLines.clear();
        draftOrderUnitPrices.clear();
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
            showCommercialNotice(FeedbackType.WARNING, "Seleccione uma encomenda", "Escolha na tabela a encomenda que pretende faturar.");
            return;
        }

        Long orderId = (Long) ordersTableModel.getValueAt(row, 0);
        String orderNum = (String) ordersTableModel.getValueAt(row, 1);
        String statusStr = (String) ordersTableModel.getValueAt(row, 3);

        if (!"SEPARATED".equalsIgnoreCase(statusStr)) {
            showCommercialNotice(FeedbackType.WARNING, "Encomenda ainda não separada",
                    "Apenas pedidos separados podem ser faturados. Estado actual: " + UIHelper.humanStatus(statusStr) + ".");
            return;
        }

        UIHelper.runWithProgress(this, "A faturar pedido separado…",
                () -> comercialApiClient.billFulfillmentOrder(orderId, CustomerOrderFulfillmentActions.terminalName()), invoice -> {
            ToastManager.success(this, "Encomenda " + orderNum + " faturada · fatura " + invoice.invoiceNumber() + ".");

            loadOrdersTable();
            loadInvoicesTable();
        }, error -> showCommercialError("faturar encomenda", error));
    }

    void convertSelectedOrderToGuide() {
        int row = TableFilter.selectedModelRow(ordersTable);
        if (row < 0) {
            showCommercialNotice(FeedbackType.WARNING, "Seleccione uma encomenda",
                    "Escolha na tabela a encomenda que pretende converter em guia.");
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
            showCommercialNotice(FeedbackType.WARNING, "Encomenda indisponível para guia",
                    "A encomenda deve estar pendente e aprovada. Estado actual: " + UIHelper.humanStatus(status) + ".");
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
                "Responsável pelo transporte *:", responsibleField,
                "Viatura / Matrícula *:", vehicleField,
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
                    if (responsible == null) {
                        responsibleField.requestFocusInWindow();
                        throw new IllegalArgumentException("O transportador / responsável é obrigatório para emitir a guia de remessa.");
                    }
                    if (vehicle == null) {
                        vehicleField.requestFocusInWindow();
                        throw new IllegalArgumentException("A viatura / matrícula é obrigatória para emitir a guia de remessa.");
                    }
                    return () -> created[0] = comercialApiClient.createDeliveryGuide(
                            order.id(), responsible, vehicle, notes);
                });

        if (dialog.showDialog() && created[0] != null) {
            ToastManager.success(this, "Guia " + created[0].guideNumber() + " criada e submetida para aprovação.");
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

    /** Reposição interna → transferência entre armazéns. Ver docs/REPOSICAO_INTERNA_SPEC.md. */
    void convertSelectedOrderToTransfer() {
        OrderToTransferAction.convertSelected(this, comercialApiClient);
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
            showCommercialNotice(FeedbackType.WARNING, "Seleccione uma fatura", "Escolha uma fatura na tabela para continuar.");
            return;
        }
        Long invoiceId = (Long) invoicesTableModel.getValueAt(row, 0);
        String invoiceNum = String.valueOf(invoicesTableModel.getValueAt(row, 1));
        UIHelper.runWithProgress(this, "A gerar fatura em PDF…", () -> comercialApiClient.renderInvoice(invoiceId),
                pdf -> PrintPreviewDialog.show(this, pdf, "fatura-" + invoiceNum),
                error -> showCommercialError("gerar fatura em PDF", error));
    }

    void printSelectedGuide() {
        int row = TableFilter.selectedModelRow(invoicesTable);
        if (row < 0) {
            showCommercialNotice(FeedbackType.WARNING, "Seleccione uma fatura", "Escolha uma fatura na tabela para continuar.");
            return;
        }
        Long invoiceId = (Long) invoicesTableModel.getValueAt(row, 0);
        String invoiceNum = String.valueOf(invoicesTableModel.getValueAt(row, 1));
        UIHelper.runWithProgress(this, "A gerar guia em PDF…", () -> comercialApiClient.renderGuide(invoiceId),
                pdf -> PrintPreviewDialog.show(this, pdf, "guia-remessa-" + invoiceNum),
                error -> showCommercialError("gerar guia em PDF", error));
    }

    void exportInvoicesTable() {
        TableExportAction.export(this, printApiClient, invoicesTable, "Faturas Emitidas", "faturas");
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
            showCommercialNotice(FeedbackType.WARNING, "Seleccione uma encomenda", "Escolha uma encomenda na tabela para continuar.");
            return;
        }
        orderDetailsDialog.open((Long) ordersTableModel.getValueAt(row, 0));
    }

    void exportOrdersTable() {
        TableExportAction.export(this, printApiClient, ordersTable, "Encomendas", "encomendas");
    }

    private void showCommercialLoadError(String area, Throwable error) {
        feedback.show(FeedbackType.ERROR, "Não foi possível carregar " + area,
                error.getMessage(), "Tentar novamente", this::onPanelSelected);
    }

    void showCommercialError(String action, Throwable error) {
        showCommercialNotice(FeedbackType.ERROR, "Não foi possível " + action, error.getMessage());
    }

    public void showCommercialNotice(FeedbackType type, String title, String message) {
        feedback.show(type, title, message, null, null);
    }

    public void showCommercialSuccess(String message) { ToastManager.success(this, message); }

    private record CommercialMetadata(List<ClientDTO> clients, List<ProductDTO> products) {}
}
