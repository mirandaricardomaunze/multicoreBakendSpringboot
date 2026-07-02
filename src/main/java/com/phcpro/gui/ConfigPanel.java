package com.phcpro.gui;

import com.phcpro.architecture.security.CurrentUserContext;
import com.phcpro.gui.components.ModernButton;
import com.phcpro.gui.components.ModernFormDialog;
import com.phcpro.gui.components.ModernPanel;
import com.phcpro.gui.components.Theme;
import com.phcpro.gui.components.UIHelper;
import com.phcpro.modules.users.model.AppUser;
import com.phcpro.modules.users.service.AppUserService;
import com.phcpro.modules.audit.model.AuditLog;
import com.phcpro.modules.audit.service.AuditLogService;
import com.phcpro.modules.backup.dto.BackupVerificationDTO;
import com.phcpro.modules.backup.dto.PhysicalBackupResultDTO;
import com.phcpro.modules.backup.service.BackupService;
import com.phcpro.modules.backup.service.DatabaseBackupService;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.File;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class ConfigPanel extends JPanel {

    private final AppUserService userService;
    private final AuditLogService auditLogService;
    private final BackupService backupService;
    private final DatabaseBackupService databaseBackupService;

    // TAB 1: AUDIT LOGS
    private DefaultTableModel auditTableModel;
    private JTable auditTable;

    // TAB 2: BACKUPS
    private JTextArea backupLogArea;
    private DefaultTableModel backupFilesModel;
    private JTable backupFilesTable;

    // TAB 3: USERS
    private static final String[] USER_ROLES = {"EMPLOYEE", "MANAGER", "ADMIN"};
    private DefaultTableModel usersTableModel;
    private JTable usersTable;

    public ConfigPanel(AppUserService userService, AuditLogService auditLogService, BackupService backupService,
                       DatabaseBackupService databaseBackupService) {
        this.userService = userService;
        this.auditLogService = auditLogService;
        this.backupService = backupService;
        this.databaseBackupService = databaseBackupService;

        setLayout(new BorderLayout());
        setBackground(UIHelper.BG_DARK);
        setBorder(new EmptyBorder(10, 10, 10, 10));

        add(buildAppearanceBar(), BorderLayout.NORTH);

        JTabbedPane tabbedPane = new JTabbedPane();
        UIHelper.styleTabbedPane(tabbedPane);

        // TAB 1: AUDIT LOGS
        JPanel tabAudit = createAuditTab();
        tabbedPane.addTab("Log de Auditoria Geral", UIHelper.icon("fas-clipboard-list", 16, UIHelper.TEXT_LIGHT), tabAudit);

        // TAB 2: BACKUPS
        JPanel tabBackups = createBackupsTab();
        tabbedPane.addTab("Cópias de Segurança & Backups", UIHelper.icon("fas-database", 16, UIHelper.TEXT_LIGHT), tabBackups);

        // TAB 3: USERS
        JPanel tabUsers = createUsersTab();
        tabbedPane.addTab("Utilizadores & Permissões", UIHelper.icon("fas-user-shield", 16, UIHelper.TEXT_LIGHT), tabUsers);

        add(tabbedPane, BorderLayout.CENTER);

        onPanelSelected();
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
        card.add(scroll, BorderLayout.CENTER);

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        btnPanel.setOpaque(false);
        ModernButton refreshBtn = UIHelper.createSecondaryButton("Atualizar Registos");
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

        JLabel desc = new JLabel("<html><body>O <b>backup lógico (.json)</b> é um snapshot de verificação por empresa (auditoria). Para recuperação de desastres use o <b>backup físico (BD)</b>, restaurável com fidelidade total via pg_dump/pg_restore. Os backups lógicos automáticos ocorrem a cada faturação.</body></html>");
        desc.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        desc.setForeground(UIHelper.TEXT_MUTED);
        consoleCard.add(desc, BorderLayout.NORTH);

        backupLogArea = new JTextArea();
        backupLogArea.setBackground(new Color(15, 23, 42)); // darker console bg
        backupLogArea.setForeground(new Color(34, 197, 94)); // green code style
        backupLogArea.setFont(new Font("Consolas", Font.PLAIN, 12));
        backupLogArea.setEditable(false);
        backupLogArea.setMargin(new Insets(10, 10, 10, 10));
        JScrollPane scrollConsole = new JScrollPane(backupLogArea);
        UIHelper.styleScrollPane(scrollConsole);
        consoleCard.add(scrollConsole, BorderLayout.CENTER);

        ModernButton runBackupBtn = UIHelper.createSuccessButton("Backup Lógico (.json)");
        runBackupBtn.setIcon(UIHelper.icon("fas-file-code", 14));
        ModernButton runPhysicalBtn = UIHelper.createPrimaryButton("Backup Físico (BD)");
        runPhysicalBtn.setIcon(UIHelper.icon("fas-database", 14));
        JPanel backupActions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        backupActions.setOpaque(false);
        backupActions.add(runBackupBtn);
        backupActions.add(runPhysicalBtn);
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
        ModernButton refreshArchiveBtn = UIHelper.createSecondaryButton("Atualizar Arquivo");
        refreshArchiveBtn.setIcon(UIHelper.icon("fas-sync-alt", 14));
        actionRow.add(verifyBackupBtn);
        actionRow.add(refreshArchiveBtn);
        archiveCard.add(actionRow, BorderLayout.SOUTH);

        rightPanel.add(archiveCard, BorderLayout.CENTER);
        panel.add(rightPanel);

        // LISTENERS
        runBackupBtn.addActionListener(e -> runManualBackup());
        runPhysicalBtn.addActionListener(e -> runPhysicalBackup());
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
        ModernButton updateRoleBtn = UIHelper.createPrimaryButton("Alterar Perfil");
        updateRoleBtn.setIcon(UIHelper.icon("fas-user-shield", 14));
        ModernButton refreshUsersBtn = UIHelper.createSecondaryButton("Atualizar Lista");
        refreshUsersBtn.setIcon(UIHelper.icon("fas-sync-alt", 14));
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        actions.setOpaque(false);
        actions.add(refreshUsersBtn);
        actions.add(updateRoleBtn);
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
        JScrollPane scroll = new JScrollPane(usersTable);
        UIHelper.styleScrollPane(scroll);
        listCard.add(scroll, BorderLayout.CENTER);
        panel.add(listCard, BorderLayout.CENTER);

        // LISTENERS
        newUserBtn.addActionListener(e -> registerUser());
        updateRoleBtn.addActionListener(e -> updateSelectedUserRole());
        refreshUsersBtn.addActionListener(e -> loadUsersList());

        return panel;
    }

    public void onPanelSelected() {
        loadAuditLogs();
        loadBackupFilesList();
        loadUsersList();
    }

    private void loadAuditLogs() {
        auditTableModel.setRowCount(0);
        Long companyId = CurrentUserContext.getCurrentCompanyId();
        List<AuditLog> logs = auditLogService.getLogsByCompany(companyId);
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

        for (AuditLog l : logs) {
            auditTableModel.addRow(new Object[]{
                    l.getEventTime().format(dtf),
                    l.getUsername(),
                    l.getAction(),
                    l.getDetails()
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
        try {
            String path = backupService.executeBackup();
            backupLogArea.append(">> Backup efetuado com sucesso!\n");
            backupLogArea.append(">> Destino: " + path + "\n");
            
            // Log backup event
            auditLogService.logEvent(activeUser, CurrentUserContext.getCurrentCompanyId(), "BACKUP_MANUAL", "Cópia de segurança gerada: " + path);
            
            JOptionPane.showMessageDialog(this, "Cópia de segurança gravada com sucesso em:\n" + path, "Backup Concluído", JOptionPane.INFORMATION_MESSAGE);
            loadBackupFilesList();
            loadAuditLogs();
        } catch (Exception ex) {
            backupLogArea.append(">> ERRO: " + ex.getMessage() + "\n");
            JOptionPane.showMessageDialog(this, "Erro ao efetuar backup: " + ex.getMessage(), "Erro", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void runPhysicalBackup() {
        String activeUser = CurrentUserContext.getUsername();
        if (!"ADMIN".equalsIgnoreCase(CurrentUserContext.getRole())) {
            JOptionPane.showMessageDialog(this, "Apenas utilizadores com cargo ADMIN podem gerar backups físicos.", "Acesso Recusado", JOptionPane.ERROR_MESSAGE);
            return;
        }

        backupLogArea.append(">> A iniciar backup físico da base de dados (" + activeUser + ")...\n");
        UIHelper.runWithProgress(this, "A gerar backup físico da base de dados…",
                databaseBackupService::executePhysicalBackup,
                result -> {
                    backupLogArea.append(">> Backup físico concluído!\n");
                    backupLogArea.append(">> Destino: " + result.filePath() + "\n");
                    backupLogArea.append(">> Base de dados: " + result.database() + " (" + (result.sizeBytes() / 1024) + " KB)\n");
                    auditLogService.logEvent(activeUser, CurrentUserContext.getCurrentCompanyId(), "BACKUP_FISICO",
                            "Backup físico gerado: " + result.filePath());
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
        File backupFile = new File("backups", fileName);
        backupLogArea.append(">> A verificar backup: " + fileName + "\n");
        try {
            BackupVerificationDTO verification = backupService.verifyBackup(backupFile.getPath());
            backupLogArea.append(">> Backup válido para a empresa " + verification.companyId() + "\n");
            backupLogArea.append(">> Gerado em: " + verification.generatedAt() + "\n");
            backupLogArea.append(">> Secções verificadas: " + verification.totalSections() + "\n");
            backupLogArea.append(">> Registos: " + verification.itemCounts() + "\n");
            auditLogService.logEvent(CurrentUserContext.getUsername(),
                    CurrentUserContext.getCurrentCompanyId(),
                    "BACKUP_VERIFY",
                    "Backup verificado: " + verification.fileName());
            JOptionPane.showMessageDialog(this,
                    "Backup verificado com sucesso.\nFicheiro: " + verification.fileName(),
                    "Backup Válido",
                    JOptionPane.INFORMATION_MESSAGE);
            loadAuditLogs();
        } catch (Exception ex) {
            backupLogArea.append(">> ERRO DE VERIFICAÇÃO: " + ex.getMessage() + "\n");
            JOptionPane.showMessageDialog(this, "Erro ao verificar backup: " + ex.getMessage(), "Erro", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void loadBackupFilesList() {
        backupFilesModel.setRowCount(0);
        File backupsDir = new File("backups");
        if (backupsDir.exists() && backupsDir.isDirectory()) {
            String prefix = "company_" + CurrentUserContext.getCurrentCompanyId() + "_backup_";
            File[] files = backupsDir.listFiles((dir, name) ->
                    name.startsWith(prefix) && name.toLowerCase().endsWith(".json"));
            if (files != null) {
                for (File f : files) {
                    double kbSize = (double) f.length() / 1024.0;
                    backupFilesModel.addRow(new Object[]{
                            f.getName(),
                            String.format("%.2f KB", kbSize)
                    });
                }
            }
        }
    }

    private void loadUsersList() {
        usersTableModel.setRowCount(0);
        if (!"ADMIN".equalsIgnoreCase(CurrentUserContext.getRole())) {
            usersTableModel.addRow(new Object[]{
                    "Acesso restrito", "Apenas administradores podem gerir utilizadores.", "", ""
            });
            return;
        }
        List<AppUser> users = userService.getAllUsers();
        for (AppUser u : users) {
            usersTableModel.addRow(new Object[]{
                    u.getUsername(),
                    u.getName(),
                    u.getRoleForCompany(CurrentUserContext.getCurrentCompanyId()),
                    u.isActive() ? "ATIVO" : "INATIVO"
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

        JPanel form = UIHelper.createDialogForm(
                "Username:", usernameField,
                "Função / Cargo:", roleCombo,
                "Nome Completo:", fullNameField,
                "Palavra-Passe:", passwordField
        );

        ModernFormDialog dlg = new ModernFormDialog(UIHelper.mainWindow, "Criar Novo Utilizador",
                "fas-user-plus", "Conta de acesso ao sistema", form).setConfirmButton("Registar", "fas-user-plus");
        dlg.setOnSave(() -> {
            String username = usernameField.getText().trim();
            String fullName = fullNameField.getText().trim();
            String password = new String(passwordField.getPassword()).trim();
            String role = (String) roleCombo.getSelectedItem();
            if (username.isEmpty() || fullName.isEmpty() || password.isEmpty()) {
                throw new IllegalArgumentException("Todos os campos são obrigatórios.");
            }
            userService.createUser(username, fullName, password, role);
        });

        if (dlg.showDialog()) {
            JOptionPane.showMessageDialog(this, "Utilizador criado com sucesso!", "Sucesso", JOptionPane.INFORMATION_MESSAGE);
            loadUsersList();
        }
    }

    /** Alteração de perfil do utilizador seleccionado, em modal profissional. */
    private void updateSelectedUserRole() {
        int selectedRow = usersTable.getSelectedRow();
        if (selectedRow < 0) {
            JOptionPane.showMessageDialog(this, "Selecione um utilizador na lista.", "Perfil", JOptionPane.WARNING_MESSAGE);
            return;
        }

        String username = String.valueOf(usersTableModel.getValueAt(selectedRow, 0));
        JComboBox<String> roleCombo = new JComboBox<>(USER_ROLES);
        UIHelper.styleComboBox(roleCombo);
        roleCombo.setSelectedItem(String.valueOf(usersTableModel.getValueAt(selectedRow, 2)));

        JPanel form = UIHelper.createDialogForm("Novo Perfil:", roleCombo);
        ModernFormDialog dlg = new ModernFormDialog(UIHelper.mainWindow, "Alterar Perfil",
                "fas-user-shield", "Utilizador: " + username, form).setConfirmButton("Alterar", "fas-check");
        dlg.setOnSave(() -> userService.updateCompanyRole(username, (String) roleCombo.getSelectedItem()));

        if (dlg.showDialog()) {
            JOptionPane.showMessageDialog(this, "Perfil de '" + username + "' atualizado nesta empresa.",
                    "Perfil Atualizado", JOptionPane.INFORMATION_MESSAGE);
            loadUsersList();
        }
    }
}
