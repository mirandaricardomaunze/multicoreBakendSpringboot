package mz.multicore.erp.desktop.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import mz.multicore.erp.desktop.config.DesktopApiConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpHeaders;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * OR-UI-02 (docs/CENTROS_CUSTO_ORCAMENTO_SPEC.md §4.3): os avisos {@code X-Operation-Warning} de uma resposta de
 * sucesso chegam descodificados a quem os mostra (o toast); respostas com erro não avisam.
 */
class OperationWarningRelayTest {

    private static final String WARNING = "Orçamento: o centro ADM atingiu 90% dos custos de Março/2026";

    private final List<String> received = new ArrayList<>();
    private final HttpClient httpClient = mock(HttpClient.class);
    private final DesktopApiClient client = new DesktopApiClient(new DesktopApiConfig("https://test.local"), null,
            httpClient, new ObjectMapper().findAndRegisterModules());

    @AfterEach
    void clearListener() {
        OperationWarningRelay.setListener(null);
    }

    @Test
    @DisplayName("OR-UI-02: cada cabeçalho X-Operation-Warning da resposta de sucesso chega descodificado ao toast")
    void orUi02_avisoDaRespostaChegaAoOuvinte() throws Exception {
        OperationWarningRelay.setListener(received::add);
        String encoded = URLEncoder.encode(WARNING, StandardCharsets.UTF_8);
        stub(201, Map.of(OperationWarningRelay.HEADER, List.of(encoded, URLEncoder.encode("Outro aviso", StandardCharsets.UTF_8))));

        client.post("/api/purchases", Map.of("x", 1), Void.class);

        assertThat(received).containsExactly(WARNING, "Outro aviso");
    }

    @Test
    @DisplayName("OR-UI-02: sem cabeçalho, sem ouvinte ou com erro não há aviso nem falha")
    void orUi02_semAvisoNemFalha() throws Exception {
        stub(200, Map.of(OperationWarningRelay.HEADER, List.of(URLEncoder.encode(WARNING, StandardCharsets.UTF_8))));
        client.get("/api/x", Void.class); // sem ouvinte: ignora

        OperationWarningRelay.setListener(received::add);
        stub(200, Map.of());
        client.get("/api/x", Void.class);
        assertThat(received).isEmpty();

        stub(400, Map.of(OperationWarningRelay.HEADER, List.of(URLEncoder.encode(WARNING, StandardCharsets.UTF_8))));
        assertThatThrownBy(() -> client.get("/api/x", Void.class)).isInstanceOf(ApiClientException.class);
        assertThat(received).as("resposta com erro descarta o aviso").isEmpty();
    }

    @SuppressWarnings("unchecked")
    private void stub(int status, Map<String, List<String>> headers) throws Exception {
        HttpResponse<String> response = mock(HttpResponse.class);
        when(response.statusCode()).thenReturn(status);
        when(response.body()).thenReturn("");
        when(response.headers()).thenReturn(HttpHeaders.of(headers, (name, value) -> true));
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class))).thenReturn(response);
    }
}
