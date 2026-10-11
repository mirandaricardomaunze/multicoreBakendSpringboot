package mz.multicore.erp.modules.intelligence.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AiOperationalDraftRequest(@NotBlank @Size(min = 8, max = 2000) String request) {}
