package mz.multicore.erp.gui.components;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.Timer;
import javax.swing.border.EmptyBorder;
import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Barra lateral retrátil executiva (Collapsible Sidebar).
 * Organiza a navegação em categorias semânticas funcionais, suporta modo
 * expandido (240px) e modo recolhido/rail (64px), integra badges, mini-card
 * de perfil com avatar e indicador online, e adapta-se aos temas claro e escuro.
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
    private static Color toggleBg()      { return UIHelper.isLight() ? new Color(0, 0, 0, 8) : new Color(255, 255, 255, 12); }
    private static Color toggleBgHover() { return UIHelper.isLight() ? new Color(0, 0, 0, 22) : new Color(255, 255, 255, 28); }
    private static Color toggleGlyph()   { return UIHelper.isLight() ? new Color(71, 85, 105)   : new Color(226, 232, 240); }

    private final JPanel body;
    private final JPanel headerPanel;
    private final LogoBadge logoBadge;
    private final JPanel brandStack;
    private final JLabel brandLabel;
    private final JLabel brandSubLabel;
    private final ToggleButton toggleButton;
    private final JPanel footerPanel;
    private final UserProfileCard userProfileCard;
    private final JLabel footerVersionLabel;

    private final JPanel favoritesContainer;
    private final List<SidebarNavItem> favoriteNavItems = new ArrayList<>();
    private final List<SidebarNavItem> navItems = new ArrayList<>();
    private final List<JComponent> expandedOnlyComponents = new ArrayList<>();
    private final List<JLabel> sectionLabels = new ArrayList<>();
    private final List<SectionHeader> sectionHeaders = new ArrayList<>();

    private boolean collapsed = false;
    private int currentWidth = EXPANDED_WIDTH;
    private Timer animator;
    private Consumer<Boolean> collapseListener;

    public CollapsibleSidebar(String brand, String subBrand) {
        setLayout(new BorderLayout());
        setBackground(sidebarBg());
        setBorder(null);
        setPreferredSize(new Dimension(EXPANDED_WIDTH, 800));

        // ---- Header (Emblema 3D + Marca + Toggle)
        logoBadge = new LogoBadge(32);

        brandLabel = new JLabel(brand != null && !brand.isBlank() ? brand : "MULTICORE");
        brandLabel.setFont(new Font(UIHelper.FONT, Font.BOLD, 17));
        brandLabel.setForeground(headerText());

        brandSubLabel = new JLabel(subBrand != null && !subBrand.isBlank() ? subBrand : "ERP Profissional");
        brandSubLabel.setFont(new Font(UIHelper.FONT, Font.PLAIN, 11));
        brandSubLabel.setForeground(subText());
        brandSubLabel.setToolTipText("Empresa activa");

        brandStack = new JPanel();
        brandStack.setOpaque(false);
        brandStack.setLayout(new BoxLayout(brandStack, BoxLayout.Y_AXIS));
        brandLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        brandSubLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        brandStack.add(brandLabel);
        brandStack.add(Box.createRigidArea(new Dimension(0, 1)));
        brandStack.add(brandSubLabel);

        toggleButton = new ToggleButton();
        toggleButton.addActionListener(e -> toggle());

        headerPanel = new JPanel();
        headerPanel.setOpaque(false);
        add(headerPanel, BorderLayout.NORTH);

        // ---- Body (seções e itens de navegação num scroll vertical suave)
        body = new JPanel();
        body.setOpaque(false);
        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
        body.setBorder(new EmptyBorder(4, 6, 10, 6));

        favoritesContainer = new JPanel();
        favoritesContainer.setOpaque(false);
        favoritesContainer.setLayout(new BoxLayout(favoritesContainer, BoxLayout.Y_AXIS));
        favoritesContainer.setAlignmentX(Component.LEFT_ALIGNMENT);
        body.add(favoritesContainer);

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

        // ---- Footer (Card de Perfil de Utilizador e Versão)
        footerPanel = new JPanel();
        footerPanel.setOpaque(false);

        userProfileCard = new UserProfileCard();
        footerVersionLabel = new JLabel("MULTICORE v1.0.0");
        footerVersionLabel.setFont(new Font(UIHelper.FONT, Font.PLAIN, 10));
        footerVersionLabel.setForeground(subText());
        footerVersionLabel.setBorder(new EmptyBorder(0, 4, 0, 0));

        add(footerPanel, BorderLayout.SOUTH);

        // Configuração inicial de layout e componentes
        applyChromeForState();
    }

    public void setSubBrand(String text) {
        brandSubLabel.setText(text == null || text.isBlank() ? "ERP Profissional" : text);
    }

    public void setUserProfile(String displayName, String role) {
        userProfileCard.updateProfile(displayName, role);
    }

    /** Adiciona título de seção funcional (OPERAÇÕES, GESTÃO, etc.) com divisória elegante. */
    public void addSection(String title) {
        boolean showDivider = !sectionHeaders.isEmpty() || !navItems.isEmpty();
        SectionHeader section = new SectionHeader(title, showDivider);
        sectionHeaders.add(section);
        body.add(section);
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
        rebuildFavorites();
        return item;
    }

    public void rebuildFavorites() {
        favoritesContainer.removeAll();
        favoriteNavItems.clear();
        List<String> favs = SidebarFavoritesManager.getInstance().getFavorites();
        if (!favs.isEmpty()) {
            SectionHeader favHeader = new SectionHeader("FAVORITOS", false, UIHelper.icon("fas-star", 10, UIHelper.PENDING_YELLOW));
            favHeader.setCollapsed(collapsed);
            favoritesContainer.add(favHeader);

            for (String favLabel : favs) {
                SidebarNavItem match = navItems.stream()
                        .filter(item -> favLabel.equalsIgnoreCase(item.getLabel()))
                        .findFirst()
                        .orElse(null);
                if (match != null) {
                    SidebarNavItem favItem = (match.getIcon() != null)
                            ? new SidebarNavItem(match.getIcon(), match.getLabel(), match.getAccent(), match.getOnClick())
                            : new SidebarNavItem(match.getIconGlyph(), match.getLabel(), match.getAccent(), match.getOnClick());
                    favItem.setCollapsed(collapsed);
                    favItem.setAlignmentX(Component.LEFT_ALIGNMENT);
                    favoriteNavItems.add(favItem);
                    favoritesContainer.add(favItem);
                    favoritesContainer.add(Box.createRigidArea(new Dimension(0, 2)));
                }
            }
            favoritesContainer.add(Box.createRigidArea(new Dimension(0, 6)));
        }
        favoritesContainer.revalidate();
        favoritesContainer.repaint();
    }

    public void setBadge(String label, int count) {
        for (SidebarNavItem item : navItems) {
            if (matchesLabel(item, label)) {
                item.setBadgeCount(count);
                break;
            }
        }
        for (SidebarNavItem item : favoriteNavItems) {
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
        for (SidebarNavItem item : favoriteNavItems) {
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
        for (JLabel s : sectionLabels) s.setVisible(!collapsed);
        for (SectionHeader sh : sectionHeaders) sh.setCollapsed(collapsed);
        for (JComponent c : expandedOnlyComponents) c.setVisible(!collapsed);
        for (SidebarNavItem item : navItems) item.setCollapsed(collapsed);
        for (SidebarNavItem item : favoriteNavItems) item.setCollapsed(collapsed);
        userProfileCard.setCollapsed(collapsed);
        footerVersionLabel.setVisible(!collapsed);

        headerPanel.removeAll();
        if (collapsed) {
            headerPanel.setLayout(new BoxLayout(headerPanel, BoxLayout.Y_AXIS));
            headerPanel.setBorder(new EmptyBorder(12, 6, 10, 6));
            logoBadge.setAlignmentX(Component.CENTER_ALIGNMENT);
            toggleButton.setAlignmentX(Component.CENTER_ALIGNMENT);
            headerPanel.add(logoBadge);
            headerPanel.add(Box.createRigidArea(new Dimension(0, 8)));
            headerPanel.add(toggleButton);

            footerPanel.removeAll();
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

            footerPanel.removeAll();
            footerPanel.setLayout(new BoxLayout(footerPanel, BoxLayout.Y_AXIS));
            footerPanel.setBorder(new EmptyBorder(8, 12, 12, 12));
            userProfileCard.setAlignmentX(Component.LEFT_ALIGNMENT);
            footerVersionLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
            footerPanel.add(userProfileCard);
            footerPanel.add(Box.createRigidArea(new Dimension(0, 4)));
            footerPanel.add(footerVersionLabel);
        }
        headerPanel.revalidate();
        headerPanel.repaint();
        footerPanel.revalidate();
        footerPanel.repaint();
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

    /** Emblema 3D da aplicação no topo da Sidebar. */
    private static final class LogoBadge extends JComponent {
        private final Image iconImage;

        LogoBadge(int size) {
            setPreferredSize(new Dimension(size, size));
            setMaximumSize(new Dimension(size, size));
            setMinimumSize(new Dimension(size, size));
            this.iconImage = UIHelper.getAppIcon(size - 6);
            setOpaque(false);
            setToolTipText("Multicore ERP");
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            try {
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int w = getWidth();
                int h = getHeight();

                Color bgTop = UIHelper.isLight() ? new Color(241, 245, 249) : new Color(30, 41, 59);
                Color bgBot = UIHelper.isLight() ? new Color(226, 232, 240) : new Color(15, 23, 42);
                g2.setPaint(new GradientPaint(0, 0, bgTop, 0, h, bgBot));
                g2.fillRoundRect(0, 0, w, h, 8, 8);

                g2.setColor(UIHelper.isLight() ? new Color(203, 213, 225) : new Color(51, 65, 85));
                g2.drawRoundRect(0, 0, w - 1, h - 1, 8, 8);

                if (iconImage != null) {
                    int ix = (w - iconImage.getWidth(null)) / 2;
                    int iy = (h - iconImage.getHeight(null)) / 2;
                    g2.drawImage(iconImage, ix, iy, null);
                }
            } finally {
                g2.dispose();
            }
        }
    }

    /** Card de perfil do operador ativo com avatar e status online. */
    private static final class UserProfileCard extends JPanel {
        private String displayName = "";
        private String role = "";
        private boolean collapsed = false;
        private final JLabel nameLabel;
        private final JLabel roleLabel;
        private final AvatarWidget avatar;
        private final JPanel textStack;
        private final JButton logoutBtn;

        UserProfileCard() {
            setOpaque(false);
            setLayout(new BorderLayout(8, 0));

            avatar = new AvatarWidget(30);
            avatar.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            avatar.addMouseListener(new MouseAdapter() {
                @Override
                public void mousePressed(MouseEvent e) {
                    showUserMenu(avatar);
                }
            });

            nameLabel = new JLabel("Multicore");
            nameLabel.setFont(new Font(UIHelper.FONT, Font.BOLD, 12));
            nameLabel.setForeground(headerText());

            roleLabel = new JLabel("Sistema");
            roleLabel.setFont(new Font(UIHelper.FONT, Font.PLAIN, 10));
            roleLabel.setForeground(subText());

            textStack = new JPanel();
            textStack.setOpaque(false);
            textStack.setLayout(new BoxLayout(textStack, BoxLayout.Y_AXIS));
            textStack.add(nameLabel);
            textStack.add(Box.createRigidArea(new Dimension(0, 1)));
            textStack.add(roleLabel);

            logoutBtn = new JButton(UIHelper.icon("fas-sign-out-alt", 13, UIHelper.REJECTED_RED));
            logoutBtn.setToolTipText("Terminar Sessão (Logout)");
            logoutBtn.setFocusPainted(false);
            logoutBtn.setContentAreaFilled(false);
            logoutBtn.setBorder(new EmptyBorder(4, 4, 4, 4));
            logoutBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            logoutBtn.addActionListener(e -> UIHelper.requestLogout(UserProfileCard.this));

            add(avatar, BorderLayout.WEST);
            add(textStack, BorderLayout.CENTER);
            add(logoutBtn, BorderLayout.EAST);
        }

        private void showUserMenu(Component invoker) {
            JPopupMenu popup = new JPopupMenu();
            popup.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(UIHelper.BORDER),
                    new EmptyBorder(4, 6, 4, 6)));
            JMenuItem header = new JMenuItem(displayName + " (" + role + ")");
            header.setEnabled(false);
            popup.add(header);
            popup.addSeparator();
            JMenuItem logoutItem = new JMenuItem("Terminar Sessão", UIHelper.icon("fas-sign-out-alt", 12, UIHelper.REJECTED_RED));
            logoutItem.setForeground(UIHelper.REJECTED_RED);
            logoutItem.addActionListener(ev -> UIHelper.requestLogout(UserProfileCard.this));
            popup.add(logoutItem);
            popup.show(invoker, 0, invoker.getHeight());
        }

        void updateProfile(String name, String roleTitle) {
            this.displayName = (name != null && !name.isBlank()) ? name : "Utilizador";
            this.role = (roleTitle != null && !roleTitle.isBlank()) ? roleTitle : "Operador";
            nameLabel.setText(displayName);
            roleLabel.setText(role);
            avatar.setInitials(getInitials(displayName));
            String tip = displayName + " (" + role + ") — Clique para opções";
            setToolTipText(tip);
            avatar.setToolTipText(tip);
            repaint();
        }

        void setCollapsed(boolean collapsed) {
            this.collapsed = collapsed;
            textStack.setVisible(!collapsed);
            logoutBtn.setVisible(!collapsed);
            revalidate();
            repaint();
        }

        private static String getInitials(String name) {
            if (name == null || name.isBlank()) return "MC";
            String[] parts = name.trim().split("\\s+");
            if (parts.length == 1) {
                return parts[0].substring(0, Math.min(2, parts[0].length())).toUpperCase();
            }
            return ("" + parts[0].charAt(0) + parts[parts.length - 1].charAt(0)).toUpperCase();
        }

        private static final class AvatarWidget extends JComponent {
            private String initials = "MC";

            AvatarWidget(int size) {
                setPreferredSize(new Dimension(size, size));
                setMaximumSize(new Dimension(size, size));
                setMinimumSize(new Dimension(size, size));
                setOpaque(false);
                setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
            }

            void setInitials(String in) {
                this.initials = (in != null && !in.isBlank()) ? in : "MC";
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

                    // Círculo de avatar com gradiente azul executivo
                    Color c1 = UIHelper.ACCENT_BLUE;
                    Color c2 = new Color(29, 78, 216);
                    g2.setPaint(new GradientPaint(0, 0, c1, 0, h, c2));
                    g2.fillOval(0, 0, w, h);

                    // Borda sutil
                    g2.setColor(new Color(255, 255, 255, 60));
                    g2.drawOval(0, 0, w - 1, h - 1);

                    // Iniciais centralizadas
                    g2.setColor(Color.WHITE);
                    g2.setFont(new Font(UIHelper.FONT, Font.BOLD, 10));
                    FontMetrics fm = g2.getFontMetrics();
                    int tw = fm.stringWidth(initials);
                    int tx = (w - tw) / 2;
                    int ty = (h + fm.getAscent() - fm.getDescent()) / 2;
                    g2.drawString(initials, tx, ty);

                    // Indicador esmeralda de status online (#10B981)
                    int dotSize = 8;
                    int dotX = w - dotSize;
                    int dotY = h - dotSize;
                    g2.setColor(sidebarBg());
                    g2.fillOval(dotX - 1, dotY - 1, dotSize + 2, dotSize + 2);
                    g2.setColor(new Color(16, 185, 129));
                    g2.fillOval(dotX, dotY, dotSize, dotSize);
                } finally {
                    g2.dispose();
                }
            }
        }
    }

    /** Cabeçalho de seção semântica com divisória suave. */
    private final class SectionHeader extends JPanel {
        private final JLabel label;
        private final HairlineDivider divider;

        SectionHeader(String title, boolean showDivider) {
            this(title, showDivider, null);
        }

        SectionHeader(String title, boolean showDivider, javax.swing.Icon icon) {
            setOpaque(false);
            setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
            setAlignmentX(Component.LEFT_ALIGNMENT);
            setMaximumSize(new Dimension(Integer.MAX_VALUE, showDivider ? 32 : 18));

            if (showDivider) {
                add(Box.createRigidArea(new Dimension(0, 8)));
                divider = new HairlineDivider();
                add(divider);
                add(Box.createRigidArea(new Dimension(0, 8)));
            } else {
                divider = null;
                add(Box.createRigidArea(new Dimension(0, 4)));
            }

            label = new JLabel(title.toUpperCase());
            if (icon != null) {
                label.setIcon(icon);
                label.setIconTextGap(5);
            }
            label.setFont(new Font(UIHelper.FONT, Font.BOLD, 10));
            label.setForeground(sectionText());
            label.setAlignmentX(Component.LEFT_ALIGNMENT);
            label.setBorder(new EmptyBorder(0, 10, 3, 0));
            add(label);
            sectionLabels.add(label);
        }

        void setCollapsed(boolean collapsed) {
            label.setVisible(!collapsed);
            if (divider != null) {
                divider.repaint();
            }
            revalidate();
            repaint();
        }
    }

    /** Linha divisória fina que se adapta ao modo recolhido e expandido. */
    private final class HairlineDivider extends JComponent {
        HairlineDivider() {
            setPreferredSize(new Dimension(20, 1));
            setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));
            setMinimumSize(new Dimension(20, 1));
            setAlignmentX(Component.LEFT_ALIGNMENT);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            try {
                int w = getWidth();
                g2.setColor(UIHelper.isLight() ? new Color(226, 232, 240, 180) : new Color(51, 65, 85, 160));
                if (collapsed) {
                    int lineW = 24;
                    int lx = (w - lineW) / 2;
                    g2.fillRect(lx, 0, lineW, 1);
                } else {
                    g2.fillRect(8, 0, Math.max(0, w - 16), 1);
                }
            } finally {
                g2.dispose();
            }
        }
    }

    /** Botão de alternância estilizado com chevron vetorial e acessível. */
    private static final class ToggleButton extends JButton {
        private boolean collapsed = false;
        private boolean hover = false;

        ToggleButton() {
            setPreferredSize(new Dimension(30, 30));
            setMaximumSize(new Dimension(30, 30));
            setMinimumSize(new Dimension(30, 30));
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
                g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);

                int w = getWidth();
                int h = getHeight();
                g2.setColor(hover ? toggleBgHover() : toggleBg());
                g2.fillRoundRect(1, 1, w - 2, h - 2, 6, 6);
                g2.setColor(new Color(UIHelper.BORDER.getRed(), UIHelper.BORDER.getGreen(), UIHelper.BORDER.getBlue(), hover ? 140 : 60));
                g2.drawRoundRect(1, 1, w - 2, h - 2, 6, 6);

                // Chevron vetorial nítido anti-serrilhado
                g2.setColor(toggleGlyph());
                g2.setStroke(new BasicStroke(1.8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                int cx = w / 2;
                int cy = h / 2;
                if (collapsed) {
                    // Aponta para a direita: >
                    g2.drawLine(cx - 2, cy - 5, cx + 3, cy);
                    g2.drawLine(cx + 3, cy, cx - 2, cy + 5);
                } else {
                    // Aponta para a esquerda: <
                    g2.drawLine(cx + 2, cy - 5, cx - 3, cy);
                    g2.drawLine(cx - 3, cy, cx + 2, cy + 5);
                }
            } finally {
                g2.dispose();
            }
        }
    }
}
