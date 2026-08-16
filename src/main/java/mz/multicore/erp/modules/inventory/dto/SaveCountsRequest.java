package mz.multicore.erp.modules.inventory.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;
import java.util.List;

/** Contagens a guardar numa sessão DRAFT: uma entrada por artigo contado. */
public record SaveCountsRequest(
        @NotNull(message = "Contagens são obrigatórias.")
        @Valid List<CountEntry> counts
) {
    /** Contagem física de um artigo. */
    public record CountEntry(
            @NotNull(message = "Artigo é obrigatório.") Long productId,
            @NotNull(message = "Quantidade contada é obrigatória.")
            @PositiveOrZero(message = "Quantidade contada não pode ser negativa.") BigDecimal countedQuantity
    ) {}
}
