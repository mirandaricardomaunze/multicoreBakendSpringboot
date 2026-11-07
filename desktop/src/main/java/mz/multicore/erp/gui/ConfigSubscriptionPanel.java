package mz.multicore.erp.gui;

import mz.multicore.erp.gui.components.ModernButton;
import mz.multicore.erp.gui.components.ModernPanel;
import mz.multicore.erp.gui.components.UIHelper;
import mz.multicore.erp.modules.subscription.dto.MySubscriptionDTO;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

/**
 * Gestão e apresentação do estado da assinatura corporativa da empresa,
 * alertas de validade e acionamento de renovação/ativação de planos.
 */
final class ConfigSubscriptionPanel {

    private final ConfigPanel owner;
    private JPanel subscriptionCard;

    ConfigSubscriptionPanel(ConfigPanel owner) { this.owner = owner; }

    public JPanel buildPanel() {
        JPanel panel = new JPanel(new BorderLayout(0, 15));
        panel.setBackground(UIHelper.BG_DARK);
        panel.setBorder(new EmptyBorder(15, 15, 15, 15));

        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        header.add(UIHelper.createHeading("Estado da Assinatura"), BorderLayout.WEST);

        ModernButton renewBtn = UIHelper.createPrimaryButton("Renovar / Activar Plano");
        renewBtn.setIcon(UIHelper.icon("fas-crown", 13, Color.WHITE));
        renewBtn.addActionListener(e -> SubscriptionRenewalDialog.show(owner, owner.mySubscriptionApiClient, this::applyMySubscription));

        ModernButton refreshBtn = UIHelper.createRefreshButton(() -> loadMySubscription());

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        actions.setOpaque(false);
        actions.add(renewBtn);
        actions.add(refreshBtn);
        header.add(actions, BorderLayout.EAST);
        panel.add(header, BorderLayout.NORTH);

        subscriptionCard = new ModernPanel(16);
        subscriptionCard.setLayout(new GridBagLayout());
        subscriptionCard.setBorder(new EmptyBorder(24, 24, 24, 24));
        JPanel wrap = new JPanel(new BorderLayout());
        wrap.setOpaque(false);
        wrap.add(subscriptionCard, BorderLayout.NORTH);
        panel.add(wrap, BorderLayout.CENTER);
        return panel;
    }

    public void refresh() { loadMySubscription(); }

    void loadMySubscription() {
        if (subscriptionCard == null) return;
        UIHelper.loadAsync(owner, owner.mySubscriptionApiClient::getMySubscription, this::applyMySubscription,
                error -> ConfigSubscriptionCardRenderer.renderError(subscriptionCard, error.getMessage()));
    }

    private void applyMySubscription(MySubscriptionDTO sub) {
        ConfigSubscriptionCardRenderer.renderSubscription(subscriptionCard, sub);
    }
}
