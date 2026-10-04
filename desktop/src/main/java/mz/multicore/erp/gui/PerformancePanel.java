package mz.multicore.erp.gui;

import mz.multicore.erp.desktop.client.PerformanceApiClient;
import mz.multicore.erp.desktop.session.DesktopSession;
import mz.multicore.erp.desktop.session.DesktopSessionStore;
import mz.multicore.erp.gui.components.InlineFeedbackPanel;
import mz.multicore.erp.gui.components.UIHelper;
import mz.multicore.erp.gui.performance.BonusTab;
import mz.multicore.erp.gui.performance.GoalsTab;
import mz.multicore.erp.gui.performance.ProgressTab;
import mz.multicore.erp.gui.performance.RankingTab;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

/**
 * Centro de Desempenho Comercial (CDC).
 * Reúne metas comerciais, progresso em tempo real, ranking de vendedores e gestão de prémios/bónus.
 */
public class PerformancePanel extends JPanel {

    private final JTabbedPane tabbedPane;
    private final GoalsTab goalsTab;
    private final ProgressTab progressTab;
    private final RankingTab rankingTab;
    private final BonusTab bonusTab;
    private final InlineFeedbackPanel feedback = new InlineFeedbackPanel();

    public PerformancePanel(PerformanceApiClient apiClient, DesktopSessionStore sessionStore) {
        this(apiClient, sessionStore.requireSession());
    }

    public PerformancePanel(PerformanceApiClient apiClient, DesktopSession session) {
        setLayout(new BorderLayout(0, 15));
        setBackground(UIHelper.BG_DARK);
        setBorder(new EmptyBorder(25, 25, 25, 25));

        JPanel north = new JPanel();
        north.setOpaque(false);
        north.setLayout(new BoxLayout(north, BoxLayout.Y_AXIS));

        JComponent heading = UIHelper.createHeading("Centro de Desempenho Comercial");
        heading.setAlignmentX(Component.LEFT_ALIGNMENT);
        feedback.setAlignmentX(Component.LEFT_ALIGNMENT);

        north.add(heading);
        north.add(feedback);
        add(north, BorderLayout.NORTH);

        this.progressTab = new ProgressTab(apiClient, session);
        this.bonusTab = new BonusTab(apiClient, session);
        this.rankingTab = new RankingTab(apiClient, session);
        this.goalsTab = new GoalsTab(apiClient, session, () -> {
            progressTab.reload();
            bonusTab.reload();
        });

        tabbedPane = new JTabbedPane();
        UIHelper.styleTabbedPaneMulticore(tabbedPane);

        tabbedPane.addTab("Metas Comerciais", UIHelper.icon("fas-bullseye", 16, UIHelper.ACCENT_BLUE), goalsTab);
        tabbedPane.addTab("Progresso em Tempo Real", UIHelper.icon("fas-chart-line", 16, UIHelper.APPROVED_GREEN), progressTab);
        tabbedPane.addTab("Ranking da Equipa", UIHelper.icon("fas-trophy", 16, UIHelper.PENDING_YELLOW), rankingTab);
        tabbedPane.addTab("Prémios & Bónus", UIHelper.icon("fas-award", 16, UIHelper.ACCENT_PINK), bonusTab);

        tabbedPane.addChangeListener(e -> reloadActiveTab());

        add(tabbedPane, BorderLayout.CENTER);
    }

    public void onPanelSelected() {
        reloadActiveTab();
    }

    public void selectTab(int index) {
        if (index >= 0 && index < tabbedPane.getTabCount()) {
            tabbedPane.setSelectedIndex(index);
        }
    }

    private void reloadActiveTab() {
        int index = tabbedPane.getSelectedIndex();
        switch (index) {
            case 0 -> goalsTab.reload();
            case 1 -> progressTab.reload();
            case 2 -> rankingTab.reload();
            case 3 -> bonusTab.reload();
        }
    }
}
