package mz.multicore.erp.gui;

import mz.multicore.erp.desktop.client.SystemMonitoringApiClient;
import mz.multicore.erp.gui.components.ModernButton;
import mz.multicore.erp.gui.components.UIHelper;
import mz.multicore.erp.gui.monitoring.SystemMonitoringExporter;
import mz.multicore.erp.gui.monitoring.SystemMonitoringOverviewBuilder;
import mz.multicore.erp.gui.monitoring.SystemMonitoringTabsBuilder;
import mz.multicore.erp.modules.monitoring.dto.*;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.List;

/**
 * Diálogo executivo de monitoramento em tempo real, observabilidade multi-tenant e central de alarmes.
 * Apresenta o status consolidado de recursos JVM, integridade de empresas e gestão de incidentes.
 */
public class SystemMonitoringDialog {

    private final Window parent;
    private final SystemMonitoringApiClient monitoringClient;
    private final boolean superAdmin;
    private final JDialog dialog;

    private final SystemMonitoringOverviewBuilder overviewBuilder = new SystemMonitoringOverviewBuilder();
    private final SystemMonitoringTabsBuilder tabsBuilder = new SystemMonitoringTabsBuilder();

    private ModernButton refreshButton;
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

    public JDialog getDialog() {
        return dialog;
    }

    public SystemHealthDTO getLastHealthSnapshot() {
        return lastHealthSnapshot;
    }

    private void initUi() {
        dialog.setSize(1020, 720);
        dialog.setMinimumSize(new Dimension(880, 600));
        dialog.getContentPane().setBackground(UIHelper.BG_DARK);

        JPanel mainPanel = new JPanel(new BorderLayout(0, 14));
        mainPanel.setBackground(UIHelper.BG_DARK);
        mainPanel.setBorder(new EmptyBorder(16, 18, 16, 18));

        mainPanel.add(overviewBuilder.buildHeader(), BorderLayout.NORTH);

        JTabbedPane tabbedPane = new JTabbedPane();
        UIHelper.styleTabbedPaneMulticore(tabbedPane);

        tabbedPane.addTab("Diagnóstico Geral", buildGeneralDiagnosticsPanel());
        tabbedPane.addTab("Saúde Multi-Tenant", tabsBuilder.buildMultiTenantPanel());
        if (superAdmin) {
            tabbedPane.addTab("Central de Alarmes", tabsBuilder.buildAlertsPanel(dialog, this::handleTestEmail));
        }

        mainPanel.add(tabbedPane, BorderLayout.CENTER);
        mainPanel.add(buildFooter(), BorderLayout.SOUTH);

        dialog.setContentPane(mainPanel);
    }

    private JPanel buildGeneralDiagnosticsPanel() {
        JPanel panel = new JPanel(new BorderLayout(0, 12));
        panel.setOpaque(false);
        panel.add(overviewBuilder.buildMetricsGrid(), BorderLayout.NORTH);
        panel.add(tabsBuilder.buildSubsystemsPanel(), BorderLayout.CENTER);
        return panel;
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

        ModernButton copyButton = new ModernButton("Copiar Diagnóstico", UIHelper.ACCENT, UIHelper.ACCENT_HOVER);
        copyButton.setIcon(UIHelper.icon("fas-copy", 14, Color.WHITE));
        copyButton.addActionListener(e -> SystemMonitoringExporter.copyToClipboard(dialog, lastDiagnosticsExport));
        leftActions.add(copyButton);

        ModernButton exportButton = new ModernButton("Exportar Relatório (.txt)", UIHelper.APPROVED_GREEN, UIHelper.APPROVED_GREEN_HOVER);
        exportButton.setIcon(UIHelper.icon("fas-file-download", 14, Color.WHITE));
        exportButton.addActionListener(e -> SystemMonitoringExporter.exportToFile(dialog, lastDiagnosticsExport));
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

    private void handleTestEmail() {
        SystemMonitoringExporter.handleTestEmail(dialog, monitoringClient);
    }

    public void refreshDataAsync() {
        if (refreshButton != null) refreshButton.setEnabled(false);
        overviewBuilder.setUpdatingState();

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
                if (refreshButton != null) refreshButton.setEnabled(true);
                try {
                    SystemHealthDTO health = get();
                    if (health != null) {
                        lastHealthSnapshot = health;
                        overviewBuilder.applyHealthData(health);
                        tabsBuilder.applySubsystems(health.subsystems());
                    } else {
                        overviewBuilder.setOfflineState();
                    }
                    tabsBuilder.applyTenants(lastTenantsSnapshot);
                    if (superAdmin) {
                        tabsBuilder.applyIncidents(lastIncidentsSnapshot);
                    }
                } catch (Exception ex) {
                    overviewBuilder.setErrorState();
                }
            }
        };
        worker.execute();
    }
}
