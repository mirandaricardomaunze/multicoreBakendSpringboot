package mz.multicore.erp.modules.crm.dto;

import jakarta.validation.constraints.Size;

/** Atribuição de um pedido de assistência: quem o trata e com que urgência. */
public record UpdateTicketRequest(
    /** Nome do enum {@code TicketPriority}. Nulo = mantém a prioridade actual. */
    String priority,

    /** Técnico responsável. String vazia liberta o pedido; nulo mantém o responsável actual. */
    @Size(max = 255, message = "O nome do técnico não pode exceder 255 caracteres.")
    String assignedTechnician
) {}
