package com.phcpro.modules.comercial.model;

/**
 * Estados da Guia de Remessa ao cliente. Espelha o ciclo da Guia de Transferência
 * ({@code TransferStatus}): nasce pendente e o stock só sai na aprovação.
 */
public enum DeliveryGuideStatus {

    PENDING_APPROVAL("Pendente de Aprovação"),
    APPROVED("Aprovada"),
    REJECTED("Rejeitada"),
    CANCELLED("Cancelada");

    private final String label;

    DeliveryGuideStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
