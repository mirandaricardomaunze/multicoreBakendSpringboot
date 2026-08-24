package mz.multicore.erp.modules.hr.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.time.LocalTime;

public record CreateTimeEntryRequest(
        @NotNull(message = "O colaborador é obrigatório.") Long employeeId,
        @NotNull(message = "A data da marcação é obrigatória.") LocalDate entryDate,
        @NotNull(message = "A hora de entrada é obrigatória.") LocalTime checkIn,
        @NotNull(message = "A hora de saída é obrigatória.") LocalTime checkOut,
        @Min(value = 0, message = "A pausa não pode ser negativa.") int breakMinutes,
        /** MANUAL, IMPORTADO ou TERMINAL. Em branco assume MANUAL. */
        String source,
        @Size(max = 300, message = "A observação não pode exceder 300 caracteres.") String notes
) {}
