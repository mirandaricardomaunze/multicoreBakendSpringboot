package mz.multicore.erp.gui;

import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.architecture.security.PermissionGuard;
import mz.multicore.erp.gui.components.ModernButton;
import mz.multicore.erp.gui.components.ActionMenuButton;
import mz.multicore.erp.gui.components.ModernFormDialog;
import mz.multicore.erp.gui.components.ModernPanel;
import mz.multicore.erp.gui.components.TableFilter;
import mz.multicore.erp.gui.components.Theme;
import mz.multicore.erp.gui.components.UIHelper;
import mz.multicore.erp.desktop.client.AuditApiClient;
import mz.multicore.erp.desktop.client.BackupApiClient;
import mz.multicore.erp.desktop.client.UserApiClient;
import mz.multicore.erp.modules.users.dto.AppUserDTO;
import mz.multicore.erp.modules.audit.dto.AuditLogDTO;
import mz.multicore.erp.modules.backup.dto.BackupStatusDTO;
import mz.multicore.erp.modules.backup.dto.BackupVerificationDTO;
import mz.multicore.erp.modules.backup.dto.PhysicalBackupResultDTO;
import mz.multicore.erp.modules.documents.dto.DocumentColumnsDTO;
import mz.multicore.erp.desktop.client.DocumentConfigApiClient;
import mz.multicore.erp.modules.subscription.dto.MySubscriptionDTO;
import mz.multicore.erp.desktop.client.MySubscriptionApiClient;
import mz.multicore.erp.modules.support.dto.CreateTicketRequest;
import mz.multicore.erp.modules.support.dto.SupportTicketDTO;
import mz.multicore.erp.desktop.client.SupportApiClient;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.File;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class ConfigPanel extends JPanel {

    private final UserApiClient userApiClient;
    private final AuditApiClient auditApiClient;
    private final BackupApiClient backupApiClient;
    private final DocumentConfigApiClient documentConfigApiClient;
    final SupportApiClient supportApiClient;
    private final ConfigSupportPanel supportPanel;
    private final MySubscriptionApiClient mySubscriptionApiClient;

    // TAB 5: SUPORTE À PLATAFORMA
    DefaultTableModel supportModel;
    JTable supportTable;
    java.util.List<SupportTicketDTO> supportTickets = new java.util.ArrayList<>();

    // TAB 6: A MINHA ASSINATURA
    private JPanel subscriptionCard;

    // TAB 1: AUDIT LOGS
    private DefaultTableModel auditTableModel;
    private JTable auditTable;

    // TAB 2: BACKUPS
    private JTextArea backupLogArea;
    private JLabel backupAutoStatus;
    private DefaultTableModel backupFilesModel;
    private JTable backupFilesTable;

    // TAB 3: USERS
    private static final String[] USER_ROLES = {"EMPLOYEE", "SELLER", "MANAGER", "ADMIN"};
    private DefaultTableModel usersTableModel;
    private JTable usersTable;

    // TAB 4: DOCUMENT COLUMNS
    private JComboBox<String> docTypeCombo;
    private JCheckBox colBarcode;
    private JCheckBox colReference;
    private JCheckBox colDescription;
    private JCheckBox colExpiry;
    private JCheckBox colQuantity;
    private JCheckBox colUnitPrice;
    private JCheckBox colTax;
    private JCheckBox colSubtotal;
    private javax.swing.JTextField footerField;

    public ConfigPanel(UserApiClient userApiClient, AuditApiClient auditApiClient, BackupApiClient backupApiClient,
                       DocumentConfigApiClient documentConfigApiClient, SupportApiClient supportApiClient,
                       MySubscriptionApiClient mySubscriptionApiClient) {
        this.userApiClient = userApiClient;
        this.auditApiClient = auditApiClient;
        this.backupApiClient = backupApiClient;
        this.documentConfigApiClient = documentConfigApiClient;
        this.supportApiClient = supportApiClient;
        this.supportPanel = new ConfigSupportPanel(this);
        this.mySubscriptionApiClient = mySubscriptionApiClient;

        setLayout(new BorderLayout());
        setBackground(UIHelper.BG_DARK);
        setBorder(new EmptyBorder(10, 10, 10, 10));

        add(buildAppearanceBar(), BorderLayout.NORTH);

        JTabbedPane tabbedPane = new JTabbedPane();
        UIHelper.styleTabbedPaneMulticore(tabbedPane);

        // TAB 1: AUDIT LOGS
        JPanel tabAudit = createAuditTab();
        tabbedPane.addTab("Log de Auditoria Geral", UIHelper.icon("fas-clipboard-list", 16, UIHelper.TEXT_LIGHT), tabAudit);

        // TAB 2: BACKUPS
        JPanel tabBackups = createBackupsTab();
        tabbedPane.addTab("Cópias de Segurança & Backups", UIHelper.icon("fas-database", 16, UIHelper.TEXT_LIGHT), tabBackups);

        // TAB 3: USERS
        JPanel tabUsers = createUsersTab();
        tabbedPane.addTab("Utilizadores & Permissões", UIHelper.icon("fas-user-shield", 16, UIHelper.TEXT_LIGHT), tabUsers);

        // TAB 4: DOCUMENT COLUMNS
        JPanel tabColumns = createDocumentColumnsTab();
        tabbedPane.addTab("Colunas dos Documentos", UIHelper.icon("fas-table", 16, UIHelper.TEXT_LIGHT), tabColumns);

        // TAB 5: SUPORTE À PLATAFORMA
        JPanel tabSupport = createSupportTab();
        tabbedPane.addTab("Suporte à Plataforma", UIHelper.icon("fas-headset", 16, UIHelper.TEXT_LIGHT), tabSupport);

        // TAB 6: A MINHA ASSINATURA
        JPanel tabSubscription = createSubscriptionTab();
        tabbedPane.addTab("A Minha Assinatura", UIHelper.icon("fas-id-card", 16, UIHelper.TEXT_LIGHT), tabSubscription);

        add(tabbedPane, BorderLayout.CENTER);

        // Carregamento preguiçoso: dados por HTTP em onPanelSelected() (via navigate), não no
        // construtor — arranque resiliente se o backend falhar.
    }

    private JPanel buildAppearanceBar() {
        JPanel bar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 4));
        bar.setOpaque(false);

        JLabel label = new JLabel("Aparência:");
        label.setForeground(UIHelper.TEXT_LIGHT);
        bar.add(label);

        ModernButton themeBtn = UIHelper.createSecondaryButton(themeButtonLabel());
        themeBtn.setIcon(UIHelper.icon(UIHelper.isLight() ? "fas-moon" : "fas-sun", 14));
        themeBtn.addActionListener(e -> {
            UIHelper.setTheme(UIHelper.isLight() ? Theme.DARK : Theme.LIGHT);
            themeBtn.setText(themeButtonLabel());
            themeBtn.setIcon(UIHelper.icon(UIHelper.isLight() ? "fas-moon" : "fas-sun", 14));
        });
        bar.add(themeBtn);
        return bar;
    }

    private String themeButtonLabel() {
        return UIHelper.isLight() ? "Mudar para Tema Escuro" : "Mudar para Tema Claro";
    }

    private JPanel createAuditTab() {
        JPanel panel = new JPanel(new BorderLayout(0, 15));
        panel.setBackground(UIHelper.BG_DARK);
        panel.setBorder(new EmptyBorder(15, 15, 15, 15));

        JLabel title = UIHelper.createHeading("Registo de Auditoria de Ações Críticas");
        panel.add(title, BorderLayout.NORTH);

        ModernPanel card = new ModernPanel(16);
        card.setLayout(new BorderLayout(0, 10));
        card.setBorder(new EmptyBorder(20, 20, 20, 20));

        String[] cols = {"Data/Hora", "Utilizador", "Ação", "Detalhes do Evento"};
        auditTableModel = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int r, int c) { return false; }
        };
        auditTable = new JTable(auditTableModel);
        UIHelper.styleTable(auditTable);
        JScrollPane scroll = new JScrollPane(auditTable);
        UIHelper.styleScrollPane(scroll);

        JTextField auditSearch = TableFilter.searchField("Utilizador, ação ou detalhe…");
        JComboBox<String> auditPeriodo = TableFilter.periodCombo();
        TableFilter.install(auditTable, auditSearch,
                java.util.List.of(),
                java.util.List.of(new TableFilter.PeriodFilter(auditPeriodo, 0)));
        JPanel auditBar = TableFilter.bar(auditSearch,
                TableFilter.label("Data:", "fas-calendar-alt"), auditPeriodo);
        auditBar.setBorder(new EmptyBorder(0, 0, 10, 0));
        card.add(auditBar, BorderLayout.NORTH);
        card.add(scroll, BorderLayout.CENTER);

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        btnPanel.setOpaque(false);
        ModernButton refreshBtn = UIHelper.createSecondaryButton("Actualizar Registos");
        refreshBtn.setIcon(UIHelper.icon("fas-sync-alt", 14));
        btnPanel.add(refreshBtn);
        card.add(btnPanel, BorderLayout.SOUTH);

        panel.add(card, BorderLayout.CENTER);

        // LISTENERS
        refreshBtn.addActionListener(e -> loadAuditLogs());

        return panel;
    }

    private JPanel createBackupsTab() {
        JPanel panel = new JPanel(new GridLayout(1, 2, 20, 0));
        panel.setBackground(UIHelper.BG_DARK);
        panel.setBorder(new EmptyBorder(15, 15, 15, 15));

        // LEFT: BACKUP ACTION & CONSOLE
        JPanel leftPanel = new JPanel(new BorderLayout(0, 15));
        leftPanel.setOpaque(false);
        leftPanel.add(UIHelper.createHeading("Gestão de Cópias de Segurança"), BorderLayout.NORTH);

        ModernPanel consoleCard = new ModernPanel(16);
        consoleCard.setLayout(new BorderLayout(0, 15));
        consoleCard.setBorder(new EmptyBorder(20, 20, 20, 20));

        JLabel desc = new JLabel("<html><body>O <b>backup lógico (.json)</b> é um snapshot de verificação por empresa (auditoria). Para recuperação de desastres use o <b>backup físico (BD)</b>, restaurável com fidelidade total via pg_dump/pg_restore. O <b>backup físico automático</b> corre diariamente e apaga cópias antigas conforme a retenção.</body></html>");
        desc.setFont(new Font(UIHelper.FONT, Font.PLAIN, 13));
        desc.setForeground(UIHelper.TEXT_MUTED);

        backupAutoStatus = new JLabel(" ");
        backupAutoStatus.setFont(new Font(UIHelper.FONT, Font.BOLD, 12));
        backupAutoStatus.setBorder(new EmptyBorder(8, 0, 0, 0));

        JPanel northInfo = new JPanel(new BorderLayout(0, 4));
        northInfo.setOpaque(false);
        northInfo.add(desc, BorderLayout.NORTH);
        northInfo.add(backupAutoStatus, BorderLayout.SOUTH);
        consoleCard.add(northInfo, BorderLayout.NORTH);

        backupLogArea = new JTextArea();
        backupLogArea.setBackground(UIHelper.BG_DARK);
        backupLogArea.setForeground(UIHelper.APPROVED_GREEN);
        backupLogArea.setFont(new Font("Consolas", Font.PLAIN, 12));
        backupLogArea.setEditable(false);
        backupLogArea.setMargin(new Insets(10, 10, 10, 10));
        JScrollPane scrollConsole = new JScrollPane(backupLogArea);
        UIHelper.styleScrollPane(scrollConsole);
        consoleCard.add(scrollConsole, BorderLayout.CENTER);

        ActionMenuButton createBackupBtn = UIHelper.createActionMenuButton("Criar backup")
                .addAction("Backup lógico (.json)", UIHelper.icon("fas-file-code", 14), this::runManualBackup)
                .addAction("Backup físico (BD)", UIHelper.icon("fas-database", 14), this::runPhysicalBackup)
                .addAction("Backup automático agora", UIHelper.icon("fas-clock", 14), this::runAutoBackupNow);
        JPanel backupActions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        backupActions.setOpaque(false);
        backupActions.add(createBackupBtn);
        consoleCard.add(backupActions, BorderLayout.SOUTH);

        leftPanel.add(consoleCard, BorderLayout.CENTER);
        panel.add(leftPanel);

        // RIGHT: BACKUPS ARCHIVE LIST
        JPanel rightPanel = new JPanel(new BorderLayout(0, 15));
        rightPanel.setOpaque(false);
        rightPanel.add(UIHelper.createHeading("Ficheiros de Cópia de Segurança (.json)"), BorderLayout.NORTH);

        ModernPanel archiveCard = new ModernPanel(16);
        archiveCard.setLayout(new BorderLayout(0, 10));
        archiveCard.setBorder(new EmptyBorder(20, 20, 20, 20));

        String[] archiveCols = {"Nome do Ficheiro", "Tamanho (KB)"};
        backupFilesModel = new DefaultTableModel(archiveCols, 0) {
            @Override
            public boolean isCellEditable(int r, int c) { return false; }
        };
        backupFilesTable = new JTable(backupFilesModel);
        UIHelper.styleTable(backupFilesTable);
        JScrollPane archiveScroll = new JScrollPane(backupFilesTable);
        UIHelper.styleScrollPane(archiveScroll);
        archiveCard.add(archiveScroll, BorderLayout.CENTER);

        JPanel actionRow = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        actionRow.setOpaque(false);
        ModernButton verifyBackupBtn = UIHelper.createPrimaryButton("Verificar Backup");
        verifyBackupBtn.setIcon(UIHelper.icon("fas-shield-alt", 14));
        ModernButton refreshArchiveBtn = UIHelper.createSecondaryButton("Actualizar Arquivo");
        refreshArchiveBtn.setIcon(UIHelper.icon("fas-sync-alt", 14));
        actionRow.add(verifyBackupBtn);
        actionRow.add(refreshArchiveBtn);
        archiveCard.add(actionRow, BorderLayout.SOUTH);

        rightPanel.add(archiveCard, BorderLayout.CENTER);
        panel.add(rightPanel);

        // LISTENERS
        verifyBackupBtn.addActionListener(e -> verifySelectedBackup());
        refreshArchiveBtn.addActionListener(e -> loadBackupFilesList());

        return panel;
    }

    private JPanel createUsersTab() {
        JPanel panel = new JPanel(new BorderLayout(0, 12));
        panel.setBackground(UIHelper.BG_DARK);
        panel.setBorder(new EmptyBorder(15, 15, 15, 15));

        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        header.add(UIHelper.createHeading("Utilizadores do Sistema"), BorderLayout.WEST);

        ModernButton newUserBtn = UIHelper.createSuccessButton("Novo Utilizador");
        newUserBtn.setIcon(UIHelper.icon("fas-user-plus", 14));
        ModernButton editUserBtn = UIHelper.createPrimaryButton("Editar");
        editUserBtn.setIcon(UIHelper.icon("fas-pen", 14));
        ActionMenuButton moreBtn = UIHelper.createActionMenuButton("Mais acções")
                .addAction("Alterar Perfil", UIHelper.icon("fas-user-shield", 14), this::updateSelectedUserRole)
                .addAction("Actualizar Lista", UIHelper.icon("fas-sync-alt", 14), this::loadUsersList);
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        actions.setOpaque(false);
        actions.add(moreBtn);
        actions.add(editUserBtn);
        actions.add(newUserBtn);
        header.add(actions, BorderLayout.EAST);
        panel.add(header, BorderLayout.NORTH);

        ModernPanel listCard = new ModernPanel(16);
        listCard.setLayout(new BorderLayout());
        listCard.setBorder(new EmptyBorder(15, 15, 15, 15));

        String[] userCols = {"Username", "Nome Completo", "Role", "Estado"};
        usersTableModel = new DefaultTableModel(userCols, 0) {
            @Override
            public boolean isCellEditable(int r, int c) { return false; }
        };
        usersTable = new JTable(usersTableModel);
        UIHelper.styleTable(usersTable);
        usersTable.getColumnModel().getColumn(2).setCellRenderer(mz.multicore.erp.gui.components.TableCellRenderers.role());
        usersTable.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                if (e.getClickCount() == 2) editSelectedUserName();
            }
        });
        JScrollPane scroll = new JScrollPane(usersTable);
        UIHelper.styleScrollPane(scroll);

        JTextField usrSearch = TableFilter.searchField("Username ou nome…");
        JComboBox<String> usrRole = TableFilter.combo("Todos os perfis", "ADMIN", "MANAGER", "EMPLOYEE");
        JComboBox<String> usrEstado = TableFilter.combo("Todos os estados", "ATIVO", "INATIVO");
        TableFilter.install(usersTable, usrSearch,
                new TableFilter.ColumnFilter(usrRole, 2),
                new TableFilter.ColumnFilter(usrEstado, 3));
        JPanel usrBar = TableFilter.bar(usrSearch,
                TableFilter.label("Perfil:"), usrRole,
                TableFilter.label("Estado:"), usrEstado);
        usrBar.setBorder(new EmptyBorder(0, 0, 10, 0));
        listCard.add(usrBar, BorderLayout.NORTH);
        listCard.add(scroll, BorderLayout.CENTER);
        panel.add(listCard, BorderLayout.CENTER);

        // LISTENERS
        newUserBtn.addActionListener(e -> registerUser());
        editUserBtn.addActionListener(e -> editSelectedUserName());

        return panel;
    }

    /** Edição do nome do utilizador seleccionado (o username é imutável). */
    private void editSelectedUserName() {
        int selectedRow = TableFilter.selectedModelRow(usersTable);
        if (selectedRow < 0) {
            JOptionPane.showMessageDialog(this, "Selecione um utilizador na lista.", "Editar", JOptionPane.WARNING_MESSAGE);
            return;
        }
        String username = String.valueOf(usersTableModel.getValueAt(selectedRow, 0));
        String currentName = String.valueOf(usersTableModel.getValueAt(selectedRow, 1));

        javax.swing.JTextField nameField = new javax.swing.JTextField(currentName);
        UIHelper.styleTextField(nameField);
        JPanel form = UIHelper.createDialogForm("Nome completo:", nameField);
        ModernFormDialog dlg = new ModernFormDialog(UIHelper.mainWindow, "Editar Utilizador",
                "fas-pen", "Utilizador: " + username, form).setConfirmButton("Guardar", "fas-check");
        dlg.setOnSaveAsync(() -> {
            if (nameField.getText().trim().isEmpty()) {
                throw new IllegalArgumentException("O nome é obrigatório.");
            }
            String updatedName = nameField.getText().trim();
            return () -> {
                userApiClient.updateUserName(username, updatedName);
                return null;
            };
        });
        if (dlg.showDialog()) {
            JOptionPane.showMessageDialog(this, "Utilizador atualizado.", "Sucesso", JOptionPane.INFORMATION_MESSAGE);
            loadUsersList();
        }
    }

    private JPanel createDocumentColumnsTab() {
        JPanel panel = new JPanel(new BorderLayout(0, 15));
        panel.setBackground(UIHelper.BG_DARK);
        panel.setBorder(new EmptyBorder(15, 15, 15, 15));

        JPanel north = new JPanel(new BorderLayout(0, 10));
        north.setOpaque(false);
        north.add(UIHelper.createHeading("Colunas Visíveis nos Documentos"), BorderLayout.NORTH);
        JPanel typeRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        typeRow.setOpaque(false);
        JLabel typeLbl = new JLabel("Tipo de documento:");
        typeLbl.setForeground(UIHelper.TEXT_MUTED);
        docTypeCombo = new JComboBox<>();
        for (var t : mz.multicore.erp.modules.documents.model.DocumentType.values()) docTypeCombo.addItem(t.label());
        UIHelper.styleComboBox(docTypeCombo);
        docTypeCombo.addActionListener(e -> loadDocumentColumns());
        typeRow.add(typeLbl);
        typeRow.add(docTypeCombo);
        north.add(typeRow, BorderLayout.SOUTH);
        panel.add(north, BorderLayout.NORTH);

        ModernPanel card = new ModernPanel(16);
        card.setLayout(new BorderLayout(0, 15));
        card.setBorder(new EmptyBorder(20, 20, 20, 20));

        JLabel desc = new JLabel("<html><body>Escolha que colunas aparecem na tabela de linhas da <b>Fatura</b>, "
                + "<b>Encomenda</b>, <b>Nota de Crédito</b> e <b>Guia de Remessa</b>. A configuração é por empresa "
                + "e não altera totais nem IVA — apenas a presença visual das colunas.</body></html>");
        desc.setFont(new Font(UIHelper.FONT, Font.PLAIN, 13));
        desc.setForeground(UIHelper.TEXT_MUTED);
        card.add(desc, BorderLayout.NORTH);

        JPanel checks = new JPanel(new GridLayout(0, 2, 12, 8));
        checks.setOpaque(false);
        colBarcode = columnCheckBox("Código de Barras");
        colReference = columnCheckBox("Referência");
        colDescription = columnCheckBox("Descrição");
        colExpiry = columnCheckBox("Validade");
        colQuantity = columnCheckBox("Quantidade");
        colUnitPrice = columnCheckBox("Preço Unitário");
        colTax = columnCheckBox("IVA");
        colSubtotal = columnCheckBox("Subtotal");
        checks.add(colBarcode);
        checks.add(colReference);
        checks.add(colDescription);
        checks.add(colExpiry);
        checks.add(colQuantity);
        checks.add(colUnitPrice);
        checks.add(colTax);
        checks.add(colSubtotal);

        JPanel center = new JPanel(new BorderLayout(0, 14));
        center.setOpaque(false);
        center.add(checks, BorderLayout.NORTH);
        JPanel footerRow = new JPanel(new BorderLayout(0, 4));
        footerRow.setOpaque(false);
        JLabel footerLbl = new JLabel("Comentário do recibo (rodapé) — só para Recibo POS:");
        footerLbl.setForeground(UIHelper.TEXT_MUTED);
        footerField = new javax.swing.JTextField();
        UIHelper.styleTextField(footerField);
        footerField.setToolTipText("Vazio = 'Obrigado pela sua preferência!'. Aplica-se apenas ao Recibo POS.");
        footerRow.add(footerLbl, BorderLayout.NORTH);
        footerRow.add(footerField, BorderLayout.CENTER);
        center.add(footerRow, BorderLayout.CENTER);
        card.add(center, BorderLayout.CENTER);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        actions.setOpaque(false);
        ModernButton saveBtn = UIHelper.createSuccessButton("Guardar");
        saveBtn.setIcon(UIHelper.icon("fas-save", 14));
        actions.add(saveBtn);
        card.add(actions, BorderLayout.SOUTH);

        panel.add(card, BorderLayout.CENTER);

        saveBtn.addActionListener(e -> saveDocumentColumns());
        return panel;
    }

    private JCheckBox columnCheckBox(String label) {
        JCheckBox box = new JCheckBox(label);
        box.setOpaque(false);
        box.setForeground(UIHelper.TEXT_LIGHT);
        return box;
    }

    private mz.multicore.erp.modules.documents.model.DocumentType selectedDocType() {
        int idx = docTypeCombo == null ? 0 : Math.max(0, docTypeCombo.getSelectedIndex());
        return mz.multicore.erp.modules.documents.model.DocumentType.values()[idx];
    }

    private void loadDocumentColumns() {
        if (documentConfigApiClient == null || colBarcode == null) {
            return;
        }
        var documentType = selectedDocType();
        Long companyId = CurrentUserContext.getCurrentCompanyId();
        UIHelper.loadAsync(this, () -> documentConfigApiClient.getColumns(companyId, documentType), this::applyDocumentColumns,
                error -> showConfigError("configuração das colunas", error));
    }

    private void applyDocumentColumns(DocumentColumnsDTO cols) {
        colBarcode.setSelected(cols.barcode());
        colReference.setSelected(cols.reference());
        colDescription.setSelected(cols.description());
        colExpiry.setSelected(cols.expiry());
        colQuantity.setSelected(cols.quantity());
        colUnitPrice.setSelected(cols.unitPrice());
        colTax.setSelected(cols.tax());
        colSubtotal.setSelected(cols.subtotal());
        footerField.setText(cols.footer() == null ? "" : cols.footer());
    }

    private void saveDocumentColumns() {
        DocumentColumnsDTO dto = new DocumentColumnsDTO(
                colBarcode.isSelected(),
                colReference.isSelected(),
                colDescription.isSelected(),
                colExpiry.isSelected(),
                colQuantity.isSelected(),
                colUnitPrice.isSelected(),
                colTax.isSelected(),
                colSubtotal.isSelected(),
                footerField.getText().trim().isEmpty() ? null : footerField.getText().trim()
        );
        var documentType = selectedDocType();
        Long companyId = CurrentUserContext.getCurrentCompanyId();
        UIHelper.runWithProgress(this, "A guardar configuração do documento…", () -> {
            documentConfigApiClient.save(companyId, documentType, dto);
            return null;
        }, ignored -> {
            JOptionPane.showMessageDialog(this, "Configuração de " + selectedDocType().label() + " guardada com sucesso.",
                    "Configuração Guardada", JOptionPane.INFORMATION_MESSAGE);
            loadAuditLogs();
        }, error -> showConfigError("configuração do documento", error));
    }

    public void onPanelSelected() {
        loadAuditLogs();
        loadBackupFilesList();
        refreshAutoBackupStatus();
        loadUsersList();
        loadDocumentColumns();
        loadSupportTickets();
        loadMySubscription();
    }

    /** Estado do backup físico automático (activo/última execução) para visibilidade + alerta. */
    private void refreshAutoBackupStatus() {
        if (backupAutoStatus == null) return;
        UIHelper.loadAsync(this, backupApiClient::status, this::applyAutoBackupStatus,
                error -> showConfigError("estado do backup", error));
    }

    private void applyAutoBackupStatus(BackupStatusDTO st) {
        String base = st.autoEnabled()
                ? "Backup automático: ACTIVO (diário)"
                : "Backup automático: desativado";
        if (st.lastTime() == null) {
            backupAutoStatus.setText(base + " — ainda sem execução nesta sessão.");
            backupAutoStatus.setForeground(UIHelper.TEXT_MUTED);
            return;
        }
        boolean ok = Boolean.TRUE.equals(st.lastSuccess());
        java.time.format.DateTimeFormatter f = java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
        backupAutoStatus.setText(base + " — última: " + st.lastTime().format(f)
                + (ok ? "  ✓ OK" : "  ✗ FALHOU"));
        backupAutoStatus.setForeground(ok ? UIHelper.APPROVED_GREEN : UIHelper.REJECTED_RED);
    }

    /** Executa já o backup físico automático (retenção + registo). Só ADMIN. */
    private void runAutoBackupNow() {
        if (!"ADMIN".equalsIgnoreCase(CurrentUserContext.getRole())) {
            JOptionPane.showMessageDialog(this, "Apenas administradores podem executar o backup.",
                    "Acesso restrito", JOptionPane.WARNING_MESSAGE);
            return;
        }
        UIHelper.runWithProgress(this, "A executar backup automático…",
                () -> backupApiClient.runAuto(),
                res -> {
                    backupLogArea.append((res.success() ? "[OK] " : "[FALHA] ") + res.message() + "\n");
                    refreshAutoBackupStatus();
                    loadBackupFilesList();
                },
                ex -> JOptionPane.showMessageDialog(this, ex.getMessage(), "Erro", JOptionPane.ERROR_MESSAGE));
    }

    private void loadAuditLogs() {
        Long companyId = CurrentUserContext.getCurrentCompanyId();
        UIHelper.loadAsync(this, () -> auditApiClient.getLogsByCompany(companyId), this::applyAuditLogs,
                error -> showConfigError("registo de auditoria", error));
    }

    private void applyAuditLogs(List<AuditLogDTO> logs) {
        auditTableModel.setRowCount(0);
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

        for (AuditLogDTO l : logs) {
            auditTableModel.addRow(new Object[]{
                    l.eventTime().format(dtf),
                    l.username(),
                    l.action(),
                    l.details()
            });
        }
    }

    private void runManualBackup() {
        String activeUser = CurrentUserContext.getUsername();
        String activeRole = CurrentUserContext.getRole();

        if (!"ADMIN".equalsIgnoreCase(activeRole)) {
            JOptionPane.showMessageDialog(this, "Apenas utilizadores com cargo ADMIN podem iniciar cópias de segurança manuais.", "Acesso Recusado", JOptionPane.ERROR_MESSAGE);
            return;
        }

        backupLogArea.append(">> A iniciar cópia de segurança manual (" + activeUser + ")...\n");
        UIHelper.runWithProgress(this, "A efectuar cópia de segurança…", backupApiClient::executeBackup, path -> {
            backupLogArea.append(">> Backup efetuado com sucesso!\n");
            backupLogArea.append(">> Destino: " + path + "\n");
            // A auditoria (BACKUP_MANUAL) é registada pelo servidor.
            JOptionPane.showMessageDialog(this, "Cópia de segurança gravada com sucesso em:\n" + path, "Backup Concluído", JOptionPane.INFORMATION_MESSAGE);
            loadBackupFilesList();
            loadAuditLogs();
        }, error -> {
            backupLogArea.append(">> ERRO: " + error.getMessage() + "\n");
            showConfigError("backup", error);
        });
    }

    private void runPhysicalBackup() {
        String activeUser = CurrentUserContext.getUsername();
        if (!"ADMIN".equalsIgnoreCase(CurrentUserContext.getRole())) {
            JOptionPane.showMessageDialog(this, "Apenas utilizadores com cargo ADMIN podem gerar backups físicos.", "Acesso Recusado", JOptionPane.ERROR_MESSAGE);
            return;
        }

        backupLogArea.append(">> A iniciar backup físico da base de dados (" + activeUser + ")...\n");
        UIHelper.runWithProgress(this, "A gerar backup físico da base de dados…",
                backupApiClient::executePhysical,
                result -> {
                    backupLogArea.append(">> Backup físico concluído!\n");
                    backupLogArea.append(">> Destino: " + result.filePath() + "\n");
                    backupLogArea.append(">> Base de dados: " + result.database() + " (" + (result.sizeBytes() / 1024) + " KB)\n");
                    JOptionPane.showMessageDialog(this, "Backup físico restaurável gravado em:\n" + result.filePath(),
                            "Backup Físico Concluído", JOptionPane.INFORMATION_MESSAGE);
                    loadAuditLogs();
                },
                error -> {
                    backupLogArea.append(">> ERRO: " + error.getMessage() + "\n");
                    JOptionPane.showMessageDialog(this, "Erro ao gerar backup físico: " + error.getMessage(), "Erro", JOptionPane.ERROR_MESSAGE);
                });
    }

    private void verifySelectedBackup() {
        if (!"ADMIN".equalsIgnoreCase(CurrentUserContext.getRole())) {
            JOptionPane.showMessageDialog(this, "Apenas utilizadores com cargo ADMIN podem verificar cópias de segurança.", "Acesso Recusado", JOptionPane.ERROR_MESSAGE);
            return;
        }

        int selectedRow = backupFilesTable.getSelectedRow();
        if (selectedRow < 0) {
            JOptionPane.showMessageDialog(this, "Selecione um ficheiro de backup no arquivo.", "Backup", JOptionPane.WARNING_MESSAGE);
            return;
        }

        String fileName = String.valueOf(backupFilesModel.getValueAt(selectedRow, 0));
        backupLogArea.append(">> A verificar backup: " + fileName + "\n");
        UIHelper.runWithProgress(this, "A verificar cópia de segurança…", () -> backupApiClient.verify(fileName), verification -> {
            backupLogArea.append(">> Backup válido para a empresa " + verification.companyId() + "\n");
            backupLogArea.append(">> Gerado em: " + verification.generatedAt() + "\n");
            backupLogArea.append(">> Secções verificadas: " + verification.totalSections() + "\n");
            backupLogArea.append(">> Registos: " + verification.itemCounts() + "\n");
            JOptionPane.showMessageDialog(this,
                    "Backup verificado com sucesso.\nFicheiro: " + verification.fileName(),
                    "Backup Válido",
                    JOptionPane.INFORMATION_MESSAGE);
            loadAuditLogs();
        }, error -> {
            backupLogArea.append(">> ERRO DE VERIFICAÇÃO: " + error.getMessage() + "\n");
            showConfigError("verificação do backup", error);
        });
    }

    private void loadBackupFilesList() {
        String prefix = "company_" + CurrentUserContext.getCurrentCompanyId() + "_backup_";
        UIHelper.loadAsync(this, backupApiClient::files, files -> {
            backupFilesModel.setRowCount(0);
            for (String name : files) {
                if (name.startsWith(prefix) && name.toLowerCase().endsWith(".json")) {
                    backupFilesModel.addRow(new Object[]{name, "—"});
                }
            }
        }, error -> showConfigError("arquivo de backups", error));
    }

    private void loadUsersList() {
        usersTableModel.setRowCount(0);
        if (!"ADMIN".equalsIgnoreCase(CurrentUserContext.getRole())) {
            usersTableModel.addRow(new Object[]{
                    "Acesso restrito", "Apenas administradores podem gerir utilizadores.", "", ""
            });
            return;
        }
        UIHelper.loadAsync(this, userApiClient::getAllUsers, this::applyUsers,
                error -> showConfigError("utilizadores", error));
    }

    private void applyUsers(List<AppUserDTO> users) {
        usersTableModel.setRowCount(0);
        for (AppUserDTO u : users) {
            usersTableModel.addRow(new Object[]{
                    u.username(),
                    u.name(),
                    u.role(),
                    u.active() ? "ATIVO" : "INATIVO"
            });
        }
    }

    /** Criação de utilizador em modal profissional. */
    private void registerUser() {
        JTextField usernameField = new JTextField();
        JTextField fullNameField = new JTextField();
        JPasswordField passwordField = new JPasswordField();
        JComboBox<String> roleCombo = new JComboBox<>(USER_ROLES);
        UIHelper.styleComboBox(roleCombo);
        UIHelper.humanizeRoleCombo(roleCombo);

        JPanel form = UIHelper.createDialogForm(
                "Username:", usernameField,
                "Função / Cargo:", roleCombo,
                "Nome Completo:", fullNameField,
                "Palavra-Passe:", passwordField
        );

        ModernFormDialog dlg = new ModernFormDialog(UIHelper.mainWindow, "Criar Novo Utilizador",
                "fas-user-plus", "Conta de acesso ao sistema", form).setConfirmButton("Registar", "fas-user-plus");
        dlg.setOnSaveAsync(() -> {
            String username = usernameField.getText().trim();
            String fullName = fullNameField.getText().trim();
            String password = new String(passwordField.getPassword()).trim();
            String role = (String) roleCombo.getSelectedItem();
            if (username.isEmpty() || fullName.isEmpty() || password.isEmpty()) {
                throw new IllegalArgumentException("Todos os campos são obrigatórios.");
            }
            return () -> userApiClient.createUser(username, fullName, password, role);
        });

        if (dlg.showDialog()) {
            JOptionPane.showMessageDialog(this, "Utilizador criado com sucesso!", "Sucesso", JOptionPane.INFORMATION_MESSAGE);
            loadUsersList();
        }
    }

    /** Alteração de perfil do utilizador seleccionado, em modal profissional. */
    private void updateSelectedUserRole() {
        int selectedRow = TableFilter.selectedModelRow(usersTable);
        if (selectedRow < 0) {
            JOptionPane.showMessageDialog(this, "Selecione um utilizador na lista.", "Perfil", JOptionPane.WARNING_MESSAGE);
            return;
        }

        String username = String.valueOf(usersTableModel.getValueAt(selectedRow, 0));
        JComboBox<String> roleCombo = new JComboBox<>(USER_ROLES);
        UIHelper.styleComboBox(roleCombo);
        UIHelper.humanizeRoleCombo(roleCombo);
        roleCombo.setSelectedItem(String.valueOf(usersTableModel.getValueAt(selectedRow, 2)));

        JPanel form = UIHelper.createDialogForm("Novo Perfil:", roleCombo);
        ModernFormDialog dlg = new ModernFormDialog(UIHelper.mainWindow, "Alterar Perfil",
                "fas-user-shield", "Utilizador: " + username, form).setConfirmButton("Alterar", "fas-check");
        dlg.setOnSaveAsync(() -> {
            String role = (String) roleCombo.getSelectedItem();
            return () -> userApiClient.updateCompanyRole(username, role);
        });

        if (dlg.showDialog()) {
            JOptionPane.showMessageDialog(this, "Perfil de '" + username + "' atualizado nesta empresa.",
                    "Perfil Atualizado", JOptionPane.INFORMATION_MESSAGE);
            loadUsersList();
        }
    }

    // ------------------------------------------------------------- TAB 5: Suporte à Plataforma

    private static final String[] TICKET_PRIORITIES = {"LOW", "NORMAL", "HIGH", "URGENT"};

    private JPanel createSupportTab() { return supportPanel.buildPanel(); }

    private void loadSupportTickets() { supportPanel.refresh(); }

    private JPanel createSubscriptionTab() {
        JPanel panel = new JPanel(new BorderLayout(0, 15));
        panel.setBackground(UIHelper.BG_DARK);
        panel.setBorder(new EmptyBorder(15, 15, 15, 15));

        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        header.add(UIHelper.createHeading("Estado da Assinatura"), BorderLayout.WEST);
        ModernButton refreshBtn = UIHelper.createSecondaryButton("Actualizar");
        refreshBtn.setIcon(UIHelper.icon("fas-sync-alt", 14));
        refreshBtn.addActionListener(e -> loadMySubscription());
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        actions.setOpaque(false);
        actions.add(refreshBtn);
        header.add(actions, BorderLayout.EAST);
        panel.add(header, BorderLayout.NORTH);

        subscriptionCard = new ModernPanel(16);
        subscriptionCard.setLayout(new GridBagLayout());
        subscriptionCard.setBorder(new EmptyBorder(24, 24, 24, 24));
        JPanel wrap = new JPanel(new BorderLayout());
        wrap.setOpaque(false);
        wrap.add(subscriptionCard, BorderLayout.NORTH);
        panel.add(wrap, BorderLayout.CENTER);

        return panel;
    }

    private void loadMySubscription() {
        if (subscriptionCard == null) return;
        UIHelper.loadAsync(this, mySubscriptionApiClient::getMySubscription, this::applyMySubscription,
                error -> showSubscriptionError(error.getMessage()));
    }

    private void applyMySubscription(MySubscriptionDTO sub) {
        subscriptionCard.removeAll();
            if (!sub.hasSubscription()) {
                subscriptionCard.add(bigInfo("Sem assinatura definida",
                        "A sua empresa ainda não tem um plano associado. Contacte o suporte da plataforma.",
                        UIHelper.TEXT_MUTED));
            } else {
                subscriptionCard.setLayout(new GridLayout(0, 2, 24, 14));
                subscriptionCard.add(field("Empresa", sub.companyName()));
                subscriptionCard.add(field("Plano", sub.planLabel()));
                subscriptionCard.add(field("Estado", sub.statusLabel(), statusColor(sub.status())));
                subscriptionCard.add(field("Válida até",
                        sub.validUntil() == null ? "—" : sub.validUntil().toString()));
                subscriptionCard.add(field("Dias restantes", daysText(sub.daysRemaining()),
                        daysColor(sub.daysRemaining())));
                subscriptionCard.add(field("Mensalidade",
                        sub.monthlyPrice() == null ? "—" : sub.monthlyPrice().toPlainString() + " MT"));
            }
        subscriptionCard.revalidate();
        subscriptionCard.repaint();
    }

    private void showSubscriptionError(String message) {
        subscriptionCard.removeAll();
        subscriptionCard.setLayout(new GridBagLayout());
        subscriptionCard.add(bigInfo("Não foi possível carregar a assinatura", message, UIHelper.REJECTED_RED));
        subscriptionCard.revalidate();
        subscriptionCard.repaint();
    }

    void showConfigError(String area, Throwable error) {
        JOptionPane.showMessageDialog(this, "Não foi possível processar " + area + ": " + error.getMessage(),
                "Erro", JOptionPane.ERROR_MESSAGE);
    }

    private String daysText(Long days) {
        if (days == null) return "—";
        if (days < 0) return "Expirada há " + Math.abs(days) + " dia(s)";
        if (days == 0) return "Expira hoje";
        return days + " dia(s)";
    }

    private Color daysColor(Long days) {
        if (days == null) return UIHelper.TEXT_LIGHT;
        if (days < 0) return UIHelper.REJECTED_RED;
        if (days <= 7) return UIHelper.PENDING_YELLOW;
        return UIHelper.APPROVED_GREEN;
    }

    private Color statusColor(String status) {
        if ("ACTIVE".equals(status) || "TRIAL".equals(status)) return UIHelper.APPROVED_GREEN;
        if ("EXPIRED".equals(status) || "SUSPENDED".equals(status)) return UIHelper.REJECTED_RED;
        return UIHelper.TEXT_LIGHT;
    }

    private JPanel field(String label, String value) {
        return field(label, value, UIHelper.TEXT_LIGHT);
    }

    private JPanel field(String label, String value, Color valueColor) {
        JPanel p = new JPanel();
        p.setOpaque(false);
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        JLabel l = new JLabel(label);
        l.setFont(new Font(UIHelper.FONT, Font.BOLD, 11));
        l.setForeground(UIHelper.TEXT_MUTED);
        JLabel v = new JLabel(value == null ? "—" : value);
        v.setFont(new Font(UIHelper.FONT, Font.BOLD, 18));
        v.setForeground(valueColor);
        p.add(l);
        p.add(v);
        return p;
    }

    private JPanel bigInfo(String title, String detail, Color color) {
        JPanel p = new JPanel();
        p.setOpaque(false);
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        JLabel t = new JLabel(title);
        t.setFont(new Font(UIHelper.FONT, Font.BOLD, 16));
        t.setForeground(color);
        JLabel d = new JLabel("<html><body style='width:360px'>" + (detail == null ? "" : detail) + "</body></html>");
        d.setFont(new Font(UIHelper.FONT, Font.PLAIN, 12));
        d.setForeground(UIHelper.TEXT_MUTED);
        p.add(t);
        p.add(javax.swing.Box.createVerticalStrut(6));
        p.add(d);
        return p;
    }
}
