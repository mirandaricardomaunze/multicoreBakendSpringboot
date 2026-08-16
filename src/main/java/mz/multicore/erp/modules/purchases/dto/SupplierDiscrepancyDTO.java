package mz.multicore.erp.modules.purchases.dto;

import java.math.BigDecimal;

/**
 * Divergências acumuladas de um fornecedor num período.
 *
 * <p>É a linha que responde à pergunta que ninguém faz até ser tarde: <i>"quanto é que este
 * fornecedor me custou em mercadoria que paguei e não recebi?"</i>
 *
 * @param damagedAmount valor da mercadoria que chegou estragada
 * @param missingAmount valor da que nunca chegou
 * @param totalAmount   o que há a reclamar no total
 * @param openAmount    a parte que ainda não foi resolvida com o fornecedor
 */
public record SupplierDiscrepancyDTO(
        Long supplierId,
        String supplierName,
        int occurrences,
        BigDecimal damagedQuantity,
        BigDecimal missingQuantity,
        BigDecimal damagedAmount,
        BigDecimal missingAmount,
        BigDecimal totalAmount,
        BigDecimal openAmount
) {}
