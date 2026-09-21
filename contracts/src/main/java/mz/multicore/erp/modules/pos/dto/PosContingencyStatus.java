package mz.multicore.erp.modules.pos.dto;

/**
 * Estado do ciclo de vida de uma venda efectuada em regime de contingência local no POS.
 */
public enum PosContingencyStatus {
    /** Venda gravada na fila local e com talão provisório emitido, aguardando envio para a API. */
    PENDING_SYNC,
    /** Venda sincronizada com sucesso no backend e convertida em fatura oficial FT. */
    SYNCED,
    /** Venda com erro de negócio ou semântico que requer intervenção manual do operador/gerência. */
    REVISION_NEEDED
}
