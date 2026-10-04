package mz.multicore.erp.architecture.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(BusinessRuleException.class)
    public ResponseEntity<ErrorResponse> handleBusinessRule(BusinessRuleException ex) {
        ErrorResponse response = new ErrorResponse(
                LocalDateTime.now(),
                HttpStatus.BAD_REQUEST.value(),
                "Business Rule Violation",
                ex.getMessage()
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        String readableMsg = ex.getBindingResult().getFieldErrors().stream()
                .map(err -> err.getField() + ": " + (err.getDefaultMessage() != null ? err.getDefaultMessage() : "valor inválido"))
                .collect(java.util.stream.Collectors.joining("; ", "Validação falhou: ", "."));

        ex.getBindingResult().getAllErrors().forEach((error) -> {
            String fieldName = error instanceof FieldError fe ? fe.getField() : error.getObjectName();
            String errorMessage = error.getDefaultMessage() != null ? error.getDefaultMessage() : "valor inválido";
            errors.put(fieldName, errorMessage);
        });

        log.warn("Erro de validação de argumentos: {}", readableMsg);

        ErrorResponse response = new ErrorResponse(
                LocalDateTime.now(),
                HttpStatus.BAD_REQUEST.value(),
                "Validation Error",
                readableMsg
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(jakarta.validation.ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraintViolation(jakarta.validation.ConstraintViolationException ex) {
        String detail = ex.getConstraintViolations().stream()
                .map(cv -> cv.getPropertyPath() + ": " + cv.getMessage())
                .collect(java.util.stream.Collectors.joining("; ", "Validação de restrição falhou: ", "."));
        log.warn("Violação de restrição Jakarta: {}", detail);
        ErrorResponse response = new ErrorResponse(
                LocalDateTime.now(),
                HttpStatus.BAD_REQUEST.value(),
                "Constraint Violation",
                detail
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(org.springframework.dao.DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleDataIntegrity(org.springframework.dao.DataIntegrityViolationException ex) {
        log.warn("Violação de integridade de dados na base de dados: {}", ex.getMessage());
        ErrorResponse response = new ErrorResponse(
                LocalDateTime.now(),
                HttpStatus.BAD_REQUEST.value(),
                "Data Integrity Violation",
                "Operação não permitida por conflito de integridade de dados (registo duplicado ou dependência activa)."
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler({org.springframework.orm.ObjectOptimisticLockingFailureException.class,
                       jakarta.persistence.OptimisticLockException.class})
    public ResponseEntity<ErrorResponse> handleOptimisticLock(Exception ex) {
        log.warn("Conflito de concorrência optimista (versão desactualizada): {}", ex.getMessage());
        ErrorResponse response = new ErrorResponse(
                LocalDateTime.now(),
                HttpStatus.CONFLICT.value(),
                "Conflict",
                "Este registo foi alterado ou aprovado concorrentemente por outro utilizador. Por favor, actualize os dados antes de gravar."
        );
        return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(IllegalArgumentException ex) {
        log.warn("Argumento inválido: {}", ex.getMessage());
        ErrorResponse response = new ErrorResponse(
                LocalDateTime.now(),
                HttpStatus.BAD_REQUEST.value(),
                "Bad Request",
                ex.getMessage() != null && !ex.getMessage().isBlank() ? ex.getMessage() : "Argumento ou parâmetro inválido."
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    /**
     * Caminho que não existe é <b>404</b>, não 500.
     *
     * <p>Sem este tratamento, qualquer URL enganado caía no {@code handleGeneric} e saía como
     * <i>Internal Server Error</i>, com a linha "Erro não tratado" no log. Duas consequências, e
     * nenhuma delas óbvia: quem vigia o servidor deixa de conseguir separar uma avaria real de um
     * cliente a bater na porta errada, e um desktop de versão antiga a chamar um endpoint que já
     * não existe recebe "o servidor está avariado" em vez de "isso aqui não existe" — que é
     * precisamente a distinção de que a compatibilidade de versões (§6 da spec dos módulos)
     * depende.
     */
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(NoResourceFoundException ex) {
        log.warn("Caminho inexistente: {}", ex.getResourcePath());
        ErrorResponse response = new ErrorResponse(
                LocalDateTime.now(),
                HttpStatus.NOT_FOUND.value(),
                "Not Found",
                "O endereço pedido não existe neste servidor."
        );
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }

    /**
     * Pedido mal formado é <b>400</b>, não 500.
     *
     * <p>Mesma família do 404 acima, e mais frequente na prática: faltar um parâmetro obrigatório
     * ({@code ?companyId=}), mandá-lo com o tipo errado, ou enviar um corpo JSON que não se lê. São
     * todos erros de <b>quem chama</b>, e nenhum deles é uma avaria do servidor — mas todos saíam
     * como <i>Internal Server Error</i> e escreviam "Erro não tratado" no log, com stack trace.
     *
     * <p>A mensagem diz <b>o que falta</b>, porque quem integra contra a API tem de o poder
     * descobrir sem acesso ao log do servidor.
     */
    @ExceptionHandler({MissingServletRequestParameterException.class,
                       MethodArgumentTypeMismatchException.class,
                       HttpMessageNotReadableException.class})
    public ResponseEntity<ErrorResponse> handleMalformedRequest(Exception ex) {
        String detail = switch (ex) {
            case MissingServletRequestParameterException missing ->
                    "Falta o parâmetro obrigatório \"" + missing.getParameterName() + "\".";
            case MethodArgumentTypeMismatchException mismatch ->
                    "O parâmetro \"" + mismatch.getName() + "\" tem um valor inválido.";
            default -> "O corpo do pedido não pôde ser interpretado.";
        };
        log.warn("Pedido mal formado: {}", detail);
        ErrorResponse response = new ErrorResponse(
                LocalDateTime.now(), HttpStatus.BAD_REQUEST.value(), "Bad Request", detail);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneric(Exception ex) {
        // O detalhe da exceção (mensagem, stack, eventual SQL) fica só no log do servidor;
        // o cliente recebe uma mensagem genérica para não expor internos.
        log.error("Erro não tratado a processar pedido", ex);
        ErrorResponse response = new ErrorResponse(
                LocalDateTime.now(),
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                "Internal Server Error",
                "Ocorreu um erro interno. Contacte o suporte se persistir."
        );
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }

    public record ErrorResponse(
            LocalDateTime timestamp,
            int status,
            String error,
            String message
    ) {}
}
