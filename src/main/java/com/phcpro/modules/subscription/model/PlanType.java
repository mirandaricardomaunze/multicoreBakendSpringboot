package com.phcpro.modules.subscription.model;

/** Plano de assinatura de uma empresa na plataforma. */
public enum PlanType {
    TRIAL("Avaliação"),
    BASIC("Básico"),
    PRO("Profissional"),
    ENTERPRISE("Empresarial");

    private final String label;

    PlanType(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }
}
