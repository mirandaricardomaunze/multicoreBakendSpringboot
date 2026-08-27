package mz.multicore.erp.modules.crm.dto;

import java.time.LocalDateTime;

/**
 * Pedido de assistência na fronteira HTTP. {@code status}/{@code priority} viajam como nome do
 * enum (a tabela pinta o badge a partir dele) e os {@code *Label} trazem o texto PT-MZ já feito,
 * para o desktop não ter de repetir a tradução em cada ecrã.
 */
public record SupportTicketDTO(
    Long id,
    Long clientId,
    String clientName,
    String subject,
    String description,
    String status,
    String statusLabel,
    String priority,
    String priorityLabel,
    String assignedTechnician,
    String closingNote,
    LocalDateTime createdAt,
    LocalDateTime resolvedAt
) {}
