package mz.multicore.erp.modules.accounting.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/** Lançamento manual (ajustes, amortizações, regularizações). */
public record CreateJournalEntryRequest(
        @NotNull(message = "A data do lançamento é obrigatória.")
        LocalDate entryDate,

        @NotBlank(message = "A descrição do lançamento é obrigatória.") @Size(max = 500)
        String description,

        @NotEmpty(message = "O lançamento tem de ter partidas.")
        @Valid
        List<Line> lines
) {

    /** Uma partida: conta + valor a débito <b>ou</b> a crédito. */
    public record Line(
            @NotBlank(message = "Indique a conta da partida.") @Size(max = 50)
            String accountCode,
            @PositiveOrZero BigDecimal debit,
            @PositiveOrZero BigDecimal credit,
            @Size(max = 500) String description
    ) {}
}
