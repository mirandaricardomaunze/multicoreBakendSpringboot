package mz.multicore.erp.gui.sector;

import mz.multicore.erp.gui.components.KpiCard;
import mz.multicore.erp.gui.components.UIHelper;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Page shell shared by the business-sector modules (Preços, Levantamentos, Obras): a header with the module
 * icon, title and purpose, a strip of live indicators, and the working area. It gives the sector modules the
 * same structure as the main modules so they read as first-class screens, not as tabs hidden elsewhere.
 */
public class ModulePage extends JPanel {

    private final Map<String, JLabel> indicators = new LinkedHashMap<>();
    private final JPanel kpiGrid;

    public ModulePage(String iconCode, Color accent, String title, String subtitle, String[][] kpis, JComponent content) {
        setLayout(new BorderLayout(0, 12));
        setBackground(UIHelper.BG_DARK);
        setBorder(new EmptyBorder(14, 16, 12, 16));

        JPanel header = new JPanel(new BorderLayout(12, 0));
        header.setOpaque(false);
        JLabel icon = new JLabel(UIHelper.icon(iconCode, 28, accent));
        header.add(icon, BorderLayout.WEST);
        JPanel text = new JPanel(new BorderLayout(0, 2));
        text.setOpaque(false);
        JLabel heading = UIHelper.createHeading(title);
        text.add(heading, BorderLayout.NORTH);
        text.add(ResponsiveSupport.wrappingText(subtitle, new Font(UIHelper.FONT, Font.PLAIN, 13), UIHelper.TEXT_MUTED),
                BorderLayout.CENTER);
        header.add(text, BorderLayout.CENTER);

        kpiGrid = KpiCard.createGrid(Math.max(1, kpis.length), 12, 12);
        kpiGrid.setOpaque(false);
        // Indicators fill one row on wide windows and wrap to two on a laptop instead of squeezing their text.
        ResponsiveSupport.adaptiveColumns(kpiGrid, (GridLayout) kpiGrid.getLayout(), 210, Math.max(1, kpis.length));
        for (String[] kpi : kpis) {
            JLabel value = KpiCard.valueLabel("—", 22);
            indicators.put(kpi[0], value);
            kpiGrid.add(KpiCard.createMetricCard(kpi[0], value, kpi[1], kpi[2], accentFor(kpi.length > 3 ? kpi[3] : null, accent)));
        }

        JPanel north = new JPanel(new BorderLayout(0, 12));
        north.setOpaque(false);
        north.add(header, BorderLayout.NORTH);
        if (kpis.length > 0) north.add(kpiGrid, BorderLayout.CENTER);
        add(north, BorderLayout.NORTH);
        add(content, BorderLayout.CENTER);
    }

    /** Updates one indicator by its title; unknown titles are ignored. */
    public void setIndicator(String title, String value) {
        JLabel label = indicators.get(title);
        if (label != null) label.setText(value == null ? "—" : value);
    }

    private static Color accentFor(String name, Color fallback) {
        if (name == null) return fallback;
        return switch (name) {
            case "green" -> UIHelper.APPROVED_GREEN;
            case "red" -> UIHelper.REJECTED_RED;
            case "amber" -> UIHelper.PENDING_YELLOW;
            case "blue" -> UIHelper.ACCENT_BLUE;
            default -> fallback;
        };
    }
}
