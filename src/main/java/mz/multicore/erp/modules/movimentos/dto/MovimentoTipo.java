package mz.multicore.erp.modules.movimentos.dto;

/**
 * Tipo de documento comercial agregado na vista unificada de movimentos.
 * O {@code label} é a designação em PT-MZ para apresentação directa na UI/PDF.
 */
public enum MovimentoTipo {

    FATURA("Fatura"),
    ENCOMENDA("Encomenda"),
    GUIA_REMESSA("Guia de Remessa"),
    NOTA_CREDITO("Nota de Crédito"),
    NOTA_DEBITO("Nota de Débito");

    private final String label;

    MovimentoTipo(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
