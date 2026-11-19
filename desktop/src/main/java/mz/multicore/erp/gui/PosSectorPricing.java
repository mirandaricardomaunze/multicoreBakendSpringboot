package mz.multicore.erp.gui;

import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.desktop.client.PricingApiClient;
import mz.multicore.erp.desktop.client.UnitsApiClient;
import mz.multicore.erp.gui.components.FeedbackType;
import mz.multicore.erp.gui.sector.ActivityCapabilities;
import mz.multicore.erp.modules.comercial.dto.ProductDTO;
import mz.multicore.erp.modules.units.dto.UnitBarcodeItemDTO;

import javax.swing.*;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Business-sector pricing and units at the POS (SA-C02, SA-C03), kept out of {@link POSPanel}, which is at its line
 * ceiling:
 * <ul>
 *   <li>the cart shows the unit price the server will charge: the client's price list, or the wholesale tier. The
 *       desktop still sends no price, so the server stays the single source and the receipt matches the screen;</li>
 *   <li>the barcode of a sellable alternative unit (a pallet) adds its base quantity (40 bags).</li>
 * </ul>
 */
final class PosSectorPricing {

    static final String KEY = "multicore.pos.sectorPricing";

    private final POSPanel owner;
    private final UnitsApiClient units;
    private final PricingApiClient pricing;
    private final Map<Long, Boolean> clientHasList = new ConcurrentHashMap<>();
    /** Client and quantity each line was priced for; touched only on the EDT. */
    private final Map<POSPanel.CartItem, String> pricedFor = new IdentityHashMap<>();
    private boolean refreshQueued;

    private PosSectorPricing(POSPanel owner, UnitsApiClient units, PricingApiClient pricing) {
        this.owner = owner;
        this.units = units;
        this.pricing = pricing;
    }

    static void install(POSPanel pos, UnitsApiClient units, PricingApiClient pricing) {
        if (pos == null || (units == null && pricing == null)) return;
        PosSectorPricing sector = new PosSectorPricing(pos, units, pricing);
        pos.putClientProperty(KEY, sector);
        if (pricing == null) return;
        if (pos.clientCombo != null) pos.clientCombo.addActionListener(e -> sector.queueRefresh());
        if (pos.cartModel != null) pos.cartModel.addTableModelListener(e -> sector.queueRefresh());
    }

    /** Tries the barcode as an alternative unit; runs {@code notFound} when it is not one (or units are off). */
    static void addByUnitBarcode(POSPanel pos, String code, Long warehouseId, Runnable notFound) {
        PosSectorPricing sector = pos.getClientProperty(KEY) instanceof PosSectorPricing s ? s : null;
        Long companyId = CurrentUserContext.findCurrentCompanyId();
        if (sector == null || sector.units == null || companyId == null
                || !ActivityCapabilities.enabled(ActivityCapabilities.MULTI_UNIT_SALES)) {
            notFound.run();
            return;
        }
        mz.multicore.erp.gui.components.UIHelper.loadAsync(pos, () -> sector.units.findByBarcode(companyId, code, warehouseId),
                match -> {
                    if (match == null || match.item() == null) notFound.run();
                    else sector.addUnits(match);
                }, error -> notFound.run());
    }

    private void addUnits(UnitBarcodeItemDTO match) {
        ProductDTO product = match.item().product();
        if (match.item().sellable()) owner.registerSellableProduct(product);
        if (!owner.isProductSellable(product)) {
            owner.showPosNotice(FeedbackType.WARNING, "Sem stock", "O artigo '" + product.name() + "' está esgotado.");
            clearBarcode();
            return;
        }
        if (owner.activeSession == null) {
            owner.showPosNotice(FeedbackType.WARNING, "Caixa fechado", "Abra o caixa antes de adicionar artigos.");
            return;
        }
        BigDecimal qty = match.factorToBase();
        String note = "1 " + match.unitName().toLowerCase() + " = " + qty.stripTrailingZeros().toPlainString();
        clearBarcode();
        for (POSPanel.CartItem it : owner.cartItems) {
            if (it.serial == null && it.product.id().equals(product.id())) {
                it.qty = it.qty.add(qty);
                owner.updateCartTotal(owner.cartItems.indexOf(it));
                return;
            }
        }
        Long companyId = CurrentUserContext.getCurrentCompanyId();
        mz.multicore.erp.gui.components.UIHelper.loadAsync(owner,
                () -> owner.promotionApiClient.bestPromotion(companyId, product.id(), product.categoryId(), qty), promo -> {
                    POSPanel.CartItem item = new POSPanel.CartItem(product, qty,
                            promo.map(p -> p.discountPercent()).orElse(BigDecimal.ZERO), null, null);
                    item.note = promo.map(p -> "Promo: " + p.name()).orElse(note);
                    owner.cartItems.add(item);
                    owner.updateCartTotal(owner.cartItems.size() - 1);
                }, error -> owner.showPosNotice(FeedbackType.ERROR, "Não foi possível consultar promoções", error.getMessage()));
    }

    private void clearBarcode() {
        owner.barcodeField.setText("");
        owner.barcodeField.requestFocusInWindow();
    }

    // ─── Cart prices ────────────────────────────────────────────────────────────

    private void queueRefresh() {
        if (refreshQueued) return;
        refreshQueued = true;
        SwingUtilities.invokeLater(this::refreshPrices);
    }

    private record Pending(POSPanel.CartItem item, Long productId, BigDecimal qty, boolean wholesale, String key) {}

    private record Priced(Pending line, BigDecimal unitPrice) {}

    private void refreshPrices() {
        refreshQueued = false;
        Long companyId = CurrentUserContext.findCurrentCompanyId();
        if (companyId == null) return;
        Long clientId = selectedClientId();
        pricedFor.keySet().retainAll(new ArrayList<>(owner.cartItems));
        List<Pending> pending = new ArrayList<>();
        for (POSPanel.CartItem item : owner.cartItems) {
            if (item.product == null || item.customUnitPrice != null || item.qty == null) continue;
            String key = clientId + "|" + item.qty.stripTrailingZeros().toPlainString();
            if (key.equals(pricedFor.get(item))) continue;
            BigDecimal wholesale = item.product.wholesalePrice();
            pending.add(new Pending(item, item.product.id(), item.qty, wholesale != null && wholesale.signum() > 0, key));
        }
        if (pending.isEmpty()) return;
        new SwingWorker<List<Priced>, Void>() {
            @Override protected List<Priced> doInBackground() {
                boolean hasList = clientId != null && clientHasList.computeIfAbsent(clientId, id -> {
                    var assignment = pricing.clientAssignment(companyId, id);
                    return assignment != null && assignment.priceListId() != null;
                });
                List<Priced> result = new ArrayList<>();
                for (Pending line : pending) {
                    BigDecimal price = hasList || line.wholesale()
                            ? pricing.resolve(companyId, clientId, line.productId(), line.qty()).unitPrice() : null;
                    result.add(new Priced(line, price));
                }
                return result;
            }

            @Override protected void done() {
                try {
                    apply(get());
                } catch (Exception ignored) {
                    // Offline or refused: the cart keeps the normal price and the server still decides the charge.
                }
            }
        }.execute();
    }

    private void apply(List<Priced> priced) {
        boolean changed = false;
        for (Priced p : priced) {
            POSPanel.CartItem item = p.line().item();
            if (!owner.cartItems.contains(item) || item.qty.compareTo(p.line().qty()) != 0) continue;
            pricedFor.put(item, p.line().key());
            if (!samePrice(item.listUnitPrice, p.unitPrice())) {
                item.listUnitPrice = p.unitPrice();
                changed = true;
            }
        }
        if (changed) owner.updateCartTotal();
    }

    private static boolean samePrice(BigDecimal a, BigDecimal b) {
        return a == null ? b == null : b != null && a.compareTo(b) == 0;
    }

    private Long selectedClientId() {
        int index = owner.clientCombo == null ? -1 : owner.clientCombo.getSelectedIndex();
        return index >= 0 && index < owner.filteredClients.size() ? owner.filteredClients.get(index).id() : null;
    }
}
