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
import javax.swing.SwingConstants;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Cabeçalho do painel de TV em duas linhas: identidade, contexto e relógio; depois o período
 * (controlo segmentado), a unidade, o rodízio e as acções em ícones com dica.
 * O cabeçalho é reconstruído ao mudar de tema; o selector de unidade é sempre a mesma instância.
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

    public static final String ALL_UNITS = "Todas as unidades";
    private static final Locale PT = Locale.forLanguageTag("pt-MZ");
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm", PT);
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("EEEE, d 'de' MMMM 'de' yyyy", PT);

    private final HeaderActions actions;
    private final Map<ExecutiveTvDashboardDialog.Period, ModernButton> periodButtons =
            new EnumMap<>(ExecutiveTvDashboardDialog.Period.class);
    private final JComboBox<String> businessUnitCombo = new JComboBox<>(new String[]{ALL_UNITS});
    private JPanel container;
    private JLabel timeLabel, dateLabel, contextLabel;
    private ModernButton slideshowButton;
    private boolean lightMode, fullScreen, slideshowActive;
    private int slideshowSeconds = ExecutiveTvTimerCoordinator.SLIDESHOW_DURATION_SECONDS;
    private ExecutiveTvDashboardDialog.Period period = ExecutiveTvDashboardDialog.Period.ESTE_MES;
    private String context = "Visão consolidada";
    private LocalDateTime lastClock = LocalDateTime.now();

    public ExecutiveTvHeaderBuilder(HeaderActions actions) {
        this.actions = actions;
        businessUnitCombo.addActionListener(e -> actions.onBusinessUnitChanged(businessUnitCombo.getSelectedIndex()));
    }

    public JPanel build(boolean lightMode, ExecutiveTvDashboardDialog.Period currentPeriod) {
        container = new JPanel();
        container.setLayout(new BoxLayout(container, BoxLayout.Y_AXIS));
        container.setOpaque(true);
        this.period = currentPeriod;
        applyTheme(lightMode);
        return container;
    }

    /** Reconstrói o cabeçalho com as cores do tema, mantendo o estado actual. */
    public void applyTheme(boolean lightMode) {
        this.lightMode = lightMode;
        if (container == null) return;
        container.removeAll();
        container.setBackground(ExecutiveTvTheme.getHeaderBgColor(lightMode));
        container.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, ExecutiveTvTheme.getCardBorderColor(lightMode)),
                BorderFactory.createEmptyBorder(14, 20, 14, 20)));
        container.add(buildIdentityRow());
        container.add(Box.createVerticalStrut(14));
        container.add(buildToolbarRow());
        setClock(lastClock);
        container.revalidate();
        container.repaint();
    }

    private JPanel buildIdentityRow() {
        JPanel row = ExecutiveTvComponents.transparent(new BorderLayout(16, 0));
        JPanel brand = ExecutiveTvComponents.transparent(new FlowLayout(FlowLayout.LEFT, 12, 0));
        brand.add(ExecutiveTvComponents.iconChip("fas-chart-line", UIHelper.ACCENT_BLUE, lightMode));
        JPanel titles = ExecutiveTvComponents.transparent(new GridLayout(2, 1, 0, 0));
        titles.add(ExecutiveTvComponents.label("Painel Executivo", Font.BOLD, 20, ExecutiveTvTheme.getTextLightColor(lightMode)));
        contextLabel = ExecutiveTvComponents.label(contextText(), Font.PLAIN, 13, ExecutiveTvTheme.getTextMutedColor(lightMode));
        titles.add(contextLabel);
        brand.add(titles);
        row.add(brand, BorderLayout.WEST);

        JPanel clock = ExecutiveTvComponents.transparent(new GridLayout(2, 1, 0, 0));
        timeLabel = ExecutiveTvComponents.label("", Font.BOLD, 24, ExecutiveTvTheme.getTextLightColor(lightMode));
        timeLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        dateLabel = ExecutiveTvComponents.label("", Font.PLAIN, 13, ExecutiveTvTheme.getTextMutedColor(lightMode));
        dateLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        clock.add(timeLabel);
        clock.add(dateLabel);
        row.add(clock, BorderLayout.EAST);
        return row;
    }

    private JPanel buildToolbarRow() {
        JPanel row = ExecutiveTvComponents.transparent(new BorderLayout(16, 0));
        row.add(buildPeriodSegments(), BorderLayout.WEST);

        JPanel tools = ExecutiveTvComponents.transparent(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        ExecutiveTvComponents.styleCombo(businessUnitCombo, 250, lightMode);
        businessUnitCombo.setToolTipText("Unidade de negócio apresentada");
        tools.add(businessUnitCombo);
        slideshowButton = ExecutiveTvComponents.ghostButton("", "fas-play", UIHelper.APPROVED_GREEN,
                actions::onToggleSlideshow, lightMode);
        updateSlideshowButton(slideshowActive, slideshowSeconds);
        tools.add(slideshowButton);
        tools.add(ExecutiveTvComponents.divider(lightMode));
        tools.add(ExecutiveTvComponents.iconButton("fas-bullseye", "Metas por unidade", actions::onOpenGoals, lightMode));
        tools.add(ExecutiveTvComponents.iconButton("fas-users", "Desempenho dos vendedores", actions::onOpenSellers, lightMode));
        tools.add(ExecutiveTvComponents.iconButton(lightMode ? "fas-moon" : "fas-sun",
                lightMode ? "Tema escuro" : "Tema claro", actions::onToggleTheme, lightMode));
        tools.add(ExecutiveTvComponents.iconButton(fullScreen ? "fas-compress" : "fas-expand",
                fullScreen ? "Sair do ecrã inteiro (F11)" : "Ecrã inteiro (F11)", actions::onToggleFullScreen, lightMode));
        tools.add(ExecutiveTvComponents.iconButton("fas-times", "Fechar o painel (Esc)", actions::onClose, lightMode));
        row.add(tools, BorderLayout.EAST);
        return row;
    }

    private JPanel buildPeriodSegments() {
        JPanel track = new ExecutiveTvComponents.RoundedSurface(new FlowLayout(FlowLayout.LEFT, 2, 0),
                ExecutiveTvTheme.getSurfaceAltColor(lightMode), ExecutiveTvTheme.getCardBorderColor(lightMode), 12);
        track.setBorder(BorderFactory.createEmptyBorder(3, 3, 3, 3));
        periodButtons.clear();
        for (ExecutiveTvDashboardDialog.Period p : ExecutiveTvDashboardDialog.Period.values()) {
            ModernButton segment = new ModernButton(p.getShortLabel());
            segment.setFont(ExecutiveTvComponents.font(Font.BOLD, 13));
            segment.setCornerRadius(9);
            segment.setPreferredSize(new Dimension(92, ExecutiveTvComponents.CONTROL_HEIGHT - 6));
            segment.addActionListener(e -> actions.onPeriodSelected(p));
            periodButtons.put(p, segment);
            track.add(segment);
        }
        updatePeriodStyles(period, lightMode);
        return track;
    }

    public void updatePeriodStyles(ExecutiveTvDashboardDialog.Period currentPeriod, boolean lightMode) {
        this.period = currentPeriod;
        Color idle = ExecutiveTvTheme.getSurfaceAltColor(lightMode);
        for (Map.Entry<ExecutiveTvDashboardDialog.Period, ModernButton> entry : periodButtons.entrySet()) {
            ModernButton segment = entry.getValue();
            if (entry.getKey() == currentPeriod) {
                segment.setColors(UIHelper.ACCENT_BLUE, UIHelper.ACCENT_BLUE_HOVER);
            } else {
                segment.setColors(idle, ExecutiveTvTheme.tint(ExecutiveTvTheme.getTextLightColor(lightMode), idle, 0.08));
                segment.setForeground(ExecutiveTvTheme.getTextMutedColor(lightMode));
            }
        }
        refreshContext();
    }

    public void setClock(LocalDateTime now) {
        lastClock = now;
        if (timeLabel != null) timeLabel.setText(now.format(TIME));
        if (dateLabel != null) {
            String date = now.format(DATE);
            dateLabel.setText(Character.toUpperCase(date.charAt(0)) + date.substring(1));
        }
    }

    public void setFullScreenState(boolean isFullScreen) {
        this.fullScreen = isFullScreen;
        applyTheme(lightMode);
    }

    public void updateSlideshowButton(boolean active, int secondsRemaining) {
        this.slideshowActive = active;
        this.slideshowSeconds = secondsRemaining;
        if (slideshowButton == null) return;
        slideshowButton.setText(active ? String.format("Rodízio · %ds", secondsRemaining) : "Rodízio");
        slideshowButton.setIcon(UIHelper.icon(active ? "fas-pause" : "fas-play", 13,
                active ? UIHelper.PENDING_YELLOW : UIHelper.APPROVED_GREEN));
        slideshowButton.setToolTipText(active ? "Pausar o rodízio entre unidades (Espaço)"
                : "Mostrar cada unidade durante " + ExecutiveTvTimerCoordinator.SLIDESHOW_DURATION_SECONDS + " s (Espaço)");
        slideshowButton.setPreferredSize(new Dimension(130, ExecutiveTvComponents.CONTROL_HEIGHT));
    }

    public void populateBusinessUnits(List<BusinessUnitDTO> units) {
        Object selected = businessUnitCombo.getSelectedItem();
        businessUnitCombo.removeAllItems();
        businessUnitCombo.addItem(ALL_UNITS);
        if (units != null) units.forEach(bu -> businessUnitCombo.addItem(bu.name()));
        if (selected != null) businessUnitCombo.setSelectedItem(selected);
        refreshContext();
    }

    /** Actualiza a linha de contexto (unidade · período) depois de mudar de vista. */
    public void refreshContext() {
        int index = businessUnitCombo.getSelectedIndex();
        context = index <= 0 ? "Visão consolidada" : String.valueOf(businessUnitCombo.getSelectedItem());
        if (contextLabel != null) contextLabel.setText(contextText());
    }

    private String contextText() {
        return context + "  ·  " + period.getLabel();
    }

    public JComboBox<String> getBusinessUnitCombo() { return businessUnitCombo; }
}
