package mz.multicore.erp.modules.purchases.service;

import mz.multicore.erp.modules.purchases.model.Purchase;
import mz.multicore.erp.modules.purchases.model.PurchaseLine;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Unidade de negócio de uma compra, tirada dos artigos das linhas: a única, "Misto" quando há várias (sem id), ou
 * nenhuma. Partilhada pelas contas a pagar e pelo extracto do fornecedor.
 */
record PurchaseBusinessUnit(Long id, String name) {

    private static final PurchaseBusinessUnit NONE = new PurchaseBusinessUnit(null, null);

    static PurchaseBusinessUnit of(Purchase purchase) {
        if (purchase.getLines() == null) return NONE;
        Map<String, Long> idsByName = new LinkedHashMap<>();
        for (PurchaseLine line : purchase.getLines()) {
            if (line.getProduct() == null || line.getProduct().getBusinessUnit() == null) continue;
            String name = line.getProduct().getBusinessUnit().getName();
            if (name != null && idsByName.get(name) == null) {
                idsByName.put(name, line.getProduct().getBusinessUnit().getId());
            }
        }
        if (idsByName.isEmpty()) return NONE;
        if (idsByName.size() > 1) return new PurchaseBusinessUnit(null, "Misto");
        Map.Entry<String, Long> only = idsByName.entrySet().iterator().next();
        return new PurchaseBusinessUnit(only.getValue(), only.getKey());
    }
}
