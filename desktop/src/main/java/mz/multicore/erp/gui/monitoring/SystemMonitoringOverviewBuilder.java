package mz.multicore.erp.gui.monitoring;

import mz.multicore.erp.gui.components.ModernPanel;
import mz.multicore.erp.gui.components.SoundAlertManager;
import mz.multicore.erp.gui.components.UIHelper;
import mz.multicore.erp.modules.monitoring.dto.SystemHealthDTO;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

/**
 * Montador do cabeçalho executivo e dos cartões de métricas em tempo real (JVM, Disco, DB, Threads).
 */
public class SystemMonitoringOverviewBuilder {

    private final JLabel overallBadge = new JLabel("A VERIFICAR", SwingConstants.CENTER);
    private final JLabel uptimeLabel = new JLabel("A carregar telemetria do sistema...");
    private final JProgressBar memoryProgressBar = createProgressBar();
    private final JLabel memoryDetailLabel = createDetailLabel("— MB usados de — MB");
    private final JProgressBar storageProgressBar = createProgressBar();
    private final JLabel storageDetailLabel = createDetailLabel("— GB livres");
    private final JLabel dbEngineLabel = createDetailLabel("Motor: —");
    private final JLabel dbLatencyLabel = createDetailLabel("Latência de Ping: — ms");
    private final JLabel dbPoolLabel = createDetailLabel("Conexões no Pool: —");
    private final JLabel threadsLabel = createDetailLabel("Threads Activas: — (Pico: —)");
    private final JLabel jvmVersionLabel = createDetailLabel("JVM: —");

    public SystemMonitoringOverviewBuilder() {
        overallBadge.setOpaque(true);
        overallBadge.setBackground(UIHelper.BG_CARD);
        overallBadge.setForeground(UIHelper.TEXT_MUTED);
        overallBadge.setFont(new Font("Segoe UI", Font.BOLD, 12));
        overallBadge.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UIHelper.BORDER, 1), new EmptyBorder(6, 16, 6, 16)));
        uptimeLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        uptimeLabel.setForeground(UIHelper.TEXT_MUTED);
    }

    public JPanel buildHeader() {
        JPanel header = new JPanel(new BorderLayout(16, 0));
        header.setOpaque(false);

        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
        left.setOpaque(false);
        left.add(new JLabel(UIHelper.icon("fas-heartbeat", 30, UIHelper.ACCENT_CYAN)));

        JPanel titles = new JPanel(new GridLayout(2, 1, 0, 2));
        titles.setOpaque(false);
        JLabel title = new JLabel("Diagnóstico & Saúde Operacional");
        title.setFont(new Font("Segoe UI", Font.BOLD, 17));
        title.setForeground(UIHelper.TEXT_LIGHT);
        titles.add(title);
        titles.add(uptimeLabel);
        left.add(titles);

        header.add(left, BorderLayout.WEST);
        header.add(overallBadge, BorderLayout.EAST);
        return header;
    }

    public JPanel buildMetricsGrid() {
        JPanel grid = new JPanel(new GridLayout(2, 2, 10, 10));
        grid.setOpaque(false);
        grid.add(buildSubMetricCard("Memória Heap da JVM", "fas-memory", UIHelper.ACCENT, memoryProgressBar, memoryDetailLabel));
        grid.add(buildSubMetricCard("Espaço em Disco", "fas-hdd", UIHelper.ACCENT_CYAN, storageProgressBar, storageDetailLabel));

        ModernPanel dbCard = createCard("Base de Dados (HikariCP)", "fas-database", UIHelper.APPROVED_GREEN);
        JPanel dbBody = new JPanel(new GridLayout(3, 1, 0, 3));
        dbBody.setOpaque(false);
        dbBody.add(dbEngineLabel);
        dbBody.add(dbLatencyLabel);
        dbBody.add(dbPoolLabel);
        dbCard.add(dbBody, BorderLayout.CENTER);
        grid.add(dbCard);

        ModernPanel threadCard = createCard("Threads & Ambiente de Execução", "fas-microchip", UIHelper.ACCENT_ORANGE);
        JPanel threadBody = new JPanel(new GridLayout(2, 1, 0, 3));
        threadBody.setOpaque(false);
        threadBody.add(threadsLabel);
        threadBody.add(jvmVersionLabel);
        threadCard.add(threadBody, BorderLayout.CENTER);
        grid.add(threadCard);
        return grid;
    }

    private ModernPanel buildSubMetricCard(String title, String icon, Color color, JProgressBar pb, JLabel lbl) {
        ModernPanel card = createCard(title, icon, color);
        JPanel content = new JPanel(new BorderLayout(0, 6));
        content.setOpaque(false);
        content.add(pb, BorderLayout.NORTH);
        content.add(lbl, BorderLayout.CENTER);
        card.add(content, BorderLayout.CENTER);
        return card;
    }

    public void setUpdatingState() {
        overallBadge.setText("A ACTUALIZAR...");
        overallBadge.setForeground(UIHelper.TEXT_MUTED);
        overallBadge.setBackground(UIHelper.BG_CARD);
        overallBadge.setIcon(null);
    }

    public void setOfflineState() {
        setBadge("OFFLINE", null, UIHelper.REJECTED_RED, Color.WHITE);
        uptimeLabel.setText("Não foi possível estabelecer ligação ao serviço de monitoramento.");
    }

    public void setErrorState() {
        overallBadge.setText("ERRO");
        overallBadge.setIcon(null);
    }

    public void applyHealthData(SystemHealthDTO h) {
        String status = h.overallStatus();
        if ("HEALTHY".equalsIgnoreCase(status)) {
            setBadge("SISTEMA SAUDÁVEL", "fas-check-circle", UIHelper.APPROVED_GREEN, Color.WHITE);
        } else if ("WARNING".equalsIgnoreCase(status)) {
            setBadge("ATENÇÃO REQUERIDA", "fas-exclamation-triangle", UIHelper.PENDING_YELLOW, Color.BLACK);
        } else {
            setBadge("ESTADO CRÍTICO", "fas-times-circle", UIHelper.REJECTED_RED, Color.WHITE);
            SoundAlertManager.getInstance().playAlertAsync();
        }

        long days = h.uptimeSeconds() / 86400;
        long hours = (h.uptimeSeconds() % 86400) / 3600;
        long mins = (h.uptimeSeconds() % 3600) / 60;
        uptimeLabel.setText(String.format("Uptime do Servidor: %d d, %d h, %d min | Servidor: v%s | SO: %s",
                days, hours, mins, h.serverVersion(), h.osName()));

        var mem = h.memory();
        if (mem != null) {
            int pct = (int) Math.round(mem.heapUsedPercent());
            updateProgress(memoryProgressBar, pct, pct > 85 ? UIHelper.REJECTED_RED : (pct > 70 ? UIHelper.PENDING_YELLOW : UIHelper.ACCENT));
            memoryDetailLabel.setText(String.format("%d MB usados de %d MB alocados", mem.heapUsedBytes() / (1024 * 1024), mem.heapMaxBytes() / (1024 * 1024)));
        }

        var storage = h.storage();
        if (storage != null) {
            int pct = (int) Math.round(storage.usedPercent());
            updateProgress(storageProgressBar, pct, pct > 90 ? UIHelper.REJECTED_RED : (pct > 75 ? UIHelper.PENDING_YELLOW : UIHelper.ACCENT_CYAN));
            storageDetailLabel.setText(String.format("%d GB livres de %d GB totais", storage.freeBytes() / (1024 * 1024 * 1024), storage.totalBytes() / (1024 * 1024 * 1024)));
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
    }

    private void updateProgress(JProgressBar pb, int pct, Color fg) {
        pb.setValue(pct);
        pb.setString(pct + "%");
        pb.setForeground(fg);
    }

    private void setBadge(String text, String icon, Color bg, Color fg) {
        overallBadge.setText(text);
        overallBadge.setIcon(icon != null ? UIHelper.icon(icon, 12, fg) : null);
        overallBadge.setBackground(bg);
        overallBadge.setForeground(fg);
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
