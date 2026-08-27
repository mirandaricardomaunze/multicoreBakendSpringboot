package mz.multicore.erp.modules.accounting.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/** Lançamento manual (ajustes, amortizações, regularizações). */
public record CreateJournalEntryRequest(
        @NotNull(message = "A data do lançamento é obrigatória.")
        LocalDate entryDate,

        @NotBlank(message = "A descrição do lançamento é obrigatória.")
        String description,

        @NotEmpty(message = "O lançamento tem de ter partidas.")
        @Valid
        List<Line> lines
) {

    /** Uma partida: conta + valor a débito <b>ou</b> a crédito. */
    public record Line(
            @NotBlank(message = "Indique a conta da partida.")
            String accountCode,
            BigDecimal debit,
            BigDecimal credit,
            String description
    ) {}
}
