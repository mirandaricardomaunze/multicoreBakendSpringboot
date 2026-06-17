package com.phcpro.modules.users.model;

import com.phcpro.architecture.exception.BusinessRuleException;

import java.util.Locale;

public enum UserRole {
    EMPLOYEE,
    MANAGER,
    ADMIN;

    public static String normalize(String value) {
        if (value == null || value.isBlank()) {
            throw new BusinessRuleException("Perfil do utilizador é obrigatório.");
        }
        try {
            return valueOf(value.trim().toUpperCase(Locale.ROOT)).name();
        } catch (IllegalArgumentException ex) {
            throw new BusinessRuleException("Perfil inválido. Use EMPLOYEE, MANAGER ou ADMIN.");
        }
    }
}
