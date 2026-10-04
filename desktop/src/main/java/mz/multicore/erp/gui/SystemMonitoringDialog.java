package mz.multicore.erp.gui;

import mz.multicore.erp.desktop.client.SystemMonitoringApiClient;
import mz.multicore.erp.gui.components.ModernButton;
import mz.multicore.erp.gui.components.ModernPanel;
import mz.multicore.erp.gui.components.SoundAlertManager;
import mz.multicore.erp.gui.components.UIHelper;
import mz.multicore.erp.gui.components.FeedbackType;
import mz.multicore.erp.gui.components.ModernMessageDialog;
import mz.multicore.erp.gui.components.ToastManager;
import mz.multicore.erp.modules.monitoring.dto.*;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.io.File;
import java.nio.file.Files;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Diálogo executivo de monitoramento em tempo real, observabilidade multi-tenant e central de alarmes.
 * Apresenta o status consolidado de recursos JVM, integridade de empresas e gestão de incidentes.
 */
public class SystemMonitoringDialog {

    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss")
            .withZone(ZoneId.systemDefault());

    private final Window parent;
    private final SystemMonitoringApiClient monitoringClient;
    private final boolean superAdmin;
    private final JDialog dialog;

    private JLabel overallBadge;
    private JLabel uptimeLabel;
    private JLabel memoryDetailLabel;
    private JProgressBar memoryProgressBar;
    private JLabel storageDetailLabel;
    private JProgressBar storageProgressBar;
    private JLabel dbEngineLabel;
    private JLabel dbLatencyLabel;
    private JLabel dbPoolLabel;
    private JLabel threadsLabel;
    private JLabel jvmVersionLabel;
    private DefaultTableModel subsystemsTableModel;
    private DefaultTableModel tenantsTableModel;
    private DefaultTableModel incidentsTableModel;
    private ModernButton refreshButton;
    private ModernButton exportButton;
    private ModernButton copyButton;
    private JCheckBox soundAlertCheckBox;

    private SystemHealthDTO lastHealthSnapshot;
    private SystemDiagnosticsExportDTO lastDiagnosticsExport;
    private List<TenantHealthDTO> lastTenantsSnapshot;
    private List<SystemAlertIncidentDTO> lastIncidentsSnapshot;

    public SystemMonitoringDialog(Window parent, SystemMonitoringApiClient monitoringClient) {
        this.parent = parent;
        this.monitoringClient = monitoringClient;
        this.superAdmin = monitoringClient.isSuperAdmin();
        this.dialog = new JDialog(parent, "Observabilidade & Diagnóstico do Sistema", Dialog.ModalityType.APPLICATION_MODAL);
        initUi();
    }

    public void show() {
        dialog.setLocationRelativeTo(parent);
        refreshDataAsync();
        dialog.setVisible(true);
    }

    private void initUi() {
        dialog.setSize(1020, 720);
        dialog.setMinimumSize(new Dimension(880, 600));
        dialog.getContentPane().setBackground(UIHelper.BG_DARK);

        JPanel mainPanel = new JPanel(new BorderLayout(0, 14));
        mainPanel.setBackground(UIHelper.BG_DARK);
        mainPanel.setBorder(new EmptyBorder(16, 18, 16, 18));

        mainPanel.add(buildHeader(), BorderLayout.NORTH);

        JTabbedPane tabbedPane = new JTabbedPane();
        UIHelper.styleTabbedPaneMulticore(tabbedPane);

        tabbedPane.addTab("Diagnóstico Geral", buildGeneralDiagnosticsPanel());
        tabbedPane.addTab("Saúde Multi-Tenant", buildMultiTenantPanel());
        if (superAdmin) {
            tabbedPane.addTab("Central de Alarmes", buildAlertsPanel());
        }

        mainPanel.add(tabbedPane, BorderLayout.CENTER);
        mainPanel.add(buildFooter(), BorderLayout.SOUTH);

        dialog.setContentPane(mainPanel);
    }

    private JPanel buildHeader() {
        JPanel header = new JPanel(new BorderLayout(16, 0));
        header.setOpaque(false);

        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
        left.setOpaque(false);

        JLabel iconLabel = new JLabel(UIHelper.icon("fas-heartbeat", 30, UIHelper.ACCENT_CYAN));
        left.add(iconLabel);

        JPanel titles = new JPanel(new GridLayout(2, 1, 0, 2));
        titles.setOpaque(false);
        JLabel title = new JLabel("Diagnóstico & Saúde Operacional");
        title.setFont(new Font("Segoe UI", Font.BOLD, 17));
        title.setForeground(UIHelper.TEXT_LIGHT);

        uptimeLabel = new JLabel("A carregar telemetria do sistema...");
        uptimeLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        uptimeLabel.setForeground(UIHelper.TEXT_MUTED);

        titles.add(title);
        titles.add(uptimeLabel);
        left.add(titles);

        overallBadge = new JLabel("A VERIFICAR", SwingConstants.CENTER);
        overallBadge.setOpaque(true);
        overallBadge.setBackground(UIHelper.BG_CARD);
        overallBadge.setForeground(UIHelper.TEXT_MUTED);
        overallBadge.setFont(new Font("Segoe UI", Font.BOLD, 12));
        overallBadge.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UIHelper.BORDER, 1),
                new EmptyBorder(6, 16, 6, 16)
        ));

        header.add(left, BorderLayout.WEST);
        header.add(overallBadge, BorderLayout.EAST);
        return header;
    }

    private JPanel buildGeneralDiagnosticsPanel() {
        JPanel panel = new JPanel(new BorderLayout(0, 12));
        panel.setOpaque(false);
        panel.add(buildMetricsGrid(), BorderLayout.NORTH);
        panel.add(buildSubsystemsPanel(), BorderLayout.CENTER);
        return panel;
    }

    private JPanel buildMetricsGrid() {
        JPanel grid = new JPanel(new GridLayout(2, 2, 10, 10));
        grid.setOpaque(false);

        // Cartão 1: Memória Heap JVM
        ModernPanel memCard = createCard("Memória Heap da JVM", "fas-memory", UIHelper.ACCENT);
        memoryProgressBar = createProgressBar();
        memoryDetailLabel = createDetailLabel("— MB usados de — MB");
        JPanel memContent = new JPanel(new BorderLayout(0, 6));
        memContent.setOpaque(false);
        memContent.add(memoryProgressBar, BorderLayout.NORTH);
        memContent.add(memoryDetailLabel, BorderLayout.CENTER);
        memCard.add(memContent, BorderLayout.CENTER);
        grid.add(memCard);

        // Cartão 2: Armazenamento em Disco
        ModernPanel diskCard = createCard("Espaço em Disco", "fas-hdd", UIHelper.ACCENT_CYAN);
        storageProgressBar = createProgressBar();
        storageDetailLabel = createDetailLabel("— GB livres");
        JPanel diskContent = new JPanel(new BorderLayout(0, 6));
        diskContent.setOpaque(false);
        diskContent.add(storageProgressBar, BorderLayout.NORTH);
        diskContent.add(storageDetailLabel, BorderLayout.CENTER);
        diskCard.add(diskContent, BorderLayout.CENTER);
        grid.add(diskCard);

        // Cartão 3: Persistência & Base de Dados
        ModernPanel dbCard = createCard("Base de Dados (HikariCP)", "fas-database", UIHelper.APPROVED_GREEN);
        JPanel dbBody = new JPanel(new GridLayout(3, 1, 0, 3));
        dbBody.setOpaque(false);
        dbEngineLabel = createDetailLabel("Motor: —");
        dbLatencyLabel = createDetailLabel("Latência de Ping: — ms");
        dbPoolLabel = createDetailLabel("Conexões no Pool: —");
        dbBody.add(dbEngineLabel);
        dbBody.add(dbLatencyLabel);
        dbBody.add(dbPoolLabel);
        dbCard.add(dbBody, BorderLayout.CENTER);
        grid.add(dbCard);

        // Cartão 4: Threads & Ambiente
        ModernPanel threadCard = createCard("Threads & Ambiente de Execução", "fas-microchip", UIHelper.ACCENT_ORANGE);
        JPanel threadBody = new JPanel(new GridLayout(2, 1, 0, 3));
        threadBody.setOpaque(false);
        threadsLabel = createDetailLabel("Threads Activas: — (Pico: —)");
        jvmVersionLabel = createDetailLabel("JVM: —");
        threadBody.add(threadsLabel);
        threadBody.add(jvmVersionLabel);
        threadCard.add(threadBody, BorderLayout.CENTER);
        grid.add(threadCard);

        return grid;
    }

    private JPanel buildSubsystemsPanel() {
        ModernPanel panel = new ModernPanel();
        panel.setLayout(new BorderLayout(0, 8));
        panel.setBorder(new EmptyBorder(10, 12, 10, 12));

        JLabel title = new JLabel("Estado Operacional dos Subsistemas");
        title.setFont(new Font("Segoe UI", Font.BOLD, 13));
        title.setForeground(UIHelper.TEXT_LIGHT);
        panel.add(title, BorderLayout.NORTH);

        String[] cols = {"Subsistema", "Estado", "Latência", "Detalhes"};
        subsystemsTableModel = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        JTable table = new JTable(subsystemsTableModel);
        table.setRowHeight(28);
        table.setBackground(UIHelper.BG_DARK);
        table.setForeground(UIHelper.TEXT_LIGHT);
        table.setGridColor(UIHelper.BORDER);
        table.getTableHeader().setBackground(UIHelper.BG_CARD);
        table.getTableHeader().setForeground(UIHelper.TEXT_MUTED);
        table.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 12));

        table.getColumnModel().getColumn(1).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object val, boolean isSel, boolean hasFoc, int r, int c) {
                JLabel l = (JLabel) super.getTableCellRendererComponent(t, val, isSel, hasFoc, r, c);
                l.setHorizontalAlignment(SwingConstants.CENTER);
                String st = String.valueOf(val);
                if ("HEALTHY".equalsIgnoreCase(st) || "UP".equalsIgnoreCase(st)) {
                    l.setForeground(UIHelper.APPROVED_GREEN);
                    l.setText("SAUDAVEL");
                    l.setFont(l.getFont().deriveFont(Font.BOLD));
                } else if ("WARNING".equalsIgnoreCase(st) || "DEGRADED".equalsIgnoreCase(st)) {
                    l.setForeground(UIHelper.PENDING_YELLOW);
                    l.setText("ATENCAO");
                    l.setFont(l.getFont().deriveFont(Font.BOLD));
                } else {
                    l.setForeground(UIHelper.REJECTED_RED);
                    l.setText("CRITICO");
                    l.setFont(l.getFont().deriveFont(Font.BOLD));
                }
                return l;
            }
        });

        table.getColumnModel().getColumn(0).setPreferredWidth(200);
        table.getColumnModel().getColumn(1).setPreferredWidth(120);
        table.getColumnModel().getColumn(2).setPreferredWidth(90);
        table.getColumnModel().getColumn(3).setPreferredWidth(450);

        JScrollPane scroll = new JScrollPane(table);
        scroll.setBorder(BorderFactory.createLineBorder(UIHelper.BORDER, 1));
        scroll.getViewport().setBackground(UIHelper.BG_DARK);
        panel.add(scroll, BorderLayout.CENTER);

        return panel;
    }

    private JPanel buildMultiTenantPanel() {
        ModernPanel panel = new ModernPanel();
        panel.setLayout(new BorderLayout(0, 10));
        panel.setBorder(new EmptyBorder(12, 14, 12, 14));

        JLabel title = new JLabel("Integridade das Empresas & Inquilinos");
        title.setFont(new Font("Segoe UI", Font.BOLD, 14));
        title.setForeground(UIHelper.TEXT_LIGHT);
        panel.add(title, BorderLayout.NORTH);

        String[] cols = {"Empresa", "NUIT", "Estado Operacional", "Utilizadores", "Backup Automático", "Anomalias Pendentes"};
        tenantsTableModel = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        JTable table = new JTable(tenantsTableModel);
        table.setRowHeight(30);
        table.setBackground(UIHelper.BG_DARK);
        table.setForeground(UIHelper.TEXT_LIGHT);
        table.setGridColor(UIHelper.BORDER);
        table.getTableHeader().setBackground(UIHelper.BG_CARD);
        table.getTableHeader().setForeground(UIHelper.TEXT_MUTED);
        table.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 12));

        table.getColumnModel().getColumn(2).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object val, boolean isSel, boolean hasFoc, int r, int c) {
                JLabel l = (JLabel) super.getTableCellRendererComponent(t, val, isSel, hasFoc, r, c);
                l.setHorizontalAlignment(SwingConstants.CENTER);
                String st = String.valueOf(val);
                if ("HEALTHY".equalsIgnoreCase(st)) {
                    l.setForeground(UIHelper.APPROVED_GREEN);
                    l.setText("SAUDAVEL");
                    l.setFont(l.getFont().deriveFont(Font.BOLD));
                } else if ("WARNING".equalsIgnoreCase(st)) {
                    l.setForeground(UIHelper.PENDING_YELLOW);
                    l.setText("ATENCAO");
                    l.setFont(l.getFont().deriveFont(Font.BOLD));
                } else {
                    l.setForeground(UIHelper.REJECTED_RED);
                    l.setText("CRITICO");
                    l.setFont(l.getFont().deriveFont(Font.BOLD));
                }
                return l;
            }
        });

        table.getColumnModel().getColumn(4).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object val, boolean isSel, boolean hasFoc, int r, int c) {
                JLabel l = (JLabel) super.getTableCellRendererComponent(t, val, isSel, hasFoc, r, c);
                l.setHorizontalAlignment(SwingConstants.CENTER);
                String st = String.valueOf(val);
                if ("UP_TO_DATE".equalsIgnoreCase(st)) {
                    l.setForeground(UIHelper.APPROVED_GREEN);
                    l.setText("Actualizado");
                    l.setFont(l.getFont().deriveFont(Font.BOLD));
                } else if ("FAILED".equalsIgnoreCase(st)) {
                    l.setForeground(UIHelper.REJECTED_RED);
                    l.setText("Falhado");
                    l.setFont(l.getFont().deriveFont(Font.BOLD));
                } else {
                    l.setForeground(UIHelper.TEXT_MUTED);
                    l.setText("Nao agendado");
                }
                return l;
            }
        });

        table.getColumnModel().getColumn(0).setPreferredWidth(230);
        table.getColumnModel().getColumn(1).setPreferredWidth(120);
        table.getColumnModel().getColumn(2).setPreferredWidth(140);
        table.getColumnModel().getColumn(3).setPreferredWidth(100);
        table.getColumnModel().getColumn(4).setPreferredWidth(150);
        table.getColumnModel().getColumn(5).setPreferredWidth(140);

        JScrollPane scroll = new JScrollPane(table);
        scroll.setBorder(BorderFactory.createLineBorder(UIHelper.BORDER, 1));
        scroll.getViewport().setBackground(UIHelper.BG_DARK);
        panel.add(scroll, BorderLayout.CENTER);

        return panel;
    }

    private JPanel buildAlertsPanel() {
        ModernPanel panel = new ModernPanel();
        panel.setLayout(new BorderLayout(0, 10));
        panel.setBorder(new EmptyBorder(12, 14, 12, 14));

        // Barra superior de ações e preferências
        JPanel topBar = new JPanel(new BorderLayout(10, 0));
        topBar.setOpaque(false);

        JPanel leftActions = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        leftActions.setOpaque(false);

        ModernButton testSoundButton = new ModernButton("Testar Alarme Sonoro", UIHelper.ACCENT_ORANGE, UIHelper.ACCENT_HOVER);
        testSoundButton.setIcon(UIHelper.icon("fas-volume-up", 14, Color.WHITE));
        testSoundButton.addActionListener(e -> {
            SoundAlertManager.getInstance().playTestAlertAsync();
            ToastManager.success(dialog, "Tom de alarme de emergência disparado para o canal de áudio.");
        });
        leftActions.add(testSoundButton);

        ModernButton testEmailButton = new ModernButton("Testar E-mail de Alerta", UIHelper.ACCENT_CYAN, UIHelper.ACCENT_BLUE_HOVER);
        testEmailButton.setIcon(UIHelper.icon("fas-envelope", 14, Color.WHITE));
        testEmailButton.addActionListener(e -> handleTestEmail());
        leftActions.add(testEmailButton);

        soundAlertCheckBox = new JCheckBox("Activar Alarme Sonoro em Falhas Críticas", SoundAlertManager.getInstance().isSoundAlertEnabled());
        soundAlertCheckBox.setOpaque(false);
        soundAlertCheckBox.setForeground(UIHelper.TEXT_LIGHT);
        soundAlertCheckBox.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        soundAlertCheckBox.addActionListener(e -> SoundAlertManager.getInstance().setSoundAlertEnabled(soundAlertCheckBox.isSelected()));
        leftActions.add(soundAlertCheckBox);

        topBar.add(leftActions, BorderLayout.WEST);
        panel.add(topBar, BorderLayout.NORTH);

        // Tabela de Incidentes Recentes
        String[] cols = {"ID", "Subsistema", "Empresa", "Severidade", "Mensagem", "Data / Hora", "Estado"};
        incidentsTableModel = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        JTable table = new JTable(incidentsTableModel);
        table.setRowHeight(28);
        table.setBackground(UIHelper.BG_DARK);
        table.setForeground(UIHelper.TEXT_LIGHT);
        table.setGridColor(UIHelper.BORDER);
        table.getTableHeader().setBackground(UIHelper.BG_CARD);
        table.getTableHeader().setForeground(UIHelper.TEXT_MUTED);
        table.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 12));

        table.getColumnModel().getColumn(3).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object val, boolean isSel, boolean hasFoc, int r, int c) {
                JLabel l = (JLabel) super.getTableCellRendererComponent(t, val, isSel, hasFoc, r, c);
                l.setHorizontalAlignment(SwingConstants.CENTER);
                String sev = String.valueOf(val);
                if ("CRITICAL".equalsIgnoreCase(sev)) {
                    l.setForeground(UIHelper.REJECTED_RED);
                    l.setText("CRÍTICO");
                } else if ("WARNING".equalsIgnoreCase(sev)) {
                    l.setForeground(UIHelper.PENDING_YELLOW);
                    l.setText("AVISO");
                } else {
                    l.setForeground(UIHelper.ACCENT_CYAN);
                    l.setText("INFO");
                }
                return l;
            }
        });

        table.getColumnModel().getColumn(0).setPreferredWidth(120);
        table.getColumnModel().getColumn(1).setPreferredWidth(110);
        table.getColumnModel().getColumn(2).setPreferredWidth(140);
        table.getColumnModel().getColumn(3).setPreferredWidth(90);
        table.getColumnModel().getColumn(4).setPreferredWidth(320);
        table.getColumnModel().getColumn(5).setPreferredWidth(140);
        table.getColumnModel().getColumn(6).setPreferredWidth(80);

        JScrollPane scroll = new JScrollPane(table);
        scroll.setBorder(BorderFactory.createLineBorder(UIHelper.BORDER, 1));
        scroll.getViewport().setBackground(UIHelper.BG_DARK);
        panel.add(scroll, BorderLayout.CENTER);

        return panel;
    }

    private void handleTestEmail() {
        String email = ModernMessageDialog.prompt(dialog, FeedbackType.INFO, "Teste de Alerta por E-mail",
                "Endereço de e-mail para receber a prova de alerta:", "admin@multicore.co.mz", false);
        if (email == null || email.isBlank()) return;

        SwingWorker<SystemAlertTestResultDTO, Void> worker = new SwingWorker<>() {
            @Override
            protected SystemAlertTestResultDTO doInBackground() {
                return monitoringClient.testEmailAlert(email.trim());
            }

            @Override
            protected void done() {
                try {
                    SystemAlertTestResultDTO res = get();
                    if (res != null && res.success()) {
                        ToastManager.success(dialog, res.message());
                    } else {
                        ToastManager.show(dialog, FeedbackType.WARNING,
                                res != null ? res.message() : "Falha ao enviar alerta de teste.");
                    }
                } catch (Exception ex) {
                    ToastManager.show(dialog, FeedbackType.ERROR, "Erro ao disparar teste: " + ex.getMessage());
                }
            }
        };
        worker.execute();
    }

    private JPanel buildFooter() {
        JPanel footer = new JPanel(new BorderLayout(10, 0));
        footer.setOpaque(false);

        JPanel leftActions = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        leftActions.setOpaque(false);

        refreshButton = new ModernButton("Actualizar", UIHelper.ACCENT_BLUE, UIHelper.ACCENT_BLUE_HOVER);
        refreshButton.setIcon(UIHelper.icon("fas-sync-alt", 14, Color.WHITE));
        refreshButton.addActionListener(e -> refreshDataAsync());
        leftActions.add(refreshButton);

        copyButton = new ModernButton("Copiar Diagnóstico", UIHelper.ACCENT, UIHelper.ACCENT_HOVER);
        copyButton.setIcon(UIHelper.icon("fas-copy", 14, Color.WHITE));
        copyButton.addActionListener(e -> copyDiagnosticsToClipboard());
        leftActions.add(copyButton);

        exportButton = new ModernButton("Exportar Relatório (.txt)", UIHelper.APPROVED_GREEN, UIHelper.APPROVED_GREEN_HOVER);
        exportButton.setIcon(UIHelper.icon("fas-file-download", 14, Color.WHITE));
        exportButton.addActionListener(e -> exportDiagnosticsToFile());
        leftActions.add(exportButton);

        JPanel rightActions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        rightActions.setOpaque(false);
        ModernButton closeButton = UIHelper.createDangerButton("Fechar");
        closeButton.setIcon(UIHelper.icon("fas-times", 14, Color.WHITE));
        closeButton.addActionListener(e -> dialog.dispose());
        rightActions.add(closeButton);

        footer.add(leftActions, BorderLayout.WEST);
        footer.add(rightActions, BorderLayout.EAST);
        return footer;
    }

    private void refreshDataAsync() {
        refreshButton.setEnabled(false);
        overallBadge.setText("A ACTUALIZAR...");
        overallBadge.setForeground(UIHelper.TEXT_MUTED);

        SwingWorker<SystemHealthDTO, Void> worker = new SwingWorker<>() {
            @Override
            protected SystemHealthDTO doInBackground() {
                try {
                    lastDiagnosticsExport = monitoringClient.getDiagnosticsExport();
                    lastTenantsSnapshot = monitoringClient.getTenantsHealth();
                    lastIncidentsSnapshot = superAdmin ? monitoringClient.getIncidents() : List.of();
                    return monitoringClient.getSystemHealth();
                } catch (Exception ex) {
                    return null;
                }
            }

            @Override
            protected void done() {
                refreshButton.setEnabled(true);
                try {
                    SystemHealthDTO health = get();
                    if (health != null) {
                        lastHealthSnapshot = health;
                        applyHealthData(health);
                    } else {
                        overallBadge.setText("OFFLINE");
                        overallBadge.setBackground(UIHelper.REJECTED_RED);
                        overallBadge.setForeground(Color.WHITE);
                        uptimeLabel.setText("Não foi possível estabelecer ligação ao serviço de monitoramento.");
                    }
                    applyTenantsData(lastTenantsSnapshot);
                    if (superAdmin) {
                        applyIncidentsData(lastIncidentsSnapshot);
                    }
                } catch (Exception ex) {
                    overallBadge.setText("ERRO");
                }
            }
        };
        worker.execute();
    }

    private void applyHealthData(SystemHealthDTO h) {
        String status = h.overallStatus();
        if ("HEALTHY".equalsIgnoreCase(status)) {
            overallBadge.setText("SISTEMA SAUDÁVEL");
            overallBadge.setIcon(UIHelper.icon("fas-check-circle", 12, Color.WHITE));
            overallBadge.setBackground(UIHelper.APPROVED_GREEN);
            overallBadge.setForeground(Color.WHITE);
        } else if ("WARNING".equalsIgnoreCase(status)) {
            overallBadge.setText("ATENÇÃO REQUERIDA");
            overallBadge.setIcon(UIHelper.icon("fas-exclamation-triangle", 12, Color.BLACK));
            overallBadge.setBackground(UIHelper.PENDING_YELLOW);
            overallBadge.setForeground(Color.BLACK);
        } else {
            overallBadge.setText("ESTADO CRÍTICO");
            overallBadge.setIcon(UIHelper.icon("fas-times-circle", 12, Color.WHITE));
            overallBadge.setBackground(UIHelper.REJECTED_RED);
            overallBadge.setForeground(Color.WHITE);
            // Disparar alarme sonoro em tempo real
            SoundAlertManager.getInstance().playAlertAsync();
        }

        long days = h.uptimeSeconds() / 86400;
        long hours = (h.uptimeSeconds() % 86400) / 3600;
        long mins = (h.uptimeSeconds() % 3600) / 60;
        uptimeLabel.setText(String.format("Uptime do Servidor: %d d, %d h, %d min | Servidor: v%s | SO: %s",
                days, hours, mins, h.serverVersion(), h.osName()));

        var mem = h.memory();
        if (mem != null) {
            long usedMb = mem.heapUsedBytes() / (1024 * 1024);
            long maxMb = mem.heapMaxBytes() / (1024 * 1024);
            int pct = (int) Math.round(mem.heapUsedPercent());
            memoryProgressBar.setValue(pct);
            memoryProgressBar.setString(pct + "%");
            memoryProgressBar.setForeground(pct > 85 ? UIHelper.REJECTED_RED : (pct > 70 ? UIHelper.PENDING_YELLOW : UIHelper.ACCENT));
            memoryDetailLabel.setText(String.format("%d MB usados de %d MB alocados", usedMb, maxMb));
        }

        var storage = h.storage();
        if (storage != null) {
            long freeGb = storage.freeBytes() / (1024 * 1024 * 1024);
            long totalGb = storage.totalBytes() / (1024 * 1024 * 1024);
            int pct = (int) Math.round(storage.usedPercent());
            storageProgressBar.setValue(pct);
            storageProgressBar.setString(pct + "%");
            storageProgressBar.setForeground(pct > 90 ? UIHelper.REJECTED_RED : (pct > 75 ? UIHelper.PENDING_YELLOW : UIHelper.ACCENT_CYAN));
            storageDetailLabel.setText(String.format("%d GB livres de %d GB totais", freeGb, totalGb));
        }

        var db = h.database();
        if (db != null) {
            dbEngineLabel.setText("Motor: " + db.databaseEngine());
            dbLatencyLabel.setText("Latência de Ping: " + db.responseTimeMs() + " ms (" + db.status() + ")");
            dbPoolLabel.setText(String.format("Conexões no Pool: %d activas / %d ociosas (Máx: %d)",
                    db.activeConnections(), db.idleConnections(), db.maxConnections()));
        }

        var threads = h.threadPool();
        if (threads != null) {
            threadsLabel.setText(String.format("Threads Activas: %d (Pico: %d, Daemon: %d)",
                    threads.liveThreads(), threads.peakThreads(), threads.daemonThreads()));
        }
        jvmVersionLabel.setText("Java: " + h.javaVersion());

        subsystemsTableModel.setRowCount(0);
        if (h.subsystems() != null) {
            for (var sub : h.subsystems()) {
                subsystemsTableModel.addRow(new Object[]{
                        sub.name(),
                        sub.status(),
                        sub.latencyMs() + " ms",
                        sub.details()
                });
            }
        }
    }

    private void applyTenantsData(List<TenantHealthDTO> tenants) {
        if (tenantsTableModel == null) return;
        tenantsTableModel.setRowCount(0);
        if (tenants == null) return;

        for (TenantHealthDTO t : tenants) {
            tenantsTableModel.addRow(new Object[]{
                    t.companyName(),
                    t.companyNuit() != null ? t.companyNuit() : "—",
                    t.status(),
                    t.activeUsers(),
                    t.lastBackupStatus(),
                    t.pendingAnomaliesCount()
            });
        }
    }

    private void applyIncidentsData(List<SystemAlertIncidentDTO> incidents) {
        if (incidentsTableModel == null) return;
        incidentsTableModel.setRowCount(0);
        if (incidents == null) return;

        for (SystemAlertIncidentDTO inc : incidents) {
            String timeStr = inc.timestamp() != null ? TIME_FMT.format(inc.timestamp()) : "—";
            incidentsTableModel.addRow(new Object[]{
                    inc.incidentId(),
                    inc.subsystem(),
                    inc.tenantName(),
                    inc.severity(),
                    inc.message(),
                    timeStr,
                    inc.resolved() ? "Resolvido" : "Activo"
            });
        }
    }

    private void copyDiagnosticsToClipboard() {
        if (lastDiagnosticsExport == null || lastDiagnosticsExport.reportFormattedText() == null) {
            ToastManager.show(dialog, FeedbackType.WARNING, "Nenhum relatório de diagnóstico carregado ainda.");
            return;
        }
        try {
            StringSelection sel = new StringSelection(lastDiagnosticsExport.reportFormattedText());
            Toolkit.getDefaultToolkit().getSystemClipboard().setContents(sel, sel);
            ToastManager.success(dialog, "Relatório de diagnóstico copiado para a área de transferência.");
        } catch (Exception ex) {
            ToastManager.show(dialog, FeedbackType.ERROR, "Erro ao copiar: " + ex.getMessage());
        }
    }

    private void exportDiagnosticsToFile() {
        if (lastDiagnosticsExport == null || lastDiagnosticsExport.reportFormattedText() == null) {
            ToastManager.show(dialog, FeedbackType.WARNING, "Nenhum relatório de diagnóstico carregado ainda.");
            return;
        }

        JFileChooser chooser = new JFileChooser();
        chooser.setSelectedFile(new File("diagnostico-multicore-" + System.currentTimeMillis() + ".txt"));
        int opt = chooser.showSaveDialog(dialog);
        if (opt == JFileChooser.APPROVE_OPTION) {
            try {
                File target = chooser.getSelectedFile();
                Files.writeString(target.toPath(), lastDiagnosticsExport.reportFormattedText());
                ToastManager.success(dialog, "Relatório exportado com sucesso para: " + target.getAbsolutePath());
            } catch (Exception ex) {
                ToastManager.show(dialog, FeedbackType.ERROR, "Erro ao gravar ficheiro: " + ex.getMessage());
            }
        }
    }

    private ModernPanel createCard(String title, String iconName, Color iconColor) {
        ModernPanel p = new ModernPanel();
        p.setLayout(new BorderLayout(0, 6));
        p.setBorder(new EmptyBorder(8, 10, 8, 10));

        JPanel header = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        header.setOpaque(false);
        header.add(new JLabel(UIHelper.icon(iconName, 16, iconColor)));
        JLabel lbl = new JLabel(title);
        lbl.setFont(new Font("Segoe UI", Font.BOLD, 13));
        lbl.setForeground(UIHelper.TEXT_LIGHT);
        header.add(lbl);

        p.add(header, BorderLayout.NORTH);
        return p;
    }

    private JProgressBar createProgressBar() {
        JProgressBar pb = new JProgressBar(0, 100);
        pb.setValue(0);
        pb.setStringPainted(true);
        pb.setBackground(UIHelper.BG_DARK);
        pb.setForeground(UIHelper.ACCENT);
        pb.setBorder(BorderFactory.createLineBorder(UIHelper.BORDER, 1));
        pb.setPreferredSize(new Dimension(100, 20));
        return pb;
    }

    private JLabel createDetailLabel(String text) {
        JLabel l = new JLabel(text);
        l.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        l.setForeground(UIHelper.TEXT_MUTED);
        return l;
    }
}
