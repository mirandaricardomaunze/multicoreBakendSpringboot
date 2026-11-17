package mz.multicore.erp.gui;

import mz.multicore.erp.gui.components.*;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;

/**
 * Gestão de cópias de segurança (backups lógicos, físicos e automáticos),
 * consola de auditoria de operações e arquivo de snapshots.
 */
final class ConfigBackupsPanel {

    private final ConfigPanel owner;
    private JTextArea backupLogArea;
    private JLabel backupAutoStatus;
    private DefaultTableModel backupFilesModel;
    private JTable backupFilesTable;

    ConfigBackupsPanel(ConfigPanel owner) { this.owner = owner; }

    public JPanel buildPanel() {
        JPanel panel = new JPanel(new GridLayout(1, 2, 20, 0));
        panel.setBackground(UIHelper.BG_DARK);
        panel.setBorder(new EmptyBorder(Spacing.LG, Spacing.LG, Spacing.LG, Spacing.LG));

        JPanel leftPanel = new JPanel(new BorderLayout(0, 15));
        leftPanel.setOpaque(false);
        leftPanel.add(UIHelper.createHeading("Gestão de Cópias de Segurança"), BorderLayout.NORTH);

        ModernPanel consoleCard = new ModernPanel(16);
        consoleCard.setLayout(new BorderLayout(0, 15));
        consoleCard.setBorder(new EmptyBorder(Spacing.XL, Spacing.XL, Spacing.XL, Spacing.XL));

        JLabel desc = new JLabel("<html><body>O <b>cópia lógica (.json)</b> é um retrato de verificação por empresa (auditoria). Para recuperação use a <b>cópia físico (BD)</b>, restaurável via pg_dump/pg_restore. O <b>backup automático</b> corre diariamente e apaga cópias antigas conforme retenção.</body></html>");
        desc.setFont(Typography.EMPHASIS);
        desc.setForeground(UIHelper.TEXT_MUTED);

        backupAutoStatus = new JLabel(" ");
        backupAutoStatus.setFont(Typography.BODY_BOLD);
        backupAutoStatus.setBorder(new EmptyBorder(Spacing.SM, 0, 0, 0));

        JPanel northInfo = new JPanel(new BorderLayout(0, 4));
        northInfo.setOpaque(false);
        northInfo.add(desc, BorderLayout.NORTH);
        northInfo.add(backupAutoStatus, BorderLayout.SOUTH);
        consoleCard.add(northInfo, BorderLayout.NORTH);

        backupLogArea = new JTextArea();
        backupLogArea.setBackground(UIHelper.BG_DARK);
        backupLogArea.setForeground(UIHelper.APPROVED_GREEN);
        backupLogArea.setFont(Typography.MONO);
        backupLogArea.setEditable(false);
        backupLogArea.setMargin(new Insets(10, 10, 10, 10));
        consoleCard.add(new JScrollPane(backupLogArea), BorderLayout.CENTER);

        ActionMenuButton createBackupBtn = UIHelper.createActionMenuButton("Criar cópia")
                .addAction("Cópia lógica (.json)", UIHelper.icon("fas-file-code", 14), () -> ConfigBackupOperations.runManualBackup(owner, backupLogArea, backupFilesModel))
                .addAction("Cópia física (BD)", UIHelper.icon("fas-database", 14), () -> ConfigBackupOperations.runPhysicalBackup(owner, backupLogArea))
                .addAction("Cópia automática agora", UIHelper.icon("fas-clock", 14), () -> ConfigBackupOperations.runAutoBackupNow(owner, backupLogArea, backupAutoStatus, backupFilesModel));
        JPanel backupActions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        backupActions.setOpaque(false);
        backupActions.add(createBackupBtn);
        consoleCard.add(backupActions, BorderLayout.SOUTH);
        leftPanel.add(consoleCard, BorderLayout.CENTER);
        panel.add(leftPanel);

        JPanel rightPanel = new JPanel(new BorderLayout(0, 15));
        rightPanel.setOpaque(false);
        ModernPanel archiveCard = new ModernPanel(16);
        archiveCard.setLayout(new BorderLayout(0, 10));
        archiveCard.setBorder(new EmptyBorder(Spacing.XL, Spacing.XL, Spacing.XL, Spacing.XL));

        backupFilesModel = new DefaultTableModel(new String[]{"Nome do Ficheiro", "Tamanho (KB)"}, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        backupFilesTable = new JTable(backupFilesModel);
        UIHelper.styleTable(backupFilesTable);
        UIHelper.ensureHeadersFit(backupFilesTable);
        JScrollPane archiveScroll = new JScrollPane(backupFilesTable);
        UIHelper.styleScrollPane(archiveScroll);

        ModernButton verifyBackupBtn = UIHelper.createPrimaryButton("Verificar Cópia");
        verifyBackupBtn.setIcon(UIHelper.icon("fas-shield-alt", 14));
        verifyBackupBtn.addActionListener(e -> ConfigBackupOperations.verifySelectedBackup(owner, backupLogArea, backupFilesTable, backupFilesModel));

        ModernButton refreshArchiveBtn = UIHelper.createRefreshButton(this::loadBackupFilesList);
        archiveCard.add(UIHelper.tableCardTop("Cópias Disponíveis", null,
                refreshArchiveBtn, verifyBackupBtn), BorderLayout.NORTH);
        archiveCard.add(archiveScroll, BorderLayout.CENTER);

        TableQuickPeekController.install(backupFilesTable, archiveCard,
                (peek, row) -> ConfigBackupOperations.renderQuickPeek(peek, row, backupFilesModel, verifyBackupBtn::doClick));

        rightPanel.add(archiveCard, BorderLayout.CENTER);
        panel.add(rightPanel);
        return panel;
    }

    public void refresh() {
        loadBackupFilesList();
        refreshAutoBackupStatus();
    }

    void refreshAutoBackupStatus() {
        ConfigBackupOperations.refreshAutoBackupStatus(owner, backupAutoStatus);
    }

    void loadBackupFilesList() {
        ConfigBackupOperations.loadBackupFilesList(owner, backupFilesModel);
    }
}
