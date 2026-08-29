package mz.multicore.erp.architecture.exception;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Um caminho que não existe é <b>404</b>, não 500.
 *
 * <p>Encontrado a exercitar o RH ponta a ponta: um endereço enganado
 * ({@code /api/hr/payslips/1/pay}, quando o endpoint é {@code mark-paid}) devolvia
 * <i>Internal Server Error</i> e escrevia "Erro não tratado a processar pedido" no log, com stack
 * trace. Duas consequências, nenhuma visível a correr o programa:
 *
 * <ul>
 *   <li>Quem vigia o servidor deixa de conseguir separar uma <b>avaria real</b> de um cliente a
 *       bater na porta errada — e é a contagem de 500 que costuma disparar o alarme.
 *   <li>Um desktop de versão antiga a chamar um endpoint que já não existe recebe "o servidor está
 *       avariado" em vez de "isso aqui não existe" — que é precisamente a distinção de que a
 *       compatibilidade entre versões depende (§6 da spec dos módulos).
 * </ul>
 */
class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void unknownPathIsNotFoundAndNotAServerError() {
        NoResourceFoundException missing =
                new NoResourceFoundException(HttpMethod.POST, "api/hr/payslips/1/pay");

        ResponseEntity<GlobalExceptionHandler.ErrorResponse> response = handler.handleNotFound(missing);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertEquals(404, response.getBody().status());
        assertEquals("Not Found", response.getBody().error());
    }

    /** A resposta não pode devolver o caminho pedido — não se confirma a sondagens o que existe. */
    @Test
    void theAnswerDoesNotEchoTheProbedPath() {
        ResponseEntity<GlobalExceptionHandler.ErrorResponse> response = handler.handleNotFound(
                new NoResourceFoundException(HttpMethod.GET, "api/interno/segredo"));

        assertFalse(response.getBody().message().contains("interno"));
        assertFalse(response.getBody().message().contains("segredo"));
    }

    /**
     * Faltar um parâmetro obrigatório é erro de quem chama, não avaria do servidor.
     *
     * <p>Encontrado ao chamar {@code /api/inventory/warehouses} sem {@code ?companyId=}: saía
     * <i>Internal Server Error</i>. É o caso mais frequente da família — mais do que um caminho
     * errado — porque acontece a toda a gente que integra contra a API pela primeira vez.
     */
    @Test
    void missingRequiredParameterIsBadRequestAndSaysWhichOne() {
        ResponseEntity<GlobalExceptionHandler.ErrorResponse> response = handler.handleMalformedRequest(
                new MissingServletRequestParameterException("companyId", "Long"));

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertTrue(response.getBody().message().contains("companyId"),
                "quem integra tem de descobrir o que falta sem acesso ao log do servidor");
    }

    /** Uma regra de negócio continua a ser 400, com a razão em PT-MZ para o operador. */
    @Test
    void businessRuleStaysABadRequestWithItsOwnMessage() {
        ResponseEntity<GlobalExceptionHandler.ErrorResponse> response =
                handler.handleBusinessRule(new BusinessRuleException("Este exame já foi pago."));

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("Este exame já foi pago.", response.getBody().message());
    }

    /** E uma falha a sério continua 500, sem expor internos ao cliente. */
    @Test
    void genuineFailureStaysAServerErrorWithoutLeakingInternals() {
        ResponseEntity<GlobalExceptionHandler.ErrorResponse> response =
                handler.handleGeneric(new IllegalStateException("ORA-00942: table or view does not exist"));

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertFalse(response.getBody().message().contains("ORA-00942"),
                "o detalhe da avaria fica no log do servidor, não na resposta");
    }

}
