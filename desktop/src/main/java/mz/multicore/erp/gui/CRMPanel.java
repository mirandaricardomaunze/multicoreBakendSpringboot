package mz.multicore.erp.gui;

import mz.multicore.erp.gui.components.ModernButton;
import mz.multicore.erp.gui.components.ModernPanel;
import mz.multicore.erp.gui.components.TableFilter;
import mz.multicore.erp.gui.components.TableCellRenderers;
import mz.multicore.erp.gui.components.UIHelper;
import mz.multicore.erp.gui.components.ActionMenuButton;
import mz.multicore.erp.gui.components.FeedbackType;
import mz.multicore.erp.gui.components.InlineFeedbackPanel;
import mz.multicore.erp.gui.components.ToastManager;
import mz.multicore.erp.desktop.client.CRMApiClient;
import mz.multicore.erp.desktop.client.ComercialApiClient;
import mz.multicore.erp.modules.crm.dto.*;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * CRM & Assistência: pedidos dos clientes da loja e folhas de obra dos técnicos.
 *
 * <p>O painel trata do <b>layout e dos dados</b>; os diálogos e as regras de cada acção vivem em
 * {@link CrmTicketActions} e {@link CrmWorkSheetActions}, no mesmo molde do
 * {@code StockTransferActions}/{@code PosCashSessionActions}. Antes a aba dos pedidos não tinha
 * uma única acção — nem sequer era possível abrir um pedido de assistência a partir da aplicação.
 */
public class CRMPanel extends JPanel {

    static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    final CRMApiClient crmApiClient;
    private final CrmTicketActions ticketActions;
    private final CrmWorkSheetActions workSheetActions;
    private final InlineFeedbackPanel feedback = new InlineFeedbackPanel();

    private DefaultTableModel ticketsModel;
    JTable ticketsTable;

    private DefaultTableModel worksheetsModel;
    JTable worksheetsTable;

    /** Pedidos e folhas na ordem do modelo — as acções resolvem a selecção por aqui. */
    List<SupportTicketDTO> ticketsList = new ArrayList<>();
    List<WorkSheetDTO> worksheetsList = new ArrayList<>();

    public CRMPanel(CRMApiClient crmApiClient, ComercialApiClient comercialApiClient) {
        this.crmApiClient = crmApiClient;
        this.ticketActions = new CrmTicketActions(this, crmApiClient, comercialApiClient);
        this.workSheetActions = new CrmWorkSheetActions(this, crmApiClient);

        setLayout(new BorderLayout(0, 15));
        setBackground(UIHelper.BG_DARK);
        setBorder(new EmptyBorder(25, 25, 25, 25));

        JPanel north = new JPanel();
        north.setOpaque(false);
        north.setLayout(new BoxLayout(north, BoxLayout.Y_AXIS));
        JComponent heading = UIHelper.createHeading("CRM & Assistência");
        heading.setAlignmentX(Component.LEFT_ALIGNMENT);
        feedback.setAlignmentX(Component.LEFT_ALIGNMENT);
        north.add(heading);
        north.add(feedback);
        add(north, BorderLayout.NORTH);

        // Cada tabela na sua aba, para ganhar espaço vertical em vez de ficarem apertadas juntas.
        JTabbedPane tabbedPane = new JTabbedPane();
        UIHelper.styleTabbedPaneMulticore(tabbedPane);
        tabbedPane.addTab("Pedidos de Assistência", UIHelper.icon("fas-headset", 16, UIHelper.MODULE_CRM),
                createTicketsTab());
        tabbedPane.addTab("Folhas de Obra", UIHelper.icon("fas-tools", 16, UIHelper.ACCENT_ORANGE),
                createWorkSheetsTab());
        add(tabbedPane, BorderLayout.CENTER);

        // Carregamento preguiçoso: os dados vêm por HTTP em onPanelSelected() (via navigate), não no
        // construtor — evita uma chamada à API no arranque para quem não tem empresa activa. Mesmo
        // padrão do ClientesPanel/ApprovalsPanel.
    }

    /** Aba dos pedidos de assistência: acções no topo, tabela por baixo. */
    private JPanel createTicketsTab() {
        JPanel panel = new JPanel(new BorderLayout(0, 10));
        panel.setOpaque(false);
        panel.setBorder(new EmptyBorder(15, 0, 0, 0));

        ModernButton newTicketBtn = UIHelper.createSuccessButton("Novo Pedido");
        newTicketBtn.setIcon(UIHelper.icon("fas-headset", 14));
        newTicketBtn.setToolTipText("Abrir um pedido de assistência para um cliente");
        newTicketBtn.addActionListener(e -> ticketActions.createTicket());

        ModernButton detailBtn = UIHelper.createPrimaryButton("Abrir Pedido");
        detailBtn.setIcon(UIHelper.icon("fas-folder-open", 14));
        detailBtn.setToolTipText("Ver o pedido, atribuir técnico, resolver, anular ou reabrir");
        detailBtn.addActionListener(e -> ticketActions.openSelectedTicket());

        ModernButton refreshBtn = UIHelper.createSecondaryButton("Actualizar");
        refreshBtn.setIcon(UIHelper.icon("fas-sync-alt", 14));
        refreshBtn.addActionListener(e -> refreshData());

        ModernPanel ticketsCard = new ModernPanel(16);
        ticketsCard.setLayout(new BorderLayout());
        ticketsCard.setBorder(new EmptyBorder(15, 15, 15, 15));

        String[] ticketCols = {"ID", "Data", "Cliente", "Assunto", "Prioridade", "Técnico", "Estado"};
        ticketsModel = new DefaultTableModel(ticketCols, 0) {
            @Override
            public boolean isCellEditable(int r, int c) { return false; }
        };
        ticketsTable = new JTable(ticketsModel);
        UIHelper.styleTable(ticketsTable);
        ticketsTable.putClientProperty("noRowInspector", Boolean.TRUE);
        ticketsTable.getColumnModel().getColumn(6).setCellRenderer(TableCellRenderers.status());
        ticketsTable.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override public void mouseClicked(java.awt.event.MouseEvent ev) {
                if (ev.getClickCount() == 2) ticketActions.openSelectedTicket();
            }
        });
        JScrollPane tScroll = new JScrollPane(ticketsTable);
        UIHelper.styleScrollPane(tScroll);

        JTextField tSearch = TableFilter.searchField("Cliente, assunto ou técnico…");
        JComboBox<String> tEstado = TableFilter.combo("Todos os estados",
                "Aberto", "Em curso", "Resolvido", "Anulado");
        JComboBox<String> tPrioridade = TableFilter.combo("Todas as prioridades",
                "Urgente", "Alta", "Normal", "Baixa");
        JComboBox<String> tPeriodo = TableFilter.periodCombo();
        TableFilter.install(ticketsTable, tSearch,
                java.util.List.of(new TableFilter.ColumnFilter(tEstado, 6),
                        new TableFilter.ColumnFilter(tPrioridade, 4)),
                java.util.List.of(new TableFilter.PeriodFilter(tPeriodo, 1)));
        JPanel tBar = TableFilter.bar(tSearch, TableFilter.label("Estado:"), tEstado,
                TableFilter.label("Prioridade:", "fas-flag"), tPrioridade,
                TableFilter.label("Data:", "fas-calendar-alt"), tPeriodo);
        tBar.setBorder(new EmptyBorder(0, 0, 10, 0));
        ticketsCard.add(UIHelper.tableCardTop("Pedidos dos Clientes", tBar,
                refreshBtn, detailBtn, newTicketBtn), BorderLayout.NORTH);
        ticketsCard.add(tScroll, BorderLayout.CENTER);
        panel.add(ticketsCard, BorderLayout.CENTER);
        return panel;
    }

    /** Aba das folhas de obra (cabeçalho com acções + tabela). */
    private JPanel createWorkSheetsTab() {
        JPanel wsPanel = new JPanel(new BorderLayout(0, 10));
        wsPanel.setOpaque(false);
        wsPanel.setBorder(new EmptyBorder(15, 0, 0, 0));

        ModernButton newWsBtn = UIHelper.createSuccessButton("Registar Folha de Obra");
        newWsBtn.setIcon(UIHelper.icon("fas-tools", 14));
        newWsBtn.addActionListener(e -> workSheetActions.registerWorkSheet());

        ModernButton billBtn = UIHelper.createPrimaryButton("Faturar Folha de Obra");
        billBtn.setIcon(UIHelper.icon("fas-file-invoice-dollar", 14));
        billBtn.addActionListener(e -> workSheetActions.billWorkSheet());

        ActionMenuButton moreBtn = UIHelper.createActionMenuButton("Ações da Folha")
                .addAction("Imprimir PDF", UIHelper.icon("fas-print", 14), workSheetActions::printWorkSheet)
                .addAction("Corrigir", UIHelper.icon("fas-edit", 14), workSheetActions::editWorkSheet)
                .addAction("Anular", UIHelper.icon("fas-ban", 14), workSheetActions::voidWorkSheet)
                .addAction("Tarifa/hora", UIHelper.icon("fas-money-bill-wave", 14), workSheetActions::editHourlyRate);

        ModernPanel wsCard = new ModernPanel(16);
        wsCard.setLayout(new BorderLayout());
        wsCard.setBorder(new EmptyBorder(15, 15, 15, 15));

        String[] wsCols = {"ID", "Data", "Cliente", "Técnico", "Horas", "Tarifa/h", "Total", "Estado"};
        worksheetsModel = new DefaultTableModel(wsCols, 0) {
            @Override
            public boolean isCellEditable(int r, int c) { return false; }
        };
        worksheetsTable = new JTable(worksheetsModel);
        UIHelper.styleTable(worksheetsTable);
        worksheetsTable.putClientProperty("noRowInspector", Boolean.TRUE);
        worksheetsTable.getColumnModel().getColumn(5).setCellRenderer(TableCellRenderers.money());
        worksheetsTable.getColumnModel().getColumn(6).setCellRenderer(TableCellRenderers.money());
        worksheetsTable.getColumnModel().getColumn(7).setCellRenderer(TableCellRenderers.status());
        worksheetsTable.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override public void mouseClicked(java.awt.event.MouseEvent ev) {
                if (ev.getClickCount() == 2) workSheetActions.printWorkSheet();
            }
        });
        JScrollPane wsScroll = new JScrollPane(worksheetsTable);
        UIHelper.styleScrollPane(wsScroll);

        JTextField wsSearch = TableFilter.searchField("Cliente ou técnico…");
        JComboBox<String> wsEstado = TableFilter.combo("Todos os estados",
                "Por faturar", "Faturada", "Anulada");
        JComboBox<String> wsPeriodo = TableFilter.periodCombo();
        TableFilter.install(worksheetsTable, wsSearch,
                java.util.List.of(new TableFilter.ColumnFilter(wsEstado, 7)),
                java.util.List.of(new TableFilter.PeriodFilter(wsPeriodo, 1)));
        JPanel wsBar = TableFilter.bar(wsSearch, TableFilter.label("Estado:"), wsEstado,
                TableFilter.label("Data:", "fas-calendar-alt"), wsPeriodo);
        wsBar.setBorder(new EmptyBorder(0, 0, 10, 0));
        ModernButton wsRefreshBtn = UIHelper.createRefreshButton(this::refreshData);
        wsCard.add(UIHelper.tableCardTop("Folhas de Obra Registadas", wsBar,
                wsRefreshBtn, moreBtn, billBtn, newWsBtn), BorderLayout.NORTH);
        wsCard.add(wsScroll, BorderLayout.CENTER);
        wsPanel.add(wsCard, BorderLayout.CENTER);
        return wsPanel;
    }

    public void refreshData() {
        UIHelper.loadAsync(this,
                () -> new CRMData(crmApiClient.getAllTickets(), crmApiClient.getAllWorkSheets()),
                this::applyData,
                error -> showNotice(FeedbackType.ERROR, "Não foi possível carregar CRM e assistência",
                        error.getMessage(), "Tentar novamente", this::refreshData));
    }

    private void applyData(CRMData data) {
        ticketsList = data.tickets();
        ticketsModel.setRowCount(0);
        for (SupportTicketDTO ticket : ticketsList) {
            ticketsModel.addRow(new Object[]{
                    ticket.id(),
                    ticket.createdAt().format(DATE_FMT),
                    ticket.clientName(),
                    ticket.subject(),
                    ticket.priorityLabel(),
                    ticket.assignedTechnician() == null ? "—" : ticket.assignedTechnician(),
                    ticket.statusLabel()
            });
        }

        worksheetsList = data.worksheets();
        worksheetsModel.setRowCount(0);
        for (WorkSheetDTO ws : worksheetsList) {
            worksheetsModel.addRow(new Object[]{
                    ws.id(),
                    ws.createdAt().format(DATE_FMT),
                    ws.clientName(),
                    ws.technicianName(),
                    ws.hoursWorked() + " h",
                    ws.hourlyRate(),
                    ws.totalValue(),
                    ws.statusLabel()
            });
        }
    }

    /** Pedidos que ainda aceitam trabalho — o combo do registo de folha só mostra estes. */
    List<SupportTicketDTO> openTickets() {
        List<SupportTicketDTO> open = new ArrayList<>();
        for (SupportTicketDTO ticket : ticketsList) {
            if ("OPEN".equals(ticket.status()) || "IN_PROGRESS".equals(ticket.status())) {
                open.add(ticket);
            }
        }
        return open;
    }

    void showNotice(FeedbackType type, String title, String message) {
        showNotice(type, title, message, null, null);
    }

    void showNotice(FeedbackType type, String title, String message, String actionLabel, Runnable action) {
        feedback.show(type, title, message, actionLabel, action);
    }

    void showSuccess(String message) {
        ToastManager.success(this, message);
    }

    private record CRMData(List<SupportTicketDTO> tickets, List<WorkSheetDTO> worksheets) {}

    public void onPanelSelected() {
        refreshData();
    }
}
