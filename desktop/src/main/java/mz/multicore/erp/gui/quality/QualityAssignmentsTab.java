package mz.multicore.erp.gui.quality;

import mz.multicore.erp.desktop.client.HRApiClient;
import mz.multicore.erp.desktop.client.PrintApiClient;
import mz.multicore.erp.desktop.client.QualityApiClient;
import mz.multicore.erp.gui.QualityAssignmentDialog;
import mz.multicore.erp.gui.components.*;
import mz.multicore.erp.modules.quality.dto.AssignmentStatus;
import mz.multicore.erp.modules.quality.dto.WorkerEquipmentAssignmentDTO;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.time.format.DateTimeFormatter;
import java.util.function.Supplier;

public class QualityAssignmentsTab extends JPanel {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final QualityApiClient qualityApiClient;
    private final HRApiClient hrApiClient;
    private final PrintApiClient printApiClient;
    private final Supplier<Long> companyIdSupplier;
    private final Runnable onDataChanged;

    private DefaultTableModel assignmentsTableModel;
    private JTable assignmentsTable;
    private TablePager assignmentsPager;
    private final JTextField assignmentsSearchField = new JTextField(15);
    private final JComboBox<String> assignmentsStatusFilter = new JComboBox<>(new String[]{
            "Todos os estados", "Válidos", "A Expirar", "Expirados", "Renovados", "Devolvidos"
    });

    public QualityAssignmentsTab(QualityApiClient qualityApiClient, HRApiClient hrApiClient,
                                 PrintApiClient printApiClient, Supplier<Long> companyIdSupplier,
                                 Runnable onDataChanged) {
        super(new BorderLayout());
        this.qualityApiClient = qualityApiClient;
        this.hrApiClient = hrApiClient;
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

        String[] cols = {"ID", "Colaborador", "Departamento", "Código", "Equipamento", "Qtd", "Entrega", "Validade", "Estado"};
        assignmentsTableModel = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int r, int c) { return false; }
        };
        assignmentsTable = new JTable(assignmentsTableModel);
        assignmentsTable.putClientProperty(ClientTablePagination.DISABLED, Boolean.TRUE);
        UIHelper.styleTable(assignmentsTable);
        UIHelper.ensureHeadersFit(assignmentsTable);

        assignmentsTable.getColumnModel().getColumn(0).setMinWidth(0);
        assignmentsTable.getColumnModel().getColumn(0).setMaxWidth(0);
        assignmentsTable.getColumnModel().getColumn(0).setPreferredWidth(0);

        assignmentsTable.getColumnModel().getColumn(8).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object val, boolean isSel, boolean hasFocus, int r, int c) {
                super.getTableCellRendererComponent(table, val, isSel, hasFocus, r, c);
                setHorizontalAlignment(SwingConstants.CENTER);
                String st = val != null ? val.toString() : "";
                if ("Expirado".equals(st)) {
                    setForeground(UIHelper.REJECTED_RED);
                    setFont(getFont().deriveFont(Font.BOLD));
                } else if ("A Expirar".equals(st)) {
                    setForeground(UIHelper.PENDING_YELLOW);
                    setFont(getFont().deriveFont(Font.BOLD));
                } else if ("Válido".equals(st)) {
                    setForeground(UIHelper.APPROVED_GREEN);
                    setFont(getFont().deriveFont(Font.BOLD));
                } else {
                    setForeground(UIHelper.TEXT_MUTED);
                }
                return this;
            }
        });

        UIHelper.styleTextField(assignmentsSearchField);
        assignmentsSearchField.putClientProperty("JTextField.placeholderText", "Pesquisar colaborador / equipamento...");
        assignmentsSearchField.setPreferredSize(new Dimension(180, UIHelper.FORM_CONTROL_HEIGHT));
        assignmentsSearchField.addActionListener(e -> loadPage(0, 25));

        UIHelper.styleComboBox(assignmentsStatusFilter);
        assignmentsStatusFilter.setPreferredSize(new Dimension(150, UIHelper.FORM_CONTROL_HEIGHT));
        assignmentsStatusFilter.addActionListener(e -> loadPage(0, 25));

        JPanel filterBar = TableFilter.bar(assignmentsSearchField, assignmentsStatusFilter);

        ModernButton refreshBtn = UIHelper.createRefreshButton(this::refresh);
        ModernButton newAssignBtn = UIHelper.createPrimaryButton("+ Atribuir Equipamento");
        newAssignBtn.addActionListener(e -> openNewAssignmentDialog());

        ActionMenuButton actionsMenu = UIHelper.createActionMenuButton("Gerir Entrega");
        actionsMenu.addAction("Imprimir Termo de Entrega", UIHelper.icon("fas-print", 13), this::printSelectedDeliverySheet);
        actionsMenu.addAction("Renovar Equipamento", UIHelper.icon("fas-sync", 13), this::renewSelectedAssignment);
        actionsMenu.addAction("Devolver Equipamento", UIHelper.icon("fas-undo", 13), this::returnSelectedAssignment);
        actionsMenu.addAction("Exportar Lista (PDF)", UIHelper.icon("fas-file-pdf", 13), this::exportAssignmentsList);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        actions.setOpaque(false);
        actions.add(refreshBtn);
        actions.add(newAssignBtn);
        actions.add(actionsMenu);

        card.add(UIHelper.tableCardTop("Atribuições Registadas", actions, filterBar), BorderLayout.NORTH);
        card.add(new JScrollPane(assignmentsTable), BorderLayout.CENTER);

        assignmentsPager = new TablePager(this::loadPage);
        card.add(assignmentsPager, BorderLayout.SOUTH);

        add(card, BorderLayout.CENTER);
    }

    public void refresh() {
        loadPage(0, 25);
    }

    public void loadPage(int page, int size) {
        final Long companyId = companyIdSupplier != null ? companyIdSupplier.get() : 1L;
        if (qualityApiClient == null) return;

        String search = assignmentsSearchField.getText().trim();
        int stIdx = assignmentsStatusFilter.getSelectedIndex();
        AssignmentStatus status = switch (stIdx) {
            case 1 -> AssignmentStatus.ACTIVE;
            case 2 -> AssignmentStatus.EXPIRING_SOON;
            case 3 -> AssignmentStatus.EXPIRED;
            case 4 -> AssignmentStatus.RENEWED;
            case 5 -> AssignmentStatus.RETURNED;
            default -> null;
        };

        UIHelper.runWithProgress(this, "A carregar atribuições...", () -> {
            return qualityApiClient.searchAssignments(companyId, status, null, search, page, size);
        }, resp -> {
            assignmentsTableModel.setRowCount(0);
            for (WorkerEquipmentAssignmentDTO a : resp.items()) {
                assignmentsTableModel.addRow(new Object[]{
                        a.id(),
                        a.employeeName(),
                        a.employeeDepartment(),
                        a.equipmentCode(),
                        a.equipmentName(),
                        a.quantity(),
                        a.assignedDate() != null ? a.assignedDate().format(DATE_FMT) : "-",
                        a.expiryDate() != null ? a.expiryDate().format(DATE_FMT) : "-",
                        a.statusLabel()
                });
            }
            if (assignmentsPager != null) {
                assignmentsPager.apply(resp);
            }
        }, err -> ToastManager.show(this, FeedbackType.ERROR, "Erro ao carregar atribuições: " + err.getMessage()));
    }

    private void openNewAssignmentDialog() {
        Window win = SwingUtilities.getWindowAncestor(this);
        QualityAssignmentDialog dlg = new QualityAssignmentDialog(win, qualityApiClient, hrApiClient, onDataChanged);
        dlg.setVisible(true);
    }

    private void renewSelectedAssignment() {
        int row = assignmentsTable.getSelectedRow();
        if (row < 0) {
            ToastManager.show(this, FeedbackType.INFO, "Por favor selecione uma atribuição para renovar.");
            return;
        }
        int modelRow = assignmentsTable.convertRowIndexToModel(row);
        Long id = (Long) assignmentsTableModel.getValueAt(modelRow, 0);
        final Long cid = companyIdSupplier != null ? companyIdSupplier.get() : 1L;
        QualityRenewalDialog.show(this, qualityApiClient, id, cid, onDataChanged);
    }

    private void printSelectedDeliverySheet() {
        int row = assignmentsTable.getSelectedRow();
        if (row < 0) {
            ToastManager.show(this, FeedbackType.INFO, "Por favor selecione uma linha para imprimir o Termo de Entrega.");
            return;
        }
        int modelRow = assignmentsTable.convertRowIndexToModel(row);
        Long id = (Long) assignmentsTableModel.getValueAt(modelRow, 0);
        final Long cid = companyIdSupplier != null ? companyIdSupplier.get() : 1L;
        QualityPrintHelper.printDeliverySheet(this, qualityApiClient, id, cid);
    }

    private void exportAssignmentsList() {
        if (printApiClient == null) {
            ToastManager.show(this, FeedbackType.WARNING, "Serviço de impressão não disponível.");
            return;
        }
        TableExportAction.export(this, printApiClient, assignmentsTable, "Atribuições de Equipamentos e EPIs", "atribuicoes-equipamentos");
    }

    private void returnSelectedAssignment() {
        int row = assignmentsTable.getSelectedRow();
        if (row < 0) {
            ToastManager.show(this, FeedbackType.INFO, "Por favor selecione uma atribuição para devolver.");
            return;
        }
        int modelRow = assignmentsTable.convertRowIndexToModel(row);
        Long id = (Long) assignmentsTableModel.getValueAt(modelRow, 0);
        final Long cid = companyIdSupplier != null ? companyIdSupplier.get() : 1L;

        UIHelper.runWithProgress(this, "A registar devolução...", () -> {
            qualityApiClient.returnAssignment(id, cid, "Devolvido ao stock");
            return null;
        }, done -> {
            ToastManager.show(this, FeedbackType.SUCCESS, "Equipamento Devolvido ao stock com sucesso.");
            if (onDataChanged != null) onDataChanged.run();
        }, err -> ToastManager.show(this, FeedbackType.ERROR, "Erro ao devolver: " + err.getMessage()));
    }
}
