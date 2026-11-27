package mz.multicore.erp.modules.publicapi.controller;

import mz.multicore.erp.architecture.exception.BusinessRuleException;
import mz.multicore.erp.modules.comercial.service.InsufficientStockException;
import mz.multicore.erp.modules.publicapi.service.PublicApiException;
import org.springframework.context.support.DefaultMessageSourceResolvable;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;
import java.util.Objects;

/**
 * Errors of the public API controllers (docs/API_PUBLICA_WEBHOOKS_SPEC.md §3.2, §3.4.1): JSON with a stable
 * {@code code} and a PT-MZ {@code message}. Scoped to this package and ahead of the internal API's global handler.
 */
@RestControllerAdvice(basePackageClasses = PublicApiExceptionHandler.class)
@Order(Ordered.HIGHEST_PRECEDENCE)
public class PublicApiExceptionHandler {

    @ExceptionHandler(PublicApiException.class)
    public ResponseEntity<Map<String, String>> publicApi(PublicApiException e) {
        return body(HttpStatus.valueOf(e.status()), e.code(), e.getMessage());
    }

    @ExceptionHandler(InsufficientStockException.class)
    public ResponseEntity<Map<String, String>> insufficientStock(InsufficientStockException e) {
        return body(HttpStatus.UNPROCESSABLE_ENTITY, "INSUFFICIENT_STOCK", e.getMessage());
    }

    @ExceptionHandler(BusinessRuleException.class)
    public ResponseEntity<Map<String, String>> businessRule(BusinessRuleException e) {
        return body(HttpStatus.UNPROCESSABLE_ENTITY, "BUSINESS_RULE_VIOLATION", e.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> invalidBody(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getAllErrors().stream()
                .map(DefaultMessageSourceResolvable::getDefaultMessage)
                .filter(Objects::nonNull)
                .findFirst().orElse("Pedido inválido.");
        return body(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", message);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, String>> unreadableBody(HttpMessageNotReadableException e) {
        return body(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "O corpo do pedido não é JSON válido.");
    }

    private static ResponseEntity<Map<String, String>> body(HttpStatus status, String code, String message) {
        return ResponseEntity.status(status).body(Map.of("code", code, "message", message == null ? "" : message));
    }
}
