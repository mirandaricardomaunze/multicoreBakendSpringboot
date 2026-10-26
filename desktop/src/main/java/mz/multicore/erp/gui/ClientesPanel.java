package mz.multicore.erp.gui;

import mz.multicore.erp.desktop.client.AccountStatementApiClient;
import mz.multicore.erp.desktop.client.ComercialApiClient;
import mz.multicore.erp.desktop.client.CreditRiskApiClient;
import mz.multicore.erp.desktop.client.PrintApiClient;
import mz.multicore.erp.gui.commercial.ClientesOperationsCoordinator;
import mz.multicore.erp.gui.components.*;
import mz.multicore.erp.modules.comercial.dto.ClientDTO;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class ClientesPanel extends JPanel {

    private final ComercialApiClient comercialApiClient;
    private final PrintApiClient printApiClient;
    private final CreditRiskPanel creditRiskPanel;
    private final CustomerStatementPanel customerStatementPanel;
    private final ClientesOperationsCoordinator coordinator;

    private JTabbedPane tabs;
    private JTextField searchField;
    private DefaultTableModel model;
    private JTable table;
    private List<ClientDTO> allClients = new ArrayList<>();
    private List<ClientDTO> visibleClients = new ArrayList<>();
    private final InlineFeedbackPanel feedback = new InlineFeedbackPanel();

    public ClientesPanel(
            ComercialApiClient comercialApiClient,
            PrintApiClient printApiClient,
            CreditRiskApiClient creditRiskApiClient
    ) {
        this(comercialApiClient, printApiClient, creditRiskApiClient, null);
    }

    public ClientesPanel(
            ComercialApiClient comercialApiClient,
            PrintApiClient printApiClient,
            CreditRiskApiClient creditRiskApiClient,
            AccountStatementApiClient statementApiClient
    ) {
        this.comercialApiClient = comercialApiClient;
        this.printApiClient = printApiClient;
        this.coordinator = new ClientesOperationsCoordinator(comercialApiClient);
        this.creditRiskPanel = creditRiskApiClient != null ? new CreditRiskPanel(creditRiskApiClient) : null;
        this.customerStatementPanel = statementApiClient != null ? new CustomerStatementPanel(statementApiClient, comercialApiClient) : null;

        setLayout(new BorderLayout());
        setBackground(UIHelper.BG_DARK);

        tabs = new JTabbedPane();
        UIHelper.styleTabbedPaneMulticore(tabs);

        tabs.addTab("Directório de Clientes", UIHelper.icon("fas-address-book", 16, UIHelper.MODULE_CLIENTES), buildDirectoryTab());
        if (creditRiskPanel != null) {
            tabs.addTab("Risco de Crédito & Cobrança (Aging)", UIHelper.icon("fas-file-invoice-dollar", 16, UIHelper.PENDING_YELLOW), creditRiskPanel);
        }
        if (customerStatementPanel != null) {
            tabs.addTab("Conta Corrente & Reconciliação", UIHelper.icon("fas-file-invoice", 16, UIHelper.ACCENT), customerStatementPanel);
        }

        tabs.addChangeListener(e -> {
            Component sel = tabs.getSelectedComponent();
            if (sel == creditRiskPanel && creditRiskPanel != null) {
                creditRiskPanel.refreshData();
            } else if (sel == customerStatementPanel && customerStatementPanel != null) {
                customerStatementPanel.refreshData();
            }
        });

        add(tabs, BorderLayout.CENTER);
    }

    public void selectCreditRiskTab() {
        if (tabs != null && tabs.getTabCount() > 1 && creditRiskPanel != null) {
            tabs.setSelectedComponent(creditRiskPanel);
            creditRiskPanel.refreshData();
        }
    }

    public void selectStatementTab() {
        if (tabs != null && customerStatementPanel != null) {
            tabs.setSelectedComponent(customerStatementPanel);
            customerStatementPanel.refreshData();
        }
    }

    private JPanel buildDirectoryTab() {
        JPanel panel = new JPanel(new BorderLayout(0, 10));
        panel.setBackground(UIHelper.BG_DARK);
        panel.setBorder(new EmptyBorder(10, 12, 10, 12));

        ModernButton viewBtn = UIHelper.createSecondaryButton("Ver Ficha");
        viewBtn.setIcon(UIHelper.icon("fas-id-card", 14, UIHelper.ACCENT_CYAN));
        viewBtn.setPreferredSize(new Dimension(130, UIHelper.FORM_CONTROL_HEIGHT));

        ModernButton newBtn = UIHelper.createSuccessButton("Novo Cliente");
        newBtn.setIcon(UIHelper.icon("fas-user-plus", 14));
        newBtn.setPreferredSize(new Dimension(140, UIHelper.FORM_CONTROL_HEIGHT));

        ModernButton editBtn = UIHelper.createPrimaryButton("Editar");
        editBtn.setIcon(UIHelper.icon("fas-edit", 14));
        editBtn.setPreferredSize(new Dimension(120, UIHelper.FORM_CONTROL_HEIGHT));

        ModernButton deleteBtn = UIHelper.createDangerButton("Eliminar");
        deleteBtn.setIcon(UIHelper.icon("fas-trash", 14));
        deleteBtn.setPreferredSize(new Dimension(130, UIHelper.FORM_CONTROL_HEIGHT));

        ModernButton exportBtn = UIHelper.createSecondaryButton("Exportar PDF");
        exportBtn.setIcon(UIHelper.icon("fas-file-pdf", 14));
        exportBtn.setPreferredSize(new Dimension(140, UIHelper.FORM_CONTROL_HEIGHT));
        exportBtn.setToolTipText("Exportar a lista filtrada para PDF");

        ModernButton refreshBtn = UIHelper.createRefreshButton(this::onPanelSelected);

        ActionMenuButton moreBtn = UIHelper.createActionMenuButton("Mais acções")
                .addAction("Exportar PDF", UIHelper.icon("fas-file-pdf", 14, UIHelper.REJECTED_RED), exportBtn::doClick)
                .addAction("Ver Ficha", UIHelper.icon("fas-id-card", 14, UIHelper.ACCENT_CYAN), viewBtn::doClick)
                .addAction("Editar", UIHelper.icon("fas-edit", 14, UIHelper.ACCENT_BLUE), editBtn::doClick)
                .addAction("Eliminar", UIHelper.icon("fas-trash", 14, UIHelper.REJECTED_RED), deleteBtn::doClick);
        panel.add(feedback, BorderLayout.NORTH);

        ModernPanel card = new ModernPanel(16);
        card.setLayout(new BorderLayout(0, 8));
        card.setBorder(new EmptyBorder(12, 14, 12, 14));

        searchField = TableFilter.searchField("Filtrar por nome, NUIT, email ou endereço…");

        ModernButton peekToggleBtn = UIHelper.createSecondaryButton("");
        peekToggleBtn.setIcon(UIHelper.icon("fas-columns", 12));
        peekToggleBtn.setToolTipText("Espreitar dados do cliente (Espaço)");
        peekToggleBtn.getAccessibleContext().setAccessibleName("Espreitar cliente");
        peekToggleBtn.setPreferredSize(new Dimension(30, UIHelper.FORM_CONTROL_HEIGHT));

        JPanel searchRow = TableFilter.bar(searchField, peekToggleBtn);
        searchRow.setBorder(new EmptyBorder(0, 0, 4, 0));
        card.add(UIHelper.tableCardTop("Gestão de Clientes", searchRow, refreshBtn, moreBtn, newBtn), BorderLayout.NORTH);

        String[] cols = {"ID", "Nome", "NUIT / NIF", "Email", "Endereço", "Prazo (dias)", "Limite de Crédito"};
        model = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int r, int c) {
                return false;
            }
        };
        table = new JTable(model);
        UIHelper.styleTable(table);
        table.putClientProperty("noRowInspector", Boolean.TRUE);
        if (table.getColumnModel().getColumnCount() > 0) {
            table.getColumnModel().getColumn(0).setMaxWidth(60);
        }
        UIHelper.ensureHeadersFit(table);
        JScrollPane scroll = new JScrollPane(table);
        UIHelper.styleScrollPane(scroll);
        TableFilter.install(table, searchField);

        TableQuickPeekController peekController = TableQuickPeekController.install(table, card, (peek, mr) ->
                ClientesOperationsCoordinator.populatePeek(peek, model, mr, viewBtn::doClick));
        peekToggleBtn.addActionListener(e -> peekController.toggle());

        card.add(scroll, BorderLayout.CENTER);
        card.add(ClientTablePagination.install(table), BorderLayout.SOUTH);
        panel.add(card, BorderLayout.CENTER);

        // Ações
        viewBtn.addActionListener(e -> openSelectedDetail());
        newBtn.addActionListener(e -> coordinator.openClientDialog(this, null, this::onPanelSelected));
        editBtn.addActionListener(e -> {
            ClientDTO selected = selectedClient();
            if (selected != null) coordinator.openClientDialog(this, selected, this::onPanelSelected);
        });
        deleteBtn.addActionListener(e -> coordinator.deleteClient(this, selectedClient(), feedback, this::onPanelSelected));
        exportBtn.addActionListener(e ->
                TableExportAction.export(this, printApiClient, table, "Clientes", "clientes"));
        table.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent ev) {
                if (ev.getClickCount() == 2) {
                    openSelectedDetail();
                }
            }
        });

        return panel;
    }

    private void openSelectedDetail() {
        ClientDTO selected = selectedClient();
        if (selected != null) {
            coordinator.openDetail(this, selected,
                    () -> coordinator.openClientDialog(this, selected, this::onPanelSelected),
                    this::selectStatementTab);
        }
    }

    public void onPanelSelected() {
        UIHelper.loadAsync(this, comercialApiClient::getClients, clients -> {
            allClients = clients;
            refilter();
        }, error -> feedback.show(FeedbackType.ERROR, "Não foi possível carregar os clientes",
                error.getMessage(), "Tentar novamente", this::onPanelSelected));

        if (creditRiskPanel != null && tabs != null && tabs.getSelectedIndex() == 1) {
            creditRiskPanel.refreshData();
        }
    }

    private void refilter() {
        visibleClients = allClients;
        model.setRowCount(0);
        for (ClientDTO c : visibleClients) {
            model.addRow(new Object[]{
                    c.id(),
                    c.name(),
                    c.taxId(),
                    c.email(),
                    c.address() == null ? "" : c.address(),
                    ClientesOperationsCoordinator.formatTerms(c.paymentTermsDays()),
                    ClientesOperationsCoordinator.formatCreditLimit(c.creditLimit())
            });
        }
    }

    private ClientDTO selectedClient() {
        int row = TableFilter.selectedModelRow(table);
        if (row < 0) {
            feedback.show(FeedbackType.WARNING, "Seleccione um cliente",
                    "Escolha um cliente na tabela para continuar.", null, null);
            return null;
        }
        return visibleClients.get(row);
    }
}
