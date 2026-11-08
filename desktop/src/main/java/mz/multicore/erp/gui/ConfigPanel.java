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
        setBorder(new EmptyBorder(10, 10, 10, 10));

        add(buildAppearanceBar(), BorderLayout.NORTH);

        JTabbedPane tabbedPane = new JTabbedPane();
        UIHelper.styleTabbedPaneMulticore(tabbedPane);

        tabbedPane.addTab("Log de Auditoria Geral", UIHelper.icon("fas-clipboard-list", 16, UIHelper.ACCENT_BLUE), createAuditTab());
        tabbedPane.addTab("Cópias de Segurança & Backups", UIHelper.icon("fas-database", 16, UIHelper.ACCENT_CYAN), backupsPanel.buildPanel());
        tabbedPane.addTab("Utilizadores & Permissões", UIHelper.icon("fas-user-shield", 16, UIHelper.ACCENT), createUsersTab());
        tabbedPane.addTab("Colunas dos Documentos", UIHelper.icon("fas-table", 16, UIHelper.PENDING_YELLOW), columnsPanel.buildPanel());
        tabbedPane.addTab("Suporte à Plataforma", UIHelper.icon("fas-headset", 16, UIHelper.ACCENT_SKY), supportPanel.buildPanel());
        tabbedPane.addTab("A Minha Assinatura", UIHelper.icon("fas-id-card", 16, UIHelper.APPROVED_GREEN), subscriptionPanel.buildPanel());
        if (activityPanel != null) {
            tabbedPane.addTab("Perfil de Actividade", UIHelper.icon("fas-industry", 16, UIHelper.ACCENT_ORANGE), activityPanel);
            tabbedPane.addTab("Balança deste Posto", UIHelper.icon("fas-balance-scale", 16, UIHelper.ACCENT_BLUE),
                    new mz.multicore.erp.gui.pos.scale.ScaleSettingsPanel());
        }

        add(tabbedPane, BorderLayout.CENTER);
    }

    private JPanel buildAppearanceBar() {
        JPanel bar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 4));
        bar.setOpaque(false);

        JLabel label = new JLabel("Aparência:");
        label.setForeground(UIHelper.TEXT_LIGHT);
        bar.add(label);

        ModernButton monitorBtn = new ModernButton("Diagnóstico & Saúde", UIHelper.ACCENT_CYAN, UIHelper.ACCENT_CYAN.darker());
        monitorBtn.setIcon(UIHelper.icon("fas-heartbeat", 14, Color.WHITE));
        monitorBtn.addActionListener(e -> new SystemMonitoringDialog(SwingUtilities.getWindowAncestor(this), monitoringApiClient).show());
        bar.add(monitorBtn);

        ModernButton themeBtn = UIHelper.createSecondaryButton(themeButtonLabel());
        themeBtn.setIcon(UIHelper.icon(themeButtonIcon(), 14));
        themeBtn.addActionListener(e -> {
            UIHelper.cycleTheme();
            themeBtn.setText(themeButtonLabel());
            themeBtn.setIcon(UIHelper.icon(themeButtonIcon(), 14));
        });
        bar.add(themeBtn);

        ModernButton densityBtn = UIHelper.createSecondaryButton(densityButtonLabel());
        densityBtn.setIcon(UIHelper.icon("fas-text-height", 14));
        densityBtn.addActionListener(e -> {
            UiDensityManager.getInstance().cycleDensity();
            densityBtn.setText(densityButtonLabel());
        });
        bar.add(densityBtn);
        return bar;
    }

    private String densityButtonLabel() {
        return "Densidade: " + UiDensityManager.getInstance().getDensity().getLabel().split(" ")[0];
    }

    private String themeButtonLabel() {
        if (UIHelper.isHighContrast()) return "Tema: Alto Contraste (Clique p/ Escuro)";
        return UIHelper.isLight() ? "Tema: Claro (Clique p/ Alto Contraste)" : "Tema: Escuro (Clique p/ Claro)";
    }

    private String themeButtonIcon() {
        if (UIHelper.isHighContrast()) return "fas-adjust";
        return UIHelper.isLight() ? "fas-moon" : "fas-sun";
    }

    private JPanel createAuditTab() {
        JPanel panel = new JPanel(new BorderLayout(0, 15));
        panel.setBackground(UIHelper.BG_DARK);
        panel.setBorder(new EmptyBorder(15, 15, 15, 15));

        ModernPanel card = new ModernPanel(16);
        card.setLayout(new BorderLayout(0, 10));
        card.setBorder(new EmptyBorder(20, 20, 20, 20));

        String[] cols = {"Data/Hora", "Utilizador", "Ação", "Detalhes do Evento"};
        auditTableModel = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        auditTable = new JTable(auditTableModel);
        UIHelper.styleTable(auditTable);
        UIHelper.ensureHeadersFit(auditTable);
        JScrollPane scroll = new JScrollPane(auditTable);
        UIHelper.styleScrollPane(scroll);

        JTextField auditSearch = TableFilter.searchField("Utilizador, ação ou detalhe…");
        JComboBox<String> auditPeriodo = TableFilter.periodCombo();
        TableFilter.install(auditTable, auditSearch,
                List.of(),
                List.of(new TableFilter.PeriodFilter(auditPeriodo, 0)));
        ModernButton refreshBtn = UIHelper.createRefreshButton(this::loadAuditLogs);
        JPanel auditToolbar = TableFilter.bar(auditSearch, TableFilter.label("Data:", "fas-calendar-alt"), auditPeriodo);
        auditToolbar.setBorder(new EmptyBorder(0, 0, 10, 0));
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
