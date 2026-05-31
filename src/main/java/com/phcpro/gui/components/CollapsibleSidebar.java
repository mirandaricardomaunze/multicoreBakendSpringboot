package com.phcpro.gui.components;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.Timer;
import javax.swing.border.EmptyBorder;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Collapsible left-rail sidebar. Holds a brand header, section dividers,
 * navigation items, and a footer. Responsibility: layout + collapse animation
 * + active-item tracking. Does not own any business logic.
 */
public class CollapsibleSidebar extends JPanel {

    public static final int EXPANDED_WIDTH = 240;
    public static final int COLLAPSED_WIDTH = 64;

    private static final int ANIMATION_DURATION_MS = 160;
    private static final int ANIMATION_TICK_MS = 14;
    private static final Color HEADER_TEXT = new Color(243, 244, 246);
    private static final Color SECTION_LABEL = new Color(107, 114, 128);
    private static final Color TOGGLE_BG = new Color(55, 65, 81);
    private static final Color TOGGLE_BG_HOVER = new Color(75, 85, 99);
    private static final Color BG = new Color(24, 32, 47);
    private static final Color BORDER_RIGHT = new Color(45, 55, 72);

    private final JPanel body;
    private final JPanel headerPanel;
    private final JLabel brandLabel;
    private final JLabel brandSubLabel;
    private final ToggleButton toggleButton;
    private final JLabel footerLabel;

    private final List<SidebarNavItem> navItems = new ArrayList<>();
    private final List<JComponent> expandedOnlyComponents = new ArrayList<>();
    private final List<JLabel> sectionLabels = new ArrayList<>();

    private boolean collapsed = false;
    private int currentWidth = EXPANDED_WIDTH;
    private Timer animator;

    private Consumer<Boolean> collapseListener;

    public CollapsibleSidebar(String brand, String subBrand) {
        setLayout(new BorderLayout());
        setBackground(BG);
        setBorder(null);
        setPreferredSize(new Dimension(EXPANDED_WIDTH, 800));

        // ---- Header (brand + toggle)
        brandLabel = new JLabel(brand);
        brandLabel.setFont(new Font("Segoe UI", Font.BOLD, 20));
        brandLabel.setForeground(HEADER_TEXT);

        brandSubLabel = new JLabel(subBrand);
        brandSubLabel.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        brandSubLabel.setForeground(SECTION_LABEL);

        JPanel brandStack = new JPanel();
        brandStack.setOpaque(false);
        brandStack.setLayout(new BoxLayout(brandStack, BoxLayout.Y_AXIS));
        brandLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        brandSubLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        brandStack.add(brandLabel);
        brandStack.add(Box.createRigidArea(new Dimension(0, 2)));
        brandStack.add(brandSubLabel);

        toggleButton = new ToggleButton();
        toggleButton.addActionListener(e -> toggle());

        headerPanel = new JPanel(new BorderLayout());
        headerPanel.setOpaque(false);
        headerPanel.setBorder(new EmptyBorder(20, 18, 14, 14));
        headerPanel.add(brandStack, BorderLayout.CENTER);
        headerPanel.add(toggleButton, BorderLayout.EAST);

        add(headerPanel, BorderLayout.NORTH);

        // ---- Body (sections + nav items, vertically stacked)
        body = new JPanel();
        body.setOpaque(false);
        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
        body.setBorder(new EmptyBorder(6, 10, 10, 10));

        // Wrap in a scroll pane so the bottom items remain reachable on small screens.
        JScrollPane bodyScroll = new JScrollPane(body);
        bodyScroll.setBorder(BorderFactory.createEmptyBorder());
        bodyScroll.setOpaque(false);
        bodyScroll.getViewport().setOpaque(false);
        bodyScroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        bodyScroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        bodyScroll.getVerticalScrollBar().setUnitIncrement(16);
        bodyScroll.getVerticalScrollBar().setPreferredSize(new Dimension(8, 0));
        add(bodyScroll, BorderLayout.CENTER);

        // ---- Footer
        footerLabel = new JLabel("v1.0.0");
        footerLabel.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        footerLabel.setForeground(SECTION_LABEL);
        footerLabel.setBorder(new EmptyBorder(10, 18, 16, 14));
        add(footerLabel, BorderLayout.SOUTH);
    }

    /** Add a section header label. Hidden in collapsed mode. */
    public void addSection(String title) {
        if (!sectionLabels.isEmpty() || !navItems.isEmpty()) {
            body.add(Box.createRigidArea(new Dimension(0, 14)));
        }
        JLabel section = new JLabel(title.toUpperCase());
        section.setFont(new Font("Segoe UI", Font.BOLD, 10));
        section.setForeground(SECTION_LABEL);
        section.setAlignmentX(Component.LEFT_ALIGNMENT);
        section.setBorder(new EmptyBorder(0, 14, 6, 0));
        body.add(section);
        sectionLabels.add(section);
    }

    /** Add a navigation item using a text glyph. Returns it so the caller can wire active-state. */
    public SidebarNavItem addItem(String iconGlyph, String label, Color accent, Runnable onClick) {
        SidebarNavItem item = new SidebarNavItem(iconGlyph, label, accent, () -> {
            setActive(label);
            if (onClick != null) onClick.run();
        });
        return registerItem(item);
    }

    /** Add a navigation item using a Swing Icon (preferred — Ikonli / FontAwesome). */
    public SidebarNavItem addItem(javax.swing.Icon icon, String label, Color accent, Runnable onClick) {
        SidebarNavItem item = new SidebarNavItem(icon, label, accent, () -> {
            setActive(label);
            if (onClick != null) onClick.run();
        });
        return registerItem(item);
    }

    private SidebarNavItem registerItem(SidebarNavItem item) {
        item.setAlignmentX(Component.LEFT_ALIGNMENT);
        navItems.add(item);
        body.add(item);
        body.add(Box.createRigidArea(new Dimension(0, 2)));
        return item;
    }

    /** Add an expanded-only component (e.g. company/user combos). */
    public void addExpandedOnly(JComponent component) {
        component.setAlignmentX(Component.LEFT_ALIGNMENT);
        expandedOnlyComponents.add(component);
        body.add(component);
        body.add(Box.createRigidArea(new Dimension(0, 8)));
    }

    /** Highlight the nav item whose label matches the given key. */
    public void setActive(String activeLabel) {
        for (SidebarNavItem item : navItems) {
            item.setActive(matchesLabel(item, activeLabel));
        }
    }

    /** Listener invoked after the sidebar finishes a collapse/expand transition. */
    public void onCollapsedChanged(Consumer<Boolean> listener) {
        this.collapseListener = listener;
    }

    public boolean isCollapsedState() { return collapsed; }

    public void toggle() {
        setCollapsedState(!collapsed);
    }

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

        // Update fixed-state widgets immediately for crisp visual feedback.
        applyChromeForState();

        animator = new Timer(ANIMATION_TICK_MS, e -> {
            long elapsed = System.currentTimeMillis() - startTime;
            float t = Math.min(1f, elapsed / (float) ANIMATION_DURATION_MS);
            float eased = easeInOut(t);
            currentWidth = Math.round(startW + (endW - startW) * eased);
            setPreferredSize(new Dimension(currentWidth, getHeight()));
            revalidate();
            getParent().repaint();
            if (t >= 1f) {
                ((Timer) e.getSource()).stop();
                currentWidth = endW;
                if (collapseListener != null) collapseListener.accept(collapsed);
            }
        });
        animator.start();
    }

    private void applyChromeForState() {
        toggleButton.setCollapsed(collapsed);
        brandLabel.setVisible(!collapsed);
        brandSubLabel.setVisible(!collapsed);
        footerLabel.setVisible(!collapsed);
        for (JLabel s : sectionLabels) s.setVisible(!collapsed);
        for (JComponent c : expandedOnlyComponents) c.setVisible(!collapsed);
        for (SidebarNavItem item : navItems) item.setCollapsed(collapsed);
        headerPanel.setBorder(collapsed
                ? new EmptyBorder(18, 8, 14, 8)
                : new EmptyBorder(20, 18, 14, 14));
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        try {
            g2.setColor(BG);
            g2.fillRect(0, 0, getWidth(), getHeight());
            g2.setColor(BORDER_RIGHT);
            g2.fillRect(getWidth() - 1, 0, 1, getHeight());
        } finally {
            g2.dispose();
        }
    }

    private boolean matchesLabel(SidebarNavItem item, String activeLabel) {
        String tooltip = item.getToolTipText();
        return tooltip != null && tooltip.equals(activeLabel);
    }

    private float easeInOut(float t) {
        return t < 0.5f ? 2 * t * t : -1 + (4 - 2 * t) * t;
    }

    /** Minimal flat toggle button. Shows « when expanded, » when collapsed. */
    private static final class ToggleButton extends JButton {
        private boolean collapsed = false;
        private boolean hover = false;

        ToggleButton() {
            setPreferredSize(new Dimension(32, 32));
            setFocusPainted(false);
            setBorderPainted(false);
            setContentAreaFilled(false);
            setOpaque(false);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setToolTipText("Recolher / Expandir menu");
            addMouseListener(new MouseAdapter() {
                @Override public void mouseEntered(MouseEvent e) { hover = true; repaint(); }
                @Override public void mouseExited(MouseEvent e)  { hover = false; repaint(); }
            });
        }

        void setCollapsed(boolean collapsed) {
            this.collapsed = collapsed;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            try {
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

                int w = getWidth();
                int h = getHeight();
                g2.setColor(hover ? TOGGLE_BG_HOVER : TOGGLE_BG);
                g2.fillRoundRect(0, 0, w, h, 8, 8);

                String glyph = collapsed ? "›" : "‹";
                g2.setFont(new Font("Segoe UI", Font.BOLD, 18));
                int tw = g2.getFontMetrics().stringWidth(glyph);
                int tx = (w - tw) / 2;
                int ty = (h + g2.getFontMetrics().getAscent() - g2.getFontMetrics().getDescent()) / 2 - 1;
                g2.setColor(Color.WHITE);
                g2.drawString(glyph, tx, ty);
            } finally {
                g2.dispose();
            }
        }
    }
}
