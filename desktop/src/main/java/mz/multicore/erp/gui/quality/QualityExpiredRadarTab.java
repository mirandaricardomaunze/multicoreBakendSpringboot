package mz.multicore.erp.gui.quality;

import mz.multicore.erp.desktop.client.PrintApiClient;
import mz.multicore.erp.desktop.client.QualityApiClient;
import mz.multicore.erp.gui.components.*;
import mz.multicore.erp.modules.quality.dto.ExpiredWorkerAlertDTO;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.time.format.DateTimeFormatter;
import java.util.function.Supplier;

public class QualityExpiredRadarTab extends JPanel {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final QualityApiClient qualityApiClient;
    private final PrintApiClient printApiClient;
    private final Supplier<Long> companyIdSupplier;
    private final Runnable onDataChanged;

    private DefaultTableModel expiredTableModel;
    private JTable expiredTable;
    private final JLabel expiredSummaryLabel = new JLabel("0 colaboradores com equipamentos expirados");

    public QualityExpiredRadarTab(QualityApiClient qualityApiClient, PrintApiClient printApiClient,
                                  Supplier<Long> companyIdSupplier, Runnable onDataChanged) {
        super(new BorderLayout());
        this.qualityApiClient = qualityApiClient;
        this.printApiClient = printApiClient;
        this.companyIdSupplier = companyIdSupplier;
        this.onDataChanged = onDataChanged;

        setOpaque(false);
        initTab();
    }

    private void initTab() {
        ModernPanel card = new ModernPanel(16);
        card.setLayout(new BorderLayout(0, 8));
        card.setBorder(BorderFactory.createEmptyBorder(12, 12, 8, 12));

        String[] cols = {"AssignmentID", "Colaborador", "Departamento", "Equipamento", "Entrega", "Validade", "Atraso / Prazo", "Estado"};
        expiredTableModel = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int r, int c) { return false; }
        };
        expiredTable = new JTable(expiredTableModel);
        expiredTable.putClientProperty(ClientTablePagination.DISABLED, Boolean.TRUE);
        UIHelper.styleTable(expiredTable);
        UIHelper.ensureHeadersFit(expiredTable);

        expiredTable.getColumnModel().getColumn(0).setMinWidth(0);
        expiredTable.getColumnModel().getColumn(0).setMaxWidth(0);
        expiredTable.getColumnModel().getColumn(0).setPreferredWidth(0);

        expiredTable.getColumnModel().getColumn(6).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object val, boolean isSel, boolean hasFocus, int r, int c) {
                super.getTableCellRendererComponent(table, val, isSel, hasFocus, r, c);
                setHorizontalAlignment(SwingConstants.CENTER);
                setForeground(UIHelper.REJECTED_RED);
                setFont(getFont().deriveFont(Font.BOLD));
                return this;
            }
        });

        expiredSummaryLabel.setForeground(UIHelper.TEXT_MUTED);
        expiredSummaryLabel.setFont(new Font(UIHelper.FONT, Font.PLAIN, 12));

        ModernButton refreshBtn = UIHelper.createRefreshButton(this::refresh);
        ModernButton renewBtn = UIHelper.createPrimaryButton("Renovar Seleccionado");
        renewBtn.addActionListener(e -> renewSelectedFromRadar());

        ActionMenuButton radarMenu = UIHelper.createActionMenuButton("Documentos");
        radarMenu.addAction("Imprimir Termo de Entrega", UIHelper.icon("fas-print", 13), this::printSelectedDeliverySheet);
        radarMenu.addAction("Exportar Relatório (PDF)", UIHelper.icon("fas-file-pdf", 13), this::exportRadarList);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        actions.setOpaque(false);
        actions.add(expiredSummaryLabel);
        actions.add(Box.createHorizontalStrut(10));
        actions.add(refreshBtn);
        actions.add(renewBtn);
        actions.add(radarMenu);

        card.add(UIHelper.tableCardTop("Radar Automático de Colaboradores com Validade Expirada", actions, (JComponent) null), BorderLayout.NORTH);
        card.add(new JScrollPane(expiredTable), BorderLayout.CENTER);

        add(card, BorderLayout.CENTER);
    }

    public void refresh() {
        final Long companyId = companyIdSupplier != null ? companyIdSupplier.get() : 1L;
        if (qualityApiClient == null) return;

        UIHelper.runWithProgress(this, "A analisar validades de equipamentos...", () -> {
            return qualityApiClient.getExpiredWorkerAlerts(companyId);
        }, alerts -> {
            expiredTableModel.setRowCount(0);
            int totalWorkers = alerts.size();
            int totalItems = 0;

            for (ExpiredWorkerAlertDTO alert : alerts) {
                for (ExpiredWorkerAlertDTO.ExpiredEquipmentItemDTO item : alert.items()) {
                    totalItems++;
                    String overdueText = item.daysOverdueOrRemaining() < 0
                            ? Math.abs(item.daysOverdueOrRemaining()) + " dias atrasado"
                            : item.daysOverdueOrRemaining() + " dias restantes";

                    expiredTableModel.addRow(new Object[]{
                            item.assignmentId(),
                            alert.employeeName(),
                            alert.employeeDepartment(),
                            item.equipmentCode() + " - " + item.equipmentName(),
                            item.assignedDate() != null ? item.assignedDate().format(DATE_FMT) : "-",
                            item.expiryDate() != null ? item.expiryDate().format(DATE_FMT) : "-",
                            overdueText,
                            item.statusLabel()
                    });
                }
            }
            expiredSummaryLabel.setText(totalWorkers + " colaborador(es) com " + totalItems + " equipamento(s) expirados");
        }, err -> ToastManager.show(this, FeedbackType.ERROR, "Erro no radar: " + err.getMessage()));
    }

    private void renewSelectedFromRadar() {
        int row = expiredTable.getSelectedRow();
        if (row < 0) {
            ToastManager.show(this, FeedbackType.INFO, "Por favor selecione um registo do radar para renovar.");
            return;
        }
        int modelRow = expiredTable.convertRowIndexToModel(row);
        Long id = (Long) expiredTableModel.getValueAt(modelRow, 0);
        final Long cid = companyIdSupplier != null ? companyIdSupplier.get() : 1L;
        QualityRenewalDialog.show(this, qualityApiClient, id, cid, onDataChanged);
    }

    private void printSelectedDeliverySheet() {
        int row = expiredTable.getSelectedRow();
        if (row < 0) {
            ToastManager.show(this, FeedbackType.INFO, "Por favor selecione uma linha para imprimir o Termo de Entrega.");
            return;
        }
        int modelRow = expiredTable.convertRowIndexToModel(row);
        Long id = (Long) expiredTableModel.getValueAt(modelRow, 0);
        final Long cid = companyIdSupplier != null ? companyIdSupplier.get() : 1L;
        QualityPrintHelper.printDeliverySheet(this, qualityApiClient, id, cid);
    }

    private void exportRadarList() {
        if (printApiClient == null) {
            ToastManager.show(this, FeedbackType.WARNING, "Serviço de impressão não disponível.");
            return;
        }
        TableExportAction.export(this, printApiClient, expiredTable, "Radar de Validades e Equipamentos Expirados", "radar-equipamentos-expirados");
    }
}
