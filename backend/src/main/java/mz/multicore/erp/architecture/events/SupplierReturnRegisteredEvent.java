package mz.multicore.erp.architecture.events;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Goods went back to the supplier: the debt to the supplier, the goods account and the deductible VAT go down by the
 * value of the purchase lines returned (debit Fornecedores / credit Mercadorias and IVA dedutível).
 */
public record SupplierReturnRegisteredEvent(Long companyId, Long returnId, String returnNumber, LocalDate date,
                                            BigDecimal netAmount, BigDecimal taxAmount, BigDecimal totalAmount) {}
