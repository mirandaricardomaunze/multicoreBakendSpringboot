package mz.multicore.erp.modules.comercial.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;

/**
 * Linha pedida numa cotação. <b>Não tem preço unitário nem taxa de IVA</b>, e isso é deliberado:
 * o preço é apreçado pelo servidor a partir do artigo ({@code Product.effectiveUnitPrice} +
 * {@code effectiveTaxRate}), pelo que não existe campo por onde um cliente HTTP pudesse ditar
 * quanto custa — que foi o que o campo {@code taxRate} do {@code CreateInvoiceLineRequest} permitiu
 * até 2026-08-06. Sem campo, não há porta a fechar. Ver docs/COTACAO_SPEC.md §3.
 */
public record CreateQuotationLineRequest(
        @NotNull(message = "O ID do produto é obrigatório.")
        Long productId,

        @NotNull(message = "A quantidade é obrigatória.")
        @Positive(message = "A quantidade deve ser positiva.")
        BigDecimal quantity,

        @PositiveOrZero(message = "O desconto não pode ser negativo.")
        BigDecimal discountPercentage
) {
    public CreateQuotationLineRequest(Long productId, BigDecimal quantity) {
        this(productId, quantity, BigDecimal.ZERO);
    }
}
