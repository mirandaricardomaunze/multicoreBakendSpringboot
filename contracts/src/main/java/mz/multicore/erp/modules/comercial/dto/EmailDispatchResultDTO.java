package mz.multicore.erp.modules.comercial.dto;

/**
 * Resultado da operação de envio de documento por correio eletrónico.
 */
public record EmailDispatchResultDTO(
        boolean success,
        String message,
        String recipientEmail,
        String messageId
) {}
