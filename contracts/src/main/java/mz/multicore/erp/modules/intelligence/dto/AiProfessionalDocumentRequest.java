package mz.multicore.erp.modules.intelligence.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Pedido de documento informativo; documentos transaccionais continuam nos módulos de origem. */
public record AiProfessionalDocumentRequest(
        @NotBlank @Size(max = 40) String documentType,
        @NotBlank @Size(max = 160) String title,
        @NotBlank @Size(min = 5, max = 2000) String instructions,
        AiAssistantResponse analysis
) {}
