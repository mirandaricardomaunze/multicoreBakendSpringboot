package mz.multicore.erp.gui;

import mz.multicore.erp.architecture.exception.BusinessRuleException;
import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.desktop.client.ComercialApiClient;
import mz.multicore.erp.desktop.client.InventoryApiClient;
import mz.multicore.erp.desktop.client.StockWasteApiClient;
import mz.multicore.erp.gui.components.*;
import mz.multicore.erp.modules.comercial.dto.ProductDTO;
import mz.multicore.erp.modules.inventory.dto.ExpiringBatchAlertDTO;
import mz.multicore.erp.modules.inventory.dto.StockWasteDTO;
import mz.multicore.erp.modules.inventory.dto.WarehouseDTO;
import mz.multicore.erp.modules.inventory.dto.WasteSummaryDTO;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

/**
 * Centro de Gestão de Quebras, Perdas e Prevenção de Desperdício (Waste & Shrinkage Management).
 * Orquestrador desacoplado alinhado com o Design System Multicore e fluxo de aprovação por alçada.
 */
public class StockWastePanel extends JPanel {

    private final StockWasteApiClient wasteApiClient;
    private final InventoryApiClient inventoryApiClient;
    private final ComercialApiClient comercialApiClient;

    final JLabel kpiTotalCost = new JLabel("0,00 MT");
    final JLabel kpiTotalQty = new JLabel("0 un");
    final JLabel kpiWasteRate = new JLabel("0,00%");
    final JLabel kpiRiskBatches = new JLabel("0 lotes");

    // Aba 1: Histórico & Validação
    DefaultTableModel wasteTableModel;
    JTable wasteTable;
    JComboBox<String> statusFilterCombo;
    JComboBox<String> reasonFilterCombo;
    JComboBox<String> periodFilterCombo;
    JComboBox<String> wasteBusinessUnitCombo;
    JTextField wasteSearchField;
    ModernButton approveBtn;
    ModernButton rejectBtn;
    List<StockWasteDTO> currentWasteList = new ArrayList<>();

    // Sub-vistas desacopladas
    private final StockWasteRadarView radarView;
    private final StockWasteMetricsView metricsView;

    // Campos de compatibilidade para testes de integração
    DefaultTableModel radarTableModel;
    JTable radarTable;
    JComboBox<String> urgencyFilterCombo;
    JTextField radarSearchField;
    List<ExpiringBatchAlertDTO> currentRadarList = new ArrayList<>();

    private List<WarehouseDTO> cachedWarehouses = new ArrayList<>();
    private List<ProductDTO> cachedProducts = new ArrayList<>();

    public StockWastePanel(StockWasteApiClient wasteApiClient, InventoryApiClient inventoryApiClient, ComercialApiClient comercialApiClient) {
        this.wasteApiClient = wasteApiClient;
        this.inventoryApiClient = inventoryApiClient;
        this.comercialApiClient = comercialApiClient;

        this.radarView = new StockWasteRadarView(this::reloadData, this::openRegisterWasteDialog);
        this.metricsView = new StockWasteMetricsView(this.wasteApiClient, this::updateKpisFromSummary);
        this.radarTable = radarView.getRadarTable();
        this.radarTableModel = radarView.getRadarTableModel();
        this.urgencyFilterCombo = radarView.urgencyFilterCombo;
        this.radarSearchField = radarView.radarSearchField;
        UIHelper.ensureHeadersFit(radarTable);

        setLayout(new BorderLayout(0, 6));
        setOpaque(false);
        setBorder(new EmptyBorder(6, 4, 4, 4));
        add(buildKpiBar(), BorderLayout.NORTH);

        JTabbedPane tabs = new JTabbedPane();
        UIHelper.styleTabbedPaneMulticore(tabs);
        tabs.addTab("Registo & Validação de Quebras", UIHelper.icon("fas-clipboard-check", 16, UIHelper.REJECTED_RED), buildHistoryTab());
        tabs.addTab("Radar de Validades & Prevenção", UIHelper.icon("fas-shield-alt", 16, UIHelper.PENDING_YELLOW), radarView);
        tabs.addTab("Métricas & Relatório Executivo", UIHelper.icon("fas-chart-pie", 16, UIHelper.ACCENT_BLUE), metricsView);
        add(tabs, BorderLayout.CENTER);
    }

    private JPanel buildKpiBar() {
        JPanel kpiRow = KpiCard.createGrid(4);
        kpiRow.add(KpiCard.createCard("PERDA FINANCEIRA TOTAL", kpiTotalCost, "Custo acumulado no período", "fas-dollar-sign", UIHelper.REJECTED_RED));
        kpiRow.add(KpiCard.createCard("QTD TOTAL DESPERDIÇADA", kpiTotalQty, "Unidades abatidas de stock", "fas-boxes", UIHelper.PENDING_YELLOW));
        kpiRow.add(KpiCard.createCard("RÁCIO DE QUEBRA S/ VENDAS", kpiWasteRate, "Meta recomendada: < 1.5%", "fas-percent", UIHelper.ACCENT_BLUE));
        kpiRow.add(KpiCard.createCard("LOTES EM RISCO IMINENTE", kpiRiskBatches, "A vencer nos próximos 7 dias", "fas-exclamation-triangle", UIHelper.ACCENT_ORANGE));
        return kpiRow;
    }

    private JPanel buildHistoryTab() {
        JPanel panel = new JPanel(new BorderLayout(0, 0));
        panel.setOpaque(false);
        panel.setBorder(new EmptyBorder(2, 0, 0, 0));

        statusFilterCombo = new JComboBox<>(new String[]{"Todos os Estados", "Aprovado", "Pendente Aprovação", "Rejeitado"});
        UIHelper.styleComboBox(statusFilterCombo);
        statusFilterCombo.setPreferredSize(new Dimension(135, UIHelper.FORM_CONTROL_HEIGHT));
        statusFilterCombo.addActionListener(e -> applyWasteFilters());

        reasonFilterCombo = new JComboBox<>(new String[]{"Todos os Motivos", "Validade Expirada", "Avaria / Quebra Física",
                "Falha de Refrigeração", "Consumo Interno / Amostra", "Furto / Quebra Desconhecida", "Outro Motivo"});
        UIHelper.styleComboBox(reasonFilterCombo);
        reasonFilterCombo.setPreferredSize(new Dimension(150, UIHelper.FORM_CONTROL_HEIGHT));
        reasonFilterCombo.addActionListener(e -> applyWasteFilters());

        wasteBusinessUnitCombo = TableFilter.combo("Todos os negócios");
        wasteBusinessUnitCombo.setPreferredSize(new Dimension(140, UIHelper.FORM_CONTROL_HEIGHT));
        wasteBusinessUnitCombo.addActionListener(e -> applyWasteFilters());

        periodFilterCombo = TableFilter.periodCombo();
        UIHelper.styleComboBox(periodFilterCombo);
        periodFilterCombo.setPreferredSize(new Dimension(120, UIHelper.FORM_CONTROL_HEIGHT));
        periodFilterCombo.addActionListener(e -> applyWasteFilters());

        wasteSearchField = new SearchField("Pesquisar por produto, armazém, lote ou registador...");
        wasteSearchField.setPreferredSize(new Dimension(160, UIHelper.FORM_CONTROL_HEIGHT));
        UIHelper.onTextChange(wasteSearchField, this::applyWasteFilters);

        ModernButton reloadBtn = UIHelper.createSecondaryButton("Actualizar");
        reloadBtn.setIcon(UIHelper.icon("fas-sync-alt", 14));
        reloadBtn.addActionListener(e -> reloadData());

        ModernButton registerBtn = UIHelper.createDangerButton("Registar Quebra");
        registerBtn.setIcon(UIHelper.icon("fas-plus", 14));
        registerBtn.addActionListener(e -> openRegisterWasteDialog(null));

        approveBtn = UIHelper.createSuccessButton("Aprovar");
        approveBtn.setIcon(UIHelper.icon("fas-check", 14));
        approveBtn.setEnabled(false);
        approveBtn.addActionListener(e -> handleApproveSelected(true));

        rejectBtn = UIHelper.createDangerButton("Rejeitar");
        rejectBtn.setIcon(UIHelper.icon("fas-times", 14));
        rejectBtn.setEnabled(false);
        rejectBtn.addActionListener(e -> handleApproveSelected(false));

        ModernButton peekToggleBtn = UIHelper.createSecondaryButton("");
        peekToggleBtn.setIcon(UIHelper.icon("fas-columns", 12));
        peekToggleBtn.setToolTipText("Espreitar registo de quebra (Espaço)");
        peekToggleBtn.getAccessibleContext().setAccessibleName("Espreitar quebra");
        peekToggleBtn.setPreferredSize(new Dimension(30, UIHelper.FORM_CONTROL_HEIGHT));

        JPanel filters = TableFilter.bar(wasteSearchField, TableFilter.label("Negócio:"), wasteBusinessUnitCombo,
                statusFilterCombo, reasonFilterCombo, periodFilterCombo, peekToggleBtn);
        filters.setBorder(new EmptyBorder(0, 0, 4, 0));

        String[] cols = {"ID", "Data", "Produto", "Armazém", "Lote", "Motivo", "Quantidade", "Custo Unit.", "Total Perda", "Estado", "Registado Por", "Aprovado Por", "Negócio"};
        wasteTableModel = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        wasteTable = new JTable(wasteTableModel);
        UIHelper.styleTable(wasteTable);
        wasteTable.setRowHeight(42);
        wasteTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        wasteTable.getSelectionModel().addListSelectionListener(e -> updateApprovalButtonsState());

        int[] widths = {50, 110, 180, 130, 100, 160, 90, 100, 110, 140, 110, 110, 110};
        for (int i = 0; i < widths.length; i++) wasteTable.getColumnModel().getColumn(i).setPreferredWidth(widths[i]);

        DefaultTableCellRenderer rightRenderer = new DefaultTableCellRenderer();
        rightRenderer.setHorizontalAlignment(SwingConstants.RIGHT);
        wasteTable.getColumnModel().getColumn(6).setCellRenderer(rightRenderer);
        wasteTable.getColumnModel().getColumn(7).setCellRenderer(rightRenderer);
        wasteTable.getColumnModel().getColumn(8).setCellRenderer(rightRenderer);

        wasteTable.getColumnModel().getColumn(9).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                setHorizontalAlignment(SwingConstants.CENTER);
                String val = String.valueOf(value);
                if (val.contains("Aprovado")) setForeground(UIHelper.APPROVED_GREEN);
                else if (val.contains("Pendente")) setForeground(UIHelper.PENDING_YELLOW);
                else if (val.contains("Rejeitado")) setForeground(UIHelper.REJECTED_RED);
                setFont(getFont().deriveFont(Font.BOLD));
                return c;
            }
        });

        JScrollPane scroll = new JScrollPane(wasteTable);
        UIHelper.styleScrollPane(scroll);
        UIHelper.ensureHeadersFit(wasteTable);

        ModernPanel card = new ModernPanel(16);
        card.setLayout(new BorderLayout(0, 6));
        card.setBorder(new EmptyBorder(8, 12, 8, 12));
        card.add(UIHelper.tableCardTop("Registo de Quebras & Perdas", filters, reloadBtn, registerBtn, approveBtn, rejectBtn), BorderLayout.NORTH);
        card.add(scroll, BorderLayout.CENTER);
        card.add(ClientTablePagination.install(wasteTable), BorderLayout.SOUTH);

        TableQuickPeekController peekController = TableQuickPeekController.install(wasteTable, card, (peek, mr) -> {
            if (mr < 0 || mr >= wasteTableModel.getRowCount()) return;
            peek.setTitle("Quebra #" + wasteTableModel.getValueAt(mr, 0));
            peek.setSubtitle(String.valueOf(wasteTableModel.getValueAt(mr, 2)));
            peek.setItems(List.of(new QuickPeekPanel.PeekItem("Data", String.valueOf(wasteTableModel.getValueAt(mr, 1)), false),
                    new QuickPeekPanel.PeekItem("Armazém", String.valueOf(wasteTableModel.getValueAt(mr, 3)), false),
                    new QuickPeekPanel.PeekItem("Lote", String.valueOf(wasteTableModel.getValueAt(mr, 4)), false),
                    new QuickPeekPanel.PeekItem("Motivo", String.valueOf(wasteTableModel.getValueAt(mr, 5)), false),
                    new QuickPeekPanel.PeekItem("Quantidade", String.valueOf(wasteTableModel.getValueAt(mr, 6)), false),
                    new QuickPeekPanel.PeekItem("Custo Unit.", String.valueOf(wasteTableModel.getValueAt(mr, 7)), false),
                    new QuickPeekPanel.PeekItem("Perda Total", String.valueOf(wasteTableModel.getValueAt(mr, 8)), true),
                    new QuickPeekPanel.PeekItem("Estado", String.valueOf(wasteTableModel.getValueAt(mr, 9)), false)));
            peek.getOpenFullButton().setText("Ficha de Quebra");
        });
        peekToggleBtn.addActionListener(e -> peekController.toggle());
        panel.add(card, BorderLayout.CENTER);
        return panel;
    }

    public void reload() { reloadData(); }

    private void reloadData() {
        Long companyId = CurrentUserContext.requireCurrentCompanyId();

        new SwingWorker<Void, Void>() {
            @Override protected Void doInBackground() {
                try {
                    cachedWarehouses = inventoryApiClient.getWarehousesByCompany(companyId);
                    cachedProducts = comercialApiClient.getAllProducts();
                } catch (Exception ignored) {}
                return null;
            }
        }.execute();

        new SwingWorker<List<StockWasteDTO>, Void>() {
            @Override protected List<StockWasteDTO> doInBackground() { return wasteApiClient.findByCompany(companyId, null); }
            @Override protected void done() {
                try {
                    currentWasteList = get();
                    if (wasteBusinessUnitCombo != null) {
                        Object prev = wasteBusinessUnitCombo.getSelectedItem();
                        Set<String> bus = new TreeSet<>();
                        for (StockWasteDTO w : currentWasteList) {
                            if (w.businessUnitName() != null && !w.businessUnitName().isBlank()) bus.add(w.businessUnitName());
                        }
                        wasteBusinessUnitCombo.removeAllItems();
                        wasteBusinessUnitCombo.addItem("Todos os negócios");
                        for (String b : bus) wasteBusinessUnitCombo.addItem(b);
                        if (prev != null) wasteBusinessUnitCombo.setSelectedItem(prev);
                    }
                    applyWasteFilters();
                } catch (Exception ex) {
                    ToastManager.show(StockWastePanel.this, FeedbackType.ERROR, "Erro ao carregar quebras: " + ex.getMessage());
                }
            }
        }.execute();

        new SwingWorker<List<ExpiringBatchAlertDTO>, Void>() {
            @Override protected List<ExpiringBatchAlertDTO> doInBackground() { return wasteApiClient.getExpiringRadar(companyId, 30); }
            @Override protected void done() {
                try {
                    currentRadarList = get();
                    radarView.setRadarList(currentRadarList);
                    long riskCount = currentRadarList.stream().filter(b -> b.daysUntilExpiration() <= 7).count();
                    kpiRiskBatches.setText(riskCount + " lotes");
                } catch (Exception ex) {
                    ToastManager.show(StockWastePanel.this, FeedbackType.ERROR, "Erro ao carregar radar: " + ex.getMessage());
                }
            }
        }.execute();

        metricsView.reloadMetrics();
    }

    private void updateKpisFromSummary(WasteSummaryDTO s) {
        if (s == null) return;
        kpiTotalCost.setText(UIHelper.formatMzn(s.totalWasteCost()));
        kpiTotalQty.setText(s.totalWasteQuantity().stripTrailingZeros().toPlainString() + " un");
        kpiWasteRate.setText(s.wasteRatePercentage().toPlainString() + "%");
    }

    void applyWasteFilters() {
        String buSel = (String) (wasteBusinessUnitCombo != null ? wasteBusinessUnitCombo.getSelectedItem() : "Todos os negócios");
        String statusSel = (String) statusFilterCombo.getSelectedItem();
        String reasonSel = (String) reasonFilterCombo.getSelectedItem();
        String periodSel = (String) (periodFilterCombo != null ? periodFilterCombo.getSelectedItem() : "Todo o período");
        String query = wasteSearchField.getText().trim().toLowerCase();

        wasteTableModel.setRowCount(0);
        LocalDate today = LocalDate.now();
        for (StockWasteDTO w : currentWasteList) {
            if (buSel != null && !"Todos os negócios".equals(buSel)) {
                String wBu = w.businessUnitName() != null ? w.businessUnitName() : "";
                if (!buSel.equalsIgnoreCase(wBu)) continue;
            }
            if (!"Todos os Estados".equals(statusSel) && !w.status().getDescription().equals(statusSel)) continue;
            if (!"Todos os Motivos".equals(reasonSel) && !w.reason().getDescription().equals(reasonSel)) continue;
            if (w.createdAt() != null && !TableFilter.matchesPeriod(w.createdAt().toLocalDate(), periodSel, today)) continue;
            if (!query.isEmpty()) {
                boolean match = (w.productName() != null && w.productName().toLowerCase().contains(query))
                        || (w.productSku() != null && w.productSku().toLowerCase().contains(query))
                        || (w.warehouseName() != null && w.warehouseName().toLowerCase().contains(query))
                        || (w.batchNumber() != null && w.batchNumber().toLowerCase().contains(query))
                        || (w.registeredBy() != null && w.registeredBy().toLowerCase().contains(query));
                if (!match) continue;
            }

            wasteTableModel.addRow(new Object[]{
                    w.id(), w.createdAt() != null ? w.createdAt().format(UIHelper.DATETIME_FMT) : "-",
                    w.productName(), w.warehouseName(), BatchDisplay.number(w.batchNumber()),
                    w.reason().getDescription(), w.quantity().stripTrailingZeros().toPlainString() + " un",
                    UIHelper.formatMzn(w.unitCost()), UIHelper.formatMzn(w.totalCost()),
                    w.status().getDescription(), w.registeredBy(),
                    w.approvedBy() != null ? w.approvedBy() : "-",
                    w.businessUnitName() != null && !w.businessUnitName().isBlank() ? w.businessUnitName() : "—"
            });
        }
        updateApprovalButtonsState();
    }

    void updateApprovalButtonsState() {
        int row = TableFilter.selectedModelRow(wasteTable);
        if (row < 0 || row >= wasteTableModel.getRowCount()) {
            approveBtn.setEnabled(false);
            rejectBtn.setEnabled(false);
            return;
        }
        String statusStr = (String) wasteTableModel.getValueAt(row, 9);
        boolean isPending = statusStr != null && statusStr.contains("Pendente");
        approveBtn.setEnabled(isPending);
        rejectBtn.setEnabled(isPending);
    }

    private void handleApproveSelected(boolean approve) {
        int row = TableFilter.selectedModelRow(wasteTable);
        if (row < 0) {
            ToastManager.show(this, FeedbackType.WARNING, "Seleccione uma quebra para continuar.");
            return;
        }
        Long id = (Long) wasteTableModel.getValueAt(row, 0);
        String actionTitle = approve ? "Aprovar Registo de Quebra" : "Rejeitar Registo de Quebra";
        String prompt = approve ? "Deseja aprovar esta quebra de stock? O stock será abatido no sistema." : "Indique o motivo da rejeição da quebra:";

        JPanel form = new JPanel(new BorderLayout(0, 10));
        form.setOpaque(false);
        JLabel lbl = new JLabel("<html><body style='width:360px'>" + prompt + "</body></html>");
        lbl.setForeground(UIHelper.TEXT_LIGHT);
        lbl.setFont(new Font(UIHelper.FONT, Font.PLAIN, 13));
        form.add(lbl, BorderLayout.NORTH);

        JTextField notesField = new JTextField();
        UIHelper.styleTextField(notesField);
        if (!approve) form.add(notesField, BorderLayout.CENTER);

        ModernFormDialog dlg = new ModernFormDialog(SwingUtilities.getWindowAncestor(this), actionTitle, form);
        dlg.setConfirmButton(approve ? "Aprovar" : "Rejeitar", approve ? "fas-check" : "fas-times");
        final String[] resultNotes = new String[1];
        dlg.setOnSave(() -> {
            String text = notesField.getText().trim();
            if (!approve && text.isEmpty()) throw new BusinessRuleException("Indique o motivo da rejeição.");
            resultNotes[0] = text;
        });
        if (!dlg.showDialog()) return;

        UIHelper.runWithProgress(this, (approve ? "A aprovar" : "A rejeitar") + " quebra...", () -> {
            wasteApiClient.approve(id, approve, resultNotes[0]);
            return null;
        }, res -> {
            ToastManager.success(this, "Registo " + (approve ? "aprovado" : "rejeitado") + " com sucesso!");
            reloadData();
        }, err -> ToastManager.show(this, FeedbackType.ERROR, "Falha ao processar: " + err.getMessage()));
    }

    private void openRegisterWasteDialog(ExpiringBatchAlertDTO prefillBatch) {
        StockWasteRegistrationDialog.open(this, wasteApiClient, cachedWarehouses, cachedProducts, prefillBatch, this::reloadData);
    }
}
