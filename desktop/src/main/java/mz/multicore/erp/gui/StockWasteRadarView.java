package mz.multicore.erp.gui;

import mz.multicore.erp.gui.components.*;
import mz.multicore.erp.modules.inventory.dto.ExpiringBatchAlertDTO;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Vista da aba "Radar de Validades & Prevenção" de quebras de stock.
 */
public class StockWasteRadarView extends JPanel {

    final DefaultTableModel radarTableModel;
    final JTable radarTable;
    final JComboBox<String> urgencyFilterCombo;
    final JTextField radarSearchField;
    private final Consumer<ExpiringBatchAlertDTO> onWasteFromBatch;
    private List<ExpiringBatchAlertDTO> currentRadarList = new ArrayList<>();

    public StockWasteRadarView(Runnable onReload, Consumer<ExpiringBatchAlertDTO> onWasteFromBatch) {
        super(new BorderLayout(0, 0));
        this.onWasteFromBatch = onWasteFromBatch;
        setOpaque(false);
        setBorder(new EmptyBorder(2, 0, 0, 0));

        urgencyFilterCombo = new JComboBox<>(new String[]{"Todas as Urgências", "Vencidos", "Crítico (≤ 3 dias)", "Alto (≤ 7 dias)", "Médio (≤ 15 dias)", "Atenção (≤ 30 dias)"});
        UIHelper.styleComboBox(urgencyFilterCombo);
        urgencyFilterCombo.setPreferredSize(new Dimension(180, UIHelper.FORM_CONTROL_HEIGHT));
        urgencyFilterCombo.addActionListener(e -> applyRadarFilters());

        radarSearchField = TableFilter.searchField("Pesquisar por lote, produto, armazém…");
        radarSearchField.setPreferredSize(new Dimension(280, UIHelper.FORM_CONTROL_HEIGHT));
        UIHelper.onTextChange(radarSearchField, this::applyRadarFilters);

        ModernButton reloadRadarBtn = UIHelper.createRefreshButton(() -> { if (onReload != null) onReload.run(); });

        ModernButton wasteRadarBtn = UIHelper.createDangerButton("Registar Quebra deste Lote");
        wasteRadarBtn.setIcon(UIHelper.icon("fas-trash-alt", 14));
        wasteRadarBtn.setEnabled(false);
        wasteRadarBtn.setToolTipText("Selecione um lote na tabela para registar quebra");
        wasteRadarBtn.addActionListener(e -> triggerWasteFromSelected());

        ModernButton peekRadarBtn = UIHelper.createSecondaryButton("");
        peekRadarBtn.setIcon(UIHelper.icon("fas-columns", 12));
        peekRadarBtn.setToolTipText("Espreitar lote em risco (Espaço)");
        peekRadarBtn.getAccessibleContext().setAccessibleName("Espreitar lote");
        peekRadarBtn.setPreferredSize(new Dimension(30, UIHelper.FORM_CONTROL_HEIGHT));

        JPanel radarFilters = TableFilter.bar(radarSearchField, urgencyFilterCombo, peekRadarBtn);
        radarFilters.setBorder(new EmptyBorder(0, 0, 4, 0));

        String[] cols = {"Lote", "Produto", "Categoria", "Armazém", "Qtd Disponível", "Custo Unit.", "Perda Estimada", "Data Validade", "Dias Restantes", "Urgência"};
        radarTableModel = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        radarTable = new JTable(radarTableModel);
        UIHelper.styleTable(radarTable);
        radarTable.setRowHeight(42);
        radarTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        radarTable.getSelectionModel().addListSelectionListener(e -> wasteRadarBtn.setEnabled(radarTable.getSelectedRow() >= 0));

        DefaultTableCellRenderer rightRenderer = new DefaultTableCellRenderer();
        rightRenderer.setHorizontalAlignment(SwingConstants.RIGHT);
        radarTable.getColumnModel().getColumn(4).setCellRenderer(rightRenderer);
        radarTable.getColumnModel().getColumn(5).setCellRenderer(rightRenderer);
        radarTable.getColumnModel().getColumn(6).setCellRenderer(rightRenderer);

        radarTable.getColumnModel().getColumn(9).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                setHorizontalAlignment(SwingConstants.CENTER);
                String val = String.valueOf(value);
                if (val.contains("VENCIDO") || val.contains("CRÍTICO")) setForeground(UIHelper.REJECTED_RED);
                else if (val.contains("ALTO")) setForeground(UIHelper.KPI_ORANGE_DARK);
                else if (val.contains("MÉDIO")) setForeground(UIHelper.PENDING_YELLOW);
                else setForeground(UIHelper.ACCENT_BLUE);
                setFont(getFont().deriveFont(Font.BOLD));
                return c;
            }
        });
        UIHelper.ensureHeadersFit(radarTable);

        JScrollPane scroll = new JScrollPane(radarTable);
        UIHelper.styleScrollPane(scroll);

        ModernPanel card = new ModernPanel(16);
        card.setLayout(new BorderLayout(0, 6));
        card.setBorder(new EmptyBorder(8, 12, 8, 12));
        card.add(UIHelper.tableCardTop("Radar de Validades & Prevenção", radarFilters, reloadRadarBtn, wasteRadarBtn), BorderLayout.NORTH);
        card.add(scroll, BorderLayout.CENTER);
        card.add(ClientTablePagination.install(radarTable), BorderLayout.SOUTH);

        TableQuickPeekController radarPeek = TableQuickPeekController.install(radarTable, card, (peek, mr) -> {
            if (mr < 0 || mr >= radarTableModel.getRowCount()) return;
            String batch = String.valueOf(radarTableModel.getValueAt(mr, 0)), prod = String.valueOf(radarTableModel.getValueAt(mr, 1)),
                    cat = String.valueOf(radarTableModel.getValueAt(mr, 2)), wh = String.valueOf(radarTableModel.getValueAt(mr, 3)),
                    qty = String.valueOf(radarTableModel.getValueAt(mr, 4)), cost = String.valueOf(radarTableModel.getValueAt(mr, 5)),
                    loss = String.valueOf(radarTableModel.getValueAt(mr, 6)), exp = String.valueOf(radarTableModel.getValueAt(mr, 7)),
                    days = String.valueOf(radarTableModel.getValueAt(mr, 8)), urg = String.valueOf(radarTableModel.getValueAt(mr, 9));
            peek.setTitle("Lote " + batch);
            peek.setSubtitle(prod + " • " + wh);
            peek.setStatus(urg, urg.contains("VENCIDO") || urg.contains("CRÍTICO") ? UIHelper.REJECTED_RED : UIHelper.PENDING_YELLOW);
            peek.setItems(List.of(new QuickPeekPanel.PeekItem("Categoria", cat, false), new QuickPeekPanel.PeekItem("Qtd em Risco", qty, false),
                    new QuickPeekPanel.PeekItem("Custo Unit.", cost, false), new QuickPeekPanel.PeekItem("Perda Estimada", loss, true),
                    new QuickPeekPanel.PeekItem("Validade", exp, false), new QuickPeekPanel.PeekItem("Dias Restantes", days, false)));
            peek.getOpenFullButton().setText("Registar Quebra");
            peek.setOnOpenFullAction(v -> triggerWasteFromSelected());
        });
        peekRadarBtn.addActionListener(e -> radarPeek.toggle());
        add(card, BorderLayout.CENTER);
    }

    public void setRadarList(List<ExpiringBatchAlertDTO> alerts) {
        this.currentRadarList = alerts != null ? alerts : new ArrayList<>();
        applyRadarFilters();
    }

    public List<ExpiringBatchAlertDTO> getCurrentRadarList() {
        return currentRadarList;
    }

    public void applyRadarFilters() {
        String urgencySel = (String) urgencyFilterCombo.getSelectedItem();
        String query = radarSearchField != null ? radarSearchField.getText().trim().toLowerCase() : "";
        radarTableModel.setRowCount(0);

        for (ExpiringBatchAlertDTO b : currentRadarList) {
            if ("Vencidos".equals(urgencySel) && b.daysUntilExpiration() >= 0) continue;
            if ("Crítico (≤ 3 dias)".equals(urgencySel) && (b.daysUntilExpiration() < 0 || b.daysUntilExpiration() > 3)) continue;
            if ("Alto (≤ 7 dias)".equals(urgencySel) && (b.daysUntilExpiration() < 0 || b.daysUntilExpiration() > 7)) continue;
            if ("Médio (≤ 15 dias)".equals(urgencySel) && (b.daysUntilExpiration() < 0 || b.daysUntilExpiration() > 15)) continue;
            if ("Atenção (≤ 30 dias)".equals(urgencySel) && (b.daysUntilExpiration() < 0 || b.daysUntilExpiration() > 30)) continue;

            if (!query.isEmpty()) {
                boolean match = (b.batchNumber() != null && b.batchNumber().toLowerCase().contains(query))
                        || (b.productName() != null && b.productName().toLowerCase().contains(query))
                        || (b.categoryName() != null && b.categoryName().toLowerCase().contains(query))
                        || (b.warehouseName() != null && b.warehouseName().toLowerCase().contains(query));
                if (!match) continue;
            }

            radarTableModel.addRow(new Object[]{
                    b.batchNumber(), b.productName(), b.categoryName(), b.warehouseName(),
                    b.quantity().stripTrailingZeros().toPlainString() + " un", UIHelper.formatMzn(b.unitCost()),
                    UIHelper.formatMzn(b.potentialLossValue()),
                    b.expirationDate() != null ? b.expirationDate().format(UIHelper.DATE_FMT) : "-",
                    b.daysUntilExpiration() < 0 ? "Venceu há " + (-b.daysUntilExpiration()) + " dias" : b.daysUntilExpiration() + " dias",
                    b.alertLevel()
            });
        }
    }

    public ExpiringBatchAlertDTO getSelectedAlert() {
        int row = TableFilter.selectedModelRow(radarTable);
        if (row < 0) return null;
        String batchNum = (String) radarTableModel.getValueAt(row, 0);
        return currentRadarList.stream().filter(b -> b.batchNumber().equals(batchNum)).findFirst().orElse(null);
    }

    private void triggerWasteFromSelected() {
        ExpiringBatchAlertDTO alert = getSelectedAlert();
        if (alert == null) {
            ToastManager.show(this, FeedbackType.INFO, "Selecione primeiro um lote na tabela do radar.");
            return;
        }
        if (onWasteFromBatch != null) onWasteFromBatch.accept(alert);
    }

    public JTable getRadarTable() { return radarTable; }
    public DefaultTableModel getRadarTableModel() { return radarTableModel; }
}
