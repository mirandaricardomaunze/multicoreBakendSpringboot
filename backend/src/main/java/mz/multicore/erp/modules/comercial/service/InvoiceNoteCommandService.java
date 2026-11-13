package mz.multicore.erp.modules.comercial.service;

import mz.multicore.erp.modules.comercial.model.CreditNote;
import mz.multicore.erp.modules.comercial.repository.CreditNoteRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Comandos de escrita de notas de crédito/débito para os outros módulos (POS): gravam por aqui, nunca pelo
 * {@link CreditNoteRepository} (docs/MODULE_DECOUPLING_SPEC.md §2). Leitura: {@link InvoiceNoteLookup}.
 */
@Service
@Transactional
public class InvoiceNoteCommandService {

    private final CreditNoteRepository creditNoteRepository;

    public InvoiceNoteCommandService(CreditNoteRepository creditNoteRepository) {
        this.creditNoteRepository = creditNoteRepository;
    }

    /** Marca a nota de crédito criada por uma devolução POS com a chave de idempotência e a impressão digital do pedido. */
    public void markCreditNotePosReturn(Long noteId, String idempotencyKey, String requestFingerprint) {
        CreditNote note = creditNoteRepository.findById(noteId)
                .orElseThrow(() -> new IllegalStateException("Nota de crédito criada não foi encontrada."));
        note.setPosReturnIdempotencyKey(idempotencyKey);
        note.setPosReturnRequestFingerprint(requestFingerprint);
        creditNoteRepository.save(note);
    }
}
