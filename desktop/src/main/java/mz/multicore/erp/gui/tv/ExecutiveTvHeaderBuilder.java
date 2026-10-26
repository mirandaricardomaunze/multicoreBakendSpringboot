package mz.multicore.erp.gui.tv;

import mz.multicore.erp.gui.components.ModernButton;
import mz.multicore.erp.gui.components.UIHelper;
import mz.multicore.erp.modules.purchases.dto.BusinessUnitDTO;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Construtor do cabeçalho corporativo em duas linhas para o painel de TV Executiva.
 */
public class ExecutiveTvHeaderBuilder {

    public interface HeaderActions {
        void onToggleTheme();
        void onOpenGoals();
        void onOpenSellers();
        void onToggleFullScreen();
        void onClose();
        void onPeriodSelected(ExecutiveTvDashboardDialog.Period period);
        void onBusinessUnitChanged(int selectedIndex);
        void onToggleSlideshow();
    }

    private final HeaderActions actions;
    private JPanel container;
    private JLabel clockLabel;
    private ModernButton themeToggleBtn;
    private ModernButton fullScreenBtn;
    private ModernButton autoSlideBtn;
    private JComboBox<String> businessUnitCombo;
    private final Map<ExecutiveTvDashboardDialog.Period, ModernButton> periodButtons = new EnumMap<>(ExecutiveTvDashboardDialog.Period.class);

    public ExecutiveTvHeaderBuilder(HeaderActions actions) {
        this.actions = actions;
    }

    public JPanel build(boolean lightMode, ExecutiveTvDashboardDialog.Period currentPeriod) {
        container = new JPanel();
        container.setLayout(new BoxLayout(container, BoxLayout.Y_AXIS));
        container.setOpaque(true);
        container.setBackground(ExecutiveTvTheme.getHeaderBgColor(lightMode));
        container.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, ExecutiveTvTheme.getCardBorderColor(lightMode)),
                new EmptyBorder(8, 16, 8, 16)
        ));

        container.add(buildRow1(lightMode));
        container.add(Box.createVerticalStrut(8));
        container.add(buildRow2(lightMode, currentPeriod));
        return container;
    }

    private JPanel buildRow1(boolean lightMode) {
        JPanel row1 = new JPanel(new BorderLayout(12, 0));
        row1.setOpaque(false);

        JPanel brandBox = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        brandBox.setOpaque(false);
        brandBox.add(new JLabel(UIHelper.icon("fas-tv", 16, UIHelper.ACCENT_BLUE)));

        JLabel brandLbl = new JLabel("MULTICORE ERP");
        brandLbl.setFont(new Font(UIHelper.FONT, Font.BOLD, 15));
        brandLbl.setForeground(ExecutiveTvTheme.getTextLightColor(lightMode));
        brandBox.add(brandLbl);

        JLabel badgeLbl = new JLabel("  SALA DE REUNIÕES  ");
        badgeLbl.setFont(new Font(UIHelper.FONT, Font.BOLD, 10));
        badgeLbl.setForeground(UIHelper.PENDING_YELLOW);
        badgeLbl.setOpaque(true);
        badgeLbl.setBackground(new Color(245, 158, 11, 25));
        badgeLbl.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(245, 158, 11, 80), 1, true),
                BorderFactory.createEmptyBorder(2, 6, 2, 6)
        ));
        brandBox.add(badgeLbl);
        row1.add(brandBox, BorderLayout.WEST);

        clockLabel = new JLabel("—");
        clockLabel.setFont(new Font(UIHelper.FONT, Font.BOLD, 13));
        clockLabel.setForeground(ExecutiveTvTheme.getTextLightColor(lightMode));
        clockLabel.setHorizontalAlignment(JLabel.CENTER);
        row1.add(clockLabel, BorderLayout.CENTER);

        JPanel sysControls = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        sysControls.setOpaque(false);

        themeToggleBtn = createBtn(lightMode ? "Tema Escuro" : "Tema Claro", lightMode ? "fas-moon" : "fas-sun", UIHelper.PENDING_YELLOW, 115, actions::onToggleTheme);
        fullScreenBtn = createBtn("Tela Cheia", "fas-expand", UIHelper.ACCENT_BLUE, 105, actions::onToggleFullScreen);

        sysControls.add(themeToggleBtn);
        sysControls.add(createBtn("Metas", "fas-bullseye", UIHelper.PENDING_YELLOW, 85, actions::onOpenGoals));
        sysControls.add(createBtn("Vendedores", "fas-users", UIHelper.ACCENT_CYAN, 105, actions::onOpenSellers));
        sysControls.add(fullScreenBtn);
        sysControls.add(createBtn("Sair", "fas-times", UIHelper.REJECTED_RED, 75, actions::onClose));

        row1.add(sysControls, BorderLayout.EAST);
        return row1;
    }

    private ModernButton createBtn(String title, String icon, Color iconColor, int width, Runnable action) {
        ModernButton btn = UIHelper.createSecondaryButton(title);
        btn.setIcon(UIHelper.icon(icon, 12, iconColor));
        btn.setPreferredSize(new Dimension(width, 28));
        btn.addActionListener(e -> action.run());
        return btn;
    }

    private JPanel buildRow2(boolean lightMode, ExecutiveTvDashboardDialog.Period currentPeriod) {
        JPanel row2 = new JPanel(new BorderLayout(12, 0));
        row2.setOpaque(false);

        JPanel periodBox = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        periodBox.setOpaque(false);

        JLabel perLbl = new JLabel("Período: ");
        perLbl.setFont(new Font(UIHelper.FONT, Font.BOLD, 11));
        perLbl.setForeground(ExecutiveTvTheme.getTextMutedColor(lightMode));
        periodBox.add(perLbl);

        for (ExecutiveTvDashboardDialog.Period p : ExecutiveTvDashboardDialog.Period.values()) {
            ModernButton btn = UIHelper.createSecondaryButton(p.getLabel());
            btn.setFont(new Font(UIHelper.FONT, Font.BOLD, 11));
            btn.setPreferredSize(new Dimension(btn.getPreferredSize().width + 10, 26));
            btn.addActionListener(e -> actions.onPeriodSelected(p));
            periodButtons.put(p, btn);
            periodBox.add(btn);
        }
        updatePeriodStyles(currentPeriod, lightMode);
        row2.add(periodBox, BorderLayout.WEST);

        JPanel filterControls = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        filterControls.setOpaque(false);

        JLabel buLbl = new JLabel("Unidade de Negócio: ");
        buLbl.setFont(new Font(UIHelper.FONT, Font.BOLD, 11));
        buLbl.setForeground(ExecutiveTvTheme.getTextMutedColor(lightMode));
        filterControls.add(buLbl);

        businessUnitCombo = new JComboBox<>(new String[]{"Todas as Unidades (Visão Consolidada)"});
        businessUnitCombo.setFont(new Font(UIHelper.FONT, Font.PLAIN, 12));
        businessUnitCombo.setPreferredSize(new Dimension(240, 28));
        UIHelper.styleComboBox(businessUnitCombo);
        businessUnitCombo.addActionListener(e -> actions.onBusinessUnitChanged(businessUnitCombo.getSelectedIndex()));
        filterControls.add(businessUnitCombo);

        autoSlideBtn = UIHelper.createSecondaryButton("Rodízio TV (15s)");
        autoSlideBtn.setIcon(UIHelper.icon("fas-play", 11, UIHelper.APPROVED_GREEN));
        autoSlideBtn.setFont(new Font(UIHelper.FONT, Font.BOLD, 11));
        autoSlideBtn.setPreferredSize(new Dimension(135, 28));
        autoSlideBtn.addActionListener(e -> actions.onToggleSlideshow());
        filterControls.add(autoSlideBtn);

        row2.add(filterControls, BorderLayout.EAST);
        return row2;
    }

    public void updatePeriodStyles(ExecutiveTvDashboardDialog.Period currentPeriod, boolean lightMode) {
        for (Map.Entry<ExecutiveTvDashboardDialog.Period, ModernButton> entry : periodButtons.entrySet()) {
            boolean active = entry.getKey() == currentPeriod;
            ModernButton btn = entry.getValue();
            if (active) {
                btn.setColors(UIHelper.ACCENT_BLUE, UIHelper.ACCENT_BLUE_HOVER);
                btn.setForeground(Color.WHITE);
                btn.setBorder(BorderFactory.createEmptyBorder(4, 10, 4, 10));
            } else {
                btn.setColors(lightMode ? Color.WHITE : new Color(30, 41, 59), lightMode ? new Color(241, 245, 249) : new Color(51, 65, 85));
                btn.setForeground(ExecutiveTvTheme.getTextLightColor(lightMode));
                btn.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(ExecutiveTvTheme.getCardBorderColor(lightMode), 1, true), BorderFactory.createEmptyBorder(3, 9, 3, 9)));
            }
        }
    }

    public void applyTheme(boolean lightMode) {
        if (container != null) {
            container.setBackground(ExecutiveTvTheme.getHeaderBgColor(lightMode));
            container.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, ExecutiveTvTheme.getCardBorderColor(lightMode)), new EmptyBorder(8, 16, 8, 16)));
        }
        if (themeToggleBtn != null) {
            themeToggleBtn.setText(lightMode ? "Tema Escuro" : "Tema Claro");
            themeToggleBtn.setIcon(UIHelper.icon(lightMode ? "fas-moon" : "fas-sun", 12, UIHelper.PENDING_YELLOW));
        }
        if (clockLabel != null) clockLabel.setForeground(ExecutiveTvTheme.getTextLightColor(lightMode));
    }

    public void setClockText(String text) { if (clockLabel != null) clockLabel.setText(text); }

    public void setFullScreenState(boolean isFullScreen) {
        if (fullScreenBtn != null) {
            fullScreenBtn.setText(isFullScreen ? "Janela" : "Tela Cheia");
            fullScreenBtn.setIcon(UIHelper.icon(isFullScreen ? "fas-compress" : "fas-expand", 12, UIHelper.ACCENT_BLUE));
        }
    }

    public void updateSlideshowButton(boolean active, int secondsRemaining) {
        if (autoSlideBtn == null) return;
        autoSlideBtn.setIcon(UIHelper.icon(active ? "fas-pause" : "fas-play", 11, active ? UIHelper.PENDING_YELLOW : UIHelper.APPROVED_GREEN));
        autoSlideBtn.setText(active ? String.format("Rodízio (%ds)", secondsRemaining) : "Rodízio TV (15s)");
    }

    public void populateBusinessUnits(List<BusinessUnitDTO> units) {
        if (businessUnitCombo == null) return;
        String selected = (String) businessUnitCombo.getSelectedItem();
        businessUnitCombo.removeAllItems();
        businessUnitCombo.addItem("Todas as Unidades (Visão Consolidada)");
        if (units != null) {
            for (BusinessUnitDTO bu : units) businessUnitCombo.addItem(bu.name());
        }
        if (selected != null) businessUnitCombo.setSelectedItem(selected);
    }

    public JComboBox<String> getBusinessUnitCombo() { return businessUnitCombo; }
}
