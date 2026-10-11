package mz.multicore.erp.modules.intelligence.dto;

import java.time.LocalDateTime;
import java.util.List;

/** Resposta somente de leitura, acompanhada da evidência operacional usada. */
public record AiAssistantResponse(
        String answer,
        LocalDateTime generatedAt,
        List<AiEvidenceDTO> evidence,
        boolean externalModelUsed,
        String notice
) {
    public AiAssistantResponse {
        evidence = evidence == null ? List.of() : List.copyOf(evidence);
    }
}
