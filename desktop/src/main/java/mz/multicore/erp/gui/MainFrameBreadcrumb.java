package mz.multicore.erp.gui;

import mz.multicore.erp.gui.components.TabTitles;
import mz.multicore.erp.gui.components.TopNavBar;

import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.SwingUtilities;
import java.awt.Component;

/** Keeps the top bar path "Module › Tab" in step with the tab selected on the visible screen. */
final class MainFrameBreadcrumb {

    private static final String FOLLOWED = "breadcrumb.followed";
    private final TopNavBar bar;
    private final JPanel cards;

    MainFrameBreadcrumb(TopNavBar bar, JPanel cards) {
        this.bar = bar;
        this.cards = cards;
    }

    void followVisibleCard() {
        if (bar == null) return;
        Component card = visibleCard();
        JTabbedPane tabs = card == null ? null : TabTitles.firstTabs(card);
        if (tabs != null && tabs.getClientProperty(FOLLOWED) == null) {
            tabs.putClientProperty(FOLLOWED, Boolean.TRUE);
            tabs.addChangeListener(event -> {
                Component current = visibleCard();
                if (current != null && SwingUtilities.isDescendingFrom(tabs, current)) bar.setSection(TabTitles.selectedTitle(tabs));
            });
        }
        bar.setSection(tabs == null ? null : TabTitles.selectedTitle(tabs));
    }

    private Component visibleCard() {
        for (Component card : cards.getComponents()) {
            if (card.isVisible()) return card;
        }
        return null;
    }
}
