package com.phcpro.architecture.security;

import com.phcpro.architecture.exception.BusinessRuleException;

import java.util.Locale;
import java.util.Set;

/**
 * Centraliza verificacoes simples de role para regras sensiveis.
 * Permissoes granulares podem substituir este guard sem mudar os Services.
 */
public final class PermissionGuard {

    private static final Set<String> MANAGER_ROLES = Set.of("MANAGER", "ADMIN");
    private static final Set<String> ADMIN_ROLES = Set.of("ADMIN");

    private PermissionGuard() {
    }

    public static void requireManagerOrAdmin(String operation) {
        requireAny(MANAGER_ROLES, operation, "MANAGER ou ADMIN");
    }

    public static void requireAdmin(String operation) {
        requireAny(ADMIN_ROLES, operation, "ADMIN");
    }

    public static boolean isManagerOrAdmin() {
        return MANAGER_ROLES.contains(normalizedRole());
    }

    private static void requireAny(Set<String> allowedRoles, String operation, String allowedLabel) {
        String role = normalizedRole();
        if (!allowedRoles.contains(role)) {
            throw new BusinessRuleException(String.format(
                    "Sem permissão para %s. Esta operação requer perfil %s.",
                    operation == null || operation.isBlank() ? "continuar" : operation,
                    allowedLabel));
        }
    }

    private static String normalizedRole() {
        String role = CurrentUserContext.getRole();
        return role == null ? "" : role.trim().toUpperCase(Locale.ROOT);
    }
}
