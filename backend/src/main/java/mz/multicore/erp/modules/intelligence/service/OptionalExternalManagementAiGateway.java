package mz.multicore.erp.modules.intelligence.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;
import java.util.Optional;

/** Adaptador externo opcional. Sem configuração, devolve vazio e o Service usa o analisador local. */
@Component
public class OptionalExternalManagementAiGateway implements ManagementAiGateway {

    private static final int MAX_RESPONSE_CHARS = 4_000;

    private final String endpoint;
    private final String apiKey;
    private final String model;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    @org.springframework.beans.factory.annotation.Autowired
    public OptionalExternalManagementAiGateway(
            @Value("${multicore.ai.endpoint:${MULTICORE_AI_ENDPOINT:}}") String endpoint,
            @Value("${multicore.ai.api-key:${MULTICORE_AI_API_KEY:}}") String apiKey,
            @Value("${multicore.ai.model:${MULTICORE_AI_MODEL:}}") String model,
            ObjectMapper objectMapper
    ) {
        this(endpoint, apiKey, model, objectMapper,
                HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build());
    }

    OptionalExternalManagementAiGateway(String endpoint, String apiKey, String model,
                                        ObjectMapper objectMapper, HttpClient httpClient) {
        this.endpoint = endpoint == null ? "" : endpoint.trim();
        this.apiKey = apiKey == null ? "" : apiKey.trim();
        this.model = model == null ? "" : model.trim();
        this.objectMapper = objectMapper;
        this.httpClient = httpClient;
    }

    @Override
    public Optional<String> answer(String question, ManagementAiContext context) {
        if (!configured()) return Optional.empty();
        try {
            String system = "Responda em português de Moçambique. Analise somente as métricas agregadas fornecidas. "
                    + "Não invente dados, não peça dados pessoais e não ordene lançamentos automáticos.";
            String metrics = objectMapper.writeValueAsString(context);
            Map<String, Object> payload = Map.of(
                    "model", model,
                    "messages", java.util.List.of(
                            Map.of("role", "system", "content", system),
                            Map.of("role", "user", "content", "Pergunta: " + question + "\nMétricas: " + metrics)),
                    "temperature", 0.2);
            HttpRequest request = HttpRequest.newBuilder(URI.create(endpoint))
                    .timeout(Duration.ofSeconds(20))
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(payload)))
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) return Optional.empty();
            JsonNode root = objectMapper.readTree(response.body());
            String answer = root.path("choices").path(0).path("message").path("content").asText("").trim();
            if (answer.isBlank()) return Optional.empty();
            return Optional.of(answer.substring(0, Math.min(answer.length(), MAX_RESPONSE_CHARS)));
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            return Optional.empty();
        } catch (Exception ignored) {
            return Optional.empty();
        }
    }

    private boolean configured() {
        if (endpoint.isBlank() || apiKey.isBlank() || model.isBlank()) return false;
        try { return "https".equalsIgnoreCase(URI.create(endpoint).getScheme()); }
        catch (IllegalArgumentException ignored) { return false; }
    }
}
