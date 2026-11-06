package mz.multicore.erp.gui;

import mz.multicore.erp.gui.components.*;
import mz.multicore.erp.modules.approvals.dto.ApprovalRequestDTO;
import mz.multicore.erp.modules.approvals.model.ApprovalStatus;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/** Painel de histórico e auditoria de aprovações com filtros semânticos e Quick Peek. */
public class ApprovalsHistoryView extends JPanel {

    private final DefaultTableModel historyModel;
    private final JTable historyTable;
    private static final DateTimeFormatter DATE_TIME_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    public ApprovalsHistoryView(Runnable onRefresh) {
        setLayout(new BorderLayout(0, 8));
        setOpaque(false);
        setBorder(new EmptyBorder(8, 0, 0, 0));

        ModernPanel historyCard = new ModernPanel(16);
        historyCard.setLayout(new BorderLayout(0, 8));
        historyCard.setBorder(new EmptyBorder(12, 14, 12, 14));

        String[] historyCols = {"Data", "Tipo Doc", "Submissor", "Valor", "Estado Final", "Motivo / Comentário"};
        historyModel = new DefaultTableModel(historyCols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        historyTable = new JTable(historyModel);
        UIHelper.styleTable(historyTable);
        UIHelper.ensureHeadersFit(historyTable);
        historyTable.getColumnModel().getColumn(3).setCellRenderer(TableCellRenderers.money());
        historyTable.getColumnModel().getColumn(4).setCellRenderer(TableCellRenderers.status());

        JScrollPane historyScroll = new JScrollPane(historyTable);
        UIHelper.styleScrollPane(historyScroll);

        TableQuickPeekController.install(historyTable, historyCard, (peek, mr) -> {
            int row = mr; // o controlador já entrega a linha do modelo
            peek.setHeaderIcon("fas-history", UIHelper.ACCENT_BLUE);
            peek.setTitle(String.valueOf(historyModel.getValueAt(row, 1)));
            peek.setSubtitle("Data: " + historyModel.getValueAt(row, 0));
            peek.setStatus(String.valueOf(historyModel.getValueAt(row, 4)), UIHelper.ACCENT_BLUE);
            List<QuickPeekPanel.PeekItem> items = new ArrayList<>();
            items.add(new QuickPeekPanel.PeekItem("Submissor", peek.cell(historyModel.getValueAt(row, 2), 2), false));
            items.add(new QuickPeekPanel.PeekItem("Valor", peek.cell(historyModel.getValueAt(row, 3), 3), true));
            items.add(new QuickPeekPanel.PeekItem("Decisão Final", peek.cell(historyModel.getValueAt(row, 4), 4), false));
            items.add(new QuickPeekPanel.PeekItem("Motivo / Parecer", peek.cell(historyModel.getValueAt(row, 5), 5), false));
            peek.setItems(items);
        });

        JTextField hSearch = TableFilter.searchField("Documento ou submissor…");
        JComboBox<String> hEstado = TableFilter.combo("Todos os estados", "APPROVED", "REJECTED");
        JComboBox<String> hPeriodo = TableFilter.periodCombo();
        TableFilter.install(historyTable, hSearch,
                List.of(new TableFilter.ColumnFilter(hEstado, 4)),
                List.of(new TableFilter.PeriodFilter(hPeriodo, 0)));

        JPanel hBar = TableFilter.bar(hSearch, TableFilter.label("Estado final:"), hEstado,
                TableFilter.label("Data:", "fas-calendar-alt"), hPeriodo);
        hBar.setBorder(new EmptyBorder(0, 0, 4, 0));

        ModernButton hRefreshBtn = UIHelper.createRefreshButton(onRefresh != null ? onRefresh : () -> {});
        historyCard.add(UIHelper.tableCardTop("Histórico e Auditoria de Aprovações", hBar, hRefreshBtn), BorderLayout.NORTH);
        historyCard.add(historyScroll, BorderLayout.CENTER);
        historyCard.add(ClientTablePagination.install(historyTable), BorderLayout.SOUTH);

        add(historyCard, BorderLayout.CENTER);
    }

    public void populate(List<ApprovalRequestDTO> historyList) {
        historyModel.setRowCount(0);
        if (historyList == null) return;
        for (ApprovalRequestDTO req : historyList) {
            if (req.status() != ApprovalStatus.PENDING) {
                String docLabel = ApprovalsOperationsCoordinator.humanType(req.documentType()) + " #" + req.documentId();
                historyModel.addRow(new Object[]{
                        req.createdAt().format(DATE_TIME_FMT),
                        docLabel,
                        req.submitter(),
                        req.amount(),
                        req.status().name(),
                        req.status() == ApprovalStatus.REJECTED ? req.rejectionReason() : "Validado administrativo"
                });
            }
        }
    }
}
