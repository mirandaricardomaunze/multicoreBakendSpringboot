package mz.multicore.erp.modules.purchases.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.List;

/** Devolução de mercadoria ao fornecedor: linhas da fatura de compra e quantidades que saem. */
public record CreateSupplierReturnRequest(
        @NotBlank(message = "Indique o motivo da devolução.")
        @Size(max = 300, message = "O motivo não pode exceder 300 caracteres.") String reason,
        @NotEmpty(message = "Indique pelo menos uma linha a devolver.") @Valid List<Line> lines
) {
    public record Line(
            @NotNull(message = "Linha da compra em falta.") Long purchaseLineId,
            @NotNull(message = "Indique a quantidade a devolver.")
            @Positive(message = "A quantidade a devolver tem de ser positiva.") BigDecimal quantity
    ) {}
}
