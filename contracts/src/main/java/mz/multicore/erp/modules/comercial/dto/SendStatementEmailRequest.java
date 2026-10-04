package mz.multicore.erp.modules.comercial.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/**
 * Requisição para envio de extrato de conta corrente por correio eletrónico (email).
 */
public record SendStatementEmailRequest(
        @NotNull(message = "O cliente é obrigatório.")
        Long clientId,

        @Email(message = "O email de destino deve ter um formato válido.")
        @Size(max = 200, message = "O email não pode exceder 200 caracteres.")
        String recipientEmail,

        @Size(max = 200, message = "O assunto não pode exceder 200 caracteres.")
        String subject,

        @Size(max = 2000, message = "A nota não pode exceder 2000 caracteres.")
        String messageNote,

        LocalDate startDate,
        LocalDate endDate
) {}
