package mz.multicore.erp.gui;

import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.desktop.client.HRApiClient;
import mz.multicore.erp.desktop.client.PrintApiClient;
import mz.multicore.erp.desktop.client.QualityApiClient;
import mz.multicore.erp.gui.components.*;
import mz.multicore.erp.gui.quality.QualityAssignmentsTab;
import mz.multicore.erp.gui.quality.QualityCatalogTab;
import mz.multicore.erp.gui.quality.QualityExpiredRadarTab;

import javax.swing.*;
import java.awt.*;

public class QualityPanel extends JPanel {

    private final QualityApiClient qualityApiClient;
    private final HRApiClient hrApiClient;
    private final PrintApiClient printApiClient;

    // Top KPIs (Linha horizontal única com altura uniforme de 96px)
    private final JLabel activeAssignmentsVal = new JLabel("0");
    private final JLabel expiringSoonVal = new JLabel("0");
    private final JLabel expiredVal = new JLabel("0");
    private final JLabel totalEquipmentsVal = new JLabel("0");

    private final ModernPanel activeAssignmentsKpi = KpiCard.createMetricCard("Atribuições Ativas", activeAssignmentsVal, "EPIs e ferramentas em uso", "fas-clipboard-check", UIHelper.ACCENT_BLUE);
    private final ModernPanel expiringSoonKpi = KpiCard.createMetricCard("A Expirar em 30d", expiringSoonVal, "Exigem renovação próxima", "fas-exclamation-circle", UIHelper.PENDING_YELLOW);
    private final ModernPanel expiredKpi = KpiCard.createMetricCard("Validade Expirada", expiredVal, "Situação irregular", "fas-exclamation-triangle", UIHelper.REJECTED_RED);
    private final ModernPanel totalEquipmentsKpi = KpiCard.createMetricCard("Equipamentos", totalEquipmentsVal, "Cadastrados no catálogo", "fas-boxes", UIHelper.APPROVED_GREEN);

    // Abas Especializadas
    private QualityAssignmentsTab assignmentsTab;
    private QualityExpiredRadarTab expiredRadarTab;
    private QualityCatalogTab catalogTab;

    public QualityPanel(QualityApiClient qualityApiClient, HRApiClient hrApiClient) {
        this(qualityApiClient, hrApiClient, null);
    }

    public QualityPanel(QualityApiClient qualityApiClient, HRApiClient hrApiClient, PrintApiClient printApiClient) {
        super(new BorderLayout(0, 10));
        this.qualityApiClient = qualityApiClient;
        this.hrApiClient = hrApiClient;
        this.printApiClient = printApiClient;

        setOpaque(false);
        setBorder(BorderFactory.createEmptyBorder(12, 16, 12, 16));

        initComponents();
    }

    public void onPanelSelected() {
        refreshAll();
    }

    private void initComponents() {
        // 1. Top KPIs (Linha horizontal única via KpiCard.createGrid)
        JPanel kpiGrid = KpiCard.createGrid(4, 8, 8);
        kpiGrid.add(activeAssignmentsKpi);
        kpiGrid.add(expiringSoonKpi);
        kpiGrid.add(expiredKpi);
        kpiGrid.add(totalEquipmentsKpi);
        add(kpiGrid, BorderLayout.NORTH);

        // 2. Abas Corporativas Desacopladas
        JTabbedPane tabbedPane = new JTabbedPane();
        UIHelper.styleTabbedPaneMulticore(tabbedPane);

        assignmentsTab = new QualityAssignmentsTab(qualityApiClient, hrApiClient, printApiClient, this::getCompanyId, this::refreshAll);
        expiredRadarTab = new QualityExpiredRadarTab(qualityApiClient, printApiClient, this::getCompanyId, this::refreshAll);
        catalogTab = new QualityCatalogTab(qualityApiClient, printApiClient, this::getCompanyId, this::refreshAll);

        tabbedPane.addTab("Atribuições a Colaboradores", assignmentsTab);
        tabbedPane.addTab("Radar de Validades Expiradas", expiredRadarTab);
        tabbedPane.addTab("Catálogo de Equipamentos", catalogTab);

        add(tabbedPane, BorderLayout.CENTER);
    }

    private Long getCompanyId() {
        Long cid = CurrentUserContext.findCurrentCompanyId();
        return cid != null ? cid : 1L;
    }

    public void refreshAll() {
        loadKpis();
        if (assignmentsTab != null) assignmentsTab.refresh();
        if (expiredRadarTab != null) expiredRadarTab.refresh();
        if (catalogTab != null) catalogTab.refresh();
    }

    private void loadKpis() {
        final Long companyId = getCompanyId();
        if (qualityApiClient == null) return;

        UIHelper.runWithProgress(this, "A carregar KPIs de qualidade...", () -> {
            return qualityApiClient.getQualityKpis(companyId);
        }, kpis -> {
            activeAssignmentsVal.setText(String.valueOf(kpis.activeAssignmentsCount()));
            expiringSoonVal.setText(String.valueOf(kpis.expiringSoonCount()));
            expiredVal.setText(String.valueOf(kpis.expiredWorkersCount()));
            totalEquipmentsVal.setText(String.valueOf(kpis.totalEquipmentsCount()));
        }, null);
    }
}
