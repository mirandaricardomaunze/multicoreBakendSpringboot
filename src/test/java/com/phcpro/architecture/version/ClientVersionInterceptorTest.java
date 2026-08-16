package com.phcpro.architecture.version;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Aperto de mão de versão no servidor (AC-10..AC-16).
 * Ver docs/ACTUALIZACOES_CLIENTE_SPEC.md.
 */
class ClientVersionInterceptorTest {

    private final HttpServletRequest request = mock(HttpServletRequest.class);
    private final HttpServletResponse response = mock(HttpServletResponse.class);
    private final java.io.StringWriter body = new java.io.StringWriter();

    private boolean preHandle(String clientVersion, String minVersion, boolean require) throws Exception {
        when(request.getHeader(ClientVersionInterceptor.HEADER)).thenReturn(clientVersion);
        when(response.getWriter()).thenReturn(new java.io.PrintWriter(body));
        return new ClientVersionInterceptor(minVersion, require, mock(ClientVersionRegistry.class))
                .preHandle(request, response, new Object());
    }

    /**
     * O corpo escrito na recusa. Verifica-se o <b>corpo</b> e não o {@code sendError}, porque
     * ao correr ao vivo descobriu-se que o {@code sendError} descarta a mensagem — o cliente
     * recebia um 426 sem explicação nenhuma. Ver spec §2.
     */
    private String rejectionBody() {
        verify(response).setStatus(426);
        return body.toString();
    }

    @Test // AC-10
    void clienteActualPassa() throws Exception {
        assertTrue(preHandle("1.4.0", "1.2.0", false));
        verify(response, never()).setStatus(anyInt());
    }

    @Test // AC-11
    void clienteExactamenteNoMinimoPassa() throws Exception {
        assertTrue(preHandle("1.2.0", "1.2.0", false));
        verify(response, never()).setStatus(anyInt());
    }

    @Test // AC-12
    void clienteAntigoERecusadoCom426() throws Exception {
        assertFalse(preHandle("1.1.9", "1.2.0", false));

        String message = rejectionBody();
        assertTrue(message.contains("1.1.9"), "diz qual é a versão instalada");
        assertTrue(message.contains("1.2.0"), "diz qual é a mínima exigida");
        assertTrue(message.contains("Actualize"), "diz o que fazer");
    }

    @Test // AC-13
    void semCabecalhoPassaPorOmissao() throws Exception {
        // curl, testes e integrações antigas não mandam o cabeçalho — não têm de partir.
        assertTrue(preHandle(null, "1.2.0", false));
        assertTrue(preHandle("  ", "1.2.0", false));
        verify(response, never()).setStatus(anyInt());
    }

    @Test // AC-14
    void semCabecalhoERecusadoQuandoExigido() throws Exception {
        assertFalse(preHandle(null, "1.2.0", true));
        assertTrue(rejectionBody().contains("sem versão declarada"), rejectionBody());
    }

    @Test // AC-15
    void minimoZeroDeixaPassarTudo() throws Exception {
        // O default do sistema não pode trancar ninguém fora.
        assertTrue(preHandle("0.0.1", "0.0.0", false));
        assertTrue(preHandle(ClientVersion.UNKNOWN, "0.0.0", false));
        verify(response, never()).setStatus(anyInt());
    }

    @Test // AC-16
    void versaoIlegivelERecusadaQuandoHaMinimo() throws Exception {
        assertFalse(preHandle("qualquer-coisa", "1.0.0", false));
        assertFalse(rejectionBody().isBlank(), "a recusa tem de trazer corpo explicativo");
    }
}
