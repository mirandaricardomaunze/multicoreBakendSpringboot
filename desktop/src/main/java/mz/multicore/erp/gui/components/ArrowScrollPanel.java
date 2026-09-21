package mz.multicore.erp.gui.components;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * Painel de scroll moderno com navegação vertical por botões de seta (topo e fundo)
 * e eliminação integral de overflow horizontal através de tracking de viewport (SPEC-ASO-001).
 *
 * Substitui barras de rolagem nativas cinzentas e intrusivas por dois botões elegantes
 * de seta estilizados no padrão do Multicore ERP, preservando suporte natural ao rato
 * (mouse wheel) e atalhos de teclado (Page Up / Page Down).
 */
public class ArrowScrollPanel extends JPanel {

    private static final int DEFAULT_SCROLL_STEP = 260;
    private static final int SCROLL_UNIT_INCREMENT = 24;
    private static final int SCROLL_BLOCK_INCREMENT = 200;

    private final JScrollPane scrollPane;
    private final WidthTrackingContainer viewContainer;
    private final ModernButton btnUp;
    private final ModernButton btnDown;
    private final ScrollIndicatorTrack indicatorTrack;
    private final JPanel sideNav;

    public ArrowScrollPanel(JComponent content) {
        super(new BorderLayout(8, 0));
        setOpaque(false);

        // Contentor com tracking estrito de largura da viewport
        this.viewContainer = new WidthTrackingContainer(content);

        this.scrollPane = new JScrollPane(viewContainer);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.setOpaque(false);
        scrollPane.getViewport().setOpaque(false);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_NEVER);
        scrollPane.getVerticalScrollBar().setUnitIncrement(SCROLL_UNIT_INCREMENT);

        // Botões de navegação por seta (Topo e Fundo)
        this.btnUp = UIHelper.createSecondaryButton("");
        btnUp.setIcon(UIHelper.icon("fas-chevron-up", 12, Color.WHITE));
        btnUp.setToolTipText("Rolar para cima (Page Up / ↑)");
        btnUp.setPreferredSize(new Dimension(32, 32));
        btnUp.addActionListener(e -> scrollSmoothly(-DEFAULT_SCROLL_STEP));

        this.btnDown = UIHelper.createSecondaryButton("");
        btnDown.setIcon(UIHelper.icon("fas-chevron-down", 12, Color.WHITE));
        btnDown.setToolTipText("Rolar para baixo (Page Down / ↓)");
        btnDown.setPreferredSize(new Dimension(32, 32));
        btnDown.addActionListener(e -> scrollSmoothly(DEFAULT_SCROLL_STEP));

        // Indicador minimalista de posição e progresso no centro
        this.indicatorTrack = new ScrollIndicatorTrack();

        // Calha vertical de navegação
        this.sideNav = new JPanel(new BorderLayout(0, 6));
        sideNav.setOpaque(false);
        sideNav.setPreferredSize(new Dimension(34, 0));
        sideNav.setBorder(new EmptyBorder(4, 0, 4, 0));
        sideNav.add(btnUp, BorderLayout.NORTH);
        sideNav.add(indicatorTrack, BorderLayout.CENTER);
        sideNav.add(btnDown, BorderLayout.SOUTH);

        add(scrollPane, BorderLayout.CENTER);
        add(sideNav, BorderLayout.EAST);

        // Sincronização do estado e limites da viewport
        scrollPane.getViewport().addChangeListener(e -> updateNavigationState());
        setupKeyboardShortcuts();

        SwingUtilities.invokeLater(this::updateNavigationState);
    }

    public void scrollSmoothly(int delta) {
        JViewport vp = scrollPane.getViewport();
        Point current = vp.getViewPosition();
        int targetY = Math.max(0, Math.min(current.y + delta, getMaxScrollY()));
        if (targetY == current.y) return;

        // Animação rápida em 6 passos para resposta tátil e suave
        int frames = 6;
        int step = (targetY - current.y) / frames;
        Timer timer = new Timer(16, null);
        final int[] count = {0};
        timer.addActionListener(e -> {
            count[0]++;
            Point p = vp.getViewPosition();
            if (count[0] >= frames) {
                vp.setViewPosition(new Point(p.x, targetY));
                timer.stop();
                updateNavigationState();
            } else {
                int nextY = Math.max(0, Math.min(p.y + step, getMaxScrollY()));
                vp.setViewPosition(new Point(p.x, nextY));
                updateNavigationState();
            }
        });
        timer.start();
    }

    public void scrollToTop() {
        scrollPane.getViewport().setViewPosition(new Point(0, 0));
        updateNavigationState();
    }

    public void scrollToBottom() {
        scrollPane.getViewport().setViewPosition(new Point(0, getMaxScrollY()));
        updateNavigationState();
    }

    public int getMaxScrollY() {
        Component view = scrollPane.getViewport().getView();
        if (view == null) return 0;
        int viewHeight = Math.max(view.getHeight(), view.getPreferredSize() != null ? view.getPreferredSize().height : 0);
        int vpHeight = scrollPane.getViewport().getHeight();
        if (vpHeight <= 0) {
            vpHeight = Math.max(scrollPane.getHeight(), getHeight());
        }
        return Math.max(0, viewHeight - vpHeight);
    }

    public void updateNavigationState() {
        JViewport vp = scrollPane.getViewport();
        int y = vp.getViewPosition().y;
        int max = getMaxScrollY();

        boolean canScroll = max > 0;
        btnUp.setEnabled(canScroll && y > 0);
        btnDown.setEnabled(canScroll && y < max);
        indicatorTrack.repaint();
    }

    private void setupKeyboardShortcuts() {
        InputMap im = getInputMap(WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
        ActionMap am = getActionMap();

        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_PAGE_UP, 0), "pageUp");
        am.put("pageUp", new AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) {
                scrollSmoothly(-DEFAULT_SCROLL_STEP * 2);
            }
        });

        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_PAGE_DOWN, 0), "pageDown");
        am.put("pageDown", new AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) {
                scrollSmoothly(DEFAULT_SCROLL_STEP * 2);
            }
        });

        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_HOME, KeyEvent.CTRL_DOWN_MASK), "home");
        am.put("home", new AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) {
                scrollToTop();
            }
        });

        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_END, KeyEvent.CTRL_DOWN_MASK), "end");
        am.put("end", new AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) {
                scrollToBottom();
            }
        });
    }

    public JScrollPane getScrollPane() {
        return scrollPane;
    }

    public ModernButton getBtnUp() {
        return btnUp;
    }

    public ModernButton getBtnDown() {
        return btnDown;
    }

    /**
     * Contentor que implementa {@link Scrollable} e força a largura exacta da viewport,
     * impedindo qualquer overflow horizontal.
     */
    public static class WidthTrackingContainer extends JPanel implements Scrollable {

        public WidthTrackingContainer(JComponent child) {
            super(new BorderLayout());
            setOpaque(false);
            add(child, BorderLayout.CENTER);
        }

        @Override
        public Dimension getPreferredScrollableViewportSize() {
            return getPreferredSize();
        }

        @Override
        public int getScrollableUnitIncrement(Rectangle visibleRect, int orientation, int direction) {
            return SCROLL_UNIT_INCREMENT;
        }

        @Override
        public int getScrollableBlockIncrement(Rectangle visibleRect, int orientation, int direction) {
            return SCROLL_BLOCK_INCREMENT;
        }

        @Override
        public boolean getScrollableTracksViewportWidth() {
            return true;
        }

        @Override
        public boolean getScrollableTracksViewportHeight() {
            return false;
        }
    }

    /**
     * Barra fina indicadora de progresso do scroll com clique rápido para posicionamento.
     */
    private class ScrollIndicatorTrack extends JComponent {

        public ScrollIndicatorTrack() {
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            addMouseListener(new MouseAdapter() {
                @Override
                public void mousePressed(MouseEvent e) {
                    jumpToMouse(e.getY());
                }
            });
        }

        private void jumpToMouse(int mouseY) {
            int trackH = getHeight();
            if (trackH <= 0) return;
            float ratio = (float) mouseY / (float) trackH;
            int max = getMaxScrollY();
            int targetY = Math.round(ratio * max);
            scrollPane.getViewport().setViewPosition(new Point(0, Math.max(0, Math.min(targetY, max))));
            updateNavigationState();
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            int w = getWidth();
            int h = getHeight();
            if (h <= 0 || w <= 0) return;

            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int trackW = 4;
            int trackX = (w - trackW) / 2;

            // Linha vertical de fundo
            g2.setColor(UIHelper.GRID);
            g2.fillRoundRect(trackX, 4, trackW, Math.max(0, h - 8), trackW, trackW);

            // Cápsula indicadora proporcional
            int max = getMaxScrollY();
            int currentY = scrollPane.getViewport().getViewPosition().y;
            int vpHeight = scrollPane.getViewport().getHeight();
            Component view = scrollPane.getViewport().getView();
            int totalHeight = view != null ? view.getHeight() : vpHeight;

            if (totalHeight > 0 && max > 0) {
                float visibleRatio = Math.min(1.0f, (float) vpHeight / (float) totalHeight);
                int thumbH = Math.max(20, Math.round(visibleRatio * (h - 8)));
                float scrollRatio = (float) currentY / (float) max;
                int thumbY = 4 + Math.round(scrollRatio * (h - 8 - thumbH));

                g2.setColor(UIHelper.ACCENT_BLUE);
                g2.fillRoundRect(trackX - 1, thumbY, trackW + 2, thumbH, trackW + 2, trackW + 2);
            }

            g2.dispose();
        }
    }
}
