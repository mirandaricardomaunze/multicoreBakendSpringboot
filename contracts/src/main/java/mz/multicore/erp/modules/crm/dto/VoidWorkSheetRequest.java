package mz.multicore.erp.modules.crm.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Anulação de folha de obra por faturar. O motivo é obrigatório — a folha continua visível. */
public record VoidWorkSheetRequest(
    @NotBlank(message = "É obrigatório indicar o motivo da anulação.")
    @Size(max = 500, message = "O motivo não pode exceder 500 caracteres.")
    String reason
) {}
