package mz.multicore.erp.modules.pos.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;

public record POSCheckoutLineRequest(
        @NotNull(message = "Produto é obrigatório.") Long productId,
        @NotNull(message = "Quantidade é obrigatória.")
        @Positive(message = "Quantidade deve ser positiva.") BigDecimal quantity,
        @PositiveOrZero(message = "Desconto não pode ser negativo.") BigDecimal discountPercentage,
        String batchNumber,
        String serialNumber,
        /** Preço unitário personalizado/cotado (opcional). Se nulo, aplica tabela/grosso do catálogo. */
        BigDecimal unitPrice
) {
    public POSCheckoutLineRequest(Long productId, BigDecimal quantity, BigDecimal discountPercentage,
                                  String batchNumber, String serialNumber) {
        this(productId, quantity, discountPercentage, batchNumber, serialNumber, null);
    }

    public POSCheckoutLineRequest(Long productId, Integer quantity, BigDecimal discountPercentage,
                                  String batchNumber, String serialNumber) {
        this(productId, BigDecimal.valueOf(quantity), discountPercentage, batchNumber, serialNumber, null);
    }
}
