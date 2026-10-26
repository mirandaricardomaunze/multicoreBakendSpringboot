package mz.multicore.erp.modules.inventory.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record CreateStockAdjustmentRequest(
        @NotNull(message = "Empresa é obrigatória.") @Positive Long companyId,
        @NotNull(message = "Produto é obrigatório.") @Positive Long productId,
        @NotNull(message = "Armazém é obrigatório.") @Positive Long warehouseId,
        @NotNull(message = "Quantidade contada é obrigatória.")
        @PositiveOrZero(message = "Quantidade contada não pode ser negativa.")
        @Digits(integer = 9, fraction = 3) BigDecimal countedQuantity,
        @NotBlank(message = "Motivo do ajuste é obrigatório.") @Size(max = 160) String reason,
        @Size(max = 60, message = "A chave de idempotência não pode exceder 60 caracteres.")
        @Pattern(regexp = "[A-Za-z0-9._:-]+") String idempotencyKey
) {
    public CreateStockAdjustmentRequest {
        reason = reason == null ? null : reason.trim();
        idempotencyKey = idempotencyKey == null || idempotencyKey.isBlank() ? null : idempotencyKey.trim();
    }

    public CreateStockAdjustmentRequest(Long companyId, Long productId, Long warehouseId,
                                        BigDecimal countedQuantity, String reason) {
        this(companyId, productId, warehouseId, countedQuantity, reason, null);
    }
}
