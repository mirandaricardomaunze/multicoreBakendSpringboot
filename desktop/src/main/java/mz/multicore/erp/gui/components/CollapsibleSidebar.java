package mz.multicore.erp.gui.components;

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
 * Barra lateral retrátil (Collapsible Sidebar).
 * Organiza a navegação em categorias semânticas funcionais, suporta modo
 * expandido (240px) e modo recolhido/rail (64px), integra badges e adapta-se
 * fluidamente aos temas claro e escuro.
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
    private static Color sectionText()   { return UIHelper.isLight() ? new Color(148, 163, 184) : new Color(156, 163, 175); }
    private static Color toggleBg()      { return UIHelper.isLight() ? new Color(241, 245, 249) : new Color(55, 65, 81); }
    private static Color toggleBgHover() { return UIHelper.isLight() ? new Color(226, 232, 240) : new Color(75, 85, 99); }
    private static Color toggleGlyph()   { return UIHelper.isLight() ? new Color(71, 85, 105)   : Color.WHITE; }

    private final JPanel body;
    private final JPanel headerPanel;
    private final JLabel brandLabel;
    private final JLabel brandSubLabel;
    private final ToggleButton toggleButton;
    private final JPanel footerPanel;
    private final JLabel footerVersionLabel;
    private final JLabel footerUserLabel;

    private final List<SidebarNavItem> navItems = new ArrayList<>();
    private final List<JComponent> expandedOnlyComponents = new ArrayList<>();
    private final List<JLabel> sectionLabels = new ArrayList<>();

    private boolean collapsed = false;
    private int currentWidth = EXPANDED_WIDTH;
    private Timer animator;
    private Consumer<Boolean> collapseListener;

    public CollapsibleSidebar(String brand, String subBrand) {
        setLayout(new BorderLayout());
        setBackground(sidebarBg());
        setBorder(null);
        setPreferredSize(new Dimension(EXPANDED_WIDTH, 800));

        // ---- Header (Marca + Toggle)
        brandLabel = new JLabel(brand);
        brandLabel.setFont(new Font(UIHelper.FONT, Font.BOLD, 18));
        brandLabel.setForeground(headerText());

        brandSubLabel = new JLabel(subBrand != null && !subBrand.isBlank() ? subBrand : "ERP Profissional");
        brandSubLabel.setFont(new Font(UIHelper.FONT, Font.PLAIN, 11));
        brandSubLabel.setForeground(subText());
        brandSubLabel.setToolTipText("Empresa ativa");

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

        headerPanel = new JPanel(new BorderLayout(8, 0));
        headerPanel.setOpaque(false);
        headerPanel.setBorder(new EmptyBorder(16, 14, 12, 12));
        headerPanel.add(brandStack, BorderLayout.CENTER);
        headerPanel.add(toggleButton, BorderLayout.EAST);
        add(headerPanel, BorderLayout.NORTH);

        // ---- Body (seções e itens de navegação num scroll vertical suave)
        body = new JPanel();
        body.setOpaque(false);
        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
        body.setBorder(new EmptyBorder(4, 6, 10, 6));

        JScrollPane bodyScroll = new JScrollPane(body);
        bodyScroll.setBorder(BorderFactory.createEmptyBorder());
        bodyScroll.setOpaque(false);
        bodyScroll.getViewport().setOpaque(false);
        bodyScroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        bodyScroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        bodyScroll.getVerticalScrollBar().setUnitIncrement(16);
        bodyScroll.getVerticalScrollBar().setPreferredSize(new Dimension(6, 0));
        add(bodyScroll, BorderLayout.CENTER);

        // ---- Footer (versão e utilizador ativo)
        footerPanel = new JPanel();
        footerPanel.setOpaque(false);
        footerPanel.setLayout(new BoxLayout(footerPanel, BoxLayout.Y_AXIS));
        footerPanel.setBorder(new EmptyBorder(8, 14, 12, 14));

        footerUserLabel = new JLabel("");
        footerUserLabel.setFont(new Font(UIHelper.FONT, Font.BOLD, 11));
        footerUserLabel.setForeground(headerText());
        footerUserLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        footerVersionLabel = new JLabel("MULTICORE v1.0.0");
        footerVersionLabel.setFont(new Font(UIHelper.FONT, Font.PLAIN, 10));
        footerVersionLabel.setForeground(subText());
        footerVersionLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        footerPanel.add(footerUserLabel);
        footerPanel.add(Box.createRigidArea(new Dimension(0, 2)));
        footerPanel.add(footerVersionLabel);
        add(footerPanel, BorderLayout.SOUTH);
    }

    public void setSubBrand(String text) {
        brandSubLabel.setText(text == null || text.isBlank() ? "ERP Profissional" : text);
    }

    public void setUserProfile(String displayName, String role) {
        if (displayName != null && !displayName.isBlank()) {
            footerUserLabel.setText(displayName + (role != null && !role.isBlank() ? " (" + role + ")" : ""));
            footerUserLabel.setToolTipText(displayName + " — " + role);
        } else {
            footerUserLabel.setText("");
        }
    }

    /** Adiciona título de seção funcional (OPERAÇÕES, GESTÃO, etc.). */
    public void addSection(String title) {
        if (!sectionLabels.isEmpty() || !navItems.isEmpty()) {
            body.add(Box.createRigidArea(new Dimension(0, 10)));
        }
        JLabel section = new JLabel(title.toUpperCase());
        section.setFont(new Font(UIHelper.FONT, Font.BOLD, 10));
        section.setForeground(sectionText());
        section.setAlignmentX(Component.LEFT_ALIGNMENT);
        section.setBorder(new EmptyBorder(4, 12, 4, 0));
        body.add(section);
        sectionLabels.add(section);
    }

    public SidebarNavItem addItem(String iconGlyph, String label, Color accent, Runnable onClick) {
        SidebarNavItem item = new SidebarNavItem(iconGlyph, label, accent, () -> {
            setActive(label);
            if (onClick != null) onClick.run();
        });
        return registerItem(item);
    }

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

    public void setBadge(String label, int count) {
        for (SidebarNavItem item : navItems) {
            if (matchesLabel(item, label)) {
                item.setBadgeCount(count);
                break;
            }
        }
    }

    public void addExpandedOnly(JComponent component) {
        component.setAlignmentX(Component.LEFT_ALIGNMENT);
        expandedOnlyComponents.add(component);
        body.add(component);
        body.add(Box.createRigidArea(new Dimension(0, 6)));
    }

    public void setActive(String activeLabel) {
        for (SidebarNavItem item : navItems) {
            item.setActive(matchesLabel(item, activeLabel));
        }
    }

    public List<SidebarNavItem> getNavItems() {
        return List.copyOf(navItems);
    }

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

        applyChromeForState();

        animator = new Timer(ANIMATION_TICK_MS, e -> {
            long elapsed = System.currentTimeMillis() - startTime;
            float t = Math.min(1f, elapsed / (float) ANIMATION_DURATION_MS);
            float eased = easeInOut(t);
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
        footerPanel.setVisible(!collapsed);
        for (JLabel s : sectionLabels) s.setVisible(!collapsed);
        for (JComponent c : expandedOnlyComponents) c.setVisible(!collapsed);
        for (SidebarNavItem item : navItems) item.setCollapsed(collapsed);
        headerPanel.setBorder(collapsed
                ? new EmptyBorder(14, 6, 12, 6)
                : new EmptyBorder(16, 14, 12, 12));
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

    private float easeInOut(float t) {
        return t < 0.5f ? 2 * t * t : -1 + (4 - 2 * t) * t;
    }

    /** Botão de alternância estilizado e acessível. */
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
            setToolTipText("Recolher menu lateral (Ctrl+B)");
            getAccessibleContext().setAccessibleName("Recolher menu lateral");

            addMouseListener(new MouseAdapter() {
                @Override public void mouseEntered(MouseEvent e) { hover = true; repaint(); }
                @Override public void mouseExited(MouseEvent e)  { hover = false; repaint(); }
            });
        }

        void setCollapsed(boolean collapsed) {
            this.collapsed = collapsed;
            setToolTipText(collapsed ? "Expandir menu lateral (Ctrl+B)" : "Recolher menu lateral (Ctrl+B)");
            getAccessibleContext().setAccessibleName(collapsed ? "Expandir menu lateral" : "Recolher menu lateral");
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
                g2.setColor(hover ? toggleBgHover() : toggleBg());
                g2.fillRoundRect(0, 0, w, h, 8, 8);

                String glyph = collapsed ? "›" : "‹";
                g2.setFont(new Font(UIHelper.FONT, Font.BOLD, 17));
                int tw = g2.getFontMetrics().stringWidth(glyph);
                int tx = (w - tw) / 2;
                int ty = (h + g2.getFontMetrics().getAscent() - g2.getFontMetrics().getDescent()) / 2 - 1;
                g2.setColor(toggleGlyph());
                g2.drawString(glyph, tx, ty);
            } finally {
                g2.dispose();
            }
        }
    }
}
