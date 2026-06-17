package com.phcpro.modules.backup.dto;

import java.util.Map;

public record BackupVerificationDTO(
        String fileName,
        Long companyId,
        String generatedAt,
        int totalSections,
        Map<String, Integer> itemCounts
) {
}
