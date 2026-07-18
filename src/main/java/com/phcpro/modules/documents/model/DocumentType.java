package com.phcpro.modules.documents.model;

/** Tipo de documento com configuração de colunas independente. */
public enum DocumentType {
    COMMERCIAL("Documentos Comerciais"),
    POS_RECEIPT("Recibo POS");

    private final String label;

    DocumentType(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }
}
