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
    private JTextField poExpectedField;
    private JTable poLinesTable;
    private DefaultTableModel poListModel;
    private JTable poListTable;
    PurchaseOrdersPanel(ComprasPanel owner) { this.owner = owner; }

    public void refreshCombos() {
        if (owner.poWarehouseCombo == null) return;
        owner.poWarehouseCombo.removeAllItems();
        for (WarehouseDTO w : owner.warehousesList) owner.poWarehouseCombo.addItem(w.name());
        owner.poProductCombo.removeAllItems();
        for (ProductDTO p : owner.productsList) owner.poProductCombo.addItem(p.name() + " (" + p.sku() + ")");
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
        owner.poProductCombo = new JComboBox<>(); UIHelper.styleComboBox(owner.poProductCombo);
        poQtyField = new QuantityField("1", true);
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
        g.gridx = 0; g.gridy = 5; formCard.add(poQtyField, g);
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
        JPanel listActions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0)); listActions.setOpaque(false);
        ModernButton receiveBtn = UIHelper.createSuccessButton("Receber");
        receiveBtn.setIcon(UIHelper.icon("fas-dolly", 14));
        receiveBtn.addActionListener(e -> receiveSelectedPO());
        ModernButton receivePartialBtn = UIHelper.createPrimaryButton("Receber Parcial…");
        receivePartialBtn.setIcon(UIHelper.icon("fas-dolly-flatbed", 14));
        receivePartialBtn.addActionListener(e -> receivePartialSelectedPO());
        ModernButton cancelBtn = UIHelper.createDangerButton("Cancelar");
        cancelBtn.setIcon(UIHelper.icon("fas-ban", 14));
        cancelBtn.addActionListener(e -> cancelSelectedPO());
        ModernButton refreshBtn = UIHelper.createSecondaryButton("Actualizar");
        refreshBtn.setIcon(UIHelper.icon("fas-sync-alt", 14));
        refreshBtn.addActionListener(e -> { owner.poSearchField.setText(""); refresh(); });
        ModernButton newOrderBtn = UIHelper.createPrimaryButton("Nova Encomenda…");
        newOrderBtn.setIcon(UIHelper.icon("fas-clipboard-check", 14));
        newOrderBtn.addActionListener(e -> openPurchaseOrderFormDialog());
        listActions.add(receiveBtn); listActions.add(receivePartialBtn); listActions.add(cancelBtn); listActions.add(refreshBtn);
        listActions.add(newOrderBtn);
        listHeader.add(listActions, BorderLayout.EAST);

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
        JComboBox<String> poPeriodo = TableFilter.periodCombo();
        TableFilter.install(poListTable, owner.poSearchField,
                java.util.List.of(new TableFilter.ColumnFilter(poEstado, 2)),
                java.util.List.of(new TableFilter.PeriodFilter(poPeriodo, 4)));
        JPanel poBar = TableFilter.bar(owner.poSearchField,
                TableFilter.label("Estado:"), poEstado,
                TableFilter.label("Data:", "fas-calendar-alt"), poPeriodo);
        poBar.setBorder(new EmptyBorder(10, 0, 0, 0));
        listHeader.add(poBar, BorderLayout.SOUTH);

        ModernPanel listCard = new ModernPanel(16);
        listCard.setLayout(new BorderLayout(0, 10));
        listCard.setBorder(new EmptyBorder(12, 16, 12, 16));
        listCard.add(listHeader, BorderLayout.NORTH);
        listCard.add(listScroll, BorderLayout.CENTER);

        // Lista de encomendas ocupa a tab inteira; o formulário vive no modal.
        tab.add(listCard, BorderLayout.CENTER);
        return tab;
    }

    private void openPurchaseOrderFormDialog() {
        if (owner.supplierComboList.isEmpty()) {
            JOptionPane.showMessageDialog(owner, "Cadastre um fornecedor activo primeiro.", "Aviso", JOptionPane.WARNING_MESSAGE);
            return;
        }
        // Reset do rascunho ao abrir.
        owner.poDraftLines.clear();
        if (owner.poLinesModel != null) owner.poLinesModel.setRowCount(0);
        recomputePoTotal();
        poExpectedField.setText("");
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
            JOptionPane.showMessageDialog(owner, "Encomenda " + created[0].orderNumber() + " criada.",
                    "Sucesso", JOptionPane.INFORMATION_MESSAGE);
            refresh();
        }
    }

    private JLabel label(String text) {
        JLabel l = new JLabel(text);
        l.setForeground(UIHelper.TEXT_MUTED);
        return l;
    }

    private void addPoDraftLine() {
        int prodIdx = owner.poProductCombo.getSelectedIndex();
        if (prodIdx < 0 || prodIdx >= owner.productsList.size()) {
            JOptionPane.showMessageDialog(owner, "Selecione um produto.", "Aviso", JOptionPane.WARNING_MESSAGE);
            return;
        }
        try {
            BigDecimal qty = poQtyField.value();
            BigDecimal price = owner.poPriceField.value();
            if (qty.signum() <= 0 || price.signum() < 0) throw new NumberFormatException();
            ProductDTO product = owner.productsList.get(prodIdx);
            owner.poDraftLines.add(new CreatePurchaseOrderLineRequest(
                    product.id(), qty, price, null, null, null));
            owner.poLinesModel.addRow(new Object[]{
                    product.name(), qty.toPlainString(),
                    String.format("%,.2f", price), "-", "-",
                    String.format("%,.2f MT", qty.multiply(price))});
            recomputePoTotal();
            poQtyField.setText("1"); owner.poPriceField.setText("0");
        } catch (IllegalArgumentException ex) {
            JOptionPane.showMessageDialog(owner, ex.getMessage(), "Erro", JOptionPane.ERROR_MESSAGE);
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
            JOptionPane.showMessageDialog(owner, "Selecione uma encomenda.", "Aviso", JOptionPane.WARNING_MESSAGE);
            return null;
        }
        return owner.poList.get(row);
    }

    private void receiveSelectedPO() {
        PurchaseOrderDTO sel = selectedPO();
        if (sel == null) return;
        int opt = JOptionPane.showConfirmDialog(owner,
                "Receber a encomenda " + sel.orderNumber() + "? O stock do armazém será actualizado.",
                "Confirmar Recepção", JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE);
        if (opt != JOptionPane.OK_OPTION) return;
        UIHelper.runWithProgress(owner, "A receber encomenda…", () -> owner.purchaseApiClient.receiveOrder(sel.id()), ignored -> {
            JOptionPane.showMessageDialog(owner, "Encomenda recebida e stock actualizado.",
                    "Sucesso", JOptionPane.INFORMATION_MESSAGE);
            refresh();
            owner.loadPurchasesHistory();
        }, owner::showPurchaseError);
    }

    private void receivePartialSelectedPO() {
        PurchaseOrderDTO sel = selectedPO();
        if (sel == null) return;
        if (!"ORDERED".equals(sel.status()) && !"PARTIALLY_RECEIVED".equals(sel.status())) {
            JOptionPane.showMessageDialog(owner, "Só encomendas por receber podem ser recebidas.",
                    "Aviso", JOptionPane.WARNING_MESSAGE);
            return;
        }

        // Tabela: Produto | Encomendado | Recebido | Em falta | A receber agora (editável).
        String[] cols = {"Produto", "Encomendado", "Recebido", "Em falta", "A receber agora"};
        DefaultTableModel model = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return c == 4; }
        };
        List<PurchaseOrderLineDTO> lines = sel.lines();
        for (PurchaseOrderLineDTO l : lines) {
            BigDecimal outstanding = l.outstandingQuantity();
            model.addRow(new Object[]{
                    l.productName(),
                    l.quantity().toPlainString(),
                    l.receivedQuantity().toPlainString(),
                    outstanding.toPlainString(),
                    outstanding.toPlainString() // pré-preenche com o em falta
            });
        }
        JTable table = new JTable(model);
        UIHelper.styleTable(table);
        JScrollPane scroll = new JScrollPane(table);
        scroll.setMinimumSize(new Dimension(460, 180));

        JPanel panel = new JPanel(new BorderLayout(0, 8));
        panel.setOpaque(false);
        panel.add(new JLabel("Encomenda " + sel.orderNumber() + " — indique a quantidade a receber agora:"),
                BorderLayout.NORTH);
        panel.add(scroll, BorderLayout.CENTER);

        int opt = JOptionPane.showConfirmDialog(owner, panel, "Recepção Parcial",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (opt != JOptionPane.OK_OPTION) return;
        if (table.isEditing()) table.getCellEditor().stopCellEditing();

        List<ReceivePurchaseOrderRequest.ReceiveLine> toReceive = new ArrayList<>();
        try {
            for (int i = 0; i < lines.size(); i++) {
                String raw = String.valueOf(model.getValueAt(i, 4)).trim().replace(',', '.');
                if (raw.isEmpty()) continue;
                BigDecimal qty = new BigDecimal(raw);
                if (qty.signum() > 0) {
                    toReceive.add(new ReceivePurchaseOrderRequest.ReceiveLine(lines.get(i).id(), qty));
                }
            }
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(owner, "Quantidade inválida.", "Erro", JOptionPane.ERROR_MESSAGE);
            return;
        }
        if (toReceive.isEmpty()) {
            JOptionPane.showMessageDialog(owner, "Indique pelo menos uma quantidade a receber.",
                    "Aviso", JOptionPane.WARNING_MESSAGE);
            return;
        }
        ReceivePurchaseOrderRequest request = new ReceivePurchaseOrderRequest(toReceive);
        UIHelper.runWithProgress(owner, "A registar recepção parcial…",
                () -> owner.purchaseApiClient.receivePartial(sel.id(), request), updated -> {
            JOptionPane.showMessageDialog(owner,
                    "Recepção registada. Estado da encomenda: " + updated.status() + ".",
                    "Sucesso", JOptionPane.INFORMATION_MESSAGE);
            refresh();
            owner.loadPurchasesHistory();
        }, owner::showPurchaseError);
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

}
