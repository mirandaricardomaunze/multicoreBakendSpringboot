package mz.multicore.erp.gui;

import mz.multicore.erp.desktop.client.ComercialApiClient;
import mz.multicore.erp.desktop.client.POSApiClient;
import mz.multicore.erp.gui.components.KpiCard;
import mz.multicore.erp.gui.components.UIHelper;
import mz.multicore.erp.modules.pos.dto.PosZReportDTO;
import mz.multicore.erp.modules.pos.dto.TillSessionDTO;

import javax.swing.*;
import java.awt.*;
import java.math.BigDecimal;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * Coordenador de apresentação, snapshot assíncrono e estrutura de KPIs do vendedor.
 */
public final class SellerDashboardViewCoordinator {

    private SellerDashboardViewCoordinator() {}

    public record SellerSnapshot(boolean cashOpen, BigDecimal sales, int saleCount,
                                 long pendingOrders, long openQuotations) {}

    public static String formatWelcome(String displayName) {
        String name = displayName == null || displayName.isBlank() ? "Vendedor" : displayName.trim();
        return "Olá, " + name + ". Pronto para vender?";
    }

    public static JPanel buildHeader(JLabel welcomeLabel, JLabel cashStatus) {
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

    public static JPanel buildKpis(JLabel salesVal, JLabel salesCnt, JLabel ordersVal,
                                   JLabel quotesVal, Consumer<String> onAction) {
        JPanel grid = KpiCard.createHeroGrid(4, 10, 10);
        grid.setMaximumSize(new Dimension(Integer.MAX_VALUE, KpiCard.STANDARD_CARD_HEIGHT));
        grid.add(KpiCard.createInteractiveCard("VENDAS NA SESSÃO", salesVal, "Apenas o seu caixa actual",
                "fas-cash-register", UIHelper.APPROVED_GREEN, "Abrir o POS", () -> onAction.accept("pos")));
        grid.add(KpiCard.createInteractiveCard("ATENDIMENTOS", salesCnt, "Vendas concluídas na sessão",
                "fas-shopping-basket", UIHelper.ACCENT_CYAN, "Abrir o POS", () -> onAction.accept("pos")));
        grid.add(KpiCard.createInteractiveCard("ENCOMENDAS PENDENTES", ordersVal, "Operação comercial da empresa",
                "fas-clipboard-list", UIHelper.ACCENT_BLUE, "Consultar encomendas", () -> onAction.accept("orders")));
        grid.add(KpiCard.createInteractiveCard("COTAÇÕES ABERTAS", quotesVal, "Propostas que exigem seguimento",
                "fas-file-signature", UIHelper.PENDING_YELLOW, "Consultar cotações", () -> onAction.accept("quotations")));
        return grid;
    }

    public static SellerSnapshot loadSnapshot(ComercialApiClient comercialApiClient, POSApiClient posApiClient,
                                              Long companyId, String username) {
        Optional<TillSessionDTO> session = posApiClient.getActiveSession(username, companyId);
        PosZReportDTO z = session.map(value -> posApiClient.getZReport(value.id())).orElse(null);
        long pendingOrders = comercialApiClient.getPendingOrdersByCompany(companyId).size();
        long openQuotations = comercialApiClient.getOpenQuotationsByCompany(companyId).size();
        return new SellerSnapshot(session.isPresent(), z == null ? BigDecimal.ZERO : z.totalSales(),
                z == null ? 0 : z.saleCount(), pendingOrders, openQuotations);
    }

    public static void applySnapshot(SellerSnapshot value, JLabel salesValue, JLabel salesCount,
                                     JLabel ordersValue, JLabel quotationsValue, JLabel cashStatus) {
        salesValue.setText(String.format("%,.2f MT", value.sales()));
        salesCount.setText(String.valueOf(value.saleCount()));
        ordersValue.setText(String.valueOf(value.pendingOrders()));
        quotationsValue.setText(String.valueOf(value.openQuotations()));
        cashStatus.setText(value.cashOpen() ? "Caixa aberto no seu nome" : "Caixa fechado - abra-o antes de vender");
        cashStatus.setForeground(value.cashOpen() ? UIHelper.APPROVED_GREEN : UIHelper.PENDING_YELLOW);
    }

    public static void applyLoadError(JLabel cashStatus) {
        cashStatus.setText("Não foi possível actualizar agora. Use Actualizar para tentar novamente.");
        cashStatus.setForeground(UIHelper.PENDING_YELLOW);
    }
}
