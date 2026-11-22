package mz.multicore.erp.modules.purchases.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Pedido de um documento "Saldo Inicial Fornecedor" (série SIF): uma factura de um fornecedor da empresa activa ainda
 * por pagar, trazida de outro ERP (IMPORTACAO_DADOS_SPEC §5.3). A chave é fornecedor + {@code originalNumber}.
 *
 * @param supplierId     fornecedor da empresa activa
 * @param originalNumber número da factura no sistema antigo
 * @param documentDate   data da factura antiga
 * @param dueDate        vencimento da factura antiga
 * @param amount         valor ainda em dívida
 * @param description    descrição livre, opcional
 */
public record CreateOpeningPayableRequest(
        Long supplierId,
        String originalNumber,
        LocalDate documentDate,
        LocalDate dueDate,
        BigDecimal amount,
        String description
) {
}
