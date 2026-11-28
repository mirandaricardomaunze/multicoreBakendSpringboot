package mz.multicore.erp.gui;

import mz.multicore.erp.desktop.client.OnlineShopApiClient;
import mz.multicore.erp.desktop.session.SignedInUser;
import mz.multicore.erp.gui.NotificationFeed.NotificationItem;
import mz.multicore.erp.gui.components.UIHelper;
import mz.multicore.erp.modules.onlineshop.dto.OnlineShopUnbilledOrderDTO;

import java.util.List;

/**
 * Encomendas da loja online no sino (docs/LOJA_ONLINE_SPEC.md §3.5, LO-UI-03): uma linha por encomenda
 * {@code LOJA_ONLINE} ainda por facturar e não cancelada, que abre o card das encomendas. Só os perfis de vendas
 * ({@link SignedInUser#isSellerOrAbove()}) as vêem; os outros não fazem pedido.
 */
final class NotificationOnlineShopAlertProvider {

    static final String TYPE = "Loja online";
    static final String TITLE = "Encomenda da loja online por facturar";
    static final String MODULE_CARD = "comercial";
    static final int PRIORITY = 2;

    private NotificationOnlineShopAlertProvider() {}

    static void addOnlineShopOrders(List<NotificationItem> items, OnlineShopApiClient onlineShopApiClient) {
        if (onlineShopApiClient == null || !SignedInUser.isSellerOrAbove()) return;
        try {
            List<OnlineShopUnbilledOrderDTO> orders = onlineShopApiClient.unbilledOrders();
            if (orders == null) return;
            for (OnlineShopUnbilledOrderDTO order : orders) {
                items.add(new NotificationItem(TYPE, TITLE, detail(order), when(order), MODULE_CARD, PRIORITY));
            }
        } catch (Exception ignored) {
            // O sino nunca falha por um fornecedor: sem encomendas online, mostra os restantes.
        }
    }

    /** "{número} — {cliente} · {fulfilmentLabel}", sem as partes que faltarem. */
    static String detail(OnlineShopUnbilledOrderDTO order) {
        StringBuilder text = new StringBuilder(order.number() == null ? "" : order.number());
        if (order.customerName() != null && !order.customerName().isBlank()) {
            text.append(" — ").append(order.customerName());
        }
        if (order.fulfilmentLabel() != null && !order.fulfilmentLabel().isBlank()) {
            text.append(" · ").append(order.fulfilmentLabel());
        }
        return text.toString();
    }

    private static String when(OnlineShopUnbilledOrderDTO order) {
        return order.createdAt() == null ? "Por facturar" : order.createdAt().toLocalDate().format(UIHelper.DATE_FMT);
    }
}
