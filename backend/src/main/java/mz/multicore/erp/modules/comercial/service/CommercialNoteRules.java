package mz.multicore.erp.modules.comercial.service;

import mz.multicore.erp.architecture.exception.BusinessRuleException;
import mz.multicore.erp.modules.comercial.model.Invoice;
import mz.multicore.erp.modules.comercial.model.NoteStatus;
import mz.multicore.erp.modules.inventory.model.Warehouse;

/** Regras comuns às notas de crédito e de débito. */
final class CommercialNoteRules {

    private CommercialNoteRules() {}

    static void requireValidInvoice(Invoice invoice) {
        if (invoice != null && (invoice.getStatus() == null || !invoice.getStatus().isRealisedSale())) {
            throw new BusinessRuleException("A fatura " + invoice.getInvoiceNumber()
                    + " já não é válida: só se emitem ou aprovam notas sobre uma fatura válida.");
        }
    }

    /** Armazém da nota (o seu, senão o da fatura) — dá o centro de custo do lançamento (CC-05); {@code null} se nenhum. */
    static Long warehouseIdOf(Warehouse noteWarehouse, Invoice invoice) {
        Warehouse warehouse = noteWarehouse != null ? noteWarehouse : invoice == null ? null : invoice.getWarehouse();
        return warehouse == null ? null : warehouse.getId();
    }

    static void requirePending(NoteStatus status) {
        if (status != NoteStatus.PENDING_APPROVAL && status != NoteStatus.DRAFT) {
            throw new BusinessRuleException("Esta nota já foi rejeitada ou cancelada.");
        }
    }
}
