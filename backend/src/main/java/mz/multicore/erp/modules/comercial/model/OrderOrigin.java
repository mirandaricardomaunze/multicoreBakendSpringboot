package mz.multicore.erp.modules.comercial.model;

/** Por onde entrou uma encomenda de cliente (docs/API_PUBLICA_WEBHOOKS_SPEC.md §3.4). */
public enum OrderOrigin {
    /** Registada por um operador no desktop (ou convertida de cotação). */
    DESKTOP,
    /** Criada por um sistema externo com uma chave da API pública. */
    API,
    /** Encomendada pelo cliente na loja online pública (docs/LOJA_ONLINE_SPEC.md §3.3). */
    LOJA_ONLINE;

    /**
     * Rótulo PT-MZ da origem para o desktop (docs/LOJA_ONLINE_SPEC.md §3.5): "Desktop", "API · {chave}" ou
     * "Loja online"; uma origem em falta conta como do desktop.
     */
    public static String label(OrderOrigin origin, String originName) {
        if (origin == null || origin == DESKTOP) {
            return "Desktop";
        }
        if (origin == API) {
            return originName == null || originName.isBlank() ? "API" : "API · " + originName;
        }
        return "Loja online";
    }
}
