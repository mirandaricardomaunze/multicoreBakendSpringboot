package mz.multicore.erp.modules.intelligence.dto;

import java.util.List;

public record AiOperationalDraftResponse(
        String draftType, String targetModule, String summary,
        List<String> explicitFields, List<String> missingFields,
        boolean readyForReview, String warning
) {
    public AiOperationalDraftResponse {
        explicitFields = explicitFields == null ? List.of() : List.copyOf(explicitFields);
        missingFields = missingFields == null ? List.of() : List.copyOf(missingFields);
    }
}
