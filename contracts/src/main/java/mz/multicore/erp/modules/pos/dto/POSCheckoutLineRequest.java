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
        BigDecimal unitPrice,
        /**
         * For products sold by weight: "SCALE" when the quantity is the one read from the scale (capture button or
         * scale label), "MANUAL" when it was typed. {@code null} for other products and for older clients.
         */
        String weightSource
) {
    public static final String WEIGHT_FROM_SCALE = "SCALE";
    public static final String WEIGHT_TYPED = "MANUAL";

    public POSCheckoutLineRequest(Long productId, BigDecimal quantity, BigDecimal discountPercentage,
                                  String batchNumber, String serialNumber, BigDecimal unitPrice) {
        this(productId, quantity, discountPercentage, batchNumber, serialNumber, unitPrice, null);
    }

    public POSCheckoutLineRequest(Long productId, BigDecimal quantity, BigDecimal discountPercentage,
                                  String batchNumber, String serialNumber) {
        this(productId, quantity, discountPercentage, batchNumber, serialNumber, null);
    }

    public POSCheckoutLineRequest(Long productId, Integer quantity, BigDecimal discountPercentage,
                                  String batchNumber, String serialNumber) {
        this(productId, BigDecimal.valueOf(quantity), discountPercentage, batchNumber, serialNumber, null);
    }
}
