package mz.multicore.erp.gui;

import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.desktop.client.*;
import mz.multicore.erp.gui.components.*;
import mz.multicore.erp.gui.users.UserManagementPanel;
import mz.multicore.erp.modules.audit.dto.AuditLogDTO;
import mz.multicore.erp.modules.support.dto.SupportTicketDTO;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Painel central de configurações corporativas, auditoria e preferências do sistema.
 */
public class ConfigPanel extends JPanel {

    final UserApiClient userApiClient;
    final AuditApiClient auditApiClient;
    final BackupApiClient backupApiClient;
    final DocumentConfigApiClient documentConfigApiClient;
    final SupportApiClient supportApiClient;
    final MySubscriptionApiClient mySubscriptionApiClient;
    final SystemMonitoringApiClient monitoringApiClient;

    private final ConfigSupportPanel supportPanel;
    private final ConfigBackupsPanel backupsPanel;
    private final ConfigDocumentColumnsPanel columnsPanel;
    private final ConfigSubscriptionPanel subscriptionPanel;
    /** Activity profile panel (SA-C01); {@code null} in legacy constructors. */
    private final mz.multicore.erp.gui.sector.ActivityProfilePanel activityPanel;
    /** Exchange rates and base currency (FX-UI-03); {@code null} in legacy constructors. */
    private final mz.multicore.erp.gui.currency.ExchangeRatesPanel exchangeRatesPanel;
    /** Cost centres and the default centre (CC-UI-01); {@code null} in legacy constructors. */
    private final mz.multicore.erp.gui.accounting.CostCentersPanel costCentersPanel;
    /** Public API keys (API-UI-01); {@code null} in legacy constructors. */
    private final mz.multicore.erp.gui.integrations.IntegrationsPanel integrationsPanel;
    private UserManagementPanel usersPanel;

    // TAB 1: AUDIT LOGS
    private DefaultTableModel auditTableModel;
    private JTable auditTable;

    // TAB 5: SUPORTE À PLATAFORMA (compatibilidade retroativa)
    DefaultTableModel supportModel;
    JTable supportTable;
    List<SupportTicketDTO> supportTickets = new ArrayList<>();

    public ConfigPanel(UserApiClient userApiClient, AuditApiClient auditApiClient, BackupApiClient backupApiClient,
                       DocumentConfigApiClient documentConfigApiClient, SupportApiClient supportApiClient,
                       MySubscriptionApiClient mySubscriptionApiClient,
                       SystemMonitoringApiClient monitoringApiClient) {
        this(userApiClient, auditApiClient, backupApiClient, documentConfigApiClient, supportApiClient,
                mySubscriptionApiClient, monitoringApiClient, null);
    }

    /** With the company activity profile (SA-C01): appends its tab at the end. */
    public ConfigPanel(UserApiClient userApiClient, AuditApiClient auditApiClient, BackupApiClient backupApiClient,
                       DocumentConfigApiClient documentConfigApiClient, SupportApiClient supportApiClient,
                       MySubscriptionApiClient mySubscriptionApiClient,
                       SystemMonitoringApiClient monitoringApiClient,
                       mz.multicore.erp.desktop.client.ActivityApiClient activityApiClient) {
        this(userApiClient, auditApiClient, backupApiClient, documentConfigApiClient, supportApiClient,
                mySubscriptionApiClient, monitoringApiClient, activityApiClient, null);
    }

    /** With the CSV import screen (IMPORTACAO_DADOS_SPEC §3.4): appends "Importar dados" at the end. */
    public ConfigPanel(UserApiClient userApiClient, AuditApiClient auditApiClient, BackupApiClient backupApiClient,
                       DocumentConfigApiClient documentConfigApiClient, SupportApiClient supportApiClient,
                       MySubscriptionApiClient mySubscriptionApiClient,
                       SystemMonitoringApiClient monitoringApiClient,
                       mz.multicore.erp.desktop.client.ActivityApiClient activityApiClient,
                       DataImportApiClient dataImportApiClient) {
        this(userApiClient, auditApiClient, backupApiClient, documentConfigApiClient, supportApiClient,
                mySubscriptionApiClient, monitoringApiClient, activityApiClient, dataImportApiClient, null);
    }

    /** With the exchange-rate table and base currency (MOEDA_ESTRANGEIRA_IMPORTACAO_SPEC §4.5): appended at the end. */
    public ConfigPanel(UserApiClient userApiClient, AuditApiClient auditApiClient, BackupApiClient backupApiClient,
                       DocumentConfigApiClient documentConfigApiClient, SupportApiClient supportApiClient,
                       MySubscriptionApiClient mySubscriptionApiClient,
                       SystemMonitoringApiClient monitoringApiClient,
                       mz.multicore.erp.desktop.client.ActivityApiClient activityApiClient,
                       DataImportApiClient dataImportApiClient,
                       ExchangeRateApiClient exchangeRateApiClient) {
        this(userApiClient, auditApiClient, backupApiClient, documentConfigApiClient, supportApiClient,
                mySubscriptionApiClient, monitoringApiClient, activityApiClient, dataImportApiClient,
                exchangeRateApiClient, null);
    }

    /** With the cost centres and the default centre (CENTROS_CUSTO_ORCAMENTO_SPEC §3.3): appended at the end. */
    public ConfigPanel(UserApiClient userApiClient, AuditApiClient auditApiClient, BackupApiClient backupApiClient,
                       DocumentConfigApiClient documentConfigApiClient, SupportApiClient supportApiClient,
                       MySubscriptionApiClient mySubscriptionApiClient,
                       SystemMonitoringApiClient monitoringApiClient,
                       mz.multicore.erp.desktop.client.ActivityApiClient activityApiClient,
                       DataImportApiClient dataImportApiClient,
                       ExchangeRateApiClient exchangeRateApiClient,
                       AccountingApiClient accountingApiClient) {
        this(userApiClient, auditApiClient, backupApiClient, documentConfigApiClient, supportApiClient,
                mySubscriptionApiClient, monitoringApiClient, activityApiClient, dataImportApiClient,
                exchangeRateApiClient, accountingApiClient, null);
    }

    /** With the public API keys (API_PUBLICA_WEBHOOKS_SPEC §4.4.1): "Integrações" appended as the last tab. */
    public ConfigPanel(UserApiClient userApiClient, AuditApiClient auditApiClient, BackupApiClient backupApiClient,
                       DocumentConfigApiClient documentConfigApiClient, SupportApiClient supportApiClient,
                       MySubscriptionApiClient mySubscriptionApiClient,
                       SystemMonitoringApiClient monitoringApiClient,
                       mz.multicore.erp.desktop.client.ActivityApiClient activityApiClient,
                       DataImportApiClient dataImportApiClient,
                       ExchangeRateApiClient exchangeRateApiClient,
                       AccountingApiClient accountingApiClient,
                       ApiKeyApiClient apiKeyApiClient) {
        this(userApiClient, auditApiClient, backupApiClient, documentConfigApiClient, supportApiClient,
                mySubscriptionApiClient, monitoringApiClient, activityApiClient, dataImportApiClient,
                exchangeRateApiClient, accountingApiClient, apiKeyApiClient, null);
    }

    /** With the webhooks too (API_PUBLICA_WEBHOOKS_SPEC §4.4.2): second section of "Integrações". */
    public ConfigPanel(UserApiClient userApiClient, AuditApiClient auditApiClient, BackupApiClient backupApiClient,
                       DocumentConfigApiClient documentConfigApiClient, SupportApiClient supportApiClient,
                       MySubscriptionApiClient mySubscriptionApiClient,
                       SystemMonitoringApiClient monitoringApiClient,
                       mz.multicore.erp.desktop.client.ActivityApiClient activityApiClient,
                       DataImportApiClient dataImportApiClient,
                       ExchangeRateApiClient exchangeRateApiClient,
                       AccountingApiClient accountingApiClient,
                       ApiKeyApiClient apiKeyApiClient,
                       mz.multicore.erp.desktop.client.WebhookApiClient webhookApiClient) {
        this.integrationsPanel = apiKeyApiClient == null ? null
                : new mz.multicore.erp.gui.integrations.IntegrationsPanel(apiKeyApiClient, webhookApiClient);
        this.costCentersPanel = accountingApiClient == null ? null
                : new mz.multicore.erp.gui.accounting.CostCentersPanel(accountingApiClient);
        this.exchangeRatesPanel = exchangeRateApiClient == null ? null
                : new mz.multicore.erp.gui.currency.ExchangeRatesPanel(exchangeRateApiClient);
        this.activityPanel = activityApiClient == null ? null
                : new mz.multicore.erp.gui.sector.ActivityProfilePanel(activityApiClient);
        this.userApiClient = userApiClient;
        this.auditApiClient = auditApiClient;
        this.backupApiClient = backupApiClient;
        this.documentConfigApiClient = documentConfigApiClient;
        this.supportApiClient = supportApiClient;
        this.mySubscriptionApiClient = mySubscriptionApiClient;
        this.monitoringApiClient = monitoringApiClient;

        this.supportPanel = new ConfigSupportPanel(this);
        this.backupsPanel = new ConfigBackupsPanel(this);
        this.columnsPanel = new ConfigDocumentColumnsPanel(this);
        this.subscriptionPanel = new ConfigSubscriptionPanel(this);

        setLayout(new BorderLayout());
        setBackground(UIHelper.BG_DARK);
        setBorder(new EmptyBorder(Spacing.SM, Spacing.SM, Spacing.SM, Spacing.SM));

        add(buildAppearanceBar(), BorderLayout.NORTH);

        JTabbedPane tabbedPane = new JTabbedPane();
        UIHelper.styleTabbedPaneMulticore(tabbedPane);

        tabbedPane.addTab("Log de Auditoria Geral", UIHelper.icon("fas-clipboard-list", 16, UIHelper.ACCENT_BLUE), createAuditTab());
        tabbedPane.addTab("Cópias de Segurança e Integridade", UIHelper.icon("fas-database", 16, UIHelper.ACCENT_CYAN), backupsPanel.buildPanel());
        tabbedPane.addTab("Utilizadores & Permissões", UIHelper.icon("fas-user-shield", 16, UIHelper.ACCENT), createUsersTab());
        tabbedPane.addTab("Colunas dos Documentos", UIHelper.icon("fas-table", 16, UIHelper.PENDING_YELLOW), columnsPanel.buildPanel());
        tabbedPane.addTab("Suporte à Plataforma", UIHelper.icon("fas-headset", 16, UIHelper.ACCENT_SKY), supportPanel.buildPanel());
        tabbedPane.addTab("A Minha Assinatura", UIHelper.icon("fas-id-card", 16, UIHelper.APPROVED_GREEN), subscriptionPanel.buildPanel());
        if (activityPanel != null) {
            tabbedPane.addTab("Perfil de Actividade", UIHelper.icon("fas-industry", 16, UIHelper.ACCENT_ORANGE), activityPanel);
            tabbedPane.addTab("Balança deste Posto", UIHelper.icon("fas-balance-scale", 16, UIHelper.ACCENT_BLUE),
                    new mz.multicore.erp.gui.pos.scale.ScaleSettingsPanel());
            tabbedPane.addTab("Gaveta deste Posto", UIHelper.icon("fas-cash-register", 16, UIHelper.APPROVED_GREEN),
                    new mz.multicore.erp.gui.pos.drawer.DrawerSettingsPanel());
        }
        if (dataImportApiClient != null) {
            tabbedPane.addTab("Importar dados", UIHelper.icon("fas-file-import", 16, UIHelper.ACCENT_BLUE),
                    new mz.multicore.erp.gui.dataimport.DataImportPanel(dataImportApiClient));
        }
        if (exchangeRatesPanel != null) {
            tabbedPane.addTab("Câmbios e Moeda Base", UIHelper.icon("fas-exchange-alt", 16, UIHelper.ACCENT_CYAN),
                    exchangeRatesPanel);
        }
        if (costCentersPanel != null) {
            tabbedPane.addTab("Centros de Custo", UIHelper.icon("fas-sitemap", 16, UIHelper.ACCENT_ORANGE),
                    costCentersPanel);
        }
        if (integrationsPanel != null) {
            tabbedPane.addTab("Integrações", UIHelper.icon("fas-plug", 16, UIHelper.ACCENT_SKY), integrationsPanel);
        }

        add(tabbedPane, BorderLayout.CENTER);
    }

    private JPanel buildAppearanceBar() {
        JPanel bar = new JPanel(new FlowLayout(FlowLayout.RIGHT, Spacing.SM, Spacing.XS));
        bar.setOpaque(false);

        ModernButton monitorBtn = UIHelper.createSecondaryButton("Diagnóstico e Saúde");
        monitorBtn.setIcon(UIHelper.icon("fas-heartbeat", 14));
        monitorBtn.addActionListener(e -> new SystemMonitoringDialog(SwingUtilities.getWindowAncestor(this), monitoringApiClient).show());
        bar.add(monitorBtn);

        // Every choice visible and picked directly (docs/ECRAS_FORA_DA_REGRA_SPEC.md); both rebuild the window.
        Theme[] themes = {Theme.LIGHT, Theme.DARK, Theme.HIGH_CONTRAST};
        bar.add(caption("Tema:"));
        bar.add(SegmentedControl.of("Tema", new String[]{"Claro", "Escuro", "Alto contraste"},
                java.util.Arrays.asList(themes).indexOf(UIHelper.currentTheme()), i -> UIHelper.setTheme(themes[i])));

        UiDensity[] densities = {UiDensity.COMPACT, UiDensity.STANDARD, UiDensity.COMFORTABLE};
        bar.add(caption("Densidade:"));
        bar.add(SegmentedControl.of("Densidade", new String[]{"Compacto", "Padrão", "Confortável"},
                java.util.Arrays.asList(densities).indexOf(UiDensityManager.getInstance().getDensity()),
                i -> UiDensityManager.getInstance().setDensity(densities[i])));
        return bar;
    }

    private static JLabel caption(String text) {
        JLabel label = new JLabel(text);
        label.setFont(Typography.BODY_BOLD);
        label.setForeground(UIHelper.TEXT_MUTED);
        return label;
    }

    private JPanel createAuditTab() {
        JPanel panel = new JPanel(new BorderLayout(0, 15));
        panel.setBackground(UIHelper.BG_DARK);
        panel.setBorder(new EmptyBorder(Spacing.LG, Spacing.LG, Spacing.LG, Spacing.LG));

        ModernPanel card = new ModernPanel(16);
        card.setLayout(new BorderLayout(0, 10));
        card.setBorder(new EmptyBorder(Spacing.XL, Spacing.XL, Spacing.XL, Spacing.XL));

        String[] cols = {"Data/Hora", "Utilizador", "Acção", "Detalhes do Evento"};
        auditTableModel = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        auditTable = new JTable(auditTableModel);
        UIHelper.styleTable(auditTable);
        UIHelper.ensureHeadersFit(auditTable);
        JScrollPane scroll = new JScrollPane(auditTable);
        UIHelper.styleScrollPane(scroll);

        JTextField auditSearch = TableFilter.searchField("Utilizador, acção ou detalhe…");
        JComboBox<String> auditPeriodo = TableFilter.periodCombo();
        TableFilter.install(auditTable, auditSearch,
                List.of(),
                List.of(new TableFilter.PeriodFilter(auditPeriodo, 0)));
        ModernButton refreshBtn = UIHelper.createRefreshButton(this::loadAuditLogs);
        JPanel auditToolbar = TableFilter.bar(auditSearch, TableFilter.label("Data:", "fas-calendar-alt"), auditPeriodo);
        auditToolbar.setBorder(new EmptyBorder(0, 0, Spacing.SM, 0));
        card.add(UIHelper.tableCardTop("Registo de Auditoria de Acções Críticas", auditToolbar, refreshBtn), BorderLayout.NORTH);
        card.add(scroll, BorderLayout.CENTER);
        TableQuickPeekController.install(auditTable, card, (peek, row) -> {
            String data = String.valueOf(auditTableModel.getValueAt(row, 0));
            String user = String.valueOf(auditTableModel.getValueAt(row, 1));
            String acao = String.valueOf(auditTableModel.getValueAt(row, 2));
            String det = String.valueOf(auditTableModel.getValueAt(row, 3));
            peek.setHeaderIcon("fas-history", UIHelper.ACCENT_CYAN);
            peek.setTitle(acao);
            peek.setSubtitle("Utilizador: " + user);
            peek.setStatus("LOG AUDITORIA", UIHelper.TEXT_MUTED);
            peek.setItems(List.of(
                    new QuickPeekPanel.PeekItem("Data/Hora", data, false),
                    new QuickPeekPanel.PeekItem("Detalhes", det, false)
            ));
        });

        panel.add(card, BorderLayout.CENTER);
        return panel;
    }

    private JPanel createUsersTab() {
        this.usersPanel = new UserManagementPanel(userApiClient);
        return usersPanel;
    }

    public void onPanelSelected() {
        loadAuditLogs();
        if (backupsPanel != null) backupsPanel.refresh();
        if (usersPanel != null) usersPanel.refreshDataAsync();
        if (columnsPanel != null) columnsPanel.refresh();
        if (supportPanel != null) supportPanel.refresh();
        if (subscriptionPanel != null) subscriptionPanel.refresh();
        if (activityPanel != null) activityPanel.refresh();
        if (exchangeRatesPanel != null) exchangeRatesPanel.refresh();
        if (costCentersPanel != null) costCentersPanel.refresh();
        if (integrationsPanel != null) integrationsPanel.refresh();
    }

    void loadAuditLogs() {
        Long companyId = CurrentUserContext.getCurrentCompanyId();
        UIHelper.loadAsync(this, () -> auditApiClient.getLogsByCompany(companyId), this::applyAuditLogs,
                error -> showConfigError("registo de auditoria", error));
    }

    private void applyAuditLogs(List<AuditLogDTO> logs) {
        if (auditTableModel == null) return;
        auditTableModel.setRowCount(0);
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
        for (AuditLogDTO l : logs) {
            auditTableModel.addRow(new Object[]{
                    l.eventTime().format(dtf),
                    l.username(),
                    AuditActionLabels.label(l.action()),
                    l.details()
            });
        }
    }

    void showConfigError(String area, Throwable error) {
        ToastManager.show(this, FeedbackType.ERROR,
                "Não foi possível processar " + area + ": " + error.getMessage());
    }
}
