package mz.multicore.erp.gui.components;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JWindow;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Font;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Point;
import java.awt.Window;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayDeque;
import java.util.Deque;

/** Fila de notificações breves, não bloqueantes e animadas, ancorada à janela activa. */
public final class ToastManager {

    static final int DEFAULT_DURATION_MS = 3600;
    private static final int ANIMATION_STEPS = 6;
    private static final int ANIMATION_STEP_MS = 20; // 120ms total duration

    private static final Deque<Toast> QUEUE = new ArrayDeque<>();
    private static boolean showing;

    private ToastManager() {}

    public static void show(Component owner, FeedbackType type, String message) {
        runOnEdt(() -> {
            QUEUE.addLast(new Toast(owner, type == null ? FeedbackType.INFO : type, message));
            showNext();
        });
    }

    public static void success(Component owner, String message) {
        show(owner, FeedbackType.SUCCESS, message);
    }

    static JPanel buildContent(FeedbackType type, String message) {
        return buildContent(type, message, null);
    }

    static JPanel buildContent(FeedbackType type, String message, Runnable onDismiss) {
        ModernPanel panel = new ModernPanel(12);
        panel.setLayout(new BorderLayout(10, 0));
        panel.setBackground(UIHelper.BG_CARD);
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(type.color(), 1),
                BorderFactory.createEmptyBorder(10, 14, 10, 12)));

        JLabel icon = new JLabel(UIHelper.icon(type.iconCode(), 18, type.color()));
        JLabel text = new JLabel("<html><body style='width:300px'>" + escape(message) + "</body></html>");
        text.setForeground(UIHelper.TEXT_LIGHT);
        text.setFont(new Font(UIHelper.FONT, Font.PLAIN, 12));

        JLabel closeBtn = new JLabel(UIHelper.icon("fas-times", 12, UIHelper.TEXT_MUTED));
        closeBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        closeBtn.setToolTipText("Fechar notificação");

        panel.add(icon, BorderLayout.WEST);
        panel.add(text, BorderLayout.CENTER);
        panel.add(closeBtn, BorderLayout.EAST);
        panel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        MouseAdapter dismissAdapter = new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (onDismiss != null) onDismiss.run();
            }
        };
        panel.addMouseListener(dismissAdapter);
        closeBtn.addMouseListener(dismissAdapter);

        panel.getAccessibleContext().setAccessibleName(type.title() + ": " + message);
        return panel;
    }

    private static void showNext() {
        if (showing || QUEUE.isEmpty() || GraphicsEnvironment.isHeadless()) return;
        Toast toast = QUEUE.removeFirst();
        Window owner = toast.owner() == null ? UIHelper.mainWindow : SwingUtilities.getWindowAncestor(toast.owner());
        if (owner == null || !owner.isShowing()) {
            showNext();
            return;
        }
        showing = true;
        JWindow window = new JWindow(owner);
        window.setFocusableWindowState(false);
        window.setAlwaysOnTop(true);

        boolean[] closing = {false};
        Runnable dismissAction = () -> animateExit(window, closing);

        window.add(buildContent(toast.type(), toast.message(), dismissAction));
        window.pack();

        Point p = owner.getLocationOnScreen();
        int targetX = Math.max(p.x + 10, p.x + owner.getWidth() - window.getWidth() - 20);
        int targetY = Math.max(p.y + 10, p.y + owner.getHeight() - window.getHeight() - 42);

        boolean supportsTranslucency = false;
        try {
            GraphicsDevice gd = owner.getGraphicsConfiguration().getDevice();
            supportsTranslucency = gd.isWindowTranslucencySupported(GraphicsDevice.WindowTranslucency.TRANSLUCENT);
        } catch (Exception ignored) {}

        if (supportsTranslucency) {
            try {
                window.setOpacity(0.0f);
            } catch (Exception ignored) {
                supportsTranslucency = false;
            }
        }

        int startY = targetY + 8;
        window.setLocation(targetX, startY);
        window.setVisible(true);

        // Slide-up e Fade-in
        final boolean translucencyActive = supportsTranslucency;
        Timer enterTimer = new Timer(ANIMATION_STEP_MS, null);
        int[] step = {0};
        enterTimer.addActionListener(e -> {
            step[0]++;
            float ratio = (float) step[0] / ANIMATION_STEPS;
            int currentY = startY - Math.round(8 * ratio);
            window.setLocation(targetX, currentY);
            if (translucencyActive) {
                try {
                    window.setOpacity(Math.min(1.0f, ratio));
                } catch (Exception ignored) {}
            }
            if (step[0] >= ANIMATION_STEPS) {
                window.setLocation(targetX, targetY);
                if (translucencyActive) {
                    try { window.setOpacity(1.0f); } catch (Exception ignored) {}
                }
                enterTimer.stop();
            }
        });
        enterTimer.start();

        // Temporizador de exibição padrão
        Timer durationTimer = new Timer(DEFAULT_DURATION_MS, e -> animateExit(window, closing));
        durationTimer.setRepeats(false);
        durationTimer.start();

        MouseAdapter hoverAdapter = new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                if (durationTimer.isRunning()) {
                    durationTimer.stop();
                }
            }

            @Override
            public void mouseExited(MouseEvent e) {
                if (!closing[0]) {
                    durationTimer.restart();
                }
            }
        };
        attachHoverRecursive(window, hoverAdapter);
    }

    private static void attachHoverRecursive(Component c, MouseAdapter adapter) {
        c.addMouseListener(adapter);
        if (c instanceof java.awt.Container container) {
            for (Component child : container.getComponents()) {
                attachHoverRecursive(child, adapter);
            }
        }
    }

    private static void animateExit(JWindow window, boolean[] closingFlag) {
        if (closingFlag[0]) return;
        closingFlag[0] = true;

        if (GraphicsEnvironment.isHeadless()) {
            window.dispose();
            showing = false;
            showNext();
            return;
        }

        Point currentLoc = window.getLocation();
        int initialY = currentLoc.y;
        Timer exitTimer = new Timer(ANIMATION_STEP_MS, null);
        int[] step = {0};
        exitTimer.addActionListener(e -> {
            step[0]++;
            float ratio = (float) step[0] / ANIMATION_STEPS;
            window.setLocation(currentLoc.x, initialY + Math.round(6 * ratio));
            try {
                window.setOpacity(Math.max(0.0f, 1.0f - ratio));
            } catch (Exception ignored) {}

            if (step[0] >= ANIMATION_STEPS) {
                exitTimer.stop();
                window.dispose();
                showing = false;
                showNext();
            }
        });
        exitTimer.start();
    }

    private static String escape(String value) {
        if (value == null || value.isBlank()) return "Operação concluída.";
        return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    private static void runOnEdt(Runnable task) {
        if (SwingUtilities.isEventDispatchThread()) task.run(); else SwingUtilities.invokeLater(task);
    }

    private record Toast(Component owner, FeedbackType type, String message) {}
}
