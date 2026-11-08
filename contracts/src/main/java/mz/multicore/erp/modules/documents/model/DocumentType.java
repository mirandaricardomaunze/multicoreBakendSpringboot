package mz.multicore.erp.modules.documents.model;

/** Document type with an independent column configuration. */
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
