package com.phcpro.modules.support.dto;

import java.time.LocalDateTime;

/** Ticket de assistência. {@code companyName} só é preenchido na vista do superadmin. */
public record SupportTicketDTO(
        Long id,
        Long companyId,
        String companyName,
        String subject,
        String description,
        String status,
        String statusLabel,
        String priority,
        String priorityLabel,
        String assignee,
        String createdBy,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        long messageCount
) {}
