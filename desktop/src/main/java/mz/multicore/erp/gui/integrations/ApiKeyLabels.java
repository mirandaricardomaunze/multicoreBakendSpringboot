package mz.multicore.erp.gui.integrations;

import mz.multicore.erp.modules.publicapi.dto.ApiKeyDTO;
import mz.multicore.erp.modules.publicapi.model.ApiKeyPermission;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

/** PT-MZ texts of an API key as the screen shows them (docs/API_PUBLICA_WEBHOOKS_SPEC.md §4.4.1). */
final class ApiKeyLabels {

    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final String SECRET_PREFIX = "mk_live_";

    private ApiKeyLabels() {
    }

    static String permission(ApiKeyPermission permission) {
        return switch (permission) {
            case CATALOG_READ -> "Ler catálogo";
            case ORDERS_READ -> "Ler encomendas";
            case ORDERS_WRITE -> "Criar encomendas";
        };
    }

    /** Labels in the enum order, so every row reads the same way. */
    static String permissions(Set<ApiKeyPermission> permissions) {
        if (permissions == null) return "";
        return Arrays.stream(ApiKeyPermission.values()).filter(permissions::contains)
                .map(ApiKeyLabels::permission).collect(Collectors.joining(", "));
    }

    static String identification(ApiKeyDTO key) {
        return SECRET_PREFIX + key.keyPrefix() + "...";
    }

    static String created(ApiKeyDTO key) {
        return dateTime(key.createdAt()) + by(key.createdBy());
    }

    static String lastUse(ApiKeyDTO key) {
        return key.lastUsedAt() == null ? "Nunca usada" : dateTime(key.lastUsedAt());
    }

    static String state(ApiKeyDTO key) {
        if (key.active()) return "Activa";
        return "Revogada em " + dateTime(key.revokedAt()) + by(key.revokedBy());
    }

    static String dateTime(LocalDateTime value) {
        return value == null ? "" : DATE_TIME.format(value);
    }

    private static String by(String user) {
        return user == null || user.isBlank() ? "" : " por " + user;
    }
}
