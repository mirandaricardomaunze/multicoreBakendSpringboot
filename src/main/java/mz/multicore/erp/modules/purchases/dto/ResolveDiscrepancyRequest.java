package mz.multicore.erp.modules.purchases.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * @param resolutionNotes como ficou resolvido (nota de crédito, substituição, perdoado).
 *                        Obrigatório: sem explicação, "resolvido" não vale nada daqui a seis meses.
 */
public record ResolveDiscrepancyRequest(
        @NotBlank(message = "Indique como a divergência foi resolvida.")
        String resolutionNotes
) {}
