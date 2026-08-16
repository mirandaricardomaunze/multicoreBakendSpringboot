package mz.multicore.erp.modules.printing;

import mz.multicore.erp.architecture.exception.BusinessRuleException;

/** Raised when PDF generation fails. Mapped by GlobalExceptionHandler. */
public class PdfGenerationException extends BusinessRuleException {
    public PdfGenerationException(String message, Throwable cause) {
        super(message);
        initCause(cause);
    }
}
