package com.phcpro.architecture.version;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * Aperto de mão de versão entre o desktop e o backend.
 *
 * <p><b>O problema que resolve:</b> o desktop e o backend são o mesmo codebase mas vivem em
 * sítios diferentes — o servidor actualiza hoje, a loja da Beira só daqui a dois meses. Nesse
 * intervalo as duas metades falam línguas diferentes, e o sintoma que o operador vê é um erro
 * sem explicação a meio de uma factura.
 *
 * <p>Com este cabeçalho, um cliente demasiado antigo recebe <b>426 Upgrade Required</b> e uma
 * mensagem que diz o que fazer, em vez de um 400 misterioso vindo de um campo que já não existe.
 *
 * <p>Cliente <b>sem</b> o cabeçalho passa: são as integrações antigas e as ferramentas de
 * diagnóstico (curl, testes). Bloquear quem não se identifica partiria o que já funciona hoje
 * para ganhar pouco — quem quiser apertar isso muda {@code app.client.require-version}.
 */
@Component
public class ClientVersionInterceptor implements HandlerInterceptor {

    /** Cabeçalho que o desktop preenche com a sua própria versão. */
    public static final String HEADER = "X-Client-Version";

    private final String minClientVersion;
    private final boolean requireVersion;
    private final ClientVersionRegistry registry;

    public ClientVersionInterceptor(
            @Value("${app.client.min-version:0.0.0}") String minClientVersion,
            @Value("${app.client.require-version:false}") boolean requireVersion,
            ClientVersionRegistry registry) {
        this.minClientVersion = minClientVersion;
        this.requireVersion = requireVersion;
        this.registry = registry;
    }

    /**
     * Regista a versão vista <b>depois</b> do pedido, e não no {@code preHandle}.
     *
     * <p>A razão é de segurança: no {@code preHandle} ainda não houve autenticação, pelo que
     * qualquer um poderia encher a tabela com versões inventadas. Aqui já se sabe quem é e a que
     * empresa pertence — e só se regista o que passou pela porta.
     */
    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler,
                           org.springframework.web.servlet.ModelAndView modelAndView) {
        if (registry == null) return;
        String clientVersion = request.getHeader(HEADER);
        if (clientVersion == null || clientVersion.isBlank()) return;
        Long companyId = com.phcpro.architecture.security.CurrentUserContext.findCurrentCompanyId();
        if (companyId == null) return;
        registry.record(companyId, clientVersion,
                com.phcpro.architecture.security.CurrentUserContext.findCurrentUser() == null
                        ? null : com.phcpro.architecture.security.CurrentUserContext.getUsername());
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception {
        String clientVersion = request.getHeader(HEADER);

        if (clientVersion == null || clientVersion.isBlank()) {
            if (!requireVersion) return true;
            reject(response, "Cliente sem versão declarada.");
            return false;
        }

        if (SemanticVersion.isOlderThan(clientVersion, minClientVersion)) {
            reject(response, String.format(
                    "Esta versão do programa (%s) já não é suportada por este servidor "
                            + "(mínima: %s). Actualize o programa para continuar.",
                    clientVersion.trim(), minClientVersion));
            return false;
        }
        return true;
    }

    /**
     * 426 (Upgrade Required) e não 400: o 400 diz "o teu pedido está mal feito" e manda o técnico
     * procurar no sítio errado. O 426 diz exactamente qual é o problema — o programa está velho.
     * A constante não existe no {@code HttpServletResponse} (só vai até ao 505 do HTTP/1.1).
     */
    public static final int SC_UPGRADE_REQUIRED = 426;

    /**
     * Escreve o corpo <b>à mão</b> em vez de usar {@code sendError(código, mensagem)}.
     *
     * <p>Verificado ao vivo: o {@code sendError} <b>descarta o texto</b> — o cliente recebia
     * {@code {"status":426,"error":"Upgrade Required"}} e a mensagem cuidada ("actualize o
     * programa", com as versões) nunca chegava a ninguém. A alternativa seria ligar
     * {@code server.error.include-message=always}, mas isso exporia as mensagens internas de
     * <b>todos</b> os erros do sistema para resolver um caso — preço alto de mais.
     *
     * <p>O formato espelha o do {@code GlobalExceptionHandler} para o desktop o ler pelo mesmo
     * caminho ({@code DesktopApiClient.errorMessage} procura o campo {@code message}).
     */
    private void reject(HttpServletResponse response, String message) throws java.io.IOException {
        response.setStatus(SC_UPGRADE_REQUIRED);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write(String.format(
                "{\"status\":426,\"error\":\"Upgrade Required\",\"message\":\"%s\"}",
                message.replace("\"", "'")));
        response.getWriter().flush();
    }
}
