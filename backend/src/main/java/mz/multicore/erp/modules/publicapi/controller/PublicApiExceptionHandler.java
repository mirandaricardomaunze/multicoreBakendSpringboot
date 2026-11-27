package mz.multicore.erp.modules.publicapi.controller;

import jakarta.persistence.OptimisticLockException;
import mz.multicore.erp.architecture.exception.BusinessRuleException;
import mz.multicore.erp.architecture.exception.PublicApiError;
import mz.multicore.erp.modules.comercial.service.InsufficientStockException;
import mz.multicore.erp.modules.publicapi.service.PublicApiException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.support.DefaultMessageSourceResolvable;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.Objects;

/**
 * Errors of the public API controllers (docs/API_PUBLICA_WEBHOOKS_SPEC.md §3.2.1, §3.4.1, API-06): JSON
 * {@link PublicApiError} with a stable {@code code} and a PT-MZ {@code message}, never internals. Scoped to the public
 * controllers (not the internal key management of {@link ApiKeyController}) and ahead of the internal API's global
 * handler. Errors raised before a controller is chosen (unknown route, wrong method) are shaped by the global handler.
 */
@RestControllerAdvice(assignableTypes = {PublicCatalogController.class, PublicOrderController.class})
@Order(Ordered.HIGHEST_PRECEDENCE)
public class PublicApiExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(PublicApiExceptionHandler.class);

    @ExceptionHandler(PublicApiException.class)
    public ResponseEntity<PublicApiError> publicApi(PublicApiException e) {
        return body(HttpStatus.valueOf(e.status()), e.code(), e.getMessage());
    }

    @ExceptionHandler(InsufficientStockException.class)
    public ResponseEntity<PublicApiError> insufficientStock(InsufficientStockException e) {
        return body(HttpStatus.UNPROCESSABLE_ENTITY, "INSUFFICIENT_STOCK", e.getMessage());
    }

    @ExceptionHandler(BusinessRuleException.class)
    public ResponseEntity<PublicApiError> businessRule(BusinessRuleException e) {
        return body(HttpStatus.UNPROCESSABLE_ENTITY, "BUSINESS_RULE_VIOLATION", e.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<PublicApiError> invalidBody(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getAllErrors().stream()
                .map(DefaultMessageSourceResolvable::getDefaultMessage)
                .filter(Objects::nonNull)
                .findFirst().orElse("Pedido inválido.");
        return validation(message);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<PublicApiError> unreadableBody(HttpMessageNotReadableException e) {
        return validation("O corpo do pedido não é JSON válido.");
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<PublicApiError> invalidParameter(MethodArgumentTypeMismatchException e) {
        return validation("O parâmetro " + e.getName() + " tem um valor inválido.");
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<PublicApiError> missingParameter(MissingServletRequestParameterException e) {
        return validation("Falta o parâmetro obrigatório " + e.getParameterName() + ".");
    }

    @ExceptionHandler({DataIntegrityViolationException.class, ObjectOptimisticLockingFailureException.class,
                       OptimisticLockException.class})
    public ResponseEntity<PublicApiError> concurrentWrite(Exception e) {
        log.warn("API pública: pedido em conflito com outro gravado em simultâneo: {}", e.getMessage());
        return body(HttpStatus.CONFLICT, "CONFLICT",
                "O pedido colidiu com outro gravado ao mesmo tempo; nada ficou duplicado. Repita o pedido.");
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<PublicApiError> unexpected(Exception e) {
        log.error("API pública: erro não tratado", e);
        return body(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR",
                "Ocorreu um erro interno. Tente novamente; se persistir, contacte o suporte da loja.");
    }

    private static ResponseEntity<PublicApiError> validation(String message) {
        return body(HttpStatus.BAD_REQUEST, PublicApiException.VALIDATION_ERROR, message);
    }

    private static ResponseEntity<PublicApiError> body(HttpStatus status, String code, String message) {
        return ResponseEntity.status(status).body(new PublicApiError(code, message));
    }
}
