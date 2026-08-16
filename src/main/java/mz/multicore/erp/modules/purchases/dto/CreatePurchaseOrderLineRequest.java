package mz.multicore.erp.modules.purchases.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;

public record CreatePurchaseOrderLineRequest(
        @NotNull(message = "Produto é obrigatório.") Long productId,
        @NotNull(message = "Quantidade é obrigatória.")
        @DecimalMin(value = "0.001", message = "Quantidade deve ser positiva.") BigDecimal quantity,
        @NotNull(message = "Preço unitário é obrigatório.")
        @DecimalMin(value = "0.00", message = "Preço não pode ser negativo.") BigDecimal unitPrice,
        String batchNumber,
        LocalDate expirationDate,
        String serialNumber,
        /** Taxa de IVA acordada com o fornecedor. Vazio ⇒ taxa do artigo. */
        @DecimalMin(value = "0.00", message = "Taxa de IVA não pode ser negativa.")
        @DecimalMax(value = "1.00", message = "Taxa de IVA é uma fracção (ex.: 0.16 para 16%).")
        BigDecimal taxRate
) {
    /** Retrocompatível: chamadas antigas sem taxa caem na taxa do artigo. */
    public CreatePurchaseOrderLineRequest(Long productId, BigDecimal quantity, BigDecimal unitPrice,
                                          String batchNumber, LocalDate expirationDate, String serialNumber) {
        this(productId, quantity, unitPrice, batchNumber, expirationDate, serialNumber, null);
    }
}
