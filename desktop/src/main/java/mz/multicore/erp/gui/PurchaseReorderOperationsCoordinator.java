package mz.multicore.erp.gui;

import mz.multicore.erp.gui.components.QuickPeekPanel;
import mz.multicore.erp.gui.components.UIHelper;
import mz.multicore.erp.modules.purchases.dto.ReorderSuggestionDTO;

import javax.swing.*;
import java.awt.Color;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Coordenador de operações do Quick Peek e geração de encomendas de reposição. */
public final class PurchaseReorderOperationsCoordinator {

    private PurchaseReorderOperationsCoordinator() {}

    public static void populatePeek(QuickPeekPanel peek, List<ReorderSuggestionDTO> reorderList, int modelRow, Runnable onOpenFull) {
        if (reorderList == null || modelRow < 0 || modelRow >= reorderList.size()) return;
        ReorderSuggestionDTO s = reorderList.get(modelRow);
        String status = s.urgencyStatus() != null ? s.urgencyStatus()
                : (s.currentStock().signum() <= 0 ? "ESGOTADO" : "BAIXO");
        Color statusColor = "ESGOTADO".equalsIgnoreCase(status) ? UIHelper.REJECTED_RED
                : (status.toUpperCase(Locale.ROOT).startsWith("CR") ? UIHelper.PENDING_YELLOW : UIHelper.ACCENT_BLUE);

        peek.setHeaderIcon("fas-cart-plus", UIHelper.ACCENT_BLUE);
        peek.setTitle(s.name());
        peek.setSubtitle("SKU " + (s.sku() != null ? s.sku() : "—"));
        peek.setStatus(status, statusColor);

        String supplier = (s.supplierName() != null && !s.supplierName().isBlank()) ? s.supplierName() : "—";
        BigDecimal rate = s.dailySalesRate() != null ? s.dailySalesRate() : BigDecimal.ZERO;
        BigDecimal unitPrice = s.estimatedUnitPrice() != null ? s.estimatedUnitPrice() : BigDecimal.ZERO;
        BigDecimal totalCost = s.estimatedTotalCost() != null ? s.estimatedTotalCost() : BigDecimal.ZERO;

        List<QuickPeekPanel.PeekItem> items = new ArrayList<>();
        if (s.businessUnitName() != null && !s.businessUnitName().isBlank()) {
            items.add(new QuickPeekPanel.PeekItem("Linha de Negócio", s.businessUnitName(), false));
        }
        items.add(new QuickPeekPanel.PeekItem("Fornecedor habitual", supplier, false));
        items.add(new QuickPeekPanel.PeekItem("Stock actual / mínimo",
                String.format(Locale.ROOT, "%,.2f / %,.2f", s.currentStock(), s.minStock()), false));
        items.add(new QuickPeekPanel.PeekItem("Venda média diária",
                String.format(Locale.ROOT, "%,.2f /dia", rate), false));
        items.add(new QuickPeekPanel.PeekItem("Cobertura restante",
                s.daysRemaining() != null ? s.daysRemaining() + (s.daysRemaining() == 1 ? " dia" : " dias") : "—", false));
        items.add(new QuickPeekPanel.PeekItem("Embalagem",
                s.unitsPerBox() + " und/cx — sugerido " + String.format(Locale.ROOT, "%,.0f cx (%,.0f und)",
                s.suggestedBoxes(), s.suggestedUnits()), false));
        items.add(new QuickPeekPanel.PeekItem("Preço unitário estimado",
                String.format(Locale.ROOT, "%,.2f MT", unitPrice), false));
        items.add(new QuickPeekPanel.PeekItem("Custo total previsto",
                String.format(Locale.ROOT, "%,.2f MT", totalCost), true));
        peek.setItems(items);
        peek.setOnOpenFullAction(ignored -> {
            if (onOpenFull != null) onOpenFull.run();
        });
    }

    public static void orderSelectedOrOpen(JTable reorderTable, List<ReorderSuggestionDTO> reorderList, PurchaseOrdersPanel purchaseOrdersPanel) {
        if (purchaseOrdersPanel == null) return;
        int viewRow = reorderTable != null ? reorderTable.getSelectedRow() : -1;
        if (viewRow >= 0 && reorderList != null) {
            int modelRow = reorderTable.convertRowIndexToModel(viewRow);
            if (modelRow >= 0 && modelRow < reorderList.size()) {
                ReorderSuggestionDTO sel = reorderList.get(modelRow);
                purchaseOrdersPanel.openPurchaseOrderForSuggestion(
                        sel.supplierId(),
                        sel.productId(),
                        sel.suggestedUnits(),
                        sel.estimatedUnitPrice()
                );
                return;
            }
        }
        purchaseOrdersPanel.openPurchaseOrderFormDialog();
    }
}
