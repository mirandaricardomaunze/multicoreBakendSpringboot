package mz.multicore.erp.modules.publicapi.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

/** One line of {@code POST /orders} (SPEC §3.4.1): the article and how many; the price always comes from the system. */
public record PublicOrderLineRequest(
        @NotBlank(message = "Indique o SKU do artigo em cada linha.") String sku,
        @NotNull(message = "Indique a quantidade em cada linha.")
        @Positive(message = "A quantidade de cada linha deve ser positiva.") BigDecimal quantity) {
}
