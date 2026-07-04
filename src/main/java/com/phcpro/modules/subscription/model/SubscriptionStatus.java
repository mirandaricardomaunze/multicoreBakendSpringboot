package com.phcpro.modules.subscription.model;

/** Estado da assinatura. EXPIRED é derivado (validade no passado); os restantes são explícitos. */
public enum SubscriptionStatus {
    TRIAL("Avaliação"),
    ACTIVE("Activa"),
    SUSPENDED("Suspensa"),
    EXPIRED("Expirada");

    private final String label;

    SubscriptionStatus(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }

    /** Estados que permitem o login dos utilizadores da empresa. */
    public boolean allowsLogin() {
        return this == ACTIVE || this == TRIAL;
    }
}
