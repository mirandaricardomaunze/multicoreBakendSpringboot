package com.phcpro.modules.fiscal.dto;

import java.math.BigDecimal;

/**
 * Resultado da exportação fiscal de vendas (estrutura SAF-T).
 * {@code xml} é o ficheiro completo; os totais permitem conferir o conteúdo sem voltar a parsear.
 */
public record FiscalSalesExportDTO(
        String xml,
        int numberOfInvoices,
        BigDecimal totalNet,
        BigDecimal totalTax,
        BigDecimal totalGross
) {
}
