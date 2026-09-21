package mz.multicore.erp.modules.comercial.dto;

import java.time.LocalDate;

/**
 * Requisição para envio de extrato de conta corrente por correio eletrónico (email).
 */
public record SendStatementEmailRequest(
        Long clientId,
        String recipientEmail,
        String subject,
        String messageNote,
        LocalDate startDate,
        LocalDate endDate
) {}
