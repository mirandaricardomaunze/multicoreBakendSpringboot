package mz.multicore.erp.gui;

import mz.multicore.erp.gui.components.*;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.function.Consumer;

/** Aba 2 do Dashboard: Cockpit Operacional 360°, Alertas e Pulso Operacional/Estratégico. */
final class DashboardOperationsTab {

    private final Consumer<String> navigationHandler;
    private final StrategicPulseWidget strategicPulseWidget;
    private final OperationalPulseWidget operationalPulseWidget;

    private JLabel approvalsValLabel;
    private JLabel ticketsValLabel;
    private JLabel stockAlertsLabel;
    private JLabel expiryAlertsLabel;
    private JLabel expiryAlertsSub;

    DashboardOperationsTab(
            Consumer<String> navigationHandler,
            StrategicPulseWidget strategicPulseWidget,
            OperationalPulseWidget operationalPulseWidget
    ) {
        this.navigationHandler = navigationHandler;
        this.strategicPulseWidget = strategicPulseWidget;
        this.operationalPulseWidget = operationalPulseWidget;
    }

    public JPanel buildTab() {
        JPanel tab2 = new JPanel(new BorderLayout(0, 12));
        tab2.setOpaque(false);
        tab2.setBorder(new EmptyBorder(12, 4, 4, 4));

        JPanel operationalKpiGrid = KpiCard.createGrid(4, 10, 10);

        approvalsValLabel = KpiCard.valueLabel("0 Pedidos", 19);
        operationalKpiGrid.add(buildKpiCard("APROVAÇÕES PENDENTES", "fas-clipboard-check", UIHelper.KPI_WARNING_SOFT,
                approvalsValLabel, null, UIHelper.KPI_WARNING_DARK, UIHelper.KPI_WARNING_END,
                "Clique para abrir Aprovações", "approvals"));

        ticketsValLabel = KpiCard.valueLabel("0 Tickets", 19);
        operationalKpiGrid.add(buildKpiCard("SUPORTE CRM / ASSISTÊNCIAS", "fas-headset", UIHelper.KPI_NEUTRAL_SOFT,
                ticketsValLabel, null, UIHelper.KPI_NEUTRAL_DARK, UIHelper.KPI_NEUTRAL_END,
                "Clique para abrir CRM & Assistência", "crm"));

        stockAlertsLabel = KpiCard.valueLabel("0 Artigos", 19);
        JLabel stockAlertsSub = new JLabel("Qtd < 5 un no armazém");
        stockAlertsSub.setFont(new Font(UIHelper.FONT, Font.PLAIN, 10));
        stockAlertsSub.setForeground(UIHelper.KPI_DANGER_SOFT);
        operationalKpiGrid.add(buildKpiCard("ALERTAS DE STOCK", "fas-exclamation-triangle", UIHelper.KPI_DANGER_SOFT,
                stockAlertsLabel, stockAlertsSub, UIHelper.KPI_DANGER_DARK, UIHelper.KPI_DANGER_END,
                "Clique para abrir Gestão de Stock", "stock"));

        expiryAlertsLabel = KpiCard.valueLabel("0 Lotes", 19);
        expiryAlertsSub = new JLabel("Vencimento ≤ " + DashboardDataCoordinator.EXPIRY_ALERT_DAYS + " dias");
        expiryAlertsSub.setFont(new Font(UIHelper.FONT, Font.PLAIN, 10));
        expiryAlertsSub.setForeground(UIHelper.KPI_ORANGE_SOFT);
        operationalKpiGrid.add(buildKpiCard("ALERTAS DE VALIDADE", "fas-calendar-times", UIHelper.KPI_ORANGE_SOFT,
                expiryAlertsLabel, expiryAlertsSub, UIHelper.KPI_ORANGE_DARK, UIHelper.KPI_ORANGE_END,
                "Clique para abrir Gestão de Lotes / Stock", "stock"));

        tab2.add(operationalKpiGrid, BorderLayout.NORTH);

        JPanel tab2Center = new JPanel();
        tab2Center.setOpaque(false);
        tab2Center.setLayout(new BoxLayout(tab2Center, BoxLayout.Y_AXIS));

        if (strategicPulseWidget != null) {
            tab2Center.add(strategicPulseWidget);
            tab2Center.add(Box.createVerticalStrut(14));
        }
        if (operationalPulseWidget != null) {
            tab2Center.add(operationalPulseWidget);
        }

        tab2.add(new ArrowScrollPanel(tab2Center), BorderLayout.CENTER);
        return tab2;
    }

    private ModernPanel buildKpiCard(String title, String iconCode, Color titleColor,
                                     JLabel valueLabel, JLabel subLabel,
                                     Color gradientStart, Color gradientEnd,
                                     String tooltip, String targetModule) {
        ModernPanel card = KpiCard.create(title, iconCode, titleColor, valueLabel, subLabel, gradientStart, gradientEnd);
        if (navigationHandler != null && targetModule != null) {
            KpiCard.makeInteractive(card, tooltip, () -> navigationHandler.accept(targetModule));
        }
        return card;
    }

    void applyData(DashboardDataCoordinator.DashboardData data) {
        approvalsValLabel.setText(data.approvalCount() + " Pedidos");
        ticketsValLabel.setText(data.ticketCount() + " Abertos");
        stockAlertsLabel.setText(data.lowStockCount() + " Artigo" + (data.lowStockCount() == 1 ? "" : "s"));
        expiryAlertsLabel.setText(data.expiringCount() + " Lote" + (data.expiringCount() == 1 ? "" : "s"));
        expiryAlertsSub.setText(String.format("%d vencido%s · %d a vencer",
                data.expiredCount(), data.expiredCount() == 1 ? "" : "s", data.soonCount()));
    }
}
