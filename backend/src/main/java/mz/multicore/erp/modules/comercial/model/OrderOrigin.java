package mz.multicore.erp.modules.comercial.model;

/** Por onde entrou uma encomenda de cliente (docs/API_PUBLICA_WEBHOOKS_SPEC.md §3.4). */
public enum OrderOrigin {
    /** Registada por um operador no desktop (ou convertida de cotação). */
    DESKTOP,
    /** Criada por um sistema externo com uma chave da API pública. */
    API,
    /** Encomendada pelo cliente na loja online pública (docs/LOJA_ONLINE_SPEC.md §3.3). */
    LOJA_ONLINE
}
