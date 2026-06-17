package com.phcpro.desktop.config;

import org.springframework.core.env.Environment;

public record DesktopApiConfig(String baseUrl) {

    public static DesktopApiConfig from(Environment environment) {
        String configured = environment.getProperty("desktop.api.base-url", "http://localhost:8080");
        return new DesktopApiConfig(normalize(configured));
    }

    private static String normalize(String value) {
        String trimmed = value == null ? "" : value.trim();
        if (trimmed.isBlank()) {
            throw new IllegalArgumentException("O endereço da API do desktop é obrigatório.");
        }
        return trimmed.endsWith("/") ? trimmed.substring(0, trimmed.length() - 1) : trimmed;
    }
}
