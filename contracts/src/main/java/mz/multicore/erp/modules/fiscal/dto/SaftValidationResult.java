package mz.multicore.erp.modules.fiscal.dto;

import java.util.List;

/**
 * Result of validating a SAF-T file against the XSD.
 *
 * @param xsdConfigured whether the XSD is configured and accessible
 * @param valid         whether the XML conforms to the XSD; meaningful only when {@code xsdConfigured}
 * @param errors        validation errors, empty when valid
 * @param message       human-readable message for the UI
 */
public record SaftValidationResult(
        boolean xsdConfigured,
        boolean valid,
        List<String> errors,
        String message
) {}
