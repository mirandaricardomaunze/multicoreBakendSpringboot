package mz.multicore.erp.modules.intelligence.dto;

import java.math.BigDecimal;

/** Métrica agregada que sustenta a resposta do assistente. */
public record AiEvidenceDTO(String metric, String label, BigDecimal value, String period) {
}
