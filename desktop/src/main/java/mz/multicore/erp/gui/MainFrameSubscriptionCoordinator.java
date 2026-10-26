package mz.multicore.erp.gui;

import mz.multicore.erp.desktop.client.MySubscriptionApiClient;
import mz.multicore.erp.gui.components.FeedbackType;
import mz.multicore.erp.gui.components.ToastManager;
import mz.multicore.erp.gui.components.UIHelper;
import mz.multicore.erp.modules.subscription.dto.MySubscriptionDTO;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.Timer;
import javax.swing.border.EmptyBorder;
import java.awt.Color;
import java.awt.Component;
import java.awt.Font;

/**
 * Coordenador do ciclo de vida e monitorização de assinaturas empresariais no {@link MainFrame}.
 */
final class MainFrameSubscriptionCoordinator {

    private static final long SUB_ALERT_DAYS = 7;
    private static final int SUB_WATCH_INTERVAL_MS = 6 * 60 * 60 * 1000;

    private MainFrameSubscriptionCoordinator() {
    }

    static MySubscriptionDTO mySubscriptionSafe(MySubscriptionApiClient client, boolean superAdmin) {
        if (superAdmin || client == null) return null;
        try {
            return client.getMySubscription();
        } catch (RuntimeException ex) {
            return null;
        }
    }

    static int subscriptionSeverity(MySubscriptionDTO s) {
        if (s == null || !s.hasSubscription()) return 1;
        if ("EXPIRED".equals(s.status()) || "SUSPENDED".equals(s.status())) return -1;
        Long d = s.daysRemaining();
        if (d != null && d >= 0 && d <= SUB_ALERT_DAYS) return 0;
        return 1;
    }

    static JComponent buildSubscriptionChip(MySubscriptionApiClient client, boolean superAdmin) {
        MySubscriptionDTO s = mySubscriptionSafe(client, superAdmin);
        int sev = subscriptionSeverity(s);
        if (sev == 1 || s == null) return null;

        boolean expired = sev == -1;
        Color color = expired ? UIHelper.REJECTED_RED : UIHelper.PENDING_YELLOW;
        String text = expired
                ? "Assinatura " + s.statusLabel().toLowerCase()
                : (s.daysRemaining() == 0 ? "Assinatura expira hoje" : "Assinatura: " + s.daysRemaining() + " dia(s)");

        JLabel chip = new JLabel(text, UIHelper.icon("fas-exclamation-triangle", 14, color), JLabel.LEFT);
        chip.setFont(new Font(UIHelper.FONT, Font.BOLD, 12));
        chip.setForeground(color);
        chip.setToolTipText("Veja Configurações → A Minha Assinatura, ou contacte o suporte.");
        chip.setBorder(new EmptyBorder(0, 6, 0, 6));
        return chip;
    }

    static void checkSubscriptionOnStartup(Component parent, MySubscriptionApiClient client, boolean superAdmin, Runnable onExpired) {
        MySubscriptionDTO s = mySubscriptionSafe(client, superAdmin);
        int sev = subscriptionSeverity(s);
        if (sev == 1 || s == null) return;
        if (sev == -1) {
            enforceExpiredSubscription(parent, s, onExpired);
            return;
        }
        String msg = "A assinatura da sua empresa expira "
                + (s.daysRemaining() == 0 ? "hoje" : "em " + s.daysRemaining() + " dia(s)")
                + " (" + s.validUntil() + ").\nContacte o suporte da plataforma para renovar a tempo.";
        ToastManager.show(parent, FeedbackType.WARNING, msg);
    }

    static Timer startSubscriptionWatch(Component parent, MySubscriptionApiClient client, boolean superAdmin, Runnable onExpired) {
        if (superAdmin || client == null) return null;
        Timer timer = new Timer(SUB_WATCH_INTERVAL_MS, e -> {
            MySubscriptionDTO s = mySubscriptionSafe(client, superAdmin);
            if (subscriptionSeverity(s) == -1) {
                enforceExpiredSubscription(parent, s, onExpired);
            }
        });
        timer.setInitialDelay(SUB_WATCH_INTERVAL_MS);
        timer.start();
        return timer;
    }

    static void enforceExpiredSubscription(Component parent, MySubscriptionDTO s, Runnable onExpired) {
        String estado = (s != null && s.statusLabel() != null) ? s.statusLabel().toLowerCase() : "expirada";
        JOptionPane.showMessageDialog(parent,
                "A assinatura da sua empresa está " + estado + ".\n"
                        + "O acesso vai ser suspenso. Contacte o suporte da plataforma para regularizar.",
                "Assinatura", JOptionPane.ERROR_MESSAGE);
        if (onExpired != null) {
            onExpired.run();
        }
    }
}
