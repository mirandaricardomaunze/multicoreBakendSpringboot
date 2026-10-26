package mz.multicore.erp.modules.comercial.dto;

/**
 * Modelo de mensagem de cobrança ou lembrete preventivo gerado para um canal de comunicação.
 */
public record CollectionReminderTemplateDTO(
        Long invoiceId,
        String invoiceNumber,
        String clientName,
        String channel,
        String recipientContact,
        String subject,
        String message
) {
}
