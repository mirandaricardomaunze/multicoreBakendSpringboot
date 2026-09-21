package mz.multicore.erp.gui.components;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Font;

/** Banner contextual reutilizavel para erro, aviso ou informacao dentro do proprio fluxo. */
public final class InlineFeedbackPanel extends JPanel {

    private final JLabel icon = new JLabel();
    private final JLabel title = new JLabel();
    private final JLabel message = new JLabel();
    private final ModernButton actionButton = UIHelper.createSecondaryButton("Tentar novamente");
    private final ModernButton closeButton = UIHelper.createIconButton("Fechar mensagem", "fas-times");
    private FeedbackType type = FeedbackType.INFO;

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

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        actions.setOpaque(false);
        actionButton.setVisible(false);
        closeButton.addActionListener(e -> clear());
        actions.add(actionButton);
        actions.add(closeButton);

        add(icon, BorderLayout.WEST);
        add(text, BorderLayout.CENTER);
        add(actions, BorderLayout.EAST);
        setVisible(false);
        getAccessibleContext().setAccessibleName("Mensagem de estado");
    }

    @Override
    public void updateUI() {
        super.updateUI();
        if (title != null) applyVisualStyle();
    }

    public void show(FeedbackType feedbackType, String detail) {
        show(feedbackType, feedbackType.title(), detail, null, null);
    }

    public void show(FeedbackType feedbackType, String heading, String detail,
                     String actionLabel, Runnable action) {
        Runnable update = () -> {
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
        };
        if (SwingUtilities.isEventDispatchThread()) update.run(); else SwingUtilities.invokeLater(update);
    }

    public void clear() {
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
