package mz.multicore.erp.gui;

import mz.multicore.erp.desktop.client.ApprovalApiClient;
import mz.multicore.erp.desktop.client.ComercialApiClient;
import mz.multicore.erp.desktop.client.PrintApiClient;
import mz.multicore.erp.desktop.client.StockTransferApiClient;
import mz.multicore.erp.gui.components.*;
import mz.multicore.erp.modules.approvals.dto.ApprovalRequestDTO;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.util.ArrayList;
import java.util.List;

/**
 * Painel Executivo da Central de Governança, Alçadas e Aprovações Documentais.
 * Gestão de pedidos pendentes com validação e histórico de auditoria administrativa.
 */
public class ApprovalsPanel extends JPanel {

    private final ApprovalApiClient approvalApiClient;
    private final StockTransferApiClient stockTransferApiClient;
    private final ComercialApiClient comercialApiClient;
    private final PrintApiClient printApiClient;

    private DefaultTableModel pendingModel;
    private JTable pendingTable;
    private ApprovalsHistoryView historyView;

    private JLabel kpiPendingCount;
    private JLabel kpiPendingAmount;
    private JLabel kpiApprovedCount;
    private JLabel kpiRejectedCount;

    private ModernButton openBtn;
    private final InlineFeedbackPanel feedback = new InlineFeedbackPanel();
    private List<ApprovalRequestDTO> pendingList = new ArrayList<>();

    public ApprovalsPanel(ApprovalApiClient approvalApiClient) {
        this(approvalApiClient, null, null, null);
    }

    public ApprovalsPanel(
            ApprovalApiClient approvalApiClient,
            StockTransferApiClient stockTransferApiClient,
            ComercialApiClient comercialApiClient
    ) {
        this(approvalApiClient, stockTransferApiClient, comercialApiClient, null);
    }

    public ApprovalsPanel(
            ApprovalApiClient approvalApiClient,
            StockTransferApiClient stockTransferApiClient,
            ComercialApiClient comercialApiClient,
            PrintApiClient printApiClient
    ) {
        this.approvalApiClient = approvalApiClient;
        this.stockTransferApiClient = stockTransferApiClient;
        this.comercialApiClient = comercialApiClient;
        this.printApiClient = printApiClient;

        setLayout(new BorderLayout(0, 10));
        setBackground(UIHelper.BG_DARK);
        setBorder(new EmptyBorder(10, 12, 10, 12));

        JPanel kpiGrid = KpiCard.createGrid(4);
        kpiGrid.add(KpiCard.createCard("Pedidos Pendentes", kpiPendingCount = new JLabel("0"), "Aguardam decisão", "fas-hourglass-half", UIHelper.PENDING_YELLOW));
        kpiGrid.add(KpiCard.createCard("Valor em Análise", kpiPendingAmount = new JLabel("0,00 MT"), "Montante sob validação", "fas-money-bill-wave", UIHelper.ACCENT_BLUE));
        kpiGrid.add(KpiCard.createCard("Pedidos Aprovados", kpiApprovedCount = new JLabel("0"), "Decisões favoráveis", "fas-check-circle", UIHelper.APPROVED_GREEN));
        kpiGrid.add(KpiCard.createCard("Pedidos Rejeitados", kpiRejectedCount = new JLabel("0"), "Devolvidos com motivo", "fas-times-circle", UIHelper.REJECTED_RED));
        add(kpiGrid, BorderLayout.NORTH);

        historyView = new ApprovalsHistoryView(this::refreshData);

        JTabbedPane tabbedPane = new JTabbedPane();
        UIHelper.styleTabbedPaneMulticore(tabbedPane);
        tabbedPane.addTab("Pendentes", UIHelper.icon("fas-hourglass-half", 16, UIHelper.PENDING_YELLOW), createPendingTab());
        tabbedPane.addTab("Histórico", UIHelper.icon("fas-clipboard-check", 16, UIHelper.APPROVED_GREEN), historyView);

        JPanel body = new JPanel(new BorderLayout(0, 8));
        body.setOpaque(false);
        body.add(feedback, BorderLayout.NORTH);
        body.add(tabbedPane, BorderLayout.CENTER);
        add(body, BorderLayout.CENTER);

        pendingTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                boolean hasSel = pendingTable.getSelectedRow() >= 0;
                openBtn.setEnabled(hasSel);
                openBtn.setToolTipText(hasSel ? "Decidir pedido selecionado" : "Selecione um pedido pendente para decidir");
            }
        });
        pendingTable.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override public void mouseClicked(java.awt.event.MouseEvent ev) {
                if (ev.getClickCount() == 2) openDecisionForSelected();
            }
        });
    }

    public void refreshData() {
        UIHelper.loadAsync(this,
                () -> new ApprovalData(approvalApiClient.getPendingRequests(), approvalApiClient.getAllRequests()),
                this::applyData,
                error -> feedback.show(FeedbackType.ERROR, "Não foi possível carregar as aprovações",
                        error.getMessage(), "Tentar novamente", this::refreshData));
    }

    private JPanel createPendingTab() {
        JPanel pendingPanel = new JPanel(new BorderLayout(0, 8));
        pendingPanel.setOpaque(false);
        pendingPanel.setBorder(new EmptyBorder(8, 0, 0, 0));

        openBtn = UIHelper.createPrimaryButton("Abrir / Decidir");
        openBtn.setIcon(UIHelper.icon("fas-gavel", 14));
        openBtn.setEnabled(false);
        openBtn.setToolTipText("Selecione um pedido pendente para decidir");
        openBtn.addActionListener(e -> openDecisionForSelected());

        ModernPanel pendingCard = new ModernPanel(16);
        pendingCard.setLayout(new BorderLayout(0, 10));
        pendingCard.setBorder(new EmptyBorder(12, 14, 12, 14));

        String[] pendingCols = {"ID", "Documento", "Submissor", "Valor", "Perfil Requerido"};
        pendingModel = new DefaultTableModel(pendingCols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        pendingTable = new JTable(pendingModel);
        UIHelper.styleTable(pendingTable);
        UIHelper.ensureHeadersFit(pendingTable);
        pendingTable.getColumnModel().getColumn(3).setCellRenderer(TableCellRenderers.money());
        pendingTable.getColumnModel().getColumn(4).setCellRenderer(TableCellRenderers.role());
        pendingTable.putClientProperty("noRowInspector", Boolean.TRUE);

        JScrollPane pendingScroll = new JScrollPane(pendingTable);
        UIHelper.styleScrollPane(pendingScroll);

        TableQuickPeekController.install(pendingTable, pendingCard, (peek, mr) -> {
            int row = pendingTable.convertRowIndexToModel(mr);
            peek.setHeaderIcon("fas-gavel", UIHelper.PENDING_YELLOW);
            peek.setTitle(String.valueOf(pendingModel.getValueAt(row, 1)));
            peek.setSubtitle("ID #" + pendingModel.getValueAt(row, 0) + " • Submetido por " + pendingModel.getValueAt(row, 2));
            peek.setStatus("PENDENTE", UIHelper.PENDING_YELLOW);
            List<QuickPeekPanel.PeekItem> items = new ArrayList<>();
            items.add(new QuickPeekPanel.PeekItem("Submissor", String.valueOf(pendingModel.getValueAt(row, 2)), false));
            items.add(new QuickPeekPanel.PeekItem("Valor Envolvido", String.valueOf(pendingModel.getValueAt(row, 3)), true));
            items.add(new QuickPeekPanel.PeekItem("Perfil Requerido", String.valueOf(pendingModel.getValueAt(row, 4)), false));
            peek.setItems(items);
            peek.getOpenFullButton().setText("Decidir Pedido");
            peek.setOnOpenFullAction(v -> openDecisionForSelected());
        });

        JTextField pSearch = TableFilter.searchField("Documento ou submissor…");
        JComboBox<String> pPerfil = TableFilter.combo("Todos os perfis", "ADMIN", "MANAGER");
        UIHelper.humanizeRoleCombo(pPerfil);
        TableFilter.install(pendingTable, pSearch, new TableFilter.ColumnFilter(pPerfil, 4));
        JPanel pBar = TableFilter.bar(pSearch, TableFilter.label("Perfil:"), pPerfil);
        pBar.setBorder(new EmptyBorder(0, 0, 4, 0));

        ModernButton pRefreshBtn = UIHelper.createRefreshButton(this::refreshData);
        pendingCard.setLayout(new BorderLayout(0, 8));
        pendingCard.add(UIHelper.tableCardTop("Pedidos a Aguardar Decisão", pBar, pRefreshBtn, openBtn), BorderLayout.NORTH);
        pendingCard.add(pendingScroll, BorderLayout.CENTER);
        pendingCard.add(ClientTablePagination.install(pendingTable), BorderLayout.SOUTH);
        pendingPanel.add(pendingCard, BorderLayout.CENTER);

        return pendingPanel;
    }

    private void applyData(ApprovalData data) {
        pendingModel.setRowCount(0);
        pendingList = data.pending();
        for (ApprovalRequestDTO req : pendingList) {
            String docLabel = ApprovalsOperationsCoordinator.humanType(req.documentType()) + " #" + req.documentId();
            pendingModel.addRow(new Object[]{
                    req.id(), docLabel, req.submitter(), req.amount(), req.requiredRole()
            });
        }

        if (historyView != null) {
            historyView.populate(data.history());
        }

        ApprovalsOperationsCoordinator.updateKpis(
                pendingList, data.history(),
                kpiPendingCount, kpiPendingAmount, kpiApprovedCount, kpiRejectedCount
        );

        if (openBtn != null) {
            openBtn.setEnabled(false);
            openBtn.setToolTipText("Selecione um pedido pendente para decidir");
            pendingTable.clearSelection();
        }
    }

    private record ApprovalData(List<ApprovalRequestDTO> pending, List<ApprovalRequestDTO> history) {}

    private void openDecisionForSelected() {
        int row = TableFilter.selectedModelRow(pendingTable);
        if (row < 0 || row >= pendingList.size()) return;
        ApprovalsOperationsCoordinator.openDecision(
                this, pendingList.get(row),
                approvalApiClient, stockTransferApiClient, comercialApiClient, printApiClient,
                () -> {
                    ToastManager.success(this, "Decisão de aprovação processada.");
                    refreshData();
                }
        );
    }

    public void onPanelSelected() {
        refreshData();
    }
}
