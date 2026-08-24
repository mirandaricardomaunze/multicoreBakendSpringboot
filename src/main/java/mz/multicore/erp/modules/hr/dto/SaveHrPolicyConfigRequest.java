package mz.multicore.erp.modules.hr.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

/**
 * Todos os valores são <b>opcionais</b> de propósito: uma empresa pode confirmar os prazos de
 * entrega com o contabilista esta semana e o direito a férias só no mês que vem. Obrigar a preencher
 * tudo de uma vez levaria a preencher à sorte, que é exactamente o que esta configuração existe
 * para evitar.
 */
public record SaveHrPolicyConfigRequest(
        @NotBlank(message = "Dê um nome a esta configuração.") String name,
        @NotNull(message = "Indique a partir de quando vigora.") LocalDate effectiveFrom,
        LocalDate effectiveTo,
        @Min(0) @Max(365) Integer vacationDaysYear1,
        @Min(0) @Max(365) Integer vacationDaysYear2,
        @Min(0) @Max(365) Integer vacationDaysYear3Plus,
        @Min(1) @Max(31) Integer irpsDeliveryDay,
        @Min(1) @Max(31) Integer inssDeliveryDay,
        @Min(0) @Max(365) Integer noticeDaysEmployee,
        @Min(0) @Max(365) Integer noticeDaysEmployer,
        String legalBasis
) {}
