package mz.multicore.erp.gui;

import mz.multicore.erp.desktop.session.DesktopSession;
import mz.multicore.erp.gui.components.TopNavBar;
import mz.multicore.erp.gui.components.UIHelper;

import javax.swing.*;
import java.awt.*;

/** Construtor da barra superior (TopNavBar), botões de utilitários e chip de utilizador da janela principal. */
final class MainFrameTopBarBuilder {

    private final JLabel sessionUserLabel = new JLabel("—");
    private final JLabel sessionRoleLabel = new JLabel("—");

    MainFrameTopBarBuilder() {
        sessionUserLabel.setFont(new Font(UIHelper.FONT, Font.BOLD, 12));
        sessionUserLabel.setForeground(UIHelper.TEXT_LIGHT);
        sessionRoleLabel.setFont(new Font(UIHelper.FONT, Font.PLAIN, 10));
        sessionRoleLabel.setForeground(UIHelper.TEXT_MUTED);
    }

    TopNavBar build(boolean superAdmin, MainFrame frame, Runnable openSearch, Runnable openHistory,
                    JComponent notificationBell, JComponent subChip,
                    JComboBox<DesktopSession.CompanyAccess> companyCombo, boolean hasMultipleCompanies) {
        if (superAdmin) {
            TopNavBar bar = new TopNavBar("Consola da Plataforma", "");
            bar.setSearchAction(openSearch);
            bar.addTrailing(buildThemeToggle());
            bar.addTrailing(buildUserChip(frame));
            return bar;
        }

        TopNavBar bar = new TopNavBar("Painel Inicial", "");
        bar.setSearchAction(openSearch);
        bar.addTrailing(buildHistoryButton(openHistory));
        bar.addTrailing(buildThemeToggle());
        if (notificationBell != null) bar.addTrailing(notificationBell);
        if (subChip != null) bar.addTrailing(subChip);
        if (hasMultipleCompanies && companyCombo != null) bar.addTrailing(companyCombo);
        bar.addTrailing(buildUserChip(frame));
        return bar;
    }

    void updateUserProfile(String displayName, String humanRole) {
        sessionUserLabel.setText(displayName);
        sessionRoleLabel.setText(humanRole);
    }

    void updateRole(String humanRole) {
        sessionRoleLabel.setText(humanRole);
    }

    private JComponent buildHistoryButton(Runnable openHistory) {
        JLabel btn = new JLabel(UIHelper.icon("fas-history", 18, UIHelper.TEXT_MUTED));
        btn.setToolTipText("Histórico de Itens Recentes (Ctrl+H)");
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setBorder(BorderFactory.createEmptyBorder(0, 6, 0, 6));
        btn.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override public void mousePressed(java.awt.event.MouseEvent e) { openHistory.run(); }
        });
        return btn;
    }

    private JComponent buildThemeToggle() {
        String code = UIHelper.isHighContrast() ? "fas-adjust" : (UIHelper.isLight() ? "fas-moon" : "fas-sun");
        String tip = UIHelper.isHighContrast()
                ? "Mudar para Tema Escuro (Atual: Alto Contraste)"
                : (UIHelper.isLight() ? "Mudar para Alto Contraste (Atual: Claro)" : "Mudar para Tema Claro (Atual: Escuro)");
        JLabel toggle = new JLabel(UIHelper.icon(code, 18, UIHelper.TEXT_MUTED));
        toggle.setToolTipText(tip);
        toggle.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        toggle.setBorder(BorderFactory.createEmptyBorder(0, 6, 0, 6));
        toggle.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override public void mousePressed(java.awt.event.MouseEvent e) { UIHelper.cycleTheme(); }
        });
        return toggle;
    }

    private JPanel buildUserChip(MainFrame frame) {
        JPanel chip = new JPanel(new BorderLayout(8, 0));
        chip.setOpaque(false);

        JLabel avatar = new JLabel(UIHelper.icon("fas-user-circle", 28, UIHelper.ACCENT));
        chip.add(avatar, BorderLayout.WEST);

        JPanel textStack = new JPanel();
        textStack.setOpaque(false);
        textStack.setLayout(new BoxLayout(textStack, BoxLayout.Y_AXIS));
        textStack.add(sessionUserLabel);
        textStack.add(sessionRoleLabel);
        chip.add(textStack, BorderLayout.CENTER);

        JButton logoutBtn = new JButton(UIHelper.icon("fas-sign-out-alt", 15, UIHelper.REJECTED_RED));
        logoutBtn.setToolTipText("Terminar Sessão (Logout)");
        logoutBtn.setFocusPainted(false);
        logoutBtn.setBorderPainted(false);
        logoutBtn.setContentAreaFilled(false);
        logoutBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        logoutBtn.addActionListener(e -> UIHelper.requestLogout(frame));
        chip.add(logoutBtn, BorderLayout.EAST);

        return chip;
    }
}
