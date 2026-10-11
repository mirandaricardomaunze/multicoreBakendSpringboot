package mz.multicore.erp.modules.intelligence.dto;

import java.time.LocalDateTime;

public record AiManagedDocumentDTO(
        Long id,
        String documentNumber,
        String title,
        String documentType,
        String status,
        int currentVersion,
        String authorUsername,
        LocalDateTime createdAt,
        LocalDateTime submittedAt,
        LocalDateTime reviewedAt,
        String reviewedBy,
        String rejectionReason
) {}
