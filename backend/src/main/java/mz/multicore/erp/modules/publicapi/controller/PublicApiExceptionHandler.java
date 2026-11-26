package mz.multicore.erp.modules.publicapi.controller;

import mz.multicore.erp.modules.publicapi.service.PublicApiNotFoundException;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

/**
 * Errors of the public API controllers (docs/API_PUBLICA_WEBHOOKS_SPEC.md §3.2): JSON with a stable {@code code} and a
 * PT-MZ {@code message}. Scoped to this package and ahead of the internal API's global handler.
 */
@RestControllerAdvice(basePackageClasses = PublicApiExceptionHandler.class)
@Order(Ordered.HIGHEST_PRECEDENCE)
public class PublicApiExceptionHandler {

    @ExceptionHandler(PublicApiNotFoundException.class)
    public ResponseEntity<Map<String, String>> notFound(PublicApiNotFoundException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("code", e.code(), "message", e.getMessage()));
    }
}
