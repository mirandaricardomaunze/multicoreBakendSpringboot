package mz.multicore.erp.gui;

import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.gui.components.*;
import mz.multicore.erp.modules.comercial.dto.ProductDTO;
import mz.multicore.erp.modules.inventory.dto.WarehouseDTO;
import mz.multicore.erp.modules.purchases.dto.*;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

/** Editor, listagem e recepção de encomendas a fornecedor. */
final class PurchaseOrdersPanel {
    private final ComprasPanel owner;
    private QuantityField poQtyField;
    private PackageQuantityEditor poPackageEditor;
    private JTextField poExpectedField;
    private JTable poLinesTable;
    private DefaultTableModel poListModel;
    private JTable poListTable;
    PurchaseOrdersPanel(ComprasPanel owner) { this.owner = owner; }

    public void refreshCombos() {
        if (owner.poWarehouseCombo == null) return;
        owner.poWarehouseCombo.removeAllItems();
        for (WarehouseDTO w : owner.warehousesList) owner.poWarehouseCombo.addItem(w.name());
        if (owner.poProductCombo != null) {
            owner.poProductCombo.setProducts(owner.productsList);
        }
        refreshPackagingFactor();
    }

    // ===== Encomendas a Fornecedor =====

    public JPanel buildPanel() {
        JPanel tab = new JPanel(new BorderLayout(0, 12));
        tab.setOpaque(false);
        tab.setBorder(new EmptyBorder(12, 5, 5, 5));

        // ---- formulário (topo) ----
        ModernPanel formCard = new ModernPanel(16);
        formCard.setLayout(new GridBagLayout());
        formCard.setBorder(new EmptyBorder(12, 16, 12, 16));
        GridBagConstraints g = new GridBagConstraints();
        g.fill = GridBagConstraints.HORIZONTAL; g.insets = new Insets(4, 8, 4, 8); g.weightx = 1;

        owner.poSupplierCombo = new JComboBox<>(); UIHelper.styleComboBox(owner.poSupplierCombo);
        owner.poWarehouseCombo = new JComboBox<>(); UIHelper.styleComboBox(owner.poWarehouseCombo);
        owner.poProductCombo = new mz.multicore.erp.gui.components.ProductSearchComboBox();
        poPackageEditor = new PackageQuantityEditor();
        poQtyField = poPackageEditor.totalField();
        owner.poProductCombo.addActionListener(event -> refreshPackagingFactor());
        owner.poPriceField = new MoneyField("0");
        poExpectedField = new JTextField(); UIHelper.styleTextField(poExpectedField);
        poExpectedField.setToolTipText("Data prevista de entrega (aaaa-MM-dd) — opcional");

        g.gridx = 0; g.gridy = 0; g.weightx = 0.5; formCard.add(label("Fornecedor:"), g);
        g.gridx = 1; formCard.add(label("Armazém de destino:"), g);
        g.gridx = 0; g.gridy = 1; formCard.add(owner.poSupplierCombo, g);
        g.gridx = 1; formCard.add(owner.poWarehouseCombo, g);
        g.gridx = 0; g.gridy = 2; g.gridwidth = 2; g.weightx = 1; formCard.add(label("Produto:"), g);
        g.gridy = 3; formCard.add(owner.poProductCombo, g);
        g.gridwidth = 1; g.weightx = 0.33;
        g.gridx = 0; g.gridy = 4; formCard.add(label("Qtd:"), g);
        g.gridx = 1; formCard.add(label("Preço unit. (compra):"), g);
        g.gridx = 2; formCard.add(label("Entrega prevista:"), g);
        g.gridx = 0; g.gridy = 5; formCard.add(poPackageEditor, g);
        g.gridx = 1; formCard.add(owner.poPriceField, g);
        g.gridx = 2; formCard.add(poExpectedField, g);

        ModernButton addLineBtn = UIHelper.createAddLineButton();
        addLineBtn.addActionListener(e -> addPoDraftLine());
        JPanel addRow = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0)); addRow.setOpaque(false);
        addRow.add(addLineBtn);
        g.gridx = 0; g.gridy = 6; g.gridwidth = 3; g.weightx = 1; g.insets = new Insets(10, 8, 4, 8);
        formCard.add(addRow, g);

        // ---- linhas (rascunho) ----
        String[] lineCols = {"Produto", "Qtd", "Preço Unit.", "Lote", "Validade", "Total"};
        owner.poLinesModel = new DefaultTableModel(lineCols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        poLinesTable = new JTable(owner.poLinesModel);
        UIHelper.styleTable(poLinesTable);
        JScrollPane linesScroll = new JScrollPane(poLinesTable);
        UIHelper.styleScrollPane(linesScroll);

        owner.poTotalLabel = new JLabel("Total da Encomenda: 0.00 MT");
        owner.poTotalLabel.setForeground(Color.WHITE);
        owner.poTotalLabel.setFont(new Font(UIHelper.FONT, Font.BOLD, 14));
        JPanel poFooter = new JPanel(new BorderLayout()); poFooter.setOpaque(false);
        poFooter.setBorder(new EmptyBorder(8, 0, 0, 0));
        poFooter.add(owner.poTotalLabel, BorderLayout.WEST);

        ModernPanel draftCard = new ModernPanel(16);
        draftCard.setLayout(new BorderLayout(0, 10));
        draftCard.setBorder(new EmptyBorder(12, 16, 12, 16));
        draftCard.add(UIHelper.createSubheading("Linhas da Encomenda"), BorderLayout.NORTH);
        draftCard.add(linesScroll, BorderLayout.CENTER);
        draftCard.add(poFooter, BorderLayout.SOUTH);

        // Conteúdo do formulário (modal responsivo): inputs + linhas.
        JPanel poFormContentPanel = new JPanel(new BorderLayout(0, 10));
        poFormContentPanel.setOpaque(false);
        poFormContentPanel.add(formCard, BorderLayout.NORTH);
        poFormContentPanel.add(draftCard, BorderLayout.CENTER);
        owner.poFormContent = poFormContentPanel;

        // ---- lista de encomendas (base) ----
        JPanel listHeader = new JPanel(new BorderLayout(8, 0)); listHeader.setOpaque(false);
        listHeader.add(UIHelper.createHeading("Encomendas Registadas"), BorderLayout.WEST);

        ModernButton refreshBtn = UIHelper.createSecondaryButton("Actualizar");
        refreshBtn.setIcon(UIHelper.icon("fas-sync-alt", 14));
        refreshBtn.addActionListener(e -> { owner.poSearchField.setText(""); refresh(); });

        ActionMenuButton actionsMenu = UIHelper.createActionMenuButton("Ações da Encomenda");
        actionsMenu.addAction("Receber Encomenda", UIHelper.icon("fas-dolly", 14, UIHelper.APPROVED_GREEN), this::receiveSelectedPO);
        actionsMenu.addAction("Receber Parcial…", UIHelper.icon("fas-dolly-flatbed", 14, UIHelper.ACCENT_BLUE), this::receivePartialSelectedPO);
        actionsMenu.addAction("Imprimir Etiquetas", UIHelper.icon("fas-barcode", 14, UIHelper.ACCENT), this::printSelectedPOReceiptLabels);
        actionsMenu.addAction("Cancelar Encomenda", UIHelper.icon("fas-ban", 14, UIHelper.REJECTED_RED), this::cancelSelectedPO);

        ModernButton newOrderBtn = UIHelper.createPrimaryButton("Nova Encomenda…");
        newOrderBtn.setIcon(UIHelper.icon("fas-clipboard-check", 14));
        newOrderBtn.addActionListener(e -> openPurchaseOrderFormDialog());

        listHeader.add(UIHelper.actionsBar(refreshBtn, actionsMenu, newOrderBtn), BorderLayout.EAST);

        String[] cols = {"Nº", "Fornecedor", "Estado", "Total", "Data", "Entrega prev."};
        poListModel = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        poListTable = new JTable(poListModel);
        UIHelper.styleTable(poListTable);
        poListTable.getColumnModel().getColumn(2).setCellRenderer(TableCellRenderers.status());
        poListTable.getColumnModel().getColumn(3).setCellRenderer(TableCellRenderers.money());
        JScrollPane listScroll = new JScrollPane(poListTable);
        UIHelper.styleScrollPane(listScroll);

        owner.poSearchField = TableFilter.searchField("Nº ou fornecedor…");
        JComboBox<String> poEstado = TableFilter.combo("Todos os estados",
                "ORDERED", "PARTIALLY_RECEIVED", "RECEIVED", "CANCELLED");
        UIHelper.styleComboBox(poEstado);
        poEstado.setPreferredSize(new Dimension(180, UIHelper.FORM_CONTROL_HEIGHT));

        JComboBox<String> poPeriodo = TableFilter.periodCombo();
        UIHelper.styleComboBox(poPeriodo);
        poPeriodo.setPreferredSize(new Dimension(180, UIHelper.FORM_CONTROL_HEIGHT));

        TableFilter.install(poListTable, owner.poSearchField,
                java.util.List.of(new TableFilter.ColumnFilter(poEstado, 2)),
                java.util.List.of(new TableFilter.PeriodFilter(poPeriodo, 4)));

        JPanel poFilters = new JPanel(new GridBagLayout());
        poFilters.setOpaque(false);
        GridBagConstraints pg = new GridBagConstraints();
        pg.gridy = 0;
        pg.fill = GridBagConstraints.HORIZONTAL;
        pg.insets = new Insets(0, 0, 0, 12);

        pg.gridx = 0; pg.weightx = 0; poFilters.add(filterLabel("Estado"), pg);
        pg.gridx = 1; pg.weightx = 0; poFilters.add(filterLabel("Período"), pg);
        pg.gridx = 2; pg.weightx = 1.0; pg.insets = new Insets(0, 0, 0, 0);
        poFilters.add(filterLabel("Pesquisa"), pg);

        pg.gridy = 1;
        pg.insets = new Insets(4, 0, 0, 12);
        pg.gridx = 0; pg.weightx = 0; poFilters.add(poEstado, pg);
        pg.gridx = 1; pg.weightx = 0; poFilters.add(poPeriodo, pg);
        pg.gridx = 2; pg.weightx = 1.0; pg.insets = new Insets(4, 0, 0, 0);
        poFilters.add(owner.poSearchField, pg);

        poFilters.setBorder(new EmptyBorder(10, 0, 0, 0));
        listHeader.add(poFilters, BorderLayout.SOUTH);

        ModernPanel listCard = new ModernPanel(16);
        listCard.setLayout(new BorderLayout(0, 10));
        listCard.setBorder(new EmptyBorder(12, 16, 12, 16));
        listCard.add(listHeader, BorderLayout.NORTH);
        listCard.add(listScroll, BorderLayout.CENTER);
        listCard.add(ClientTablePagination.install(poListTable), BorderLayout.SOUTH);

        // Lista de encomendas ocupa a tab inteira; o formulário vive no modal.
        tab.add(listCard, BorderLayout.CENTER);
        return tab;
    }

    void openPurchaseOrderFormDialog() {
        openPurchaseOrderForSuggestion(null, null, null, null);
    }

    void openPurchaseOrderForSuggestion(Long supplierId, Long productId, BigDecimal quantity, BigDecimal unitPrice) {
        if (owner.supplierComboList.isEmpty()) {
            owner.showPurchaseNotice(FeedbackType.WARNING, "Fornecedor necessário",
                    "Registe um fornecedor activo antes de criar a encomenda.");
            return;
        }
        // Reset do rascunho ao abrir.
        owner.poDraftLines.clear();
        if (owner.poLinesModel != null) owner.poLinesModel.setRowCount(0);
        recomputePoTotal();
        poExpectedField.setText("");

        // Pré-selecionar fornecedor se fornecido
        if (supplierId != null) {
            for (int i = 0; i < owner.supplierComboList.size(); i++) {
                if (supplierId.equals(owner.supplierComboList.get(i).id())) {
                    owner.poSupplierCombo.setSelectedIndex(i);
                    break;
                }
            }
        }

        // Pré-selecionar produto e pré-adicionar linha se fornecido
        if (productId != null) {
            owner.poProductCombo.selectProduct(productId);
            refreshPackagingFactor();
            if (quantity != null && quantity.signum() > 0) {
                poQtyField.setText(quantity.toPlainString());
            }
            if (unitPrice != null && unitPrice.signum() >= 0) {
                owner.poPriceField.setText(unitPrice.toPlainString());
            }
            addPoDraftLine();
        }

        Window parent = SwingUtilities.getWindowAncestor(owner);
        ModernFormDialog dlg = new ModernFormDialog(parent, "Nova Encomenda a Fornecedor", owner.poFormContent);
        dlg.setSize(880, 640);
        PurchaseOrderDTO[] created = new PurchaseOrderDTO[1];
        dlg.setOnSaveAsync(() -> {
            CreatePurchaseOrderRequest request = buildPurchaseOrderRequest();
            return () -> created[0] = owner.purchaseApiClient.createOrder(request);
        });
        if (dlg.showDialog()) {
            owner.poDraftLines.clear();
            if (owner.poLinesModel != null) owner.poLinesModel.setRowCount(0);
            recomputePoTotal();
            poExpectedField.setText("");
            owner.showPurchaseSuccess("Encomenda " + created[0].orderNumber() + " criada.");
            refresh();
            owner.reorderPanel.refresh();
        }
    }

    private JLabel label(String text) {
        JLabel l = new JLabel(text);
        l.setForeground(UIHelper.TEXT_MUTED);
        return l;
    }

    private void addPoDraftLine() {
        ProductDTO product = owner.poProductCombo.selectedProduct();
        if (product == null) {
            owner.showPurchaseNotice(FeedbackType.WARNING, "Produto necessário", "Seleccione um produto para adicionar.");
            return;
        }
        try {
            BigDecimal qty = poQtyField.value();
            BigDecimal price = owner.poPriceField.value();
            if (qty.signum() <= 0 || price.signum() < 0) throw new NumberFormatException();
            owner.poDraftLines.add(new CreatePurchaseOrderLineRequest(
                    product.id(), qty, price, null, null, null));
            owner.poLinesModel.addRow(new Object[]{
                    product.name(), qty.toPlainString(),
                    String.format("%,.2f", price), "-", "-",
                    String.format("%,.2f MT", qty.multiply(price))});
            recomputePoTotal();
            poPackageEditor.reset(); owner.poPriceField.setText("0");
        } catch (IllegalArgumentException ignored) {
            // Os campos canónicos já apresentam o estado inválido no formulário activo.
        }
    }

    private void refreshPackagingFactor() {
        if (poPackageEditor == null) return;
        ProductDTO p = owner.poProductCombo != null ? owner.poProductCombo.selectedProduct() : null;
        if (p != null) {
            poPackageEditor.setUnitsPerBox(p.unitsPerBox());
        }
    }

    private void recomputePoTotal() {
        BigDecimal total = BigDecimal.ZERO;
        for (CreatePurchaseOrderLineRequest l : owner.poDraftLines) {
            total = total.add(l.quantity().multiply(l.unitPrice()));
        }
        owner.poTotalLabel.setText(String.format("Total da Encomenda: %,.2f MT", total));
    }

    /** Validação + criação da encomenda. Lança RuntimeException em erro (mantém o modal aberto). */
    private CreatePurchaseOrderRequest buildPurchaseOrderRequest() {
        int supIdx = owner.poSupplierCombo.getSelectedIndex();
        int whIdx = owner.poWarehouseCombo.getSelectedIndex();
        if (supIdx < 0 || supIdx >= owner.supplierComboList.size() || whIdx < 0 || whIdx >= owner.warehousesList.size()) {
            throw new RuntimeException("Selecione fornecedor e armazém.");
        }
        if (owner.poDraftLines.isEmpty()) {
            throw new RuntimeException("Adicione pelo menos uma linha.");
        }
        LocalDate expected = null;
        String t = poExpectedField.getText().trim();
        if (!t.isEmpty()) {
            try {
                expected = LocalDate.parse(t);
            } catch (DateTimeParseException ex) {
                throw new RuntimeException("Data de entrega inválida (aaaa-MM-dd).");
            }
        }
        return new CreatePurchaseOrderRequest(
                owner.supplierComboList.get(supIdx).id(),
                owner.warehousesList.get(whIdx).id(),
                CurrentUserContext.getCurrentCompanyId(),
                expected, null, new ArrayList<>(owner.poDraftLines));
    }

    public void refresh() {
        if (poListModel == null) return;
        Long companyId = CurrentUserContext.getCurrentCompanyId();
        // Carrega todas; a pesquisa/estado/data é aplicada pelo TableFilter (cliente).
        UIHelper.loadAsync(owner, () -> owner.purchaseApiClient.findOrdersByCompany(companyId), this::applyPurchaseOrders,
                error -> owner.showPurchaseLoadError("encomendas", error));
    }

    private void applyPurchaseOrders(List<PurchaseOrderDTO> loaded) {
        owner.poList = loaded;
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
        poListModel.setRowCount(0);
        for (PurchaseOrderDTO o : owner.poList) {
            poListModel.addRow(new Object[]{
                    o.orderNumber(), o.supplierName(), o.status(),
                    o.totalAmount() == null ? BigDecimal.ZERO : o.totalAmount(),
                    o.orderDate() == null ? "-" : o.orderDate().format(dtf),
                    o.expectedDate() == null ? "-" : o.expectedDate().toString()});
        }
    }

    private PurchaseOrderDTO selectedPO() {
        int row = TableFilter.selectedModelRow(poListTable);
        if (row < 0 || row >= owner.poList.size()) {
            owner.showPurchaseNotice(FeedbackType.WARNING, "Seleccione uma encomenda",
                    "Escolha uma encomenda na tabela para continuar.");
            return null;
        }
        return owner.poList.get(row);
    }

    private void receiveSelectedPO() {
        PurchaseOrderDTO sel = selectedPO();
        if (sel == null) return;
        if (!ModernMessageDialog.confirm(SwingUtilities.getWindowAncestor(owner), FeedbackType.WARNING,
                "Confirmar recepção",
                "Receber a encomenda " + sel.orderNumber() + "? O stock do armazém será actualizado.",
                "Receber encomenda")) return;
        UIHelper.runWithProgress(owner, "A receber encomenda…", () -> owner.purchaseApiClient.receiveOrder(sel.id()), ignored -> {
            owner.showPurchaseSuccess("Encomenda recebida e stock actualizado.");
            refresh();
            owner.loadPurchasesHistory();
        }, owner::showPurchaseError);
    }

    private void receivePartialSelectedPO() {
        PurchaseOrderDTO sel = selectedPO();
        if (sel == null) return;
        if (!"ORDERED".equals(sel.status()) && !"PARTIALLY_RECEIVED".equals(sel.status())) {
            owner.showPurchaseNotice(FeedbackType.WARNING, "Recepção indisponível",
                    "Só encomendas por receber podem ser recebidas.");
            return;
        }

        PurchaseOrderReceivingDialog.show(SwingUtilities.getWindowAncestor(owner), sel, request -> {
            UIHelper.runWithProgress(owner, "A registar recepção parcial…",
                    () -> owner.purchaseApiClient.receivePartial(sel.id(), request), updated -> {
                owner.showPurchaseSuccess("Recepção registada · " + UIHelper.humanStatus(updated.status()) + ".");
                refresh();
                owner.loadPurchasesHistory();
            }, owner::showPurchaseError);
        });
    }

    private void cancelSelectedPO() {
        PurchaseOrderDTO sel = selectedPO();
        if (sel == null) return;
        String reason = UIHelper.promptRequiredText("Cancelar Encomenda", "fas-ban",
                "Encomenda " + sel.orderNumber(), "Motivo do cancelamento:");
        if (reason == null) return;
        UIHelper.runWithProgress(owner, "A cancelar encomenda…", () -> {
            owner.purchaseApiClient.cancelOrder(sel.id(), reason);
            return null;
        }, ignored -> refresh(), owner::showPurchaseError);
    }


    void printSelectedPOReceiptLabels() {
        PurchaseOrderDTO sel = selectedPO();
        if (sel == null) return;
        List<ProductDTO> products = extractReceivedProducts(sel);
        if (products.isEmpty()) {
            owner.showPurchaseNotice(FeedbackType.WARNING, "Sem artigos recebidos", "Esta encomenda ainda não possui quantidades recebidas para emissão de etiquetas.");
            return;
        }
        ShelfLabelsDialog.show(SwingUtilities.getWindowAncestor(owner), products);
    }

    public static List<ProductDTO> extractReceivedProducts(PurchaseOrderDTO order) {
        List<ProductDTO> result = new ArrayList<>();
        if (order == null || order.lines() == null) return result;
        for (PurchaseOrderLineDTO line : order.lines()) {
            BigDecimal qty = line.receivedQuantity();
            if (qty != null && qty.signum() > 0) {
                String sku = line.productSku() != null && !line.productSku().isBlank()
                        ? line.productSku() : "ART-" + line.productId();
                String barcode = (line.productSku() != null && !line.productSku().isBlank())
                        ? line.productSku()
                        : (line.serialNumber() != null ? line.serialNumber() : sku);
                BigDecimal price = line.unitPrice() != null ? line.unitPrice() : BigDecimal.ZERO;
                result.add(new ProductDTO(
                        line.productId(),
                        sku,
                        sku,
                        barcode,
                        line.productName(),
                        price,
                        price,
                        BigDecimal.ZERO,
                        BigDecimal.ZERO,
                        BigDecimal.ZERO,
                        1,
                        "UNIT",
                        true,
                        null,
                        "Geral",
                        null,
                        line.taxRate() != null ? line.taxRate() : BigDecimal.ZERO,
                        "IVA",
                        "Entrada por Encomenda",
                        null,
                        BigDecimal.ZERO,
                        BigDecimal.ZERO
                ));
            }
        }
        return result;
    }

    private JLabel filterLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(new Font("Segoe UI", Font.BOLD, 11));
        label.setForeground(UIHelper.TEXT_MUTED);
        return label;
    }
}
