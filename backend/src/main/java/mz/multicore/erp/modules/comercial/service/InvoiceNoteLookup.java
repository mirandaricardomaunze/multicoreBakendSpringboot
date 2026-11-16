package mz.multicore.erp.modules.comercial.service;

import mz.multicore.erp.modules.comercial.model.CreditNote;
import mz.multicore.erp.modules.comercial.model.DebitNote;
import mz.multicore.erp.modules.comercial.repository.CreditNoteRepository;
import mz.multicore.erp.modules.comercial.repository.DebitNoteRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Porta de leitura das notas que corrigem uma factura (crédito e débito) para os outros módulos
 * (docs/MODULE_DECOUPLING_SPEC.md §2). As notas de débito, só lidas pelos movimentos, ficam aqui como agregado
 * mais próximo. As entidades nunca saem para um Controller. Escrita: {@link InvoiceNoteCommandService}.
 */
@Service
@Transactional(readOnly = true)
public class InvoiceNoteLookup {

    private final CreditNoteRepository creditNoteRepository;
    private final DebitNoteRepository debitNoteRepository;

    public InvoiceNoteLookup(CreditNoteRepository creditNoteRepository, DebitNoteRepository debitNoteRepository) {
        this.creditNoteRepository = creditNoteRepository;
        this.debitNoteRepository = debitNoteRepository;
    }

    public Optional<CreditNote> findCreditNote(Long id) {
        return id == null ? Optional.empty() : creditNoteRepository.findById(id);
    }

    /** Nota de crédito com linhas e produtos já carregados, filtrada pela empresa. */
    public Optional<CreditNote> findCreditNoteWithLinesForCompany(Long id, Long companyId) {
        return creditNoteRepository.findByIdWithLinesAndCompanyId(id, companyId);
    }

    /** Nota de crédito já gerada por uma devolução POS com a mesma chave de idempotência. */
    public Optional<CreditNote> findCreditNoteByPosReturnKey(Long invoiceId, String idempotencyKey) {
        return creditNoteRepository.findByInvoiceIdAndPosReturnIdempotencyKey(invoiceId, idempotencyKey);
    }

    /** Notas de crédito da empresa, mais recentes primeiro. */
    public List<CreditNote> listCreditNotesForCompany(Long companyId) {
        return creditNoteRepository.findByCompanyId(companyId);
    }

    /** Notas de débito da empresa, mais recentes primeiro. */
    public List<DebitNote> listDebitNotesForCompany(Long companyId) {
        return debitNoteRepository.findByCompanyId(companyId);
    }
}
