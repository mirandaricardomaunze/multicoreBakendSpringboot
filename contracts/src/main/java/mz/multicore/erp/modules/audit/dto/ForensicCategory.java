package mz.multicore.erp.modules.audit.dto;

public enum ForensicCategory {
    DOC_CANCELLATION("Cancelamento de Documento"),
    EXCESSIVE_DISCOUNT("Desconto Excessivo / Anormal"),
    STOCK_SHRINKAGE("Quebra Anormal de Stock"),
    CREDIT_OVERRIDE("Excepção de Limite de Crédito"),
    PAYMENT_VOID("Estorno / Anulação Financeira"),
    AUDIT_SECURITY("Alerta de Segurança / Acesso");

    private final String label;

    ForensicCategory(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
