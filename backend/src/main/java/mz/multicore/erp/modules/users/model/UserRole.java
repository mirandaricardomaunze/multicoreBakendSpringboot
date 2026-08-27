package mz.multicore.erp.modules.users.model;

import mz.multicore.erp.architecture.exception.BusinessRuleException;

import java.util.Locale;

public enum UserRole {
    EMPLOYEE,
    SELLER,
    MANAGER,
    ADMIN;

    public static String normalize(String value) {
        if (value == null || value.isBlank()) {
            throw new BusinessRuleException("Perfil do utilizador é obrigatório.");
        }
        try {
            return valueOf(value.trim().toUpperCase(Locale.ROOT)).name();
        } catch (IllegalArgumentException ex) {
            throw new BusinessRuleException("Perfil inválido. Use EMPLOYEE, SELLER, MANAGER ou ADMIN.");
        }
    }
}
