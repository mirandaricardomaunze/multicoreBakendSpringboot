package mz.multicore.erp.modules.accounting.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Creates or edits a manual cost centre (docs/CENTROS_CUSTO_ORCAMENTO_SPEC.md §3.1).
 *
 * @param active blank keeps the current state (new centres start active)
 */
public record SaveCostCenterRequest(
        @NotBlank(message = "O código do centro de custo é obrigatório.")
        @Size(max = 20) String code,

        @NotBlank(message = "O nome do centro de custo é obrigatório.")
        @Size(max = 120) String name,

        Boolean active
) {}
