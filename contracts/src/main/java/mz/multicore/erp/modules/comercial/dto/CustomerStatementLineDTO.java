package mz.multicore.erp.modules.comercial.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Movimento mercantil ou financeiro individual no extrato de conta corrente do cliente.
 */
public record CustomerStatementLineDTO(
        LocalDate date,
        String documentType,      // FT, RC, NC, ND
        String documentNumber,    // Ex: FT-2026/001, RC-2026/010
        String description,       // Ex: Fatura a Crédito, Recibo Parcial
        String reference,         // Ex: Referência a documento original
        BigDecimal debit,         // Aumenta dívida do cliente (FT, ND)
        BigDecimal credit,        // Reduz dívida do cliente (RC, NC)
        BigDecimal runningBalance // Saldo progressivo acumulado após este lançamento
) {}
