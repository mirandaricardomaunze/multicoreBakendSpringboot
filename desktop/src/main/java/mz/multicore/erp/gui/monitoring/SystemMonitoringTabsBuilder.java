package mz.multicore.erp.gui.monitoring;

import mz.multicore.erp.gui.components.ModernButton;
import mz.multicore.erp.gui.components.ModernPanel;
import mz.multicore.erp.gui.components.SoundAlertManager;
import mz.multicore.erp.gui.components.ToastManager;
import mz.multicore.erp.gui.components.UIHelper;
import mz.multicore.erp.modules.monitoring.dto.SystemAlertIncidentDTO;
import mz.multicore.erp.modules.monitoring.dto.SubsystemStatusDTO;
import mz.multicore.erp.modules.monitoring.dto.TenantHealthDTO;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Montador de abas e tabelas corporativas de observabilidade (Subsistemas, Multi-Tenant e Central de Alarmes).
 */
public class SystemMonitoringTabsBuilder {

    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss")
            .withZone(ZoneId.systemDefault());

    private DefaultTableModel subsystemsTableModel;
    private DefaultTableModel tenantsTableModel;
    private DefaultTableModel incidentsTableModel;

    public JPanel buildSubsystemsPanel() {
        ModernPanel panel = createTabCard("Estado Operacional dos Subsistemas");
        subsystemsTableModel = createNonEditableModel(new String[]{"Subsistema", "Estado", "Latência", "Detalhes"});
        JTable table = createDarkTable(subsystemsTableModel);
        table.getColumnModel().getColumn(1).setCellRenderer(createStatusCellRenderer());
        configureColumns(table, 200, 120, 90, 450);
        panel.add(wrapScroll(table), BorderLayout.CENTER);
        return panel;
    }

    public JPanel buildMultiTenantPanel() {
        ModernPanel panel = createTabCard("Integridade das Empresas & Inquilinos");
        String[] cols = {"Empresa", "NUIT", "Estado Operacional", "Utilizadores", "Backup Automático", "Anomalias Pendentes"};
        tenantsTableModel = createNonEditableModel(cols);
        JTable table = createDarkTable(tenantsTableModel);
        table.getColumnModel().getColumn(2).setCellRenderer(createStatusCellRenderer());
        table.getColumnModel().getColumn(4).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object val, boolean sel, boolean foc, int r, int c) {
                JLabel l = (JLabel) super.getTableCellRendererComponent(t, val, sel, foc, r, c);
                l.setHorizontalAlignment(SwingConstants.CENTER);
                String st = String.valueOf(val);
                boolean upToDate = "UP_TO_DATE".equalsIgnoreCase(st);
                boolean failed = "FAILED".equalsIgnoreCase(st);
                l.setForeground(upToDate ? UIHelper.APPROVED_GREEN : (failed ? UIHelper.REJECTED_RED : UIHelper.TEXT_MUTED));
                l.setText(upToDate ? "Actualizado" : (failed ? "Falhado" : "Nao agendado"));
                l.setFont(l.getFont().deriveFont(Font.BOLD));
                return l;
            }
        });
        configureColumns(table, 230, 120, 140, 100, 150, 140);
        panel.add(wrapScroll(table), BorderLayout.CENTER);
        return panel;
    }

    public JPanel buildAlertsPanel(Window parentDialog, Runnable onTestEmail) {
        ModernPanel panel = new ModernPanel();
        panel.setLayout(new BorderLayout(0, 10));
        panel.setBorder(new EmptyBorder(12, 14, 12, 14));

        JPanel topBar = new JPanel(new BorderLayout(10, 0));
        topBar.setOpaque(false);
        JPanel leftActions = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        leftActions.setOpaque(false);

        ModernButton testSoundBtn = new ModernButton("Testar Alarme Sonoro", UIHelper.ACCENT_ORANGE, UIHelper.ACCENT_HOVER);
        testSoundBtn.setIcon(UIHelper.icon("fas-volume-up", 14, Color.WHITE));
        testSoundBtn.addActionListener(e -> {
            SoundAlertManager.getInstance().playTestAlertAsync();
            ToastManager.success(parentDialog, "Tom de alarme de emergência disparado para o canal de áudio.");
        });
        leftActions.add(testSoundBtn);

        ModernButton testEmailBtn = new ModernButton("Testar E-mail de Alerta", UIHelper.ACCENT_CYAN, UIHelper.ACCENT_BLUE_HOVER);
        testEmailBtn.setIcon(UIHelper.icon("fas-envelope", 14, Color.WHITE));
        testEmailBtn.addActionListener(e -> onTestEmail.run());
        leftActions.add(testEmailBtn);

        JCheckBox soundBox = new JCheckBox("Activar Alarme Sonoro em Falhas Críticas", SoundAlertManager.getInstance().isSoundAlertEnabled());
        soundBox.setOpaque(false);
        soundBox.setForeground(UIHelper.TEXT_LIGHT);
        soundBox.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        soundBox.addActionListener(e -> SoundAlertManager.getInstance().setSoundAlertEnabled(soundBox.isSelected()));
        leftActions.add(soundBox);

        topBar.add(leftActions, BorderLayout.WEST);
        panel.add(topBar, BorderLayout.NORTH);

        String[] cols = {"ID", "Subsistema", "Empresa", "Severidade", "Mensagem", "Data / Hora", "Estado"};
        incidentsTableModel = createNonEditableModel(cols);
        JTable table = createDarkTable(incidentsTableModel);
        table.getColumnModel().getColumn(3).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object val, boolean sel, boolean foc, int r, int c) {
                JLabel l = (JLabel) super.getTableCellRendererComponent(t, val, sel, foc, r, c);
                l.setHorizontalAlignment(SwingConstants.CENTER);
                String sev = String.valueOf(val);
                boolean crit = "CRITICAL".equalsIgnoreCase(sev);
                boolean warn = "WARNING".equalsIgnoreCase(sev);
                l.setForeground(crit ? UIHelper.REJECTED_RED : (warn ? UIHelper.PENDING_YELLOW : UIHelper.ACCENT_CYAN));
                l.setText(crit ? "CRÍTICO" : (warn ? "AVISO" : "INFO"));
                return l;
            }
        });
        configureColumns(table, 120, 110, 140, 90, 320, 140, 80);
        panel.add(wrapScroll(table), BorderLayout.CENTER);
        return panel;
    }

    public void applySubsystems(List<SubsystemStatusDTO> subsystems) {
        if (subsystemsTableModel == null) return;
        subsystemsTableModel.setRowCount(0);
        if (subsystems == null) return;
        for (var sub : subsystems) {
            subsystemsTableModel.addRow(new Object[]{sub.name(), sub.status(), sub.latencyMs() + " ms", sub.details()});
        }
    }

    public void applyTenants(List<TenantHealthDTO> tenants) {
        if (tenantsTableModel == null) return;
        tenantsTableModel.setRowCount(0);
        if (tenants == null) return;
        for (TenantHealthDTO t : tenants) {
            tenantsTableModel.addRow(new Object[]{
                    t.companyName(), t.companyNuit() != null ? t.companyNuit() : "—",
                    t.status(), t.activeUsers(), t.lastBackupStatus(), t.pendingAnomaliesCount()
            });
        }
    }

    public void applyIncidents(List<SystemAlertIncidentDTO> incidents) {
        if (incidentsTableModel == null) return;
        incidentsTableModel.setRowCount(0);
        if (incidents == null) return;
        for (SystemAlertIncidentDTO inc : incidents) {
            String timeStr = inc.timestamp() != null ? TIME_FMT.format(inc.timestamp()) : "—";
            incidentsTableModel.addRow(new Object[]{
                    inc.incidentId(), inc.subsystem(), inc.tenantName(),
                    inc.severity(), inc.message(), timeStr, inc.resolved() ? "Resolvido" : "Activo"
            });
        }
    }

    private static DefaultTableCellRenderer createStatusCellRenderer() {
        return new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object val, boolean sel, boolean foc, int r, int c) {
                JLabel l = (JLabel) super.getTableCellRendererComponent(t, val, sel, foc, r, c);
                l.setHorizontalAlignment(SwingConstants.CENTER);
                String st = String.valueOf(val);
                boolean healthy = "HEALTHY".equalsIgnoreCase(st) || "UP".equalsIgnoreCase(st);
                boolean warn = "WARNING".equalsIgnoreCase(st) || "DEGRADED".equalsIgnoreCase(st);
                l.setForeground(healthy ? UIHelper.APPROVED_GREEN : (warn ? UIHelper.PENDING_YELLOW : UIHelper.REJECTED_RED));
                l.setText(healthy ? "SAUDAVEL" : (warn ? "ATENCAO" : "CRITICO"));
                l.setFont(l.getFont().deriveFont(Font.BOLD));
                return l;
            }
        };
    }

    private static void configureColumns(JTable table, int... widths) {
        for (int i = 0; i < widths.length && i < table.getColumnModel().getColumnCount(); i++) {
            table.getColumnModel().getColumn(i).setPreferredWidth(widths[i]);
        }
        UIHelper.ensureHeadersFit(table);
    }

    private ModernPanel createTabCard(String titleText) {
        ModernPanel panel = new ModernPanel();
        panel.setLayout(new BorderLayout(0, 8));
        panel.setBorder(new EmptyBorder(10, 12, 10, 12));
        JLabel title = new JLabel(titleText);
        title.setFont(new Font("Segoe UI", Font.BOLD, 13));
        title.setForeground(UIHelper.TEXT_LIGHT);
        panel.add(title, BorderLayout.NORTH);
        return panel;
    }

    private DefaultTableModel createNonEditableModel(String[] cols) {
        return new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int r, int c) {
                return false;
            }
        };
    }

    private JTable createDarkTable(DefaultTableModel model) {
        JTable table = new JTable(model);
        table.setRowHeight(28);
        table.setBackground(UIHelper.BG_DARK);
        table.setForeground(UIHelper.TEXT_LIGHT);
        table.setGridColor(UIHelper.BORDER);
        table.getTableHeader().setBackground(UIHelper.BG_CARD);
        table.getTableHeader().setForeground(UIHelper.TEXT_MUTED);
        table.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 12));
        return table;
    }

    private JScrollPane wrapScroll(JTable table) {
        JScrollPane scroll = new JScrollPane(table);
        scroll.setBorder(BorderFactory.createLineBorder(UIHelper.BORDER, 1));
        scroll.getViewport().setBackground(UIHelper.BG_DARK);
        return scroll;
    }
}
