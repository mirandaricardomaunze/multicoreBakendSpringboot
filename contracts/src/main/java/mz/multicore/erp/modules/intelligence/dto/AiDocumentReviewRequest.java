package mz.multicore.erp.modules.intelligence.dto;

import jakarta.validation.constraints.Size;

public record AiDocumentReviewRequest(@Size(max = 500) String reason) {}
