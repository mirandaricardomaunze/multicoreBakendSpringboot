package mz.multicore.erp.gui;

import mz.multicore.erp.gui.components.ModernButton;
import mz.multicore.erp.gui.components.ModernFormDialog;
import mz.multicore.erp.gui.components.ModernPanel;
import mz.multicore.erp.gui.components.TableQuickPeekController;
import mz.multicore.erp.gui.components.TableFilter;
import mz.multicore.erp.gui.components.TableCellRenderers;
import mz.multicore.erp.gui.components.UIHelper;
import mz.multicore.erp.gui.components.ClientTablePagination;
import mz.multicore.erp.gui.components.FeedbackType;
import mz.multicore.erp.gui.components.InlineFeedbackPanel;
import mz.multicore.erp.gui.components.KpiCard;
import mz.multicore.erp.gui.components.ToastManager;
import mz.multicore.erp.desktop.client.ApprovalApiClient;
import mz.multicore.erp.modules.approvals.dto.ApprovalRequestDTO;
import mz.multicore.erp.modules.approvals.model.ApprovalStatus;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class ApprovalsPanel extends JPanel {

    private final ApprovalApiClient approvalApiClient;
    private final mz.multicore.erp.desktop.client.StockTransferApiClient stockTransferApiClient;
    private final mz.multicore.erp.desktop.client.ComercialApiClient comercialApiClient;
    private final mz.multicore.erp.desktop.client.PrintApiClient printApiClient;

    // GUI Tables
    private DefaultTableModel pendingModel;
    private JTable pendingTable;

    private DefaultTableModel historyModel;
    private JTable historyTable;

    // KPIs Superiores
    private JLabel kpiPendingCount;
    private JLabel kpiPendingAmount;
    private JLabel kpiApprovedCount;
    private JLabel kpiRejectedCount;

    // Toolbar
    private ModernButton openBtn;
    private final InlineFeedbackPanel feedback = new InlineFeedbackPanel();

    // Data lists
    private List<ApprovalRequestDTO> pendingList = new ArrayList<>();
    private List<ApprovalRequestDTO> historyList = new ArrayList<>();

    public ApprovalsPanel(ApprovalApiClient approvalApiClient) {
        this(approvalApiClient, null, null, null);
    }

    public ApprovalsPanel(
            ApprovalApiClient approvalApiClient,
            mz.multicore.erp.desktop.client.StockTransferApiClient stockTransferApiClient,
            mz.multicore.erp.desktop.client.ComercialApiClient comercialApiClient
    ) {
        this(approvalApiClient, stockTransferApiClient, comercialApiClient, null);
    }

    public ApprovalsPanel(
            ApprovalApiClient approvalApiClient,
            mz.multicore.erp.desktop.client.StockTransferApiClient stockTransferApiClient,
            mz.multicore.erp.desktop.client.ComercialApiClient comercialApiClient,
            mz.multicore.erp.desktop.client.PrintApiClient printApiClient
    ) {
        this.approvalApiClient = approvalApiClient;
        this.stockTransferApiClient = stockTransferApiClient;
        this.comercialApiClient = comercialApiClient;
        this.printApiClient = printApiClient;

        setLayout(new BorderLayout(0, 10));
        setBackground(UIHelper.BG_DARK);
        setBorder(new EmptyBorder(10, 12, 10, 12));

        // KPIS Superiores Canónicos
        JPanel kpiGrid = KpiCard.createGrid(4);
        kpiGrid.add(KpiCard.createCard("Pedidos Pendentes", kpiPendingCount = new JLabel("0"), "Aguardam decisão", "fas-hourglass-half", UIHelper.PENDING_YELLOW));
        kpiGrid.add(KpiCard.createCard("Valor em Análise", kpiPendingAmount = new JLabel("0,00 MT"), "Montante sob validação", "fas-money-bill-wave", UIHelper.ACCENT_BLUE));
        kpiGrid.add(KpiCard.createCard("Pedidos Aprovados", kpiApprovedCount = new JLabel("0"), "Decisões favoráveis", "fas-check-circle", UIHelper.APPROVED_GREEN));
        kpiGrid.add(KpiCard.createCard("Pedidos Rejeitados", kpiRejectedCount = new JLabel("0"), "Devolvidos com motivo", "fas-times-circle", UIHelper.REJECTED_RED));

        add(kpiGrid, BorderLayout.NORTH);

        // Cada tabela na sua aba, para ganhar espaço vertical em vez de ficarem apertadas juntas.
        JTabbedPane tabbedPane = new JTabbedPane();
        UIHelper.styleTabbedPaneMulticore(tabbedPane);
        tabbedPane.addTab("Pendentes", UIHelper.icon("fas-hourglass-half", 16, UIHelper.PENDING_YELLOW),
                createPendingTab());
        tabbedPane.addTab("Histórico", UIHelper.icon("fas-clipboard-check", 16, UIHelper.APPROVED_GREEN),
                createHistoryTab());
        JPanel body = new JPanel(new BorderLayout(0, 8));
        body.setOpaque(false);
        body.add(feedback, BorderLayout.NORTH);
        body.add(tabbedPane, BorderLayout.CENTER);
        add(body, BorderLayout.CENTER);

        // LISTENERS
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

        // Carregamento preguiçoso: os dados são obtidos por HTTP em onPanelSelected() (via navigate),
        // não no construtor — uma chamada à API no arranque falharia para quem não tem empresa activa
        // (sem X-Company-Id o servidor recusa). Mesmo padrão do ClientesPanel.
    }

    public void refreshData() {
        UIHelper.loadAsync(this,
                () -> new ApprovalData(approvalApiClient.getPendingRequests(), approvalApiClient.getAllRequests()),
                this::applyData,
                error -> feedback.show(FeedbackType.ERROR, "Não foi possível carregar as aprovações",
                        error.getMessage(), "Tentar novamente", this::refreshData));
    }

    /** Aba dos pedidos pendentes (cabeçalho com acção + tabela a ocupar toda a aba). */
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
            @Override
            public boolean isCellEditable(int r, int c) { return false; }
        };
        pendingTable = new JTable(pendingModel);
        UIHelper.styleTable(pendingTable);
        UIHelper.ensureHeadersFit(pendingTable);
        pendingTable.getColumnModel().getColumn(3).setCellRenderer(TableCellRenderers.money());
        pendingTable.getColumnModel().getColumn(4).setCellRenderer(TableCellRenderers.role());
        // Duplo-clique abre o modal de decisão (não o inspector genérico do styleTable).
        pendingTable.putClientProperty("noRowInspector", Boolean.TRUE);
        JScrollPane pendingScroll = new JScrollPane(pendingTable);
        UIHelper.styleScrollPane(pendingScroll);

        TableQuickPeekController peekController = TableQuickPeekController.install(pendingTable, pendingCard, (peek, mr) -> {
            int row = pendingTable.convertRowIndexToModel(mr);
            peek.setHeaderIcon("fas-gavel", UIHelper.PENDING_YELLOW);
            peek.setTitle(String.valueOf(pendingModel.getValueAt(row, 1)));
            peek.setSubtitle("ID #" + pendingModel.getValueAt(row, 0) + " • Submetido por " + pendingModel.getValueAt(row, 2));
            peek.setStatus("PENDENTE", UIHelper.PENDING_YELLOW);
            java.util.List<mz.multicore.erp.gui.components.QuickPeekPanel.PeekItem> items = new java.util.ArrayList<>();
            items.add(new mz.multicore.erp.gui.components.QuickPeekPanel.PeekItem("Submissor", String.valueOf(pendingModel.getValueAt(row, 2)), false));
            items.add(new mz.multicore.erp.gui.components.QuickPeekPanel.PeekItem("Valor Envolvido", String.valueOf(pendingModel.getValueAt(row, 3)), true));
            items.add(new mz.multicore.erp.gui.components.QuickPeekPanel.PeekItem("Perfil Requerido", String.valueOf(pendingModel.getValueAt(row, 4)), false));
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
        pendingCard.add(UIHelper.tableCardTop("Pedidos a Aguardar Decisão", pBar,
                pRefreshBtn, openBtn), BorderLayout.NORTH);
        pendingCard.add(pendingScroll, BorderLayout.CENTER);
        pendingCard.add(ClientTablePagination.install(pendingTable), BorderLayout.SOUTH);
        pendingPanel.add(pendingCard, BorderLayout.CENTER);
        return pendingPanel;
    }

    /** Aba do histórico e auditoria (tabela ocupa toda a aba). */
    private JPanel createHistoryTab() {
        JPanel bottomPanel = new JPanel(new BorderLayout(0, 8));
        bottomPanel.setOpaque(false);
        bottomPanel.setBorder(new EmptyBorder(8, 0, 0, 0));
        ModernPanel historyCard = new ModernPanel(16);
        historyCard.setLayout(new BorderLayout(0, 8));
        historyCard.setBorder(new EmptyBorder(12, 14, 12, 14));

        String[] historyCols = {"Data", "Tipo Doc", "Submissor", "Valor", "Estado Final", "Motivo / Comentário"};
        historyModel = new DefaultTableModel(historyCols, 0) {
            @Override
            public boolean isCellEditable(int r, int c) { return false; }
        };
        historyTable = new JTable(historyModel);
        UIHelper.styleTable(historyTable);
        UIHelper.ensureHeadersFit(historyTable);
        historyTable.getColumnModel().getColumn(3).setCellRenderer(TableCellRenderers.money());
        historyTable.getColumnModel().getColumn(4).setCellRenderer(TableCellRenderers.status());
        JScrollPane historyScroll = new JScrollPane(historyTable);
        UIHelper.styleScrollPane(historyScroll);

        TableQuickPeekController hPeekController = TableQuickPeekController.install(historyTable, historyCard, (peek, mr) -> {
            int row = historyTable.convertRowIndexToModel(mr);
            peek.setHeaderIcon("fas-history", UIHelper.ACCENT_BLUE);
            peek.setTitle(String.valueOf(historyModel.getValueAt(row, 1)));
            peek.setSubtitle("Data: " + historyModel.getValueAt(row, 0));
            peek.setStatus(String.valueOf(historyModel.getValueAt(row, 4)), UIHelper.ACCENT_BLUE);
            java.util.List<mz.multicore.erp.gui.components.QuickPeekPanel.PeekItem> items = new java.util.ArrayList<>();
            items.add(new mz.multicore.erp.gui.components.QuickPeekPanel.PeekItem("Submissor", String.valueOf(historyModel.getValueAt(row, 2)), false));
            items.add(new mz.multicore.erp.gui.components.QuickPeekPanel.PeekItem("Valor", String.valueOf(historyModel.getValueAt(row, 3)), true));
            items.add(new mz.multicore.erp.gui.components.QuickPeekPanel.PeekItem("Decisão Final", String.valueOf(historyModel.getValueAt(row, 4)), false));
            items.add(new mz.multicore.erp.gui.components.QuickPeekPanel.PeekItem("Motivo / Parecer", String.valueOf(historyModel.getValueAt(row, 5)), false));
            peek.setItems(items);
        });
        JTextField hSearch = TableFilter.searchField("Documento ou submissor…");
        JComboBox<String> hEstado = TableFilter.combo("Todos os estados", "APPROVED", "REJECTED");
        JComboBox<String> hPeriodo = TableFilter.periodCombo();
        TableFilter.install(historyTable, hSearch,
                java.util.List.of(new TableFilter.ColumnFilter(hEstado, 4)),
                java.util.List.of(new TableFilter.PeriodFilter(hPeriodo, 0)));
        JPanel hBar = TableFilter.bar(hSearch, TableFilter.label("Estado final:"), hEstado,
                TableFilter.label("Data:", "fas-calendar-alt"), hPeriodo);
        hBar.setBorder(new EmptyBorder(0, 0, 4, 0));
        ModernButton hRefreshBtn = UIHelper.createRefreshButton(this::refreshData);
        historyCard.add(UIHelper.tableCardTop("Histórico e Auditoria de Aprovações", hBar, hRefreshBtn), BorderLayout.NORTH);
        historyCard.add(historyScroll, BorderLayout.CENTER);
        historyCard.add(ClientTablePagination.install(historyTable), BorderLayout.SOUTH);
        bottomPanel.add(historyCard, BorderLayout.CENTER);
        return bottomPanel;
    }

    /** Tipo de documento em PT para a área de aprovação. Desconhecidos ficam como estão. */
    private static String humanType(String documentType) {
        if (documentType == null) return "";
        return switch (documentType.toUpperCase(java.util.Locale.ROOT)) {
            case "ORDER" -> "Encomenda";
            case "INVOICE" -> "Fatura";
            case "EXPENSE", "EXPENSE_CLAIM" -> "Despesa";
            case "STOCK_TRANSFER", "TRANSFER" -> "Transferência de Stock";
            case "STOCK_WASTE", "WASTE" -> "Quebra de Stock";
            case "PURCHASE_ORDER", "PURCHASE" -> "Ordem de Compra";
            default -> documentType;
        };
    }

    private void applyData(ApprovalData data) {
        pendingModel.setRowCount(0);
        pendingList = data.pending();
        for (ApprovalRequestDTO req : pendingList) {
            pendingModel.addRow(new Object[]{
                    req.id(),
                    humanType(req.documentType()) + " #" + req.documentId(),
                    req.submitter(),
                    req.amount(),
                    req.requiredRole()
            });
        }
        historyModel.setRowCount(0);
        historyList = data.history();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
        for (ApprovalRequestDTO req : historyList) {
            if (req.status() != ApprovalStatus.PENDING) {
                historyModel.addRow(new Object[]{
                        req.createdAt().format(formatter),
                        humanType(req.documentType()) + " #" + req.documentId(),
                        req.submitter(),
                        req.amount(),
                        req.status().name(),
                        req.status() == ApprovalStatus.REJECTED ? req.rejectionReason() : "Validado administrativo"
                });
            }
        }

        // Actualizar KPIs
        if (kpiPendingCount != null) {
            kpiPendingCount.setText(String.valueOf(pendingList.size()));
        }
        if (kpiPendingAmount != null) {
            BigDecimal totalPendingAmount = pendingList.stream()
                    .map(ApprovalRequestDTO::amount)
                    .filter(Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            kpiPendingAmount.setText(String.format("%,.2f MT", totalPendingAmount));
        }
        if (kpiApprovedCount != null) {
            long approvedCount = historyList.stream()
                    .filter(r -> r.status() == ApprovalStatus.APPROVED)
                    .count();
            kpiApprovedCount.setText(String.valueOf(approvedCount));
        }
        if (kpiRejectedCount != null) {
            long rejectedCount = historyList.stream()
                    .filter(r -> r.status() == ApprovalStatus.REJECTED)
                    .count();
            kpiRejectedCount.setText(String.valueOf(rejectedCount));
        }

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
        ApprovalDetailsDialog.show(
                SwingUtilities.getWindowAncestor(this),
                pendingList.get(row),
                approvalApiClient,
                stockTransferApiClient,
                comercialApiClient,
                printApiClient,
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
