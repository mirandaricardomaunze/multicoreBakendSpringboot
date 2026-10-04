package mz.multicore.erp.gui.components;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Point;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * Banner contextual reutilizável para erro, aviso ou informação dentro do próprio fluxo.
 * Suporta fecho manual pelo utilizador e fecho automático temporizado (com pausa ao passar o rato).
 */
public final class InlineFeedbackPanel extends JPanel {

    public static final int DURATION_SUCCESS_MS = 5000;
    public static final int DURATION_INFO_MS = 5000;
    public static final int DURATION_WARNING_MS = 7000;
    public static final int DURATION_ERROR_MS = 8000;

    private final JLabel icon = new JLabel();
    private final JLabel title = new JLabel();
    private final JLabel message = new JLabel();
    private final ModernButton actionButton = UIHelper.createSecondaryButton("Tentar novamente");
    private final ModernButton closeButton = UIHelper.createIconButton("Fechar mensagem", "fas-times");
    private FeedbackType type = FeedbackType.INFO;

    private Timer autoCloseTimer;
    private boolean autoCloseEnabled = true;
    private boolean hovered = false;

    public InlineFeedbackPanel() {
        setLayout(new BorderLayout(12, 0));
        setOpaque(true);
        setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UIHelper.BORDER),
                BorderFactory.createEmptyBorder(10, 12, 10, 10)));

        JPanel text = new JPanel();
        text.setOpaque(false);
        text.setLayout(new BoxLayout(text, BoxLayout.Y_AXIS));
        title.setFont(new Font(UIHelper.FONT, Font.BOLD, 12));
        message.setFont(new Font(UIHelper.FONT, Font.PLAIN, 12));
        text.add(title);
        text.add(Box.createVerticalStrut(2));
        text.add(message);

        closeButton.setPreferredSize(new Dimension(28, 28));
        closeButton.setMinimumSize(new Dimension(28, 28));
        closeButton.setToolTipText("Fechar mensagem");
        closeButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        closeButton.addActionListener(e -> clear());

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        actions.setOpaque(false);
        actionButton.setVisible(false);
        actions.add(actionButton);
        actions.add(closeButton);

        add(icon, BorderLayout.WEST);
        add(text, BorderLayout.CENTER);
        add(actions, BorderLayout.EAST);
        setVisible(false);
        getAccessibleContext().setAccessibleName("Mensagem de estado");

        installHoverListeners(this);
    }

    public static int defaultDurationFor(FeedbackType feedbackType) {
        if (feedbackType == null) return DURATION_INFO_MS;
        return switch (feedbackType) {
            case SUCCESS -> DURATION_SUCCESS_MS;
            case INFO -> DURATION_INFO_MS;
            case WARNING -> DURATION_WARNING_MS;
            case ERROR -> DURATION_ERROR_MS;
        };
    }

    @Override
    public void updateUI() {
        super.updateUI();
        if (title != null) applyVisualStyle();
    }

    public void show(FeedbackType feedbackType, String detail) {
        show(feedbackType, feedbackType == null ? null : feedbackType.title(), detail, null, null, defaultDurationFor(feedbackType));
    }

    public void show(FeedbackType feedbackType, String detail, int autoCloseDurationMs) {
        show(feedbackType, feedbackType == null ? null : feedbackType.title(), detail, null, null, autoCloseDurationMs);
    }

    public void show(FeedbackType feedbackType, String heading, String detail,
                     String actionLabel, Runnable action) {
        show(feedbackType, heading, detail, actionLabel, action, defaultDurationFor(feedbackType));
    }

    public void show(FeedbackType feedbackType, String heading, String detail,
                     String actionLabel, Runnable action, int autoCloseDurationMs) {
        Runnable update = () -> {
            stopAutoCloseTimer();
            type = feedbackType == null ? FeedbackType.INFO : feedbackType;
            title.setText(safe(heading, type.title()));
            message.setText(toHtml(safe(detail, "Ocorreu um problema inesperado.")));
            icon.setIcon(UIHelper.icon(type.iconCode(), 18, type.color()));
            applyVisualStyle();
            actionButton.setText(safe(actionLabel, "Tentar novamente"));
            for (var listener : actionButton.getActionListeners()) actionButton.removeActionListener(listener);
            if (action != null) actionButton.addActionListener(e -> action.run());
            actionButton.setVisible(action != null);
            setVisible(true);
            revalidate();
            repaint();
            if (getParent() != null) {
                getParent().revalidate();
                getParent().repaint();
            }

            if (autoCloseEnabled && autoCloseDurationMs > 0) {
                startAutoCloseTimer(autoCloseDurationMs);
            }
        };
        if (SwingUtilities.isEventDispatchThread()) update.run(); else SwingUtilities.invokeLater(update);
    }

    private void startAutoCloseTimer(int durationMs) {
        stopAutoCloseTimer();
        autoCloseTimer = new Timer(durationMs, e -> {
            if (!hovered) {
                clear();
            }
        });
        autoCloseTimer.setRepeats(false);
        if (!hovered) {
            autoCloseTimer.start();
        }
    }

    public void stopAutoCloseTimer() {
        if (autoCloseTimer != null) {
            autoCloseTimer.stop();
            autoCloseTimer = null;
        }
    }

    public void clear() {
        stopAutoCloseTimer();
        hovered = false;
        setVisible(false);
        actionButton.setVisible(false);
        revalidate();
        if (getParent() != null) {
            getParent().revalidate();
            getParent().repaint();
        }
    }

    public FeedbackType feedbackType() { return type; }
    public String messageText() { return message.getText(); }
    public boolean hasAction() { return actionButton.isVisible(); }
    public boolean isAutoCloseEnabled() { return autoCloseEnabled; }
    public void setAutoCloseEnabled(boolean autoCloseEnabled) {
        this.autoCloseEnabled = autoCloseEnabled;
        if (!autoCloseEnabled) {
            stopAutoCloseTimer();
        }
    }
    public Timer getAutoCloseTimer() { return autoCloseTimer; }
    public boolean isHovered() { return hovered; }
    public ModernButton getCloseButton() { return closeButton; }

    private void installHoverListeners(Component c) {
        c.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                hovered = true;
                if (autoCloseTimer != null && autoCloseTimer.isRunning()) {
                    autoCloseTimer.stop();
                }
            }

            @Override
            public void mouseExited(MouseEvent e) {
                Point p = SwingUtilities.convertPoint(c, e.getPoint(), InlineFeedbackPanel.this);
                if (!contains(p)) {
                    hovered = false;
                    if (autoCloseTimer != null && isVisible() && autoCloseEnabled) {
                        autoCloseTimer.restart();
                    }
                }
            }
        });
        if (c instanceof java.awt.Container container) {
            for (Component child : container.getComponents()) {
                installHoverListeners(child);
            }
        }
    }

    private void applyVisualStyle() {
        title.setForeground(type.color());
        message.setForeground(UIHelper.TEXT_LIGHT);
        setBackground(tintedBackground(type.color()));
        setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UIHelper.BORDER),
                BorderFactory.createEmptyBorder(10, 12, 10, 10)));
    }

    private static String safe(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private static String toHtml(String value) {
        String escaped = value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
        return "<html><body style='width:420px'>" + escaped.replace("\n", "<br>") + "</body></html>";
    }

    private static Color tintedBackground(Color accent) {
        Color base = UIHelper.BG_CARD;
        return new Color(
                (base.getRed() * 9 + accent.getRed()) / 10,
                (base.getGreen() * 9 + accent.getGreen()) / 10,
                (base.getBlue() * 9 + accent.getBlue()) / 10);
    }
}
