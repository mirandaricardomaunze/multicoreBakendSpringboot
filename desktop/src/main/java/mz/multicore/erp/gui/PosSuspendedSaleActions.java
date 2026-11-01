package mz.multicore.erp.gui;

import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.gui.components.FeedbackType;
import mz.multicore.erp.gui.components.ToastManager;
import mz.multicore.erp.gui.components.UIHelper;
import mz.multicore.erp.gui.pos.suspended.PosSuspendedSale;
import mz.multicore.erp.gui.pos.suspended.PosSuspendedSaleStore;
import mz.multicore.erp.modules.comercial.dto.ClientDTO;
import mz.multicore.erp.modules.comercial.dto.ProductDTO;

import javax.swing.*;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/** Suspende e retoma atendimentos para o caixa poder servir o cliente seguinte. */
final class PosSuspendedSaleActions {

    private final POSPanel owner;
    private final PosSuspendedSaleStore store;

    PosSuspendedSaleActions(POSPanel owner) {
        this(owner, new PosSuspendedSaleStore());
    }

    PosSuspendedSaleActions(POSPanel owner, PosSuspendedSaleStore store) {
        this.owner = owner;
        this.store = store;
    }

    void suspendCurrentSale() {
        if (owner.cartItems.isEmpty()) {
            owner.showPosNotice(FeedbackType.WARNING, "Carrinho vazio", "Não há nenhuma venda para suspender.");
            return;
        }
        try {
            store.add(capture());
        } catch (UncheckedIOException e) {
            owner.showPosNotice(FeedbackType.ERROR, "Venda não suspensa", e.getMessage());
            return;
        }
        owner.clearCart();
        refreshButton();
        ToastManager.success(owner, "Venda suspensa. Retome-a com Ctrl+R.");
    }

    void openResumeDialog() {
        if (!owner.cartItems.isEmpty()) {
            owner.showPosNotice(FeedbackType.WARNING, "Carrinho em uso",
                    "Finalize, suspenda ou limpe a venda actual antes de retomar outra.");
            return;
        }
        List<PosSuspendedSale> sales = store.forCompany(CurrentUserContext.findCurrentCompanyId());
        if (sales.isEmpty()) {
            owner.showPosNotice(FeedbackType.INFO, "Sem vendas em espera", "Não há vendas suspensas neste posto.");
            return;
        }
        PosSuspendedSale selected = PosSuspendedSalesDialog.pick(sales, this::discard);
        if (selected != null) resume(selected);
    }

    private void discard(PosSuspendedSale sale) {
        store.remove(sale.id());
        refreshButton();
    }

    private void resume(PosSuspendedSale sale) {
        UIHelper.loadAsync(owner, owner.comercialApiClient::getAllProducts, products -> apply(sale, products),
                error -> owner.showPosNotice(FeedbackType.ERROR, "Não foi possível retomar a venda", error.getMessage()));
    }

    /** A venda só sai da espera depois de voltar ao carrinho. */
    void apply(PosSuspendedSale sale, List<ProductDTO> products) {
        if (!owner.cartItems.isEmpty()) {
            owner.showPosNotice(FeedbackType.WARNING, "Carrinho em uso", "A venda continua em espera.");
            return;
        }
        Map<Long, ProductDTO> byId = products.stream().collect(Collectors.toMap(ProductDTO::id, Function.identity(), (a, b) -> a));
        List<String> unavailable = new ArrayList<>();
        List<POSPanel.CartItem> items = new ArrayList<>();
        for (PosSuspendedSale.Line line : sale.lines()) {
            ProductDTO product = byId.get(line.productId());
            if (product == null) {
                unavailable.add(line.productName());
                continue;
            }
            POSPanel.CartItem item = new POSPanel.CartItem(product, line.quantity(), line.discount(),
                    line.batch(), line.serial(), line.customUnitPrice());
            item.note = line.note();
            items.add(item);
        }
        if (items.isEmpty()) {
            owner.showPosNotice(FeedbackType.WARNING, "Venda não retomada",
                    "Nenhum dos artigos existe no catálogo. A venda continua em espera.");
            return;
        }
        restoreClient(sale);
        restoreWarehouse(sale.warehouseId());
        owner.cartItems.addAll(items);
        owner.currentQuotationId = sale.quotationId();
        owner.currentQuotationNumber = sale.quotationNumber();
        owner.updateQuotationBanner();
        owner.updateCartTotal(-1);
        store.remove(sale.id());
        refreshButton();
        if (unavailable.isEmpty()) {
            ToastManager.success(owner, "Venda retomada.");
        } else {
            owner.showPosNotice(FeedbackType.WARNING, "Artigos fora da venda",
                    "Já não existem no catálogo: " + String.join(", ", unavailable) + ".");
        }
    }

    void refreshButton() {
        if (owner.suspendedSalesBtn == null) return;
        int count = store.forCompany(CurrentUserContext.findCurrentCompanyId()).size();
        owner.suspendedSalesBtn.setVisible(count > 0);
        owner.suspendedSalesBtn.setText("Em espera (" + count + ")");
    }

    private PosSuspendedSale capture() {
        int clientIndex = owner.clientCombo.getSelectedIndex();
        ClientDTO client = clientIndex >= 0 && clientIndex < owner.filteredClients.size()
                ? owner.filteredClients.get(clientIndex) : null;
        String clientName = client != null ? client.name() : owner.clientSearchField.getText().trim();
        List<PosSuspendedSale.Line> lines = owner.cartItems.stream()
                .map(item -> new PosSuspendedSale.Line(item.product.id(), item.product.name(), item.qty, item.discount,
                        item.batch, item.serial, item.note, item.customUnitPrice))
                .toList();
        BigDecimal total = owner.cartItems.stream().map(POSPanel.CartItem::getTotal).reduce(BigDecimal.ZERO, BigDecimal::add);
        return new PosSuspendedSale(UUID.randomUUID().toString(), CurrentUserContext.findCurrentCompanyId(),
                CurrentUserContext.getUsername(), LocalDateTime.now(), client != null ? client.id() : null, clientName,
                owner.getSelectedWarehouseId(), owner.currentQuotationId, owner.currentQuotationNumber, total, lines);
    }

    private void restoreClient(PosSuspendedSale sale) {
        if (sale.clientId() != null) {
            for (int i = 0; i < owner.filteredClients.size(); i++) {
                if (sale.clientId().equals(owner.filteredClients.get(i).id())) {
                    owner.clientCombo.setSelectedIndex(i);
                    return;
                }
            }
        }
        if (sale.clientName() != null && !sale.clientName().isBlank()) {
            owner.clientSearchField.setText(sale.clientName());
        }
    }

    private void restoreWarehouse(Long warehouseId) {
        for (int i = 0; i < owner.warehousesList.size(); i++) {
            if (owner.warehousesList.get(i).id().equals(warehouseId)) {
                owner.warehouseCombo.setSelectedIndex(i);
                return;
            }
        }
    }
}
