package mz.multicore.erp.modules.crm.dto;

import java.time.LocalDateTime;

public record SupportTicketDTO(
    Long id,
    Long clientId,
    String clientName,
    String subject,
    String description,
    String status,
    LocalDateTime createdAt
) {}
