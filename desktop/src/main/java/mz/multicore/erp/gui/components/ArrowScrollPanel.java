package mz.multicore.erp.gui.components;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

/**
 * Vertical scroll container for dashboards and long panels (SPEC-ASO-001).
 *
 * <p>The content tracks the viewport width, so there is never a horizontal overflow. Scrolling uses
 * the mouse wheel, Page Up / Page Down, Ctrl+Home / Ctrl+End and a slim scrollbar shown only when the
 * content is taller than the viewport. The former up/down arrow rail was removed in 2026-11: it took
 * space and looked foreign next to the rest of the design system.
 */
public class ArrowScrollPanel extends JPanel {

    private static final int DEFAULT_SCROLL_STEP = 260;
    private static final int SCROLL_UNIT_INCREMENT = 24;
    private static final int SCROLL_BLOCK_INCREMENT = 200;

    private final JScrollPane scrollPane;
    private final WidthTrackingContainer viewContainer;

    public ArrowScrollPanel(JComponent content) {
        super(new BorderLayout());
        setOpaque(false);

        this.viewContainer = new WidthTrackingContainer(content);

        this.scrollPane = new JScrollPane(viewContainer);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.setOpaque(false);
        scrollPane.getViewport().setOpaque(false);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        JScrollBar vertical = scrollPane.getVerticalScrollBar();
        vertical.setUI(new SlimScrollBarUI());
        vertical.setOpaque(false);
        vertical.setUnitIncrement(SCROLL_UNIT_INCREMENT);

        add(scrollPane, BorderLayout.CENTER);
        setupKeyboardShortcuts();
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
            } else {
                int nextY = Math.max(0, Math.min(p.y + step, getMaxScrollY()));
                vp.setViewPosition(new Point(p.x, nextY));
            }
        });
        timer.start();
    }

    public void scrollToTop() {
        scrollPane.getViewport().setViewPosition(new Point(0, 0));
    }

    public void scrollToBottom() {
        scrollPane.getViewport().setViewPosition(new Point(0, getMaxScrollY()));
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
}
