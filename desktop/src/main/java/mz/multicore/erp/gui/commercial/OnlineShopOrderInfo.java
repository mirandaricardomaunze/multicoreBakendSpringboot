package mz.multicore.erp.gui.commercial;

import mz.multicore.erp.gui.components.RecordDetailsDialog;
import mz.multicore.erp.modules.comercial.dto.OrderDTO;

import java.util.ArrayList;
import java.util.List;

/**
 * O que a encomenda da loja online mostra a mais no desktop (docs/LOJA_ONLINE_SPEC.md §3.5, LO-UI-03): a marca
 * "Loja online" na coluna "Origem" da lista e, no detalhe, o contacto, a entrega e a morada. Os rótulos vêm do
 * servidor ({@code OrderDTO.originLabel}, {@code fulfilmentLabel}); o desktop não os traduz.
 */
public final class OnlineShopOrderInfo {

    public static final String ONLINE_ORIGIN = "LOJA_ONLINE";
    static final String ONLINE_LABEL = "Loja online";

    private OnlineShopOrderInfo() {}

    public static boolean isOnlineShop(OrderDTO order) {
        return order != null && ONLINE_ORIGIN.equals(order.origin());
    }

    /**
     * Texto da coluna "Origem": "Loja online" na encomenda da loja; nas outras mantém-se a origem comercial
     * ("Directa" ou "Cotação {número}").
     */
    public static String originColumn(OrderDTO order) {
        if (isOnlineShop(order)) {
            return order.originLabel() == null || order.originLabel().isBlank() ? ONLINE_LABEL : order.originLabel();
        }
        return order.quotationNumber() == null ? "Directa" : "Cotação " + order.quotationNumber();
    }

    /** Linhas do detalhe: "Contacto", "Entrega" e, só na entrega, "Morada"; vazio fora da loja online. */
    public static List<RecordDetailsDialog.PropertyItem> details(OrderDTO order) {
        List<RecordDetailsDialog.PropertyItem> items = new ArrayList<>();
        if (!isOnlineShop(order)) return items;
        addIfPresent(items, "Contacto", order.contactPhone());
        addIfPresent(items, "Entrega", order.fulfilmentLabel());
        addIfPresent(items, "Morada", order.deliveryAddress());
        return items;
    }

    private static void addIfPresent(List<RecordDetailsDialog.PropertyItem> items, String label, String value) {
        if (value != null && !value.isBlank()) items.add(new RecordDetailsDialog.PropertyItem(label, value));
    }
}
