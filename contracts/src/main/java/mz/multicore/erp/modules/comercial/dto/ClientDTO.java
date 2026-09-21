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
    String code
) {
    public ClientDTO(Long id, String name, String taxId, String email, String address, int paymentTermsDays, BigDecimal creditLimit) {
        this(id, name, taxId, email, address, paymentTermsDays, creditLimit, BigDecimal.ZERO, null);
    }

    public String nuit() { return taxId; }
}
