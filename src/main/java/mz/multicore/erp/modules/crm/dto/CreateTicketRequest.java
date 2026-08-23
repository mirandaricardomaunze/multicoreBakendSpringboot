package mz.multicore.erp.modules.crm.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateTicketRequest(
    @NotNull(message = "O ID do cliente é obrigatório.")
    Long clientId,

    @NotBlank(message = "O assunto é obrigatório.")
    @Size(max = 200, message = "O assunto não pode exceder 200 caracteres.")
    String subject,

    @NotBlank(message = "A descrição é obrigatória.")
    @Size(max = 1000, message = "A descrição não pode exceder 1000 caracteres.")
    String description,

    /** Nome do enum {@code TicketPriority}. Nulo/vazio = NORMAL. */
    String priority,

    /** Técnico responsável. Opcional — um pedido pode entrar sem dono e ser atribuído depois. */
    @Size(max = 255, message = "O nome do técnico não pode exceder 255 caracteres.")
    String assignedTechnician
) {
    /** Compatibilidade com quem abre um pedido simples (povoamento, testes, integrações antigas). */
    public CreateTicketRequest(Long clientId, String subject, String description) {
        this(clientId, subject, description, null, null);
    }
}
