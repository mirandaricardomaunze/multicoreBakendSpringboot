package com.phcpro.modules.printing;

import com.phcpro.architecture.exception.BusinessRuleException;

/** Raised when PDF generation fails. Mapped by GlobalExceptionHandler. */
public class PdfGenerationException extends BusinessRuleException {
    public PdfGenerationException(String message, Throwable cause) {
        super(message);
        initCause(cause);
    }
}
