package mz.multicore.erp.gui;

import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.desktop.client.ComercialApiClient;
import mz.multicore.erp.desktop.client.POSApiClient;
import mz.multicore.erp.gui.components.KpiCard;
import mz.multicore.erp.gui.components.ModernButton;
import mz.multicore.erp.gui.components.ModernPanel;
import mz.multicore.erp.gui.components.UIHelper;
import mz.multicore.erp.modules.pos.dto.PosZReportDTO;
import mz.multicore.erp.modules.pos.dto.TillSessionDTO;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingWorker;
import javax.swing.border.EmptyBorder;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.math.BigDecimal;
import java.util.Optional;
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
        add(buildHeader(), BorderLayout.NORTH);

        JPanel content = new JPanel();
        content.setOpaque(false);
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.add(buildKpis());
        content.add(Box.createVerticalStrut(14));
        content.add(buildQuickActions());
        add(content, BorderLayout.CENTER);
    }

    private JPanel buildHeader() {
        JPanel header = new JPanel(new BorderLayout(12, 6));
        header.setOpaque(false);
        JPanel titles = new JPanel();
        titles.setOpaque(false);
        titles.setLayout(new BoxLayout(titles, BoxLayout.Y_AXIS));
        welcomeLabel.setFont(new Font(UIHelper.FONT, Font.BOLD, 22));
        welcomeLabel.setForeground(UIHelper.TEXT_LIGHT);
        JLabel subtitle = new JLabel("Venda rapidamente e acompanhe apenas a sua operação comercial.");
        subtitle.setFont(new Font(UIHelper.FONT, Font.PLAIN, 13));
        subtitle.setForeground(UIHelper.TEXT_MUTED);
        titles.add(welcomeLabel);
        titles.add(subtitle);
        cashStatus.setFont(new Font(UIHelper.FONT, Font.BOLD, 12));
        cashStatus.setForeground(UIHelper.ACCENT_CYAN);
        header.add(titles, BorderLayout.WEST);
        header.add(cashStatus, BorderLayout.EAST);
        return header;
    }

    private JPanel buildKpis() {
        JPanel grid = KpiCard.createGrid(4, 10, 10);
        grid.setMaximumSize(new Dimension(Integer.MAX_VALUE, KpiCard.STANDARD_CARD_HEIGHT));
        grid.add(KpiCard.createInteractiveCard("VENDAS NA SESSÃO", salesValue, "Apenas o seu caixa actual",
                "fas-cash-register", UIHelper.APPROVED_GREEN, "Abrir o POS", () -> action("pos")));
        grid.add(KpiCard.createInteractiveCard("ATENDIMENTOS", salesCount, "Vendas concluídas na sessão",
                "fas-shopping-basket", UIHelper.ACCENT_CYAN, "Abrir o POS", () -> action("pos")));
        grid.add(KpiCard.createInteractiveCard("ENCOMENDAS PENDENTES", ordersValue, "Operação comercial da empresa",
                "fas-clipboard-list", UIHelper.ACCENT_BLUE, "Consultar encomendas", () -> action("orders")));
        grid.add(KpiCard.createInteractiveCard("COTAÇÕES ABERTAS", quotationsValue, "Propostas que exigem seguimento",
                "fas-file-signature", UIHelper.PENDING_YELLOW, "Consultar cotações", () -> action("quotations")));
        return grid;
    }

    private JPanel buildQuickActions() {
        ModernPanel card = new ModernPanel(16);
        card.setLayout(new BorderLayout(0, 12));
        card.setBorder(BorderFactory.createCompoundBorder(card.getBorder(), new EmptyBorder(16, 16, 16, 16)));
        JLabel title = UIHelper.createSubheading("Acções rápidas do vendedor");
        card.add(title, BorderLayout.NORTH);

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

    private ModernButton actionButton(String text, String icon, java.awt.Color color, String action) {
        ModernButton button = UIHelper.createSecondaryButton(text);
        button.setIcon(UIHelper.icon(icon, 16, color));
        button.setPreferredSize(new Dimension(180, 48));
        button.addActionListener(event -> action(action));
        return button;
    }

    private void action(String action) {
        if (actionHandler != null) actionHandler.accept(action);
    }

    public void updateWelcomeMessage(String displayName) {
        String name = displayName == null || displayName.isBlank() ? "Vendedor" : displayName.trim();
        welcomeLabel.setText("Olá, " + name + ". Pronto para vender?");
    }

    public void refreshData() {
        int version = ++refreshVersion;
        Long companyId = CurrentUserContext.getCurrentCompanyId();
        String username = CurrentUserContext.getUsername();
        new SwingWorker<Snapshot, Void>() {
            @Override protected Snapshot doInBackground() {
                Optional<TillSessionDTO> session = posApiClient.getActiveSession(username, companyId);
                PosZReportDTO z = session.map(value -> posApiClient.getZReport(value.id())).orElse(null);
                long pendingOrders = comercialApiClient.getPendingOrdersByCompany(companyId).size();
                long openQuotations = comercialApiClient.getOpenQuotationsByCompany(companyId).size();
                return new Snapshot(session.isPresent(), z == null ? BigDecimal.ZERO : z.totalSales(),
                        z == null ? 0 : z.saleCount(), pendingOrders, openQuotations);
            }

            @Override protected void done() {
                if (version != refreshVersion) return;
                try {
                    Snapshot value = get();
                    salesValue.setText(String.format("%,.2f MT", value.sales()));
                    salesCount.setText(String.valueOf(value.saleCount()));
                    ordersValue.setText(String.valueOf(value.pendingOrders()));
                    quotationsValue.setText(String.valueOf(value.openQuotations()));
                    cashStatus.setText(value.cashOpen() ? "Caixa aberto no seu nome" : "Caixa fechado — abra-o antes de vender");
                    cashStatus.setForeground(value.cashOpen() ? UIHelper.APPROVED_GREEN : UIHelper.PENDING_YELLOW);
                } catch (Exception error) {
                    cashStatus.setText("Não foi possível actualizar agora. Use Actualizar para tentar novamente.");
                    cashStatus.setForeground(UIHelper.PENDING_YELLOW);
                }
            }
        }.execute();
    }

    private record Snapshot(boolean cashOpen, BigDecimal sales, int saleCount,
                            long pendingOrders, long openQuotations) { }
}
