package mz.multicore.erp.gui;

import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.desktop.client.ComercialApiClient;
import mz.multicore.erp.desktop.client.POSApiClient;
import mz.multicore.erp.gui.SellerDashboardViewCoordinator.SellerSnapshot;
import mz.multicore.erp.gui.components.KpiCard;
import mz.multicore.erp.gui.components.ModernButton;
import mz.multicore.erp.gui.components.ModernPanel;
import mz.multicore.erp.gui.components.UIHelper;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.function.Consumer;

/** Painel operacional do vendedor, sem indicadores financeiros globais ou funções de gestão. */
public final class SellerDashboardPanel extends JPanel {

    private final ComercialApiClient comercialApiClient;
    private final POSApiClient posApiClient;
    private final Consumer<String> actionHandler;
    private final JLabel welcomeLabel = new JLabel("Olá, Vendedor.");
    private final JLabel salesValue = KpiCard.valueLabel("0,00 MT", 19);
    private final JLabel salesCount = KpiCard.valueLabel("0", 19);
    private final JLabel ordersValue = KpiCard.valueLabel("0", 19);
    private final JLabel quotationsValue = KpiCard.valueLabel("0", 19);
    private final JLabel cashStatus = new JLabel("A verificar sessão de caixa...");
    private int refreshVersion;

    public SellerDashboardPanel(ComercialApiClient comercialApiClient, POSApiClient posApiClient,
                                Consumer<String> actionHandler) {
        this.comercialApiClient = comercialApiClient;
        this.posApiClient = posApiClient;
        this.actionHandler = actionHandler;

        setLayout(new BorderLayout(0, 14));
        setBackground(UIHelper.BG_DARK);
        setBorder(new EmptyBorder(20, 22, 20, 22));
        add(SellerDashboardViewCoordinator.buildHeader(welcomeLabel, cashStatus), BorderLayout.NORTH);

        JPanel content = new JPanel();
        content.setOpaque(false);
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.add(SellerDashboardViewCoordinator.buildKpis(salesValue, salesCount, ordersValue, quotationsValue, this::action));
        content.add(Box.createVerticalStrut(14));
        content.add(buildQuickActions());
        add(content, BorderLayout.CENTER);
    }

    private JPanel buildQuickActions() {
        ModernPanel card = new ModernPanel(16);
        card.setLayout(new BorderLayout(0, 12));
        card.setBorder(BorderFactory.createCompoundBorder(card.getBorder(), new EmptyBorder(16, 16, 16, 16)));
        card.add(UIHelper.createSubheading("Acções rápidas do vendedor"), BorderLayout.NORTH);

        JPanel actions = new JPanel(new GridLayout(2, 3, 10, 10));
        actions.setOpaque(false);
        actions.add(actionButton("Nova venda POS", "fas-cash-register", UIHelper.APPROVED_GREEN, "pos"));
        actions.add(actionButton("Nova encomenda", "fas-clipboard-list", UIHelper.ACCENT_BLUE, "new-order"));
        actions.add(actionButton("Nova cotação", "fas-file-signature", UIHelper.PENDING_YELLOW, "new-quotation"));
        actions.add(actionButton("Pesquisar clientes", "fas-address-book", UIHelper.MODULE_CLIENTES, "clients"));
        actions.add(actionButton("Consultar produtos", "fas-box-open", UIHelper.ACCENT_CYAN, "products"));
        actions.add(actionButton("Notificações", "fas-bell", UIHelper.ACCENT_ORANGE, "notifications"));
        card.add(actions, BorderLayout.CENTER);
        return card;
    }

    private ModernButton actionButton(String text, String icon, Color color, String act) {
        ModernButton button = UIHelper.createSecondaryButton(text);
        button.setIcon(UIHelper.icon(icon, 16, color));
        button.setPreferredSize(new Dimension(180, 48));
        button.addActionListener(event -> action(act));
        return button;
    }

    private void action(String action) {
        if (actionHandler != null) actionHandler.accept(action);
    }

    public void updateWelcomeMessage(String displayName) {
        welcomeLabel.setText(SellerDashboardViewCoordinator.formatWelcome(displayName));
    }

    public void refreshData() {
        int version = ++refreshVersion;
        Long companyId = CurrentUserContext.getCurrentCompanyId();
        String username = CurrentUserContext.getUsername();
        new SwingWorker<SellerSnapshot, Void>() {
            @Override protected SellerSnapshot doInBackground() {
                return SellerDashboardViewCoordinator.loadSnapshot(comercialApiClient, posApiClient, companyId, username);
            }
            @Override protected void done() {
                if (version != refreshVersion) return;
                try {
                    SellerDashboardViewCoordinator.applySnapshot(get(), salesValue, salesCount,
                            ordersValue, quotationsValue, cashStatus);
                } catch (Exception error) {
                    SellerDashboardViewCoordinator.applyLoadError(cashStatus);
                }
            }
        }.execute();
    }
}
