package mz.multicore.erp.gui;

import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.gui.components.FeedbackType;
import mz.multicore.erp.gui.components.ModernMessageDialog;
import mz.multicore.erp.gui.components.UIHelper;
import mz.multicore.erp.modules.comercial.dto.ProductDTO;
import mz.multicore.erp.modules.inventory.dto.StockDTO;
import mz.multicore.erp.modules.inventory.dto.WarehouseDTO;

import javax.swing.*;
import java.awt.*;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Auxiliar para cálculo e sugestão inteligente de reposição de stock entre armazéns.
 */
final class StockTransferReplenishmentHelper {

    record ReplenishmentItem(ProductDTO product, BigDecimal qtyNeeded, BigDecimal qtyOrigin) {}

    private StockTransferReplenishmentHelper() {}

    static void suggest(StockPanel owner, Component parent, List<WarehouseDTO> warehouses,
                        List<ProductDTO> products, int originIdx, int destIdx,
                        Consumer<List<ReplenishmentItem>> onApply) {
        if (originIdx < 0 || destIdx < 0 || originIdx >= warehouses.size() || destIdx >= warehouses.size()) {
            owner.showStockNotice(FeedbackType.WARNING, "Armazéns necessários",
                    "Seleccione os armazéns de origem e de destino antes de pedir a sugestão.");
            return;
        }
        if (originIdx == destIdx) {
            owner.showStockNotice(FeedbackType.WARNING, "Armazéns iguais",
                    "O armazém de origem e o de destino devem ser diferentes.");
            return;
        }

        WarehouseDTO origin = warehouses.get(originIdx);
        WarehouseDTO dest = warehouses.get(destIdx);

        UIHelper.loadAsync(owner, () -> owner.inventoryApiClient.getStocksByCompany(CurrentUserContext.getCurrentCompanyId()), loadedStocks -> {
            applySuggestions(owner, parent, origin, dest, products, loadedStocks, onApply);
        }, owner::showStockError);
    }

    private static void applySuggestions(StockPanel owner, Component parent, WarehouseDTO origin,
                                         WarehouseDTO dest, List<ProductDTO> products,
                                         List<StockDTO> loadedStocks,
                                         Consumer<List<ReplenishmentItem>> onApply) {
        if (loadedStocks == null || loadedStocks.isEmpty()) {
            owner.showStockNotice(FeedbackType.INFO, "Sem dados de stock",
                    "Não foram encontrados dados de saldo para calcular a reposição.");
            return;
        }

        Map<Long, BigDecimal> destStock = new HashMap<>();
        Map<Long, BigDecimal> destMinStock = new HashMap<>();
        Map<Long, BigDecimal> originStock = new HashMap<>();

        for (StockDTO s : loadedStocks) {
            if (dest.id().equals(s.warehouseId())) {
                destStock.put(s.productId(), s.quantity() != null ? s.quantity() : BigDecimal.ZERO);
                if (s.minStock() != null && s.minStock().compareTo(BigDecimal.ZERO) > 0) {
                    destMinStock.put(s.productId(), s.minStock());
                }
            } else if (origin.id().equals(s.warehouseId())) {
                originStock.put(s.productId(), s.quantity() != null ? s.quantity() : BigDecimal.ZERO);
            }
        }

        List<ReplenishmentItem> suggestions = new ArrayList<>();
        for (ProductDTO product : products) {
            BigDecimal currentDest = destStock.getOrDefault(product.id(), BigDecimal.ZERO);
            BigDecimal min = destMinStock.get(product.id());

            BigDecimal needed = BigDecimal.ZERO;
            if (min != null && currentDest.compareTo(min) < 0) {
                needed = min.subtract(currentDest);
            } else if (currentDest.compareTo(BigDecimal.ZERO) <= 0) {
                int unitsPerBox = product.packagesPerBox() > 0 && product.unitsPerPackage() > 0
                        ? product.packagesPerBox() * product.unitsPerPackage() : 1;
                needed = BigDecimal.valueOf(unitsPerBox);
            }

            if (needed.compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal availableOrigin = originStock.getOrDefault(product.id(), BigDecimal.ZERO);
                if (availableOrigin.compareTo(BigDecimal.ZERO) > 0) {
                    BigDecimal transferQty = needed.min(availableOrigin);
                    suggestions.add(new ReplenishmentItem(product, transferQty, availableOrigin));
                }
            }
        }

        if (suggestions.isEmpty()) {
            owner.showStockNotice(FeedbackType.INFO, "Reposição desnecessária",
                    "Nenhum produto em falta no armazém '" + dest.name() + "' tem saldo disponível em '" + origin.name() + "'.");
            return;
        }

        Window ownerWin = parent instanceof Window win ? win : SwingUtilities.getWindowAncestor(parent);
        boolean confirm = ModernMessageDialog.confirm(
                ownerWin,
                FeedbackType.INFO,
                "Sugestão de Reposição Inteligente",
                "Foram identificados " + suggestions.size() + " produto(s) com necessidade de reposição no armazém '" + dest.name() + "'.\n\n"
                        + "Deseja carregar estes artigos automaticamente na grelha da transferência?",
                "Carregar Itens"
        );
        if (!confirm) return;

        if (onApply != null) onApply.accept(suggestions);
        owner.showStockSuccess(suggestions.size() + " artigo(s) carregados por sugestão de reposição.");
    }
}
