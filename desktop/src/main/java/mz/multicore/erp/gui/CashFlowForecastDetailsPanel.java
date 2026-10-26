package mz.multicore.erp.gui;

import mz.multicore.erp.gui.components.*;
import mz.multicore.erp.modules.financeira.dto.CashFlowItemDTO;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.time.format.DateTimeFormatter;
import java.util.List;

/** Painel em abas dos maiores recebimentos previstos e pagamentos a fornecedores. */
public class CashFlowForecastDetailsPanel extends ModernPanel {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final DefaultTableModel topReceivablesModel;
    private final JTable topReceivablesTable;

    private final DefaultTableModel topPayablesModel;
    private final JTable topPayablesTable;

    public CashFlowForecastDetailsPanel() {
        super(16);
        setLayout(new BorderLayout(0, 6));
        setBorder(new EmptyBorder(12, 14, 12, 14));

        JTabbedPane tabs = new JTabbedPane();
        UIHelper.styleTabbedPaneMulticore(tabs);

        String[] rCols = {"Documento", "Cliente", "Vencimento", "Montante Pendente", "Período"};
        topReceivablesModel = new DefaultTableModel(rCols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        topReceivablesTable = new JTable(topReceivablesModel);
        topReceivablesTable.putClientProperty(ClientTablePagination.DISABLED, Boolean.TRUE);
        UIHelper.styleTable(topReceivablesTable);
        topReceivablesTable.setPreferredScrollableViewportSize(new Dimension(100, 260));
        topReceivablesTable.getColumnModel().getColumn(3).setCellRenderer(TableCellRenderers.money());
        UIHelper.ensureHeadersFit(topReceivablesTable);
        JScrollPane rScroll = new JScrollPane(topReceivablesTable);
        UIHelper.styleScrollPane(rScroll);
        tabs.addTab("Maiores Recebimentos Previstos", UIHelper.icon("fas-hand-holding-usd", 13, UIHelper.APPROVED_GREEN), rScroll);

        String[] pCols = {"Documento", "Fornecedor", "Vencimento", "Montante Pendente", "Período"};
        topPayablesModel = new DefaultTableModel(pCols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        topPayablesTable = new JTable(topPayablesModel);
        topPayablesTable.putClientProperty(ClientTablePagination.DISABLED, Boolean.TRUE);
        UIHelper.styleTable(topPayablesTable);
        topPayablesTable.setPreferredScrollableViewportSize(new Dimension(100, 260));
        topPayablesTable.getColumnModel().getColumn(3).setCellRenderer(TableCellRenderers.money());
        UIHelper.ensureHeadersFit(topPayablesTable);
        JScrollPane pScroll = new JScrollPane(topPayablesTable);
        UIHelper.styleScrollPane(pScroll);
        tabs.addTab("Maiores Pagamentos a Fornecedores", UIHelper.icon("fas-file-invoice-dollar", 13, UIHelper.REJECTED_RED), pScroll);

        UIHelper.installQuickPeek(topReceivablesTable, this);
        UIHelper.installQuickPeek(topPayablesTable, this);

        add(tabs, BorderLayout.CENTER);
        setPreferredSize(new Dimension(0, 360));
        setMaximumSize(new Dimension(Integer.MAX_VALUE, 360));
    }

    public void populate(List<CashFlowItemDTO> receivables, List<CashFlowItemDTO> payables) {
        topReceivablesModel.setRowCount(0);
        if (receivables != null) {
            for (CashFlowItemDTO r : receivables) {
                topReceivablesModel.addRow(new Object[]{
                        r.documentNumber(), r.entityName(),
                        r.dueDate() != null ? r.dueDate().format(DATE_FMT) : "-",
                        r.amount(), r.bucketCode()
                });
            }
        }

        topPayablesModel.setRowCount(0);
        if (payables != null) {
            for (CashFlowItemDTO p : payables) {
                topPayablesModel.addRow(new Object[]{
                        p.documentNumber(), p.entityName(),
                        p.dueDate() != null ? p.dueDate().format(DATE_FMT) : "-",
                        p.amount(), p.bucketCode()
                });
            }
        }
    }
}
