package mz.multicore.erp.gui.landedcost;

import mz.multicore.erp.architecture.format.DisplayNumbers;
import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.desktop.client.ComercialApiClient;
import mz.multicore.erp.desktop.client.ExchangeRateApiClient;
import mz.multicore.erp.desktop.client.PurchaseApiClient;
import mz.multicore.erp.gui.components.*;
import mz.multicore.erp.modules.fiscal.dto.TaxRateDTO;
import mz.multicore.erp.modules.purchases.dto.*;
import mz.multicore.erp.modules.purchases.model.LandedCostAllocationMethod;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Compras › Custos de Importação (docs/MOEDA_ESTRANGEIRA_IMPORTACAO_SPEC.md §4.5): choose received purchases, type
 * the cost lines, pick the allocation method, preview the effect per article and confirm the CI. Confirming is only
 * possible after previewing the current data; any change clears the preview. The rules live in the backend.
 */
public class LandedCostPanel extends JPanel {

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final String RECEIVED = "COMPLETED";

    private final PurchaseApiClient purchaseApiClient;
    private final ComercialApiClient comercialApiClient;
    private final ExchangeRateApiClient exchangeRateApiClient;
    private final InlineFeedbackPanel feedback = new InlineFeedbackPanel();

    private final List<PurchaseDTO> purchases = new ArrayList<>();
    private final DefaultTableModel purchasesModel = new DefaultTableModel(
            new String[]{"Escolher", "Nº Documento", "Fornecedor", "Data", "Moeda", "Total (moeda base)"}, 0) {
        @Override public boolean isCellEditable(int row, int column) { return column == 0; }
        @Override public Class<?> getColumnClass(int column) { return column == 0 ? Boolean.class : Object.class; }
    };
    private final JTable purchasesTable = new JTable(purchasesModel);

    private final List<LandedCostChargeDraft> charges = new ArrayList<>();
    private final DefaultTableModel chargesModel = readOnlyModel("Tipo", "Fornecedor", "Moeda", "Câmbio", "Valor", "IVA");
    private final JTable chargesTable = new JTable(chargesModel);

    private final DefaultTableModel previewModel = readOnlyModel("Compra", "Artigo", "Qtd recebida",
            "Custo unit. original", "Custo acrescentado", "Custo unit. final");
    private final JTable previewTable = new JTable(previewModel);
    private final JLabel previewSummary = new JLabel(" ");

    private final JComboBox<LandedCostAllocationMethod> methodCombo =
            UIHelper.createComboBox(LandedCostAllocationMethod.values(), LandedCostAllocationMethod::label);
    private final ModernButton addChargeBtn = UIHelper.createSecondaryButton("Nova linha");
    private final ModernButton removeChargeBtn = UIHelper.createSecondaryButton("Remover linha");
    private final ModernButton previewBtn = UIHelper.createSecondaryButton("Pré-visualizar");
    private final ModernButton confirmBtn = UIHelper.createSuccessButton("Confirmar CI");

    private List<SupplierDTO> suppliers = List.of();
    private List<TaxRateDTO> vatRates = List.of();

    public LandedCostPanel(PurchaseApiClient purchaseApiClient, ComercialApiClient comercialApiClient,
                           ExchangeRateApiClient exchangeRateApiClient) {
        this.purchaseApiClient = purchaseApiClient;
        this.comercialApiClient = comercialApiClient;
        this.exchangeRateApiClient = exchangeRateApiClient;
        setLayout(new BorderLayout(0, Spacing.SM));
        setBackground(UIHelper.BG_DARK);
        setBorder(new EmptyBorder(Spacing.XS, 0, 0, 0));

        JPanel inputs = new JPanel(new GridLayout(1, 2, Spacing.LG, 0));
        inputs.setOpaque(false);
        inputs.add(buildPurchasesCard());
        inputs.add(buildChargesCard());
        JPanel content = new JPanel(new GridLayout(2, 1, 0, Spacing.LG));
        content.setOpaque(false);
        content.add(inputs);
        content.add(buildPreviewCard());

        add(feedback, BorderLayout.NORTH);
        add(content, BorderLayout.CENTER);

        purchasesModel.addTableModelListener(event -> clearPreview());
        methodCombo.addActionListener(event -> clearPreview());
        confirmBtn.setEnabled(false);
    }

    private JComponent buildPurchasesCard() {
        ModernButton refreshBtn = UIHelper.createRefreshButton(this::refresh);
        purchasesTable.getColumnModel().getColumn(5).setCellRenderer(TableCellRenderers.money());
        return card("Compras recebidas", caption("Escolha as compras a que os custos se aplicam."),
                purchasesTable, refreshBtn);
    }

    private JComponent buildChargesCard() {
        addChargeBtn.setIcon(UIHelper.icon("fas-plus", 14));
        addChargeBtn.addActionListener(event -> openChargeDialog());
        removeChargeBtn.setIcon(UIHelper.icon("fas-trash-alt", 14));
        removeChargeBtn.addActionListener(event -> removeSelectedCharge());
        return card("Linhas de custo", caption("Cada linha gera uma dívida ao seu fornecedor."),
                chargesTable, removeChargeBtn, addChargeBtn);
    }

    private JComponent buildPreviewCard() {
        previewBtn.setIcon(UIHelper.icon("fas-search-dollar", 14));
        previewBtn.addActionListener(event -> preview());
        confirmBtn.setIcon(UIHelper.icon("fas-check", 14));
        confirmBtn.addActionListener(event -> confirm());
        for (int column = 2; column <= 5; column++) {
            previewTable.getColumnModel().getColumn(column).setCellRenderer(
                    column == 2 ? TableCellRenderers.quantity() : TableCellRenderers.money());
        }
        previewSummary.setFont(Typography.EMPHASIS_BOLD);
        previewSummary.setForeground(UIHelper.TEXT_LIGHT);
        JPanel filters = new JPanel(new FlowLayout(FlowLayout.LEFT, Spacing.SM, 0));
        filters.setOpaque(false);
        filters.add(TableFilter.label("Repartição:"));
        filters.add(methodCombo);
        filters.add(previewSummary);
        return card("Pré-visualização por artigo (moeda base)", filters, previewTable, previewBtn, confirmBtn);
    }

    private static ModernPanel card(String title, JComponent filters, JTable table, JComponent... actions) {
        UIHelper.styleTable(table);
        UIHelper.ensureHeadersFit(table);
        JScrollPane scroll = new JScrollPane(table);
        UIHelper.styleScrollPane(scroll);
        ModernPanel card = new ModernPanel(16);
        card.setLayout(new BorderLayout(0, Spacing.SM));
        card.setBorder(new EmptyBorder(Spacing.SM, Spacing.MD, Spacing.SM, Spacing.MD));
        card.add(UIHelper.tableCardTop(title, filters, actions), BorderLayout.NORTH);
        card.add(scroll, BorderLayout.CENTER);
        return card;
    }

    private static JLabel caption(String text) {
        JLabel label = new JLabel(text);
        label.setFont(Typography.CAPTION);
        label.setForeground(UIHelper.TEXT_MUTED);
        return label;
    }

    private static DefaultTableModel readOnlyModel(String... columns) {
        return new DefaultTableModel(columns, 0) {
            @Override public boolean isCellEditable(int row, int column) { return false; }
        };
    }

    public void refresh() {
        Long companyId = CurrentUserContext.findCurrentCompanyId();
        if (companyId == null) return;
        UIHelper.loadAsync(this, () -> purchaseApiClient.getPurchasesByCompany(companyId), this::applyPurchases,
                error -> showError("carregar as compras recebidas", error));
        UIHelper.loadAsync(this, purchaseApiClient::getSuppliers, list -> suppliers = list,
                error -> showError("carregar os fornecedores", error));
        UIHelper.loadAsync(this, comercialApiClient::getActiveVatRates, list -> vatRates = list,
                error -> showError("carregar as taxas de IVA", error));
    }

    /** Only received purchases with a warehouse: CI debts and supplier opening balances have none. */
    void applyPurchases(List<PurchaseDTO> list) {
        purchases.clear();
        purchasesModel.setRowCount(0);
        if (list == null) return;
        for (PurchaseDTO purchase : list) {
            if (!RECEIVED.equals(purchase.status()) || purchase.warehouseId() == null) continue;
            purchases.add(purchase);
            purchasesModel.addRow(new Object[]{Boolean.FALSE, purchase.purchaseNumber(), purchase.supplierName(),
                    purchase.purchaseDate() == null ? "" : DATE.format(purchase.purchaseDate()),
                    purchase.currency(),
                    purchase.baseTotalAmount() != null ? purchase.baseTotalAmount() : purchase.totalAmount()});
        }
    }

    void addCharge(LandedCostChargeDraft draft) {
        charges.add(draft);
        LandedCostChargeRequest line = draft.request();
        chargesModel.addRow(new Object[]{line.type().label(), draft.supplierName(), line.currency(),
                draft.rateLabel(), DisplayNumbers.money(line.amount()), draft.vatLabel()});
        clearPreview();
    }

    private void removeSelectedCharge() {
        int row = chargesTable.getSelectedRow();
        if (row < 0) {
            feedback.show(FeedbackType.WARNING, "Seleccione a linha de custo a remover.");
            return;
        }
        int index = chargesTable.convertRowIndexToModel(row);
        charges.remove(index);
        chargesModel.removeRow(index);
        clearPreview();
    }

    private void openChargeDialog() {
        new LandedCostChargeDialog(suppliers, vatRates, exchangeRateApiClient).show().ifPresent(this::addCharge);
    }

    /** The CI as chosen on screen; {@link IllegalArgumentException} with the warning when something is missing. */
    CreateLandedCostRequest request() {
        List<Long> chosen = new ArrayList<>();
        for (int row = 0; row < purchasesModel.getRowCount(); row++) {
            if (Boolean.TRUE.equals(purchasesModel.getValueAt(row, 0))) chosen.add(purchases.get(row).id());
        }
        if (chosen.isEmpty()) throw new IllegalArgumentException("Escolha pelo menos uma compra recebida.");
        if (charges.isEmpty()) throw new IllegalArgumentException("Indique pelo menos uma linha de custo.");
        return new CreateLandedCostRequest(chosen, (LandedCostAllocationMethod) methodCombo.getSelectedItem(), null,
                charges.stream().map(LandedCostChargeDraft::request).toList());
    }

    private void preview() {
        CreateLandedCostRequest request = requestOrWarn();
        if (request == null) return;
        UIHelper.loadAsync(this, () -> purchaseApiClient.previewLandedCost(request), this::applyPreview,
                error -> showError("pré-visualizar os custos de importação", error));
    }

    void applyPreview(LandedCostPreviewDTO preview) {
        previewModel.setRowCount(0);
        for (LandedCostAllocationDTO line : preview.lines()) {
            previewModel.addRow(new Object[]{line.purchaseNumber(), line.productName(), line.receivedQuantity(),
                    line.unitCostBefore(), line.shareAmount(), line.unitCostAfter()});
        }
        previewSummary.setText("Custo nos artigos " + DisplayNumbers.money(preview.capitalizableCost())
                + " · IVA dedutível " + DisplayNumbers.money(preview.deductibleVat())
                + " · Total das dívidas " + DisplayNumbers.money(preview.payableTotal()));
        confirmBtn.setEnabled(true);
    }

    private void confirm() {
        CreateLandedCostRequest request = requestOrWarn();
        if (request == null) return;
        confirmBtn.setEnabled(false);
        UIHelper.loadAsync(this, () -> purchaseApiClient.confirmLandedCost(request), this::applyConfirmed, error -> {
            confirmBtn.setEnabled(true);
            showError("confirmar o documento CI", error);
        });
    }

    void applyConfirmed(LandedCostDocumentDTO document) {
        String debts = document.charges().stream().map(LandedCostChargeDTO::payableNumber)
                .collect(Collectors.joining(", "));
        charges.clear();
        chargesModel.setRowCount(0);
        for (int row = 0; row < purchasesModel.getRowCount(); row++) purchasesModel.setValueAt(Boolean.FALSE, row, 0);
        methodCombo.setSelectedItem(LandedCostAllocationMethod.VALOR);
        clearPreview();
        feedback.show(FeedbackType.SUCCESS, document.documentNumber() + " confirmado",
                "Dívidas criadas em contas a pagar: " + debts + ".", null, null);
    }

    private CreateLandedCostRequest requestOrWarn() {
        try {
            return request();
        } catch (IllegalArgumentException missing) {
            feedback.show(FeedbackType.WARNING, "Falta informação", missing.getMessage(), null, null);
            return null;
        }
    }

    private void clearPreview() {
        previewModel.setRowCount(0);
        previewSummary.setText(" ");
        confirmBtn.setEnabled(false);
    }

    private void showError(String action, Throwable error) {
        feedback.show(FeedbackType.ERROR, "Não foi possível " + action, error.getMessage(), null, null);
    }

    JTable purchasesTable() { return purchasesTable; }
    JTable chargesTable() { return chargesTable; }
    JTable previewTable() { return previewTable; }
    JLabel previewSummary() { return previewSummary; }
    JComboBox<LandedCostAllocationMethod> methodCombo() { return methodCombo; }
    ModernButton previewButton() { return previewBtn; }
    ModernButton confirmButton() { return confirmBtn; }
    ModernButton removeChargeButton() { return removeChargeBtn; }
    InlineFeedbackPanel feedback() { return feedback; }
}
