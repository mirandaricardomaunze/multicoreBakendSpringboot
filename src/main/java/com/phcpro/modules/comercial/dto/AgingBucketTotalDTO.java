package com.phcpro.modules.comercial.dto;

import com.phcpro.modules.comercial.model.AgingBucket;

import java.math.BigDecimal;

/**
 * Total em dívida num escalão de antiguidade.
 *
 * @param label rótulo em PT-MZ, resolvido no servidor para a UI não repetir a tradução
 */
public record AgingBucketTotalDTO(
        AgingBucket bucket,
        String label,
        BigDecimal amount,
        int invoiceCount
) {}
