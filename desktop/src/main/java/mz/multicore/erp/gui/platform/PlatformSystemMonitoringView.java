package mz.multicore.erp.gui.platform;

import mz.multicore.erp.desktop.client.SystemMonitoringApiClient;
import mz.multicore.erp.gui.SystemMonitoringDialog;
import mz.multicore.erp.gui.components.ModernButton;
import mz.multicore.erp.gui.components.ModernPanel;
import mz.multicore.erp.gui.components.UIHelper;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

/**
 * Vista de acesso à consola de telemetria, integridade de microsserviços e base de dados.
 */
public class PlatformSystemMonitoringView extends JPanel {

    public PlatformSystemMonitoringView(SystemMonitoringApiClient monitoringApiClient) {
        setLayout(new GridBagLayout());
        setBackground(UIHelper.BG_DARK);

        ModernPanel card = new ModernPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBorder(new EmptyBorder(36, 48, 36, 48));

        JLabel icon = new JLabel(UIHelper.icon("fas-heartbeat", 48, UIHelper.ACCENT_CYAN));
        icon.setAlignmentX(Component.CENTER_ALIGNMENT);
        card.add(icon);
        card.add(Box.createVerticalStrut(16));

        JLabel title = new JLabel("Consola de Observabilidade & Diagnóstico");
        title.setFont(new Font("Segoe UI", Font.BOLD, 18));
        title.setForeground(UIHelper.TEXT_LIGHT);
        title.setAlignmentX(Component.CENTER_ALIGNMENT);
        card.add(title);
        card.add(Box.createVerticalStrut(8));

        JLabel desc = new JLabel("<html><center>Monitore em tempo real a integridade da persistência, pool HikariCP,<br>memória JVM, espaço em disco e subsistemas de faturação e auditoria.</center></html>");
        desc.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        desc.setForeground(UIHelper.TEXT_MUTED);
        desc.setAlignmentX(Component.CENTER_ALIGNMENT);
        card.add(desc);
        card.add(Box.createVerticalStrut(24));

        ModernButton btn = new ModernButton("Abrir Diagnóstico do Sistema", UIHelper.ACCENT_CYAN, UIHelper.ACCENT_CYAN.darker());
        btn.setIcon(UIHelper.icon("fas-external-link-alt", 14, Color.WHITE));
        btn.setAlignmentX(Component.CENTER_ALIGNMENT);
        btn.addActionListener(e -> new SystemMonitoringDialog(SwingUtilities.getWindowAncestor(this), monitoringApiClient).show());
        card.add(btn);

        add(card);
    }
}
