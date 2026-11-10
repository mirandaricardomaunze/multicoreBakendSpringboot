package mz.multicore.erp.modules.accounting.service;

import java.math.BigDecimal;

/**
 * Tells accounting which products the company makes itself. PGC-NIRF keeps them in 3401 Produtos acabados, apart
 * from the goods bought for resale (3201 Mercadorias). Implemented by the manufacturing module, which owns the
 * technical sheets (BOM); accounting stays unaware of how a product is made.
 */
public interface FinishedGoodsLookup {

    boolean isManufactured(Long companyId, Long productId);

    /** Cost of the manufactured products sold on the invoice (same basis as the sale's cost of goods). */
    BigDecimal finishedCostOfInvoice(Long companyId, Long invoiceId);

    /** Cost of the manufactured products returned on the credit note. */
    BigDecimal finishedCostOfCreditNote(Long companyId, Long noteId);
}
