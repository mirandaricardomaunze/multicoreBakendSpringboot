package mz.multicore.erp.gui.components;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Barra lateral retrátil executiva (Collapsible Sidebar).
 * Suporta modo expandido (240px) e modo recolhido/rail (64px), integra badges,
 * mini-card de perfil com avatar e indicador online, e adapta-se aos temas claro e escuro.
 */
public class CollapsibleSidebar extends JPanel {

    public static final int EXPANDED_WIDTH = 240;
    public static final int COLLAPSED_WIDTH = 64;

    private static final int ANIMATION_DURATION_MS = 140;
    private static final int ANIMATION_TICK_MS = 14;

    private static Color sidebarBg()     { return UIHelper.isLight() ? new Color(255, 255, 255) : new Color(24, 32, 47); }
    private static Color sidebarBorder() { return UIHelper.isLight() ? new Color(226, 232, 240) : new Color(45, 55, 72); }
    private static Color headerText()    { return UIHelper.isLight() ? new Color(31, 41, 55)    : new Color(243, 244, 246); }
    private static Color subText()       { return UIHelper.isLight() ? new Color(100, 116, 139) : new Color(203, 213, 225); }

    private final JPanel body = new JPanel();
    private final JPanel headerPanel = new JPanel();
    private final SidebarLogoBadge logoBadge = new SidebarLogoBadge(32);
    private final JPanel brandStack = new JPanel();
    private final JLabel brandLabel = new JLabel("MULTICORE");
    private final JLabel brandSubLabel = new JLabel("ERP Profissional");
    private final SidebarToggleButton toggleButton = new SidebarToggleButton();
    private final JPanel footerPanel = new JPanel();
    private final SidebarUserProfileCard userProfileCard = new SidebarUserProfileCard();
    private final JLabel footerVersionLabel = createFooterVersionLabel();

    private final JPanel favoritesContainer = new JPanel();
    private final SidebarFavoritesCoordinator favoritesCoordinator;
    private final List<SidebarNavItem> navItems = new ArrayList<>();
    private final List<JComponent> expandedOnlyComponents = new ArrayList<>();
    private final List<SidebarSectionHeader> sectionHeaders = new ArrayList<>();

    private boolean collapsed = false;
    private int currentWidth = EXPANDED_WIDTH;
    private Timer animator;
    private Consumer<Boolean> collapseListener;

    public CollapsibleSidebar(String brand, String subBrand) {
        setLayout(new BorderLayout());
        setBackground(sidebarBg());
        setPreferredSize(new Dimension(EXPANDED_WIDTH, 800));

        brandLabel.setText(brand != null && !brand.isBlank() ? brand : "MULTICORE");
        brandLabel.setFont(new Font(UIHelper.FONT, Font.BOLD, 17));
        brandLabel.setForeground(headerText());
        brandLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        brandSubLabel.setText(subBrand != null && !subBrand.isBlank() ? subBrand : "ERP Profissional");
        brandSubLabel.setFont(new Font(UIHelper.FONT, Font.PLAIN, 11));
        brandSubLabel.setForeground(subText());
        brandSubLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        brandSubLabel.setToolTipText("Empresa activa");

        brandStack.setOpaque(false);
        brandStack.setLayout(new BoxLayout(brandStack, BoxLayout.Y_AXIS));
        brandStack.add(brandLabel);
        brandStack.add(Box.createRigidArea(new Dimension(0, 1)));
        brandStack.add(brandSubLabel);

        toggleButton.addActionListener(e -> toggle());
        headerPanel.setOpaque(false);
        add(headerPanel, BorderLayout.NORTH);

        body.setOpaque(false);
        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
        body.setBorder(new EmptyBorder(4, 6, 10, 6));

        favoritesContainer.setOpaque(false);
        favoritesContainer.setLayout(new BoxLayout(favoritesContainer, BoxLayout.Y_AXIS));
        favoritesContainer.setAlignmentX(Component.LEFT_ALIGNMENT);
        body.add(favoritesContainer);

        favoritesCoordinator = new SidebarFavoritesCoordinator(favoritesContainer, () -> navItems);
        SidebarFavoritesManager.getInstance().addChangeListener(this::rebuildFavorites);

        JScrollPane bodyScroll = new JScrollPane(body);
        bodyScroll.setBorder(BorderFactory.createEmptyBorder());
        bodyScroll.setOpaque(false);
        bodyScroll.getViewport().setOpaque(false);
        bodyScroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        bodyScroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        bodyScroll.getVerticalScrollBar().setUnitIncrement(16);
        bodyScroll.getVerticalScrollBar().setPreferredSize(new Dimension(6, 0));
        add(bodyScroll, BorderLayout.CENTER);

        footerPanel.setOpaque(false);
        add(footerPanel, BorderLayout.SOUTH);

        applyChromeForState();
    }

    public void setSubBrand(String text) {
        brandSubLabel.setText(text == null || text.isBlank() ? "ERP Profissional" : text);
    }

    public void setUserProfile(String displayName, String role) {
        userProfileCard.updateProfile(displayName, role);
    }

    public void addSection(String title) {
        boolean showDivider = !sectionHeaders.isEmpty() || !navItems.isEmpty();
        SidebarSectionHeader section = new SidebarSectionHeader(title, showDivider);
        sectionHeaders.add(section);
        body.add(section);
    }

    public SidebarNavItem addItem(String iconGlyph, String label, Color accent, Runnable onClick) {
        return registerItem(new SidebarNavItem(iconGlyph, label, accent, () -> { setActive(label); if (onClick != null) onClick.run(); }));
    }

    public SidebarNavItem addItem(javax.swing.Icon icon, String label, Color accent, Runnable onClick) {
        return registerItem(new SidebarNavItem(icon, label, accent, () -> { setActive(label); if (onClick != null) onClick.run(); }));
    }

    private SidebarNavItem registerItem(SidebarNavItem item) {
        item.setAlignmentX(Component.LEFT_ALIGNMENT);
        navItems.add(item);
        body.add(item);
        body.add(Box.createRigidArea(new Dimension(0, 2)));
        rebuildFavorites();
        return item;
    }

    public void rebuildFavorites() {
        favoritesCoordinator.rebuildFavorites(collapsed);
    }

    public void setBadge(String label, int count) {
        for (SidebarNavItem item : navItems) {
            if (matchesLabel(item, label)) { item.setBadgeCount(count); break; }
        }
        favoritesCoordinator.setBadge(label, count);
    }

    public void addExpandedOnly(JComponent component) {
        component.setAlignmentX(Component.LEFT_ALIGNMENT);
        expandedOnlyComponents.add(component);
        body.add(component);
        body.add(Box.createRigidArea(new Dimension(0, 6)));
    }

    public void setActive(String activeLabel) {
        for (SidebarNavItem item : navItems) item.setActive(matchesLabel(item, activeLabel));
        favoritesCoordinator.setActive(activeLabel);
    }

    public List<SidebarNavItem> getNavItems() { return List.copyOf(navItems); }
    public void onCollapsedChanged(Consumer<Boolean> listener) { this.collapseListener = listener; }
    public boolean isCollapsedState() { return collapsed; }
    public void toggle() { setCollapsedState(!collapsed); }

    public void setCollapsedState(boolean target) {
        if (this.collapsed == target) return;
        this.collapsed = target;
        startAnimation();
    }

    private void startAnimation() {
        if (animator != null && animator.isRunning()) animator.stop();
        final int startW = currentWidth;
        final int endW = collapsed ? COLLAPSED_WIDTH : EXPANDED_WIDTH;
        final long startTime = System.currentTimeMillis();
        applyChromeForState();

        animator = new Timer(ANIMATION_TICK_MS, e -> {
            long elapsed = System.currentTimeMillis() - startTime;
            float t = Math.min(1f, elapsed / (float) ANIMATION_DURATION_MS);
            float eased = t < 0.5f ? 2 * t * t : -1 + (4 - 2 * t) * t;
            currentWidth = Math.round(startW + (endW - startW) * eased);
            setPreferredSize(new Dimension(currentWidth, getHeight()));
            revalidate();
            if (getParent() != null) getParent().repaint();
            if (t >= 1f) {
                ((Timer) e.getSource()).stop();
                currentWidth = endW;
                setPreferredSize(new Dimension(endW, getHeight()));
                revalidate();
                if (collapseListener != null) collapseListener.accept(collapsed);
            }
        });
        animator.start();
    }

    private void applyChromeForState() {
        toggleButton.setCollapsed(collapsed);
        brandLabel.setVisible(!collapsed);
        brandSubLabel.setVisible(!collapsed);
        for (SidebarSectionHeader sh : sectionHeaders) sh.setCollapsed(collapsed);
        for (JComponent c : expandedOnlyComponents) c.setVisible(!collapsed);
        for (SidebarNavItem item : navItems) item.setCollapsed(collapsed);
        favoritesCoordinator.setCollapsed(collapsed);
        userProfileCard.setCollapsed(collapsed);
        footerVersionLabel.setVisible(!collapsed);

        headerPanel.removeAll();
        footerPanel.removeAll();
        if (collapsed) {
            headerPanel.setLayout(new BoxLayout(headerPanel, BoxLayout.Y_AXIS));
            headerPanel.setBorder(new EmptyBorder(12, 6, 10, 6));
            logoBadge.setAlignmentX(Component.CENTER_ALIGNMENT);
            toggleButton.setAlignmentX(Component.CENTER_ALIGNMENT);
            headerPanel.add(logoBadge);
            headerPanel.add(Box.createRigidArea(new Dimension(0, 8)));
            headerPanel.add(toggleButton);

            footerPanel.setLayout(new BoxLayout(footerPanel, BoxLayout.Y_AXIS));
            footerPanel.setBorder(new EmptyBorder(8, 6, 12, 6));
            userProfileCard.setAlignmentX(Component.CENTER_ALIGNMENT);
            footerPanel.add(userProfileCard);
        } else {
            headerPanel.setLayout(new BorderLayout(8, 0));
            headerPanel.setBorder(new EmptyBorder(14, 12, 12, 12));
            headerPanel.add(logoBadge, BorderLayout.WEST);
            headerPanel.add(brandStack, BorderLayout.CENTER);
            headerPanel.add(toggleButton, BorderLayout.EAST);

            footerPanel.setLayout(new BoxLayout(footerPanel, BoxLayout.Y_AXIS));
            footerPanel.setBorder(new EmptyBorder(8, 12, 12, 12));
            userProfileCard.setAlignmentX(Component.LEFT_ALIGNMENT);
            footerVersionLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
            footerPanel.add(userProfileCard);
            footerPanel.add(Box.createRigidArea(new Dimension(0, 4)));
            footerPanel.add(footerVersionLabel);
        }
        headerPanel.revalidate(); headerPanel.repaint();
        footerPanel.revalidate(); footerPanel.repaint();
    }

    private JLabel createFooterVersionLabel() {
        JLabel lbl = new JLabel("MULTICORE v1.0.0");
        lbl.setFont(new Font(UIHelper.FONT, Font.PLAIN, 10));
        lbl.setForeground(subText());
        lbl.setBorder(new EmptyBorder(0, 4, 0, 0));
        lbl.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        lbl.setToolTipText("MULTICORE ERP • Clique para ver detalhes e licença do sistema");
        lbl.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override public void mouseClicked(java.awt.event.MouseEvent e) { AboutDialog.show(CollapsibleSidebar.this); }
        });
        return lbl;
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        try {
            g2.setColor(sidebarBg());
            g2.fillRect(0, 0, getWidth(), getHeight());
            g2.setColor(sidebarBorder());
            g2.fillRect(getWidth() - 1, 0, 1, getHeight());
        } finally {
            g2.dispose();
        }
    }

    private boolean matchesLabel(SidebarNavItem item, String activeLabel) {
        String tooltip = item.getToolTipText();
        return (tooltip != null && tooltip.equalsIgnoreCase(activeLabel))
                || (item.getLabel() != null && item.getLabel().equalsIgnoreCase(activeLabel));
    }
}
