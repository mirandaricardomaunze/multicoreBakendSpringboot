package mz.multicore.erp.desktop.config;

import org.springframework.core.env.Environment;
import java.net.URI;
import java.util.Locale;

public record DesktopApiConfig(String baseUrl) {

    public DesktopApiConfig {
        baseUrl = normalize(baseUrl);
    }

    public static DesktopApiConfig from(Environment environment) {
        String configured = environment.getProperty("desktop.api.base-url", "http://localhost:8080");
        String persisted = DesktopLocalSettings.readBaseUrl(DesktopLocalSettings.settingsFile());
        if ((configured == null || configured.isBlank() || "http://localhost:8080".equals(configured.trim()))
                && persisted != null && !persisted.isBlank()) {
            configured = persisted;
        }
        return new DesktopApiConfig(configured);
    }

    private static String normalize(String value) {
        String trimmed = value == null ? "" : value.trim();
        if (trimmed.isBlank()) {
            throw new IllegalArgumentException("O endereço da API do desktop é obrigatório.");
        }
        URI uri;
        try {
            uri = URI.create(trimmed);
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("O endereço da API do desktop é inválido.", ex);
        }
        String scheme = uri.getScheme() == null ? "" : uri.getScheme().toLowerCase(Locale.ROOT);
        String host = uri.getHost() == null ? "" : uri.getHost().toLowerCase(Locale.ROOT);
        boolean local = "localhost".equals(host) || "127.0.0.1".equals(host)
                || "::1".equals(host) || "[::1]".equals(host);
        if (host.isBlank() || uri.getRawUserInfo() != null || uri.getRawQuery() != null
                || uri.getRawFragment() != null || !("https".equals(scheme) || ("http".equals(scheme) && local))) {
            throw new IllegalArgumentException("A API remota exige HTTPS; HTTP é permitido apenas neste computador.");
        }
        return trimmed.endsWith("/") ? trimmed.substring(0, trimmed.length() - 1) : trimmed;
    }
}
