package mz.multicore.erp.gui.components;

import javax.swing.Box;
import javax.swing.JPanel;
import java.awt.Component;
import java.awt.Dimension;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * Coordenador dos itens favoritos na barra lateral retrátil.
 */
public class SidebarFavoritesCoordinator {

    private final JPanel container;
    private final Supplier<List<SidebarNavItem>> masterNavSupplier;
    private final List<SidebarNavItem> favoriteNavItems = new ArrayList<>();
    private SidebarSectionHeader favoriteHeader;

    public SidebarFavoritesCoordinator(JPanel container, Supplier<List<SidebarNavItem>> masterNavSupplier) {
        this.container = container;
        this.masterNavSupplier = masterNavSupplier;
    }

    public void rebuildFavorites(boolean collapsed) {
        container.removeAll();
        favoriteNavItems.clear();
        favoriteHeader = null;
        List<String> favs = SidebarFavoritesManager.getInstance().getFavorites();
        for (SidebarNavItem item : masterNavSupplier.get()) {
            item.setVisible(favs.stream().noneMatch(fav -> fav.equalsIgnoreCase(item.getLabel())));
        }
        if (!favs.isEmpty()) {
            favoriteHeader = new SidebarSectionHeader("FAVORITOS", false, UIHelper.icon("fas-star", 10, UIHelper.PENDING_YELLOW));
            favoriteHeader.setCollapsed(collapsed);
            favoriteHeader.setVisible(!collapsed);
            container.add(favoriteHeader);

            List<SidebarNavItem> masterItems = masterNavSupplier.get();
            for (String favLabel : favs) {
                SidebarNavItem match = masterItems.stream()
                        .filter(item -> favLabel.equalsIgnoreCase(item.getLabel()))
                        .findFirst().orElse(null);
                if (match != null) {
                    SidebarNavItem favItem = (match.getIcon() != null)
                            ? new SidebarNavItem(match.getIcon(), match.getLabel(), match.getAccent(), match.getOnClick())
                            : new SidebarNavItem(match.getIconGlyph(), match.getLabel(), match.getAccent(), match.getOnClick());
                    favItem.setCollapsed(collapsed);
                    favItem.setAlignmentX(Component.LEFT_ALIGNMENT);
                    favoriteNavItems.add(favItem);
                    container.add(favItem);
                    container.add(Box.createRigidArea(new Dimension(0, 2)));
                }
            }
            container.add(Box.createRigidArea(new Dimension(0, 6)));
        }
        container.revalidate();
        container.repaint();
    }

    public void setActive(String activeLabel) {
        for (SidebarNavItem item : favoriteNavItems) {
            item.setActive(matchesLabel(item, activeLabel));
        }
    }

    public void setBadge(String label, int count) {
        for (SidebarNavItem item : favoriteNavItems) {
            if (matchesLabel(item, label)) {
                item.setBadgeCount(count);
                break;
            }
        }
    }

    public void setCollapsed(boolean collapsed) {
        if (favoriteHeader != null) {
            favoriteHeader.setCollapsed(collapsed);
            favoriteHeader.setVisible(!collapsed);
        }
        for (SidebarNavItem item : favoriteNavItems) {
            item.setCollapsed(collapsed);
        }
    }

    public List<SidebarNavItem> getFavoriteNavItems() {
        return favoriteNavItems;
    }

    private boolean matchesLabel(SidebarNavItem item, String activeLabel) {
        String tooltip = item.getToolTipText();
        return (tooltip != null && tooltip.equalsIgnoreCase(activeLabel))
                || (item.getLabel() != null && item.getLabel().equalsIgnoreCase(activeLabel));
    }
}
