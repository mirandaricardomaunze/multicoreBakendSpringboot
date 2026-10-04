package mz.multicore.erp.gui.components;

import mz.multicore.erp.desktop.client.BackupApiClient;
import mz.multicore.erp.modules.backup.dto.BackupStatusDTO;
import mz.multicore.erp.modules.backup.dto.BackupVerificationDTO;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Diálogo executivo para cópias de segurança (Backup) e verificação de integridade da base de dados.
 */
public class DatabaseBackupDialog extends JDialog {

    private static final DateTimeFormatter DT_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    private final BackupApiClient backupApiClient;
    private JLabel statusLabel;
    private JLabel lastRunLabel;
    private DefaultTableModel filesTableModel;
    private JTable filesTable;
    private ModernButton generateBtn;
    private ModernButton verifyBtn;
    private JLabel resultSummaryLabel;

    public DatabaseBackupDialog(Window owner, BackupApiClient backupApiClient) {
        super(owner, "Centro de Cópias de Segurança (Backup) e Integridade", ModalityType.APPLICATION_MODAL);
        this.backupApiClient = backupApiClient;

        setSize(720, 560);
        setLocationRelativeTo(owner);
        initComponents();
        loadData();
    }

    private void initComponents() {
        JPanel root = new JPanel(new BorderLayout(0, 16));
        root.setBackground(UIHelper.BG_DARK);
        root.setBorder(new EmptyBorder(20, 22, 20, 22));

        // Topo
        JPanel header = new JPanel(new BorderLayout(0, 6));
        header.setOpaque(false);
        JLabel title = UIHelper.createHeading("Cópias de Segurança & Integridade");
        title.setIcon(UIHelper.icon("fas-database", 18));
        JLabel subtitle = new JLabel("Geração de cópias completas dos dados e validação de arquivos.");
        subtitle.setFont(new Font(UIHelper.FONT, Font.PLAIN, 12));
        subtitle.setForeground(UIHelper.TEXT_MUTED);

        header.add(title, BorderLayout.NORTH);
        header.add(subtitle, BorderLayout.SOUTH);
        root.add(header, BorderLayout.NORTH);

        // Painel Central com Cartões
        JPanel center = new JPanel(new BorderLayout(0, 12));
        center.setOpaque(false);

        // Card 1: Estado e Ação Rápida
        ModernPanel quickCard = new ModernPanel(14);
        quickCard.setLayout(new BorderLayout(14, 0));
        quickCard.setBorder(new EmptyBorder(14, 16, 14, 16));

        JPanel statusBox = new JPanel();
        statusBox.setLayout(new BoxLayout(statusBox, BoxLayout.Y_AXIS));
        statusBox.setOpaque(false);

        statusLabel = new JLabel("Estado: A verificar...");
        statusLabel.setFont(new Font(UIHelper.FONT, Font.BOLD, 13));
        statusLabel.setForeground(UIHelper.TEXT_LIGHT);

        lastRunLabel = new JLabel("Último Backup Automático: --");
        lastRunLabel.setFont(new Font(UIHelper.FONT, Font.PLAIN, 12));
        lastRunLabel.setForeground(UIHelper.TEXT_MUTED);

        statusBox.add(statusLabel);
        statusBox.add(Box.createVerticalStrut(4));
        statusBox.add(lastRunLabel);
        quickCard.add(statusBox, BorderLayout.CENTER);

        generateBtn = UIHelper.createPrimaryButton("Criar Backup Agora");
        generateBtn.setIcon(UIHelper.icon("fas-download", 14));
        generateBtn.addActionListener(e -> generateBackup());
        quickCard.add(generateBtn, BorderLayout.EAST);

        center.add(quickCard, BorderLayout.NORTH);

        // Card 2: Ficheiros de Backup e Verificação
        ModernPanel tableCard = new ModernPanel(14);
        tableCard.setLayout(new BorderLayout(0, 10));
        tableCard.setBorder(new EmptyBorder(14, 16, 14, 16));

        JLabel tableTitle = new JLabel("Arquivos de Cópia de Segurança no Servidor:");
        tableTitle.setFont(new Font(UIHelper.FONT, Font.BOLD, 13));
        tableTitle.setForeground(UIHelper.TEXT_LIGHT);
        tableCard.add(tableTitle, BorderLayout.NORTH);

        String[] cols = {"Nome do Ficheiro", "Tipo", "Estado"};
        filesTableModel = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        filesTable = new JTable(filesTableModel);
        UIHelper.styleTable(filesTable);
        filesTable.setFillsViewportHeight(true);
        filesTable.putClientProperty("emptyText", "Nenhuma cópia de segurança encontrada.");

        JScrollPane scroll = new JScrollPane(filesTable);
        UIHelper.styleScrollPane(scroll);
        tableCard.add(scroll, BorderLayout.CENTER);

        JPanel tableActions = new JPanel(new BorderLayout(10, 0));
        tableActions.setOpaque(false);

        resultSummaryLabel = new JLabel("Selecione um arquivo para verificar.");
        resultSummaryLabel.setFont(new Font(UIHelper.FONT, Font.ITALIC, 12));
        resultSummaryLabel.setForeground(UIHelper.TEXT_MUTED);

        verifyBtn = UIHelper.createSecondaryButton("Verificar Integridade");
        verifyBtn.setIcon(UIHelper.icon("fas-shield-alt", 13));
        verifyBtn.addActionListener(e -> verifySelectedBackup());

        tableCard.add(UIHelper.actionsBar(verifyBtn), BorderLayout.NORTH);
        tableActions.add(resultSummaryLabel, BorderLayout.CENTER);
        tableCard.add(tableActions, BorderLayout.SOUTH);

        center.add(tableCard, BorderLayout.CENTER);
        root.add(center, BorderLayout.CENTER);

        // Rodapé
        JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        footer.setOpaque(false);
        ModernButton closeBtn = UIHelper.createSecondaryButton("Fechar");
        closeBtn.addActionListener(e -> dispose());
        footer.add(closeBtn);
        root.add(footer, BorderLayout.SOUTH);

        setContentPane(root);
    }

    private void loadData() {
        if (backupApiClient == null) return;
        setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));

        SwingWorker<Void, Void> worker = new SwingWorker<>() {
            private BackupStatusDTO status;
            private List<String> files;

            @Override
            protected Void doInBackground() {
                try {
                    status = backupApiClient.status();
                    files = backupApiClient.files();
                } catch (Exception ignored) {}
                return null;
            }

            @Override
            protected void done() {
                setCursor(Cursor.getDefaultCursor());
                if (status != null) {
                    statusLabel.setText("Agendamento: " + (status.autoEnabled() ? "Ativo (Diário)" : "Inativo"));
                    statusLabel.setForeground(status.autoEnabled() ? UIHelper.APPROVED_GREEN : UIHelper.PENDING_YELLOW);
                    if (status.lastTime() != null) {
                        String ok = Boolean.TRUE.equals(status.lastSuccess()) ? "Sucesso" : "Falha";
                        lastRunLabel.setText("Último Backup: " + status.lastTime().format(DT_FORMAT) + " (" + ok + ")");
                    }
                }

                filesTableModel.setRowCount(0);
                if (files != null) {
                    for (String f : files) {
                        String type = f.endsWith(".dump") || f.endsWith(".sql") ? "Base de Dados" : "Arquivo";
                        filesTableModel.addRow(new Object[]{f, type, "Disponível"});
                    }
                }
            }
        };
        worker.execute();
    }

    private void generateBackup() {
        if (backupApiClient == null) return;
        generateBtn.setEnabled(false);
        setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        resultSummaryLabel.setText("A gerar cópia de segurança no servidor...");

        SwingWorker<String, Void> worker = new SwingWorker<>() {
            @Override
            protected String doInBackground() {
                return backupApiClient.executeBackup();
            }

            @Override
            protected void done() {
                setCursor(Cursor.getDefaultCursor());
                generateBtn.setEnabled(true);
                try {
                    String path = get();
                    if (path != null) {
                        resultSummaryLabel.setIcon(UIHelper.icon("fas-check-circle", 14, UIHelper.APPROVED_GREEN));
                        resultSummaryLabel.setText("Backup gerado com sucesso: " + path);
                        resultSummaryLabel.setForeground(UIHelper.APPROVED_GREEN);
                        loadData();
                    }
                } catch (Exception e) {
                    resultSummaryLabel.setIcon(UIHelper.icon("fas-times-circle", 14, UIHelper.REJECTED_RED));
                    resultSummaryLabel.setText("Falha ao gerar backup: " + e.getMessage());
                    resultSummaryLabel.setForeground(UIHelper.REJECTED_RED);
                }
            }
        };
        worker.execute();
    }

    private void verifySelectedBackup() {
        int row = filesTable.getSelectedRow();
        if (row < 0) {
            ToastManager.show(this, FeedbackType.WARNING, "Por favor selecione um arquivo de backup na tabela.");
            return;
        }

        String fileName = (String) filesTableModel.getValueAt(row, 0);
        verifyBtn.setEnabled(false);
        resultSummaryLabel.setIcon(null);
        resultSummaryLabel.setText("A verificar integridade de " + fileName + "...");

        SwingWorker<BackupVerificationDTO, Void> worker = new SwingWorker<>() {
            @Override
            protected BackupVerificationDTO doInBackground() {
                return backupApiClient.verify(fileName);
            }

            @Override
            protected void done() {
                verifyBtn.setEnabled(true);
                try {
                    BackupVerificationDTO res = get();
                    if (res != null) {
                        resultSummaryLabel.setIcon(UIHelper.icon("fas-check-circle", 14, UIHelper.APPROVED_GREEN));
                        resultSummaryLabel.setText(String.format("Arquivo Íntegro: %s (Secções: %d, Gerado: %s)",
                                res.fileName(), res.totalSections(), res.generatedAt() != null ? res.generatedAt() : "Sim"));
                        resultSummaryLabel.setForeground(UIHelper.APPROVED_GREEN);
                    } else {
                        resultSummaryLabel.setIcon(UIHelper.icon("fas-times-circle", 14, UIHelper.REJECTED_RED));
                        resultSummaryLabel.setText("Arquivo com integridade inválida ou corrompido.");
                        resultSummaryLabel.setForeground(UIHelper.REJECTED_RED);
                    }
                } catch (Exception e) {
                    resultSummaryLabel.setIcon(UIHelper.icon("fas-times-circle", 14, UIHelper.REJECTED_RED));
                    resultSummaryLabel.setText("Erro ao verificar arquivo: " + e.getMessage());
                    resultSummaryLabel.setForeground(UIHelper.REJECTED_RED);
                }
            }
        };
        worker.execute();
    }
}
