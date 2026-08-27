package mz.multicore.erp.modules.crm.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Mudança de estado de um pedido de assistência — assumir, resolver ao telefone, anular ou reabrir.
 * O caminho antigo obrigava a registar folha de obra só para fechar um pedido.
 */
public record ChangeTicketStatusRequest(
    @NotBlank(message = "O novo estado é obrigatório.")
    String status,

    /** Nota de fecho ou motivo da anulação. Obrigatória para anular (regra no CRMService). */
    @Size(max = 500, message = "A nota não pode exceder 500 caracteres.")
    String note
) {}
