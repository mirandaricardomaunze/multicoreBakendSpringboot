package mz.multicore.erp.gui.components;

import mz.multicore.erp.architecture.security.CurrentUserContext;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.util.Map;
import java.util.Objects;
import java.util.WeakHashMap;
import java.util.concurrent.Callable;
import java.util.function.Consumer;

final class UiAsyncTaskSupport {

    private static final Map<Window, Integer> ACTIVE_LOADS = new WeakHashMap<>();

    private UiAsyncTaskSupport() {}

    static void beginLoading(Window window) {
        if (window == null) return;
        int active = ACTIVE_LOADS.merge(window, 1, Integer::sum);
        if (active == 1) {
            window.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        }
    }

    static void endLoading(Window window) {
        if (window == null) return;
        Integer active = ACTIVE_LOADS.get(window);
        if (active == null) return;
        if (active <= 1) {
            ACTIVE_LOADS.remove(window);
            window.setCursor(Cursor.getDefaultCursor());
        } else {
            ACTIVE_LOADS.put(window, active - 1);
        }
    }

    static int activeLoads(Window window) {
        return window == null ? 0 : ACTIVE_LOADS.getOrDefault(window, 0);
    }

    static <T> SwingWorker<T, Void> loadAsync(JComponent scope, Callable<T> fetch,
                                              Consumer<T> onDone, Consumer<Throwable> onError) {
        Window window = scope == null ? null : SwingUtilities.getWindowAncestor(scope);
        beginLoading(window);
        if (scope != null) {
            scope.putClientProperty("loading", Boolean.TRUE);
            scope.putClientProperty("loadError", null);
        }
        CurrentUserContext.UserSession capturedUser = CurrentUserContext.findCurrentUser();
        Long capturedCompany = CurrentUserContext.findCurrentCompanyId();

        SwingWorker<T, Void> worker = new SwingWorker<>() {
            @Override protected T doInBackground() throws Exception {
                if (capturedUser != null) {
                    CurrentUserContext.setCurrentUser(capturedUser.username(), capturedUser.role());
                }
                if (capturedCompany != null) {
                    CurrentUserContext.setCurrentCompanyId(capturedCompany);
                }
                try {
                    return fetch.call();
                } finally {
                    CurrentUserContext.clear();
                }
            }

            @Override protected void done() {
                try {
                    T result = get();
                    Long activeCompany = CurrentUserContext.findCurrentCompanyId();
                    if (Objects.equals(capturedCompany, activeCompany) && onDone != null) {
                        onDone.accept(result);
                    }
                } catch (Exception ex) {
                    Throwable cause = ex.getCause() != null ? ex.getCause() : ex;
                    if (onError != null) onError.accept(cause);
                } finally {
                    if (scope != null) scope.putClientProperty("loading", Boolean.FALSE);
                    endLoading(window);
                }
            }
        };
        worker.execute();
        return worker;
    }

    static <T> SwingWorker<T, Void> submitAsync(AbstractButton button, Callable<T> task,
                                                Consumer<T> onDone, Consumer<Throwable> onError) {
        if (Boolean.TRUE.equals(button.getClientProperty("submitting"))) return null;
        button.putClientProperty("submitting", Boolean.TRUE);
        button.setEnabled(false);
        return loadAsync(button, task, result -> {
            button.putClientProperty("submitting", Boolean.FALSE);
            button.setEnabled(true);
            if (onDone != null) onDone.accept(result);
        }, error -> {
            button.putClientProperty("submitting", Boolean.FALSE);
            button.setEnabled(true);
            if (onError != null) onError.accept(error);
        });
    }

    static JProgressBar createBusyBar() {
        JProgressBar bar = new JProgressBar();
        bar.setIndeterminate(true);
        bar.setBorderPainted(false);
        bar.setBackground(UIHelper.BG_CARD);
        bar.setForeground(UIHelper.ACCENT);
        bar.setPreferredSize(new Dimension(0, 6));
        return bar;
    }

    static <T> void runWithProgress(Component owner, String message, Callable<T> task,
                                    Consumer<T> onSuccess, Consumer<Throwable> onError) {
        Window win = owner == null ? null : SwingUtilities.getWindowAncestor(owner);
        JDialog dialog = new JDialog(win, Dialog.ModalityType.APPLICATION_MODAL);
        dialog.setUndecorated(true);

        JPanel panel = new JPanel(new BorderLayout(0, 12));
        panel.setBackground(UIHelper.BG_CARD);
        panel.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(UIHelper.BORDER, 1, true), new EmptyBorder(18, 26, 18, 26)));
        JLabel label = new JLabel(message);
        label.setForeground(UIHelper.TEXT_LIGHT);
        label.setFont(new Font(UIHelper.FONT, Font.BOLD, 13));
        panel.add(label, BorderLayout.NORTH);
        panel.add(createBusyBar(), BorderLayout.SOUTH);
        dialog.setContentPane(panel);
        dialog.pack();
        dialog.setMinimumSize(new Dimension(260, dialog.getHeight()));
        dialog.setLocationRelativeTo(win);

        CurrentUserContext.UserSession capturedUser = CurrentUserContext.findCurrentUser();
        Long capturedCompany = CurrentUserContext.findCurrentCompanyId();

        SwingWorker<T, Void> worker = new SwingWorker<>() {
            @Override protected T doInBackground() throws Exception {
                if (capturedUser != null) {
                    CurrentUserContext.setCurrentUser(capturedUser.username(), capturedUser.role());
                }
                if (capturedCompany != null) {
                    CurrentUserContext.setCurrentCompanyId(capturedCompany);
                }
                try {
                    return task.call();
                } finally {
                    CurrentUserContext.clear();
                }
            }
            @Override protected void done() {
                dialog.dispose();
                try {
                    T result = get();
                    if (onSuccess != null) onSuccess.accept(result);
                } catch (Exception ex) {
                    Throwable cause = ex.getCause() != null ? ex.getCause() : ex;
                    if (onError != null) onError.accept(cause);
                }
            }
        };
        worker.execute();
        dialog.setVisible(true);
    }
}
