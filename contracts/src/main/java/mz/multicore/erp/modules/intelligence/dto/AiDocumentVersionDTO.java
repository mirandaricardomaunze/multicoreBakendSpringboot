package mz.multicore.erp.modules.intelligence.dto;

import java.time.LocalDateTime;

public record AiDocumentVersionDTO(
        Long id,
        int versionNumber,
        String instructions,
        String generatedContent,
        String generatedBy,
        LocalDateTime createdAt
) {}
