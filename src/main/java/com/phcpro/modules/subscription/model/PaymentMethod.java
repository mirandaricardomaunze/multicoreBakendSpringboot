package com.phcpro.modules.subscription.model;

/** Método usado para pagar a assinatura (registo manual pelo superadmin). */
public enum PaymentMethod {
    DINHEIRO("Dinheiro"),
    MPESA("M-Pesa"),
    EMOLA("e-Mola"),
    TRANSFERENCIA("Transferência bancária"),
    OUTRO("Outro");

    private final String label;

    PaymentMethod(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }
}
