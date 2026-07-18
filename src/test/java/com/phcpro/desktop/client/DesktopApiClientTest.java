package com.phcpro.desktop.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.phcpro.desktop.config.DesktopApiConfig;
import com.phcpro.desktop.session.DesktopSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Teste de contrato da camada HTTP partilhada por TODOS os clientes tipados do desktop
 * ({@link DesktopApiClient}). Não abre o Spring nem faz rede — o {@link HttpClient} é mockado.
 * Ver DESKTOP_THIN_CLIENT_HARNESS (TC-01..05).
 */
class DesktopApiClientTest {

    private HttpClient httpClient;
    private DesktopApiClient client;      // com sessão activa (token + empresa)
    private DesktopApiClient anonymous;   // sem sessão (fluxo de login)

    @BeforeEach
    void setUp() {
        httpClient = mock(HttpClient.class);
        ObjectMapper mapper = new ObjectMapper().findAndRegisterModules();
        DesktopApiConfig config = new DesktopApiConfig("http://test.local");

        DesktopSession session = new DesktopSession("tok-123", Instant.now().plusSeconds(3600),
                "ana", "Ana", false, List.of(new DesktopSession.CompanyAccess(7L, "ACME", "ADMIN")));
        session.selectCompany(7L);

        client = new DesktopApiClient(config, session, httpClient, mapper);
        anonymous = new DesktopApiClient(config, null, httpClient, mapper);
    }

    @SuppressWarnings("unchecked")
    private void stubResponse(int status, String body) throws Exception {
        HttpResponse<String> response = mock(HttpResponse.class);
        when(response.statusCode()).thenReturn(status);
        when(response.body()).thenReturn(body);
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class))).thenReturn(response);
    }

    private HttpRequest captureRequest() throws Exception {
        ArgumentCaptor<HttpRequest> captor = ArgumentCaptor.forClass(HttpRequest.class);
        verify(httpClient).send(captor.capture(), any());
        return captor.getValue();
    }

    // TC-01 — GET leva token + empresa nos cabeçalhos e parseia o corpo.
    @Test
    void get_sendsAuthAndCompanyHeaders_andParsesBody() throws Exception {
        stubResponse(200, "{\"id\":1,\"name\":\"Ana\"}");

        Sample result = client.get("/api/x/1", Sample.class);

        assertEquals(1, result.id());
        assertEquals("Ana", result.name());
        HttpRequest req = captureRequest();
        assertEquals("http://test.local/api/x/1", req.uri().toString());
        assertEquals("GET", req.method());
        assertEquals("Bearer tok-123", req.headers().firstValue("Authorization").orElse(null));
        assertEquals("7", req.headers().firstValue("X-Company-Id").orElse(null));
    }

    // TC-02 — getList parseia um array JSON.
    @Test
    void getList_parsesJsonArray() throws Exception {
        stubResponse(200, "[{\"id\":1,\"name\":\"A\"},{\"id\":2,\"name\":\"B\"}]");

        List<Sample> result = client.getList("/api/x", Sample.class);

        assertEquals(2, result.size());
        assertEquals("B", result.get(1).name());
    }

    // TC-03 — POST usa o método POST e Content-Type JSON, e parseia a resposta.
    @Test
    void post_usesPostMethod_andParsesResponse() throws Exception {
        stubResponse(200, "{\"id\":9,\"name\":\"Nova\"}");

        Sample result = client.post("/api/x", new Sample(0, "Nova"), Sample.class);

        assertEquals(9, result.id());
        HttpRequest req = captureRequest();
        assertEquals("POST", req.method());
        assertEquals("application/json", req.headers().firstValue("Content-Type").orElse(null));
    }

    // TC-04 — resposta não-2xx vira ApiClientException com a mensagem do servidor.
    @Test
    void nonSuccess_throwsApiClientExceptionWithServerMessage() throws Exception {
        stubResponse(400, "{\"message\":\"Regra de negócio violada.\"}");

        ApiClientException ex = assertThrows(ApiClientException.class,
                () -> client.get("/api/x/1", Sample.class));
        assertTrue(ex.getMessage().contains("Regra de negócio violada."));
        assertEquals(400, ex.getStatusCode());
    }

    // TC-05 — sem sessão (login) não vai cabeçalho Authorization.
    @Test
    void noSession_omitsAuthorizationHeader() throws Exception {
        stubResponse(200, "{\"id\":1,\"name\":\"X\"}");

        anonymous.get("/api/public", Sample.class);

        HttpRequest req = captureRequest();
        assertTrue(req.headers().firstValue("Authorization").isEmpty());
    }

    @SuppressWarnings("unchecked")
    private void stubBytes(int status, byte[] body) throws Exception {
        HttpResponse<byte[]> response = mock(HttpResponse.class);
        when(response.statusCode()).thenReturn(status);
        when(response.body()).thenReturn(body);
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class))).thenReturn(response);
    }

    // TC-06 — postForList parseia um array JSON devolvido por um POST.
    @Test
    void postForList_parsesJsonArray_fromPost() throws Exception {
        stubResponse(200, "[{\"id\":1,\"name\":\"A\"},{\"id\":2,\"name\":\"B\"}]");

        List<Sample> result = client.postForList("/api/x/process", null, Sample.class);

        assertEquals(2, result.size());
        assertEquals("POST", captureRequest().method());
    }

    // TC-07 — getBytes devolve o corpo binário (PDF) e pede Accept: application/pdf.
    @Test
    void getBytes_returnsBinaryBody_withPdfAccept() throws Exception {
        byte[] pdf = {0x25, 0x50, 0x44, 0x46}; // "%PDF"
        stubBytes(200, pdf);

        byte[] result = client.getBytes("/api/print/payslip/9");

        assertArrayEquals(pdf, result);
        HttpRequest req = captureRequest();
        assertEquals("GET", req.method());
        assertTrue(req.headers().allValues("Accept").contains("application/pdf"));
    }

    record Sample(long id, String name) {}
}
