package mz.multicore.erp.gui;

import mz.multicore.erp.gui.components.UIHelper;
import mz.multicore.erp.modules.subscription.dto.MySubscriptionDTO;

import javax.swing.*;
import java.awt.*;

/**
 * Renderizador da superfície visual do cartão de subscrição corporativa em ConfigSubscriptionPanel.
 */
final class ConfigSubscriptionCardRenderer {

    private ConfigSubscriptionCardRenderer() {}

    static void renderSubscription(JPanel card, MySubscriptionDTO sub) {
        if (card == null) return;
        card.removeAll();
        if (sub == null || !sub.hasSubscription()) {
            renderEmpty(card);
            return;
        }
        card.setLayout(new GridLayout(0, 2, 24, 14));
        card.add(field("Empresa", sub.companyName()));
        card.add(field("Plano", sub.planLabel()));
        card.add(field("Estado", sub.statusLabel(), statusColor(sub.status())));
        card.add(field("Válida até", sub.validUntil() == null ? "—" : sub.validUntil().toString()));
        card.add(field("Dias restantes", daysText(sub.daysRemaining()), daysColor(sub.daysRemaining())));
        card.add(field("Mensalidade", sub.monthlyPrice() == null ? "—" : sub.monthlyPrice().toPlainString() + " MT"));
        refreshCard(card);
    }

    static void renderEmpty(JPanel card) {
        if (card == null) return;
        card.removeAll();
        card.setLayout(new GridBagLayout());
        card.add(bigInfo("Sem assinatura definida", "A sua empresa ainda não tem um plano associado.", UIHelper.TEXT_MUTED));
        refreshCard(card);
    }

    static void renderError(JPanel card, String message) {
        if (card == null) return;
        card.removeAll();
        card.setLayout(new GridBagLayout());
        card.add(bigInfo("Não foi possível carregar a assinatura", message, UIHelper.REJECTED_RED));
        refreshCard(card);
    }

    static String daysText(Long days) {
        if (days == null) return "—";
        if (days < 0) return "Expirada há " + Math.abs(days) + " dia(s)";
        return days == 0 ? "Expira hoje" : days + " dia(s)";
    }

    static Color daysColor(Long days) {
        if (days == null) return UIHelper.TEXT_LIGHT;
        if (days < 0) return UIHelper.REJECTED_RED;
        return days <= 7 ? UIHelper.PENDING_YELLOW : UIHelper.APPROVED_GREEN;
    }

    static Color statusColor(String status) {
        if ("ACTIVE".equals(status) || "TRIAL".equals(status)) return UIHelper.APPROVED_GREEN;
        return ("EXPIRED".equals(status) || "SUSPENDED".equals(status)) ? UIHelper.REJECTED_RED : UIHelper.TEXT_LIGHT;
    }

    private static void refreshCard(JPanel card) {
        card.revalidate();
        card.repaint();
    }

    private static JPanel field(String label, String value) { return field(label, value, UIHelper.TEXT_LIGHT); }

    private static JPanel field(String label, String value, Color valueColor) {
        JPanel p = new JPanel(); p.setOpaque(false); p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        JLabel l = new JLabel(label); l.setFont(new Font(UIHelper.FONT, Font.BOLD, 11)); l.setForeground(UIHelper.TEXT_MUTED);
        JLabel v = new JLabel(value == null ? "—" : value); v.setFont(new Font(UIHelper.FONT, Font.BOLD, 18)); v.setForeground(valueColor);
        p.add(l); p.add(v);
        return p;
    }

    private static JPanel bigInfo(String title, String detail, Color color) {
        JPanel p = new JPanel(); p.setOpaque(false); p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        JLabel t = new JLabel(title); t.setFont(new Font(UIHelper.FONT, Font.BOLD, 16)); t.setForeground(color);
        JLabel d = new JLabel("<html><body style='width:360px'>" + (detail == null ? "" : detail) + "</body></html>");
        d.setFont(new Font(UIHelper.FONT, Font.PLAIN, 12)); d.setForeground(UIHelper.TEXT_MUTED);
        p.add(t); p.add(Box.createVerticalStrut(6)); p.add(d);
        return p;
    }
}
