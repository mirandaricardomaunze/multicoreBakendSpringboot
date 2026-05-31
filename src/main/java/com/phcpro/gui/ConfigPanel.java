package com.phcpro.gui;

import com.phcpro.architecture.security.CurrentUserContext;
import com.phcpro.gui.components.ModernButton;
import com.phcpro.gui.components.ModernPanel;
import com.phcpro.gui.components.UIHelper;
import com.phcpro.modules.users.model.AppUser;
import com.phcpro.modules.users.service.AppUserService;
import com.phcpro.modules.audit.model.AuditLog;
import com.phcpro.modules.audit.service.AuditLogService;
import com.phcpro.modules.backup.service.BackupService;

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

    // TAB 1: AUDIT LOGS
    private DefaultTableModel auditTableModel;
    private JTable auditTable;

    // TAB 2: BACKUPS
    private JTextArea backupLogArea;
    private DefaultTableModel backupFilesModel;
    private JTable backupFilesTable;

    // TAB 3: USERS
    private JTextField usernameField;
    private JTextField fullNameField;
    private JPasswordField passwordField;
    private JComboBox<String> roleCombo;
    private DefaultTableModel usersTableModel;
    private JTable usersTable;

    public ConfigPanel(AppUserService userService, AuditLogService auditLogService, BackupService backupService) {
        this.userService = userService;
        this.auditLogService = auditLogService;
        this.backupService = backupService;

        setLayout(new BorderLayout());
        setBackground(UIHelper.BG_DARK);
        setBorder(new EmptyBorder(10, 10, 10, 10));

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
        ModernButton refreshBtn = new ModernButton("Atualizar Registos", new Color(75, 85, 99), new Color(107, 114, 128));
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

        JLabel desc = new JLabel("<html><body>O sistema ERP grava dados em base de dados em memória. Para evitar perdas de informação, gere backups de segurança regulares em ficheiros de formato JSON. Os backups automáticos ocorrem a cada faturação.</body></html>");
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

        ModernButton runBackupBtn = new ModernButton("Executar Backup Manual Agora");
        runBackupBtn.setIcon(UIHelper.icon("fas-database", 14));
        runBackupBtn.setGradient(UIHelper.APPROVED_GREEN, UIHelper.APPROVED_GREEN.darker());
        consoleCard.add(runBackupBtn, BorderLayout.SOUTH);

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
        ModernButton refreshArchiveBtn = new ModernButton("Atualizar Arquivo", new Color(75, 85, 99), new Color(107, 114, 128));
        refreshArchiveBtn.setIcon(UIHelper.icon("fas-sync-alt", 14));
        actionRow.add(refreshArchiveBtn);
        archiveCard.add(actionRow, BorderLayout.SOUTH);

        rightPanel.add(archiveCard, BorderLayout.CENTER);
        panel.add(rightPanel);

        // LISTENERS
        runBackupBtn.addActionListener(e -> runManualBackup());
        refreshArchiveBtn.addActionListener(e -> loadBackupFilesList());

        return panel;
    }

    private JPanel createUsersTab() {
        JPanel panel = new JPanel(new GridLayout(1, 2, 20, 0));
        panel.setBackground(UIHelper.BG_DARK);
        panel.setBorder(new EmptyBorder(15, 15, 15, 15));

        // LEFT: NEW USER FORM
        JPanel leftPanel = new JPanel(new GridBagLayout());
        leftPanel.setOpaque(false);

        ModernPanel formCard = new ModernPanel(16);
        formCard.setPreferredSize(new Dimension(420, 460));
        formCard.setLayout(new GridBagLayout());
        formCard.setBorder(new EmptyBorder(25, 25, 25, 25));

        GridBagConstraints cardGbc = new GridBagConstraints();
        cardGbc.fill = GridBagConstraints.HORIZONTAL;
        cardGbc.weightx = 1.0;

        cardGbc.gridx = 0; cardGbc.gridy = 0; cardGbc.gridwidth = 2;
        cardGbc.insets = new Insets(0, 8, 12, 8);
        JLabel formTitle = UIHelper.createSubheading("Criar Novo Utilizador");
        formTitle.setHorizontalAlignment(SwingConstants.CENTER);
        formCard.add(formTitle, cardGbc);

        // Username & Função (Side by Side)
        cardGbc.gridx = 0; cardGbc.gridy = 1; cardGbc.gridwidth = 1; cardGbc.weightx = 0.5;
        cardGbc.insets = new Insets(8, 8, 2, 8);
        JLabel userLbl = new JLabel("Username:");
        userLbl.setForeground(UIHelper.TEXT_MUTED);
        formCard.add(userLbl, cardGbc);

        cardGbc.gridx = 1;
        JLabel roleLbl = new JLabel("Função / Cargo:");
        roleLbl.setForeground(UIHelper.TEXT_MUTED);
        formCard.add(roleLbl, cardGbc);

        cardGbc.gridx = 0; cardGbc.gridy = 2;
        cardGbc.insets = new Insets(2, 8, 12, 8);
        usernameField = new JTextField();
        UIHelper.styleTextField(usernameField);
        formCard.add(usernameField, cardGbc);

        cardGbc.gridx = 1;
        String[] roles = {"EMPLOYEE", "MANAGER", "ADMIN"};
        roleCombo = new JComboBox<>(roles);
        UIHelper.styleComboBox(roleCombo);
        formCard.add(roleCombo, cardGbc);

        // Full Name (Full Width)
        cardGbc.gridx = 0; cardGbc.gridy = 3; cardGbc.gridwidth = 2; cardGbc.weightx = 1.0;
        cardGbc.insets = new Insets(8, 8, 2, 8);
        JLabel nameLbl = new JLabel("Nome Completo:");
        nameLbl.setForeground(UIHelper.TEXT_MUTED);
        formCard.add(nameLbl, cardGbc);

        cardGbc.gridy = 4;
        cardGbc.insets = new Insets(2, 8, 12, 8);
        fullNameField = new JTextField();
        UIHelper.styleTextField(fullNameField);
        formCard.add(fullNameField, cardGbc);

        // Password (Full Width)
        cardGbc.gridy = 5;
        cardGbc.insets = new Insets(8, 8, 2, 8);
        JLabel passLbl = new JLabel("Palavra-Passe:");
        passLbl.setForeground(UIHelper.TEXT_MUTED);
        formCard.add(passLbl, cardGbc);

        cardGbc.gridy = 6;
        cardGbc.insets = new Insets(2, 8, 12, 8);
        passwordField = new JPasswordField();
        UIHelper.stylePasswordField(passwordField);
        formCard.add(passwordField, cardGbc);

        // Register Button (Full Width)
        cardGbc.gridy = 7;
        cardGbc.insets = new Insets(20, 8, 8, 8);
        ModernButton saveUserBtn = new ModernButton("Registar Utilizador", UIHelper.ACCENT_BLUE, UIHelper.ACCENT_BLUE.brighter());
        saveUserBtn.setIcon(UIHelper.icon("fas-user-plus", 14));
        formCard.add(saveUserBtn, cardGbc);

        leftPanel.add(formCard);
        panel.add(leftPanel);

        // RIGHT: SYSTEM USERS LIST
        JPanel rightPanel = new JPanel(new BorderLayout(0, 15));
        rightPanel.setOpaque(false);
        rightPanel.add(UIHelper.createHeading("Utilizadores do Sistema"), BorderLayout.NORTH);

        ModernPanel listCard = new ModernPanel(16);
        listCard.setLayout(new BorderLayout(0, 10));
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

        JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        btnRow.setOpaque(false);
        ModernButton refreshUsersBtn = new ModernButton("Atualizar Lista", new Color(75, 85, 99), new Color(107, 114, 128));
        refreshUsersBtn.setIcon(UIHelper.icon("fas-sync-alt", 14));
        btnRow.add(refreshUsersBtn);
        listCard.add(btnRow, BorderLayout.SOUTH);

        rightPanel.add(listCard, BorderLayout.CENTER);
        panel.add(rightPanel);

        // LISTENERS
        saveUserBtn.addActionListener(e -> registerUser());
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

    private void loadBackupFilesList() {
        backupFilesModel.setRowCount(0);
        File backupsDir = new File("backups");
        if (backupsDir.exists() && backupsDir.isDirectory()) {
            File[] files = backupsDir.listFiles((dir, name) -> name.toLowerCase().endsWith(".json"));
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
        List<AppUser> users = userService.getAllUsers();
        for (AppUser u : users) {
            usersTableModel.addRow(new Object[]{
                    u.getUsername(),
                    u.getName(),
                    u.getRole(),
                    u.isActive() ? "ATIVO" : "INATIVO"
            });
        }
    }

    private void registerUser() {
        String username = usernameField.getText().trim();
        String fullName = fullNameField.getText().trim();
        String password = new String(passwordField.getPassword()).trim();
        String role = (String) roleCombo.getSelectedItem();

        if (username.isEmpty() || fullName.isEmpty() || password.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Todos os campos são obrigatórios para registar o utilizador.", "Erro", JOptionPane.ERROR_MESSAGE);
            return;
        }

        try {
            userService.createUser(username, fullName, password, role);
            JOptionPane.showMessageDialog(this, "Utilizador '" + username + "' criado com sucesso!", "Sucesso", JOptionPane.INFORMATION_MESSAGE);

            usernameField.setText("");
            fullNameField.setText("");
            passwordField.setText("");

            loadUsersList();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Erro ao criar utilizador: " + ex.getMessage(), "Erro", JOptionPane.ERROR_MESSAGE);
        }
    }
}
