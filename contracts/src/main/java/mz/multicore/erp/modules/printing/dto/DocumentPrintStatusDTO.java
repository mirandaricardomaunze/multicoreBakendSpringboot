package mz.multicore.erp.modules.printing.dto;

import java.time.LocalDateTime;

public record DocumentPrintStatusDTO(
        SensitiveDocumentType documentType,
        Long documentId,
        int printCount,
        LocalDateTime printedAt,
        String lastPrintedBy,
        String lastAuthorizedBy,
        String lastReason
) {
    public static DocumentPrintStatusDTO neverPrinted(SensitiveDocumentType type, Long id) {
        return new DocumentPrintStatusDTO(type, id, 0, null, null, null, null);
    }
}
