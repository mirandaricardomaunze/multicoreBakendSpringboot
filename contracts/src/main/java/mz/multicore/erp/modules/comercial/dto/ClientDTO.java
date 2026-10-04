package mz.multicore.erp.modules.comercial.dto;

import java.math.BigDecimal;

/**
 * @param paymentTermsDays prazo de pagamento acordado, em dias (0 = pronto pagamento)
 * @param creditLimit      tecto de dívida em aberto; {@code null} = sem limite (crédito livre)
 * @param loyaltyPoints    pontos acumulados no programa de fidelização
 * @param code             código do cartão de fidelidade / referência interna
 */
public record ClientDTO(
    Long id,
    String name,
    String taxId,
    String email,
    String address,
    int paymentTermsDays,
    BigDecimal creditLimit,
    BigDecimal loyaltyPoints,
    String code,
    Long version
) {
    public ClientDTO(Long id, String name, String taxId, String email, String address, int paymentTermsDays, BigDecimal creditLimit, BigDecimal loyaltyPoints, String code) {
        this(id, name, taxId, email, address, paymentTermsDays, creditLimit, loyaltyPoints, code, 0L);
    }

    public ClientDTO(Long id, String name, String taxId, String email, String address, int paymentTermsDays, BigDecimal creditLimit) {
        this(id, name, taxId, email, address, paymentTermsDays, creditLimit, BigDecimal.ZERO, null, 0L);
    }

    public String nuit() { return taxId; }
}
