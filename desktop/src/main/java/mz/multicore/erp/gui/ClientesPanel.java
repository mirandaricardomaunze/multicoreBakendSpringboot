package mz.multicore.erp.gui;

import mz.multicore.erp.desktop.client.AccountStatementApiClient;
import mz.multicore.erp.desktop.client.ComercialApiClient;
import mz.multicore.erp.desktop.client.CreditRiskApiClient;
import mz.multicore.erp.desktop.client.PrintApiClient;
import mz.multicore.erp.gui.components.FeedbackType;
import mz.multicore.erp.gui.components.ActionMenuButton;
import mz.multicore.erp.gui.components.ClientTablePagination;
import mz.multicore.erp.gui.components.FormField;
import mz.multicore.erp.gui.components.InlineFeedbackPanel;
import mz.multicore.erp.gui.components.IntegerField;
import mz.multicore.erp.gui.components.ModernButton;
import mz.multicore.erp.gui.components.ModernFormDialog;
import mz.multicore.erp.gui.components.ModernPanel;
import mz.multicore.erp.gui.components.MoneyField;
import mz.multicore.erp.gui.components.NuitValidator;
import mz.multicore.erp.gui.components.TableExportAction;
import mz.multicore.erp.gui.components.TableFilter;
import mz.multicore.erp.gui.components.ToastManager;
import mz.multicore.erp.gui.components.UIHelper;
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
        JPanel panel = new JPanel(new BorderLayout(0, 15));
        panel.setBackground(UIHelper.BG_DARK);
        panel.setBorder(new EmptyBorder(20, 20, 20, 20));

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
        card.setLayout(new BorderLayout());
        card.setBorder(new EmptyBorder(15, 15, 15, 15));

        searchField = TableFilter.searchField("Filtrar por nome, NUIT, email ou endereço…");
        JPanel searchRow = new JPanel(new GridBagLayout());
        searchRow.setOpaque(false);
        GridBagConstraints g = new GridBagConstraints();
        g.gridy = 0;
        g.fill = GridBagConstraints.HORIZONTAL;
        g.gridx = 0; g.weightx = 1.0;
        searchRow.add(filterLabel("Pesquisa"), g);
        g.gridy = 1;
        g.insets = new Insets(4, 0, 0, 0);
        searchRow.add(searchField, g);
        searchRow.setBorder(new EmptyBorder(0, 0, 12, 0));
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
        JScrollPane scroll = new JScrollPane(table);
        UIHelper.styleScrollPane(scroll);
        TableFilter.install(table, searchField);
        card.add(scroll, BorderLayout.CENTER);
        card.add(ClientTablePagination.install(table), BorderLayout.SOUTH);
        panel.add(card, BorderLayout.CENTER);

        // LISTENERS
        viewBtn.addActionListener(e -> {
            ClientDTO selected = selectedClient();
            if (selected != null) openClientDetailDialog(selected);
        });
        newBtn.addActionListener(e -> openClientDialog(null));
        editBtn.addActionListener(e -> {
            ClientDTO selected = selectedClient();
            if (selected != null) openClientDialog(selected);
        });
        deleteBtn.addActionListener(e -> deleteSelected());
        exportBtn.addActionListener(e ->
                TableExportAction.export(this, printApiClient, table, "Clientes", "clientes"));
        table.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent ev) {
                if (ev.getClickCount() == 2) {
                    ClientDTO selected = selectedClient();
                    if (selected != null) openClientDetailDialog(selected);
                }
            }
        });

        return panel;
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
                    c.paymentTermsDays() == 0 ? "Pronto pagamento" : c.paymentTermsDays(),
                    c.creditLimit() == null ? "Sem limite" : String.format("%,.2f MT", c.creditLimit())
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

    private void openClientDialog(ClientDTO existing) {
        JTextField nameField = new JTextField(existing == null ? "" : existing.name());
        JTextField taxIdField = new JTextField(existing == null ? "" : existing.taxId());
        JTextField emailField = new JTextField(existing == null ? "" : existing.email());
        JTextField addressField = new JTextField(existing == null || existing.address() == null ? "" : existing.address());
        IntegerField termsField = new IntegerField(
                String.valueOf(existing == null ? 0 : existing.paymentTermsDays()), 0, 365, "O prazo de pagamento");
        MoneyField creditField = new MoneyField(existing == null || existing.creditLimit() == null
                ? "" : existing.creditLimit().toPlainString());
        UIHelper.styleTextField(nameField);
        UIHelper.styleTextField(taxIdField);
        UIHelper.styleTextField(emailField);
        UIHelper.styleTextField(addressField);
        UIHelper.installDigitsOnlyFilter(taxIdField, 9);

        FormField nameForm = new FormField("Nome", nameField, true, "Nome completo ou denominação social");
        FormField taxForm = new FormField("NUIT / NIF", taxIdField, true, null);
        FormField emailForm = new FormField("Email", emailField, true, null);
        FormField addressForm = new FormField("Endereço", addressField, false, null);
        FormField termsForm = new FormField("Prazo de pagamento (dias)", termsField, false,
                "0 = pronto pagamento. Define o vencimento das faturas futuras deste cliente.");
        FormField creditForm = new FormField("Limite de crédito (MT)", creditField, false,
                "Em branco = sem limite. Zero = não vende a crédito.");
        JPanel form = UIHelper.createDialogForm(
                "", nameForm, "", taxForm, "", emailForm, "", addressForm, "", termsForm, "", creditForm);

        String title = existing == null ? "Novo Cliente" : "Editar Cliente — " + existing.name();
        ModernFormDialog dialog = new ModernFormDialog(
                UIHelper.mainWindow, title, null, "Dados de cadastro do cliente", form);
        dialog.setOnSaveAsync(() -> {
            boolean valid = nameForm.validateMinLength(3, "Nome")
                    & taxForm.validateNuit()
                    & emailForm.validateEmail();
            if (!valid) throw new IllegalArgumentException("Corrija os campos assinalados com formato inválido.");
            String name = nameField.getText().trim();
            String taxId = taxIdField.getText().trim();
            String email = emailField.getText().trim();
            String address = addressField.getText().trim();
            int terms = termsField.value();
            java.math.BigDecimal creditLimit = creditField.optionalValue();
            return () -> {
                if (existing == null) {
                    comercialApiClient.createClient(name, taxId, email, address, terms, creditLimit);
                } else {
                    comercialApiClient.updateClient(existing.id(), name, taxId, email, address, terms, creditLimit);
                }
                return null;
            };
        });
        boolean confirmed = dialog.showDialog();
        if (!confirmed) return;
        ToastManager.success(this, existing == null ? "Cliente criado." : "Cliente actualizado.");
        onPanelSelected();
    }

    private void deleteSelected() {
        ClientDTO c = selectedClient();
        if (c == null) return;
        int confirm = JOptionPane.showConfirmDialog(this,
                "Eliminar o cliente '" + c.name() + "'? Esta ação não pode ser revertida.",
                "Confirmar Eliminação", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (confirm != JOptionPane.YES_OPTION) return;
        try {
            comercialApiClient.deleteClient(c.id());
            ToastManager.success(this, "Cliente eliminado.");
            onPanelSelected();
        } catch (Exception ex) {
            feedback.show(FeedbackType.ERROR, "Não foi possível eliminar o cliente", ex.getMessage(), null, null);
        }
    }

    private void openClientDetailDialog(ClientDTO client) {
        new mz.multicore.erp.gui.commercial.CustomerDetailDialog(this, client,
                () -> openClientDialog(client),
                this::selectStatementTab).show();
    }
    private JLabel filterLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(new Font("Segoe UI", Font.BOLD, 11));
        label.setForeground(UIHelper.TEXT_MUTED);
        return label;
    }
}
