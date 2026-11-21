package mz.multicore.erp.modules.dataimport.dto;

/**
 * Estado de uma linha na pré-visualização de uma importação (IMPORTACAO_DADOS_SPEC §3.1).
 */
public enum ImportRowStatus {
    NOVO("NOVO"),
    JA_EXISTE("JÁ EXISTE"),
    ASSOCIADO("ASSOCIADO"),
    ERRO("ERRO");

    private final String label;

    ImportRowStatus(String label) {
        this.label = label;
    }

    /** Texto mostrado ao utilizador (PT-MZ). */
    public String label() {
        return label;
    }
}
