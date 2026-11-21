package mz.multicore.erp.modules.comercial.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Pedido de um documento "Saldo Inicial" (série SI): uma factura em aberto de um cliente da empresa activa, trazida
 * de outro ERP (IMPORTACAO_DADOS_SPEC §5.2). A chave é cliente + {@code originalNumber}.
 *
 * @param clientId       cliente ligado à empresa activa
 * @param originalNumber número da factura no sistema antigo
 * @param documentDate   data da factura antiga
 * @param dueDate        vencimento da factura antiga
 * @param amount         valor ainda em dívida
 * @param description    descrição livre, opcional
 */
public record CreateOpeningReceivableRequest(
        Long clientId,
        String originalNumber,
        LocalDate documentDate,
        LocalDate dueDate,
        BigDecimal amount,
        String description
) {
}
