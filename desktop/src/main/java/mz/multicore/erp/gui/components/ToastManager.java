package mz.multicore.erp.gui.components;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JWindow;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.awt.Point;
import java.awt.Window;
import java.util.ArrayDeque;
import java.util.Deque;

/** Fila de notificacoes breves e nao bloqueantes, ancorada a janela activa. */
public final class ToastManager {

    static final int DEFAULT_DURATION_MS = 3600;
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
        JPanel panel = new JPanel(new BorderLayout(10, 0));
        panel.setBackground(UIHelper.BG_CARD);
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(type.color(), 1),
                BorderFactory.createEmptyBorder(11, 13, 11, 15)));
        JLabel icon = new JLabel(UIHelper.icon(type.iconCode(), 18, type.color()));
        JLabel text = new JLabel("<html><body style='width:300px'>" + escape(message) + "</body></html>");
        text.setForeground(UIHelper.TEXT_LIGHT);
        text.setFont(new Font(UIHelper.FONT, Font.PLAIN, 12));
        panel.add(icon, BorderLayout.WEST);
        panel.add(text, BorderLayout.CENTER);
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
        window.add(buildContent(toast.type(), toast.message()));
        window.pack();
        Point p = owner.getLocationOnScreen();
        int x = p.x + owner.getWidth() - window.getWidth() - 20;
        int y = p.y + owner.getHeight() - window.getHeight() - 42;
        window.setLocation(Math.max(p.x + 10, x), Math.max(p.y + 10, y));
        window.setVisible(true);
        Timer timer = new Timer(DEFAULT_DURATION_MS, e -> {
            window.dispose();
            showing = false;
            showNext();
        });
        timer.setRepeats(false);
        timer.start();
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
