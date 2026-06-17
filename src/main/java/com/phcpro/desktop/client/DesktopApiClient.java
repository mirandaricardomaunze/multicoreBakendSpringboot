package com.phcpro.desktop.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.phcpro.desktop.config.DesktopApiConfig;
import com.phcpro.desktop.session.DesktopSession;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;

public class DesktopApiClient {

    private final DesktopApiConfig config;
    private final DesktopSession session;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public DesktopApiClient(DesktopApiConfig config, DesktopSession session) {
        this(config, session, HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build(),
                new ObjectMapper().findAndRegisterModules());
    }

    DesktopApiClient(DesktopApiConfig config, DesktopSession session, HttpClient httpClient,
                     ObjectMapper objectMapper) {
        this.config = config;
        this.session = session;
        this.httpClient = httpClient;
        this.objectMapper = objectMapper;
    }

    public <T> T get(String path, Class<T> responseType) {
        return send(request(path).GET().build(), responseType);
    }

    public <T> List<T> getList(String path, Class<T> elementType) {
        try {
            HttpResponse<String> response = sendRaw(request(path).GET().build());
            return objectMapper.readValue(response.body(),
                    objectMapper.getTypeFactory().constructCollectionType(List.class, elementType));
        } catch (IOException ex) {
            throw new ApiClientException("O servidor devolveu dados num formato inválido.", ex);
        }
    }

    public <T> T post(String path, Object body, Class<T> responseType) {
        return send(request(path)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(writeJson(body)))
                .build(), responseType);
    }

    public void post(String path, Object body) {
        post(path, body, Void.class);
    }

    public <T> T put(String path, Object body, Class<T> responseType) {
        return send(request(path)
                .header("Content-Type", "application/json")
                .PUT(HttpRequest.BodyPublishers.ofString(writeJson(body)))
                .build(), responseType);
    }

    public void delete(String path) {
        send(request(path).DELETE().build(), Void.class);
    }

    private HttpRequest.Builder request(String path) {
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(config.baseUrl() + normalizePath(path)))
                .timeout(Duration.ofSeconds(30))
                .header("Accept", "application/json");
        if (session != null && session.token() != null) {
            builder.header("Authorization", "Bearer " + session.token());
            if (session.activeCompanyId() != null) {
                builder.header("X-Company-Id", String.valueOf(session.activeCompanyId()));
            }
        }
        return builder;
    }

    private <T> T send(HttpRequest request, Class<T> responseType) {
        try {
            HttpResponse<String> response = sendRaw(request);
            if (responseType == Void.class || response.body() == null || response.body().isBlank()) {
                return null;
            }
            return objectMapper.readValue(response.body(), responseType);
        } catch (IOException ex) {
            throw new ApiClientException("O servidor devolveu dados num formato inválido.", ex);
        }
    }

    private HttpResponse<String> sendRaw(HttpRequest request) {
        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new ApiClientException(response.statusCode(), errorMessage(response.body()));
            }
            return response;
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new ApiClientException("A comunicação com o servidor foi interrompida.", ex);
        } catch (IOException | IllegalArgumentException ex) {
            throw new ApiClientException("Não foi possível comunicar com o servidor em " + config.baseUrl() + ".", ex);
        }
    }

    private String writeJson(Object body) {
        try {
            return objectMapper.writeValueAsString(body);
        } catch (IOException ex) {
            throw new ApiClientException("Não foi possível preparar o pedido para o servidor.", ex);
        }
    }

    private String errorMessage(String body) {
        try {
            JsonNode json = objectMapper.readTree(body);
            if (json.hasNonNull("message")) {
                return json.get("message").asText();
            }
        } catch (Exception ignored) {
            // Fall back to a stable user-facing message when the server did not return JSON.
        }
        return "O servidor recusou o pedido.";
    }

    private String normalizePath(String path) {
        return path.startsWith("/") ? path : "/" + path;
    }
}
